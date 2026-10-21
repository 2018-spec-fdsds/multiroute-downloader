/* Private owner-only hot profile commands. Existing netifs/listener ports stay stable. */
static uint32_t configured_gateway_order[MAX_CONFIGURED_PROFILES];
static int configured_gateway_order_count;
static void ready_hot_fields(FILE *f) {
  int i,first=1;fprintf(f,",\"hot_gateway_updates\":true,\"max_gateway_profiles\":32,\"control_revision\":%llu,\"control_error\":",(unsigned long long)control_revision);
  json_string(f,control_error);fputs(",\"configured_gateways\":[",f);
  if(configured_gateway_order_count){
    for(i=0;i<configured_gateway_order_count;i++){char gateway[16];mr_lan_ip(gateway,configured_gateway_order[i]);if(i)fputc(',',f);json_string(f,gateway);}
  } else for(i=0;i<profile_count;i++)if(profiles[i].configured){if(!first)fputc(',',f);first=0;json_string(f,profiles[i].gateway);}
  fputc(']',f);
}
static int read_profile_control(char *data,size_t capacity) {
  char name[4300];size_t n=0;snprintf(name,sizeof(name),"%s.gateways",ready_path);
#ifdef _WIN32
  WCHAR path[4300];HANDLE file;BY_HANDLE_FILE_INFORMATION info;DWORD read,size;
  if(!MultiByteToWideChar(CP_UTF8,MB_ERR_INVALID_CHARS,name,-1,path,4300))return -1;
  file=CreateFileW(path,GENERIC_READ,FILE_SHARE_READ|FILE_SHARE_DELETE,NULL,OPEN_EXISTING,FILE_FLAG_OPEN_REPARSE_POINT,NULL);
  if(file==INVALID_HANDLE_VALUE)return 0;
  if(!GetFileInformationByHandle(file,&info)||(info.dwFileAttributes&(FILE_ATTRIBUTE_DIRECTORY|FILE_ATTRIBUTE_REPARSE_POINT))||info.nNumberOfLinks!=1||info.nFileSizeHigh||info.nFileSizeLow>=capacity){CloseHandle(file);return -1;}
  size=info.nFileSizeLow;if(!ReadFile(file,data,size,&read,NULL)||read!=size){CloseHandle(file);return -1;}n=read;CloseHandle(file);
#else
  int file=open(name,O_RDONLY|O_NOFOLLOW);struct stat st;ssize_t got;if(file<0)return errno==ENOENT?0:-1;
  if(fstat(file,&st)||!S_ISREG(st.st_mode)||st.st_uid!=target_uid||st.st_nlink!=1||(st.st_mode&0077)||st.st_size<0||(uint64_t)st.st_size>=capacity){close(file);return -1;}
  while(n<(size_t)st.st_size){got=read(file,data+n,(size_t)st.st_size-n);if(got<0&&errno==EINTR)continue;if(got<=0){close(file);return -1;}n+=(size_t)got;}close(file);
#endif
  data[n]=0;return 1;
}
static int hot_add_profile(const char *gateway,uint32_t host) {
  profile *r;ip4_addr_t ip,mask,gw;int index=profile_count;
  if(index>=MAX_PROFILES)return 0;
  r=&profiles[index];memset(r,0,sizeof(*r));strcpy(r->gateway,gateway);r->gateway_host=host;r->listener=r->socks_listener=INVALID_SOCKET;
  ip.addr=htonl(virtual_host);mask.addr=htonl(mask_host);gw.addr=htonl(host);
  LOCK_TCPIP_CORE();
  if(!netif_add(&r->netif,&ip,&mask,&gw,r,netif_init_app,tcpip_input)){UNLOCK_TCPIP_CORE();return 0;}
  netif_set_link_up(&r->netif);netif_set_up(&r->netif);UNLOCK_TCPIP_CORE();
  /* Publish only fully initialized stable storage. A failed listener occupies an inactive
   * tombstone: detached callbacks must never observe a reused profile object. */
  InterlockedIncrement(&profile_count_raw);
  if(!start_profile_listeners(r))return 0;
  arp_send(virtual_host,host,1,NULL);return 1;
}
static int (*profile_activator)(const char*,uint32_t)=hot_add_profile;
static int (*profile_topology_reader)(const uint32_t*,size_t,uint32_t*,char*,size_t)=read_selected_topology;
static int apply_profile_command(char *data) {
  char *revision_text,*list,*end,*p;unsigned long long revision;uint32_t hosts[32];char names[32][16];int count=0,i,j;
  if(strncmp(data,"MR_GATEWAYS_3\n",14))return 0;
  revision_text=data+14;end=strchr(revision_text,'\n');if(!end)return 0;*end=0;
  if(!*revision_text)return 0;for(p=revision_text;*p;p++)if(*p<'0'||*p>'9')return 0;
  revision=strtoull(revision_text,&p,10);if(!revision||*p||revision<=control_revision)return 0;
  list=end+1;end=strchr(list,'\n');if(!end||end[1]){control_revision=revision;strcpy(control_error,"Invalid gateway profile command");return 1;}*end=0;
  if(!*list||list[0]==','||strstr(list,",,")||list[strlen(list)-1]==','){control_revision=revision;strcpy(control_error,"Invalid gateway profile command");return 1;}
  for(p=list;*p;){char *comma=strchr(p,',');size_t length=comma?(size_t)(comma-p):strlen(p);
    if(count>=32||length<7||length>15)goto invalid;
    memcpy(names[count],p,length);names[count][length]=0;
    if(!parse_ip(names[count],&hosts[count])||hosts[count]==virtual_host||(hosts[count]&mask_host)!=(virtual_host&mask_host)||hosts[count]==(virtual_host&mask_host)||hosts[count]==((virtual_host&mask_host)|~mask_host))goto invalid;
    for(i=0;i<count;i++)if(hosts[i]==hosts[count])goto invalid;count++;if(!comma)break;p=comma+1;
  }
  /* Validate the complete desired list before creating/enabling any profile. A bad
   * new gateway must not stop or change the existing working profile set. */
  {uint32_t computer_ip;char error[512];
    if(!profile_topology_reader(hosts,(size_t)count,&computer_ip,error,sizeof(error))){
      control_revision=revision;snprintf(control_error,sizeof(control_error),"%s",error);report_topology_error(control_error);return 1;
    }
  }
  for(i=0;i<count;i++){
    for(j=0;j<profile_count;j++)if(profiles[j].gateway_host==hosts[i])break;
    if(j==profile_count){if(!profile_activator(names[i],hosts[i])){control_revision=revision;strcpy(control_error,"Cannot allocate a gateway profile; restart the application to reclaim inactive profiles");return 1;}}
    else if(profiles[j].listener==INVALID_SOCKET||profiles[j].socks_listener==INVALID_SOCKET){control_revision=revision;strcpy(control_error,"Gateway profile listener is unavailable; restart the application");return 1;}
  }
  EnterCriticalSection(&ready_lock);
  for(i=0;i<profile_count;i++){int enabled=0;for(j=0;j<count;j++)if(profiles[i].gateway_host==hosts[j]){enabled=1;break;}InterlockedExchange(&profiles[i].configured,enabled);}
  memcpy(configured_gateway_order,hosts,(size_t)count*sizeof(hosts[0]));configured_gateway_order_count=count;
  control_revision=revision;control_error[0]=0;LeaveCriticalSection(&ready_lock);return 1;
invalid:
  control_revision=revision;strcpy(control_error,"Gateway profiles must be unique addresses in the application's Ethernet subnet (1 to 32)");report_topology_error(control_error);return 1;
}
static void poll_profile_control(void) {
  char data[1024];int result=read_profile_control(data,sizeof(data));
  if(result>0&&apply_profile_command(data))ready_result(NULL);
}

/* Offline state-machine test: fake profile creation never opens network/file handles. */
static int profile_test_activate(const char *gateway,uint32_t host){
  profile *r;if(profile_count>=MAX_PROFILES)return 0;r=&profiles[profile_count];memset(r,0,sizeof(*r));
  strcpy(r->gateway,gateway);r->gateway_host=host;r->listener=(SOCKET)(1000+profile_count);r->socks_listener=(SOCKET)(2000+profile_count);
  r->proxy_port=(unsigned short)(3000+profile_count);r->socks_port=(unsigned short)(4000+profile_count);r->mac_known=1;
  InterlockedIncrement(&profile_count_raw);return 1;
}
static int profile_test_topology(const uint32_t *hosts,size_t count,uint32_t *computer_ip,char *error,size_t size){
  uint32_t os_address=0xc0a80164;mr_lan_result result=mr_lan_validate(os_address,mask_host,virtual_host,hosts,count,&os_address,1);
  if(result!=MR_LAN_OK)return topology_error(error,size,mr_lan_reason(result));
  *computer_ip=os_address;return 1;
}
static int profile_control_self_test(void){
  char command[1024];int i,n;uint32_t oldmask=mask_host;int (*old)(const char*,uint32_t)=profile_activator;
  int (*old_topology)(const uint32_t*,size_t,uint32_t*,char*,size_t)=profile_topology_reader;
  if(profile_count||capture||transmit||configured_gateway_order_count)return 1;profile_activator=profile_test_activate;profile_topology_reader=profile_test_topology;mask_host=0xffffff00;
  for(i=1;i<=4;i++){char gateway[16];snprintf(gateway,sizeof(gateway),"192.168.1.%d",i);if(!profile_test_activate(gateway,0xc0a80100U+(uint32_t)i))return 11;profiles[i-1].configured=1;}
  fputs("INITIAL_PROFILE_READY {\"fixture\":true",stdout);ready_hot_fields(stdout);puts("}");
  memset(profiles,0,sizeof(profiles));InterlockedExchange(&profile_count_raw,0);
  strcpy(command,"MR_GATEWAYS_3\n1\n192.168.1.1,192.168.1.2,192.168.1.3,192.168.1.4,192.168.1.5\n");
  if(!apply_profile_command(command)||profile_count!=5||control_error[0])return 2;
  profiles[0].tx_bytes=771;unsigned short stable=profiles[0].proxy_port;
  strcpy(command,"MR_GATEWAYS_3\n2\n192.168.1.1,192.168.1.6\n");
  if(!apply_profile_command(command)||profile_count!=6||control_error[0]||profiles[0].tx_bytes!=771||profiles[0].proxy_port!=stable||profiles[1].configured||!profiles[5].configured)return 3;
  strcpy(command,"MR_GATEWAYS_3\n3\n192.168.1.1,192.168.1.1\n");if(!apply_profile_command(command)||!control_error[0]||!profiles[0].configured||!profiles[5].configured)return 4;
  strcpy(command,"MR_GATEWAYS_3\n4\n192.168.2.1\n");if(!apply_profile_command(command)||!control_error[0]||profile_count!=6)return 5;
  n=snprintf(command,sizeof(command),"MR_GATEWAYS_3\n5\n");
  for(i=1;i<=32;i++)n+=snprintf(command+n,sizeof(command)-(size_t)n,"%s192.168.1.%d",i==1?"":",",i);strcat(command,"\n");
  if(!apply_profile_command(command)||profile_count!=32||control_error[0])return 6;
  for(i=0;i<32;i++)if(!profiles[i].configured)return 7;
  strcpy(command,"MR_GATEWAYS_3\n5\n192.168.1.33\n");if(apply_profile_command(command)||profile_count!=32)return 8;
  strcpy(command,"MR_GATEWAYS_3\n6\n192.168.1.6,192.168.1.1\n");
  if(!apply_profile_command(command)||control_error[0]||configured_gateway_order_count!=2||configured_gateway_order[0]!=0xc0a80106||configured_gateway_order[1]!=0xc0a80101||profiles[0].proxy_port!=stable)return 9;
  strcpy(command,"MR_GATEWAYS_3\n7\n192.168.1.1,192.168.1.100\n");
  if(!apply_profile_command(command)||!strstr(control_error,"assigned")||profile_count!=32||configured_gateway_order_count!=2||configured_gateway_order[0]!=0xc0a80106||profiles[0].proxy_port!=stable||!profiles[5].configured)return 10;
  profile_activator=old;profile_topology_reader=old_topology;mask_host=oldmask;memset(profiles,0,sizeof(profiles));InterlockedExchange(&profile_count_raw,0);control_revision=0;control_error[0]=0;configured_gateway_order_count=0;
  puts("HOT_PROFILE_SELF_TEST_OK stable ports, live add/remove, ordered profiles, rejected topology leaves old profiles, 32 profiles, no network handles");return 0;
}
