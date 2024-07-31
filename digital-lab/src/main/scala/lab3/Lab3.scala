package scala3.lab3
import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import scala.lab3.Adder

class Lab3 extends Module {
  val io = IO(new Bundle {
    val inA      = Input(SInt(4.W))
    val inB      = Input(SInt(4.W))
    val op       = Input(UInt(3.W))
    val result   = Output(SInt(4.W))
    val carry    = Output(Bool())
    val zero     = Output(Bool())
    val overflow = Output(Bool())
  })
  val subFlag = io.op(0) || io.op(2, 1).andR
  val adder   = Module(new Adder(4))
  adder.io.ina    := io.inA
  adder.io.inb    := io.inB
  adder.io.addSub := subFlag
  val addResult  = adder.io.result
  val subResult  = adder.io.result
  val notResult  = ~io.inA
  val andResult  = io.inA & io.inB
  val orResult   = io.inA | io.inB
  val xorResult  = io.inA ^ io.inB
  val lessResult = Fill(4, adder.io.result(3).asUInt ^ adder.io.overflow.asUInt).asSInt
  val eqResult   = Fill(4, adder.io.zero.asUInt).asSInt
  io.result := MuxLookup(io.op, addResult)(
    Seq(
      "b000".U -> addResult,
      "b001".U -> subResult,
      "b010".U -> notResult,
      "b011".U -> andResult,
      "b100".U -> orResult,
      "b101".U -> xorResult,
      "b110".U -> lessResult,
      "b111".U -> eqResult
    )
  )
  io.carry    := adder.io.carry
  io.overflow := adder.io.overflow
  io.zero     := adder.io.zero
}

object Lab3 extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      // make yosys happy
      // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket"
    ).reduce(_ + "," + _)
  )
  ChiselStage.emitSystemVerilogFile(new Lab3(), args, firtoolOptions)
}
