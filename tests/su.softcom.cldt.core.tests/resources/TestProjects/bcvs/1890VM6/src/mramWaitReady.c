

#include "psc.h"
#include "mram.h"

#include <time.h>
#include <board.h>

/*
	�������� ���������� MRAM
	��������! ����� ��������� �� ��������. ������� mram_mutex �� 
 ����������. ����� 25��� ����� �������� ������������ ������� udelay().
 �������:
  0 - �����, ���� ����������
 -1 - ������, ���������� �� ���������; ���� mram_flag ����������
*/
int
mramWaitReady(
  unsigned reg //=0x1002�0x1005 - ����� �������� ���������� MRAM

)
{
 int i;
 int ret=0; //�����
 psc_mram_reg_t w;
 udelay(1); // gmu: 1 ���, ����� ��� ����������� �������� ready

 for(i=0;i<600;i++) { //~15�� (600 = 15��/25���)
    w.wrd=pscRead(reg);
    if(w.fld.READY!=0) break;
    udelay(25); 
 }

 if(w.fld.READY==0) {
    mram_flag|=1;
    ret=-1; //������
 }

 return ret;
}
