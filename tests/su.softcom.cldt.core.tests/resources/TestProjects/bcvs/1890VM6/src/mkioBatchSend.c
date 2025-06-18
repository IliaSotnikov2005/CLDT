
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

void free_batch(mkioBatchDesc_t *p);


/*@
	mkioBatchSend() - ������ ������ ������ � ����
*/
int
mkioBatchSend(
  mkioBatchDesc_t * p /*��������� �� ��������� ������*/
 )
{
 unsigned nMKIO,nMod,nIU;
 mkioIUDesc_t     * pi; //���������� ��

 //�������� ���������
 mkio_assert_r(p!=0, -1);
 mkio_assert_r(p->sgn==0x000BA7C1, -2);
 mkio_assert_r(p->nMKIO<=3, -3);
 mkio_assert_r(p->chain.ck_len<=MANCH_BATCH_MAX,-4);


 //�������� ���� �����
 if(pthread_mutex_lock(&p->mtx)!=0) { //�������� �������:??
    pthread_mutex_unlock(&p->mtx);   //������ ���??
    free(p);                        //��������� ������ ������??
    //free_batch(p);
    return -10;                       //�� ����� ��� ������������???
 }

 //�������� ����������� � ������������� ������� ������
 if( (p->del!=0) && (p->started==0) ) {
    //����� �� ������� � ������� ��������
    p->started=1;                  //����� ������ �� �������
    p->del=1;                      //�� ��� ������
    p->sgn=0xFFFFFFFF;             //�������� ��������� ��� ���������� ������
    pthread_mutex_unlock(&p->mtx);//������ �������       
    mkio_info("free");
    free(p);                     //��������� ������ ������
    //free_batch(p);
    return -11;                    //�� ���������
 }
 if(p->started!=0) { //���� ����� ��� ���-�� �������:
    pthread_mutex_unlock(&p->mtx); //��������� �����
    return -12;                    //��� �������� - ��� � ������
 }

 //����� ����� � ����� ���������
 p->started=1;   //�� ��������� (�������� ����)
 p->del=1;       //�� ��� � ������
 nMKIO= p->nMKIO;
 nMod = (nMKIO>>1)&1;
 nIU  = nMKIO&1;
 pi   = &mkioDrvDesc.Module[nMod].IUDesc[nIU];

 /////////////////////////
 //������������� � ������ ��
 mkioBatchCK(pi, p);
 //������ ����������� ��������� ��������� ������
 mkioBatchNotify(pi->bc_func, p);
 /////////////////////////

 //������������ ������
 mkio_info("free");
 //�� ��������� - �� � ������ (p->started �� ��������??)
 p->sgn=0xFFFFFFFF;             //�������� ��������� ��� ���������� ������
 pthread_mutex_unlock(&p->mtx);//������ �������!!
 free(p);     //��������� ������ ������
 //free_batch(p);

 return 0;
}
