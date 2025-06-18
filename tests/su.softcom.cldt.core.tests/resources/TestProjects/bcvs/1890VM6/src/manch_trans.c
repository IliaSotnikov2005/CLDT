

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



/*
	manch_trans() - ����� ��������������� ������� � ��
*/
int
manch_trans(
   int device,      //������������� ����� � ����
   u_short address, //=0-31 - ����� ��
   u_short cmd,     //=1-30 - ��������
   u_short *buf,    //!=0   - ����� ������
   u_long length,   //=1-32 - ���������� ���� ������
   u_char np        //����������� ��������:
		    //   MANCH_WRITE - KK->�� (manch_send)
 )                  //   MANCH_READ  - ��<-�� (manch_recive)
{
 int ret=0, er=0, os;

 //������:
 unsigned nMKIO=device;       //����
 unsigned nMod =(nMKIO>>1)&1; //������ ����
 unsigned nIU  =nMKIO&1;      //�� �� ������

 //�������� ��:
 mkioIUDesc_t     * pi; //���������� ��

bt401transf tr = {
  .buf=buf,     /* ��������� �� ����� ������ */
  .rt1=address, /* ����� ��1 (�� 0 �� 30) */
  .sa1=cmd,     /* ����� ������������� 1 (�� 1 �� 30) */
  .rt2=0,       /* ����� ��2 (��� ������ ������� ���� ��-��) */
  .sa2=0,       /* ����� ������������� 2 (��� ������ ������� ���� ��-��)*/ 
  .size=length, /* ���������� ���� ������ (�� ����� 320) */ 
  .delay=0,     /* �������� �������������� ����� ���������� ������ ������� ������ (1 = 10 ���) */ 
 };

 mkio_info("enter");

 //�������� ���������
 mkio_assert_r(nMod<=1    , ME_INVALID_PARAMETER);
 mkio_assert_r(nIU <=1    , ME_INVALID_PARAMETER);
 mkio_assert_r(address<=30, ME_INVALID_PARAMETER);
 mkio_assert_r(cmd!=0     , ME_INVALID_PARAMETER);
 mkio_assert_r(cmd<=30    , ME_INVALID_PARAMETER);
 mkio_assert_r(buf!=0     , ME_INVALID_PARAMETER);
 mkio_assert_r(((((int)buf)&1)==0), ME_INVALID_PARAMETER);
 mkio_assert_r(length!=0  , ME_INVALID_PARAMETER);
 mkio_assert_r(length<=32 , ME_INVALID_PARAMETER);
 mkio_assert_r(np<=1      , ME_INVALID_PARAMETER);

 //�������� manch_close()
 mkio_assert_g(pthread_mutex_lock(&mkio_mutex[nMKIO])==0,
               HE_SPECIFIC_7, 
               Error);

 //�������� ��������� ��
 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
 mkio_assert_g(pi->ini==1, HE_SPECIFIC_0, Error);
 mkio_assert_g(pi->fd >=0, HE_SPECIFIC_1, Error);

 //������ ��������� � ����
 er=0; ///------
 if(np==MANCH_WRITE) {
    mkio_info("???FIOSEND");
    er=ioctl(pi->fd, FIOSEND, (int)&tr);
 }else{ //MANCH_READ?
    mkio_info("???FIORECEIVE ");
    er=ioctl(pi->fd, FIORECEIVE , (int)&tr);
 }
 if(er!=0) { //���� ������ ������:
    mkio_info("FIOGETSTATUSIO0");
    os=ioctl(pi->fd,FIOGETSTATUSIO,0); //������ �������� �����
    //printf("os= 0x%x\n",os); //gmu: ��� �������
    if(os==0) { //��� ��
       mkio_info("no answer word");
       ret=HE_SPECIFIC_2; //???
       goto Error;
    }else{
       if((os&(RW_BUSY|RW_ABERR|RW_GET_CONTROL|RW_RTERR))!=0) {
          mkio_info("invalid answer word");
          ret=HE_SPECIFIC_3; //???
          goto Error;
       }
    }
 }
// ret=0;

Error:
 pthread_mutex_unlock(&mkio_mutex[nMKIO]);
 mkio_info("exit");
 return ret;
}
