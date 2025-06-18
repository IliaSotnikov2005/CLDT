/*

Based upon IDT provided code with the following release:

This source code has been made available to you by IDT on an AS-IS
basis. Anyone receiving this source is licensed under IDT copyrights
to use it in any way he or she deems fit, including copying it,
modifying it, compiling it, and redistributing it either with or
without modifications.  No license under IDT patents or patent
applications is to be implied by the copyright license.

Any user of this software should understand that IDT cannot provide
technical support for this software and will not be responsible for
any consequences resulting from the use of this software.

Any person who transfers this source code or any derivative work must
include the IDT copyright notice, this paragraph, and the preceeding
two paragraphs in the transferred software.

COPYRIGHT IDT CORPORATION 1996
LICENSED MATERIAL - PROGRAM PROPERTY OF IDT

  iregdef.h,v 1.2 1996/09/11 19:16:04 joel Exp
*/ 

/*
**  iregdef.h - IDT R3000 register structure header file
**
**  Copyright 1989 Integrated Device Technology, Inc
**  All Rights Reserved
**
*/
#ifndef __IREGDEF_H__
#define __IREGDEF_H__
#ifndef CPU
#error What CPU?
#endif

#ifdef __ASSEMBLER__
/*
 * 950313: Ketan added sreg/lreg and R_SZ for 64-bit saves
 *         added Register definition for XContext reg.
 *         Look towards end of this file.
 */
/*
** register names 
*/
#define r0  $0
#define r1  $1
#define r2  $2
#define r3  $3
#define r4  $4
#define r5  $5
#define r6  $6
#define r7  $7
#define r8  $8
#define r9  $9
#define r10 $10
#define r11 $11
#define r12 $12
#define r13 $13

#define r14 $14
#define r15 $15
#define r16 $16
#define r17 $17
#define r18 $18
#define r19 $19
#define r20 $20
#define r21 $21
#define r22 $22
#define r23 $23
#define r24 $24
#define r25 $25
#define r26 $26
#define r27 $27
#define r28 $28
#define r29 $29
#define r30 $30
#define r31 $31

#define fp0 $f0
#define fp1 $f1
#define fp2 $f2
#define fp3 $f3
#define fp4 $f4
#define fp5 $f5
#define fp6 $f6
#define fp7 $f7
#define fp8 $f8
#define fp9 $f9
#define fp10    $f10
#define fp11    $f11
#define fp12    $f12
#define fp13    $f13
#define fp14    $f14
#define fp15    $f15
#define fp16    $f16
#define fp17    $f17
#define fp18    $f18
#define fp19    $f19
#define fp20    $f20
#define fp21    $f21
#define fp22    $f22
#define fp23    $f23
#define fp24    $f24
#define fp25    $f25
#define fp26    $f26
#define fp27    $f27
#define fp28    $f28
#define fp29    $f29
#define fp30    $f30
#define fp31    $f31

#define fcr0    $0
#define fcr30   $30
#define fcr31   $31

#define C1_IR   $0  /* implementation/revision reg */
#define C1_SR   $31 /* control/status reg */

#define zero    $0  /* wired zero */
#define AT  $at /* assembler temp */
#define v0  $2  /* return value */
#define v1  $3
#define a0  $4  /* argument registers a0-a3 */
#define a1  $5
#define a2  $6
#define a3  $7
#define t0  $8  /* caller saved  t0-t9 */
#define t1  $9
#define t2  $10
#define t3  $11
#define t4  $12
#define t5  $13
#define t6  $14
#define t7  $15
#define s0  $16 /* callee saved s0-s8 */
#define s1  $17
#define s2  $18
#define s3  $19
#define s4  $20
#define s5  $21
#define s6  $22
#define s7  $23
#define t8  $24
#define t9  $25
#define k0  $26 /* kernel usage */
#define k1  $27 /* kernel usage */
#define gp  $28 /* sdata pointer */
#define sp  $29 /* stack pointer */
#define s8  $30 /* yet another saved reg for the callee */
#define fp  $30 /* frame pointer - this is being phased out by MIPS */
#define ra  $31 /* return address */
#endif
/* Ketan added the following */
#if CPU == R3000
#define sreg    sw
#define lreg    lw
#define rmfc0   mfc0
#define rmtc0   mtc0
#define R_SZ    4
#endif

#if CPU == R4000
#if __mips < 3
#define sreg    sw
#define lreg    lw
#define rmfc0   mfc0
#define rmtc0   mtc0
#define R_SZ    4
#else
#define sreg    sd
#define lreg    ld
#define rmfc0   dmfc0
#define rmtc0   dmtc0
#define R_SZ    8
#endif
#endif
/* Ketan till here */

/* Смещение во фрагменте стека для запоминания регистров                               *
 *                                      в момент возникновения исключительной ситуации */
#define OFFSET 4*R_SZ
#define R_R0        (0*R_SZ+OFFSET)
#define R_R1        (1*R_SZ+OFFSET)
#define R_R2        (2*R_SZ+OFFSET)
#define R_R3        (3*R_SZ+OFFSET)
#define R_R4        (4*R_SZ+OFFSET)
#define R_R5        (5*R_SZ+OFFSET)
#define R_R6        (6*R_SZ+OFFSET)
#define R_R7        (7*R_SZ+OFFSET)
#define R_R8        (8*R_SZ+OFFSET)
#define R_R9        (9*R_SZ+OFFSET)
#define R_R10       (10*R_SZ+OFFSET)
#define R_R11       (11*R_SZ+OFFSET)
#define R_R12       (12*R_SZ+OFFSET)
#define R_R13       (13*R_SZ+OFFSET)
#define R_R14       (14*R_SZ+OFFSET)
#define R_R15       (15*R_SZ+OFFSET)
#define R_R16       (16*R_SZ+OFFSET)
#define R_R17       (17*R_SZ+OFFSET)
#define R_R18       (18*R_SZ+OFFSET)
#define R_R19       (19*R_SZ+OFFSET)
#define R_R20       (20*R_SZ+OFFSET)
#define R_R21       (21*R_SZ+OFFSET)
#define R_R22       (22*R_SZ+OFFSET)
#define R_R23       (23*R_SZ+OFFSET)
#define R_R24       (24*R_SZ+OFFSET)
#define R_R25       (25*R_SZ+OFFSET)
#define R_R26       (26*R_SZ+OFFSET)
#define R_R27       (27*R_SZ+OFFSET)
#define R_R28       (28*R_SZ+OFFSET)
#define R_R29       (29*R_SZ+OFFSET)
#define R_R30       (30*R_SZ+OFFSET)
#define R_R31       (31*R_SZ+OFFSET)
#define NCLIENTREGS (32*R_SZ+OFFSET)
#define R_EPC       (32*R_SZ+OFFSET)
#define R_MDHI      (33*R_SZ+OFFSET)
#define R_MDLO      (34*R_SZ+OFFSET)
#define R_SR        (35*R_SZ+OFFSET)
#define R_CAUSE     (36*R_SZ+OFFSET)
#define R_BADVADDR  (37*R_SZ+OFFSET)
#define R_IC        (38*R_SZ+OFFSET)   /* interrupt control RM7000 */
#if OS_MAJOR >= 3
#define R_KST       (39*R_SZ+OFFSET)
#define R_SK1       (40*R_SZ+OFFSET)
#define NREGS        41
#else
#define R_EXCTYPE   (39*R_SZ+OFFSET)
#define R_ERRNO     (40*R_SZ+OFFSET)
#define NREGS        41
#endif
#define ISR_FRAME_SIZE (NREGS*R_SZ+OFFSET)

/*
** For those who like to think in terms of the compiler names for the regs
*/
#define R_ZERO      R_R0
#define R_AT        R_R1
#define R_V0        R_R2
#define R_V1        R_R3
#define R_A0        R_R4
#define R_A1        R_R5
#define R_A2        R_R6
#define R_A3        R_R7
#define R_T0        R_R8
#define R_T1        R_R9
#define R_T2        R_R10
#define R_T3        R_R11
#define R_T4        R_R12
#define R_T5        R_R13
#define R_T6        R_R14
#define R_T7        R_R15
#define R_S0        R_R16
#define R_S1        R_R17
#define R_S2        R_R18
#define R_S3        R_R19
#define R_S4        R_R20
#define R_S5        R_R21
#define R_S6        R_R22
#define R_S7        R_R23
#define R_T8        R_R24
#define R_T9        R_R25
#define R_K0        R_R26
#define R_K1        R_R27
#define R_GP        R_R28
#define R_SP        R_R29
#define R_FP        R_R30
#define R_RA        R_R31

#endif /* __IREGDEF_H__ */

