/* Shared Windows/POSIX managed-DNS transport. Every OS socket below is loopback;
 * public resolver TCP uses lwIP and the explicitly authenticated profile. */
#ifndef MULTIROUTE_DNS_BROKER_H
#define MULTIROUTE_DNS_BROKER_H
#include "gateway_dns_broker_protocol.h"
static CRITICAL_SECTION mr_dns_policy_lock;
static mr_dns_policy mr_dns_active_policy;
static int managed_dns_required;
static SOCKET mr_dns_tunnel_listener=INVALID_SOCKET;
static unsigned short mr_dns_tunnel_port;
static volatile LONG mr_dns_tunnel_connections;
static void mr_dns_broker_init(void){InitializeCriticalSection(&mr_dns_policy_lock);}
static int mr_dns_policy_snapshot(mr_dns_policy *out){int active;EnterCriticalSection(&mr_dns_policy_lock);*out=mr_dns_active_policy;active=out->revision!=0;LeaveCriticalSection(&mr_dns_policy_lock);return active;}
static uint64_t mr_dns_current_revision(void){mr_dns_policy p;mr_dns_policy_snapshot(&p);return p.revision;}
static int mr_dns_policy_current(const mr_dns_policy *expected){mr_dns_policy p;return mr_dns_policy_snapshot(&p)&&mr_dns_same_policy(&p,expected);}
static int mr_dns_policy_permitted(const mr_dns_policy *next){mr_dns_policy old;mr_dns_policy_snapshot(&old);return next->revision>old.revision||(next->revision==old.revision&&mr_dns_same_policy(next,&old));}
static void mr_dns_policy_commit(const mr_dns_policy *next){EnterCriticalSection(&mr_dns_policy_lock);mr_dns_active_policy=*next;LeaveCriticalSection(&mr_dns_policy_lock);}
static void mr_dns_ready_fields(FILE *f){mr_dns_policy p;mr_dns_policy_snapshot(&p);fprintf(f,",\"managed_dns\":true,\"managed_dns_active\":%s,\"dns_tunnel_port\":%u,\"dns_revision\":%llu",p.revision?"true":"false",mr_dns_tunnel_port,(unsigned long long)p.revision);}
static int mr_dns_os_nonblocking(SOCKET s){
#ifdef _WIN32
  u_long mode=1;return ioctlsocket(s,FIONBIO,&mode)==0;
#else
  return set_nonblocking(s,1)==0;
#endif
}
static int mr_dns_os_pending(int error){
#ifdef _WIN32
  return error==WSAEWOULDBLOCK||error==WSAEINPROGRESS||error==WSAEALREADY||error==WSAEINTR;
#else
  return error==EAGAIN||error==EWOULDBLOCK||error==EINPROGRESS||error==EALREADY||error==EINTR;
#endif
}
static int mr_dns_route_live(profile *route){return !cancelled()&&(!route||(route->configured&&route->mac_known));}
static int mr_dns_os_wait(SOCKET s,int writing,ULONGLONG deadline,profile *route){
  while(mr_dns_route_live(route)&&GetTickCount64()<deadline){fd_set set;struct timeval wait;ULONGLONG now=GetTickCount64(),remaining;int result;
    if(now>=deadline){return 0;}
    remaining=deadline-now;
    if(remaining>50){remaining=50;}
    wait.tv_sec=0;wait.tv_usec=(long)remaining*1000;FD_ZERO(&set);FD_SET(s,&set);
#ifdef _WIN32
    result=select(0,writing?NULL:&set,writing?&set:NULL,NULL,&wait);
#else
    result=select(s+1,writing?NULL:&set,writing?&set:NULL,NULL,&wait);
#endif
    if(result>0){return 1;}
    if(result<0&&!mr_dns_os_pending(WSAGetLastError()))return 0;
  }return 0;
}
static int mr_dns_os_bytes(SOCKET s,void *data,size_t size,int writing,ULONGLONG deadline,profile *route){
  unsigned char *cursor=(unsigned char*)data;
  while(size&&mr_dns_route_live(route)&&GetTickCount64()<deadline){int n;
    if(!mr_dns_os_wait(s,writing,deadline,route))return 0;
    n=writing?send(s,(const char*)cursor,(int)size,0):recv(s,(char*)cursor,(int)size,0);
    if(n>0){cursor+=n;size-=(size_t)n;continue;}if(!n||!mr_dns_os_pending(WSAGetLastError()))return 0;
  }return size==0;
}
static SOCKET mr_dns_broker_connect(unsigned short port,ULONGLONG deadline,profile *route){
  SOCKET s=socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);struct sockaddr_in address;int error=0;
#ifdef _WIN32
  int size=sizeof(error);
#else
  socklen_t size=sizeof(error);
#endif
  if(s==INVALID_SOCKET)return INVALID_SOCKET;
  memset(&address,0,sizeof(address));address.sin_family=AF_INET;address.sin_addr.s_addr=htonl(INADDR_LOOPBACK);address.sin_port=htons(port);
  if(!mr_dns_os_nonblocking(s))goto fail;
  if(connect(s,(struct sockaddr*)&address,sizeof(address))&& !mr_dns_os_pending(WSAGetLastError()))goto fail;
  if(!mr_dns_os_wait(s,1,deadline,route)||getsockopt(s,SOL_SOCKET,SO_ERROR,(char*)&error,&size)||error)goto fail;
  return s;
fail:closesocket(s);return INVALID_SOCKET;
}
static int mr_dns_broker_exchange(profile *route,const char *host,const mr_dns_policy *policy,uint32_t *ip,uint32_t *ttl,ULONGLONG deadline){
  unsigned char query[512],request[MR_DNS_BROKER_HEADER+512],header[3],*answer=NULL;uint16_t id;size_t length,wire_length;SOCKET s=INVALID_SOCKET;int found=0;ULONGLONG now=GetTickCount64();
  if(now>=deadline||!mr_dns_route_live(route)||!mr_dns_policy_current(policy))return 0;
  id=(uint16_t)LWIP_RAND();length=mr_dns_query(host,id,query,sizeof(query));
  wire_length=mr_dns_broker_encode(request,sizeof(request),policy,route->gateway,(unsigned)(deadline-now),query,length);if(!wire_length)return 0;
  s=mr_dns_broker_connect(policy->broker_port,deadline,route);if(s==INVALID_SOCKET)goto done;
  if(!mr_dns_os_bytes(s,request,wire_length,1,deadline,route)||!mr_dns_os_bytes(s,header,3,0,deadline,route))goto done;
  length=mr_dns_u16(header+1);
  if(header[0]!=1){if(header[0]==0&&length<=MR_DNS_MAX_ERROR){unsigned char ignored[MR_DNS_MAX_ERROR];mr_dns_os_bytes(s,ignored,length,0,deadline,route);}goto done;}
  if(length<12||length>MR_DNS_MAX_ANSWER)goto done;
  answer=(unsigned char*)malloc(length);if(!answer)goto done;
  if(!mr_dns_os_bytes(s,answer,length,0,deadline,route)||!mr_dns_route_live(route)||!mr_dns_policy_current(policy))goto done;
  found=mr_dns_answer(answer,length,id,host,ip,ttl);
done:
  if(answer){free(answer);}
  if(s!=INVALID_SOCKET){shutdown(s,SD_BOTH);closesocket(s);}
  if(!found){fprintf(stderr,"DNS_BROKER_FAILURE gateway=%s dns_revision=%llu no_fallback=true\n",route->gateway,(unsigned long long)policy->revision);fflush(stderr);}return found;
}
typedef struct mr_dns_tunnel_client{SOCKET local;profile *route;struct netconn *conn;uint16_t port;}mr_dns_tunnel_client;
static int mr_dns_tunnel_error(SOCKET s,const char *message,ULONGLONG deadline){unsigned char response[3+MR_DNS_MAX_ERROR];size_t n=strlen(message);if(n>MR_DNS_MAX_ERROR)n=MR_DNS_MAX_ERROR;response[0]=0;mr_dns_put16(response+1,(uint16_t)n);memcpy(response+3,message,n);return mr_dns_os_bytes(s,response,n+3,1,deadline,NULL);}
static profile *mr_dns_tunnel_route(const char *gateway){int i;for(i=0;i<profile_count;i++)if(profiles[i].configured&&profiles[i].mac_known&&!strcmp(profiles[i].gateway,gateway))return &profiles[i];return NULL;}
static int mr_dns_tunnel_open(mr_dns_tunnel_client *c,const mr_dns_tunnel_request *request,ULONGLONG deadline){
  ip_addr_t local,remote;err_t error=ERR_ARG;unsigned tries;
  c->conn=netconn_new(NETCONN_TCP);if(!c->conn)return 0;
  ip_addr_set_ip4_u32(&local,htonl(virtual_host));ip_addr_set_ip4_u32(&remote,htonl(request->destination));
  if(netconn_bind_if(c->conn,netif_get_index(&c->route->netif))!=ERR_OK)return 0;
  for(tries=0;tries<128&&mr_dns_route_live(c->route)&&GetTickCount64()<deadline;tries++){
    uint16_t port=(uint16_t)(20000+LWIP_RAND()%40000);error=netconn_bind(c->conn,&local,port);
    if(error==ERR_OK){if(!mr_dns_tcp_claim(port,c->route))return 0;c->port=port;break;}if(error!=ERR_USE)return 0;
  }
  if(error!=ERR_OK||!c->port)return 0;
  netconn_set_nonblocking(c->conn,1);error=netconn_connect(c->conn,&remote,request->port);if(error!=ERR_OK&&error!=ERR_INPROGRESS)return 0;
  while(mr_dns_route_live(c->route)&&GetTickCount64()<deadline&&mr_dns_tcp_connected(c->conn)==0)Sleep(5);
  if(!mr_dns_route_live(c->route)||GetTickCount64()>=deadline||mr_dns_tcp_connected(c->conn)!=1)return 0;
  netconn_set_nonblocking(c->conn,0);netconn_set_recvtimeout(c->conn,5);netconn_set_sendtimeout(c->conn,50);return 1;
}
static void mr_dns_tunnel_pump(mr_dns_tunnel_client *c,ULONGLONG deadline){
  while(mr_dns_route_live(c->route)&&GetTickCount64()<deadline){unsigned char buffer[8192];int count=recv(c->local,(char*)buffer,sizeof(buffer),0);err_t error;struct netbuf *reply=NULL;
    if(count>0){size_t used=0;while(used<(size_t)count&&mr_dns_route_live(c->route)&&GetTickCount64()<deadline){size_t written=0;error=netconn_write_partly(c->conn,buffer+used,(size_t)count-used,NETCONN_COPY,&written);used+=written;if(error!=ERR_OK&&error!=ERR_TIMEOUT&&error!=ERR_WOULDBLOCK)return;if(!written)Sleep(1);}if(used!=(size_t)count)return;}
    else if(count==0||!mr_dns_os_pending(WSAGetLastError()))return;
    error=netconn_recv(c->conn,&reply);
    if(error==ERR_TIMEOUT||error==ERR_WOULDBLOCK){if(count<0)Sleep(1);continue;}
    if(error!=ERR_OK||!reply)return;
    do{void *payload;u16_t size;netbuf_data(reply,&payload,&size);if(!mr_dns_os_bytes(c->local,payload,size,1,deadline,c->route)){netbuf_delete(reply);return;}}while(netbuf_next(reply)>=0);
    netbuf_delete(reply);
  }
}
#ifdef _WIN32
#define MR_DNS_THREAD_RESULT DWORD WINAPI
#define MR_DNS_THREAD_ARGUMENT LPVOID
#else
#define MR_DNS_THREAD_RESULT void *
#define MR_DNS_THREAD_ARGUMENT void *
#endif
static MR_DNS_THREAD_RESULT mr_dns_tunnel_serve(MR_DNS_THREAD_ARGUMENT pointer){
  mr_dns_tunnel_client *c=(mr_dns_tunnel_client*)pointer;unsigned char input[MR_DNS_TUNNEL_HEADER],success=1;mr_dns_policy policy;mr_dns_tunnel_request request;ULONGLONG deadline=GetTickCount64()+MR_DNS_TOTAL_MS;
  if(!mr_dns_os_nonblocking(c->local)||!mr_dns_os_bytes(c->local,input,sizeof(input),0,deadline,NULL))goto done;
  if(!mr_dns_policy_snapshot(&policy)||!mr_dns_tunnel_decode(input,sizeof(input),&policy,&request)){mr_dns_tunnel_error(c->local,"DNS tunnel authentication or request failed",deadline);goto done;}
  c->route=mr_dns_tunnel_route(request.gateway);
  if(!c->route){mr_dns_tunnel_error(c->local,"Selected DNS gateway is unavailable",deadline);goto done;}
  if(!mr_dns_tunnel_open(c,&request,deadline)){mr_dns_tunnel_error(c->local,"Selected DNS gateway TCP connection failed",deadline);goto done;}
  if(mr_dns_os_bytes(c->local,&success,1,1,deadline,c->route))mr_dns_tunnel_pump(c,deadline);
done:
  if(c->port){mr_dns_tcp_unroute(c->port,c->route);}
  if(c->conn)netconn_delete(c->conn);
  shutdown(c->local,SD_BOTH);closesocket(c->local);free(c);InterlockedDecrement(&mr_dns_tunnel_connections);return 0;
}
static MR_DNS_THREAD_RESULT mr_dns_tunnel_accept(MR_DNS_THREAD_ARGUMENT ignored){
  (void)ignored;
  while(!cancelled()){SOCKET s,listener=mr_dns_tunnel_listener;struct sockaddr_in peer;fd_set set;struct timeval wait={0,50000};mr_dns_tunnel_client *c;
#ifdef _WIN32
    int peer_size=sizeof(peer);HANDLE thread;
#else
    socklen_t peer_size=sizeof(peer);thread_handle thread;
#endif
    if(listener==INVALID_SOCKET){break;}
    FD_ZERO(&set);FD_SET(listener,&set);
#ifdef _WIN32
    if(select(0,&set,NULL,NULL,&wait)<=0)continue;
#else
    if(select(listener+1,&set,NULL,NULL,&wait)<=0)continue;
#endif
    s=accept(listener,(struct sockaddr*)&peer,&peer_size);if(s==INVALID_SOCKET)continue;
    if(ntohl(peer.sin_addr.s_addr)!=INADDR_LOOPBACK){closesocket(s);continue;}
    if(InterlockedIncrement(&mr_dns_tunnel_connections)>MR_DNS_TUNNEL_LIMIT){InterlockedDecrement(&mr_dns_tunnel_connections);closesocket(s);continue;}
    c=(mr_dns_tunnel_client*)calloc(1,sizeof(*c));if(!c){InterlockedDecrement(&mr_dns_tunnel_connections);closesocket(s);continue;}c->local=s;
#ifdef _WIN32
    thread=CreateThread(NULL,0,mr_dns_tunnel_serve,c,0,NULL);if(thread)CloseHandle(thread);
#else
    thread=spawn_thread(mr_dns_tunnel_serve,c);if(thread)detach_thread(thread);
#endif
    if(!thread){free(c);closesocket(s);InterlockedDecrement(&mr_dns_tunnel_connections);}
  }return 0;
}
static int mr_dns_tunnel_start(void){
  SOCKET s;struct sockaddr_in address;
#ifdef _WIN32
  int size=sizeof(address);HANDLE thread;
#else
  socklen_t size=sizeof(address);thread_handle thread;
#endif
  s=socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);if(s==INVALID_SOCKET)return 0;
  memset(&address,0,sizeof(address));address.sin_family=AF_INET;address.sin_addr.s_addr=htonl(INADDR_LOOPBACK);
  if(bind(s,(struct sockaddr*)&address,sizeof(address))||listen(s,MR_DNS_TUNNEL_LIMIT)||getsockname(s,(struct sockaddr*)&address,&size)){closesocket(s);return 0;}
  mr_dns_tunnel_listener=s;mr_dns_tunnel_port=ntohs(address.sin_port);
#ifdef _WIN32
  thread=CreateThread(NULL,0,mr_dns_tunnel_accept,NULL,0,NULL);if(thread)CloseHandle(thread);
#else
  thread=spawn_thread(mr_dns_tunnel_accept,NULL);if(thread)detach_thread(thread);
#endif
  if(thread){return 1;}
  mr_dns_tunnel_listener=INVALID_SOCKET;mr_dns_tunnel_port=0;closesocket(s);return 0;
}
static void mr_dns_tunnel_stop(void){SOCKET s=mr_dns_tunnel_listener;mr_dns_tunnel_listener=INVALID_SOCKET;if(s!=INVALID_SOCKET)closesocket(s);}
#undef MR_DNS_THREAD_RESULT
#undef MR_DNS_THREAD_ARGUMENT
#endif
