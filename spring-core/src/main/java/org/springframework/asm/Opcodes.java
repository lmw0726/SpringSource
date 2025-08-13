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
 * JVM操作码、访问标志和数组类型代码。此接口没有定义所有JVM操作码，因为某些操作码是自动处理的。
 * 例如，xLOAD和xSTORE操作码在可能的情况下会自动替换为xLOAD_n和xSTORE_n操作码。
 * 因此xLOAD_n和xSTORE_n操作码未在此接口中定义。同样，LDC在必要时会自动替换为LDC_W或LDC2_W，
 * 还有WIDE、GOTO_W和JSR_W。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html">JVMS 6</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
// DontCheck(InterfaceIsType): 无法修复（为了向后二进制兼容性）。
public interface Opcodes {

  // ASM API版本。

  int ASM4 = 4 << 16 | 0 << 8;
  int ASM5 = 5 << 16 | 0 << 8;
  int ASM6 = 6 << 16 | 0 << 8;
  int ASM7 = 7 << 16 | 0 << 8;
  int ASM8 = 8 << 16 | 0 << 8;
  int ASM9 = 9 << 16 | 0 << 8;

  /**
   * <i>实验性功能，使用风险自负。当此字段变稳定时将被重命名，这将破坏使用它的现有代码。
   * 只有使用--enable-preview编译的代码才能使用此功能。</i>
   * <p>SPRING补丁：对ASM 10实验性功能不进行预览模式检查，默认启用。
   */
  int ASM10_EXPERIMENTAL = 1 << 24 | 10 << 16 | 0 << 8;

  /*
   * 用于重定向对废弃方法调用的内部标志。例如，如果API_OLD中的visitOldStuff方法
   * 被废弃并在API_NEW中替换为visitNewStuff，那么重定向应按以下方式进行：
   *
   * <pre>
   * public class StuffVisitor {
   *   ...
   *
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
   *     // SOURCE_DEPRECATED表示"来自使用旧'api'值的废弃方法的调用"。
   *     visitNewStuf(arg | (api &#60; API_NEW ? SOURCE_DEPRECATED : 0), ...);
   *   }
   *
   *   public void visitNewStuff(int argAndSource, ...) {
   *     if (api &#60; API_NEW &#38;&#38; (argAndSource &#38; SOURCE_DEPRECATED) == 0) {
   *       visitOldStuff(argAndSource, ...);
   *     } else {
   *       int arg = argAndSource &#38; ~SOURCE_MASK;
   *       [ 执行操作 ]
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>如果'api'等于API_NEW，有两种情况：
   *
   * <ul>
   *   <li>调用visitNewStuff：跳过重定向测试，直接执行'执行操作'。
   *   <li>调用visitOldSuff：在调用visitNewStuff之前不会将源设置为SOURCE_DEPRECATED，
   *       但在visitNewStuff中仍会跳过重定向测试，直接执行'执行操作'。
   * </ul>
   *
   * <p>如果'api'等于API_OLD，有两种情况：
   *
   * <ul>
   *   <li>调用visitOldSuff：在调用visitNewStuff之前将源设置为SOURCE_DEPRECATED。
   *       因此visitNewStuff不会重定向回visitOldStuff，而是执行'执行操作'。
   *   <li>调用visitNewStuff：因为源为0，调用被重定向到visitOldStuff。
   *       visitOldStuff现在将源设置为SOURCE_DEPRECATED并回调visitNewStuff。
   *       这次visitNewStuff不会重定向调用，而是执行'执行操作'。
   * </ul>
   *
   * <h1>用户子类</h1>
   *
   * <p>如果用户子类重写了这些方法之一，只有两种情况：要么'api'是API_OLD且重写了
   * visitOldStuff（而没有重写visitNewStuff），要么'api'是API_NEW或更高版本且重写了
   * visitNewStuff（而没有重写visitOldStuff）。任何其他情况都是用户编程错误。
   *
   * <p>如果'api'等于API_NEW，类层次结构相当于
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) { visitNewStuf(arg, ...); }
   *   public void visitNewStuff(int arg, ...) { [ 执行操作 ] }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int arg, ...) {
   *     super.visitNewStuff(int arg, ...); // 可选
   *     [ 执行用户操作 ]
   *   }
   * }
   * </pre>
   *
   * <p>很明显，无论调用visitNewStuff还是visitOldStuff，'执行操作'和'执行用户操作'
   * 都会按此顺序执行。
   *
   * <p>如果'api'等于API_OLD，类层次结构相当于
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
   *     visitNewStuff(arg | SOURCE_DEPRECATED, ...);
   *   }
   *   public void visitNewStuff(int argAndSource...) {
   *     if ((argAndSource & SOURCE_DEPRECATED) == 0) {
   *       visitOldStuff(argAndSource, ...);
   *     } else {
   *       int arg = argAndSource &#38; ~SOURCE_MASK;
   *       [ 执行操作 ]
   *     }
   *   }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitOldStuff(int arg, ...) {
   *     super.visitOldStuff(int arg, ...); // 可选
   *     [ 执行用户操作 ]
   *   }
   * }
   * </pre>
   *
   * <p>有两种情况：
   *
   * <ul>
   *   <li>调用visitOldStuff：在调用super.visitOldStuff时，源被设置为SOURCE_DEPRECATED
   *       并调用visitNewStuff。这里执行'执行操作'是因为源之前被设置为SOURCE_DEPRECATED，
   *       执行最终返回到UserStuffVisitor.visitOldStuff，在那里执行'执行用户操作'。
   *   <li>调用visitNewStuff：因为源为0，调用被重定向到UserStuffVisitor.visitOldStuff。
   *       执行如前一种情况继续，结果是按此顺序执行'执行操作'和'执行用户操作'。
   * </ul>
   *
   * <h1>ASM子类</h1>
   *
   * <p>在ASM包中，StuffVisitor的子类通常可以被用户再次子类化，并可以与API_OLD或
   * API_NEW一起使用。因此，如果这样的子类必须重写visitNewStuff，它必须按以下方式
   * 执行（且不得重写visitOldStuff）：
   *
   * <pre>
   * public class AsmStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int argAndSource, ...) {
   *     if (api &#60; API_NEW &#38;&#38; (argAndSource &#38; SOURCE_DEPRECATED) == 0) {
   *       super.visitNewStuff(argAndSource, ...);
   *       return;
   *     }
   *     super.visitNewStuff(argAndSource, ...); // 可选
   *     int arg = argAndSource &#38; ~SOURCE_MASK;
   *     [ 执行其他操作 ]
   *   }
   * }
   * </pre>
   *
   * <p>如果用户类以'api'等于API_NEW扩展此类，类层次结构相当于
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) { visitNewStuf(arg, ...); }
   *   public void visitNewStuff(int arg, ...) { [ 执行操作 ] }
   * }
   * public class AsmStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int arg, ...) {
   *     super.visitNewStuff(arg, ...);
   *     [ 执行其他操作 ]
   *   }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int arg, ...) {
   *     super.visitNewStuff(int arg, ...);
   *     [ 执行用户操作 ]
   *   }
   * }
   * </pre>
   *
   * <p>很明显，无论调用visitNewStuff还是visitOldStuff，'执行操作'、'执行其他操作'
   * 和'执行用户操作'都会按此顺序执行。另一方面，如果用户类以'api'等于API_OLD
   * 扩展AsmStuffVisitor，类层次结构相当于
   *
   * <pre>
   * public class StuffVisitor {
   *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
   *     visitNewStuf(arg | SOURCE_DEPRECATED, ...);
   *   }
   *   public void visitNewStuff(int argAndSource, ...) {
   *     if ((argAndSource & SOURCE_DEPRECATED) == 0) {
   *       visitOldStuff(argAndSource, ...);
   *     } else {
   *       int arg = argAndSource &#38; ~SOURCE_MASK;
   *       [ 执行操作 ]
   *     }
   *   }
   * }
   * public class AsmStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitNewStuff(int argAndSource, ...) {
   *     if ((argAndSource &#38; SOURCE_DEPRECATED) == 0) {
   *       super.visitNewStuff(argAndSource, ...);
   *       return;
   *     }
   *     super.visitNewStuff(argAndSource, ...); // 可选
   *     int arg = argAndSource &#38; ~SOURCE_MASK;
   *     [ 执行其他操作 ]
   *   }
   * }
   * class UserStuffVisitor extends StuffVisitor {
   *   &#64;Override public void visitOldStuff(int arg, ...) {
   *     super.visitOldStuff(arg, ...);
   *     [ 执行用户操作 ]
   *   }
   * }
   * </pre>
   *
   * <p>同样，无论调用visitNewStuff还是visitOldStuff，'执行操作'、'执行其他操作'
   * 和'执行用户操作'都会按此顺序执行（练习留给读者）。
   *
   * <h1>注意事项</h1>
   *
   * <ul>
   *   <li>只有当'api'是API_OLD时，才会在调用visitNewStuff之前设置SOURCE_DEPRECATED标志。
   *       根据假设，用户不会重写此方法。因此，用户类永远不会看到此标志。只有ASM子类必须
   *       注意通过清除源标志来提取实际参数值。
   *   <li>因为SOURCE_DEPRECATED标志在调用者中立即清除，调用者可以在委托访问者上调用
   *       visitOldStuff或visitNewStuff（在'执行操作'和'执行用户操作'中）而无任何风险
   *       （破坏重定向逻辑、"泄漏"标志等）。
   *   <li>上述讨论的所有场景都在MethodVisitorTest中进行了单元测试。
   * </ul>
   */

  int SOURCE_DEPRECATED = 0x100; // 源已废弃标志
  int SOURCE_MASK = SOURCE_DEPRECATED; // 源掩码

  // Java类文件版本（次版本号存储在最高16位，主版本号存储在最低16位）。
  int V1_1 = 3 << 16 | 45;
  int V1_2 = 0 << 16 | 46;
  int V1_3 = 0 << 16 | 47;
  int V1_4 = 0 << 16 | 48;
  int V1_5 = 0 << 16 | 49;
  int V1_6 = 0 << 16 | 50;
  int V1_7 = 0 << 16 | 51;
  int V1_8 = 0 << 16 | 52;
  int V9 = 0 << 16 | 53;
  int V10 = 0 << 16 | 54;
  int V11 = 0 << 16 | 55;
  int V12 = 0 << 16 | 56;
  int V13 = 0 << 16 | 57;
  int V14 = 0 << 16 | 58;
  int V15 = 0 << 16 | 59;
  int V16 = 0 << 16 | 60;
  int V17 = 0 << 16 | 61;
  int V18 = 0 << 16 | 62;
  int V19 = 0 << 16 | 63;

  /**
   * 版本标志，表示类正在使用'预览'特性。
   *
   * <p>{@code version & V_PREVIEW == V_PREVIEW} 测试版本是否标记了 {@code
   * V_PREVIEW}。
   */
  int V_PREVIEW = 0xFFFF0000;

  // 访问标志值，定义在：
  // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1-200-E.1
  // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5-200-A.1
  // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6-200-A.1
  // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25

  int ACC_PUBLIC = 0x0001; // 类、字段、方法
  int ACC_PRIVATE = 0x0002; // 类、字段、方法
  int ACC_PROTECTED = 0x0004; // 类、字段、方法
  int ACC_STATIC = 0x0008; // 字段、方法
  int ACC_FINAL = 0x0010; // 类、字段、方法、参数
  int ACC_SUPER = 0x0020; // 类
  int ACC_SYNCHRONIZED = 0x0020; // 方法
  int ACC_OPEN = 0x0020; // 模块
  int ACC_TRANSITIVE = 0x0020; // 模块依赖
  int ACC_VOLATILE = 0x0040; // 字段
  int ACC_BRIDGE = 0x0040; // 方法
  int ACC_STATIC_PHASE = 0x0040; // 模块依赖
  int ACC_VARARGS = 0x0080; // 方法
  int ACC_TRANSIENT = 0x0080; // 字段
  int ACC_NATIVE = 0x0100; // 方法
  int ACC_INTERFACE = 0x0200; // 类
  int ACC_ABSTRACT = 0x0400; // 类、方法
  int ACC_STRICT = 0x0800; // 方法
  int ACC_SYNTHETIC = 0x1000; // 类、字段、方法、参数、模块 *
  int ACC_ANNOTATION = 0x2000; // 类
  int ACC_ENUM = 0x4000; // 类(?) 字段 内部
  int ACC_MANDATED = 0x8000; // 字段、方法、参数、模块、模块 *
  int ACC_MODULE = 0x8000; // 类

  // ASM特定的访问标志。
  // 警告：最低16位不得使用，以避免与标准访问标志冲突，
  // 并确保这些标志在写入类文件时被自动过滤掉（因为访问标志只用16位存储）。

  int ACC_RECORD = 0x10000; // 类
  int ACC_DEPRECATED = 0x20000; // 类、字段、方法

  // NEWARRAY指令的type操作数的可能值。
  // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5.newarray。

  int T_BOOLEAN = 4;
  int T_CHAR = 5;
  int T_FLOAT = 6;
  int T_DOUBLE = 7;
  int T_BYTE = 8;
  int T_SHORT = 9;
  int T_INT = 10;
  int T_LONG = 11;

  // CONSTANT_MethodHandle_info结构中reference_kind字段的可能值。
  // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.8。
  int H_GETFIELD = 1;
  int H_GETSTATIC = 2;
  int H_PUTFIELD = 3;
  int H_PUTSTATIC = 4;
  int H_INVOKEVIRTUAL = 5;
  int H_INVOKESTATIC = 6;
  int H_INVOKESPECIAL = 7;
  int H_NEWINVOKESPECIAL = 8;
  int H_INVOKEINTERFACE = 9;

  // ASM特定的栈映射帧类型，用于 {@link ClassVisitor#visitFrame}。

  /** 展开帧。参见 {@link ClassReader#EXPAND_FRAMES}。 */
  int F_NEW = -1;

  /** 包含完整帧数据的压缩帧。 */
  int F_FULL = 0;

  /**
   * 压缩帧，其中局部变量与前一帧的局部变量相同，除了额外定义了1-3个局部变量，且栈为空。
   */
  int F_APPEND = 1;

  /**
   * 压缩帧，其中局部变量与前一帧的局部变量相同，除了最后1-3个局部变量不存在，且栈为空。
   */
  int F_CHOP = 2;

  /**
   * 压缩帧，局部变量与前一帧完全相同，且栈为空。
   */
  int F_SAME = 3;

  /**
   * 压缩帧，局部变量与前一帧完全相同，且栈上有单个值。
   */
  int F_SAME1 = 4;

  // 标准栈映射帧元素类型，用于 {@link ClassVisitor#visitFrame}。

  Integer TOP = Frame.ITEM_TOP;
  Integer INTEGER = Frame.ITEM_INTEGER;
  Integer FLOAT = Frame.ITEM_FLOAT;
  Integer DOUBLE = Frame.ITEM_DOUBLE;
  Integer LONG = Frame.ITEM_LONG;
  Integer NULL = Frame.ITEM_NULL;
  Integer UNINITIALIZED_THIS = Frame.ITEM_UNINITIALIZED_THIS;

  // JVM操作码值（注释中为用于访问它们的MethodVisitor方法名，
  // 其中'-'表示'与上一行相同的方法名'）。
  // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html。

  int NOP = 0; // visitInsn - 无操作指令
  int ACONST_NULL = 1; // -
  int ICONST_M1 = 2; // -
  int ICONST_0 = 3; // -
  int ICONST_1 = 4; // -
  int ICONST_2 = 5; // -
  int ICONST_3 = 6; // -
  int ICONST_4 = 7; // -
  int ICONST_5 = 8; // -
  int LCONST_0 = 9; // -
  int LCONST_1 = 10; // -
  int FCONST_0 = 11; // -
  int FCONST_1 = 12; // -
  int FCONST_2 = 13; // -
  int DCONST_0 = 14; // -
  int DCONST_1 = 15; // -
  int BIPUSH = 16; // visitIntInsn - 将byte值推入栈（扩展为int）
  int SIPUSH = 17; // -
  int LDC = 18; // visitLdcInsn - 从常量池加载常量值
  int ILOAD = 21; // visitVarInsn - 加载局部int变量
  int LLOAD = 22; // -
  int FLOAD = 23; // -
  int DLOAD = 24; // -
  int ALOAD = 25; // -
  int IALOAD = 46; // visitInsn - 从int数组加载元素
  int LALOAD = 47; // -
  int FALOAD = 48; // -
  int DALOAD = 49; // -
  int AALOAD = 50; // -
  int BALOAD = 51; // -
  int CALOAD = 52; // -
  int SALOAD = 53; // -
  int ISTORE = 54; // visitVarInsn - 存储int值到局部变量
  int LSTORE = 55; // -
  int FSTORE = 56; // -
  int DSTORE = 57; // -
  int ASTORE = 58; // -
  int IASTORE = 79; // visitInsn - 存储值到int数组
  int LASTORE = 80; // -
  int FASTORE = 81; // -
  int DASTORE = 82; // -
  int AASTORE = 83; // -
  int BASTORE = 84; // -
  int CASTORE = 85; // -
  int SASTORE = 86; // -
  int POP = 87; // -
  int POP2 = 88; // -
  int DUP = 89; // -
  int DUP_X1 = 90; // -
  int DUP_X2 = 91; // -
  int DUP2 = 92; // -
  int DUP2_X1 = 93; // -
  int DUP2_X2 = 94; // -
  int SWAP = 95; // -
  int IADD = 96; // -
  int LADD = 97; // -
  int FADD = 98; // -
  int DADD = 99; // -
  int ISUB = 100; // -
  int LSUB = 101; // -
  int FSUB = 102; // -
  int DSUB = 103; // -
  int IMUL = 104; // -
  int LMUL = 105; // -
  int FMUL = 106; // -
  int DMUL = 107; // -
  int IDIV = 108; // -
  int LDIV = 109; // -
  int FDIV = 110; // -
  int DDIV = 111; // -
  int IREM = 112; // -
  int LREM = 113; // -
  int FREM = 114; // -
  int DREM = 115; // -
  int INEG = 116; // -
  int LNEG = 117; // -
  int FNEG = 118; // -
  int DNEG = 119; // -
  int ISHL = 120; // -
  int LSHL = 121; // -
  int ISHR = 122; // -
  int LSHR = 123; // -
  int IUSHR = 124; // -
  int LUSHR = 125; // -
  int IAND = 126; // -
  int LAND = 127; // -
  int IOR = 128; // -
  int LOR = 129; // -
  int IXOR = 130; // -
  int LXOR = 131; // -
  int IINC = 132; // visitIincInsn - 局部int变量自增
  int I2L = 133; // visitInsn - int转换为long
  int I2F = 134; // -
  int I2D = 135; // -
  int L2I = 136; // -
  int L2F = 137; // -
  int L2D = 138; // -
  int F2I = 139; // -
  int F2L = 140; // -
  int F2D = 141; // -
  int D2I = 142; // -
  int D2L = 143; // -
  int D2F = 144; // -
  int I2B = 145; // -
  int I2C = 146; // -
  int I2S = 147; // -
  int LCMP = 148; // -
  int FCMPL = 149; // -
  int FCMPG = 150; // -
  int DCMPL = 151; // -
  int DCMPG = 152; // -
  int IFEQ = 153; // visitJumpInsn - 如果等于0则跳转
  int IFNE = 154; // -
  int IFLT = 155; // -
  int IFGE = 156; // -
  int IFGT = 157; // -
  int IFLE = 158; // -
  int IF_ICMPEQ = 159; // -
  int IF_ICMPNE = 160; // -
  int IF_ICMPLT = 161; // -
  int IF_ICMPGE = 162; // -
  int IF_ICMPGT = 163; // -
  int IF_ICMPLE = 164; // -
  int IF_ACMPEQ = 165; // -
  int IF_ACMPNE = 166; // -
  int GOTO = 167; // -
  int JSR = 168; // -
  int RET = 169; // visitVarInsn - 返回指令（从子程序返回）
  int TABLESWITCH = 170; // visiTableSwitchInsn - 表格跳转指令
  int LOOKUPSWITCH = 171; // visitLookupSwitch - 查找跳转指令
  int IRETURN = 172; // visitInsn - 返回int值
  int LRETURN = 173; // -
  int FRETURN = 174; // -
  int DRETURN = 175; // -
  int ARETURN = 176; // -
  int RETURN = 177; // -
  int GETSTATIC = 178; // visitFieldInsn - 获取静态字段值
  int PUTSTATIC = 179; // -
  int GETFIELD = 180; // -
  int PUTFIELD = 181; // -
  int INVOKEVIRTUAL = 182; // visitMethodInsn - 调用虚方法
  int INVOKESPECIAL = 183; // -
  int INVOKESTATIC = 184; // -
  int INVOKEINTERFACE = 185; // -
  int INVOKEDYNAMIC = 186; // visitInvokeDynamicInsn - 调用动态方法
  int NEW = 187; // visitTypeInsn - 创建新对象实例
  int NEWARRAY = 188; // visitIntInsn - 创建基本类型数组
  int ANEWARRAY = 189; // visitTypeInsn - 创建引用类型数组
  int ARRAYLENGTH = 190; // visitInsn - 获取数组长度
  int ATHROW = 191; // -
  int CHECKCAST = 192; // visitTypeInsn - 类型检查转换
  int INSTANCEOF = 193; //
  int MONITORENTER = 194; // visitInsn - 进入同步块
  int MONITOREXIT = 195; // -
  int MULTIANEWARRAY = 197; // visitMultiANewArrayInsn - 创建多维数组
  int IFNULL = 198; // visitJumpInsn - 如果为null则跳转
  int IFNONNULL = 199; // -
}
