import "DPI-C" function void decode_inst(input int unsigned inst, input longint exec_cycle);
import "DPI-C" function void sync_rvcpu(input int unsigned npc, input int unsigned inst, input int unsigned mtvec, input int unsigned mepc);

module InstTracer (
  input clock,
  input [31:0] npc,
  input [31:0] inst,
  input [31:0] mtvec,
  input [31:0] mepc,
  input [63:0] exec_cycle,
  input en
);
always @(posedge clock) begin
  if (en) begin
    decode_inst(inst, exec_cycle);
    sync_rvcpu(npc, inst, mtvec, mepc);
  end
end

endmodule //InstTracer
