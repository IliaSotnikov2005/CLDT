
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

int
manch_delete_batch(
   int batch 
 )
{
 int ret=0;
 mkioBatchDesc_t * p = (void*)batch;

 mkio_info("enter");

//�������� ������� ���������
 mkio_assert_r((batch&3)==0, ME_INVALID_PARAMETER);
 mkio_assert_r(batch!=0    , ME_INVALID_PARAMETER);

//�������� ��������� �� ��������� ������
 mkio_assert_r(p->sgn==0x000BA7C1, ME_INVALID_PARAMETER);
 mkio_assert_r(p->nMKIO<=3       , ME_INVALID_PARAMETER);

//�������� ����� � �������� ���
 mkio_assert_g(pthread_mutex_lock(&p->mtx)==0, ME_INVALID_PARAMETER, Error);
 //���������� ����� ������ �������
 mkio_assert_g(p->started==0, ME_INVALID_PARAMETER, Error);
 //�������� ����� �� ����� �������
 mkio_assert_g(p->del==0    , ME_INVALID_PARAMETER, Error);

 p->del=1; 
 pthread_mutex_unlock(&p->mtx); 
 manch_start(batch); //�������� ������ ������ �������
 return ret;

Error:
 pthread_mutex_unlock(&p->mtx); 
 mkio_info("exit");
 return ret;
}

