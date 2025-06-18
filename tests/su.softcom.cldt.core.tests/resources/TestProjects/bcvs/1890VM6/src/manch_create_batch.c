
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>

#include <usr401a.h>
#include <pthread.h>

#include "libbcvs.h"
#include "manchester.h"

mkioBatchDesc_t* assign_storage_batch(void); // gmu: ��������� ������� ��� ������ manch_create_batch
mkioBatchDesc_t mkioBatchDesc[1024]; //gmu: ������������� ����� ��� �������� �������
unsigned mkioBatchDesc_busy[32];     //gmu: ��� ����������� ��������� ����� ��� ����� � mkioBatchDesc


/*
  manch_create_batch() - ������ �����
  �������:
  -1    - ������;
  ����� - ������������� ���������� ������.
*/
int
manch_create_batch(
   int device
 )
{
 mkioBatchDesc_t * p;

 mkio_info("enter");
 mkio_assert_r(device<=3, -1);
 mkio_assert_r(device>=0, -1); //gmu:  ����� ���������, ��� �������� �� ������������� ����� 

 //������� ������ ��� ������
// mkio_assert_r((p=malloc(sizeof(mkioBatchDesc_t))!=0), -1);

 p=malloc(sizeof(mkioBatchDesc_t)); // last_work
// p = assign_storage_batch(); //gmu: �������� ������ malloc
 mkio_assert_r(p!=0, -1);

 p->nMKIO= device;
 p->mtx  = PTHREAD_MUTEX_INITIALIZER;
 p->chain.ck_len=0; //����� ������
 p->sgn  = 0x000BA7C1;
 p->started = 0; //gmu: ��� ���� ���� ���� ���������������� 
 p->del=0;	 //gmu

 mkio_info("exit");
 return (int)p;
}

/*
    assign_storage_batch - �������� ��������� ����� � ������� mkioBatchDesc
    ������� ����������� mkioBatchDesc_busy, ���������� ��������� ����� � ������� mkioBatchDesc
    ���������� ����� ��� ���������� ����������� ������.
    �������:
    0      - ��� ����� � �������;
    �����  - ����� ����������� ������. 
 
    ����������:
    ������ mkioBatchDesc_busy �������� 32 �����, ������ ��� � ���� ����� 
    ���������� ����� � �������  mkioBatchDesc. �-� �������� ���� ��������,
    ��� ���������� � ���� ������� �� ����� � ����� ���� �����������.
*/
mkioBatchDesc_t*
assign_storage_batch(void)
{
  int i,j;
  
  // ����������� mkioBatchDesc_busy
  for(i=0;i<32;i++)
  {
     if(mkioBatchDesc_busy[i]!=0xffffffff) 
     {
         for(j=0;j<32;j++)
         {
           if(((mkioBatchDesc_busy[i]>>j)&1)==0) // ����� �� �����, ����� �������� 
           {             
              mkioBatchDesc_busy[i]|=(1<<j); // ������������� ������� ���������
  //            printf(" create_batch mkioBatchDesc[%d] \n",i*32+j);
  //            printf(" mkioBatchDesc_busy[%d] = 0x%x \n",i,mkioBatchDesc_busy[i]);           
              return &mkioBatchDesc[i*32+j];
           }
         }
     }
  }

 return 0;
}


