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
 * 当{@link ClassWriter}生成的类的常量池过大时抛出的异常。
 *
 * @author Jason Zaugg
 */
public final class ClassTooLargeException extends IndexOutOfBoundsException {
  private static final long serialVersionUID = 160715609518896765L;

  private final String className;
  private final int constantPoolCount;

  /**
   * 构造一个新的{@link ClassTooLargeException}。
   *
   * @param className 类的内部名称。
   * @param constantPoolCount 类的常量池项数量。
   */
  public ClassTooLargeException(final String className, final int constantPoolCount) {
    super("Class too large: " + className);
    this.className = className;
    this.constantPoolCount = constantPoolCount;
  }

  /**
   * 返回类的内部名称。
   *
   * @return 类的内部名称。
   */
  public String getClassName() {
    return className;
  }

  /**
   * 返回类的常量池项数量。
   *
   * @return 类的常量池项数量。
   */
  public int getConstantPoolCount() {
    return constantPoolCount;
  }
}
