
#ifndef inc_psc_h
#define inc_psc_h

#include <pscLib.h>


/*

typedef union {
 unsigned wrd;
 struct { unsigned 
    r0           :12, //31�20 ������
    sVM6LIVEINC  : 4, //19�16 ��������������� ��� �������� 1890��6�
    r1           : 3, //15�13 ������
    sTLMBANK     : 1, //12 ���� 2 ���
    sTLMREADY    : 1, //11 ���� 3 ���
    sTLMSTARTPCI : 1, //10 ���� 1 ���
    sA           :10; //9�0 ���������� ������� ��� 1 ���
 };
} psc_1000h_t;


typedef union {
 unsigned wrd;
 struct { unsigned 
    r31          :12, //31�20 ������
    sVM7LIVEINC  : 4, //19�16 ��������������� ��� �������� 1890��7�
    r15          : 3, //15�13 ������
    sTLMBANK     : 1, //12 ���� 2 ���
    sTLMREADY    : 1, //11 ���� 3 ���
    sTLMSTARTPCI : 1, //10 ���� 1 ���
    sB           :10; //9�0 ���������� ������� ��� 2 ���
 };
} psc_1001h_t;



//�������� ���������� MRAM
typedef union {
 unsigned wrd;
 struct { unsigned 
    START   : 1, //31    ���� ������������� �������� ����������� ��/��
    LOOP    : 1, //30    ���� ������������� �������� ����������� ��/��
    READY   : 1, //29    ���� ���������� ����������� � �������� ��/��
    sMRAMNW : 9, //28�20 ���������� ����
    sMRAMAW :20; //19�0  �������� � �������� ������������ MRAM
 } fld;
} psc_mram_reg_t;


//�������� ������������ ������ TLM � RAM2 ��� ������� � ��� ����� PCI
typedef struct {
 unsigned RAM2[4][512]; //��� ������� ������
 unsigned TLM[2][1024]; //����������

 psc_1000h_t _1000h;
 psc_1001h_t _1001h;

 psc_mram_reg_t _1002h;
 psc_mram_reg_t _1003h;
 psc_mram_reg_t _1004h;
 psc_mram_reg_t _1005h;

 //1006:
 unsigned  sTMIWRD; //31�0 ���������� ��������� ���
 //1007:
 unsigned  sTIMECNT;//31�1 ����� � ������� ��������� � 3,2 ��� 
                    //0    ������� ���������� ������� ����� �������
 //1008:
 unsigned  sMOVECNT;//31�1 ����� � ������� ������ �������� � 3,2 ��� 
                    //0    ������� ������ ��������, ������ sMOVE
 //1009:
 unsigned  sSSSSCNT;//31�1 ����� � ������� ��������� ����������� �� � 3,2 ��� 
                    //0    ������� ��������� ����������� ��, ������ sENDVOZ
} pscAS_t;

//��������� PSC(PCI Slave controller)
typedef struct {
 pscAS_t * cpuAdr; //������� ������������ �����
 unsigned  pciAdr; //��������������� PCI-�����
 int bus;
 int dev;
 int func;
} pscDesc_t;

extern pscDesc_t pscDesc;
*/

int pscInit(void);
unsigned pscRead(unsigned nWord);
void pscWrite(unsigned nWord, unsigned Word);
void pscLiveInc(unsigned n);

#endif /*inc_psc_h*/
