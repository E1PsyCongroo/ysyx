AM_SRCS := platform/ysyxsoc/trm.c \
           platform/ysyxsoc/ioe/ioe.c \
           platform/ysyxsoc/ioe/timer.c \
           platform/ysyxsoc/ioe/input.c \
           platform/ysyxsoc/ioe/gpu.c \
           platform/ysyxsoc/ioe/audio.c \
           platform/ysyxsoc/ioe/disk.c \
           platform/ysyxsoc/ioe/uart.c \
           platform/dummy/vme.c \
           platform/dummy/mpe.c

CFLAGS    += -fdata-sections -ffunction-sections
LDFLAGS   += -T $(AM_HOME)/scripts/ysyxsoc-linker.ld
LDFLAGS   += --gc-sections -e _start
override NPCFLAGS += -l $(shell dirname $(IMAGE).elf)/npc-log.txt
# override NPCFLAGS += -w $(shell dirname $(IMAGE).elf)/npc-wave.fst

CFLAGS += -DMAINARGS=\"$(mainargs)\"
CFLAGS += -I$(AM_HOME)/am/src/platform/ysyxsoc/include
.PHONY: $(AM_HOME)/am/src/platform/ysyxsoc/trm.c

image: $(IMAGE).elf
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S -O binary --set-section-flags .bss=alloc,contents $(IMAGE).elf $(IMAGE).bin

run: image
	$(MAKE) -C $(NPC_HOME) run ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin ELF=$(IMAGE).elf

gdb: image
	$(MAKE) -C $(NPC_HOME) gdb ARGS="$(NPCFLAGS)" IMG=$(IMAGE).bin ELF=$(IMAGE).elf
