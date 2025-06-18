

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
  manch_close() - ������ ����� � ��������� ����
  �������:
  0 - �����
  ME_INVALID_PARAMETR - �������� ��������
  DE_DRIVER_SPECIFIC - ������:
    - �� �� �������������������
    - �������� ���������� ����� ��
    - �� ������� ������� ���� ��
*/
int
manch_close(
   int device
 )
{
 int ret=0;
 //������:
 unsigned nMKIO=device;       //���������� ����� ����
 unsigned nMod =(nMKIO>>1)&1; //������ ����
 unsigned nIU  =nMKIO&1;      //�� �� ������

//����� �� ��� ioctl()
bt401mode manchMode = {
 .mode=NON,     /* ����� ��:
                   BC - ���������� ������,
                   RT - ��������� ����������,
                   MT - ������� ������, 
                   NON - ��������� ��������� */
//TODO:NON �������� �� BC (NON - ������ ��� ��2000)
 .wait=WAIT_40, /* ����� �������� �� � ������ BC:
                   WAIT_40 - 40���,
                   WAIT_80 - 80 ���,
                   WAIT_160 - 160��� ,
                   WAIT_UNLIM - ������������� */
 .rep=0,      /* ���������� ������� ��������� ��� ��������� ������ */
 .dev_n=0,    /* ����� �� (� ������ RT) */
 .grp_en=0,   /* ���������� ������ ���������� ��������� (RT) */
 .grp_dif=0,  /* (�� �����������)*/
 .mode_wait=1,/* ???����� �������� ���������� ������ �������*/
};

 //�������� ��:
 mkioIUDesc_t     * pi; //���������� ��

 mkio_info("enter");

 //�������� ���������
 mkio_assert_r(device<=3, ME_INVALID_PARAMETER);
 mkio_assert_r(device>=0, ME_INVALID_PARAMETER); //gmu �������, ��� ��������� ����� �� �������������

 //�������� ��
 pthread_mutex_lock(&manch_mutex);
 pthread_mutex_lock(&mkio_mutex[nMKIO]);

 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
 //�������� ��������� ��������� ��
  //�� �������������������?
 mkio_assert_g(pi->ini==1, DE_DRIVER_SPECIFIC, Error);
 mkio_assert_g(pi->open==1, DE_DRIVER_SPECIFIC, Error);
  //������ ���������� �����?
 mkio_assert_g(pi->fd>=0, DE_DRIVER_SPECIFIC, Error);

 //����� ��
 mkio_info("FIORESET");
 ioctl(pi->fd, FIORESET, 0 );
 mkio_info("FIOSETMODE401A");
 ioctl(pi->fd, FIOSETMODE401A, (int)&manchMode);
 mkio_info("FIOSETMODE401A end");
 pi->rgm=MANCH_NONE;
 pi->open=0; //���� ��� �� �� ������

 //������� ���� ��
 mkio_info("close");
 close(pi->fd);

//TODO: ������� �� ������ �������� mkioThread[] mkioRTThread[] ???
//      ���������������� �� ��������������� pthread_once_t ???

Error:
 pthread_mutex_unlock(&mkio_mutex[nMKIO]);
 pthread_mutex_unlock(&manch_mutex);
 mkio_info("exit");
 return ret;
}

