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
 * 一个基本块的输入和输出的堆栈映射帧（stack map frames）。
 *
 * <p>堆栈映射帧的计算分为两个步骤：
 *
 * <ul>
 *   <li>在 MethodWriter 访问每条指令时，通过模拟该指令对先前状态（称为“输出帧”）的影响，
 *       更新当前基本块末尾的帧状态。
 *   <li>在所有指令访问完成后，MethodWriter 采用不动点算法计算每个基本块的“输入帧”
 *       （即基本块开始时的堆栈映射帧）。详见 {@link MethodWriter#computeAllFrames}。
 * </ul>
 *
 * <p>输出堆栈映射帧是相对于基本块的输入帧计算的，而输入帧在计算输出帧时尚未知晓。
 * 因此，需要能够表示抽象类型，例如“输入帧本地变量第 x 位的类型”，
 * “输入帧操作数栈自顶向下第 x 位的类型”，甚至“输入帧中第 x 位类型的数组维度加减 y”。
 * 这解释了本类中较为复杂的类型格式，详述如下。
 *
 * <p>输入和输出帧中的本地变量和操作数栈包含所谓的“抽象类型”。抽象类型由四个字段
 * DIM、KIND、FLAGS 和 VALUE 表示，为了性能和内存效率，这四个字段被打包成一个 int：
 *
 * <pre>
 *   =====================================
 *   |...DIM|KIND|.F|...............VALUE|
 *   =====================================
 * </pre>
 *
 * <ul>
 *   <li>DIM 字段占用最高的 6 位，是一个带符号的数组维度数（范围从 -32 到 31），
 *       可通过 {@link #DIM_MASK} 和右移 {@link #DIM_SHIFT} 获得。
 *   <li>KIND 字段占用 4 位，表示 VALUE 的类型。可通过 {@link #KIND_MASK} 获得，
 *       不移位时应等于 {@link #CONSTANT_KIND}、{@link #REFERENCE_KIND}、{@link #UNINITIALIZED_KIND}、
 *       {@link #LOCAL_KIND} 或 {@link #STACK_KIND}。
 *   <li>FLAGS 字段占用 2 位，存储最多两个布尔标志。目前仅定义了一个标志 {@link #TOP_IF_LONG_OR_DOUBLE_FLAG}。
 *   <li>VALUE 字段占用剩余的 20 位，表示具体的值：
 *       <ul>
 *         <li>当 KIND 是 {@link #CONSTANT_KIND} 时，VALUE 是 {@link #ITEM_TOP}、{@link #ITEM_ASM_BOOLEAN}、
 *             {@link #ITEM_ASM_BYTE}、{@link #ITEM_ASM_CHAR}、{@link #ITEM_ASM_SHORT}、
 *             {@link #ITEM_INTEGER}、{@link #ITEM_FLOAT}、{@link #ITEM_LONG}、{@link #ITEM_DOUBLE}、
 *             {@link #ITEM_NULL} 或 {@link #ITEM_UNINITIALIZED_THIS} 等常量之一。
 *         <li>当 KIND 是 {@link #REFERENCE_KIND} 时，VALUE 是 {@link Symbol#TYPE_TAG} 类型
 *             的 {@link Symbol} 在类型表中的索引。
 *         <li>当 KIND 是 {@link #UNINITIALIZED_KIND} 时，VALUE 是
 *             {@link Symbol#UNINITIALIZED_TYPE_TAG} 类型 {@link Symbol} 在类型表中的索引。
 *         <li>当 KIND 是 {@link #LOCAL_KIND} 时，VALUE 是输入帧本地变量表中的索引。
 *         <li>当 KIND 是 {@link #STACK_KIND} 时，VALUE 是输入帧操作数栈顶部相对位置。
 *       </ul>
 * </ul>
 *
 * <p>输出帧可以包含任意 KIND 的抽象类型，且数组维度可正可负（甚至允许 0，表示未赋值类型）。
 * 输入帧只允许包含 KIND 为 CONSTANT_KIND、REFERENCE_KIND 和 UNINITIALIZED_KIND，
 * 且数组维度为正或零的抽象类型。类型表只包含内部类型名（禁止使用数组类型描述符，数组维度用 DIM 字段表示）。
 *
 * <p>LONG 和 DOUBLE 类型始终使用两个槽表示（LONG + TOP 或 DOUBLE + TOP），
 * 无论是在本地变量还是操作数栈中。这是为了正确模拟 DUPx_y 指令，其效果依赖于操作数栈中抽象类型的具体类型，
 * 而该具体类型有时未知。
 *
 * @author Eric Bruneton
 */
class Frame {

  // StackMapTable 属性中使用的常量。
  // 详情见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4。

  static final int SAME_FRAME = 0;
  static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64;
  static final int RESERVED = 128;
  static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247;
  static final int CHOP_FRAME = 248;
  static final int SAME_FRAME_EXTENDED = 251;
  static final int APPEND_FRAME = 252;
  static final int FULL_FRAME = 255;

  static final int ITEM_TOP = 0;
  static final int ITEM_INTEGER = 1;
  static final int ITEM_FLOAT = 2;
  static final int ITEM_DOUBLE = 3;
  static final int ITEM_LONG = 4;
  static final int ITEM_NULL = 5;
  static final int ITEM_UNINITIALIZED_THIS = 6;
  static final int ITEM_OBJECT = 7;
  static final int ITEM_UNINITIALIZED = 8;
  // ASM 特定的额外常量，用于下面的抽象类型。
  private static final int ITEM_ASM_BOOLEAN = 9;
  private static final int ITEM_ASM_BYTE = 10;
  private static final int ITEM_ASM_CHAR = 11;
  private static final int ITEM_ASM_SHORT = 12;

  // 抽象类型各字段的大小和偏移。

  private static final int DIM_SIZE = 6;
  private static final int KIND_SIZE = 4;
  private static final int FLAGS_SIZE = 2;
  private static final int VALUE_SIZE = 32 - DIM_SIZE - KIND_SIZE - FLAGS_SIZE;

  private static final int DIM_SHIFT = KIND_SIZE + FLAGS_SIZE + VALUE_SIZE;
  private static final int KIND_SHIFT = FLAGS_SIZE + VALUE_SIZE;
  private static final int FLAGS_SHIFT = VALUE_SIZE;

  // 抽象类型字段的掩码。

  private static final int DIM_MASK = ((1 << DIM_SIZE) - 1) << DIM_SHIFT;
  private static final int KIND_MASK = ((1 << KIND_SIZE) - 1) << KIND_SHIFT;
  private static final int VALUE_MASK = (1 << VALUE_SIZE) - 1;

  // 操作 DIM 字段的常量。

  /** 在抽象类型中加一维数组维度的常量。 */
  private static final int ARRAY_OF = +1 << DIM_SHIFT;

  /** 在抽象类型中减一维数组维度的常量。 */
  private static final int ELEMENT_OF = -1 << DIM_SHIFT;

  // 抽象类型 KIND 字段的可能值。

  private static final int CONSTANT_KIND = 1 << KIND_SHIFT;
  private static final int REFERENCE_KIND = 2 << KIND_SHIFT;
  private static final int UNINITIALIZED_KIND = 3 << KIND_SHIFT;
  private static final int LOCAL_KIND = 4 << KIND_SHIFT;
  private static final int STACK_KIND = 5 << KIND_SHIFT;

  // 抽象类型 FLAGS 字段的可能标志。

  /**
   * LOCAL_KIND 和 STACK_KIND 抽象类型的标志，
   * 表示如果解析出的具体类型是 LONG 或 DOUBLE，则使用 TOP 代替（因为该值已被 xSTORE 指令部分覆盖）。
   */
  private static final int TOP_IF_LONG_OR_DOUBLE_FLAG = 1 << FLAGS_SHIFT;

  // 预定义的抽象类型（所有可能的 CONSTANT_KIND 类型）。

  private static final int TOP = CONSTANT_KIND | ITEM_TOP;
  private static final int BOOLEAN = CONSTANT_KIND | ITEM_ASM_BOOLEAN;
  private static final int BYTE = CONSTANT_KIND | ITEM_ASM_BYTE;
  private static final int CHAR = CONSTANT_KIND | ITEM_ASM_CHAR;
  private static final int SHORT = CONSTANT_KIND | ITEM_ASM_SHORT;
  private static final int INTEGER = CONSTANT_KIND | ITEM_INTEGER;
  private static final int FLOAT = CONSTANT_KIND | ITEM_FLOAT;
  private static final int LONG = CONSTANT_KIND | ITEM_LONG;
  private static final int DOUBLE = CONSTANT_KIND | ITEM_DOUBLE;
  private static final int NULL = CONSTANT_KIND | ITEM_NULL;
  private static final int UNINITIALIZED_THIS = CONSTANT_KIND | ITEM_UNINITIALIZED_THIS;

  // -----------------------------------------------------------------------------------------------
  // 实例字段
  // -----------------------------------------------------------------------------------------------

  /** 该输入和输出堆栈映射帧所属的基本块。 */
  Label owner;

  /** 输入堆栈映射帧的本地变量表，存储抽象类型数组。 */
  private int[] inputLocals;

  /** 输入堆栈映射帧的操作数栈。是一个抽象类型数组。 */
  private int[] inputStack;

  /** 输出堆栈映射帧的本地变量表。是一个抽象类型数组。 */
  private int[] outputLocals;

  /** 输出堆栈映射帧的操作数栈。是一个抽象类型数组。 */
  private int[] outputStack;

  /**
   * 输出堆栈相对于输入堆栈的起始位置偏移。该偏移总是非正（负数或零）。
   * 0 表示输出堆栈应追加到输入堆栈末尾；
   * 负数 -n 表示输出堆栈的前 n 个元素替换输入堆栈栈顶的 n 个元素，其余元素追加到输入堆栈末尾。
   */
  private short outputStackStart;

  /** {@link #outputStack} 中的栈顶元素索引。 */
  private short outputStackTop;

  /** 在基本块中已初始化的类型数量。详见 {@link #initializations}。 */
  private int initializationCount;

  /**
   * 在基本块中已初始化的抽象类型列表。
   * 对于 UNINITIALIZED 或 UNINITIALIZED_THIS 类型的构造函数调用，
   * 需要替换局部变量和操作数栈中该类型的<i>所有出现</i>。
   * 该替换不能在算法第一步完成，因为当时局部变量和操作数栈仍是抽象类型，
   * 因此必须记录该基本块中调用的构造函数抽象类型，以便算法第二步（帧计算完成后）替换。
   * 注意，该数组中的抽象类型可能相对于输入的局部变量或输入的操作数栈。
   */
  private int[] initializations;

  // -----------------------------------------------------------------------------------------------
  // 构造函数
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 Frame 实例。
   *
   * @param owner 该输入输出堆栈映射帧对应的基本块。
   */
  Frame(final Label owner) {
    this.owner = owner;
  }

  /**
   * 将本帧设置为给定帧的值。
   *
   * <p>警告：调用此方法后，两个帧将共享相同的数据结构。
   * 建议弃用给定的帧以避免意外的副作用。
   *
   * @param frame 新的帧值。
   */
  final void copyFrom(final Frame frame) {
    inputLocals = frame.inputLocals;
    inputStack = frame.inputStack;
    outputStackStart = 0;
    outputLocals = frame.outputLocals;
    outputStack = frame.outputStack;
    outputStackTop = frame.outputStackTop;
    initializationCount = frame.initializationCount;
    initializations = frame.initializations;
  }

  // -----------------------------------------------------------------------------------------------
  // 将其它类型格式转换为抽象类型的静态方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 根据给定的公共 API 帧元素类型返回对应的抽象类型。
   *
   * @param symbolTable 用于查找和存储类型 {@link Symbol} 的类型表。
   * @param type 使用与 {@link MethodVisitor#visitFrame} 中相同格式描述的帧元素类型，
   *             可能是 {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT},
   *             {@link Opcodes#LONG}, {@link Opcodes#DOUBLE}, {@link Opcodes#NULL}, {@link Opcodes#UNINITIALIZED_THIS}，
   *             也可能是类的内部名，或指向 NEW 指令的 Label（用于未初始化类型）。
   * @return 对应的抽象类型。
   */
  static int getAbstractTypeFromApiFormat(final SymbolTable symbolTable, final Object type) {
    if (type instanceof Integer) {
      return CONSTANT_KIND | ((Integer) type).intValue();
    } else if (type instanceof String) {
      String descriptor = Type.getObjectType((String) type).getDescriptor();
      return getAbstractTypeFromDescriptor(symbolTable, descriptor, 0);
    } else {
      return UNINITIALIZED_KIND
          | symbolTable.addUninitializedType("", ((Label) type).bytecodeOffset);
    }
  }

  /**
   * 根据类的内部名称返回对应的抽象类型。
   *
   * @param symbolTable 用于查找和存储类型 {@link Symbol} 的类型表。
   * @param internalName 类的内部名称，不能是数组类型描述符。
   * @return 给定内部名称对应的抽象类型值。
   */
  static int getAbstractTypeFromInternalName(
      final SymbolTable symbolTable, final String internalName) {
    return REFERENCE_KIND | symbolTable.addType(internalName);
  }

  /**
   * 根据给定的类型描述符返回对应的抽象类型。
   *
   * @param symbolTable 用于查找和存储类型 {@link Symbol} 的类型表。
   * @param buffer 包含类型描述符的字符串。
   * @param offset 类型描述符在 buffer 中的起始偏移。
   * @return 对应的抽象类型。
   */
  private static int getAbstractTypeFromDescriptor(
      final SymbolTable symbolTable, final String buffer, final int offset) {
    String internalName;
    switch (buffer.charAt(offset)) {
      case 'V':
        return 0;
      case 'Z':
      case 'C':
      case 'B':
      case 'S':
      case 'I':
        return INTEGER;
      case 'F':
        return FLOAT;
      case 'J':
        return LONG;
      case 'D':
        return DOUBLE;
      case 'L':
        internalName = buffer.substring(offset + 1, buffer.length() - 1);
        return REFERENCE_KIND | symbolTable.addType(internalName);
      case '[':
        int elementDescriptorOffset = offset + 1;
        while (buffer.charAt(elementDescriptorOffset) == '[') {
          ++elementDescriptorOffset;
        }
        int typeValue;
        switch (buffer.charAt(elementDescriptorOffset)) {
          case 'Z':
            typeValue = BOOLEAN;
            break;
          case 'C':
            typeValue = CHAR;
            break;
          case 'B':
            typeValue = BYTE;
            break;
          case 'S':
            typeValue = SHORT;
            break;
          case 'I':
            typeValue = INTEGER;
            break;
          case 'F':
            typeValue = FLOAT;
            break;
          case 'J':
            typeValue = LONG;
            break;
          case 'D':
            typeValue = DOUBLE;
            break;
          case 'L':
            internalName = buffer.substring(elementDescriptorOffset + 1, buffer.length() - 1);
            typeValue = REFERENCE_KIND | symbolTable.addType(internalName);
            break;
          default:
            throw new IllegalArgumentException();
        }
        return ((elementDescriptorOffset - offset) << DIM_SHIFT) | typeValue;
      default:
        throw new IllegalArgumentException();
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 与输入帧相关的方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 根据方法描述符设置输入帧。该方法用于初始化方法的第一个隐式帧（不在 StackMapTable 显式存储）。
   *
   * @param symbolTable 用于查找和存储类型 {@link Symbol} 的类型表。
   * @param access 方法访问标志。
   * @param descriptor 方法描述符。
   * @param maxLocals 方法的最大本地变量数。
   */
  final void setInputFrameFromDescriptor(
      final SymbolTable symbolTable,
      final int access,
      final String descriptor,
      final int maxLocals) {
    inputLocals = new int[maxLocals];
    inputStack = new int[0];
    int inputLocalIndex = 0;
    if ((access & Opcodes.ACC_STATIC) == 0) {
      if ((access & Constants.ACC_CONSTRUCTOR) == 0) {
        inputLocals[inputLocalIndex++] =
            REFERENCE_KIND | symbolTable.addType(symbolTable.getClassName());
      } else {
        inputLocals[inputLocalIndex++] = UNINITIALIZED_THIS;
      }
    }
    for (Type argumentType : Type.getArgumentTypes(descriptor)) {
      int abstractType =
          getAbstractTypeFromDescriptor(symbolTable, argumentType.getDescriptor(), 0);
      inputLocals[inputLocalIndex++] = abstractType;
      if (abstractType == LONG || abstractType == DOUBLE) {
        inputLocals[inputLocalIndex++] = TOP;
      }
    }
    while (inputLocalIndex < maxLocals) {
      inputLocals[inputLocalIndex++] = TOP;
    }
  }

  /**
   * 根据公共 API 格式的帧描述设置输入帧。
   *
   * @param symbolTable 用于查找和存储类型 {@link Symbol} 的类型表。
   * @param numLocal 本地变量数量。
   * @param local 本地变量类型，格式同 {@link MethodVisitor#visitFrame}。
   * @param numStack 操作数栈元素数量。
   * @param stack 操作数栈类型，格式同 {@link MethodVisitor#visitFrame}。
   */
  final void setInputFrameFromApiFormat(
      final SymbolTable symbolTable,
      final int numLocal,
      final Object[] local,
      final int numStack,
      final Object[] stack) {
    int inputLocalIndex = 0;
    for (int i = 0; i < numLocal; ++i) {
      inputLocals[inputLocalIndex++] = getAbstractTypeFromApiFormat(symbolTable, local[i]);
      if (local[i] == Opcodes.LONG || local[i] == Opcodes.DOUBLE) {
        inputLocals[inputLocalIndex++] = TOP;
      }
    }
    while (inputLocalIndex < inputLocals.length) {
      inputLocals[inputLocalIndex++] = TOP;
    }
    int numStackTop = 0;
    for (int i = 0; i < numStack; ++i) {
      if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
        ++numStackTop;
      }
    }
    inputStack = new int[numStack + numStackTop];
    int inputStackIndex = 0;
    for (int i = 0; i < numStack; ++i) {
      inputStack[inputStackIndex++] = getAbstractTypeFromApiFormat(symbolTable, stack[i]);
      if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
        inputStack[inputStackIndex++] = TOP;
      }
    }
    outputStackTop = 0;
    initializationCount = 0;
  }

  final int getInputStackSize() {
    return inputStack.length;
  }

  // -----------------------------------------------------------------------------------------------
  // 与输出帧相关的方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回输出帧中指定本地变量索引处存储的抽象类型。
   *
   * @param localIndex 目标本地变量的索引。
   * @return 该本地变量索引处的抽象类型。
   */
  private int getLocal(final int localIndex) {
    if (outputLocals == null || localIndex >= outputLocals.length) {
      // 如果该局部变量在当前基本块中从未被赋值，
      // 它仍然等于输入帧中的值。
      return LOCAL_KIND | localIndex;
    } else {
      int abstractType = outputLocals[localIndex];
      if (abstractType == 0) {
        // 如果该局部变量在当前基本块中从未被赋值，
        // 它仍然等于输入帧中的值。
        abstractType = outputLocals[localIndex] = LOCAL_KIND | localIndex;
      }
      return abstractType;
    }
  }

  /**
   * 替换输出帧中指定本地变量索引处存储的抽象类型。
   *
   * @param localIndex 目标本地变量索引。
   * @param abstractType 要设置的抽象类型值。
   */
  private void setLocal(final int localIndex, final int abstractType) {
    // 如有必要，创建和/或调整输出局部变量数组的大小。
    if (outputLocals == null) {
      outputLocals = new int[10];
    }
    int outputLocalsLength = outputLocals.length;
    if (localIndex >= outputLocalsLength) {
      int[] newOutputLocals = new int[Math.max(localIndex + 1, 2 * outputLocalsLength)];
      System.arraycopy(outputLocals, 0, newOutputLocals, 0, outputLocalsLength);
      outputLocals = newOutputLocals;
    }
    // 设置局部变量。
    outputLocals[localIndex] = abstractType;
  }

  /**
   * 将给定的抽象类型推入输出帧的操作数栈。
   *
   * @param abstractType 要推入的抽象类型。
   */
  private void push(final int abstractType) {
    // 如有必要，创建和/或调整输出栈数组的大小。
    if (outputStack == null) {
      outputStack = new int[10];
    }
    int outputStackLength = outputStack.length;
    if (outputStackTop >= outputStackLength) {
      int[] newOutputStack = new int[Math.max(outputStackTop + 1, 2 * outputStackLength)];
      System.arraycopy(outputStack, 0, newOutputStack, 0, outputStackLength);
      outputStack = newOutputStack;
    }
    // 将抽象类型推入输出栈。
    outputStack[outputStackTop++] = abstractType;
    // 如果需要，更新输出栈达到的最大大小（注意这个大小是相对于输入栈的大小，输入栈大小尚未知）。
    short outputStackSize = (short) (outputStackStart + outputStackTop);
    if (outputStackSize > owner.outputStackMax) {
      owner.outputStackMax = outputStackSize;
    }
  }

  /**
   * 根据给定的类型描述符，将对应的抽象类型推入输出栈。
   *
   * @param symbolTable 用于查找和存储类型的类型表。
   * @param descriptor 类型或方法描述符（如果是方法，则推入返回类型）。
   */
  private void push(final SymbolTable symbolTable, final String descriptor) {
    int typeDescriptorOffset =
        descriptor.charAt(0) == '(' ? Type.getReturnTypeOffset(descriptor) : 0;
    int abstractType = getAbstractTypeFromDescriptor(symbolTable, descriptor, typeDescriptorOffset);
    if (abstractType != 0) {
      push(abstractType);
      if (abstractType == LONG || abstractType == DOUBLE) {
        push(TOP);
      }
    }
  }

  /**
   * 从输出帧操作数栈弹出一个抽象类型并返回。
   *
   * @return 被弹出的抽象类型。
   */
  private int pop() {
    if (outputStackTop > 0) {
      return outputStack[--outputStackTop];
    } else {
      // 如果输出帧栈为空，则从输入栈中弹出元素。
      return STACK_KIND | -(--outputStackStart);
    }
  }

  /**
   * 弹出指定数量的抽象类型。
   *
   * @param elements 需要弹出的抽象类型数量。
   */
  private void pop(final int elements) {
    if (outputStackTop >= elements) {
      outputStackTop -= elements;
    } else {
      // 如果要弹出的元素数量大于输出栈中的元素数量，
      // 则清空输出栈，并从输入栈中弹出剩余的元素。
      outputStackStart -= elements - outputStackTop;
      outputStackTop = 0;
    }
  }

  /**
   * 根据给定描述符弹出对应数量的抽象类型。
   *
   * @param descriptor 类型或方法描述符（如果是方法则弹出参数类型）。
   */
  private void pop(final String descriptor) {
    char firstDescriptorChar = descriptor.charAt(0);
    if (firstDescriptorChar == '(') {
      pop((Type.getArgumentsAndReturnSizes(descriptor) >> 2) - 1);
    } else if (firstDescriptorChar == 'J' || firstDescriptorChar == 'D') {
      pop(2);
    } else {
      pop(1);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 处理未初始化类型的方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 添加一个在基本块中调用构造函数的抽象类型。
   *
   * @param abstractType 被调用构造函数的抽象类型。
   */
  private void addInitializedType(final int abstractType) {
    // 如有必要，创建和/或调整初始化数组的大小。
    if (initializations == null) {
      initializations = new int[2];
    }
    int initializationsLength = initializations.length;
    if (initializationCount >= initializationsLength) {
      int[] newInitializations =
          new int[Math.max(initializationCount + 1, 2 * initializationsLength)];
      System.arraycopy(initializations, 0, newInitializations, 0, initializationsLength);
      initializations = newInitializations;
    }
    // 存储抽象类型。
    initializations[initializationCount++] = abstractType;
  }

  /**
   * 返回给定抽象类型对应的“已初始化”抽象类型。
   *
   * @param symbolTable 用于查找和存储类型的类型表 {@link Symbol}。
   * @param abstractType 一个抽象类型。
   * @return 如果 abstractType 是 UNINITIALIZED_THIS 或者是基本块中调用过构造函数的
   *     UNINITIALIZED_KIND 类型之一，则返回对应的 REFERENCE_KIND 类型。否则返回原抽象类型。
   */
  private int getInitializedType(final SymbolTable symbolTable, final int abstractType) {
    if (abstractType == UNINITIALIZED_THIS
        || (abstractType & (DIM_MASK | KIND_MASK)) == UNINITIALIZED_KIND) {
      for (int i = 0; i < initializationCount; ++i) {
        int initializedType = initializations[i];
        int dim = initializedType & DIM_MASK;
        int kind = initializedType & KIND_MASK;
        int value = initializedType & VALUE_MASK;
        if (kind == LOCAL_KIND) {
          initializedType = dim + inputLocals[value];
        } else if (kind == STACK_KIND) {
          initializedType = dim + inputStack[inputStack.length - value];
        }
        if (abstractType == initializedType) {
          if (abstractType == UNINITIALIZED_THIS) {
            return REFERENCE_KIND | symbolTable.addType(symbolTable.getClassName());
          } else {
            return REFERENCE_KIND
                | symbolTable.addType(symbolTable.getType(abstractType & VALUE_MASK).value);
          }
        }
      }
    }
    return abstractType;
  }

  // -----------------------------------------------------------------------------------------------
  // 主要方法，模拟执行每条指令对输出帧的影响
  // -----------------------------------------------------------------------------------------------

  /**
   * 模拟给定指令对输出栈帧的作用。
   *
   * @param opcode 指令的操作码。
   * @param arg 指令的数字操作数（如果有）。
   * @param argSymbol 指令的符号操作数（如果有）。
   * @param symbolTable 用于查找和存储类型的类型表 {@link Symbol}。
   */
  void execute(
      final int opcode, final int arg, final Symbol argSymbol, final SymbolTable symbolTable) {
  // 从操作栈中弹出或从局部变量中读取的抽象类型。
    int abstractType1;
    int abstractType2;
    int abstractType3;
    int abstractType4;
    switch (opcode) {
      case Opcodes.NOP:
      case Opcodes.INEG:
      case Opcodes.LNEG:
      case Opcodes.FNEG:
      case Opcodes.DNEG:
      case Opcodes.I2B:
      case Opcodes.I2C:
      case Opcodes.I2S:
      case Opcodes.GOTO:
      case Opcodes.RETURN:
        break;
      case Opcodes.ACONST_NULL:
        push(NULL);
        break;
      case Opcodes.ICONST_M1:
      case Opcodes.ICONST_0:
      case Opcodes.ICONST_1:
      case Opcodes.ICONST_2:
      case Opcodes.ICONST_3:
      case Opcodes.ICONST_4:
      case Opcodes.ICONST_5:
      case Opcodes.BIPUSH:
      case Opcodes.SIPUSH:
      case Opcodes.ILOAD:
        push(INTEGER);
        break;
      case Opcodes.LCONST_0:
      case Opcodes.LCONST_1:
      case Opcodes.LLOAD:
        push(LONG);
        push(TOP);
        break;
      case Opcodes.FCONST_0:
      case Opcodes.FCONST_1:
      case Opcodes.FCONST_2:
      case Opcodes.FLOAD:
        push(FLOAT);
        break;
      case Opcodes.DCONST_0:
      case Opcodes.DCONST_1:
      case Opcodes.DLOAD:
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.LDC:
        switch (argSymbol.tag) {
          case Symbol.CONSTANT_INTEGER_TAG:
            push(INTEGER);
            break;
          case Symbol.CONSTANT_LONG_TAG:
            push(LONG);
            push(TOP);
            break;
          case Symbol.CONSTANT_FLOAT_TAG:
            push(FLOAT);
            break;
          case Symbol.CONSTANT_DOUBLE_TAG:
            push(DOUBLE);
            push(TOP);
            break;
          case Symbol.CONSTANT_CLASS_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/Class"));
            break;
          case Symbol.CONSTANT_STRING_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/String"));
            break;
          case Symbol.CONSTANT_METHOD_TYPE_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/invoke/MethodType"));
            break;
          case Symbol.CONSTANT_METHOD_HANDLE_TAG:
            push(REFERENCE_KIND | symbolTable.addType("java/lang/invoke/MethodHandle"));
            break;
          case Symbol.CONSTANT_DYNAMIC_TAG:
            push(symbolTable, argSymbol.value);
            break;
          default:
            throw new AssertionError();
        }
        break;
      case Opcodes.ALOAD:
        push(getLocal(arg));
        break;
      case Opcodes.LALOAD:
      case Opcodes.D2L:
        pop(2);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.DALOAD:
      case Opcodes.L2D:
        pop(2);
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.AALOAD:
        pop(1);
        abstractType1 = pop();
        push(abstractType1 == NULL ? abstractType1 : ELEMENT_OF + abstractType1);
        break;
      case Opcodes.ISTORE:
      case Opcodes.FSTORE:
      case Opcodes.ASTORE:
        abstractType1 = pop();
        setLocal(arg, abstractType1);
        if (arg > 0) {
          int previousLocalType = getLocal(arg - 1);
          if (previousLocalType == LONG || previousLocalType == DOUBLE) {
            setLocal(arg - 1, TOP);
          } else if ((previousLocalType & KIND_MASK) == LOCAL_KIND
              || (previousLocalType & KIND_MASK) == STACK_KIND) {
            // 前一个局部变量的类型尚未确定，但如果之后它被识别为 LONG 或 DOUBLE，
            // 那么应当将其设置为 TOP。
            setLocal(arg - 1, previousLocalType | TOP_IF_LONG_OR_DOUBLE_FLAG);
          }
        }
        break;
      case Opcodes.LSTORE:
      case Opcodes.DSTORE:
        pop(1);
        abstractType1 = pop();
        setLocal(arg, abstractType1);
        setLocal(arg + 1, TOP);
        if (arg > 0) {
          int previousLocalType = getLocal(arg - 1);
          if (previousLocalType == LONG || previousLocalType == DOUBLE) {
            setLocal(arg - 1, TOP);
          } else if ((previousLocalType & KIND_MASK) == LOCAL_KIND
              || (previousLocalType & KIND_MASK) == STACK_KIND) {
            // 前一个局部变量的类型尚未确定，但如果之后它被识别为 LONG 或 DOUBLE，
            // 那么应当将其设置为 TOP。
            setLocal(arg - 1, previousLocalType | TOP_IF_LONG_OR_DOUBLE_FLAG);
          }
        }
        break;
      case Opcodes.IASTORE:
      case Opcodes.BASTORE:
      case Opcodes.CASTORE:
      case Opcodes.SASTORE:
      case Opcodes.FASTORE:
      case Opcodes.AASTORE:
        pop(3);
        break;
      case Opcodes.LASTORE:
      case Opcodes.DASTORE:
        pop(4);
        break;
      case Opcodes.POP:
      case Opcodes.IFEQ:
      case Opcodes.IFNE:
      case Opcodes.IFLT:
      case Opcodes.IFGE:
      case Opcodes.IFGT:
      case Opcodes.IFLE:
      case Opcodes.IRETURN:
      case Opcodes.FRETURN:
      case Opcodes.ARETURN:
      case Opcodes.TABLESWITCH:
      case Opcodes.LOOKUPSWITCH:
      case Opcodes.ATHROW:
      case Opcodes.MONITORENTER:
      case Opcodes.MONITOREXIT:
      case Opcodes.IFNULL:
      case Opcodes.IFNONNULL:
        pop(1);
        break;
      case Opcodes.POP2:
      case Opcodes.IF_ICMPEQ:
      case Opcodes.IF_ICMPNE:
      case Opcodes.IF_ICMPLT:
      case Opcodes.IF_ICMPGE:
      case Opcodes.IF_ICMPGT:
      case Opcodes.IF_ICMPLE:
      case Opcodes.IF_ACMPEQ:
      case Opcodes.IF_ACMPNE:
      case Opcodes.LRETURN:
      case Opcodes.DRETURN:
        pop(2);
        break;
      case Opcodes.DUP:
        abstractType1 = pop();
        push(abstractType1);
        push(abstractType1);
        break;
      case Opcodes.DUP_X1:
        abstractType1 = pop();
        abstractType2 = pop();
        push(abstractType1);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP_X2:
        abstractType1 = pop();
        abstractType2 = pop();
        abstractType3 = pop();
        push(abstractType1);
        push(abstractType3);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP2:
        abstractType1 = pop();
        abstractType2 = pop();
        push(abstractType2);
        push(abstractType1);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP2_X1:
        abstractType1 = pop();
        abstractType2 = pop();
        abstractType3 = pop();
        push(abstractType2);
        push(abstractType1);
        push(abstractType3);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.DUP2_X2:
        abstractType1 = pop();
        abstractType2 = pop();
        abstractType3 = pop();
        abstractType4 = pop();
        push(abstractType2);
        push(abstractType1);
        push(abstractType4);
        push(abstractType3);
        push(abstractType2);
        push(abstractType1);
        break;
      case Opcodes.SWAP:
        abstractType1 = pop();
        abstractType2 = pop();
        push(abstractType1);
        push(abstractType2);
        break;
      case Opcodes.IALOAD:
      case Opcodes.BALOAD:
      case Opcodes.CALOAD:
      case Opcodes.SALOAD:
      case Opcodes.IADD:
      case Opcodes.ISUB:
      case Opcodes.IMUL:
      case Opcodes.IDIV:
      case Opcodes.IREM:
      case Opcodes.IAND:
      case Opcodes.IOR:
      case Opcodes.IXOR:
      case Opcodes.ISHL:
      case Opcodes.ISHR:
      case Opcodes.IUSHR:
      case Opcodes.L2I:
      case Opcodes.D2I:
      case Opcodes.FCMPL:
      case Opcodes.FCMPG:
        pop(2);
        push(INTEGER);
        break;
      case Opcodes.LADD:
      case Opcodes.LSUB:
      case Opcodes.LMUL:
      case Opcodes.LDIV:
      case Opcodes.LREM:
      case Opcodes.LAND:
      case Opcodes.LOR:
      case Opcodes.LXOR:
        pop(4);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.FALOAD:
      case Opcodes.FADD:
      case Opcodes.FSUB:
      case Opcodes.FMUL:
      case Opcodes.FDIV:
      case Opcodes.FREM:
      case Opcodes.L2F:
      case Opcodes.D2F:
        pop(2);
        push(FLOAT);
        break;
      case Opcodes.DADD:
      case Opcodes.DSUB:
      case Opcodes.DMUL:
      case Opcodes.DDIV:
      case Opcodes.DREM:
        pop(4);
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.LSHL:
      case Opcodes.LSHR:
      case Opcodes.LUSHR:
        pop(3);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.IINC:
        setLocal(arg, INTEGER);
        break;
      case Opcodes.I2L:
      case Opcodes.F2L:
        pop(1);
        push(LONG);
        push(TOP);
        break;
      case Opcodes.I2F:
        pop(1);
        push(FLOAT);
        break;
      case Opcodes.I2D:
      case Opcodes.F2D:
        pop(1);
        push(DOUBLE);
        push(TOP);
        break;
      case Opcodes.F2I:
      case Opcodes.ARRAYLENGTH:
      case Opcodes.INSTANCEOF:
        pop(1);
        push(INTEGER);
        break;
      case Opcodes.LCMP:
      case Opcodes.DCMPL:
      case Opcodes.DCMPG:
        pop(4);
        push(INTEGER);
        break;
      case Opcodes.JSR:
      case Opcodes.RET:
        throw new IllegalArgumentException("JSR/RET are not supported with computeFrames option");
      case Opcodes.GETSTATIC:
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.PUTSTATIC:
        pop(argSymbol.value);
        break;
      case Opcodes.GETFIELD:
        pop(1);
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.PUTFIELD:
        pop(argSymbol.value);
        pop();
        break;
      case Opcodes.INVOKEVIRTUAL:
      case Opcodes.INVOKESPECIAL:
      case Opcodes.INVOKESTATIC:
      case Opcodes.INVOKEINTERFACE:
        pop(argSymbol.value);
        if (opcode != Opcodes.INVOKESTATIC) {
          abstractType1 = pop();
          if (opcode == Opcodes.INVOKESPECIAL && argSymbol.name.charAt(0) == '<') {
            addInitializedType(abstractType1);
          }
        }
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.INVOKEDYNAMIC:
        pop(argSymbol.value);
        push(symbolTable, argSymbol.value);
        break;
      case Opcodes.NEW:
        push(UNINITIALIZED_KIND | symbolTable.addUninitializedType(argSymbol.value, arg));
        break;
      case Opcodes.NEWARRAY:
        pop();
        switch (arg) {
          case Opcodes.T_BOOLEAN:
            push(ARRAY_OF | BOOLEAN);
            break;
          case Opcodes.T_CHAR:
            push(ARRAY_OF | CHAR);
            break;
          case Opcodes.T_BYTE:
            push(ARRAY_OF | BYTE);
            break;
          case Opcodes.T_SHORT:
            push(ARRAY_OF | SHORT);
            break;
          case Opcodes.T_INT:
            push(ARRAY_OF | INTEGER);
            break;
          case Opcodes.T_FLOAT:
            push(ARRAY_OF | FLOAT);
            break;
          case Opcodes.T_DOUBLE:
            push(ARRAY_OF | DOUBLE);
            break;
          case Opcodes.T_LONG:
            push(ARRAY_OF | LONG);
            break;
          default:
            throw new IllegalArgumentException();
        }
        break;
      case Opcodes.ANEWARRAY:
        String arrayElementType = argSymbol.value;
        pop();
        if (arrayElementType.charAt(0) == '[') {
          push(symbolTable, '[' + arrayElementType);
        } else {
          push(ARRAY_OF | REFERENCE_KIND | symbolTable.addType(arrayElementType));
        }
        break;
      case Opcodes.CHECKCAST:
        String castType = argSymbol.value;
        pop();
        if (castType.charAt(0) == '[') {
          push(symbolTable, castType);
        } else {
          push(REFERENCE_KIND | symbolTable.addType(castType));
        }
        break;
      case Opcodes.MULTIANEWARRAY:
        pop(arg);
        push(symbolTable, argSymbol.value);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Frame 合并相关方法，用于 StackMapFrame 计算算法的第二步
  // -----------------------------------------------------------------------------------------------

  /**
   * 根据给定的抽象输出类型计算具体输出类型。
   *
   * @param abstractOutputType 抽象输出类型。
   * @param numStack 输入栈大小，用于解析 STACK_KIND 类型。
   * @return 对应的具体输出类型。
   */
  private int getConcreteOutputType(final int abstractOutputType, final int numStack) {
    int dim = abstractOutputType & DIM_MASK;
    int kind = abstractOutputType & KIND_MASK;
    if (kind == LOCAL_KIND) {
      // LOCAL_KIND 类型表示该抽象类型对应基本块开始时的局部变量具体类型
      // （调用此方法时已知，但计算抽象类型时未知）。
      int concreteOutputType = dim + inputLocals[abstractOutputType & VALUE_MASK];
      if ((abstractOutputType & TOP_IF_LONG_OR_DOUBLE_FLAG) != 0
          && (concreteOutputType == LONG || concreteOutputType == DOUBLE)) {
        concreteOutputType = TOP;
      }
      return concreteOutputType;
    } else if (kind == STACK_KIND) {
      // STACK_KIND 类型表示该抽象类型对应基本块开始时的操作数栈具体类型
      // （调用此方法时已知，但计算抽象类型时未知）。
      int concreteOutputType = dim + inputStack[numStack - (abstractOutputType & VALUE_MASK)];
      if ((abstractOutputType & TOP_IF_LONG_OR_DOUBLE_FLAG) != 0
          && (concreteOutputType == LONG || concreteOutputType == DOUBLE)) {
        concreteOutputType = TOP;
      }
      return concreteOutputType;
    } else {
      return abstractOutputType;
    }
  }

  /**
   * 合并给定 Frame 的输入帧与当前 Frame 的输入和输出帧。
   * 如果给定 Frame 发生变化返回 true。
   *
   * @param symbolTable 用于类型查找和存储的类型表。
   * @param dstFrame 目标 Frame，其输入帧需要更新（通常是当前 Frame 的后继）。
   * @param catchTypeIndex 若为异常处理基本块，捕获异常类型在类型表的索引，否则为 0。
   * @return 若输入帧有变更则返回 true。
   */
  final boolean merge(
      final SymbolTable symbolTable, final Frame dstFrame, final int catchTypeIndex) {
    boolean frameChanged = false;

    // 计算当前基本块结束时局部变量的具体类型（通过解析抽象输出类型），
    // 并将这些具体类型与目标帧dstFrame输入局部变量进行合并。
    int numLocal = inputLocals.length;
    int numStack = inputStack.length;
    if (dstFrame.inputLocals == null) {
      dstFrame.inputLocals = new int[numLocal];
      frameChanged = true;
    }
    for (int i = 0; i < numLocal; ++i) {
      int concreteOutputType;
      if (outputLocals != null && i < outputLocals.length) {
        int abstractOutputType = outputLocals[i];
        if (abstractOutputType == 0) {
          // 如果该局部变量在基本块中未被赋值，则它的值等同于块开始时的值。
          concreteOutputType = inputLocals[i];
        } else {
          concreteOutputType = getConcreteOutputType(abstractOutputType, numStack);
        }
      } else {
        // 如果该局部变量在基本块中未被赋值，则它的值等同于块开始时的值。
        concreteOutputType = inputLocals[i];
      }
      // concreteOutputType 可能是未初始化类型（来自输入局部变量或输入操作数栈）。
      // 若在基本块中已调用构造函数，则该类型在基本块结束时不再是未初始化类型。
      if (initializations != null) {
        concreteOutputType = getInitializedType(symbolTable, concreteOutputType);
      }
      frameChanged |= merge(symbolTable, concreteOutputType, dstFrame.inputLocals, i);
    }

    // 若 dstFrame 是异常处理块，可能从对应基本块的任意指令达到，尤其是第一条指令。
    // 因此，dstFrame 的输入局部变量应与当前帧的输入局部变量兼容合并，
    // 且 dstFrame 输入操作数栈应与只包含捕获异常类型的单元素栈兼容合并。
    if (catchTypeIndex > 0) {
      for (int i = 0; i < numLocal; ++i) {
        frameChanged |= merge(symbolTable, inputLocals[i], dstFrame.inputLocals, i);
      }
      if (dstFrame.inputStack == null) {
        dstFrame.inputStack = new int[1];
        frameChanged = true;
      }
      frameChanged |= merge(symbolTable, catchTypeIndex, dstFrame.inputStack, 0);
      return frameChanged;
    }

    // 计算当前基本块结束时操作数栈的具体类型（通过解析抽象输出类型），
    // 并与目标帧dstFrame输入操作数栈合并。
    int numInputStack = inputStack.length + outputStackStart;
    if (dstFrame.inputStack == null) {
      dstFrame.inputStack = new int[numInputStack + outputStackTop];
      frameChanged = true;
    }
    // 先处理未被弹出的操作数栈元素，这些元素等于输入帧中的值（未初始化类型可能已初始化）。
    for (int i = 0; i < numInputStack; ++i) {
      int concreteOutputType = inputStack[i];
      if (initializations != null) {
        concreteOutputType = getInitializedType(symbolTable, concreteOutputType);
      }
      frameChanged |= merge(symbolTable, concreteOutputType, dstFrame.inputStack, i);
    }
    // 再处理在基本块内压入的操作数栈元素（处理方式同局部变量）。
    for (int i = 0; i < outputStackTop; ++i) {
      int abstractOutputType = outputStack[i];
      int concreteOutputType = getConcreteOutputType(abstractOutputType, numStack);
      if (initializations != null) {
        concreteOutputType = getInitializedType(symbolTable, concreteOutputType);
      }
      frameChanged |=
          merge(symbolTable, concreteOutputType, dstFrame.inputStack, numInputStack + i);
    }
    return frameChanged;
  }

  /**
   * 合并 dstTypes[dstIndex] 与 sourceType 的抽象类型。
   *
   * @param symbolTable 类型表。
   * @param sourceType 源抽象类型，类型应为 CONSTANT_KIND、REFERENCE_KIND 或 UNINITIALIZED_KIND。
   * @param dstTypes 目标抽象类型数组。
   * @param dstIndex 目标索引。
   * @return 若目标数组被修改返回 true。
   */
  private static boolean merge(
      final SymbolTable symbolTable,
      final int sourceType,
      final int[] dstTypes,
      final int dstIndex) {
    int dstType = dstTypes[dstIndex];
    if (dstType == sourceType) {
      // 如果类型相同，则 merge(sourceType, dstType) = dstType，无变化。
      return false;
    }
    int srcType = sourceType;
    if ((sourceType & ~DIM_MASK) == NULL) {
      if (dstType == NULL) {
        return false;
      }
      srcType = NULL;
    }
    if (dstType == 0) {
      // 如果 dstTypes[dstIndex] 从未赋值，则 merge(srcType, dstType) = srcType。
      dstTypes[dstIndex] = srcType;
      return true;
    }
    int mergedType;
    if ((dstType & DIM_MASK) != 0 || (dstType & KIND_MASK) == REFERENCE_KIND) {
      // 如果 dstType 是任意数组维度的引用类型。
      if (srcType == NULL) {
        // 如果 srcType 是 NULL 类型，merge(srcType, dstType) = dstType，无变化。
        return false;
      } else if ((srcType & (DIM_MASK | KIND_MASK)) == (dstType & (DIM_MASK | KIND_MASK))) {
        // 如果 srcType 和 dstType 具有相同数组维度和相同类型种类。
        if ((dstType & KIND_MASK) == REFERENCE_KIND) {
          // 如果 srcType 和 dstType 是相同数组维度的引用类型，
          // merge(srcType, dstType) = 维度(srcType) | 两者的公共父类。
          mergedType =
              (srcType & DIM_MASK)
                  | REFERENCE_KIND
                  | symbolTable.addMergedType(srcType & VALUE_MASK, dstType & VALUE_MASK);
        } else {
          // 如果 srcType 和 dstType 是相同维度但不同元素类型的数组类型，
          // merge(srcType, dstType) = (维度(srcType) - 1) | java/lang/Object。
          int mergedDim = ELEMENT_OF + (srcType & DIM_MASK);
          mergedType = mergedDim | REFERENCE_KIND | symbolTable.addType("java/lang/Object");
        }
      } else if ((srcType & DIM_MASK) != 0 || (srcType & KIND_MASK) == REFERENCE_KIND) {
        // 如果 srcType 是其他引用或数组类型，
        // merge(srcType, dstType) = min(srcDim, dstDim) | java/lang/Object，
        // 其中 srcDim 和 dstDim 为数组维度，如果元素类型不是引用则减 1。
        int srcDim = srcType & DIM_MASK;
        if (srcDim != 0 && (srcType & KIND_MASK) != REFERENCE_KIND) {
          srcDim = ELEMENT_OF + srcDim;
        }
        int dstDim = dstType & DIM_MASK;
        if (dstDim != 0 && (dstType & KIND_MASK) != REFERENCE_KIND) {
          dstDim = ELEMENT_OF + dstDim;
        }
        mergedType =
            Math.min(srcDim, dstDim) | REFERENCE_KIND | symbolTable.addType("java/lang/Object");
      } else {
        // 如果 srcType 是其他类型，merge(srcType, dstType) = TOP。
        mergedType = TOP;
      }
    } else if (dstType == NULL) {
      // 如果 dstType 是 NULL 类型，
      // merge(srcType, dstType) = srcType；如果 srcType 不是数组或引用类型则为 TOP。
      mergedType =
          (srcType & DIM_MASK) != 0 || (srcType & KIND_MASK) == REFERENCE_KIND ? srcType : TOP;
    } else {
      // 如果 dstType 是其他类型，不管 srcType 是什么，merge(srcType, dstType) = TOP。
      mergedType = TOP;
    }
    if (mergedType != dstType) {
      dstTypes[dstIndex] = mergedType;
      return true;
    }
    return false;
  }

  // -----------------------------------------------------------------------------------------------
  // Frame 输出方法，用于生成 StackMapFrame 属性
  // -----------------------------------------------------------------------------------------------

  /**
   * 让给定的 {@link MethodWriter} 访问此 {@link Frame} 的输入帧。
   *
   * @param methodWriter 访问者。
   */
  final void accept(final MethodWriter methodWriter) {
    // 计算局部变量数量，忽略LONG或DOUBLE后紧跟的TOP类型，以及所有尾随的TOP类型。
    int[] localTypes = inputLocals;
    int numLocal = 0;
    int numTrailingTop = 0;
    int i = 0;
    while (i < localTypes.length) {
      int localType = localTypes[i];
      i += (localType == LONG || localType == DOUBLE) ? 2 : 1;
      if (localType == TOP) {
        numTrailingTop++;
      } else {
        numLocal += numTrailingTop + 1;
        numTrailingTop = 0;
      }
    }
    // 计算操作数栈大小，忽略LONG或DOUBLE后紧跟的TOP类型。
    int[] stackTypes = inputStack;
    int numStack = 0;
    i = 0;
    while (i < stackTypes.length) {
      int stackType = stackTypes[i];
      i += (stackType == LONG || stackType == DOUBLE) ? 2 : 1;
      numStack++;
    }
    // 访问帧及其内容。
    int frameIndex = methodWriter.visitFrameStart(owner.bytecodeOffset, numLocal, numStack);
    i = 0;
    while (numLocal-- > 0) {
      int localType = localTypes[i];
      i += (localType == LONG || localType == DOUBLE) ? 2 : 1;
      methodWriter.visitAbstractType(frameIndex++, localType);
    }
    i = 0;
    while (numStack-- > 0) {
      int stackType = stackTypes[i];
      i += (stackType == LONG || stackType == DOUBLE) ? 2 : 1;
      methodWriter.visitAbstractType(frameIndex++, stackType);
    }
    methodWriter.visitFrameEnd();
  }

  /**
   * 将给定的抽象类型以 JVMS verification_type_info 格式写入 ByteVector。
   *
   * @param symbolTable 类型表。
   * @param abstractType 抽象类型，仅限 CONSTANT_KIND、REFERENCE_KIND 或 UNINITIALIZED_KIND。
   * @param output 输出目标 ByteVector。
   */
  static void putAbstractType(
      final SymbolTable symbolTable, final int abstractType, final ByteVector output) {
    int arrayDimensions = (abstractType & Frame.DIM_MASK) >> DIM_SHIFT;
    if (arrayDimensions == 0) {
      int typeValue = abstractType & VALUE_MASK;
      switch (abstractType & KIND_MASK) {
        case CONSTANT_KIND:
          output.putByte(typeValue);
          break;
        case REFERENCE_KIND:
          output
              .putByte(ITEM_OBJECT)
              .putShort(symbolTable.addConstantClass(symbolTable.getType(typeValue).value).index);
          break;
        case UNINITIALIZED_KIND:
          output.putByte(ITEM_UNINITIALIZED).putShort((int) symbolTable.getType(typeValue).data);
          break;
        default:
          throw new AssertionError();
      }
    } else {
      // 数组类型的情况，我们需要先构建它的描述符。
      StringBuilder typeDescriptor = new StringBuilder(32);  // SPRING 补丁：增大初始容量
      while (arrayDimensions-- > 0) {
        typeDescriptor.append('[');
      }
      if ((abstractType & KIND_MASK) == REFERENCE_KIND) {
        typeDescriptor
            .append('L')
            .append(symbolTable.getType(abstractType & VALUE_MASK).value)
            .append(';');
      } else {
        switch (abstractType & VALUE_MASK) {
          case Frame.ITEM_ASM_BOOLEAN:
            typeDescriptor.append('Z');
            break;
          case Frame.ITEM_ASM_BYTE:
            typeDescriptor.append('B');
            break;
          case Frame.ITEM_ASM_CHAR:
            typeDescriptor.append('C');
            break;
          case Frame.ITEM_ASM_SHORT:
            typeDescriptor.append('S');
            break;
          case Frame.ITEM_INTEGER:
            typeDescriptor.append('I');
            break;
          case Frame.ITEM_FLOAT:
            typeDescriptor.append('F');
            break;
          case Frame.ITEM_LONG:
            typeDescriptor.append('J');
            break;
          case Frame.ITEM_DOUBLE:
            typeDescriptor.append('D');
            break;
          default:
            throw new AssertionError();
        }
      }
      output
          .putByte(ITEM_OBJECT)
          .putShort(symbolTable.addConstantClass(typeDescriptor.toString()).index);
    }
  }
}
