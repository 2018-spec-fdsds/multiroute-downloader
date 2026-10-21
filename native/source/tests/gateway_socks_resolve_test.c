/* Pure SOCKS RESOLVE regression: no sockets, sessions, OS DNS or packet capture. */
#define _CRT_SECURE_NO_WARNINGS
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#define MR_DNS_PARSER_ONLY
#include "../gateway_dns.h"
#include "../gateway_socks_protocol.h"
static unsigned checks,lookup_calls;
static int lookup_ok=1;
static uint32_t lookup_ip;
static const char *expected_host="example.com";
static void *expected_route;
static void check(int valid){checks++;assert(valid);}
static int lookup(void *route,const char *host,uint32_t *address){
  lookup_calls++;check(route==expected_route);check(!strcmp(host,expected_host));*address=lookup_ip;return lookup_ok;
}
static uint32_t u32(const unsigned char *p){return ((uint32_t)p[0]<<24)|((uint32_t)p[1]<<16)|((uint32_t)p[2]<<8)|p[3];}
static size_t domain_request(unsigned char *out,const char *host,unsigned short port){
  size_t n=strlen(host);assert(n<=255);out[0]=3;out[1]=(unsigned char)n;memcpy(out+2,host,n);out[n+2]=(unsigned char)(port>>8);out[n+3]=(unsigned char)port;return n+4;
}
static void failure(socks_address *address,void *route,int available,unsigned char expected){
  uint32_t result=0xffffffffU;unsigned before=lookup_calls;unsigned char reply[10];
  check(socks_resolve_request(route,available,address,lookup,&result)==expected);
  check(result==0);check(lookup_calls==before);
  socks_reply_bytes(reply,expected,result,0);check(reply[0]==5&&reply[1]==expected&&reply[2]==0&&reply[3]==1);
  check(u32(reply+4)==0&&reply[8]==0&&reply[9]==0);
}
int main(void){
  unsigned char request[260],reply[10],ipv4[]={1,8,8,8,8,0,0};socks_address address;size_t n,offset;int routes[4]={1,2,3,4};uint32_t result;
  check(gateway_dns_self_test()==0);
  check(socks_command_supported(1)&&socks_command_supported(3)&&socks_command_supported(SOCKS_COMMAND_RESOLVE));
  check(!socks_command_supported(2)&&!socks_command_supported(0xf1)&&!socks_command_supported(0));
  n=domain_request(request,"example.com",0);
  for(size_t i=0;i<n;i++)check(socks_parse_address(request,i,&address)!=0);
  check(socks_parse_address(request,n,&address)==0&&address.type==3&&address.port==0&&address.bytes==n);
  for(unsigned i=0;i<4;i++){
    expected_route=&routes[i];lookup_ip=0x5db8d822U+i;unsigned before=lookup_calls;
    check(socks_resolve_request(expected_route,1,&address,lookup,&result)==0);check(lookup_calls==before+1);check(result==lookup_ip);
    socks_reply_bytes(reply,0,result,0);check(reply[0]==5&&reply[1]==0&&reply[2]==0&&reply[3]==1);
    check(u32(reply+4)==lookup_ip&&reply[8]==0&&reply[9]==0);
  }
  expected_route=&routes[0];address.port=53;failure(&address,expected_route,1,1);address.port=0;
  failure(&address,expected_route,0,3);failure(&address,NULL,1,3);
  check(socks_parse_address(ipv4,sizeof(ipv4),&address)==0);failure(&address,expected_route,1,8);
  request[0]=4;check(socks_parse_address(request,20,&address)==8);
  for(size_t i=0;i<4;i++){
    const char *bad[] = {"http://source.invalid/", "example.com\r\nInjected", "user@host", "a b"};
    n=domain_request(request,bad[i],0);check(socks_parse_address(request,n,&address)!=0);
  }
  n=domain_request(request,"example.com",0);check(socks_parse_address(request,n,&address)==0);
  strcpy(address.domain,"a..b");failure(&address,expected_route,1,4);
  memset(address.domain,'a',sizeof(address.domain));failure(&address,expected_route,1,4);
  memset(address.domain,'a',255);address.domain[255]=0;failure(&address,expected_route,1,4);
  strcpy(address.domain,"example.com");lookup_ok=0;result=1;check(socks_resolve_request(expected_route,1,&address,lookup,&result)==4&&result==0);lookup_ok=1;
  {
    const uint32_t forbidden[]={0,0x0a000001U,0x7f000001U,0xa9fe0001U,0xac100001U,0xc0a80101U,0x64400001U,0xc0000201U,0xc6336401U,0xcb007101U,0xc6120001U,0xe0000001U,0xffffffffU};
    for(size_t i=0;i<sizeof(forbidden)/sizeof(forbidden[0]);i++){lookup_ip=forbidden[i];result=1;check(socks_resolve_request(expected_route,1,&address,lookup,&result)==4&&result==0);}
  }
  strcpy(address.domain,"EXAMPLE.COM.");lookup_ip=0x08080808U;check(socks_resolve_request(expected_route,1,&address,lookup,&result)==0&&result==0x08080808U);
  {
    char longest[254];memset(longest,'a',253);longest[63]=longest[127]=longest[191]='.';longest[253]=0;
    n=domain_request(request,longest,0);check(socks_parse_address(request,n,&address)==0);expected_host=longest;
    check(socks_resolve_request(expected_route,1,&address,lookup,&result)==0);expected_host="example.com";
  }
  check(socks_resolve_request(expected_route,1,NULL,lookup,&result)==1);
  check(socks_resolve_request(expected_route,1,&address,NULL,&result)==1);
  check(socks_resolve_request(expected_route,1,&address,lookup,NULL)==1);
  {
    unsigned char datagram[]={0,0,0,1,8,8,8,8,0,53,1,2,3};
    check(!socks_parse_datagram(datagram,sizeof(datagram),&address,&offset)&&offset==10&&address.port==53);
    datagram[2]=1;check(socks_parse_datagram(datagram,sizeof(datagram),&address,&offset)!=0);datagram[2]=0;datagram[9]=0;
    check(socks_parse_datagram(datagram,sizeof(datagram),&address,&offset)!=0);
  }
  socks_reply_bytes(reply,0,0x08080404U,443);check(u32(reply+4)==0x08080404U&&reply[8]==1&&reply[9]==187);
  printf("GATEWAY_SOCKS_RESOLVE_TEST_OK checks=%u capture=false sockets=false os_dns=false\n",checks);return 0;
}
