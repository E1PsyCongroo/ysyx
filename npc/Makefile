PRJ 														:= RVCPU
ABS_TOP 												:= $(NPC_HOME)
BUILD_DIR 											:= $(ABS_TOP)/build
OBJ_DIR 												:= $(BUILD_DIR)/obj_dir
SRC_DIR													:= $(ABS_TOP)/src/main
VSRC_DIR 												:= $(SRC_DIR)/vsrc
CSRC_DIR 												:= $(SRC_DIR)/csrc
RESOURCES_DIR										:= $(SRC_DIR)/resources
CONSTR_DIR											:= $(SRC_DIR)/constr
CHISEL_SRC_DIR									:= $(SRC_DIR)/scala

# project source
VSRCS 													+= $(shell find $(VSRC_DIR) -type f -name "*.v" -or -name "*.sv")
CSRCS 													+= $(shell find $(CSRC_DIR) -type f -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CHISELSRCS											+= $(shell find $(CHISEL_SRC_DIR) -type f -name "*.scala" -or -name "*.sc")

# verilator flags
VERILATOR 											:= verilator
VERILATOR_CFLAGS 								?= --MMD --build -sv --cc -O3 --x-assign fast --x-initial fast --noassert --trace

# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# rules for verilator
INC_PATH												+= $(CSRC_DIR)
INCFLAGS 												:= $(addprefix -I, $(INC_PATH))
CXXFLAGS 												+= $(INCFLAGS)

IMG 														?=

$(shell mkdir -p $(BUILD_DIR))

test:
	mill -i $(PRJ).test

verilog:
	$(call git_commit, "generate verilog")
	mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)

help:
	mill -i $(PRJ).runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR)

$(BUILD_DIR)/$(PRJ)_auto_bind.cpp: $(CONSTR_DIR)/$(PRJ).nxdc
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

$(VSRCS): $(CHISELSRCS)
	@make verilog

$(BUILD_DIR)/$(PRJ): $(VSRCS) $(BUILD_DIR)/$(PRJ)_auto_bind.cpp $(CSRCS) $(RESOURCES_DIR)/* $(NVBOARD_ARCHIVE)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(PRJ) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		-CFLAGS -DTOP_NAME=V$(PRJ) \
		--Mdir $(OBJ_DIR) --exe -o $(BUILD_DIR)/$(PRJ)

sim: $(BUILD_DIR)/$(PRJ)
	$^ $(IMG)

.PHONY: test help reformat checkformat clean sim
.PRECIOUS: $(BUILD_DIR)/$(PRJ)_auto_bind.cpp $(BUILD_DIR)/$(PRJ) $(VSRCS)
-include ../Makefile
