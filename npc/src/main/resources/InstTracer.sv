import "DPI-C" function void decode_inst(input int unsigned inst, input longint exec_cycle);

module InstTracer (
  input clock,
  input [31:0] npc,
  input [31:0] inst,
  input [63:0] exec_cycle,
  input en
);
always @(posedge clock) begin
  if (en) decode_inst(inst, exec_cycle);
end

endmodule //InstTracer
