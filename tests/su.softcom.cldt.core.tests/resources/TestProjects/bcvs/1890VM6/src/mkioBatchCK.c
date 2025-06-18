
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

unsigned mkioBatchCnt[4];


/*@
	mkioBatchCK() - ������ ������� ������ ������
*/
int
mkioBatchCK(
  mkioIUDesc_t    *pi, /*��������� �� ���������� ��*/
  mkioBatchDesc_t *p   /*��������� �� ��������� ������*/
 )
{
 int i,ret=-1,ini,snd,mask;
 unsigned nCK;

 if( (pi!=0) && (p!=0) ) {
    //������� ����������???
    for(i=0;i<p->chain.ck_len;i++) {
        p->chain.ck[i].aw1=0xFFFF;
        p->chain.ck[i].res=0xFFFF;     
    }

    //��������� ������� ��� ioctl
    mask = intLock();
    nCK=1&(mkioBatchCnt[(int)pi->nMKIO]++); //����� ������� ������
    intUnlock(mask);
    if(nCK==0) {
       ini=FIOINITCHAINA;
       snd=FIOSTARTCHAINA;
    }else{
       ini=FIOINITCHAINB;
       snd=FIOSTARTCHAINB;
    }

    //������������� ��
    mkio_info("FIOINITCHAINA/B");
    ret=ioctl(pi->fd, ini, (int)&p->chain);

    if(ret==0) { //������������� ������ �������: ������ ��
       mkio_info("FIOSTARTCHAINA/B");
       ret=ioctl(pi->fd, snd, (int)&p->chain); 
       if(ret!=0) {
          mkio_info("Error start chain");
       }
    }else{
       mkio_info("Error init chain");
    }
 }

 return ret;
}
