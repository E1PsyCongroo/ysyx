package rvcpu.core

import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.BundleLiterals._

class ALUControlOutput extends Bundle {
  val aluSel     = Output(UInt(3.W))
  val isArith    = Output(Bool())
  val isLeft     = Output(Bool())
  val isUnsigned = Output(Bool())
  val isSub      = Output(Bool())
}

class ALUControlIO extends ALUControlOutput {
  val aluCtr = Input(UInt(4.W))
}

object ALUOutSel {
  def getWidth     = 3
  val selectAdder  = BitPat("b000")
  val selectShift1 = BitPat("b001")
  val selectSlt    = BitPat("b010")
  val selectB      = BitPat("b011")
  val selectXor    = BitPat("b100")
  val selectShift2 = BitPat("b101")
  val selectOr     = BitPat("b110")
  val selectAnd    = BitPat("b111")
}
import ALUOutSel._

class ALUControl extends Module {
  val io = IO(new ALUControlIO)

  case class ALUControlPattern(
    val aluCtr3:   BitPat = BitPat.dontCare(1),
    val aluCtr2_0: BitPat)
      extends DecodePattern {
    def bitPat: BitPat = aluCtr3 ## aluCtr2_0
  }

  object ALUControlPattern {
    def apply(aluCtr: BitPat): ALUControlPattern = {
      ALUControlPattern(aluCtr3 = aluCtr(3), aluCtr2_0 = aluCtr(2, 0))
    }
  }

  object ALUControlField extends DecodeField[ALUControlPattern, ALUControlOutput] {
    def name:       String           = "ALU Control"
    def chiselType: ALUControlOutput = new ALUControlOutput
    def genTable(op: ALUControlPattern): BitPat = {
      val aluSel  = op.aluCtr2_0
      val isArith = op.aluCtr3
      val isLeft = op.aluCtr2_0(2).rawString match {
        case "0" => BitPat.Y(1)
        case "1" => BitPat.N(1)
        case _   => dc
      }
      val isUnsigned = op.aluCtr3
      val isSub = op.bitPat.rawString match {
        case "0000"                   => BitPat.N(1)
        case "1000" | "0010" | "1010" => BitPat.Y(1)
        case _                        => BitPat.dontCare(1)
      }
      aluSel ## isArith ## isLeft ## isUnsigned ## isSub
    }
  }

  val possiblePatterns = Seq(
    ALUControlPattern(ALUOp.aluAdd),
    ALUControlPattern(ALUOp.aluSub),
    ALUControlPattern(ALUOp.aluSll),
    ALUControlPattern(ALUOp.aluSlt),
    ALUControlPattern(ALUOp.aluSltu),
    ALUControlPattern(ALUOp.aluBout),
    ALUControlPattern(ALUOp.aluXor),
    ALUControlPattern(ALUOp.aluSrl),
    ALUControlPattern(ALUOp.aluSra),
    ALUControlPattern(ALUOp.aluOr),
    ALUControlPattern(ALUOp.aluAnd)
  )

  val decodeTable = new DecodeTable(possiblePatterns, Seq(ALUControlField))

  val decodeResult = decodeTable.decode(io.aluCtr)(ALUControlField)

  io.aluSel     := decodeResult.aluSel
  io.isArith    := decodeResult.isArith
  io.isLeft     := decodeResult.isLeft
  io.isUnsigned := decodeResult.isUnsigned
  io.isSub      := decodeResult.isSub
}

class ALUIO(xlen: Int = 32) extends Bundle {
  val aluCtr = Input(UInt(4.W))
  val inA    = Input(UInt(xlen.W))
  val inB    = Input(UInt(xlen.W))
  val aluOut = Output(UInt(xlen.W))
  val less   = Output(Bool())
  val zero   = Output(Bool())
}

class ALU(xlen: Int = 32) extends Module {
  val io = IO(new ALUIO(xlen))

  val aluControl  = Module(new ALUControl)
  val adder       = Module(new Adder(xlen))
  val barrelShift = Module(new BarrelShift(xlen))

  aluControl.io.aluCtr := io.aluCtr

  adder.io.addSub := aluControl.io.isSub
  adder.io.ina    := io.inA.asSInt
  adder.io.inb    := io.inB.asSInt

  barrelShift.io.in      := io.inA
  barrelShift.io.shamt   := io.inB(4, 0)
  barrelShift.io.isArith := aluControl.io.isArith
  barrelShift.io.isLeft  := aluControl.io.isLeft

  val less = Mux(
    aluControl.io.isUnsigned,
    adder.io.carry ^ aluControl.io.isSub,
    adder.io.overflow ^ adder.io.result(xlen - 1).asBool
  )
  val zero     = adder.io.zero
  val adderOut = adder.io.result.asUInt
  val shiftOut = barrelShift.io.out
  val sltOut   = 0.U((xlen - 1).W) ## less.asUInt
  val BOut     = io.inB
  val xorOut   = io.inA ^ io.inB
  val orOut    = io.inA | io.inB
  val andOut   = io.inA & io.inB
  val aluOut = MuxCase(
    adderOut,
    Seq(
      selectAdder -> adderOut,
      selectShift1 -> shiftOut,
      selectSlt -> sltOut,
      selectB -> BOut,
      selectXor -> xorOut,
      selectShift2 -> shiftOut,
      selectOr -> orOut,
      selectAnd -> andOut
    ).map { case (key, data) => (aluControl.io.aluSel === key, data) }
  )

  io.aluOut := aluOut
  io.less   := less
  io.zero   := zero
}
