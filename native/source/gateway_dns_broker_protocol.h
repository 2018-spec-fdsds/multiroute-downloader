/* Pure, bounded framing for the private managed-DNS broker and numeric tunnel.
 * Include after gateway_dns.h's public-address and hostname validators. */
#ifndef MULTIROUTE_DNS_BROKER_PROTOCOL_H
#define MULTIROUTE_DNS_BROKER_PROTOCOL_H
#include <limits.h>
#include <stdint.h>
#include <stddef.h>
#include <string.h>
#define MR_DNS_KEY_BYTES 64
#define MR_DNS_BROKER_HEADER 100
#define MR_DNS_TUNNEL_HEADER 92
#define MR_DNS_TUNNEL_LIMIT 8
#define MR_DNS_TOTAL_MS 8000
#define MR_DNS_MAX_ERROR 512
typedef struct mr_dns_policy {
  uint64_t revision;
  unsigned short broker_port;
  char key[MR_DNS_KEY_BYTES+1];
} mr_dns_policy;
typedef struct mr_dns_tunnel_request {
  char gateway[16];
  uint32_t destination;
  unsigned short port;
} mr_dns_tunnel_request;
static int mr_dns_decimal(const char *text,uint64_t maximum,uint64_t *out) {
  uint64_t value=0;const unsigned char *p=(const unsigned char*)text;
  if(!p||!*p||(*p=='0'&&p[1]))return 0;
  while(*p){unsigned digit=(unsigned)(*p++-'0');if(digit>9||digit>maximum||value>(maximum-digit)/10)return 0;value=value*10+digit;}
  *out=value;return 1;
}
static int mr_dns_key_valid(const char *key) {
  size_t i;if(!key||strlen(key)!=MR_DNS_KEY_BYTES)return 0;
  for(i=0;i<MR_DNS_KEY_BYTES;i++)if(!((key[i]>='0'&&key[i]<='9')||(key[i]>='a'&&key[i]<='f')||(key[i]>='A'&&key[i]<='F')))return 0;
  return 1;
}
static int mr_dns_policy_fields(const char *port,const char *key,const char *revision,mr_dns_policy *out) {
  uint64_t p,r;mr_dns_policy next;
  if(!out||!mr_dns_decimal(port,65535,&p)||!p||!mr_dns_decimal(revision,INT64_MAX,&r)||!r||!mr_dns_key_valid(key))return 0;
  memset(&next,0,sizeof(next));next.broker_port=(unsigned short)p;next.revision=r;memcpy(next.key,key,MR_DNS_KEY_BYTES);*out=next;return 1;
}
static int mr_dns_same_key(const unsigned char *a,const char *b) {
  size_t i;unsigned difference=0;for(i=0;i<MR_DNS_KEY_BYTES;i++)difference|=(unsigned)(a[i]^(unsigned char)b[i]);return difference==0;
}
static int mr_dns_same_policy(const mr_dns_policy *a,const mr_dns_policy *b) {
  return a->revision==b->revision&&a->broker_port==b->broker_port&&mr_dns_same_key((const unsigned char*)a->key,b->key);
}
static int mr_dns_padded_gateway(const unsigned char *in,char out[16]) {
  size_t n=0,i;unsigned part=0,digits=0,dots=0;
  while(n<16&&in[n])n++;
  if(n<7||n>15)return 0;
  for(i=n;i<16;i++)if(in[i])return 0;
  for(i=0;i<n;i++){
    unsigned char c=in[i];
    if(c=='.'){if(!digits||dots==3)return 0;dots++;part=0;digits=0;}
    else if(c>='0'&&c<='9'){
      if((digits==1&&part==0)||++digits>3)return 0;
      part=part*10+(unsigned)(c-'0');if(part>255)return 0;
    }else return 0;
  }
  if(dots!=3||!digits)return 0;
  memcpy(out,in,n);out[n]=0;return 1;
}
static int mr_dns_tunnel_decode(const unsigned char *input,size_t size,const mr_dns_policy *policy,mr_dns_tunnel_request *out) {
  mr_dns_tunnel_request next;uint32_t ip;unsigned short port;
  if(!input||!out||!policy||!policy->revision||size!=MR_DNS_TUNNEL_HEADER||memcmp(input,"MRDNT1",6)||!mr_dns_same_key(input+6,policy->key))return 0;
  memset(&next,0,sizeof(next));if(!mr_dns_padded_gateway(input+70,next.gateway))return 0;
  ip=mr_dns_u32(input+86);port=mr_dns_u16(input+90);
  if(!mr_dns_public(ip)||!port)return 0;
  next.destination=ip;next.port=port;*out=next;return 1;
}
static size_t mr_dns_broker_encode(unsigned char *output,size_t capacity,const mr_dns_policy *policy,const char *gateway,unsigned remaining,const unsigned char *query,size_t length) {
  unsigned char padded[16]={0};char checked[16];size_t n;
  if(!output||!policy||!policy->revision||!query||length<12||length>4096||capacity<MR_DNS_BROKER_HEADER+length||!remaining||remaining>MR_DNS_TOTAL_MS)return 0;
  n=strlen(gateway);if(n>15)return 0;memcpy(padded,gateway,n);if(!mr_dns_padded_gateway(padded,checked))return 0;
  memcpy(output,"MRDNS2",6);memcpy(output+6,policy->key,MR_DNS_KEY_BYTES);memcpy(output+70,padded,16);
  for(n=0;n<8;n++)output[86+n]=(unsigned char)(policy->revision>>(56-8*n));
  output[94]=(unsigned char)(remaining>>24);output[95]=(unsigned char)(remaining>>16);output[96]=(unsigned char)(remaining>>8);output[97]=(unsigned char)remaining;
  mr_dns_put16(output+98,(uint16_t)length);memcpy(output+100,query,length);return MR_DNS_BROKER_HEADER+length;
}
static int mr_dns_broker_protocol_self_test(void) {
  mr_dns_policy p,q;mr_dns_tunnel_request decoded;unsigned char packet[128],query[12]={0};char key[65];uint64_t number;size_t length;
  memset(key,'a',64);key[64]=0;
  if(!mr_dns_policy_fields("4321",key,"1",&p)||mr_dns_policy_fields("0",key,"1",&q)||mr_dns_policy_fields("4321",key,"0",&q))return 1;
  if(mr_dns_decimal("18446744073709551616",UINT64_MAX,&number)||mr_dns_decimal("01",100,&number)||mr_dns_decimal("-1",100,&number))return 2;
  length=mr_dns_broker_encode(packet,sizeof(packet),&p,"192.168.1.4",8000,query,sizeof(query));
  if(length!=112||memcmp(packet,"MRDNS2",6)||packet[93]!=1||mr_dns_u32(packet+94)!=8000||mr_dns_u16(packet+98)!=12)return 3;
  memcpy(packet,"MRDNT1",6);packet[86]=1;packet[87]=1;packet[88]=1;packet[89]=1;mr_dns_put16(packet+90,443);
  if(!mr_dns_tunnel_decode(packet,92,&p,&decoded)||strcmp(decoded.gateway,"192.168.1.4")||decoded.destination!=0x01010101U||decoded.port!=443)return 4;
  packet[6]^=1;if(mr_dns_tunnel_decode(packet,92,&p,&decoded))return 5;packet[6]^=1;
  packet[85]=1;if(mr_dns_tunnel_decode(packet,92,&p,&decoded))return 6;packet[85]=0;
  packet[86]=127;if(mr_dns_tunnel_decode(packet,92,&p,&decoded))return 7;
  return 0;
}
#endif
