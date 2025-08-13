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
 * 访问一个 Java 方法的访问者。此类的方法必须按照以下顺序调用：
 * ( {@code visitParameter} )* [ {@code visitAnnotationDefault} ] 
 * ( {@code visitAnnotation} | {@code visitAnnotableParameterCount} | {@code visitParameterAnnotation} | {@code visitTypeAnnotation} | {@code visitAttribute} )* 
 * [ {@code visitCode} ( {@code visitFrame} | {@code visit<i>X</i>Insn} | {@code visitLabel} | {@code visitInsnAnnotation} | {@code visitTryCatchBlock} | {@code visitTryCatchAnnotation} | {@code visitLocalVariable} | {@code visitLocalVariableAnnotation} | {@code visitLineNumber} )* {@code visitMaxs} ] 
 * {@code visitEnd}。
 *
 * 此外，{@code visit<i>X</i>Insn} 和 {@code visitLabel} 方法必须按照被访问代码的字节码指令顺序调用，
 * {@code visitInsnAnnotation} 必须在注解的指令 <i>之后</i> 调用，
 * {@code visitTryCatchBlock} 必须在被传递的标签被访问之前调用，
 * {@code visitTryCatchBlockAnnotation} 必须在对应的 try catch 块访问后调用，
 * {@code visitLocalVariable}、{@code visitLocalVariableAnnotation} 和 {@code visitLineNumber} 必须在被传递的标签被访问后调用。
 *
 * @author Eric Bruneton
 */
public abstract class MethodVisitor {

  private static final String REQUIRES_ASM5 = "This feature requires ASM5";

  /**
   * 此访问者实现的 ASM API 版本。该字段值必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 值之一。
   */
  protected final int api;

  /**
   * 此访问者委托调用的方法访问者。可能为 {@literal null}。
   */
  protected MethodVisitor mv;

  /**
   * 构造一个新的 {@link MethodVisitor}。
   *
   * @param api 此访问者实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 值之一。
   */
  protected MethodVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link MethodVisitor}。
   *
   * @param api 此访问者实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 值之一。
   * @param methodVisitor 此访问者委托调用的方法访问者，可能为 null。
   */
  protected MethodVisitor(final int api, final MethodVisitor methodVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: 不对 ASM experimental 版本做预览模式检查
    this.api = api;
    this.mv = methodVisitor;
  }

  // -----------------------------------------------------------------------------------------------
  // 方法参数、注解和非标准属性
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问该方法的一个参数。
   *
   * @param name 参数名，若无则为 {@literal null}。
   * @param access 参数访问标志，只允许 {@code ACC_FINAL}、{@code ACC_SYNTHETIC} 和/或 {@code ACC_MANDATED}（详见 {@link Opcodes}）。
   */
  public void visitParameter(final String name, final int access) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      mv.visitParameter(name, access);
    }
  }

  /**
   * 访问该注解接口方法的默认值。
   *
   * @return 访问该注解接口方法默认值的访问者，若不感兴趣则返回 {@literal null}。调用该访问者的方法时，传入的 'name' 参数会被忽略。
   *         另外，必须且只能调用一个访问方法，随后调用 visitEnd。
   */
  public AnnotationVisitor visitAnnotationDefault() {
    if (mv != null) {
      return mv.visitAnnotationDefault();
    }
    return null;
  }

  /**
   * 访问该方法的注解。
   *
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问者，如果不关心此注解，则返回 {@literal null}。
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (mv != null) {
      return mv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问方法签名中类型的注解。
   *
   * @param typeRef 被注解类型的引用。该类型引用的类别必须是
   *     {@link TypeReference#METHOD_TYPE_PARAMETER}、{@link TypeReference#METHOD_TYPE_PARAMETER_BOUND}、
   *     {@link TypeReference#METHOD_RETURN}、{@link TypeReference#METHOD_RECEIVER}、
   *     {@link TypeReference#METHOD_FORMAL_PARAMETER} 或 {@link TypeReference#THROWS}。参见 {@link TypeReference}。
   * @param typePath 指向被注解类型参数、通配符边界、数组元素类型或静态内部类型的路径。
   *     如果注解针对整个 typeRef，则可能为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问者，如果不关心此注解，则返回 {@literal null}。
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问可被注解的参数个数。默认情况下（即未调用此方法时），方法描述符中的所有参数都可以被注解。
   *
   * @param parameterCount 可被注解的参数数量。此值必须小于等于方法描述符中的参数类型数量。
   *     当方法存在合成参数且这些参数被忽略时，此值可以小于参数类型数量。
   * @param visible 如果为 {@literal true}，定义在运行时可见的注解参数数量；否则定义不可见注解参数数量。
   */
  public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
    if (mv != null) {
      mv.visitAnnotableParameterCount(parameterCount, visible);
    }
  }

  /**
   * 访问方法参数上的注解。
   *
   * @param parameter 参数索引。此索引必须严格小于方法描述符中的参数数量，以及
   *     {@link #visitAnnotableParameterCount} 中指定的参数数量。注意：参数索引不一定与
   *     方法描述符中参数的顺序对应，特别是存在合成参数时。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问者，如果不关心此注解，则返回 {@literal null}。
   */
  public AnnotationVisitor visitParameterAnnotation(
      final int parameter, final String descriptor, final boolean visible) {
    if (mv != null) {
      return mv.visitParameterAnnotation(parameter, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问该方法的非标准属性。
   *
   * @param attribute 属性对象。
   */
  public void visitAttribute(final Attribute attribute) {
    if (mv != null) {
      mv.visitAttribute(attribute);
    }
  }

  /** 开始访问方法代码（即非抽象方法）。 */
  public void visitCode() {
    if (mv != null) {
      mv.visitCode();
    }
  }

  /**
   * 访问当前局部变量和操作数栈元素的状态。此方法必须<i>紧接在</i>无条件分支指令（如 GOTO 或 THROW）之后调用，
   * 或跳转指令的目标处，或异常处理块的开始处。访问的帧类型必须描述执行该指令 <b>前</b> 局部变量和操作数栈的值。<br>
   * <br>
   * (*) 仅对版本大于等于 {@link Opcodes#V1_6} 的类强制要求调用此方法。<br>
   * <br>
   * 方法的帧必须全部采用扩展格式，或全部采用压缩格式（单个方法内不得混合使用两种格式）：
   *
   * <ul>
   *   <li>扩展格式中，所有帧必须是 {@link Opcodes#F_NEW} 类型。
   *   <li>压缩格式中，帧表示与前一帧状态的“增量”：
   *       <ul>
   *         <li>{@link Opcodes#F_SAME} 表示帧与前一帧局部变量相同，操作栈为空。
   *         <li>{@link Opcodes#F_SAME1} 表示帧与前一帧局部变量相同，操作栈有且仅有一个元素。
   *         <li>{@link Opcodes#F_APPEND} 表示帧局部变量为前一帧局部变量加上 1-3 个新局部变量，操作栈为空。
   *         <li>{@link Opcodes#F_CHOP} 表示帧局部变量为前一帧去掉最后 1-3 个局部变量，操作栈为空。
   *         <li>{@link Opcodes#F_FULL} 表示完整帧数据。
   *       </ul>
   * </ul>
   *
   * <br>
   * 第一帧对应方法参数和访问标志，为隐式帧，不能访问。禁止对同一代码位置连续访问多个帧（除非是 {@link Opcodes#F_SAME}，它会被忽略）。
   *
   * @param type 当前栈映射帧的类型。必须为扩展格式 {@link Opcodes#F_NEW}，或压缩格式中的
   *     {@link Opcodes#F_FULL}, {@link Opcodes#F_APPEND}, {@link Opcodes#F_CHOP}, {@link Opcodes#F_SAME} 或 {@link Opcodes#F_SAME1}。
   * @param numLocal 当前帧中局部变量的数量。
   * @param local 当前帧中局部变量的类型数组，不能修改。基本类型使用 {@link Opcodes#TOP}、{@link Opcodes#INTEGER}、{@link Opcodes#FLOAT}、{@link Opcodes#LONG}、{@link Opcodes#DOUBLE}、{@link Opcodes#NULL} 或 {@link Opcodes#UNINITIALIZED_THIS} 表示（long 和 double 用单个元素表示）。引用类型使用字符串（内部名称），未初始化类型使用标签对象（表示对应的 NEW 指令）。
   * @param numStack 当前帧中操作数栈元素数量。
   * @param stack 当前帧中操作数栈元素类型数组，格式与 local 相同，不能修改。
   * @throws IllegalStateException 如果连续访问了两个帧且中间无指令（除非帧类型为 {@link Opcodes#F_SAME}，该情况被忽略）。
   */
  public void visitFrame(
      final int type,
      final int numLocal,
      final Object[] local,
      final int numStack,
      final Object[] stack) {
    if (mv != null) {
      mv.visitFrame(type, numLocal, local, numStack, stack);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 普通指令
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问无操作数指令。
   *
   * @param opcode 要访问的指令操作码。可能为 NOP、ACONST_NULL、ICONST_M1、ICONST_0、ICONST_1、ICONST_2、
   *               ICONST_3、ICONST_4、ICONST_5、LCONST_0、LCONST_1、FCONST_0、FCONST_1、FCONST_2、DCONST_0、
   *               DCONST_1、IALOAD、LALOAD、FALOAD、DALOAD、AALOAD、BALOAD、CALOAD、SALOAD、IASTORE、
   *               LASTORE、FASTORE、DASTORE、AASTORE、BASTORE、CASTORE、SASTORE、POP、POP2、DUP、DUP_X1、
   *               DUP_X2、DUP2、DUP2_X1、DUP2_X2、SWAP、IADD、LADD、FADD、DADD、ISUB、LSUB、FSUB、DSUB、
   *               IMUL、LMUL、FMUL、DMUL、IDIV、LDIV、FDIV、DDIV、IREM、LREM、FREM、DREM、INEG、LNEG、
   *               FNEG、DNEG、ISHL、LSHL、ISHR、LSHR、IUSHR、LUSHR、IAND、LAND、IOR、LOR、IXOR、LXOR、
   *               I2L、I2F、I2D、L2I、L2F、L2D、F2I、F2L、F2D、D2I、D2L、D2F、I2B、I2C、I2S、LCMP、
   *               FCMPL、FCMPG、DCMPL、DCMPG、IRETURN、LRETURN、FRETURN、DRETURN、ARETURN、RETURN、
   *               ARRAYLENGTH、ATHROW、MONITORENTER 或 MONITOREXIT。
   */
  public void visitInsn(final int opcode) {
    if (mv != null) {
      mv.visitInsn(opcode);
    }
  }

  /**
   * 访问带单个 int 操作数的指令。
   *
   * @param opcode 要访问的指令操作码。可能为 BIPUSH、SIPUSH 或 NEWARRAY。
   * @param operand 指令操作数。<br>
   *     当 opcode 为 BIPUSH 时，operand 应在 Byte.MIN_VALUE 到 Byte.MAX_VALUE 范围内。<br>
   *     当 opcode 为 SIPUSH 时，operand 应在 Short.MIN_VALUE 到 Short.MAX_VALUE 范围内。<br>
   *     当 opcode 为 NEWARRAY 时，operand 应为 {@link Opcodes#T_BOOLEAN}、{@link Opcodes#T_CHAR}、
   *     {@link Opcodes#T_FLOAT}、{@link Opcodes#T_DOUBLE}、{@link Opcodes#T_BYTE}、{@link Opcodes#T_SHORT}、
   *     {@link Opcodes#T_INT} 或 {@link Opcodes#T_LONG} 之一。
   */
  public void visitIntInsn(final int opcode, final int operand) {
    if (mv != null) {
      mv.visitIntInsn(opcode, operand);
    }
  }

  /**
   * 访问局部变量指令。局部变量指令是加载或存储局部变量值的指令。
   *
   * @param opcode 要访问的指令操作码。可能为 ILOAD、LLOAD、FLOAD、DLOAD、ALOAD、ISTORE、LSTORE、
   *               FSTORE、DSTORE、ASTORE 或 RET。
   * @param varIndex 指令操作数，即局部变量索引。
   */
  public void visitVarInsn(final int opcode, final int varIndex) {
    if (mv != null) {
      mv.visitVarInsn(opcode, varIndex);
    }
  }

  /**
   * 访问类型指令。类型指令是以类的内部名称为参数的指令。
   *
   * @param opcode 要访问的指令操作码。可能为 NEW、ANEWARRAY、CHECKCAST 或 INSTANCEOF。
   * @param type 指令操作数，即对象或数组类的内部名称（见 {@link Type#getInternalName()}）。
   */
  public void visitTypeInsn(final int opcode, final String type) {
    if (mv != null) {
      mv.visitTypeInsn(opcode, type);
    }
  }

  /**
   * 访问字段指令。字段指令是加载或存储对象字段值的指令。
   *
   * @param opcode 要访问的指令操作码。该操作码为 GETSTATIC、PUTSTATIC、GETFIELD 或 PUTFIELD 之一。
   * @param owner 字段所属类的内部名称（见 {@link Type#getInternalName()}）。
   * @param name 字段名。
   * @param descriptor 字段描述符（见 {@link Type}）。
   */
  public void visitFieldInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    if (mv != null) {
      mv.visitFieldInsn(opcode, owner, name, descriptor);
    }
  }

  /**
   * 访问方法指令。方法指令是调用方法的指令。
   *
   * @param opcode 要访问的指令操作码。该操作码为 INVOKEVIRTUAL、INVOKESPECIAL、INVOKESTATIC 或 INVOKEINTERFACE 之一。
   * @param owner 方法所属类的内部名称（见 {@link Type#getInternalName()}）。
   * @param name 方法名。
   * @param descriptor 方法描述符（见 {@link Type}）。
   * @deprecated 使用 {@link #visitMethodInsn(int, String, String, String, boolean)} 替代。
   */
  @Deprecated
  public void visitMethodInsn(
      final int opcode, final String owner, final String name, final String descriptor) {
    int opcodeAndSource = opcode | (api < Opcodes.ASM5 ? Opcodes.SOURCE_DEPRECATED : 0);
    visitMethodInsn(opcodeAndSource, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE);
  }

  /**
   * 访问方法指令。方法指令是调用方法的指令。
   *
   * @param opcode 要访问的指令操作码。该操作码为 INVOKEVIRTUAL、INVOKESPECIAL、INVOKESTATIC 或 INVOKEINTERFACE 之一。
   * @param owner 方法所属类的内部名称（见 {@link Type#getInternalName()}）。
   * @param name 方法名。
   * @param descriptor 方法描述符（见 {@link Type}）。
   * @param isInterface 方法所属类是否为接口。
   */
  public void visitMethodInsn(
      final int opcode,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    if (api < Opcodes.ASM5 && (opcode & Opcodes.SOURCE_DEPRECATED) == 0) {
      if (isInterface != (opcode == Opcodes.INVOKEINTERFACE)) {
        throw new UnsupportedOperationException("INVOKESPECIAL/STATIC on interfaces requires ASM5");
      }
      visitMethodInsn(opcode, owner, name, descriptor);
      return;
    }
    if (mv != null) {
      mv.visitMethodInsn(opcode & ~Opcodes.SOURCE_MASK, owner, name, descriptor, isInterface);
    }
  }

  /**
   * 访问 invokedynamic 指令。
   *
   * @param name 方法名。
   * @param descriptor 方法描述符（见 {@link Type}）。
   * @param bootstrapMethodHandle 启动方法句柄。
   * @param bootstrapMethodArguments 启动方法的常量参数。每个参数必须是 {@link Integer}、{@link Float}、{@link Long}、{@link Double}、
   *                                 {@link String}、{@link Type}、{@link Handle} 或 {@link ConstantDynamic}。此方法可能修改该数组内容，调用者应注意。
   */
  public void visitInvokeDynamicInsn(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }
  }

  /**
   * 访问跳转指令。跳转指令是可能跳转到另一条指令的指令。
   *
   * @param opcode 要访问的指令操作码。该操作码为 IFEQ、IFNE、IFLT、IFGE、IFGT、IFLE、IF_ICMPEQ、
   *               IF_ICMPNE、IF_ICMPLT、IF_ICMPGE、IF_ICMPGT、IF_ICMPLE、IF_ACMPEQ、IF_ACMPNE、
   *               GOTO、JSR、IFNULL 或 IFNONNULL 之一。
   * @param label 指令操作数，指定跳转目标指令的标签。
   */
  public void visitJumpInsn(final int opcode, final Label label) {
    if (mv != null) {
      mv.visitJumpInsn(opcode, label);
    }
  }

  /**
   * 访问标签。标签标识紧随其后的指令。
   *
   * @param label {@link Label} 对象。
   */
  public void visitLabel(final Label label) {
    if (mv != null) {
      mv.visitLabel(label);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 特殊指令
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问一个 LDC 指令。注意，未来的 Java 虚拟机版本可能会添加新的常量类型。
   * 为了方便检测新的常量类型，该方法的实现应检查未知的常量类型，如下示例：
   *
   * <pre>
   * if (cst instanceof Integer) {
   *     // ...
   * } else if (cst instanceof Float) {
   *     // ...
   * } else if (cst instanceof Long) {
   *     // ...
   * } else if (cst instanceof Double) {
   *     // ...
   * } else if (cst instanceof String) {
   *     // ...
   * } else if (cst instanceof Type) {
   *     int sort = ((Type) cst).getSort();
   *     if (sort == Type.OBJECT) {
   *         // ...
   *     } else if (sort == Type.ARRAY) {
   *         // ...
   *     } else if (sort == Type.METHOD) {
   *         // ...
   *     } else {
   *         // 抛出异常
   *     }
   * } else if (cst instanceof Handle) {
   *     // ...
   * } else if (cst instanceof ConstantDynamic) {
   *     // ...
   * } else {
   *     // 抛出异常
   * }
   * </pre>
   *
   * @param value 将被加载到栈上的常量。该参数必须是非空的 {@link Integer}、{@link Float}、{@link Long}、{@link Double}、
   *              {@link String}，或者对于版本为 49 的类，{@link Type}（OBJECT 或 ARRAY 类型，用于 .class 常量），
   *              对于 MethodType，为 METHOD 类型的 {@link Type}，对于版本为 51 的类，为 {@link Handle}，对于版本为 55 的类，为 {@link ConstantDynamic}。
   */
  public void visitLdcInsn(final Object value) {
    if (api < Opcodes.ASM5
        && (value instanceof Handle
            || (value instanceof Type && ((Type) value).getSort() == Type.METHOD))) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (api < Opcodes.ASM7 && value instanceof ConstantDynamic) {
      throw new UnsupportedOperationException("This feature requires ASM7");
    }
    if (mv != null) {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * 访问一个 IINC 指令。
   *
   * @param var 需要递增的局部变量索引。
   * @param increment 递增的量。
   */
  public void visitIincInsn(final int var, final int increment) {
    if (mv != null) {
      mv.visitIincInsn(var, increment);
    }
  }

  /**
   * 访问一个 TABLESWITCH 指令。
   *
   * @param min 最小的键值。
   * @param max 最大的键值。
   * @param dflt 默认处理块的起始位置。
   * @param labels 处理块的起始位置数组。labels[i] 是键值 min + i 对应的处理块起始位置。
   */
  public void visitTableSwitchInsn(
      final int min, final int max, final Label dflt, final Label... labels) {
    if (mv != null) {
      mv.visitTableSwitchInsn(min, max, dflt, labels);
    }
  }

  /**
   * 访问一个 LOOKUPSWITCH 指令。
   *
   * @param dflt 默认处理块的起始位置。
   * @param keys 键值数组。
   * @param labels 处理块的起始位置数组。labels[i] 是键值 keys[i] 对应的处理块起始位置。
   */
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    if (mv != null) {
      mv.visitLookupSwitchInsn(dflt, keys, labels);
    }
  }

  /**
   * 访问一个 MULTIANEWARRAY 指令。
   *
   * @param descriptor 数组类型描述符（参考 {@link Type}）。
   * @param numDimensions 要分配的数组维数。
   */
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    if (mv != null) {
      mv.visitMultiANewArrayInsn(descriptor, numDimensions);
    }
  }

  /**
   * 访问指令上的注解。此方法必须在被注解的指令 <i>之后</i> 调用。
   * 同一条指令可以多次调用此方法访问多个注解。
   *
   * @param typeRef 被注解类型的引用。该类型引用的种类必须是 {@link TypeReference#INSTANCEOF}、
   *                {@link TypeReference#NEW}、{@link TypeReference#CONSTRUCTOR_REFERENCE}、
   *                {@link TypeReference#METHOD_REFERENCE}、{@link TypeReference#CAST}、
   *                {@link TypeReference#CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、
   *                {@link TypeReference#METHOD_INVOCATION_TYPE_ARGUMENT}、
   *                {@link TypeReference#CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT} 或
   *                {@link TypeReference#METHOD_REFERENCE_TYPE_ARGUMENT}。
   * @param typePath 注解目标类型参数、通配符边界、数组元素类型或静态内部类的路径。
   *                 如果注解针对整个 'typeRef'，则可为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器，或者如果不关心此注解则返回 {@literal null}。
   */
  public AnnotationVisitor visitInsnAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  // -----------------------------------------------------------------------------------------------
  // 异常表条目、调试信息、最大栈深和最大局部变量数
  // -----------------------------------------------------------------------------------------------

  /**
   * 访问一个 try-catch 块。
   *
   * @param start 异常处理范围的开始位置（包含）。
   * @param end 异常处理范围的结束位置（不包含）。
   * @param handler 异常处理代码的开始位置。
   * @param type 异常处理的异常类型的内部名称，或 {@literal null} 表示捕获所有异常（例如 finally 块）。
   * @throws IllegalArgumentException 如果标签之一已被此访问器访问过（通过 {@link #visitLabel} 方法）。
   */
  public void visitTryCatchBlock(
      final Label start, final Label end, final Label handler, final String type) {
    if (mv != null) {
      mv.visitTryCatchBlock(start, end, handler, type);
    }
  }

  /**
   * 访问异常处理器类型上的注解。此方法必须在对应的 {@link #visitTryCatchBlock} 之后调用。
   * 同一异常处理器可以多次调用此方法访问多个注解。
   *
   * @param typeRef 被注解的类型引用。该引用的类型必须是 {@link TypeReference#EXCEPTION_PARAMETER}。
   * @param typePath 被注解类型参数、通配符边界、数组元素类型或静态内部类的路径。
   *                 如果注解作用于整个 'typeRef'，则可为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器，或如果不关心此注解则返回 {@literal null}。
   */
  public AnnotationVisitor visitTryCatchAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问局部变量声明。
   *
   * @param name 局部变量名。
   * @param descriptor 局部变量的类型描述符。
   * @param signature 局部变量的类型签名。如果类型不使用泛型，则可能为 {@literal null}。
   * @param start 局部变量作用域的起始指令（包含）。
   * @param end 局部变量作用域的结束指令（不包含）。
   * @param index 局部变量索引。
   * @throws IllegalArgumentException 如果标签之一尚未被访问（通过 {@link #visitLabel} 方法）。
   */
  public void visitLocalVariable(
      final String name,
      final String descriptor,
      final String signature,
      final Label start,
      final Label end,
      final int index) {
    if (mv != null) {
      mv.visitLocalVariable(name, descriptor, signature, start, end, index);
    }
  }

  /**
   * 访问局部变量类型上的注解。
   *
   * @param typeRef 被注解的类型引用。类型必须是 {@link TypeReference#LOCAL_VARIABLE} 或 {@link TypeReference#RESOURCE_VARIABLE}。
   * @param typePath 被注解类型参数、通配符边界、数组元素类型或静态内部类的路径。
   *                 如果注解作用于整个 'typeRef'，则可为 {@literal null}。
   * @param start 作用域连续范围的起始指令数组（包含）。
   * @param end 作用域连续范围的结束指令数组（不包含）。该数组大小必须与 start 相同。
   * @param index 每个范围内局部变量的索引数组。该数组大小必须与 start 相同。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见，则为 {@literal true}。
   * @return 用于访问注解值的访问器，或如果不关心此注解则返回 {@literal null}。
   */
  public AnnotationVisitor visitLocalVariableAnnotation(
      final int typeRef,
      final TypePath typePath,
      final Label[] start,
      final Label[] end,
      final int[] index,
      final String descriptor,
      final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException(REQUIRES_ASM5);
    }
    if (mv != null) {
      return mv.visitLocalVariableAnnotation(
          typeRef, typePath, start, end, index, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问行号声明。
   *
   * @param line 行号，指向编译该类的源文件中的对应行。
   * @param start 与此行号对应的第一个指令。
   * @throws IllegalArgumentException 如果 start 尚未被访问（通过 {@link #visitLabel} 方法）。
   */
  public void visitLineNumber(final int line, final Label start) {
    if (mv != null) {
      mv.visitLineNumber(line, start);
    }
  }

  /**
   * 访问方法的最大栈深和最大局部变量数量。
   *
   * @param maxStack 方法的最大栈深。
   * @param maxLocals 方法的最大局部变量数量。
   */
  public void visitMaxs(final int maxStack, final int maxLocals) {
    if (mv != null) {
      mv.visitMaxs(maxStack, maxLocals);
    }
  }

  /**
   * 访问方法结束。此方法是最后被调用的方法，
   * 用于通知访问器所有方法的注解和属性均已访问完毕。
   */
  public void visitEnd() {
    if (mv != null) {
      mv.visitEnd();
    }
  }
}
