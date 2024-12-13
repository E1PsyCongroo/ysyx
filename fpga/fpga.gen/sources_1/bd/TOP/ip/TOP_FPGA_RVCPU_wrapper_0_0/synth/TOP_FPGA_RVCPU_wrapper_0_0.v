// (c) Copyright 1986-2022 Xilinx, Inc. All Rights Reserved.
// (c) Copyright 2022-2024 Advanced Micro Devices, Inc. All rights reserved.
// 
// This file contains confidential and proprietary information
// of AMD and is protected under U.S. and international copyright
// and other intellectual property laws.
// 
// DISCLAIMER
// This disclaimer is not a license and does not grant any
// rights to the materials distributed herewith. Except as
// otherwise provided in a valid license issued to you by
// AMD, and to the maximum extent permitted by applicable
// law: (1) THESE MATERIALS ARE MADE AVAILABLE "AS IS" AND
// WITH ALL FAULTS, AND AMD HEREBY DISCLAIMS ALL WARRANTIES
// AND CONDITIONS, EXPRESS, IMPLIED, OR STATUTORY, INCLUDING
// BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY, NON-
// INFRINGEMENT, OR FITNESS FOR ANY PARTICULAR PURPOSE; and
// (2) AMD shall not be liable (whether in contract or tort,
// including negligence, or under any other theory of
// liability) for any loss or damage of any kind or nature
// related to, arising under or in connection with these
// materials, including for any direct, or any indirect,
// special, incidental, or consequential loss or damage
// (including loss of data, profits, goodwill, or any type of
// loss or damage suffered as a result of any action brought
// by a third party) even if such damage or loss was
// reasonably foreseeable or AMD had been advised of the
// possibility of the same.
// 
// CRITICAL APPLICATIONS
// AMD products are not designed or intended to be fail-
// safe, or for use in any application requiring fail-safe
// performance, such as life-support or safety devices or
// systems, Class III medical devices, nuclear facilities,
// applications related to the deployment of airbags, or any
// other applications that could lead to death, personal
// injury, or severe property or environmental damage
// (individually and collectively, "Critical
// Applications"). Customer assumes the sole risk and
// liability of any use of AMD products in Critical
// Applications, subject only to applicable laws and
// regulations governing limitations on product liability.
// 
// THIS COPYRIGHT NOTICE AND DISCLAIMER MUST BE RETAINED AS
// PART OF THIS FILE AT ALL TIMES.
// 
// DO NOT MODIFY THIS FILE.


// IP VLNV: xilinx.com:module_ref:FPGA_RVCPU_wrapper:1.0
// IP Revision: 1

(* X_CORE_INFO = "FPGA_RVCPU_wrapper,Vivado 2024.2" *)
(* CHECK_LICENSE_TYPE = "TOP_FPGA_RVCPU_wrapper_0_0,FPGA_RVCPU_wrapper,{}" *)
(* CORE_GENERATION_INFO = "TOP_FPGA_RVCPU_wrapper_0_0,FPGA_RVCPU_wrapper,{x_ipProduct=Vivado 2024.2,x_ipVendor=xilinx.com,x_ipLibrary=module_ref,x_ipName=FPGA_RVCPU_wrapper,x_ipVersion=1.0,x_ipCoreRevision=1,x_ipLanguage=VERILOG,x_ipSimLanguage=MIXED}" *)
(* IP_DEFINITION_SOURCE = "module_ref" *)
(* DowngradeIPIdentifiedWarnings = "yes" *)
module TOP_FPGA_RVCPU_wrapper_0_0 (
  clock,
  reset,
  io_master_awready,
  io_master_awvalid,
  io_master_awaddr,
  io_master_wready,
  io_master_wvalid,
  io_master_wdata,
  io_master_wstrb,
  io_master_bready,
  io_master_bvalid,
  io_master_bresp,
  io_master_arready,
  io_master_arvalid,
  io_master_araddr,
  io_master_rready,
  io_master_rvalid,
  io_master_rresp,
  io_master_rdata,
  io_master_awid,
  io_master_awlen,
  io_master_awsize,
  io_master_awburst,
  io_master_wlast,
  io_master_bid,
  io_master_arid,
  io_master_arlen,
  io_master_arsize,
  io_master_arburst,
  io_master_rlast,
  io_master_rid
);

(* X_INTERFACE_INFO = "xilinx.com:signal:clock:1.0 clock CLK" *)
(* X_INTERFACE_MODE = "slave" *)
(* X_INTERFACE_PARAMETER = "XIL_INTERFACENAME clock, ASSOCIATED_BUSIF io_master, ASSOCIATED_RESET reset, FREQ_HZ 100000000, FREQ_TOLERANCE_HZ 0, PHASE 0.0, CLK_DOMAIN TOP_processing_system7_0_0_FCLK_CLK0, INSERT_VIP 0" *)
input wire clock;
(* X_INTERFACE_INFO = "xilinx.com:signal:reset:1.0 reset RST" *)
(* X_INTERFACE_MODE = "slave" *)
(* X_INTERFACE_PARAMETER = "XIL_INTERFACENAME reset, POLARITY ACTIVE_HIGH, INSERT_VIP 0" *)
input wire reset;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWREADY" *)
(* X_INTERFACE_MODE = "master" *)
(* X_INTERFACE_PARAMETER = "XIL_INTERFACENAME io_master, DATA_WIDTH 32, PROTOCOL AXI4, FREQ_HZ 100000000, ID_WIDTH 4, ADDR_WIDTH 32, AWUSER_WIDTH 0, ARUSER_WIDTH 0, WUSER_WIDTH 0, RUSER_WIDTH 0, BUSER_WIDTH 0, READ_WRITE_MODE READ_WRITE, HAS_BURST 1, HAS_LOCK 0, HAS_PROT 0, HAS_CACHE 0, HAS_QOS 0, HAS_REGION 0, HAS_WSTRB 1, HAS_BRESP 1, HAS_RRESP 1, SUPPORTS_NARROW_BURST 1, NUM_READ_OUTSTANDING 2, NUM_WRITE_OUTSTANDING 2, MAX_BURST_LENGTH 256, PHASE 0.0, CLK_DOMAIN TOP_processing_system7_0_0_FCLK_CLK0, NUM_READ_THREADS 1, \
NUM_WRITE_THREADS 1, RUSER_BITS_PER_BYTE 0, WUSER_BITS_PER_BYTE 0, INSERT_VIP 0" *)
input wire io_master_awready;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWVALID" *)
output wire io_master_awvalid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWADDR" *)
output wire [31 : 0] io_master_awaddr;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WREADY" *)
input wire io_master_wready;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WVALID" *)
output wire io_master_wvalid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WDATA" *)
output wire [31 : 0] io_master_wdata;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WSTRB" *)
output wire [3 : 0] io_master_wstrb;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BREADY" *)
output wire io_master_bready;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BVALID" *)
input wire io_master_bvalid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BRESP" *)
input wire [1 : 0] io_master_bresp;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARREADY" *)
input wire io_master_arready;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARVALID" *)
output wire io_master_arvalid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARADDR" *)
output wire [31 : 0] io_master_araddr;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RREADY" *)
output wire io_master_rready;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RVALID" *)
input wire io_master_rvalid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RRESP" *)
input wire [1 : 0] io_master_rresp;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RDATA" *)
input wire [31 : 0] io_master_rdata;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWID" *)
output wire [3 : 0] io_master_awid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWLEN" *)
output wire [7 : 0] io_master_awlen;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWSIZE" *)
output wire [2 : 0] io_master_awsize;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master AWBURST" *)
output wire [1 : 0] io_master_awburst;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master WLAST" *)
output wire io_master_wlast;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master BID" *)
input wire [3 : 0] io_master_bid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARID" *)
output wire [3 : 0] io_master_arid;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARLEN" *)
output wire [7 : 0] io_master_arlen;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARSIZE" *)
output wire [2 : 0] io_master_arsize;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master ARBURST" *)
output wire [1 : 0] io_master_arburst;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RLAST" *)
input wire io_master_rlast;
(* X_INTERFACE_INFO = "xilinx.com:interface:aximm:1.0 io_master RID" *)
input wire [3 : 0] io_master_rid;

  FPGA_RVCPU_wrapper inst (
    .clock(clock),
    .reset(reset),
    .io_master_awready(io_master_awready),
    .io_master_awvalid(io_master_awvalid),
    .io_master_awaddr(io_master_awaddr),
    .io_master_wready(io_master_wready),
    .io_master_wvalid(io_master_wvalid),
    .io_master_wdata(io_master_wdata),
    .io_master_wstrb(io_master_wstrb),
    .io_master_bready(io_master_bready),
    .io_master_bvalid(io_master_bvalid),
    .io_master_bresp(io_master_bresp),
    .io_master_arready(io_master_arready),
    .io_master_arvalid(io_master_arvalid),
    .io_master_araddr(io_master_araddr),
    .io_master_rready(io_master_rready),
    .io_master_rvalid(io_master_rvalid),
    .io_master_rresp(io_master_rresp),
    .io_master_rdata(io_master_rdata),
    .io_master_awid(io_master_awid),
    .io_master_awlen(io_master_awlen),
    .io_master_awsize(io_master_awsize),
    .io_master_awburst(io_master_awburst),
    .io_master_wlast(io_master_wlast),
    .io_master_bid(io_master_bid),
    .io_master_arid(io_master_arid),
    .io_master_arlen(io_master_arlen),
    .io_master_arsize(io_master_arsize),
    .io_master_arburst(io_master_arburst),
    .io_master_rlast(io_master_rlast),
    .io_master_rid(io_master_rid)
  );
endmodule
