
#ifndef inc_exptst_h
#define inc_exptst_h

#define AREA_FOR_EXPTST_INFO_ADR     0xA000EFC0 
#define AREA_FOR_EXPTST_INFO_SIZE    0x40 

typedef struct {	
unsigned gpio            /*��������, ��������� � ���������� GPIO ��� ������� ����*/;
unsigned startExpTstInfo;/* ����� ������ ������� ���������� ����������� � �������������� ������������ �� ����� ���������� �������� ������������*/
unsigned freeExpTstInfo; /* ����� ��������� ��������� ������ ������� ExpTstInfo*/
unsigned err[2];  /*������� ������ ����������� ��������-������*/
unsigned tstDone[2]; /*������� ������ ������� ����������� ��������-������ */
unsigned reserv[11]; /*������*/
}State_t;

unsigned exptstGetResult(void);    //������� ��� ��������� ����������� ������
State_t * exptstGetState(void); //������� ��� ��������� ���������� ����������

#endif /*inc_exptst_h*/
