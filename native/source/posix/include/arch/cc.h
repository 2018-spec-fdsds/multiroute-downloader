#ifndef MULTIROUTE_POSIX_CC_H
#define MULTIROUTE_POSIX_CC_H
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/time.h>
#include <arpa/inet.h>
#define LWIP_DONT_PROVIDE_BYTEORDER_FUNCTIONS 1
#define LWIP_TIMEVAL_PRIVATE 0
typedef unsigned int sys_prot_t;
#define PACK_STRUCT_BEGIN
#define PACK_STRUCT_END
#define PACK_STRUCT_STRUCT __attribute__((packed))
#define PACK_STRUCT_FIELD(x) x
void multiroute_lwip_diag(const char *format,...);
#define LWIP_PLATFORM_DIAG(x) multiroute_lwip_diag x
#define LWIP_PLATFORM_ASSERT(x) do {multiroute_lwip_diag("lwIP assertion: %s (%s:%d)\n",(x),__FILE__,__LINE__);abort();} while(0)
uint32_t multiroute_lwip_rand(void);
#define LWIP_RAND() multiroute_lwip_rand()
#endif
