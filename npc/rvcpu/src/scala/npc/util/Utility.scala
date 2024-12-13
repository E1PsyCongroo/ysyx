package rvcpu.utility

import rvcpu._
import rvcpu.core._
import chisel3._
import chisel3.util._

class AdderOut(width: Int) extends Bundle {
  val carry    = Output(Bool())
  val zero     = Output(Bool())
  val overflow = Output(Bool())
  val result   = Output(UInt(width.W))
}

object Adder {
  def apply(ina: UInt, inb: UInt, addSub: Bool): AdderOut = {
    require(ina.getWidth == inb.getWidth)
    val width  = ina.getWidth
    val out    = Wire(new AdderOut(width))
    val tA     = ina
    val tB     = inb
    val tnoCin = (Fill(width, addSub) ^ tB)
    val result = Wire(UInt((width + 1).W))
    result       := (tA +& tnoCin) + addSub
    out.result   := result(width - 1, 0)
    out.carry    := result(width).asBool
    out.zero     := ~(out.result.orR)
    out.overflow := (tA(width - 1) === tnoCin(width - 1)) && (result(width - 1) =/= tA(width - 1))
    out
  }
}

object Shifter {
  def apply(in: UInt, shamt: UInt, isLeft: Bool, isArith: Bool): UInt = {
    val width = in.getWidth
    require(width == (1 << shamt.getWidth))
    val sll = in << shamt
    val srl = in >> shamt
    val sra = (in.asSInt >> shamt).asUInt
    val out = Wire(UInt(width.W))
    out := Mux(isLeft, sll, Mux(isArith, sra, srl))
    out
  }
}

object BarrelShifter {
  def apply(in: UInt, shamt: UInt, isLeft: Bool, isArith: Bool): UInt = {
    val w = in.getWidth
    require(w == (1 << shamt.getWidth))
    val leftIn = Mux(isArith, in(w - 1), false.B) // 右移时从左边移入的位
    def layer(din: Seq[Bool], n: Int): Seq[Bool] = { // 描述第n级选择器如何排布
      val s = 1 << n // 需要移动的位数
      def shiftRight(i: Int) = if (i + s >= w) leftIn else din(i + s) // 描述右移时第i位输出
      def shiftLeft(i:  Int) = if (i < s) false.B else din(i - s) // 描述左移时第i位输出
      val sel = Cat(isLeft, shamt(n)) // 将移位方向和移位量作为选择器的选择信号
      din.zipWithIndex.map {
        case (b, i) => // 对于每一位输入b,
          VecInit(b, shiftRight(i), b, shiftLeft(i))(sel)
      } // 都从4种输入中选择一种作为输出
    }
    def barrelshift(din: Seq[Bool], k: Int): Seq[Bool] = // 描述有k级的桶形移位器如何排布
      if (k == 0) din // 若移位器只有0级, 则结果和输入相同
      // 否则实例化一个有k-1级的桶形移位器和第k-1级选择器, 并将后者的输出作为前者的输入
      else barrelshift(layer(din, k - 1), k - 1)
    val out = Wire(UInt(w.W))
    out := Cat(barrelshift(in.asBools, log2Up(w)).reverse) // 实例化一个有log2(w)级的桶形移位器
    out
  }
}

object LSFR {
  def width = 8.W
  def apply(next: Bool): UInt = {
    val reg    = RegInit(1.U(width))
    val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
    reg := Mux(next, Mux(reg === 0.U, 1.U, newBit ## reg(7, 1)), reg)
    reg
  }
}
