/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.cache.support;

import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.cache.Cache.ValueWrapper} 的简单实现，
 * 在构造时保存传入的值，并通过 {@link #get()} 返回该值。
 *
 * @author Costin Leau
 * @since 3.1
 */
public class SimpleValueWrapper implements ValueWrapper {

	@Nullable
	private final Object value;


	/**
	 * 创建一个新的 SimpleValueWrapper 实例，用于暴露给定的值。
	 * @param value 要暴露的值（可以为 {@code null}）
	 */
	public SimpleValueWrapper(@Nullable Object value) {
		this.value = value;
	}


	/**
	 * 直接返回构造时传入的值。
	 */
	@Override
	@Nullable
	public Object get() {
		return this.value;
	}

}
