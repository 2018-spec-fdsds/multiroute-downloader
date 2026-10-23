/* Real owned loopback IPC against the production broker/tunnel implementation.
 * lwIP below is fake: no public socket, packet capture, OS DNS or route changes. */
#define _CRT_SECURE_NO_WARNINGS
#define _DEFAULT_SOURCE 1
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#else
#include <sys/socket.h>
#include <sys/select.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <stdint.h>
typedef int SOCKET;typedef int32_t LONG;typedef uint64_t ULONGLONG;typedef void *PVOID;typedef pthread_mutex_t CRITICAL_SECTION;typedef pthread_t *thread_handle;
#define INVALID_SOCKET (-1)
#define SD_BOTH SHUT_RDWR
#define WSAGetLastError() errno
#define closesocket close
#define InitializeCriticalSection(p) pthread_mutex_init((p),NULL)
#define EnterCriticalSection(p) pthread_mutex_lock(p)
#define LeaveCriticalSection(p) pthread_mutex_unlock(p)
#define InterlockedIncrement(p) __atomic_add_fetch((p),1,__ATOMIC_SEQ_CST)
#define InterlockedDecrement(p) __atomic_sub_fetch((p),1,__ATOMIC_SEQ_CST)
#define InterlockedExchange(p,v) __atomic_exchange_n((p),(v),__ATOMIC_SEQ_CST)
static ULONGLONG GetTickCount64(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return (uint64_t)t.tv_sec*1000+(uint64_t)t.tv_nsec/1000000;}
static void Sleep(unsigned ms){struct timespec t={ms/1000,(long)(ms%1000)*1000000};nanosleep(&t,NULL);}
static int set_nonblocking(SOCKET s,int mode){int flags=fcntl(s,F_GETFL,0);return flags<0?-1:fcntl(s,F_SETFL,mode?(flags|O_NONBLOCK):(flags&~O_NONBLOCK));}
static thread_handle spawn_thread(void *(*entry)(void*),void *arg){thread_handle t=malloc(sizeof(*t));if(!t)return NULL;if(pthread_create(t,NULL,entry,arg)){free(t);return NULL;}return t;}
static void detach_thread(thread_handle t){pthread_detach(*t);free(t);}
#endif
#include <assert.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
typedef uint16_t u16_t;typedef int err_t;typedef struct {uint32_t addr;} ip_addr_t;
struct netif{unsigned index;};
typedef struct profile{char gateway[16];uint32_t gateway_host;volatile LONG configured;int mac_known;struct netif netif;}profile;
struct tcp_pcb{int state;};
struct netconn{int type;unsigned index;uint16_t port;struct tcp_pcb tcp;struct{struct tcp_pcb *tcp;}pcb;unsigned char echo[8192];size_t used;};
struct netbuf{unsigned char data[8192];u16_t len;ip_addr_t from;uint16_t fromport;};
#define ERR_OK 0
#define ERR_USE -8
#define ERR_INPROGRESS -5
#define ERR_TIMEOUT -3
#define ERR_WOULDBLOCK -7
#define ERR_ARG -16
#define NETCONN_UDP 1
#define NETCONN_TCP 2
#define NETCONN_COPY 1
#define ESTABLISHED 4
#define LOCK_TCPIP_CORE() ((void)0)
#define UNLOCK_TCPIP_CORE() ((void)0)
#define netif_get_index(n) ((n)->index)
#define ip_addr_set_ip4_u32(p,n) ((p)->addr=(n))
#define ip_addr_cmp(a,b) ((a)->addr==(b)->addr)
static profile profiles[4];static int profile_count=4;static profile *volatile port_routes[65536],*volatile udp_port_routes[65536];
static uint32_t virtual_host=0xc0a80146U;static volatile LONG stopped,rng,created,deleted,legacy_queries,broker_calls,per_route[4],egress_per_route[4];static LONG bound_ports[65536];static CRITICAL_SECTION fixture_lock;static unsigned checks;
static volatile LONG connection_count=96;
static int cancelled(void){return stopped!=0;}
static uint32_t test_random(void){return (uint32_t)InterlockedIncrement(&rng);}
#define LWIP_RAND() test_random()
static int parse_ip(const char *text,uint32_t *out){struct in_addr a;
#ifdef _WIN32
  if(InetPtonA(AF_INET,text,&a)!=1)return 0;
#else
  if(inet_pton(AF_INET,text,&a)!=1)return 0;
#endif
  *out=ntohl(a.s_addr);return 1;
}
static void check(int yes){checks++;assert(yes);}
static struct netconn *netconn_new(int type){struct netconn *c=calloc(1,sizeof(*c));if(c){c->type=type;c->pcb.tcp=&c->tcp;InterlockedIncrement(&created);}return c;}
static err_t netconn_bind_if(struct netconn *c,unsigned index){c->index=index;return ERR_OK;}
static err_t netconn_bind(struct netconn *c,const ip_addr_t *local,uint16_t port){err_t result=ERR_OK;assert(local->addr==htonl(virtual_host));EnterCriticalSection(&fixture_lock);if(bound_ports[port])result=ERR_USE;else{bound_ports[port]=1;c->port=port;}LeaveCriticalSection(&fixture_lock);return result;}
static err_t netconn_connect(struct netconn *c,const ip_addr_t *remote,uint16_t port){(void)remote;if(c->type==NETCONN_UDP){InterlockedIncrement(&legacy_queries);assert(port==53);}else{assert(c->index>=1&&c->index<=4);InterlockedIncrement(&egress_per_route[c->index-1]);c->tcp.state=ESTABLISHED;}return ERR_OK;}
static err_t netconn_err(struct netconn *c){(void)c;return ERR_OK;}
static void netconn_set_nonblocking(struct netconn *c,int mode){(void)c;(void)mode;}
static void netconn_set_recvtimeout(struct netconn *c,int ms){(void)c;(void)ms;}
static void netconn_set_sendtimeout(struct netconn *c,int ms){(void)c;(void)ms;}
static struct netbuf *netbuf_new(void){return calloc(1,sizeof(struct netbuf));}
static void *netbuf_alloc(struct netbuf *b,u16_t n){assert(n<=sizeof(b->data));b->len=n;return b->data;}
static void netbuf_delete(struct netbuf *b){free(b);}
static err_t netconn_send(struct netconn *c,struct netbuf *b){(void)c;(void)b;InterlockedIncrement(&legacy_queries);return ERR_TIMEOUT;}
static u16_t netbuf_len(struct netbuf *b){return b->len;}
static uint16_t netbuf_fromport(struct netbuf *b){return b->fromport;}
static ip_addr_t *netbuf_fromaddr(struct netbuf *b){return &b->from;}
static u16_t netbuf_copy(struct netbuf *b,void *out,u16_t n){if(n>b->len)return 0;memcpy(out,b->data,n);return n;}
static void netbuf_data(struct netbuf *b,void **out,u16_t *n){*out=b->data;*n=b->len;}
static int netbuf_next(struct netbuf *b){(void)b;return -1;}
static err_t netconn_write_partly(struct netconn *c,const void *data,size_t n,int flags,size_t *written){(void)flags;if(c->type==NETCONN_UDP||n>sizeof(c->echo)-c->used)return ERR_ARG;memcpy(c->echo+c->used,data,n);c->used+=n;*written=n;return ERR_OK;}
static err_t netconn_recv(struct netconn *c,struct netbuf **out){*out=NULL;if(!c->used)return ERR_TIMEOUT;*out=netbuf_new();(*out)->len=(u16_t)c->used;memcpy((*out)->data,c->echo,c->used);c->used=0;return ERR_OK;}
static void netconn_delete(struct netconn *c){if(c->port){EnterCriticalSection(&fixture_lock);bound_ports[c->port]=0;LeaveCriticalSection(&fixture_lock);}InterlockedIncrement(&deleted);free(c);}
#define MR_DNS_MANAGED_SUPPORT
#include "../gateway_dns.h"
#ifdef _WIN32
#define TEST_THREAD_RESULT DWORD WINAPI
#define TEST_THREAD_ARGUMENT LPVOID
#else
#define TEST_THREAD_RESULT void *
#define TEST_THREAD_ARGUMENT void *
#endif
static SOCKET broker_listener=INVALID_SOCKET;
static TEST_THREAD_RESULT broker_server(TEST_THREAD_ARGUMENT ignored){
  (void)ignored;
  while(!cancelled()){
    SOCKET s;unsigned char header[100],query[4096],answer[8192],response[3];size_t n,offset=12;char host[254],gateway[16];mr_dns_policy policy;int index;uint64_t revision=0;ULONGLONG deadline;
    if(!mr_dns_os_wait(broker_listener,0,GetTickCount64()+50,NULL))continue;
    s=accept(broker_listener,NULL,NULL);if(s==INVALID_SOCKET)continue;mr_dns_os_nonblocking(s);deadline=GetTickCount64()+2000;
    if(!mr_dns_os_bytes(s,header,100,0,deadline,NULL))goto close_client;
    assert(!memcmp(header,"MRDNS2",6));mr_dns_policy_snapshot(&policy);assert(mr_dns_same_key(header+6,policy.key));assert(mr_dns_padded_gateway(header+70,gateway));
    for(size_t i=0;i<8;i++)revision=(revision<<8)|header[86+i];assert(revision==policy.revision);assert(mr_dns_u32(header+94)>0&&mr_dns_u32(header+94)<=8000);
    n=mr_dns_u16(header+98);assert(n>=12&&n<=4096);if(!mr_dns_os_bytes(s,query,n,0,deadline,NULL))goto close_client;
    assert(mr_dns_name(query,n,&offset,host));index=gateway[strlen(gateway)-1]-'1';assert(index>=0&&index<4);InterlockedIncrement(&broker_calls);InterlockedIncrement(&per_route[index]);
    if(!strcmp(host,"slow.example"))Sleep(150);
    if(!strcmp(host,"switch.example")){policy.revision++;mr_dns_policy_commit(&policy);}
    n=offset+4;memcpy(answer,query,n);answer[2]=0x81;answer[3]=0x80;answer[6]=0;answer[7]=1;answer[10]=answer[11]=0;
    answer[n++]=0xc0;answer[n++]=12;mr_dns_put16(answer+n,1);n+=2;mr_dns_put16(answer+n,1);n+=2;memset(answer+n,0,4);answer[n+3]=(unsigned char)(!strcmp(host,"zero.example")?0:60);n+=4;mr_dns_put16(answer+n,4);n+=2;answer[n++]=93;answer[n++]=184;answer[n++]=216;answer[n++]=(unsigned char)(34+index);
    if(!strcmp(host,"bad-id.example"))answer[1]^=1;
    if(!strcmp(host,"private.example"))answer[n-4]=127;
    response[0]=1;mr_dns_put16(response+1,(uint16_t)n);
    if(!mr_dns_os_bytes(s,response,3,1,deadline,NULL))goto close_client;
    for(size_t i=0;i<n;i+=3){size_t count=n-i<3?n-i:3;if(!mr_dns_os_bytes(s,answer+i,count,1,deadline,NULL))break;}
close_client:shutdown(s,SD_BOTH);closesocket(s);
  }return 0;
}
static unsigned short start_broker(void){struct sockaddr_in address;
#ifdef _WIN32
  int size=sizeof(address);HANDLE thread;
#else
  socklen_t size=sizeof(address);thread_handle thread;
#endif
  broker_listener=socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);assert(broker_listener!=INVALID_SOCKET);memset(&address,0,sizeof(address));address.sin_family=AF_INET;address.sin_addr.s_addr=htonl(INADDR_LOOPBACK);assert(!bind(broker_listener,(struct sockaddr*)&address,sizeof(address))&&!listen(broker_listener,16)&&!getsockname(broker_listener,(struct sockaddr*)&address,&size));
#ifdef _WIN32
  thread=CreateThread(NULL,0,broker_server,NULL,0,NULL);assert(thread);CloseHandle(thread);
#else
  thread=spawn_thread(broker_server,NULL);assert(thread);detach_thread(thread);
#endif
  return ntohs(address.sin_port);
}
static SOCKET open_tunnel(unsigned route,int corrupt,int private_destination){
  mr_dns_policy policy;unsigned char request[92]={0};SOCKET s;ULONGLONG deadline=GetTickCount64()+2000;
  mr_dns_policy_snapshot(&policy);s=mr_dns_broker_connect(mr_dns_tunnel_port,deadline,NULL);assert(s!=INVALID_SOCKET);
  memcpy(request,"MRDNT1",6);memcpy(request+6,policy.key,64);memcpy(request+70,profiles[route].gateway,strlen(profiles[route].gateway));request[86]=(unsigned char)(private_destination?127:1);request[87]=1;request[88]=1;request[89]=1;mr_dns_put16(request+90,443);if(corrupt)request[6]^=1;
  assert(mr_dns_os_bytes(s,request,sizeof(request),1,deadline,NULL));return s;
}
int main(void){
  mr_dns_policy policy;ip_addr_t resolved;SOCKET sockets[8],extra;unsigned char status,echo[32];LONG calls,old_created;uint32_t ip,ttl;ULONGLONG started;FILE *report;char text[512];
#ifdef _WIN32
  WSADATA wsa;assert(!WSAStartup(MAKEWORD(2,2),&wsa));
#else
  signal(SIGPIPE,SIG_IGN);
#endif
  InitializeCriticalSection(&fixture_lock);gateway_dns_init();
  for(unsigned i=0;i<4;i++){snprintf(profiles[i].gateway,16,"192.168.1.%u",i+1);profiles[i].gateway_host=0xc0a80101U+i;profiles[i].configured=profiles[i].mac_known=1;profiles[i].netif.index=i+1;}
  memset(&policy,0,sizeof(policy));managed_dns_required=1;check(!resolve_remote(&profiles[0],"example.com",&resolved));check(legacy_queries==0);
  memset(policy.key,'a',64);policy.revision=1;policy.broker_port=start_broker();mr_dns_policy_commit(&policy);
  check(mr_dns_broker_protocol_self_test()==0&&gateway_dns_self_test()==0);check(mr_dns_policy_permitted(&policy));
  for(unsigned i=0;i<4;i++){check(resolve_remote(&profiles[i],"example.com",&resolved));check(resolved.addr==htonl(0x5db8d822U+i));check(per_route[i]==1);}
  calls=broker_calls;check(resolve_remote(&profiles[0],"example.com",&resolved)&&broker_calls==calls);
  policy.revision++;mr_dns_policy_commit(&policy);check(resolve_remote(&profiles[0],"example.com",&resolved)&&broker_calls==calls+1);
  calls=broker_calls;check(resolve_remote(&profiles[0],"zero.example",&resolved));check(resolve_remote(&profiles[0],"zero.example",&resolved)&&broker_calls==calls+2);
  check(!resolve_remote(&profiles[0],"bad-id.example",&resolved));check(!resolve_remote(&profiles[0],"private.example",&resolved));check(!resolve_remote(&profiles[0],"switch.example",&resolved));mr_dns_policy_snapshot(&policy);
  started=GetTickCount64();check(!mr_dns_broker_exchange(&profiles[0],"slow.example",&policy,&ip,&ttl,started+80));check(GetTickCount64()-started<400);Sleep(170);
  check(legacy_queries==0);check(mr_dns_tunnel_start());
  report=tmpfile();assert(report);mr_dns_ready_fields(report);rewind(report);memset(text,0,sizeof(text));assert(fread(text,1,sizeof(text)-1,report)>0);fclose(report);check(strstr(text,"\"managed_dns\":true")!=NULL&&strstr(text,policy.key)==NULL);
  old_created=created;extra=open_tunnel(0,1,0);check(mr_dns_os_bytes(extra,&status,1,0,GetTickCount64()+2000,NULL)&&status==0);closesocket(extra);extra=open_tunnel(0,0,1);check(mr_dns_os_bytes(extra,&status,1,0,GetTickCount64()+2000,NULL)&&status==0);closesocket(extra);started=GetTickCount64();while(mr_dns_tunnel_connections&&GetTickCount64()-started<1000)Sleep(1);check(created==old_created&&!mr_dns_tunnel_connections);
  /* Production tunnel admission never consults the saturated payload count. */
  for(unsigned i=0;i<8;i++){sockets[i]=open_tunnel(i%4,0,0);check(mr_dns_os_bytes(sockets[i],&status,1,0,GetTickCount64()+2000,NULL)&&status==1);}
  check(mr_dns_tunnel_connections==8&&connection_count==96);extra=mr_dns_broker_connect(mr_dns_tunnel_port,GetTickCount64()+1000,NULL);check(extra!=INVALID_SOCKET&&!mr_dns_os_bytes(extra,&status,1,0,GetTickCount64()+500,NULL));closesocket(extra);
  for(unsigned i=0;i<8;i++){const char *message="owned TLS bytes";size_t length=strlen(message);check(mr_dns_os_bytes(sockets[i],(void*)message,length,1,GetTickCount64()+1000,NULL));check(mr_dns_os_bytes(sockets[i],echo,length,0,GetTickCount64()+1000,NULL)&&!memcmp(echo,message,length));closesocket(sockets[i]);}
  started=GetTickCount64();while(mr_dns_tunnel_connections&&GetTickCount64()-started<2000)Sleep(5);check(!mr_dns_tunnel_connections&&created==deleted);for(unsigned i=0;i<4;i++)check(egress_per_route[i]==2);
  for(unsigned i=0;i<65536;i++)assert(!port_routes[i]&&!udp_port_routes[i]&&!bound_ports[i]);check(legacy_queries==0);
  InterlockedExchange(&stopped,1);mr_dns_tunnel_stop();shutdown(broker_listener,SD_BOTH);closesocket(broker_listener);Sleep(100);
#ifdef _WIN32
  WSACleanup();
#endif
  printf("GATEWAY_DNS_BROKER_TRANSPORT_TEST_OK checks=%u loopback_only=true fake_lwip=true selected_profiles=4 simulated_payload_slots=96 reserved_slots=8 stale_revision_rejected=true os_network_changed=false\n",checks);return 0;
}
