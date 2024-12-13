import "DPI-C" function int unsigned rvcpu_pmem_read(input int unsigned raddr);
import "DPI-C" function void rvcpu_pmem_write(input int unsigned waddr, input int unsigned wdata, input byte wmask);

module DPIMem(
  input clock,
  input reset,
  input ren,
  input [31:0] raddr,
  output reg [31:0] rdata,

  input wen,
  input [31:0] waddr,
  input [31:0] wdata,
  input [3:0] wmask
);

  always @(*) begin
    if (ren && !reset) begin
      rdata = rvcpu_pmem_read(raddr);
    end
    else begin
      rdata = 0;
    end
  end

  always @(posedge clock) begin
    if (wen && !reset) begin
      rvcpu_pmem_write(waddr, wdata, {4'b0, wmask});
    end
  end
endmodule
