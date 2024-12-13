-- Copyright 1986-2022 Xilinx, Inc. All Rights Reserved.
-- Copyright 2022-2024 Advanced Micro Devices, Inc. All Rights Reserved.
-- --------------------------------------------------------------------------------
-- Tool Version: Vivado v.2024.2 (lin64) Build 5239630 Fri Nov 08 22:34:34 MST 2024
-- Date        : Tue Dec 10 13:49:04 2024
-- Host        : FocusedXYArchlinuxLaptop running 64-bit Arch Linux
-- Command     : write_vhdl -force -mode synth_stub
--               /home/focused_xy/cs/ysyx/fpga/fpga.gen/sources_1/bd/TOP/ip/TOP_FPGA_RVCPU_wrapper_0_0/TOP_FPGA_RVCPU_wrapper_0_0_stub.vhdl
-- Design      : TOP_FPGA_RVCPU_wrapper_0_0
-- Purpose     : Stub declaration of top-level module interface
-- Device      : xc7z020clg400-1
-- --------------------------------------------------------------------------------
library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity TOP_FPGA_RVCPU_wrapper_0_0 is
  Port ( 
    clock : in STD_LOGIC;
    reset : in STD_LOGIC;
    io_master_awready : in STD_LOGIC;
    io_master_awvalid : out STD_LOGIC;
    io_master_awaddr : out STD_LOGIC_VECTOR ( 31 downto 0 );
    io_master_wready : in STD_LOGIC;
    io_master_wvalid : out STD_LOGIC;
    io_master_wdata : out STD_LOGIC_VECTOR ( 31 downto 0 );
    io_master_wstrb : out STD_LOGIC_VECTOR ( 3 downto 0 );
    io_master_bready : out STD_LOGIC;
    io_master_bvalid : in STD_LOGIC;
    io_master_bresp : in STD_LOGIC_VECTOR ( 1 downto 0 );
    io_master_arready : in STD_LOGIC;
    io_master_arvalid : out STD_LOGIC;
    io_master_araddr : out STD_LOGIC_VECTOR ( 31 downto 0 );
    io_master_rready : out STD_LOGIC;
    io_master_rvalid : in STD_LOGIC;
    io_master_rresp : in STD_LOGIC_VECTOR ( 1 downto 0 );
    io_master_rdata : in STD_LOGIC_VECTOR ( 31 downto 0 );
    io_master_awid : out STD_LOGIC_VECTOR ( 3 downto 0 );
    io_master_awlen : out STD_LOGIC_VECTOR ( 7 downto 0 );
    io_master_awsize : out STD_LOGIC_VECTOR ( 2 downto 0 );
    io_master_awburst : out STD_LOGIC_VECTOR ( 1 downto 0 );
    io_master_wlast : out STD_LOGIC;
    io_master_bid : in STD_LOGIC_VECTOR ( 3 downto 0 );
    io_master_arid : out STD_LOGIC_VECTOR ( 3 downto 0 );
    io_master_arlen : out STD_LOGIC_VECTOR ( 7 downto 0 );
    io_master_arsize : out STD_LOGIC_VECTOR ( 2 downto 0 );
    io_master_arburst : out STD_LOGIC_VECTOR ( 1 downto 0 );
    io_master_rlast : in STD_LOGIC;
    io_master_rid : in STD_LOGIC_VECTOR ( 3 downto 0 )
  );

  attribute CHECK_LICENSE_TYPE : string;
  attribute CHECK_LICENSE_TYPE of TOP_FPGA_RVCPU_wrapper_0_0 : entity is "TOP_FPGA_RVCPU_wrapper_0_0,FPGA_RVCPU_wrapper,{}";
  attribute CORE_GENERATION_INFO : string;
  attribute CORE_GENERATION_INFO of TOP_FPGA_RVCPU_wrapper_0_0 : entity is "TOP_FPGA_RVCPU_wrapper_0_0,FPGA_RVCPU_wrapper,{x_ipProduct=Vivado 2024.2,x_ipVendor=xilinx.com,x_ipLibrary=module_ref,x_ipName=FPGA_RVCPU_wrapper,x_ipVersion=1.0,x_ipCoreRevision=1,x_ipLanguage=VERILOG,x_ipSimLanguage=MIXED}";
  attribute DowngradeIPIdentifiedWarnings : string;
  attribute DowngradeIPIdentifiedWarnings of TOP_FPGA_RVCPU_wrapper_0_0 : entity is "yes";
  attribute IP_DEFINITION_SOURCE : string;
  attribute IP_DEFINITION_SOURCE of TOP_FPGA_RVCPU_wrapper_0_0 : entity is "module_ref";
end TOP_FPGA_RVCPU_wrapper_0_0;

architecture stub of TOP_FPGA_RVCPU_wrapper_0_0 is
  attribute syn_black_box : boolean;
  attribute black_box_pad_pin : string;
  attribute syn_black_box of stub : architecture is true;
  attribute black_box_pad_pin of stub : architecture is "clock,reset,io_master_awready,io_master_awvalid,io_master_awaddr[31:0],io_master_wready,io_master_wvalid,io_master_wdata[31:0],io_master_wstrb[3:0],io_master_bready,io_master_bvalid,io_master_bresp[1:0],io_master_arready,io_master_arvalid,io_master_araddr[31:0],io_master_rready,io_master_rvalid,io_master_rresp[1:0],io_master_rdata[31:0],io_master_awid[3:0],io_master_awlen[7:0],io_master_awsize[2:0],io_master_awburst[1:0],io_master_wlast,io_master_bid[3:0],io_master_arid[3:0],io_master_arlen[7:0],io_master_arsize[2:0],io_master_arburst[1:0],io_master_rlast,io_master_rid[3:0]";
  attribute X_INTERFACE_INFO : string;
  attribute X_INTERFACE_INFO of clock : signal is "xilinx.com:signal:clock:1.0 clock CLK";
  attribute X_INTERFACE_MODE : string;
  attribute X_INTERFACE_MODE of clock : signal is "slave";
  attribute X_INTERFACE_PARAMETER : string;
  attribute X_INTERFACE_PARAMETER of clock : signal is "XIL_INTERFACENAME clock, ASSOCIATED_BUSIF io_master, ASSOCIATED_RESET reset, FREQ_HZ 100000000, FREQ_TOLERANCE_HZ 0, PHASE 0.0, CLK_DOMAIN TOP_processing_system7_0_0_FCLK_CLK0, INSERT_VIP 0";
  attribute X_INTERFACE_INFO of reset : signal is "xilinx.com:signal:reset:1.0 reset RST";
  attribute X_INTERFACE_MODE of reset : signal is "slave";
  attribute X_INTERFACE_PARAMETER of reset : signal is "XIL_INTERFACENAME reset, POLARITY ACTIVE_HIGH, INSERT_VIP 0";
  attribute X_INTERFACE_INFO of io_master_awready : signal is "xilinx.com:interface:aximm:1.0 io_master AWREADY";
  attribute X_INTERFACE_MODE of io_master_awready : signal is "master";
  attribute X_INTERFACE_PARAMETER of io_master_awready : signal is "XIL_INTERFACENAME io_master, DATA_WIDTH 32, PROTOCOL AXI4, FREQ_HZ 100000000, ID_WIDTH 4, ADDR_WIDTH 32, AWUSER_WIDTH 0, ARUSER_WIDTH 0, WUSER_WIDTH 0, RUSER_WIDTH 0, BUSER_WIDTH 0, READ_WRITE_MODE READ_WRITE, HAS_BURST 1, HAS_LOCK 0, HAS_PROT 0, HAS_CACHE 0, HAS_QOS 0, HAS_REGION 0, HAS_WSTRB 1, HAS_BRESP 1, HAS_RRESP 1, SUPPORTS_NARROW_BURST 1, NUM_READ_OUTSTANDING 2, NUM_WRITE_OUTSTANDING 2, MAX_BURST_LENGTH 256, PHASE 0.0, CLK_DOMAIN TOP_processing_system7_0_0_FCLK_CLK0, NUM_READ_THREADS 1, NUM_WRITE_THREADS 1, RUSER_BITS_PER_BYTE 0, WUSER_BITS_PER_BYTE 0, INSERT_VIP 0";
  attribute X_INTERFACE_INFO of io_master_awvalid : signal is "xilinx.com:interface:aximm:1.0 io_master AWVALID";
  attribute X_INTERFACE_INFO of io_master_awaddr : signal is "xilinx.com:interface:aximm:1.0 io_master AWADDR";
  attribute X_INTERFACE_INFO of io_master_wready : signal is "xilinx.com:interface:aximm:1.0 io_master WREADY";
  attribute X_INTERFACE_INFO of io_master_wvalid : signal is "xilinx.com:interface:aximm:1.0 io_master WVALID";
  attribute X_INTERFACE_INFO of io_master_wdata : signal is "xilinx.com:interface:aximm:1.0 io_master WDATA";
  attribute X_INTERFACE_INFO of io_master_wstrb : signal is "xilinx.com:interface:aximm:1.0 io_master WSTRB";
  attribute X_INTERFACE_INFO of io_master_bready : signal is "xilinx.com:interface:aximm:1.0 io_master BREADY";
  attribute X_INTERFACE_INFO of io_master_bvalid : signal is "xilinx.com:interface:aximm:1.0 io_master BVALID";
  attribute X_INTERFACE_INFO of io_master_bresp : signal is "xilinx.com:interface:aximm:1.0 io_master BRESP";
  attribute X_INTERFACE_INFO of io_master_arready : signal is "xilinx.com:interface:aximm:1.0 io_master ARREADY";
  attribute X_INTERFACE_INFO of io_master_arvalid : signal is "xilinx.com:interface:aximm:1.0 io_master ARVALID";
  attribute X_INTERFACE_INFO of io_master_araddr : signal is "xilinx.com:interface:aximm:1.0 io_master ARADDR";
  attribute X_INTERFACE_INFO of io_master_rready : signal is "xilinx.com:interface:aximm:1.0 io_master RREADY";
  attribute X_INTERFACE_INFO of io_master_rvalid : signal is "xilinx.com:interface:aximm:1.0 io_master RVALID";
  attribute X_INTERFACE_INFO of io_master_rresp : signal is "xilinx.com:interface:aximm:1.0 io_master RRESP";
  attribute X_INTERFACE_INFO of io_master_rdata : signal is "xilinx.com:interface:aximm:1.0 io_master RDATA";
  attribute X_INTERFACE_INFO of io_master_awid : signal is "xilinx.com:interface:aximm:1.0 io_master AWID";
  attribute X_INTERFACE_INFO of io_master_awlen : signal is "xilinx.com:interface:aximm:1.0 io_master AWLEN";
  attribute X_INTERFACE_INFO of io_master_awsize : signal is "xilinx.com:interface:aximm:1.0 io_master AWSIZE";
  attribute X_INTERFACE_INFO of io_master_awburst : signal is "xilinx.com:interface:aximm:1.0 io_master AWBURST";
  attribute X_INTERFACE_INFO of io_master_wlast : signal is "xilinx.com:interface:aximm:1.0 io_master WLAST";
  attribute X_INTERFACE_INFO of io_master_bid : signal is "xilinx.com:interface:aximm:1.0 io_master BID";
  attribute X_INTERFACE_INFO of io_master_arid : signal is "xilinx.com:interface:aximm:1.0 io_master ARID";
  attribute X_INTERFACE_INFO of io_master_arlen : signal is "xilinx.com:interface:aximm:1.0 io_master ARLEN";
  attribute X_INTERFACE_INFO of io_master_arsize : signal is "xilinx.com:interface:aximm:1.0 io_master ARSIZE";
  attribute X_INTERFACE_INFO of io_master_arburst : signal is "xilinx.com:interface:aximm:1.0 io_master ARBURST";
  attribute X_INTERFACE_INFO of io_master_rlast : signal is "xilinx.com:interface:aximm:1.0 io_master RLAST";
  attribute X_INTERFACE_INFO of io_master_rid : signal is "xilinx.com:interface:aximm:1.0 io_master RID";
  attribute X_CORE_INFO : string;
  attribute X_CORE_INFO of stub : architecture is "FPGA_RVCPU_wrapper,Vivado 2024.2";
begin
end;
