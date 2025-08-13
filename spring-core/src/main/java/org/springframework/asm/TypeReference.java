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
 * 类、字段或方法声明中，或在指令上的类型引用。
 * 此类引用指定了该类型在类中的出现位置（例如 'extends'、'implements' 或 'throws' 子句，
 * 'new' 指令、'catch' 子句、类型转换、局部变量声明等）。
 *
 * @author Eric Bruneton
 */
public class TypeReference {

  /**
   * 针对泛型类类型参数的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CLASS_TYPE_PARAMETER = 0x00;

  /**
   * 针对泛型方法类型参数的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_TYPE_PARAMETER = 0x01;

  /**
   * 针对类的超类或其实现的接口的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CLASS_EXTENDS = 0x10;

  /**
   * 针对泛型类类型参数的边界的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CLASS_TYPE_PARAMETER_BOUND = 0x11;

  /**
   * 针对泛型方法类型参数的边界的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_TYPE_PARAMETER_BOUND = 0x12;

  /** 针对字段类型的类型引用 sort。参见 {@link #getSort}。 */
  public static final int FIELD = 0x13;

  /** 针对方法返回值类型的类型引用 sort。参见 {@link #getSort}。 */
  public static final int METHOD_RETURN = 0x14;

  /**
   * 针对方法接收者类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_RECEIVER = 0x15;

  /**
   * 针对方法形参类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_FORMAL_PARAMETER = 0x16;

  /**
   * 针对方法 throws 子句中声明的异常类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int THROWS = 0x17;

  /**
   * 针对方法中局部变量类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int LOCAL_VARIABLE = 0x40;

  /**
   * 针对方法中资源变量类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int RESOURCE_VARIABLE = 0x41;

  /**
   * 针对方法中 'catch' 子句异常类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int EXCEPTION_PARAMETER = 0x42;

  /**
   * 针对 'instanceof' 指令中声明的类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int INSTANCEOF = 0x43;

  /**
   * 针对 'new' 指令创建的对象类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int NEW = 0x44;

  /**
   * 针对构造函数引用接收者类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CONSTRUCTOR_REFERENCE = 0x45;

  /**
   * 针对方法引用接收者类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_REFERENCE = 0x46;

  /**
   * 针对显式或隐式类型转换（cast）指令中声明类型的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CAST = 0x47;

  /**
   * 针对构造函数调用中泛型构造函数的类型参数的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;

  /**
   * 针对方法调用中泛型方法的类型参数的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;

  /**
   * 针对构造函数引用中泛型构造函数的类型参数的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;

  /**
   * 针对方法引用中泛型方法的类型参数的类型引用 sort。参见 {@link #getSort}。
   */
  public static final int METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

  /**
   * 与此类型引用对应的 target_type 和 target_info 结构 —— 按照 Java 虚拟机规范（JVMS）的定义。
   * target_type 占用 1 个字节，所有 target_info 联合体字段最多占用 3 个字节（除了 localvar_target，
   * 它通过特定方法 {@link MethodVisitor#visitLocalVariableAnnotation} 处理）。
   * 因此，这两个结构可以存储在一个 int 中。
   *
   * <p>该 int 字段在最高有效字节中存储 target_type（在该类的公共 API 中称为 TypeReference 的“sort”），
   * 后面依次是 target_info 字段。根据 target_type 的不同，该字段最低有效字节中的 1、2 或甚至 3 个字节可能未使用。
   * 引用字节码偏移量的 target_info 字段会被设置为 0（这些偏移量在 ClassReader 中会被忽略，
   * 并在 MethodWriter 中重新计算）。
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS 4.7.20</a>
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.1">JVMS 4.7.20.1</a>
   */
  private final int targetTypeAndInfo;

  /**
   * 构造一个新的 TypeReference。
   *
   * @param typeRef 类型引用的 int 编码值，在访问与类型注解相关的方法（如 {@link ClassVisitor#visitTypeAnnotation}）时接收。
   */
  public TypeReference(final int typeRef) {
    this.targetTypeAndInfo = typeRef;
  }

  /**
   * 返回指定 sort 的类型引用。
   *
   * @param sort 取值可以是 {@link #FIELD}、{@link #METHOD_RETURN}、{@link #METHOD_RECEIVER}、
   *             {@link #LOCAL_VARIABLE}、{@link #RESOURCE_VARIABLE}、{@link #INSTANCEOF}、
   *             {@link #NEW}、{@link #CONSTRUCTOR_REFERENCE} 或 {@link #METHOD_REFERENCE}。
   * @return 指定 sort 的类型引用。
   */
  public static TypeReference newTypeReference(final int sort) {
    return new TypeReference(sort << 24);
  }

  /**
   * 返回对泛型类或方法的某个类型参数的引用。
   *
   * @param sort 取值可以是 {@link #CLASS_TYPE_PARAMETER} 或 {@link #METHOD_TYPE_PARAMETER}。
   * @param paramIndex 类型参数索引。
   * @return 对指定泛型类或方法类型参数的引用。
   */
  public static TypeReference newTypeParameterReference(final int sort, final int paramIndex) {
    return new TypeReference((sort << 24) | (paramIndex << 16));
  }

  /**
   * 返回对泛型类或方法的某个类型参数边界的引用。
   *
   * @param sort 取值可以是 {@link #CLASS_TYPE_PARAMETER} 或 {@link #METHOD_TYPE_PARAMETER}。
   * @param paramIndex 类型参数索引。
   * @param boundIndex 在上述类型参数中的类型边界索引。
   * @return 对指定泛型类或方法类型参数边界的引用。
   */
  public static TypeReference newTypeParameterBoundReference(
      final int sort, final int paramIndex, final int boundIndex) {
    return new TypeReference((sort << 24) | (paramIndex << 16) | (boundIndex << 8));
  }

  /**
   * 返回对超类或类的 implements 子句中接口的引用。
   *
   * @param itfIndex 类的 implements 子句中接口的索引，若为 -1 则引用该类的超类。
   * @return 对指定类的超类型的引用。
   */
  public static TypeReference newSuperTypeReference(final int itfIndex) {
    return new TypeReference((CLASS_EXTENDS << 24) | ((itfIndex & 0xFFFF) << 8));
  }

  /**
   * 返回对方法形参类型的引用。
   *
   * @param paramIndex 方法形参索引。
   * @return 对指定方法形参类型的引用。
   */
  public static TypeReference newFormalParameterReference(final int paramIndex) {
    return new TypeReference((METHOD_FORMAL_PARAMETER << 24) | (paramIndex << 16));
  }

  /**
   * 返回对方法'throws'子句中异常类型的引用。
   *
   * @param exceptionIndex 方法'throws'子句中异常的索引。
   * @return 对给定异常类型的引用。
   */
  public static TypeReference newExceptionReference(final int exceptionIndex) {
    return new TypeReference((THROWS << 24) | (exceptionIndex << 8));
  }

  /**
   * 返回对方法'catch'子句中声明的异常类型的引用。
   *
   * @param tryCatchBlockIndex try catch块的索引（使用visitTryCatchBlock访问它们的顺序）。
   * @return 对给定异常类型的引用。
   */
  public static TypeReference newTryCatchReference(final int tryCatchBlockIndex) {
    return new TypeReference((EXCEPTION_PARAMETER << 24) | (tryCatchBlockIndex << 8));
  }

  /**
   * 返回对构造函数或方法调用或引用中类型参数的类型引用。
   *
   * @param sort {@link #CAST}、{@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、
   *     {@link #METHOD_INVOCATION_TYPE_ARGUMENT}、{@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}
   *     或{@link #METHOD_REFERENCE_TYPE_ARGUMENT}中的一个。
   * @param argIndex 类型参数索引。
   * @return 对给定类型参数的类型引用。
   */
  public static TypeReference newTypeArgumentReference(final int sort, final int argIndex) {
    return new TypeReference((sort << 24) | argIndex);
  }

  /**
   * 返回此类型引用的排序。
   *
   * @return {@link #CLASS_TYPE_PARAMETER}、{@link #METHOD_TYPE_PARAMETER}、{@link #CLASS_EXTENDS}、
   *     {@link #CLASS_TYPE_PARAMETER_BOUND}、{@link #METHOD_TYPE_PARAMETER_BOUND}、{@link #FIELD}、
   *     {@link #METHOD_RETURN}、{@link #METHOD_RECEIVER}、{@link #METHOD_FORMAL_PARAMETER}、
   *     {@link #THROWS}、{@link #LOCAL_VARIABLE}、{@link #RESOURCE_VARIABLE}、
   *     {@link #EXCEPTION_PARAMETER}、{@link #INSTANCEOF}、{@link #NEW}、
   *     {@link #CONSTRUCTOR_REFERENCE}、{@link #METHOD_REFERENCE}、{@link #CAST}、
   *     {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、{@link #METHOD_INVOCATION_TYPE_ARGUMENT}、
   *     {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}或{@link #METHOD_REFERENCE_TYPE_ARGUMENT}中的一个。
   */
  public int getSort() {
    return targetTypeAndInfo >>> 24;
  }

  /**
   * 返回此类型引用所引用的类型参数的索引。此方法只能用于排序为{@link #CLASS_TYPE_PARAMETER}、
   * {@link #METHOD_TYPE_PARAMETER}、{@link #CLASS_TYPE_PARAMETER_BOUND}或
   * {@link #METHOD_TYPE_PARAMETER_BOUND}的类型引用。
   *
   * @return 类型参数索引。
   */
  public int getTypeParameterIndex() {
    return (targetTypeAndInfo & 0x00FF0000) >> 16;
  }

  /**
   * 返回类型参数绑定的索引，该绑定在此类型引用所引用的类型参数{@link #getTypeParameterIndex}内。
   * 此方法只能用于排序为{@link #CLASS_TYPE_PARAMETER_BOUND}或{@link #METHOD_TYPE_PARAMETER_BOUND}的类型引用。
   *
   * @return 类型参数绑定索引。
   */
  public int getTypeParameterBoundIndex() {
    return (targetTypeAndInfo & 0x0000FF00) >> 8;
  }

  /**
   * 返回此类型引用所引用的类的"超类型"的索引。
   * 此方法只能用于排序为{@link #CLASS_EXTENDS}的类型引用。
   *
   * @return 类的'implements'子句中接口的索引，如果此类型引用引用超类的类型，则返回-1。
   */
  public int getSuperTypeIndex() {
    return (short) ((targetTypeAndInfo & 0x00FFFF00) >> 8);
  }

  /**
   * 返回此类型引用所引用的形式参数的索引。此方法只能用于排序为
   * {@link #METHOD_FORMAL_PARAMETER}的类型引用。
   *
   * @return 形式参数索引。
   */
  public int getFormalParameterIndex() {
    return (targetTypeAndInfo & 0x00FF0000) >> 16;
  }

  /**
   * 返回方法'throws'子句中异常的索引，该异常的类型由此类型引用所引用。
   * 此方法只能用于排序为{@link #THROWS}的类型引用。
   *
   * @return 方法'throws'子句中异常的索引。
   */
  public int getExceptionIndex() {
    return (targetTypeAndInfo & 0x00FFFF00) >> 8;
  }

  /**
   * 返回try catch块的索引（使用visitTryCatchBlock访问它们的顺序），其'catch'类型由此类型引用所引用。
   * 此方法只能用于排序为{@link #EXCEPTION_PARAMETER}的类型引用。
   *
   * @return 方法'throws'子句中异常的索引。
   */
  public int getTryCatchBlockIndex() {
    return (targetTypeAndInfo & 0x00FFFF00) >> 8;
  }

  /**
   * 返回此类型引用所引用的类型参数的索引。此方法只能用于排序为{@link #CAST}、
   * {@link #CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}、{@link #METHOD_INVOCATION_TYPE_ARGUMENT}、
   * {@link #CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}或{@link #METHOD_REFERENCE_TYPE_ARGUMENT}的类型引用。
   *
   * @return 类型参数索引。
   */
  public int getTypeArgumentIndex() {
    return targetTypeAndInfo & 0xFF;
  }

  /**
   * 返回此类型引用的int编码值，适合在与类型注解相关的访问方法中使用，如visitTypeAnnotation。
   *
   * @return 此类型引用的int编码值。
   */
  public int getValue() {
    return targetTypeAndInfo;
  }

  /**
   * 将给定的target_type和target_info JVMS结构放入给定的ByteVector中。
   *
   * @param targetTypeAndInfo 按照{@link #targetTypeAndInfo}编码的target_type和target_info结构。
   *     不支持LOCAL_VARIABLE和RESOURCE_VARIABLE目标类型。
   * @param output 必须放入类型引用的地方。
   */
  static void putTarget(final int targetTypeAndInfo, final ByteVector output) {
    switch (targetTypeAndInfo >>> 24) {
      case CLASS_TYPE_PARAMETER:
      case METHOD_TYPE_PARAMETER:
      case METHOD_FORMAL_PARAMETER:
        output.putShort(targetTypeAndInfo >>> 16);
        break;
      case FIELD:
      case METHOD_RETURN:
      case METHOD_RECEIVER:
        output.putByte(targetTypeAndInfo >>> 24);
        break;
      case CAST:
      case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
      case METHOD_INVOCATION_TYPE_ARGUMENT:
      case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
      case METHOD_REFERENCE_TYPE_ARGUMENT:
        output.putInt(targetTypeAndInfo);
        break;
      case CLASS_EXTENDS:
      case CLASS_TYPE_PARAMETER_BOUND:
      case METHOD_TYPE_PARAMETER_BOUND:
      case THROWS:
      case EXCEPTION_PARAMETER:
      case INSTANCEOF:
      case NEW:
      case CONSTRUCTOR_REFERENCE:
      case METHOD_REFERENCE:
        output.put12(targetTypeAndInfo >>> 24, (targetTypeAndInfo & 0xFFFF00) >> 8);
        break;
      default:
        throw new IllegalArgumentException();
    }
  }
}
