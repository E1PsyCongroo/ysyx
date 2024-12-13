`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company:
// Engineer:
//
// Create Date: 2024/12/08 20:38:47
// Design Name:
// Module Name: BRAM
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


module BRAM(
  input s_axi_aclk,
  input s_axi_aresetn,
  input [3:0]s_axi_awid,
  input [31:0]s_axi_awaddr,
  input [7:0]s_axi_awlen,
  input [2:0]s_axi_awsize,
  input [1:0]s_axi_awburst,
  input s_axi_awvalid,
  output s_axi_awready,
  input [31:0]s_axi_wdata,
  input [3:0]s_axi_wstrb,
  input s_axi_wlast,
  input s_axi_wvalid,
  output s_axi_wready,
  output [3:0]s_axi_bid,
  output [1:0]s_axi_bresp,
  output s_axi_bvalid,
  input s_axi_bready,
  input [3:0]s_axi_arid,
  input [31:0]s_axi_araddr,
  input [7:0]s_axi_arlen,
  input [2:0]s_axi_arsize,
  input [1:0]s_axi_arburst,
  input s_axi_arvalid,
  output s_axi_arready,
  output [3:0]s_axi_rid,
  output [31:0]s_axi_rdata,
  output [1:0]s_axi_rresp,
  output s_axi_rlast,
  output s_axi_rvalid,
  input s_axi_rready
);

  RAM_IP bram (
    .s_aclk(s_axi_aclk),                // input wire s_aclk
    .s_aresetn(s_axi_aresetn),          // input wire s_aresetn
    .s_axi_awid(s_axi_awid),        // input wire [3 : 0] s_axi_awid
    .s_axi_awaddr(s_axi_awaddr),    // input wire [31 : 0] s_axi_awaddr
    .s_axi_awlen(s_axi_awlen),      // input wire [7 : 0] s_axi_awlen
    .s_axi_awsize(s_axi_awsize),    // input wire [2 : 0] s_axi_awsize
    .s_axi_awburst(s_axi_awburst),  // input wire [1 : 0] s_axi_awburst
    .s_axi_awvalid(s_axi_awvalid),  // input wire s_axi_awvalid
    .s_axi_awready(s_axi_awready),  // output wire s_axi_awready
    .s_axi_wdata(s_axi_wdata),      // input wire [31 : 0] s_axi_wdata
    .s_axi_wstrb(s_axi_wstrb),      // input wire [3 : 0] s_axi_wstrb
    .s_axi_wlast(s_axi_wlast),      // input wire s_axi_wlast
    .s_axi_wvalid(s_axi_wvalid),    // input wire s_axi_wvalid
    .s_axi_wready(s_axi_wready),    // output wire s_axi_wready
    .s_axi_bid(s_axi_bid),          // output wire [3 : 0] s_axi_bid
    .s_axi_bresp(s_axi_bresp),      // output wire [1 : 0] s_axi_bresp
    .s_axi_bvalid(s_axi_bvalid),    // output wire s_axi_bvalid
    .s_axi_bready(s_axi_bready),    // input wire s_axi_bready
    .s_axi_arid(s_axi_arid),        // input wire [3 : 0] s_axi_arid
    .s_axi_araddr(s_axi_araddr),    // input wire [31 : 0] s_axi_araddr
    .s_axi_arlen(s_axi_arlen),      // input wire [7 : 0] s_axi_arlen
    .s_axi_arsize(s_axi_arsize),    // input wire [2 : 0] s_axi_arsize
    .s_axi_arburst(s_axi_arburst),  // input wire [1 : 0] s_axi_arburst
    .s_axi_arvalid(s_axi_arvalid),  // input wire s_axi_arvalid
    .s_axi_arready(s_axi_arready),  // output wire s_axi_arready
    .s_axi_rid(s_axi_rid),          // output wire [3 : 0] s_axi_rid
    .s_axi_rdata(s_axi_rdata),      // output wire [31 : 0] s_axi_rdata
    .s_axi_rresp(s_axi_rresp),      // output wire [1 : 0] s_axi_rresp
    .s_axi_rlast(s_axi_rlast),      // output wire s_axi_rlast
    .s_axi_rvalid(s_axi_rvalid),    // output wire s_axi_rvalid
    .s_axi_rready(s_axi_rready)    // input wire s_axi_rready
  );
endmodule
