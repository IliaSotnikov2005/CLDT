

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



/*
	manch_send_com() - ������� ������� ���������� �����������

	��� ������ ���������� �� ������ ����� ������������ ����� ������`
 ���� ��������� � buf[0]; ���������� ����� ������ ����� ��������� � buf[0];
 �������� ����� ����� ��������� � buf[0] ������ ��� ������� ����������`
 "�������� �������� �����".

 �������:
 0     - �����;
 ����� - ������.
*/
int
manch_send_com(
   int device,      //������������� ����� � ����
   u_short address, //=0-31 - ����� ��
   u_short cod,     //=0-31 - ��� ������� ���������� c ������� ���������� 0
                    //=32-63 - ��� ������� ���������� c ������� ���������� 0x1F (0x20|(��� �������))
   u_short *buf     //!=0   - ��������� �� ����� ������ �������
 )
{
 int ret=0, er=0;

 //������:
 unsigned nMKIO=device;       //����
 unsigned nMod =(nMKIO>>1)&1; //������ ����
 unsigned nIU  =nMKIO&1;      //�� �� ������

 //�������� ��:
 mkioIUDesc_t     * pi; //���������� ��

 bt401ctrl ku = {
   .com=cod,
   .dev=address,
   .data=0
 };

 //�������� ���������
 mkio_assert_r(nMod<=1    , ME_INVALID_PARAMETER);
 mkio_assert_r(nIU <=1    , ME_INVALID_PARAMETER);
 mkio_assert_r(address<=31, ME_INVALID_PARAMETER);
 mkio_assert_r(cod<=63    , ME_INVALID_PARAMETER);  // gmu: �������� ������� 

 if(buf!=0) {
   mkio_assert_r(((((int)buf)&1)==0), ME_INVALID_PARAMETER);
   ku.data=buf[0];
 }

 //�������� manch_close()
 mkio_assert_g(pthread_mutex_lock(&mkio_mutex[nMKIO])==0,
               HE_SPECIFIC_0, 
               Error);

 //�������� ��������� ��
 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
 mkio_assert_g(pi->ini==1, HE_SPECIFIC_1, Error);
 mkio_assert_g(pi->fd >=0, HE_SPECIFIC_2, Error);

 //������ ������� ���������� � ����
 er=ioctl(pi->fd, FIOSENDCOM, (int)&ku);
 if(er!=0) {
    ret=HE_SPECIFIC_3;
 }

 if(buf!=0) {
    switch(cod&0x1F) {//����� �� ��� ���������� ����� ������  // gmu: ��������� ����� 0x1F
       case GIVEOS:      buf[0]=ku.data; break;
       case GIVEVECWORD: buf[0]=ku.data; break;
       case GIVELASTCOM: buf[0]=ku.data; break;
       case GIVEVSK:     buf[0]=ku.data; break;
       case LOCKi:       buf[0]=ku.data; break;
       case UNLOCKi:     buf[0]=ku.data; break;
    }
 }


Error:
 pthread_mutex_unlock(&mkio_mutex[nMKIO]);
 mkio_info("exit");
 return ret;
}
