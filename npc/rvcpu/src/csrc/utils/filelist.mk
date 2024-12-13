ifeq ($(CONFIG_ITRACE)$(CONFIG_IQUEUE),)
SRCS-BLACKLIST-y += rvcpu/src/csrc/utils/disasm.c
else
LIBCAPSTONE = tools/capstone/repo/libcapstone.so.5
CFLAGS += -I tools/capstone/repo/include -DLIBCAPSTONE=\"$(LIBCAPSTONE)\"
rvcpu/src/csrc/utils/disasm.c: $(LIBCAPSTONE)
$(LIBCAPSTONE):
	$(MAKE) -C tools/capstone
endif