.DEFAULT_GOAL = app

# Add necessary options if the target is a shared library
ifeq ($(SHARE),1)
SO = -so
CFLAGS  += -fPIC -fvisibility=hidden
LDFLAGS += -shared -fPIC
endif

PRJ 														:= RVCPU
WORK_DIR  											:= $(shell pwd)
BUILD_DIR 											:= $(WORK_DIR)/build
OBJ_DIR  												:= $(BUILD_DIR)/obj-$(NAME)$(SO)
SRC_DIR													:= $(WORK_DIR)/src/main
VSRC_DIR 												:= $(SRC_DIR)/vsrc
CSRC_DIR 												:= $(SRC_DIR)/csrc
RESOURCES_DIR										:= $(SRC_DIR)/resources
CONSTR_DIR											:= $(SRC_DIR)/constr
CHISEL_SRC_DIR									:= $(SRC_DIR)/scala

VERILATOR_ROOT									:= /home/focused_xy/.conda/envs/ysyx/share/verilator/
VERILATOR_INC_PATH 							:= $(VERILATOR_ROOT)/include $(VERILATOR_ROOT)/include/vltstd
INC_PATH 												:= $(WORK_DIR)/include $(VERILATOR_INC_PATH) $(OBJ_DIR) $(INC_PATH)
BINARY   												:= $(BUILD_DIR)/$(NAME)$(SO)


# project source
VSRCS 													?= $(shell find $(VSRC_DIR) -type f -name "*.v" -or -name "*.sv")
CHISELSRCS											?= $(shell find $(CHISEL_SRC_DIR) -type f -name "*.scala" -or -name "*.sc")
RESOURCES												?= $(shell find $(RESOURCES_DIR) -type f -name "*.v" -or -name "*.sv")

# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# Compilation flags
ifeq ($(CC),clang)
CXX 														:= clang++
else
CXX 														:= g++
endif
LD 															:= $(CXX)
INCLUDES 												= $(addprefix -I, $(INC_PATH))
CFLAGS  												:= -O2 -MMD -Wall -Werror $(INCLUDES) $(CFLAGS)
LDFLAGS 												:= -O2 $(LDFLAGS)

# Verilator flags
VERILATOR 											:= verilator
VERILATOR_CFLAGS 								?= --MMD --build --cc -O3 --x-assign fast --x-initial fast --noassert --trace

# Verilating
.stamp.verilog: $(CHISELSRCS)
	$(call git_commit, "generate verilog")
	@echo + VERILOG $(VSRC_DIR)
	@mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)
	@touch $@

$(OBJ_DIR)/lib$(PRJ).a: .stamp.verilog $(RESOURCES)
	@echo + VERILATOR $(RESOURCES) $(VSRCS)
	@mkdir -p $(OBJ_DIR)
	@$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(PRJ) $(RESOURCES) $(VSRCS) \
		--lib-create $(PRJ) --Mdir $(OBJ_DIR)

# NVBOARD
$(BUILD_DIR)/$(PRJ)_auto_bind.cc: $(CONSTR_DIR)/$(PRJ).nxdc
	@echo + AUTO-BIND $<
	@mkdir -p $(dir $@)
	@python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

CXXSRC += $(BUILD_DIR)/$(PRJ)_auto_bind.cc
ARCHIVES += $(OBJ_DIR)/lib$(PRJ).a $(NVBOARD_ARCHIVE)
OBJS = $(SRCS:%.c=$(OBJ_DIR)/%.o) $(CXXSRC:%.cc=$(OBJ_DIR)/%.o)

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

verilator: $(BUILD_DIR)/lib$(PRJ).a

clean:
	-rm -rf $(BUILD_DIR)
