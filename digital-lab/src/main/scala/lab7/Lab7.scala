import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline
import os.stat

class ps2Keyboard extends Module {
  val io = IO(new Bundle {
    val ps2Clk = Input(UInt(1.W))
    val ps2Data = Input(UInt(1.W))
    val nextdata_n = Input(UInt(1.W))
    val data = Output(UInt(8.W))
    val ready = Output(Bool())
    val overflow = Output(Bool())
  })

  val buffer = RegInit(VecInit(Seq.fill(10)(0.U(1.W))))
  val fifo = RegInit(VecInit(Seq.fill(8)(0.U(8.W))))
  val wPtr = RegInit(0.U(3.W))
  val rPtr = RegInit(0.U(3.W))
  val count = RegInit(0.U(4.W))
  val ps2ClkSync = RegInit(0.U(3.W))

  ps2ClkSync := ps2ClkSync(1, 0) ## io.ps2Clk
  val rising = ps2ClkSync(2) & ~ps2ClkSync(1)

  val readyReg = RegInit(false.B)
  val overflowReg = RegInit(false.B)

  when (readyReg) {
    when (io.nextdata_n === 0.U) {
      rPtr := rPtr + 1.U
      when (wPtr === (rPtr + 1.U)) {
        readyReg := false.B
      }
    }
  }

  when (rising) {
    when (count === 10.U) {
      when (buffer(0) === 0.U && io.ps2Data.asBool && (buffer.asUInt(9, 1).xorR)) {
        // fifo(wPtr) := Reverse(buffer.asUInt(8, 1))
        fifo(wPtr) := buffer.asUInt(8, 1)
        wPtr := wPtr + 1.U
        readyReg := true.B
        overflowReg := overflowReg | (rPtr === (wPtr + 1.U))
      }
      count := 0.U
    } otherwise {
      buffer(count) := io.ps2Data
      count := count + 1.U
    }
  }

  io.data := fifo(rPtr)
  io.ready := readyReg
  io.overflow := overflowReg
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

object SevenSegDecoder {
  def apply(sw: UInt) = {
    val sevSegDecoder = Module(new SevenSegDecoder)
    sevSegDecoder.io.sw := sw
    sevSegDecoder.io.seg
  }
}

class Lab7 extends Module {
  val io = IO(new Bundle{
    val ps2Clk = Input(UInt(1.W))
    val ps2Data = Input(UInt(1.W))
    val seg0 = Output(UInt(8.W))
    val seg1 = Output(UInt(8.W))
    val seg2 = Output(UInt(8.W))
    val seg3 = Output(UInt(8.W))
    val seg4 = Output(UInt(8.W))
    val seg5 = Output(UInt(8.W))
    val ready = Output(Bool())
    val overflow = Output(Bool())
  })
  val nextdata_n = RegInit(true.B)
  val keyboard = Module(new ps2Keyboard)
  keyboard.io.ps2Clk := io.ps2Clk
  keyboard.io.ps2Data := io.ps2Data
  keyboard.io.nextdata_n := nextdata_n
  val ready = keyboard.io.ready
  val data = keyboard.io.data

  object State extends ChiselEnum {
    val idle, pressed, release = Value
  }

  import State._
  val state = RegInit(idle)
  val readFlag = state === idle && ready
  val codeReg = RegEnable(data, readFlag)
  val code2ASCII = Mem(256, UInt(8.W))
  loadMemoryFromFileInline(code2ASCII, "/home/focused_xy/cs/ysyx/digital-lab/src/main/scala/lab7/code2ascii.hex")
  val asciiReg = RegEnable(code2ASCII.read(data), readFlag)

  val count = RegInit(0.U(8.W))
  count := Mux(readFlag, count + 1.U, count)

  state := MuxLookup(state, idle)(Seq(
    idle -> Mux(ready, pressed, idle),
    pressed -> Mux(ready && data === "hF0".U, release, pressed),
    release -> Mux(ready && data === codeReg, idle, release)
  ))

  nextdata_n := ~ready && nextdata_n

  val sevSegDecoder = Seq.fill(6)(Module(new SevenSegDecoder))
  sevSegDecoder(0).io.sw := Mux(state === pressed, codeReg(3, 0), 0.U)
  sevSegDecoder(1).io.sw := Mux(state === pressed, codeReg(7, 4), 0.U)
  sevSegDecoder(2).io.sw := Mux(state === pressed, asciiReg(3, 0), 0.U)
  sevSegDecoder(3).io.sw := Mux(state === pressed, asciiReg(7, 4), 0.U)
  sevSegDecoder(4).io.sw := count(3, 0)
  sevSegDecoder(5).io.sw := count(7, 4)

  io.overflow := keyboard.io.overflow
  io.ready := ready
  io.seg0 := sevSegDecoder(0).io.seg
  io.seg1 := sevSegDecoder(1).io.seg
  io.seg2 := sevSegDecoder(2).io.seg
  io.seg3 := sevSegDecoder(3).io.seg
  io.seg4 := sevSegDecoder(4).io.seg
  io.seg5 := sevSegDecoder(5).io.seg
}

object Lab7 extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new Lab7, args, firtoolOptions)
}
