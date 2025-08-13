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
 * 方法的控制流图中的一条边。该图的每个节点是一个基本块，  
 * 用对应其第一条指令的 Label 表示。每条边连接一个节点到另一个节点，  
 * 即从一个基本块到另一个基本块（分别称为前驱块和后继块）。  
 * 一条边对应于跳转或返回指令，或异常处理器。  
 *
 * @see Label
 * @author Eric Bruneton
 */
final class Edge {

  /**
   * 控制流图中对应跳转或返回指令的边。仅在 {@link ClassWriter#COMPUTE_FRAMES} 中使用。  
   */
  static final int JUMP = 0;

  /**
   * 控制流图中对应异常处理器的边。仅在 {@link ClassWriter#COMPUTE_MAXS} 中使用。  
   */
  static final int EXCEPTION = 0x7FFFFFFF;

  /**
   * 关于此控制流图边的信息。  
   *
   * <ul>
   *   <li>如果使用 {@link ClassWriter#COMPUTE_MAXS}，该字段包含栈大小增量（对应跳转指令的边），  
   *       或者 EXCEPTION 值（对应异常处理器的边）。栈大小增量为跳转指令执行后栈大小减去  
   *       前驱基本块（包含跳转指令的块）开始时的栈大小。  
   *   <li>如果使用 {@link ClassWriter#COMPUTE_FRAMES}，该字段包含 JUMP 值（对应跳转指令的边），  
   *       或异常类型在 {@link ClassWriter} 类型表中的索引（对应异常处理器的边）。  
   * </ul>
   */
  final int info;

  /** 此控制流图边所指向的后继基本块。 */
  final Label successor;

  /**
   * 基本块的出边链表中的下一条边。见 {@link Label#outgoingEdges}。  
   */
  Edge nextEdge;

  /**
   * 构造一个新的 Edge 实例。  
   *
   * @param info 参见 {@link #info}。  
   * @param successor 参见 {@link #successor}。  
   * @param nextEdge 参见 {@link #nextEdge}。  
   */
  Edge(final int info, final Label successor, final Edge nextEdge) {
    this.info = info;
    this.successor = successor;
    this.nextEdge = nextEdge;
  }
}
