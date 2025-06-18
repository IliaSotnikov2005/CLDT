

#include <stdio.h>
#include <pciLib.h>
#include "psc.h"
#include "mram.h"
#include <pthread.h>

/*
	������ ������� ���� � MRAM
	��� �������������� �������������� ������� ������ ������� � MRAM
 ���������� ������� mram_mutex_wr.
 �������:
  0 - �����
 -1 - errno:
        EADDRNOTAVAIL - �������� ��� ����������
        EINVAL - �������� �������� from,to ��� size
        ENOMEM - ����� ����� ������� �� ������������ ������� MRAM
        EBUSY  - ������: ��� ���������� MRAM
*/
int
mramWrite(
  unsigned cpu,  //��� ���������� = 0/1 - MRAM 1890��6�/1890��7�
  void    *from, //��������� �� ������������ ������
  unsigned to,   //=0�0xFFFFF - ����� ������� ����� ������� � MRAM
  unsigned size  //=0,4,8,�,0x400000 - ������ ������� � ������
 )
{
 unsigned * ufr=from;
 int ret=0;

 unsigned reg=0x1002+(cpu&1); //0x1002 ��� 0x1003;
 unsigned len;
 int i;

 //�������� ���������
 if(cpu>=2) {
    errno = EADDRNOTAVAIL;
    return -1;
 }
 if((((int)from|size)&3)!=0) {
    errno = EINVAL;
    return -1;
 }
 if(to>0xFFFFF) {
    errno = EINVAL;
    return -1;
 }
 if(size>0x400000) {
    errno = EINVAL;
    return -1;
 }
 if(((to*4)+size)>0x400000) { //����� ����� ������� �� ������������ �������
    errno = ENOMEM;
    return -1;
 }

 //��� �����������
 pthread_mutex_lock(&mram_mutex_wr); //����������� MRAM
 mramStop(reg);                      //������������� (� �� ��� �����) //gmu: �������� ����� ��� ������

 //��������� ������ ���->���->MRAM
 mram_flag=0;
 while(size>0) {
    len=min(512,size/4);           //��������� ����� �� 512 ����
    mramWaitReady(reg);            //�������� ���������
    mramStop(reg);                 //gmu: ����� ������� ���� ��������
    if(mram_flag!=0) break;        //������ --> �����
    for(i=0; i<len; i++) {         //�������� ���->���
       mramWriteCache(cpu,i,ufr[i]);
    }
    mramStart(reg,to,len,0);       //�������� ���->MRAM
    ufr +=len;
    to  +=len;
    size-=len*4;
 }

 //������ ����������
 if(mram_flag!=0) {
    mramStop(reg);
    errno = EBUSY;
    ret=-1;
 }else{ //�������� ������ ���������
    mramWaitReady(reg); //�� ������ �������, ���� �� ���������� ������
 }

 //��������� MRAM ��� ������ �������
 pthread_mutex_unlock(&mram_mutex_wr);

 return ret;
}
