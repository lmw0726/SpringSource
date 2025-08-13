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

package org.springframework.core;

import org.springframework.util.Assert;

/**
 * {@link InheritableThreadLocal} 子类，暴露指定的名称作为 {@link #toString()} 的结果（便于检测）。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @param <T> 值的类型
 * @see NamedThreadLocal
 */
public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {

	private final String name;


	/**
	 * 使用给定名称创建一个新的 NamedInheritableThreadLocal。
	 * @param name 该 ThreadLocal 的描述性名称
	 */
	public NamedInheritableThreadLocal(String name) {
		Assert.hasText(name, "Name must not be empty");
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

}

