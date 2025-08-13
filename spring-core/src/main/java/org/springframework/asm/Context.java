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
 * 关于在 {@link ClassReader} 中被解析的类的信息。  
 *
 * @author Eric Bruneton
 */
final class Context {

  /** 必须在该类中解析的属性原型数组。 */
  Attribute[] attributePrototypes;

  /**
   * 解析该类时使用的选项。可能是 {@link ClassReader#SKIP_CODE}、{@link ClassReader#SKIP_DEBUG}、  
   * {@link ClassReader#SKIP_FRAMES}、{@link ClassReader#EXPAND_FRAMES} 或 {@link ClassReader#EXPAND_ASM_INSNS} 中的一个或多个。  
   */
  int parsingOptions;

  /** 用于读取常量池中字符串的缓冲区。 */
  char[] charBuffer;

  // 当前方法的信息，即在最近一次调用 {@link ClassReader#readMethod()} 时读取的方法。

  /** 当前方法的访问标志。 */
  int currentMethodAccessFlags;

  /** 当前方法的名称。 */
  String currentMethodName;

  /** 当前方法的描述符。 */
  String currentMethodDescriptor;

  /**
   * 当前方法的标签数组，按字节码偏移索引（只有需要标签的字节码偏移对应的 Label 非空）。  
   */
  Label[] currentMethodLabels;

  // 当前类型注解目标的信息，即在最近一次调用 {@link ClassReader#readAnnotationTarget()} 时读取的目标。

  /**
   * 当前类型注解目标的 target_type 和 target_info，按 {@link TypeReference} 中描述的格式编码。  
   */
  int currentTypeAnnotationTarget;

  /** 当前类型注解目标的 target_path。 */
  TypePath currentTypeAnnotationTargetPath;

  /** 当前局部变量注解中每个局部变量范围的开始标签。 */
  Label[] currentLocalVariableAnnotationRangeStarts;

  /** 当前局部变量注解中每个局部变量范围的结束标签。 */
  Label[] currentLocalVariableAnnotationRangeEnds;

  /** 当前局部变量注解中每个局部变量范围的局部变量索引。 */
  int[] currentLocalVariableAnnotationRangeIndices;

  // 当前栈映射帧的信息，即在最近一次调用 {@link ClassReader#readFrame()} 时读取的帧。

  /** 当前栈映射帧的字节码偏移量。 */
  int currentFrameOffset;

  /**
   * 当前栈映射帧的类型。可能是 {@link Opcodes#F_FULL}、{@link Opcodes#F_APPEND}、  
   * {@link Opcodes#F_CHOP}、{@link Opcodes#F_SAME} 或 {@link Opcodes#F_SAME1}。  
   */
  int currentFrameType;

  /**
   * 当前栈映射帧中局部变量类型的数量。每个类型用一个数组元素表示（即使是 long 和 double 也是如此）。  
   */
  int currentFrameLocalCount;

  /**
   * 当前栈映射帧中局部变量类型数量的增量（每个类型用一个数组元素表示，包括 long 和 double）。  
   * 该值为当前帧的局部变量类型数减去前一帧的局部变量类型数。  
   */
  int currentFrameLocalCountDelta;

  /**
   * 当前栈映射帧中局部变量的类型。每个类型用一个数组元素表示（包括 long 和 double），  
   * 使用 {@link MethodVisitor#visitFrame} 中描述的格式。  
   * 根据 {@link #currentFrameType}，该数组包含所有局部变量类型，或仅包含新增局部变量的类型。  
   */
  Object[] currentFrameLocalTypes;

  /**
   * 当前栈映射帧中栈元素类型的数量。每个类型用一个数组元素表示（包括 long 和 double）。  
   */
  int currentFrameStackCount;

  /**
   * 当前栈映射帧中栈元素的类型。每个类型用一个数组元素表示（包括 long 和 double），  
   * 使用 {@link MethodVisitor#visitFrame} 中描述的格式。  
   */
  Object[] currentFrameStackTypes;
}
