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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * Interface defining a generic contract for attaching and accessing metadata
 * to/from arbitrary objects.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
public interface AttributeAccessor {

	/**
	 * 将 {@code name} 定义的属性设置为提供的 {@code value}。
	 * <p> 如果 {@code value} 为 {@code null}，则属性将被 {@link #removeAttribute}移除。
	 * <p> 通常，用户应注意通过使用完全限定的名称 (也许使用类或包名称作为前缀) 来防止与其他元数据属性重叠。
	 *
	 * @param name  唯一属性键
	 * @param value 要附加的属性值
	 */
	void setAttribute(String name, @Nullable Object value);

	/**
	 * 获取由 {@code name} 标识的属性的值。
	 * <p> 如果属性不存在，则返回 {@code null}。
	 *
	 * @param name 唯一属性键
	 * @return 属性的当前值 (如果有)
	 */
	@Nullable
	Object getAttribute(String name);

	/**
	 * 如有必要，为 {@code name} 属性标识计算新值，并在这个{@code AttributeAccessor}中通过 {@linkplain #setAttribute }设置新值。
	 * <p> 如果此 {@code AttributeAccessor} 中已经存在由 {@code name} 标识的属性的值，则将在不应用提供的计算函数的情况下返回现有值。
	 * <p> 此方法的默认实现不是线程安全的，但是可以被该接口的具体实现覆盖。
	 *
	 * @param <T>             属性值的类型
	 * @param name            唯一属性键
	 * @param computeFunction 为属性名称计算新值的函数; 该函数不得返回 {@code null} 值
	 * @return 命名属性的现有值或新计算的值
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 * @since 5.3.3
	 */
	@SuppressWarnings("unchecked")
	default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		//获取属性值
		Object value = getAttribute(name);
		if (value == null) {
			//如果值为空，则应用计算函数，将name转为value
			value = computeFunction.apply(name);
			Assert.state(value != null,
					() -> String.format("Compute function must not return null for attribute named '%s'", name));
			//设置属性值
			setAttribute(name, value);
		}
		//返回属性值
		return (T) value;
	}

	/**
	 * 删除 {@code name} 标识的属性并返回其值。
	 * <p> 如果在 {@code name} 下未找到任何属性，则返回 {@code null}。
	 *
	 * @param name 唯一属性键
	 * @return 属性的最后一个值 (如果有)
	 */
	@Nullable
	Object removeAttribute(String name);

	/**
	 * 如果存在 {@code name} 标识的属性，则返回 {@code true}。
	 * <p> 否则返回 {@code false}。
	 *
	 * @param name 唯一属性键
	 */
	boolean hasAttribute(String name);

	/**
	 * 返回所有属性的名称。
	 */
	String[] attributeNames();

}
