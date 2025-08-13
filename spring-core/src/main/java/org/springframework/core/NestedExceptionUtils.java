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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * 用于实现能够保存嵌套异常的异常类的辅助类。
 * 这是必要的，因为不同异常类型之间无法共享基类。
 *
 * <p>主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see NestedRuntimeException
 * @see NestedCheckedException
 * @see NestedIOException
 * @see org.springframework.web.util.NestedServletException
 */
public abstract class NestedExceptionUtils {

	/**
	 * 构建给定基础消息和根本原因的完整消息。
	 * @param message 基础消息
	 * @param cause 根本原因
	 * @return 完整的异常消息
	 */
	@Nullable
	public static String buildMessage(@Nullable String message, @Nullable Throwable cause) {
		if (cause == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(64);
		if (message != null) {
			sb.append(message).append("; ");
		}
		sb.append("nested exception is ").append(cause);
		return sb.toString();
	}

	/**
	 * 获取给定异常的最深层次原因（根本原因），如果有的话。
	 * @param original 要检查的原始异常
	 * @return 最深层次的异常，若无则返回 {@code null}
	 * @since 4.3.9
	 */
	@Nullable
	public static Throwable getRootCause(@Nullable Throwable original) {
		if (original == null) {
			return null;
		}
		Throwable rootCause = null;
		Throwable cause = original.getCause();
		while (cause != null && cause != rootCause) {
			rootCause = cause;
			cause = cause.getCause();
		}
		return rootCause;
	}

	/**
	 * 获取给定异常的最具体原因，也就是最深层的原因（根本原因）或异常本身。
	 * <p>与 {@link #getRootCause} 不同的是，如果没有根本原因，则返回原始异常。
	 * @param original 要检查的原始异常
	 * @return 最具体的原因（永不为 {@code null}）
	 * @since 4.3.9
	 */
	public static Throwable getMostSpecificCause(Throwable original) {
		Throwable rootCause = getRootCause(original);
		return (rootCause != null ? rootCause : original);
	}

}
