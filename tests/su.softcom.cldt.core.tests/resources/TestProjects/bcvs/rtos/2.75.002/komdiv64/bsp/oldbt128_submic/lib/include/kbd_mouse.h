//  ���� keyboard_lib.h �������� �������� ������� ��� ������ c
//		�����������.
//	(c) ����� ��� ������ 1.0 21-01-2004 ����.										

#ifndef __KEYBOARD_LIB_H__
#define __KEYBOARD_LIB_H__

#ifdef __cplusplus
extern "C" {
#endif

enum KL85_CMD
{
	KL85_CMD_MODE	= 0xf0,	// ���������� ����� ����-����� (�� KL_85_MODE)
	KL85_CMD_ACK	= 0xfa,	// �������������
};

enum KL85_MODE
{
	KL85_MODE_XT	= 0x01,	// ���������� ����� ����-����� XT
	KL85_MODE_AT	= 0x02,	// ���������� ����� ����-����� AT
};

enum TYPE_MOUSE
{
	PC_MOUSE,
	MS_MOUSE
};

extern int readCim( int fn, char* buf );
extern int setKbdLeds( int fn, int leds );
extern int setKbdXT( int fn );
extern int serial_port_kl85_mode( int device_fd, int mode );
extern int serial_port_kl85_init( char *device_name, int mode );
extern int serial_port_mouse_init( char *device_name,	int mode );

#ifdef __cplusplus
}
#endif

#endif // __KEYBOARD_LIB_H__ 
