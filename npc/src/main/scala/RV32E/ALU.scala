package RV32E

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

class ALUControlOutput extends Bundle {
  val aluSel = Output(UInt(3.W))
  val isArith = Output(Bool())
  val isLeft = Output(Bool())
  val isUnsigned = Output(Bool())
  val isSub = Output(Bool())
}

class ALUControlIO extends ALUControlOutput {
  val aluCtr = Input(UInt(4.W))
}

object ALUOutSel {
  val selectAdder = "b000".U(3.W)
  val selectShift1 = "b001".U(3.W)
  val selectSlt = "b010".U(3.W)
  val selectB = "b011".U(3.W)
  val selectXor = "b100".U(3.W)
  val selectShift2 = "b101".U(3.W)
  val selectOr = "b110".U(3.W)
  val selectAnd = "b111".U(3.W)
}
import ALUOutSel._

class ALUControl extends Module {
  val io = IO(new ALUControlIO)

  case class ALUControlPattern(
    val aluCtr3: BitPat = BitPat.dontCare(1),
    val aluCtr2_0: BitPat
  ) extends DecodePattern {
    def bitPat: BitPat = aluCtr3 ## aluCtr2_0
  }

  object ALUControlField extends DecodeField[ALUControlPattern, ALUControlOutput] {
    def name: String = "ALU Control"
    def chiselType: ALUControlOutput = new ALUControlOutput
    def genTable(op: ALUControlPattern): BitPat = {
      val aluSel = op.aluCtr2_0
      val isArith = op.aluCtr3
      val isLeft = op.aluCtr2_0(2).rawString match {
        case "0" => BitPat("b1".U(1.W))
        case "1" => BitPat("b0".U(1.W))
        case _ => BitPat.dontCare(1)
      }
      val isUnsigned = op.aluCtr3
      val isSub = op.aluCtr3
      aluSel ## isArith ## isLeft ## isUnsigned ## isSub
    }
  }

  val possiblePatterns = Seq(
    ALUControlPattern(aluCtr3 = BitPat(0.U(1.W)), aluCtr2_0 = BitPat("b000".U(3.W))),
    ALUControlPattern(aluCtr3 = BitPat(1.U(1.W)), aluCtr2_0 = BitPat("b000".U(3.W))),
    ALUControlPattern(aluCtr2_0 = BitPat("b001".U(3.W))),
    ALUControlPattern(aluCtr3 = BitPat(0.U(1.W)), aluCtr2_0 = BitPat("b010".U(3.W))),
    ALUControlPattern(aluCtr3 = BitPat(1.U(1.W)), aluCtr2_0 = BitPat("b010".U(3.W))),
    ALUControlPattern(aluCtr2_0 = BitPat("b011".U(3.W))),
    ALUControlPattern(aluCtr2_0 = BitPat("b100".U(3.W))),
    ALUControlPattern(aluCtr3 = BitPat(0.U(1.W)), aluCtr2_0 = BitPat("b101".U(3.W))),
    ALUControlPattern(aluCtr3 = BitPat(1.U(1.W)), aluCtr2_0 = BitPat("b101".U(3.W))),
    ALUControlPattern(aluCtr2_0 = BitPat("b110".U(3.W))),
    ALUControlPattern(aluCtr2_0 = BitPat("b111".U(3.W))),
  )

  val decodeTable = new DecodeTable(possiblePatterns, Seq(ALUControlField))

  val decodeResult = decodeTable.decode(io.aluCtr)(ALUControlField)

  io.aluSel := decodeResult.aluSel
  io.isArith := decodeResult.isArith
  io.isLeft := decodeResult.isLeft
  io.isUnsigned := decodeResult.isUnsigned
  io.isSub := decodeResult.isSub
}

class ALUIO(xlen: Int = 32) extends Bundle {
  val aluCtr = Input(UInt(4.W))
  val inA = Input(UInt(xlen.W))
  val inB = Input(UInt(xlen.W))
  val aluOut = Output(UInt(xlen.W))
  val less = Output(Bool())
  val zero = Output(Bool())
}

class ALU(xlen: Int = 32) extends Module {
  val io = IO(new ALUIO(xlen))

  val aluControl = Module(new ALUControl)
  val adder = Module(new Adder(xlen))
  val barrelShift = Module(new BarrelShift(xlen))

  aluControl.io.aluCtr := io.aluCtr

  adder.io.addSub := aluControl.io.isSub
  adder.io.ina := io.inA.asSInt
  adder.io.inb := io.inB.asSInt

  barrelShift.io.in := io.inA
  barrelShift.io.shamt := io.inB(4, 0)
  barrelShift.io.isArith := aluControl.io.isArith
  barrelShift.io.isLeft := aluControl.io.isLeft

  val less =  Mux(
    aluControl.io.isUnsigned,
    adder.io.carry ^ aluControl.io.isSub,
    adder.io.overflow ^ adder.io.result(xlen-1).asBool
  )
  val zero = adder.io.zero
  val adderOut = adder.io.result.asUInt
  val shiftOut = barrelShift.io.out
  val sltOut = 0.U((xlen-1).W) ## less.asUInt
  val BOut = io.inB
  val xorOut = io.inA ^ io.inB
  val orOut = io.inA | io.inB
  val andOut = io.inA & io.inB
  val aluOut = MuxLookup(aluControl.io.aluSel, adderOut)(Seq(
    selectAdder -> adderOut,
    selectShift1 -> shiftOut,
    selectSlt -> sltOut,
    selectB -> BOut,
    selectXor -> xorOut,
    selectShift2 -> shiftOut,
    selectOr -> orOut,
    selectAnd -> andOut
  ))

  io.aluOut := aluOut
  io.less := less
  io.zero := zero
}

object ALU extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new ALU(), Array("--target-dir", "generate"), firtoolOptions)
}