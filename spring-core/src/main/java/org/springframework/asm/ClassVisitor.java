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
 * 用于访问 Java 类的访问者。该类的方法调用必须遵循以下顺序：
 * {@code visit} [ {@code visitSource} ] [ {@code visitModule} ] [ {@code visitNestHost} ] 
 * [ {@code visitOuterClass} ] ( {@code visitAnnotation} | {@code visitTypeAnnotation} | 
 * {@code visitAttribute} )* ( {@code visitNestMember} | [ {@code visitPermittedSubclass} ] | 
 * {@code visitInnerClass} | {@code visitRecordComponent} | {@code visitField} | 
 * {@code visitMethod} )* {@code visitEnd}.
 *
 * @author Eric Bruneton
 */
public abstract class ClassVisitor {

  /**
   * 该访问者实现的 ASM API 版本。
   * 该字段的值必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 常量之一。
   */
  protected final int api;

  /** 该访问者需要将方法调用委托给的另一个类访问者，可能为 {@literal null}。 */
  protected ClassVisitor cv;

  /**
   * 构造一个新的 {@link ClassVisitor}。
   *
   * @param api 该访问者实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 常量之一。
   */
  protected ClassVisitor(final int api) {
    this(api, null);
  }

  /**
   * 构造一个新的 {@link ClassVisitor}。
   *
   * @param api 该访问者实现的 ASM API 版本。必须是 {@link Opcodes} 中的 {@code ASM}<i>x</i> 常量之一。
   * @param classVisitor 该访问者需要将方法调用委托给的另一个类访问者，可能为 null。
   */
  protected ClassVisitor(final int api, final ClassVisitor classVisitor) {
    if (api != Opcodes.ASM9
        && api != Opcodes.ASM8
        && api != Opcodes.ASM7
        && api != Opcodes.ASM6
        && api != Opcodes.ASM5
        && api != Opcodes.ASM4
        && api != Opcodes.ASM10_EXPERIMENTAL) {
      throw new IllegalArgumentException("Unsupported api " + api);
    }
    // SPRING PATCH: 对 ASM 实验版本不进行预览模式检查
    this.api = api;
    this.cv = classVisitor;
  }

  /**
   * 访问类的头部信息。
   *
   * @param version 类版本号。次版本号存储在高 16 位，主版本号存储在低 16 位。
   * @param access 类的访问标志（见 {@link Opcodes}）。此参数还可指示类是否为已弃用 {@link Opcodes#ACC_DEPRECATED} 或 record {@link Opcodes#ACC_RECORD}。
   * @param name 类的内部名称（见 {@link Type#getInternalName()}）。
   * @param signature 类的签名。如果类不是泛型类，且没有继承或实现泛型类或接口，则可能为 {@literal null}。
   * @param superName 超类的内部名称（见 {@link Type#getInternalName()}）。对于接口，其超类为 {@link Object}。
   *                  可能为 {@literal null}，但仅限 {@link Object} 类。
   * @param interfaces 类实现的接口的内部名称（见 {@link Type#getInternalName()}）。可能为 {@literal null}。
   */
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    if (api < Opcodes.ASM8 && (access & Opcodes.ACC_RECORD) != 0) {
      throw new UnsupportedOperationException("Records requires ASM8");
    }
    if (cv != null) {
      cv.visit(version, access, name, signature, superName, interfaces);
    }
  }

  /**
   * 访问类的源信息。
   *
   * @param source 类编译来源文件的名称，可能为 {@literal null}。
   * @param debug 额外的调试信息，用于计算源代码与类中编译元素的对应关系，可能为 {@literal null}。
   */
  public void visitSource(final String source, final String debug) {
    if (cv != null) {
      cv.visitSource(source, debug);
    }
  }

  /**
   * 访问与类对应的模块信息。
   *
   * @param name 模块的完全限定名（使用点号分隔）。
   * @param access 模块的访问标志，可包含 {@code ACC_OPEN}、{@code ACC_SYNTHETIC} 和 {@code ACC_MANDATED}。
   * @param version 模块版本，可能为 {@literal null}。
   * @return 用于访问模块值的访问者，如果不需要访问模块则返回 {@literal null}。
   */
  public ModuleVisitor visitModule(final String name, final int access, final String version) {
    if (api < Opcodes.ASM6) {
      throw new UnsupportedOperationException("Module requires ASM6");
    }
    if (cv != null) {
      return cv.visitModule(name, access, version);
    }
    return null;
  }

  /**
   * 访问类的 nest 主类（nest host）。nest 是一组同一包内的类，这些类可以共享它们的私有成员。
   * 其中一个类称为主类（host），它会列出 nest 中的其他成员类，这些成员类反过来也应当链接回它们 nest 的主类。
   * 此方法必须且只能调用一次，并且仅当被访问的类是某个 nest 的非主类成员时才可调用。
   * 类默认是它自己 nest 的主类，因此如果将被访问类的名称作为参数调用此方法是无效的。
   *
   * @param nestHost nest 主类的内部名称。
   */
  public void visitNestHost(final String nestHost) {
    if (api < Opcodes.ASM7) {
      throw new UnsupportedOperationException("NestHost requires ASM7");
    }
    if (cv != null) {
      cv.visitNestHost(nestHost);
    }
  }

  /**
   * 访问类的外围类（enclosing class）。仅当该类确实有外围类时才可调用此方法。
   *
   * @param owner 外围类的内部名称。
   * @param name 包含该类的方法名，如果该类不是定义在外围类的方法中则为 {@literal null}。
   * @param descriptor 包含该类的方法的描述符，如果该类不是定义在外围类的方法中则为 {@literal null}。
   */
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    if (cv != null) {
      cv.visitOuterClass(owner, name, descriptor);
    }
  }

  /**
   * 访问类的注解。
   *
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见则为 {@literal true}。
   * @return 用于访问注解值的访问器，如果对此注解不感兴趣则返回 {@literal null}。
   */
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (cv != null) {
      return cv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * 访问类签名（signature）中类型上的注解。
   *
   * @param typeRef 被注解的类型引用。该类型引用的种类必须是
   *     {@link TypeReference#CLASS_TYPE_PARAMETER}、{@link TypeReference#CLASS_TYPE_PARAMETER_BOUND}
   *     或 {@link TypeReference#CLASS_EXTENDS}。参见 {@link TypeReference}。
   * @param typePath 指向被注解的类型参数、通配符边界、数组元素类型或静态内部类型的路径。
   *     如果注解的目标是整个 'typeRef'，则可以为 {@literal null}。
   * @param descriptor 注解类的类描述符。
   * @param visible 如果注解在运行时可见则为 {@literal true}。
   * @return 用于访问注解值的访问器，如果对此注解不感兴趣则返回 {@literal null}。
   */
  public AnnotationVisitor visitTypeAnnotation(
      final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (api < Opcodes.ASM5) {
      throw new UnsupportedOperationException("TypeAnnotation requires ASM5");
    }
    if (cv != null) {
      return cv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * 访问类的一个非标准属性。
   *
   * @param attribute 一个属性。
   */
  public void visitAttribute(final Attribute attribute) {
    if (cv != null) {
      cv.visitAttribute(attribute);
    }
  }

  /**
   * 访问 nest 的一个成员类。nest 是一组同一包内的类，这些类可以共享它们的私有成员。
   * 其中一个类称为主类（host），它会列出 nest 中的其他成员类，这些成员类反过来也应当链接回它们 nest 的主类。
   * 此方法必须且只能在被访问类是某个 nest 的主类时调用。
   * 主类默认是其自身 nest 的成员，因此如果将被访问类的名称作为参数调用此方法是无效的。
   *
   * @param nestMember nest 成员类的内部名称。
   */
  public void visitNestMember(final String nestMember) {
    if (api < Opcodes.ASM7) {
      throw new UnsupportedOperationException("NestMember requires ASM7");
    }
    if (cv != null) {
      cv.visitNestMember(nestMember);
    }
  }

  /**
   * 访问一个被允许的子类（permitted subclass）。
   * 被允许的子类是当前类的允许继承类之一。
   *
   * @param permittedSubclass 被允许的子类的内部名称。
   */
  public void visitPermittedSubclass(final String permittedSubclass) {
    if (api < Opcodes.ASM9) {
      throw new UnsupportedOperationException("PermittedSubclasses requires ASM9");
    }
    if (cv != null) {
      cv.visitPermittedSubclass(permittedSubclass);
    }
  }

  /**
   * 访问一个内部类的信息。这个内部类不一定是正在被访问的类的成员。
   *
   * @param name 内部类的内部名称（参见 {@link Type#getInternalName()}）。
   * @param outerName 内部类所属外部类的内部名称（参见 {@link Type#getInternalName()}）。对于非成员类可为 {@literal null}。
   * @param innerName 内部类在其封闭类中的（简单）名称。对于匿名内部类可为 {@literal null}。
   * @param access 内部类在封闭类中声明时的访问标志。
   */
  public void visitInnerClass(
      final String name, final String outerName, final String innerName, final int access) {
    if (cv != null) {
      cv.visitInnerClass(name, outerName, innerName, access);
    }
  }

  /**
   * 访问类的一个记录组件（record component）。
   *
   * @param name 记录组件的名称。
   * @param descriptor 记录组件的描述符（参见 {@link Type}）。
   * @param signature 记录组件的签名。如果记录组件类型未使用泛型，则可为 {@literal null}。
   * @return 一个用于访问该记录组件的注解和属性的访问器，或者 {@literal null}（如果该类访问器不关心这些内容）。
   */
  public RecordComponentVisitor visitRecordComponent(
      final String name, final String descriptor, final String signature) {
    if (api < Opcodes.ASM8) {
      throw new UnsupportedOperationException("Record requires ASM8");
    }
    if (cv != null) {
      return cv.visitRecordComponent(name, descriptor, signature);
    }
    return null;
  }

  /**
   * 访问类的一个字段。
   *
   * @param access 字段的访问标志（参见 {@link Opcodes}）。此参数还指示字段是否为 synthetic 和/或 deprecated。
   * @param name 字段名称。
   * @param descriptor 字段的描述符（参见 {@link Type}）。
   * @param signature 字段的签名。如果字段类型未使用泛型，则可为 {@literal null}。
   * @param value 字段的初始值。可为 {@literal null}（如果字段没有初始值）。该值必须是 {@link Integer}、{@link Float}、{@link Long}、{@link Double} 或 {@link String}（分别对应 {@code int}、{@code float}、{@code long}、{@code String} 字段）。
   *              <i>该参数仅用于静态字段</i>。非静态字段必须通过构造方法或方法中的字节码指令初始化。
   * @return 一个用于访问字段注解和属性的访问器，或者 {@literal null}（如果该类访问器不关心这些内容）。
   */
  public FieldVisitor visitField(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final Object value) {
    if (cv != null) {
      return cv.visitField(access, name, descriptor, signature, value);
    }
    return null;
  }

  /**
   * 访问类的一个方法。此方法<i>必须</i>每次调用都返回一个新的 {@link MethodVisitor} 实例（或 {@literal null}），
   * 即不应返回之前返回过的访问器。
   *
   * @param access 方法的访问标志（参见 {@link Opcodes}）。此参数还指示方法是否为 synthetic 和/或 deprecated。
   * @param name 方法名称。
   * @param descriptor 方法的描述符（参见 {@link Type}）。
   * @param signature 方法的签名。如果方法参数、返回值类型和异常未使用泛型，则可为 {@literal null}。
   * @param exceptions 方法的异常类的内部名称数组（参见 {@link Type#getInternalName()}）。可为 {@literal null}。
   * @return 一个用于访问该方法字节码的对象，或者 {@literal null}（如果该类访问器不关心方法代码）。
   */
  public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions) {
    if (cv != null) {
      return cv.visitMethod(access, name, descriptor, signature, exceptions);
    }
    return null;
  }

  /**
   * 访问类的结束。此方法是最后一个被调用的方法，用于通知访问器类的所有字段和方法都已访问完毕。
   */
  public void visitEnd() {
    if (cv != null) {
      cv.visitEnd();
    }
  }
}
