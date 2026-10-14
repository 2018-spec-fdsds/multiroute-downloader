#ifndef MULTIROUTE_POSIX_SYS_ARCH_H
#define MULTIROUTE_POSIX_SYS_ARCH_H
#include <pthread.h>
struct mr_sem;
struct mr_mutex;
struct mr_mbox;
typedef struct mr_sem *sys_sem_t;
typedef struct mr_mutex *sys_mutex_t;
typedef struct mr_mbox *sys_mbox_t;
typedef pthread_t sys_thread_t;
#define SYS_SEM_NULL NULL
#define SYS_MBOX_NULL NULL
#define sys_sem_valid(p) ((p)!=NULL&&*(p)!=NULL)
#define sys_sem_set_invalid(p) (*(p)=NULL)
#define sys_mutex_valid(p) ((p)!=NULL&&*(p)!=NULL)
#define sys_mutex_set_invalid(p) (*(p)=NULL)
#define sys_mbox_valid(p) ((p)!=NULL&&*(p)!=NULL)
#define sys_mbox_set_invalid(p) (*(p)=NULL)
void sys_mark_tcpip_thread(void);
#define LWIP_MARK_TCPIP_THREAD() sys_mark_tcpip_thread()
void sys_lock_tcpip_core(void);
void sys_unlock_tcpip_core(void);
#define LOCK_TCPIP_CORE() sys_lock_tcpip_core()
#define UNLOCK_TCPIP_CORE() sys_unlock_tcpip_core()
#endif
