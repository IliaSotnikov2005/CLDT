

#include <stdio.h>
#include <pciLib.h>
#include "psc.h"
#include "mram.h"
#include <pthread.h>



/*
	������ ������� ���� �� MRAM
	��� �������������� �������������� ������� ������ ������� � MRAM
 ���������� ������� mram_mutex_rd.

 �������:
  0 - �����
 -1 - errno:
        EADDRNOTAVAIL - �������� ��� ����������
        EINVAL - �������� �������� from, to ��� size
        ENOMEM - ����� ����� ������� �� ������������ ������� MRAM
        EBUSY  - ������: ��� ���������� MRAM
*/
int
mramRead(
  unsigned cpu,   //��� ���������� = 0/1 - MRAM 1890��6�/1890��7�
  unsigned from,  //=0�0xFFFFF - ����� ������� ����� ������� � MRAM
  void    *to,    //��������� �� ������� ��� ���������� �������
  unsigned size   //=0,4,8,�,0x400000 - ������ ������� � ������
 )
{
 unsigned * uto=to;
 int ret=0;

 unsigned reg=0x1004+cpu; //0x1004 ��� 0x1005
 unsigned len;
 int i;

 //�������� ���������
 if(cpu>=2) {
    errno = EADDRNOTAVAIL;
    return -1;
 }
 if(((size|((int)to))&3)!=0) {
    errno = EINVAL;
    return -1;
 }
 if(from>0xFFFFF) {
    errno = EINVAL;
    return -1;
 }
 if(size>0x400000) {
    errno = EINVAL;
    return -1;
 }
 if(((from*4)+size)>0x400000) { //����� ����� ������� �� ������������ �������
    errno = ENOMEM;
    return -1;
 }


 //��� �����������
 pthread_mutex_lock(&mram_mutex_rd);//����������� MRAM
 mramStop(reg);                     //�������������
 mramWaitReady(reg);                //�������� ������ ���������

 //��������� ������ MRAM->���->���
 mram_flag=0;
 while(size>0) {
    len=min(512,size/4);           //��������� ����� �� 512 ����
    mramStart(reg,from,len,0);     //�������� MRAM->���
    mramWaitReady(reg);            //�������� ��������� �����������
    mramStop(reg);                 //gmu: ����� ��������� ������� ���� ��������
    if(mram_flag!=0) break;        //������ --> �����
    for(i=0; i<len; i++) {         //�������� ���->���
       uto[i]=mramReadCache(cpu,i);
    }
    uto +=len;
    from+=len;
    size-=len*4;
 }

 //������ ����������
 if(mram_flag!=0) {
    mramStop(reg);
    errno = EBUSY;
    ret=-1;
 }

 //��������� MRAM ��� ������ �������
 pthread_mutex_unlock(&mram_mutex_rd);

 return ret;
}
