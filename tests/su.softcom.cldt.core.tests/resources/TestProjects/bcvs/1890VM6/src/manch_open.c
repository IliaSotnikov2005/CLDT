

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>

#include <usr401a.h>
#include <pthread.h>

#include "libbcvs.h"
#include "manchester.h"




//��������� �������� ����
mkioDrvDesc_t mkioDrvDesc;


pthread_mutex_t manch_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t mkio_mutex[4] = {
 PTHREAD_MUTEX_INITIALIZER,
 PTHREAD_MUTEX_INITIALIZER,
 PTHREAD_MUTEX_INITIALIZER,
 PTHREAD_MUTEX_INITIALIZER,
};


pthread_once_t mkioBatchServer_once_control = PTHREAD_ONCE_INIT;//����������� ���������� ��� ������� ������� mkioBatchServer

//����������� ���������� ��� ������� ������� mkioRTServer0-mkioRTServer3
pthread_once_t mkioRTServer0_once_control = PTHREAD_ONCE_INIT;
pthread_once_t mkioRTServer1_once_control = PTHREAD_ONCE_INIT;
pthread_once_t mkioRTServer2_once_control = PTHREAD_ONCE_INIT;
pthread_once_t mkioRTServer3_once_control = PTHREAD_ONCE_INIT;

pthread_once_t mkioDrvInit_once_control = PTHREAD_ONCE_INIT;// ����������� ���������� ��� ������������� ��������

void mkioDrvInit(void);

/*
	manch_open() - ��������� ����� � ��������� ����

 �������:
  >=0                 - �����: ������������� ����� � ����
  ME_INVALID_PARAMETR - �������� ���������
  ME_INVALID_EXCHANGE - �� �������� ��������� ��� ������ 
                        bt401Drv(), bt401Init(),bt401DevCreate(), open(), ioctl()   //gmu �������� bt401DevCreate
  DE_DRIVER_SPECIFIC  - ���������� ����� ��� ioctl() ������ 0
  DE_ALREADY_USED     - ������������ ������� ��������� ������ ������ ����
*/
int
manch_open(
   int device, /*=0,1 - ����� ���������� (������ ����)*/
   int chanel, /*=0,1 - ����� �� �� ���������� device*/
   u_char rgm  /*=MANCH_BC/MANCH_MT/MANCH_RT... - ����� ������ ����*/
 )
{
 int ret=0;

 //������:
 unsigned nMod =device;        //������ ����
 unsigned nIU  =chanel;        //�� �� ������
 unsigned nMKIO=(nMod<<1)|nIU; //���������� ����� ����

 //�������� ��:
 mkioDrvDesc_t    * pd; //���������� ��������
 mkioModuleDesc_t * pm; //���������� ������
 mkioIUDesc_t     * pi; //���������� ��
 char             * pn; //��� ����� ��

//����� �� ��� ioctl()
bt401mode manchMode = {
 .mode=NON,     /* ����� ��:
                   BC - ���������� ������,
                   RT - ��������� ����������,
                   MT - ������� ������, 
                   NON - ��������� ��������� */
 .wait=WAIT_40, /* ����� �������� �� � ������ BC:
                   WAIT_40 - 40���,
                   WAIT_80 - 80 ���,
                   WAIT_160 - 160��� ,
                   WAIT_UNLIM - ������������� */
 .rep=0,      /* ���������� ������� ��������� ��� ��������� ������ */
 .dev_n=0,    /* ����� �� (� ������ RT) */
 .grp_en=1,   /* ���������� ������ ���������� ��������� (RT) =1 21.11.2013 �� ������� �������*/
 .grp_dif=0,  /* (�� �����������)*/
 .mode_wait=1,/* ???����� �������� ���������� ������ �������*/
};
 mkio_info("enter");

 //�������� ���������
 mkio_assert_r(nMod<=1 ,ME_INVALID_PARAMETER);
 mkio_assert_r(nIU <=1 ,ME_INVALID_PARAMETER);
 mkio_assert_r(rgm <=MANCH_RGM_MAX, ME_INVALID_PARAMETER);

 mkio_assert_g(pthread_mutex_lock(&manch_mutex)==0,
               DE_DRIVER_SPECIFIC, Error);
 mkio_assert_g(pthread_mutex_lock(&mkio_mutex[nMKIO])==0,
               DE_DRIVER_SPECIFIC, Error);

 pthread_once(&mkioBatchServer_once_control, mkioBatchServer_start);
 mkio_assert_g(mkio_mq!=((mqd_t)-1),ME_INVALID_PARAMETER,Error);
 
 
 //������������� ��������
 pd = &mkioDrvDesc;
 pthread_once(&mkioDrvInit_once_control, mkioDrvInit); //������������ ������������� (������� mkioDrvInit ���������� ������ 1 ���)
 mkio_assert_g(pd->err==0, ME_INVALID_EXCHANGE, Error);


 //������������� ������
 pm = &mkioDrvDesc.Module[nMod];
 if(pm->ini==0) {
    pm->nMod=nMod;
    pm->offset=0;      //=0 - ��� ������ ���23-401???
    pm->mod=1;         //=1 - ��� ������ ���23-401???��� ������???
    pm->dev=nMod+1;    //=1,2 - ����� PMC-�����, �� ������� ���������� ������???
    pm->interrupt=nMod+1;//=1,2 - ����� ����� ���������� ���� PCI (INTB,INTC)  //gmu: ��� bcvs 
    /////////////////////////////////////////////////////////////////////////
    mkio_info("bt401Init");   
    pm->module = bt401Init(pm->dev, pm->interrupt, pm->offset, 0, pm->mod);//
    mkio_assert_g(pm->module>=0, ME_INVALID_EXCHANGE, Error);
    pm->IUDesc[0].ini=0;
    pm->IUDesc[1].ini=0;
    pm->ini=1;
 };


 //������������� ��
 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
 if(pi->ini==0) {
    //������ �� ������������������ - ������
    mkio_assert_g(pm->module>=0, ME_INVALID_EXCHANGE, Error);
    pi->nMod =nMod;      //����� ������ ����
    pi->nIU  =nIU;       //����� �� �� ������
    pi->nMKIO=nMKIO;     //���������� ����� ����
    pi->rgm  =MANCH_NONE;//����� ������ �� �����
    pn=&pi->name[0];

    // ������� �� ������������� ��� /dev/mcn, ��� n ���������� ����� ���� (/dev/mc0.../dev/mc3)
    // ��� ����������� �� ���� ����� ����� ������ ���� ����� � ���������� ������ manch_open
    *pn++='/'; *pn++='d'; *pn++='e'; *pn++='v'; *pn++='/'; *pn++='m';  *pn++='c';
    *pn++='0'+nMKIO;
    *pn=0;

    pi->rdBufSize=1024;  //������ ����������� ���������� ������ ������
    pi->wrtSubBufSize=33;//������ ���������� ��������� ������� ������ 

    /////////////////////////////////////////////////////
    mkio_info("bt401DevCreate");
    pd->err = bt401DevCreate(&pi->name[0], pm->module, nIU,  //��������� �������??  //gmu: ?? 3 �������� �� ����� ��
                    pi->rdBufSize, pi->wrtSubBufSize); //

    mkio_assert_g(pd->err==0,ME_INVALID_EXCHANGE, Error);
    
    pi->ini=1;
 }

   // ����� ������������� ���������� ������� ����� �� ��������� �����������
 if(pi->open==0){ 
    /////////////////////////////////////////////////////
    pi->fd = open(&pi->name[0], O_RDWR, 0); //���������� ����� ��    
    mkio_assert_g(pi->fd>=0, ME_INVALID_EXCHANGE, Error); 

    pi->open=1;  
  }

 //����� ������ ������ ��
 if(rgm==MANCH_NONE) { //������� � ��������� ���������
    manchMode.mode=BC; //NON - ��� ���������� ����� ��2000!!!;
    mkio_info("MANCH_NON(BC)");
 }else{
    //��� ����� �� ��� ��� ����� ������ �����?
    //(�������� ����� ������ ����� MANCH_NONE)
    mkio_assert_g(pi->rgm==MANCH_NONE, DE_ALREADY_USED, Error);
    if(rgm==MANCH_BC) {
       manchMode.mode = BC;
       mkio_info("MANCH_BC");
    }else if(rgm==MANCH_MT) {
       manchMode.mode = MT;
       mkio_info("MANCH_MT");
    }else{
       manchMode.dev_n = rgm; //���
       mkio_info("MANCH_RT");      
       manchMode.mode = RT;
    }
 } //TODO �������� ������ �������� ����� ������ ����� MANCH_NONE

 //��������� ������ ������ ��
 mkio_assert_g(pi->fd>=0, DE_DRIVER_SPECIFIC, Error);
 mkio_info("FIORESET");
 ioctl(pi->fd, FIORESET, 0 ); //����� ��
 mkio_info("FIOSETMODE401A");
 mkio_assert_g(ioctl(pi->fd, FIOSETMODE401A, (int)&manchMode)==0,
               ME_INVALID_EXCHANGE, Error);
 pi->rgm=rgm;
 ret=nMKIO;

 if(manchMode.mode==RT) {
    switch(nMKIO) {
       case 0: pthread_once(&mkioRTServer0_once_control, mkioRTServer0_start);
               break;
       case 1: pthread_once(&mkioRTServer1_once_control, mkioRTServer1_start);
               break;
       case 2: pthread_once(&mkioRTServer2_once_control, mkioRTServer2_start);
               break;
       case 3: pthread_once(&mkioRTServer3_once_control, mkioRTServer3_start);
               break;
    }
 }

Error:
 pthread_mutex_unlock(&mkio_mutex[nMKIO]);
 pthread_mutex_unlock(&manch_mutex);
 mkio_info("exit");
 return ret;
}

//----------------------------------------------------------------------------

/*
   ������� ��� ��������� ������������� �������� � �������� ������
   �������� ������� bt401Drv ��� ��������� ������������� ��������
   
*/
void mkioDrvInit(void)
{
   mkioDrvDesc_t    * pd; //���������� ��������
   pd = &mkioDrvDesc;

   //////////////////////
   mkio_info("bt401Drv");
   pd->err=bt401Drv(); //
   ////////////////////// 
   pd->Module[0].ini=0; //�� ���������������
   pd->Module[1].ini=0; //�� ���������������
   pd->ini=1; 
}



