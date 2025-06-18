
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <pthread.h>

#include <usr401a.h>

//#include <mqueue.h>

#include "libbcvs.h"
#include "manchester.h"

u_short *mkioRTbuf[4][32];

void 
mkioRTServer(int *arg)
{
 int i;
 int nMKIO =  *(arg); //gmu: � �������� �������� ���������� �� ���������� � �� �����
 unsigned nMod =(nMKIO>>1)&1; //������ ����
 unsigned nIU  =nMKIO&1;      //�� �� ������
 int ks; //��������� �����/����-��� � �����
 u_short sa,cw,tr,sd;
 int ret;
 int er;

 u_short buf[64];
 u_short * pbuf;

 bt401block rs;
 bt401open  ou;


 mkioIUDesc_t * pi=&mkioDrvDesc.Module[nMod].IUDesc[nIU]; //���������� ��

 PCALLBACKFUNC func;

/*
 SA_MODE_DOUBLE: � ��������� ������ ����������� � ����������� � ���������
 ���������� �� ��� ������. ���� ����� �������� �������� � ������������
 ����������� �� ��� ������/�������� �/�� ������, ������ - �����������/��������
 ����������. �� ����� ������ ������ ��������, ��������, ��� ������ ������
 ������. "������������" ������ ���������.
*/
 mkio_info("FIOOPENSA");
 for(i=1; i<31; i++) {
    ou.sa = i;
    ou.mode_rd = SA_MODE_DOUBLE;
    ou.mode_td = SA_MODE_DOUBLE;
    ou.size_rd = 32;
    ou.size_td = 32;
    ret = ioctl(pi->fd, FIOOPENSA, (int)&ou);     
 }

 //����� ������ ��� ���� ���������� �� ���� � ��� ��
 mkio_info("FIOCLRRDBUF");
 ret = ioctl(pi->fd, FIOCLRRDBUF,0); 

 //������ ������ ��� ���� ������������(???) �� ������.
 mkio_info("FIOCLRWRTBUF");
 for(i=1;i<31;i++) {
    ret = ioctl(pi->fd, FIOCLRWRTBUF,i);//????   
 }

 //���������� � ����� ������ ������� "�������� ������"
  mkio_info("FIORTSAVEALLCOM");
  ret = ioctl(pi->fd, FIORTSAVEALLCOM,1); 
 //�� ������������� ��������� ��???
  mkio_info("FIOBUSYSET");
  ret = ioctl(pi->fd, FIOBUSYSET, 0);    
 for(;;) {
    mkio_info("wait");
    ks=-1; //����� �����   
    er=ioctl(pi->fd, FIOWAITCOM, (int)&ks);    
    if(er!=0)  continue;  //������ �� ��������
    mkio_info("recieve");     
    //���-�� ��������:
    rs.buf=buf; //����� ��� ��������� ������
    er = ioctl (pi->fd, FIOGETBLOCK, &rs); //������ �� ������
    sa=rs.sa;    //��������
    cw=rs.size;  //�����

    //-----------------------------------------------------
    //��������: ��� �������� �� ������ ����� ��������� ��!
    //  ������� � �� ���������� ��������:
    ks= (ks&0xFC1F) | ((sa&0x1f)<<5);
    //-----------------------------------------------------

    if( (sa==0) && (cw==0) ) continue; //����� ������ ����
    if( sa==0 ) {
       ks=rs.buf[0]; // "�������� ������" / "������� ����������"
       if(cw==2) sd=rs.buf[1];  //�� ���������� � �������� ����������
    }
    //������ ����������� ks
    tr=(ks>>10)&0x01; //����������� ��������
    //printf("ks = 0x%x \n",ks);  //gmu: ��� ������� 
 
    sa = ((ks>>5)&0x1f); //gmu: ��� ����� �� ��������� ������������� ������� ������

    if(sa==0x00) continue; //��� ������� ����������
    if(sa==0x1f) continue; //��� ������� ����������
    if(tr==1) { //��<-��  (������ �� ��)
       mkio_info("read OY");      
       pbuf=mkioRTbuf[nMKIO][sa];      

       if((unsigned)pbuf!=0) { //����� ��������������� (func �� ��������)
          //��������� ������ �� ������������������� ������ � ����� ��
          mkio_info("buf enable");  //gmu 
          rs.buf =pbuf;
          rs.sa  =sa;
          rs.size=32;
          ret=ioctl(pi->fd, FIOSETBLOCK, (int)&rs);          
          continue; //???
       }
       mkio_info("buf disable");  //gmu
    }else {mkio_info("write OY");}

    func=pi->rt_func;
    if(func==0) continue;
    //����� ������� ��������� ������
    ret=func(ks, &buf[0], cw);
    if(ret==0) continue; //������ �� ������
    if(ret>0) {
       //????????������� Trigger �� proxy????????
       continue;
    }    
    //����� ret<0
    if(tr==1) { //���� ��<-��: ���������� ����� ������ �� func ��� ��
       cw=-ret; //���������� ���� � buf �� func 
       rs.buf =buf;
       rs.sa  =sa;
       rs.size=cw;
       mkio_info("write buf");  //gmu 
       ioctl(pi->fd, FIOSETBLOCK, (int)&rs);
    }
 }
}




pthread_t mkioRTThread[4];
static pthread_attr_t attr;
static struct sched_param shparam = {.sched_priority = 102}; //?????


///int mkioRT_num=2; //???



void mkioRTServer0_start(void)
{
 mkioRTServer_start(0);
}

void mkioRTServer1_start(void)
{
 mkioRTServer_start(1);
}

void mkioRTServer2_start(void)
{
 mkioRTServer_start(2);
}

void mkioRTServer3_start(void)
{
 mkioRTServer_start(3);
}



int mkioRTn[4]={0,1,2,3};

/*
	mkioRTServer_start() - ������ �������� ����-��
*/
void mkioRTServer_start(int i)
{
//--- int i=mkioRT_num;
 if(mkioRTThread[i]!=0) return; //����� ��� ������

 //������������� ���������� 
 pthread_attr_init(&attr);
 pthread_attr_setdetachstate(&attr,PTHREAD_CREATE_DETACHED);
 pthread_attr_setfpustate(&attr, PTHREAD_FPU_DISABLE);
 pthread_attr_setschedparam(&attr, &shparam);

 //�������� ������
 mkio_info("create mkioRTServer");
 pthread_create(&mkioRTThread[i], &attr, (void*)&mkioRTServer, &mkioRTn[i]);

}



