/* MultiRoute pthread port for the bundled lwIP 2.1.3 API.
 * Written for this application; upstream lwIP retains its bundled BSD licence.
 */
#define _DEFAULT_SOURCE 1
#define _DARWIN_C_SOURCE 1
#define _POSIX_C_SOURCE 200809L
#include <pthread.h>
#include <time.h>
#include <stdarg.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#ifndef __APPLE__
#include <sys/random.h>
#endif
#include "lwip/opt.h"
#include "lwip/sys.h"
#include "lwip/tcpip.h"

struct mr_sem {pthread_mutex_t lock;pthread_cond_t changed;unsigned int count;};
struct mr_mutex {pthread_mutex_t lock;};
struct mr_mbox {pthread_mutex_t lock;pthread_cond_t readable,writable;void **items;size_t capacity,head,count;};
static pthread_mutex_t protect_lock;
static pthread_once_t protect_once=PTHREAD_ONCE_INIT;
static void initialize_protect(void){pthread_mutexattr_t attr;pthread_mutexattr_init(&attr);pthread_mutexattr_settype(&attr,PTHREAD_MUTEX_RECURSIVE);pthread_mutex_init(&protect_lock,&attr);pthread_mutexattr_destroy(&attr);}
static void deadline(struct timespec *t,u32_t ms){clock_gettime(CLOCK_REALTIME,t);t->tv_sec+=ms/1000;t->tv_nsec+=(long)(ms%1000)*1000000;if(t->tv_nsec>=1000000000){t->tv_sec++;t->tv_nsec-=1000000000;}}
void sys_init(void){pthread_once(&protect_once,initialize_protect);}
u32_t sys_now(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return (u32_t)((uint64_t)t.tv_sec*1000+(uint64_t)t.tv_nsec/1000000);}
u32_t sys_jiffies(void){return sys_now();}
sys_prot_t sys_arch_protect(void){sys_init();pthread_mutex_lock(&protect_lock);return 0;}
void sys_arch_unprotect(sys_prot_t value){(void)value;pthread_mutex_unlock(&protect_lock);}
err_t sys_sem_new(sys_sem_t *out,u8_t count){struct mr_sem *s=calloc(1,sizeof(*s));*out=NULL;if(!s)return ERR_MEM;if(pthread_mutex_init(&s->lock,NULL)){free(s);return ERR_MEM;}if(pthread_cond_init(&s->changed,NULL)){pthread_mutex_destroy(&s->lock);free(s);return ERR_MEM;}s->count=count;*out=s;return ERR_OK;}
void sys_sem_signal(sys_sem_t *p){struct mr_sem *s=*p;pthread_mutex_lock(&s->lock);if(s->count<UINT32_MAX)s->count++;pthread_cond_signal(&s->changed);pthread_mutex_unlock(&s->lock);}
u32_t sys_arch_sem_wait(sys_sem_t *p,u32_t timeout){struct mr_sem *s=*p;struct timespec until;u32_t start=sys_now();int error=0;if(timeout)deadline(&until,timeout);pthread_mutex_lock(&s->lock);while(!s->count){error=timeout?pthread_cond_timedwait(&s->changed,&s->lock,&until):pthread_cond_wait(&s->changed,&s->lock);if(error==ETIMEDOUT||error){pthread_mutex_unlock(&s->lock);return SYS_ARCH_TIMEOUT;}}s->count--;pthread_mutex_unlock(&s->lock);return sys_now()-start;}
void sys_sem_free(sys_sem_t *p){if(!*p)return;pthread_cond_destroy(&(*p)->changed);pthread_mutex_destroy(&(*p)->lock);free(*p);*p=NULL;}
err_t sys_mutex_new(sys_mutex_t *out){struct mr_mutex *m=calloc(1,sizeof(*m));*out=NULL;if(!m)return ERR_MEM;if(pthread_mutex_init(&m->lock,NULL)){free(m);return ERR_MEM;}*out=m;return ERR_OK;}
void sys_mutex_lock(sys_mutex_t *p){pthread_mutex_lock(&(*p)->lock);}
void sys_mutex_unlock(sys_mutex_t *p){pthread_mutex_unlock(&(*p)->lock);}
void sys_mutex_free(sys_mutex_t *p){if(!*p)return;pthread_mutex_destroy(&(*p)->lock);free(*p);*p=NULL;}
void sys_mark_tcpip_thread(void){}
void sys_lock_tcpip_core(void){sys_mutex_lock(&lock_tcpip_core);}
void sys_unlock_tcpip_core(void){sys_mutex_unlock(&lock_tcpip_core);}
err_t sys_mbox_new(sys_mbox_t *out,int size){struct mr_mbox *m;*out=NULL;if(size<1||size>65536)return ERR_MEM;m=calloc(1,sizeof(*m));if(!m)return ERR_MEM;m->items=calloc((size_t)size,sizeof(void*));if(!m->items){free(m);return ERR_MEM;}if(pthread_mutex_init(&m->lock,NULL)){free(m->items);free(m);return ERR_MEM;}if(pthread_cond_init(&m->readable,NULL)){pthread_mutex_destroy(&m->lock);free(m->items);free(m);return ERR_MEM;}if(pthread_cond_init(&m->writable,NULL)){pthread_cond_destroy(&m->readable);pthread_mutex_destroy(&m->lock);free(m->items);free(m);return ERR_MEM;}m->capacity=(size_t)size;*out=m;return ERR_OK;}
void sys_mbox_post(sys_mbox_t *p,void *msg){struct mr_mbox *m=*p;pthread_mutex_lock(&m->lock);while(m->count==m->capacity)pthread_cond_wait(&m->writable,&m->lock);m->items[(m->head+m->count)%m->capacity]=msg;m->count++;pthread_cond_signal(&m->readable);pthread_mutex_unlock(&m->lock);}
err_t sys_mbox_trypost(sys_mbox_t *p,void *msg){struct mr_mbox *m=*p;pthread_mutex_lock(&m->lock);if(m->count==m->capacity){pthread_mutex_unlock(&m->lock);return ERR_MEM;}m->items[(m->head+m->count)%m->capacity]=msg;m->count++;pthread_cond_signal(&m->readable);pthread_mutex_unlock(&m->lock);return ERR_OK;}
err_t sys_mbox_trypost_fromisr(sys_mbox_t *p,void *msg){return sys_mbox_trypost(p,msg);}
u32_t sys_arch_mbox_fetch(sys_mbox_t *p,void **out,u32_t timeout){struct mr_mbox *m=*p;struct timespec until;u32_t start=sys_now();int error;if(timeout)deadline(&until,timeout);pthread_mutex_lock(&m->lock);while(!m->count){error=timeout?pthread_cond_timedwait(&m->readable,&m->lock,&until):pthread_cond_wait(&m->readable,&m->lock);if(error){pthread_mutex_unlock(&m->lock);return SYS_ARCH_TIMEOUT;}}if(out)*out=m->items[m->head];m->items[m->head]=NULL;m->head=(m->head+1)%m->capacity;m->count--;pthread_cond_signal(&m->writable);pthread_mutex_unlock(&m->lock);return sys_now()-start;}
u32_t sys_arch_mbox_tryfetch(sys_mbox_t *p,void **out){struct mr_mbox *m=*p;pthread_mutex_lock(&m->lock);if(!m->count){pthread_mutex_unlock(&m->lock);return SYS_MBOX_EMPTY;}if(out)*out=m->items[m->head];m->items[m->head]=NULL;m->head=(m->head+1)%m->capacity;m->count--;pthread_cond_signal(&m->writable);pthread_mutex_unlock(&m->lock);return 0;}
void sys_mbox_free(sys_mbox_t *p){if(!*p)return;pthread_cond_destroy(&(*p)->readable);pthread_cond_destroy(&(*p)->writable);pthread_mutex_destroy(&(*p)->lock);free((*p)->items);free(*p);*p=NULL;}
struct thread_start {lwip_thread_fn fn;void *arg;char name[64];};
static void *thread_entry(void *pointer){struct thread_start start=*(struct thread_start*)pointer;free(pointer);
#ifdef __APPLE__
  pthread_setname_np(start.name);
#endif
  start.fn(start.arg);return NULL;}
sys_thread_t sys_thread_new(const char *name,lwip_thread_fn fn,void *arg,int stacksize,int prio){pthread_t thread;struct thread_start *start=calloc(1,sizeof(*start));pthread_attr_t attr;(void)prio;if(!start)abort();start->fn=fn;start->arg=arg;snprintf(start->name,sizeof(start->name),"%s",name?name:"lwIP");pthread_attr_init(&attr);if(stacksize>=PTHREAD_STACK_MIN)pthread_attr_setstacksize(&attr,(size_t)stacksize);if(pthread_create(&thread,&attr,thread_entry,start))abort();pthread_attr_destroy(&attr);pthread_detach(thread);return thread;}
void multiroute_lwip_diag(const char *format,...){va_list args;va_start(args,format);vfprintf(stderr,format,args);va_end(args);}
uint32_t multiroute_lwip_rand(void){uint32_t value;
#ifdef __APPLE__
  return arc4random();
#else
  size_t done=0;while(done<sizeof(value)){ssize_t n=getrandom((char*)&value+done,sizeof(value)-done,0);if(n>0)done+=(size_t)n;else if(errno!=EINTR)abort();}return value;
#endif
}
