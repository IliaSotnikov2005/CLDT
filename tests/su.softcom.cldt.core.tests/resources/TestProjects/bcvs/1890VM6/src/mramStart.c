

#include <stdio.h>
#include <pciLib.h>
#include "psc.h"
#include "mram.h"

/*
	������ �������� ������/������ ��/� ��� �/�� MRAM
*/
void
mramStart(
  unsigned reg, //=0x1002�0x1005 - ����� �������� ���������� MRAM
  unsigned wrd, //=0�0xFFFFF - ����� ����� � MRAM
  unsigned len, //=1�512 - ���������� ����
  unsigned cyc  //=0/1 - �����������/����������� �������
 )
{
 psc_mram_reg_t w;

 if(len==0) return;

 w.wrd=0;
 w.fld.sMRAMAW=wrd;
 w.fld.sMRAMNW=len; //0 - ��� 512
 if(cyc==0) { //�����������
    w.fld.START=1;
 }else{       //�����������
    w.fld.LOOP =1;
 }
 pscWrite(reg,w.wrd);
 
 return;
}
