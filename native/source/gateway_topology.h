/* Pure IPv4 topology validation shared by startup, live profile changes and monitoring.
 * Addresses use host byte order. This code never reads or changes OS networking. */
#ifndef MR_GATEWAY_TOPOLOGY_H
#define MR_GATEWAY_TOPOLOGY_H
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>

#define MR_LAN_MAX_ADDRESSES 512
typedef struct mr_lan_address {uint32_t address,mask;} mr_lan_address;
typedef struct mr_lan_state {
  uint32_t computer_ipv4,virtual_ipv4,mask,network;
  unsigned int prefix_length;
  uint64_t generation;
} mr_lan_state;
typedef enum mr_lan_result {
  MR_LAN_OK,MR_LAN_INVALID_MASK,MR_LAN_VIRTUAL_ASSIGNED,MR_LAN_GATEWAY_ASSIGNED,
  MR_LAN_VIRTUAL_OUTSIDE,MR_LAN_GATEWAY_OUTSIDE,MR_LAN_VIRTUAL_RESERVED,
  MR_LAN_GATEWAY_RESERVED,MR_LAN_COMPUTER_RESERVED,MR_LAN_PREFIX_CHANGED,
  MR_LAN_SUBNET_CHANGED,MR_LAN_NO_ADDRESS
} mr_lan_result;
static const char *mr_lan_reason(mr_lan_result result) {
  switch(result) {
    case MR_LAN_OK:return "Application Ethernet subnet validated";
    case MR_LAN_INVALID_MASK:return "Ethernet subnet mask is invalid or unsupported";
    case MR_LAN_VIRTUAL_ASSIGNED:return "Virtual IP is already assigned to this computer; choose another application IP";
    case MR_LAN_GATEWAY_ASSIGNED:return "Gateway IP is assigned to this computer; choose a router address";
    case MR_LAN_VIRTUAL_OUTSIDE:return "Application virtual IP is outside the computer's Ethernet subnet";
    case MR_LAN_GATEWAY_OUTSIDE:return "Gateway IP is outside the computer's Ethernet subnet";
    case MR_LAN_VIRTUAL_RESERVED:return "Application virtual IP is a subnet network or broadcast address";
    case MR_LAN_GATEWAY_RESERVED:return "Gateway IP is a subnet network or broadcast address";
    case MR_LAN_COMPUTER_RESERVED:return "Computer IPv4 is not a usable address in its Ethernet subnet";
    case MR_LAN_PREFIX_CHANGED:return "Computer subnet prefix changed; check and apply the application network settings again";
    case MR_LAN_SUBNET_CHANGED:return "Computer IPv4 moved outside the application Ethernet subnet; check and apply the application network settings again";
    case MR_LAN_NO_ADDRESS:return "No active IPv4 address remains on the selected Ethernet adapter";
  }
  return "Application Ethernet subnet validation failed";
}
static unsigned int mr_lan_prefix(uint32_t mask) {
  uint32_t inverse=~mask;unsigned int prefix=0;
  if(!mask||(inverse&(inverse+1U))||inverse<3)return 0;
  while(mask&0x80000000U){prefix++;mask<<=1;}return prefix;
}
static int mr_lan_reserved(uint32_t address,uint32_t network,uint32_t mask) {
  return address==network||address==(network|~mask);
}
/* IPv4 aliases on one interface represent one adapter candidate. */
static int mr_lan_add_adapter(unsigned int *identities,size_t *count,size_t capacity,unsigned int identity) {
  size_t i;for(i=0;i<*count;i++)if(identities[i]==identity)return 0;
  if(!identity||*count>=capacity)return -1;
  identities[(*count)++]=identity;return 1;
}
static mr_lan_result mr_lan_collisions(uint32_t virtual_ip,const uint32_t *gateways,size_t gateway_count,
    const uint32_t *os_addresses,size_t os_count) {
  size_t i,j;
  for(i=0;i<os_count;i++) {
    if(os_addresses[i]==virtual_ip)return MR_LAN_VIRTUAL_ASSIGNED;
    for(j=0;j<gateway_count;j++)if(os_addresses[i]==gateways[j])return MR_LAN_GATEWAY_ASSIGNED;
  }
  return MR_LAN_OK;
}
static mr_lan_result mr_lan_validate(uint32_t computer_ip,uint32_t mask,uint32_t virtual_ip,
    const uint32_t *gateways,size_t gateway_count,const uint32_t *os_addresses,size_t os_count) {
  uint32_t network=computer_ip&mask;size_t i;
  mr_lan_result collision=mr_lan_collisions(virtual_ip,gateways,gateway_count,os_addresses,os_count);
  if(collision!=MR_LAN_OK)return collision;
  if(!mr_lan_prefix(mask))return MR_LAN_INVALID_MASK;
  if(mr_lan_reserved(computer_ip,network,mask))return MR_LAN_COMPUTER_RESERVED;
  if((virtual_ip&mask)!=network)return MR_LAN_VIRTUAL_OUTSIDE;
  if(mr_lan_reserved(virtual_ip,network,mask))return MR_LAN_VIRTUAL_RESERVED;
  for(i=0;i<gateway_count;i++) {
    if((gateways[i]&mask)!=network)return MR_LAN_GATEWAY_OUTSIDE;
    if(mr_lan_reserved(gateways[i],network,mask))return MR_LAN_GATEWAY_RESERVED;
  }
  return MR_LAN_OK;
}
static void mr_lan_start(mr_lan_state *state,uint32_t computer_ip,uint32_t mask,uint32_t virtual_ip) {
  state->computer_ipv4=computer_ip;state->virtual_ipv4=virtual_ip;state->mask=mask;
  state->network=computer_ip&mask;state->prefix_length=mr_lan_prefix(mask);state->generation=1;
}
/* Select a compatible current alias on the SAME physical adapter. A prefix/subnet
 * change requires a new address claim and stack setup; never migrate silently. */
static mr_lan_result mr_lan_current(const mr_lan_state *state,const mr_lan_address *addresses,size_t count,
    const uint32_t *gateways,size_t gateway_count,const uint32_t *os_addresses,size_t os_count,uint32_t *computer_ip) {
  size_t i;int found=0,same_mask=0;uint32_t selected=0;mr_lan_result invalid=MR_LAN_OK;
  mr_lan_result result=mr_lan_collisions(state->virtual_ipv4,gateways,gateway_count,os_addresses,os_count);
  if(result!=MR_LAN_OK)return result;
  if(!count)return MR_LAN_NO_ADDRESS;
  for(i=0;i<count;i++) {
    if(addresses[i].mask!=state->mask)continue;
    same_mask=1;if((addresses[i].address&state->mask)!=state->network)continue;
    result=mr_lan_validate(addresses[i].address,state->mask,state->virtual_ipv4,
        gateways,gateway_count,os_addresses,os_count);
    if(result!=MR_LAN_OK){invalid=result;continue;}
    if(!found||addresses[i].address==state->computer_ipv4){selected=addresses[i].address;found=1;}
  }
  if(found){*computer_ip=selected;return MR_LAN_OK;}
  if(invalid!=MR_LAN_OK)return invalid;
  return same_mask?MR_LAN_SUBNET_CHANGED:MR_LAN_PREFIX_CHANGED;
}
/* Only observation metadata changes; the private virtual IP/netmask stay stable. */
static int mr_lan_observe(mr_lan_state *state,uint32_t computer_ip) {
  if(state->computer_ipv4==computer_ip)return 0;
  state->computer_ipv4=computer_ip;state->generation++;return 1;
}
static void mr_lan_ip(char out[16],uint32_t ip) {
  snprintf(out,16,"%u.%u.%u.%u",(unsigned int)(ip>>24),(unsigned int)((ip>>16)&255),
      (unsigned int)((ip>>8)&255),(unsigned int)(ip&255));
}
static void mr_lan_cidr(char out[20],const mr_lan_state *state) {
  char network[16];mr_lan_ip(network,state->network);
  snprintf(out,20,"%s/%u",network,state->prefix_length);
}
static void mr_lan_ready(FILE *file,const mr_lan_state *state,int validated) {
  char computer[16]="",mask[16]="",cidr[20]="";
  if(state->generation){mr_lan_ip(computer,state->computer_ipv4);mr_lan_ip(mask,state->mask);mr_lan_cidr(cidr,state);}
  fprintf(file,",\"computer_ipv4\":\"%s\",\"netmask\":\"%s\",\"prefix_length\":%u,"
      "\"network_cidr\":\"%s\",\"network_generation\":%llu,\"network_validation_ok\":%s,"
      "\"network_validation_status\":\"%s\"",computer,mask,state->prefix_length,cidr,
      (unsigned long long)state->generation,validated?"true":"false",validated?"valid":"recheck_required");
}
#endif
