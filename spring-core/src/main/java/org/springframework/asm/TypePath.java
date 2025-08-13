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
 * 表示封闭类型中，指向类型参数、通配符边界、数组元素类型或静态内部类型的路径。
 *
 * @author Eric Bruneton
 */
public final class TypePath {

  /** 类型路径中的一步，进入数组类型的元素类型。参见 {@link #getStep}。 */
  public static final int ARRAY_ELEMENT = 0;

  /** 类型路径中的一步，进入类类型的嵌套类型。参见 {@link #getStep}。 */
  public static final int INNER_TYPE = 1;

  /** 类型路径中的一步，进入通配符类型的边界。参见 {@link #getStep}。 */
  public static final int WILDCARD_BOUND = 2;

  /** 类型路径中的一步，进入泛型类型的某个类型参数。参见 {@link #getStep}。 */
  public static final int TYPE_ARGUMENT = 3;

  /**
   * 存储与此 TypePath 对应的 `type_path` 结构（如 Java 虚拟机规范 JVMS 中定义）的字节数组。
   * 该结构在此数组中的起始位置由 {@link #typePathOffset} 指定。
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.2">
   *      JVMS 4.7.20.2</a>
   */
  private final byte[] typePathContainer;

  /** {@link #typePathContainer} 中 type_path JVMS 结构首字节的偏移量。 */
  private final int typePathOffset;

  /**
   * 构造一个新的 TypePath。
   *
   * @param typePathContainer 包含 type_path JVMS 结构的字节数组。
   * @param typePathOffset 该结构在 typePathContainer 中的首字节偏移量。
   */
  TypePath(final byte[] typePathContainer, final int typePathOffset) {
    this.typePathContainer = typePathContainer;
    this.typePathOffset = typePathOffset;
  }

  /**
   * 返回此路径的长度，即路径中的步骤数。
   *
   * @return 路径长度。
   */
  public int getLength() {
    // path_length 存储在 type_path 的第一个字节中。
    return typePathContainer[typePathOffset];
  }

  /**
   * 返回此路径中指定步骤的值。
   *
   * @param index 步骤索引，取值范围为 0（含）到 {@link #getLength()}（不含）。
   * @return {@link #ARRAY_ELEMENT}、{@link #INNER_TYPE}、{@link #WILDCARD_BOUND} 或 {@link #TYPE_ARGUMENT} 之一。
   */
  public int getStep(final int index) {
    // 返回给定索引的路径元素的 type_path_kind。
    return typePathContainer[typePathOffset + 2 * index + 1];
  }

  /**
   * 返回指定步骤所进入的类型参数索引。仅适用于步骤值为 {@link #TYPE_ARGUMENT} 的情况。
   *
   * @param index 步骤索引，取值范围为 0（含）到 {@link #getLength()}（不含）。
   * @return 此步骤所进入的类型参数的索引。
   */
  public int getStepArgument(final int index) {
    // 返回给定索引的路径元素的 type_argument_index。
    return typePathContainer[typePathOffset + 2 * index + 2];
  }

  /**
   * 将字符串形式的类型路径（由 {@link #toString()} 使用的格式）转换为 TypePath 对象。
   *
   * @param typePath 字符串形式的类型路径，可以为 {@literal null} 或空。
   * @return 对应的 TypePath 对象，如果路径为空则返回 {@literal null}。
   */
  public static TypePath fromString(final String typePath) {
    if (typePath == null || typePath.length() == 0) {
      return null;
    }
    int typePathLength = typePath.length();
    ByteVector output = new ByteVector(typePathLength);
    output.putByte(0);
    int typePathIndex = 0;
    while (typePathIndex < typePathLength) {
      char c = typePath.charAt(typePathIndex++);
      if (c == '[') {
        output.put11(ARRAY_ELEMENT, 0);
      } else if (c == '.') {
        output.put11(INNER_TYPE, 0);
      } else if (c == '*') {
        output.put11(WILDCARD_BOUND, 0);
      } else if (c >= '0' && c <= '9') {
        int typeArg = c - '0';
        while (typePathIndex < typePathLength) {
          c = typePath.charAt(typePathIndex++);
          if (c >= '0' && c <= '9') {
            typeArg = typeArg * 10 + c - '0';
          } else if (c == ';') {
            break;
          } else {
            throw new IllegalArgumentException();
          }
        }
        output.put11(TYPE_ARGUMENT, typeArg);
      } else {
        throw new IllegalArgumentException();
      }
    }
    output.data[0] = (byte) (output.length / 2);
    return new TypePath(output.data, 0);
  }

  /**
   * 返回此类型路径的字符串表示形式。
   * {@link #ARRAY_ELEMENT} 步骤用 '[' 表示，
   * {@link #INNER_TYPE} 用 '.' 表示，
   * {@link #WILDCARD_BOUND} 用 '*' 表示，
   * {@link #TYPE_ARGUMENT} 用类型参数索引（十进制）加 ';' 表示。
   */
  @Override
  public String toString() {
    int length = getLength();
    StringBuilder result = new StringBuilder(length * 2);
    for (int i = 0; i < length; ++i) {
      switch (getStep(i)) {
        case ARRAY_ELEMENT:
          result.append('[');
          break;
        case INNER_TYPE:
          result.append('.');
          break;
        case WILDCARD_BOUND:
          result.append('*');
          break;
        case TYPE_ARGUMENT:
          result.append(getStepArgument(i)).append(';');
          break;
        default:
          throw new AssertionError();
      }
    }
    return result.toString();
  }

  /**
   * 将给定的 TypePath 对应的 type_path JVMS 结构写入指定的 ByteVector。
   *
   * @param typePath TypePath 实例，若为空路径则为 {@literal null}。
   * @param output 要写入的 ByteVector。
   */
  static void put(final TypePath typePath, final ByteVector output) {
    if (typePath == null) {
      output.putByte(0);
    } else {
      int length = typePath.typePathContainer[typePath.typePathOffset] * 2 + 1;
      output.putByteArray(typePath.typePathContainer, typePath.typePathOffset, length);
    }
  }
}
