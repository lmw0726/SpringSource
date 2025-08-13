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

package org.springframework.util.function;

import org.springframework.lang.Nullable;

import java.util.function.Supplier;

/**
 * 用于处理{@link java.util.function.Supplier}的便捷工具类。
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see SingletonSupplier
 */
public abstract class SupplierUtils {

	/**
	 * 解析给定的{@code Supplier}，获取其结果；如果supplier为null则立即返回null。
	 * @param supplier 要解析的supplier
	 * @return supplier的结果，如果没有则返回{@code null}
	 */
	@Nullable
	public static <T> T resolve(@Nullable Supplier<T> supplier) {
		return (supplier != null ? supplier.get() : null);
	}

}
