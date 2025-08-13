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
 * 定义额外的 JVM 操作码、访问标志及常量，这些不属于 ASM 公共 API 范围。  
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html">JVMS 6</a>
 * @author Eric Bruneton
 */
final class Constants {

  // ClassFile 属性名，按照其在  
  // https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.7-300 中定义的顺序。

  static final String CONSTANT_VALUE = "ConstantValue";
  static final String CODE = "Code";
  static final String STACK_MAP_TABLE = "StackMapTable";
  static final String EXCEPTIONS = "Exceptions";
  static final String INNER_CLASSES = "InnerClasses";
  static final String ENCLOSING_METHOD = "EnclosingMethod";
  static final String SYNTHETIC = "Synthetic";
  static final String SIGNATURE = "Signature";
  static final String SOURCE_FILE = "SourceFile";
  static final String SOURCE_DEBUG_EXTENSION = "SourceDebugExtension";
  static final String LINE_NUMBER_TABLE = "LineNumberTable";
  static final String LOCAL_VARIABLE_TABLE = "LocalVariableTable";
  static final String LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable";
  static final String DEPRECATED = "Deprecated";
  static final String RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
  static final String RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";
  static final String RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations";
  static final String RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations";
  static final String RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations";
  static final String RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations";
  static final String ANNOTATION_DEFAULT = "AnnotationDefault";
  static final String BOOTSTRAP_METHODS = "BootstrapMethods";
  static final String METHOD_PARAMETERS = "MethodParameters";
  static final String MODULE = "Module";
  static final String MODULE_PACKAGES = "ModulePackages";
  static final String MODULE_MAIN_CLASS = "ModuleMainClass";
  static final String NEST_HOST = "NestHost";
  static final String NEST_MEMBERS = "NestMembers";
  static final String PERMITTED_SUBCLASSES = "PermittedSubclasses";
  static final String RECORD = "Record";

  // ASM 特定的访问标志。
  // 警告：最低的16位不能使用，以避免与标准访问标志冲突，
  // 并确保这些标志在写入 class 文件时会被自动过滤（因为访问标志仅用16位存储）。

  static final int ACC_CONSTRUCTOR = 0x40000; // 方法访问标志。  

  // ASM 特定的栈映射帧类型，用于 {@link ClassVisitor#visitFrame}。

  /**
   * 插入在已有帧之间的帧。  
   * 这种内部栈映射帧类型（除 {@link Opcodes} 中声明的类型外）  
   * 仅在可以根据前一个已有帧及该帧与插入帧之间的指令计算出帧内容，  
   * 且无需类型层次结构信息时使用。  
   * 这种帧只在扩展 ASM 特定指令时，在方法中插入无条件跳转时使用。  
   * 请与 Opcodes.java 保持同步。  
   */
  static final int F_INSERT = 256;

  // 不属于 ASM 公共 API 的 JVM 操作码值。
  // 详见 https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html。

  static final int LDC_W = 19;
  static final int LDC2_W = 20;
  static final int ILOAD_0 = 26;
  static final int ILOAD_1 = 27;
  static final int ILOAD_2 = 28;
  static final int ILOAD_3 = 29;
  static final int LLOAD_0 = 30;
  static final int LLOAD_1 = 31;
  static final int LLOAD_2 = 32;
  static final int LLOAD_3 = 33;
  static final int FLOAD_0 = 34;
  static final int FLOAD_1 = 35;
  static final int FLOAD_2 = 36;
  static final int FLOAD_3 = 37;
  static final int DLOAD_0 = 38;
  static final int DLOAD_1 = 39;
  static final int DLOAD_2 = 40;
  static final int DLOAD_3 = 41;
  static final int ALOAD_0 = 42;
  static final int ALOAD_1 = 43;
  static final int ALOAD_2 = 44;
  static final int ALOAD_3 = 45;
  static final int ISTORE_0 = 59;
  static final int ISTORE_1 = 60;
  static final int ISTORE_2 = 61;
  static final int ISTORE_3 = 62;
  static final int LSTORE_0 = 63;
  static final int LSTORE_1 = 64;
  static final int LSTORE_2 = 65;
  static final int LSTORE_3 = 66;
  static final int FSTORE_0 = 67;
  static final int FSTORE_1 = 68;
  static final int FSTORE_2 = 69;
  static final int FSTORE_3 = 70;
  static final int DSTORE_0 = 71;
  static final int DSTORE_1 = 72;
  static final int DSTORE_2 = 73;
  static final int DSTORE_3 = 74;
  static final int ASTORE_0 = 75;
  static final int ASTORE_1 = 76;
  static final int ASTORE_2 = 77;
  static final int ASTORE_3 = 78;
  static final int WIDE = 196;
  static final int GOTO_W = 200;
  static final int JSR_W = 201;

  // 用于在普通跳转指令和宽跳转指令之间转换的常量。

  // GOTO_W 和 JSR_W 指令与 GOTO 和 JUMP 指令的操作码差值。
  static final int WIDE_JUMP_OPCODE_DELTA = GOTO_W - Opcodes.GOTO;

  // 用于将 JVM 操作码转换为对应的 ASM 特定操作码，反之亦然的常量。

  // ASM_IFEQ、...、ASM_IF_ACMPNE、ASM_GOTO 和 ASM_JSR 操作码与
  // IFEQ、...、IF_ACMPNE、GOTO 和 JSR 的操作码差值。
  static final int ASM_OPCODE_DELTA = 49;

  // ASM_IFNULL 和 ASM_IFNONNULL 操作码与 IFNULL 和 IFNONNULL 的操作码差值。
  static final int ASM_IFNULL_OPCODE_DELTA = 20;

  // ASM 特定的操作码，用于表示长距离向前跳转指令。

  static final int ASM_IFEQ = Opcodes.IFEQ + ASM_OPCODE_DELTA;
  static final int ASM_IFNE = Opcodes.IFNE + ASM_OPCODE_DELTA;
  static final int ASM_IFLT = Opcodes.IFLT + ASM_OPCODE_DELTA;
  static final int ASM_IFGE = Opcodes.IFGE + ASM_OPCODE_DELTA;
  static final int ASM_IFGT = Opcodes.IFGT + ASM_OPCODE_DELTA;
  static final int ASM_IFLE = Opcodes.IFLE + ASM_OPCODE_DELTA;
  static final int ASM_IF_ICMPEQ = Opcodes.IF_ICMPEQ + ASM_OPCODE_DELTA;
  static final int ASM_IF_ICMPNE = Opcodes.IF_ICMPNE + ASM_OPCODE_DELTA;
  static final int ASM_IF_ICMPLT = Opcodes.IF_ICMPLT + ASM_OPCODE_DELTA;
  static final int ASM_IF_ICMPGE = Opcodes.IF_ICMPGE + ASM_OPCODE_DELTA;
  static final int ASM_IF_ICMPGT = Opcodes.IF_ICMPGT + ASM_OPCODE_DELTA;
  static final int ASM_IF_ICMPLE = Opcodes.IF_ICMPLE + ASM_OPCODE_DELTA;
  static final int ASM_IF_ACMPEQ = Opcodes.IF_ACMPEQ + ASM_OPCODE_DELTA;
  static final int ASM_IF_ACMPNE = Opcodes.IF_ACMPNE + ASM_OPCODE_DELTA;
  static final int ASM_GOTO = Opcodes.GOTO + ASM_OPCODE_DELTA;
  static final int ASM_JSR = Opcodes.JSR + ASM_OPCODE_DELTA;
  static final int ASM_IFNULL = Opcodes.IFNULL + ASM_IFNULL_OPCODE_DELTA;
  static final int ASM_IFNONNULL = Opcodes.IFNONNULL + ASM_IFNULL_OPCODE_DELTA;
  static final int ASM_GOTO_W = 220;

  private Constants() {}
}
