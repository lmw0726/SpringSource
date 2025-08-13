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

final class RecordComponentWriter extends RecordComponentVisitor {
  /** 该 RecordComponentWriter 使用的常量存储表。 */
  private final SymbolTable symbolTable;

  // 注意：字段顺序按照 record_component_info 结构排列，属性相关字段按照 JVMS 4.7 节排序。

  /** Record 组件的 name_index 字段。 */
  private final int nameIndex;

  /** Record 组件的 descriptor_index 字段。 */
  private final int descriptorIndex;

  /**
   * Signature 属性的 signature_index 字段，如果没有 Signature 属性则为 0。
   */
  private int signatureIndex;

  /**
   * 最后一个运行时可见注解。之前的注解通过 {@link AnnotationWriter#previousAnnotation} 链接访问，可能为 null。
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 最后一个运行时不可见注解。之前的注解通过 {@link AnnotationWriter#previousAnnotation} 链接访问，可能为 null。
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * 最后一个运行时可见类型注解。之前的注解通过 {@link AnnotationWriter#previousAnnotation} 链接访问，可能为 null。
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 最后一个运行时不可见类型注解。之前的注解通过 {@link AnnotationWriter#previousAnnotation} 链接访问，可能为 null。
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * 第一个非标准属性。后续属性通过 {@link Attribute#nextAttribute} 链接访问，可能为 null。
   * <p><b>注意：</b>该链表以访问顺序的逆序存储属性，即 {@link #visitAttribute(Attribute)} 访问的最后一个属性是链表首元素。
   * {@link #putRecordComponentInfo(ByteVector)} 方法会按链表顺序写入属性，从而实现用户指定的顺序。
   */
  private Attribute firstAttribute;

  /**
   * 构造新的 RecordComponentWriter。
   *
   * @param symbolTable 用于存储常量的符号表。
   * @param name 记录组件名称。
   * @param descriptor 记录组件描述符（参见 {@link Type}）。
   * @param signature 记录组件签名，可为 null。
   */
  RecordComponentWriter(
      final SymbolTable symbolTable,
      final String name,
      final String descriptor,
      final String signature) {
    super(/* 最新 ASM 版本 */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
  }

  // -------------------------------------------------------------------------------
  // FieldVisitor 抽象类的方法实现
  // -------------------------------------------------------------------------------

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
    // 按访问顺序逆序存储属性链表
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public void visitEnd() {
    // 无需处理
  }

  // -------------------------------------------------------------------------------
  // 工具方法
  // -------------------------------------------------------------------------------

  /**
   * 计算此记录组件对应的 record_component_info 结构大小。
   * 同时将该组件的属性名称添加到常量池。
   *
   * @return record_component_info 结构的字节大小。
   */
  int computeRecordComponentInfoSize() {
    // name_index, descriptor_index 和 attributes_count 字段共 6 字节。
    int size = 6;
    size += Attribute.computeAttributesSize(symbolTable, 0, signatureIndex);
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
   * 将此记录组件的内容写入指定的 ByteVector 中。
   *
   * @param output 目标 ByteVector，写入 record_component_info 结构。
   */
  void putRecordComponentInfo(final ByteVector output) {
    output.putShort(nameIndex).putShort(descriptorIndex);
    // 计算并写入 attributes_count 字段。
    int attributesCount = 0;
    if (signatureIndex != 0) {
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
    Attribute.putAttributes(symbolTable, 0, signatureIndex, output);
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
   * 将此记录组件的所有属性添加到给定的属性原型集合中。
   *
   * @param attributePrototypes 属性原型集合。
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
  }
}
