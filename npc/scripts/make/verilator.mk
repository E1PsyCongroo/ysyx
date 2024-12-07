ifdef CONFIG_ISA_riscv_ysyxsoc
CXXSRC             += $(OBJ_DIR)/$(PRJ)_auto_bind.cc
endif
ARCHIVES           += $(VERILATOR_DIR)/lib$(PRJ).so $(NVBOARD_ARCHIVE)
ifdef CONFIG_ISA_riscv_ysyxsoc
TOP_MODULE          := ysyxSoCFull
else ifdef CONFIG_ISA_riscv_npc
TOP_MODULE          := NPC
endif
VERILATOR_DIR       := $(OBJ_DIR)/verilator
VSRC_DIR            := $(OBJ_DIR)/verilog
INC_PATH            += $(VERILATOR_ROOT)/include $(VERILATOR_ROOT)/include/vltstd $(VERILATOR_DIR)

# Project sources
RESOURCES           ?= $(shell find $(RESOURCES_DIR) -type f -name "*.v" -or -name "*.sv")
VSRCS               ?= $(wildcard $(VSRC_DIR)/*.v $(VSRC_DIR)/*.sv)
ifdef CONFIG_ISA_riscv_ysyxsoc
VSRCS               += $(shell find $(YSYXSOC_HOME)/perip -type f -name "*.v" -or -name "*.sv")
VSRCS               += $(YSYXSOC_HOME)/build/ysyxSoCFull.v
endif
CHISELSRCS          += $(shell find $(CHISEL_SRC_DIR) -type f -name "*.scala" -or -name "*.sc")
CHISELSRCS          += $(SRC_DIR)/Elaborate.scala

# Verilator flags
VERILATOR           := $(VERILATOR_ROOT)/bin/verilator
VERILATOR_INC_PATH  := $(YSYXSOC_HOME)/perip/uart16550/rtl $(YSYXSOC_HOME)/perip/spi/rtl
VERILATOR_INCLUDES  := $(addprefix -y , $(VERILATOR_INC_PATH))
VERILATOR_CFLAGS    := --MMD --build --cc -O3 --trace-fst --x-assign fast --x-initial fast \
                       --autoflush --noassert --timescale "1ns/1ns" --no-timing --threads 2 -j 0 \
											 $(VERILATOR_INCLUDES)

# Rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

$(BUILD_DIR)/.stamp.verilog$(PLATFORM): $(CHISELSRCS) $(RESOURCES)
	$(call git_commit, "generate verilog")
	@echo + VERILOG $^
	@mkdir -p $(VSRC_DIR)
	@mill -i $(PRJ).runMain $(PLATFORM) --target-dir $(VSRC_DIR) --split-verilog
	@touch $@

# Verilating
$(VERILATOR_DIR)/lib$(PRJ).%: $(BUILD_DIR)/.stamp.verilog$(PLATFORM) $(VSRCS)
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

ifdef CONFIG_ISA_riscv_ysyxsoc
CXXSRC             += $(OBJ_DIR)/$(PRJ)_auto_bind.cc
endif
ARCHIVES           += $(VERILATOR_DIR)/lib$(PRJ).so $(NVBOARD_ARCHIVE)
