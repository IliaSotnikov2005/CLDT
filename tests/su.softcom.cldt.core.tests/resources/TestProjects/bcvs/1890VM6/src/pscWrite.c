
#include <pciLib.h>
#include "psc.h"

/*
	������ ����� � PSC
*/
void
pscWrite(
  unsigned nWord, /*����� ����� � �������� ������������ ������*/
                  /*TLM � RAM2 ��� ������� � ��� ����� PCI    */
  unsigned Word  /*������������ �����                         */
 )
{
   PSC_WRITE(nWord,Word);
}
