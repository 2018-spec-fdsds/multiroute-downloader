/* Offline transport regression. All netconn, clocks and packet buffers below
 * are owned fakes; this fixture opens no sockets/capture and changes no routes. */
#define _CRT_SECURE_NO_WARNINGS
#include <assert.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
typedef uint64_t ULONGLONG;
typedef uint16_t u16_t;
typedef int err_t;
typedef int CRITICAL_SECTION;
typedef void *PVOID;
typedef struct { uint32_t addr; } ip_addr_t;
struct netif { unsigned index; };
typedef struct profile { char gateway[16];uint32_t gateway_host;int configured,mac_known;struct netif netif; } profile;
struct tcp_pcb { int state; };
struct netconn { int type;unsigned index;uint16_t port;ip_addr_t remote;struct tcp_pcb tcp;struct {struct tcp_pcb *tcp;} pcb;unsigned char request[514],reply[4200];size_t request_used,reply_used,reply_sent; };
struct netbuf { unsigned char data[4200];u16_t len;ip_addr_t from;uint16_t fromport; };
#define ERR_OK 0
#define ERR_USE -8
#define ERR_INPROGRESS -5
#define ERR_TIMEOUT -3
#define ERR_WOULDBLOCK -7
#define ERR_CLSD -15
#define ERR_RST -14
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
static uint32_t mock_htonl(uint32_t n){return ((n&255U)<<24)|((n&65280U)<<8)|((n>>8)&65280U)|(n>>24);}
#ifdef htonl
#undef htonl
#endif
#define htonl(n) mock_htonl(n)
static profile routes[4],other;
static profile *volatile port_routes[65536],*volatile udp_port_routes[65536];
static uint32_t virtual_host=0xc0a80146U;
static struct {
  ULONGLONG now,cancel_at,disable_at;
  unsigned rng,created,deleted,allocated,freed,tcp_connections,udp_queries,checks,bind_calls,bind_failures;
  int udp_mode,tcp_mode,stall_connect,stall_recv,partial_eof,swap_owner;
  size_t read_chunk,write_chunk;
  uint32_t tcp_success_resolver;
  uint16_t last_port;
  profile *expected;
} fixture;
static void check(int yes){fixture.checks++;assert(yes);}
static ULONGLONG GetTickCount64(void){if(fixture.disable_at&&fixture.now>=fixture.disable_at)fixture.expected->configured=0;return fixture.now;}
static int cancelled(void){return fixture.cancel_at&&fixture.now>=fixture.cancel_at;}
static void Sleep(unsigned n){fixture.now+=n;}
static unsigned mock_random(void){return ++fixture.rng;}
#define LWIP_RAND() mock_random()
static void InitializeCriticalSection(CRITICAL_SECTION *s){*s=0;}
static void EnterCriticalSection(CRITICAL_SECTION *s){(void)s;}
static void LeaveCriticalSection(CRITICAL_SECTION *s){(void)s;}
#ifdef _WIN32
static PVOID InterlockedCompareExchangePointer(PVOID volatile *target,PVOID desired,PVOID expected){PVOID previous=*target;if(previous==expected)*target=desired;return previous;}
#endif
static int parse_ip(const char *text,uint32_t *result){unsigned a,b,c,d;char extra;if(sscanf(text,"%u.%u.%u.%u%c",&a,&b,&c,&d,&extra)!=4||a>255||b>255||c>255||d>255)return 0;*result=(a<<24)|(b<<16)|(c<<8)|d;return 1;}
static struct netconn *netconn_new(int type){struct netconn*c=(struct netconn*)calloc(1,sizeof(*c));assert(c);c->type=type;c->pcb.tcp=&c->tcp;fixture.created++;return c;}
static err_t netconn_bind_if(struct netconn*c,unsigned index){check(index==fixture.expected->netif.index);c->index=index;return ERR_OK;}
static err_t netconn_bind(struct netconn*c,const ip_addr_t*local,uint16_t port){check(local->addr==htonl(virtual_host));fixture.bind_calls++;if(fixture.bind_failures){fixture.bind_failures--;return ERR_USE;}c->port=port;fixture.last_port=port;return ERR_OK;}
static err_t netconn_connect(struct netconn*c,const ip_addr_t*remote,uint16_t port){check(c->index==fixture.expected->netif.index&&port==53);c->remote=*remote;if(c->type==NETCONN_TCP){fixture.tcp_connections++;if(fixture.tcp_success_resolver&&remote->addr!=htonl(fixture.tcp_success_resolver))return ERR_RST;c->tcp.state=fixture.stall_connect?0:ESTABLISHED;}return ERR_OK;}
static void netconn_set_recvtimeout(struct netconn*c,int n){(void)c;(void)n;}
static void netconn_set_sendtimeout(struct netconn*c,int n){(void)c;(void)n;}
static void netconn_set_nonblocking(struct netconn*c,int n){(void)c;(void)n;}
static err_t netconn_err(struct netconn*c){(void)c;return ERR_OK;}
static struct netbuf *netbuf_new(void){struct netbuf*b=(struct netbuf*)calloc(1,sizeof(*b));assert(b);fixture.allocated++;return b;}
static void *netbuf_alloc(struct netbuf*b,u16_t n){assert(n<=sizeof(b->data));b->len=n;return b->data;}
static void netbuf_delete(struct netbuf*b){fixture.freed++;free(b);}
static void mock_put16(unsigned char*p,unsigned n){p[0]=(unsigned char)(n>>8);p[1]=(unsigned char)n;}
static size_t mock_answer(unsigned char*out,const unsigned char*query,size_t length,int mode){
  size_t n=length-11;assert(length>=23);memcpy(out,query,n);out[2]=0x81;out[3]=0x80;out[6]=0;out[7]=1;out[10]=out[11]=0;
  if(mode==2){out[2]|=2;out[6]=out[7]=0;return n;}
  out[n++]=0xc0;out[n++]=12;mock_put16(out+n,1);n+=2;mock_put16(out+n,1);n+=2;
  out[n++]=0;out[n++]=0;out[n++]=0;out[n++]=60;mock_put16(out+n,4);n+=2;
  out[n++]=(unsigned char)(mode==4?10:188);out[n++]=93;out[n++]=16;out[n++]=211;
  if(mode==3)out[1]^=1;
  return n;
}
static err_t netconn_send(struct netconn*c,struct netbuf*b){assert(c->type==NETCONN_UDP);fixture.udp_queries++;memcpy(c->request,b->data,b->len);c->request_used=b->len;return ERR_OK;}
static err_t netconn_write_partly(struct netconn*c,const void*data,size_t n,int flags,size_t*written){size_t take=n;(void)flags;if(take>fixture.write_chunk)take=fixture.write_chunk;assert(c->request_used+take<=sizeof(c->request));memcpy(c->request+c->request_used,data,take);c->request_used+=take;*written=take;fixture.now++;return ERR_OK;}
static u16_t netbuf_len(struct netbuf*b){return b->len;}
static uint16_t netbuf_fromport(struct netbuf*b){return b->fromport;}
static ip_addr_t *netbuf_fromaddr(struct netbuf*b){return &b->from;}
static u16_t netbuf_copy(struct netbuf*b,void*out,u16_t n){if(n>b->len)return 0;memcpy(out,b->data,n);return n;}
static err_t netconn_recv(struct netconn*c,struct netbuf**out){
  size_t take;*out=NULL;
  if(c->type==NETCONN_UDP){
    if(!fixture.udp_mode||c->reply_sent){fixture.now+=50;return ERR_TIMEOUT;}
    *out=netbuf_new();(*out)->len=(u16_t)mock_answer((*out)->data,c->request,c->request_used,fixture.udp_mode);(*out)->from=c->remote;(*out)->fromport=53;c->reply_sent=1;fixture.now++;return ERR_OK;
  }
  if(fixture.stall_recv){fixture.now+=50;return ERR_TIMEOUT;}
  if(!c->reply_used){
    assert(c->request_used>=2&&c->request_used==(size_t)((c->request[0]<<8)|c->request[1])+2);
    c->reply_used=mock_answer(c->reply+2,c->request+2,c->request_used-2,fixture.tcp_mode);mock_put16(c->reply,(unsigned)c->reply_used);c->reply_used+=2;
    if(fixture.tcp_mode==5)mock_put16(c->reply,4097);
    if(fixture.tcp_mode==6)mock_put16(c->reply,11);
    if(fixture.partial_eof)c->reply_used=1;
  }
  if(c->reply_sent==c->reply_used)return ERR_CLSD;
  take=c->reply_used-c->reply_sent;if(take>fixture.read_chunk)take=fixture.read_chunk;
  *out=netbuf_new();(*out)->len=(u16_t)take;memcpy((*out)->data,c->reply+c->reply_sent,take);c->reply_sent+=take;fixture.now++;
  if(fixture.swap_owner)port_routes[c->port]=&other;
  return ERR_OK;
}
static void netconn_delete(struct netconn*c){fixture.deleted++;free(c);}
#include "../gateway_dns.h"
static void reset(profile*route){unsigned checks=fixture.checks;memset(&fixture,0,sizeof(fixture));fixture.checks=checks;fixture.now=1000;fixture.rng=40;fixture.expected=route;fixture.write_chunk=514;fixture.read_chunk=4200;route->configured=route->mac_known=1;memset((void*)port_routes,0,sizeof(port_routes));memset((void*)udp_port_routes,0,sizeof(udp_port_routes));memset(mr_dns_cache,0,sizeof(mr_dns_cache));}
static void clean(void){check(fixture.created==fixture.deleted&&fixture.allocated==fixture.freed);for(unsigned i=0;i<65536;i++)assert(udp_port_routes[i]==NULL);}
int main(void){
  unsigned char query[512],packet[1024];uint32_t ip=0,ttl=0;size_t n,length;ip_addr_t result;ULONGLONG start;
  for(unsigned i=0;i<4;i++){snprintf(routes[i].gateway,sizeof(routes[i].gateway),"192.168.1.%u",i+1);routes[i].gateway_host=0xc0a80101U+i;routes[i].netif.index=i+1;}gateway_dns_init();check(gateway_dns_self_test()==0);
  reset(&routes[0]);fixture.udp_mode=1;check(resolve_remote(&routes[0],"speedtest.selectel.ru",&result));check(fixture.tcp_connections==0&&result.addr==htonl(0xbc5d10d3U));clean();
  for(unsigned i=0;i<4;i++){reset(&routes[i]);check(resolve_remote(&routes[i],"speedtest.selectel.ru",&result));check(fixture.tcp_connections==1&&fixture.udp_queries==1&&result.addr==htonl(0xbc5d10d3U));check(port_routes[fixture.last_port]==NULL);clean();}
  reset(&routes[0]);fixture.tcp_success_resolver=0x01010101U;check(resolve_remote(&routes[0],"speedtest.selectel.ru",&result));check(fixture.tcp_connections==2&&fixture.udp_queries==2);clean();
  reset(&routes[0]);fixture.udp_mode=2;start=fixture.now;check(resolve_remote(&routes[0],"speedtest.selectel.ru",&result));check(fixture.now-start<1000);clean();
  length=mr_dns_query("speedtest.selectel.ru",77,query,sizeof(query));n=mock_answer(packet,query,length,2);check(mr_dns_truncated(packet,n,77,"speedtest.selectel.ru"));check(!mr_dns_truncated(packet,n,78,"speedtest.selectel.ru"));check(!mr_dns_truncated(packet,n,77,"other.invalid"));packet[n-1]=2;check(!mr_dns_truncated(packet,n,77,"speedtest.selectel.ru"));
  reset(&routes[0]);fixture.read_chunk=1;fixture.write_chunk=3;check(mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(ip==0xbc5d10d3U&&ttl==60);clean();
  for(int mode=3;mode<=6;mode++){reset(&routes[0]);fixture.tcp_mode=mode;check(!mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(port_routes[fixture.last_port]==NULL);clean();}
  reset(&routes[0]);fixture.partial_eof=1;check(!mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));clean();
  reset(&routes[0]);fixture.stall_connect=1;start=fixture.now;check(!resolve_remote(&routes[0],"speedtest.selectel.ru",&result));check(fixture.now-start<=8050&&fixture.now-start>=8000);clean();
  reset(&routes[0]);fixture.stall_recv=1;fixture.cancel_at=fixture.now+100;check(!mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(fixture.now<=fixture.cancel_at+50);clean();
  reset(&routes[0]);fixture.stall_recv=1;fixture.disable_at=fixture.now+100;check(!mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(!routes[0].configured);clean();
  reset(&routes[0]);fixture.bind_failures=2;check(mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(fixture.bind_calls==3);clean();
  reset(&routes[0]);port_routes[20042]=&other;check(mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(fixture.last_port==20042&&port_routes[20042]==NULL);clean();
  reset(&routes[0]);fixture.swap_owner=1;check(mr_dns_tcp_exchange(&routes[0],"speedtest.selectel.ru",0x01010101U,&ip,&ttl,fixture.now+2000));check(port_routes[fixture.last_port]==&other);clean();
  reset(&routes[0]);fixture.cancel_at=fixture.now+100;check(!resolve_remote(&routes[0],"speedtest.selectel.ru",&result));check(fixture.tcp_connections==0);clean();
  reset(&routes[0]);fixture.disable_at=fixture.now+100;check(!resolve_remote(&routes[0],"speedtest.selectel.ru",&result));check(fixture.tcp_connections==0);clean();
  printf("GATEWAY_DNS_TRANSPORT_TEST_OK checks=%u capture=false os_network_changed=false\n",fixture.checks);return 0;
}
