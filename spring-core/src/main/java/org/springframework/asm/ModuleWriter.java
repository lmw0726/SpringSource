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
 * 一个 {@link ModuleVisitor}，用于生成对应的 Module、ModulePackages 和 ModuleMainClass 属性，
 * 这些属性在 Java 虚拟机规范（JVMS）中有定义。
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25">JVMS
 *     4.7.25</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.26">JVMS
 *     4.7.26</a>
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.27">JVMS
 *     4.7.27</a>
 * @author Remi Forax
 * @author Eric Bruneton
 */
final class ModuleWriter extends ModuleVisitor {

  /** 这个 AnnotationWriter 中使用的常量存储位置。 */
  private final SymbolTable symbolTable;

  /** JVMS Module 属性中的 module_name_index 字段。 */
  private final int moduleNameIndex;

  /** JVMS Module 属性中的 module_flags 字段。 */
  private final int moduleFlags;

  /** JVMS Module 属性中的 module_version_index 字段。 */
  private final int moduleVersionIndex;

  /** JVMS Module 属性中的 requires_count 字段。 */
  private int requiresCount;

  /** JVMS Module 属性中 'requires' 数组的二进制内容。 */
  private final ByteVector requires;

  /** JVMS Module 属性中的 exports_count 字段。 */
  private int exportsCount;

  /** JVMS Module 属性中 'exports' 数组的二进制内容。 */
  private final ByteVector exports;

  /** JVMS Module 属性中的 opens_count 字段。 */
  private int opensCount;

  /** JVMS Module 属性中 'opens' 数组的二进制内容。 */
  private final ByteVector opens;

  /** JVMS Module 属性中的 uses_count 字段。 */
  private int usesCount;

  /** JVMS Module 属性中 'uses_index' 数组的二进制内容。 */
  private final ByteVector usesIndex;

  /** JVMS Module 属性中的 provides_count 字段。 */
  private int providesCount;

  /** JVMS Module 属性中 'provides' 数组的二进制内容。 */
  private final ByteVector provides;

  /** JVMS ModulePackages 属性中的 provides_count 字段。 */
  private int packageCount;

  /** JVMS ModulePackages 属性中 'package_index' 数组的二进制内容。 */
  private final ByteVector packageIndex;

  /** JVMS ModuleMainClass 属性中的 main_class_index 字段，或者为 0。 */
  private int mainClassIndex;

  ModuleWriter(final SymbolTable symbolTable, final int name, final int access, final int version) {
    super(/* latest api = */ Opcodes.ASM9);
    this.symbolTable = symbolTable;
    this.moduleNameIndex = name;
    this.moduleFlags = access;
    this.moduleVersionIndex = version;
    this.requires = new ByteVector();
    this.exports = new ByteVector();
    this.opens = new ByteVector();
    this.usesIndex = new ByteVector();
    this.provides = new ByteVector();
    this.packageIndex = new ByteVector();
  }

  @Override
  public void visitMainClass(final String mainClass) {
    this.mainClassIndex = symbolTable.addConstantClass(mainClass).index;
  }

  @Override
  public void visitPackage(final String packaze) {
    packageIndex.putShort(symbolTable.addConstantPackage(packaze).index);
    packageCount++;
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    requires
        .putShort(symbolTable.addConstantModule(module).index)
        .putShort(access)
        .putShort(version == null ? 0 : symbolTable.addConstantUtf8(version));
    requiresCount++;
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    exports.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access);
    if (modules == null) {
      exports.putShort(0);
    } else {
      exports.putShort(modules.length);
      for (String module : modules) {
        exports.putShort(symbolTable.addConstantModule(module).index);
      }
    }
    exportsCount++;
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    opens.putShort(symbolTable.addConstantPackage(packaze).index).putShort(access);
    if (modules == null) {
      opens.putShort(0);
    } else {
      opens.putShort(modules.length);
      for (String module : modules) {
        opens.putShort(symbolTable.addConstantModule(module).index);
      }
    }
    opensCount++;
  }

  @Override
  public void visitUse(final String service) {
    usesIndex.putShort(symbolTable.addConstantClass(service).index);
    usesCount++;
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    provides.putShort(symbolTable.addConstantClass(service).index);
    provides.putShort(providers.length);
    for (String provider : providers) {
      provides.putShort(symbolTable.addConstantClass(provider).index);
    }
    providesCount++;
  }

  @Override
  public void visitEnd() {
    // 无需执行任何操作。
  }

  /**
   * 返回由该 ModuleWriter 生成的 Module、ModulePackages 和 ModuleMainClass 属性的数量。
   *
   * @return Module、ModulePackages 和 ModuleMainClass 属性的数量（介于1到3之间）。
   */
  int getAttributeCount() {
    return 1 + (packageCount > 0 ? 1 : 0) + (mainClassIndex > 0 ? 1 : 0);
  }

  /**
   * 返回由该 ModuleWriter 生成的 Module、ModulePackages 和 ModuleMainClass 属性的字节大小。
   * 同时将这些属性的名称添加到常量池中。
   *
   * @return Module、ModulePackages 和 ModuleMainClass 属性的字节大小。
   */
  int computeAttributesSize() {
    symbolTable.addConstantUtf8(Constants.MODULE);
    // 6字节的属性头，6字节的名称、标志和版本，5个计数字段各2字节。
    int size =
        22 + requires.length + exports.length + opens.length + usesIndex.length + provides.length;
    if (packageCount > 0) {
      symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES);
      // 6字节的属性头，2字节的package_count字段。
      size += 8 + packageIndex.length;
    }
    if (mainClassIndex > 0) {
      symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS);
      // 6字节的属性头，2字节的main_class_index字段。
      size += 8;
    }
    return size;
  }

  /**
   * 将该 ModuleWriter 生成的 Module、ModulePackages 和 ModuleMainClass 属性写入给定的 ByteVector。
   *
   * @param output 用于写入属性的 ByteVector。
   */
  void putAttributes(final ByteVector output) {
    // 6字节的名称、标志和版本字段，5个计数字段各2字节。
    int moduleAttributeLength =
        16 + requires.length + exports.length + opens.length + usesIndex.length + provides.length;
    output
        .putShort(symbolTable.addConstantUtf8(Constants.MODULE))
        .putInt(moduleAttributeLength)
        .putShort(moduleNameIndex)
        .putShort(moduleFlags)
        .putShort(moduleVersionIndex)
        .putShort(requiresCount)
        .putByteArray(requires.data, 0, requires.length)
        .putShort(exportsCount)
        .putByteArray(exports.data, 0, exports.length)
        .putShort(opensCount)
        .putByteArray(opens.data, 0, opens.length)
        .putShort(usesCount)
        .putByteArray(usesIndex.data, 0, usesIndex.length)
        .putShort(providesCount)
        .putByteArray(provides.data, 0, provides.length);
    if (packageCount > 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.MODULE_PACKAGES))
          .putInt(2 + packageIndex.length)
          .putShort(packageCount)
          .putByteArray(packageIndex.data, 0, packageIndex.length);
    }
    if (mainClassIndex > 0) {
      output
          .putShort(symbolTable.addConstantUtf8(Constants.MODULE_MAIN_CLASS))
          .putInt(2)
          .putShort(mainClassIndex);
    }
  }
}
