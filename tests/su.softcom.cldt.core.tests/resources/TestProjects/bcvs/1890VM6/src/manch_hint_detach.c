



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


/*
	manch_hint_detach() - �������� ������� ��������� ������
*/
int
manch_hint_detach(
   int device,   //������������� ����� � ����
   int cookie    //�� manch_hint_attach()
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
 mkio_assert_r(device==cookie, ME_INVALID_PARAMETER);
 mkio_assert_r(device<=3, ME_INVALID_PARAMETER);
 mkio_assert_r(device>=0, ME_INVALID_PARAMETER); //gmu

 //�������� ��
 mkio_assert_g(pthread_mutex_lock(&mkio_mutex[nMKIO])==0, 
               DE_DRIVER_SPECIFIC, Error);

 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
 //�������� ��������� ��������� ��
  //�� �������������������?
 mkio_assert_g(pi->ini==1, DE_DRIVER_SPECIFIC, Error);

 pi->rt_func=0;
//ret=0;

Error:
 pthread_mutex_unlock(&mkio_mutex[nMKIO]);
 mkio_info("exit");
 return ret;
}
