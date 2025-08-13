// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package org.springframework.asm;

/**
 * 方法字节码中的一个位置。标签用于跳转（jump）、goto 和 switch 指令，以及 try-catch 块。
 * 标签指向的是紧跟其后的 <i>指令</i>。不过，标签与其指定指令之间可能存在其他元素（如其他标签、栈映射帧、行号等）。
 *
 * @author Eric Bruneton
 */
public class Label {

  /**
   * 标记标签仅用于调试属性。这样的标签既不是基本块的开始，
   * 也不是跳转指令的目标或异常处理程序，可在控制流图分析中安全忽略（用于优化）。
   */
  static final int FLAG_DEBUG_ONLY = 1;

  /**
   * 标记标签是跳转指令的目标，或异常处理程序的开始。
   */
  static final int FLAG_JUMP_TARGET = 2;

  /** 标记标签的字节码偏移量已知。 */
  static final int FLAG_RESOLVED = 4;

  /** 标记标签对应的基本块可达。 */
  static final int FLAG_REACHABLE = 8;

  /**
   * 标记标签对应的基本块以子程序调用（jsr指令）结束。
   * 根据 {@link MethodWriter#visitJumpInsn} 的设计，带此标记的标签至少有两个出边：
   * <ul>
   *   <li>第一个出边指向 jsr 指令之后的字节码指令，即子程序返回后继续执行的位置。
   *       这是一个虚拟控制流边，因为执行并非直接从 jsr 跳到下一条指令，
   *       而是跳到子程序，随后通过 ret 指令返回。
   *       这个虚拟边用于 {@link #addSubroutineRetSuccessors} 方法中计算以 ret 结尾的基本块的真实出边。
   *   <li>第二个出边指向 jsr 指令的目标（子程序入口）。
   * </ul>
   */
  static final int FLAG_SUBROUTINE_CALLER = 16;

  /** 标记标签对应的基本块是子程序的起始位置。 */
  static final int FLAG_SUBROUTINE_START = 32;

  /** 标记标签对应的基本块是子程序的结束位置。 */
  static final int FLAG_SUBROUTINE_END = 64;

  /**
   * 当 {@link #otherLineNumbers} 数组需要扩容以存储新的源代码行号时，
   * 增加的元素数量。
   */
  static final int LINE_NUMBERS_CAPACITY_INCREMENT = 4;

  /**
   * 当 {@link #forwardReferences} 数组需要扩容以存储新的前向引用时，
   * 增加的元素数量。
   */
  static final int FORWARD_REFERENCES_CAPACITY_INCREMENT = 6;

  /**
   * 用于提取前向引用类型的位掩码。提取结果是
   * {@link #FORWARD_REFERENCE_TYPE_SHORT} 或 {@link #FORWARD_REFERENCE_TYPE_WIDE}。
   *
   * @see #forwardReferences
   */
  static final int FORWARD_REFERENCE_TYPE_MASK = 0xF0000000;

  /**
   * 以两个字节存储的前向引用类型。例如 ifnull 指令的前向引用即为此类型。
   */
  static final int FORWARD_REFERENCE_TYPE_SHORT = 0x10000000;

  /**
   * 以四字节存储的前向引用类型。例如 lookupswitch 指令的前向引用即为此类型。
   */
  static final int FORWARD_REFERENCE_TYPE_WIDE = 0x20000000;

  /**
   * 提取前向引用“句柄”的位掩码。句柄是字节码中存储前向引用值的偏移量（2或4字节），
   * 具体长度由 {@link #FORWARD_REFERENCE_TYPE_MASK} 指示。
   *
   * @see #forwardReferences
   */
  static final int FORWARD_REFERENCE_HANDLE_MASK = 0x0FFFFFFF;

  /**
   * 标签列表结束的哨兵元素。
   *
   * @see #nextListElement
   */
  static final Label EMPTY_LIST = new Label();

  /**
   * 关联到该标签的用户管理状态。注意：ASM树包会用到此字段，
   * 使用ASM树包时需在 MethodNode 中重写 getLabelNode 方法。
   */
  public Object info;

  /**
   * 标签或其对应基本块的类型和状态。必须是以下标志的零个或多个组合：
   * {@link #FLAG_DEBUG_ONLY}, {@link #FLAG_JUMP_TARGET}, {@link #FLAG_RESOLVED},
   * {@link #FLAG_REACHABLE}, {@link #FLAG_SUBROUTINE_CALLER}, {@link #FLAG_SUBROUTINE_START},
   * {@link #FLAG_SUBROUTINE_END}。
   */
  short flags;

  /**
   * 此标签对应的源代码行号，若有多个行号则存储第一个，
   * 其他行号存储在 {@link #otherLineNumbers} 中。
   */
  private short lineNumber;

  /**
   * 除 {@link #lineNumber} 外，此标签对应的其他源代码行号数组。
   * 数组第一个元素为行号数量 n，具体行号存储在索引 1 到 n。
   */
  private int[] otherLineNumbers;

  /**
   * 此标签在方法字节码中的偏移量（字节数）。
   * 仅当 {@link #FLAG_RESOLVED} 标志设置时有效。
   */
  int bytecodeOffset;

  /**
   * 此标签的前向引用列表。
   * 第一个元素为前向引用数量乘2（对应数组中最后使用元素的索引）。
   * 每个前向引用由两个连续整数组成，分别为：
   * - sourceInsnBytecodeOffset：包含前向引用的指令的字节码偏移；
   * - reference：存储前向引用值的字节码偏移及其类型，可用 {@link #FORWARD_REFERENCE_TYPE_MASK}
   *   和 {@link #FORWARD_REFERENCE_HANDLE_MASK} 解析。
   *
   * 例如：对于偏移为 x 的 ifnull 指令，
   * sourceInsnBytecodeOffset = x，
   * reference 类型为 {@link #FORWARD_REFERENCE_TYPE_SHORT}，值为 x + 1（ifnull 的偏移占 2 字节，
   * 从指令后第1字节开始存储）。
   *
   * 又如：对于偏移为 x 的 lookupswitch 指令，
   * sourceInsnBytecodeOffset = x，
   * reference 类型为 {@link #FORWARD_REFERENCE_TYPE_WIDE}，值在 x + 1 到 x + 4 之间
   * （lookupswitch 的偏移占 4 字节，从指令后第1到第4字节存储）。
   */
  private int[] forwardReferences;

  // -----------------------------------------------------------------------------------------------
  // 用于控制流和数据流图分析算法的字段（用于计算最大栈大小或栈映射帧）。
  // 控制流图由若干“基本块”节点组成，每个节点对应方法中一个基本块的第一条指令，
  // 节点间边对应从一个基本块跳转到另一个基本块的跳转指令。
  // 每个节点保存其后继节点链表，链表由 Edge 对象链接。
  // 计算最大栈大小或栈映射帧的分析算法分两步：
  // 第一步，在访问每条指令时，计算基本块结束时的局部变量和操作数栈状态（“输出帧”，
  // 相对该基本块开始时的“输入帧”，此时输入帧未知）；
  // 第二步，在 MethodWriter#computeAllFrames 和 MethodWriter#computeMaxStackAndLocal 中，
  // 通过迭代计算每个基本块的输入帧信息（从方法签名已知的第一个基本块输入帧开始），
  // 并结合第一步计算的输出帧，达到收敛。

  /**
   * 对应此标签的基本块的输入栈元素数量。
   * 该字段由 {@link MethodWriter#computeMaxStackAndLocal} 计算。
   */
  short inputStackSize;

  /**
   * 对应此标签的基本块的输出栈元素数量（仅在以 RET 指令结尾的基本块计算）。
   */
  short outputStackSize;

  /**
   * 对应此标签的基本块输出栈相对输入栈的最大高度，始终为正数或0。
   */
  short outputStackMax;

  /**
   * 此基本块所属子程序ID，或0。
   * 若基本块属于多个子程序，取最早（调用链上最上层）的子程序ID。
   * 该字段由 {@link MethodWriter#computeMaxStackAndLocal} 计算（仅当方法含 JSR 指令时）。
   */
  short subroutineId;

  /**
   * 此标签对应基本块的输入和输出栈映射帧。
   * 仅在 {@link MethodWriter#COMPUTE_ALL_FRAMES} 或 {@link MethodWriter#COMPUTE_INSERTED_FRAMES} 选项启用时使用。
   */
  Frame frame;

  /**
   * 按访问顺序，基本块对应的下一个标签。
   * 该链表不包含仅用于调试信息的标签。
   * 若启用 {@link MethodWriter#COMPUTE_ALL_FRAMES} 或 {@link MethodWriter#COMPUTE_INSERTED_FRAMES}，
   * 则不会包含对应同一字节码偏移的连续标签（仅第一个标签出现在链表中）。
   */
  Label nextBasicBlock;

  /**
   * 此标签对应基本块在控制流图中的出边链表，存储为 {@link Edge} 链表，
   * 通过 {@link Edge#nextEdge} 字段链接。
   */
  Edge outgoingEdges;

  /**
   * 此标签所在的标签列表中的下一个元素，或 {@literal null}（不在任何列表中）。
   * 所有标签列表必须以 {@link #EMPTY_LIST} 作为哨兵结尾，确保此字段为空时，表示标签不在任何列表中。
   * 允许存在多个标签列表，但标签一次最多属于一个列表（除非共享尾部，但实际不使用）。
   *
   * <p>标签列表在 {@link MethodWriter#computeAllFrames} 和 {@link MethodWriter#computeMaxStackAndLocal} 中使用，
   * 用于计算栈映射帧和最大栈大小，
   * 也在 {@link #markSubroutine} 和 {@link #addSubroutineRetSuccessors} 中用于计算子程序基本块及其出边。
   * 除上述方法外，此字段应始终为 null（该属性是这些方法的前置和后置条件）。
   */
  Label nextListElement;

  // -----------------------------------------------------------------------------------------------
  // 构造函数和访问方法
  // -----------------------------------------------------------------------------------------------

  /** 创建一个新的标签实例。 */
  public Label() {
    // 无需额外初始化。
  }

  /**
   * 返回此标签对应的字节码偏移（相对于方法字节码起始处）。
   * <i>此方法主要供 {@link Attribute} 子类使用，一般类生成器或适配器不需调用。</i>
   *
   * @return 此标签对应的字节码偏移。
   * @throws IllegalStateException 如果标签尚未解析（偏移未知）。
   */
  public int getOffset() {
    if ((flags & FLAG_RESOLVED) == 0) {
      throw new IllegalStateException("Label offset position has not been resolved yet");
    }
    return bytecodeOffset;
  }

  /**
   * 返回此标签对应字节码偏移的“规范”标签实例（若已知），否则返回自身。
   * 规范实例为对应该偏移的第一个被 {@link MethodVisitor#visitLabel} 访问的标签。
   * 未访问的标签无法确定规范实例。
   *
   * <p><i>仅在启用 {@link MethodWriter#COMPUTE_ALL_FRAMES} 选项时使用此方法。</i>
   *
   * @return 若 {@link #frame} 为 null，返回自身；否则返回 {@link Frame#owner} 字段对应标签，
   *         即“规范”标签实例。
   */
  final Label getCanonicalInstance() {
    return frame == null ? this : frame.owner;
  }

  // -----------------------------------------------------------------------------------------------
  // 行号管理相关方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 添加一个与此标签对应的源代码行号。
   *
   * @param lineNumber 源代码行号（应严格为正数）。
   */
  final void addLineNumber(final int lineNumber) {
    if (this.lineNumber == 0) {
      this.lineNumber = (short) lineNumber;
    } else {
      if (otherLineNumbers == null) {
        otherLineNumbers = new int[LINE_NUMBERS_CAPACITY_INCREMENT];
      }
      int otherLineNumberIndex = ++otherLineNumbers[0];
      if (otherLineNumberIndex >= otherLineNumbers.length) {
        int[] newLineNumbers = new int[otherLineNumbers.length + LINE_NUMBERS_CAPACITY_INCREMENT];
        System.arraycopy(otherLineNumbers, 0, newLineNumbers, 0, otherLineNumbers.length);
        otherLineNumbers = newLineNumbers;
      }
      otherLineNumbers[otherLineNumberIndex] = lineNumber;
    }
  }

  /**
   * 使给定的访问者访问此标签及其对应的源代码行号（如适用）。
   *
   * @param methodVisitor 方法访问者。
   * @param visitLineNumbers 是否访问此标签对应的源代码行号（如果存在）。
   */
  final void accept(final MethodVisitor methodVisitor, final boolean visitLineNumbers) {
    methodVisitor.visitLabel(this);
    if (visitLineNumbers && lineNumber != 0) {
      methodVisitor.visitLineNumber(lineNumber & 0xFFFF, this);
      if (otherLineNumbers != null) {
        for (int i = 1; i <= otherLineNumbers[0]; ++i) {
          methodVisitor.visitLineNumber(otherLineNumbers[i], this);
        }
      }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 计算偏移及处理前向引用相关方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 在方法字节码中为此标签放置一个引用。
   * 如果标签的字节码偏移已知，计算并直接写入标签相对于引用指令的偏移；
   * 否则写入空的偏移值，并声明一个新的前向引用。
   *
   * @param code 方法的字节码，引用将写入此处。
   * @param sourceInsnBytecodeOffset 包含该引用的指令的字节码偏移。
   * @param wideReference 是否需使用4字节（否则为2字节）存储引用。
   */
  final void put(
      final ByteVector code, final int sourceInsnBytecodeOffset, final boolean wideReference) {
    if ((flags & FLAG_RESOLVED) == 0) {
      if (wideReference) {
        addForwardReference(sourceInsnBytecodeOffset, FORWARD_REFERENCE_TYPE_WIDE, code.length);
        code.putInt(-1);
      } else {
        addForwardReference(sourceInsnBytecodeOffset, FORWARD_REFERENCE_TYPE_SHORT, code.length);
        code.putShort(-1);
      }
    } else {
      if (wideReference) {
        code.putInt(bytecodeOffset - sourceInsnBytecodeOffset);
      } else {
        code.putShort(bytecodeOffset - sourceInsnBytecodeOffset);
      }
    }
  }

  /**
   * 添加一个前向引用。此方法仅应在标签尚未解析时调用（即真正的前向引用）。
   * 对于回溯引用，偏移可直接计算存储。
   *
   * @param sourceInsnBytecodeOffset 包含该引用的指令的字节码偏移。
   * @param referenceType {@link #FORWARD_REFERENCE_TYPE_SHORT} 或 {@link #FORWARD_REFERENCE_TYPE_WIDE}。
   * @param referenceHandle 前向引用值应存储的字节码偏移。
   */
  private void addForwardReference(
      final int sourceInsnBytecodeOffset, final int referenceType, final int referenceHandle) {
    if (forwardReferences == null) {
      forwardReferences = new int[FORWARD_REFERENCES_CAPACITY_INCREMENT];
    }
    int lastElementIndex = forwardReferences[0];
    if (lastElementIndex + 2 >= forwardReferences.length) {
      int[] newValues = new int[forwardReferences.length + FORWARD_REFERENCES_CAPACITY_INCREMENT];
      System.arraycopy(forwardReferences, 0, newValues, 0, forwardReferences.length);
      forwardReferences = newValues;
    }
    forwardReferences[++lastElementIndex] = sourceInsnBytecodeOffset;
    forwardReferences[++lastElementIndex] = referenceType | referenceHandle;
    forwardReferences[0] = lastElementIndex;
  }

  /**
   * 设置此标签的字节码偏移，并解析对该标签的所有前向引用（如有）。
   * 此方法应在标签被加入方法字节码（即偏移已知）时调用。
   * 它会填补之前为前向引用预留的空白。
   *
   * @param code 方法字节码。
   * @param bytecodeOffset 此标签的字节码偏移。
   * @return 若预留的空白区域太小以致偏移无法存储，则返回 {@literal true}。
   *         这种情况下，对应跳转指令将被替换为ASM特定指令（使用无符号2字节偏移），
   *         在ClassReader中再替换为4字节偏移的标准字节码指令。
   */
  final boolean resolve(final byte[] code, final int bytecodeOffset) {
    this.flags |= FLAG_RESOLVED;
    this.bytecodeOffset = bytecodeOffset;
    if (forwardReferences == null) {
      return false;
    }
    boolean hasAsmInstructions = false;
    for (int i = forwardReferences[0]; i > 0; i -= 2) {
      final int sourceInsnBytecodeOffset = forwardReferences[i - 1];
      final int reference = forwardReferences[i];
      final int relativeOffset = bytecodeOffset - sourceInsnBytecodeOffset;
      int handle = reference & FORWARD_REFERENCE_HANDLE_MASK;
      if ((reference & FORWARD_REFERENCE_TYPE_MASK) == FORWARD_REFERENCE_TYPE_SHORT) {
        if (relativeOffset < Short.MIN_VALUE || relativeOffset > Short.MAX_VALUE) {
          // 修改跳转指令的操作码，使其能在 ClassReader 中被识别为 ASM 特殊指令。
          // 这些 ASM 特殊指令类似于跳转指令，但偏移量为无符号 2 字节，范围是 0~65535，
          // 足够表示方法内偏移，因为方法大小限制为 65535 字节。
          int opcode = code[sourceInsnBytecodeOffset] & 0xFF;
          if (opcode < Opcodes.IFNULL) {
            // 将 IFEQ ... JSR 改为 ASM_IFEQ ... ASM_JSR。
            code[sourceInsnBytecodeOffset] = (byte) (opcode + Constants.ASM_OPCODE_DELTA);
          } else {
            // 将 IFNULL 和 IFNONNULL 改为 ASM_IFNULL 和 ASM_IFNONNULL。
            code[sourceInsnBytecodeOffset] = (byte) (opcode + Constants.ASM_IFNULL_OPCODE_DELTA);
          }
          hasAsmInstructions = true;
        }
        code[handle++] = (byte) (relativeOffset >>> 8);
        code[handle] = (byte) relativeOffset;
      } else {
        code[handle++] = (byte) (relativeOffset >>> 24);
        code[handle++] = (byte) (relativeOffset >>> 16);
        code[handle++] = (byte) (relativeOffset >>> 8);
        code[handle] = (byte) relativeOffset;
      }
    }
    return hasAsmInstructions;
  }

  // -----------------------------------------------------------------------------------------------
  // 与子程序相关的方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 查找属于以此标签对应的基本块为起点的子程序的所有基本块，并将这些块标记为属于该子程序。
   * 此方法沿控制流图寻找所有从当前基本块可达的基本块，但不跟踪任何 jsr 目标。
   *
   * <p>注意：此方法的前置和后置条件是所有标签的 {@link #nextListElement} 均为 null。
   *
   * @param subroutineId 以此标签对应的基本块为起点的子程序的ID。
   */
  final void markSubroutine(final short subroutineId) {
  // 数据流算法：将此基本块加入待处理列表（这些块属于子程序 subroutineId），
  // 当待处理列表不空时，从中取出一个块，标记它属于该子程序，
  // 并将其控制流图中的后继基本块加入待处理列表（如果尚未加入）。

    Label listOfBlocksToProcess = this;
    listOfBlocksToProcess.nextListElement = EMPTY_LIST;
    while (listOfBlocksToProcess != EMPTY_LIST) {
      // 从待处理列表中取出一个基本块。
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
      basicBlock.nextListElement = null;

      // 如果该块尚未标记属于任何子程序，则标记为属于 subroutineId，
      // 并将其后继加入待处理列表（避免重复加入）。
      if (basicBlock.subroutineId == 0) {
        basicBlock.subroutineId = subroutineId;
        listOfBlocksToProcess = basicBlock.pushSuccessors(listOfBlocksToProcess);
      }
    }
  }

  /**
   * 查找以此标签对应的基本块为起点的子程序的所有结束基本块，
   * 并为每个结束块添加一条到给定子程序调用后继基本块的出边。
   * 换言之，完善控制流图，添加对应子程序返回的边（从调用者块指定）。
   *
   * <p>注意：此方法的前置和后置条件是所有标签的 {@link #nextListElement} 均为 null。
   *
   * @param subroutineCaller 以 jsr 结束且跳转至此标签对应基本块的调用者基本块，假设此标签为子程序入口。
   */
  final void addSubroutineRetSuccessors(final Label subroutineCaller) {
// 数据流算法：将此基本块加入待处理块列表（属于以此标签开头的子程序的块），
// 当待处理列表不空时，从中取出一个基本块，放入已处理块列表，
// 如果适用，则为 subroutineCaller 的后继添加返回边，
// 并将其控制流图中的后继基本块加入待处理列表（如果尚未加入）。
    Label listOfProcessedBlocks = EMPTY_LIST;
    Label listOfBlocksToProcess = this;
    listOfBlocksToProcess.nextListElement = EMPTY_LIST;
    while (listOfBlocksToProcess != EMPTY_LIST) {
      // 将一个基本块从待处理列表移至已处理列表。
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = basicBlock.nextListElement;
      basicBlock.nextListElement = listOfProcessedBlocks;
      listOfProcessedBlocks = basicBlock;

      // 如果该块是子程序结束块，且不属于同一子程序，则添加一条边连接到 subroutineCaller 的后继。
      if ((basicBlock.flags & FLAG_SUBROUTINE_END) != 0
          && basicBlock.subroutineId != subroutineCaller.subroutineId) {
        basicBlock.outgoingEdges =
            new Edge(
                basicBlock.outputStackSize,
                // 根据设计，以 jsr 指令结束的基本块的第一个后继是 jsr 续块，
                // 即 ret 调用时执行继续的地方（参见 {@link #FLAG_SUBROUTINE_CALLER}）。
                subroutineCaller.outgoingEdges.successor,
                basicBlock.outgoingEdges);
      }
      // 将该基本块的后继加入待处理列表。
      // 注意 pushSuccessors 不会加入已在待处理或已处理列表中的块，防止重复处理。
      listOfBlocksToProcess = basicBlock.pushSuccessors(listOfBlocksToProcess);
    }
// 重置所有已处理块的 nextListElement 字段，方便下次调用此方法处理不同子程序或调用者。
    while (listOfProcessedBlocks != EMPTY_LIST) {
      Label newListOfProcessedBlocks = listOfProcessedBlocks.nextListElement;
      listOfProcessedBlocks.nextListElement = null;
      listOfProcessedBlocks = newListOfProcessedBlocks;
    }
  }

  /**
   * 将此标签在方法控制流图中的后继基本块（除 jsr 目标和已在列表中的块）加入给定的待处理块列表，
   * 并返回更新后的待处理列表。
   *
   * @param listOfLabelsToProcess 待处理基本块列表，通过其 {@link #nextListElement} 字段链表链接。
   * @return 更新后的待处理基本块列表。
   */
  private Label pushSuccessors(final Label listOfLabelsToProcess) {
    Label newListOfLabelsToProcess = listOfLabelsToProcess;
    Edge outgoingEdge = outgoingEdges;
    while (outgoingEdge != null) {
      // 根据结构，一个以jsr指令结尾的基本块的第二个出口边
      // 指向jsr目标（参见 {@link #FLAG_SUBROUTINE_CALLER}）。
      boolean isJsrTarget =
          (flags & Label.FLAG_SUBROUTINE_CALLER) != 0 && outgoingEdge == outgoingEdges.nextEdge;
      if (!isJsrTarget && outgoingEdge.successor.nextListElement == null) {
        // 如果该后继节点不在任何标签列表中，则将其添加到待处理标签列表。
        outgoingEdge.successor.nextListElement = newListOfLabelsToProcess;
        newListOfLabelsToProcess = outgoingEdge.successor;
      }
      outgoingEdge = outgoingEdge.nextEdge;
    }
    return newListOfLabelsToProcess;
  }

  // -----------------------------------------------------------------------------------------------
  // 重写的 Object 方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回此标签的字符串表示。
   *
   * @return 此标签的字符串表示。
   */
  @Override
  public String toString() {
    return "L" + System.identityHashCode(this);
  }
}
