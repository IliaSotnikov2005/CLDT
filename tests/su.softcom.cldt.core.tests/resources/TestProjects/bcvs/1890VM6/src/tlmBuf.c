
#include <board.h>
#include <string.h>
#include <time.h>
#include <pciLib.h>
#include "tlmBuf.h"


void _bcopy(unsigned from, unsigned to, unsigned size );

//����� ��� ���������� � ������� ����:
// 0 - ���1
// 1 - ���2
//��� ��������� ���� ������������

/*
	������������� ����������
	�������� pscInit().
*/
int
tlmBufInit(
    int n //=0,1 - ����� ���1,���2
)
{
 int res;
// n&=1;

 res=pscInit();
 if(res!=0) return res;

 return 0;
}



/*
	������ ��������� ������� � ������� ��������� ����
*/
void
tlmBufWrite(
    int    n,   /*=0,1 - ����� ���1,���2*/
    void * adr, /*����� ������ ������� (�� ������ ���� ������ 4)*/
    size_t size /*������ ������� � ������*/
)
{
 unsigned i;
 unsigned * p;
 psc_1000h_t w0;
 psc_1001h_t w1;
 unsigned *adr_mas = (unsigned*)adr;
 n&=1;

 p=&pscDesc.cpuAdr->TLM[n][0];//������ ��� ����������

 //�������� �����(!) � ���1,���2
 // memcpy(p, adr, size);
// _bcopy((unsigned)adr,(unsigned)p,size); //gmu: ����������������, ����� 2 ���� �� ���������� �� PCI � ���

 //������������ ����� � ������(!)
 for(i=0;i<((size+3)>>2);i++) {
 //  p[i]=CT_LE_L(p[i]);
   p[i]=CT_LE_L(adr_mas[i]); // gmu: �������������� �����, ����� 2 ���� �� ���������� �� PCI � ���
 }

 if(n==0) {
    w0.wrd=pscRead(0x1000);   
    w0.sA=(size+3)>>2;  /*����� � ������*/    
    pscWrite(0x1000,w0.wrd);  
 }else{
    w1.wrd=pscRead(0x1001);   
    w1.sB=(size+3)>>2;  /*����� � ������*/    
    pscWrite(0x1001,w1.wrd);
 }

}


/*
	������ �������� ���������� ���������� � ������
*/
int
tlmBufIsReady(
    int n      /*=0,1 - ����� ���1,���2*/
)
{
 int ret=0;
 psc_1000h_t w0;
 psc_1001h_t w1;

 n&=1;

 if(n==0) {
     w0.wrd=pscRead(0x1000);
     ret=w0.sTLMREADY;
 }else{
     w1.wrd=pscRead(0x1001);
     ret=w1.sTLMREADY;
 }

 return ret;
}



/*
	�������� ���������� � ������ ����������
	������������ �������� ������� nanosleep() �� �������� �����, ����
 �� �������� ���������� � tlmBufIsReady(). 
*/
void
tlmBufWaitReady(
    int n,         /*=0,1 - ����� ���1,���2*/
    unsigned delay /*�������� ������ ���������� � ������������*/
)
{
 int rdy;
// n&=1;
 rdy=tlmBufIsReady(n); //���������� � ������
 while(rdy==0) {   
    udelay(delay); 
    rdy=tlmBufIsReady(n);
 }
}

//gmu: ��������� � ������� ������� ���������� ����� �� ������� ���������� (�� 29.02.16)
/*
	������ ������ ����������
	������������ � �������� �������������� ��������������� ��� ��������.
*/
int
tlmBufStart(
    int n,      /*=0,1 - ����� ���1,���2*/
    size_t size /*������ ������� � ������*/
 )
{
 psc_1000h_t w0;
 psc_1001h_t w1;
 n&=1;

 if(n==0) {
    w0.wrd=pscRead(0x1000);
  //  w0.sVM6LIVEINC++; //gmu: ����� �� ������� ����������
    w0.sA=(size+3)>>2;  /*����� � ������*/
    w0.sTLMSTARTPCI|=1; /*���� ��� �������*/
    pscWrite(0x1000,w0.wrd);

    w0.sTLMSTARTPCI=0; /* ���������� ������� sTLMSTARTPCI = 0*/ //gmu
    pscWrite(0x1000,w0.wrd);  //gmu
 }else{
    w1.wrd=pscRead(0x1001);
  //  w1.sVM7LIVEINC++;    //gmu: ����� �� ������� ����������
    w1.sB=(size+3)>>2;  /*����� � ������*/
    w1.sTLMSTARTPCI|=1; /*���� ��� �������*/
    pscWrite(0x1001,w1.wrd);

    w1.sTLMSTARTPCI=0; /* ���������� ������� sTLMSTARTPCI = 0*/ //gmu
    pscWrite(0x1001,w1.wrd);  //gmu
 }

 return 0;
}

/*
	������ ����������
	�������� tlmBufWrite(), tlmBufWaitReady(n,10) � tlmBufStart().
*/
void
tlmBufSend(
    int    n,   /*=0,1 - ����� ���1,���2*/
    void * adr, /*����� ������ �������*/
    size_t size /*������ ������� � ������*/
 )
{
// n&=1;
 tlmBufWrite(n, adr, size); //������������ ������
 tlmBufWaitReady(n, 10);    //��� ���������� � ���������� ������ 10���
 tlmBufStart(n, size);      //��������� ������
}


//////////////////////////////////////////////////////////////////////////////////////////////
#define UNALIGNED(x) (((unsigned)(x) & (sizeof (unsigned) - 1)))
    
void
_bcopy ( unsigned from,  /* ������ */
         unsigned to,    /* ����   */
         unsigned size   /* ����� ������� � ������ */
 )
{
  char *src = (char*)from;
  char *dst = (char*)to;
  unsigned  *aligned_src;
  unsigned  *aligned_dst;

  while ( UNALIGNED(src) || UNALIGNED(dst) ) {
     if ( size==0 ) return;
     size--;
     *dst++ = *src++;
  }

  aligned_dst = (unsigned *)dst;
  aligned_src = (unsigned *)src;

  while (size >= 16) {
      size -= 16;
      *aligned_dst++ = *aligned_src++;
      *aligned_dst++ = *aligned_src++;
      *aligned_dst++ = *aligned_src++;
      *aligned_dst++ = *aligned_src++;
  }

  while (size >= 4) {
      size -= 4;
      *aligned_dst++ = *aligned_src++;
  }

  dst = (char*)aligned_dst;
  src = (char*)aligned_src;
  while (size>0) {
    size-=1;
    *dst++ = *src++;
  }
}

/*
	������ ��������� ������� � ������� ��������� ����, � ���������� ��������
        ����������. ���������� ������� � ����������� �������� �� ���������.
*/
void
tlmBufWritePiece(
    int    n,   /*=0,1 - ����� ���1,���2*/
    unsigned * adr, /*����� ������ ������� */
    size_t size,    /*������ ������� � ������ (������ ���� ������ 4)*/
    unsigned offset /*�������� � ������ �� ������ ��� (������ ���� ������ 4)*/
)
{
 unsigned i;
 unsigned * p; 
 n&=1;

 p=&pscDesc.cpuAdr->TLM[n][0];//������ ��� ����������
 p+=offset/4; // ������������� ��������

 //�������� �����(!) � ���1,���2, ������������ ����� � ������(!)
 for(i=0;i<((size+3)>>2);i++) {
    p[i]=CT_LE_L(adr[i]); // gmu: �������������� �����, ����� 2 ���� �� ���������� �� PCI � ���
 }
}

/*
     ������ ������������� ���������� ������ � ���1/���2
*/
void
tlmSetCount(  
    int    n,     /*=0,1 - ����� ���1,���2*/
    size_t count /* ���������� ������� */)
{
   psc_1000h_t w0;
   psc_1001h_t w1;

    count = (count>=1024)?0:count;

    if(n==0) {
      w0.wrd=pscRead(0x1000);   
      w0.sA=count;  /*����� � ������*/    
      pscWrite(0x1000,w0.wrd);  
    }else{
      w1.wrd=pscRead(0x1001);   
      w1.sB=count;  /*����� � ������*/    
      pscWrite(0x1001,w1.wrd);
    }
}

