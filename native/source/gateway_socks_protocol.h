/* Pure SOCKS framing and lookup dispatch. Include after gateway_dns.h so the
 * same bounded hostname/public-address validators protect CONNECT and RESOLVE. */
#ifndef MULTIROUTE_SOCKS_PROTOCOL_H
#define MULTIROUTE_SOCKS_PROTOCOL_H
#define SOCKS_MAX_DATAGRAM 65507
#define SOCKS_MAX_PAYLOAD (SOCKS_MAX_DATAGRAM-10)
#define SOCKS_COMMAND_RESOLVE 0xf0
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
  if(a->type==1){if(n<7)return 1;a->ip=((uint32_t)p[1]<<24)|((uint32_t)p[2]<<16)|((uint32_t)p[3]<<8)|p[4];index=5;}
  else if(a->type==3){if(n<2)return 1;index=(size_t)p[1]+2;if(n<index+2||!socks_domain_valid(p+2,p[1]))return 1;memcpy(a->domain,p+2,p[1]);a->domain[p[1]]=0;}
  else return 8;
  a->port=(unsigned short)(((unsigned)p[index]<<8)|p[index+1]);a->bytes=index+2;return 0;
}
static int socks_parse_datagram(const unsigned char *p,size_t n,socks_address *a,size_t *offset) {
  int error;if(n<4||p[0]||p[1]||p[2])return 1;
  error=socks_parse_address(p+3,n-3,a);if(error)return error;
  *offset=a->bytes+3;if(!a->port||n-*offset>SOCKS_MAX_PAYLOAD)return 1;return 0;
}
static int socks_command_supported(unsigned char command) {
  return command==1||command==3||command==SOCKS_COMMAND_RESOLVE;
}
static void socks_reply_bytes(unsigned char out[10],unsigned char error,uint32_t ip,unsigned short port) {
  out[0]=5;out[1]=error;out[2]=0;out[3]=1;
  out[4]=(unsigned char)(ip>>24);out[5]=(unsigned char)(ip>>16);out[6]=(unsigned char)(ip>>8);out[7]=(unsigned char)ip;
  out[8]=(unsigned char)(port>>8);out[9]=(unsigned char)port;
}
typedef int (*socks_gateway_lookup_fn)(void *selected_route,const char *host,uint32_t *address);
/* The lookup callback is the selected private gateway's resolver, never the
 * system resolver. This command cannot create a destination TCP connection. */
static unsigned char socks_resolve_request(void *selected_route,int available,const socks_address *address,
    socks_gateway_lookup_fn lookup,uint32_t *result) {
  char normalized[254];uint32_t resolved=0;
  if(!address||!result||!lookup)return 1;
  *result=0;
  if(address->type!=3)return 8;
  if(address->port)return 1;
  if(!selected_route||!available)return 3;
  if(!memchr(address->domain,0,sizeof(address->domain))||!mr_dns_host(address->domain,normalized))return 4;
  if(!lookup(selected_route,normalized,&resolved)||!mr_dns_public(resolved))return 4;
  *result=resolved;return 0;
}
#endif
