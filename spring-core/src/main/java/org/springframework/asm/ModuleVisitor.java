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
 * 访问Java模块的访问者。该类的方法调用顺序必须是：
 * ( {@code visitMainClass} | ( {@code visitPackage} | {@code visitRequire} | {@code visitExport} | {@code visitOpen} |
 * {@code visitUse} | {@code visitProvide} )* ) {@code visitEnd}。
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public abstract class ModuleVisitor {
  /**
   * 该访问者实现的ASM API版本。该字段的值必须是{@link Opcodes#ASM6}或{@link Opcodes#ASM7}之一。
   */
  protected final int api;

  /**
   * 该访问者委托方法调用的模块访问者。可能为{@literal null}。
   */
  protected ModuleVisitor mv;

  /**
   * 构造一个新的{@link ModuleVisitor}。
   *
   * @param api 该访问者实现的ASM API版本。必须是{@link Opcodes#ASM6}或{@link Opcodes#ASM7}之一。
   */
  protected ModuleVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的{@link ModuleVisitor}。
   *
   * @param api 该访问者实现的ASM API版本。必须是{@link Opcodes#ASM6}或{@link Opcodes#ASM7}之一。
   * @param moduleVisitor 委托方法调用的模块访问者。可以为null。
   */
  protected ModuleVisitor(final int api, final ModuleVisitor moduleVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: 不对ASM experimental进行预览模式检查
    this.api = api;
    this.mv = moduleVisitor;
  }

  /**
   * 访问当前模块的主类。
   *
   * @param mainClass 当前模块主类的内部名称。
   */
  public void visitMainClass(final String mainClass) {
    if (mv != null) {
      mv.visitMainClass(mainClass);
    }
  }

  /**
   * 访问当前模块的一个包。
   *
   * @param packaze 包的内部名称。
   */
  public void visitPackage(final String packaze) {
    if (mv != null) {
      mv.visitPackage(packaze);
    }
  }

  /**
   * 访问当前模块的一个依赖模块。
   *
   * @param module 依赖模块的全限定名（用点号分隔）。
   * @param access 依赖模块的访问标志，可能值包括 {@code ACC_TRANSITIVE}、{@code ACC_STATIC_PHASE}、{@code ACC_SYNTHETIC} 和 {@code ACC_MANDATED}。
   * @param version 编译时依赖模块的版本，可能为 {@literal null}。
   */
  public void visitRequire(final String module, final int access, final String version) {
    if (mv != null) {
      mv.visitRequire(module, access, version);
    }
  }

  /**
   * 访问当前模块导出的包。
   *
   * @param packaze 导出包的内部名称。
   * @param access 导出包的访问标志，有效值包括 {@code ACC_SYNTHETIC} 和 {@code ACC_MANDATED}。
   * @param modules 可以访问该导出包的模块的全限定名数组，或 {@literal null}。
   */
  public void visitExport(final String packaze, final int access, final String... modules) {
    if (mv != null) {
      mv.visitExport(packaze, access, modules);
    }
  }

  /**
   * 访问当前模块的一个开放包。
   *
   * @param packaze 开放包的内部名称。
   * @param access 开放包的访问标志，有效值包括 {@code ACC_SYNTHETIC} 和 {@code ACC_MANDATED}。
   * @param modules 可以使用深度反射访问该开放包类的模块的全限定名数组，或 {@literal null}。
   */
  public void visitOpen(final String packaze, final int access, final String... modules) {
    if (mv != null) {
      mv.visitOpen(packaze, access, modules);
    }
  }

  /**
   * 访问当前模块使用的服务。名称必须是接口或类的内部名称。
   *
   * @param service 服务的内部名称。
   */
  public void visitUse(final String service) {
    if (mv != null) {
      mv.visitUse(service);
    }
  }

  /**
   * 访问服务的实现。
   *
   * @param service 服务的内部名称。
   * @param providers 服务的实现类的内部名称数组（至少有一个实现）。
   */
  public void visitProvide(final String service, final String... providers) {
    if (mv != null) {
      mv.visitProvide(service, providers);
    }
  }

  /**
   * 访问模块的结束。该方法是最后被调用的方法，用于通知访问者访问已完成。
   */
  public void visitEnd() {
    if (mv != null) {
      mv.visitEnd();
    }
  }
}
