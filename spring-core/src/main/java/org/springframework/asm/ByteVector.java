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
 * 一个动态可扩展的字节向量。此类大致等价于基于 ByteArrayOutputStream 的 DataOutputStream，
 * 但性能更优。
 *
 * @author Eric Bruneton
 */
public class ByteVector {

  /** 该向量的内容，只有前 {@link #length} 个字节是有效数据。 */
  byte[] data;

  /** 该向量中实际包含的字节数。 */
  int length;

  /** 使用默认初始容量构造新的 {@link ByteVector} 实例。 */
  public ByteVector() {
    data = new byte[64];
  }

  /**
   * 使用指定的初始容量构造新的 {@link ByteVector} 实例。
   *
   * @param initialCapacity 初始容量大小。
   */
  public ByteVector(final int initialCapacity) {
    data = new byte[initialCapacity];
  }

  /**
   * 用给定的初始数据构造新的 {@link ByteVector} 实例。
   *
   * @param data 初始数据。
   */
  ByteVector(final byte[] data) {
    this.data = data;
    this.length = data.length;
  }

  /**
   * 返回该向量实际包含的字节数。
   *
   * @return 向量中实际包含的字节数。
   */
  public int size() {
    return length;
  }

  /**
   * 向字节向量中写入一个字节。必要时自动扩容。
   *
   * @param byteValue 一个字节。
   * @return 当前的字节向量实例。
   */
  public ByteVector putByte(final int byteValue) {
    int currentLength = length;
    if (currentLength + 1 > data.length) {
      enlarge(1);
    }
    data[currentLength++] = (byte) byteValue;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入两个字节。必要时自动扩容。
   *
   * @param byteValue1 第一个字节。
   * @param byteValue2 第二个字节。
   * @return 当前的字节向量实例。
   */
  final ByteVector put11(final int byteValue1, final int byteValue2) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个短整型（2字节）。必要时自动扩容。
   *
   * @param shortValue 短整型数值。
   * @return 当前的字节向量实例。
   */
  public ByteVector putShort(final int shortValue) {
    int currentLength = length;
    if (currentLength + 2 > data.length) {
      enlarge(2);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个字节和一个短整型。必要时自动扩容。
   *
   * @param byteValue 字节值。
   * @param shortValue 短整型值。
   * @return 当前的字节向量实例。
   */
  final ByteVector put12(final int byteValue, final int shortValue) {
    int currentLength = length;
    if (currentLength + 3 > data.length) {
      enlarge(3);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入两个字节和一个短整型。必要时自动扩容。
   *
   * @param byteValue1 第一个字节。
   * @param byteValue2 第二个字节。
   * @param shortValue 短整型值。
   * @return 当前的字节向量实例。
   */
  final ByteVector put112(final int byteValue1, final int byteValue2, final int shortValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue1;
    currentData[currentLength++] = (byte) byteValue2;
    currentData[currentLength++] = (byte) (shortValue >>> 8);
    currentData[currentLength++] = (byte) shortValue;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个整型（4字节）。必要时自动扩容。
   *
   * @param intValue 整型数值。
   * @return 当前的字节向量实例。
   */
  public ByteVector putInt(final int intValue) {
    int currentLength = length;
    if (currentLength + 4 > data.length) {
      enlarge(4);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个字节和两个短整型。字节向量会在必要时自动扩容。
   *
   * @param byteValue 一个字节。
   * @param shortValue1 第一个短整型。
   * @param shortValue2 第二个短整型。
   * @return 当前的字节向量实例。
   */
  final ByteVector put122(final int byteValue, final int shortValue1, final int shortValue2) {
    int currentLength = length;
    if (currentLength + 5 > data.length) {
      enlarge(5);
    }
    byte[] currentData = data;
    currentData[currentLength++] = (byte) byteValue;
    currentData[currentLength++] = (byte) (shortValue1 >>> 8);
    currentData[currentLength++] = (byte) shortValue1;
    currentData[currentLength++] = (byte) (shortValue2 >>> 8);
    currentData[currentLength++] = (byte) shortValue2;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个 long 类型数值。字节向量会在必要时自动扩容。
   *
   * @param longValue 一个 long 类型值。
   * @return 当前的字节向量实例。
   */
  public ByteVector putLong(final long longValue) {
    int currentLength = length;
    if (currentLength + 8 > data.length) {
      enlarge(8);
    }
    byte[] currentData = data;
    int intValue = (int) (longValue >>> 32);
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    intValue = (int) longValue;
    currentData[currentLength++] = (byte) (intValue >>> 24);
    currentData[currentLength++] = (byte) (intValue >>> 16);
    currentData[currentLength++] = (byte) (intValue >>> 8);
    currentData[currentLength++] = (byte) intValue;
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个 UTF8 编码的字符串。字节向量会在必要时自动扩容。
   *
   * @param stringValue 一个 UTF8 编码字符串，长度必须小于 65536。
   * @return 当前的字节向量实例。
   */
// DontCheck(AbbreviationAsWordInName): 无法重命名（为保持二进制兼容性）。
  public ByteVector putUTF8(final String stringValue) {
    int charLength = stringValue.length();
    if (charLength > 65535) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    int currentLength = length;
    if (currentLength + 2 + charLength > data.length) {
      enlarge(2 + charLength);
    }
    byte[] currentData = data;
    // 乐观算法：假设字符长度即为字节长度（大多数情况成立），先写长度信息并尝试直接写字符。
    currentData[currentLength++] = (byte) (charLength >>> 8);
    currentData[currentLength++] = (byte) charLength;
    for (int i = 0; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= '\u0001' && charValue <= '\u007F') {
        currentData[currentLength++] = (byte) charValue;
      } else {
        length = currentLength;
        return encodeUtf8(stringValue, i, 65535);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个 UTF8 编码的字符串，字符串长度以两字节编码（如果有空间）。
   * 从 offset 索引的字符开始编码，前面的字符假定已编码为单字节。
   *
   * @param stringValue 需要编码的字符串。
   * @param offset 第一个需要编码字符的索引，之前字符假定已编码。
   * @param maxByteLength 最大编码字节数（包含已编码部分）。
   * @return 当前的字节向量实例。
   */
  final ByteVector encodeUtf8(final String stringValue, final int offset, final int maxByteLength) {
    int charLength = stringValue.length();
    int byteLength = offset;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        byteLength++;
      } else if (charValue <= 0x07FF) {
        byteLength += 2;
      } else {
        byteLength += 3;
      }
    }
    if (byteLength > maxByteLength) {
      throw new IllegalArgumentException("UTF8 string too large");
    }
    // 计算并写入字节长度
    int byteLengthOffset = length - offset - 2;
    if (byteLengthOffset >= 0) {
      data[byteLengthOffset] = (byte) (byteLength >>> 8);
      data[byteLengthOffset + 1] = (byte) byteLength;
    }
    if (length + byteLength - offset > data.length) {
      enlarge(byteLength - offset);
    }
    int currentLength = length;
    for (int i = offset; i < charLength; ++i) {
      char charValue = stringValue.charAt(i);
      if (charValue >= 0x0001 && charValue <= 0x007F) {
        data[currentLength++] = (byte) charValue;
      } else if (charValue <= 0x07FF) {
        data[currentLength++] = (byte) (0xC0 | charValue >> 6 & 0x1F);
        data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      } else {
        data[currentLength++] = (byte) (0xE0 | charValue >> 12 & 0xF);
        data[currentLength++] = (byte) (0x80 | charValue >> 6 & 0x3F);
        data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
      }
    }
    length = currentLength;
    return this;
  }

  /**
   * 向字节向量中写入一个字节数组。字节向量会在必要时自动扩容。
   *
   * @param byteArrayValue 目标字节数组。可能为 {@literal null}，此时写入 {@code byteLength} 个 0 字节。
   * @param byteOffset 要复制的字节数组的起始索引。
   * @param byteLength 要复制的字节数量。
   * @return 当前的字节向量实例。
   */
  public ByteVector putByteArray(
      final byte[] byteArrayValue, final int byteOffset, final int byteLength) {
    if (length + byteLength > data.length) {
      enlarge(byteLength);
    }
    if (byteArrayValue != null) {
      System.arraycopy(byteArrayValue, byteOffset, data, length, byteLength);
    }
    length += byteLength;
    return this;
  }

  /**
   * 扩容字节向量，使其至少能容纳额外的 size 个字节。
   *
   * @param size 需要额外容纳的字节数。
   */
  private void enlarge(final int size) {
    if (length > data.length) {
      throw new AssertionError("内部错误");
    }
    int doubleCapacity = 2 * data.length;
    int minimalCapacity = length + size;
    byte[] newData = new byte[doubleCapacity > minimalCapacity ? doubleCapacity : minimalCapacity];
    System.arraycopy(data, 0, newData, 0, length);
    data = newData;
  }
}
