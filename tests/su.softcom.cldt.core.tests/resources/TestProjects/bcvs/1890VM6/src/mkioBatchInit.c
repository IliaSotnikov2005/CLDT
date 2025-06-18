
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
    mkioBatchInit() - ���������� ������ 
 �������:
  0     - �����;
  ����� - ������:�� ������� �������� ��������� � ����� ������� manch_add_cmd().
*/
int
mkioBatchInit(
  int batch,          /*������������� ���������� ������*/
  PMANCH_COMMAND buf, /*������ ���������� ���������    */
  u_short count       /*���������� ��������� � ������  */
 )
{
 
 int i;
 PMANCH_COMMAND pc;
 int l;

 //��������� ��� ������� � �����
 for(i=0;i<count;i++) {
    pc=buf++;
    l= pc->command.fld.length;
    if(l==0) l=32;
    if(manch_add_cmd(batch, pc->command.fld.flag, 
                     pc->command.fld.address, pc->command.fld.subaddres,
                     pc->buf, l) != 0 ) {
       manch_delete_batch(batch);
       return -1; //�� ������� �������� ��������� � �����
    }
 }

 return 0;
}

