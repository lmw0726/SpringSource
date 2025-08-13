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
 * 当{@link ClassWriter}生成的方法的Code属性过大时抛出的异常。
 *
 * @author Jason Zaugg
 */
public final class MethodTooLargeException extends IndexOutOfBoundsException {
  private static final long serialVersionUID = 6807380416709738314L;

  private final String className;
  private final String methodName;
  private final String descriptor;
  private final int codeSize;

  /**
   * 构造一个新的{@link MethodTooLargeException}。
   *
   * @param className 拥有者类的内部名称。
   * @param methodName 方法的名称。
   * @param descriptor 方法的描述符。
   * @param codeSize 方法Code属性的大小，以字节为单位。
   */
  public MethodTooLargeException(
      final String className,
      final String methodName,
      final String descriptor,
      final int codeSize) {
    super("Method too large: " + className + "." + methodName + " " + descriptor);
    this.className = className;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.codeSize = codeSize;
  }

  /**
   * 返回拥有者类的内部名称。
   *
   * @return 拥有者类的内部名称。
   */
  public String getClassName() {
    return className;
  }

  /**
   * 返回方法的名称。
   *
   * @return 方法的名称。
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * 返回方法的描述符。
   *
   * @return 方法的描述符。
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * 返回方法Code属性的大小，以字节为单位。
   *
   * @return 方法Code属性的大小，以字节为单位。
   */
  public int getCodeSize() {
    return codeSize;
  }
}
