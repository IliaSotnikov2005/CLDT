
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <usr401a.h>

#include "libbcvs.h"
#include "manchester.h"

#include <arch/interrupt.h>
#include <mqueue.h>


/*@
	mkioBatchNotify() - ����� ������� ��������� ������ ��� ��������� ������
*/
void
mkioBatchNotify(
  PNOTIFYFUNC    func,/*��������� �� ������� ��������� ������*/
  mkioBatchDesc_t * p /*��������� �� ��������� ������*/
 )
{
 int i;
 u_long  cmd,length;
 u_short * buf;
 bt401com * pc; //��������� �� ��������� �������
 u_short ks,res;

 if( (func!=0) && (p!=0) ) {
    mkio_info("check result");
    //��� ������� ��������� � ������
    for(i=0;i<p->chain.ck_len;i++) { 
       pc=&p->chain.ck[i];          
       //��������� ����������� �������� ��
       if((pc->com==RT_BC)||(pc->com==RT_BC_CHAN_A)||(pc->com==RT_BC_CHAN_B)) {
          //������ �� ��:
          buf   =(void*)pc->buf;
          length=pc->size; //< 7- 0> - ���������� �������� ���� ������
          ks=MAKECOMMAND(pc->rt1,MANCH_READ,pc->sa1,pc->size);
       }else{ //������ � ��:
          buf   =0;
          length=0;        //< 7- 0> - ���������� �������� ���� ������
          ks=MAKECOMMAND(pc->rt1,MANCH_WRITE,pc->sa1,pc->size);
       }
          
       //�������� ���������:
       res=pc->res;         
       if( (length==0)                && //������ � ��         �
           ((res&(1<<12)) ==0 )       && //��� ����� ������    � 
           ((res&(1<<10)) ==0 )       && //��� ������ �������  �         
           ((u_short)pc->aw1!=0xFFFF) ){ //���� �������� �����
          return; //���������� ������ � �� - �� �������� func
       }
       //�����: ������ �� �� ��� ������ � �� � �������:
       //�������� func()
       cmd=(res<<16)|ks;   //<15- 0> - ������������ ��������� �����
                           //<31-16> - ���������: 0 - �����, ����� - ������
                           //< 7- 0> - ���������� �������� ���� ������
       length|=i<<8;       //<15- 8> - ����� ������� � ������
       length|=pc->aw1<<16;//<31-16> - ���������� �������� �����
                           //          (=0xFFFFF - ��� ��������� �����)
       func((u_long)p, cmd, buf, length);
    }
 }
}
