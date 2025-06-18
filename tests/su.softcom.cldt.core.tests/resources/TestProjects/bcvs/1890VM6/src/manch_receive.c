

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
	manch_receive() - ������ �������������� ���� �� ��
*/
int
manch_receive(
   int device,      //������������� ����� � ����
   u_short address, //=0-30 - ����� ��
   u_short cmd,     //=1-30 - ��������
   u_short *buf,    //!=0   - ����� ��� ���� ������
   u_long length    //=1-32 - ���������� ���� ������
 )
{
 int ret;
 mkio_info("enter");
 ret=manch_trans(device,address,cmd,buf,length,MANCH_READ);
 mkio_info("exit");
 return ret;
}

