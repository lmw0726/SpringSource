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
 * 一个 {@link FieldVisitor}，用于生成对应的 'field_info' 结构，  
 * 该结构定义在 Java 虚拟机规范（JVMS）中。  
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5">JVMS
 *     4.5</a>
 * @author Eric Bruneton
 */
final class FieldWriter extends FieldVisitor {

  /** 该 FieldWriter 使用的常量存储位置。 */
  private final SymbolTable symbolTable;

  // 注意：字段顺序与 field_info 结构中一致，与属性相关的字段顺序与 JVMS 第4.7节一致。

  /**
   * field_info 结构中的 access_flags 字段。  
   * 该字段可包含 ASM 特定的访问标志，如 {@link Opcodes#ACC_DEPRECATED}，  
   * 生成 ClassFile 结构时会移除这些标志。  
   */
  private final int accessFlags;

  /** field_info 结构中的 name_index 字段。 */
  private final int nameIndex;

  /** field_info 结构中的 descriptor_index 字段。 */
  private final int descriptorIndex;

  /**
   * 该 field_info 的 Signature 属性的 signature_index 字段，  
   * 如果没有 Signature 属性，则为 0。  
   */
  private int signatureIndex;

  /**
   * 该 field_info 的 ConstantValue 属性的 constantvalue_index 字段，  
   * 如果没有 ConstantValue 属性，则为 0。  
   */
  private int constantValueIndex;

  /**
   * 该字段的最后一个运行时可见注解。  
   * 之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 访问，可能为 {@literal null}。  
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 该字段的最后一个运行时不可见注解。  
   * 之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 访问，可能为 {@literal null}。  
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * 该字段的最后一个运行时可见类型注解。  
   * 之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 访问，可能为 {@literal null}。  
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 该字段的最后一个运行时不可见类型注解。  
   * 之前的注解可以通过 {@link AnnotationWriter#previousAnnotation} 访问，可能为 {@literal null}。  
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * 该字段的第一个非标准属性。  
   * 后续属性可通过 {@link Attribute#nextAttribute} 访问，可能为 {@literal null}。  
   *
   * <p><b>注意：</b>此列表以访问顺序的 <i>逆序</i> 存储属性。  
   * firstAttribute 实际上是 {@link #visitAttribute} 中访问的最后一个属性。  
   * {@link #putFieldInfo} 方法按照此列表定义的顺序写入属性，即用户访问顺序的逆序。  
   */
  private Attribute firstAttribute;

  // -----------------------------------------------------------------------------------------------
  // 构造函数
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link FieldWriter} 实例。  
   *
   * @param symbolTable 该 FieldWriter 使用的常量存储表。  
   * @param access 字段访问标志（见 {@link Opcodes}）。  
   * @param name 字段名。  
   * @param descriptor 字段描述符（见 {@link Type}）。  
   * @param signature 字段签名，可能为 {@literal null}。  
   * @param constantValue 字段的常量值，可能为 {@literal null}。  
   */
  FieldWriter(
      final SymbolTable symbolTable,
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object constantValue) {
    super(/* 最新API版本 = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.accessFlags = access;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
    if (constantValue != null) {
      this.constantValueIndex = symbolTable.addConstant(constantValue).index;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // FieldVisitor 抽象类的实现
  // -----------------------------------------------------------------------------------------------

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
  public void visitAttribute(final Attribute attribute) {
    // 将属性以访问顺序的逆序存储
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public void visitEnd() {
    // 无需操作
  }

  // -----------------------------------------------------------------------------------------------
  // 工具方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回该 FieldWriter 生成的 field_info JVMS 结构的大小，同时将字段属性名添加到常量池中。  
   *
   * @return field_info 结构的字节大小。  
   */
  int computeFieldInfoSize() {
    // access_flags, name_index, descriptor_index 和 attributes_count 字段共用8字节
    int size = 8;
    // 按照 JVMS 4.7 节属性顺序方便引用
    if (constantValueIndex != 0) {
      // ConstantValue 属性固定使用8字节
      symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE);
      size += 8;
    }
    size += Attribute.computeAttributesSize(symbolTable, accessFlags, signatureIndex);
    size +=
        AnnotationWriter.computeAnnotationsSize(
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation);
    if (firstAttribute != null) {
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * 将该 FieldWriter 生成的 field_info JVMS 结构写入给定的 ByteVector。  
   *
   * @param output 存放 field_info 结构的 ByteVector。  
   */
  void putFieldInfo(final ByteVector output) {
    boolean useSyntheticAttribute = symbolTable.getMajorVersion() < Opcodes.V1_5;
    // 写入 access_flags, name_index 和 descriptor_index 字段
    int mask = useSyntheticAttribute ? Opcodes.ACC_SYNTHETIC : 0;
    output.putShort(accessFlags & ~mask).putShort(nameIndex).putShort(descriptorIndex);
    // 计算并写入 attributes_count 字段
    int attributesCount = 0;
    if (constantValueIndex != 0) {
      ++attributesCount;
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
      ++attributesCount;
    }
    if (signatureIndex != 0) {
      ++attributesCount;
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
    }
    output.putShort(attributesCount);
    // 写入 field_info 的属性，按照 JVMS 4.7 节的顺序
    if (constantValueIndex != 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE))
          .putInt(2)
          .putShort(constantValueIndex);
    }
    Attribute.putAttributes(symbolTable, accessFlags, signatureIndex, output);
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        output);
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }

  /**
   * 收集该字段的属性到给定的属性原型集合中。  
   *
   * @param attributePrototypes 属性原型集合。  
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
  }
}
