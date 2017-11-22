#!/bin/bash

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
