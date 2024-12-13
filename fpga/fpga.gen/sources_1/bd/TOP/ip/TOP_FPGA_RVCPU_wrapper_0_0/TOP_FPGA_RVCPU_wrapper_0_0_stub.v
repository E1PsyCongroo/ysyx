// Copyright 1986-2022 Xilinx, Inc. All Rights Reserved.
// Copyright 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
// --------------------------------------------------------------------------------
// Tool Version: Vivado v.2024.2 (lin64) Build 5239630 Fri Nov 08 22:34:34 MST 2024
// Date        : Tue Dec 10 13:49:04 2024
// Host        : FocusedXYArchlinuxLaptop running 64-bit Arch Linux
// Command     : write_verilog -force -mode synth_stub
//               /home/focused_xy/cs/ysyx/fpga/fpga.gen/sources_1/bd/TOP/ip/TOP_FPGA_RVCPU_wrapper_0_0/TOP_FPGA_RVCPU_wrapper_0_0_stub.v
// Design      : TOP_FPGA_RVCPU_wrapper_0_0
// Purpose     : Stub declaration of top-level module interface
// Device      : xc7z020clg400-1
// --------------------------------------------------------------------------------

// This empty module with port declaration file causes synthesis tools to infer a black box for IP.
// The synthesis directives are for Synopsys Synplify support to prevent IO buffer insertion.
// Please paste the declaration into a Verilog source file or add the file as an additional source.
(* CHECK_LICENSE_TYPE = "TOP_FPGA_RVCPU_wrapper_0_0,FPGA_RVCPU_wrapper,{}" *) (* CORE_GENERATION_INFO = "TOP_FPGA_RVCPU_wrapper_0_0,FPGA_RVCPU_wrapper,{x_ipProduct=Vivado 2024.2,x_ipVendor=xilinx.com,x_ipLibrary=module_ref,x_ipName=FPGA_RVCPU_wrapper,x_ipVersion=1.0,x_ipCoreRevision=1,x_ipLanguage=VERILOG,x_ipSimLanguage=MIXED}" *) (* DowngradeIPIdentifiedWarnings = "yes" *) 
(* IP_DEFINITION_SOURCE = "module_ref" *) (* X_CORE_INFO = "FPGA_RVCPU_wrapper,Vivado 2024.2" *) 
module TOP_FPGA_RVCPU_wrapper_0_0(clock, reset, io_master_awready, 
  io_master_awvalid, io_master_awaddr, io_master_wready, io_master_wvalid, io_master_wdata, 
  io_master_wstrb, io_master_bready, io_master_bvalid, io_master_bresp, io_master_arready, 
  io_master_arvalid, io_master_araddr, io_master_rready, io_master_rvalid, io_master_rresp, 
  io_master_rdata, io_master_awid, io_master_awlen, io_master_awsize, io_master_awburst, 
  io_master_wlast, io_master_bid, io_master_arid, io_master_arlen, io_master_arsize, 
  io_master_arburst, io_master_rlast, io_master_rid)
/* synthesis syn_black_box black_box_pad_pin="reset,io_master_awready,io_master_awvalid,io_master_awaddr[31:0],io_master_wready,io_master_wvalid,io_master_wdata[31:0],io_master_wstrb[3:0],io_master_bready,io_master_bvalid,io_master_bresp[1:0],io_master_arready,io_master_arvalid,io_master_araddr[31:0],io_master_rready,io_master_rvalid,io_master_rresp[1:0],io_master_rdata[31:0],io_master_awid[3:0],io_master_awlen[7:0],io_master_awsize[2:0],io_master_awburst[1:0],io_master_wlast,io_master_bid[3:0],io_master_arid[3:0],io_master_arlen[7:0],io_master_arsize[2:0],io_master_arburst[1:0],io_master_rlast,io_master_rid[3:0]" */
/* synthesis syn_force_seq_prim="clock" */;
  (* X_INTERFACE_INFO = "xilinx.com:signal:clock:1.0 clock CLK" *) (* X_INTERFACE_MODE = "slave" *) (* X_INTERFACE_PARAMETER = "XIL_INTERFACENAME clock, ASSOCIATED_BUSIF io_master, ASSOCIATED_RESET reset, FREQ_HZ 100000000, FREQ_TOLERANCE_HZ 0, PHASE 0.0, CLK_DOMAIN TOP_processing_system7_0_0_FCLK_CLK0, INSERT_VIP 0" *) input clock /* synthesis syn_isclock = 1 */;
  (* X_INTERFACE_INFO = "xilinx.com:signal:reset:1.0 reset RST" *) (* X_INTERFACE_MODE = "slave" *) (* X_INTERFACE_PARAMETER = "XIL_INTERFACENAME reset, POLARITY ACTIVE_HIGH, INSERT_VIP 0" *) input reset;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWREADY" *) (* X_INTERFACE_MODE = "master" *) (* X_INTERFACE_PARAMETER = "XIL_INTERFACENAME io_master, DATA_WIDTH 32, PROTOCOL AXI4, FREQ_HZ 100000000, ID_WIDTH 4, ADDR_WIDTH 32, AWUSER_WIDTH 0, ARUSER_WIDTH 0, WUSER_WIDTH 0, RUSER_WIDTH 0, BUSER_WIDTH 0, READ_WRITE_MODE READ_WRITE, HAS_BURST 1, HAS_LOCK 0, HAS_PROT 0, HAS_CACHE 0, HAS_QOS 0, HAS_REGION 0, HAS_WSTRB 1, HAS_BRESP 1, HAS_RRESP 1, SUPPORTS_NARROW_BURST 1, NUM_READ_OUTSTANDING 2, NUM_WRITE_OUTSTANDING 2, MAX_BURST_LENGTH 256, PHASE 0.0, CLK_DOMAIN TOP_processing_system7_0_0_FCLK_CLK0, NUM_READ_THREADS 1, NUM_WRITE_THREADS 1, RUSER_BITS_PER_BYTE 0, WUSER_BITS_PER_BYTE 0, INSERT_VIP 0" *) input io_master_awready;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWVALID" *) output io_master_awvalid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWADDR" *) output [31:0]io_master_awaddr;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WREADY" *) input io_master_wready;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WVALID" *) output io_master_wvalid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WDATA" *) output [31:0]io_master_wdata;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WSTRB" *) output [3:0]io_master_wstrb;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BREADY" *) output io_master_bready;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BVALID" *) input io_master_bvalid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BRESP" *) input [1:0]io_master_bresp;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARREADY" *) input io_master_arready;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARVALID" *) output io_master_arvalid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARADDR" *) output [31:0]io_master_araddr;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RREADY" *) output io_master_rready;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RVALID" *) input io_master_rvalid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RRESP" *) input [1:0]io_master_rresp;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RDATA" *) input [31:0]io_master_rdata;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWID" *) output [3:0]io_master_awid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWLEN" *) output [7:0]io_master_awlen;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWSIZE" *) output [2:0]io_master_awsize;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWBURST" *) output [1:0]io_master_awburst;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WLAST" *) output io_master_wlast;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BID" *) input [3:0]io_master_bid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARID" *) output [3:0]io_master_arid;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARLEN" *) output [7:0]io_master_arlen;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARSIZE" *) output [2:0]io_master_arsize;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARBURST" *) output [1:0]io_master_arburst;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RLAST" *) input io_master_rlast;
  (* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RID" *) input [3:0]io_master_rid;
endmodule
