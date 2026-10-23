/* Offline contract tests. No sockets, packet capture, OS resolver or user data. */
#define _CRT_SECURE_NO_WARNINGS
#include <assert.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#define MR_DNS_PARSER_ONLY
#include "../gateway_dns.h"
#include "../gateway_dns_broker_protocol.h"
static unsigned checks;
static void check(int value){checks++;assert(value);}
int main(void){
  mr_dns_policy p,q;mr_dns_tunnel_request target;unsigned char packet[8192],wire[512],padded[16],answer[8192];char key[65],gateway[16];uint64_t number;uint32_t ip,ttl;size_t size,n;
  const char *bad_numbers[]={"","-1","+1"," 1","1 ","1\n","01","0x1","18446744073709551616","999999999999999999999999"};
  const char *bad_gateways[]={"","1.2.3","1.2.3.4.5","01.2.3.4","256.2.3.4","1..3.4","1.2.3.","1.2.3.a","1.2.3.400"};
  check(gateway_dns_self_test()==0);check(mr_dns_broker_protocol_self_test()==0);
  memset(key,'a',64);key[64]=0;check(mr_dns_policy_fields("4321",key,"1",&p));check(p.broker_port==4321&&p.revision==1);
  check(mr_dns_decimal("18446744073709551615",UINT64_MAX,&number)&&number==UINT64_MAX);
  check(mr_dns_decimal("9223372036854775807",INT64_MAX,&number));
  for(size_t i=0;i<sizeof(bad_numbers)/sizeof(bad_numbers[0]);i++)check(!mr_dns_decimal(bad_numbers[i],UINT64_MAX,&number));
  check(!mr_dns_policy_fields("65536",key,"1",&q));check(!mr_dns_policy_fields("0",key,"1",&q));check(!mr_dns_policy_fields("4321",key,"0",&q));check(!mr_dns_policy_fields("4321",key,"9223372036854775808",&q));
  q=p;check(mr_dns_same_policy(&p,&q));q.revision++;check(!mr_dns_same_policy(&p,&q));q=p;q.broker_port++;check(!mr_dns_same_policy(&p,&q));q=p;q.key[63]='b';check(!mr_dns_same_policy(&p,&q));
  for(size_t i=0;i<64;i++){char saved=key[i];key[i]='z';check(!mr_dns_key_valid(key));key[i]=saved;}
  check(!mr_dns_key_valid("a"));check(!mr_dns_key_valid(NULL));
  memset(padded,0,16);memcpy(padded,"192.168.1.4",11);check(mr_dns_padded_gateway(padded,gateway)&&!strcmp(gateway,"192.168.1.4"));
  for(size_t i=0;i<sizeof(bad_gateways)/sizeof(bad_gateways[0]);i++){memset(padded,0,16);memcpy(padded,bad_gateways[i],strlen(bad_gateways[i]));check(!mr_dns_padded_gateway(padded,gateway));}
  memset(padded,'1',16);check(!mr_dns_padded_gateway(padded,gateway));
  size=mr_dns_query("example.com",77,wire,sizeof(wire));check(size>=12);
  n=mr_dns_broker_encode(packet,sizeof(packet),&p,"192.168.1.4",8000,wire,size);check(n==100+size);check(!memcmp(packet,"MRDNS2",6)&&packet[93]==1&&mr_dns_u32(packet+94)==8000&&mr_dns_u16(packet+98)==size&&memcmp(packet+100,wire,size)==0);
  check(!mr_dns_broker_encode(packet,100+size-1,&p,"192.168.1.4",8000,wire,size));check(!mr_dns_broker_encode(packet,sizeof(packet),&p,"192.168.1.4",0,wire,size));check(!mr_dns_broker_encode(packet,sizeof(packet),&p,"192.168.1.4",8001,wire,size));check(!mr_dns_broker_encode(packet,sizeof(packet),&p,"192.168.1.4",8000,wire,11));
  mr_dns_broker_encode(packet,sizeof(packet),&p,"192.168.1.4",8000,wire,size);memcpy(packet,"MRDNT1",6);packet[86]=1;packet[87]=1;packet[88]=1;packet[89]=1;mr_dns_put16(packet+90,853);
  check(mr_dns_tunnel_decode(packet,92,&p,&target)&&target.destination==0x01010101U&&target.port==853&&!strcmp(target.gateway,"192.168.1.4"));
  for(size_t i=0;i<64;i++){packet[6+i]^=1;check(!mr_dns_tunnel_decode(packet,92,&p,&target));packet[6+i]^=1;}
  check(!mr_dns_tunnel_decode(packet,91,&p,&target));check(!mr_dns_tunnel_decode(packet,93,&p,&target));
  for(size_t i=0;i<6;i++){packet[i]^=1;check(!mr_dns_tunnel_decode(packet,92,&p,&target));packet[i]^=1;}
  packet[85]=1;check(!mr_dns_tunnel_decode(packet,92,&p,&target));packet[85]=0;mr_dns_put16(packet+90,0);check(!mr_dns_tunnel_decode(packet,92,&p,&target));mr_dns_put16(packet+90,443);
  packet[86]=127;check(!mr_dns_tunnel_decode(packet,92,&p,&target));packet[86]=10;check(!mr_dns_tunnel_decode(packet,92,&p,&target));packet[86]=192;packet[87]=168;check(!mr_dns_tunnel_decode(packet,92,&p,&target));
  /* A valid encrypted reply may exceed the legacy4096 UDP/TCP buffer. */
  n=size-11;memcpy(answer,wire,n);answer[2]=0x81;answer[3]=0x80;answer[6]=0;answer[7]=1;answer[10]=0;answer[11]=1;
  answer[n++]=0xc0;answer[n++]=12;mr_dns_put16(answer+n,1);n+=2;mr_dns_put16(answer+n,1);n+=2;memset(answer+n,0,4);n+=4;mr_dns_put16(answer+n,4);n+=2;answer[n++]=93;answer[n++]=184;answer[n++]=216;answer[n++]=34;
  answer[n++]=0xc0;answer[n++]=12;mr_dns_put16(answer+n,16);n+=2;mr_dns_put16(answer+n,1);n+=2;memset(answer+n,0,4);n+=4;mr_dns_put16(answer+n,4100);n+=2;memset(answer+n,0,4100);n+=4100;
  check(n>4096&&mr_dns_answer(answer,n,77,"example.com",&ip,&ttl)&&ip==0x5db8d822U&&ttl==0);
  check(!mr_dns_answer(answer,n,78,"example.com",&ip,&ttl));check(!mr_dns_answer(answer,n,77,"wrong.example",&ip,&ttl));check(!mr_dns_answer(answer,65536,77,"example.com",&ip,&ttl));
  answer[2]|=2;check(mr_dns_truncated(answer,n<4096?n:4096,77,"example.com"));
  printf("GATEWAY_DNS_BROKER_PROTOCOL_TEST_OK checks=%u network_handles=false os_network_changed=false\n",checks);return 0;
}
