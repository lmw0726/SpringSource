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
 * 一个 {@link MethodVisitor}，用于生成对应的 'method_info' 结构，
 * 如 Java 虚拟机规范（JVMS）中定义的那样。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6">JVMS 4.6</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class MethodWriter extends MethodVisitor {

  /** 表示不需要进行任何计算。 */
  static final int COMPUTE_NOTHING = 0;

  /**
   * 表示必须从头计算最大栈深和最大本地变量数量。
   */
  static final int COMPUTE_MAX_STACK_AND_LOCAL = 1;

  /**
   * 表示必须基于已有的栈映射帧计算最大栈深和最大本地变量数量。
   * 这种方法比 {@link #COMPUTE_MAX_STACK_AND_LOCAL} 使用的控制流图算法更高效，
   * 通过对字节码指令的线性扫描实现。
   */
  static final int COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES = 2;

  /**
   * 表示必须计算类型为 F_INSERT 的栈映射帧。
   * 其他类型的帧不计算，它们都应为 F_NEW 类型，
   * 并且足以配合 F_NEW 和 F_INSERT 帧之间的字节码指令计算 F_INSERT 帧的内容，
   * 无需了解类型层次结构（这是 F_INSERT 的定义）。
   */
  static final int COMPUTE_INSERTED_FRAMES = 3;

  /**
   * 表示必须计算所有的栈映射帧。
   * 在此情况下，也会计算最大栈深和最大本地变量数量。
   */
  static final int COMPUTE_ALL_FRAMES = 4;

  /** 表示 {@link #STACK_SIZE_DELTA} 不适用（不恒定或未使用）。 */
  private static final int NA = 0;

  /**
   * 对应每个 JVM 操作码的栈大小变化。
   * 操作码 'o' 的栈大小变化由数组中索引为 'o' 的元素给出。
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html">JVMS 6</a>
   */
  private static final int[] STACK_SIZE_DELTA = {
    0, // nop = 0 (0x0)
    1, // aconst_null = 1 (0x1)
    1, // iconst_m1 = 2 (0x2)
    1, // iconst_0 = 3 (0x3)
    1, // iconst_1 = 4 (0x4)
    1, // iconst_2 = 5 (0x5)
    1, // iconst_3 = 6 (0x6)
    1, // iconst_4 = 7 (0x7)
    1, // iconst_5 = 8 (0x8)
    2, // lconst_0 = 9 (0x9)
    2, // lconst_1 = 10 (0xa)
    1, // fconst_0 = 11 (0xb)
    1, // fconst_1 = 12 (0xc)
    1, // fconst_2 = 13 (0xd)
    2, // dconst_0 = 14 (0xe)
    2, // dconst_1 = 15 (0xf)
    1, // bipush = 16 (0x10)
    1, // sipush = 17 (0x11)
    1, // ldc = 18 (0x12)
    NA, // ldc_w = 19 (0x13)
    NA, // ldc2_w = 20 (0x14)
    1, // iload = 21 (0x15)
    2, // lload = 22 (0x16)
    1, // fload = 23 (0x17)
    2, // dload = 24 (0x18)
    1, // aload = 25 (0x19)
    NA, // iload_0 = 26 (0x1a)
    NA, // iload_1 = 27 (0x1b)
    NA, // iload_2 = 28 (0x1c)
    NA, // iload_3 = 29 (0x1d)
    NA, // lload_0 = 30 (0x1e)
    NA, // lload_1 = 31 (0x1f)
    NA, // lload_2 = 32 (0x20)
    NA, // lload_3 = 33 (0x21)
    NA, // fload_0 = 34 (0x22)
    NA, // fload_1 = 35 (0x23)
    NA, // fload_2 = 36 (0x24)
    NA, // fload_3 = 37 (0x25)
    NA, // dload_0 = 38 (0x26)
    NA, // dload_1 = 39 (0x27)
    NA, // dload_2 = 40 (0x28)
    NA, // dload_3 = 41 (0x29)
    NA, // aload_0 = 42 (0x2a)
    NA, // aload_1 = 43 (0x2b)
    NA, // aload_2 = 44 (0x2c)
    NA, // aload_3 = 45 (0x2d)
    -1, // iaload = 46 (0x2e)
    0, // laload = 47 (0x2f)
    -1, // faload = 48 (0x30)
    0, // daload = 49 (0x31)
    -1, // aaload = 50 (0x32)
    -1, // baload = 51 (0x33)
    -1, // caload = 52 (0x34)
    -1, // saload = 53 (0x35)
    -1, // istore = 54 (0x36)
    -2, // lstore = 55 (0x37)
    -1, // fstore = 56 (0x38)
    -2, // dstore = 57 (0x39)
    -1, // astore = 58 (0x3a)
    NA, // istore_0 = 59 (0x3b)
    NA, // istore_1 = 60 (0x3c)
    NA, // istore_2 = 61 (0x3d)
    NA, // istore_3 = 62 (0x3e)
    NA, // lstore_0 = 63 (0x3f)
    NA, // lstore_1 = 64 (0x40)
    NA, // lstore_2 = 65 (0x41)
    NA, // lstore_3 = 66 (0x42)
    NA, // fstore_0 = 67 (0x43)
    NA, // fstore_1 = 68 (0x44)
    NA, // fstore_2 = 69 (0x45)
    NA, // fstore_3 = 70 (0x46)
    NA, // dstore_0 = 71 (0x47)
    NA, // dstore_1 = 72 (0x48)
    NA, // dstore_2 = 73 (0x49)
    NA, // dstore_3 = 74 (0x4a)
    NA, // astore_0 = 75 (0x4b)
    NA, // astore_1 = 76 (0x4c)
    NA, // astore_2 = 77 (0x4d)
    NA, // astore_3 = 78 (0x4e)
    -3, // iastore = 79 (0x4f)
    -4, // lastore = 80 (0x50)
    -3, // fastore = 81 (0x51)
    -4, // dastore = 82 (0x52)
    -3, // aastore = 83 (0x53)
    -3, // bastore = 84 (0x54)
    -3, // castore = 85 (0x55)
    -3, // sastore = 86 (0x56)
    -1, // pop = 87 (0x57)
    -2, // pop2 = 88 (0x58)
    1, // dup = 89 (0x59)
    1, // dup_x1 = 90 (0x5a)
    1, // dup_x2 = 91 (0x5b)
    2, // dup2 = 92 (0x5c)
    2, // dup2_x1 = 93 (0x5d)
    2, // dup2_x2 = 94 (0x5e)
    0, // swap = 95 (0x5f)
    -1, // iadd = 96 (0x60)
    -2, // ladd = 97 (0x61)
    -1, // fadd = 98 (0x62)
    -2, // dadd = 99 (0x63)
    -1, // isub = 100 (0x64)
    -2, // lsub = 101 (0x65)
    -1, // fsub = 102 (0x66)
    -2, // dsub = 103 (0x67)
    -1, // imul = 104 (0x68)
    -2, // lmul = 105 (0x69)
    -1, // fmul = 106 (0x6a)
    -2, // dmul = 107 (0x6b)
    -1, // idiv = 108 (0x6c)
    -2, // ldiv = 109 (0x6d)
    -1, // fdiv = 110 (0x6e)
    -2, // ddiv = 111 (0x6f)
    -1, // irem = 112 (0x70)
    -2, // lrem = 113 (0x71)
    -1, // frem = 114 (0x72)
    -2, // drem = 115 (0x73)
    0, // ineg = 116 (0x74)
    0, // lneg = 117 (0x75)
    0, // fneg = 118 (0x76)
    0, // dneg = 119 (0x77)
    -1, // ishl = 120 (0x78)
    -1, // lshl = 121 (0x79)
    -1, // ishr = 122 (0x7a)
    -1, // lshr = 123 (0x7b)
    -1, // iushr = 124 (0x7c)
    -1, // lushr = 125 (0x7d)
    -1, // iand = 126 (0x7e)
    -2, // land = 127 (0x7f)
    -1, // ior = 128 (0x80)
    -2, // lor = 129 (0x81)
    -1, // ixor = 130 (0x82)
    -2, // lxor = 131 (0x83)
    0, // iinc = 132 (0x84)
    1, // i2l = 133 (0x85)
    0, // i2f = 134 (0x86)
    1, // i2d = 135 (0x87)
    -1, // l2i = 136 (0x88)
    -1, // l2f = 137 (0x89)
    0, // l2d = 138 (0x8a)
    0, // f2i = 139 (0x8b)
    1, // f2l = 140 (0x8c)
    1, // f2d = 141 (0x8d)
    -1, // d2i = 142 (0x8e)
    0, // d2l = 143 (0x8f)
    -1, // d2f = 144 (0x90)
    0, // i2b = 145 (0x91)
    0, // i2c = 146 (0x92)
    0, // i2s = 147 (0x93)
    -3, // lcmp = 148 (0x94)
    -1, // fcmpl = 149 (0x95)
    -1, // fcmpg = 150 (0x96)
    -3, // dcmpl = 151 (0x97)
    -3, // dcmpg = 152 (0x98)
    -1, // ifeq = 153 (0x99)
    -1, // ifne = 154 (0x9a)
    -1, // iflt = 155 (0x9b)
    -1, // ifge = 156 (0x9c)
    -1, // ifgt = 157 (0x9d)
    -1, // ifle = 158 (0x9e)
    -2, // if_icmpeq = 159 (0x9f)
    -2, // if_icmpne = 160 (0xa0)
    -2, // if_icmplt = 161 (0xa1)
    -2, // if_icmpge = 162 (0xa2)
    -2, // if_icmpgt = 163 (0xa3)
    -2, // if_icmple = 164 (0xa4)
    -2, // if_acmpeq = 165 (0xa5)
    -2, // if_acmpne = 166 (0xa6)
    0, // goto = 167 (0xa7)
    1, // jsr = 168 (0xa8)
    0, // ret = 169 (0xa9)
    -1, // tableswitch = 170 (0xaa)
    -1, // lookupswitch = 171 (0xab)
    -1, // ireturn = 172 (0xac)
    -2, // lreturn = 173 (0xad)
    -1, // freturn = 174 (0xae)
    -2, // dreturn = 175 (0xaf)
    -1, // areturn = 176 (0xb0)
    0, // return = 177 (0xb1)
    NA, // getstatic = 178 (0xb2)
    NA, // putstatic = 179 (0xb3)
    NA, // getfield = 180 (0xb4)
    NA, // putfield = 181 (0xb5)
    NA, // invokevirtual = 182 (0xb6)
    NA, // invokespecial = 183 (0xb7)
    NA, // invokestatic = 184 (0xb8)
    NA, // invokeinterface = 185 (0xb9)
    NA, // invokedynamic = 186 (0xba)
    1, // new = 187 (0xbb)
    0, // newarray = 188 (0xbc)
    0, // anewarray = 189 (0xbd)
    0, // arraylength = 190 (0xbe)
    NA, // athrow = 191 (0xbf)
    0, // checkcast = 192 (0xc0)
    0, // instanceof = 193 (0xc1)
    -1, // monitorenter = 194 (0xc2)
    -1, // monitorexit = 195 (0xc3)
    NA, // wide = 196 (0xc4)
    NA, // multianewarray = 197 (0xc5)
    -1, // ifnull = 198 (0xc6)
    -1, // ifnonnull = 199 (0xc7)
    NA, // goto_w = 200 (0xc8)
    NA // jsr_w = 201 (0xc9)
  };

  /** 此 MethodWriter 使用到的常量存储位置。 */
  private final SymbolTable symbolTable;

  // 注意：字段的排列顺序与 method_info 结构一致，
  // 与属性相关的字段则按 JVMS 第 4.7 节中的顺序排列。

  /**
   * method_info JVMS 结构中的 access_flags 字段。
   * 此字段可以包含 ASM 特有的访问标志，例如 {@link Opcodes#ACC_DEPRECATED}，
   * 这些标志在生成 ClassFile 结构时会被移除。
   */
  private final int accessFlags;

  /** method_info JVMS 结构中的 name_index 字段。 */
  private final int nameIndex;

  /** 此方法的名称。 */
  private final String name;

  /** method_info JVMS 结构中的 descriptor_index 字段。 */
  private final int descriptorIndex;

  /** 此方法的描述符。 */
  private final String descriptor;

  // Code 属性字段及其子属性：

  /** Code 属性中的 max_stack 字段。 */
  private int maxStack;

  /** Code 属性中的 max_locals 字段。 */
  private int maxLocals;

  /** Code 属性中的 'code' 字段。 */
  private final ByteVector code = new ByteVector();

  /**
   * 异常处理器链表的第一个元素（用于生成 Code 属性的 exception_table）。
   * 后续元素可通过 {@link Handler#nextHandler} 字段访问。
   * 可能为 {@literal null}。
   */
  private Handler firstHandler;

  /**
   * 异常处理器链表的最后一个元素（用于生成 Code 属性的 exception_table）。
   * 后续元素可通过 {@link Handler#nextHandler} 字段访问。
   * 可能为 {@literal null}。
   */
  private Handler lastHandler;

  /** LineNumberTable 代码属性的 line_number_table_length 字段。 */
  private int lineNumberTableLength;

  /** LineNumberTable 代码属性的 line_number_table 数组，或 {@literal null}。 */
  private ByteVector lineNumberTable;

  /** LocalVariableTable 代码属性的 local_variable_table_length 字段。 */
  private int localVariableTableLength;

  /**
   * LocalVariableTable 代码属性的 local_variable_table 数组，或 {@literal null}。
   */
  private ByteVector localVariableTable;

  /** LocalVariableTypeTable 代码属性的 local_variable_type_table_length 字段。 */
  private int localVariableTypeTableLength;

  /**
   * LocalVariableTypeTable 代码属性的 local_variable_type_table 数组，或 {@literal null}。
   */
  private ByteVector localVariableTypeTable;

  /** StackMapTable 代码属性的 number_of_entries 字段。 */
  private int stackMapTableNumberOfEntries;

  /** StackMapTable 代码属性的 'entries' 数组。 */
  private ByteVector stackMapTableEntries;

  /**
   * Code 属性的最后一个运行时可见类型注解。
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter lastCodeRuntimeVisibleTypeAnnotation;

  /**
   * Code 属性的最后一个运行时不可见类型注解。
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter lastCodeRuntimeInvisibleTypeAnnotation;

  /**
   * Code 属性的第一个非标准属性。后续属性可通过 {@link Attribute#nextAttribute} 字段访问。
   * 可能为 {@literal null}。
   *
   * <p><b>警告</b>：此列表按访问顺序的<i>相反</i>顺序存储属性。
   * firstAttribute 实际上是 {@link #visitAttribute} 中最后访问的属性。
   * {@link #putMethodInfo} 方法会按照该列表定义的顺序写出属性，
   * 即用户指定顺序的反序。
   */
  private Attribute firstCodeAttribute;

  // 其他 method_info 属性：

  /** Exceptions 属性的 number_of_exceptions 字段。 */
  private final int numberOfExceptions;

  /** Exceptions 属性的 exception_index_table 数组，或 {@literal null}。 */
  private final int[] exceptionIndexTable;

  /** Signature 属性的 signature_index 字段。 */
  private final int signatureIndex;

  /**
   * 此方法的最后一个运行时可见注解。
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 此方法的最后一个运行时不可见注解。
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /** 可以具有运行时可见注解的方法参数数量，如果没有则为 0。 */
  private int visibleAnnotableParameterCount;

  /**
   * 此方法的运行时可见参数注解。
   * 每个数组元素包含某个参数的最后一个注解（可能为 {@literal null}；
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问）。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter[] lastRuntimeVisibleParameterAnnotations;

  /** 可以具有运行时不可见注解的方法参数数量，如果没有则为 0。 */
  private int invisibleAnnotableParameterCount;

  /**
   * 此方法的运行时不可见参数注解。
   * 每个数组元素包含某个参数的最后一个注解（可能为 {@literal null}；
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问）。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter[] lastRuntimeInvisibleParameterAnnotations;

  /**
   * 此方法的最后一个运行时可见类型注解。
   * 之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 字段访问。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 此方法的最后一个运行时不可见类型注解。之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 字段访问。
   * 可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /** AnnotationDefault 属性的 default_value 字段，或 {@literal null}。 */
  private ByteVector defaultValue;

  /** MethodParameters 属性的 parameters_count 字段。 */
  private int parametersCount;

  /** MethodParameters 属性的 'parameters' 数组，或 {@literal null}。 */
  private ByteVector parameters;

  /**
   * 此方法的第一个非标准属性。后续属性可通过 {@link Attribute#nextAttribute} 字段访问。
   * 可能为 {@literal null}。
   *
   * <p><b>警告</b>：此列表按它们访问的<i>相反</i>顺序存储属性。
   * firstAttribute 实际上是 {@link #visitAttribute} 中最后访问的属性。
   * {@link #putMethodInfo} 方法会按照此列表定义的顺序写出属性，即用户指定顺序的反序。
   */
  private Attribute firstAttribute;

  // -----------------------------------------------------------------------------------------------
  // 用于计算最大栈深、局部变量数量及栈映射帧的字段
  // -----------------------------------------------------------------------------------------------

  /**
   * 表示需要计算的内容。必须是 {@link #COMPUTE_ALL_FRAMES}、{@link #COMPUTE_INSERTED_FRAMES}、
   * {@link COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES}、{@link #COMPUTE_MAX_STACK_AND_LOCAL}
   * 或 {@link #COMPUTE_NOTHING} 之一。
   */
  private final int compute;

  /**
   * 方法的第一个基本块。按字节码偏移顺序的下一个基本块可通过 {@link Label#nextBasicBlock} 字段访问。
   */
  private Label firstBasicBlock;

  /**
   * 方法的最后一个基本块（按字节码偏移顺序）。
   * 每遇到一个基本块时都会更新该字段，用于将其追加到基本块链表末尾。
   */
  private Label lastBasicBlock;

  /**
   * 当前基本块，即最后访问指令所在的基本块。
   * 当 {@link #compute} 等于 {@link #COMPUTE_MAX_STACK_AND_LOCAL} 或 {@link #COMPUTE_ALL_FRAMES} 时，
   * 对于不可达代码，该字段为 {@literal null}。
   * 当 {@link #compute} 等于 {@link #COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES} 或
   * {@link #COMPUTE_INSERTED_FRAMES} 时，该字段在整个方法期间保持不变
   * （即整个代码被视为单个基本块，因为假设现有帧足以计算任何中间帧以及最大栈深，
   * 无需使用控制流图）。
   */
  private Label currentBasicBlock;

  /**
   * 最后访问指令后的相对栈深。
   * 该大小是相对于 {@link #currentBasicBlock} 开始位置的，
   * 即最后访问指令后的实际栈深 = 当前基本块的 {@link Label#inputStackSize} + {@link #relativeStackSize}。
   * 当 {@link #compute} 等于 {@link #COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES} 时，
   * {@link #currentBasicBlock} 总是方法起始位置，因此该相对大小等于最后访问指令后的绝对栈深。
   */
  private int relativeStackSize;

  /**
   * 最后访问指令后的最大相对栈深。
   * 该大小是相对于 {@link #currentBasicBlock} 开始位置的，
   * 即实际最大栈深 = 当前基本块的 {@link Label#inputStackSize} + {@link #maxRelativeStackSize}。
   * 当 {@link #compute} 等于 {@link #COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES} 时，
   * {@link #currentBasicBlock} 总是方法起始位置，因此该相对大小等于最后访问指令后的绝对最大栈深。
   */
  private int maxRelativeStackSize;

  /** 最后访问的栈映射帧中的局部变量数量。 */
  private int currentLocals;

  /** 在 {@link #stackMapTableEntries} 中最后写入的帧的字节码偏移量。 */
  private int previousFrameOffset;

  /**
   * 在 {@link #stackMapTableEntries} 中最后写入的帧。该字段的格式与 {@link #currentFrame} 相同。
   */
  private int[] previousFrame;

  /**
   * 当前的栈映射帧。第一个元素是与该帧对应的指令的字节码偏移量，第二个元素是本地变量数量，
   * 第三个元素是操作数栈元素数量。本地变量从索引 3 开始，后面紧跟着操作数栈元素。
   * 总结：frame[0] = offset，frame[1] = numLocal，frame[2] = numStack。
   * 本地变量和操作数栈条目包含抽象类型（参见 {@link Frame}），但仅限于
   * {@link Frame#CONSTANT_KIND}、{@link Frame#REFERENCE_KIND} 或
   * {@link Frame#UNINITIALIZED_KIND} 抽象类型。long 和 double 类型只占用一个数组元素。
   */
  private int[] currentFrame;

  /** 该方法是否包含子程序。 */
  private boolean hasSubroutines;

  // -----------------------------------------------------------------------------------------------
  // 其他杂项状态字段
  // -----------------------------------------------------------------------------------------------

  /** 该方法的字节码是否包含 ASM 特定的指令。 */
  private boolean hasAsmInstructions;

  /**
   * 最近访问的指令的起始偏移量。
   * 用于设置类型为 "offset_target" 的类型注解的 offset 字段
   * （参见 <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.1">JVMS 4.7.20.1</a>）。
   */
  private int lastBytecodeOffset;

  /**
   * 在 {@link SymbolTable#getSource} 中的字节偏移量，从该位置开始复制该方法的 method_info
   * （不包括前 6 个字节），如果为 0 则表示不需要复制。
   */
  private int sourceOffset;

  /**
   * 在 {@link SymbolTable#getSource} 中需要复制的字节长度，
   * 以获取该方法的 method_info（不包括前 6 个字节，即 access_flags、name_index 和 descriptor_index）。
   */
  private int sourceLength;

  // -----------------------------------------------------------------------------------------------
  // 构造方法与访问器
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link MethodWriter}。
   *
   * @param symbolTable 常量池存储位置，本 AnnotationWriter 使用到的常量会存储在此处。
   * @param access 方法的访问标志（参见 {@link Opcodes}）。
   * @param name 方法名。
   * @param descriptor 方法描述符（参见 {@link Type}）。
   * @param signature 方法签名，可以为 {@literal null}。
   * @param exceptions 方法异常的内部名称数组，可以为 {@literal null}。
   * @param compute 需要计算的内容（参见 #compute）。
   */
  MethodWriter(
      final SymbolTable symbolTable,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions,
      final int compute) {
    super(/* 最新的API = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.accessFlags = "<init>".equals(name) ? access | Constants.ACC_CONSTRUCTOR : access;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.name = name;
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    this.descriptor = descriptor;
    this.signatureIndex = signature == null ? 0 : symbolTable.addConstantUtf8(signature);
    if (exceptions != null && exceptions.length > 0) {
      numberOfExceptions = exceptions.length;
      this.exceptionIndexTable = new int[numberOfExceptions];
      for (int i = 0; i < numberOfExceptions; ++i) {
        this.exceptionIndexTable[i] = symbolTable.addConstantClass(exceptions[i]).index;
      }
    } else {
      numberOfExceptions = 0;
      this.exceptionIndexTable = null;
    }
    this.compute = compute;
    if (compute != COMPUTE_NOTHING) {
      // 更新maxLocals和currentLocals。
      int argumentsSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
      if ((access & Opcodes.ACC_STATIC) != 0) {
        --argumentsSize;
      }
      maxLocals = argumentsSize;
      currentLocals = argumentsSize;
      // 创建并访问第一个基本块的标签。
      firstBasicBlock = new Label();
      visitLabel(firstBasicBlock);
    }
  }

  boolean hasFrames() {
    return stackMapTableNumberOfEntries > 0;
  }

  boolean hasAsmInstructions() {
    return hasAsmInstructions;
  }

  // -----------------------------------------------------------------------------------------------
  // MethodVisitor 抽象类的实现
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitParameter(final String name, final int access) {
    if (parameters == null) {
      parameters = new ByteVector();
    }
    ++parametersCount;
    parameters.putShort((name == null) ? 0 : symbolTable.addConstantUtf8(name)).putShort(access);
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    defaultValue = new ByteVector();
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ false, defaultValue, null);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
    } else {
      return lastRuntimeInvisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastRuntimeVisibleTypeAnnotation);
    } else {
      return lastRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    if (visible) {
      visibleAnnotableParameterCount = parameterCount;
    } else {
      invisibleAnnotableParameterCount = parameterCount;
    }
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String annotationDescriptor, final boolean visible) {
    if (visible) {
      if (lastRuntimeVisibleParameterAnnotations == null) {
        lastRuntimeVisibleParameterAnnotations =
            new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
      }
      return lastRuntimeVisibleParameterAnnotations[parameter] =
          AnnotationWriter.create(
              symbolTable, annotationDescriptor, lastRuntimeVisibleParameterAnnotations[parameter]);
    } else {
      if (lastRuntimeInvisibleParameterAnnotations == null) {
        lastRuntimeInvisibleParameterAnnotations =
            new AnnotationWriter[Type.getArgumentTypes(descriptor).length];
      }
      return lastRuntimeInvisibleParameterAnnotations[parameter] =
          AnnotationWriter.create(
              symbolTable,
              annotationDescriptor,
              lastRuntimeInvisibleParameterAnnotations[parameter]);
    }
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    // 按照该方法访问它们的<i>逆序</i>存储属性。
    if (attribute.isCodeAttribute()) {
      attribute.nextAttribute = firstCodeAttribute;
      firstCodeAttribute = attribute;
    } else {
      attribute.nextAttribute = firstAttribute;
      firstAttribute = attribute;
    }
  }

  @Override
  public void visitCode() {
    // 无需执行任何操作。
  }

  @Override
  public void visitFrame(
      final int type,
      final int numLocal,
      final Object[] local,
      final int numStack,
      final Object[] stack) {
    if (compute == COMPUTE_ALL_FRAMES) {
      return;
    }

    if (compute == COMPUTE_INSERTED_FRAMES) {
      if (currentBasicBlock.frame == null) {
        // 这种情况只会发生一次，即隐式的第一个帧（如果使用了 EXPAND_ASM_INSNS 选项，
        // 在 ClassReader 中会显式访问它 —— 而且如果没有使用 EXPAND_ASM_INSNS 选项，
        // 就不可能设置 COMPUTE_INSERTED_FRAMES）。
        currentBasicBlock.frame = new CurrentFrame(currentBasicBlock);
        currentBasicBlock.frame.setInputFrameFromDescriptor(
            symbolTable, accessFlags, descriptor, numLocal);
        currentBasicBlock.frame.accept(this);
      } else {
        if (type == Opcodes.F_NEW) {
          currentBasicBlock.frame.setInputFrameFromApiFormat(
              symbolTable, numLocal, local, numStack, stack);
        }
        // 如果 type 不是 F_NEW，那么根据假设它是 F_INSERT，
        // 而 currentBlock.frame 包含当前指令处的栈映射帧，
        // 该帧是由上一个 F_NEW 帧以及其间的字节码指令（通过调用 CurrentFrame#execute）计算得出的。
        currentBasicBlock.frame.accept(this);
      }
    } else if (type == Opcodes.F_NEW) {
      if (previousFrame == null) {
        int argumentsSize = Type.getArgumentsAndReturnSizes(descriptor) >> 2;
        Frame implicitFirstFrame = new Frame(new Label());
        implicitFirstFrame.setInputFrameFromDescriptor(
            symbolTable, accessFlags, descriptor, argumentsSize);
        implicitFirstFrame.accept(this);
      }
      currentLocals = numLocal;
      int frameIndex = visitFrameStart(code.length, numLocal, numStack);
      for (int i = 0; i < numLocal; ++i) {
        currentFrame[frameIndex++] = Frame.getAbstractTypeFromApiFormat(symbolTable, local[i]);
      }
      for (int i = 0; i < numStack; ++i) {
        currentFrame[frameIndex++] = Frame.getAbstractTypeFromApiFormat(symbolTable, stack[i]);
      }
      visitFrameEnd();
    } else {
      if (symbolTable.getMajorVersion() < Opcodes.V1_6) {
        throw new IllegalArgumentException("Class versions V1_5 or less must use F_NEW frames.");
      }
      int offsetDelta;
      if (stackMapTableEntries == null) {
        stackMapTableEntries = new ByteVector();
        offsetDelta = code.length;
      } else {
        offsetDelta = code.length - previousFrameOffset - 1;
        if (offsetDelta < 0) {
          if (type == Opcodes.F_SAME) {
            return;
          } else {
            throw new IllegalStateException();
          }
        }
      }

      switch (type) {
        case Opcodes.F_FULL:
          currentLocals = numLocal;
          stackMapTableEntries.putByte(Frame.FULL_FRAME).putShort(offsetDelta).putShort(numLocal);
          for (int i = 0; i < numLocal; ++i) {
            putFrameType(local[i]);
          }
          stackMapTableEntries.putShort(numStack);
          for (int i = 0; i < numStack; ++i) {
            putFrameType(stack[i]);
          }
          break;
        case Opcodes.F_APPEND:
          currentLocals += numLocal;
          stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED + numLocal).putShort(offsetDelta);
          for (int i = 0; i < numLocal; ++i) {
            putFrameType(local[i]);
          }
          break;
        case Opcodes.F_CHOP:
          currentLocals -= numLocal;
          stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED - numLocal).putShort(offsetDelta);
          break;
        case Opcodes.F_SAME:
          if (offsetDelta < 64) {
            stackMapTableEntries.putByte(offsetDelta);
          } else {
            stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED).putShort(offsetDelta);
          }
          break;
        case Opcodes.F_SAME1:
          if (offsetDelta < 64) {
            stackMapTableEntries.putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME + offsetDelta);
          } else {
            stackMapTableEntries
                .putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
                .putShort(offsetDelta);
          }
          putFrameType(stack[0]);
          break;
        default:
          throw new IllegalArgumentException();
      }

      previousFrameOffset = code.length;
      ++stackMapTableNumberOfEntries;
    }

    if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) {
      relativeStackSize = numStack;
      for (int i = 0; i < numStack; ++i) {
        if (stack[i] == Opcodes.LONG || stack[i] == Opcodes.DOUBLE) {
          relativeStackSize++;
        }
      }
      if (relativeStackSize > maxRelativeStackSize) {
        maxRelativeStackSize = relativeStackSize;
      }
    }

    maxStack = Math.max(maxStack, numStack);
    maxLocals = Math.max(maxLocals, currentLocals);
  }

  @Override
  public void visitInsn(final int opcode) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    code.putByte(opcode);
    // 如果需要，更新最大栈深、局部变量数量以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, 0, null, null);
      } else {
        int size = relativeStackSize + STACK_SIZE_DELTA[opcode];
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
      if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
        endCurrentBasicBlockWithNoSuccessor();
      }
    }
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    if (opcode == Opcodes.SIPUSH) {
      code.put12(opcode, operand);
    } else { // BIPUSH 或 NEWARRAY
      code.put11(opcode, operand);
    }
    // 如果需要，更新最大栈深、局部变量数量以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, operand, null, null);
      } else if (opcode != Opcodes.NEWARRAY) {
        // 对于 BIPUSH 或 SIPUSH，栈大小变化为 +1；对于 NEWARRAY，变化为 0。
        int size = relativeStackSize + 1;
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitVarInsn(final int opcode, final int varIndex) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    if (varIndex < 4 && opcode != Opcodes.RET) {
      int optimizedOpcode;
      if (opcode < Opcodes.ISTORE) {
        optimizedOpcode = Constants.ILOAD_0 + ((opcode - Opcodes.ILOAD) << 2) + varIndex;
      } else {
        optimizedOpcode = Constants.ISTORE_0 + ((opcode - Opcodes.ISTORE) << 2) + varIndex;
      }
      code.putByte(optimizedOpcode);
    } else if (varIndex >= 256) {
      code.putByte(Constants.WIDE).put12(opcode, varIndex);
    } else {
      code.put11(opcode, varIndex);
    }
    // 如果需要，更新最大栈深度、本地变量数以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, varIndex, null, null);
      } else {
        if (opcode == Opcodes.RET) {
          // RET 指令不会改变栈深度。
          currentBasicBlock.flags |= Label.FLAG_SUBROUTINE_END;
          currentBasicBlock.outputStackSize = (short) relativeStackSize;
          endCurrentBasicBlockWithNoSuccessor();
        } else { // xLOAD 或 xSTORE
          int size = relativeStackSize + STACK_SIZE_DELTA[opcode];
          if (size > maxRelativeStackSize) {
            maxRelativeStackSize = size;
          }
          relativeStackSize = size;
        }
      }
    }
    if (compute != COMPUTE_NOTHING) {
      int currentMaxLocals;
      if (opcode == Opcodes.LLOAD
          || opcode == Opcodes.DLOAD
          || opcode == Opcodes.LSTORE
          || opcode == Opcodes.DSTORE) {
        currentMaxLocals = varIndex + 2;
      } else {
        currentMaxLocals = varIndex + 1;
      }
      if (currentMaxLocals > maxLocals) {
        maxLocals = currentMaxLocals;
      }
    }
    if (opcode >= Opcodes.ISTORE && compute == COMPUTE_ALL_FRAMES && firstHandler != null) {
      // 如果存在异常处理器块，那么处理器范围内的每一条指令在理论上都是一个基本块
      // （因为执行可能从该指令跳转到异常处理器）。
      // 因此，处理器块开始处的本地变量类型应该是处理器范围内所有指令的本地变量类型的合并结果。
      // 但是，我们可以通过一种更高效的方式实现这个目的，而不必为每条指令都创建一个基本块：
      // 在每条 xSTORE 指令之后开始一个新的基本块，这就是这里的实现。
      visitLabel(new Label());
    }
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    Symbol typeSymbol = symbolTable.addConstantClass(type);
    code.put12(opcode, typeSymbol.index);
    // 如果需要，更新最大栈深度、本地变量数以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, lastBytecodeOffset, typeSymbol, symbolTable);
      } else if (opcode == Opcodes.NEW) {
        // 对于 NEW 指令，栈深度变化为 +1；对于 ANEWARRAY、CHECKCAST、INSTANCEOF 栈深度变化为 0。
        int size = relativeStackSize + 1;
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    Symbol fieldrefSymbol = symbolTable.addConstantFieldref(owner, name, descriptor);
    code.put12(opcode, fieldrefSymbol.index);
    // 如果需要，更新最大栈深度、本地变量数以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, 0, fieldrefSymbol, symbolTable);
      } else {
        int size;
        char firstDescChar = descriptor.charAt(0);
        switch (opcode) {
          case Opcodes.GETSTATIC:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? 2 : 1);
            break;
          case Opcodes.PUTSTATIC:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? -2 : -1);
            break;
          case Opcodes.GETFIELD:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? 1 : 0);
            break;
          case Opcodes.PUTFIELD:
          default:
            size = relativeStackSize + (firstDescChar == 'D' || firstDescChar == 'J' ? -3 : -2);
            break;
        }
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitMethodInsn(
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    Symbol methodrefSymbol = symbolTable.addConstantMethodref(owner, name, descriptor, isInterface);
    if (opcode == Opcodes.INVOKEINTERFACE) {
      code.put12(Opcodes.INVOKEINTERFACE, methodrefSymbol.index)
          .put11(methodrefSymbol.getArgumentsAndReturnSizes() >> 2, 0);
    } else {
      code.put12(opcode, methodrefSymbol.index);
    }
    // 如果需要，更新最大栈深度、本地变量数，以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(opcode, 0, methodrefSymbol, symbolTable);
      } else {
        int argumentsAndReturnSize = methodrefSymbol.getArgumentsAndReturnSizes();
        int stackSizeDelta = (argumentsAndReturnSize & 3) - (argumentsAndReturnSize >> 2);
        int size;
        if (opcode == Opcodes.INVOKESTATIC) {
          size = relativeStackSize + stackSizeDelta + 1;
        } else {
          size = relativeStackSize + stackSizeDelta;
        }
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitInvokeDynamicInsn(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    Symbol invokeDynamicSymbol =
        symbolTable.addConstantInvokeDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    code.put12(Opcodes.INVOKEDYNAMIC, invokeDynamicSymbol.index);
    code.putShort(0);
    // 如果需要，更新最大栈深度、本地变量数，以及栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(Opcodes.INVOKEDYNAMIC, 0, invokeDynamicSymbol, symbolTable);
      } else {
        int argumentsAndReturnSize = invokeDynamicSymbol.getArgumentsAndReturnSizes();
        int stackSizeDelta = (argumentsAndReturnSize & 3) - (argumentsAndReturnSize >> 2) + 1;
        int size = relativeStackSize + stackSizeDelta;
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    lastBytecodeOffset = code.length;
    // 将该指令添加到方法的字节码中。
    // 计算“基础”操作码，如果 opcode 是 GOTO_W 或 JSR_W 则转为 GOTO 或 JSR，否则保持不变。
    int baseOpcode =
        opcode >= Constants.GOTO_W ? opcode - Constants.WIDE_JUMP_OPCODE_DELTA : opcode;
    boolean nextInsnIsJumpTarget = false;
    if ((label.flags & Label.FLAG_RESOLVED) != 0
        && label.bytecodeOffset - code.length < Short.MIN_VALUE) {
      // 向后跳转且偏移量小于 -32768 的情况。
      // 此时自动将 GOTO 替换为 GOTO_W，JSR 替换为 JSR_W，
      // IFxxx <l> 替换为 IFNOTxxx <L> GOTO_W <l> L:...
      // 其中 IFNOTxxx 是 IFxxx 的“相反”条件码，例如 IFEQ 的相反是 IFNE，
      // <L> 表示 GOTO_W 后的那条指令。
      if (baseOpcode == Opcodes.GOTO) {
        code.putByte(Constants.GOTO_W);
      } else if (baseOpcode == Opcodes.JSR) {
        code.putByte(Constants.JSR_W);
      } else {
        // 写入 baseOpcode 的“相反”条件码。
        // 对于 IFNULL 和 IFNONNULL，通过翻转最低有效位得到；
        // 对于 IFEQ 等条件码，需要加偏移再翻转。
        // 跳转偏移固定为 8（IFNOTxxx 占 3 字节，GOTO_W 占 5 字节）。
        code.putByte(baseOpcode >= Opcodes.IFNULL ? baseOpcode ^ 1 : ((baseOpcode + 1) ^ 1) - 1);
        code.putShort(8);
        // 理论上这里可以直接写入 GOTO_W，但考虑到 ASM 特殊指令和帧计算的问题，
        // 为了不漏掉可能需要插入的帧，这里使用 ASM_GOTO_W，会强制进行一次额外的
        // ClassReader -> ClassWriter 循环。
        code.putByte(Constants.ASM_GOTO_W);
        hasAsmInstructions = true;
        // GOTO_W 后的那条指令会成为 IFNOT 指令的跳转目标。
        nextInsnIsJumpTarget = true;
      }
      label.put(code, code.length - 1, true);
    } else if (baseOpcode != opcode) {
      // 用户显式指定了 GOTO_W 或 JSR_W（通常由 ClassReader 生成以移除 ASM 特殊指令），
      // 在这种情况下保持原指令不变。
      code.putByte(opcode);
      label.put(code, code.length - 1, true);
    } else {
      // 跳转偏移量 >= -32768，或偏移量未知的情况。
      // 此时用 2 字节存储（如果不够，将在 ClassReader -> ClassWriter 过程中扩展）。
      code.putByte(baseOpcode);
      label.put(code, code.length - 1, false);
    }

    // 如果需要，更新最大栈深度、本地变量数，以及栈映射帧。
    if (currentBasicBlock != null) {
      Label nextBasicBlock = null;
      if (compute == COMPUTE_ALL_FRAMES) {
        currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
        // 记录 label 是跳转指令的目标。
        label.getCanonicalInstance().flags |= Label.FLAG_JUMP_TARGET;
        // 将 label 添加为当前基本块的后继。
        addSuccessorToCurrentBasicBlock(Edge.JUMP, label);
        if (baseOpcode != Opcodes.GOTO) {
          // 下一条指令开始一个新的基本块（GOTO 除外，默认 GOTO 之后的代码不可达，
          // 除非显式有标签指向它）。
          nextBasicBlock = new Label();
        }
      } else if (compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(baseOpcode, 0, null, null);
      } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) {
        // 无需更新 maxRelativeStackSize（栈深变化始终为负）。
        relativeStackSize += STACK_SIZE_DELTA[baseOpcode];
      } else {
        if (baseOpcode == Opcodes.JSR) {
          // 记录 label 表示一个子程序的起始点（如果还未标记）。
          if ((label.flags & Label.FLAG_SUBROUTINE_START) == 0) {
            label.flags |= Label.FLAG_SUBROUTINE_START;
            hasSubroutines = true;
          }
          currentBasicBlock.flags |= Label.FLAG_SUBROUTINE_CALLER;
          // 按构造规则，调用子程序的块至少有两个后继：
          // 第一个指向 JSR 之后的指令（虚拟后继，不是实际执行路径）；
          // 第二个指向子程序的起点 label。
          // 第一个后继用于计算以 ret 结束的基本块的后继。
          addSuccessorToCurrentBasicBlock(relativeStackSize + 1, label);
          // JSR 之后的指令开始一个新的基本块。
          nextBasicBlock = new Label();
        } else {
          // 无需更新 maxRelativeStackSize（栈深变化始终为负）。
          relativeStackSize += STACK_SIZE_DELTA[baseOpcode];
          addSuccessorToCurrentBasicBlock(relativeStackSize, label);
        }
      }
      // 如果下一条指令开始新基本块，调用 visitLabel，
      // 将该标签添加为当前块的后继并启动新基本块。
      if (nextBasicBlock != null) {
        if (nextInsnIsJumpTarget) {
          nextBasicBlock.flags |= Label.FLAG_JUMP_TARGET;
        }
        visitLabel(nextBasicBlock);
      }
      if (baseOpcode == Opcodes.GOTO) {
        endCurrentBasicBlockWithNoSuccessor();
      }
    }
  }

  @Override
  public void visitLabel(final Label label) {
    // 解析对该标签的前向引用（如果有）。
    hasAsmInstructions |= label.resolve(code.data, code.length);
    // visitLabel 会开始一个新的基本块（除非是仅调试用的标签），
    // 因此需要更新前一个和当前基本块引用及后继列表。
    if ((label.flags & Label.FLAG_DEBUG_ONLY) != 0) {
      return;
    }
    if (compute == COMPUTE_ALL_FRAMES) {
      if (currentBasicBlock != null) {
        if (label.bytecodeOffset == currentBasicBlock.bytecodeOffset) {
          // 使用 {@link Label#getCanonicalInstance} 使基本块状态只存储一处，
          // 但尚未访问的标签不适用。
          // 因此当检测到两个标签有相同偏移时，需要：
          // - 合并两实例的状态到 canonical 实例：
          currentBasicBlock.flags |= (label.flags & Label.FLAG_JUMP_TARGET);
          // - 确保两实例共享同一个 Frame（这里 label.frame 应为 null）：
          label.frame = currentBasicBlock.frame;
          // - 并确保不将 label 赋给 currentBasicBlock 或 lastBasicBlock，
          // 以保持对该偏移 canonical 实例的引用。
          return;
        }
        // 结束当前基本块（有一个新后继）。
        addSuccessorToCurrentBasicBlock(Edge.JUMP, label);
      }
      // 将 label 追加到基本块链表末尾。
      if (lastBasicBlock != null) {
        if (label.bytecodeOffset == lastBasicBlock.bytecodeOffset) {
          // 同上注释。
          lastBasicBlock.flags |= (label.flags & Label.FLAG_JUMP_TARGET);
          // 此处 label.frame 应为 null。
          label.frame = lastBasicBlock.frame;
          currentBasicBlock = lastBasicBlock;
          return;
        }
        lastBasicBlock.nextBasicBlock = label;
      }
      lastBasicBlock = label;
      // 设为新的当前基本块。
      currentBasicBlock = label;
      // 此处 label.frame 应为 null。
      label.frame = new Frame(label);
    } else if (compute == COMPUTE_INSERTED_FRAMES) {
      if (currentBasicBlock == null) {
        // 该情况应只出现一次，即构造函数中 visitLabel 调用时。
        currentBasicBlock = label;
      } else {
        // 更新帧的拥有者，以便 Frame.accept() 中计算正确的帧偏移。
        currentBasicBlock.frame.owner = label;
      }
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
      if (currentBasicBlock != null) {
        // 结束当前基本块（有一个新后继）。
        currentBasicBlock.outputStackMax = (short) maxRelativeStackSize;
        addSuccessorToCurrentBasicBlock(relativeStackSize, label);
      }
      // 开始新的当前基本块，并重置当前及最大相对栈大小。
      currentBasicBlock = label;
      relativeStackSize = 0;
      maxRelativeStackSize = 0;
      // 追加新基本块到基本块链表末尾。
      if (lastBasicBlock != null) {
        lastBasicBlock.nextBasicBlock = label;
      }
      lastBasicBlock = label;
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES && currentBasicBlock == null) {
      // 该情况应只出现一次，即构造函数中 visitLabel 调用时。
      currentBasicBlock = label;
    }
  }

  @Override
  public void visitLdcInsn(final Object value) {
    lastBytecodeOffset = code.length;
    // 将指令添加到方法字节码。
    Symbol constantSymbol = symbolTable.addConstant(value);
    int constantIndex = constantSymbol.index;
    char firstDescriptorChar;
    boolean isLongOrDouble =
        constantSymbol.tag == Symbol.CONSTANT_LONG_TAG
            || constantSymbol.tag == Symbol.CONSTANT_DOUBLE_TAG
            || (constantSymbol.tag == Symbol.CONSTANT_DYNAMIC_TAG
                && ((firstDescriptorChar = constantSymbol.value.charAt(0)) == 'J'
                    || firstDescriptorChar == 'D'));
    if (isLongOrDouble) {
      code.put12(Constants.LDC2_W, constantIndex);
    } else if (constantIndex >= 256) {
      code.put12(Constants.LDC_W, constantIndex);
    } else {
      code.put11(Opcodes.LDC, constantIndex);
    }
    // 如有必要，更新最大栈大小、本地变量数量和栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(Opcodes.LDC, 0, constantSymbol, symbolTable);
      } else {
        int size = relativeStackSize + (isLongOrDouble ? 2 : 1);
        if (size > maxRelativeStackSize) {
          maxRelativeStackSize = size;
        }
        relativeStackSize = size;
      }
    }
  }

  @Override
  public void visitIincInsn(final int varIndex, final int increment) {
    lastBytecodeOffset = code.length;
    // 将指令添加到方法字节码。
    if ((varIndex > 255) || (increment > 127) || (increment < -128)) {
      code.putByte(Constants.WIDE).put12(Opcodes.IINC, varIndex).putShort(increment);
    } else {
      code.putByte(Opcodes.IINC).put11(varIndex, increment);
    }
    // 如有必要，更新最大栈大小、本地变量数量和栈映射帧。
    if (currentBasicBlock != null
        && (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES)) {
      currentBasicBlock.frame.execute(Opcodes.IINC, varIndex, null, null);
    }
    if (compute != COMPUTE_NOTHING) {
      int currentMaxLocals = varIndex + 1;
      if (currentMaxLocals > maxLocals) {
        maxLocals = currentMaxLocals;
      }
    }
  }

  @Override
  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label... labels) {
    lastBytecodeOffset = code.length;
    // 将指令添加到方法的字节码中。
    code.putByte(Opcodes.TABLESWITCH).putByteArray(null, 0, (4 - code.length % 4) % 4);
    dflt.put(code, lastBytecodeOffset, true);
    code.putInt(min).putInt(max);
    for (Label label : labels) {
      label.put(code, lastBytecodeOffset, true);
    }
    // 如有必要，更新最大栈大小、本地变量数量和栈映射帧。
    visitSwitchInsn(dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    lastBytecodeOffset = code.length;
    // 将指令添加到方法的字节码中。
    code.putByte(Opcodes.LOOKUPSWITCH).putByteArray(null, 0, (4 - code.length % 4) % 4);
    dflt.put(code, lastBytecodeOffset, true);
    code.putInt(labels.length);
    for (int i = 0; i < labels.length; ++i) {
      code.putInt(keys[i]);
      labels[i].put(code, lastBytecodeOffset, true);
    }
    // 如有必要，更新最大栈大小、本地变量数量和栈映射帧。
    visitSwitchInsn(dflt, labels);
  }

  private void visitSwitchInsn(final Label dflt, final Label[] labels) {
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES) {
        currentBasicBlock.frame.execute(Opcodes.LOOKUPSWITCH, 0, null, null);
        // 将所有标签作为当前基本块的后继节点添加。
        addSuccessorToCurrentBasicBlock(Edge.JUMP, dflt);
        dflt.getCanonicalInstance().flags |= Label.FLAG_JUMP_TARGET;
        for (Label label : labels) {
          addSuccessorToCurrentBasicBlock(Edge.JUMP, label);
          label.getCanonicalInstance().flags |= Label.FLAG_JUMP_TARGET;
        }
      } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
        // 无需更新 maxRelativeStackSize（栈大小变化总是负数）。
        --relativeStackSize;
        // 将所有标签作为当前基本块的后继节点添加。
        addSuccessorToCurrentBasicBlock(relativeStackSize, dflt);
        for (Label label : labels) {
          addSuccessorToCurrentBasicBlock(relativeStackSize, label);
        }
      }
      // 结束当前基本块，且无后继。
      endCurrentBasicBlockWithNoSuccessor();
    }
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    lastBytecodeOffset = code.length;
    // 将指令添加到方法的字节码中。
    Symbol descSymbol = symbolTable.addConstantClass(descriptor);
    code.put12(Opcodes.MULTIANEWARRAY, descSymbol.index).putByte(numDimensions);
    // 如有必要，更新最大栈大小、本地变量数量和栈映射帧。
    if (currentBasicBlock != null) {
      if (compute == COMPUTE_ALL_FRAMES || compute == COMPUTE_INSERTED_FRAMES) {
        currentBasicBlock.frame.execute(
            Opcodes.MULTIANEWARRAY, numDimensions, descSymbol, symbolTable);
      } else {
        // 无需更新 maxRelativeStackSize（栈大小变化总是负数）。
        relativeStackSize += 1 - numDimensions;
      }
    }
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastCodeRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable,
              (typeRef & 0xFF0000FF) | (lastBytecodeOffset << 8),
              typePath,
              descriptor,
              lastCodeRuntimeVisibleTypeAnnotation);
    } else {
      return lastCodeRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable,
              (typeRef & 0xFF0000FF) | (lastBytecodeOffset << 8),
              typePath,
              descriptor,
              lastCodeRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    Handler newHandler =
        new Handler(
            start, end, handler, type != null ? symbolTable.addConstantClass(type).index : 0, type);
    if (firstHandler == null) {
      firstHandler = newHandler;
    } else {
      lastHandler.nextHandler = newHandler;
    }
    lastHandler = newHandler;
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastCodeRuntimeVisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastCodeRuntimeVisibleTypeAnnotation);
    } else {
      return lastCodeRuntimeInvisibleTypeAnnotation =
          AnnotationWriter.create(
              symbolTable, typeRef, typePath, descriptor, lastCodeRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitLocalVariable(
      final String name,
      final String descriptor,
      final String signature,
      final Label start,
      final Label end,
      final int index) {
    if (signature != null) {
      if (localVariableTypeTable == null) {
        localVariableTypeTable = new ByteVector();
      }
      ++localVariableTypeTableLength;
      localVariableTypeTable
          .putShort(start.bytecodeOffset)
          .putShort(end.bytecodeOffset - start.bytecodeOffset)
          .putShort(symbolTable.addConstantUtf8(name))
          .putShort(symbolTable.addConstantUtf8(signature))
          .putShort(index);
    }
    if (localVariableTable == null) {
      localVariableTable = new ByteVector();
    }
    ++localVariableTableLength;
    localVariableTable
        .putShort(start.bytecodeOffset)
        .putShort(end.bytecodeOffset - start.bytecodeOffset)
        .putShort(symbolTable.addConstantUtf8(name))
        .putShort(symbolTable.addConstantUtf8(descriptor))
        .putShort(index);
    if (compute != COMPUTE_NOTHING) {
      char firstDescChar = descriptor.charAt(0);
      int currentMaxLocals = index + (firstDescChar == 'J' || firstDescChar == 'D' ? 2 : 1);
      if (currentMaxLocals > maxLocals) {
        maxLocals = currentMaxLocals;
      }
    }
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(
      final int typeRef,
      final TypePath typePath,
      final Label[] start,
      final Label[] end,
      final int[] index,
      final String descriptor,
      final boolean visible) {
    // 创建一个 ByteVector 用于存储 'type_annotation' JVMS 结构。
    // 参考 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20。
    ByteVector typeAnnotation = new ByteVector();
    // 写入 target_type、target_info 和 target_path。
    typeAnnotation.putByte(typeRef >>> 24).putShort(start.length);
    for (int i = 0; i < start.length; ++i) {
      typeAnnotation
          .putShort(start[i].bytecodeOffset)
          .putShort(end[i].bytecodeOffset - start[i].bytecodeOffset)
          .putShort(index[i]);
    }
    TypePath.put(typePath, typeAnnotation);
    // 写入 type_index 并为 num_element_value_pairs 预留空间。
    typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    if (visible) {
      return lastCodeRuntimeVisibleTypeAnnotation =
          new AnnotationWriter(
              symbolTable,
              /* useNamedValues = */ true,
              typeAnnotation,
              lastCodeRuntimeVisibleTypeAnnotation);
    } else {
      return lastCodeRuntimeInvisibleTypeAnnotation =
          new AnnotationWriter(
              symbolTable,
              /* useNamedValues = */ true,
              typeAnnotation,
              lastCodeRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    if (lineNumberTable == null) {
      lineNumberTable = new ByteVector();
    }
    ++lineNumberTableLength;
    lineNumberTable.putShort(start.bytecodeOffset);
    lineNumberTable.putShort(line);
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    if (compute == COMPUTE_ALL_FRAMES) {
      computeAllFrames();
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
      computeMaxStackAndLocal();
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES) {
      this.maxStack = maxRelativeStackSize;
    } else {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
  }

  /**
   * 从头开始计算方法的所有栈映射帧。
   */
  private void computeAllFrames() {
    // 用异常处理器块完善控制流图。
    Handler handler = firstHandler;
    while (handler != null) {
      String catchTypeDescriptor =
          handler.catchTypeDescriptor == null ? "java/lang/Throwable" : handler.catchTypeDescriptor;
      int catchType = Frame.getAbstractTypeFromInternalName(symbolTable, catchTypeDescriptor);
      // 标记 handlerBlock 作为异常处理器。
      Label handlerBlock = handler.handlerPc.getCanonicalInstance();
      handlerBlock.flags |= Label.FLAG_JUMP_TARGET;
      // 将 handlerBlock 作为异常处理范围内所有基本块的后继节点添加。
      Label handlerRangeBlock = handler.startPc.getCanonicalInstance();
      Label handlerRangeEnd = handler.endPc.getCanonicalInstance();
      while (handlerRangeBlock != handlerRangeEnd) {
        handlerRangeBlock.outgoingEdges =
            new Edge(catchType, handlerBlock, handlerRangeBlock.outgoingEdges);
        handlerRangeBlock = handlerRangeBlock.nextBasicBlock;
      }
      handler = handler.nextHandler;
    }

    // 创建并访问第一个（隐式）栈帧。
    Frame firstFrame = firstBasicBlock.frame;
    firstFrame.setInputFrameFromDescriptor(symbolTable, accessFlags, descriptor, this.maxLocals);
    firstFrame.accept(this);

    // 不动点算法：将第一个基本块加入待处理列表（即栈映射帧发生变化的块），
    // 当列表非空时，移除一个块，更新其后继块的栈映射帧（可能改变它们，若改变则需处理并加入列表）。
    // 同时计算方法的最大栈大小，作为副产品。
    Label listOfBlocksToProcess = firstBasicBlock;
    listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
    int maxStackSize = 0;
    while (listOfBlocksToProcess != Label.EMPTY_LIST) {
      // 从待处理列表中移除一个基本块。
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
      basicBlock.nextListElement = null;
      // 根据定义，basicBlock 是可达的。
      basicBlock.flags |= Label.FLAG_REACHABLE;
      // 更新（绝对）最大栈大小。
      int maxBlockStackSize = basicBlock.frame.getInputStackSize() + basicBlock.outputStackMax;
      if (maxBlockStackSize > maxStackSize) {
        maxStackSize = maxBlockStackSize;
      }
      // 更新控制流图中 basicBlock 的后继块。
      Edge outgoingEdge = basicBlock.outgoingEdges;
      while (outgoingEdge != null) {
        Label successorBlock = outgoingEdge.successor.getCanonicalInstance();
        boolean successorBlockChanged =
            basicBlock.frame.merge(symbolTable, successorBlock.frame, outgoingEdge.info);
        if (successorBlockChanged && successorBlock.nextListElement == null) {
          // 如果后继块发生变化，需处理它。若它未在待处理列表中，则加入该列表。
          successorBlock.nextListElement = listOfBlocksToProcess;
          listOfBlocksToProcess = successorBlock;
        }
        outgoingEdge = outgoingEdge.nextEdge;
      }
    }

    // 遍历所有基本块，访问必须存储在 StackMapTable 属性中的栈映射帧。
    // 还将不可达代码替换为 NOP* ATHROW，并将其从异常处理范围移除。
    Label basicBlock = firstBasicBlock;
    while (basicBlock != null) {
      if ((basicBlock.flags & (Label.FLAG_JUMP_TARGET | Label.FLAG_REACHABLE))
          == (Label.FLAG_JUMP_TARGET | Label.FLAG_REACHABLE)) {
        basicBlock.frame.accept(this);
      }
      if ((basicBlock.flags & Label.FLAG_REACHABLE) == 0) {
        // 找到该不可达块的起始和结束字节码偏移。
        Label nextBasicBlock = basicBlock.nextBasicBlock;
        int startOffset = basicBlock.bytecodeOffset;
        int endOffset = (nextBasicBlock == null ? code.length : nextBasicBlock.bytecodeOffset) - 1;
        if (endOffset >= startOffset) {
          // 将其指令替换为 NOP ... NOP ATHROW。
          for (int i = startOffset; i < endOffset; ++i) {
            code.data[i] = Opcodes.NOP;
          }
          code.data[endOffset] = (byte) Opcodes.ATHROW;
          // 为该不可达块发出一个栈帧，局部变量为空，栈上有一个 Throwable
          // （这样如果可达，ATHROW 会消费这个 Throwable）。
          int frameIndex = visitFrameStart(startOffset, /* numLocal = */ 0, /* numStack = */ 1);
          currentFrame[frameIndex] =
              Frame.getAbstractTypeFromInternalName(symbolTable, "java/lang/Throwable");
          visitFrameEnd();
          // 将该不可达基本块从异常处理范围移除。
          firstHandler = Handler.removeRange(firstHandler, basicBlock, nextBasicBlock);
          // 最大栈大小现在至少为 1，因为上面声明了 Throwable。
          maxStackSize = Math.max(maxStackSize, 1);
        }
      }
      basicBlock = basicBlock.nextBasicBlock;
    }

    this.maxStack = maxStackSize;
  }

  /** 计算方法的最大栈大小。 */
  private void computeMaxStackAndLocal() {
    // 用异常处理器块完善控制流图。
    Handler handler = firstHandler;
    while (handler != null) {
      Label handlerBlock = handler.handlerPc;
      Label handlerRangeBlock = handler.startPc;
      Label handlerRangeEnd = handler.endPc;
      // 将 handlerBlock 作为异常处理范围内所有基本块的后继节点添加。
      while (handlerRangeBlock != handlerRangeEnd) {
        if ((handlerRangeBlock.flags & Label.FLAG_SUBROUTINE_CALLER) == 0) {
          handlerRangeBlock.outgoingEdges =
              new Edge(Edge.EXCEPTION, handlerBlock, handlerRangeBlock.outgoingEdges);
        } else {
          // 如果 handlerRangeBlock 是 JSR 块，则在前两个出边之后插入 handlerBlock，
          // 以保持关于 JSR 块后继顺序的假设（见 {@link #visitJumpInsn}）。
          handlerRangeBlock.outgoingEdges.nextEdge.nextEdge =
              new Edge(
                  Edge.EXCEPTION, handlerBlock, handlerRangeBlock.outgoingEdges.nextEdge.nextEdge);
        }
        handlerRangeBlock = handlerRangeBlock.nextBasicBlock;
      }
      handler = handler.nextHandler;
    }

    // 如果需要，补全子程序的后继块到控制流图中。
    if (hasSubroutines) {
      // 第一步：查找子程序。此步骤确定每个基本块属于哪个子程序。
      // 从主“子程序”开始：
      short numSubroutines = 1;
      firstBasicBlock.markSubroutine(numSubroutines);
      // 然后标记主子程序调用的子程序，以及被它们调用的子程序，依此类推。
      for (short currentSubroutine = 1; currentSubroutine <= numSubroutines; ++currentSubroutine) {
        Label basicBlock = firstBasicBlock;
        while (basicBlock != null) {
          if ((basicBlock.flags & Label.FLAG_SUBROUTINE_CALLER) != 0
              && basicBlock.subroutineId == currentSubroutine) {
            Label jsrTarget = basicBlock.outgoingEdges.nextEdge.successor;
            if (jsrTarget.subroutineId == 0) {
              // 如果该子程序尚未标记，则标记其基本块。
              jsrTarget.markSubroutine(++numSubroutines);
            }
          }
          basicBlock = basicBlock.nextBasicBlock;
        }
      }
      // 第二步：查找以 RET 指令结束的子程序基本块 'r' 在控制流图中的后继。
      // 这些后继是以 JSR 指令结束的基本块的虚拟后继（见 {@link #visitJumpInsn}），这些基本块可以到达 'r'。
      Label basicBlock = firstBasicBlock;
      while (basicBlock != null) {
        if ((basicBlock.flags & Label.FLAG_SUBROUTINE_CALLER) != 0) {
          // 按结构，JSR 目标存储在以 JSR 指令结尾的基本块第二条出边中（见 {@link #FLAG_SUBROUTINE_CALLER}）。
          Label subroutine = basicBlock.outgoingEdges.nextEdge.successor;
          subroutine.addSubroutineRetSuccessors(basicBlock);
        }
        basicBlock = basicBlock.nextBasicBlock;
      }
    }

    // 数据流算法：将第一个基本块放入待处理列表（即输入栈大小已改变的块），
    // 当列表非空时，移除一个基本块，更新其后继块的输入栈大小，并将后继块加入待处理列表（若未加入）。
    Label listOfBlocksToProcess = firstBasicBlock;
    listOfBlocksToProcess.nextListElement = Label.EMPTY_LIST;
    int maxStackSize = maxStack;
    while (listOfBlocksToProcess != Label.EMPTY_LIST) {
      // 从待处理列表中移除一个基本块。注意这里没有将 basicBlock.nextListElement 置 null，
      // 以确保不重复处理已处理的基本块。
      Label basicBlock = listOfBlocksToProcess;
      listOfBlocksToProcess = listOfBlocksToProcess.nextListElement;
      // 计算该块的输入栈大小和最大栈大小。
      int inputStackTop = basicBlock.inputStackSize;
      int maxBlockStackSize = inputStackTop + basicBlock.outputStackMax;
      // 更新方法的绝对最大栈大小。
      if (maxBlockStackSize > maxStackSize) {
        maxStackSize = maxBlockStackSize;
      }
      // 更新控制流图中 basicBlock 后继块的输入栈大小，
      // 并将这些后继块加入待处理列表（如果未加入）。
      Edge outgoingEdge = basicBlock.outgoingEdges;
      if ((basicBlock.flags & Label.FLAG_SUBROUTINE_CALLER) != 0) {
        // 忽略以 jsr 结尾基本块的第一条出边：这些是虚拟边，指向 jsr 之后的指令，
        // 不对应可能的执行路径（见 {@link #visitJumpInsn} 和 {@link Label#FLAG_SUBROUTINE_CALLER}）。
        outgoingEdge = outgoingEdge.nextEdge;
      }
      while (outgoingEdge != null) {
        Label successorBlock = outgoingEdge.successor;
        if (successorBlock.nextListElement == null) {
          successorBlock.inputStackSize =
              (short) (outgoingEdge.info == Edge.EXCEPTION ? 1 : inputStackTop + outgoingEdge.info);
          successorBlock.nextListElement = listOfBlocksToProcess;
          listOfBlocksToProcess = successorBlock;
        }
        outgoingEdge = outgoingEdge.nextEdge;
      }
    }
    this.maxStack = maxStackSize;
  }

  @Override
  public void visitEnd() {
    // 无操作
  }

  // -----------------------------------------------------------------------------------------------
  // 工具方法：控制流分析算法
  // -----------------------------------------------------------------------------------------------

  /**
   * 向 {@link #currentBasicBlock} 添加一个后继节点（控制流边）。
   *
   * @param info 要添加的控制流边信息。
   * @param successor 要添加的后继基本块。
   */
  private void addSuccessorToCurrentBasicBlock(final int info, final Label successor) {
    currentBasicBlock.outgoingEdges = new Edge(info, successor, currentBasicBlock.outgoingEdges);
  }

  /**
   * 结束当前基本块。用于当前基本块无后继时调用。
   *
   * <p>注意：此方法必须在当前访问指令已写入 {@link #code} 后调用（如果计算栈帧，会插入新 Label 以开始新基本块）。
   */
  private void endCurrentBasicBlockWithNoSuccessor() {
    if (compute == COMPUTE_ALL_FRAMES) {
      Label nextBasicBlock = new Label();
      nextBasicBlock.frame = new Frame(nextBasicBlock);
      nextBasicBlock.resolve(code.data, code.length);
      lastBasicBlock.nextBasicBlock = nextBasicBlock;
      lastBasicBlock = nextBasicBlock;
      currentBasicBlock = null;
    } else if (compute == COMPUTE_MAX_STACK_AND_LOCAL) {
      currentBasicBlock.outputStackMax = (short) maxRelativeStackSize;
      currentBasicBlock = null;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 工具方法：栈映射帧（stack map frames）
  // -----------------------------------------------------------------------------------------------

  /**
   * 开始访问一个新的栈映射帧，保存在 {@link #currentFrame} 中。
   *
   * @param offset 与该帧对应的指令的字节码偏移量。
   * @param numLocal 帧中局部变量的数量。
   * @param numStack 帧中栈元素的数量。
   * @return 此帧中下一个待写元素的索引。
   */
  int visitFrameStart(final int offset, final int numLocal, final int numStack) {
    int frameLength = 3 + numLocal + numStack;
    if (currentFrame == null || currentFrame.length < frameLength) {
      currentFrame = new int[frameLength];
    }
    currentFrame[0] = offset;
    currentFrame[1] = numLocal;
    currentFrame[2] = numStack;
    return 3;
  }

  /**
   * 设置 {@link #currentFrame} 中的抽象类型。
   *
   * @param frameIndex {@link #currentFrame} 中待设置元素的索引。
   * @param abstractType 一个抽象类型。
   */
  void visitAbstractType(final int frameIndex, final int abstractType) {
    currentFrame[frameIndex] = abstractType;
  }

  /**
   * 结束对 {@link #currentFrame} 的访问，将其写入 StackMapTable 条目，并更新 StackMapTable 的条目数量
   * （除非当前帧是第一个帧，StackMapTable 中是隐式表示的）。
   * 然后将 {@link #currentFrame} 重置为 {@literal null}。
   */
  void visitFrameEnd() {
    if (previousFrame != null) {
      if (stackMapTableEntries == null) {
        stackMapTableEntries = new ByteVector();
      }
      putFrame();
      ++stackMapTableNumberOfEntries;
    }
    previousFrame = currentFrame;
    currentFrame = null;
  }

  /** 将 {@link #currentFrame} 压缩并写入新的 StackMapTable 条目。 */
  private void putFrame() {
    final int numLocal = currentFrame[1];
    final int numStack = currentFrame[2];
    if (symbolTable.getMajorVersion() < Opcodes.V1_6) {
      // 生成 StackMap 属性条目，始终不压缩。
      stackMapTableEntries.putShort(currentFrame[0]).putShort(numLocal);
      putAbstractTypes(3, 3 + numLocal);
      stackMapTableEntries.putShort(numStack);
      putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
      return;
    }
    final int offsetDelta =
        stackMapTableNumberOfEntries == 0
            ? currentFrame[0]
            : currentFrame[0] - previousFrame[0] - 1;
    final int previousNumlocal = previousFrame[1];
    final int numLocalDelta = numLocal - previousNumlocal;
    int type = Frame.FULL_FRAME;
    if (numStack == 0) {
      switch (numLocalDelta) {
        case -3:
        case -2:
        case -1:
          type = Frame.CHOP_FRAME;
          break;
        case 0:
          type = offsetDelta < 64 ? Frame.SAME_FRAME : Frame.SAME_FRAME_EXTENDED;
          break;
        case 1:
        case 2:
        case 3:
          type = Frame.APPEND_FRAME;
          break;
        default:
          // 保持 FULL_FRAME 类型。
          break;
      }
    } else if (numLocalDelta == 0 && numStack == 1) {
      type =
          offsetDelta < 63
              ? Frame.SAME_LOCALS_1_STACK_ITEM_FRAME
              : Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
    }
    if (type != Frame.FULL_FRAME) {
      // 验证局部变量是否与前一帧相同。
      int frameIndex = 3;
      for (int i = 0; i < previousNumlocal && i < numLocal; i++) {
        if (currentFrame[frameIndex] != previousFrame[frameIndex]) {
          type = Frame.FULL_FRAME;
          break;
        }
        frameIndex++;
      }
    }
    switch (type) {
      case Frame.SAME_FRAME:
        stackMapTableEntries.putByte(offsetDelta);
        break;
      case Frame.SAME_LOCALS_1_STACK_ITEM_FRAME:
        stackMapTableEntries.putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME + offsetDelta);
        putAbstractTypes(3 + numLocal, 4 + numLocal);
        break;
      case Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED:
        stackMapTableEntries
            .putByte(Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED)
            .putShort(offsetDelta);
        putAbstractTypes(3 + numLocal, 4 + numLocal);
        break;
      case Frame.SAME_FRAME_EXTENDED:
        stackMapTableEntries.putByte(Frame.SAME_FRAME_EXTENDED).putShort(offsetDelta);
        break;
      case Frame.CHOP_FRAME:
        stackMapTableEntries
            .putByte(Frame.SAME_FRAME_EXTENDED + numLocalDelta)
            .putShort(offsetDelta);
        break;
      case Frame.APPEND_FRAME:
        stackMapTableEntries
            .putByte(Frame.SAME_FRAME_EXTENDED + numLocalDelta)
            .putShort(offsetDelta);
        putAbstractTypes(3 + previousNumlocal, 3 + numLocal);
        break;
      case Frame.FULL_FRAME:
      default:
        stackMapTableEntries.putByte(Frame.FULL_FRAME).putShort(offsetDelta).putShort(numLocal);
        putAbstractTypes(3, 3 + numLocal);
        stackMapTableEntries.putShort(numStack);
        putAbstractTypes(3 + numLocal, 3 + numLocal + numStack);
        break;
    }
  }

  /**
   * 使用 StackMapTable 属性中 JVMS verification_type_info 格式，
   * 将 {@link #currentFrame} 中的部分抽象类型写入 {@link #stackMapTableEntries}。
   *
   * @param start {@link #currentFrame} 中第一个要写入的类型的索引。
   * @param end {@link #currentFrame} 中最后一个要写入的类型的索引（不包括该索引）。
   */
  private void putAbstractTypes(final int start, final int end) {
    for (int i = start; i < end; ++i) {
      Frame.putAbstractType(symbolTable, currentFrame[i], stackMapTableEntries);
    }
  }

  /**
   * 使用 StackMapTable 属性中 JVMS verification_type_info 格式，
   * 将给定的公共 API 帧元素类型写入 {@link #stackMapTableEntries}。
   *
   * @param type 帧元素类型，格式与 {@link MethodVisitor#visitFrame} 相同，
   *             即 {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link Opcodes#FLOAT},
   *             {@link Opcodes#LONG}, {@link Opcodes#DOUBLE}, {@link Opcodes#NULL},
   *             {@link Opcodes#UNINITIALIZED_THIS}，类的内部名称，或表示 NEW 指令（未初始化类型）的 Label。
   */
  private void putFrameType(final Object type) {
    if (type instanceof Integer) {
      stackMapTableEntries.putByte(((Integer) type).intValue());
    } else if (type instanceof String) {
      stackMapTableEntries
          .putByte(Frame.ITEM_OBJECT)
          .putShort(symbolTable.addConstantClass((String) type).index);
    } else {
      stackMapTableEntries
          .putByte(Frame.ITEM_UNINITIALIZED)
          .putShort(((Label) type).bytecodeOffset);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 工具方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回是否可以从给定方法的属性复制此方法的属性（假设在给定的 ClassReader 和此 MethodWriter 之间没有方法访问器）。
   * 此方法应仅在此 MethodWriter 创建后且访问任何内容之前调用。
   * 如果与构造函数参数对应的属性（最多包括 Signature、Exception、Deprecated 和 Synthetic 属性）
   * 与给定方法中对应的属性相同，则返回 true。
   *
   * @param source 可能用于复制此方法属性的源 ClassReader。
   * @param hasSyntheticAttribute 源方法的 method_info JVMS 结构中是否包含 Synthetic 属性。
   * @param hasDeprecatedAttribute 源方法的 method_info JVMS 结构中是否包含 Deprecated 属性。
   * @param descriptorIndex 源方法的 method_info JVMS 结构中的 descriptor_index 字段。
   * @param signatureIndex 源方法的 method_info JVMS 结构中 Signature 属性包含的常量池索引，或为 0。
   * @param exceptionsOffset 源方法的 method_info JVMS 结构中 Exceptions 属性在 source.b 中的偏移，或为 0。
   * @return 是否可以从源方法的 method_info JVMS 结构中复制此方法的属性。
   */
  boolean canCopyMethodAttributes(
      final ClassReader source,
      final boolean hasSyntheticAttribute,
      final boolean hasDeprecatedAttribute,
      final int descriptorIndex,
      final int signatureIndex,
      final int exceptionsOffset) {
    // 如果方法描述符已更改，且局部变量数量多于原 Code 属性中的 max_locals 字段（如果有），
    // 则不能复制原始方法属性。这里对描述符变更做保守检查，避免复杂判断，因为这种情况较少见——
    // 大多数情况下，方法描述符变化会伴随代码变化。
    if (source != symbolTable.getSource()
        || descriptorIndex != this.descriptorIndex
        || signatureIndex != this.signatureIndex
        || hasDeprecatedAttribute != ((accessFlags & Opcodes.ACC_DEPRECATED) != 0)) {
      return false;
    }
    boolean needSyntheticAttribute =
        symbolTable.getMajorVersion() < Opcodes.V1_5 && (accessFlags & Opcodes.ACC_SYNTHETIC) != 0;
    if (hasSyntheticAttribute != needSyntheticAttribute) {
      return false;
    }
    if (exceptionsOffset == 0) {
      if (numberOfExceptions != 0) {
        return false;
      }
    } else if (source.readUnsignedShort(exceptionsOffset) == numberOfExceptions) {
      int currentExceptionOffset = exceptionsOffset + 2;
      for (int i = 0; i < numberOfExceptions; ++i) {
        if (source.readUnsignedShort(currentExceptionOffset) != exceptionIndexTable[i]) {
          return false;
        }
        currentExceptionOffset += 2;
      }
    }
    return true;
  }

  /**
   * 设置将从其复制此方法属性的源。
   *
   * @param methodInfoOffset method_info JVMS 结构在 'symbolTable.getSource()' 中的偏移。
   * @param methodInfoLength method_info JVMS 结构在 'symbolTable.getSource()' 中的长度。
   */
  void setMethodAttributesSource(final int methodInfoOffset, final int methodInfoLength) {
    // 先不复制属性，而是存储它们在源 ClassReader 中的位置，
    // 以便稍后在 {@link #putMethodInfo} 中复制。
    // 注意跳过 method_info JVMS 结构的 6 字节头部。
    this.sourceOffset = methodInfoOffset + 6;
    this.sourceLength = methodInfoLength - 6;
  }

  /**
   * 返回此 MethodWriter 生成的 method_info JVMS 结构的大小。
   * 同时将此方法的属性名称加入常量池。
   *
   * @return method_info JVMS 结构的字节大小。
   */
  int computeMethodInfoSize() {
    // 如果 method_info 必须从已有的复制，大小计算就非常简单。
    if (sourceOffset != 0) {
      // sourceLength 不包含前面 6 个字节，即 access_flags、name_index 和 descriptor_index。
      return 6 + sourceLength;
    }
  // access_flags、name_index、descriptor_index 和 attributes_count 各占 2 字节。
    int size = 8;
    // 为便于参考，这里使用与 JVMS 第 4.7 节中相同的属性顺序。
    if (code.length > 0) {
      if (code.length > 65535) {
        throw new MethodTooLargeException(
            symbolTable.getClassName(), name, descriptor, code.length);
      }
      symbolTable.addConstantUtf8(Constants.CODE);
    // Code 属性包含 6 个头部字节，另外分别为 max_stack、max_locals、code_length 和 attributes_count 各 2、2、4 和 2 字节，
    // 以及字节码和异常表。
      size += 16 + code.length + Handler.getExceptionTableSize(firstHandler);
      if (stackMapTableEntries != null) {
        boolean useStackMapTable = symbolTable.getMajorVersion() >= Opcodes.V1_6;
        symbolTable.addConstantUtf8(useStackMapTable ? Constants.STACK_MAP_TABLE : "StackMap");
        // 6 字节的头部信息和 2 字节的 number_of_entries。
        size += 8 + stackMapTableEntries.length;
      }
      if (lineNumberTable != null) {
        symbolTable.addConstantUtf8(Constants.LINE_NUMBER_TABLE);
        // 6 字节的头部信息和 2 字节的 line_number_table_length。
        size += 8 + lineNumberTable.length;
      }
      if (localVariableTable != null) {
        symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TABLE);
        // 6 字节的头部信息和 2 字节的 local_variable_table_length。
        size += 8 + localVariableTable.length;
      }
      if (localVariableTypeTable != null) {
        symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TYPE_TABLE);
         // 6 字节的头部信息和 2 字节的 local_variable_type_table_length。
        size += 8 + localVariableTypeTable.length;
      }
      if (lastCodeRuntimeVisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
      }
      if (lastCodeRuntimeInvisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
      }
      if (firstCodeAttribute != null) {
        size +=
            firstCodeAttribute.computeAttributesSize(
                symbolTable, code.data, code.length, maxStack, maxLocals);
      }
    }
    if (numberOfExceptions > 0) {
      symbolTable.addConstantUtf8(Constants.EXCEPTIONS);
      size += 8 + 2 * numberOfExceptions;
    }
    size += Attribute.computeAttributesSize(symbolTable, accessFlags, signatureIndex);
    size +=
        AnnotationWriter.computeAnnotationsSize(
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation);
    if (lastRuntimeVisibleParameterAnnotations != null) {
      size +=
          AnnotationWriter.computeParameterAnnotationsSize(
              Constants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
              lastRuntimeVisibleParameterAnnotations,
              visibleAnnotableParameterCount == 0
                  ? lastRuntimeVisibleParameterAnnotations.length
                  : visibleAnnotableParameterCount);
    }
    if (lastRuntimeInvisibleParameterAnnotations != null) {
      size +=
          AnnotationWriter.computeParameterAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
              lastRuntimeInvisibleParameterAnnotations,
              invisibleAnnotableParameterCount == 0
                  ? lastRuntimeInvisibleParameterAnnotations.length
                  : invisibleAnnotableParameterCount);
    }
    if (defaultValue != null) {
      symbolTable.addConstantUtf8(Constants.ANNOTATION_DEFAULT);
      size += 6 + defaultValue.length;
    }
    if (parameters != null) {
      symbolTable.addConstantUtf8(Constants.METHOD_PARAMETERS);
      // 6 个头部字节和 1 个字节的 parameters_count。
      size += 7 + parameters.length;
    }
    if (firstAttribute != null) {
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * 将此 MethodWriter 生成的 method_info JVMS 结构的内容写入给定的 ByteVector。
   *
   * @param output 要写入 method_info 结构的目标 ByteVector。
   */
  void putMethodInfo(final ByteVector output) {
    boolean useSyntheticAttribute = symbolTable.getMajorVersion() < Opcodes.V1_5;
    int mask = useSyntheticAttribute ? Opcodes.ACC_SYNTHETIC : 0;
    output.putShort(accessFlags & ~mask).putShort(nameIndex).putShort(descriptorIndex);
    // 如果此 method_info 必须从现有的复制，则立即复制并提前返回。
    if (sourceOffset != 0) {
      output.putByteArray(symbolTable.getSource().classFileBuffer, sourceOffset, sourceLength);
      return;
    }
    // 为便于参考，这里使用与 JVMS 第 4.7 节中相同的属性顺序。
    int attributeCount = 0;
    if (code.length > 0) {
      ++attributeCount;
    }
    if (numberOfExceptions > 0) {
      ++attributeCount;
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
      ++attributeCount;
    }
    if (signatureIndex != 0) {
      ++attributeCount;
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      ++attributeCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributeCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributeCount;
    }
    if (lastRuntimeVisibleParameterAnnotations != null) {
      ++attributeCount;
    }
    if (lastRuntimeInvisibleParameterAnnotations != null) {
      ++attributeCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributeCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributeCount;
    }
    if (defaultValue != null) {
      ++attributeCount;
    }
    if (parameters != null) {
      ++attributeCount;
    }
    if (firstAttribute != null) {
      attributeCount += firstAttribute.getAttributeCount();
    }
    // 为便于参考，这里使用与 JVMS 第 4.7 节中相同的属性顺序。
    output.putShort(attributeCount);
    if (code.length > 0) {
    // 分别为 max_stack、max_locals、code_length 和 attributes_count 占用 2、2、4 和 2 字节，外加字节码和异常表。
      int size = 10 + code.length + Handler.getExceptionTableSize(firstHandler);
      int codeAttributeCount = 0;
      if (stackMapTableEntries != null) {
        // 6 字节的头部信息和 2 字节的 number_of_entries。
        size += 8 + stackMapTableEntries.length;
        ++codeAttributeCount;
      }
      if (lineNumberTable != null) {
        // 6 字节的头部信息和 2 字节的 line_number_table_length。
        size += 8 + lineNumberTable.length;
        ++codeAttributeCount;
      }
      if (localVariableTable != null) {
        // 6 字节的头部信息和 2 字节的 local_variable_table_length。
        size += 8 + localVariableTable.length;
        ++codeAttributeCount;
      }
      if (localVariableTypeTable != null) {
         // 6 字节的头部信息和 2 字节的 local_variable_type_table_length。
        size += 8 + localVariableTypeTable.length;
        ++codeAttributeCount;
      }
      if (lastCodeRuntimeVisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
        ++codeAttributeCount;
      }
      if (lastCodeRuntimeInvisibleTypeAnnotation != null) {
        size +=
            lastCodeRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
                Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
        ++codeAttributeCount;
      }
      if (firstCodeAttribute != null) {
        size +=
            firstCodeAttribute.computeAttributesSize(
                symbolTable, code.data, code.length, maxStack, maxLocals);
        codeAttributeCount += firstCodeAttribute.getAttributeCount();
      }
      output
          .putShort(symbolTable.addConstantUtf8(Constants.CODE))
          .putInt(size)
          .putShort(maxStack)
          .putShort(maxLocals)
          .putInt(code.length)
          .putByteArray(code.data, 0, code.length);
      Handler.putExceptionTable(firstHandler, output);
      output.putShort(codeAttributeCount);
      if (stackMapTableEntries != null) {
        boolean useStackMapTable = symbolTable.getMajorVersion() >= Opcodes.V1_6;
        output
            .putShort(
                symbolTable.addConstantUtf8(
                    useStackMapTable ? Constants.STACK_MAP_TABLE : "StackMap"))
            .putInt(2 + stackMapTableEntries.length)
            .putShort(stackMapTableNumberOfEntries)
            .putByteArray(stackMapTableEntries.data, 0, stackMapTableEntries.length);
      }
      if (lineNumberTable != null) {
        output
            .putShort(symbolTable.addConstantUtf8(Constants.LINE_NUMBER_TABLE))
            .putInt(2 + lineNumberTable.length)
            .putShort(lineNumberTableLength)
            .putByteArray(lineNumberTable.data, 0, lineNumberTable.length);
      }
      if (localVariableTable != null) {
        output
            .putShort(symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TABLE))
            .putInt(2 + localVariableTable.length)
            .putShort(localVariableTableLength)
            .putByteArray(localVariableTable.data, 0, localVariableTable.length);
      }
      if (localVariableTypeTable != null) {
        output
            .putShort(symbolTable.addConstantUtf8(Constants.LOCAL_VARIABLE_TYPE_TABLE))
            .putInt(2 + localVariableTypeTable.length)
            .putShort(localVariableTypeTableLength)
            .putByteArray(localVariableTypeTable.data, 0, localVariableTypeTable.length);
      }
      if (lastCodeRuntimeVisibleTypeAnnotation != null) {
        lastCodeRuntimeVisibleTypeAnnotation.putAnnotations(
            symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output);
      }
      if (lastCodeRuntimeInvisibleTypeAnnotation != null) {
        lastCodeRuntimeInvisibleTypeAnnotation.putAnnotations(
            symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output);
      }
      if (firstCodeAttribute != null) {
        firstCodeAttribute.putAttributes(
            symbolTable, code.data, code.length, maxStack, maxLocals, output);
      }
    }
    if (numberOfExceptions > 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.EXCEPTIONS))
          .putInt(2 + 2 * numberOfExceptions)
          .putShort(numberOfExceptions);
      for (int exceptionIndex : exceptionIndexTable) {
        output.putShort(exceptionIndex);
      }
    }
    Attribute.putAttributes(symbolTable, accessFlags, signatureIndex, output);
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        output);
    if (lastRuntimeVisibleParameterAnnotations != null) {
      AnnotationWriter.putParameterAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS),
          lastRuntimeVisibleParameterAnnotations,
          visibleAnnotableParameterCount == 0
              ? lastRuntimeVisibleParameterAnnotations.length
              : visibleAnnotableParameterCount,
          output);
    }
    if (lastRuntimeInvisibleParameterAnnotations != null) {
      AnnotationWriter.putParameterAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS),
          lastRuntimeInvisibleParameterAnnotations,
          invisibleAnnotableParameterCount == 0
              ? lastRuntimeInvisibleParameterAnnotations.length
              : invisibleAnnotableParameterCount,
          output);
    }
    if (defaultValue != null) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.ANNOTATION_DEFAULT))
          .putInt(defaultValue.length)
          .putByteArray(defaultValue.data, 0, defaultValue.length);
    }
    if (parameters != null) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.METHOD_PARAMETERS))
          .putInt(1 + parameters.length)
          .putByte(parametersCount)
          .putByteArray(parameters.data, 0, parameters.length);
    }
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }

  /**
   * 将该方法的属性收集到给定的属性原型集合中。
   *
   * @param attributePrototypes 属性原型集合。
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
    attributePrototypes.addAttributes(firstCodeAttribute);
  }
}
