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
 * 对字段或方法的引用。
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public final class Handle {

  /**
   * 该句柄所表示的字段或方法的类型。应当是 {@link Opcodes#H_GETFIELD}、
   * {@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、{@link Opcodes#H_PUTSTATIC}、
   * {@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、{@link Opcodes#H_INVOKESPECIAL}、
   * {@link Opcodes#H_NEWINVOKESPECIAL} 或 {@link Opcodes#H_INVOKEINTERFACE} 之一。
   */
  private final int tag;

  /** 拥有该字段或方法的类的内部名称。 */
  private final String owner;

  /** 该句柄所指字段或方法的名称。 */
  private final String name;

  /** 该字段或方法的描述符。 */
  private final String descriptor;

  /** 指示拥有者是否为接口。 */
  private final boolean isInterface;

  /**
   * 构造一个新的字段或方法句柄。
   *
   * @param tag 该句柄所表示的字段或方法类型，必须是 {@link Opcodes#H_GETFIELD}、
   *     {@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、{@link Opcodes#H_PUTSTATIC}、
   *     {@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、
   *     {@link Opcodes#H_INVOKESPECIAL}、{@link Opcodes#H_NEWINVOKESPECIAL} 或 {@link Opcodes#H_INVOKEINTERFACE}。
   * @param owner 拥有该字段或方法的类的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   * @deprecated 此构造函数已被 {@link #Handle(int, String, String, String, boolean)} 取代。
   */
  @Deprecated
  public Handle(final int tag, final String owner, final String name, final String descriptor) {
    this(tag, owner, name, descriptor, tag == Opcodes.H_INVOKEINTERFACE);
  }

  /**
   * 构造一个新的字段或方法句柄。
   *
   * @param tag 该句柄所表示的字段或方法类型，必须是 {@link Opcodes#H_GETFIELD}、
   *     {@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、{@link Opcodes#H_PUTSTATIC}、
   *     {@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、
   *     {@link Opcodes#H_INVOKESPECIAL}、{@link Opcodes#H_NEWINVOKESPECIAL} 或 {@link Opcodes#H_INVOKEINTERFACE}。
   * @param owner 拥有该字段或方法的类的内部名称。
   * @param name 字段或方法名称。
   * @param descriptor 字段或方法描述符。
   * @param isInterface 拥有者是否为接口。
   */
  public Handle(
      final int tag,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    this.tag = tag;
    this.owner = owner;
    this.name = name;
    this.descriptor = descriptor;
    this.isInterface = isInterface;
  }

  /**
   * 返回该句柄所表示字段或方法的类型。
   *
   * @return {@link Opcodes#H_GETFIELD}、{@link Opcodes#H_GETSTATIC}、{@link Opcodes#H_PUTFIELD}、
   *     {@link Opcodes#H_PUTSTATIC}、{@link Opcodes#H_INVOKEVIRTUAL}、{@link Opcodes#H_INVOKESTATIC}、
   *     {@link Opcodes#H_INVOKESPECIAL}、{@link Opcodes#H_NEWINVOKESPECIAL} 或 {@link Opcodes#H_INVOKEINTERFACE}。
   */
  public int getTag() {
    return tag;
  }

  /**
   * 返回拥有该字段或方法的类的内部名称。
   *
   * @return 拥有该字段或方法的类的内部名称。
   */
  public String getOwner() {
    return owner;
  }

  /**
   * 返回该句柄指向的字段或方法的名称。
   *
   * @return 字段或方法名称。
   */
  public String getName() {
    return name;
  }

  /**
   * 返回该句柄指向字段或方法的描述符。
   *
   * @return 字段或方法描述符。
   */
  public String getDesc() {
    return descriptor;
  }

  /**
   * 判断拥有者是否为接口。
   *
   * @return 如果拥有者是接口，返回 true。
   */
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof Handle)) {
      return false;
    }
    Handle handle = (Handle) object;
    return tag == handle.tag
        && isInterface == handle.isInterface
        && owner.equals(handle.owner)
        && name.equals(handle.name)
        && descriptor.equals(handle.descriptor);
  }

  @Override
  public int hashCode() {
    return tag
        + (isInterface ? 64 : 0)
        + owner.hashCode() * name.hashCode() * descriptor.hashCode();
  }

  /**
   * 返回该句柄的文本表示形式。格式如下：
   *
   * <ul>
   *   <li>对于类的引用：owner "." name descriptor " (" tag ")"
   *   <li>对于接口的引用：owner "." name descriptor " (" tag " itf)"。
   * </ul>
   */
  @Override
  public String toString() {
    return owner + '.' + name + descriptor + " (" + tag + (isInterface ? " itf" : "") + ')';
  }
}
