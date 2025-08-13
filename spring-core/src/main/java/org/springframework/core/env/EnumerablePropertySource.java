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

package org.springframework.core.env;

import org.springframework.util.ObjectUtils;

/**
 * 一个能够查询其底层源对象以枚举所有可能的属性名/值对的 {@link PropertySource} 实现。
 * 公开 {@link #getPropertyNames()} 方法，允许调用者在不必访问底层源对象的情况下内省可用属性。
 * 这也有助于更高效地实现 {@link #containsProperty(String)}，它可以调用 {@link #getPropertyNames()}
 * 并遍历返回的数组，而不是尝试调用可能更昂贵的 {@link #getProperty(String)}。
 * 实现可以考虑缓存 {@link #getPropertyNames()} 的结果以充分利用这种性能优势。
 *
 * <p>大多数框架提供的 {@code PropertySource} 实现都是可枚举的；一个反例是 {@code JndiPropertySource}，
 * 由于 JNDI 的特性，无法在任何给定时间确定所有可能的属性名；相反，只能尝试访问一个属性
 * （通过 {@link #getProperty(String)}）来评估它是否存在。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @param <T> 源类型
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

	/**
	 * 使用给定的名称和源对象创建一个新的 {@code EnumerablePropertySource}。
	 * @param name 关联的名称
	 * @param source 源对象
	 */
	public EnumerablePropertySource(String name, T source) {
		super(name, source);
	}

	/**
	 * 使用给定的名称创建一个新的 {@code EnumerablePropertySource}，并以新的
	 * {@code Object} 实例作为底层源。
	 * @param name 关联的名称
	 */
	protected EnumerablePropertySource(String name) {
		super(name);
	}


	/**
	 * 返回此 {@code PropertySource} 是否包含具有给定名称的属性。
	 * <p>此实现检查给定名称是否存在于 {@link #getPropertyNames()} 数组中。
	 * @param name 要查找的属性名称
	 */
	@Override
	public boolean containsProperty(String name) {
		return ObjectUtils.containsElement(getPropertyNames(), name);
	}

	/**
	 * 返回 {@linkplain #getSource() 源} 对象包含的所有属性的名称（永远不会是 {@code null}）。
	 */
	public abstract String[] getPropertyNames();

}
