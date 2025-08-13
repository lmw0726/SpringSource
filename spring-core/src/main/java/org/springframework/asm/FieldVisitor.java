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
 * 访问 Java 字段的访问者。  
 * 此类的方法必须按如下顺序调用：  
 * ( {@code visitAnnotation} | {@code visitTypeAnnotation} | {@code visitAttribute} )* {@code visitEnd}。  
 *
 * @author Eric Bruneton
 */
public abstract class FieldVisitor {

  /**
   * 此访问者实现的 ASM API 版本。  
   * 该字段的值必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 之一。  
   */
  protected final int api;

  /** 该访问者委托方法调用的下一个字段访问者。可能为 {@literal null}。 */
  protected FieldVisitor fv;

  /**
   * 构造一个新的 {@link FieldVisitor}。  
   *
   * @param api 此访问者实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 之一。  
   */
  protected FieldVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link FieldVisitor}。  
   *
   * @param api 此访问者实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 之一。  
   * @param fieldVisitor 委托方法调用的字段访问者，可能为 null。  
   */
  protected FieldVisitor(final int api, final FieldVisitor fieldVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: ASM experimental 版本不检查预览模式
    this.api = api;
    this.fv = fieldVisitor;
  }

  /**
   * 访问字段上的注解。  
   *
   * @param descriptor 注解类的描述符。  
   * @param visible 如果注解在运行时可见，则为 {@literal true}。  
   * @return 访问注解值的访问者，或者如果不感兴趣则返回 {@literal null}。  
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (fv != null) {
      return fv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问字段类型上的注解。  
   *
   * @param typeRef 被注解的类型引用，该引用的类型必须是 {@link TypeReference#FIELD}。参见 {@link TypeReference}。  
   * @param typePath 被注解的类型参数、通配符边界、数组元素类型或静态内部类型的路径。  
   *                 如果注解作用于整个 typeRef，则可能为 {@literal null}。  
   * @param descriptor 注解类的描述符。  
   * @param visible 如果注解在运行时可见，则为 {@literal true}。  
   * @return 访问注解值的访问者，或者如果不感兴趣则返回 {@literal null}。  
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException("This feature requires ASM5");
    }
    if (fv != null) {
      return fv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问字段的非标准属性。  
   *
   * @param attribute 一个属性。  
   */
  public void visitAttribute(final Attribute attribute) {
    if (fv != null) {
      fv.visitAttribute(attribute);
    }
  }

  /**
   * 访问字段的结束。  
   * 该方法是最后被调用的方法，通知访问者字段的所有注解和属性均已访问完毕。  
   */
  public void visitEnd() {
    if (fv != null) {
      fv.visitEnd();
    }
  }
}
