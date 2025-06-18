/* mipsR4kTimer.c - MIPS R4000 CPU internal timer library */

#include <version.h>

#ifdef   K64SMP
#define  CPU_TIMER_DIV  1
#else
#define  CPU_TIMER_DIV  2
#endif

struct mc_ext
{
       system_time_t    mce_CurrentNanoSecond;  /* ������� �������
                                                    (���������� ��������� ������ ������) */
       u_int            mce_CurrentSecondCount; /* �������� �������� compare/count,
                                                  ��������������� ������ ������� ������� */
       u_int            mce_1s;                 /* ���������� counts � �������          */
       u_int		      mce_Nom1s;
       u_int            mce_Calc1s;
};

/* locals */

static VOIDFUNCPTR boardClockRtn; /* attached user ISR */
static long boardClockArg; /* user ISR argument */
void boardClockInt(long arg);
void boardClockDisable(void);
void boardClockConnectGet(VOIDFUNCPTR *rtn,long * arg)
{
   *rtn = boardClockRtn;
   *arg = boardClockArg;
}
/* globals */

struct mod_clock *mc;                  /* ��������� �� ��������� ����� ������ ������   */

/*--------------------------- boardClockGetTime - ����� ����� ---------------------------
 * ���������:
 *  mc  - ��������� ����� ������ ������;
 * ������������ ��������.
 *  ������� ���������� ������� ����� � ������������.
 */
#define cpuCountGet cpuCountGetR4k
static __inline__ u_int cpuCountGet()
{
    register int a;
    asm( "mfc0   %0,$9; nop;nop;nop":"=r" (a));
    return a;
}
#define cpuCountSet cpuCountSetR4k
static __inline__ void cpuCountSet(register int cnt)
{
    asm( "mtc0   %0,$9; nop;nop;nop"::"r" (cnt));
}
#define cpuCompareGet cpuCompareGetR4k
static __inline__ u_int cpuCompareGet()
{
    register int a;
    asm( "mfc0   %0,$11; nop;nop;nop":"=r" (a));
    return a;
}
#define cpuCompareSet cpuCompareSetR4k
static __inline__ void cpuCompareSet(register int cmp)
{
    asm( "mtc0   %0,$11; nop;nop;nop"::"r" (cmp));
}

system_time_t boardClockGetTime(struct mod_clock *mc)
{
    struct mc_ext *mce = (struct mc_ext *)mc->mc_Ext;
   // ������� ����� � ������������
   system_time_t time = 0;
   if( mce )
   {
      int lock = intLock();
      time = (system_time_t)(cpuCountGet() - mce->mce_CurrentSecondCount)*1000000000ULL/mce->mce_1s +
                        mce->mce_CurrentNanoSecond;
      intUnlock( lock );
   }
   return time;
}
unsigned long long boardClockCountPerSecGet(struct mod_clock *mc, int nom)
{
    struct mc_ext *mce = (struct mc_ext *)mc->mc_Ext;
    if(!mce)
        return 0;
    return nom ? mce->mce_Nom1s : mce->mce_1s;
}
/* ������� ������������� �����     */
int  boardClockAdjust(struct mod_clock *mc, system_time_t adj, system_time_t period)
{
   struct mc_ext *mce = (struct mc_ext *)mc->mc_Ext;
   int lock = intLock();
   system_time_t t = ( (system_time_t)mce->mce_Nom1s)*( period + adj ) / period;
//   mce->mce_1s =  t;
   mce->mce_Calc1s =  t;
   intUnlock(lock);
   return 0;
}
#if OS_MAJOR >= 4
#define clockGetTime modclockGetTime
#endif
/*--------------- boardClockReload - ��������� ������� ���������� �� ����� --------------
 * ���������:
 *  mc       - ��������� ����� ������ ������ ;
 *  new_time - �����, ����� ������ ��������� ����������.
 * ������������ ��������.
 *  � ������ ��������� ���������� ������� ���������� 0.
 *  ���� ��������������� ����� ��� ������ (������ ��������),
 *  ������ ������������, � ������� ���������� ������� �����.
 *
 * ������� ������������ �� ��� ��������� ����� ������
 * �� �������� ������������� ������� �������.
 */
system_time_t boardClockReload(struct mod_clock *mc, system_time_t new_time)
{
   struct mc_ext *mce = (struct mc_ext *) mc->mc_Ext;
   if (mce)
   {
#if (OS_VERSION > 301) || (OS_VERSION == 301 && OS_RELEASE >= 121)
      // ����� ��������������� ����� ������ ���� ������ ������� � ������ �����������
      assert( new_time % mc->mc_Res == 0);
#else
      assert((mc->mc_ExpectedTime - new_time) >= mc->mc_Res);
#endif

      // ����� ��������� �� ������ ������� ������� �� ���������� ������� ������� (� count)
      int lock = intLock();
      u_int new_add = (new_time - mce->mce_CurrentNanoSecond) * mce->mce_1s / 1000000000ULL;
      u_int new_compare = mce->mce_CurrentSecondCount + new_add; /* ��������� ����� � count*/
#if (OS_VERSION > 301) || (OS_VERSION == 301 && OS_RELEASE >= 121)
      if ( new_compare - (cpuCountGet()+300) > mce->mce_1s * 2 )
#else
      if ( new_compare - cpuCountGet() > mce->mce_1s )
#endif
      {
         // ����� �������� ������ 1 ������� (������, ����� �������������� ����)
         intUnlock(lock);
#if OS_MAJOR == 3
         return clockGetTime(mc); /* ������� ���������� ������� �����   */
#else
         return modclockGetTime(mc); /* ������� ���������� ������� �����   */
#endif
      }
      // kprint("reload(%03d):%lld %07lld 0x%08x\r\n",intNestLevel,new_time, new_add , new_compare);
      mc->mc_ExpectedTime = new_time;
      cpuCompareSet(new_compare);
      intUnlock(lock);
   }
   return 0;
}
/*------------------------- boardClockInit - ������������� ����� ------------------------
 * ��������:
 *  mc - ��������� ����� ������ ������.
 *
 * ������� �������� � ��������� ����� ��������� ����������:
 * - ��������� �� ������� ������ ��������� �����;
 * - ��������� �� ������� ��������� ������� ����������;
 * - ��������� �� ������� ������������� ����� (���� ���, ������, ������� �� 10 ��.);
 * - ��������� �� ������� ������� �����;
 * - ����������� ����������� ����� (��������);
 * - ������� ������ ����� (���������� count � �������).
 *
 * ������� boardClockInit ����� ���������� ������� ��������� ���������� �
 * ��������� (���������) ���������� �� �����.
 */
int boardClockInit(struct mod_clock *m)
{
   struct mc_ext *mce = (struct mc_ext *)modMPDSharedAlloc(shmemSYSDATA, sizeof(struct mc_ext), 0);
   kprint("boardClockInit: mce=%p\n\r",mce);
   mc = m;
   mc->mc_Ext = mce;
   mc->mc_GetTime = boardClockGetTime; /* ������� ������ �����                         */
   mc->mc_SetIntr = boardClockReload;  /* ������� ��������� ������� ���������� �� �����*/
   mc->mc_Adjust  = boardClockAdjust;  /* ������� ������������� �����     */
   // struct mod_clock *mc ������������������� 0
   boardClockDisable();
   if( mce )
   {
       /* ���������� counts � ������� (��� ������� ����������)*/
       mce->mce_1s = (boardCpuClockRateGet() / CPU_TIMER_DIV) ;
       mce->mce_Nom1s = mce->mce_1s;
       mce->mce_Calc1s = mce->mce_1s;
       kprint("boardCpuClockRateGet=%d\r\n",boardCpuClockRateGet());
       /* ����������� ������� ��������� ���������� �� ����� ���������� */
#if defined(_MIPS_TUNE_RM7000) || defined(BT206) || defined(BT206RUS)
       intConnect(IV_ALT_TIMER, (vfuncptr_t)boardClockInt, 0);
#else
       intConnect(IV_TIMER, (vfuncptr_t)boardClockInt, 0);
#endif
       return 0;
   } else
   {
       return ENOMEM;
   }
}

int boardClkRateSet(int _hz)
{
   return 0;
}
/*------------------------------------------------------*/
static int boardTickCount=0;
void __boardClockInt(long arg)
{
   struct mc_ext *mce = (struct mc_ext *) mc->mc_Ext;
   if (mc->mc_ExpectedTime >= (mce->mce_CurrentNanoSecond
         + NANOSECONDS_PER_SECOND))
   {
      /* ��������� ����� ��� ������� �������  */
      /* ������� ������� (���������� ��������� ������ ������) */
      mce->mce_CurrentNanoSecond += NANOSECONDS_PER_SECOND; 
      /* �������� �������� compare, ��������������� ����� ������� ������� */
      mce->mce_CurrentSecondCount += mce->mce_1s;
      mce->mce_1s = mce->mce_Calc1s;
      // kprint("%d 0x%x\r\n",mc->mc_LastSecond,mc->mc_LastSecondCompare);
   }
   //!!������ ���������� mc->mc_ExpectedTime, � �� ����� ������ �������� t?
   boardClockRtn(boardClockArg, mc->mc_ExpectedTime);
}
/*-------------------- boardClockInt - ���������� ���������� �� ����� --------------------
 * ��������:
 *   arg - ����� ������� ���������� (�� ������������).
 */
void boardClockInt(long arg)
{
  boardTickCount++;
  MODCALL (  __boardClockInt(arg);  );
}

/******************************************************************************/

/* connect a routine to the clock interrupt */
#if OS_MAJOR > 3 || (OS_MAJOR == 3 && OS_MINOR >= 31)
int boardClockConnect(funcptr_t rtn, long arg)
#else
int boardClockConnect(int (rtn)(), int arg)
#endif
{
   boardClockArg = arg;
   boardClockRtn = (void*) rtn;
   return 0;
}
int boardClkRateGet()
{
   return hz;
}
unsigned boardTickGet()
{
#if 0  // (OS_VERSION > 301) || (OS_VERSION == 301 && OS_RELEASE >= 121)
   return 0;
#else
   return boardTickCount;
#endif
}
/*------------------------- boardClockDisable - ���������� ����� ------------------------
 * ������� ��������� (���������) ���������� �� �����
 */

void boardClockDisable(void)
{
    int mask = intLock();
#if defined(_MIPS_TUNE_RM7000) || defined(BT206) || defined(BT206RUS)
    int ic = boardIntControlGet();

    if(ic & IC_TE)
    {
    boardIntControlSet(ic & ~IC_IM_TIMER);
    }
    else
    {
   mask &= ~SR_IBIT8;
    }
#elif defined(K64SMP)
    boardCauseSet(boardCauseGet() | 0x08000000);
#else
    boardConfigSet(boardConfigGet() & ~0x40000000);
#endif

    intUnlock(mask);
}

/*--------------------------- boardClockEnable - ������ ����� --------------------------
 * ������� ��������� ��������:
 * - �������������� �������� count � compare;
 * - ��������� ���������� �� �����.
 * ������� ������ � ������ (��������, ����� � ���������)
 * ����� �������������� ������ ����� ������������� � ������� �����.
 *
 * ������������ ��� ������ �������.
 */
void boardClockShow()
{
    u_int cmp = cpuCompareGet();
    u_int cnt = cpuCountGet();
    printf("cmp=0x%08x cnt=0x%08x\n",cmp,cnt);
}
void boardClockEnable(void)
{
   assert(mc);
   MODCALL_LOCK(
      struct mc_ext *mce = (struct mc_ext *)mc->mc_Ext;
      if( mce )
      {
         // � mc_LastSecondCompare) �����
         //   ���������������� ��� ��������� �����
         mce->mce_CurrentNanoSecond = 0;
         mce->mce_CurrentSecondCount = 0;
         mc->mc_ExpectedTime = 0;
         cpuCountSet(0);                            /* ����� �������� count ����������    */
         boardClockReload(mc, NANOSECONDS_PER_SECOND);/* ������ ���������� ����� 1 �������*/
#if defined(_MIPS_TUNE_RM7000) || defined(BT206)
         int ic = boardIntControlGet();
         if(ic & IC_TE)
         {
            boardIntControlSet(ic | IC_IM_TIMER);
          }
          else
         {
            __lock |= SR_IBIT8;
         }
#elif defined(K64SMP)
         boardCauseSet(boardCauseGet() & ~0x08000000);
#elif  defined(VMIPS) || defined(BT128)
         __lock |= SR_IBIT8;
   #if 	defined(BT128)
         boardConfigSet(boardConfigGet() | 0x40000000);
   #endif
#else
         boardConfigSet(boardConfigGet() | 0x40000000);
#endif
      }
   );
}

#if defined(_MIPS_TUNE_RM7000) || defined(BT206)

void boardClockAltSource(int flag)
{
    int mask = intLock();

    if(flag)
    {
   boardIntControlSet(boardIntControlGet() | IC_TE);
    }
    else
    {
   boardIntControlSet(boardIntControlGet() & ~IC_TE);
    }

    intUnlock(mask);
}

#endif   /* defined(_MIPS_TUNE_RM7000) || defined(BT206) */
