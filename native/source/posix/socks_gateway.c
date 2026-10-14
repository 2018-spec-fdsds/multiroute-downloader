/* SOCKS5 extension: one loopback listener per application gateway.
 * RFC 1928 CONNECT and UDP ASSOCIATE; no OS network configuration/fallback.
 * Included in gateway_native.c so it shares that file's verified Ethernet stack.
 */
#define SOCKS_MAX_DATAGRAM 65507
#define SOCKS_MAX_PAYLOAD (SOCKS_MAX_DATAGRAM-10)
typedef struct socks_address {
  unsigned char type;
  uint32_t ip;
  char domain[256];
  unsigned short port;
  size_t bytes;
} socks_address;
static int socks_domain_valid(const unsigned char *p,size_t n) {
  size_t i;if(!n||n>255)return 0;
  for(i=0;i<n;i++)if(!((p[i]>='a'&&p[i]<='z')||(p[i]>='A'&&p[i]<='Z')||(p[i]>='0'&&p[i]<='9')||p[i]=='-'||p[i]=='.'))return 0;
  return 1;
}
/* Parses ATYP + DST.ADDR + DST.PORT without DNS or network side effects. */
static int socks_parse_address(const unsigned char *p,size_t n,socks_address *a) {
  size_t index;memset(a,0,sizeof(*a));if(!n)return 1;a->type=p[0];
  if(a->type==1){if(n<7)return 1;a->ip=get32(p+1);index=5;}
  else if(a->type==3){if(n<2)return 1;index=(size_t)p[1]+2;if(n<index+2||!socks_domain_valid(p+2,p[1]))return 1;memcpy(a->domain,p+2,p[1]);a->domain[p[1]]=0;}
  else return 8;
  a->port=get16(p+index);a->bytes=index+2;return 0;
}
static int socks_parse_datagram(const unsigned char *p,size_t n,socks_address *a,size_t *offset) {
  int error;if(n<4||p[0]||p[1]||p[2])return 1;
  error=socks_parse_address(p+3,n-3,a);if(error)return error;
  *offset=a->bytes+3;if(!a->port||n-*offset>SOCKS_MAX_PAYLOAD)return 1;return 0;
}
static int socks_recv_exact(SOCKET s,unsigned char *out,size_t n) {
  size_t done=0;ULONGLONG until=GetTickCount64()+5000;
  while(done<n&&!cancelled()&&GetTickCount64()<until){int got=recv(s,(char*)out+done,(int)(n-done),0);if(got<=0)return 0;done+=(size_t)got;}return done==n;
}
static int socks_reply(SOCKET s,unsigned char error,uint32_t ip,unsigned short port) {
  unsigned char response[10]={5,0,0,1};response[1]=error;put32(response+4,ip);put16(response+8,port);return socket_send_all(s,response,sizeof(response));
}
static int socks_read_address(SOCKET s,unsigned char type,socks_address *address) {
  unsigned char buffer[262];size_t size;buffer[0]=type;
  if(type==1)size=7;
  else if(type==3){if(!socks_recv_exact(s,buffer+1,1))return 1;size=(size_t)buffer[1]+4;if(size<=4)return 1;}
  else return 8;
  if(!socks_recv_exact(s,buffer+(type==3?2:1),size-(type==3?2:1)))return 1;
  return socks_parse_address(buffer,size,address);
}
static int socks_resolve(profile *route,const socks_address *a,ip_addr_t *ip) {
  if(a->type==1){if(!public_ip(a->ip))return 0;ip_addr_set_ip4_u32(ip,htonl(a->ip));return 1;}
  return a->type==3&&resolve_remote(route,a->domain,ip);
}
static unsigned short socks_next_port(void) {
  for(;;){LONG port=InterlockedIncrement(&next_port);if(port>60000){InterlockedExchange(&next_port,20000);continue;}return (unsigned short)port;}
}
static int socks_tcp_connect(client *c,const socks_address *address) {
  ip_addr_t remote,local;err_t error;ULONGLONG until;
  if(!c->route->configured||!c->route->mac_known)return 3;
  if(!address->port||!socks_resolve(c->route,address,&remote))return 4;
  c->conn=netconn_new(NETCONN_TCP);if(!c->conn)return 1;
  ip_addr_set_ip4_u32(&local,htonl(virtual_host));
  for(;;){c->port=socks_next_port();error=netconn_bind(c->conn,&local,c->port);if(error==ERR_OK)break;if(error!=ERR_USE)return 1;}
  port_routes[c->port]=c->route;
  if(netconn_bind_if(c->conn,netif_get_index(&c->route->netif))!=ERR_OK)return 3;
  netconn_set_nonblocking(c->conn,1);error=netconn_connect(c->conn,&remote,address->port);
  if(error!=ERR_OK&&error!=ERR_INPROGRESS)return error==ERR_RST?5:3;
  until=GetTickCount64()+5000;
  while(!cancelled()&&remote_connected(c->conn)==0&&GetTickCount64()<until)Sleep(5);
  if(cancelled()||remote_connected(c->conn)!=1)return 4;
  netconn_set_nonblocking(c->conn,0);netconn_set_recvtimeout(c->conn,10);netconn_set_sendtimeout(c->conn,1000);
  return 0;
}
static void socks_tcp_pump(client *c) {
  unsigned char pending[65536];size_t length=0,offset=0;int local_closed=0;u_long nonblocking=1;ULONGLONG deadline=GetTickCount64()+600000;
  set_nonblocking(c->local,(int)nonblocking);
  while(!cancelled()&&GetTickCount64()<deadline){
    int received=0;err_t error;
    if(!length&&!local_closed){
      int n=recv(c->local,(char*)pending,sizeof(pending),0);
      if(n>0){length=(size_t)n;offset=0;deadline=GetTickCount64()+600000;}
      else if(!n){local_closed=1;netconn_shutdown(c->conn,0,1);}
      else if(WSAGetLastError()!=WSAEWOULDBLOCK)return;
    }
    if(length){size_t written=0;error=netconn_write_partly(c->conn,pending+offset,length-offset,NETCONN_COPY,&written);
      if(error!=ERR_OK&&error!=ERR_TIMEOUT&&error!=ERR_WOULDBLOCK)return;
      if(written){offset+=written;deadline=GetTickCount64()+600000;}
      if(offset==length)length=offset=0;
    }
    do {
      struct netbuf *buf=NULL;void *payload;u16_t n;
      error=netconn_recv(c->conn,&buf);if(error==ERR_TIMEOUT||error==ERR_WOULDBLOCK)break;
      if(error!=ERR_OK||!buf)return;
      do{netbuf_data(buf,&payload,&n);if(!socket_send_all(c->local,payload,n)){netbuf_delete(buf);return;}received++;}while(netbuf_next(buf)>=0);
      netbuf_delete(buf);deadline=GetTickCount64()+600000;
    }while(received<128&&!cancelled());
  }
}
static int socks_udp_bind(client *c) {
  ip_addr_t local;err_t error;c->conn=netconn_new(NETCONN_UDP);if(!c->conn)return 0;
  ip_addr_set_ip4_u32(&local,htonl(virtual_host));
  for(;;){c->port=socks_next_port();error=netconn_bind(c->conn,&local,c->port);if(error==ERR_OK)break;if(error!=ERR_USE)return 0;}
  if(netconn_bind_if(c->conn,netif_get_index(&c->route->netif))!=ERR_OK)return 0;
  udp_port_routes[c->port]=c->route;netconn_set_recvtimeout(c->conn,1);return 1;
}
static void socks_udp_association(client *c,const socks_address *request) {
  SOCKET relay=INVALID_SOCKET;struct sockaddr_in local,peer;socklen_t peer_size=sizeof(peer),local_size=sizeof(local);unsigned short client_port=request->port;u_long nonblocking=1;
  unsigned char input[SOCKS_MAX_DATAGRAM],output[SOCKS_MAX_DATAGRAM];int active=0;
  /* RFC 1928 associates the relay with the TCP client's source IP/port.
   * Unknown port is fixed by the first valid UDP packet; it cannot be changed. */
  if(request->type!=1||(request->ip!=0&&request->ip!=0x7f000001)||getpeername(c->local,(struct sockaddr*)&peer,&peer_size)||ntohl(peer.sin_addr.s_addr)!=INADDR_LOOPBACK){socks_reply(c->local,2,0,0);return;}
  if(!c->route->configured||!c->route->mac_known){socks_reply(c->local,3,0,0);return;}
  if(!socks_udp_bind(c)){socks_reply(c->local,1,0,0);return;}
  relay=socket(AF_INET,SOCK_DGRAM,IPPROTO_UDP);if(relay==INVALID_SOCKET){socks_reply(c->local,1,0,0);goto done;}
  memset(&local,0,sizeof(local));local.sin_family=AF_INET;local.sin_addr.s_addr=htonl(INADDR_LOOPBACK);
  if(bind(relay,(struct sockaddr*)&local,sizeof(local))||getsockname(relay,(struct sockaddr*)&local,&local_size)){socks_reply(c->local,1,0,0);goto done;}
  set_nonblocking(relay,(int)nonblocking);set_nonblocking(c->local,(int)nonblocking);
  if(!socks_reply(c->local,0,INADDR_LOOPBACK,ntohs(local.sin_port)))goto done;
  InterlockedIncrement(&c->route->udp_associations);active=1;
  while(!cancelled()){
    char control;int control_count=recv(c->local,&control,1,0),i;
    if(!control_count||control_count>0||(control_count<0&&WSAGetLastError()!=WSAEWOULDBLOCK))break;
    for(i=0;i<64&&!cancelled();i++){
      struct sockaddr_in from;socklen_t from_size=sizeof(from);int n=recvfrom(relay,(char*)input,sizeof(input),0,(struct sockaddr*)&from,&from_size);socks_address target;size_t offset;ip_addr_t destination;struct netbuf *buf;void *payload;err_t error;
      if(n<0){if(WSAGetLastError()!=WSAEWOULDBLOCK)InterlockedIncrement64(&c->route->udp_dropped);break;}
      if(ntohl(from.sin_addr.s_addr)!=INADDR_LOOPBACK||(client_port&&ntohs(from.sin_port)!=client_port)||socks_parse_datagram(input,(size_t)n,&target,&offset)||!socks_resolve(c->route,&target,&destination)){InterlockedIncrement64(&c->route->udp_dropped);continue;}
      if(!client_port)client_port=ntohs(from.sin_port);
      buf=netbuf_new();if(!buf){InterlockedIncrement64(&c->route->udp_dropped);continue;}
      payload=netbuf_alloc(buf,(u16_t)((size_t)n-offset));if(!payload){netbuf_delete(buf);InterlockedIncrement64(&c->route->udp_dropped);continue;}
      memcpy(payload,input+offset,(size_t)n-offset);error=netconn_sendto(c->conn,buf,&destination,target.port);netbuf_delete(buf);
      if(error==ERR_OK)InterlockedIncrement64(&c->route->udp_sent);else InterlockedIncrement64(&c->route->udp_dropped);
    }
    for(i=0;i<64&&!cancelled();i++){
      struct netbuf *buf=NULL;u16_t length;uint32_t remote;err_t error=netconn_recv_udp_raw_netbuf_flags(c->conn,&buf,NETCONN_DONTBLOCK);
      if(error==ERR_WOULDBLOCK||error==ERR_TIMEOUT)break;if(error!=ERR_OK||!buf)goto done;
      length=netbuf_len(buf);remote=ntohl(ip4_addr_get_u32(ip_2_ip4(netbuf_fromaddr(buf))));
      if(!client_port||length>SOCKS_MAX_PAYLOAD||!public_ip(remote)){netbuf_delete(buf);InterlockedIncrement64(&c->route->udp_dropped);continue;}
      memset(output,0,4);output[3]=1;put32(output+4,remote);put16(output+8,netbuf_fromport(buf));
      if(netbuf_copy(buf,output+10,length)!=length){netbuf_delete(buf);InterlockedIncrement64(&c->route->udp_dropped);continue;}netbuf_delete(buf);
      memset(&peer,0,sizeof(peer));peer.sin_family=AF_INET;peer.sin_addr.s_addr=htonl(INADDR_LOOPBACK);peer.sin_port=htons(client_port);
      if(sendto(relay,(const char*)output,length+10,0,(struct sockaddr*)&peer,sizeof(peer))==(int)length+10)InterlockedIncrement64(&c->route->udp_received);else InterlockedIncrement64(&c->route->udp_dropped);
    }
    Sleep(2);
  }
done:
  if(c->port)InterlockedExchangePointer((PVOID volatile*)&udp_port_routes[c->port],NULL);
  if(active)InterlockedDecrement(&c->route->udp_associations);if(relay!=INVALID_SOCKET)closesocket(relay);
}
static void * serve_socks_client(void * pointer) {
  client *c=(client*)pointer;unsigned char greeting[2],methods[255],request[4],answer[2]={5,255};socks_address address;int i,error;
  set_receive_timeout(c->local,5000);
  if(!socks_recv_exact(c->local,greeting,2)||greeting[0]!=5||!greeting[1])goto done;
  if(!socks_recv_exact(c->local,methods,greeting[1]))goto done;
  for(i=0;i<greeting[1];i++)if(!methods[i]){answer[1]=0;break;}
  if(!socket_send_all(c->local,answer,2)||answer[1]==255)goto done;
  if(!socks_recv_exact(c->local,request,4))goto done;
  if(request[0]!=5||request[2]){socks_reply(c->local,1,0,0);goto done;}
  if(request[1]!=1&&request[1]!=3){socks_reply(c->local,7,0,0);goto done;}
  error=socks_read_address(c->local,request[3],&address);if(error){socks_reply(c->local,(unsigned char)error,0,0);goto done;}
  if(request[1]==3){socks_udp_association(c,&address);goto done;}
  error=socks_tcp_connect(c,&address);if(error){socks_reply(c->local,(unsigned char)error,0,0);goto done;}
  if(socks_reply(c->local,0,virtual_host,c->port))socks_tcp_pump(c);
done:
  if(c->conn)netconn_delete(c->conn);shutdown(c->local,SD_BOTH);closesocket(c->local);InterlockedDecrement(&connection_count);free(c);return 0;
}
static void * accept_socks_loop(void * pointer) {
  profile *route=(profile*)pointer;
  while(!cancelled()&&!route->listener_stopping){
    fd_set readset;struct timeval wait={0,200000};SOCKET s;client *c;thread_handle thread;
    SOCKET listener=route->socks_listener;if(listener==INVALID_SOCKET)break;FD_ZERO(&readset);FD_SET(listener,&readset);if(select(listener+1,&readset,NULL,NULL,&wait)<=0)continue;
    s=accept(listener,NULL,NULL);if(s==INVALID_SOCKET)continue;
    if(InterlockedIncrement(&connection_count)>MAX_CONNECTIONS){InterlockedDecrement(&connection_count);closesocket(s);continue;}
    c=(client*)calloc(1,sizeof(*c));if(!c){InterlockedDecrement(&connection_count);closesocket(s);continue;}c->local=s;c->route=route;
    thread=spawn_thread(serve_socks_client,c);if(thread)detach_thread(thread);else{free(c);closesocket(s);InterlockedDecrement(&connection_count);}
  }return 0;
}
static int socks_self_test(void) {
  const unsigned char ipv4[]={1,8,8,8,8,0,53};const unsigned char domain[]={3,11,'e','x','a','m','p','l','e','.','c','o','m',1,187};unsigned char datagram[32]={0,0,0,1,8,8,8,8,0,53,1,2,3};socks_address address;size_t offset;
  if(socks_parse_address(ipv4,sizeof(ipv4),&address)||address.ip!=0x08080808||address.port!=53||address.bytes!=7)return 1;
  if(socks_parse_address(domain,sizeof(domain),&address)||strcmp(address.domain,"example.com")||address.port!=443)return 2;
  if(!socks_parse_address(domain,sizeof(domain)-1,&address))return 3;
  if(socks_parse_datagram(datagram,13,&address,&offset)||offset!=10)return 4;
  datagram[2]=1;if(!socks_parse_datagram(datagram,13,&address,&offset))return 5;datagram[2]=0;datagram[3]=4;if(socks_parse_datagram(datagram,13,&address,&offset)!=8)return 6;
  return 0;
}
