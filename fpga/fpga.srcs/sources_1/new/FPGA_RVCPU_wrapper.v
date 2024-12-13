`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company:
// Engineer:
//
// Create Date: 2024/12/08 20:22:21
// Design Name:
// Module Name: FPGA_RVCPU
// Project Name:
// Target Devices:
// Tool Versions:
// Description:
//
// Dependencies:
//
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
//
//////////////////////////////////////////////////////////////////////////////////


module FPGA_RVCPU_wrapper(
  input         clock,	// @[src/main/scala/npc/FPGA.scala:16:7]
                reset,	// @[src/main/scala/npc/FPGA.scala:16:7]
                io_master_awready,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output        io_master_awvalid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [31:0] io_master_awaddr,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input         io_master_wready,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output        io_master_wvalid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [31:0] io_master_wdata,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [3:0]  io_master_wstrb,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output        io_master_bready,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input         io_master_bvalid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input  [1:0]  io_master_bresp,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input         io_master_arready,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output        io_master_arvalid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [31:0] io_master_araddr,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output        io_master_rready,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input         io_master_rvalid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input  [1:0]  io_master_rresp,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input  [31:0] io_master_rdata,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [3:0]  io_master_awid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [7:0]  io_master_awlen,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [2:0]  io_master_awsize,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [1:0]  io_master_awburst,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output        io_master_wlast,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input  [3:0]  io_master_bid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [3:0]  io_master_arid,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [7:0]  io_master_arlen,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [2:0]  io_master_arsize,	// @[src/main/scala/npc/FPGA.scala:17:14]
  output [1:0]  io_master_arburst,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input         io_master_rlast,	// @[src/main/scala/npc/FPGA.scala:17:14]
  input  [3:0]  io_master_rid	// @[src/main/scala/npc/FPGA.scala:17:14]
    );
  FPGA_RVCPU FPGA_RVCPU_I(
    .clock(clock),
    .reset(reset),
    .io_interrupt(0),
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
