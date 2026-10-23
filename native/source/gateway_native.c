/* MultiRoute application-only Ethernet transport. No Windows route/IP configuration API is used.
 * lwIP is a separate TCP/IP stack; only this process owns the virtual IPv4 address.
 * TLS stays end-to-end in the Java HTTP client through the loopback CONNECT tunnel.
 */
#define WIN32_LEAN_AND_MEAN
#define MR_DNS_MANAGED_SUPPORT 1
#define _WIN32_WINNT 0x0601
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <iphlpapi.h>
#include <pcap.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#undef IP_STATS
#undef ICMP_STATS
#undef TCP_STATS
#undef UDP_STATS
#undef IP6_STATS
#include "lwip/init.h"
#include "lwip/api.h"
#include "lwip/netif.h"
#include "lwip/tcpip.h"
#include "lwip/tcp.h"
#include "lwip/udp.h"
#include "lwip/ip4_frag.h"
#include "lwip/inet_chksum.h"
#include "lwip/etharp.h"
#include "netif/ethernet.h"
#include "gateway_topology.h"

#define MAX_PROFILES 128
#define MAX_CONFIGURED_PROFILES 32
#define MAX_HEADER 16384
#define MAX_CONNECTIONS 96
typedef struct profile {
  char gateway[16];
  uint32_t gateway_host;
  unsigned char gateway_mac[6];
  int mac_known;
  volatile LONG configured, listener_stopping;
  struct netif netif;
  SOCKET listener;
  unsigned short proxy_port;
  SOCKET socks_listener;
  unsigned short socks_port;
  volatile LONG64 tx_bytes, rx_bytes;
  volatile LONG64 udp_sent,udp_received,udp_dropped;
  volatile LONG udp_associations;
} profile;
typedef struct client { SOCKET local; profile *route; struct netconn *conn; uint16_t port; } client;
static profile profiles[MAX_PROFILES];
static volatile LONG profile_count_raw;
static int profile_registry_count(void){
#ifdef _WIN32
  return (int)InterlockedCompareExchange(&profile_count_raw,0,0);
#else
  return (int)__atomic_load_n(&profile_count_raw,__ATOMIC_ACQUIRE);
#endif
}
#define profile_count profile_registry_count()
static ULONGLONG control_revision;
static char control_error[512];
static void ready_hot_fields(FILE *f);
static char virtual_ip[16], adapter_name[512], interface_name[512], ready_path[4096];
static uint32_t virtual_host, mask_host;
static unsigned char adapter_mac[6];
static unsigned int interface_index;
static mr_lan_state lan_topology;
static volatile LONG topology_validated;
static ULONGLONG next_topology_check;
static pcap_t *capture, *transmit;
static CRITICAL_SECTION tx_lock;
static volatile LONG stopping, connection_count, next_port = 20000;
static profile *volatile port_routes[65536];
static profile *volatile udp_port_routes[65536];
static HANDLE parent_handle, instance_mutex;
static int claimed;
static int debug_headers;
static volatile LONG debug_header_count;
static CRITICAL_SECTION ready_lock;
static char terminal_error[1024];

static uint16_t get16(const unsigned char *p) { return (uint16_t)((p[0] << 8) | p[1]); }
static uint32_t get32(const unsigned char *p) { return ((uint32_t)p[0]<<24)|((uint32_t)p[1]<<16)|((uint32_t)p[2]<<8)|p[3]; }
static void put16(unsigned char *p, uint16_t v) { p[0]=(unsigned char)(v>>8);p[1]=(unsigned char)v; }
static void put32(unsigned char *p, uint32_t v) {p[0]=(unsigned char)(v>>24);p[1]=(unsigned char)(v>>16);p[2]=(unsigned char)(v>>8);p[3]=(unsigned char)v; }
static int parse_ip(const char *s, uint32_t *out) {
  struct in_addr a;
  const char *p=s;
  int digits=0, dots=0;
  if(!s || !*s || strlen(s)>15) return 0;
  while(*p) { if(*p=='.') { if(!digits || digits>3) return 0;digits=0;dots++; } else if(*p>='0'&&*p<='9') digits++; else return 0; p++; }
  if(dots!=3 || !digits || digits>3 || InetPtonA(AF_INET,s,&a)!=1) return 0;
  *out=ntohl(a.s_addr);return 1;
}
static int cancelled(void) {
  if(stopping) return 1;
  if(parent_handle && WaitForSingleObject(parent_handle,0)==WAIT_OBJECT_0) { InterlockedExchange(&stopping,1);return 1; }
  return 0;
}
static int public_ip(uint32_t ip) {
  unsigned int a=ip>>24,b=(ip>>16)&255;
  return a>0&&a<224&&a!=10&&a!=127&&!(a==169&&b==254)&&!(a==172&&b>=16&&b<=31)&&!(a==192&&b==168)&&!(a==100&&b>=64&&b<=127);
}
static void json_string(FILE *f,const char *s) {
  const unsigned char *p=(const unsigned char*)s;
  fputc('"',f);while(*p) { if(*p=='"'||*p=='\\') {fputc('\\',f);fputc(*p,f);} else if(*p<32) fprintf(f,"\\u%04x",*p);else fputc(*p,f);p++; }fputc('"',f);
}
static void format_mac(char *out,const unsigned char *mac) {sprintf(out,"%02x:%02x:%02x:%02x:%02x:%02x",mac[0],mac[1],mac[2],mac[3],mac[4],mac[5]);}
static void ready_result(const char *error) {
  WCHAR temp[4200],path[4096];char mac[32];FILE *f;int i;
  if(!*ready_path) return;
  EnterCriticalSection(&ready_lock);
  if(error)snprintf(terminal_error,sizeof(terminal_error),"%s",error);
  if(*terminal_error)error=terminal_error;
  if(!MultiByteToWideChar(CP_UTF8,MB_ERR_INVALID_CHARS,ready_path,-1,path,4096)){fprintf(stderr,"Invalid ready file path\n");LeaveCriticalSection(&ready_lock);return;}
  swprintf_s(temp,4200,L"%s.tmp",path);
  f=_wfopen(temp,L"wb");if(!f) {fprintf(stderr,"Cannot write ready file\n");LeaveCriticalSection(&ready_lock);return;}
  fprintf(f,"{\"ok\":%s,\"application_routes_only\":true,\"virtual_ip\":",error?"false":"true");json_string(f,virtual_ip);
  if(error) {fputs(",\"error\":",f);json_string(f,error);}
  fputs(",\"interface\":",f);json_string(f,interface_name);
  fputs(",\"interface_name\":",f);json_string(f,interface_name);fprintf(f,",\"interface_index\":%u",interface_index);
  mr_lan_ready(f,&lan_topology,topology_validated);fputs(",\"profiles\":{",f);
  for(i=0;i<profile_count;i++) {
    profile *r=&profiles[i];if(i)fputc(',',f);json_string(f,r->gateway);fputs(":{\"gateway\":",f);json_string(f,r->gateway);
    fprintf(f,",\"port\":%u,\"socks_port\":%u,\"available\":%s,\"source_ip\":",r->proxy_port,r->socks_port,(r->configured&&r->mac_known)?"true":"false");json_string(f,virtual_ip);
    format_mac(mac,r->gateway_mac);fputs(",\"gateway_mac\":",f);json_string(f,mac);fputs(",\"interface_name\":",f);json_string(f,interface_name);
    fprintf(f,",\"tx_bytes\":%lld,\"rx_bytes\":%lld,\"udp_sent\":%lld,\"udp_received\":%lld,\"udp_dropped\":%lld,\"udp_associations\":%ld}",r->tx_bytes,r->rx_bytes,r->udp_sent,r->udp_received,r->udp_dropped,r->udp_associations);
  }
  fputs("},\"gateway_profiles\":[",f);
  for(i=0;i<profile_count;i++) {
    profile *r=&profiles[i];if(i)fputc(',',f);fputs("{\"gateway\":",f);json_string(f,r->gateway);
    fprintf(f,",\"available\":%s,\"port\":%u,\"socks_port\":%u,\"source_ip\":",(r->configured&&r->mac_known)?"true":"false",r->proxy_port,r->socks_port);json_string(f,virtual_ip);
    format_mac(mac,r->gateway_mac);fputs(",\"gateway_mac\":",f);json_string(f,mac);
    fputs(",\"message\":",f);json_string(f,r->mac_known?"Application Ethernet transport ready; gateway MAC confirmed":"Gateway did not answer ARP; no fallback gateway will be used");fputc('}',f);
  }
  fputc(']',f);ready_hot_fields(f);fputc('}',f);fclose(f);MoveFileExW(temp,path,MOVEFILE_REPLACE_EXISTING|MOVEFILE_WRITE_THROUGH);LeaveCriticalSection(&ready_lock);
}
static void fatal(const char *message) {InterlockedExchange(&topology_validated,0);fprintf(stderr,"%s\n",message);ready_result(message);InterlockedExchange(&stopping,1);}
static void report_topology_error(const char *reason) {fprintf(stderr,"NETWORK_TOPOLOGY_ERROR %s\n",reason);fflush(stderr);}
static int topology_error(char *error,size_t size,const char *reason) {snprintf(error,size,"%s",reason);return 0;}
static size_t current_gateway_hosts(uint32_t hosts[MAX_PROFILES]) {
  int i;size_t count=0;for(i=0;i<profile_count;i++)if(profiles[i].configured)hosts[count++]=profiles[i].gateway_host;return count;
}
static IP_ADAPTER_ADDRESSES *lan_adapters(char *error,size_t error_size) {
  ULONG size=32768,result;IP_ADAPTER_ADDRESSES *all=(IP_ADAPTER_ADDRESSES*)malloc(size);
  if(!all){topology_error(error,error_size,"Cannot allocate LAN adapter snapshot");return NULL;}
  result=GetAdaptersAddresses(AF_INET,GAA_FLAG_SKIP_ANYCAST|GAA_FLAG_SKIP_MULTICAST|GAA_FLAG_SKIP_DNS_SERVER,NULL,all,&size);
  if(result==ERROR_BUFFER_OVERFLOW){free(all);all=(IP_ADAPTER_ADDRESSES*)malloc(size);if(!all){topology_error(error,error_size,"Cannot allocate LAN adapter snapshot");return NULL;}
    result=GetAdaptersAddresses(AF_INET,GAA_FLAG_SKIP_ANYCAST|GAA_FLAG_SKIP_MULTICAST|GAA_FLAG_SKIP_DNS_SERVER,NULL,all,&size);}
  if(result!=NO_ERROR){free(all);topology_error(error,error_size,"Cannot read LAN adapters");return NULL;}return all;
}
static int lan_os_addresses(IP_ADAPTER_ADDRESSES *all,uint32_t addresses[MR_LAN_MAX_ADDRESSES],size_t *count,char *error,size_t error_size) {
  IP_ADAPTER_ADDRESSES *a;*count=0;
  for(a=all;a;a=a->Next){IP_ADAPTER_UNICAST_ADDRESS *u;for(u=a->FirstUnicastAddress;u;u=u->Next){
    if(!u->Address.lpSockaddr||u->Address.lpSockaddr->sa_family!=AF_INET)continue;
    if(*count>=MR_LAN_MAX_ADDRESSES)return topology_error(error,error_size,"Too many local IPv4 addresses to validate the application network");
    addresses[(*count)++]=ntohl(((struct sockaddr_in*)u->Address.lpSockaddr)->sin_addr.s_addr);
  }}return 1;
}

static int choose_adapter(char *error,size_t error_size) {
  IP_ADAPTER_ADDRESSES *all=lan_adapters(error,error_size),*a;int candidates=0;
  uint32_t addresses[MR_LAN_MAX_ADDRESSES],hosts[MAX_PROFILES],selected=0;size_t count,host_count=current_gateway_hosts(hosts);
  mr_lan_result result,last=MR_LAN_OK;
  if(!all)return 0;
  if(!lan_os_addresses(all,addresses,&count,error,error_size)){free(all);return 0;}
  result=mr_lan_collisions(virtual_host,hosts,host_count,addresses,count);
  if(result!=MR_LAN_OK){free(all);return topology_error(error,error_size,mr_lan_reason(result));}
  for(a=all;a;a=a->Next) {
    IP_ADAPTER_UNICAST_ADDRESS *u;
    if(a->OperStatus!=IfOperStatusUp || a->IfType!=IF_TYPE_ETHERNET_CSMACD || a->PhysicalAddressLength!=6) continue;
    for(u=a->FirstUnicastAddress;u;u=u->Next) {
      uint32_t src,mask;
      if(!u->Address.lpSockaddr||u->Address.lpSockaddr->sa_family!=AF_INET || u->OnLinkPrefixLength<1 || u->OnLinkPrefixLength>30)continue;
      src=ntohl(((struct sockaddr_in*)u->Address.lpSockaddr)->sin_addr.s_addr);
      mask=0xffffffffU<<(32-u->OnLinkPrefixLength);
      result=mr_lan_validate(src,mask,virtual_host,hosts,host_count,addresses,count);
      if(result!=MR_LAN_OK){if(last==MR_LAN_OK||(src&mask)==(virtual_host&mask))last=result;continue;}
      candidates++;
      snprintf(adapter_name,sizeof(adapter_name),"\\Device\\NPF_%s",a->AdapterName);
      WideCharToMultiByte(CP_UTF8,0,a->FriendlyName,-1,interface_name,sizeof(interface_name),NULL,NULL);
      memcpy(adapter_mac,a->PhysicalAddress,6);interface_index=a->IfIndex;mask_host=mask;selected=src;
      break;
    }
  }
  free(all);
  if(candidates!=1)return topology_error(error,error_size,candidates?"More than one physical LAN adapter matches the gateways":last!=MR_LAN_OK?mr_lan_reason(last):"No active physical Ethernet LAN adapter contains the virtual IP and gateways");
  mr_lan_start(&lan_topology,selected,mask_host,virtual_host);InterlockedExchange(&topology_validated,1);next_topology_check=GetTickCount64()+2000;
  return 1;
}
static int read_selected_topology(const uint32_t *hosts,size_t host_count,uint32_t *computer_ip,char *error,size_t error_size) {
  IP_ADAPTER_ADDRESSES *all=lan_adapters(error,error_size),*a;uint32_t addresses[MR_LAN_MAX_ADDRESSES];
  mr_lan_address selected[MR_LAN_MAX_ADDRESSES];size_t count,selected_count=0;int found=0;mr_lan_result result;
  if(!all)return 0;
  if(!lan_os_addresses(all,addresses,&count,error,error_size)){free(all);return 0;}
  for(a=all;a;a=a->Next){char name[512];IP_ADAPTER_UNICAST_ADDRESS *u;
    snprintf(name,sizeof(name),"\\Device\\NPF_%s",a->AdapterName);if(strcmp(name,adapter_name))continue;
    found=1;
    if(a->OperStatus!=IfOperStatusUp||a->IfType!=IF_TYPE_ETHERNET_CSMACD||a->PhysicalAddressLength!=6||a->IfIndex!=interface_index){free(all);return topology_error(error,error_size,"The selected Ethernet adapter is no longer active");}
    if(memcmp(a->PhysicalAddress,adapter_mac,6)){free(all);return topology_error(error,error_size,"The selected Ethernet adapter hardware address changed; check and apply the application network settings again");}
    for(u=a->FirstUnicastAddress;u;u=u->Next){uint32_t mask=0;
      if(!u->Address.lpSockaddr||u->Address.lpSockaddr->sa_family!=AF_INET)continue;
      if(selected_count>=MR_LAN_MAX_ADDRESSES){free(all);return topology_error(error,error_size,"Too many local IPv4 addresses to validate the application network");}
      if(u->OnLinkPrefixLength>=1&&u->OnLinkPrefixLength<=30)mask=0xffffffffU<<(32-u->OnLinkPrefixLength);
      selected[selected_count].address=ntohl(((struct sockaddr_in*)u->Address.lpSockaddr)->sin_addr.s_addr);selected[selected_count++].mask=mask;
    }break;
  }
  if(!found){free(all);return topology_error(error,error_size,"The selected Ethernet adapter is no longer active");}
  result=mr_lan_current(&lan_topology,selected,selected_count,hosts,host_count,addresses,count,computer_ip);free(all);
  if(result!=MR_LAN_OK)return topology_error(error,error_size,mr_lan_reason(result));
  return 1;
}
static void monitor_topology(void) {
  uint32_t hosts[MAX_PROFILES],computer_ip,previous;size_t count;char error[512],old_ip[16],new_ip[16],cidr[20];int changed;
  ULONGLONG now=GetTickCount64();if(now<next_topology_check)return;next_topology_check=now+2000;count=current_gateway_hosts(hosts);
  if(!read_selected_topology(hosts,count,&computer_ip,error,sizeof(error))){report_topology_error(error);fatal(error);return;}
  EnterCriticalSection(&ready_lock);previous=lan_topology.computer_ipv4;changed=mr_lan_observe(&lan_topology,computer_ip);LeaveCriticalSection(&ready_lock);
  if(changed){mr_lan_ip(old_ip,previous);mr_lan_ip(new_ip,computer_ip);mr_lan_cidr(cidr,&lan_topology);
    fprintf(stderr,"HOST_IPV4_CHANGED interface=%u previous=%s current=%s subnet=%s virtual_ip=%s\n",interface_index,old_ip,new_ip,cidr,virtual_ip);fflush(stderr);}
}
static int open_capture(char *error,size_t error_size) {
  char filter[8192],errbuf[PCAP_ERRBUF_SIZE]={0};struct bpf_program code;int i;
  capture=pcap_create(adapter_name,errbuf);
  if(!capture) {snprintf(error,error_size,"Npcap open: %s",errbuf);return 0;}
  pcap_set_snaplen(capture,65536);pcap_set_promisc(capture,1);pcap_set_timeout(capture,10);pcap_set_immediate_mode(capture,1);pcap_set_buffer_size(capture,8*1024*1024);
  if(pcap_activate(capture)<0 || pcap_datalink(capture)!=DLT_EN10MB) {snprintf(error,error_size,"Npcap physical Ethernet capture unavailable: %s",pcap_geterr(capture));return 0;}
  snprintf(filter,sizeof(filter),"(ip dst host %s) or (arp and (arp[14:4] = 0x%08x or arp[24:4] = 0x%08x",virtual_ip,virtual_host,virtual_host);
  for(i=0;i<profile_count;i++) {char part[80];snprintf(part,sizeof(part)," or arp[14:4] = 0x%08x",profiles[i].gateway_host);strcat(filter,part);}
  strcat(filter,"))");
  if(pcap_compile(capture,&code,filter,1,PCAP_NETMASK_UNKNOWN)<0) {snprintf(error,error_size,"Npcap filter: %s",pcap_geterr(capture));return 0;}
  if(pcap_setfilter(capture,&code)<0) {pcap_freecode(&code);snprintf(error,error_size,"Npcap filter installation failed");return 0;}pcap_freecode(&code);
  if(pcap_setnonblock(capture,1,errbuf)<0) {snprintf(error,error_size,"Npcap nonblocking: %s",errbuf);return 0;}
  transmit=pcap_open_live(adapter_name,65536,0,10,errbuf);
  if(!transmit) {snprintf(error,error_size,"Npcap injection open: %s",errbuf);return 0;}
  if(pcap_compile(transmit,&code,"less 0",1,PCAP_NETMASK_UNKNOWN)<0 || pcap_setfilter(transmit,&code)<0){snprintf(error,error_size,"Npcap transmit privacy filter failed");return 0;}pcap_freecode(&code);
  return 1;
}
static int send_frame(const unsigned char *frame,int length) {
  int result;EnterCriticalSection(&tx_lock);result=pcap_sendpacket(transmit,frame,length);LeaveCriticalSection(&tx_lock);return result;
}
static uint32_t checksum_sum(const unsigned char *p,int n,uint32_t sum) {
  while(n>1){sum+=get16(p);p+=2;n-=2;}if(n)sum+=(uint32_t)*p<<8;
  while(sum>>16)sum=(sum&65535)+(sum>>16);return sum;
}
static void debug_tcp(const char *direction,profile *r,const unsigned char *f,int n) {
  unsigned int ih,th,total,payload;const unsigned char *t;uint32_t sum;int valid;
  if(!debug_headers||n<54||get16(f+12)!=0x0800||f[23]!=6||InterlockedIncrement(&debug_header_count)>300)return;
  ih=(f[14]&15)*4;total=get16(f+16);if(ih<20||total<ih+20||14+total>(unsigned int)n)return;
  t=f+14+ih;th=(t[12]>>4)*4;if(th<20||ih+th>total)return;payload=total-ih-th;
  sum=checksum_sum(f+26,8,0);sum+=6+(total-ih);sum=checksum_sum(t,(int)(total-ih),sum);valid=sum==65535;
  fprintf(stderr,"TCP %s gw=%s ports=%u:%u flags=%02x seq=%u ack=%u wnd=%u payload=%u checksum=%s ipcheck=%s\n",direction,r->gateway,get16(t),get16(t+2),t[13],get32(t+4),get32(t+8),get16(t+14),payload,valid?"ok":"BAD",checksum_sum(f+14,(int)ih,0)==65535?"ok":"BAD");
}
static int arp_packet(const unsigned char *f,int n,uint16_t *operation,uint32_t *sender,uint32_t *target) {
  if(n<42 || get16(f+12)!=0x0806 || get16(f+14)!=1 || get16(f+16)!=0x0800 || f[18]!=6 || f[19]!=4 || memcmp(f+6,f+22,6)) return 0;
  *operation=get16(f+20);*sender=get32(f+28);*target=get32(f+38);return *operation==1||*operation==2;
}
static int is_conflict(const unsigned char *frame,int length) {
  uint16_t operation;uint32_t sender,target;
  if(!arp_packet(frame,length,&operation,&sender,&target) || !memcmp(frame+22,adapter_mac,6)) return 0;
  return sender==virtual_host || (sender==0&&target==virtual_host);
}
static int arp_send(uint32_t sender,uint32_t target,uint16_t operation,const unsigned char *target_mac) {
  unsigned char f[60]={0};
  if(target_mac) memcpy(f,target_mac,6);else memset(f,255,6);
  memcpy(f+6,adapter_mac,6);put16(f+12,0x0806);put16(f+14,1);put16(f+16,0x0800);f[18]=6;f[19]=4;put16(f+20,operation);
  memcpy(f+22,adapter_mac,6);put32(f+28,sender);if(target_mac)memcpy(f+32,target_mac,6);put32(f+38,target);
  if(send_frame(f,sizeof(f))<0){fatal("Npcap Ethernet injection failed; application transport stopped");return 0;}return 1;
}
static int collect_arp(unsigned int milliseconds,int conflict_check) {
  ULONGLONG until=GetTickCount64()+milliseconds;
  while(GetTickCount64()<until && !cancelled()) {
    struct pcap_pkthdr *header;const unsigned char *frame;int result=pcap_next_ex(capture,&header,&frame),i;
    uint16_t op;uint32_t sender,target;
    if(result<0) return 0;
    if(!result) {Sleep(2);continue;}
    if(conflict_check && is_conflict(frame,(int)header->caplen)) return 0;
    if(!arp_packet(frame,(int)header->caplen,&op,&sender,&target) || target!=virtual_host)continue;
    for(i=0;i<profile_count;i++) if(sender==profiles[i].gateway_host && !(frame[22]&1)) {memcpy(profiles[i].gateway_mac,frame+22,6);profiles[i].mac_known=1;}
  }
  return !cancelled();
}
static int address_claim(void) {
  int i;
  /* RFC 5227: random initial wait, three probes with randomized 1-2 second gaps,
   * then 2 seconds before announcing. Nothing adds the address to Windows. */
  if(!collect_arp(100+(unsigned int)(GetTickCount64()%900),1))return 0;
  for(i=0;i<3;i++) {if(!arp_send(0,virtual_host,1,NULL)||!collect_arp(i==2?2000:1000+(unsigned int)(GetTickCount64()%1000),1))return 0;}
  if(!arp_send(virtual_host,virtual_host,1,NULL))return 0;claimed=1;
  for(i=0;i<profile_count;i++)if(!arp_send(virtual_host,profiles[i].gateway_host,1,NULL))return 0;
  if(!collect_arp(500,1))return 0;
  for(i=0;i<profile_count;i++) if(!profiles[i].mac_known&&!arp_send(virtual_host,profiles[i].gateway_host,1,NULL))return 0;
  if(!collect_arp(500,1))return 0;
  for(i=0;i<profile_count;i++) if(!profiles[i].mac_known&&!arp_send(virtual_host,profiles[i].gateway_host,1,NULL))return 0;
  if(!collect_arp(1000,1))return 0;
  return arp_send(virtual_host,virtual_host,1,NULL);
}
static err_t link_output(struct netif *netif,struct pbuf *p) {
  unsigned char frame[1600];profile *r=(profile*)netif->state;u16_t length=p->tot_len;
  if(cancelled() || length>sizeof(frame) || pbuf_copy_partial(p,frame,length,0)!=length) return ERR_IF;
  if(length>=14 && get16(frame+12)==0x0800 && (!r->configured || !r->mac_known || memcmp(frame,r->gateway_mac,6))) return ERR_RTE;
  if(length<60) {memset(frame+length,0,60-length);length=60;}
  debug_tcp("TX",r,frame,length);
  if(send_frame(frame,length)<0){fatal("Npcap Ethernet injection failed; application transport stopped");return ERR_IF;}
  InterlockedAdd64(&r->tx_bytes,length);return ERR_OK;
}
static err_t netif_init_app(struct netif *netif) {
  netif->name[0]='m';netif->name[1]='r';netif->hwaddr_len=6;memcpy(netif->hwaddr,adapter_mac,6);
  netif->mtu=1500;netif->flags=NETIF_FLAG_BROADCAST|NETIF_FLAG_ETHARP|NETIF_FLAG_ETHERNET;
  netif->output=etharp_output;netif->linkoutput=link_output;return ERR_OK;
}
static void stack_ready(void *arg) {SetEvent((HANDLE)arg);}
static int initialize_stack(void) {
  HANDLE event=CreateEvent(NULL,TRUE,FALSE,NULL);ip4_addr_t ip,mask,gw;int i;
  if(!event)return 0;tcpip_init(stack_ready,event);if(WaitForSingleObject(event,5000)!=WAIT_OBJECT_0){CloseHandle(event);return 0;}CloseHandle(event);
  ip.addr=htonl(virtual_host);mask.addr=htonl(mask_host);
  InterlockedExchange(&next_port,20000+(LONG)(LWIP_RAND()%30000));
  LOCK_TCPIP_CORE();
  for(i=0;i<profile_count;i++) {
    profile *r=&profiles[i];gw.addr=htonl(r->gateway_host);
    if(!netif_add(&r->netif,&ip,&mask,&gw,r,netif_init_app,tcpip_input)){UNLOCK_TCPIP_CORE();return 0;}
    netif_set_link_up(&r->netif);netif_set_up(&r->netif);
  }
  UNLOCK_TCPIP_CORE();return 1;
}
static DWORD WINAPI receive_loop(LPVOID unused) {
  ULONGLONG next_arp=GetTickCount64()+5000;
  (void)unused;
  while(!cancelled()) {
    struct pcap_pkthdr *header;const unsigned char *frame;profile *route=NULL;int got=pcap_next_ex(capture,&header,&frame),i,n;
    uint16_t op;uint32_t sender,target;
    if(got<0) {fatal("Npcap receive failed; no other gateway will be used");break;}
    if(!got){
      if(GetTickCount64()>=next_arp){for(i=0;i<profile_count;i++)if(!arp_send(virtual_host,profiles[i].gateway_host,1,NULL))return 0;next_arp=GetTickCount64()+5000;}
      Sleep(1);continue;
    }n=(int)header->caplen;
    if(is_conflict(frame,n)){fatal("Virtual IP conflict detected; application transport stopped");break;}
    if(arp_packet(frame,n,&op,&sender,&target)) {
      if(claimed && target==virtual_host && op==1 && sender!=virtual_host && memcmp(frame+22,adapter_mac,6)&&!arp_send(virtual_host,sender,2,frame+22))break;
      if(target!=virtual_host)continue;
      for(i=0;i<profile_count;i++)if(sender==profiles[i].gateway_host && !(frame[22]&1)){
        route=&profiles[i];LOCK_TCPIP_CORE();memcpy(route->gateway_mac,frame+22,6);route->mac_known=1;UNLOCK_TCPIP_CORE();break;
      }
    } else if(n>=34 && get16(frame+12)==0x0800 && get32(frame+30)==virtual_host && !memcmp(frame,adapter_mac,6)) {
      unsigned int header_length=(frame[14]&15)*4;
      if((frame[14]>>4)!=4 || header_length<20 || n<14+(int)header_length)continue;
      if(frame[23]==6 && !(get16(frame+20)&0x1fff) && n>=14+(int)header_length+4)
        route=port_routes[get16(frame+14+header_length+2)];
      else if(frame[23]==17 && !(get16(frame+20)&0x1fff) && n>=14+(int)header_length+8)
        route=udp_port_routes[get16(frame+14+header_length+2)];
      if(route)debug_tcp("RAW-RX",route,frame,n);
      if(route && memcmp(frame+6,route->gateway_mac,6))continue;
      if(frame[23]==17 && !(get16(frame+20)&0x1fff) && !route)continue;
      if(!route)for(i=0;i<profile_count;i++)if(!memcmp(frame+6,profiles[i].gateway_mac,6)){route=&profiles[i];break;}
    }
    if(route) {
      debug_tcp("RX",route,frame,n);
      struct pbuf *p=pbuf_alloc(PBUF_RAW,(u16_t)n,PBUF_POOL);
      if(p) {pbuf_take(p,frame,(u16_t)n);if(route->netif.input(p,&route->netif)!=ERR_OK)pbuf_free(p);else InterlockedAdd64(&route->rx_bytes,n);}
    }
  }
  return 0;
}
static int socket_send_all(SOCKET s,const void *buffer,size_t size) {
  const char *p=(const char*)buffer;ULONGLONG deadline=GetTickCount64()+10000;
  while(size&&!cancelled()) {int n=send(s,p,(int)size,0);if(n>0){p+=n;size-=n;continue;}if(n<0&&WSAGetLastError()==WSAEWOULDBLOCK&&GetTickCount64()<deadline){Sleep(1);continue;}return 0;}return !size;
}
#include "gateway_diagnostics.h"
static void proxy_error(client *c,const mr_proxy_diagnostic *diagnostic,const char *stage,const char *reason,int lwip_error) {
  char response[1024],record[MR_PROXY_DIAGNOSTIC_SIZE];uint64_t now=(uint64_t)GetTickCount64();
  mr_proxy_diagnostic_format(record,diagnostic,stage,reason,now>=diagnostic->started_ms?now-diagnostic->started_ms:0,lwip_error);
  fputs(record,stderr);fflush(stderr);
  snprintf(response,sizeof(response),"HTTP/1.1 502 Bad Gateway\r\nConnection: close\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n%s\nNo fallback gateway was used.\n",reason);socket_send_all(c->local,response,strlen(response));
}
#include "gateway_dns.h"
static int split_authority(char *authority,char *host,size_t host_size,unsigned short *port,unsigned short default_port) {
  char *colon=strrchr(authority,':'),*end;size_t i;long value=default_port;
  if(colon){*colon=0;value=strtol(colon+1,&end,10);if(!*(colon+1)||*end)return 0;}
  if(value<1||value>65535||!*authority||strlen(authority)>=host_size)return 0;
  for(i=0;authority[i];i++)if(!((authority[i]>='a'&&authority[i]<='z')||(authority[i]>='A'&&authority[i]<='Z')||(authority[i]>='0'&&authority[i]<='9')||authority[i]=='.'||authority[i]=='-'))return 0;
  strcpy(host,authority);*port=(unsigned short)value;return 1;
}
static int remote_connected(struct netconn *conn) {
  int state;LOCK_TCPIP_CORE();state=conn->pcb.tcp?(int)conn->pcb.tcp->state:-1;UNLOCK_TCPIP_CORE();return state==ESTABLISHED?1:state<0?-1:0;
}
/* A plain proxy connection serves exactly one origin request. Explicit close on
 * both sides prevents Java HttpClient's proxy-only pool reusing it for another host. */
static int append_close_headers(char *out,int size,int used,const char *headers) {
  const char *rest=headers;
  if(used<=0||used>=size-3)return -1;
  while(*rest&&strncmp(rest,"\r\n",2)){
    const char *end=strstr(rest,"\r\n");size_t len;if(!end)return -1;len=(size_t)(end-rest);
    if(_strnicmp(rest,"Connection:",11)&&_strnicmp(rest,"Proxy-Connection:",17)&&_strnicmp(rest,"Proxy-Authorization:",20)){
      if(used+(int)len+3>=size)return -1;memcpy(out+used,rest,len);used+=(int)len;memcpy(out+used,"\r\n",2);used+=2;
    }rest=end+2;
  }
  if(used+2>=size)return -1;memcpy(out+used,"\r\n",3);return used+2;
}
static DWORD WINAPI serve_client(LPVOID pointer) {
  client *c=(client*)pointer;char header[MAX_HEADER+1],host[256]="",target[MAX_HEADER+1],method[16],version[24];
  char rewritten[MAX_HEADER+1],response_header[MAX_HEADER+1];int used=0,tunnel=0,header_bytes=0,local_closed=0,response_pending=1,response_used=0;unsigned short remote_port=0;ip_addr_t remote,local;err_t err;u_long nonblocking=1;ULONGLONG deadline;
  mr_proxy_diagnostic diagnostic={0};diagnostic.gateway=c->route->gateway;diagnostic.host=host;diagnostic.started_ms=(uint64_t)GetTickCount64();
  DWORD receive_timeout=5000;setsockopt(c->local,SOL_SOCKET,SO_RCVTIMEO,(const char*)&receive_timeout,sizeof(receive_timeout));
  while(used<MAX_HEADER) {int n=recv(c->local,header+used,1,0);if(n<=0)goto done;used+=n;header[used]=0;if(used>=4&&!memcmp(header+used-4,"\r\n\r\n",4))break;}
  if(used==MAX_HEADER){proxy_error(c,&diagnostic,"parse","Proxy header too large",MR_PROXY_LWIP_UNKNOWN);goto done;}
  if(sscanf(header,"%15s %16384s %23s",method,target,version)!=3){proxy_error(c,&diagnostic,"parse","Invalid proxy request",MR_PROXY_LWIP_UNKNOWN);goto done;}
  if(strcmp(version,"HTTP/1.1")&&strcmp(version,"HTTP/1.0")){proxy_error(c,&diagnostic,"parse","Unsupported HTTP version",MR_PROXY_LWIP_UNKNOWN);goto done;}
  if(!strcmp(method,"CONNECT")) {tunnel=1;if(!split_authority(target,host,sizeof(host),&remote_port,443)){proxy_error(c,&diagnostic,"parse","Invalid CONNECT authority",MR_PROXY_LWIP_UNKNOWN);goto done;}}
  else if((!strcmp(method,"GET")||!strcmp(method,"HEAD"))&&!strncmp(target,"http://",7)) {
    char *path=strchr(target+7,'/'),authority[256],*rest=strstr(header,"\r\n");size_t authority_size=path?(size_t)(path-(target+7)):strlen(target+7);
    if(authority_size>=sizeof(authority)||!rest){proxy_error(c,&diagnostic,"parse","Invalid HTTP proxy URL",MR_PROXY_LWIP_UNKNOWN);goto done;}
    memcpy(authority,target+7,authority_size);authority[authority_size]=0;
    if(!split_authority(authority,host,sizeof(host),&remote_port,80)){proxy_error(c,&diagnostic,"parse","Invalid HTTP authority",MR_PROXY_LWIP_UNKNOWN);goto done;}
    diagnostic.remote_port=remote_port;
    header_bytes=snprintf(rewritten,sizeof(rewritten),"%s %s %s\r\nConnection: close\r\n",method,path?path:"/",version);
    header_bytes=append_close_headers(rewritten,sizeof(rewritten),header_bytes,rest+2);
    if(header_bytes<=0||header_bytes>=MAX_HEADER){proxy_error(c,&diagnostic,"parse","Invalid HTTP header",MR_PROXY_LWIP_UNKNOWN);goto done;}
  } else {proxy_error(c,&diagnostic,"parse","Only CONNECT, HTTP GET and HEAD are supported",MR_PROXY_LWIP_UNKNOWN);goto done;}
  diagnostic.remote_port=remote_port;
  if(!c->route->configured||!c->route->mac_known){proxy_error(c,&diagnostic,"arp","Selected gateway did not answer ARP",MR_PROXY_LWIP_UNKNOWN);goto done;}
  if(!resolve_remote(c->route,host,&remote)){proxy_error(c,&diagnostic,"dns","No public IPv4 address for source",MR_PROXY_LWIP_UNKNOWN);goto done;}
  diagnostic.resolved=1;diagnostic.destination_ipv4=ntohl(ip_addr_get_ip4_u32(&remote));
  c->conn=netconn_new(NETCONN_TCP);if(!c->conn){proxy_error(c,&diagnostic,"tcp_open","Application TCP socket limit",MR_PROXY_LWIP_UNKNOWN);goto done;}
  ip_addr_set_ip4_u32(&local,htonl(virtual_host));
  for(;;) {
    LONG port=InterlockedIncrement(&next_port);if(port>60000){InterlockedExchange(&next_port,20000);continue;}
    c->port=(uint16_t)port;err=netconn_bind(c->conn,&local,c->port);if(err==ERR_OK)break;if(err!=ERR_USE){proxy_error(c,&diagnostic,"tcp_bind","Application TCP bind failed",err);goto done;}
  }
  port_routes[c->port]=c->route;
  err=netconn_bind_if(c->conn,netif_get_index(&c->route->netif));if(err!=ERR_OK){proxy_error(c,&diagnostic,"tcp_bind_interface","Application gateway binding failed",err);goto done;}
  netconn_set_nonblocking(c->conn,1);err=netconn_connect(c->conn,&remote,remote_port);
  if(err!=ERR_OK&&err!=ERR_INPROGRESS){proxy_error(c,&diagnostic,"tcp_connect","Selected gateway connection failed",err);goto done;}
  deadline=GetTickCount64()+5000;
  while(!cancelled()&&remote_connected(c->conn)==0&&GetTickCount64()<deadline)Sleep(5);
  if(cancelled()){proxy_error(c,&diagnostic,"tcp_connect","Selected gateway connection cancelled",MR_PROXY_LWIP_UNKNOWN);goto done;}
  if(remote_connected(c->conn)!=1){
    const char *reason=GetTickCount64()>=deadline?"Selected gateway TCP connection timed out":"Selected gateway connection closed before establishment";
    err=netconn_err(c->conn);proxy_error(c,&diagnostic,"tcp_connect",reason,err==ERR_OK||err==ERR_INPROGRESS?MR_PROXY_LWIP_UNKNOWN:err);goto done;
  }
  netconn_set_nonblocking(c->conn,0);netconn_set_recvtimeout(c->conn,10);netconn_set_sendtimeout(c->conn,1000);
  if(tunnel) {if(!socket_send_all(c->local,"HTTP/1.1 200 Connection Established\r\n\r\n",39))goto done;}
  else {size_t written=0;err=netconn_write_partly(c->conn,rewritten,(size_t)header_bytes,NETCONN_COPY,&written);if(err!=ERR_OK||written!=(size_t)header_bytes){fprintf(stderr,"HTTP_WRITE error=%d bytes=%zu/%d\n",err,written,header_bytes);goto done;}}
  ioctlsocket(c->local,FIONBIO,&nonblocking);deadline=GetTickCount64()+120000;
  fprintf(stderr,"CONNECTED gateway=%s source=%s:%u host=%s:%u\n",c->route->gateway,virtual_ip,c->port,host,remote_port);
  while(!cancelled()&&GetTickCount64()<deadline) {
    char buffer[65536];int n=local_closed?-1:recv(c->local,buffer,sizeof(buffer),0),received=0;
    if(n>0) {size_t written=0;if(!tunnel)goto done;err=netconn_write_partly(c->conn,buffer,(size_t)n,NETCONN_COPY,&written);if(debug_headers)fprintf(stderr,"LOCAL_WRITE gw=%s error=%d bytes=%zu/%d\n",c->route->gateway,err,written,n);if(err!=ERR_OK||written!=(size_t)n)break;deadline=GetTickCount64()+120000;}
    else if(n==0){local_closed=1;netconn_shutdown(c->conn,0,1);}else if(!local_closed&&WSAGetLastError()!=WSAEWOULDBLOCK)break;
    do {
      struct netbuf *buf=NULL;void *payload;u16_t length;
      err=netconn_recv(c->conn,&buf);
      if(err==ERR_TIMEOUT||err==ERR_WOULDBLOCK)break;
      if(err!=ERR_OK||!buf)goto done;
      do {
        const char *bytes;size_t remaining;
        netbuf_data(buf,&payload,&length);bytes=(const char*)payload;remaining=length;
        while(!tunnel&&response_pending&&remaining){
          if(response_used>=MAX_HEADER){netbuf_delete(buf);goto done;}
          response_header[response_used++]=*bytes++;remaining--;response_header[response_used]=0;
          if(response_used>=4&&!memcmp(response_header+response_used-4,"\r\n\r\n",4)){
            char *first=strstr(response_header,"\r\n");int status=0,first_size,out_size;
            if(!first||sscanf(response_header,"HTTP/%*s %d",&status)!=1||status<100||status==101){netbuf_delete(buf);goto done;}
            first_size=(int)(first-response_header);
            if(first_size>=MAX_HEADER-24){netbuf_delete(buf);goto done;}
            memcpy(rewritten,response_header,first_size);out_size=first_size;memcpy(rewritten+out_size,"\r\nConnection: close\r\n",21);out_size+=21;
            out_size=append_close_headers(rewritten,sizeof(rewritten),out_size,first+2);
            if(out_size<0||!socket_send_all(c->local,rewritten,(size_t)out_size)){netbuf_delete(buf);goto done;}
            response_used=0;if(status>=200){response_pending=0;if(!strcmp(method,"HEAD")){netbuf_delete(buf);goto done;}}
          }
        }
        if(remaining&&!socket_send_all(c->local,bytes,remaining)){netbuf_delete(buf);goto done;}received++;
      }while(netbuf_next(buf)>=0);
      netbuf_delete(buf);deadline=GetTickCount64()+120000;
    } while(received<128&&!cancelled());
  }
done:
  if(c->conn){netconn_delete(c->conn);}
  shutdown(c->local,SD_BOTH);closesocket(c->local);InterlockedDecrement(&connection_count);free(c);return 0;
}
static DWORD WINAPI accept_loop(LPVOID pointer) {
  profile *r=(profile*)pointer;
  while(!cancelled()&&!r->listener_stopping) {
    fd_set readset;struct timeval wait={0,200000};SOCKET s;client *c;HANDLE thread;
    SOCKET listener=r->listener;if(listener==INVALID_SOCKET)break;FD_ZERO(&readset);FD_SET(listener,&readset);if(select(0,&readset,NULL,NULL,&wait)<=0)continue;
    s=accept(listener,NULL,NULL);if(s==INVALID_SOCKET)continue;
    if(InterlockedIncrement(&connection_count)>MAX_CONNECTIONS){InterlockedDecrement(&connection_count);closesocket(s);continue;}
    c=(client*)calloc(1,sizeof(client));if(!c){InterlockedDecrement(&connection_count);closesocket(s);continue;}c->local=s;c->route=r;
    thread=CreateThread(NULL,0,serve_client,c,0,NULL);if(thread)CloseHandle(thread);else{free(c);closesocket(s);InterlockedDecrement(&connection_count);}
  }
  return 0;
}
#include "socks_gateway.c"
static int start_profile_listeners(profile *r) {
  struct sockaddr_in address;int size=sizeof(address);HANDLE first,second;
  r->listener=socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);r->socks_listener=socket(AF_INET,SOCK_STREAM,IPPROTO_TCP);
  if(r->listener==INVALID_SOCKET||r->socks_listener==INVALID_SOCKET)goto fail;
  memset(&address,0,sizeof(address));address.sin_family=AF_INET;address.sin_addr.s_addr=htonl(INADDR_LOOPBACK);
  if(bind(r->listener,(struct sockaddr*)&address,sizeof(address))||listen(r->listener,16)||getsockname(r->listener,(struct sockaddr*)&address,&size))goto fail;
  r->proxy_port=ntohs(address.sin_port);address.sin_port=0;size=sizeof(address);
  if(bind(r->socks_listener,(struct sockaddr*)&address,sizeof(address))||listen(r->socks_listener,32)||getsockname(r->socks_listener,(struct sockaddr*)&address,&size))goto fail;
  r->socks_port=ntohs(address.sin_port);first=CreateThread(NULL,0,accept_loop,r,0,NULL);if(!first)goto fail;CloseHandle(first);
  second=CreateThread(NULL,0,accept_socks_loop,r,0,NULL);if(!second)goto fail;CloseHandle(second);return 1;
fail:
  InterlockedExchange(&r->listener_stopping,1);
  if(r->listener!=INVALID_SOCKET)closesocket(r->listener);if(r->socks_listener!=INVALID_SOCKET)closesocket(r->socks_listener);
  r->listener=r->socks_listener=INVALID_SOCKET;return 0;
}
static int start_listeners(void) {int i;for(i=0;i<profile_count;i++)if(!start_profile_listeners(&profiles[i]))return 0;return 1;}
#include "gateway_profile_control.h"
static BOOL WINAPI console_control(DWORD type){(void)type;InterlockedExchange(&stopping,1);return TRUE;}
/* Offline fragment regression: these netifs have no physical output or sockets.
 * Exercise actual ip4_input -> bound UDP PCB, including zero UDP checksums. */
typedef struct reass_test_state {int route,received,error;u16_t expected;} reass_test_state;
static unsigned char reass_test_byte(int route,size_t offset){return (unsigned char)(route*41+offset*17);}
static err_t reass_test_output(struct netif *n,struct pbuf *p,const ip4_addr_t *d){(void)n;(void)p;(void)d;return ERR_IF;}
static err_t reass_test_netif_init(struct netif *n){n->name[0]='t';n->name[1]='g';n->mtu=1500;n->output=reass_test_output;return ERR_OK;}
static void reass_test_udp_recv(void *arg,struct udp_pcb *pcb,struct pbuf *p,const ip_addr_t *addr,u16_t port){
  reass_test_state *state=(reass_test_state*)arg;size_t i;(void)pcb;
  state->received++;
  if(p->tot_len!=state->expected||port!=53||ntohl(ip4_addr_get_u32(ip_2_ip4(addr)))!=0x08080808||ip_current_input_netif()->state!=state)state->error=1;
  for(i=0;i<p->tot_len;i++)if(pbuf_get_at(p,(u16_t)i)!=reass_test_byte(state->route,i)){state->error=1;break;}
  pbuf_free(p);
}
static struct pbuf *reass_test_fragment(unsigned char proto,u16_t id,const unsigned char *data,u16_t total,u16_t offset,u16_t count){
  struct pbuf *p=pbuf_alloc(PBUF_RAW,(u16_t)(20+count),PBUF_RAM);unsigned char *b;if(!p)return NULL;b=(unsigned char*)p->payload;
  memset(b,0,20);b[0]=0x45;put16(b+2,(u16_t)(20+count));put16(b+4,id);put16(b+6,(u16_t)(offset/8+((offset+count<total)?0x2000:0)));b[8]=64;b[9]=proto;
  put32(b+12,0x08080808);put32(b+16,0xc0a8014a);memcpy(b+20,data+offset,count);put16(b+10,ntohs(inet_chksum(b,20)));return p;
}
static int reass_test_direct_result(struct pbuf *p,unsigned char proto,const unsigned char *data,u16_t length){
  unsigned char *out;int ok;if(!p)return 0;out=(unsigned char*)malloc((size_t)length+20);if(!out){pbuf_free(p);return 0;}
  ok=p->tot_len==length+20&&pbuf_copy_partial(p,out,(u16_t)(length+20),0)==length+20&&out[9]==proto&&!memcmp(out+20,data,length);free(out);pbuf_free(p);return ok;
}
static void reass_test_expire(void){int i;for(i=0;i<=IP_REASS_MAXAGE;i++)ip_reass_tmr();}
static int fragment_self_test(void){
  struct netif interfaces[4];struct udp_pcb *pcbs[4]={0};reass_test_state states[4];unsigned char *messages[4]={0};ip4_addr_t local,mask,gateway;ip_addr_t bind_ip;
  int i,j,error=0;size_t offset;unsigned char a[16],b[16];struct pbuf *p,*result;
  lwip_init();memset(interfaces,0,sizeof(interfaces));memset(states,0,sizeof(states));
  IP4_ADDR(&local,192,168,1,74);IP4_ADDR(&mask,255,255,255,0);IP4_ADDR(&gateway,192,168,1,1);ip_addr_copy_from_ip4(bind_ip,local);
  for(i=0;i<4;i++){
    states[i].route=i+1;states[i].expected=65497;
    if(!netif_add(&interfaces[i],&local,&mask,&gateway,&states[i],reass_test_netif_init,ip4_input)){error=1;goto done;}netif_set_up(&interfaces[i]);
    pcbs[i]=udp_new();if(!pcbs[i]||udp_bind(pcbs[i],&bind_ip,(u16_t)(30100+i))!=ERR_OK){error=2;goto done;}
    udp_bind_netif(pcbs[i],&interfaces[i]);udp_recv(pcbs[i],reass_test_udp_recv,&states[i]);
    messages[i]=(unsigned char*)malloc(65505);if(!messages[i]){error=3;goto done;}
    put16(messages[i],53);put16(messages[i]+2,(u16_t)(30100+i));put16(messages[i]+4,65505);put16(messages[i]+6,0);
    for(j=0;j<65497;j++)messages[i][j+8]=reass_test_byte(i+1,(size_t)j);
  }
  /* Four near-maximum UDP datagrams, same src/dst/protocol/ID, 45 fragments
   * each, alternate incoming gateway netifs. Data must reach its own PCB. */
  for(offset=0;offset<65505;offset+=1480)for(i=0;i<4;i++){
    u16_t count=(u16_t)((65505-offset<1480)?65505-offset:1480);
    p=reass_test_fragment(17,0x3344,messages[i],65505,(u16_t)offset,count);if(!p||ip4_input(p,&interfaces[i])!=ERR_OK){error=4;goto done;}
  }
  for(i=0;i<4;i++)if(states[i].received!=1||states[i].error){error=5;goto done;}
  for(i=0;i<16;i++){a[i]=(unsigned char)(i+10);b[i]=(unsigned char)(i+100);}
  /* Identical ID and interface with independent TCP/UDP protocol keys. */
  ip_data.current_input_netif=&interfaces[0];
  p=reass_test_fragment(6,0x4455,a,16,0,8);if(!p||ip4_reass(p)){error=6;goto done;}
  p=reass_test_fragment(17,0x4455,b,16,0,8);if(!p||ip4_reass(p)){error=7;goto done;}
  p=reass_test_fragment(6,0x4455,a,16,8,8);result=p?ip4_reass(p):NULL;if(!reass_test_direct_result(result,6,a,16)){error=8;goto done;}
  p=reass_test_fragment(17,0x4455,b,16,8,8);result=p?ip4_reass(p):NULL;if(!reass_test_direct_result(result,17,b,16)){error=9;goto done;}
  /* Exhaust all reassembly entries with the same src/dst/ID. A different
   * protocol/netif must still permit oldest-entry eviction and completion. */
  for(i=0;i<16;i++){
    ip_data.current_input_netif=&interfaces[i%4];p=reass_test_fragment((unsigned char)(40+i),0x5566,a,24,8,8);
    if(!p||ip4_reass(p)){error=10;goto done;}
  }
  ip_data.current_input_netif=&interfaces[1];p=reass_test_fragment(101,0x5566,b,16,0,8);if(!p||ip4_reass(p)){error=11;goto done;}
  p=reass_test_fragment(101,0x5566,b,16,8,8);result=p?ip4_reass(p):NULL;if(!reass_test_direct_result(result,101,b,16)){error=12;goto done;}
done:
  ip_data.current_input_netif=NULL;reass_test_expire();
  for(i=0;i<4;i++){if(pcbs[i])udp_remove(pcbs[i]);if(interfaces[i].input)netif_remove(&interfaces[i]);free(messages[i]);}
  if(error)fprintf(stderr,"FRAGMENT_SELF_TEST_FAILED step=%d\n",error);
  else puts("FRAGMENT_SELF_TEST_OK four isolated 65497-byte zero-checksum UDP replies; protocol collision; oldest eviction; no network handles");
  return error;
}
static int self_test(void) {
  uint32_t ip;unsigned char f[60]={0};uint16_t op;uint32_t sender,target;char host[256],authority[256];unsigned short port;
  if(!parse_ip("192.168.1.70",&ip)||ip!=0xc0a80146||parse_ip("256.1.1.1",&ip)||parse_ip("host;evil",&ip))return 1;
  virtual_host=0xc0a80146;memset(adapter_mac,1,6);memset(f+6,2,6);memset(f+22,2,6);put16(f+12,0x0806);put16(f+14,1);put16(f+16,0x0800);f[18]=6;f[19]=4;put16(f+20,2);put32(f+28,virtual_host);
  if(!arp_packet(f,sizeof(f),&op,&sender,&target)||!is_conflict(f,sizeof(f)))return 2;
  put32(f+28,0);put32(f+38,virtual_host);if(!is_conflict(f,sizeof(f)))return 3;
  memcpy(f+22,adapter_mac,6);memcpy(f+6,adapter_mac,6);if(is_conflict(f,sizeof(f)))return 4;
  strcpy(authority,"fsn1-speed.hetzner.com:443");if(!split_authority(authority,host,sizeof(host),&port,80)||port!=443)return 5;
  strcpy(authority,"host:0");if(split_authority(authority,host,sizeof(host),&port,80))return 6;
  if(public_ip(0xc0a80101)||!public_ip(0x4e2eaa02))return 7;
  {char out[128]="HTTP/1.1 200 OK\r\nConnection: close\r\n";int n=append_close_headers(out,sizeof(out),(int)strlen(out),"Connection: keep-alive\r\nProxy-Connection: keep-alive\r\nContent-Length: 3\r\n\r\n");if(n<0||strstr(out,"keep-alive")||!strstr(out,"Content-Length: 3"))return 8;if(append_close_headers(out,sizeof(out),127,"\r\n")>=0)return 9;}
  if(socks_self_test())return 10;
  if(fragment_self_test())return 11;
  if(profile_control_self_test())return 12;
  if(gateway_dns_self_test())return 13;
  if(mr_dns_broker_protocol_self_test())return 14;
  puts("MANAGED_DNS_SELF_TEST_OK authenticated framing, public numeric egress, no network handles");
  puts("SELF_TEST_OK no network handles, no routing changes");return 0;
}
static int run_main(int argc,char **argv) {
  WSADATA wsa;char error[1024]={0},gateways[1024]="192.168.1.1,192.168.1.2,192.168.1.3,192.168.1.4",mutex_name[128];int i;char *token,*context;
  DWORD parent_pid=0;WCHAR dll_path[MAX_PATH];HANDLE rx;
  InitializeCriticalSection(&ready_lock);gateway_dns_init();
  strcpy(virtual_ip,"192.168.1.70");
  for(i=1;i<argc;i++) {
    if(!strcmp(argv[i],"--self-test"))return self_test();
    if(!strcmp(argv[i],"--debug-headers")){debug_headers=1;continue;}
    if(!strcmp(argv[i],"--managed-dns")){if(managed_dns_required)return 2;managed_dns_required=1;continue;}
    if(i+1>=argc){fprintf(stderr,"Missing option argument\n");return 2;}
    if(!strcmp(argv[i],"--virtual-ip")){if(strlen(argv[i+1])>=sizeof(virtual_ip)){fatal("Virtual IPv4 argument too long");return 2;}strcpy(virtual_ip,argv[++i]);}
    else if(!strcmp(argv[i],"--gateways")){if(!*argv[i+1]||strlen(argv[i+1])>=sizeof(gateways)||argv[i+1][0]==','||strstr(argv[i+1],",,")||argv[i+1][strlen(argv[i+1])-1]==','){fatal("Invalid gateway list");return 2;}strcpy(gateways,argv[++i]);}
    else if(!strcmp(argv[i],"--ready-file")){if(strlen(argv[i+1])>=sizeof(ready_path)){fatal("Ready file path too long");return 2;}strcpy(ready_path,argv[++i]);}
    else if(!strcmp(argv[i],"--parent-pid")){char *end;unsigned long value;const char *pid=argv[++i];value=strtoul(pid,&end,10);if(!*pid||*end||!value||value==ULONG_MAX){fatal("Invalid parent PID");return 2;}parent_pid=(DWORD)value;}
    else {fprintf(stderr,"Unknown option\n");return 2;}
  }
  if(!parse_ip(virtual_ip,&virtual_host)){fatal("Invalid application virtual IPv4");return 2;}
  for(token=strtok_s(gateways,",",&context);token;token=strtok_s(NULL,",",&context)) {
    if(profile_count>=MAX_CONFIGURED_PROFILES||!parse_ip(token,&profiles[profile_count].gateway_host)||profiles[profile_count].gateway_host==virtual_host){fatal("Invalid gateway list");return 2;}
    for(i=0;i<profile_count;i++)if(profiles[i].gateway_host==profiles[profile_count].gateway_host){fatal("Duplicate gateway");return 2;}
    strcpy(profiles[profile_count].gateway,token);profiles[profile_count].listener=INVALID_SOCKET;profiles[profile_count].socks_listener=INVALID_SOCKET;profiles[profile_count].configured=1;InterlockedIncrement(&profile_count_raw);
  }
  if(!profile_count){fatal("No gateways configured");return 2;}
  if(!initialize_managed_control()){fatal("Managed DNS configuration is missing or invalid; no plaintext fallback is permitted");return 2;}
  snprintf(mutex_name,sizeof(mutex_name),"Global\\MultiRoutePcap_%s",virtual_ip);instance_mutex=CreateMutexA(NULL,TRUE,mutex_name);
  if(!instance_mutex||GetLastError()==ERROR_ALREADY_EXISTS){fatal("Application virtual IP is already owned by another MultiRoute helper");return 3;}
  if(parent_pid){parent_handle=OpenProcess(SYNCHRONIZE,FALSE,parent_pid);if(!parent_handle){fatal("Parent application is no longer running");return 3;}}
  InitializeCriticalSection(&tx_lock);SetConsoleCtrlHandler(console_control,TRUE);
  GetSystemDirectoryW(dll_path,MAX_PATH);wcscat_s(dll_path,MAX_PATH,L"\\Npcap");
  SetDefaultDllDirectories(LOAD_LIBRARY_SEARCH_SYSTEM32|LOAD_LIBRARY_SEARCH_USER_DIRS);AddDllDirectory(dll_path);
  if(!LoadLibraryExW(L"wpcap.dll",NULL,LOAD_LIBRARY_SEARCH_SYSTEM32|LOAD_LIBRARY_SEARCH_USER_DIRS)){fatal("Npcap runtime is not installed or its x64 DLL could not be loaded; no network configuration was changed");return 4;}
  if(WSAStartup(MAKEWORD(2,2),&wsa)){fatal("Winsock startup failed");return 4;}
  if(!choose_adapter(error,sizeof(error))){report_topology_error(error);fatal(error);return 4;}
  if(!open_capture(error,sizeof(error))){fatal(*error?error:"LAN setup failed");return 4;}
  fprintf(stderr,"ACD probing %s on %s; no Windows IP/route configuration\n",virtual_ip,interface_name);
  if(!address_claim()){fatal("Application virtual IP is in use, probed by another host, or request cancelled; no address claimed");return 5;}
  if(!initialize_stack()){fatal("Application TCP/IP stack initialization failed");return 6;}
  rx=CreateThread(NULL,0,receive_loop,NULL,0,NULL);if(!rx||!start_listeners()||!mr_dns_tunnel_start()){fatal("Application loopback proxy startup failed");return 7;}
  ready_result(NULL);fprintf(stderr,"READY virtual_ip=%s interface=%s profiles=%d application_routes_only=true\n",virtual_ip,interface_name,profile_count);
  for(i=0;i<profile_count;i++){char mac[32];format_mac(mac,profiles[i].gateway_mac);fprintf(stderr,"GATEWAY %s proxy=127.0.0.1:%u mac=%s available=%d\n",profiles[i].gateway,profiles[i].proxy_port,mac,profiles[i].mac_known);}
  while(!cancelled()){Sleep(250);if(!cancelled()){poll_profile_control();monitor_topology();ready_result(NULL);}}
  for(i=0;i<profile_count;i++){if(profiles[i].listener!=INVALID_SOCKET)closesocket(profiles[i].listener);if(profiles[i].socks_listener!=INVALID_SOCKET)closesocket(profiles[i].socks_listener);}
  mr_dns_tunnel_stop();
  if(WaitForSingleObject(rx,2000)!=WAIT_OBJECT_0)ExitProcess(0);CloseHandle(rx);
  {ULONGLONG until=GetTickCount64()+3000;while((connection_count||mr_dns_tunnel_connections)&&GetTickCount64()<until)Sleep(10);if(connection_count||mr_dns_tunnel_connections){ExitProcess(0);}}
  if(capture)pcap_close(capture);if(transmit)pcap_close(transmit);
  if(parent_handle)CloseHandle(parent_handle);ReleaseMutex(instance_mutex);CloseHandle(instance_mutex);WSACleanup();return 0;
}
int wmain(int argc,WCHAR **wide_argv) {
  char **argv=(char**)calloc((size_t)argc,sizeof(char*));int i,result;
  if(!argv)return 2;
  for(i=0;i<argc;i++){
    int size=WideCharToMultiByte(CP_UTF8,WC_ERR_INVALID_CHARS,wide_argv[i],-1,NULL,0,NULL,NULL);
    if(!size||(argv[i]=(char*)malloc((size_t)size))==NULL)return 2;
    if(!WideCharToMultiByte(CP_UTF8,WC_ERR_INVALID_CHARS,wide_argv[i],-1,argv[i],size,NULL,NULL))return 2;
  }
  result=run_main(argc,argv);for(i=0;i<argc;i++)free(argv[i]);free(argv);return result;
}
