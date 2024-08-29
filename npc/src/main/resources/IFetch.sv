import "DPI-C" function int unsigned ifetch(input int unsigned raddr);

module IFetch (
  input clock,
  input reset,
  input [31:0] pc,
  input avalid,
  output reg aready,
  output reg [31:0] instruction,
  output reg dvalid,
  input  dready
);

  // 0 for take pc, 1 for send instruction
  reg state;
  reg [31:0] read_addr;
  wire afire = avalid & aready;
  wire dfire = dvalid & dready;
  always @(posedge clock) begin
    if (reset) begin
      state <= 1;
      read_addr <= 32'h8000_0000;
      instruction <= 0;
      dvalid <= 0;
    end
    else if (state == 0 && afire) begin
      state <= 1;
    end
    else if (state == 1 && dfire) begin
      state <= 0;
    end
  end
  assign aready = state == 0;

  always @(posedge clock) begin
    if (state == 0 && afire) begin
      read_addr <= pc;
      dvalid <= 0;
    end
    if (state == 1) begin
      instruction <= ifetch(read_addr);
      dvalid  <= 1;
    end
  end

endmodule //IFetch
