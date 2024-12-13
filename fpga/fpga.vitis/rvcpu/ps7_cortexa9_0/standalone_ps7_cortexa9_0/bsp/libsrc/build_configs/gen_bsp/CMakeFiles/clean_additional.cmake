# Additional clean files
cmake_minimum_required(VERSION 3.16)

if("${CONFIG}" STREQUAL "" OR "${CONFIG}" STREQUAL "")
  file(REMOVE_RECURSE
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/ps7_cortexa9_0/standalone_ps7_cortexa9_0/bsp/include/sleep.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/ps7_cortexa9_0/standalone_ps7_cortexa9_0/bsp/include/xiltimer.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/ps7_cortexa9_0/standalone_ps7_cortexa9_0/bsp/include/xtimer_config.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/ps7_cortexa9_0/standalone_ps7_cortexa9_0/bsp/lib/libxiltimer.a"
  )
endif()
