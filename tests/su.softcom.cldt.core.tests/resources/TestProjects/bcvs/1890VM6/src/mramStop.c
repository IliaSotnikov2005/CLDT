

#include <stdio.h>
#include <pciLib.h>
#include "psc.h"
#include "mram.h"

/*
	������� �������� �������� ������/������ MRAM
	�������� ���� START � LOOP ��������� ��������.
*/
void
mramStop(
  unsigned int reg //=0x1002�0x1005 - ����� �������� ���������� MRAM
 )
{
 psc_mram_reg_t w;

 //��������� START � LOOP
 w.wrd=pscRead(reg);
 w.fld.START=0;
 w.fld.LOOP=0;
 pscWrite(reg,w.wrd);
 
 return;
}
