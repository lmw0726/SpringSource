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
 * 一个 {@link ClassVisitor}，用于生成对应的 ClassFile 结构，符合 Java 虚拟机规范（JVMS）。  
 * 它既可以单独使用，从零开始生成一个 Java 类，也可以与一个或多个 {@link ClassReader} 及适配器 {@link ClassVisitor} 配合，  
 * 用于基于一个或多个已有 Java 类生成一个修改后的类。  
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html">JVMS 4</a>
 * @author Eric Bruneton
 */
public class ClassWriter extends ClassVisitor {

  /**
   * 一个标志，指示自动计算方法的最大操作数栈大小和最大本地变量数量。  
   * 如果设置此标志，则通过 {@link #visitMethod} 返回的 {@link MethodVisitor} 中的 {@link MethodVisitor#visitMaxs} 方法的参数将被忽略，  
   * 由 ASM 根据方法的签名和字节码自动计算。  
   *
   * <p><b>注意：</b> 对于版本为 {@link Opcodes#V1_7} 及以上的类，  
   * 该选项需要有效的栈映射帧。最大栈大小将根据这些帧及其间的字节码指令计算得出。  
   * 如果没有栈映射帧或需要重新计算帧，推荐使用 {@link #COMPUTE_FRAMES}。  
   *
   * @see #ClassWriter(int)
   */
  public static final int COMPUTE_MAXS = 1;

  /**
   * 一个标志，指示从头自动计算方法的栈映射帧。  
   * 如果设置此标志，则 {@link MethodVisitor#visitFrame} 方法的调用将被忽略，  
   * 栈映射帧将从方法的字节码重新计算。  
   * {@link MethodVisitor#visitMaxs} 方法的参数也将被忽略，并从字节码重新计算。  
   * 换句话说，{@link #COMPUTE_FRAMES} 含义上包含 {@link #COMPUTE_MAXS}。  
   *
   * @see #ClassWriter(int)
   */
  public static final int COMPUTE_FRAMES = 2;

  /**
   * 传递给构造函数的标志。必须是 {@link #COMPUTE_MAXS} 和/或 {@link #COMPUTE_FRAMES} 的组合。  
   */
  private final int flags;

  // 注意：字段顺序与 ClassFile 结构一致，属性相关字段顺序与 JVMS 第4.7节一致。

  /**
   * JVMS ClassFile 结构中的 minor_version 和 major_version 字段。  
   * minor_version 存储在高16位，major_version 存储在低16位。  
   */
  private int version;

  /** 该类的符号表（包含常量池和引导方法）。 */
  private final SymbolTable symbolTable;

  /**
   * JVMS ClassFile 结构中的 access_flags 字段。  
   * 该字段可能包含 ASM 特定的访问标志，如 {@link Opcodes#ACC_DEPRECATED} 或 {@link Opcodes#ACC_RECORD}，  
   * 生成 ClassFile 时会移除这些标志。  
   */
  private int accessFlags;

  /** JVMS ClassFile 结构中的 this_class 字段。 */
  private int thisClass;

  /** JVMS ClassFile 结构中的 super_class 字段。 */
  private int superClass;

  /** JVMS ClassFile 结构中的 interface_count 字段。 */
  private int interfaceCount;

  /** JVMS ClassFile 结构中的接口数组。 */
  private int[] interfaces;

  /**
   * 该类的字段，以 {@link FieldWriter} 链表形式存储，链表通过 {@link FieldWriter#fv} 字段链接。  
   * 该字段存储链表的第一个元素。  
   */
  private FieldWriter firstField;

  /**
   * 该类的字段，以 {@link FieldWriter} 链表形式存储，链表通过 {@link FieldWriter#fv} 字段链接。  
   * 该字段存储链表的最后一个元素。  
   */
  private FieldWriter lastField;

  /**
   * 该类的方法，以 {@link MethodWriter} 链表形式存储，链表通过 {@link MethodWriter#mv} 字段链接。  
   * 该字段存储链表的第一个元素。  
   */
  private MethodWriter firstMethod;

  /**
   * 该类的方法，以 {@link MethodWriter} 链表形式存储，链表通过 {@link MethodWriter#mv} 字段链接。  
   * 该字段存储链表的最后一个元素。  
   */
  private MethodWriter lastMethod;

  /** InnerClasses 属性中的 number_of_classes 字段，或者为0。 */
  private int numberOfInnerClasses;

  /** InnerClasses 属性中的 classes 数组，或者为 {@literal null}。 */
  private ByteVector innerClasses;

  /** EnclosingMethod 属性中的 class_index 字段，或者为0。 */
  private int enclosingClassIndex;

  /** EnclosingMethod 属性中的 method_index 字段。 */
  private int enclosingMethodIndex;

  /** Signature 属性的 signature_index 字段，或者为0。 */
  private int signatureIndex;

  /** SourceFile 属性的 source_file_index 字段，或者为0。 */
  private int sourceFileIndex;

  /** SourceDebugExtension 属性的 debug_extension 字段，或者为 {@literal null}。 */
  private ByteVector debugExtension;

  /**
   * 该类的最后一个运行时可见注解。之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * 该类的最后一个运行时不可见注解。之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * 该类的最后一个运行时可见类型注解。之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * 该类的最后一个运行时不可见类型注解。之前的注解可通过 {@link AnnotationWriter#previousAnnotation} 访问。可能为 {@literal null}。
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /** 该类的 Module 属性，或者为 {@literal null}。 */
  private ModuleWriter moduleWriter;

  /** NestHost 属性中的 host_class_index 字段，或者为0。 */
  private int nestHostClassIndex;

  /** NestMembers 属性中的 number_of_classes 字段，或者为0。 */
  private int numberOfNestMemberClasses;

  /** NestMembers 属性中的 classes 数组，或者为 {@literal null}。 */
  private ByteVector nestMemberClasses;

  /** PermittedSubclasses 属性中的 number_of_classes 字段，或者为0。 */
  private int numberOfPermittedSubclasses;

  /** PermittedSubclasses 属性中的 classes 数组，或者为 {@literal null}。 */
  private ByteVector permittedSubclasses;

  /**
   * 该类的记录组件，以 {@link RecordComponentWriter} 链表形式存储，链表通过 {@link RecordComponentWriter#delegate} 字段链接。  
   * 该字段存储链表的第一个元素。  
   */
  private RecordComponentWriter firstRecordComponent;

  /**
   * 该类的记录组件，以 {@link RecordComponentWriter} 链表形式存储，链表通过 {@link RecordComponentWriter#delegate} 字段链接。  
   * 该字段存储链表的最后一个元素。  
   */
  private RecordComponentWriter lastRecordComponent;

  /**
   * 该类的第一个非标准属性。可以通过 {@link Attribute#nextAttribute} 访问后续属性。可能为 {@literal null}。
   *
   * <p><b>警告</b>：该列表按访问顺序的 <i>反向</i> 存储属性。
   * firstAttribute 实际上是 {@link #visitAttribute} 中最后访问的属性。
   * {@link #toByteArray} 方法会按该列表定义的顺序写出属性，也即是用户指定顺序的反向。
   */
  private Attribute firstAttribute;

  /**
   * 指示在 {@link MethodWriter} 中必须自动计算的内容。
   * 必须是 {@link MethodWriter#COMPUTE_NOTHING}、{@link MethodWriter#COMPUTE_MAX_STACK_AND_LOCAL}、
   * {@link MethodWriter#COMPUTE_INSERTED_FRAMES} 或 {@link MethodWriter#COMPUTE_ALL_FRAMES} 之一。
   */
  private int compute;

  // -----------------------------------------------------------------------------------------------
  // 构造函数
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link ClassWriter} 对象。
   *
   * @param flags 可用于修改此类默认行为的选项标志。必须是 {@link #COMPUTE_MAXS} 和 {@link #COMPUTE_FRAMES} 的零个或多个组合。
   */
  public ClassWriter(final int flags) {
    this(null, flags);
  }

  /**
   * 构造一个新的 {@link ClassWriter} 对象，并启用“主要为新增”字节码转换的优化。
   * 这些优化包括：
   *
   * <ul>
   *   <li>从原始类复制常量池和引导方法，节省时间。若有需要，新常量池条目和引导方法会追加添加，但未使用的条目不会被删除。
   *   <li>未被转换的方法直接从原始字节码复制，无需为所有方法指令发出访问事件，大大节省时间。
   *       未转换的方法通过 {@link ClassReader} 接收来自 {@link ClassWriter} 的 {@link MethodVisitor} 来检测。
   * </ul>
   *
   * @param classReader 用于读取原始类的 {@link ClassReader}，用以复制常量池、引导方法及其他字节码片段。
   * @param flags 可用于修改此类默认行为的选项标志。必须是 {@link #COMPUTE_MAXS} 和 {@link #COMPUTE_FRAMES} 的零个或多个组合。
   *              <i>注意：这些标志不影响直接复制的方法，这意味着这些方法不会自动计算最大栈大小或栈帧。</i>
   */
  public ClassWriter(final ClassReader classReader, final int flags) {
    super(/* 最新的 API = */ Opcodes.ASM9);
    this.flags = flags;
    symbolTable = classReader == null ? new SymbolTable(this) : new SymbolTable(this, classReader);
    if ((flags & COMPUTE_FRAMES) != 0) {
      compute = MethodWriter.COMPUTE_ALL_FRAMES;
    } else if ((flags & COMPUTE_MAXS) != 0) {
      compute = MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL;
    } else {
      compute = MethodWriter.COMPUTE_NOTHING;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 访问器方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 如果构造函数传入的标志包含了所有给定标志，则返回 true。
   *
   * @param flags 一些选项标志。必须是 {@link #COMPUTE_MAXS} 和 {@link #COMPUTE_FRAMES} 的零个或多个组合。
   * @return 如果给定的所有标志（或更多）都包含在构造函数的标志中，则返回 true。
   */
  public boolean hasFlags(final int flags) {
    return (this.flags & flags) == flags;
  }

  // -----------------------------------------------------------------------------------------------
  // ClassVisitor 抽象类的实现
  // -----------------------------------------------------------------------------------------------

  @Override
  public final void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    this.version = version;
    this.accessFlags = access;
    this.thisClass = symbolTable.setMajorVersionAndClassName(version & 0xFFFF, name);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
    this.superClass = superName == null ? 0 : symbolTable.addConstantClass(superName).index;
    if (interfaces != null && interfaces.length > 0) {
      interfaceCount = interfaces.length;
      this.interfaces = new int[interfaceCount];
      for (int i = 0; i < interfaceCount; ++i) {
        this.interfaces[i] = symbolTable.addConstantClass(interfaces[i]).index;
      }
    }
    if (compute == MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL && (version & 0xFFFF) >= Opcodes.V1_7) {
      compute = MethodWriter.COMPUTE_MAX_STACK_AND_LOCAL_FROM_FRAMES;
    }
  }

  @Override
  public final void visitSource(final String file, final String debug) {
    if (file != null) {
      sourceFileIndex = symbolTable.addConstantUtf8(file);
    }
    if (debug != null) {
      debugExtension = new ByteVector().encodeUtf8(debug, 0, Integer.MAX_VALUE);
    }
  }

  @Override
  public final ModuleVisitor visitModule(
      final String name, final int access, final String version) {
    return moduleWriter =
        new ModuleWriter(
            symbolTable,
            symbolTable.addConstantModule(name).index,
            access,
            version == null ? 0 : symbolTable.addConstantUtf8(version));
  }

  @Override
  public final void visitNestHost(final String nestHost) {
    nestHostClassIndex = symbolTable.addConstantClass(nestHost).index;
  }

  @Override
  public final void visitOuterClass(
      final String owner, final String name, final String descriptor) {
    enclosingClassIndex = symbolTable.addConstantClass(owner).index;
    if (name != null && descriptor != null) {
      enclosingMethodIndex = symbolTable.addConstantNameAndType(name, descriptor);
    }
  }

  @Override
  public final AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
    } else {
      return lastRuntimeInvisibleAnnotation =
          AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public final AnnotationVisitor visitTypeAnnotation(
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
  public final void visitAttribute(final Attribute attribute) {
    // 以该方法访问的<i>逆序</i>存储属性。
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public final void visitNestMember(final String nestMember) {
    if (nestMemberClasses == null) {
      nestMemberClasses = new ByteVector();
    }
    ++numberOfNestMemberClasses;
    nestMemberClasses.putShort(symbolTable.addConstantClass(nestMember).index);
  }

  @Override
  public final void visitPermittedSubclass(final String permittedSubclass) {
    if (permittedSubclasses == null) {
      permittedSubclasses = new ByteVector();
    }
    ++numberOfPermittedSubclasses;
    permittedSubclasses.putShort(symbolTable.addConstantClass(permittedSubclass).index);
  }

  @Override
  public final void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    if (innerClasses == null) {
      innerClasses = new ByteVector();
    }
    // JVMS 第4.7.6节规定：
    // “常量池中的每个表示类或接口C的CONSTANT_Class_info条目（该类或接口不是包成员）
    // 必须在classes数组中有且仅有一个对应条目。”
    // 为了避免重复，我们在Symbol的info字段中记录每个CONSTANT_Class_info条目C是否已经
    // 添加了对应的内部类条目。
    // 如果已经添加，则在info字段中存储该内部类条目的索引（加一）。
    // 这个技巧允许我们以O(1)的时间检测重复。
    Symbol nameSymbol = symbolTable.addConstantClass(name);
    if (nameSymbol.info == 0) {
      ++numberOfInnerClasses;
      innerClasses.putShort(nameSymbol.index);
      innerClasses.putShort(outerName == null ? 0 : symbolTable.addConstantClass(outerName).index);
      innerClasses.putShort(innerName == null ? 0 : symbolTable.addConstantUtf8(innerName));
      innerClasses.putShort(access);
      nameSymbol.info = numberOfInnerClasses;
    }
    // 否则，比较innerClasses条目nameSymbol.info - 1与该方法参数是否一致，
    // 若不一致则抛出异常？
  }

  @Override
  public final RecordComponentVisitor visitRecordComponent(
      final String name, final String descriptor, final String signature) {
    RecordComponentWriter recordComponentWriter =
        new RecordComponentWriter(symbolTable, name, descriptor, signature);
    if (firstRecordComponent == null) {
      firstRecordComponent = recordComponentWriter;
    } else {
      lastRecordComponent.delegate = recordComponentWriter;
    }
    return lastRecordComponent = recordComponentWriter;
  }

  @Override
  public final FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    FieldWriter fieldWriter =
        new FieldWriter(symbolTable, access, name, descriptor, signature, value);
    if (firstField == null) {
      firstField = fieldWriter;
    } else {
      lastField.fv = fieldWriter;
    }
    return lastField = fieldWriter;
  }

  @Override
  public final MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    MethodWriter methodWriter =
        new MethodWriter(symbolTable, access, name, descriptor, signature, exceptions, compute);
    if (firstMethod == null) {
      firstMethod = methodWriter;
    } else {
      lastMethod.mv = methodWriter;
    }
    return lastMethod = methodWriter;
  }

  @Override
  public final void visitEnd() {
    // 不做任何操作
  }

  // -----------------------------------------------------------------------------------------------
  // 其他公共方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回该 ClassWriter 构建的类文件内容。
   *
   * @return 构建的 JVMS ClassFile 结构的二进制内容。
   * @throws ClassTooLargeException 如果类的常量池过大。
   * @throws MethodTooLargeException 如果某方法的 Code 属性过大。
   */
  public byte[] toByteArray() {
    // 第一步：计算 ClassFile 结构所需字节数。
    // magic 字段占 4 字节，10 个必需字段（minor_version, major_version,
    // constant_pool_count, access_flags, this_class, super_class, interfaces_count,
    // fields_count, methods_count 和 attributes_count）各占 2 字节，每个接口也占 2 字节。
    int size = 24 + 2 * interfaceCount;
    int fieldsCount = 0;
    FieldWriter fieldWriter = firstField;
    while (fieldWriter != null) {
      ++fieldsCount;
      size += fieldWriter.computeFieldInfoSize();
      fieldWriter = (FieldWriter) fieldWriter.fv;
    }
    int methodsCount = 0;
    MethodWriter methodWriter = firstMethod;
    while (methodWriter != null) {
      ++methodsCount;
      size += methodWriter.computeMethodInfoSize();
      methodWriter = (MethodWriter) methodWriter.mv;
    }

    // 按 JVMS 第4.7节的属性顺序计算属性数量和大小，方便管理。
    int attributesCount = 0;
    if (innerClasses != null) {
      ++attributesCount;
      size += 8 + innerClasses.length;
      symbolTable.addConstantUtf8(Constants.INNER_CLASSES);
    }
    if (enclosingClassIndex != 0) {
      ++attributesCount;
      size += 10;
      symbolTable.addConstantUtf8(Constants.ENCLOSING_METHOD);
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && (version & 0xFFFF) < Opcodes.V1_5) {
      ++attributesCount;
      size += 6;
      symbolTable.addConstantUtf8(Constants.SYNTHETIC);
    }
    if (signatureIndex != 0) {
      ++attributesCount;
      size += 8;
      symbolTable.addConstantUtf8(Constants.SIGNATURE);
    }
    if (sourceFileIndex != 0) {
      ++attributesCount;
      size += 8;
      symbolTable.addConstantUtf8(Constants.SOURCE_FILE);
    }
    if (debugExtension != null) {
      ++attributesCount;
      size += 6 + debugExtension.length;
      symbolTable.addConstantUtf8(Constants.SOURCE_DEBUG_EXTENSION);
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      ++attributesCount;
      size += 6;
      symbolTable.addConstantUtf8(Constants.DEPRECATED);
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeVisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeInvisibleAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_ANNOTATIONS);
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeVisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
      size +=
          lastRuntimeInvisibleTypeAnnotation.computeAnnotationsSize(
              Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS);
    }
    if (symbolTable.computeBootstrapMethodsSize() > 0) {
      ++attributesCount;
      size += symbolTable.computeBootstrapMethodsSize();
    }
    if (moduleWriter != null) {
      attributesCount += moduleWriter.getAttributeCount();
      size += moduleWriter.computeAttributesSize();
    }
    if (nestHostClassIndex != 0) {
      ++attributesCount;
      size += 8;
      symbolTable.addConstantUtf8(Constants.NEST_HOST);
    }
    if (nestMemberClasses != null) {
      ++attributesCount;
      size += 8 + nestMemberClasses.length;
      symbolTable.addConstantUtf8(Constants.NEST_MEMBERS);
    }
    if (permittedSubclasses != null) {
      ++attributesCount;
      size += 8 + permittedSubclasses.length;
      symbolTable.addConstantUtf8(Constants.PERMITTED_SUBCLASSES);
    }
    int recordComponentCount = 0;
    int recordSize = 0;
    if ((accessFlags & Opcodes.ACC_RECORD) != 0 || firstRecordComponent != null) {
      RecordComponentWriter recordComponentWriter = firstRecordComponent;
      while (recordComponentWriter != null) {
        ++recordComponentCount;
        recordSize += recordComponentWriter.computeRecordComponentInfoSize();
        recordComponentWriter = (RecordComponentWriter) recordComponentWriter.delegate;
      }
      ++attributesCount;
      size += 8 + recordSize;
      symbolTable.addConstantUtf8(Constants.RECORD);
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    // 注意：必须在这里最后计算 ClassFile 大小，因为前面操作可能向常量池添加了条目，改变了大小。
    size += symbolTable.getConstantPoolLength();
    int constantPoolCount = symbolTable.getConstantPoolCount();
    if (constantPoolCount > 0xFFFF) {
      throw new ClassTooLargeException(symbolTable.getClassName(), constantPoolCount);
    }

    // 第二步：分配正确大小的 ByteVector，避免动态扩容时的数组拷贝，写入 ClassFile 内容。
    ByteVector result = new ByteVector(size);
    result.putInt(0xCAFEBABE).putInt(version);
    symbolTable.putConstantPool(result);
    int mask = (version & 0xFFFF) < Opcodes.V1_5 ? Opcodes.ACC_SYNTHETIC : 0;
    result.putShort(accessFlags & ~mask).putShort(thisClass).putShort(superClass);
    result.putShort(interfaceCount);
    for (int i = 0; i < interfaceCount; ++i) {
      result.putShort(interfaces[i]);
    }
    result.putShort(fieldsCount);
    fieldWriter = firstField;
    while (fieldWriter != null) {
      fieldWriter.putFieldInfo(result);
      fieldWriter = (FieldWriter) fieldWriter.fv;
    }
    result.putShort(methodsCount);
    boolean hasFrames = false;
    boolean hasAsmInstructions = false;
    methodWriter = firstMethod;
    while (methodWriter != null) {
      hasFrames |= methodWriter.hasFrames();
      hasAsmInstructions |= methodWriter.hasAsmInstructions();
      methodWriter.putMethodInfo(result);
      methodWriter = (MethodWriter) methodWriter.mv;
    }
    // 按 JVMS 4.7 节的顺序写入属性。
    result.putShort(attributesCount);
    if (innerClasses != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.INNER_CLASSES))
          .putInt(innerClasses.length + 2)
          .putShort(numberOfInnerClasses)
          .putByteArray(innerClasses.data, 0, innerClasses.length);
    }
    if (enclosingClassIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.ENCLOSING_METHOD))
          .putInt(4)
          .putShort(enclosingClassIndex)
          .putShort(enclosingMethodIndex);
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && (version & 0xFFFF) < Opcodes.V1_5) {
      result.putShort(symbolTable.addConstantUtf8(Constants.SYNTHETIC)).putInt(0);
    }
    if (signatureIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.SIGNATURE))
          .putInt(2)
          .putShort(signatureIndex);
    }
    if (sourceFileIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.SOURCE_FILE))
          .putInt(2)
          .putShort(sourceFileIndex);
    }
    if (debugExtension != null) {
      int length = debugExtension.length;
      result
          .putShort(symbolTable.addConstantUtf8(Constants.SOURCE_DEBUG_EXTENSION))
          .putInt(length)
          .putByteArray(debugExtension.data, 0, length);
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      result.putShort(symbolTable.addConstantUtf8(Constants.DEPRECATED)).putInt(0);
    }
    AnnotationWriter.putAnnotations(
        symbolTable,
        lastRuntimeVisibleAnnotation,
        lastRuntimeInvisibleAnnotation,
        lastRuntimeVisibleTypeAnnotation,
        lastRuntimeInvisibleTypeAnnotation,
        result);
    symbolTable.putBootstrapMethods(result);
    if (moduleWriter != null) {
      moduleWriter.putAttributes(result);
    }
    if (nestHostClassIndex != 0) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.NEST_HOST))
          .putInt(2)
          .putShort(nestHostClassIndex);
    }
    if (nestMemberClasses != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.NEST_MEMBERS))
          .putInt(nestMemberClasses.length + 2)
          .putShort(numberOfNestMemberClasses)
          .putByteArray(nestMemberClasses.data, 0, nestMemberClasses.length);
    }
    if (permittedSubclasses != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.PERMITTED_SUBCLASSES))
          .putInt(permittedSubclasses.length + 2)
          .putShort(numberOfPermittedSubclasses)
          .putByteArray(permittedSubclasses.data, 0, permittedSubclasses.length);
    }
    if ((accessFlags & Opcodes.ACC_RECORD) != 0 || firstRecordComponent != null) {
      result
          .putShort(symbolTable.addConstantUtf8(Constants.RECORD))
          .putInt(recordSize + 2)
          .putShort(recordComponentCount);
      RecordComponentWriter recordComponentWriter = firstRecordComponent;
      while (recordComponentWriter != null) {
        recordComponentWriter.putRecordComponentInfo(result);
        recordComponentWriter = (RecordComponentWriter) recordComponentWriter.delegate;
      }
    }
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, result);
    }

    // 第三步：如果有 ASM 特定指令，则替换它们。
    if (hasAsmInstructions) {
      return replaceAsmInstructions(result.data, hasFrames);
    } else {
      return result.data;
    }
  }

  /**
   * 返回给定类文件的等价版本，将 ASM 特有指令替换为标准指令。该替换通过
   * ClassReader -> ClassWriter 的往返转换实现。
   *
   * @param classFile 包含 ASM 特有指令的类文件，由此 ClassWriter 生成。
   * @param hasFrames 表示 classFile 中是否至少包含一个栈映射帧。
   * @return 替换了 ASM 特有指令的等价类文件。
   */
  private byte[] replaceAsmInstructions(final byte[] classFile, final boolean hasFrames) {
    final Attribute[] attributes = getAttributePrototypes();
    firstField = null;
    lastField = null;
    firstMethod = null;
    lastMethod = null;
    lastRuntimeVisibleAnnotation = null;
    lastRuntimeInvisibleAnnotation = null;
    lastRuntimeVisibleTypeAnnotation = null;
    lastRuntimeInvisibleTypeAnnotation = null;
    moduleWriter = null;
    nestHostClassIndex = 0;
    numberOfNestMemberClasses = 0;
    nestMemberClasses = null;
    numberOfPermittedSubclasses = 0;
    permittedSubclasses = null;
    firstRecordComponent = null;
    lastRecordComponent = null;
    firstAttribute = null;
    compute = hasFrames ? MethodWriter.COMPUTE_INSERTED_FRAMES : MethodWriter.COMPUTE_NOTHING;
    new ClassReader(classFile, 0, /* checkClassVersion = */ false)
        .accept(
            this,
            attributes,
            (hasFrames ? ClassReader.EXPAND_FRAMES : 0) | ClassReader.EXPAND_ASM_INSNS);
    return toByteArray();
  }

  /**
   * 返回此类及其字段和方法所使用的属性原型数组。
   *
   * @return 此类及其字段和方法所使用的属性原型数组。
   */
  private Attribute[] getAttributePrototypes() {
    Attribute.Set attributePrototypes = new Attribute.Set();
    attributePrototypes.addAttributes(firstAttribute);
    FieldWriter fieldWriter = firstField;
    while (fieldWriter != null) {
      fieldWriter.collectAttributePrototypes(attributePrototypes);
      fieldWriter = (FieldWriter) fieldWriter.fv;
    }
    MethodWriter methodWriter = firstMethod;
    while (methodWriter != null) {
      methodWriter.collectAttributePrototypes(attributePrototypes);
      methodWriter = (MethodWriter) methodWriter.mv;
    }
    RecordComponentWriter recordComponentWriter = firstRecordComponent;
    while (recordComponentWriter != null) {
      recordComponentWriter.collectAttributePrototypes(attributePrototypes);
      recordComponentWriter = (RecordComponentWriter) recordComponentWriter.delegate;
    }
    return attributePrototypes.toArray();
  }

  // -----------------------------------------------------------------------------------------------
  // 常量池管理工具方法（供 Attribute 子类使用）
  // -----------------------------------------------------------------------------------------------

  /**
   * 向构建的类的常量池添加数字或字符串常量。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param value 要添加的常量，必须是 {@link Integer}、{@link Float}、{@link Long}、{@link Double} 或 {@link String}。
   * @return 新增或已存在的常量索引。
   */
  public int newConst(final Object value) {
    return symbolTable.addConstant(value).index;
  }

  /**
   * 向构建的类的常量池添加 UTF8 字符串。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param value 字符串值。
   * @return 新增或已存在的 UTF8 常量索引。
   */
  public int newUTF8(final String value) {
    return symbolTable.addConstantUtf8(value);
  }

  /**
   * 向构建的类的常量池添加类引用。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param value 类的内部名称。
   * @return 新增或已存在的类引用索引。
   */
  public int newClass(final String value) {
    return symbolTable.addConstantClass(value).index;
  }

  /**
   * 向构建的类的常量池添加方法类型引用。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param methodDescriptor 方法描述符。
   * @return 新增或已存在的方法类型引用索引。
   */
  public int newMethodType(final String methodDescriptor) {
    return symbolTable.addConstantMethodType(methodDescriptor).index;
  }

  /**
   * 向构建的类的常量池添加模块引用。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param moduleName 模块名称。
   * @return 新增或已存在的模块引用索引。
   */
  public int newModule(final String moduleName) {
    return symbolTable.addConstantModule(moduleName).index;
  }

  /**
   * 向构建的类的常量池添加包引用。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param packageName 包的内部名称。
   * @return 新增或已存在的包引用索引。
   */
  public int newPackage(final String packageName) {
    return symbolTable.addConstantPackage(packageName).index;
  }

  /**
   * 向构建的类的常量池添加方法句柄。
   * 如果常量池已包含类似项则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param tag 句柄类型，必须是 {@link Opcodes#H_GETFIELD}、{@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、
   *            {@link Opcodes#H_PUTSTATIC}、{@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、
   *            {@link Opcodes#H_INVOKESPECIAL}、{@link Opcodes#H_NEWINVOKESPECIAL} 或 {@link Opcodes#H_INVOKEINTERFACE}。
   * @param owner 字段或方法所属类的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   * @param isInterface 如果 owner 是接口则为 true。
   * @return 新增或已存在的方法句柄索引。
   */
  public int newHandle(
      final int tag,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    return symbolTable.addConstantMethodHandle(tag, owner, name, descriptor, isInterface).index;
  }

  /**
   * 添加动态常量引用到构建类的常量池。如果已存在类似项，则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param name 被调用方法的名称。
   * @param descriptor 常量类型的字段描述符。
   * @param bootstrapMethodHandle 引导方法句柄。
   * @param bootstrapMethodArguments 引导方法的常量参数。
   * @return 新增或已存在的动态常量引用索引。
   */
  public int newConstantDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    return symbolTable.addConstantDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        .index;
  }

  /**
   * 添加 invokedynamic 引用到构建类的常量池。如果已存在类似项，则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param name 被调用方法的名称。
   * @param descriptor 被调用方法的描述符。
   * @param bootstrapMethodHandle 引导方法句柄。
   * @param bootstrapMethodArguments 引导方法的常量参数。
   * @return 新增或已存在的 invokedynamic 引用索引。
   */
  public int newInvokeDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethodHandle,
      final Object... bootstrapMethodArguments) {
    return symbolTable.addConstantInvokeDynamic(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments)
        .index;
  }

  /**
   * 添加字段引用到构建类的常量池。如果已存在类似项，则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param owner 字段所属类的内部名称。
   * @param name 字段名。
   * @param descriptor 字段描述符。
   * @return 新增或已存在的字段引用索引。
   */
  public int newField(final String owner, final String name, final String descriptor) {
    return symbolTable.addConstantFieldref(owner, name, descriptor).index;
  }

  /**
   * 添加方法引用到构建类的常量池。如果已存在类似项，则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param owner 方法所属类的内部名称。
   * @param name 方法名。
   * @param descriptor 方法描述符。
   * @param isInterface 如果 owner 是接口则为 true。
   * @return 新增或已存在的方法引用索引。
   */
  public int newMethod(
      final String owner, final String name, final String descriptor, final boolean isInterface) {
    return symbolTable.addConstantMethodref(owner, name, descriptor, isInterface).index;
  }

  /**
   * 添加名称和类型项到构建类的常量池。如果已存在类似项，则不做任何操作。
   * <i>此方法主要供 {@link Attribute} 子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param name 名称。
   * @param descriptor 类型描述符。
   * @return 新增或已存在的名称和类型索引。
   */
  public int newNameType(final String name, final String descriptor) {
    return symbolTable.addConstantNameAndType(name, descriptor);
  }

  // -----------------------------------------------------------------------------------------------
  // 计算栈映射帧时默认的公共父类计算方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回两个类型的公共父类。该默认实现会 <i>加载</i> 两个类，并使用 java.lang.Class 的方法寻找公共父类。
   * 可重写此方法实现不同的公共父类计算，特别是无需实际加载类的场景，
   * 或考虑当前正在构建的类（无法加载）。
   *
   * @param type1 第一个类的内部名称。
   * @param type2 第二个类的内部名称。
   * @return 两个类的公共父类的内部名称。
   */
  protected String getCommonSuperClass(final String type1, final String type2) {
    ClassLoader classLoader = getClassLoader();
    Class<?> class1;
    try {
      class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
    } catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(type1, e);
    }
    Class<?> class2;
    try {
      class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
    } catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(type2, e);
    }
    if (class1.isAssignableFrom(class2)) {
      return type1;
    }
    if (class2.isAssignableFrom(class1)) {
      return type2;
    }
    if (class1.isInterface() || class2.isInterface()) {
      return "java/lang/Object";
    } else {
      do {
        class1 = class1.getSuperclass();
      } while (!class1.isAssignableFrom(class2));
      return class1.getName().replace('.', '/');
    }
  }

  /**
   * 返回默认的 ClassLoader，用于 {@link #getCommonSuperClass(String, String)} 的实现，
   * 默认返回此 ClassWriter 运行时类型的 ClassLoader。
   *
   * @return ClassLoader 实例。
   */
  protected ClassLoader getClassLoader() {
    // SPRING PATCH: 优先使用线程上下文 ClassLoader 以支持应用类加载
    ClassLoader classLoader = null;
    try {
      classLoader = Thread.currentThread().getContextClassLoader();
    } catch (Throwable ex) {
      // 无法访问线程上下文 ClassLoader，使用默认
    }
    return (classLoader != null ? classLoader : getClass().getClassLoader());
  }
}
