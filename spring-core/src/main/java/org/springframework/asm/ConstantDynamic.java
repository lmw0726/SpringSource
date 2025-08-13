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

import java.util.Arrays;

/**
 * 一个值在运行时通过引导方法计算得到的常量。  
 *
 * @author Remi Forax
 */
public final class ConstantDynamic {

  /** 常量名称（可以是任意字符串）。 */
  private final String name;

  /** 常量类型（必须是字段描述符）。 */
  private final String descriptor;

  /** 用于在运行时计算常量值的引导方法。 */
  private final Handle bootstrapMethod;

  /**
   * 传递给引导方法的参数，用于在运行时计算常量值。  
   */
  private final Object[] bootstrapMethodArguments;

  /**
   * 构造一个新的 {@link ConstantDynamic} 实例。  
   *
   * @param name 常量名称（可以是任意字符串）。  
   * @param descriptor 常量类型（必须是字段描述符）。  
   * @param bootstrapMethod 用于在运行时计算常量值的引导方法。  
   * @param bootstrapMethodArguments 传递给引导方法的参数，用于计算常量值。  
   */
  public ConstantDynamic(
      final String name,
      final String descriptor,
      final Handle bootstrapMethod,
      final Object... bootstrapMethodArguments) {
    this.name = name;
    this.descriptor = descriptor;
    this.bootstrapMethod = bootstrapMethod;
    this.bootstrapMethodArguments = bootstrapMethodArguments;
  }

  /**
   * 返回此常量的名称。  
   *
   * @return 此常量的名称。  
   */
  public String getName() {
    return name;
  }

  /**
   * 返回此常量的类型。  
   *
   * @return 以字段描述符形式表示的此常量类型。  
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * 返回用于计算此常量值的引导方法（bootstrap method）。  
   *
   * @return 用于计算此常量值的引导方法。  
   */
  public Handle getBootstrapMethod() {
    return bootstrapMethod;
  }

  /**
   * 返回传递给引导方法以计算此常量值的参数数量。  
   *
   * @return 传递给引导方法以计算此常量值的参数数量。  
   */
  public int getBootstrapMethodArgumentCount() {
    return bootstrapMethodArguments.length;
  }

  /**
   * 返回传递给引导方法的某个参数，用于计算此常量值。  
   *
   * @param index 参数索引，范围在 0 到 {@link #getBootstrapMethodArgumentCount()}（不含）之间。  
   * @return 传递给引导方法的指定索引的参数。  
   */
  public Object getBootstrapMethodArgument(final int index) {
    return bootstrapMethodArguments[index];
  }

  /**
   * 返回传递给引导方法的所有参数，用于计算此常量值。  
   * 警告：该数组不应被修改，也不应返回给用户。  
   *
   * @return 传递给引导方法的参数数组。  
   */
  Object[] getBootstrapMethodArgumentsUnsafe() {
    return bootstrapMethodArguments;
  }

  /**
   * 返回此常量的大小。  
   *
   * @return 此常量的大小，即 {@code long} 和 {@code double} 为 2，其他类型为 1。  
   */
  public int getSize() {
    char firstCharOfDescriptor = descriptor.charAt(0);
    return (firstCharOfDescriptor == 'J' || firstCharOfDescriptor == 'D') ? 2 : 1;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof ConstantDynamic)) {
      return false;
    }
    ConstantDynamic constantDynamic = (ConstantDynamic) object;
    return name.equals(constantDynamic.name)
        && descriptor.equals(constantDynamic.descriptor)
        && bootstrapMethod.equals(constantDynamic.bootstrapMethod)
        && Arrays.equals(bootstrapMethodArguments, constantDynamic.bootstrapMethodArguments);
  }

  @Override
  public int hashCode() {
    return name.hashCode()
        ^ Integer.rotateLeft(descriptor.hashCode(), 8)
        ^ Integer.rotateLeft(bootstrapMethod.hashCode(), 16)
        ^ Integer.rotateLeft(Arrays.hashCode(bootstrapMethodArguments), 24);
  }

  @Override
  public String toString() {
    return name
        + " : "
        + descriptor
        + ' '
        + bootstrapMethod
        + ' '
        + Arrays.toString(bootstrapMethodArguments);
  }
}
