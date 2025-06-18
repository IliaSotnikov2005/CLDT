

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

/*
   ������ ���������� � ���������� �����
   ������� ���������� ������ � ���������� �����, ����� ���� ����������
   ���������� ������������ ������ �� ���������� ���������
   ������������ ��������:
   0 - �����,
   ME_INVALID_PARAMETER - ����������� ����� ��������
   DE_DRIVER_SPECIFIC - �� ������������������� ��
   -1 - ������ ��� ���������� ������� FIOSETBLOCK �������� ioctl

*/
int
manch_write_buf(
   int device,        //
   u_char subaddress, //��������
   u_long off,        //=0!
   u_short *buf,      //��������� �� ������� ������ � ������� ��������� ������������ ����������
   u_long length      //����� ������������ ������
 )
{
 int res;
 mkioIUDesc_t     * pi; //���������� ��

 bt401block bl;


 //������:
 unsigned nMKIO=device;       //���������� ����� ����
 unsigned nMod =(nMKIO>>1)&1; //������ ����
 unsigned nIU  =nMKIO&1;      //�� �� ������

 //�������� ���������
 mkio_assert_r(device<=3     , ME_INVALID_PARAMETER);
 mkio_assert_r(device>=0     , ME_INVALID_PARAMETER);
 mkio_assert_r(subaddress!=0 , ME_INVALID_PARAMETER);
 mkio_assert_r(subaddress<=30, ME_INVALID_PARAMETER);
 mkio_assert_r(buf!=0, ME_INVALID_PARAMETER);
 mkio_assert_r(off==0, ME_INVALID_PARAMETER);
 mkio_assert_r(length!=0 , ME_INVALID_PARAMETER);
 mkio_assert_r(length<=32, ME_INVALID_PARAMETER);

 //�������� ��������� ��������� ��
 pi = &mkioDrvDesc.Module[nMod].IUDesc[nIU];
  //�� �������������������?
 mkio_assert_r(pi->ini==1, DE_DRIVER_SPECIFIC);
  //������ ���������� �����?
 mkio_assert_r(pi->fd>=0, DE_DRIVER_SPECIFIC);

 bl.buf =buf;
 bl.size=length;
 bl.sa  =subaddress;
 res=ioctl(pi->fd, FIOSETBLOCK, (int)&bl );

 return res;
}
