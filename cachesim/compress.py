#！ /usr/bin/env python
import struct
import sys

def compress_mem_trace(input_file, output_file):
    try:
        # 读取文本文件内容
        with open(input_file, "r") as infile:
            # 提取所有的地址值
            addresses = []
            for line in infile:
                # 移除多余的空格或换行符
                line = line.strip()
                # 忽略空行
                if not line:
                    continue
                try:
                    # 将地址值解析为整数
                    address = int(line, 16)
                    addresses.append(address)
                except ValueError:
                    print(f"跳过无法解析的行: {line}")
        
        # 写入二进制文件
        with open(output_file, "wb") as outfile:
            # 写入访问数量（uint32_t）
            outfile.write(struct.pack("I", len(addresses)))
            # 写入每个地址（uint32_t）
            for address in addresses:
                outfile.write(struct.pack("I", address))
        
        print(f"成功将 {input_file} 压缩为 {output_file}，包含 {len(addresses)} 条记录。")
    except Exception as e:
        print(f"发生错误: {e}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(f"usage: {sys.argv[0]} input_file output_file")
        sys.exit(1)
    compress_mem_trace(sys.argv[1], sys.argv[2])
