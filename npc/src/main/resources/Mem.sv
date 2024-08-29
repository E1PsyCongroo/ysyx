import "DPI-C" function int unsigned pmem_read(input int unsigned raddr);
import "DPI-C" function void pmem_write(input int unsigned waddr, input int unsigned wdata, input byte wmask);

module Mem(
  input clock,
  input reset,
  input avalid,
  output aready,

  input [31:0] raddr,

  input wen,
  input [31:0] waddr,
  input [31:0] wdata,
  input [3:0] wmask,

  output reg [31:0] rdata,
  output reg dvalid,
  input dready
);

  // 0 for take addr, 1 for mem access
  reg state;
  reg [31:0] read_data;
  wire afire = avalid & aready;
  wire dfire = dvalid & dready;
  always @(posedge clock) begin
    if (reset) begin
      state <= 0;
      read_data <= 0;
    end
    else if (state == 0 && afire && !wen) begin
      state <= 1;
    end
    else if (state == 1 && dfire) begin
      state <= 0;
    end
  end
  assign aready = state == 0;

  always @(posedge clock) begin
    if (state == 0 && afire) begin
      read_data <= pmem_read(raddr);
      if (wen) pmem_write(waddr, wdata, {4'h0, wmask});
      dvalid <= 0;
    end
    if (state == 1) begin
      rdata <= read_data;
      dvalid <= 1;
    end
  end

endmodule
