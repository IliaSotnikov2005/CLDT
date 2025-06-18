
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
    manch_send_batch() - �������� ������ � ����
 �������:
  0     - �����;
  ����� - ������:
     -1 - ����� manch_start() ��������� ��������,
          ���������� errno �������� ��� ������ mq_send();
     -2 - �� ������� ������� ����� ������� manch_create_batch();
     -3 - �� ������� �������� ��������� � ����� ������� manch_add_cmd();
     ����� - ��� �������� �� manch_start().
*/
int
manch_send_batch(int device,PMANCH_COMMAND buf, u_short count)
{
//int manch_send_batch(int device,MANCH_COMMAND * buf, u_short count)
//int manch_send_batch(int device,void * buf, u_short count)

 //�������� ������
 int batch=manch_create_batch(device);

 if(batch==-1) {
    return -2; //�� ������� ������� �����
 }

 //��������� ��� ������� � �����
 if(mkioBatchInit(batch,buf,count)!=0) {
    return -3; //�� ������� �������� ��������� � �����
 }

 //��������� �����
 return manch_start(batch);
}




