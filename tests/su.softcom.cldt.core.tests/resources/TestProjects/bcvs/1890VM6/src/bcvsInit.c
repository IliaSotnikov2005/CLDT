

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>

#include <usr401a.h>
#include "libbcvs.h"



#include <board.h>
char *boardModel(void);


unsigned bcvsInit(void)
{

  OPOLOG("\n\n");
  OPOLOG("========================\n");
  OPOLOG("����. ��������� 1890��6�\n");
  OPOLOG("------------------------\n");
  OPOLOG("boadrModel:%s\n",boardModel());
  OPOLOG("========================\n\n");


 return 0;
}

