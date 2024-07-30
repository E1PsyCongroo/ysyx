package scala3.lab6
import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Shifter(depth: Int) extends Module {
  val io = IO(new Bundle{
    val in = Input(UInt(1.W))
    val d = Input(UInt(depth.W))
    val op = Input(UInt(3.W))
    val q = Output(UInt(depth.W))
  })
  val reg = RegInit(0.U(depth.W))
  reg := MuxLookup(io.op, 0.U)(Seq(
    "b000".U -> 0.U(depth.W),
    "b001".U -> io.d,
    "b010".U -> 0.U(1.W) ## reg(depth-1, 1),
    "b011".U -> reg(depth-2, 0) ## 0.U(1.W),
    "b100".U -> reg(depth-1) ## reg(depth-1, 1),
    "b101".U -> io.in ## reg(depth-1, 1),
    "b110".U -> reg(0) ## reg(depth-1, 1),
    "b111".U -> reg(depth-2, 0) ## reg(depth-1),
  ))
  io.q := reg
}

class LSFR extends Module {
  val io = IO(new Bundle{
    val out = Output(UInt(8.W))
  })
  val reg = RegInit(1.U(8.W))
  val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
  reg := Mux(reg === 0.U, 1.U, newBit ## reg(7, 1))
  io.out := reg
}

class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val sw = Input(UInt(4.W))
    val seg = Output(UInt(8.W))
  })

  val sevSeg = WireDefault(0.U(8.W))
  val sevSegNums = VecInit(Seq(
    "b11111101".U(8.W),     // 0
    "b01100000".U(8.W),     // 1
    "b11011010".U(8.W),     // 2
    "b11110010".U(8.W),     // 3
    "b01100110".U(8.W),     // 4
    "b10110110".U(8.W),     // 5
    "b10111110".U(8.W),     // 6
    "b11100000".U(8.W),     // 7
    "b11111110".U(8.W),     // 8
    "b11100110".U(8.W),     // 9
    "b11101110".U(8.W),     // A
    "b11111110".U(8.W),     // B
    "b10011100".U(8.W),     // C
    "b11111100".U(8.W),     // D
    "b10011110".U(8.W),     // E
    "b10001110".U(8.W),     // F
  ))
  sevSeg := sevSegNums(io.sw)
  io.seg := ~sevSeg
}

class Lab6 extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(8.W))
    val seg0 = Output(UInt(8.W))
    val seg1 = Output(UInt(8.W))
  })
  val lsfr = Module(new LSFR)
  io.out := lsfr.io.out
  lsfr.clock := clock
  lsfr.reset := reset

  val sevSeg0 = Module(new SevenSegDecoder)
  sevSeg0.io.sw := io.out(3, 0)
  io.seg0 := sevSeg0.io.seg

  val sevSeg1 = Module(new SevenSegDecoder)
  sevSeg1.io.sw := io.out(7, 4)
  io.seg1 := sevSeg1.io.seg
}

object Lab6 extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new Lab6(), args, firtoolOptions)
}
