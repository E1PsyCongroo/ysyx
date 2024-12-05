#! /usr/bin/env bash

# 检查输入参数
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <input.bin> <output.coe>"
    exit 1
fi

input_file=$1
output_file=$2

# 创建.coe文件的头部信息
truncate -s 0 "$output_file"
echo "memory_initialization_radix=16;" > "$output_file"
echo "memory_initialization_vector=" >> "$output_file"

# 使用xxd读取二进制文件并格式化为32位宽
riscv64-unknown-elf-bin2hex -w 32 "$input_file" | sed 's/$/,/' >> "$output_file"

# 删除最后一行的逗号
sed -i '$ s/,$/;/' "$output_file"

echo "Conversion complete. Output saved to $output_file"
