
#include <stdio.h>
#include <pciLib.h>
#include "psc.h"


//extern pscDesc_t pscDesc; //��������� PSC

/*
	������������� PSC
 �������:
  0 - �����
 -1 - errno:
        ENODEV - ���������� �� ���������� �� ���� PCI
        ENOMEM - ������������ ������ � ������� PCI
*/
int
pscInit(void)
{
  return pscLibInit();
}
