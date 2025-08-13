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
 * 异常处理器信息。对应于 Code 属性中的 exception_table 数组元素，
 * 参照 Java 虚拟机规范（JVMS）定义。Handler 实例可以通过 {@link #nextHandler} 链接起来，
 * 描述完整的 JVMS exception_table 数组。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3">JVMS
 *     4.7.3</a>
 * @author Eric Bruneton
 */
final class Handler {

  /**
   * JVMS exception_table 条目的 start_pc 字段。
   * 对应异常处理器作用域的起始位置（包含）。
   */
  final Label startPc;

  /**
   * JVMS exception_table 条目的 end_pc 字段。
   * 对应异常处理器作用域的结束位置（不包含）。
   */
  final Label endPc;

  /**
   * JVMS exception_table 条目的 handler_pc 字段。
   * 对应异常处理器代码的起始位置。
   */
  final Label handlerPc;

  /**
   * JVMS exception_table 条目的 catch_type 字段。
   * 表示捕获异常类型的常量池索引，值为 0 表示捕获所有异常。
   */
  final int catchType;

  /**
   * 捕获异常类型的内部名称，或 {@literal null} 表示捕获所有异常。
   */
  final String catchTypeDescriptor;

  /** 下一个异常处理器。 */
  Handler nextHandler;

  /**
   * 构造一个新的 Handler。
   *
   * @param startPc JVMS exception_table 的 start_pc 字段。
   * @param endPc JVMS exception_table 的 end_pc 字段。
   * @param handlerPc JVMS exception_table 的 handler_pc 字段。
   * @param catchType JVMS exception_table 的 catch_type 字段。
   * @param catchTypeDescriptor 捕获异常类型的内部名称，或 {@literal null} 表示捕获所有异常。
   */
  Handler(
      final Label startPc,
      final Label endPc,
      final Label handlerPc,
      final int catchType,
      final String catchTypeDescriptor) {
    this.startPc = startPc;
    this.endPc = endPc;
    this.handlerPc = handlerPc;
    this.catchType = catchType;
    this.catchTypeDescriptor = catchTypeDescriptor;
  }

  /**
   * 通过给定的 Handler 以及新的作用域构造一个新的 Handler。
   *
   * @param handler 已有的 Handler。
   * @param startPc 新的 start_pc。
   * @param endPc 新的 end_pc。
   */
  Handler(final Handler handler, final Label startPc, final Label endPc) {
    this(startPc, endPc, handler.handlerPc, handler.catchType, handler.catchTypeDescriptor);
    this.nextHandler = handler.nextHandler;
  }

  /**
   * 从以给定 Handler 为头的链表中，移除位于 start 和 end 范围内的异常处理器。
   *
   * @param firstHandler Handler 链表的头节点，可能为 {@literal null}。
   * @param start 移除范围的起始标签。
   * @param end 移除范围的结束标签，可能为 {@literal null}。
   * @return 移除指定范围后的 Handler 链表。
   */
  static Handler removeRange(final Handler firstHandler, final Label start, final Label end) {
    if (firstHandler == null) {
      return null;
    } else {
      firstHandler.nextHandler = removeRange(firstHandler.nextHandler, start, end);
    }
    int handlerStart = firstHandler.startPc.bytecodeOffset;
    int handlerEnd = firstHandler.endPc.bytecodeOffset;
    int rangeStart = start.bytecodeOffset;
    int rangeEnd = end == null ? Integer.MAX_VALUE : end.bytecodeOffset;
    // 若两区间不相交，直接返回。
    if (rangeStart >= handlerEnd || rangeEnd <= handlerStart) {
      return firstHandler;
    }
    if (rangeStart <= handlerStart) {
      if (rangeEnd >= handlerEnd) {
        // [handlerStart,handlerEnd[ 完全包含于 [rangeStart,rangeEnd[，移除当前 Handler。
        return firstHandler.nextHandler;
      } else {
        // 区间差集为 [rangeEnd, handlerEnd[。
        return new Handler(firstHandler, end, firstHandler.endPc);
      }
    } else if (rangeEnd >= handlerEnd) {
      // 区间差集为 [handlerStart, rangeStart[。
      return new Handler(firstHandler, firstHandler.startPc, start);
    } else {
      // 区间差集为 [handlerStart, rangeStart[ + [rangeEnd, handlerEnd[。
      firstHandler.nextHandler = new Handler(firstHandler, end, firstHandler.endPc);
      return new Handler(firstHandler, firstHandler.startPc, start);
    }
  }

  /**
   * 返回以给定 Handler 为头的链表长度。
   *
   * @param firstHandler Handler 链表的头节点，可能为 {@literal null}。
   * @return Handler 链表中元素数量。
   */
  static int getExceptionTableLength(final Handler firstHandler) {
    int length = 0;
    Handler handler = firstHandler;
    while (handler != null) {
      length++;
      handler = handler.nextHandler;
    }
    return length;
  }

  /**
   * 返回以给定 Handler 为头的链表对应的 JVMS exception_table 大小（字节数）。
   * <i>包括 exception_table_length 字段。</i>
   *
   * @param firstHandler Handler 链表的头节点，可能为 {@literal null}。
   * @return exception_table 长度（字节数）。
   */
  static int getExceptionTableSize(final Handler firstHandler) {
    return 2 + 8 * getExceptionTableLength(firstHandler);
  }

  /**
   * 写出以给定 Handler 为头的链表对应的 JVMS exception_table。
   * <i>包括 exception_table_length 字段。</i>
   *
   * @param firstHandler Handler 链表的头节点，可能为 {@literal null}。
   * @param output 写出目标。
   */
  static void putExceptionTable(final Handler firstHandler, final ByteVector output) {
    output.putShort(getExceptionTableLength(firstHandler));
    Handler handler = firstHandler;
    while (handler != null) {
      output
          .putShort(handler.startPc.bytecodeOffset)
          .putShort(handler.endPc.bytecodeOffset)
          .putShort(handler.handlerPc.bytecodeOffset)
          .putShort(handler.catchType);
      handler = handler.nextHandler;
    }
  }
}
