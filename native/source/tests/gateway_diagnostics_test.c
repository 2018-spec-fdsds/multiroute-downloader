/* Offline fixture: no native gateway, network stack, driver or credentials. */
#include <assert.h>
#include "../gateway_diagnostics.h"

static void verify_record(const char *record) {
  size_t i,n=strlen(record);
  assert(n<MR_PROXY_DIAGNOSTIC_SIZE&&n>15&&record[n-1]=='\n');
  assert(!strncmp(record,"TUNNEL_ERROR {",14));
  for(i=0;i<n-1;i++)assert((unsigned char)record[i]>=32);
  fputs(record,stdout);
}
int main(void) {
  char record[MR_PROXY_DIAGNOSTIC_SIZE],long_host[255],long_reason[132],stage[33];
  unsigned char guarded[MR_PROXY_DIAGNOSTIC_SIZE+4];size_t i;
  mr_proxy_diagnostic diagnostic={0};
  diagnostic.gateway="192.168.1.2";diagnostic.host="huggingface.co";
  diagnostic.destination_ipv4=0x01020304;diagnostic.resolved=1;diagnostic.remote_port=443;
  mr_proxy_diagnostic_format(record,&diagnostic,"tcp_connect","Selected gateway connection failed",5012,-11);
  assert(strstr(record,"\"gateway\":\"192.168.1.2\""));
  assert(strstr(record,"\"host\":\"huggingface.co\""));
  assert(strstr(record,"\"destination_ipv4\":\"1.2.3.4\""));
  assert(strstr(record,"\"remote_port\":443"));
  assert(strstr(record,"\"elapsed_ms\":5012,\"lwip_error\":-11"));verify_record(record);

  memset(&diagnostic,0,sizeof(diagnostic));
  mr_proxy_diagnostic_format(record,&diagnostic,"parse","Invalid proxy request",0,MR_PROXY_LWIP_UNKNOWN);
  assert(strstr(record,"\"host\":\"\",\"destination_ipv4\":\"\",\"remote_port\":0"));
  assert(strstr(record,"\"lwip_error\":null"));verify_record(record);

  diagnostic.host="huggingface.co/path?token=PRIVATE\r\nFORGED";
  diagnostic.gateway="192.168.1.2\nFORGED";diagnostic.remote_port=70000;
  mr_proxy_diagnostic_format(record,&diagnostic,"parse","Invalid CONNECT authority",1,MR_PROXY_LWIP_UNKNOWN);
  assert(!strstr(record,"PRIVATE")&&!strstr(record,"FORGED")&&!strstr(record,"huggingface"));
  assert(strstr(record,"\"gateway\":\"\",\"host\":\"\""));verify_record(record);

  diagnostic.host="source.example";diagnostic.gateway="192.168.1.3";
  mr_proxy_diagnostic_format(record,&diagnostic,"parse\"\\\r\n","quote\" backslash\\ tab\t newline\n",2,0);
  assert(strstr(record,"parse\\\"\\\\\\u000d\\u000a"));
  assert(strstr(record,"tab\\u0009 newline\\u000a"));verify_record(record);

  mr_proxy_diagnostic_format(record,&diagnostic,"dns","Связь прервана — 接続失敗",3,MR_PROXY_LWIP_UNKNOWN);
  assert(strstr(record,"Связь прервана — 接続失敗"));verify_record(record);
  memset(long_reason,'a',127);memcpy(long_reason+127,"Д",3);
  mr_proxy_diagnostic_format(record,&diagnostic,"dns",long_reason,4,MR_PROXY_LWIP_UNKNOWN);
  assert(!strstr(record,"Д"));verify_record(record);
  mr_proxy_diagnostic_format(record,NULL,NULL,"invalid byte \xff",5,MR_PROXY_LWIP_UNKNOWN);
  assert(strstr(record,"\\u00ff"));verify_record(record);

  memset(long_host,'a',253);long_host[253]=0;diagnostic.host=long_host;
  memset(long_reason,1,128);long_reason[128]=0;memset(stage,2,32);stage[32]=0;
  diagnostic.gateway="255.255.255.255";diagnostic.destination_ipv4=UINT32_MAX;
  diagnostic.resolved=1;diagnostic.remote_port=65535;memset(guarded,0xab,sizeof(guarded));
  mr_proxy_diagnostic_format((char*)guarded,&diagnostic,stage,long_reason,UINT64_MAX,INT_MIN);
  for(i=MR_PROXY_DIAGNOSTIC_SIZE;i<sizeof(guarded);i++)assert(guarded[i]==0xab);
  assert(strstr((char*)guarded,long_host));verify_record((char*)guarded);
  long_host[253]='a';long_host[254]=0;
  mr_proxy_diagnostic_format(record,&diagnostic,"parse","Oversized host",6,MR_PROXY_LWIP_UNKNOWN);
  assert(strstr(record,"\"host\":\"\""));verify_record(record);
  fputs("GATEWAY_DIAGNOSTICS_TEST_OK\n",stdout);return 0;
}
