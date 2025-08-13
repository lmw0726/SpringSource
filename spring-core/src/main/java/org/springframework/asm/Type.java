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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 一个表示 Java 字段或方法类型的类。该类可以简化对类型和方法描述符的操作。
 *
 * @author Eric Bruneton
 * @author Chris Nokleberg
 */
public final class Type {

  /** {@code void} 类型的种类。详见 {@link #getSort}。 */
  public static final int VOID = 0;

  /** {@code boolean} 类型的种类。详见 {@link #getSort}。 */
  public static final int BOOLEAN = 1;

  /** {@code char} 类型的种类。详见 {@link #getSort}。 */
  public static final int CHAR = 2;

  /** {@code byte} 类型的种类。详见 {@link #getSort}。 */
  public static final int BYTE = 3;

  /** {@code short} 类型的种类。详见 {@link #getSort}。 */
  public static final int SHORT = 4;

  /** {@code int} 类型的种类。详见 {@link #getSort}。 */
  public static final int INT = 5;

  /** {@code float} 类型的种类。详见 {@link #getSort}。 */
  public static final int FLOAT = 6;

  /** {@code long} 类型的种类。详见 {@link #getSort}。 */
  public static final int LONG = 7;

  /** {@code double} 类型的种类。详见 {@link #getSort}。 */
  public static final int DOUBLE = 8;

  /** 数组引用类型的种类。详见 {@link #getSort}。 */
  public static final int ARRAY = 9;

  /** 对象引用类型的种类。详见 {@link #getSort}。 */
  public static final int OBJECT = 10;

  /** 方法类型的种类。详见 {@link #getSort}。 */
  public static final int METHOD = 11;

  /** （私有）用内部名称表示的对象引用类型的种类。 */
  private static final int INTERNAL = 12;

  /** 原始类型的描述符。 */
  private static final String PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

  /** {@code void} 类型实例。 */
  public static final Type VOID_TYPE = new Type(VOID, PRIMITIVE_DESCRIPTORS, VOID, VOID + 1);

  /** {@code boolean} 类型实例。 */
  public static final Type BOOLEAN_TYPE =
      new Type(BOOLEAN, PRIMITIVE_DESCRIPTORS, BOOLEAN, BOOLEAN + 1);

  /** {@code char} 类型实例。 */
  public static final Type CHAR_TYPE = new Type(CHAR, PRIMITIVE_DESCRIPTORS, CHAR, CHAR + 1);

  /** {@code byte} 类型实例。 */
  public static final Type BYTE_TYPE = new Type(BYTE, PRIMITIVE_DESCRIPTORS, BYTE, BYTE + 1);

  /** {@code short} 类型实例。 */
  public static final Type SHORT_TYPE = new Type(SHORT, PRIMITIVE_DESCRIPTORS, SHORT, SHORT + 1);

  /** {@code int} 类型实例。 */
  public static final Type INT_TYPE = new Type(INT, PRIMITIVE_DESCRIPTORS, INT, INT + 1);

  /** {@code float} 类型实例。 */
  public static final Type FLOAT_TYPE = new Type(FLOAT, PRIMITIVE_DESCRIPTORS, FLOAT, FLOAT + 1);

  /** {@code long} 类型实例。 */
  public static final Type LONG_TYPE = new Type(LONG, PRIMITIVE_DESCRIPTORS, LONG, LONG + 1);

  /** {@code double} 类型实例。 */
  public static final Type DOUBLE_TYPE =
      new Type(DOUBLE, PRIMITIVE_DESCRIPTORS, DOUBLE, DOUBLE + 1);

  // -----------------------------------------------------------------------------------------------
  // 字段
  // -----------------------------------------------------------------------------------------------

  /**
   * 此类型的种类。取值可以是 {@link #VOID}, {@link #BOOLEAN}, {@link #CHAR}, {@link #BYTE},
   * {@link #SHORT}, {@link #INT}, {@link #FLOAT}, {@link #LONG}, {@link #DOUBLE}, {@link #ARRAY},
   * {@link #OBJECT}, {@link #METHOD} 或 {@link #INTERNAL}。
   */
  private final int sort;

  /**
   * 包含此字段或方法类型值的缓冲区。对于 {@link #OBJECT} 和 {@link #INTERNAL} 类型，此值是内部名称；
   * 对于其他类型，则是字段或方法描述符。
   *
   * <p>对于 {@link #OBJECT} 类型，该字段也包含描述符：字符区间 [{@link #valueBegin},{@link #valueEnd}) 
   * 是内部名称，而区间 [{@link #valueBegin} - 1, {@link #valueEnd} + 1) 是描述符。
   */
  private final String valueBuffer;

  /**
   * 此 Java 字段或方法类型在 {@link #valueBuffer} 中值的起始索引（包含）。
   * 对于 {@link #OBJECT} 和 {@link #INTERNAL} 类型是内部名称，对于其他类型是字段或方法描述符。
   */
  private final int valueBegin;

  /**
   * 此 Java 字段或方法类型在 {@link #valueBuffer} 中值的结束索引（不包含）。
   * 对于 {@link #OBJECT} 和 {@link #INTERNAL} 类型是内部名称，对于其他类型是字段或方法描述符。
   */
  private final int valueEnd;

  /**
   * 构造一个引用类型。
   *
   * @param sort 此类型的种类，参见 {@link #sort}。
   * @param valueBuffer 包含此字段或方法类型值的缓冲区。
   * @param valueBegin 此字段或方法类型值在 valueBuffer 中的起始索引（包含）。
   * @param valueEnd 此字段或方法类型值在 valueBuffer 中的结束索引（不包含）。
   */
  private Type(final int sort, final String valueBuffer, final int valueBegin, final int valueEnd) {
    this.sort = sort;
    this.valueBuffer = valueBuffer;
    this.valueBegin = valueBegin;
    this.valueEnd = valueEnd;
  }

  // -----------------------------------------------------------------------------------------------
  // 根据描述符、反射得到的 Method 或 Constructor、其它类型等获取 Type 的方法
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回对应给定类型描述符的 {@link Type}。
   *
   * @param typeDescriptor 字段或方法类型描述符。
   * @return 对应给定类型描述符的 {@link Type}。
   */
  public static Type getType(final String typeDescriptor) {
    return getTypeInternal(typeDescriptor, 0, typeDescriptor.length());
  }

  /**
   * 返回对应给定类的 {@link Type}。
   *
   * @param clazz 一个类。
   * @return 对应给定类的 {@link Type}。
   */
  public static Type getType(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == Integer.TYPE) {
        return INT_TYPE;
      } else if (clazz == Void.TYPE) {
        return VOID_TYPE;
      } else if (clazz == Boolean.TYPE) {
        return BOOLEAN_TYPE;
      } else if (clazz == Byte.TYPE) {
        return BYTE_TYPE;
      } else if (clazz == Character.TYPE) {
        return CHAR_TYPE;
      } else if (clazz == Short.TYPE) {
        return SHORT_TYPE;
      } else if (clazz == Double.TYPE) {
        return DOUBLE_TYPE;
      } else if (clazz == Float.TYPE) {
        return FLOAT_TYPE;
      } else if (clazz == Long.TYPE) {
        return LONG_TYPE;
      } else {
        throw new AssertionError();
      }
    } else {
      return getType(getDescriptor(clazz));
    }
  }

  /**
   * 返回给定构造函数对应的方法 {@link Type}。
   *
   * @param constructor 构造函数对象。
   * @return 给定构造函数对应的方法 {@link Type}。
   */
  public static Type getType(final Constructor<?> constructor) {
    return getType(getConstructorDescriptor(constructor));
  }

  /**
   * 返回给定方法对应的方法 {@link Type}。
   *
   * @param method 方法对象。
   * @return 给定方法对应的方法 {@link Type}。
   */
  public static Type getType(final Method method) {
    return getType(getMethodDescriptor(method));
  }

  /**
   * 返回此数组类型的元素类型。此方法仅应用于数组类型。
   *
   * @return 此数组类型的元素类型。
   */
  public Type getElementType() {
    final int numDimensions = getDimensions();
    return getTypeInternal(valueBuffer, valueBegin + numDimensions, valueEnd);
  }

  /**
   * 返回对应给定内部名称的 {@link Type}。
   *
   * @param internalName 内部名称。
   * @return 对应给定内部名称的 {@link Type}。
   */
  public static Type getObjectType(final String internalName) {
    return new Type(
        internalName.charAt(0) == '[' ? ARRAY : INTERNAL, internalName, 0, internalName.length());
  }

  /**
   * 返回给定方法描述符对应的 {@link Type}。等价于 <code>Type.getType(methodDescriptor)</code>。
   *
   * @param methodDescriptor 方法描述符。
   * @return 给定方法描述符对应的 {@link Type}。
   */
  public static Type getMethodType(final String methodDescriptor) {
    return new Type(METHOD, methodDescriptor, 0, methodDescriptor.length());
  }

  /**
   * 返回给定返回类型和参数类型对应的方法 {@link Type}。
   *
   * @param returnType 方法的返回类型。
   * @param argumentTypes 方法的参数类型数组。
   * @return 给定返回类型和参数类型对应的方法 {@link Type}。
   */
  public static Type getMethodType(final Type returnType, final Type... argumentTypes) {
    return getType(getMethodDescriptor(returnType, argumentTypes));
  }

  /**
   * 返回此类型所表示方法的参数类型。此方法只能用于方法类型。
   *
   * @return 此类型所表示方法的参数类型数组。
   */
  public Type[] getArgumentTypes() {
    return getArgumentTypes(getDescriptor());
  }

  /**
   * 返回给定方法描述符对应的参数类型的 {@link Type} 数组。
   *
   * @param methodDescriptor 方法描述符。
   * @return 给定方法描述符对应的参数类型的 {@link Type} 数组。
   */
  public static Type[] getArgumentTypes(final String methodDescriptor) {
    // 第一步：计算方法描述符中参数类型的数量。
    int numArgumentTypes = 0;
    // 跳过第一个字符 '('。
    int currentOffset = 1;
    // 逐个解析参数类型。
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // 跳过参数描述符的内容。
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      ++numArgumentTypes;
    }

    // 第二步：为每个参数类型创建一个 Type 实例。
    Type[] argumentTypes = new Type[numArgumentTypes];
    // 重新跳过第一个字符 '('。
    currentOffset = 1;
    // 逐个解析并创建参数类型。
    int currentArgumentTypeIndex = 0;
    while (methodDescriptor.charAt(currentOffset) != ')') {
      final int currentArgumentTypeOffset = currentOffset;
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // 跳过参数描述符内容。
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
      argumentTypes[currentArgumentTypeIndex++] =
          getTypeInternal(methodDescriptor, currentArgumentTypeOffset, currentOffset);
    }
    return argumentTypes;
  }

  /**
   * 返回给定方法的参数类型对应的 {@link Type} 数组。
   *
   * @param method 方法对象。
   * @return 给定方法的参数类型对应的 {@link Type} 数组。
   */
  public static Type[] getArgumentTypes(final Method method) {
    Class<?>[] classes = method.getParameterTypes();
    Type[] types = new Type[classes.length];
    for (int i = classes.length - 1; i >= 0; --i) {
      types[i] = getType(classes[i]);
    }
    return types;
  }

  /**
   * 返回此类型所表示方法的返回类型。此方法只能用于方法类型。
   *
   * @return 此类型所表示方法的返回类型。
   */
  public Type getReturnType() {
    return getReturnType(getDescriptor());
  }

  /**
   * 返回给定方法描述符对应的返回类型的 {@link Type}。
   *
   * @param methodDescriptor 方法描述符。
   * @return 给定方法描述符对应的返回类型的 {@link Type}。
   */
  public static Type getReturnType(final String methodDescriptor) {
    return getTypeInternal(
        methodDescriptor, getReturnTypeOffset(methodDescriptor), methodDescriptor.length());
  }

  /**
   * 返回给定方法对应的返回类型的 {@link Type}。
   *
   * @param method 方法对象。
   * @return 给定方法对应的返回类型的 {@link Type}。
   */
  public static Type getReturnType(final Method method) {
    return getType(method.getReturnType());
  }

  /**
   * 返回给定方法描述符中返回类型的起始索引。
   *
   * @param methodDescriptor 方法描述符。
   * @return 给定方法描述符中返回类型的起始索引。
   */
  static int getReturnTypeOffset(final String methodDescriptor) {
    // 跳过第一个字符 '('。
    int currentOffset = 1;
    // 逐个跳过参数类型。
    while (methodDescriptor.charAt(currentOffset) != ')') {
      while (methodDescriptor.charAt(currentOffset) == '[') {
        currentOffset++;
      }
      if (methodDescriptor.charAt(currentOffset++) == 'L') {
        // 跳过参数描述符内容。
        int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
        currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
      }
    }
    return currentOffset + 1;
  }

  /**
   * 返回对应于给定字段或方法描述符的 {@link Type}。
   *
   * @param descriptorBuffer 包含字段或方法描述符的字符串。
   * @param descriptorBegin 字段或方法描述符在 descriptorBuffer 中的起始索引（包含）。
   * @param descriptorEnd 字段或方法描述符在 descriptorBuffer 中的结束索引（不包含）。
   * @return 对应于给定类型描述符的 {@link Type}。
   */
  private static Type getTypeInternal(
      final String descriptorBuffer, final int descriptorBegin, final int descriptorEnd) {
    switch (descriptorBuffer.charAt(descriptorBegin)) {
      case 'V':
        return VOID_TYPE;
      case 'Z':
        return BOOLEAN_TYPE;
      case 'C':
        return CHAR_TYPE;
      case 'B':
        return BYTE_TYPE;
      case 'S':
        return SHORT_TYPE;
      case 'I':
        return INT_TYPE;
      case 'F':
        return FLOAT_TYPE;
      case 'J':
        return LONG_TYPE;
      case 'D':
        return DOUBLE_TYPE;
      case '[':
        return new Type(ARRAY, descriptorBuffer, descriptorBegin, descriptorEnd);
      case 'L':
        return new Type(OBJECT, descriptorBuffer, descriptorBegin + 1, descriptorEnd - 1);
      case '(':
        return new Type(METHOD, descriptorBuffer, descriptorBegin, descriptorEnd);
      default:
        throw new IllegalArgumentException("Invalid descriptor: " + descriptorBuffer);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 获取类名、内部名称或描述符的方法。
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回对应于此类型的类的二进制名称。此方法不能用于方法类型。
   *
   * @return 对应于此类型的类的二进制名称。
   */
  public String getClassName() {
    switch (sort) {
      case VOID:
        return "void";
      case BOOLEAN:
        return "boolean";
      case CHAR:
        return "char";
      case BYTE:
        return "byte";
      case SHORT:
        return "short";
      case INT:
        return "int";
      case FLOAT:
        return "float";
      case LONG:
        return "long";
      case DOUBLE:
        return "double";
      case ARRAY:
        StringBuilder stringBuilder = new StringBuilder(getElementType().getClassName());
        for (int i = getDimensions(); i > 0; --i) {
          stringBuilder.append("[]");
        }
        return stringBuilder.toString();
      case OBJECT:
      case INTERNAL:
        return valueBuffer.substring(valueBegin, valueEnd).replace('/', '.');
      default:
        throw new AssertionError();
    }
  }

  /**
   * 返回对应于此对象类型或数组类型的类的内部名称。类的内部名称是其完全限定名（
   * 如 Class.getName() 返回的名称，将 '.' 替换为 '/'）。此方法仅应用于对象或数组类型。
   *
   * @return 此对象类型对应的类的内部名称。
   */
  public String getInternalName() {
    return valueBuffer.substring(valueBegin, valueEnd);
  }

  /**
   * 返回给定类的内部名称。类的内部名称是其完全限定名（
   * 如 Class.getName() 返回的名称，将 '.' 替换为 '/'）。
   *
   * @param clazz 一个对象类或数组类。
   * @return 给定类的内部名称。
   */
  public static String getInternalName(final Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  /**
   * 返回对应于此类型的描述符。
   *
   * @return 此类型对应的描述符。
   */
  public String getDescriptor() {
    if (sort == OBJECT) {
      return valueBuffer.substring(valueBegin - 1, valueEnd + 1);
    } else if (sort == INTERNAL) {
      return 'L' + valueBuffer.substring(valueBegin, valueEnd) + ';';
    } else {
      return valueBuffer.substring(valueBegin, valueEnd);
    }
  }

  /**
   * 返回给定类对应的描述符。
   *
   * @param clazz 一个对象类、基本类型类或数组类。
   * @return 给定类对应的描述符。
   */
  public static String getDescriptor(final Class<?> clazz) {
    StringBuilder stringBuilder = new StringBuilder();
    appendDescriptor(clazz, stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * 返回给定构造函数对应的描述符。
   *
   * @param constructor 一个 {@link Constructor} 对象。
   * @return 给定构造函数的描述符。
   */
  public static String getConstructorDescriptor(final Constructor<?> constructor) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = constructor.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    return stringBuilder.append(")V").toString();
  }

  /**
   * 返回给定返回类型和参数类型对应的方法描述符。
   *
   * @param returnType 方法的返回类型。
   * @param argumentTypes 方法的参数类型数组。
   * @return 给定参数和返回类型对应的方法描述符。
   */
  public static String getMethodDescriptor(final Type returnType, final Type... argumentTypes) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    for (Type argumentType : argumentTypes) {
      argumentType.appendDescriptor(stringBuilder);
    }
    stringBuilder.append(')');
    returnType.appendDescriptor(stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * 返回给定方法对应的描述符。
   *
   * @param method 一个 {@link Method} 对象。
   * @return 给定方法的描述符。
   */
  public static String getMethodDescriptor(final Method method) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    stringBuilder.append(')');
    appendDescriptor(method.getReturnType(), stringBuilder);
    return stringBuilder.toString();
  }

  /**
   * 将此类型对应的描述符附加到给定的字符串构建器中。
   *
   * @param stringBuilder 需要附加描述符的字符串构建器。
   */
  private void appendDescriptor(final StringBuilder stringBuilder) {
    if (sort == OBJECT) {
      stringBuilder.append(valueBuffer, valueBegin - 1, valueEnd + 1);
    } else if (sort == INTERNAL) {
      stringBuilder.append('L').append(valueBuffer, valueBegin, valueEnd).append(';');
    } else {
      stringBuilder.append(valueBuffer, valueBegin, valueEnd);
    }
  }

  /**
   * 将给定类对应的描述符附加到给定的字符串构建器中。
   *
   * @param clazz 需要计算描述符的类。
   * @param stringBuilder 需要附加描述符的字符串构建器。
   */
  private static void appendDescriptor(final Class<?> clazz, final StringBuilder stringBuilder) {
    Class<?> currentClass = clazz;
    while (currentClass.isArray()) {
      stringBuilder.append('[');
      currentClass = currentClass.getComponentType();
    }
    if (currentClass.isPrimitive()) {
      char descriptor;
      if (currentClass == Integer.TYPE) {
        descriptor = 'I';
      } else if (currentClass == Void.TYPE) {
        descriptor = 'V';
      } else if (currentClass == Boolean.TYPE) {
        descriptor = 'Z';
      } else if (currentClass == Byte.TYPE) {
        descriptor = 'B';
      } else if (currentClass == Character.TYPE) {
        descriptor = 'C';
      } else if (currentClass == Short.TYPE) {
        descriptor = 'S';
      } else if (currentClass == Double.TYPE) {
        descriptor = 'D';
      } else if (currentClass == Float.TYPE) {
        descriptor = 'F';
      } else if (currentClass == Long.TYPE) {
        descriptor = 'J';
      } else {
        throw new AssertionError();
      }
      stringBuilder.append(descriptor);
    } else {
      stringBuilder.append('L').append(getInternalName(currentClass)).append(';');
    }
  }

  // -----------------------------------------------------------------------------------------------
  // 获取类型的 sort、维度、大小以及对应指令码的方法。
  // -----------------------------------------------------------------------------------------------

  /**
   * 返回此类型的 sort。
   *
   * @return {@link #VOID}、{@link #BOOLEAN}、{@link #CHAR}、{@link #BYTE}、{@link #SHORT}、{@link #INT}、
   *         {@link #FLOAT}、{@link #LONG}、{@link #DOUBLE}、{@link #ARRAY}、{@link #OBJECT} 或 {@link #METHOD}。
   */
  public int getSort() {
    return sort == INTERNAL ? OBJECT : sort;
  }

  /**
   * 返回此数组类型的维度数。此方法仅应在数组类型上调用。
   *
   * @return 数组类型的维度数。
   */
  public int getDimensions() {
    int numDimensions = 1;
    while (valueBuffer.charAt(valueBegin + numDimensions) == '[') {
      numDimensions++;
    }
    return numDimensions;
  }

  /**
   * 返回此类型的大小（单位是Java虚拟机栈槽宽度）。此方法不能用于方法类型。
   *
   * @return 此类型的大小，对于 {@code long} 和 {@code double} 返回 2，
   *         对于 {@code void} 返回 0，其它类型返回 1。
   */
  public int getSize() {
    switch (sort) {
      case VOID:
        return 0;
      case BOOLEAN:
      case CHAR:
      case BYTE:
      case SHORT:
      case INT:
      case FLOAT:
      case ARRAY:
      case OBJECT:
      case INTERNAL:
        return 1;
      case LONG:
      case DOUBLE:
        return 2;
      default:
        throw new AssertionError();
    }
  }

  /**
   * 返回此类型的方法参数和返回值的大小。此方法仅应用于方法类型。
   *
   * @return 方法参数大小（加上隐式的 this 参数大小1）和返回值大小打包成的一个整数 i = 
   *         {@code (argumentsSize << 2) | returnSize}。
   *         其中，参数大小等于 {@code i >> 2}，返回值大小等于 {@code i & 0x03}。
   */
  public int getArgumentsAndReturnSizes() {
    return getArgumentsAndReturnSizes(getDescriptor());
  }

  /**
   * 计算指定方法描述符的方法参数和返回值的大小。
   *
   * @param methodDescriptor 方法描述符字符串。
   * @return 方法参数大小（加上隐式的 this 参数大小1）和返回值大小打包成的一个整数 i = 
   *         {@code (argumentsSize << 2) | returnSize}。
   *         其中，参数大小等于 {@code i >> 2}，返回值大小等于 {@code i & 0x03}。
   */
  public static int getArgumentsAndReturnSizes(final String methodDescriptor) {
    int argumentsSize = 1;
    // 跳过第一个字符 '('
    int currentOffset = 1;
    int currentChar = methodDescriptor.charAt(currentOffset);
    // 解析参数类型并累加它们的大小
    while (currentChar != ')') {
      if (currentChar == 'J' || currentChar == 'D') {
        currentOffset++;
        argumentsSize += 2;
      } else {
        while (methodDescriptor.charAt(currentOffset) == '[') {
          currentOffset++;
        }
        if (methodDescriptor.charAt(currentOffset++) == 'L') {
          // 跳过引用类型描述符内容
          int semiColumnOffset = methodDescriptor.indexOf(';', currentOffset);
          currentOffset = Math.max(currentOffset, semiColumnOffset + 1);
        }
        argumentsSize += 1;
      }
      currentChar = methodDescriptor.charAt(currentOffset);
    }
    currentChar = methodDescriptor.charAt(currentOffset + 1);
    if (currentChar == 'V') {
      return argumentsSize << 2;
    } else {
      int returnSize = (currentChar == 'J' || currentChar == 'D') ? 2 : 1;
      return argumentsSize << 2 | returnSize;
    }
  }

  /**
   * 返回一个与此 {@link Type} 适配的 JVM 指令码。此方法不能用于方法类型。
   *
   * @param opcode 一个 JVM 指令码，必须是 ILOAD、ISTORE、IALOAD、IASTORE、IADD、ISUB、IMUL、IDIV、IREM、
   *               INEG、ISHL、ISHR、IUSHR、IAND、IOR、IXOR 或 IRETURN 中的一个。
   * @return 一个类似于给定指令码但适配于此 {@link Type} 的指令码。
   *         例如，如果此类型为 {@code float} 且参数 opcode 为 IRETURN，则返回 FRETURN。
   */
  public int getOpcode(final int opcode) {
    if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
      switch (sort) {
        case BOOLEAN:
        case BYTE:
          return opcode + (Opcodes.BALOAD - Opcodes.IALOAD);
        case CHAR:
          return opcode + (Opcodes.CALOAD - Opcodes.IALOAD);
        case SHORT:
          return opcode + (Opcodes.SALOAD - Opcodes.IALOAD);
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FALOAD - Opcodes.IALOAD);
        case LONG:
          return opcode + (Opcodes.LALOAD - Opcodes.IALOAD);
        case DOUBLE:
          return opcode + (Opcodes.DALOAD - Opcodes.IALOAD);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          return opcode + (Opcodes.AALOAD - Opcodes.IALOAD);
        case METHOD:
        case VOID:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    } else {
      switch (sort) {
        case VOID:
          if (opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return Opcodes.RETURN;
        case BOOLEAN:
        case BYTE:
        case CHAR:
        case SHORT:
        case INT:
          return opcode;
        case FLOAT:
          return opcode + (Opcodes.FRETURN - Opcodes.IRETURN);
        case LONG:
          return opcode + (Opcodes.LRETURN - Opcodes.IRETURN);
        case DOUBLE:
          return opcode + (Opcodes.DRETURN - Opcodes.IRETURN);
        case ARRAY:
        case OBJECT:
        case INTERNAL:
          if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
            throw new UnsupportedOperationException();
          }
          return opcode + (Opcodes.ARETURN - Opcodes.IRETURN);
        case METHOD:
          throw new UnsupportedOperationException();
        default:
          throw new AssertionError();
      }
    }
  }

  // -----------------------------------------------------------------------------------------------
  // equals、hashCode 和 toString 方法。
  // -----------------------------------------------------------------------------------------------

  /**
   * 判断给定对象是否与此类型相等。
   *
   * @param object 要比较的对象。
   * @return 如果给定对象与此类型相等，则返回 {@literal true}。
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Type)) {
      return false;
    }
    Type other = (Type) object;
    if ((sort == INTERNAL ? OBJECT : sort) != (other.sort == INTERNAL ? OBJECT : other.sort)) {
      return false;
    }
    int begin = valueBegin;
    int end = valueEnd;
    int otherBegin = other.valueBegin;
    int otherEnd = other.valueEnd;
    // 比较具体值。
    if (end - begin != otherEnd - otherBegin) {
      return false;
    }
    for (int i = begin, j = otherBegin; i < end; i++, j++) {
      if (valueBuffer.charAt(i) != other.valueBuffer.charAt(j)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 返回此类型的哈希码值。
   *
   * @return 此类型的哈希码值。
   */
  @Override
  public int hashCode() {
    int hashCode = 13 * (sort == INTERNAL ? OBJECT : sort);
    if (sort >= ARRAY) {
      for (int i = valueBegin, end = valueEnd; i < end; i++) {
        hashCode = 17 * (hashCode + valueBuffer.charAt(i));
      }
    }
    return hashCode;
  }

  /**
   * 返回此类型的字符串表示。
   *
   * @return 此类型的描述符。
   */
  @Override
  public String toString() {
    return getDescriptor();
  }
}
