/*
   ������� ��� ��������� ����������� ��������-������

   ��� ��������� ���������� ���������� � �������������� ������������ �� ����� ���������� �������� ������������ ����� ������� ���������� � ������ ���������� � startExpTstInfo, ������ ��������������� ������� ����������� ��������� �������:
FreeExpTstInfo - startExpTstInfo
*/

#include "exptst.h"
/*
 ������� ��� ��������� ���������� ���������� � ����������� �������� ������
 ������� ���������� ��������� �� ����������� ��������� ���� State_t, ���������� ���������� � ����������� ������
*/
State_t * exptstGetState(void)
{  
  return (State_t *)AREA_FOR_EXPTST_INFO_ADR;
}

/*
 ������� ��� ��������� ����������� ������
������� ���������� ������� ������ ����������� ��������-������.
*/
unsigned exptstGetResult(void)
{
 State_t* state = (State_t *)AREA_FOR_EXPTST_INFO_ADR;
 return state->err[0];
}



