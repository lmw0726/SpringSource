/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.annotation;

/**
 * 用于确定应应用基于 JDK 代理（JDK proxy）的增强还是基于 AspectJ 织入（weaving）的增强的枚举。
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.scheduling.annotation.EnableAsync#mode()
 * @see org.springframework.scheduling.annotation.AsyncConfigurationSelector#selectImports
 * @see org.springframework.transaction.annotation.EnableTransactionManagement#mode()
 */
public enum AdviceMode {


	/**
	 * 基于 JDK 代理（JDK proxy）的增强。
	 */
	PROXY,

	/**
	 * 基于 AspectJ 织入（weaving）的增强。
	 */
	ASPECTJ

}
