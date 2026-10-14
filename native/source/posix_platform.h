#ifndef MULTIROUTE_POSIX_PLATFORM_H
#define MULTIROUTE_POSIX_PLATFORM_H
#define _DEFAULT_SOURCE 1
#define _DARWIN_C_SOURCE 1
#define _POSIX_C_SOURCE 200809L
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/file.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netdb.h>
#include <net/if.h>
#include <ifaddrs.h>
#include <pthread.h>
#include <signal.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <stdint.h>
#include <inttypes.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <time.h>
#include <pcap/pcap.h>
#ifdef __APPLE__
#include <net/if_dl.h>
#include <net/if_types.h>
#include <net/if_media.h>
#include <libproc.h>
#else
#include <netpacket/packet.h>
#include <net/if_arp.h>
#endif
/* Narrow compatibility names used by the unchanged protocol algorithms. */
#ifdef SOCKET
#undef SOCKET
#endif
#ifdef INVALID_SOCKET
#undef INVALID_SOCKET
#endif
typedef int SOCKET;
typedef int32_t LONG;
typedef int64_t LONG64;
typedef uint64_t ULONGLONG;
typedef void *PVOID;
typedef pthread_mutex_t CRITICAL_SECTION;
typedef pthread_t *thread_handle;
#define INVALID_SOCKET (-1)
#define SD_BOTH SHUT_RDWR
#define WSAEWOULDBLOCK EAGAIN
#define WSAGetLastError() errno
#define closesocket close
#define _strnicmp strncasecmp
#define InitializeCriticalSection(p) pthread_mutex_init((p),NULL)
#define EnterCriticalSection(p) pthread_mutex_lock(p)
#define LeaveCriticalSection(p) pthread_mutex_unlock(p)
#define InterlockedIncrement(p) __atomic_add_fetch((p),1,__ATOMIC_SEQ_CST)
#define InterlockedDecrement(p) __atomic_sub_fetch((p),1,__ATOMIC_SEQ_CST)
#define InterlockedIncrement64(p) __atomic_add_fetch((p),1,__ATOMIC_SEQ_CST)
#define InterlockedAdd64(p,v) __atomic_add_fetch((p),(v),__ATOMIC_SEQ_CST)
#define InterlockedExchange(p,v) __atomic_exchange_n((p),(v),__ATOMIC_SEQ_CST)
#define InterlockedExchangePointer(p,v) ((void)__atomic_exchange_n((p),(v),__ATOMIC_SEQ_CST))
static ULONGLONG GetTickCount64(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return (uint64_t)t.tv_sec*1000+(uint64_t)t.tv_nsec/1000000;}
static void Sleep(unsigned int milliseconds){struct timespec t={(time_t)(milliseconds/1000),(long)(milliseconds%1000)*1000000};while(nanosleep(&t,&t)<0&&errno==EINTR);}
static thread_handle spawn_thread(void *(*entry)(void*),void *arg){thread_handle h=malloc(sizeof(*h));if(!h)return NULL;if(pthread_create(h,NULL,entry,arg)){free(h);return NULL;}return h;}
static void detach_thread(thread_handle h){pthread_detach(*h);free(h);}
static int set_nonblocking(SOCKET s,int flag){int flags=fcntl(s,F_GETFL,0);return flags<0?-1:fcntl(s,F_SETFL,flag?(flags|O_NONBLOCK):(flags&~O_NONBLOCK));}
static int set_receive_timeout(SOCKET s,unsigned int ms){struct timeval t={(time_t)(ms/1000),(suseconds_t)(ms%1000)*1000};return setsockopt(s,SOL_SOCKET,SO_RCVTIMEO,&t,sizeof(t));}
#endif
