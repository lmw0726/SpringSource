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
 * 一个非标准的类、字段、方法或Code属性，如Java虚拟机规范(JVMS)中所定义。
 *
 * @see <a href= "https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7">JVMS
 *     4.7</a>
 * @see <a href= "https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3">JVMS
 *     4.7.3</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class Attribute {

  /** 此属性的类型，在JVMS中也称为其名称。 */
  public final String type;

  /**
   * 此属性的原始内容，仅用于未知属性（参见{@link #isUnknown()}）。
   * 属性的6个头字节（attribute_name_index和attribute_length）<i>不</i>包含在内。
   */
  private byte[] content;

  /**
   * 此属性列表中的下一个属性（Attribute实例可以通过此字段链接以存储类、字段、方法或Code属性的列表）。
   * 可能为{@literal null}。
   */
  Attribute nextAttribute;

  /**
   * 构造一个新的空属性。
   *
   * @param type 属性的类型。
   */
  protected Attribute(final String type) {
    this.type = type;
  }

  /**
   * 如果此类型的属性是未知的，则返回{@literal true}。这意味着无法解析属性内容以提取常量池引用、标签等。
   * 相反，属性内容被作为不透明字节数组读取，并按原样写回。如果内容实际包含常量池引用、标签或其他符号引用，
   * 且当常量池、方法字节码等发生变化时需要更新，这可能导致无效属性。此方法的默认实现始终返回{@literal true}。
   *
   * @return 如果此类型的属性是未知的，则返回{@literal true}。
   */
  public boolean isUnknown() {
    return true;
  }

  /**
   * 如果此类型的属性是Code属性，则返回{@literal true}。
   *
   * @return 如果此类型的属性是Code属性，则返回{@literal true}。
   */
  public boolean isCodeAttribute() {
    return false;
  }

  /**
   * 返回对应此属性的标签。
   *
   * @return 对应此属性的标签，如果此属性不是包含标签的Code属性，则返回{@literal null}。
   */
  protected Label[] getLabels() {
    return new Label[0];
  }

  /**
   * 读取一个{@link #type}属性。此方法必须返回一个<i>新的</i>{@link Attribute}对象，
   * 类型为{@link #type}，对应给定ClassReader中从'offset'开始的'length'个字节。
   *
   * @param classReader 包含要读取的属性的类。
   * @param offset 属性内容在{@link ClassReader}中第一个字节的索引。这里不考虑6个属性头字节
   *     （attribute_name_index和attribute_length）。
   * @param length 属性内容的长度（不包括6个属性头字节）。
   * @param charBuffer 用于调用需要'charBuffer'参数的ClassReader方法的缓冲区。
   * @param codeAttributeOffset 包围此属性的Code属性内容在{@link ClassReader}中第一个字节的索引，
   *     如果要读取的属性不是Code属性，则为-1。这里不考虑6个属性头字节
   *     （attribute_name_index和attribute_length）。
   * @param labels 方法代码的标签，如果要读取的属性不是Code属性，则为{@literal null}。
   * @return 对应指定字节的<i>新</i>{@link Attribute}对象。
   */
  protected Attribute read(
      final ClassReader classReader,
      final int offset,
      final int length,
      final char[] charBuffer,
      final int codeAttributeOffset,
      final Label[] labels) {
    Attribute attribute = new Attribute(type);
    attribute.content = new byte[length];
    System.arraycopy(classReader.classFileBuffer, offset, attribute.content, 0, length);
    return attribute;
  }

  /**
   * 返回此属性内容的字节数组形式。返回的ByteVector中<i>不得</i>添加6个头字节
   * （attribute_name_index和attribute_length）。
   *
   * @param classWriter 必须添加此属性的类。此参数可用于将对应此属性的项添加到此类的常量池中。
   * @param code 对应此Code属性的方法的字节码，如果此属性不是Code属性，则为{@literal null}。
   *     对应Code属性的'code'字段。
   * @param codeLength 对应此Code属性的方法字节码的长度，如果此属性不是Code属性，则为0。
   *     对应Code属性的'code_length'字段。
   * @param maxStack 对应此Code属性的方法的最大栈大小，如果此属性不是Code属性，则为-1。
   * @param maxLocals 对应此Code属性的方法的本地变量最大数量，如果此属性不是Code属性，则为-1。
   * @return 此属性的字节数组形式。
   */
  protected ByteVector write(
      final ClassWriter classWriter,
      final byte[] code,
      final int codeLength,
      final int maxStack,
      final int maxLocals) {
    return new ByteVector(content);
  }

  /**
   * 返回以此属性开始的属性列表中的属性数量。
   *
   * @return 以此属性开始的属性列表中的属性数量。
   */
  final int getAttributeCount() {
    int count = 0;
    Attribute attribute = this;
    while (attribute != null) {
      count += 1;
      attribute = attribute.nextAttribute;
    }
    return count;
  }

  /**
   * 返回以此属性开始的属性列表中所有属性的总字节大小。此大小包括每个属性的6个头字节
   * （attribute_name_index和attribute_length）。同时将属性类型名称添加到常量池。
   *
   * @param symbolTable 必须存储属性中使用的常量的地方。
   * @return 此属性列表中所有属性的大小。此大小包括属性头的大小。
   */
  final int computeAttributesSize(final SymbolTable symbolTable) {
    final byte[] code = null;
    final int codeLength = 0;
    final int maxStack = -1;
    final int maxLocals = -1;
    return computeAttributesSize(symbolTable, code, codeLength, maxStack, maxLocals);
  }

  /**
   * 返回以此属性开始的属性列表中所有属性的总字节大小。此大小包括每个属性的6个头字节
   * （attribute_name_index和attribute_length）。同时将属性类型名称添加到常量池。
   *
   * @param symbolTable 必须存储属性中使用的常量的地方。
   * @param code 对应这些Code属性的方法的字节码，如果它们不是Code属性，则为{@literal null}。
   *     对应Code属性的'code'字段。
   * @param codeLength 对应这些Code属性的方法字节码的长度，如果它们不是Code属性，则为0。
   *     对应Code属性的'code_length'字段。
   * @param maxStack 对应这些Code属性的方法的最大栈大小，如果它们不是Code属性，则为-1。
   * @param maxLocals 对应这些Code属性的方法的本地变量最大数量，如果它们不是Code属性，则为-1。
   * @return 此属性列表中所有属性的大小。此大小包括属性头的大小。
   */
  final int computeAttributesSize(
      final SymbolTable symbolTable,
      final byte[] code,
      final int codeLength,
      final int maxStack,
      final int maxLocals) {
    final ClassWriter classWriter = symbolTable.classWriter;
    int size = 0;
    Attribute attribute = this;
    while (attribute != null) {
      symbolTable.addConstantUtf8(attribute.type);
      size += 6 + attribute.write(classWriter, code, codeLength, maxStack, maxLocals).length;
      attribute = attribute.nextAttribute;
    }
    return size;
  }

  /**
   * 返回对应给定字段、方法或类访问标志和签名的所有属性的总字节大小。此大小包括每个属性的
   * 6个头字节（attribute_name_index和attribute_length）。同时将属性类型名称添加到常量池。
   *
   * @param symbolTable 必须存储属性中使用的常量的地方。
   * @param accessFlags 一些字段、方法或类的访问标志。
   * @param signatureIndex 字段、方法或类签名的常量池索引。
   * @return 所有属性的字节大小。此大小包括属性头的大小。
   */
  static int computeAttributesSize(
      final SymbolTable symbolTable, final int accessFlags, final int signatureIndex) {
    int size = 0;
    // 在Java 1.5之前，合成字段用Synthetic属性表示。
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0
        && symbolTable.getMajorVersion() < Opcodes.V1_5) {
      // Synthetic属性始终使用6个字节。
      symbolTable.addConstantUtf8(Constants.SYNTHETIC);
      size += 6;
    }
    if (signatureIndex != 0) {
      // Signature属性始终使用8个字节。
      symbolTable.addConstantUtf8(Constants.SIGNATURE);
      size += 8;
    }
    // ACC_DEPRECATED是ASM特有的，ClassFile格式使用Deprecated属性代替。
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      // Deprecated属性始终使用6个字节。
      symbolTable.addConstantUtf8(Constants.DEPRECATED);
      size += 6;
    }
    return size;
  }

  /**
   * 将以此属性开始的属性列表中的所有属性放入给定的字节向量中。这包括每个属性的
   * 6个头字节（attribute_name_index和attribute_length）。
   *
   * @param symbolTable 必须存储属性中使用的常量的地方。
   * @param output 必须写入属性的地方。
   */
  final void putAttributes(final SymbolTable symbolTable, final ByteVector output) {
    final byte[] code = null;
    final int codeLength = 0;
    final int maxStack = -1;
    final int maxLocals = -1;
    putAttributes(symbolTable, code, codeLength, maxStack, maxLocals, output);
  }

  /**
   * 将以此属性开始的属性列表中的所有属性放入给定的字节向量中。这包括每个属性的
   * 6个头字节（attribute_name_index和attribute_length）。
   *
   * @param symbolTable 必须存储属性中使用的常量的地方。
   * @param code 对应这些Code属性的方法的字节码，如果它们不是Code属性，则为{@literal null}。
   *     对应Code属性的'code'字段。
   * @param codeLength 对应这些Code属性的方法字节码的长度，如果它们不是Code属性，则为0。
   *     对应Code属性的'code_length'字段。
   * @param maxStack 对应这些Code属性的方法的最大栈大小，如果它们不是Code属性，则为-1。
   * @param maxLocals 对应这些Code属性的方法的本地变量最大数量，如果它们不是Code属性，则为-1。
   * @param output 必须写入属性的地方。
   */
  final void putAttributes(
      final SymbolTable symbolTable,
      final byte[] code,
      final int codeLength,
      final int maxStack,
      final int maxLocals,
      final ByteVector output) {
    final ClassWriter classWriter = symbolTable.classWriter;
    Attribute attribute = this;
    while (attribute != null) {
      ByteVector attributeContent =
          attribute.write(classWriter, code, codeLength, maxStack, maxLocals);
      // 放入attribute_name_index和attribute_length。
      output.putShort(symbolTable.addConstantUtf8(attribute.type)).putInt(attributeContent.length);
      output.putByteArray(attributeContent.data, 0, attributeContent.length);
      attribute = attribute.nextAttribute;
    }
  }

  /**
   * 将对应给定字段、方法或类访问标志和签名的所有属性放入给定的字节向量中。这包括每个属性的
   * 6个头字节（attribute_name_index和attribute_length）。
   *
   * @param symbolTable 必须存储属性中使用的常量的地方。
   * @param accessFlags 一些字段、方法或类的访问标志。
   * @param signatureIndex 字段、方法或类签名的常量池索引。
   * @param output 必须写入属性的地方。
   */
  static void putAttributes(
      final SymbolTable symbolTable,
      final int accessFlags,
      final int signatureIndex,
      final ByteVector output) {
    // 在Java 1.5之前，合成字段用Synthetic属性表示。
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0
        && symbolTable.getMajorVersion() < Opcodes.V1_5) {
      output.putShort(symbolTable.addConstantUtf8(Constants.SYNTHETIC)).putInt(0);
    }
    if (signatureIndex != 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.SIGNATURE))
          .putInt(2)
          .putShort(signatureIndex);
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      output.putShort(symbolTable.addConstantUtf8(Constants.DEPRECATED)).putInt(0);
    }
  }

  /** 一组属性原型集合（具有相同类型的属性被认为是相等的）。 */
  static final class Set {

    private static final int SIZE_INCREMENT = 6;

    private int size;
    private Attribute[] data = new Attribute[SIZE_INCREMENT];

    void addAttributes(final Attribute attributeList) {
      Attribute attribute = attributeList;
      while (attribute != null) {
        if (!contains(attribute)) {
          add(attribute);
        }
        attribute = attribute.nextAttribute;
      }
    }

    Attribute[] toArray() {
      Attribute[] result = new Attribute[size];
      System.arraycopy(data, 0, result, 0, size);
      return result;
    }

    private boolean contains(final Attribute attribute) {
      for (int i = 0; i < size; ++i) {
        if (data[i].type.equals(attribute.type)) {
          return true;
        }
      }
      return false;
    }

    private void add(final Attribute attribute) {
      if (size >= data.length) {
        Attribute[] newData = new Attribute[data.length + SIZE_INCREMENT];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
      }
      data[size++] = attribute;
    }
  }
}
