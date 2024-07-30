package scala3.lab2
import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val sw = Input(UInt(3.W))
    val seg = Output(UInt(8.W))
  })

  val sevSeg = WireDefault(0.U(8.W))
  val sevSegNums = VecInit(Seq(
    "b11111101".U(8.W),
    "b01100000".U(8.W),
    "b11011010".U(8.W),
    "b11110010".U(8.W),
    "b01100110".U(8.W),
    "b10110110".U(8.W),
    "b10111110".U(8.W),
    "b11100000".U(8.W)
  ))
  sevSeg := sevSegNums(io.sw)
  io.seg := ~sevSeg
}

class Lab2 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))
    val out = Output(UInt(3.W))
    val outValid = Output(Bool())
    val seg = Output(UInt(8.W))
  })
  io.out := scala.lab2.Encoder.PriorityEncoder(8, io.in)
  val segDecoder = Module(new SevenSegDecoder)
  segDecoder.io.sw := io.out
  io.seg := segDecoder.io.seg
  io.outValid := io.out =/= 0.U
}

object Lab2 extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new Lab2(), args, firtoolOptions)
}