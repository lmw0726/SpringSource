/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.asm;

/**
 * 工具类，暴露与Spring内部重新打包的ASM字节码库相关的常量：
 * 目前基于ASM 9.x版本加上少量补丁。
 *
 * <p>有关{@code org.springframework.asm}的更多信息，
 * 请参见<a href="package-summary.html">包级别javadocs</a>。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 */
public final class SpringAsmInfo {

	/**
	 * Spring的ASM访问者实现的ASM兼容性版本：
	 * 从Spring Framework 5.3开始，当前为{@link Opcodes#ASM10_EXPERIMENTAL}。
	 */
	public static final int ASM_VERSION = Opcodes.ASM10_EXPERIMENTAL;

}
