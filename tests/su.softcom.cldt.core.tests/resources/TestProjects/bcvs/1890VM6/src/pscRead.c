
#include <pciLib.h>
#include "psc.h"

/*
	������ ����� �� PSC
*/
unsigned
pscRead (
 unsigned nWord /*����� ����� � �������� ������������ ������*/
 )              /*TLM � RAM2 ��� ������� � ��� ����� PCI    */
{
 return PSC_READ(nWord);
}
