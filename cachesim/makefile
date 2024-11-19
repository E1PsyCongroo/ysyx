CC = gcc

BUILD_DIR = $(abspath ./build)
TARGET = $(BUILD_DIR)/cachesim

# 搜索源文件和头文件
CSRC += $(abspath $(shell find ./src -name "*.c"))
OBJFILES = $(patsubst $(abspath ./src)/%, $(BUILD_DIR)/%, $(CSRC:.c=.o))
DEPS = $(OBJFILES:.o=.d)

INC_FILES += $(abspath $(shell find ./include -name "*.h"))
INCPATH += $(abspath ./include)

CFLAGS += $(addprefix -I, $(INCPATH))
CFLAGS += -Wall -Werror -MMD -std=gnu11 -ggdb
LDFLAGS += -lm

all: $(TARGET)

# 生成 .o 文件时，将输出放入 BUILD_DIR 中
$(BUILD_DIR)/%.o: ./src/%.c
	@echo + CC $<
	@mkdir -p $(dir $@)
	@$(CC) $(CFLAGS) -c $< -o $@

-include $(DEPS)

# 链接最终的目标文件
$(TARGET): $(OBJFILES)
	@echo + LD $@
	@mkdir -p $(BUILD_DIR)
	@$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

# 运行目标
run: $(TARGET)
	$(TARGET) $(ARGS)

# 清理目标
clean:
	-rm -rf $(BUILD_DIR)

.PHONY: all run clean
