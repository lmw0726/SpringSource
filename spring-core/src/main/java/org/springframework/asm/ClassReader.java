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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 一个解析器，用于使 {@link ClassVisitor} 访问按照 Java 虚拟机规范（JVMS）定义的 ClassFile 结构。
 * 该类解析 ClassFile 的内容，并在遇到每个字段、方法和字节码指令时，调用给定 {@link ClassVisitor} 的相应访问方法。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html">JVMS 4</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public class ClassReader {

  /**
   * 用于跳过 Code 属性的标志。如果设置了此标志，则不会解析或访问 Code 属性。
   */
  public static final int SKIP_CODE = 1;

  /**
   * 用于跳过 SourceFile、SourceDebugExtension、LocalVariableTable、LocalVariableTypeTable、
   * LineNumberTable 和 MethodParameters 属性的标志。如果设置了此标志，这些属性将不会被解析或访问
   * （即不会调用 {@link ClassVisitor#visitSource}、{@link MethodVisitor#visitLocalVariable}、
   * {@link MethodVisitor#visitLineNumber} 和 {@link MethodVisitor#visitParameter}）。
   */
  public static final int SKIP_DEBUG = 2;

  /**
   * 用于跳过 StackMap 和 StackMapTable 属性的标志。如果设置了此标志，这些属性将不会被解析或访问
   * （即不会调用 {@link MethodVisitor#visitFrame}）。此标志在使用 {@link ClassWriter#COMPUTE_FRAMES}
   * 选项时非常有用：可以避免访问那些将被忽略并重新计算的帧信息。
   */
  public static final int SKIP_FRAMES = 4;

  /**
   * 用于展开栈映射帧（stack map frames）的标志。默认情况下，栈映射帧以其原始格式访问
   * （即版本低于 V1_6 的类使用“展开”格式，其他类使用“压缩”格式）。
   * 如果设置了此标志，栈映射帧将始终以展开格式访问（此选项会在 ClassReader 和 ClassWriter 中增加解压/压缩步骤，
   * 会显著降低性能）。
   */
  public static final int EXPAND_FRAMES = 8;

  /**
   * 用于将 ASM 特有的指令扩展为等效的标准字节码指令序列的标志。
   * 在解析前向跳转时，可能用于存储偏移量的有符号 2 字节空间不足以容纳实际的字节码偏移量。
   * 此时，跳转指令会被替换为一个使用无符号 2 字节偏移量的临时 ASM 特有指令（参见 {@link Label#resolve}）。
   * 此内部标志用于重新读取包含此类指令的类，以便将其替换为标准指令。
   * 此外，当使用此标志时，goto_w 和 jsr_w 指令将 <i>不会</i> 被转换为 goto 和 jsr，
   * 以防止在 ClassReader 中将 goto_w 替换为 goto，而在 ClassWriter 中又转换回 goto_w 所导致的无限循环。
   */
  static final int EXPAND_ASM_INSNS = 256;

  /** 可分配数组的最大大小。 */
  private static final int MAX_BUFFER_SIZE = 1024 * 1024;

  /** 用于逐块读取类输入流的临时字节数组的大小。 */
  private static final int INPUT_STREAM_DATA_CHUNK_SIZE = 4096;

  /**
   * 一个包含待解析的 JVMS ClassFile 结构的字节数组。
   *
   * @deprecated 使用 {@link #readByte(int)} 及其他读取方法代替。此字段最终将被删除。
   */
  @Deprecated
  // DontCheck(MemberName): 为保持向后二进制兼容性，不能重命名。
  public final byte[] b;

  /** ClassFile 的 access_flags 字段的字节偏移量。 */
  public final int header;

  /**
   * 一个包含待解析的 JVMS ClassFile 结构的字节数组。<i>此数组的内容不得被修改。该字段主要供 {@link Attribute} 子类使用，
   * 通常不需要被类访问器（class visitors）直接访问。</i>
   *
   * <p>注意：ClassFile 结构可以从此数组的任意偏移量开始，即不一定从偏移 0 开始。请使用 {@link #getItem} 和 {@link #header}
   * 方法来获取此字节数组中 ClassFile 各元素的正确偏移量。
   */
  final byte[] classFileBuffer;

  /**
   * 在 {@link #classFileBuffer} 中，ClassFile 的 constant_pool 数组中每个 cp_info 项的字节偏移量，<i>加一</i>。
   * 换句话说，常量池第 i 项的偏移量为 cpInfoOffsets[i] - 1，即其 cp_info 的标签字段（tag）位于 b[cpInfoOffsets[i] - 1]。
   */
  private final int[] cpInfoOffsets;

  /**
   * 与常量池中 CONSTANT_Utf8 项对应的 String 对象缓存。此缓存避免对同一个 CONSTANT_Utf8 项进行多次解析。
   */
  private final String[] constantUtf8Values;

  /**
   * 与常量池中 CONSTANT_Dynamic 项对应的 ConstantDynamic 对象缓存。此缓存避免对同一个 CONSTANT_Dynamic 项进行多次解析。
   */
  private final ConstantDynamic[] constantDynamicValues;

  /**
   * BootstrapMethods 属性中 bootstrap_methods 数组每个元素在 {@link #classFileBuffer} 中的起始偏移量。
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23">JVMS 4.7.23</a>
   */
  private final int[] bootstrapMethodOffsets;

  /**
   * 类的常量池中包含的字符串最大长度的一个保守估计值。
   */
  private final int maxStringLength;

  // -----------------------------------------------------------------------------------------------
  // 构造器（Constructors）
  // -----------------------------------------------------------------------------------------------

  /**
   * 构造一个新的 {@link ClassReader} 对象。
   *
   * @param classFile 要读取的JVMS类文件结构。
   */
  public ClassReader(final byte[] classFile) {
    this(classFile, 0, classFile.length);
  }

  /**
   * 构造一个新的 {@link ClassReader} 对象。
   *
   * @param classFileBuffer 包含要读取的 JVMS ClassFile 结构的字节数组。
   * @param classFileOffset ClassFile 起始字节在字节数组中的偏移量。
   * @param classFileLength 要读取的 ClassFile 的字节长度。
   */
  public ClassReader(
      final byte[] classFileBuffer,
      final int classFileOffset,
      final int classFileLength) { // NOPMD(UnusedFormalParameter) 为了向后兼容而保留
    this(classFileBuffer, classFileOffset, /* checkClassVersion = */ true);
  }

  /**
   * 构造一个新的 {@link ClassReader} 对象。<i>此内部构造函数不得作为公共 API 暴露</i>。
   *
   * @param classFileBuffer 包含待读取的 JVMS ClassFile 结构的字节数组。
   * @param classFileOffset byteBuffer 中待读取 ClassFile 第一个字节的偏移量。
   * @param checkClassVersion 是否检查类版本号。
   */
  ClassReader(
      final byte[] classFileBuffer, final int classFileOffset, final boolean checkClassVersion) {
    this.classFileBuffer = classFileBuffer;
    this.b = classFileBuffer;
    // 检查类的 major_version。该字段位于 magic 和 minor_version 字段之后，分别占用 4 和 2 个字节。
    if (checkClassVersion && readShort(classFileOffset + 6) > Opcodes.V19) {
      throw new IllegalArgumentException(
          "Unsupported class file major version " + readShort(classFileOffset + 6));
    }
    // 创建常量池数组。constant_pool_count 字段位于 magic、minor_version 和 major_version 字段之后，
    // 分别占用 4、2 和 2 个字节。
    int constantPoolCount = readUnsignedShort(classFileOffset + 8);
    cpInfoOffsets = new int[constantPoolCount];
    constantUtf8Values = new String[constantPoolCount];
    // 计算每个常量池项的偏移量，以及常量池字符串最大长度的保守估计值。
    // 第一个常量池项位于 magic、minor_version、major_version 和 constant_pool_count 字段之后，
    // 分别占用 4、2、2 和 2 个字节。
    int currentCpInfoIndex = 1;
    int currentCpInfoOffset = classFileOffset + 10;
    int currentMaxStringLength = 0;
    boolean hasBootstrapMethods = false;
    boolean hasConstantDynamic = false;
    // 其他项的偏移量取决于所有前驱项的总大小。
    while (currentCpInfoIndex < constantPoolCount) {
      cpInfoOffsets[currentCpInfoIndex++] = currentCpInfoOffset + 1;
      int cpInfoSize;
      switch (classFileBuffer[currentCpInfoOffset]) {
        case Symbol.CONSTANT_FIELDREF_TAG:
        case Symbol.CONSTANT_METHODREF_TAG:
        case Symbol.CONSTANT_INTERFACE_METHODREF_TAG:
        case Symbol.CONSTANT_INTEGER_TAG:
        case Symbol.CONSTANT_FLOAT_TAG:
        case Symbol.CONSTANT_NAME_AND_TYPE_TAG:
          cpInfoSize = 5;
          break;
        case Symbol.CONSTANT_DYNAMIC_TAG:
          cpInfoSize = 5;
          hasBootstrapMethods = true;
          hasConstantDynamic = true;
          break;
        case Symbol.CONSTANT_INVOKE_DYNAMIC_TAG:
          cpInfoSize = 5;
          hasBootstrapMethods = true;
          break;
        case Symbol.CONSTANT_LONG_TAG:
        case Symbol.CONSTANT_DOUBLE_TAG:
          cpInfoSize = 9;
          currentCpInfoIndex++;  // long 和 double 占两个常量池槽位
          break;
        case Symbol.CONSTANT_UTF8_TAG:
          cpInfoSize = 3 + readUnsignedShort(currentCpInfoOffset + 1);
          if (cpInfoSize > currentMaxStringLength) {
            // CONSTANT_Utf8 结构的字节大小可作为对应字符串字符长度的保守估计，
            // 相比精确计算长度，这种方式开销更小。
            currentMaxStringLength = cpInfoSize;
          }
          break;
        case Symbol.CONSTANT_METHOD_HANDLE_TAG:
          cpInfoSize = 4;
          break;
        case Symbol.CONSTANT_CLASS_TAG:
        case Symbol.CONSTANT_STRING_TAG:
        case Symbol.CONSTANT_METHOD_TYPE_TAG:
        case Symbol.CONSTANT_PACKAGE_TAG:
        case Symbol.CONSTANT_MODULE_TAG:
          cpInfoSize = 3;
          break;
        default:
          throw new IllegalArgumentException();
      }
      currentCpInfoOffset += cpInfoSize;
    }
    maxStringLength = currentMaxStringLength;
    // ClassFile 的 access_flags 字段紧接在最后一个常量池项之后。
    header = currentCpInfoOffset;

    // 如果存在至少一个 ConstantDynamic，则分配其缓存数组。
    constantDynamicValues = hasConstantDynamic ? new ConstantDynamic[constantPoolCount] : null;

    // 读取 BootstrapMethods 属性（如果存在），仅获取每个引导方法的偏移量。
    bootstrapMethodOffsets =
        hasBootstrapMethods ? readBootstrapMethodsAttribute(currentMaxStringLength) : null;
  }

  /**
   * 构造一个新的 {@link ClassReader} 对象。
   *
   * @param inputStream 要读取的 JVMS ClassFile 结构的输入流。该输入流中只能包含 ClassFile 结构本身，
   *     从当前指针位置读取到结束。
   * @throws IOException 如果读取过程中发生问题。
   */
  public ClassReader(final InputStream inputStream) throws IOException {
    this(readStream(inputStream, false));
  }

  /**
   * 构造一个新的 {@link ClassReader} 对象。
   *
   * @param className 要读取的类的完全限定名。ClassFile 结构通过当前类加载器的
   *     {@link ClassLoader#getSystemResourceAsStream} 获取。
   * @throws IOException 如果读取过程中发生异常。
   */
  public ClassReader(final String className) throws IOException {
    this(
        readStream(
            ClassLoader.getSystemResourceAsStream(className.replace('.', '/') + ".class"), true));
  }

  /**
   * 读取给定的输入流，并将其内容以字节数组形式返回。
   *
   * @param inputStream 输入流。
   * @param close 是否在读取完成后关闭输入流。
   * @return 给定输入流的内容字节数组。
   * @throws IOException 如果读取过程中发生问题。
   */
  private static byte[] readStream(final InputStream inputStream, final boolean close)
      throws IOException {
    if (inputStream == null) {
      throw new IOException("Class not found");
    }
    int bufferSize = computeBufferSize(inputStream);
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] data = new byte[bufferSize];
      int bytesRead;
      int readCount = 0;
      while ((bytesRead = inputStream.read(data, 0, bufferSize)) != -1) {
        outputStream.write(data, 0, bytesRead);
        readCount++;
      }
      outputStream.flush();
      if (readCount == 1) {
        // SPRING 补丁：某些行为异常的 InputStream 虽返回 -1 但仍会写入缓冲区（gh-27429）
        // return data;
        // 补丁结束
      }
      return outputStream.toByteArray();
    } finally {
      if (close) {
        inputStream.close();
      }
    }
  }

  private static int computeBufferSize(final InputStream inputStream) throws IOException {
    int expectedLength = inputStream.available();
    /*
     * 某些实现可能在仍有可用数据时返回 0（例如 new FileInputStream("/proc/a_file")）。
     * 此外，在某些极端情况下可能返回非常小的数值，此时我们使用默认大小。
     */
    if (expectedLength < 256) {
      return INPUT_STREAM_DATA_CHUNK_SIZE;
    }
    return Math.min(expectedLength, MAX_BUFFER_SIZE);
  }

  // -----------------------------------------------------------------------------------------------
  // 访问器方法（Accessors）
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回类的访问标志（参见 {@link Opcodes}）。当字节码版本低于 1.5 时，如果 Deprecated 和 Synthetic 标志通过属性表示，
   * 则此值可能不包含这两个标志。
   *
   * @return 类的访问标志。
   * @see ClassVisitor#visit(int, int, String, String, String, String[])
   */
  public int getAccess() {
    return readUnsignedShort(header);
  }

  /**
   * 返回类的内部名称（参见 {@link Type#getInternalName()}）。
   *
   * @return 类的内部名称。
   * @see ClassVisitor#visit(int, int, String, String, String, String[])
   */
  public String getClassName() {
    // this_class 紧随 access_flags 字段之后（占用 2 个字节）。
    return readClass(header + 2, new char[maxStringLength]);
  }

  /**
   * 返回超类的内部名称（参见 {@link Type#getInternalName()}）。对于接口，超类为 {@link Object}。
   *
   * @return 超类的内部名称，如果是 {@link Object} 类则返回 {@literal null}。
   * @see ClassVisitor#visit(int, int, String, String, String, String[])
   */
  public String getSuperName() {
    // super_class 位于 access_flags 和 this_class 字段之后（每个字段 2 字节）。
    return readClass(header + 4, new char[maxStringLength]);
  }

  /**
   * 返回实现的接口的内部名称（参见 {@link Type#getInternalName()}）。
   *
   * @return 直接实现的接口的内部名称。继承来的接口不返回。
   * @see ClassVisitor#visit(int, int, String, String, String, String[])
   */
  public String[] getInterfaces() {
    // interfaces_count 位于 access_flags、this_class 和 super_class 字段之后（每个字段 2 字节）。
    int currentOffset = header + 6;
    int interfacesCount = readUnsignedShort(currentOffset);
    String[] interfaces = new String[interfacesCount];
    if (interfacesCount > 0) {
      char[] charBuffer = new char[maxStringLength];
      for (int i = 0; i < interfacesCount; ++i) {
        currentOffset += 2;
        interfaces[i] = readClass(currentOffset, charBuffer);
      }
    }
    return interfaces;
  }

  // -----------------------------------------------------------------------------------------------
  // 公共方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 让给定的访问者访问传入构造函数的 JVMS ClassFile 结构。
   *
   * @param classVisitor 必须访问该类的访问者。
   * @param parsingOptions 用于解析该类的选项，可包含一个或多个 {@link #SKIP_CODE}、
   *     {@link #SKIP_DEBUG}、{@link #SKIP_FRAMES} 或 {@link #EXPAND_FRAMES}。
   */
  public void accept(final ClassVisitor classVisitor, final int parsingOptions) {
    accept(classVisitor, new Attribute[0], parsingOptions);
  }

  /**
   * 使给定的访问器访问传递给此类 {@link ClassReader} 构造函数的 JVMS ClassFile 结构。
   *
   * @param classVisitor 必须访问此类的访问器。
   * @param attributePrototypes 在访问类期间需要解析的属性原型。任何类型不等于这些原型之一的属性将不会被解析：
   *     其字节数组值将原样传递给 ClassWriter。<i>如果该值包含对常量池的引用，或与已被类适配器转换的类元素存在语法或语义关联，
   *     则可能导致数据损坏</i>。
   * @param parsingOptions 解析此类时使用的选项。可以是 {@link #SKIP_CODE}、{@link #SKIP_DEBUG}、{@link #SKIP_FRAMES}
   *     或 {@link #EXPAND_FRAMES} 中的一个或多个组合。
   */
  public void accept(
      final ClassVisitor classVisitor,
      final Attribute[] attributePrototypes,
      final int parsingOptions) {
    Context context = new Context();
    context.attributePrototypes = attributePrototypes;
    context.parsingOptions = parsingOptions;
    context.charBuffer = new char[maxStringLength];

    // 读取 access_flags、this_class、super_class、interface_count 和 interfaces 字段。
    char[] charBuffer = context.charBuffer;
    int currentOffset = header;
    int accessFlags = readUnsignedShort(currentOffset);
    String thisClass = readClass(currentOffset + 2, charBuffer);
    String superClass = readClass(currentOffset + 4, charBuffer);
    String[] interfaces = new String[readUnsignedShort(currentOffset + 6)];
    currentOffset += 8;
    for (int i = 0; i < interfaces.length; ++i) {
      interfaces[i] = readClass(currentOffset, charBuffer);
      currentOffset += 2;
    }

    // 读取类的属性（变量顺序遵循 JVMS 第 4.7 节的定义）。
    // 属性偏移量不包括 attribute_name_index 和 attribute_length 字段。
    // - InnerClasses 属性的偏移量，若不存在则为 0。
    int innerClassesOffset = 0;
    // - EnclosingMethod 属性的偏移量，若不存在则为 0。
    int enclosingMethodOffset = 0;
    // - 对应 Signature 属性的字符串，若不存在则为 null。
    String signature = null;
    // - 对应 SourceFile 属性的字符串，若不存在则为 null。
    String sourceFile = null;
    // - 对应 SourceDebugExtension 属性的字符串，若不存在则为 null。
    String sourceDebugExtension = null;
    // - RuntimeVisibleAnnotations 属性的偏移量，若不存在则为 0。
    int runtimeVisibleAnnotationsOffset = 0;
    // - RuntimeInvisibleAnnotations 属性的偏移量，若不存在则为 0。
    int runtimeInvisibleAnnotationsOffset = 0;
    // - RuntimeVisibleTypeAnnotations 属性的偏移量，若不存在则为 0。
    int runtimeVisibleTypeAnnotationsOffset = 0;
    // - RuntimeInvisibleTypeAnnotations 属性的偏移量，若不存在则为 0。
    int runtimeInvisibleTypeAnnotationsOffset = 0;
    // - Module 属性的偏移量，若不存在则为 0。
    int moduleOffset = 0;
    // - ModulePackages 属性的偏移量，若不存在则为 0。
    int modulePackagesOffset = 0;
    // - 对应 ModuleMainClass 属性的字符串，若不存在则为 null。
    String moduleMainClass = null;
    // - 对应 NestHost 属性的字符串，若不存在则为 null。
    String nestHostClass = null;
    // - NestMembers 属性的偏移量，若不存在则为 0。
    int nestMembersOffset = 0;
    // - PermittedSubclasses 属性的偏移量，若不存在则为 0。
    int permittedSubclassesOffset = 0;
    // - Record 属性的偏移量，若不存在则为 0。
    int recordOffset = 0;
    // - 非标准属性（通过 {@link Attribute#nextAttribute} 字段链接）。该列表顺序为 ClassFile 中属性顺序的 <i>逆序</i>。
    Attribute attributes = null;

    int currentAttributeOffset = getFirstAttributeOffset();
    for (int i = readUnsignedShort(currentAttributeOffset - 2); i > 0; --i) {
      // 读取 attribute_info 的 attribute_name 和 attribute_length 字段。
      String attributeName = readUTF8(currentAttributeOffset, charBuffer);
      int attributeLength = readInt(currentAttributeOffset + 2);
      currentAttributeOffset += 6;
      // 测试按出现频率降序排列（基于典型类中的观察频率）。
      if (Constants.SOURCE_FILE.equals(attributeName)) {
        sourceFile = readUTF8(currentAttributeOffset, charBuffer);
      } else if (Constants.INNER_CLASSES.equals(attributeName)) {
        innerClassesOffset = currentAttributeOffset;
      } else if (Constants.ENCLOSING_METHOD.equals(attributeName)) {
        enclosingMethodOffset = currentAttributeOffset;
      } else if (Constants.NEST_HOST.equals(attributeName)) {
        nestHostClass = readClass(currentAttributeOffset, charBuffer);
      } else if (Constants.NEST_MEMBERS.equals(attributeName)) {
        nestMembersOffset = currentAttributeOffset;
      } else if (Constants.PERMITTED_SUBCLASSES.equals(attributeName)) {
        permittedSubclassesOffset = currentAttributeOffset;
      } else if (Constants.SIGNATURE.equals(attributeName)) {
        signature = readUTF8(currentAttributeOffset, charBuffer);
      } else if (Constants.RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleAnnotationsOffset = currentAttributeOffset;
      } else if (Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleTypeAnnotationsOffset = currentAttributeOffset;
      } else if (Constants.DEPRECATED.equals(attributeName)) {
        accessFlags |= Opcodes.ACC_DEPRECATED;
      } else if (Constants.SYNTHETIC.equals(attributeName)) {
        accessFlags |= Opcodes.ACC_SYNTHETIC;
      } else if (Constants.SOURCE_DEBUG_EXTENSION.equals(attributeName)) {
        if (attributeLength > classFileBuffer.length - currentAttributeOffset) {
          throw new IllegalArgumentException();
        }
        sourceDebugExtension =
            readUtf(currentAttributeOffset, attributeLength, new char[attributeLength]);
      } else if (Constants.RUNTIME_INVISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleAnnotationsOffset = currentAttributeOffset;
      } else if (Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleTypeAnnotationsOffset = currentAttributeOffset;
      } else if (Constants.RECORD.equals(attributeName)) {
        recordOffset = currentAttributeOffset;
        accessFlags |= Opcodes.ACC_RECORD;
      } else if (Constants.MODULE.equals(attributeName)) {
        moduleOffset = currentAttributeOffset;
      } else if (Constants.MODULE_MAIN_CLASS.equals(attributeName)) {
        moduleMainClass = readClass(currentAttributeOffset, charBuffer);
      } else if (Constants.MODULE_PACKAGES.equals(attributeName)) {
        modulePackagesOffset = currentAttributeOffset;
      } else if (!Constants.BOOTSTRAP_METHODS.equals(attributeName)) {
        // BootstrapMethods 属性已在构造函数中读取。
        Attribute attribute =
            readAttribute(
                attributePrototypes,
                attributeName,
                currentAttributeOffset,
                attributeLength,
                charBuffer,
                -1,
                null);
        attribute.nextAttribute = attributes;
        attributes = attribute;
      }
      currentAttributeOffset += attributeLength;
    }

    // 访问类声明。minor_version 和 major_version 字段位于第一个常量池条目前 6 字节处，
    // 而常量池条目本身从 cpInfoOffsets[1] - 1 开始（根据定义）。
    classVisitor.visit(
        readInt(cpInfoOffsets[1] - 7), accessFlags, thisClass, signature, superClass, interfaces);

    // 访问 SourceFile 和 SourceDebugExtension 属性。
    if ((parsingOptions & SKIP_DEBUG) == 0
        && (sourceFile != null || sourceDebugExtension != null)) {
      classVisitor.visitSource(sourceFile, sourceDebugExtension);
    }

    // 访问 Module、ModulePackages 和 ModuleMainClass 属性。
    if (moduleOffset != 0) {
      readModuleAttributes(
          classVisitor, context, moduleOffset, modulePackagesOffset, moduleMainClass);
    }

    // 访问 NestHost 属性。
    if (nestHostClass != null) {
      classVisitor.visitNestHost(nestHostClass);
    }

    // 访问 EnclosingMethod 属性。
    if (enclosingMethodOffset != 0) {
      String className = readClass(enclosingMethodOffset, charBuffer);
      int methodIndex = readUnsignedShort(enclosingMethodOffset + 2);
      String name = methodIndex == 0 ? null : readUTF8(cpInfoOffsets[methodIndex], charBuffer);
      String type = methodIndex == 0 ? null : readUTF8(cpInfoOffsets[methodIndex] + 2, charBuffer);
      classVisitor.visitOuterClass(className, name, type);
    }

    // 访问 RuntimeVisibleAnnotations 属性。
    if (runtimeVisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                classVisitor.visitAnnotation(annotationDescriptor, /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleAnnotations 属性。
    if (runtimeInvisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                classVisitor.visitAnnotation(annotationDescriptor, /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeVisibleTypeAnnotations 属性。
    if (runtimeVisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                classVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleTypeAnnotations 属性。
    if (runtimeInvisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                classVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问非标准属性。
    while (attributes != null) {
      // 复制并重置 nextAttribute 字段，以便其也可在 ClassWriter 中使用。
      Attribute nextAttribute = attributes.nextAttribute;
      attributes.nextAttribute = null;
      classVisitor.visitAttribute(attributes);
      attributes = nextAttribute;
    }

    // 访问 NestedMembers 属性。
    if (nestMembersOffset != 0) {
      int numberOfNestMembers = readUnsignedShort(nestMembersOffset);
      int currentNestMemberOffset = nestMembersOffset + 2;
      while (numberOfNestMembers-- > 0) {
        classVisitor.visitNestMember(readClass(currentNestMemberOffset, charBuffer));
        currentNestMemberOffset += 2;
      }
    }

    // 访问 PermittedSubclasses 属性。
    if (permittedSubclassesOffset != 0) {
      int numberOfPermittedSubclasses = readUnsignedShort(permittedSubclassesOffset);
      int currentPermittedSubclassesOffset = permittedSubclassesOffset + 2;
      while (numberOfPermittedSubclasses-- > 0) {
        classVisitor.visitPermittedSubclass(
            readClass(currentPermittedSubclassesOffset, charBuffer));
        currentPermittedSubclassesOffset += 2;
      }
    }

    // 访问 InnerClasses 属性。
    if (innerClassesOffset != 0) {
      int numberOfClasses = readUnsignedShort(innerClassesOffset);
      int currentClassesOffset = innerClassesOffset + 2;
      while (numberOfClasses-- > 0) {
        classVisitor.visitInnerClass(
            readClass(currentClassesOffset, charBuffer),
            readClass(currentClassesOffset + 2, charBuffer),
            readUTF8(currentClassesOffset + 4, charBuffer),
            readUnsignedShort(currentClassesOffset + 6));
        currentClassesOffset += 8;
      }
    }

    // 访问 Record 组件。
    if (recordOffset != 0) {
      int recordComponentsCount = readUnsignedShort(recordOffset);
      recordOffset += 2;
      while (recordComponentsCount-- > 0) {
        recordOffset = readRecordComponent(classVisitor, context, recordOffset);
      }
    }

    // 访问字段和方法。
    int fieldsCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (fieldsCount-- > 0) {
      currentOffset = readField(classVisitor, context, currentOffset);
    }
    int methodsCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (methodsCount-- > 0) {
      currentOffset = readMethod(classVisitor, context, currentOffset);
    }

    // 访问类的结束部分。
    classVisitor.visitEnd();
  }

  // ----------------------------------------------------------------------------------------------
  // 用于解析模块、字段和方法的方法
  // ----------------------------------------------------------------------------------------------

  /**
   * 读取 Module、ModulePackages 和 ModuleMainClass 属性并访问它们。
   *
   * @param classVisitor 当前的类访问器
   * @param context 正在解析的类的相关信息
   * @param moduleOffset Module 属性的偏移量（不包括 attribute_info 中的 attribute_name_index 和 attribute_length 字段）
   * @param modulePackagesOffset ModulePackages 属性的偏移量（不包括 attribute_info 中的 attribute_name_index 和 attribute_length 字段），若不存在则为 0
   * @param moduleMainClass 对应 ModuleMainClass 属性的字符串，若不存在则为 {@literal null}
   */
  private void readModuleAttributes(
      final ClassVisitor classVisitor,
      final Context context,
      final int moduleOffset,
      final int modulePackagesOffset,
      final String moduleMainClass) {
    char[] buffer = context.charBuffer;

    // 读取 module_name_index、module_flags 和 module_version_index 字段，并进行访问。
    int currentOffset = moduleOffset;
    String moduleName = readModule(currentOffset, buffer);
    int moduleFlags = readUnsignedShort(currentOffset + 2);
    String moduleVersion = readUTF8(currentOffset + 4, buffer);
    currentOffset += 6;
    ModuleVisitor moduleVisitor = classVisitor.visitModule(moduleName, moduleFlags, moduleVersion);
    if (moduleVisitor == null) {
      return;
    }

    // 访问 ModuleMainClass 属性。
    if (moduleMainClass != null) {
      moduleVisitor.visitMainClass(moduleMainClass);
    }

    // 访问 ModulePackages 属性。
    if (modulePackagesOffset != 0) {
      int packageCount = readUnsignedShort(modulePackagesOffset);
      int currentPackageOffset = modulePackagesOffset + 2;
      while (packageCount-- > 0) {
        moduleVisitor.visitPackage(readPackage(currentPackageOffset, buffer));
        currentPackageOffset += 2;
      }
    }

    // 读取 'requires_count' 和 'requires' 字段。
    int requiresCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (requiresCount-- > 0) {
      // 读取 requires_index、requires_flags 和 requires_version 字段，并进行访问。
      String requires = readModule(currentOffset, buffer);
      int requiresFlags = readUnsignedShort(currentOffset + 2);
      String requiresVersion = readUTF8(currentOffset + 4, buffer);
      currentOffset += 6;
      moduleVisitor.visitRequire(requires, requiresFlags, requiresVersion);
    }

    // 读取 'exports_count' 和 'exports' 字段。
    int exportsCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (exportsCount-- > 0) {
      // 读取 exports_index、exports_flags、exports_to_count 和 exports_to_index 字段，并进行访问。
      String exports = readPackage(currentOffset, buffer);
      int exportsFlags = readUnsignedShort(currentOffset + 2);
      int exportsToCount = readUnsignedShort(currentOffset + 4);
      currentOffset += 6;
      String[] exportsTo = null;
      if (exportsToCount != 0) {
        exportsTo = new String[exportsToCount];
        for (int i = 0; i < exportsToCount; ++i) {
          exportsTo[i] = readModule(currentOffset, buffer);
          currentOffset += 2;
        }
      }
      moduleVisitor.visitExport(exports, exportsFlags, exportsTo);
    }

    // 读取 'opens_count' 和 'opens' 字段。
    int opensCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (opensCount-- > 0) {
      // 读取 opens_index、opens_flags、opens_to_count 和 opens_to_index 字段，并进行访问。
      String opens = readPackage(currentOffset, buffer);
      int opensFlags = readUnsignedShort(currentOffset + 2);
      int opensToCount = readUnsignedShort(currentOffset + 4);
      currentOffset += 6;
      String[] opensTo = null;
      if (opensToCount != 0) {
        opensTo = new String[opensToCount];
        for (int i = 0; i < opensToCount; ++i) {
          opensTo[i] = readModule(currentOffset, buffer);
          currentOffset += 2;
        }
      }
      moduleVisitor.visitOpen(opens, opensFlags, opensTo);
    }

    // 读取 'uses_count' 和 'uses' 字段。
    int usesCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (usesCount-- > 0) {
      moduleVisitor.visitUse(readClass(currentOffset, buffer));
      currentOffset += 2;
    }

    // 读取 'provides_count' 和 'provides' 字段。
    int providesCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (providesCount-- > 0) {
      // 读取 provides_index、provides_with_count 和 provides_with_index 字段，并进行访问。
      String provides = readClass(currentOffset, buffer);
      int providesWithCount = readUnsignedShort(currentOffset + 2);
      currentOffset += 4;
      String[] providesWith = new String[providesWithCount];
      for (int i = 0; i < providesWithCount; ++i) {
        providesWith[i] = readClass(currentOffset, buffer);
        currentOffset += 2;
      }
      moduleVisitor.visitProvide(provides, providesWith);
    }

    // 访问模块属性的结束部分。
    moduleVisitor.visitEnd();
  }

  /**
   * 读取一个记录组件并访问它。
   *
   * @param classVisitor 当前的类访问者
   * @param context 关于正在解析的类的信息
   * @param recordComponentOffset 当前记录组件的偏移量
   * @return 记录组件之后的第一个字节的偏移量
   */
  private int readRecordComponent(
      final ClassVisitor classVisitor, final Context context, final int recordComponentOffset) {
    char[] charBuffer = context.charBuffer;

    int currentOffset = recordComponentOffset;
    String name = readUTF8(currentOffset, charBuffer);
    String descriptor = readUTF8(currentOffset + 2, charBuffer);
    currentOffset += 4;

    // 读取记录组件的属性（变量顺序与 JVMS 第 4.7 节一致）。

    // 属性偏移量不包括 attribute_name_index 和 attribute_length 字段。
    // - 对应 Signature 属性的字符串，或 null。
    String signature = null;
    // - RuntimeVisibleAnnotations 属性的偏移量，或 0。
    int runtimeVisibleAnnotationsOffset = 0;
    // - RuntimeInvisibleAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleAnnotationsOffset = 0;
    // - RuntimeVisibleTypeAnnotations 属性的偏移量，或 0。
    int runtimeVisibleTypeAnnotationsOffset = 0;
    // - RuntimeInvisibleTypeAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleTypeAnnotationsOffset = 0;
    // - 非标准属性（通过其 {@link Attribute#nextAttribute} 字段连接）。
    //   该列表按照 ClassFile 结构中的顺序 <i>逆序</i> 链接。
    Attribute attributes = null;

    int attributesCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (attributesCount-- > 0) {
      // 读取 attribute_info 的 attribute_name 和 attribute_length 字段。
      String attributeName = readUTF8(currentOffset, charBuffer);
      int attributeLength = readInt(currentOffset + 2);
      currentOffset += 6;
      // 以下判断按出现频率从高到低排序（基于典型类的观察频率）。
      if (Constants.SIGNATURE.equals(attributeName)) {
        signature = readUTF8(currentOffset, charBuffer);
      } else if (Constants.RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleTypeAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_INVISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleTypeAnnotationsOffset = currentOffset;
      } else {
        Attribute attribute =
            readAttribute(
                context.attributePrototypes,
                attributeName,
                currentOffset,
                attributeLength,
                charBuffer,
                -1,
                null);
        attribute.nextAttribute = attributes;
        attributes = attribute;
      }
      currentOffset += attributeLength;
    }

    RecordComponentVisitor recordComponentVisitor =
        classVisitor.visitRecordComponent(name, descriptor, signature);
    if (recordComponentVisitor == null) {
      return currentOffset;
    }

    // 访问 RuntimeVisibleAnnotations 属性。
    if (runtimeVisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                recordComponentVisitor.visitAnnotation(annotationDescriptor, /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleAnnotations 属性。
    if (runtimeInvisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                recordComponentVisitor.visitAnnotation(annotationDescriptor, /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeVisibleTypeAnnotations 属性。
    if (runtimeVisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                recordComponentVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleTypeAnnotations 属性。
    if (runtimeInvisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                recordComponentVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问非标准属性。
    while (attributes != null) {
      // 复制并重置 nextAttribute 字段，以便在 FieldWriter 中也能使用。
      Attribute nextAttribute = attributes.nextAttribute;
      attributes.nextAttribute = null;
      recordComponentVisitor.visitAttribute(attributes);
      attributes = nextAttribute;
    }

    // 访问记录组件结束。
    recordComponentVisitor.visitEnd();
    return currentOffset;
  }

  /**
   * 读取 JVMS 的 field_info 结构，并让给定的访问器访问它。
   *
   * @param classVisitor      必须访问该字段的访问器。
   * @param context           有关正在解析的类的信息。
   * @param fieldInfoOffset   field_info 结构的起始偏移量。
   * @return 返回紧随 field_info 结构之后的第一个字节的偏移量。
   */
  private int readField(
      final ClassVisitor classVisitor, final Context context, final int fieldInfoOffset) {
    char[] charBuffer = context.charBuffer;

    // 读取 access_flags、name_index 和 descriptor_index 字段。
    int currentOffset = fieldInfoOffset;
    int accessFlags = readUnsignedShort(currentOffset);
    String name = readUTF8(currentOffset + 2, charBuffer);
    String descriptor = readUTF8(currentOffset + 4, charBuffer);
    currentOffset += 6;

    // 读取字段属性（变量顺序与 JVMS 第 4.7 节中一致）。
    // 属性偏移量不包括 attribute_name_index 和 attribute_length 字段。
    // - 对应 ConstantValue 属性的值，或 null。
    Object constantValue = null;
    // - 对应 Signature 属性的字符串，或 null。
    String signature = null;
    // - RuntimeVisibleAnnotations 属性的偏移量，或 0。
    int runtimeVisibleAnnotationsOffset = 0;
    // - RuntimeInvisibleAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleAnnotationsOffset = 0;
    // - RuntimeVisibleTypeAnnotations 属性的偏移量，或 0。
    int runtimeVisibleTypeAnnotationsOffset = 0;
    // - RuntimeInvisibleTypeAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleTypeAnnotationsOffset = 0;
    // - 非标准属性（通过其 {@link Attribute#nextAttribute} 字段连接）。
    //   该列表按照 ClassFile 结构中的顺序 <i>逆序</i> 链接。
    Attribute attributes = null;

    int attributesCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (attributesCount-- > 0) {
      // 读取 attribute_info 的 attribute_name 和 attribute_length 字段。
      String attributeName = readUTF8(currentOffset, charBuffer);
      int attributeLength = readInt(currentOffset + 2);
      currentOffset += 6;
      // 以下判断按出现频率从高到低排序（基于典型类的观察频率）。
      if (Constants.CONSTANT_VALUE.equals(attributeName)) {
        int constantvalueIndex = readUnsignedShort(currentOffset);
        constantValue = constantvalueIndex == 0 ? null : readConst(constantvalueIndex, charBuffer);
      } else if (Constants.SIGNATURE.equals(attributeName)) {
        signature = readUTF8(currentOffset, charBuffer);
      } else if (Constants.DEPRECATED.equals(attributeName)) {
        accessFlags |= Opcodes.ACC_DEPRECATED;
      } else if (Constants.SYNTHETIC.equals(attributeName)) {
        accessFlags |= Opcodes.ACC_SYNTHETIC;
      } else if (Constants.RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleTypeAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_INVISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleTypeAnnotationsOffset = currentOffset;
      } else {
        Attribute attribute =
            readAttribute(
                context.attributePrototypes,
                attributeName,
                currentOffset,
                attributeLength,
                charBuffer,
                -1,
                null);
        attribute.nextAttribute = attributes;
        attributes = attribute;
      }
      currentOffset += attributeLength;
    }

    // 访问字段声明。
    FieldVisitor fieldVisitor =
        classVisitor.visitField(accessFlags, name, descriptor, signature, constantValue);
    if (fieldVisitor == null) {
      return currentOffset;
    }

    // 访问 RuntimeVisibleAnnotations 属性。
    if (runtimeVisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                fieldVisitor.visitAnnotation(annotationDescriptor, /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleAnnotations 属性。
    if (runtimeInvisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                fieldVisitor.visitAnnotation(annotationDescriptor, /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeVisibleTypeAnnotations 属性。
    if (runtimeVisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                fieldVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleTypeAnnotations 属性。
    if (runtimeInvisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs，并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                fieldVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问非标准属性。
    while (attributes != null) {
      // 复制并重置 nextAttribute 字段，以便在 FieldWriter 中也能使用。
      Attribute nextAttribute = attributes.nextAttribute;
      attributes.nextAttribute = null;
      fieldVisitor.visitAttribute(attributes);
      attributes = nextAttribute;
    }

    // 访问字段结束。
    fieldVisitor.visitEnd();
    return currentOffset;
  }

  /**
   * 读取 JVMS 的 method_info 结构，并让给定的访问器访问它。
   *
   * @param classVisitor      必须访问该方法的访问器。
   * @param context           有关正在解析的类的信息。
   * @param methodInfoOffset  method_info 结构的起始偏移量。
   * @return 返回紧随 method_info 结构之后的第一个字节的偏移量。
   */
  private int readMethod(
      final ClassVisitor classVisitor, final Context context, final int methodInfoOffset) {
    char[] charBuffer = context.charBuffer;

    // 读取 access_flags、name_index 和 descriptor_index 字段。
    int currentOffset = methodInfoOffset;
    context.currentMethodAccessFlags = readUnsignedShort(currentOffset);
    context.currentMethodName = readUTF8(currentOffset + 2, charBuffer);
    context.currentMethodDescriptor = readUTF8(currentOffset + 4, charBuffer);
    currentOffset += 6;

    // 读取方法属性（变量顺序遵循 JVMS 第 4.7 节）。
    // 属性偏移量不包括 attribute_name_index 和 attribute_length 字段。
    // - Code 属性的偏移量，或 0。
    int codeOffset = 0;
    // - Exceptions 属性的偏移量，或 0。
    int exceptionsOffset = 0;
    // - 对应 Exceptions 属性的字符串，或 null。
    String[] exceptions = null;
    // - 方法是否有 Synthetic 属性。
    boolean synthetic = false;
    // - Signature 属性中包含的常量池索引，或 0。
    int signatureIndex = 0;
    // - RuntimeVisibleAnnotations 属性的偏移量，或 0。
    int runtimeVisibleAnnotationsOffset = 0;
    // - RuntimeInvisibleAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleAnnotationsOffset = 0;
    // - RuntimeVisibleParameterAnnotations 属性的偏移量，或 0。
    int runtimeVisibleParameterAnnotationsOffset = 0;
    // - RuntimeInvisibleParameterAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleParameterAnnotationsOffset = 0;
    // - RuntimeVisibleTypeAnnotations 属性的偏移量，或 0。
    int runtimeVisibleTypeAnnotationsOffset = 0;
    // - RuntimeInvisibleTypeAnnotations 属性的偏移量，或 0。
    int runtimeInvisibleTypeAnnotationsOffset = 0;
    // - AnnotationDefault 属性的偏移量，或 0。
    int annotationDefaultOffset = 0;
    // - MethodParameters 属性的偏移量，或 0。
    int methodParametersOffset = 0;
    // - 非标准属性（通过 {@link Attribute#nextAttribute} 链接）。
    //   此列表按 ClassFile 结构中的顺序的 <i>逆序</i>。
    Attribute attributes = null;

    int attributesCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (attributesCount-- > 0) {
      // 读取 attribute_info 的 attribute_name 和 attribute_length 字段。
      String attributeName = readUTF8(currentOffset, charBuffer);
      int attributeLength = readInt(currentOffset + 2);
      currentOffset += 6;
      // 按出现频率降序排序（基于典型类观察到的频率）。
      if (Constants.CODE.equals(attributeName)) {
        if ((context.parsingOptions & SKIP_CODE) == 0) {
          codeOffset = currentOffset;
        }
      } else if (Constants.EXCEPTIONS.equals(attributeName)) {
        exceptionsOffset = currentOffset;
        exceptions = new String[readUnsignedShort(exceptionsOffset)];
        int currentExceptionOffset = exceptionsOffset + 2;
        for (int i = 0; i < exceptions.length; ++i) {
          exceptions[i] = readClass(currentExceptionOffset, charBuffer);
          currentExceptionOffset += 2;
        }
      } else if (Constants.SIGNATURE.equals(attributeName)) {
        signatureIndex = readUnsignedShort(currentOffset);
      } else if (Constants.DEPRECATED.equals(attributeName)) {
        context.currentMethodAccessFlags |= Opcodes.ACC_DEPRECATED;
      } else if (Constants.RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleTypeAnnotationsOffset = currentOffset;
      } else if (Constants.ANNOTATION_DEFAULT.equals(attributeName)) {
        annotationDefaultOffset = currentOffset;
      } else if (Constants.SYNTHETIC.equals(attributeName)) {
        synthetic = true;
        context.currentMethodAccessFlags |= Opcodes.ACC_SYNTHETIC;
      } else if (Constants.RUNTIME_INVISIBLE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleTypeAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS.equals(attributeName)) {
        runtimeVisibleParameterAnnotationsOffset = currentOffset;
      } else if (Constants.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS.equals(attributeName)) {
        runtimeInvisibleParameterAnnotationsOffset = currentOffset;
      } else if (Constants.METHOD_PARAMETERS.equals(attributeName)) {
        methodParametersOffset = currentOffset;
      } else {
        Attribute attribute =
            readAttribute(
                context.attributePrototypes,
                attributeName,
                currentOffset,
                attributeLength,
                charBuffer,
                -1,
                null);
        attribute.nextAttribute = attributes;
        attributes = attribute;
      }
      currentOffset += attributeLength;
    }

    // 访问方法声明。
    MethodVisitor methodVisitor =
        classVisitor.visitMethod(
            context.currentMethodAccessFlags,
            context.currentMethodName,
            context.currentMethodDescriptor,
            signatureIndex == 0 ? null : readUtf(signatureIndex, charBuffer),
            exceptions);
    if (methodVisitor == null) {
      return currentOffset;
    }

    // 如果返回的 MethodVisitor 实际上是 MethodWriter，表示 reader 与 writer 之间没有方法适配器。
    // 在这种情况下，可能直接将方法属性复制到 writer 中。如果可以，则提前返回而不访问这些属性的内容。
    if (methodVisitor instanceof MethodWriter) {
      MethodWriter methodWriter = (MethodWriter) methodVisitor;
      if (methodWriter.canCopyMethodAttributes(
          this,
          synthetic,
          (context.currentMethodAccessFlags & Opcodes.ACC_DEPRECATED) != 0,
          readUnsignedShort(methodInfoOffset + 4),
          signatureIndex,
          exceptionsOffset)) {
        methodWriter.setMethodAttributesSource(methodInfoOffset, currentOffset - methodInfoOffset);
        return currentOffset;
      }
    }


    // 访问 MethodParameters 属性。
    if (methodParametersOffset != 0 && (context.parsingOptions & SKIP_DEBUG) == 0) {
      int parametersCount = readByte(methodParametersOffset);
      int currentParameterOffset = methodParametersOffset + 1;
      while (parametersCount-- > 0) {
        // 读取 name_index 和 access_flags 字段并访问它们。
        methodVisitor.visitParameter(
            readUTF8(currentParameterOffset, charBuffer),
            readUnsignedShort(currentParameterOffset + 2));
        currentParameterOffset += 4;
      }
    }

    // 访问 AnnotationDefault 属性。
    if (annotationDefaultOffset != 0) {
      AnnotationVisitor annotationVisitor = methodVisitor.visitAnnotationDefault();
      readElementValue(annotationVisitor, annotationDefaultOffset, null, charBuffer);
      if (annotationVisitor != null) {
        annotationVisitor.visitEnd();
      }
    }

    // 访问 RuntimeVisibleAnnotations 属性。
    if (runtimeVisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                methodVisitor.visitAnnotation(annotationDescriptor, /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeInvisibleAnnotations 属性。
    if (runtimeInvisibleAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                methodVisitor.visitAnnotation(annotationDescriptor, /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeVisibleTypeAnnotations 属性。
    if (runtimeVisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeVisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeVisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                methodVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ true),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }


    // 访问 RuntimeInvisibleTypeAnnotations 属性。
    if (runtimeInvisibleTypeAnnotationsOffset != 0) {
      int numAnnotations = readUnsignedShort(runtimeInvisibleTypeAnnotationsOffset);
      int currentAnnotationOffset = runtimeInvisibleTypeAnnotationsOffset + 2;
      while (numAnnotations-- > 0) {
        // 解析 target_type、target_info 和 target_path 字段。
        currentAnnotationOffset = readTypeAnnotationTarget(context, currentAnnotationOffset);
        // 解析 type_index 字段。
        String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
        currentAnnotationOffset += 2;
        // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
        currentAnnotationOffset =
            readElementValues(
                methodVisitor.visitTypeAnnotation(
                    context.currentTypeAnnotationTarget,
                    context.currentTypeAnnotationTargetPath,
                    annotationDescriptor,
                    /* visible = */ false),
                currentAnnotationOffset,
                /* named = */ true,
                charBuffer);
      }
    }

    // 访问 RuntimeVisibleParameterAnnotations 属性。
    if (runtimeVisibleParameterAnnotationsOffset != 0) {
      readParameterAnnotations(
          methodVisitor, context, runtimeVisibleParameterAnnotationsOffset, /* visible = */ true);
    }

    // 访问 RuntimeInvisibleParameterAnnotations 属性。
    if (runtimeInvisibleParameterAnnotationsOffset != 0) {
      readParameterAnnotations(
          methodVisitor,
          context,
          runtimeInvisibleParameterAnnotationsOffset,
          /* visible = */ false);
    }

    // 访问非标准属性。
    while (attributes != null) {
      // 复制并重置 nextAttribute 字段，以便在 MethodWriter 中也可以使用。
      Attribute nextAttribute = attributes.nextAttribute;
      attributes.nextAttribute = null;
      methodVisitor.visitAttribute(attributes);
      attributes = nextAttribute;
    }

    // 访问 Code 属性。
    if (codeOffset != 0) {
      methodVisitor.visitCode();
      readCode(methodVisitor, context, codeOffset);
    }

    // 访问方法结束。
    methodVisitor.visitEnd();
    return currentOffset;
  }

  // ----------------------------------------------------------------------------------------------
  // 解析 Code 属性的方法
  // ----------------------------------------------------------------------------------------------

  /**
   * 读取 JVMS 的 “Code” 属性，并让给定的访问器访问它。
   *
   * @param methodVisitor 必须访问 Code 属性的访问器。
   * @param context       有关正在解析的类的信息。
   * @param codeOffset    Code 属性在 {@link #classFileBuffer} 中的起始偏移量，
   *                      不包括 attribute_name_index 和 attribute_length 字段。
   */
  private void readCode(
      final MethodVisitor methodVisitor, final Context context, final int codeOffset) {
    int currentOffset = codeOffset;

    // Read the max_stack, max_locals and code_length fields.
    final byte[] classBuffer = classFileBuffer;
    final char[] charBuffer = context.charBuffer;
    final int maxStack = readUnsignedShort(currentOffset);
    final int maxLocals = readUnsignedShort(currentOffset + 2);
    final int codeLength = readInt(currentOffset + 4);
    currentOffset += 8;
    if (codeLength > classFileBuffer.length - currentOffset) {
      throw new IllegalArgumentException();
    }

// 读取字节码中的 'code' 数组，为每个被引用的指令创建一个标签。
    final int bytecodeStartOffset = currentOffset;
    final int bytecodeEndOffset = currentOffset + codeLength;
    final Label[] labels = context.currentMethodLabels = new Label[codeLength + 1];
    while (currentOffset < bytecodeEndOffset) {
      final int bytecodeOffset = currentOffset - bytecodeStartOffset;
      final int opcode = classBuffer[currentOffset] & 0xFF;
      switch (opcode) {
        case Opcodes.NOP:
        case Opcodes.ACONST_NULL:
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
        case Opcodes.IALOAD:
        case Opcodes.LALOAD:
        case Opcodes.FALOAD:
        case Opcodes.DALOAD:
        case Opcodes.AALOAD:
        case Opcodes.BALOAD:
        case Opcodes.CALOAD:
        case Opcodes.SALOAD:
        case Opcodes.IASTORE:
        case Opcodes.LASTORE:
        case Opcodes.FASTORE:
        case Opcodes.DASTORE:
        case Opcodes.AASTORE:
        case Opcodes.BASTORE:
        case Opcodes.CASTORE:
        case Opcodes.SASTORE:
        case Opcodes.POP:
        case Opcodes.POP2:
        case Opcodes.DUP:
        case Opcodes.DUP_X1:
        case Opcodes.DUP_X2:
        case Opcodes.DUP2:
        case Opcodes.DUP2_X1:
        case Opcodes.DUP2_X2:
        case Opcodes.SWAP:
        case Opcodes.IADD:
        case Opcodes.LADD:
        case Opcodes.FADD:
        case Opcodes.DADD:
        case Opcodes.ISUB:
        case Opcodes.LSUB:
        case Opcodes.FSUB:
        case Opcodes.DSUB:
        case Opcodes.IMUL:
        case Opcodes.LMUL:
        case Opcodes.FMUL:
        case Opcodes.DMUL:
        case Opcodes.IDIV:
        case Opcodes.LDIV:
        case Opcodes.FDIV:
        case Opcodes.DDIV:
        case Opcodes.IREM:
        case Opcodes.LREM:
        case Opcodes.FREM:
        case Opcodes.DREM:
        case Opcodes.INEG:
        case Opcodes.LNEG:
        case Opcodes.FNEG:
        case Opcodes.DNEG:
        case Opcodes.ISHL:
        case Opcodes.LSHL:
        case Opcodes.ISHR:
        case Opcodes.LSHR:
        case Opcodes.IUSHR:
        case Opcodes.LUSHR:
        case Opcodes.IAND:
        case Opcodes.LAND:
        case Opcodes.IOR:
        case Opcodes.LOR:
        case Opcodes.IXOR:
        case Opcodes.LXOR:
        case Opcodes.I2L:
        case Opcodes.I2F:
        case Opcodes.I2D:
        case Opcodes.L2I:
        case Opcodes.L2F:
        case Opcodes.L2D:
        case Opcodes.F2I:
        case Opcodes.F2L:
        case Opcodes.F2D:
        case Opcodes.D2I:
        case Opcodes.D2L:
        case Opcodes.D2F:
        case Opcodes.I2B:
        case Opcodes.I2C:
        case Opcodes.I2S:
        case Opcodes.LCMP:
        case Opcodes.FCMPL:
        case Opcodes.FCMPG:
        case Opcodes.DCMPL:
        case Opcodes.DCMPG:
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.DRETURN:
        case Opcodes.ARETURN:
        case Opcodes.RETURN:
        case Opcodes.ARRAYLENGTH:
        case Opcodes.ATHROW:
        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
        case Constants.ILOAD_0:
        case Constants.ILOAD_1:
        case Constants.ILOAD_2:
        case Constants.ILOAD_3:
        case Constants.LLOAD_0:
        case Constants.LLOAD_1:
        case Constants.LLOAD_2:
        case Constants.LLOAD_3:
        case Constants.FLOAD_0:
        case Constants.FLOAD_1:
        case Constants.FLOAD_2:
        case Constants.FLOAD_3:
        case Constants.DLOAD_0:
        case Constants.DLOAD_1:
        case Constants.DLOAD_2:
        case Constants.DLOAD_3:
        case Constants.ALOAD_0:
        case Constants.ALOAD_1:
        case Constants.ALOAD_2:
        case Constants.ALOAD_3:
        case Constants.ISTORE_0:
        case Constants.ISTORE_1:
        case Constants.ISTORE_2:
        case Constants.ISTORE_3:
        case Constants.LSTORE_0:
        case Constants.LSTORE_1:
        case Constants.LSTORE_2:
        case Constants.LSTORE_3:
        case Constants.FSTORE_0:
        case Constants.FSTORE_1:
        case Constants.FSTORE_2:
        case Constants.FSTORE_3:
        case Constants.DSTORE_0:
        case Constants.DSTORE_1:
        case Constants.DSTORE_2:
        case Constants.DSTORE_3:
        case Constants.ASTORE_0:
        case Constants.ASTORE_1:
        case Constants.ASTORE_2:
        case Constants.ASTORE_3:
          currentOffset += 1;
          break;
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
        case Opcodes.GOTO:
        case Opcodes.JSR:
        case Opcodes.IFNULL:
        case Opcodes.IFNONNULL:
          createLabel(bytecodeOffset + readShort(currentOffset + 1), labels);
          currentOffset += 3;
          break;
        case Constants.ASM_IFEQ:
        case Constants.ASM_IFNE:
        case Constants.ASM_IFLT:
        case Constants.ASM_IFGE:
        case Constants.ASM_IFGT:
        case Constants.ASM_IFLE:
        case Constants.ASM_IF_ICMPEQ:
        case Constants.ASM_IF_ICMPNE:
        case Constants.ASM_IF_ICMPLT:
        case Constants.ASM_IF_ICMPGE:
        case Constants.ASM_IF_ICMPGT:
        case Constants.ASM_IF_ICMPLE:
        case Constants.ASM_IF_ACMPEQ:
        case Constants.ASM_IF_ACMPNE:
        case Constants.ASM_GOTO:
        case Constants.ASM_JSR:
        case Constants.ASM_IFNULL:
        case Constants.ASM_IFNONNULL:
          createLabel(bytecodeOffset + readUnsignedShort(currentOffset + 1), labels);
          currentOffset += 3;
          break;
        case Constants.GOTO_W:
        case Constants.JSR_W:
        case Constants.ASM_GOTO_W:
          createLabel(bytecodeOffset + readInt(currentOffset + 1), labels);
          currentOffset += 5;
          break;
        case Constants.WIDE:
          switch (classBuffer[currentOffset + 1] & 0xFF) {
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
            case Opcodes.ALOAD:
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ASTORE:
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
            case Opcodes.RET:
              currentOffset += 4;
              break;
            case Opcodes.IINC:
              currentOffset += 6;
              break;
            default:
              throw new IllegalArgumentException();
          }
          break;
        case Opcodes.TABLESWITCH:
          // 跳过 0 到 3 个填充字节。
          currentOffset += 4 - (bytecodeOffset & 3);
          // 读取默认标签和表项数量。
          createLabel(bytecodeOffset + readInt(currentOffset), labels);
          int numTableEntries = readInt(currentOffset + 8) - readInt(currentOffset + 4) + 1;
          currentOffset += 12;
          // 读取表的标签。
          while (numTableEntries-- > 0) {
            createLabel(bytecodeOffset + readInt(currentOffset), labels);
            currentOffset += 4;
          }
          break;
        case Opcodes.LOOKUPSWITCH:
          // 跳过 0 到 3 个填充字节。
          currentOffset += 4 - (bytecodeOffset & 3);
          // 读取默认标签和 switch 分支数量。
          createLabel(bytecodeOffset + readInt(currentOffset), labels);
          int numSwitchCases = readInt(currentOffset + 4);
          currentOffset += 8;
          // 读取 switch 的标签。
          while (numSwitchCases-- > 0) {
            createLabel(bytecodeOffset + readInt(currentOffset + 4), labels);
            currentOffset += 8;
          }
          break;
        case Opcodes.ILOAD:
        case Opcodes.LLOAD:
        case Opcodes.FLOAD:
        case Opcodes.DLOAD:
        case Opcodes.ALOAD:
        case Opcodes.ISTORE:
        case Opcodes.LSTORE:
        case Opcodes.FSTORE:
        case Opcodes.DSTORE:
        case Opcodes.ASTORE:
        case Opcodes.RET:
        case Opcodes.BIPUSH:
        case Opcodes.NEWARRAY:
        case Opcodes.LDC:
          currentOffset += 2;
          break;
        case Opcodes.SIPUSH:
        case Constants.LDC_W:
        case Constants.LDC2_W:
        case Opcodes.GETSTATIC:
        case Opcodes.PUTSTATIC:
        case Opcodes.GETFIELD:
        case Opcodes.PUTFIELD:
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKESTATIC:
        case Opcodes.NEW:
        case Opcodes.ANEWARRAY:
        case Opcodes.CHECKCAST:
        case Opcodes.INSTANCEOF:
        case Opcodes.IINC:
          currentOffset += 3;
          break;
        case Opcodes.INVOKEINTERFACE:
        case Opcodes.INVOKEDYNAMIC:
          currentOffset += 5;
          break;
        case Opcodes.MULTIANEWARRAY:
          currentOffset += 4;
          break;
        default:
          throw new IllegalArgumentException();
      }
    }

// 读取 'exception_table_length' 和 'exception_table' 字段，为每个被引用的指令创建一个 Label，
// 并让 methodVisitor 访问相应的 try-catch 块。
    int exceptionTableLength = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (exceptionTableLength-- > 0) {
      Label start = createLabel(readUnsignedShort(currentOffset), labels);
      Label end = createLabel(readUnsignedShort(currentOffset + 2), labels);
      Label handler = createLabel(readUnsignedShort(currentOffset + 4), labels);
      String catchType = readUTF8(cpInfoOffsets[readUnsignedShort(currentOffset + 6)], charBuffer);
      currentOffset += 8;
      methodVisitor.visitTryCatchBlock(start, end, handler, catchType);
    }

    // 读取 Code 属性，为每个被引用的指令创建一个 Label
    // 变量顺序参考 JVMS 第 4.7 节。属性偏移量不包括 attribute_name_index 和 attribute_length 字段。
    // - 当前 StackMap[Table] 属性中 'stack_map_frame' 的偏移量，初始为第一个 'stack_map_frame' 条目的偏移量。
    //   每次读取完一个 stack_map_frame 后会更新此偏移量。
    int stackMapFrameOffset = 0;
    // - StackMap[Table] 属性的结束偏移量，或为 0。
    int stackMapTableEndOffset = 0;
    // - 标记 stack map frame 是否被压缩（即在 StackMapTable 中）。
    boolean compressedFrames = true;
    // - LocalVariableTable 属性的偏移量，或为 0。
    int localVariableTableOffset = 0;
    // - LocalVariableTypeTable 属性的偏移量，或为 0。
    int localVariableTypeTableOffset = 0;
    // - RuntimeVisibleTypeAnnotations 属性中每个 'type_annotation' 条目的偏移量，或为 null。
    int[] visibleTypeAnnotationOffsets = null;
    // - RuntimeInvisibleTypeAnnotations 属性中每个 'type_annotation' 条目的偏移量，或为 null。
    int[] invisibleTypeAnnotationOffsets = null;
    // - 非标准属性（通过 {@link Attribute#nextAttribute} 链接）。
    //   列表按其在 ClassFile 结构中的顺序的 <i>反向</i> 链接。
    Attribute attributes = null;

    int attributesCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (attributesCount-- > 0) {
      // 读取 attribute_info 的 attribute_name 和 attribute_length 字段
      String attributeName = readUTF8(currentOffset, charBuffer);
      int attributeLength = readInt(currentOffset + 2);
      currentOffset += 6;
      if (Constants.LOCAL_VARIABLE_TABLE.equals(attributeName)) {
        if ((context.parsingOptions & SKIP_DEBUG) == 0) {
          localVariableTableOffset = currentOffset;
          // 解析属性以找到对应的（仅调试）标签
          int currentLocalVariableTableOffset = currentOffset;
          int localVariableTableLength = readUnsignedShort(currentLocalVariableTableOffset);
          currentLocalVariableTableOffset += 2;
          while (localVariableTableLength-- > 0) {
            int startPc = readUnsignedShort(currentLocalVariableTableOffset);
            createDebugLabel(startPc, labels);
            int length = readUnsignedShort(currentLocalVariableTableOffset + 2);
            createDebugLabel(startPc + length, labels);
            // 跳过 name_index、descriptor_index 和 index 字段（每个 2 字节）
            currentLocalVariableTableOffset += 10;
          }
        }
      } else if (Constants.LOCAL_VARIABLE_TYPE_TABLE.equals(attributeName)) {
        localVariableTypeTableOffset = currentOffset;
        // 这里不提取与属性内容对应的标签，假设它们与 LocalVariableTable 的标签相同或为其子集
      } else if (Constants.LINE_NUMBER_TABLE.equals(attributeName)) {
        if ((context.parsingOptions & SKIP_DEBUG) == 0) {
          // 解析属性以找到对应的（仅调试）标签
          int currentLineNumberTableOffset = currentOffset;
          int lineNumberTableLength = readUnsignedShort(currentLineNumberTableOffset);
          currentLineNumberTableOffset += 2;
          while (lineNumberTableLength-- > 0) {
            int startPc = readUnsignedShort(currentLineNumberTableOffset);
            int lineNumber = readUnsignedShort(currentLineNumberTableOffset + 2);
            currentLineNumberTableOffset += 4;
            createDebugLabel(startPc, labels);
            labels[startPc].addLineNumber(lineNumber);
          }
        }
      } else if (Constants.RUNTIME_VISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        visibleTypeAnnotationOffsets =
            readTypeAnnotations(methodVisitor, context, currentOffset, /* visible = */ true);
        // 这里不提取与属性内容对应的标签。每次读取一个类型注解后，再提取它包含的标签。
        // 假设类型注解按字节码偏移量递增排序。
      } else if (Constants.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS.equals(attributeName)) {
        invisibleTypeAnnotationOffsets =
            readTypeAnnotations(methodVisitor, context, currentOffset, /* visible = */ false);
        // 与 RuntimeVisibleTypeAnnotations 属性相同的处理方式
      } else if (Constants.STACK_MAP_TABLE.equals(attributeName)) {
        if ((context.parsingOptions & SKIP_FRAMES) == 0) {
          stackMapFrameOffset = currentOffset + 2;
          stackMapTableEndOffset = currentOffset + attributeLength;
        }
        // 这里不提取与属性内容对应的标签。每次读取一个 frame 后再提取其标签。
        // 由于 frame 按顺序排列，只需“前瞻一个 frame”，即可保证正确。
        // UNINITIALIZED 类型偏移量除外，通过解析 stack map table 时处理。
      } else if ("StackMap".equals(attributeName)) {
        if ((context.parsingOptions & SKIP_FRAMES) == 0) {
          stackMapFrameOffset = currentOffset + 2;
          stackMapTableEndOffset = currentOffset + attributeLength;
          compressedFrames = false;
        }
        // 注意！这里假设 frame 按 StackMapTable 属性顺序排列，
        // 以便增量提取对应的标签
      } else {
        Attribute attribute =
            readAttribute(
                context.attributePrototypes,
                attributeName,
                currentOffset,
                attributeLength,
                charBuffer,
                codeOffset,
                labels);
        attribute.nextAttribute = attributes;
        attributes = attribute;
      }
      currentOffset += attributeLength;
    }

    // 初始化与栈映射帧（stack map frames）相关的上下文字段，并在需要时生成第一个（隐式）栈映射帧。
    final boolean expandFrames = (context.parsingOptions & EXPAND_FRAMES) != 0;
    if (stackMapFrameOffset != 0) {
      // 第一个显式 frame 的字节码偏移量不是 offset_delta + 1，而只是 offset_delta。
      // 将隐式 frame 的偏移量设置为 -1，使得在所有情况下都可以使用 "offset_delta + 1" 规则。
      context.currentFrameOffset = -1;
      context.currentFrameType = 0;
      context.currentFrameLocalCount = 0;
      context.currentFrameLocalCountDelta = 0;
      context.currentFrameLocalTypes = new Object[maxLocals];
      context.currentFrameStackCount = 0;
      context.currentFrameStackTypes = new Object[maxStack];
      if (expandFrames) {
        computeImplicitFrame(context);
      }
      // 查找 UNINITIALIZED frame 类型对应的标签。
      // 与其完全解码整个 stack map table，不如查找 3 个连续字节“看起来像” UNINITIALIZED 类型
      // （标记 ITEM_Uninitialized、偏移在字节码范围内、该偏移处是 NEW 指令）。
      // 可能会有误报（即不是实际的 UNINITIALIZED 类型），但这种情况很少见，
      // 唯一的后果是创建一个多余的标签。这比为每个 NEW 指令创建标签更高效，也比完全解码整个 stack map table 更快。
      for (int offset = stackMapFrameOffset; offset < stackMapTableEndOffset - 2; ++offset) {
        if (classBuffer[offset] == Frame.ITEM_UNINITIALIZED) {
          int potentialBytecodeOffset = readUnsignedShort(offset + 1);
          if (potentialBytecodeOffset >= 0
              && potentialBytecodeOffset < codeLength
              && (classBuffer[bytecodeStartOffset + potentialBytecodeOffset] & 0xFF)
                  == Opcodes.NEW) {
            createLabel(potentialBytecodeOffset, labels);
          }
        }
      }
    }
    if (expandFrames && (context.parsingOptions & EXPAND_ASM_INSNS) != 0) {
      // 展开 ASM 特定指令可能会引入 F_INSERT frame，即使方法当前没有任何 frame。
      // 这些插入的 frame 必须通过模拟字节码指令的效果逐条计算，从隐式的第一个 frame 开始。
      // 为此，MethodWriter 需要在访问第一条指令之前知道 maxLocals。
      // 为保证这一点，这里访问隐式的第一个 frame（只传递 maxLocals，其他由 MethodWriter 计算）。
      methodVisitor.visitFrame(Opcodes.F_NEW, maxLocals, null, 0, null);
    }

    // 访问字节码指令。首先，初始化用于类型注解增量解析的状态变量。

    // 下一个要读取的 runtime visible 类型注解在 visibleTypeAnnotationOffsets 数组中的索引
    int currentVisibleTypeAnnotationIndex = 0;
    // 下一个要读取的 runtime visible 类型注解的字节码偏移量，或者为 -1
    int currentVisibleTypeAnnotationBytecodeOffset =
        getTypeAnnotationBytecodeOffset(visibleTypeAnnotationOffsets, 0);

    // 下一个要读取的 runtime invisible 类型注解在 invisibleTypeAnnotationOffsets 数组中的索引
    int currentInvisibleTypeAnnotationIndex = 0;
    // 下一个要读取的 runtime invisible 类型注解的字节码偏移量，或者为 -1
    int currentInvisibleTypeAnnotationBytecodeOffset =
        getTypeAnnotationBytecodeOffset(invisibleTypeAnnotationOffsets, 0);

    // 当前指令之前是否需要插入 F_INSERT 栈映射帧
    boolean insertFrame = false;

    // 用于从 goto_w 或 jsr_w 指令中减去得到对应的 goto 或 jsr 指令的偏移量
    // 如果为 0，则 goto_w 和 jsr_w 保持不变（即展开 ASM 特定指令时）。
    final int wideJumpOpcodeDelta =
        (context.parsingOptions & EXPAND_ASM_INSNS) == 0 ? Constants.WIDE_JUMP_OPCODE_DELTA : 0;

    currentOffset = bytecodeStartOffset;
    while (currentOffset < bytecodeEndOffset) {
      final int currentBytecodeOffset = currentOffset - bytecodeStartOffset;

// 访问此字节码偏移量对应的标签和行号（如果有的话）。
      Label currentLabel = labels[currentBytecodeOffset];
      if (currentLabel != null) {
        currentLabel.accept(methodVisitor, (context.parsingOptions & SKIP_DEBUG) == 0);
      }

      // 访问此字节码偏移量对应的栈映射帧（如果有的话）。
      while (stackMapFrameOffset != 0
          && (context.currentFrameOffset == currentBytecodeOffset
              || context.currentFrameOffset == -1)) {
        // 如果该偏移量有栈映射帧，则让 methodVisitor 访问它，并读取下一个栈映射帧（如果存在）。
        if (context.currentFrameOffset != -1) {
          if (!compressedFrames || expandFrames) {
            methodVisitor.visitFrame(
                Opcodes.F_NEW,
                context.currentFrameLocalCount,
                context.currentFrameLocalTypes,
                context.currentFrameStackCount,
                context.currentFrameStackTypes);
          } else {
            methodVisitor.visitFrame(
                context.currentFrameType,
                context.currentFrameLocalCountDelta,
                context.currentFrameLocalTypes,
                context.currentFrameStackCount,
                context.currentFrameStackTypes);
          }
          // 由于此字节码偏移量已有栈映射帧，因此无需插入新的帧。
          insertFrame = false;
        }
        if (stackMapFrameOffset < stackMapTableEndOffset) {
          stackMapFrameOffset =
              readStackMapFrame(stackMapFrameOffset, compressedFrames, expandFrames, context);
        } else {
          stackMapFrameOffset = 0;
        }
      }

      // 如果在前一次迭代中通过将 insertFrame 设置为 true 请求，则为此字节码偏移量插入栈映射帧。
      // 实际的帧内容由 MethodWriter 计算。
      if (insertFrame) {
        if ((context.parsingOptions & EXPAND_FRAMES) != 0) {
          methodVisitor.visitFrame(Constants.F_INSERT, 0, null, 0, null);
        }
        insertFrame = false;
      }

      // 访问此字节码偏移量处的指令。
      int opcode = classBuffer[currentOffset] & 0xFF;
      switch (opcode) {
        case Opcodes.NOP:
        case Opcodes.ACONST_NULL:
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
        case Opcodes.IALOAD:
        case Opcodes.LALOAD:
        case Opcodes.FALOAD:
        case Opcodes.DALOAD:
        case Opcodes.AALOAD:
        case Opcodes.BALOAD:
        case Opcodes.CALOAD:
        case Opcodes.SALOAD:
        case Opcodes.IASTORE:
        case Opcodes.LASTORE:
        case Opcodes.FASTORE:
        case Opcodes.DASTORE:
        case Opcodes.AASTORE:
        case Opcodes.BASTORE:
        case Opcodes.CASTORE:
        case Opcodes.SASTORE:
        case Opcodes.POP:
        case Opcodes.POP2:
        case Opcodes.DUP:
        case Opcodes.DUP_X1:
        case Opcodes.DUP_X2:
        case Opcodes.DUP2:
        case Opcodes.DUP2_X1:
        case Opcodes.DUP2_X2:
        case Opcodes.SWAP:
        case Opcodes.IADD:
        case Opcodes.LADD:
        case Opcodes.FADD:
        case Opcodes.DADD:
        case Opcodes.ISUB:
        case Opcodes.LSUB:
        case Opcodes.FSUB:
        case Opcodes.DSUB:
        case Opcodes.IMUL:
        case Opcodes.LMUL:
        case Opcodes.FMUL:
        case Opcodes.DMUL:
        case Opcodes.IDIV:
        case Opcodes.LDIV:
        case Opcodes.FDIV:
        case Opcodes.DDIV:
        case Opcodes.IREM:
        case Opcodes.LREM:
        case Opcodes.FREM:
        case Opcodes.DREM:
        case Opcodes.INEG:
        case Opcodes.LNEG:
        case Opcodes.FNEG:
        case Opcodes.DNEG:
        case Opcodes.ISHL:
        case Opcodes.LSHL:
        case Opcodes.ISHR:
        case Opcodes.LSHR:
        case Opcodes.IUSHR:
        case Opcodes.LUSHR:
        case Opcodes.IAND:
        case Opcodes.LAND:
        case Opcodes.IOR:
        case Opcodes.LOR:
        case Opcodes.IXOR:
        case Opcodes.LXOR:
        case Opcodes.I2L:
        case Opcodes.I2F:
        case Opcodes.I2D:
        case Opcodes.L2I:
        case Opcodes.L2F:
        case Opcodes.L2D:
        case Opcodes.F2I:
        case Opcodes.F2L:
        case Opcodes.F2D:
        case Opcodes.D2I:
        case Opcodes.D2L:
        case Opcodes.D2F:
        case Opcodes.I2B:
        case Opcodes.I2C:
        case Opcodes.I2S:
        case Opcodes.LCMP:
        case Opcodes.FCMPL:
        case Opcodes.FCMPG:
        case Opcodes.DCMPL:
        case Opcodes.DCMPG:
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.DRETURN:
        case Opcodes.ARETURN:
        case Opcodes.RETURN:
        case Opcodes.ARRAYLENGTH:
        case Opcodes.ATHROW:
        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
          methodVisitor.visitInsn(opcode);
          currentOffset += 1;
          break;
        case Constants.ILOAD_0:
        case Constants.ILOAD_1:
        case Constants.ILOAD_2:
        case Constants.ILOAD_3:
        case Constants.LLOAD_0:
        case Constants.LLOAD_1:
        case Constants.LLOAD_2:
        case Constants.LLOAD_3:
        case Constants.FLOAD_0:
        case Constants.FLOAD_1:
        case Constants.FLOAD_2:
        case Constants.FLOAD_3:
        case Constants.DLOAD_0:
        case Constants.DLOAD_1:
        case Constants.DLOAD_2:
        case Constants.DLOAD_3:
        case Constants.ALOAD_0:
        case Constants.ALOAD_1:
        case Constants.ALOAD_2:
        case Constants.ALOAD_3:
          opcode -= Constants.ILOAD_0;
          methodVisitor.visitVarInsn(Opcodes.ILOAD + (opcode >> 2), opcode & 0x3);
          currentOffset += 1;
          break;
        case Constants.ISTORE_0:
        case Constants.ISTORE_1:
        case Constants.ISTORE_2:
        case Constants.ISTORE_3:
        case Constants.LSTORE_0:
        case Constants.LSTORE_1:
        case Constants.LSTORE_2:
        case Constants.LSTORE_3:
        case Constants.FSTORE_0:
        case Constants.FSTORE_1:
        case Constants.FSTORE_2:
        case Constants.FSTORE_3:
        case Constants.DSTORE_0:
        case Constants.DSTORE_1:
        case Constants.DSTORE_2:
        case Constants.DSTORE_3:
        case Constants.ASTORE_0:
        case Constants.ASTORE_1:
        case Constants.ASTORE_2:
        case Constants.ASTORE_3:
          opcode -= Constants.ISTORE_0;
          methodVisitor.visitVarInsn(Opcodes.ISTORE + (opcode >> 2), opcode & 0x3);
          currentOffset += 1;
          break;
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
        case Opcodes.GOTO:
        case Opcodes.JSR:
        case Opcodes.IFNULL:
        case Opcodes.IFNONNULL:
          methodVisitor.visitJumpInsn(
              opcode, labels[currentBytecodeOffset + readShort(currentOffset + 1)]);
          currentOffset += 3;
          break;
        case Constants.GOTO_W:
        case Constants.JSR_W:
          methodVisitor.visitJumpInsn(
              opcode - wideJumpOpcodeDelta,
              labels[currentBytecodeOffset + readInt(currentOffset + 1)]);
          currentOffset += 5;
          break;
        case Constants.ASM_IFEQ:
        case Constants.ASM_IFNE:
        case Constants.ASM_IFLT:
        case Constants.ASM_IFGE:
        case Constants.ASM_IFGT:
        case Constants.ASM_IFLE:
        case Constants.ASM_IF_ICMPEQ:
        case Constants.ASM_IF_ICMPNE:
        case Constants.ASM_IF_ICMPLT:
        case Constants.ASM_IF_ICMPGE:
        case Constants.ASM_IF_ICMPGT:
        case Constants.ASM_IF_ICMPLE:
        case Constants.ASM_IF_ACMPEQ:
        case Constants.ASM_IF_ACMPNE:
        case Constants.ASM_GOTO:
        case Constants.ASM_JSR:
        case Constants.ASM_IFNULL:
        case Constants.ASM_IFNONNULL:
          {
            // 前向跳转的偏移量大于 32767。在这种情况下，我们会自动将 ASM_GOTO 替换为 GOTO_W，
            // 将 ASM_JSR 替换为 JSR_W，并将 ASM_IFxxx <l> 替换为 IFNOTxxx <L> GOTO_W <l> L:...
            // 其中 IFNOTxxx 是 ASM_IFxxx 的“相反”操作码（例如 ASM_IFEQ 对应 IFNE），
            // <L> 表示紧跟 GOTO_W 后的指令位置。
            // 首先，将 ASM 特定操作码 ASM_IFEQ ... ASM_JSR、ASM_IFNULL 和 ASM_IFNONNULL
            // 转换为标准操作码 IFEQ ... JSR、IFNULL 和 IFNONNULL。
            opcode =
                opcode < Constants.ASM_IFNULL
                    ? opcode - Constants.ASM_OPCODE_DELTA
                    : opcode - Constants.ASM_IFNULL_OPCODE_DELTA;
            Label target = labels[currentBytecodeOffset + readUnsignedShort(currentOffset + 1)];
            if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR) {
              // 将 GOTO 替换为 GOTO_W，将 JSR 替换为 JSR_W。
              methodVisitor.visitJumpInsn(opcode + Constants.WIDE_JUMP_OPCODE_DELTA, target);
            } else {
              // 计算 opcode 的“相反操作码”。对于 IFNULL 和 IFNONNULL，可以通过翻转最低有效位实现，
              // 对于 IFEQ ... IF_ACMPEQ 也类似（加减 1 进行偏移）。
              opcode = opcode < Opcodes.GOTO ? ((opcode + 1) ^ 1) - 1 : opcode ^ 1;
              Label endif = createLabel(currentBytecodeOffset + 3, labels);
              methodVisitor.visitJumpInsn(opcode, endif);
              methodVisitor.visitJumpInsn(Constants.GOTO_W, target);
              // endif 表示紧跟 GOTO_W 之后的指令，并将在下一条指令中访问。
              // 由于它是跳转目标，因此需要在此插入栈映射帧。
              insertFrame = true;
            }
            currentOffset += 3;
            break;
          }
        case Constants.ASM_GOTO_W:
          // 将 ASM_GOTO_W 替换为 GOTO_W。
          methodVisitor.visitJumpInsn(
              Constants.GOTO_W, labels[currentBytecodeOffset + readInt(currentOffset + 1)]);
          // 紧跟其后的指令是跳转目标（因为 ASM_GOTO_W 用于 IFNOTxxx <L> ASM_GOTO_W <l> L:... 模式，
          // 参见 MethodWriter），因此需要在此插入栈映射帧。
          insertFrame = true;
          currentOffset += 5;
          break;
        case Constants.WIDE:
          opcode = classBuffer[currentOffset + 1] & 0xFF;
          if (opcode == Opcodes.IINC) {
            methodVisitor.visitIincInsn(
                readUnsignedShort(currentOffset + 2), readShort(currentOffset + 4));
            currentOffset += 6;
          } else {
            methodVisitor.visitVarInsn(opcode, readUnsignedShort(currentOffset + 2));
            currentOffset += 4;
          }
          break;
        case Opcodes.TABLESWITCH:
          {
            // 跳过 0 到 3 个填充字节。
            currentOffset += 4 - (currentBytecodeOffset & 3);
            // 读取指令。
            Label defaultLabel = labels[currentBytecodeOffset + readInt(currentOffset)];
            int low = readInt(currentOffset + 4);
            int high = readInt(currentOffset + 8);
            currentOffset += 12;
            Label[] table = new Label[high - low + 1];
            for (int i = 0; i < table.length; ++i) {
              table[i] = labels[currentBytecodeOffset + readInt(currentOffset)];
              currentOffset += 4;
            }
            methodVisitor.visitTableSwitchInsn(low, high, defaultLabel, table);
            break;
          }
        case Opcodes.LOOKUPSWITCH:
          {
            // 跳过 0 到 3 个填充字节。
            currentOffset += 4 - (currentBytecodeOffset & 3);
            // 读取指令。
            Label defaultLabel = labels[currentBytecodeOffset + readInt(currentOffset)];
            int numPairs = readInt(currentOffset + 4);
            currentOffset += 8;
            int[] keys = new int[numPairs];
            Label[] values = new Label[numPairs];
            for (int i = 0; i < numPairs; ++i) {
              keys[i] = readInt(currentOffset);
              values[i] = labels[currentBytecodeOffset + readInt(currentOffset + 4)];
              currentOffset += 8;
            }
            methodVisitor.visitLookupSwitchInsn(defaultLabel, keys, values);
            break;
          }
        case Opcodes.ILOAD:
        case Opcodes.LLOAD:
        case Opcodes.FLOAD:
        case Opcodes.DLOAD:
        case Opcodes.ALOAD:
        case Opcodes.ISTORE:
        case Opcodes.LSTORE:
        case Opcodes.FSTORE:
        case Opcodes.DSTORE:
        case Opcodes.ASTORE:
        case Opcodes.RET:
          methodVisitor.visitVarInsn(opcode, classBuffer[currentOffset + 1] & 0xFF);
          currentOffset += 2;
          break;
        case Opcodes.BIPUSH:
        case Opcodes.NEWARRAY:
          methodVisitor.visitIntInsn(opcode, classBuffer[currentOffset + 1]);
          currentOffset += 2;
          break;
        case Opcodes.SIPUSH:
          methodVisitor.visitIntInsn(opcode, readShort(currentOffset + 1));
          currentOffset += 3;
          break;
        case Opcodes.LDC:
          methodVisitor.visitLdcInsn(readConst(classBuffer[currentOffset + 1] & 0xFF, charBuffer));
          currentOffset += 2;
          break;
        case Constants.LDC_W:
        case Constants.LDC2_W:
          methodVisitor.visitLdcInsn(readConst(readUnsignedShort(currentOffset + 1), charBuffer));
          currentOffset += 3;
          break;
        case Opcodes.GETSTATIC:
        case Opcodes.PUTSTATIC:
        case Opcodes.GETFIELD:
        case Opcodes.PUTFIELD:
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKESTATIC:
        case Opcodes.INVOKEINTERFACE:
          {
            int cpInfoOffset = cpInfoOffsets[readUnsignedShort(currentOffset + 1)];
            int nameAndTypeCpInfoOffset = cpInfoOffsets[readUnsignedShort(cpInfoOffset + 2)];
            String owner = readClass(cpInfoOffset, charBuffer);
            String name = readUTF8(nameAndTypeCpInfoOffset, charBuffer);
            String descriptor = readUTF8(nameAndTypeCpInfoOffset + 2, charBuffer);
            if (opcode < Opcodes.INVOKEVIRTUAL) {
              methodVisitor.visitFieldInsn(opcode, owner, name, descriptor);
            } else {
              boolean isInterface =
                  classBuffer[cpInfoOffset - 1] == Symbol.CONSTANT_INTERFACE_METHODREF_TAG;
              methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
            if (opcode == Opcodes.INVOKEINTERFACE) {
              currentOffset += 5;
            } else {
              currentOffset += 3;
            }
            break;
          }
        case Opcodes.INVOKEDYNAMIC:
          {
            int cpInfoOffset = cpInfoOffsets[readUnsignedShort(currentOffset + 1)];
            int nameAndTypeCpInfoOffset = cpInfoOffsets[readUnsignedShort(cpInfoOffset + 2)];
            String name = readUTF8(nameAndTypeCpInfoOffset, charBuffer);
            String descriptor = readUTF8(nameAndTypeCpInfoOffset + 2, charBuffer);
            int bootstrapMethodOffset = bootstrapMethodOffsets[readUnsignedShort(cpInfoOffset)];
            Handle handle =
                (Handle) readConst(readUnsignedShort(bootstrapMethodOffset), charBuffer);
            Object[] bootstrapMethodArguments =
                new Object[readUnsignedShort(bootstrapMethodOffset + 2)];
            bootstrapMethodOffset += 4;
            for (int i = 0; i < bootstrapMethodArguments.length; i++) {
              bootstrapMethodArguments[i] =
                  readConst(readUnsignedShort(bootstrapMethodOffset), charBuffer);
              bootstrapMethodOffset += 2;
            }
            methodVisitor.visitInvokeDynamicInsn(
                name, descriptor, handle, bootstrapMethodArguments);
            currentOffset += 5;
            break;
          }
        case Opcodes.NEW:
        case Opcodes.ANEWARRAY:
        case Opcodes.CHECKCAST:
        case Opcodes.INSTANCEOF:
          methodVisitor.visitTypeInsn(opcode, readClass(currentOffset + 1, charBuffer));
          currentOffset += 3;
          break;
        case Opcodes.IINC:
          methodVisitor.visitIincInsn(
              classBuffer[currentOffset + 1] & 0xFF, classBuffer[currentOffset + 2]);
          currentOffset += 3;
          break;
        case Opcodes.MULTIANEWARRAY:
          methodVisitor.visitMultiANewArrayInsn(
              readClass(currentOffset + 1, charBuffer), classBuffer[currentOffset + 3] & 0xFF);
          currentOffset += 4;
          break;
        default:
          throw new AssertionError();
      }

        // 访问运行时可见的指令注解（如果有）。
      while (visibleTypeAnnotationOffsets != null
          && currentVisibleTypeAnnotationIndex < visibleTypeAnnotationOffsets.length
          && currentVisibleTypeAnnotationBytecodeOffset <= currentBytecodeOffset) {
        if (currentVisibleTypeAnnotationBytecodeOffset == currentBytecodeOffset) {
          // 解析 target_type、target_info 和 target_path 字段。
          int currentAnnotationOffset =
              readTypeAnnotationTarget(
                  context, visibleTypeAnnotationOffsets[currentVisibleTypeAnnotationIndex]);
          // 解析 type_index 字段。
          String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
          currentAnnotationOffset += 2;
          // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
          readElementValues(
              methodVisitor.visitInsnAnnotation(
                  context.currentTypeAnnotationTarget,
                  context.currentTypeAnnotationTargetPath,
                  annotationDescriptor,
                  /* visible = */ true),
              currentAnnotationOffset,
              /* named = */ true,
              charBuffer);
        }
        currentVisibleTypeAnnotationBytecodeOffset =
            getTypeAnnotationBytecodeOffset(
                visibleTypeAnnotationOffsets, ++currentVisibleTypeAnnotationIndex);
      }

      // 访问运行时不可见的指令注解（如果有）。
      while (invisibleTypeAnnotationOffsets != null
          && currentInvisibleTypeAnnotationIndex < invisibleTypeAnnotationOffsets.length
          && currentInvisibleTypeAnnotationBytecodeOffset <= currentBytecodeOffset) {
        if (currentInvisibleTypeAnnotationBytecodeOffset == currentBytecodeOffset) {
          // 解析 target_type、target_info 和 target_path 字段。
          int currentAnnotationOffset =
              readTypeAnnotationTarget(
                  context, invisibleTypeAnnotationOffsets[currentInvisibleTypeAnnotationIndex]);
          // 解析 type_index 字段。
          String annotationDescriptor = readUTF8(currentAnnotationOffset, charBuffer);
          currentAnnotationOffset += 2;
          // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
          readElementValues(
              methodVisitor.visitInsnAnnotation(
                  context.currentTypeAnnotationTarget,
                  context.currentTypeAnnotationTargetPath,
                  annotationDescriptor,
                  /* visible = */ false),
              currentAnnotationOffset,
              /* named = */ true,
              charBuffer);
        }
        currentInvisibleTypeAnnotationBytecodeOffset =
            getTypeAnnotationBytecodeOffset(
                invisibleTypeAnnotationOffsets, ++currentInvisibleTypeAnnotationIndex);
      }
    }
    if (labels[codeLength] != null) {
      methodVisitor.visitLabel(labels[codeLength]);
    }

    // 访问 LocalVariableTable 和 LocalVariableTypeTable 属性。
    if (localVariableTableOffset != 0 && (context.parsingOptions & SKIP_DEBUG) == 0) {
      // LocalVariableTypeTable 每个条目的 (start_pc, index, signature_index) 字段。
      int[] typeTable = null;
      if (localVariableTypeTableOffset != 0) {
        typeTable = new int[readUnsignedShort(localVariableTypeTableOffset) * 3];
        currentOffset = localVariableTypeTableOffset + 2;
        int typeTableIndex = typeTable.length;
        while (typeTableIndex > 0) {
          // 存储 'signature_index' 的偏移量，以及 'index' 和 'start_pc' 的值。
          typeTable[--typeTableIndex] = currentOffset + 6;
          typeTable[--typeTableIndex] = readUnsignedShort(currentOffset + 8);
          typeTable[--typeTableIndex] = readUnsignedShort(currentOffset);
          currentOffset += 10;
        }
      }
      int localVariableTableLength = readUnsignedShort(localVariableTableOffset);
      currentOffset = localVariableTableOffset + 2;
      while (localVariableTableLength-- > 0) {
        int startPc = readUnsignedShort(currentOffset);
        int length = readUnsignedShort(currentOffset + 2);
        String name = readUTF8(currentOffset + 4, charBuffer);
        String descriptor = readUTF8(currentOffset + 6, charBuffer);
        int index = readUnsignedShort(currentOffset + 8);
        currentOffset += 10;
        String signature = null;
        if (typeTable != null) {
          for (int i = 0; i < typeTable.length; i += 3) {
            if (typeTable[i] == startPc && typeTable[i + 1] == index) {
              signature = readUTF8(typeTable[i + 2], charBuffer);
              break;
            }
          }
        }
        methodVisitor.visitLocalVariable(
            name, descriptor, signature, labels[startPc], labels[startPc + length], index);
      }
    }

    // 访问 RuntimeVisibleTypeAnnotations 属性的局部变量类型注解。
    if (visibleTypeAnnotationOffsets != null) {
      for (int typeAnnotationOffset : visibleTypeAnnotationOffsets) {
        int targetType = readByte(typeAnnotationOffset);
        if (targetType == TypeReference.LOCAL_VARIABLE
            || targetType == TypeReference.RESOURCE_VARIABLE) {
          // 解析 target_type、target_info 和 target_path 字段。
          currentOffset = readTypeAnnotationTarget(context, typeAnnotationOffset);
          // 解析 type_index 字段。
          String annotationDescriptor = readUTF8(currentOffset, charBuffer);
          currentOffset += 2;
          // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
          readElementValues(
              methodVisitor.visitLocalVariableAnnotation(
                  context.currentTypeAnnotationTarget,
                  context.currentTypeAnnotationTargetPath,
                  context.currentLocalVariableAnnotationRangeStarts,
                  context.currentLocalVariableAnnotationRangeEnds,
                  context.currentLocalVariableAnnotationRangeIndices,
                  annotationDescriptor,
                  /* visible = */ true),
              currentOffset,
              /* named = */ true,
              charBuffer);
        }
      }
    }

    // 访问 RuntimeInvisibleTypeAnnotations 属性的局部变量类型注解。
    if (invisibleTypeAnnotationOffsets != null) {
      for (int typeAnnotationOffset : invisibleTypeAnnotationOffsets) {
        int targetType = readByte(typeAnnotationOffset);
        if (targetType == TypeReference.LOCAL_VARIABLE
            || targetType == TypeReference.RESOURCE_VARIABLE) {
          // 解析 target_type、target_info 和 target_path 字段。
          currentOffset = readTypeAnnotationTarget(context, typeAnnotationOffset);
          // 解析 type_index 字段。
          String annotationDescriptor = readUTF8(currentOffset, charBuffer);
          currentOffset += 2;
          // 解析 num_element_value_pairs 和 element_value_pairs 并访问这些值。
          readElementValues(
              methodVisitor.visitLocalVariableAnnotation(
                  context.currentTypeAnnotationTarget,
                  context.currentTypeAnnotationTargetPath,
                  context.currentLocalVariableAnnotationRangeStarts,
                  context.currentLocalVariableAnnotationRangeEnds,
                  context.currentLocalVariableAnnotationRangeIndices,
                  annotationDescriptor,
                  /* visible = */ false),
              currentOffset,
              /* named = */ true,
              charBuffer);
        }
      }
    }

    // 访问非标准属性。
    while (attributes != null) {
      // 复制并重置 nextAttribute 字段，以便在 MethodWriter 中也可以使用。
      Attribute nextAttribute = attributes.nextAttribute;
      attributes.nextAttribute = null;
      methodVisitor.visitAttribute(attributes);
      attributes = nextAttribute;
    }

    // 访问最大栈深度和最大局部变量数。
    methodVisitor.visitMaxs(maxStack, maxLocals);
  }

  /**
   * 返回与给定字节码偏移量对应的标签。此方法的默认实现在给定偏移量尚未创建标签时为其创建一个标签。
   *
   * @param bytecodeOffset 方法中的字节码偏移量。
   * @param labels 已创建的标签，按其偏移量索引。如果bytecodeOffset已存在标签，
   *     此方法不得创建新标签。否则必须将新标签存储在此数组中。
   * @return 非空的Label，必须等于labels[bytecodeOffset]。
   */
  protected Label readLabel(final int bytecodeOffset, final Label[] labels) {
    // SPRING补丁：宽容地处理偏移量不匹配
    if (bytecodeOffset >= labels.length) {
      return new Label();
    }
    // 补丁结束
    if (labels[bytecodeOffset] == null) {
      labels[bytecodeOffset] = new Label();
    }
    return labels[bytecodeOffset];
  }

  /**
   * 为给定的字节码偏移量创建一个未设置{@link Label#FLAG_DEBUG_ONLY}标志的标签。
   * 标签通过调用{@link #readLabel}创建，并清除其{@link Label#FLAG_DEBUG_ONLY}标志。
   *
   * @param bytecodeOffset 方法中的字节码偏移量。
   * @param labels 已创建的标签，按其偏移量索引。
   * @return 未设置{@link Label#FLAG_DEBUG_ONLY}标志的Label。
   */
  private Label createLabel(final int bytecodeOffset, final Label[] labels) {
    Label label = readLabel(bytecodeOffset, labels);
    label.flags &= ~Label.FLAG_DEBUG_ONLY;
    return label;
  }

  /**
   * 如果给定的字节码偏移量尚不存在标签，则创建一个设置了{@link Label#FLAG_DEBUG_ONLY}标志的标签
   * （否则不执行任何操作）。标签通过调用{@link #readLabel}创建。
   *
   * @param bytecodeOffset 方法中的字节码偏移量。
   * @param labels 已创建的标签，按其偏移量索引。
   */
  private void createDebugLabel(final int bytecodeOffset, final Label[] labels) {
    if (labels[bytecodeOffset] == null) {
      readLabel(bytecodeOffset, labels).flags |= Label.FLAG_DEBUG_ONLY;
    }
  }

  // ----------------------------------------------------------------------------------------------
  // 解析注解、类型注解和参数注解的方法
  // ----------------------------------------------------------------------------------------------

  /**
   * 解析Runtime[In]VisibleTypeAnnotations属性以找到其包含的每个type_annotation条目的偏移量，
   * 找到相应的标签，并访问try catch块注解。
   *
   * @param methodVisitor 用于访问try catch块注解的方法访问者。
   * @param context 关于正在解析的类的信息。
   * @param runtimeTypeAnnotationsOffset Runtime[In]VisibleTypeAnnotations属性的起始偏移量，
   *     不包括attribute_info的attribute_name_index和attribute_length字段。
   * @param visible 如果要解析的属性是RuntimeVisibleTypeAnnotations属性则为true，
   *     如果是RuntimeInvisibleTypeAnnotations属性则为false。
   * @return Runtime[In]VisibleTypeAnnotations_attribute的'annotations'数组字段每个条目的起始偏移量。
   */
  private int[] readTypeAnnotations(
      final MethodVisitor methodVisitor,
      final Context context,
      final int runtimeTypeAnnotationsOffset,
      final boolean visible) {
    char[] charBuffer = context.charBuffer;
    int currentOffset = runtimeTypeAnnotationsOffset;
    // 读取num_annotations字段并创建数组来存储type_annotation偏移量。
    int[] typeAnnotationsOffsets = new int[readUnsignedShort(currentOffset)];
    currentOffset += 2;
    // 解析'annotations'数组字段。
    for (int i = 0; i < typeAnnotationsOffsets.length; ++i) {
      typeAnnotationsOffsets[i] = currentOffset;
      // 解析type_annotation的target_type和target_info字段。target_info字段的大小取决于target_type的值。
      int targetType = readInt(currentOffset);
      switch (targetType >>> 24) {
        case TypeReference.LOCAL_VARIABLE:
        case TypeReference.RESOURCE_VARIABLE:
          // localvar_target具有可变大小，取决于其table_length字段的值。它还引用字节码偏移量，我们需要为其创建标签。
          int tableLength = readUnsignedShort(currentOffset + 1);
          currentOffset += 3;
          while (tableLength-- > 0) {
            int startPc = readUnsignedShort(currentOffset);
            int length = readUnsignedShort(currentOffset + 2);
            // 跳过index字段（2字节）。
            currentOffset += 6;
            createLabel(startPc, context.currentMethodLabels);
            createLabel(startPc + length, context.currentMethodLabels);
          }
          break;
        case TypeReference.CAST:
        case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
        case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
        case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
        case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
          currentOffset += 4;
          break;
        case TypeReference.CLASS_EXTENDS:
        case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
        case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
        case TypeReference.THROWS:
        case TypeReference.EXCEPTION_PARAMETER:
        case TypeReference.INSTANCEOF:
        case TypeReference.NEW:
        case TypeReference.CONSTRUCTOR_REFERENCE:
        case TypeReference.METHOD_REFERENCE:
          currentOffset += 3;
          break;
        case TypeReference.CLASS_TYPE_PARAMETER:
        case TypeReference.METHOD_TYPE_PARAMETER:
        case TypeReference.METHOD_FORMAL_PARAMETER:
        case TypeReference.FIELD:
        case TypeReference.METHOD_RETURN:
        case TypeReference.METHOD_RECEIVER:
        default:
          // 不能在Code属性中使用的TypeReference类型，或未知类型。
          throw new IllegalArgumentException();
      }
      // 解析type_annotation结构的其余部分，从target_path结构开始（其大小取决于其path_length字段）。
      int pathLength = readByte(currentOffset);
      if ((targetType >>> 24) == TypeReference.EXCEPTION_PARAMETER) {
        // 解析target_path结构并创建相应的TypePath。
        TypePath path = pathLength == 0 ? null : new TypePath(classFileBuffer, currentOffset);
        currentOffset += 1 + 2 * pathLength;
        // 解析type_index字段。
        String annotationDescriptor = readUTF8(currentOffset, charBuffer);
        currentOffset += 2;
        // 解析num_element_value_pairs和element_value_pairs并访问这些值。
        currentOffset =
            readElementValues(
                methodVisitor.visitTryCatchAnnotation(
                    targetType & 0xFFFFFF00, path, annotationDescriptor, visible),
                currentOffset,
                /* named = */ true,
                charBuffer);
      } else {
        // 我们不想访问其他target_type注解，所以我们跳过它们（这需要一些解析，
        // 因为element_value_pairs数组具有可变大小）。首先，跳过target_path结构：
        currentOffset += 3 + 2 * pathLength;
        // 然后跳过num_element_value_pairs和element_value_pairs字段
        // （通过使用null AnnotationVisitor读取它们）。
        currentOffset =
            readElementValues(
                /* annotationVisitor = */ null, currentOffset, /* named = */ true, charBuffer);
      }
    }
    return typeAnnotationsOffsets;
  }

  /**
   * 返回与指定JVMS 'type_annotation'结构对应的字节码偏移量，
   * 如果没有此类type_annotation或它没有字节码偏移量，则返回-1。
   *
   * @param typeAnnotationOffsets Runtime[In]VisibleTypeAnnotations属性中每个'type_annotation'条目的偏移量，
   *     或{@literal null}。
   * @param typeAnnotationIndex typeAnnotationOffsets中'type_annotation'条目的索引。
   * @return 与指定JVMS 'type_annotation'结构对应的字节码偏移量，
   *     如果没有此类type_annotation或它没有字节码偏移量，则返回-1。
   */
  private int getTypeAnnotationBytecodeOffset(
      final int[] typeAnnotationOffsets, final int typeAnnotationIndex) {
    if (typeAnnotationOffsets == null
        || typeAnnotationIndex >= typeAnnotationOffsets.length
        || readByte(typeAnnotationOffsets[typeAnnotationIndex]) < TypeReference.INSTANCEOF) {
      return -1;
    }
    return readUnsignedShort(typeAnnotationOffsets[typeAnnotationIndex] + 1);
  }

  /**
   * 解析JVMS type_annotation结构的头部以提取其target_type、target_info和target_path
   * （结果存储在给定的上下文中），并返回type_annotation结构其余部分的起始偏移量。
   *
   * @param context 关于正在解析的类的信息。这是提取的target_type和target_path必须存储的地方。
   * @param typeAnnotationOffset type_annotation结构的起始偏移量。
   * @return type_annotation结构其余部分的起始偏移量。
   */
  private int readTypeAnnotationTarget(final Context context, final int typeAnnotationOffset) {
    int currentOffset = typeAnnotationOffset;
    // 解析并存储target_type结构。
    int targetType = readInt(typeAnnotationOffset);
    switch (targetType >>> 24) {
      case TypeReference.CLASS_TYPE_PARAMETER:
      case TypeReference.METHOD_TYPE_PARAMETER:
      case TypeReference.METHOD_FORMAL_PARAMETER:
        targetType &= 0xFFFF0000;
        currentOffset += 2;
        break;
      case TypeReference.FIELD:
      case TypeReference.METHOD_RETURN:
      case TypeReference.METHOD_RECEIVER:
        targetType &= 0xFF000000;
        currentOffset += 1;
        break;
      case TypeReference.LOCAL_VARIABLE:
      case TypeReference.RESOURCE_VARIABLE:
        targetType &= 0xFF000000;
        int tableLength = readUnsignedShort(currentOffset + 1);
        currentOffset += 3;
        context.currentLocalVariableAnnotationRangeStarts = new Label[tableLength];
        context.currentLocalVariableAnnotationRangeEnds = new Label[tableLength];
        context.currentLocalVariableAnnotationRangeIndices = new int[tableLength];
        for (int i = 0; i < tableLength; ++i) {
          int startPc = readUnsignedShort(currentOffset);
          int length = readUnsignedShort(currentOffset + 2);
          int index = readUnsignedShort(currentOffset + 4);
          currentOffset += 6;
          context.currentLocalVariableAnnotationRangeStarts[i] =
              createLabel(startPc, context.currentMethodLabels);
          context.currentLocalVariableAnnotationRangeEnds[i] =
              createLabel(startPc + length, context.currentMethodLabels);
          context.currentLocalVariableAnnotationRangeIndices[i] = index;
        }
        break;
      case TypeReference.CAST:
      case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
      case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
      case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
      case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
        targetType &= 0xFF0000FF;
        currentOffset += 4;
        break;
      case TypeReference.CLASS_EXTENDS:
      case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
      case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
      case TypeReference.THROWS:
      case TypeReference.EXCEPTION_PARAMETER:
        targetType &= 0xFFFFFF00;
        currentOffset += 3;
        break;
      case TypeReference.INSTANCEOF:
      case TypeReference.NEW:
      case TypeReference.CONSTRUCTOR_REFERENCE:
      case TypeReference.METHOD_REFERENCE:
        targetType &= 0xFF000000;
        currentOffset += 3;
        break;
      default:
        throw new IllegalArgumentException();
    }
    context.currentTypeAnnotationTarget = targetType;
    // 解析并存储target_path结构。
    int pathLength = readByte(currentOffset);
    context.currentTypeAnnotationTargetPath =
        pathLength == 0 ? null : new TypePath(classFileBuffer, currentOffset);
    // 返回type_annotation结构其余部分的起始偏移量。
    return currentOffset + 1 + 2 * pathLength;
  }

  /**
   * 读取Runtime[In]VisibleParameterAnnotations属性并让给定的访问者访问它。
   *
   * @param methodVisitor 必须访问参数注解的访问者。
   * @param context 关于正在解析的类的信息。
   * @param runtimeParameterAnnotationsOffset Runtime[In]VisibleParameterAnnotations属性的起始偏移量，
   *     不包括attribute_info的attribute_name_index和attribute_length字段。
   * @param visible 如果要解析的属性是RuntimeVisibleParameterAnnotations属性则为true，
   *     如果是RuntimeInvisibleParameterAnnotations属性则为false。
   */
  private void readParameterAnnotations(
      final MethodVisitor methodVisitor,
      final Context context,
      final int runtimeParameterAnnotationsOffset,
      final boolean visible) {
    int currentOffset = runtimeParameterAnnotationsOffset;
    int numParameters = classFileBuffer[currentOffset++] & 0xFF;
    methodVisitor.visitAnnotableParameterCount(numParameters, visible);
    char[] charBuffer = context.charBuffer;
    for (int i = 0; i < numParameters; ++i) {
      int numAnnotations = readUnsignedShort(currentOffset);
      currentOffset += 2;
      while (numAnnotations-- > 0) {
        // 解析type_index字段。
        String annotationDescriptor = readUTF8(currentOffset, charBuffer);
        currentOffset += 2;
        // 解析num_element_value_pairs和element_value_pairs并访问这些值。
        currentOffset =
            readElementValues(
                methodVisitor.visitParameterAnnotation(i, annotationDescriptor, visible),
                currentOffset,
                /* named = */ true,
                charBuffer);
      }
    }
  }

  /**
   * 读取JVMS 'annotation'结构的元素值并让给定的访问者访问它们。
   * 此方法也可用于读取注解的'element_value'的JVMS 'array_value'字段的值。
   *
   * @param annotationVisitor 必须访问这些值的访问者。
   * @param annotationOffset 'annotation'结构（不包括其type_index字段）或'array_value'结构的起始偏移量。
   * @param named 注解值是否有名称。解析JVMS 'annotation'结构的值时应为true，
   *     解析注解element_value的JVMS 'array_value'时应为false。
   * @param charBuffer 用于读取常量池中字符串的缓冲区。
   * @return JVMS 'annotation'或'array_value'结构的结束偏移量。
   */
  private int readElementValues(
      final AnnotationVisitor annotationVisitor,
      final int annotationOffset,
      final boolean named,
      final char[] charBuffer) {
    int currentOffset = annotationOffset;
    // 读取num_element_value_pairs字段（或array_value的num_values字段）。
    int numElementValuePairs = readUnsignedShort(currentOffset);
    currentOffset += 2;
    if (named) {
      // 解析element_value_pairs数组。
      while (numElementValuePairs-- > 0) {
        String elementName = readUTF8(currentOffset, charBuffer);
        currentOffset =
            readElementValue(annotationVisitor, currentOffset + 2, elementName, charBuffer);
      }
    } else {
      // 解析array_value数组。
      while (numElementValuePairs-- > 0) {
        currentOffset =
            readElementValue(annotationVisitor, currentOffset, /* elementName= */ null, charBuffer);
      }
    }
    if (annotationVisitor != null) {
      annotationVisitor.visitEnd();
    }
    return currentOffset;
  }

  /**
   * 读取JVMS 'element_value'结构并让给定的访问者访问它。
   *
   * @param annotationVisitor 必须访问element_value结构的访问者。
   * @param elementValueOffset {@link #classFileBuffer}中要读取的element_value结构的起始偏移量。
   * @param elementName 要读取的element_value结构的名称，或{@literal null}。
   * @param charBuffer 用于读取常量池中字符串的缓冲区。
   * @return JVMS 'element_value'结构的结束偏移量。
   */
  private int readElementValue(
      final AnnotationVisitor annotationVisitor,
      final int elementValueOffset,
      final String elementName,
      final char[] charBuffer) {
    int currentOffset = elementValueOffset;
    if (annotationVisitor == null) {
      switch (classFileBuffer[currentOffset] & 0xFF) {
        case 'e': // enum_const_value
          return currentOffset + 5;
        case '@': // annotation_value
          return readElementValues(null, currentOffset + 3, /* named = */ true, charBuffer);
        case '[': // array_value
          return readElementValues(null, currentOffset + 1, /* named = */ false, charBuffer);
        default:
          return currentOffset + 3;
      }
    }
    switch (classFileBuffer[currentOffset++] & 0xFF) {
      case 'B': // const_value_index, CONSTANT_Integer
        annotationVisitor.visit(
            elementName, (byte) readInt(cpInfoOffsets[readUnsignedShort(currentOffset)]));
        currentOffset += 2;
        break;
      case 'C': // const_value_index, CONSTANT_Integer
        annotationVisitor.visit(
            elementName, (char) readInt(cpInfoOffsets[readUnsignedShort(currentOffset)]));
        currentOffset += 2;
        break;
      case 'D': // const_value_index, CONSTANT_Double
      case 'F': // const_value_index, CONSTANT_Float
      case 'I': // const_value_index, CONSTANT_Integer
      case 'J': // const_value_index, CONSTANT_Long
        annotationVisitor.visit(
            elementName, readConst(readUnsignedShort(currentOffset), charBuffer));
        currentOffset += 2;
        break;
      case 'S': // const_value_index, CONSTANT_Integer
        annotationVisitor.visit(
            elementName, (short) readInt(cpInfoOffsets[readUnsignedShort(currentOffset)]));
        currentOffset += 2;
        break;

      case 'Z': // const_value_index, CONSTANT_Integer
        annotationVisitor.visit(
            elementName,
            readInt(cpInfoOffsets[readUnsignedShort(currentOffset)]) == 0
                ? Boolean.FALSE
                : Boolean.TRUE);
        currentOffset += 2;
        break;
      case 's': // const_value_index, CONSTANT_Utf8
        annotationVisitor.visit(elementName, readUTF8(currentOffset, charBuffer));
        currentOffset += 2;
        break;
      case 'e': // enum_const_value
        annotationVisitor.visitEnum(
            elementName,
            readUTF8(currentOffset, charBuffer),
            readUTF8(currentOffset + 2, charBuffer));
        currentOffset += 4;
        break;
      case 'c': // class_info
        annotationVisitor.visit(elementName, Type.getType(readUTF8(currentOffset, charBuffer)));
        currentOffset += 2;
        break;
      case '@': // annotation_value
        currentOffset =
            readElementValues(
                annotationVisitor.visitAnnotation(elementName, readUTF8(currentOffset, charBuffer)),
                currentOffset + 2,
                true,
                charBuffer);
        break;
      case '[': // array_value
        int numValues = readUnsignedShort(currentOffset);
        currentOffset += 2;
        if (numValues == 0) {
          return readElementValues(
              annotationVisitor.visitArray(elementName),
              currentOffset - 2,
              /* named = */ false,
              charBuffer);
        }
        switch (classFileBuffer[currentOffset] & 0xFF) {
          case 'B':
            byte[] byteValues = new byte[numValues];
            for (int i = 0; i < numValues; i++) {
              byteValues[i] = (byte) readInt(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]);
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, byteValues);
            break;
          case 'Z':
            boolean[] booleanValues = new boolean[numValues];
            for (int i = 0; i < numValues; i++) {
              booleanValues[i] = readInt(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]) != 0;
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, booleanValues);
            break;
          case 'S':
            short[] shortValues = new short[numValues];
            for (int i = 0; i < numValues; i++) {
              shortValues[i] = (short) readInt(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]);
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, shortValues);
            break;
          case 'C':
            char[] charValues = new char[numValues];
            for (int i = 0; i < numValues; i++) {
              charValues[i] = (char) readInt(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]);
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, charValues);
            break;
          case 'I':
            int[] intValues = new int[numValues];
            for (int i = 0; i < numValues; i++) {
              intValues[i] = readInt(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]);
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, intValues);
            break;
          case 'J':
            long[] longValues = new long[numValues];
            for (int i = 0; i < numValues; i++) {
              longValues[i] = readLong(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]);
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, longValues);
            break;
          case 'F':
            float[] floatValues = new float[numValues];
            for (int i = 0; i < numValues; i++) {
              floatValues[i] =
                  Float.intBitsToFloat(
                      readInt(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]));
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, floatValues);
            break;
          case 'D':
            double[] doubleValues = new double[numValues];
            for (int i = 0; i < numValues; i++) {
              doubleValues[i] =
                  Double.longBitsToDouble(
                      readLong(cpInfoOffsets[readUnsignedShort(currentOffset + 1)]));
              currentOffset += 3;
            }
            annotationVisitor.visit(elementName, doubleValues);
            break;
          default:
            currentOffset =
                readElementValues(
                    annotationVisitor.visitArray(elementName),
                    currentOffset - 2,
                    /* named = */ false,
                    charBuffer);
            break;
        }
        break;
      default:
        throw new IllegalArgumentException();
    }
    return currentOffset;
  }

  // ----------------------------------------------------------------------------------------------
  // 解析栈映射帧的方法
  // ----------------------------------------------------------------------------------------------

  /**
   * 计算当前正在解析的方法的隐式帧（如给定的{@link Context}中定义的）并将其存储在给定的上下文中。
   *
   * @param context 关于正在解析的类的信息。
   */
  private void computeImplicitFrame(final Context context) {
    String methodDescriptor = context.currentMethodDescriptor;
    Object[] locals = context.currentFrameLocalTypes;
    int numLocal = 0;
    if ((context.currentMethodAccessFlags & Opcodes.ACC_STATIC) == 0) {
      if ("<init>".equals(context.currentMethodName)) {
        locals[numLocal++] = Opcodes.UNINITIALIZED_THIS;
      } else {
        locals[numLocal++] = readClass(header + 2, context.charBuffer);
      }
    }
    // 解析方法描述符，每次迭代处理一个参数类型描述符。首先跳过第一个方法描述符字符，它总是'('。
    int currentMethodDescritorOffset = 1;
    while (true) {
      int currentArgumentDescriptorStartOffset = currentMethodDescritorOffset;
      switch (methodDescriptor.charAt(currentMethodDescritorOffset++)) {
        case 'Z':
        case 'C':
        case 'B':
        case 'S':
        case 'I':
          locals[numLocal++] = Opcodes.INTEGER;
          break;
        case 'F':
          locals[numLocal++] = Opcodes.FLOAT;
          break;
        case 'J':
          locals[numLocal++] = Opcodes.LONG;
          break;
        case 'D':
          locals[numLocal++] = Opcodes.DOUBLE;
          break;
        case '[':
          while (methodDescriptor.charAt(currentMethodDescritorOffset) == '[') {
            ++currentMethodDescritorOffset;
          }
          if (methodDescriptor.charAt(currentMethodDescritorOffset) == 'L') {
            ++currentMethodDescritorOffset;
            while (methodDescriptor.charAt(currentMethodDescritorOffset) != ';') {
              ++currentMethodDescritorOffset;
            }
          }
          locals[numLocal++] =
              methodDescriptor.substring(
                  currentArgumentDescriptorStartOffset, ++currentMethodDescritorOffset);
          break;
        case 'L':
          while (methodDescriptor.charAt(currentMethodDescritorOffset) != ';') {
            ++currentMethodDescritorOffset;
          }
          locals[numLocal++] =
              methodDescriptor.substring(
                  currentArgumentDescriptorStartOffset + 1, currentMethodDescritorOffset++);
          break;
        default:
          context.currentFrameLocalCount = numLocal;
          return;
      }
    }
  }

  /**
   * 读取JVMS 'stack_map_frame'结构并将结果存储在给定的{@link Context}对象中。
   * 此方法也可用于读取full_frame结构，但排除其frame_type字段（用于解析传统的StackMap属性）。
   *
   * @param stackMapFrameOffset {@link #classFileBuffer}中要读取的stack_map_frame_value结构的起始偏移量，
   *     或full_frame结构的起始偏移量（排除其frame_type字段）。
   * @param compressed true表示读取'stack_map_frame'结构，false表示读取不含frame_type字段的'full_frame'结构。
   * @param expand 是否必须扩展栈映射帧。参见{@link #EXPAND_FRAMES}。
   * @param context 解析后的栈映射帧必须存储的位置。
   * @return JVMS 'stack_map_frame'或'full_frame'结构的结束偏移量。
   */
  private int readStackMapFrame(
      final int stackMapFrameOffset,
      final boolean compressed,
      final boolean expand,
      final Context context) {
    int currentOffset = stackMapFrameOffset;
    final char[] charBuffer = context.charBuffer;
    final Label[] labels = context.currentMethodLabels;
    int frameType;
    if (compressed) {
      // 读取frame_type字段。
      frameType = classFileBuffer[currentOffset++] & 0xFF;
    } else {
      frameType = Frame.FULL_FRAME;
      context.currentFrameOffset = -1;
    }
    int offsetDelta;
    context.currentFrameLocalCountDelta = 0;
    if (frameType < Frame.SAME_LOCALS_1_STACK_ITEM_FRAME) {
      offsetDelta = frameType;
      context.currentFrameType = Opcodes.F_SAME;
      context.currentFrameStackCount = 0;
    } else if (frameType < Frame.RESERVED) {
      offsetDelta = frameType - Frame.SAME_LOCALS_1_STACK_ITEM_FRAME;
      currentOffset =
          readVerificationTypeInfo(
              currentOffset, context.currentFrameStackTypes, 0, charBuffer, labels);
      context.currentFrameType = Opcodes.F_SAME1;
      context.currentFrameStackCount = 1;
    } else if (frameType >= Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
      offsetDelta = readUnsignedShort(currentOffset);
      currentOffset += 2;
      if (frameType == Frame.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
        currentOffset =
            readVerificationTypeInfo(
                currentOffset, context.currentFrameStackTypes, 0, charBuffer, labels);
        context.currentFrameType = Opcodes.F_SAME1;
        context.currentFrameStackCount = 1;
      } else if (frameType >= Frame.CHOP_FRAME && frameType < Frame.SAME_FRAME_EXTENDED) {
        context.currentFrameType = Opcodes.F_CHOP;
        context.currentFrameLocalCountDelta = Frame.SAME_FRAME_EXTENDED - frameType;
        context.currentFrameLocalCount -= context.currentFrameLocalCountDelta;
        context.currentFrameStackCount = 0;
      } else if (frameType == Frame.SAME_FRAME_EXTENDED) {
        context.currentFrameType = Opcodes.F_SAME;
        context.currentFrameStackCount = 0;
      } else if (frameType < Frame.FULL_FRAME) {
        int local = expand ? context.currentFrameLocalCount : 0;
        for (int k = frameType - Frame.SAME_FRAME_EXTENDED; k > 0; k--) {
          currentOffset =
              readVerificationTypeInfo(
                  currentOffset, context.currentFrameLocalTypes, local++, charBuffer, labels);
        }
        context.currentFrameType = Opcodes.F_APPEND;
        context.currentFrameLocalCountDelta = frameType - Frame.SAME_FRAME_EXTENDED;
        context.currentFrameLocalCount += context.currentFrameLocalCountDelta;
        context.currentFrameStackCount = 0;
      } else {
        final int numberOfLocals = readUnsignedShort(currentOffset);
        currentOffset += 2;
        context.currentFrameType = Opcodes.F_FULL;
        context.currentFrameLocalCountDelta = numberOfLocals;
        context.currentFrameLocalCount = numberOfLocals;
        for (int local = 0; local < numberOfLocals; ++local) {
          currentOffset =
              readVerificationTypeInfo(
                  currentOffset, context.currentFrameLocalTypes, local, charBuffer, labels);
        }
        final int numberOfStackItems = readUnsignedShort(currentOffset);
        currentOffset += 2;
        context.currentFrameStackCount = numberOfStackItems;
        for (int stack = 0; stack < numberOfStackItems; ++stack) {
          currentOffset =
              readVerificationTypeInfo(
                  currentOffset, context.currentFrameStackTypes, stack, charBuffer, labels);
        }
      }
    } else {
      throw new IllegalArgumentException();
    }
    context.currentFrameOffset += offsetDelta + 1;
    createLabel(context.currentFrameOffset, labels);
    return currentOffset;
  }

  /**
   * 读取JVMS 'verification_type_info'结构并将其存储在给定数组的指定索引处。
   *
   * @param verificationTypeInfoOffset 要读取的'verification_type_info'结构的起始偏移量。
   * @param frame 解析后的类型必须存储的数组。
   * @param index 解析后的类型必须存储在'frame'中的索引。
   * @param charBuffer 用于读取常量池中字符串的缓冲区。
   * @param labels 当前正在解析的方法的标签，按其偏移量索引。如果解析的类型是ITEM_Uninitialized，
   *     则如果对应NEW指令的新标签不存在，将在此数组中存储该标签。
   * @return JVMS 'verification_type_info'结构的结束偏移量。
   */
  private int readVerificationTypeInfo(
      final int verificationTypeInfoOffset,
      final Object[] frame,
      final int index,
      final char[] charBuffer,
      final Label[] labels) {
    int currentOffset = verificationTypeInfoOffset;
    int tag = classFileBuffer[currentOffset++] & 0xFF;
    switch (tag) {
      case Frame.ITEM_TOP:
        frame[index] = Opcodes.TOP;
        break;
      case Frame.ITEM_INTEGER:
        frame[index] = Opcodes.INTEGER;
        break;
      case Frame.ITEM_FLOAT:
        frame[index] = Opcodes.FLOAT;
        break;
      case Frame.ITEM_DOUBLE:
        frame[index] = Opcodes.DOUBLE;
        break;
      case Frame.ITEM_LONG:
        frame[index] = Opcodes.LONG;
        break;
      case Frame.ITEM_NULL:
        frame[index] = Opcodes.NULL;
        break;
      case Frame.ITEM_UNINITIALIZED_THIS:
        frame[index] = Opcodes.UNINITIALIZED_THIS;
        break;
      case Frame.ITEM_OBJECT:
        frame[index] = readClass(currentOffset, charBuffer);
        currentOffset += 2;
        break;
      case Frame.ITEM_UNINITIALIZED:
        frame[index] = createLabel(readUnsignedShort(currentOffset), labels);
        currentOffset += 2;
        break;
      default:
        throw new IllegalArgumentException();
    }
    return currentOffset;
  }

  // ----------------------------------------------------------------------------------------------
  // 解析属性的方法
  // ----------------------------------------------------------------------------------------------

  /**
   * 返回 {@link #classFileBuffer} 中，第一个 ClassFile 的 'attributes' 数组字段条目的偏移量。
   *
   * @return {@link #classFileBuffer} 中，第一个 ClassFile 的 'attributes' 数组字段条目的偏移量。
   */
  final int getFirstAttributeOffset() {
    // 跳过 access_flags、this_class、super_class 和 interfaces_count 字段（各占 2 字节），
    // 以及 interfaces 数组字段（每个接口占 2 字节）。
    int currentOffset = header + 8 + readUnsignedShort(header + 6) * 2;

    // 读取 fields_count 字段。
    int fieldsCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    // 跳过 'fields' 数组字段。
    while (fieldsCount-- > 0) {
      // 不变量：currentOffset 为 field_info 结构的偏移量。
      // 跳过 access_flags、name_index 和 descriptor_index 字段（各占 2 字节），并读取 attributes_count 字段。
      int attributesCount = readUnsignedShort(currentOffset + 6);
      currentOffset += 8;
      // 跳过 'attributes' 数组字段。
      while (attributesCount-- > 0) {
        // 不变量：currentOffset 为 attribute_info 结构的偏移量。
        // 读取 attribute_length 字段（attribute_info 起始位置之后的第 2 个字节），
        // 并跳过该长度的字节，再加上 attribute_name_index 和 attribute_length 字段的 6 个字节
        // （得到整个 attribute_info 结构的总大小）。
        currentOffset += 6 + readInt(currentOffset + 2);
      }
    }

    // 按同样的方式跳过 methods_count 和 'methods' 字段。
    int methodsCount = readUnsignedShort(currentOffset);
    currentOffset += 2;
    while (methodsCount-- > 0) {
      int attributesCount = readUnsignedShort(currentOffset + 6);
      currentOffset += 8;
      while (attributesCount-- > 0) {
        currentOffset += 6 + readInt(currentOffset + 2);
      }
    }

    // 跳过 ClassFile 的 attributes_count 字段。
    return currentOffset + 2;
  }

  /**
   * 读取 BootstrapMethods 属性，用于计算每个引导方法的偏移量。
   *
   * @param maxStringLength 类的常量池中字符串的最大长度的保守估计值。
   * @return 引导方法的偏移量数组。
   */
  private int[] readBootstrapMethodsAttribute(final int maxStringLength) {
    char[] charBuffer = new char[maxStringLength];
    int currentAttributeOffset = getFirstAttributeOffset();
    for (int i = readUnsignedShort(currentAttributeOffset - 2); i > 0; --i) {
      // 读取 attribute_info 的 attribute_name 和 attribute_length 字段。
      String attributeName = readUTF8(currentAttributeOffset, charBuffer);
      int attributeLength = readInt(currentAttributeOffset + 2);
      currentAttributeOffset += 6;
      if (Constants.BOOTSTRAP_METHODS.equals(attributeName)) {
        // 读取 num_bootstrap_methods 字段并创建对应大小的数组。
        int[] result = new int[readUnsignedShort(currentAttributeOffset)];
        // 计算并存储每个 'bootstrap_methods' 数组字段条目的偏移量。
        int currentBootstrapMethodOffset = currentAttributeOffset + 2;
        for (int j = 0; j < result.length; ++j) {
          result[j] = currentBootstrapMethodOffset;
          // 跳过 bootstrap_method_ref 和 num_bootstrap_arguments 字段（各 2 字节），
          // 以及 bootstrap_arguments 数组字段（大小为 num_bootstrap_arguments * 2 字节）。
          currentBootstrapMethodOffset +=
              4 + readUnsignedShort(currentBootstrapMethodOffset + 2) * 2;
        }
        return result;
      }
      currentAttributeOffset += attributeLength;
    }
    throw new IllegalArgumentException();
  }

  /**
   * 读取 {@link #classFileBuffer} 中的非标准 JVMS 'attribute' 结构。
   *
   * @param attributePrototypes 在类访问过程中需要解析的属性原型。
   *     任何类型不等于这些原型类型的属性将不会被解析：其字节数组值会原封不动地传递给 ClassWriter。
   * @param type 属性类型。
   * @param offset {@link #classFileBuffer} 中 JVMS 'attribute' 结构的起始偏移量。
   *     这里不包括 6 字节的属性头（attribute_name_index 和 attribute_length）。
   * @param length 属性内容的长度（不包括 6 字节的属性头）。
   * @param charBuffer 用于读取常量池中字符串的缓冲区。
   * @param codeAttributeOffset {@link #classFileBuffer} 中包含该属性的 Code 属性的起始偏移量，
   *     如果该属性不是 code 属性则为 -1。这里不包括 6 字节的属性头。
   * @param labels 方法代码的标签数组，如果该属性不是 code 属性则为 {@literal null}。
   * @return 读取到的属性。
   */
  private Attribute readAttribute(
      final Attribute[] attributePrototypes,
      final String type,
      final int offset,
      final int length,
      final char[] charBuffer,
      final int codeAttributeOffset,
      final Label[] labels) {
    for (Attribute attributePrototype : attributePrototypes) {
      if (attributePrototype.type.equals(type)) {
        return attributePrototype.read(
            this, offset, length, charBuffer, codeAttributeOffset, labels);
      }
    }
    return new Attribute(type).read(this, offset, length, null, -1, null);
  }

  // -----------------------------------------------------------------------------------------------
  // 工具方法：底层解析
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回类的常量池表中的条目数量。
   *
   * @return 常量池表中的条目数量。
   */
  public int getItemCount() {
    return cpInfoOffsets.length;
  }

  /**
   * 返回此 {@link ClassReader} 中指定 JVMS 'cp_info' 结构（即常量池条目）的起始偏移量加 1。
   * <i>此方法供 {@link Attribute} 子类使用，通常类生成器或适配器不需要使用。</i>
   *
   * @param constantPoolEntryIndex 常量池表中某条目的索引。
   * @return 对应 JVMS 'cp_info' 结构的起始偏移量加 1。
   */
  public int getItem(final int constantPoolEntryIndex) {
    return cpInfoOffsets[constantPoolEntryIndex];
  }

  /**
   * 返回类的常量池表中字符串的最大长度的保守估计值。
   *
   * @return 类的常量池表中字符串的最大长度的保守估计值。
   */
  public int getMaxStringLength() {
    return maxStringLength;
  }

  /**
   * 读取此 {@link ClassReader} 中的一个字节值。
   * <i>此方法用于 {@link Attribute} 子类，通常类生成器或适配器不需要使用。</i>
   *
   * @param offset 要读取的值在 {@link ClassReader} 中的起始偏移量。
   * @return 读取到的值。
   */
  public int readByte(final int offset) {
    return classFileBuffer[offset] & 0xFF;
  }

  /**
   * 读取此 {@link ClassReader} 中的一个无符号 short 值。
   * <i>此方法用于 {@link Attribute} 子类，通常类生成器或适配器不需要使用。</i>
   *
   * @param offset 要读取的值在 {@link ClassReader} 中的起始索引。
   * @return 读取到的值。
   */
  public int readUnsignedShort(final int offset) {
    byte[] classBuffer = classFileBuffer;
    return ((classBuffer[offset] & 0xFF) << 8) | (classBuffer[offset + 1] & 0xFF);
  }

  /**
   * 读取此 {@link ClassReader} 中的一个有符号 short 值。
   * <i>此方法用于 {@link Attribute} 子类，通常类生成器或适配器不需要使用。</i>
   *
   * @param offset 要读取的值在 {@link ClassReader} 中的起始偏移量。
   * @return 读取到的值。
   */
  public short readShort(final int offset) {
    byte[] classBuffer = classFileBuffer;
    return (short) (((classBuffer[offset] & 0xFF) << 8) | (classBuffer[offset + 1] & 0xFF));
  }

  /**
   * 读取此 {@link ClassReader} 中的一个有符号 int 值。
   * <i>此方法用于 {@link Attribute} 子类，通常类生成器或适配器不需要使用。</i>
   *
   * @param offset 要读取的值在 {@link ClassReader} 中的起始偏移量。
   * @return 读取到的值。
   */
  public int readInt(final int offset) {
    byte[] classBuffer = classFileBuffer;
    return ((classBuffer[offset] & 0xFF) << 24)
        | ((classBuffer[offset + 1] & 0xFF) << 16)
        | ((classBuffer[offset + 2] & 0xFF) << 8)
        | (classBuffer[offset + 3] & 0xFF);
  }

  /**
   * 读取此 {@link ClassReader} 中的一个有符号 long 值。
   * <i>此方法用于 {@link Attribute} 子类，通常类生成器或适配器不需要使用。</i>
   *
   * @param offset 要读取的值在 {@link ClassReader} 中的起始偏移量。
   * @return 读取到的值。
   */
  public long readLong(final int offset) {
    long l1 = readInt(offset);
    long l0 = readInt(offset + 4) & 0xFFFFFFFFL;
    return (l1 << 32) | l0;
  }

  /**
   * 读取此 {@link ClassReader} 中的 CONSTANT_Utf8 常量池项。
   * <i>此方法用于 {@link Attribute} 子类，通常类生成器或适配器不需要使用。</i>
   *
   * @param offset {@link ClassReader} 中的一个无符号 short 值的起始偏移量，
   *     该值是类的常量池表中 CONSTANT_Utf8 项的索引。
   * @param charBuffer 用于读取字符串的缓冲区。此缓冲区必须足够大，不会自动调整大小。
   * @return 指定 CONSTANT_Utf8 项对应的字符串。
   */
  // DontCheck(AbbreviationAsWordInName): 不能重命名（为了二进制向后兼容性）。
  public String readUTF8(final int offset, final char[] charBuffer) {
    int constantPoolEntryIndex = readUnsignedShort(offset);
    if (offset == 0 || constantPoolEntryIndex == 0) {
      return null;
    }
    return readUtf(constantPoolEntryIndex, charBuffer);
  }

  /**
   * 读取 {@link #classFileBuffer} 中的 CONSTANT_Utf8 常量池项。
   *
   * @param constantPoolEntryIndex 类的常量池表中 CONSTANT_Utf8 项的索引。
   * @param charBuffer 用于读取字符串的缓冲区。此缓冲区必须足够大，不会自动调整大小。
   * @return 指定 CONSTANT_Utf8 项对应的字符串。
   */
  final String readUtf(final int constantPoolEntryIndex, final char[] charBuffer) {
    String value = constantUtf8Values[constantPoolEntryIndex];
    if (value != null) {
      return value;
    }
    int cpInfoOffset = cpInfoOffsets[constantPoolEntryIndex];
    return constantUtf8Values[constantPoolEntryIndex] =
        readUtf(cpInfoOffset + 2, readUnsignedShort(cpInfoOffset), charBuffer);
  }

  /**
   * 读取 {@link #classFileBuffer} 中的 UTF8 字符串。
   *
   * @param utfOffset 要读取的 UTF8 字符串的起始偏移量。
   * @param utfLength 要读取的 UTF8 字符串的长度。
   * @param charBuffer 用于读取字符串的缓冲区。此缓冲区必须足够大，不会自动调整大小。
   * @return 指定 UTF8 字符串对应的字符串。
   */
  private String readUtf(final int utfOffset, final int utfLength, final char[] charBuffer) {
    int currentOffset = utfOffset;
    int endOffset = currentOffset + utfLength;
    int strLength = 0;
    byte[] classBuffer = classFileBuffer;
    while (currentOffset < endOffset) {
      int currentByte = classBuffer[currentOffset++];
      if ((currentByte & 0x80) == 0) {
        charBuffer[strLength++] = (char) (currentByte & 0x7F);
      } else if ((currentByte & 0xE0) == 0xC0) {
        charBuffer[strLength++] =
            (char) (((currentByte & 0x1F) << 6) + (classBuffer[currentOffset++] & 0x3F));
      } else {
        charBuffer[strLength++] =
            (char)
                (((currentByte & 0xF) << 12)
                    + ((classBuffer[currentOffset++] & 0x3F) << 6)
                    + (classBuffer[currentOffset++] & 0x3F));
      }
    }
    return new String(charBuffer, 0, strLength);
  }

  /**
   * 从 {@link #classFileBuffer} 中读取 CONSTANT_Class、CONSTANT_String、CONSTANT_MethodType、
   * CONSTANT_Module 或 CONSTANT_Package 常量池项。<i>此方法主要供 {@link Attribute} 的子类使用，
   * 通常类生成器或适配器不需要调用。</i>
   *
   * @param offset {@link #classFileBuffer} 中无符号短整型值的起始偏移量，该值是类常量池表中
   *     CONSTANT_Class、CONSTANT_String、CONSTANT_MethodType、CONSTANT_Module 或
   *     CONSTANT_Package 项的索引。
   * @param charBuffer 用于读取条目的字符缓冲区，必须足够大，且不会自动调整大小。
   * @return 与指定常量池项对应的字符串。
   */
  private String readStringish(final int offset, final char[] charBuffer) {
    // 获取 cp_info 结构的起始偏移量（加 1），并读取由该 cp_info 的前两个字节所指定的 CONSTANT_Utf8 项。
    return readUTF8(cpInfoOffsets[readUnsignedShort(offset)], charBuffer);
  }

  /**
   * 从当前 {@link ClassReader} 中读取 CONSTANT_Class 常量池项。
   * <i>此方法主要供 {@link Attribute} 的子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param offset 当前 {@link ClassReader} 中无符号短整型值的起始偏移量，
   *     该值是类常量池表中 CONSTANT_Class 项的索引。
   * @param charBuffer 用于读取条目的字符缓冲区，必须足够大，且不会自动调整大小。
   * @return 与指定 CONSTANT_Class 项对应的字符串。
   */
  public String readClass(final int offset, final char[] charBuffer) {
    return readStringish(offset, charBuffer);
  }

  /**
   * 从当前 {@link ClassReader} 中读取 CONSTANT_Module 常量池项。
   * <i>此方法主要供 {@link Attribute} 的子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param offset 当前 {@link ClassReader} 中无符号短整型值的起始偏移量，
   *     该值是类常量池表中 CONSTANT_Module 项的索引。
   * @param charBuffer 用于读取条目的字符缓冲区，必须足够大，且不会自动调整大小。
   * @return 与指定 CONSTANT_Module 项对应的字符串。
   */
  public String readModule(final int offset, final char[] charBuffer) {
    return readStringish(offset, charBuffer);
  }

  /**
   * 从当前 {@link ClassReader} 中读取 CONSTANT_Package 常量池项。
   * <i>此方法主要供 {@link Attribute} 的子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param offset 当前 {@link ClassReader} 中无符号短整型值的起始偏移量，
   *     该值是类常量池表中 CONSTANT_Package 项的索引。
   * @param charBuffer 用于读取条目的字符缓冲区，必须足够大，且不会自动调整大小。
   * @return 与指定 CONSTANT_Package 项对应的字符串。
   */
  public String readPackage(final int offset, final char[] charBuffer) {
    return readStringish(offset, charBuffer);
  }

  /**
   * 从 {@link #classFileBuffer} 中读取 CONSTANT_Dynamic 常量池项。
   *
   * @param constantPoolEntryIndex 类常量池表中 CONSTANT_Dynamic 项的索引。
   * @param charBuffer 用于读取字符串的字符缓冲区，必须足够大，且不会自动调整大小。
   * @return 与指定 CONSTANT_Dynamic 项对应的 ConstantDynamic 实例。
   */
  private ConstantDynamic readConstantDynamic(
      final int constantPoolEntryIndex, final char[] charBuffer) {
    ConstantDynamic constantDynamic = constantDynamicValues[constantPoolEntryIndex];
    if (constantDynamic != null) {
      return constantDynamic;
    }
    int cpInfoOffset = cpInfoOffsets[constantPoolEntryIndex];
    int nameAndTypeCpInfoOffset = cpInfoOffsets[readUnsignedShort(cpInfoOffset + 2)];
    String name = readUTF8(nameAndTypeCpInfoOffset, charBuffer);
    String descriptor = readUTF8(nameAndTypeCpInfoOffset + 2, charBuffer);
    int bootstrapMethodOffset = bootstrapMethodOffsets[readUnsignedShort(cpInfoOffset)];
    Handle handle = (Handle) readConst(readUnsignedShort(bootstrapMethodOffset), charBuffer);
    Object[] bootstrapMethodArguments = new Object[readUnsignedShort(bootstrapMethodOffset + 2)];
    bootstrapMethodOffset += 4;
    for (int i = 0; i < bootstrapMethodArguments.length; i++) {
      bootstrapMethodArguments[i] = readConst(readUnsignedShort(bootstrapMethodOffset), charBuffer);
      bootstrapMethodOffset += 2;
    }
    return constantDynamicValues[constantPoolEntryIndex] =
        new ConstantDynamic(name, descriptor, handle, bootstrapMethodArguments);
  }

  /**
   * 从当前 {@link ClassReader} 中读取数值或字符串类型的常量池项。
   * <i>此方法主要供 {@link Attribute} 的子类使用，通常类生成器或适配器不需要调用。</i>
   *
   * @param constantPoolEntryIndex 类常量池表中 CONSTANT_Integer、CONSTANT_Float、CONSTANT_Long、
   *     CONSTANT_Double、CONSTANT_Class、CONSTANT_String、CONSTANT_MethodType、
   *     CONSTANT_MethodHandle 或 CONSTANT_Dynamic 项的索引。
   * @param charBuffer 用于读取字符串的字符缓冲区，必须足够大，且不会自动调整大小。
   * @return 与指定常量池项对应的 {@link Integer}、{@link Float}、{@link Long}、{@link Double}、
   *     {@link String}、{@link Type}、{@link Handle} 或 {@link ConstantDynamic}。
   */
  public Object readConst(final int constantPoolEntryIndex, final char[] charBuffer) {
    int cpInfoOffset = cpInfoOffsets[constantPoolEntryIndex];
    switch (classFileBuffer[cpInfoOffset - 1]) {
      case Symbol.CONSTANT_INTEGER_TAG:
        return readInt(cpInfoOffset);
      case Symbol.CONSTANT_FLOAT_TAG:
        return Float.intBitsToFloat(readInt(cpInfoOffset));
      case Symbol.CONSTANT_LONG_TAG:
        return readLong(cpInfoOffset);
      case Symbol.CONSTANT_DOUBLE_TAG:
        return Double.longBitsToDouble(readLong(cpInfoOffset));
      case Symbol.CONSTANT_CLASS_TAG:
        return Type.getObjectType(readUTF8(cpInfoOffset, charBuffer));
      case Symbol.CONSTANT_STRING_TAG:
        return readUTF8(cpInfoOffset, charBuffer);
      case Symbol.CONSTANT_METHOD_TYPE_TAG:
        return Type.getMethodType(readUTF8(cpInfoOffset, charBuffer));
      case Symbol.CONSTANT_METHOD_HANDLE_TAG:
        int referenceKind = readByte(cpInfoOffset);
        int referenceCpInfoOffset = cpInfoOffsets[readUnsignedShort(cpInfoOffset + 1)];
        int nameAndTypeCpInfoOffset = cpInfoOffsets[readUnsignedShort(referenceCpInfoOffset + 2)];
        String owner = readClass(referenceCpInfoOffset, charBuffer);
        String name = readUTF8(nameAndTypeCpInfoOffset, charBuffer);
        String descriptor = readUTF8(nameAndTypeCpInfoOffset + 2, charBuffer);
        boolean isInterface =
            classFileBuffer[referenceCpInfoOffset - 1] == Symbol.CONSTANT_INTERFACE_METHODREF_TAG;
        return new Handle(referenceKind, owner, name, descriptor, isInterface);
      case Symbol.CONSTANT_DYNAMIC_TAG:
        return readConstantDynamic(constantPoolEntryIndex, charBuffer);
      default:
        throw new IllegalArgumentException();
    }
  }
}
