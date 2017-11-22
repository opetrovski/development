#!/bin/bash

INSTANCENAME='ts_instancename'
REQUESTING_USER='requesting_user'
NIC1_DNS_SERVER='ts_nic1_dns_server'
NIC1_DNS_SUFFIX='ts_nic1_dns_suffix'
NIC1_GATEWAY='ts_nic1_gateway'
NIC1_IP_ADDRESS='ts_nic1_ip_address'
NIC1_NETWORK_ADAPTER='ts_nic1_network_adapter'
NIC1_SUBNET_MASK='ts_nic1_subnet_mask'
SCRIPT_URL='ts_script_url'
SCRIPT_USERID='instts_script_useridancename'
WINDOWS_DOMAIN_ADMIN='ts_windows_domain_admin'
DOMAIN_NAME='ts_domain_name'
SCRIPT_PWD='ts_script_pwd'
LINUX_ROOT_PWD='ts_linux_root_pwd'
WINDOWS_DOMAIN_ADMIN_PWD='ts_windows_domain_admin_pwd'


exec >/dev/null 2>&1

MOUNTPOINT=$DATA_DISK_TARGET_1
if [ -z "$DATA_DISK_SIZE_1" ]; then
    exit 0
fi
if [ -z "$MOUNTPOINT" ]; then
    MOUNTPOINT='/data'
fi 
fdisk /dev/sdb<<EOF
n
p
1


t
83
w
EOF

sleep 5
pvcreate /dev/sdb1
LVMNAME=`basename $MOUNTPOINT`
vgcreate vg_$LVMNAME /dev/sdb1
lvcreate -n lv_$LVMNAME -l 100%VG vg_$LVMNAME
mkfs.ext3 /dev/mapper/vg_$LVMNAME-lv_$LVMNAME
mkdir -p $MOUNTPOINT
echo "/dev/mapper/vg_$LVMNAME-lv_$LVMNAME $MOUNTPOINT ext3 defaults 1 2">>/etc/fstab
mount $MOUNTPOINT
