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

import java.io.Serializable;

import org.springframework.lang.Nullable;

/**
 * 简单的可序列化类，用作 {@code null} 替代值，
 * 适用于不支持 {@code null} 值的缓存存储。
 *
 * @author Juergen Hoeller
 * @since 4.2.2
 * @see AbstractValueAdaptingCache
 */
public final class NullValue implements Serializable {

	/**
	 * {@code null} 替代值的标准表示，
	 * 由 {@link AbstractValueAdaptingCache#toStoreValue}/
	 * {@link AbstractValueAdaptingCache#fromStoreValue} 的默认实现所使用。
	 * @since 4.3.10
	 */
	public static final Object INSTANCE = new NullValue();

	private static final long serialVersionUID = 1L;


	private NullValue() {
	}

	private Object readResolve() {
		return INSTANCE;
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || obj == null);
	}

	@Override
	public int hashCode() {
		return NullValue.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
