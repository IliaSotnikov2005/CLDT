


#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <usr401a.h>

#include "libbcvs.h"
#include "manchester.h"

#include <arch/interrupt.h>

/*
	manch_add_cmd() - ��������� ������� � �����
  �������:
  0 - �����
  ME_INVALID_PARAMETR - �������� ��������
*/
int
manch_add_cmd(
   int batch,       //������������� ������
   u_short flag,    //����������� �������� MANCH_READ/MANCH_WRITE
   u_short address, //=0-30 - ����� ��
   u_short cmd,     //=1-30 - ��������
   u_short *buf,    //!=0   - ����� ������
   u_long length    //=1-32 - ���������� ���� ������
 )
{
 register int ret=0,i;
 mkioBatchDesc_t * p = (void*)batch;
 bt401com * c;

 mkio_info("enter");

//�������� ������� ���������
 mkio_assert_r((batch&3)==0, ME_INVALID_PARAMETER);
 mkio_assert_r(batch!=0    , ME_INVALID_PARAMETER);
 mkio_assert_r(address<=30 , ME_INVALID_PARAMETER);
 mkio_assert_r(cmd!=0      , ME_INVALID_PARAMETER);
 mkio_assert_r(cmd<=30     , ME_INVALID_PARAMETER);
 mkio_assert_r(buf!=0      , ME_INVALID_PARAMETER);
 mkio_assert_r(((((int)buf)&1)==0), ME_INVALID_PARAMETER);
 mkio_assert_r(length!=0   , ME_INVALID_PARAMETER);
 mkio_assert_r(length<=32  , ME_INVALID_PARAMETER);

//�������� ��������� �� ��������� ������
 mkio_assert_r(p->sgn==0x000BA7C1, ME_INVALID_PARAMETER);
 mkio_assert_r(p->nMKIO<=3       , ME_INVALID_PARAMETER); 

//�������� ����� � �������� ���
 mkio_assert_g(pthread_mutex_lock(&p->mtx)==0, ME_INVALID_PARAMETER, Error);
 mkio_assert_g(p->started==0, ME_INVALID_PARAMETER, Error); //��� �������
 mkio_assert_g(p->del==0    , ME_INVALID_PARAMETER, Error); //��� �����

//������� ������� � �������
 i=p->chain.ck_len++; //����� ����� ������
 
 mkio_assert_g(i<MANCH_BATCH_MAX, ME_INVALID_PARAMETER, Error);

 c=&p->chain.ck[i];   //��������� �� ������� ������� � ������

 c->com = (flag==MANCH_WRITE) ? (BC_RT) : (RT_BC) ;
 c->rt1 = address; /* ����� ��1*/ 
 c->sa1 = cmd;     /* �������� ��1 */ 
 c->size= length;  /* ���-�� ���� ������ (1..32) */
 c->buf = (void*)buf;/* ��������� �� ����� ������*/
 ret=0; //������

Error:
 pthread_mutex_unlock(&p->mtx); //��������� �����
 mkio_info("exit");
 return ret;
}

