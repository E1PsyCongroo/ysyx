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

SRCS-y += src/main/csrc/npc-main.c
DIRS-y += src/main/csrc/cpu src/main/csrc/monitor src/main/csrc/utils
DIRS-$(CONFIG_MODE_SYSTEM) += src/main/csrc/memory
DIRS-BLACKLIST-$(CONFIG_TARGET_AM) += src/main/csrc/monitor/sdb

SHARE = $(if $(CONFIG_TARGET_SHARE),1,0)
LIBS += $(if $(CONFIG_TARGET_NATIVE_ELF),-lreadline -ldl -pie,)

ifdef mainargs
ASFLAGS += -DBIN_PATH=\"$(mainargs)\"
endif
SRCS-$(CONFIG_TARGET_AM) += src/main/csrc/am-bin.S
.PHONY: src/main/csrc/am-bin.S
