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
 * 一个{@link AnnotationVisitor}，生成相应的'annotation'或'type_annotation'结构，
 * 如Java虚拟机规范（JVMS）中定义的那样。AnnotationWriter实例可以链接在双向链表中，
 * 通过{@link #putAnnotations}方法可以从中生成Runtime[In]Visible[Type]Annotations属性。
 * 类似地，这种列表的数组可以用来生成Runtime[In]VisibleParameterAnnotations属性。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16">JVMS
 *     4.7.16</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
 *     4.7.20</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
final class AnnotationWriter extends AnnotationVisitor {

  /** 此AnnotationWriter中使用的常量必须存储的位置。 */
  private final SymbolTable symbolTable;

  /**
   * 值是否命名。用于注解默认值和注解数组的AnnotationWriter实例使用未命名值
   * （即为每个值生成一个'element_value'结构，而不是element_name_index后跟element_value）。
   */
  private final boolean useNamedValues;

  /**
   * 对应于到目前为止访问的注解值的'annotation'或'type_annotation' JVMS结构。
   * 这些结构的所有字段（除了最后一个——element_value_pairs数组）必须在此ByteVector
   * 传递给构造器之前设置（num_element_value_pairs可以设置为0，在{@link #visitEnd()}
   * 中会重置为正确值）。element_value_pairs数组在各种visit()方法中增量填充。
   *
   * <p>注意：作为上述规则的例外，对于AnnotationDefault属性（根据定义包含单个element_value），
   * 传递给构造器时此ByteVector初始为空，且{@link #numElementValuePairsOffset}设置为-1。
   */
  private final ByteVector annotation;

  /**
   * {@link #annotation}中必须存储{@link #numElementValuePairs}的偏移量
   * （对于AnnotationDefault属性的情况为-1）。
   */
  private final int numElementValuePairsOffset;

  /** 到目前为止访问的元素值对数量。 */
  private int numElementValuePairs;

  /**
   * 前一个AnnotationWriter。此字段用于存储Runtime[In]Visible[Type]Annotations属性
   * 的注解列表。对于嵌套或数组注解（注解类型的注解值）或AnnotationDefault属性，此字段未使用。
   */
  private final AnnotationWriter previousAnnotation;

  /**
   * 下一个AnnotationWriter。此字段用于存储Runtime[In]Visible[Type]Annotations属性
   * 的注解列表。对于嵌套或数组注解（注解类型的注解值）或AnnotationDefault属性，此字段未使用。
   */
  private AnnotationWriter nextAnnotation;

  // -----------------------------------------------------------------------------------------------
  // 构造器和工厂方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的{@link AnnotationWriter}。
   *
   * @param symbolTable 此AnnotationWriter中使用的常量必须存储的位置。
   * @param useNamedValues 值是否命名。AnnotationDefault和注解数组使用未命名值。
   * @param annotation 对应于访问内容的'annotation'或'type_annotation' JVMS结构
   *     必须存储的位置。此ByteVector必须已包含结构的所有字段，除了最后一个
   *     （element_value_pairs数组）。
   * @param previousAnnotation 此注解所属的Runtime[In]Visible[Type]Annotations属性
   *     中之前访问的注解，或在其他情况下（例如嵌套或数组注解）为{@literal null}。
   */
  AnnotationWriter(
      final SymbolTable symbolTable,
      final boolean useNamedValues,
      final ByteVector annotation,
      final AnnotationWriter previousAnnotation) {
    super(/* latest api = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.useNamedValues = useNamedValues;
    this.annotation = annotation;
    // 根据假设，num_element_value_pairs存储在'annotation'的最后一个无符号短整型中。
    this.numElementValuePairsOffset = annotation.length == 0 ? -1 : annotation.length - 2;
    this.previousAnnotation = previousAnnotation;
    if (previousAnnotation != null) {
      previousAnnotation.nextAnnotation = this;
    }
  }

  /**
   * 使用命名值创建一个新的{@link AnnotationWriter}。
   *
   * @param symbolTable 此AnnotationWriter中使用的常量必须存储的位置。
   * @param descriptor 注解类的类描述符。
   * @param previousAnnotation 此注解所属的Runtime[In]Visible[Type]Annotations属性
   *     中之前访问的注解，或在其他情况下（例如嵌套或数组注解）为{@literal null}。
   * @return 给定注解描述符的新{@link AnnotationWriter}。
   */
  static AnnotationWriter create(
      final SymbolTable symbolTable,
      final String descriptor,
      final AnnotationWriter previousAnnotation) {
    // 创建一个ByteVector来保存'annotation' JVMS结构。
    // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16。
    ByteVector annotation = new ByteVector();
    // 写入type_index并为num_element_value_pairs保留空间。
    annotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(
        symbolTable, /* useNamedValues = */ true, annotation, previousAnnotation);
  }

  /**
   * 使用命名值创建一个新的{@link AnnotationWriter}。
   *
   * @param symbolTable 此AnnotationWriter中使用的常量必须存储的位置。
   * @param typeRef 对被注解类型的引用。此类型引用的排序必须是
   *     {@link TypeReference#CLASS_TYPE_PARAMETER}、{@link
   *     TypeReference#CLASS_TYPE_PARAMETER_BOUND}或{@link TypeReference#CLASS_EXTENDS}。
   *     参见{@link TypeReference}。
   * @param typePath 到被注解的类型参数、通配符边界、数组元素类型或'typeRef'内静态内部类型的路径。
   *     如果注解以整体为目标'typeRef'，可能为{@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param previousAnnotation 此注解所属的Runtime[In]Visible[Type]Annotations属性
   *     中之前访问的注解，或在其他情况下（例如嵌套或数组注解）为{@literal null}。
   * @return 给定类型注解引用和描述符的新{@link AnnotationWriter}。
   */
  static AnnotationWriter create(
      final SymbolTable symbolTable,
      final int typeRef,
      final TypePath typePath,
      final String descriptor,
      final AnnotationWriter previousAnnotation) {
    // 创建一个ByteVector来保存'type_annotation' JVMS结构。
    // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20。
    ByteVector typeAnnotation = new ByteVector();
    // 写入target_type、target_info和target_path。
    TypeReference.putTarget(typeRef, typeAnnotation);
    TypePath.put(typePath, typeAnnotation);
    // 写入type_index并为num_element_value_pairs保留空间。
    typeAnnotation.putShort(symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(
        symbolTable, /* useNamedValues = */ true, typeAnnotation, previousAnnotation);
  }

  // -----------------------------------------------------------------------------------------------
  // AnnotationVisitor抽象类的实现
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(final String name, final Object value) {
    // 具有const_value_index、class_info_index或array_index字段的element_value的情况。
    // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1。
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    if (value instanceof String) {
      annotation.put12('s', symbolTable.addConstantUtf8((String) value));
    } else if (value instanceof Byte) {
      annotation.put12('B', symbolTable.addConstantInteger(((Byte) value).byteValue()).index);
    } else if (value instanceof Boolean) {
      int booleanValue = ((Boolean) value).booleanValue() ? 1 : 0;
      annotation.put12('Z', symbolTable.addConstantInteger(booleanValue).index);
    } else if (value instanceof Character) {
      annotation.put12('C', symbolTable.addConstantInteger(((Character) value).charValue()).index);
    } else if (value instanceof Short) {
      annotation.put12('S', symbolTable.addConstantInteger(((Short) value).shortValue()).index);
    } else if (value instanceof Type) {
      annotation.put12('c', symbolTable.addConstantUtf8(((Type) value).getDescriptor()));
    } else if (value instanceof byte[]) {
      byte[] byteArray = (byte[]) value;
      annotation.put12('[', byteArray.length);
      for (byte byteValue : byteArray) {
        annotation.put12('B', symbolTable.addConstantInteger(byteValue).index);
      }
    } else if (value instanceof boolean[]) {
      boolean[] booleanArray = (boolean[]) value;
      annotation.put12('[', booleanArray.length);
      for (boolean booleanValue : booleanArray) {
        annotation.put12('Z', symbolTable.addConstantInteger(booleanValue ? 1 : 0).index);
      }
    } else if (value instanceof short[]) {
      short[] shortArray = (short[]) value;
      annotation.put12('[', shortArray.length);
      for (short shortValue : shortArray) {
        annotation.put12('S', symbolTable.addConstantInteger(shortValue).index);
      }
    } else if (value instanceof char[]) {
      char[] charArray = (char[]) value;
      annotation.put12('[', charArray.length);
      for (char charValue : charArray) {
        annotation.put12('C', symbolTable.addConstantInteger(charValue).index);
      }
    } else if (value instanceof int[]) {
      int[] intArray = (int[]) value;
      annotation.put12('[', intArray.length);
      for (int intValue : intArray) {
        annotation.put12('I', symbolTable.addConstantInteger(intValue).index);
      }
    } else if (value instanceof long[]) {
      long[] longArray = (long[]) value;
      annotation.put12('[', longArray.length);
      for (long longValue : longArray) {
        annotation.put12('J', symbolTable.addConstantLong(longValue).index);
      }
    } else if (value instanceof float[]) {
      float[] floatArray = (float[]) value;
      annotation.put12('[', floatArray.length);
      for (float floatValue : floatArray) {
        annotation.put12('F', symbolTable.addConstantFloat(floatValue).index);
      }
    } else if (value instanceof double[]) {
      double[] doubleArray = (double[]) value;
      annotation.put12('[', doubleArray.length);
      for (double doubleValue : doubleArray) {
        annotation.put12('D', symbolTable.addConstantDouble(doubleValue).index);
      }
    } else {
      Symbol symbol = symbolTable.addConstant(value);
      annotation.put12(".s.IFJDCS".charAt(symbol.tag), symbol.index);
    }
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    // 具有enum_const_value字段的element_value的情况。
    // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1。
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    annotation
        .put12('e', symbolTable.addConstantUtf8(descriptor))
        .putShort(symbolTable.addConstantUtf8(value));
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    // 具有annotation_value字段的element_value的情况。
    // 参见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1。
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    // 写入标签和type_index，并为num_element_value_pairs保留2字节。
    annotation.put12('@', symbolTable.addConstantUtf8(descriptor)).putShort(0);
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ true, annotation, null);
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    // 具有array_value字段的element_value的情况。
    // https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1
    ++numElementValuePairs;
    if (useNamedValues) {
      annotation.putShort(symbolTable.addConstantUtf8(name));
    }
    // 写入标签，并为num_values保留2字节。这里我们利用了数组类型的element_value结尾
    // 与'annotation'结构的结尾相似这一事实：一个无符号短整型num_values后跟num_values个element_value，
    // 相对于一个无符号短整型num_element_value_pairs，后跟num_element_value_pairs个
    // { element_name_index, element_value }元组。这允许我们使用具有未命名值的AnnotationWriter
    // 来访问数组元素。其num_element_value_pairs将对应于数组元素的数量，并将存储在实际的num_values中。
    annotation.put12('[', 0);
    return new AnnotationWriter(symbolTable, /* useNamedValues = */ false, annotation, null);
  }

  @Override
  public void visitEnd() {
    if (numElementValuePairsOffset != -1) {
      byte[] data = annotation.data;
      data[numElementValuePairsOffset] = (byte) (numElementValuePairs >>> 8);
      data[numElementValuePairsOffset + 1] = (byte) numElementValuePairs;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 工具方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回包含此注解及其所有<i>前驱</i>（参见{@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations属性的大小。
   * 同时将属性名添加到类的常量池中（如果不为null）。
   *
   * @param attributeName "Runtime[In]Visible[Type]Annotations"之一，或{@literal null}。
   * @return 包含此注解及其所有前驱的Runtime[In]Visible[Type]Annotations属性的字节大小。
   *     这包括attribute_name_index和attribute_length字段的大小。
   */
  int computeAnnotationsSize(final String attributeName) {
    if (attributeName != null) {
      symbolTable.addConstantUtf8(attributeName);
    }
    // attribute_name_index、attribute_length和num_annotations字段使用8字节。
    int attributeSize = 8;
    AnnotationWriter annotationWriter = this;
    while (annotationWriter != null) {
      attributeSize += annotationWriter.annotation.length;
      annotationWriter = annotationWriter.previousAnnotation;
    }
    return attributeSize;
  }

  /**
   * 返回包含给定注解及其所有<i>前驱</i>（参见{@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations属性的大小。
   * 同时将属性名添加到类的常量池中（如果不为null）。
   *
   * @param lastRuntimeVisibleAnnotation 字段、方法或类的最后一个运行时可见注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param lastRuntimeInvisibleAnnotation 字段、方法或类的最后一个运行时不可见注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param lastRuntimeVisibleTypeAnnotation 字段、方法或类的最后一个运行时可见类型注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param lastRuntimeInvisibleTypeAnnotation 字段、方法或类字段的最后一个运行时不可见类型注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @return 包含给定注解及其所有前驱的Runtime[In]Visible[Type]Annotations属性的字节大小。
   *     这包括attribute_name_index和attribute_length字段的大小。
   */
  static int computeAnnotationsSize(
      final AnnotationWriter lastRuntimeVisibleAnnotation,
      final AnnotationWriter lastRuntimeInvisibleAnnotation,
      final AnnotationWriter lastRuntimeVisibleTypeAnnotation,
      final AnnotationWriter lastRuntimeInvisibleTypeAnnotation) {
    int size = 0;
    if (lastRuntimeVisibleAnnotation != null) {
      size +=
          lastRuntimeVisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      size +=
          lastRuntimeInvisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      size +=
          lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      size +=
          lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
    }
    return size;
  }

  /**
   * 将包含此注解及其所有<i>前驱</i>（参见{@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations属性放入给定的ByteVector中。
   * 注解按访问顺序放置。
   *
   * @param attributeNameIndex 属性名称的常量池索引
   *     ("Runtime[In]Visible[Type]Annotations"之一)。
   * @param output 属性必须放入的位置。
   */
  void putAnnotations(final int attributeNameIndex, final ByteVector output) {
    int attributeLength = 2; // 用于num_annotations。
    int numAnnotations = 0;
    AnnotationWriter annotationWriter = this;
    AnnotationWriter firstAnnotation = null;
    while (annotationWriter != null) {
      // 以防用户忘记调用visitEnd()。
      annotationWriter.visitEnd();
      attributeLength += annotationWriter.annotation.length;
      numAnnotations++;
      firstAnnotation = annotationWriter;
      annotationWriter = annotationWriter.previousAnnotation;
    }
    output.putShort(attributeNameIndex);
    output.putInt(attributeLength);
    output.putShort(numAnnotations);
    annotationWriter = firstAnnotation;
    while (annotationWriter != null) {
      output.putByteArray(annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
      annotationWriter = annotationWriter.nextAnnotation;
    }
  }

  /**
   * 将包含给定注解及其所有<i>前驱</i>（参见{@link #previousAnnotation}）的
   * Runtime[In]Visible[Type]Annotations属性放入给定的ByteVector中。
   * 注解按访问顺序放置。
   *
   * @param symbolTable 存储AnnotationWriter实例中使用的常量的位置。
   * @param lastRuntimeVisibleAnnotation 字段、方法或类的最后一个运行时可见注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param lastRuntimeInvisibleAnnotation 字段、方法或类的最后一个运行时不可见注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param lastRuntimeVisibleTypeAnnotation 字段、方法或类的最后一个运行时可见类型注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param lastRuntimeInvisibleTypeAnnotation 字段、方法或类字段的最后一个运行时不可见类型注解。
   *     之前的注解可以通过{@link #previousAnnotation}字段访问。可能为{@literal null}。
   * @param output 属性必须放入的位置。
   */
  static void putAnnotations(
      final SymbolTable symbolTable,
      final AnnotationWriter lastRuntimeVisibleAnnotation,
      final AnnotationWriter lastRuntimeInvisibleAnnotation,
      final AnnotationWriter lastRuntimeVisibleTypeAnnotation,
      final AnnotationWriter lastRuntimeInvisibleTypeAnnotation,
      final ByteVector output) {
    if (lastRuntimeVisibleAnnotation != null) {
      lastRuntimeVisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_ANNOTATIONS), output);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      lastRuntimeInvisibleAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_ANNOTATIONS), output);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      lastRuntimeVisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS), output);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      lastRuntimeInvisibleTypeAnnotation.putAnnotations(
          symbolTable.addConstantUtf8(Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS), output);
    }
  }

  /**
   * 返回包含给定AnnotationWriter子数组中所有注解列表的
   * Runtime[In]VisibleParameterAnnotations属性的大小。
   * 同时将属性名添加到类的常量池中。
   *
   * @param attributeName "Runtime[In]VisibleParameterAnnotations"之一。
   * @param annotationWriters AnnotationWriter列表的数组（由它们的<i>最后</i>元素指定）。
   * @param annotableParameterCount annotationWriters中要考虑的元素数量
   *     （考虑元素[0..annotableParameterCount[）。
   * @return 对应于给定AnnotationWriter列表子数组的Runtime[In]VisibleParameterAnnotations
   *     属性的字节大小。这包括attribute_name_index和attribute_length字段的大小。
   */
  static int computeParameterAnnotationsSize(
      final String attributeName,
      final AnnotationWriter[] annotationWriters,
      final int annotableParameterCount) {
    // 注意：attributeName通过下面的computeAnnotationsSize调用添加到常量池。
    // 这假设annotationWriters子数组中至少有一个非null元素
    // （这通过MethodWriter中此数组的延迟实例化来确保）。
    // attribute_name_index、attribute_length和num_parameters字段使用7字节，
    // parameter_annotations数组的每个元素为其num_annotations字段使用2字节。
    int attributeSize = 7 + 2 * annotableParameterCount;
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      attributeSize +=
          annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(attributeName) - 8;
    }
    return attributeSize;
  }

  /**
   * 将包含给定AnnotationWriter子数组中所有注解列表的Runtime[In]VisibleParameterAnnotations
   * 属性放入给定的ByteVector中。
   *
   * @param attributeNameIndex 属性名称的常量池索引（Runtime[In]VisibleParameterAnnotations之一）。
   * @param annotationWriters AnnotationWriter列表的数组（由它们的<i>最后</i>元素指定）。
   * @param annotableParameterCount annotationWriters中要放入的元素数量
   *     （放入元素[0..annotableParameterCount[）。
   * @param output 属性必须放入的位置。
   */
  static void putParameterAnnotations(
      final int attributeNameIndex,
      final AnnotationWriter[] annotationWriters,
      final int annotableParameterCount,
      final ByteVector output) {
    // num_parameters字段使用1字节，parameter_annotations数组的每个元素
    // 为其num_annotations字段使用2字节。
    int attributeLength = 1 + 2 * annotableParameterCount;
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      attributeLength +=
          annotationWriter == null ? 0 : annotationWriter.computeAnnotationsSize(null) - 8;
    }
    output.putShort(attributeNameIndex);
    output.putInt(attributeLength);
    output.putByte(annotableParameterCount);
    for (int i = 0; i < annotableParameterCount; ++i) {
      AnnotationWriter annotationWriter = annotationWriters[i];
      AnnotationWriter firstAnnotation = null;
      int numAnnotations = 0;
      while (annotationWriter != null) {
        // 以防用户忘记调用visitEnd()。
        annotationWriter.visitEnd();
        numAnnotations++;
        firstAnnotation = annotationWriter;
        annotationWriter = annotationWriter.previousAnnotation;
      }
      output.putShort(numAnnotations);
      annotationWriter = firstAnnotation;
      while (annotationWriter != null) {
        output.putByteArray(
            annotationWriter.annotation.data, 0, annotationWriter.annotation.length);
        annotationWriter = annotationWriter.nextAnnotation;
      }
    }
  }
}
