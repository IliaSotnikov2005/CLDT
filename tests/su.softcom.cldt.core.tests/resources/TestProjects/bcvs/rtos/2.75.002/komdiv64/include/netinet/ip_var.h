/*
 * Copyright (c) 1982, 1986, 1993
 *	The Regents of the University of California.  All rights reserved.
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
 *	@(#)ip_var.h	8.2 (Berkeley) 1/9/95
 * $FreeBSD: src/sys/netinet/ip_var.h,v 1.45.2.1 1999/08/29 16:29:51 peter Exp $
 */

#ifndef _NETINET_IP_VAR_H_
#define	_NETINET_IP_VAR_H_

/*
 * Overlay for ip header used by other protocols (tcp, udp).
 */
struct ipovly {
	u_char	ih_x1[9];		/* (unused) */
	u_char	ih_pr;			/* protocol */
	u_short	ih_len;			/* protocol length */
	struct	in_addr ih_src;		/* source internet address */
	struct	in_addr ih_dst;		/* destination internet address */
};

/*
 * Ip reassembly queue structure.  Each fragment
 * being reassembled is attached to one of these structures.
 * They are timed out after ipq_ttl drops to 0, and may also
 * be reclaimed if memory becomes tight.
 */
struct ipq {
	struct	ipq *next,*prev;	/* to other reass headers */
	u_char	ipq_ttl;		/* time for reass q to live */
	u_char	ipq_p;			/* protocol of this fragment */
	u_short	ipq_id;			/* sequence id for reassembly */
	struct mbuf *ipq_frags;		/* to ip headers of fragments */
	struct	in_addr ipq_src,ipq_dst;
#ifdef IPDIVERT
	u_short ipq_divert;		/* divert protocol port */
	u_short ipq_div_cookie;		/* divert protocol cookie */
#endif
};

/*
 * Structure stored in mbuf in inpcb.ip_options
 * and passed to ip_output when ip options are in use.
 * The actual length of the options (including ipopt_dst)
 * is in m_len.
 */
#define MAX_IPOPTLEN	40

struct ipoption {
	struct	in_addr ipopt_dst;	/* first-hop dst if source routed */
	char	ipopt_list[MAX_IPOPTLEN];	/* options proper */
};

/*
 * Structure attached to inpcb.ip_moptions and
 * passed to ip_output when IP multicast options are in use.
 */
struct ip_moptions {
	struct	ifnet *imo_multicast_ifp; /* ifp for outgoing multicasts */
	u_char	imo_multicast_ttl;	/* TTL for outgoing multicasts */
	u_char	imo_multicast_loop;	/* 1 => hear sends if a member */
	u_short	imo_num_memberships;	/* no. memberships this socket */
	struct	in_multi *imo_membership[IP_MAX_MEMBERSHIPS];
	uint32_t	imo_multicast_vif;	/* vif num outgoing multicasts */
};

struct	ipstat {
	uint32_t	ips_total;		/* total packets received */
	uint32_t	ips_badsum;		/* checksum bad */
	uint32_t	ips_tooshort;		/* packet too short */
	uint32_t	ips_toosmall;		/* not enough data */
	uint32_t	ips_badhlen;		/* ip header length < data size */
	uint32_t	ips_badlen;		/* ip length < ip header length */
	uint32_t	ips_fragments;		/* fragments received */
	uint32_t	ips_fragdropped;	/* frags dropped (dups, out of space) */
	uint32_t	ips_fragtimeout;	/* fragments timed out */
	uint32_t	ips_forward;		/* packets forwarded */
	uint32_t	ips_fastforward;	/* packets fast forwarded */
	uint32_t	ips_cantforward;	/* packets rcvd for unreachable dest */
	uint32_t	ips_redirectsent;	/* packets forwarded on same net */
	uint32_t	ips_noproto;		/* unknown or unsupported protocol */
	uint32_t	ips_delivered;		/* datagrams delivered to upper level*/
	uint32_t	ips_localout;		/* total ip packets generated here */
	uint32_t	ips_odropped;		/* lost packets due to nobufs, etc. */
	uint32_t	ips_reassembled;	/* total packets reassembled ok */
	uint32_t	ips_fragmented;		/* datagrams successfully fragmented */
	uint32_t	ips_ofragments;		/* output fragments created */
	uint32_t	ips_cantfrag;		/* don't fragment flag was set, etc. */
	uint32_t	ips_badoptions;		/* error in option processing */
	uint32_t	ips_noroute;		/* packets discarded due to no route */
	uint32_t	ips_badvers;		/* ip version != 4 */
	uint32_t	ips_rawout;		/* total raw ip packets generated */
	uint32_t	ips_toolong;		/* ip length > max ip packet size */
	uint32_t	ips_notmember;		/* multicasts for unregistered grps */
};

#ifdef KERNEL

/* flags passed to ip_output as last parameter */
#define	IP_FORWARDING		0x1		/* most of ip header exists */
#define	IP_RAWOUTPUT		0x2		/* raw ip header exists */
#define	IP_ROUTETOIF		SO_DONTROUTE	/* bypass routing tables */
#define	IP_ALLOWBROADCAST	SO_BROADCAST	/* can send broadcast packets */

struct ip;
struct inpcb;
struct route;
struct sockopt;

extern struct	ipstat	ipstat;
extern u_short	ip_id;				/* ip packet ctr, for ids */
extern int	ip_defttl;			/* default IP ttl */
extern int	ipforwarding;			/* ip forwarding */
extern u_char	ip_protox[];
extern struct socket *ip_rsvpd;	/* reservation protocol daemon */
extern struct socket *ip_mrouter; /* multicast routing daemon */
extern int	(*legal_vif_num) __P((int));
extern uint32_t	(*ip_mcast_src) __P((int));
extern int rsvp_on;
extern struct	pr_usrreqs rip_usrreqs;

int	 ip_ctloutput __P((struct socket *, struct sockopt *sopt));
void	 ip_drain __P((void));
void	 ip_freemoptions __P((struct ip_moptions *));
void	 ip_init __P((void));
extern int	 (*ip_mforward) __P((struct ip *, struct ifnet *, struct mbuf *,
			  struct ip_moptions *));
int	 ip_output __P((struct mbuf *,
	    struct mbuf *, struct route *, int, struct ip_moptions *));
void	 ip_savecontrol __P((struct inpcb *, struct mbuf **, struct ip *,
		struct mbuf *));
void	 ip_slowtimo __P((void));
struct mbuf *
	 ip_srcroute __P((void));
void	 ip_stripoptions __P((struct mbuf *, struct mbuf *));
int	 rip_ctloutput __P((struct socket *, struct sockopt *));
void	 rip_ctlinput __P((int, struct sockaddr *, void *));
void	 rip_init __P((void));
void	 rip_input __P((struct mbuf *, int));
int	 rip_output __P((struct mbuf *, struct socket *, uint32_t));
void	ipip_input __P((struct mbuf *, int));
void	rsvp_input __P((struct mbuf *, int));
int	ip_rsvp_init __P((struct socket *));
int	ip_rsvp_done __P((void));
int	ip_rsvp_vif_init __P((struct socket *, struct sockopt *));
int	ip_rsvp_vif_done __P((struct socket *, struct sockopt *));
void	ip_rsvp_force_done __P((struct socket *));

#ifdef IPDIVERT
void	div_init __P((void));
void	div_input __P((struct mbuf *, int));
extern struct pr_usrreqs div_usrreqs;
extern u_short ip_divert_port;
extern u_short ip_divert_cookie;
#endif

extern struct sockaddr_in *ip_fw_fwd_addr;

#endif /* KERNEL */

#endif /* !_NETINET_IP_VAR_H_ */
