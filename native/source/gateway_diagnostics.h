/* Bounded, single-line proxy diagnostics. No raw request, URL or headers belong here.
 * This header has no networking/platform dependencies so its fixtures run offline. */
#ifndef MR_GATEWAY_DIAGNOSTICS_H
#define MR_GATEWAY_DIAGNOSTICS_H
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#define MR_PROXY_DIAGNOSTIC_SIZE 1536
#define MR_PROXY_LWIP_UNKNOWN INT_MAX
typedef struct mr_proxy_diagnostic {
  const char *gateway, *host;
  uint32_t destination_ipv4; /* Host byte order, only meaningful when resolved. */
  unsigned int remote_port;
  uint64_t started_ms;
  int resolved;
} mr_proxy_diagnostic;

/* Accept only the already-parsed ASCII hostname, never a URL or partial authority. */
static const char *mr_proxy_safe_host(const char *host) {
  size_t i;
  if(!host)return "";
  for(i=0;host[i];i++) {
    unsigned char c=(unsigned char)host[i];
    if(i>=253||!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||
        (c>='0'&&c<='9')||c=='.'||c=='-'))return "";
  }
  return host;
}
static const char *mr_proxy_safe_gateway(const char *gateway) {
  unsigned int octets=0,value=0,digits=0;size_t i;
  if(!gateway||!*gateway)return "";
  for(i=0;gateway[i];i++) {
    unsigned char c=(unsigned char)gateway[i];
    if(i>=15)return "";
    if(c=='.'){if(!digits||++octets>3)return "";digits=0;value=0;}
    else if(c>='0'&&c<='9'){value=value*10+c-'0';if(++digits>3||value>255)return "";}
    else return "";
  }
  return octets==3&&digits?gateway:"";
}
/* Escape JSON controls and quotes; copy complete UTF-8 sequences within the bound.
 * Invalid bytes are escaped individually, so diagnostics themselves remain UTF-8. */
static void mr_proxy_escape(char *out,const char *input,size_t limit) {
  static const char hex[]="0123456789abcdef";
  size_t i=0,used=0;
  if(!input)input="";
  while(i<limit&&input[i]) {
    unsigned char c=(unsigned char)input[i];size_t bytes=1,j;
    if(c>=0xc2&&c<=0xdf)bytes=2;
    else if(c>=0xe0&&c<=0xef)bytes=3;
    else if(c>=0xf0&&c<=0xf4)bytes=4;
    if(bytes>1) {
      int valid=1;
      if(i+bytes>limit)break;
      for(j=1;j<bytes;j++){unsigned char next=(unsigned char)input[i+j];if(next<0x80||next>0xbf){valid=0;break;}}
      if(valid&&((c==0xe0&&(unsigned char)input[i+1]<0xa0)||
          (c==0xed&&(unsigned char)input[i+1]>=0xa0)||
          (c==0xf0&&(unsigned char)input[i+1]<0x90)||
          (c==0xf4&&(unsigned char)input[i+1]>=0x90)))valid=0;
      if(valid){memcpy(out+used,input+i,bytes);used+=bytes;i+=bytes;continue;}
    }
    if(c=='"'||c=='\\'){out[used++]='\\';out[used++]=(char)c;}
    else if(c<32||c>=127){memcpy(out+used,"\\u00",4);used+=4;out[used++]=hex[c>>4];out[used++]=hex[c&15];}
    else out[used++]=(char)c;
    i++;
  }
  out[used]=0;
}
static void mr_proxy_diagnostic_format(char out[MR_PROXY_DIAGNOSTIC_SIZE],
    const mr_proxy_diagnostic *diagnostic,const char *stage,const char *reason,
    uint64_t elapsed_ms,int lwip_error) {
  char escaped_stage[32*6+1],escaped_reason[128*6+1],destination[16]="",error[16];
  const char *gateway="",*host="";unsigned int port=0;
  mr_proxy_escape(escaped_stage,stage,32);mr_proxy_escape(escaped_reason,reason,128);
  if(diagnostic) {
    gateway=mr_proxy_safe_gateway(diagnostic->gateway);host=mr_proxy_safe_host(diagnostic->host);
    port=diagnostic->remote_port<=65535?diagnostic->remote_port:0;
    if(diagnostic->resolved){uint32_t ip=diagnostic->destination_ipv4;
      snprintf(destination,sizeof(destination),"%u.%u.%u.%u",(unsigned int)(ip>>24),
          (unsigned int)((ip>>16)&255),(unsigned int)((ip>>8)&255),(unsigned int)(ip&255));}
  }
  if(lwip_error==MR_PROXY_LWIP_UNKNOWN)memcpy(error,"null",5);
  else snprintf(error,sizeof(error),"%d",lwip_error);
  snprintf(out,MR_PROXY_DIAGNOSTIC_SIZE,
      "TUNNEL_ERROR {\"gateway\":\"%s\",\"host\":\"%s\",\"destination_ipv4\":\"%s\","
      "\"remote_port\":%u,\"stage\":\"%s\",\"reason\":\"%s\",\"elapsed_ms\":%llu,\"lwip_error\":%s}\n",
      gateway,host,destination,port,escaped_stage,escaped_reason,(unsigned long long)elapsed_ms,error);
}
#endif
