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
 * 访问Java注解的访问者。此类的方法必须按以下顺序调用：
 * ( {@code visit} | {@code visitEnum} | {@code visitAnnotation} | {@code visitArray} )*
 * {@code visitEnd}。
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public abstract class AnnotationVisitor {

  /**
   * 此访问者实现的ASM API版本。此字段的值必须是{@link Opcodes}中的
   * {@code ASM}<i>x</i>值之一。
   */
  protected final int api;

  /**
   * 此访问者必须委托方法调用的注解访问者。可能为{@literal null}。
   */
  protected AnnotationVisitor av;

  /**
   * 构造一个新的{@link AnnotationVisitor}。
   *
   * @param api 此访问者实现的ASM API版本。必须是{@link Opcodes}中的
   *     {@code ASM}<i>x</i>值之一。
   */
  protected AnnotationVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的{@link AnnotationVisitor}。
   *
   * @param api 此访问者实现的ASM API版本。必须是{@link Opcodes}中的
   *     {@code ASM}<i>x</i>值之一。
   * @param annotationVisitor 此访问者必须委托方法调用的注解访问者。
   *     可能为{@literal null}。
   */
  protected AnnotationVisitor(final int api, final AnnotationVisitor annotationVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING补丁：对ASM实验性功能不进行预览模式检查
    this.api = api;
    this.av = annotationVisitor;
  }

  /**
   * 访问注解的基本类型值。
   *
   * @param name 值名称。
   * @param value 实际值，其类型必须是{@link Byte}、{@link Boolean}、{@link
   *     Character}、{@link Short}、{@link Integer}、{@link Long}、{@link Float}、{@link Double}、
   *     {@link String}或{@link Type#OBJECT}或{@link Type#ARRAY}类型的{@link Type}。
   *     此值也可以是byte、boolean、short、char、int、long、float或double值的数组
   *     （这相当于使用{@link #visitArray}并依次访问每个数组元素，但更方便）。
   */
  public void visit(final String name, final Object value) {
    if (av != null) {
      av.visit(name, value);
    }
  }

  /**
   * 访问注解的枚举值。
   *
   * @param name 值名称。
   * @param descriptor 枚举类的类描述符。
   * @param value 实际枚举值。
   */
  public void visitEnum(final String name, final String descriptor, final String value) {
    if (av != null) {
      av.visitEnum(name, descriptor, value);
    }
  }

  /**
   * 访问注解的嵌套注解值。
   *
   * @param name 值名称。
   * @param descriptor 嵌套注解类的类描述符。
   * @return 用于访问实际嵌套注解值的访问者，如果此访问者不感兴趣访问此嵌套注解则返回{@literal null}。
   *     <i>在调用此注解访问者的其他方法之前，必须完全访问嵌套注解值</i>。
   */
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    if (av != null) {
      return av.visitAnnotation(name, descriptor);
    }
    return null;
  }

  /**
   * 访问注解的数组值。注意基本类型值的数组（如byte、boolean、short、char、int、long、float或double）
   * 可以作为值传递给{@link #visit visit}。这就是{@link ClassReader}对非空基本类型值数组的处理方式。
   *
   * @param name 值名称。
   * @return 用于访问实际数组值元素的访问者，如果此访问者不感兴趣访问这些值则返回{@literal null}。
   *     传递给此访问者方法的'name'参数被忽略。<i>在调用此注解访问者的其他方法之前，
   *     必须访问所有数组值</i>。
   */
  public AnnotationVisitor visitArray(final String name) {
    if (av != null) {
      return av.visitArray(name);
    }
    return null;
  }

  /** 访问注解的结束。 */
  public void visitEnd() {
    if (av != null) {
      av.visitEnd();
    }
  }
}
