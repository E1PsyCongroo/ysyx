package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class BarrelShift(w: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(w.W))
    val shamt = Input(UInt(log2Up(w).W))
    val isLeft = Input(Bool())
    val isArith = Input(Bool())
    val out = Output(UInt(w.W))
  })
  val leftIn = Mux(io.isArith, io.in(w-1), false.B) // 右移时从左边移入的位
  def layer(din: Seq[Bool], n: Int): Seq[Bool] = { // 描述第n级选择器如何排布
    val s = 1 << n   // 需要移动的位数
    def shiftRight(i: Int) = if (i + s >= w) leftIn else din(i + s) // 描述右移时第i位输出
    def shiftLeft (i: Int) = if (i < s) false.B else din(i - s) // 描述左移时第i位输出
    val sel = Cat(io.isLeft, io.shamt(n)) // 将移位方向和移位量作为选择器的选择信号
    din.zipWithIndex.map{ case (b, i) =>                // 对于每一位输入b,
      VecInit(b, shiftRight(i), b, shiftLeft(i))(sel) } // 都从4种输入中选择一种作为输出
  }
  def barrelshift(din: Seq[Bool], k: Int): Seq[Bool] = // 描述有k级的桶形移位器如何排布
    if (k == 0) din  // 若移位器只有0级, 则结果和输入相同
    // 否则实例化一个有k-1级的桶形移位器和第k-1级选择器, 并将后者的输出作为前者的输入
    else barrelshift(layer(din, k - 1), k - 1)
  io.out := Cat(barrelshift(io.in.asBools, log2Up(w)).reverse) // 实例化一个有log2(w)级的桶形移位器
}

object BarrelShift extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new BarrelShift(32), Array("--target-dir", "generate"), firtoolOptions)
}