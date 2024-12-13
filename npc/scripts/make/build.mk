.DEFAULT_GOAL = app

# Add necessary options if the target is a shared library
ifeq ($(SHARE),1)
SO                   = -so
CFLAGS              += -fPIC -fvisibility=hidden
LDFLAGS             += -shared -fPIC
endif

BINARY              := $(BUILD_DIR)/$(NAME)$(SO)
OBJ_DIR             := $(BUILD_DIR)/obj-$(NAME)$(SO)
RESOURCES_DIR       := $(WORK_DIR)/rvcpu/resources
SRC_DIR             := $(WORK_DIR)/rvcpu/src
CSRC_DIR            := $(SRC_DIR)/csrc
CONSTR_DIR          := $(SRC_DIR)/constr
CHISEL_SRC_DIR      := $(SRC_DIR)/scala

INC_PATH            += $(WORK_DIR)/include

# Compilation flags
ifeq ($(CC),clang)
CXX                 := clang++
else
CXX                 := g++
endif
LD                  := $(CXX)
INCLUDES            += $(addprefix -I, $(INC_PATH))
CFLAGS              += -O2 -MMD -Wall -Werror $(INCLUDES)
LDFLAGS             += -O2

ARCHIVES            ?=
OBJS                ?= $(SRCS:%.c=$(OBJ_DIR)/%.o) $(CXXSRC:%.cc=$(OBJ_DIR)/%.o)

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
app: $(BINARY)

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: app clean

# Add rules
include $(NPC_HOME)/scripts/make/verilator.mk
include $(NPC_HOME)/scripts/make/yosys.mk
include $(NPC_HOME)/scripts/make/iverilog.mk
include $(NPC_HOME)/scripts/make/fpga.mk

$(BINARY):: $(ARCHIVES) $(OBJS)
	@echo + LD $@
	@$(LD) -o $@ $(OBJS) $(LDFLAGS) $(ARCHIVES) $(LIBS)
