# Additional clean files
cmake_minimum_required(VERSION 3.16)

if("${CONFIG}" STREQUAL "" OR "${CONFIG}" STREQUAL "")
  file(REMOVE_RECURSE
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/diskio.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/ff.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/ffconf.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/sleep.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/xilffs.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/xilffs_config.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/xilrsa.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/xiltimer.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/include/xtimer_config.h"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/lib/libxilffs.a"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/lib/libxilrsa.a"
  "/home/focused_xy/cs/ysyx/fpga/fpga.vitis/rvcpu/zynq_fsbl/zynq_fsbl_bsp/lib/libxiltimer.a"
  )
endif()
