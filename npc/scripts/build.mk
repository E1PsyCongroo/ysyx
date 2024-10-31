.DEFAULT_GOAL = app

# Add necessary options if the target is a shared library
ifeq ($(SHARE),1)
SO                   = -so
CFLAGS              += -fPIC -fvisibility=hidden
LDFLAGS             += -shared -fPIC
endif

PRJ                 := NPC
TOP_MODULE          := ysyxSoCFull
WORK_DIR            := $(shell pwd)
BUILD_DIR           := $(WORK_DIR)/build
OBJ_DIR             := $(BUILD_DIR)/obj-$(NAME)$(SO)
VERILATOR_DIR       := $(OBJ_DIR)/verilator
VSRC_DIR            := $(OBJ_DIR)/verilog
YOSYS_DIR           := $(WORK_DIR)/yosys
SRC_DIR             := $(WORK_DIR)/src/main
CSRC_DIR            := $(SRC_DIR)/csrc
RESOURCES_DIR       := $(SRC_DIR)/resources
CONSTR_DIR          := $(SRC_DIR)/constr
CHISEL_SRC_DIR      := $(SRC_DIR)/scala

VERILATOR_ROOT      := /home/focused_xy/.conda/envs/ysyx/share/verilator
INC_PATH            := $(WORK_DIR)/include $(VERILATOR_ROOT)/include $(VERILATOR_ROOT)/include/vltstd \
                       $(VERILATOR_DIR) $(INC_PATH)
BINARY              := $(BUILD_DIR)/$(NAME)$(SO)


# Project sources
RESOURCES           ?= $(shell find $(RESOURCES_DIR) -type f -name "*.v" -or -name "*.sv")
VSRCS               ?= $(shell find $(VSRC_DIR) -type f -name "*.v" -or -name "*.sv")
VSRCS               += $(shell find $(YSYXSOC_HOME)/perip -type f -name "*.v" -or -name "*.sv")
VSRCS               += $(YSYXSOC_HOME)/build/ysyxSoCFull.v
CHISELSRCS          ?= $(shell find $(CHISEL_SRC_DIR) -type f -name "*.scala" -or -name "*.sc")
CHISELSRCS          += $(SRC_DIR)/Elaborate.scala
V_FILE_GEN          := $(YOSYS_DIR)/RVCPU.sv

# Rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# Compilation flags
ifeq ($(CC),clang)
CXX                 := clang++
else
CXX                 := g++
endif
LD                  := $(CXX)
INCLUDES             = $(addprefix -I, $(INC_PATH))
CFLAGS              := -O2 -MMD -Wall -Werror $(INCLUDES) $(CFLAGS)
LDFLAGS             := -O2 $(LDFLAGS)

# Verilator flags
VERILATOR           := verilator
VERILATOR_INC_PATH  := $(YSYXSOC_HOME)/perip/uart16550/rtl $(YSYXSOC_HOME)/perip/spi/rtl
VERILATOR_INCLUDES   = $(addprefix -y , $(VERILATOR_INC_PATH))
VERILATOR_CFLAGS    := --MMD --build --cc -O3 --trace --x-assign fast --x-initial fast \
                       --autoflush --noassert --timescale "1ns/1ns" --no-timing --threads 2 -j 0 \
											 $(VERILATOR_INCLUDES)

# Generating Verilog
$(V_FILE_GEN): $(CHISELSRCS) $(RESOURCES)
	@echo + VERILOG $^
	@mill -i $(PRJ).runMain Yosys --target-dir $(YOSYS_DIR)
	@sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $@
	@sed -i '/firrtl_black_box_resource_files.f/, $$d' $@

.stamp.verilog: $(CHISELSRCS) $(RESOURCES)
	$(call git_commit, "generate verilog")
	@echo + VERILOG $^
	@mkdir -p $(VSRC_DIR)
	@mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR) --split-verilog
	@touch $@

# Verilating
$(VERILATOR_DIR)/lib$(PRJ).%: .stamp.verilog $(VSRCS)
	@echo + VERILATOR $(VSRCS)
	@mkdir -p $(VERILATOR_DIR)
	@$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOP_MODULE) $(VSRCS) \
		--lib-create $(PRJ) --Mdir $(VERILATOR_DIR)

# NVBOARD
$(OBJ_DIR)/$(PRJ)_auto_bind.cc: $(CONSTR_DIR)/$(PRJ).nxdc
	@echo + AUTO-BIND $<
	@mkdir -p $(dir $@)
	@python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

CXXSRC             += $(OBJ_DIR)/$(PRJ)_auto_bind.cc
ARCHIVES           += $(VERILATOR_DIR)/lib$(PRJ).a $(NVBOARD_ARCHIVE)
OBJS                = $(SRCS:%.c=$(OBJ_DIR)/%.o) $(CXXSRC:%.cc=$(OBJ_DIR)/%.o)

# Compilation patterns
$(OBJ_DIR)/%.o: %.c
	@echo + CC $<
	@mkdir -p $(dir $@)
	@$(CC) $(CFLAGS) -c -o $@ $<
	$(call call_fixdep, $(@:.o=.d), $@)

$(OBJ_DIR)/%.o: %.cc
	@echo + CXX $<
	@mkdir -p $(dir $@)
	@$(CXX) $(CFLAGS) $(CXXFLAGS) -c -o $@ $<
	$(call call_fixdep, $(@:.o=.d), $@)


# Depencies
-include $(OBJS:.o=.d)

# Some convenient rules

.PHONY: app verilog verilator clean

app: $(BINARY)

$(BINARY):: $(ARCHIVES) $(OBJS)
	@echo + LD $@
	@$(LD) -o $@ $(OBJS) $(LDFLAGS) $(ARCHIVES) $(LIBS)

verilog: .stamp.verilog

verilator: $(VERILATOR_DIR)/lib$(PRJ).a

clean:
	-rm -rf $(BUILD_DIR) .stamp.verilog
