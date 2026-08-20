/*
 * Copyright 2002-2020 the original author or authors.
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
 * 枚举各种作用域代理选项。
 *
 * <p>关于作用域代理的更完整讨论，请参阅 Spring 参考文档中标题为"<em>将作用域 Bean
 * 作为依赖项</em>"的部分。
 *
 * @author Mark Fisher
 * @since 2.5
 * @see ScopeMetadata
 */
public enum ScopedProxyMode {

	/**
	 * 默认值通常等于 {@link #NO}，除非在组件扫描指令级别配置了不同的默认值。
	 */
	DEFAULT,

	/**
	 * 不创建作用域代理。
	 * <p>此代理模式在用于非单例作用域的实例时通常不适用，如果要作为依赖项使用，
	 * 应优先选择 {@link #INTERFACES} 或 {@link #TARGET_CLASS} 代理模式。
	 */
	NO,

	/**
	 * 创建一个实现目标对象类所暴露的<i>所有</i>接口的 JDK 动态代理。
	 */
	INTERFACES,

	/**
	 * 创建基于类的代理（使用 CGLIB）。
	 */
	TARGET_CLASS

}
