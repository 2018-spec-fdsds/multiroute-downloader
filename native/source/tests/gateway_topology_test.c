/* Pure snapshots only: no OS enumeration, capture, address/route changes or sockets. */
#include <assert.h>
#include <string.h>
#include "../gateway_topology.h"
#define IPV4(a,b,c,d) (((uint32_t)(a)<<24)|((uint32_t)(b)<<16)|((uint32_t)(c)<<8)|(uint32_t)(d))
int main(void) {
  uint32_t gateways[4]={IPV4(192,168,1,1),IPV4(192,168,1,2),IPV4(192,168,1,3),IPV4(192,168,1,4)};
  uint32_t os_addresses[4]={IPV4(192,168,1,100),IPV4(10,0,0,10),0,0},computer,one;
  uint32_t virtual_ip=IPV4(192,168,1,70),mask=0xffffff00U;mr_lan_state state,before;
  mr_lan_address current[3];unsigned int identities[3];size_t adapter_count=0;char cidr[20],ip[16];
  assert(mr_lan_validate(os_addresses[0],mask,virtual_ip,gateways,4,os_addresses,2)==MR_LAN_OK);
  assert(mr_lan_prefix(0xffff0000U)==16&&mr_lan_prefix(0xfffffe00U)==23&&mr_lan_prefix(0xffffff80U)==25);
  assert(mr_lan_prefix(0xfffffffcU)==30&&!mr_lan_prefix(0xfffffffeU)&&!mr_lan_prefix(0xffffffffU));
  assert(!mr_lan_prefix(0)&&!mr_lan_prefix(0xffff00ffU));
  one=IPV4(192,168,2,255);
  assert(mr_lan_validate(os_addresses[0],0xffff0000U,IPV4(192,168,1,0),&one,1,os_addresses,2)==MR_LAN_OK);
  one=IPV4(192,168,0,255);
  assert(mr_lan_validate(os_addresses[0],0xfffffe00U,IPV4(192,168,1,0),&one,1,os_addresses,2)==MR_LAN_OK);
  one=IPV4(192,168,1,129);
  assert(mr_lan_validate(os_addresses[0],0xffffff80U,virtual_ip,&one,1,os_addresses,2)==MR_LAN_GATEWAY_OUTSIDE);
  assert(mr_lan_validate(os_addresses[0],mask,IPV4(192,168,2,70),gateways,4,os_addresses,2)==MR_LAN_VIRTUAL_OUTSIDE);
  one=IPV4(192,168,1,0);
  assert(mr_lan_validate(IPV4(192,168,1,1),0xfffffffcU,IPV4(192,168,1,2),&one,1,NULL,0)==MR_LAN_GATEWAY_RESERVED);
  one=IPV4(192,168,1,3);
  assert(mr_lan_validate(IPV4(192,168,1,1),0xfffffffcU,IPV4(192,168,1,2),&one,1,NULL,0)==MR_LAN_GATEWAY_RESERVED);
  assert(mr_lan_validate(IPV4(192,168,1,1),0xfffffffcU,IPV4(192,168,1,0),NULL,0,NULL,0)==MR_LAN_VIRTUAL_RESERVED);
  assert(mr_lan_validate(IPV4(192,168,1,1),0xfffffffcU,IPV4(192,168,1,3),NULL,0,NULL,0)==MR_LAN_VIRTUAL_RESERVED);
  assert(mr_lan_validate(IPV4(192,168,1,0),mask,virtual_ip,gateways,4,NULL,0)==MR_LAN_COMPUTER_RESERVED);
  assert(mr_lan_validate(os_addresses[0],0xfffffffeU,virtual_ip,gateways,4,os_addresses,2)==MR_LAN_INVALID_MASK);
  os_addresses[2]=virtual_ip;
  assert(mr_lan_validate(os_addresses[0],mask,virtual_ip,gateways,4,os_addresses,3)==MR_LAN_VIRTUAL_ASSIGNED);
  os_addresses[2]=gateways[1];
  assert(mr_lan_validate(os_addresses[0],mask,virtual_ip,gateways,4,os_addresses,3)==MR_LAN_GATEWAY_ASSIGNED);

  /* Multiple aliases for one NIC must never count as multiple matching NICs. */
  assert(mr_lan_add_adapter(identities,&adapter_count,3,7)==1);
  assert(mr_lan_add_adapter(identities,&adapter_count,3,7)==0&&adapter_count==1);
  assert(mr_lan_add_adapter(identities,&adapter_count,3,8)==1&&adapter_count==2);
  assert(mr_lan_add_adapter(identities,&adapter_count,3,7)==0&&adapter_count==2);
  assert(mr_lan_add_adapter(identities,&adapter_count,3,0)==-1);

  mr_lan_start(&state,os_addresses[0],mask,virtual_ip);before=state;
  current[0].address=IPV4(192,168,1,101);current[0].mask=mask;
  current[1].address=os_addresses[0];current[1].mask=mask;
  current[2].address=IPV4(192,168,1,102);current[2].mask=mask;
  assert(mr_lan_current(&state,current,3,gateways,4,os_addresses,2,&computer)==MR_LAN_OK&&computer==os_addresses[0]);
  assert(!mr_lan_observe(&state,computer)&&state.generation==1);
  os_addresses[0]=current[0].address;
  assert(mr_lan_current(&state,current,1,gateways,4,os_addresses,2,&computer)==MR_LAN_OK&&computer==current[0].address);
  assert(mr_lan_observe(&state,computer)&&state.generation==2);
  assert(state.virtual_ipv4==before.virtual_ipv4&&state.mask==before.mask&&state.network==before.network);
  assert(state.prefix_length==before.prefix_length);
  one=IPV4(192,168,2,1);before=state;
  assert(mr_lan_current(&state,current,1,&one,1,os_addresses,2,&computer)==MR_LAN_GATEWAY_OUTSIDE);
  assert(state.computer_ipv4==before.computer_ipv4&&state.virtual_ipv4==before.virtual_ipv4);
  assert(state.mask==before.mask&&state.network==before.network&&state.generation==before.generation);
  current[0].mask=0xffffff80U;
  assert(mr_lan_current(&state,current,1,gateways,4,os_addresses,2,&computer)==MR_LAN_PREFIX_CHANGED);
  current[0].mask=mask;current[0].address=IPV4(192,168,2,101);
  assert(mr_lan_current(&state,current,1,gateways,4,os_addresses,2,&computer)==MR_LAN_SUBNET_CHANGED);
  assert(mr_lan_current(&state,NULL,0,gateways,4,os_addresses,2,&computer)==MR_LAN_NO_ADDRESS);
  os_addresses[2]=state.virtual_ipv4;
  assert(mr_lan_current(&state,current,1,gateways,4,os_addresses,3,&computer)==MR_LAN_VIRTUAL_ASSIGNED);
  os_addresses[2]=gateways[3];
  assert(mr_lan_current(&state,current,1,gateways,4,os_addresses,3,&computer)==MR_LAN_GATEWAY_ASSIGNED);
  mr_lan_ip(ip,state.virtual_ipv4);assert(!strcmp(ip,"192.168.1.70"));
  mr_lan_cidr(cidr,&state);assert(!strcmp(cidr,"192.168.1.0/24"));
  assert(strstr(mr_lan_reason(MR_LAN_PREFIX_CHANGED),"prefix changed"));
  assert(strstr(mr_lan_reason((mr_lan_result)999),"validation failed"));
  fputs("TOPOLOGY_READY {\"fixture\":true",stdout);mr_lan_ready(stdout,&state,1);fputs("}\n",stdout);
  fputs("TOPOLOGY_RECHECK {\"fixture\":true",stdout);mr_lan_ready(stdout,&state,0);fputs("}\n",stdout);
  fputs("GATEWAY_TOPOLOGY_TEST_OK real masks, aliases, collisions, stable virtual IP and explicit recheck\n",stdout);return 0;
}
