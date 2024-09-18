import "DPI-C" function void sram_read(input int addr, input int data);
import "DPI-C" function void sram_write(input int waddr, input int wdata, input int len);
module SramTracer(
  input [31:0] raddr,
  input [31:0] rdata,
  input ren,
  input [31:0] waddr,
  input [31:0] wdata,
  input [31:0] wlen,
  input wen
);
  always @(posedge ren) begin
    sram_read(raddr, rdata);
  end
  always @(posedge wen) begin
    sram_write(waddr, wdata, wlen);
  end
endmodule
