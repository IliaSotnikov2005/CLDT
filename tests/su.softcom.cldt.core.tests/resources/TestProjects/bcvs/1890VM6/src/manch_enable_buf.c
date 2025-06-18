




#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <pthread.h>

#include "libbcvs.h"
#include "manchester.h"



extern u_short * mkioRTbuf[4][32];
extern pthread_t mkioRTThread[4];


/*
	manch_enable_buf() - ��������� ������ ������ �� ��������� ����-��
*/
int
manch_enable_buf(
   int device, 
   u_char subaddress,
   void *buf
 )
{
 int ret=0;
 mkioIUDesc_t     * pi; //���������� ��

 //������:
 unsigned nMKIO=device;       //���������� ����� ����
 unsigned nMod =(nMKIO>>1)&1; //������ ����
 unsigned nIU  =nMKIO&1;      //�� �� ������
 mkio_info("enter");
 //�������� ���������
 mkio_assert_r(device<=3, ME_INVALID_PARAMETER);
 mkio_assert_r(device>=0, ME_INVALID_PARAMETER);
 mkio_assert_r(subaddress>0, ME_INVALID_PARAMETER);
 mkio_assert_r(subaddress<=30, ME_INVALID_PARAMETER);

 //�������� ��
 mkio_assert_g(pthread_mutex_lock(&mkio_mutex[nMKIO])==0, 
               DE_DRIVER_SPECIFIC, Error);

 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
 //�������� ��������� ��������� ��
  //�� �������������������?
 mkio_assert_g(pi->ini==1, DE_DRIVER_SPECIFIC, Error);
  //������ �� �������?
 mkio_assert_g(mkioRTThread[nMKIO]!=0, DE_DRIVER_SPECIFIC, Error);

 mkioRTbuf[nMKIO][subaddress] = buf;

Error:
 pthread_mutex_unlock(&mkio_mutex[nMKIO]);
 mkio_info("exit");
 return ret;
}

