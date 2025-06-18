
#ifndef inc_mkio_h
#define inc_mkio_h
#include <pthread.h>
#include <usr401a.h>


//� ����: ��� ������ ����; �� ������ ������ - ��� ������ ����;
//� ������ ������ ���� - ��� ����� ����
//� ������������ ��������: "������������ ���������� (��)" - ��� "����� ����"



/*
 * ���������� ��
 */
typedef struct {
  char ini;          //������� ������������� ��
  char open;	     //������� �������� ����� ��� ��
  char nMod;         //����� ������ ����
  char nIU;          //����� �� �� ������
  char nMKIO;        //���������� ����� ����
  char rgm;          //����� ������ (MANCH_BC,MANCH_MT,MANCH_RT...)
  char name[16];     //��� ���������� ("/dev/bc0"...)
  int rdBufSize;     //������ ����������� ���������� ������ ������ � ������
                     //���������� ���������� � 16-�� ������ ������ (������
                     //��� ���� ������������)
  int wrtSubBufSize; //������ ���������� ��������� ������� ������ ��� �������
                     //�� ������������ � ������ ���������� ���������� � 16-��
                     //������ ������  
  int fd;            //���������� ����� ��
              //������� ��������� ������:
  PNOTIFYFUNC   bc_func;  //��� ��
  PCALLBACKFUNC rt_func;  //��� ��
} mkioIUDesc_t;

/*
 * ���������� ������ ������ ����
 */
typedef struct {
  char ini;     //������� ������������� ������
  char nMod;    //=0,1 - ����� ������ ����
  char dev;     //=1,2 - ����� PMC-�����, �� ������� ���������� ������???
  char interrupt;//=0-3 - ����� ����� ���������� ���� PCI (INTA-INTC)
  int module;  //����� ������ �� bt401DevInit()
  int offset;  //=0 - ��� ������ ���23-401???
  int mod;     //=1 - ��� ������ ���23-401???��� ������???
  mkioIUDesc_t IUDesc[2];//����������� ��
} mkioModuleDesc_t;


/*
 * ���������� �������� ������� ����
 */
typedef struct {
 char ini; //������� �������������
 int  err; //��� �������� �� bt401Drv()
 mkioModuleDesc_t Module[2]; //����������� ������� ����
} mkioDrvDesc_t;


/*
 * ���������� ������ ����
 */
typedef struct {
 u_int sgn;           //=0x000BA7C1 - ���������
 pthread_mutex_t mtx; //��� ������������ ������� ������
 u_short nMKIO;       //����� ����
 u_char  started;     //������� ������� ������
 u_char  del;         //������ �� �������� ������
 bt401chain chain;    //������� ������ ����
} mkioBatchDesc_t;
//������������ ���������� ��������� � ������:
#define MANCH_BATCH_MAX  MAX_CK_LEN

extern mkioBatchDesc_t mkioBatchDesc[1024]; //gmu: ������������� ����� ��� �������� �������
extern unsigned mkioBatchDesc_busy[32];     //gmu: ��� ����������� ��������� ����� ��� ����� � mkioBatchDesc


extern mkioDrvDesc_t mkioDrvDesc;
extern pthread_mutex_t manch_mutex;
extern pthread_mutex_t mkio_mutex[4];
extern mqd_t mkio_mq;             //���������� �������


#define MKIO_SERVERS 8 //���������� ������� ������� ����

void mkioBatchServer(void);
void mkioBatchServer_start(void);

void mkioRTServer(int *arg);
void mkioRTServer_start(int i);
void mkioRTServer0_start(void);
void mkioRTServer1_start(void);
void mkioRTServer2_start(void);
void mkioRTServer3_start(void);

int mkioBatchInit(int batch,PMANCH_COMMAND buf,u_short count);
int mkioBatchSend(mkioBatchDesc_t * p);
void mkioBatchNotify(PNOTIFYFUNC func,mkioBatchDesc_t * p);
int mkioBatchCK(mkioIUDesc_t *pi,mkioBatchDesc_t *p);


#define mkio_info(__s) do{ \
/* fprintf(stdout,"#MKIO-I# %s:%d - %s\r\n",__PRETTY_FUNCTION__,__LINE__,__s); */\
 }while(0)


#define mkio_error(__s) do{ \
 fprintf(stderr,"#MKIO-E# %s:%d - %s\r\n",__PRETTY_FUNCTION__,__LINE__,__s); \
 }while(0)


#define mkio_assert_r(__exp,__retval) \
 do { if(!(__exp)) {    \
    mkio_error(#__exp); \
    return __retval;  \
 } }while(0)

#define mkio_assert_g(__exp,__retval,__label) \
 do { if(!(__exp)) {    \
    mkio_error(#__exp); \
    ret= __retval;/*???*/\
    goto __label;       \
 } }while(0)



#endif  /*inc_mkio_h*/

