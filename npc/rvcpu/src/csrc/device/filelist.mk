#***************************************************************************************
# Copyright (c) 2014-2022 Zihao Yu, Nanjing University
#
# NEMU is licensed under Mulan PSL v2.
# You can use this software according to the terms and conditions of the Mulan PSL v2.
# You may obtain a copy of Mulan PSL v2 at:
#          http://license.coscl.org.cn/MulanPSL2
#
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
#
# See the Mulan PSL v2 for more details.
#**************************************************************************************/

DIRS-y += rvcpu/src/csrc/device/io
SRCS-$(CONFIG_DEVICE) += rvcpu/src/csrc/device/device.c rvcpu/src/csrc/device/alarm.c rvcpu/src/csrc/device/intr.c
SRCS-$(CONFIG_HAS_SERIAL) += rvcpu/src/csrc/device/serial.c
SRCS-$(CONFIG_HAS_TIMER) += rvcpu/src/csrc/device/timer.c
SRCS-$(CONFIG_HAS_KEYBOARD) += rvcpu/src/csrc/device/keyboard.c
SRCS-$(CONFIG_HAS_VGA) += rvcpu/src/csrc/device/vga.c
SRCS-$(CONFIG_HAS_AUDIO) += rvcpu/src/csrc/device/audio.c
SRCS-$(CONFIG_HAS_DISK) += rvcpu/src/csrc/device/disk.c
SRCS-$(CONFIG_HAS_SDCARD) += rvcpu/src/csrc/device/sdcard.c

SRCS-BLACKLIST-$(CONFIG_TARGET_AM) += rvcpu/src/csrc/device/alarm.c

ifdef CONFIG_DEVICE
ifndef CONFIG_TARGET_AM
LIBS += $(shell sdl2-config --libs)
endif
endif
