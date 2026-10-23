/* RFC 1035/5452 gateway-bound DNS. Include after profile/virtual_host/public_ip.
 * Parser-only fixtures define MR_DNS_PARSER_ONLY. No system resolver fallback exists.
 */
#ifndef MULTIROUTE_GATEWAY_DNS_H
#define MULTIROUTE_GATEWAY_DNS_H
#include <stdint.h>
#include <stddef.h>
#include <string.h>
#include <ctype.h>
#define MR_DNS_MAX_PACKET 4096
#define MR_DNS_MAX_ANSWER 65535
#define MR_DNS_MAX_RR 64
static uint16_t mr_dns_u16(const unsigned char*p){return (uint16_t)(((uint16_t)p[0]<<8)|p[1]);}
static uint32_t mr_dns_u32(const unsigned char*p){return ((uint32_t)p[0]<<24)|((uint32_t)p[1]<<16)|((uint32_t)p[2]<<8)|p[3];}
static void mr_dns_put16(unsigned char*p,uint16_t v){p[0]=(unsigned char)(v>>8);p[1]=(unsigned char)v;}
static int mr_dns_public(uint32_t ip){
  unsigned a=ip>>24,b=(ip>>16)&255;
  return a>0&&a<224&&a!=10&&a!=127&&!(a==169&&b==254)&&!(a==172&&b>=16&&b<=31)&&!(a==192&&b==168)&&!(a==100&&b>=64&&b<=127)&&
    (ip&0xffffff00U)!=0xc0000000U&&(ip&0xffffff00U)!=0xc0000200U&&(ip&0xffffff00U)!=0xc0586300U&&
    !(a==198&&(b==18||b==19))&&(ip&0xffffff00U)!=0xc6336400U&&(ip&0xffffff00U)!=0xcb007100U;
}
static int mr_dns_host(const char*host,char*out){
  size_t n=strlen(host),i,label=0;if(!n||n>253)return 0;if(host[n-1]=='.')n--;if(!n)return 0;
  for(i=0;i<n;i++){unsigned char c=(unsigned char)host[i];if(c=='.'){if(!label||label>63)return 0;label=0;}else{if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='-'))return 0;label++;}out[i]=(char)tolower(c);}
  if(!label||label>63)return 0;
  out[n]=0;return 1;
}
static int mr_dns_name(const unsigned char*packet,size_t n,size_t*offset,char*out){
  size_t position=*offset,used=0,consumed=0;unsigned jumps=0;int jumped=0;
  for(;;){unsigned label;if(position>=n)return 0;label=packet[position++];
    if(!label){if(!jumped)*offset=position;else *offset=consumed;if(used&&out[used-1]=='.')used--;out[used]=0;return 1;}
    if((label&0xc0)==0xc0){size_t pointer;if(position>=n||++jumps>32)return 0;pointer=((size_t)(label&63)<<8)|packet[position++];if(pointer>=position-2)return 0;if(!jumped){consumed=position;jumped=1;}position=pointer;continue;}
    if((label&0xc0)||label>63||position+label>n||used+label+1>254)return 0;
    while(label--){unsigned char c=packet[position++];if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='-'))return 0;out[used++]=(char)tolower(c);}
    out[used++]='.';
  }
}
static size_t mr_dns_query(const char*host,uint16_t id,unsigned char*out,size_t cap){
  char normalized[254];size_t used=12,i=0;if(!mr_dns_host(host,normalized)||cap<12)return 0;memset(out,0,12);mr_dns_put16(out,id);out[2]=1;out[5]=1;out[11]=1;
  while(normalized[i]){size_t start=i,label;while(normalized[i]&&normalized[i]!='.')i++;label=i-start;if(used+label+1+16>cap)return 0;out[used++]=(unsigned char)label;memcpy(out+used,normalized+start,label);used+=label;if(normalized[i])i++;}
  out[used++]=0;mr_dns_put16(out+used,1);mr_dns_put16(out+used+2,1);used+=4;
  /* EDNS0, conservative 1232-byte UDP payload, no DNSSEC request. */
  memset(out+used,0,11);mr_dns_put16(out+used+1,41);mr_dns_put16(out+used+3,1232);used+=11;return used;
}
typedef struct mr_dns_rr{char owner[254],target[254];uint32_t ip,ttl;uint16_t type;}mr_dns_rr;
static int mr_dns_answer(const unsigned char*packet,size_t n,uint16_t id,const char*host,uint32_t*ip,uint32_t*ttl){
  uint16_t flags,answers,authority,additional;size_t offset=12;unsigned i,count;char question[254],current[254],seen[9][254];uint32_t minimum=UINT32_MAX;mr_dns_rr records[MR_DNS_MAX_RR];
  if(n<12||n>MR_DNS_MAX_ANSWER||mr_dns_u16(packet)!=id||!mr_dns_host(host,current))return 0;
  flags=mr_dns_u16(packet+2);if(!(flags&0x8000)||(flags&0x780f)||(flags&0x0200)||mr_dns_u16(packet+4)!=1)return 0;
  answers=mr_dns_u16(packet+6);authority=mr_dns_u16(packet+8);additional=mr_dns_u16(packet+10);count=(unsigned)answers+authority+additional;if(!answers||answers>MR_DNS_MAX_RR||count>128)return 0;
  if(!mr_dns_name(packet,n,&offset,question)||strcmp(question,current)||offset+4>n||mr_dns_u16(packet+offset)!=1||mr_dns_u16(packet+offset+2)!=1)return 0;
  offset+=4;memset(records,0,sizeof(records));
  for(i=0;i<count;i++){char owner[254];uint16_t type,klass,length;uint32_t lifetime;size_t end;
    if(!mr_dns_name(packet,n,&offset,owner)||offset+10>n)return 0;
    type=mr_dns_u16(packet+offset);klass=mr_dns_u16(packet+offset+2);lifetime=mr_dns_u32(packet+offset+4);length=mr_dns_u16(packet+offset+8);offset+=10;end=offset+length;if(end>n)return 0;
    if(i<answers){mr_dns_rr*r=&records[i];strcpy(r->owner,owner);r->ttl=lifetime;
      if(type==1&&klass==1){if(length!=4)return 0;r->ip=mr_dns_u32(packet+offset);if(!mr_dns_public(r->ip))return 0;r->type=1;}
      else if(type==5&&klass==1){size_t target_offset=offset;if(!mr_dns_name(packet,n,&target_offset,r->target)||target_offset!=end||!*r->target)return 0;r->type=5;}
    }offset=end;
  }
  if(offset!=n)return 0;
  for(i=0;i<9;i++){unsigned j;const char*alias=NULL;uint32_t address=0;strcpy(seen[i],current);
    for(j=0;j<answers;j++){mr_dns_rr*r=&records[j];if(strcmp(r->owner,current))continue;
      if(r->type==1){if(r->ttl<minimum)minimum=r->ttl;if(!address)address=r->ip;}
      if(r->type==5){if(alias&&strcmp(alias,r->target))return 0;alias=r->target;if(r->ttl<minimum)minimum=r->ttl;}
    }
    if(address){if(alias)return 0;*ip=address;*ttl=minimum>300?300:minimum;return 1;}
    if(!alias||i==8)return 0;
    for(j=0;j<=i;j++)if(!strcmp(seen[j],alias))return 0;
    strcpy(current,alias);
  }return 0;
}
/* A truncated UDP reply is still required to match our transaction/question
 * before it can request TCP. Its incomplete answer section is not trusted. */
static int mr_dns_truncated(const unsigned char*packet,size_t n,uint16_t id,const char*host){
  size_t offset=12;char question[254],normalized[254];uint16_t flags;
  if(n<12||n>MR_DNS_MAX_PACKET||mr_dns_u16(packet)!=id||!mr_dns_host(host,normalized))return 0;
  flags=mr_dns_u16(packet+2);if((flags&0x8200)!=0x8200||(flags&0x780f)||mr_dns_u16(packet+4)!=1)return 0;
  return mr_dns_name(packet,n,&offset,question)&&!strcmp(question,normalized)&&offset+4<=n&&mr_dns_u16(packet+offset)==1&&mr_dns_u16(packet+offset+2)==1;
}

static int gateway_dns_self_test(void) {
 unsigned char b[1024];uint32_t ip,ttl;size_t n=mr_dns_query("source.invalid",77,b,sizeof(b));
 if(n<23)return 1;
 n-=11;b[10]=b[11]=0;b[2]=0x81;b[3]=0x80;b[6]=0;b[7]=1;
 b[n++]=0xc0;b[n++]=12;mr_dns_put16(b+n,1);n+=2;mr_dns_put16(b+n,1);n+=2;
 b[n++]=0;b[n++]=0;b[n++]=0;b[n++]=60;mr_dns_put16(b+n,4);n+=2;
 b[n++]=93;b[n++]=184;b[n++]=216;b[n++]=34;
 if(!mr_dns_answer(b,n,77,"source.invalid",&ip,&ttl)||ip!=0x5db8d822||ttl!=60)return 2;
 if(mr_dns_answer(b,n,78,"source.invalid",&ip,&ttl)||mr_dns_answer(b,n,77,"other.invalid",&ip,&ttl)||mr_dns_answer(b,n-1,77,"source.invalid",&ip,&ttl))return 3;
 b[n-4]=10;if(mr_dns_answer(b,n,77,"source.invalid",&ip,&ttl))return 4;
 b[n-4]=93;b[2]|=2;if(!mr_dns_truncated(b,n,77,"source.invalid")||mr_dns_truncated(b,n,78,"source.invalid")||mr_dns_truncated(b,n,77,"other.invalid"))return 5;
 fprintf(stdout,"DNS_SELF_TEST_OK bounded parser, spoofed ID/question, truncated/private answers rejected; no network handles\n");return 0;
}

#ifndef MR_DNS_PARSER_ONLY
#ifdef MR_DNS_MANAGED_SUPPORT
static void mr_dns_broker_init(void);
static uint64_t mr_dns_current_revision(void);
#else
static uint64_t mr_dns_current_revision(void){return 0;}
#endif
typedef struct mr_dns_cache_entry{profile*route;char host[254];uint32_t ip;ULONGLONG until;uint64_t dns_revision;}mr_dns_cache_entry;
static mr_dns_cache_entry mr_dns_cache[64];
static CRITICAL_SECTION mr_dns_lock;
static unsigned mr_dns_cache_cursor;
static int mr_dns_reserve(uint16_t port,profile*route){
#ifdef _WIN32
  return InterlockedCompareExchangePointer((PVOID volatile*)&udp_port_routes[port],route,NULL)==NULL;
#else
  profile*expected=NULL;return __atomic_compare_exchange_n(&udp_port_routes[port],&expected,route,0,__ATOMIC_SEQ_CST,__ATOMIC_SEQ_CST);
#endif
}
static void mr_dns_unroute(uint16_t port,profile*route){
#ifdef _WIN32
  InterlockedCompareExchangePointer((PVOID volatile*)&udp_port_routes[port],NULL,route);
#else
  profile*expected=route;__atomic_compare_exchange_n(&udp_port_routes[port],&expected,NULL,0,__ATOMIC_SEQ_CST,__ATOMIC_SEQ_CST);
#endif
}
static void gateway_dns_init(void){InitializeCriticalSection(&mr_dns_lock);
#ifdef MR_DNS_MANAGED_SUPPORT
  mr_dns_broker_init();
#endif
}
static int mr_dns_cached_revision(profile*route,const char*host,ip_addr_t*out,uint64_t revision){unsigned i;ULONGLONG now=GetTickCount64();int found=0;EnterCriticalSection(&mr_dns_lock);for(i=0;i<64;i++)if(mr_dns_cache[i].route==route&&mr_dns_cache[i].dns_revision==revision&&mr_dns_cache[i].until>now&&!strcmp(mr_dns_cache[i].host,host)){ip_addr_set_ip4_u32(out,htonl(mr_dns_cache[i].ip));found=1;break;}LeaveCriticalSection(&mr_dns_lock);return found;}
static int mr_dns_cached(profile*route,const char*host,ip_addr_t*out){return mr_dns_cached_revision(route,host,out,mr_dns_current_revision());}
static void mr_dns_remember_revision(profile*route,const char*host,uint32_t ip,uint32_t ttl,uint64_t revision){mr_dns_cache_entry*e;if(!ttl)return;EnterCriticalSection(&mr_dns_lock);e=&mr_dns_cache[mr_dns_cache_cursor++%64];e->route=route;strcpy(e->host,host);e->ip=ip;e->dns_revision=revision;e->until=GetTickCount64()+(ULONGLONG)ttl*1000;LeaveCriticalSection(&mr_dns_lock);}
static void mr_dns_remember(profile*route,const char*host,uint32_t ip,uint32_t ttl){mr_dns_remember_revision(route,host,ip,ttl,mr_dns_current_revision());}
static int mr_dns_exchange(profile*route,const char*host,uint32_t resolver,uint32_t*ip,uint32_t*ttl,ULONGLONG overall_deadline){
  struct netconn*conn=NULL;struct netbuf*query=NULL;ip_addr_t local,remote;unsigned char request[512],response[MR_DNS_MAX_PACKET];uint16_t port=0,id;size_t length;err_t error=ERR_ARG;unsigned tries;ULONGLONG deadline;int found=0;
  id=(uint16_t)LWIP_RAND();length=mr_dns_query(host,id,request,sizeof(request));if(!length)return 0;
  int registered=0;
  conn=netconn_new(NETCONN_UDP);if(!conn)return 0;ip_addr_set_ip4_u32(&local,htonl(virtual_host));ip_addr_set_ip4_u32(&remote,htonl(resolver));
  if(netconn_bind_if(conn,netif_get_index(&route->netif))!=ERR_OK)goto done;
  for(tries=0;tries<128;tries++){
    port=(uint16_t)(20000+LWIP_RAND()%40000);if(!mr_dns_reserve(port,route))continue;registered=1;
    error=netconn_bind(conn,&local,port);if(error==ERR_OK)break;
    mr_dns_unroute(port,route);registered=0;if(error!=ERR_USE)goto done;
  }
  if(error!=ERR_OK)goto done;
  if(netconn_connect(conn,&remote,53)!=ERR_OK)goto done;
  netconn_set_recvtimeout(conn,50);query=netbuf_new();if(!query)goto done;{void*data=netbuf_alloc(query,(u16_t)length);if(!data)goto done;memcpy(data,request,length);}
  if(netconn_send(conn,query)!=ERR_OK)goto done;
  netbuf_delete(query);query=NULL;deadline=GetTickCount64()+2000;if(deadline>overall_deadline)deadline=overall_deadline;
  while(!cancelled()&&route->configured&&GetTickCount64()<deadline){struct netbuf*reply=NULL;u16_t size;
    error=netconn_recv(conn,&reply);if(error==ERR_TIMEOUT||error==ERR_WOULDBLOCK)continue;if(error!=ERR_OK||!reply)break;
    size=netbuf_len(reply);if(netbuf_fromport(reply)==53&&ip_addr_cmp(netbuf_fromaddr(reply),&remote)&&size<=sizeof(response)&&netbuf_copy(reply,response,size)==size){
      found=mr_dns_answer(response,size,id,host,ip,ttl);if(!found&&mr_dns_truncated(response,size,id,host))found=2;
    }
    netbuf_delete(reply);if(found)break;
  }
done:
  if(query)netbuf_delete(query);
  if(registered)mr_dns_unroute(port,route);
  if(conn)netconn_delete(conn);
  return found;
}
static int mr_dns_tcp_claim(uint16_t port,profile*route){
  /* Called only after TCP bind succeeds. A previous closed connection may have
   * left a demux entry; the still-bound PCB excludes another live port owner. */
#ifdef _WIN32
  PVOID previous=InterlockedCompareExchangePointer((PVOID volatile*)&port_routes[port],NULL,NULL);
  return InterlockedCompareExchangePointer((PVOID volatile*)&port_routes[port],route,previous)==previous;
#else
  profile*previous=__atomic_load_n(&port_routes[port],__ATOMIC_SEQ_CST);
  return __atomic_compare_exchange_n(&port_routes[port],&previous,route,0,__ATOMIC_SEQ_CST,__ATOMIC_SEQ_CST);
#endif
}
static void mr_dns_tcp_unroute(uint16_t port,profile*route){
#ifdef _WIN32
  InterlockedCompareExchangePointer((PVOID volatile*)&port_routes[port],NULL,route);
#else
  profile*expected=route;__atomic_compare_exchange_n(&port_routes[port],&expected,NULL,0,__ATOMIC_SEQ_CST,__ATOMIC_SEQ_CST);
#endif
}
static int mr_dns_tcp_connected(struct netconn*conn){
  int state;err_t error=netconn_err(conn);if(error!=ERR_OK&&error!=ERR_INPROGRESS)return -1;
  LOCK_TCPIP_CORE();state=conn->pcb.tcp?(int)conn->pcb.tcp->state:-1;UNLOCK_TCPIP_CORE();return state==ESTABLISHED?1:state<0?-1:0;
}
static int mr_dns_tcp_exchange(profile*route,const char*host,uint32_t resolver,uint32_t*ip,uint32_t*ttl,ULONGLONG overall_deadline){
  struct netconn*conn=NULL;unsigned char request[514],response[MR_DNS_MAX_PACKET+2];ip_addr_t local,remote;
  size_t length,written=0,used=0,expected=0;uint16_t port=0,id;unsigned tries;int found=0,registered=0;err_t error;ULONGLONG deadline=GetTickCount64()+2000;
  if(deadline>overall_deadline)deadline=overall_deadline;
  if(cancelled()||!route->configured||!route->mac_known||GetTickCount64()>=deadline)return 0;
  id=(uint16_t)LWIP_RAND();length=mr_dns_query(host,id,request+2,sizeof(request)-2);if(!length)return 0;mr_dns_put16(request,(uint16_t)length);length+=2;
  conn=netconn_new(NETCONN_TCP);if(!conn)return 0;
  ip_addr_set_ip4_u32(&local,htonl(virtual_host));ip_addr_set_ip4_u32(&remote,htonl(resolver));
  if(netconn_bind_if(conn,netif_get_index(&route->netif))!=ERR_OK)goto done;
  for(tries=0;tries<128;tries++){
    port=(uint16_t)(20000+LWIP_RAND()%40000);error=netconn_bind(conn,&local,port);if(error==ERR_OK)break;if(error!=ERR_USE)goto done;
  }
  if(tries==128||!mr_dns_tcp_claim(port,route))goto done;
  registered=1;
  netconn_set_nonblocking(conn,1);error=netconn_connect(conn,&remote,53);if(error!=ERR_OK&&error!=ERR_INPROGRESS)goto done;
  while(!cancelled()&&route->configured&&route->mac_known&&GetTickCount64()<deadline&&mr_dns_tcp_connected(conn)==0)Sleep(5);
  if(cancelled()||!route->configured||!route->mac_known||GetTickCount64()>=deadline||mr_dns_tcp_connected(conn)!=1)goto done;
  netconn_set_nonblocking(conn,0);netconn_set_recvtimeout(conn,50);netconn_set_sendtimeout(conn,50);
  while(written<length&&!cancelled()&&route->configured&&route->mac_known&&GetTickCount64()<deadline){
    size_t part=0;error=netconn_write_partly(conn,request+written,length-written,NETCONN_COPY,&part);written+=part;
    if(error!=ERR_OK&&error!=ERR_TIMEOUT&&error!=ERR_WOULDBLOCK)goto done;
    if(!part)Sleep(1);
  }
  if(written!=length)goto done;
  while(!cancelled()&&route->configured&&route->mac_known&&GetTickCount64()<deadline){
    struct netbuf*reply=NULL;size_t take;error=netconn_recv(conn,&reply);if(error==ERR_TIMEOUT||error==ERR_WOULDBLOCK)continue;if(error!=ERR_OK||!reply)break;
    if(cancelled()||!route->configured||!route->mac_known){netbuf_delete(reply);break;}
    take=netbuf_len(reply);if(take>sizeof(response)-used)take=sizeof(response)-used;
    if(netbuf_copy(reply,response+used,(u16_t)take)!=take){netbuf_delete(reply);goto done;}used+=take;netbuf_delete(reply);
    if(used>=2&&!expected){expected=mr_dns_u16(response);if(expected<12||expected>MR_DNS_MAX_PACKET)goto done;}
    if(expected&&used>=expected+2){found=mr_dns_answer(response+2,expected,id,host,ip,ttl);break;}
    if(used==sizeof(response))break;
  }
done:
  /* Release before deleting the bound PCB: a reused port cannot lose its new
   * owner's demux entry. Compare/exchange also preserves unexpected owners. */
  if(registered)mr_dns_tcp_unroute(port,route);
  if(conn)netconn_delete(conn);
  return found;
}
#ifdef MR_DNS_MANAGED_SUPPORT
#include "gateway_dns_broker.h"
#endif
static int resolve_remote(profile*route,const char*host,ip_addr_t*destination){
  char normalized[254];uint32_t numeric,ip,ttl;uint32_t resolvers[3];unsigned i;ULONGLONG deadline;
  if(!route||!route->configured||!route->mac_known||cancelled())return 0;
  if(parse_ip(host,&numeric)){if(!mr_dns_public(numeric))return 0;ip_addr_set_ip4_u32(destination,htonl(numeric));return 1;}
  if(!mr_dns_host(host,normalized))return 0;
#ifdef MR_DNS_MANAGED_SUPPORT
  {mr_dns_policy policy;
    if(mr_dns_policy_snapshot(&policy)){
      if(mr_dns_cached_revision(route,normalized,destination,policy.revision))return mr_dns_policy_current(&policy);
      deadline=GetTickCount64()+MR_DNS_TOTAL_MS;
      if(!mr_dns_broker_exchange(route,normalized,&policy,&ip,&ttl,deadline)||!mr_dns_policy_current(&policy)||!route->configured||!route->mac_known||cancelled())return 0;
      ip_addr_set_ip4_u32(destination,htonl(ip));mr_dns_remember_revision(route,normalized,ip,ttl,policy.revision);return 1;
    }
    if(managed_dns_required)return 0;
  }
#endif
  if(mr_dns_cached(route,normalized,destination))return 1;
  /* Router DNS first; public fallback queries still use the same gateway netif. */
  resolvers[0]=route->gateway_host;resolvers[1]=0x01010101U;resolvers[2]=0x08080808U;
  deadline=GetTickCount64()+8000;
  for(i=0;i<3&&!cancelled()&&route->configured&&GetTickCount64()<deadline;i++){
    int found=mr_dns_exchange(route,normalized,resolvers[i],&ip,&ttl,deadline);
    if(found!=1&&!cancelled()&&route->configured&&GetTickCount64()<deadline){
      char resolver_text[16];found=mr_dns_tcp_exchange(route,normalized,resolvers[i],&ip,&ttl,deadline);
      snprintf(resolver_text,sizeof(resolver_text),"%u.%u.%u.%u",(unsigned)(resolvers[i]>>24),(unsigned)((resolvers[i]>>16)&255),(unsigned)((resolvers[i]>>8)&255),(unsigned)(resolvers[i]&255));
      fprintf(stderr,"DNS_TCP_FALLBACK gateway=%s resolver=%s result=%s\n",route->gateway,resolver_text,found?"ok":"failed");fflush(stderr);
    }
    if(found==1){ip_addr_set_ip4_u32(destination,htonl(ip));mr_dns_remember(route,normalized,ip,ttl);return 1;}
  }
  return 0;
}
#endif
#endif
