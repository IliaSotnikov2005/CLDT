

#include <stdio.h>
#include <pciLib.h>
#include "psc.h"
#include "mram.h"

/*
	������ ������ ����� � ��� MRAM
*/
void
mramWriteCache(
  unsigned cpu,  //��� ���������� = 0/1 - MRAM 1890��6�/1890��7�
  unsigned wrd,  //=0�0x1FF - ����� ���������������� ����� � ���
  unsigned val   //����������� ��������
 )
{
 pscAS_t * p=0;
 unsigned off;

 //�������� � ����� ����� � ���
 off=(unsigned)&p->RAM2[0+(cpu&1)][wrd&0x1FF];
 wrd=off>>2;

 //������ ����� � ���
 pscWrite(wrd,val);
}
