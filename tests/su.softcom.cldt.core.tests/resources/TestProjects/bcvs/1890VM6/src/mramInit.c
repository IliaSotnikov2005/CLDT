

#include <stdio.h>
#include <pciLib.h>
#include "psc.h"
#include <pthread.h>


pthread_mutex_t mram_mutex_rd = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t mram_mutex_wr = PTHREAD_MUTEX_INITIALIZER;

volatile unsigned mram_flag;

/*
	������������� ����������� MRAM
	������������� � �������� ��������� ���������� ����������� MRAM.
	�������� pscInit().
	��������! ���������� MRAM �� ��������.
 �������:
  0 - �����
 -1 - errno:
        ENODEV        - pscInit(): ���������� �� ���������� �� ���� PCI
        ENOMEM        - pscInit(): ������������ ������ � ������� PCI
        EADDRNOTAVAIL - �������� ��� ����������
*/
int
mramInit(
  unsigned cpu //= 0/1 ��� MRAM ���������� 1890��6�/1890��7�
 )
{
 int res;

 unsigned nRegWrd;

 if(cpu>=2) {
    errno = EADDRNOTAVAIL;
    return -1;
 }

 res=pscInit();
 if(res!=0) return res;

 nRegWrd=0x1002+cpu; // 0x1002 ��� 0x1003

 pscWrite(nRegWrd,0);
 pscWrite(nRegWrd+2,0);

 mram_flag=0;
  
 return 0;
}
