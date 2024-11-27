import "DPI-C" function void decode_inst(input int unsigned inst, input longint exec_cycle);
import "DPI-C" function void sync_rvcpu(input int unsigned npc, input int unsigned inst);

module InstTracer (
  input clock,
  input [31:0] npc,
  input [31:0] inst,
  input [63:0] exec_cycle,
  input en
);
always @(posedge clock) begin
  if (en) begin
    decode_inst(inst, exec_cycle);
    sync_rvcpu(npc, inst);
  end
end

endmodule //InstTracer
