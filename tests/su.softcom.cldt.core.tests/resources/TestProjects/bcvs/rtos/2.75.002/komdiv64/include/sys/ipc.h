/*
 * Copyright (c) 1988 University of Utah.
 * Copyright (c) 1990, 1993
 *	The Regents of the University of California.  All rights reserved.
 * (c) UNIX System Laboratories, Inc.
 * All or some portions of this file are derived from material licensed
 * to the University of California by American Telephone and Telegraph
 * Co. or Unix System Laboratories, Inc. and are reproduced herein with
 * the permission of UNIX System Laboratories, Inc.
 *
 * This code is derived from software contributed to Berkeley by
 * the Systems Programming Group of the University of Utah Computer
 * Science Department.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the University of
 *	California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 *	@(#)ipc.h	8.3 (Berkeley) 1/21/94
 * $Id: ipc.h,v 1.1.1.1 2004/05/11 13:36:46 nkigor Exp $
 */

/*
 * SVID compatible ipc.h file
 */
#ifndef _SYS_IPC_H_
#define _SYS_IPC_H_

typedef	long	key_t;	/* XXX should be in types.h */

struct ipc_perm {
	ushort	cuid;	/* creator user id */
	ushort	cgid;	/* creator group id */
	ushort	uid;	/* user id */
	ushort	gid;	/* group id */
	ushort	mode;	/* r/w permission */
	ushort	seq;	/* sequence # (to generate unique msg/sem/shm id) */
	key_t	key;	/* user specified msg/sem/shm key */
};

/* common mode bits */
#define	IPC_R		000400	/* read permission */
#define	IPC_W		000200	/* write/alter permission */
#define	IPC_M		010000	/* permission to change control info */

/* SVID required constants (same values as system 5) */
#define	IPC_CREAT	001000	/* create entry if key does not exist */
#define	IPC_EXCL	002000	/* fail if key exists */
#define	IPC_NOWAIT	004000	/* error if request must wait */

#define	IPC_PRIVATE	(key_t)0 /* private key */

#define	IPC_RMID	0	/* remove identifier */
#define	IPC_SET		1	/* set options */
#define	IPC_STAT	2	/* get options */

#if defined(KERNEL) && 0
/* Macros to convert between ipc ids and array indices or sequence ids */
#define	IPCID_TO_IX(id)		((id) & 0xffff)
#define	IPCID_TO_SEQ(id)	(((id) >> 16) & 0xffff)
#define	IXSEQ_TO_IPCID(ix,perm)	(((perm.seq) << 16) | (ix & 0xffff))

/*
int	ipcperm __P((struct ucred *,struct ipc_perm *,int));
*/
#else /* ! KERNEL */

/* XXX doesn't really belong here, but has been historical practice in SysV. */

#ifndef _POSIX_SOURCE
#include <sys/cdefs.h>

__BEGIN_DECLS
key_t	ftok __P((const char *, int));
__END_DECLS
#endif /* ! POSIX */

#endif /* KERNEL */

#endif /* !_SYS_IPC_H_ */
