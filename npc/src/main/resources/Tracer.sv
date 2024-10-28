import "DPI-C" function void isa_trace_read(input int addr, input int len, input int data);
import "DPI-C" function void isa_trace_write(input int addr, input int len, input int data);
module Tracer(
  input [31:0] raddr,
  input [31:0] rlen,
  input [31:0] rdata,
  input ren,
  input [31:0] waddr,
  input [31:0] wlen,
  input [31:0] wdata,
  input wen
);
  always @(posedge ren) begin
    isa_trace_read(raddr, rlen, rdata);
  end
  always @(posedge wen) begin
    isa_trace_write(waddr, wlen, wdata);
  end
endmodule
