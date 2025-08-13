/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;

/**
 * {@link IOException} 的子类，能够正确处理根本原因，
 * 并像 NestedChecked/RuntimeException 一样暴露根本原因。
 *
 * <p>Java 6 开始，标准的 {@code IOException} 添加了对根本原因的支持，
 * 因此 Spring 最初引入 {@code NestedIOException} 以兼容 Java 6 之前的版本。
 *
 * <p>由于本类需要继承自 IOException，因此和 NestedChecked/RuntimeException
 * 之间的相似性是不可避免的。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getMessage
 * @see #printStackTrace
 * @see org.springframework.core.NestedCheckedException
 * @see org.springframework.core.NestedRuntimeException
 */
@SuppressWarnings("serial")
public class NestedIOException extends IOException {

	static {
		// 预先加载 NestedExceptionUtils 类，避免在 OSGi 环境调用 getMessage() 时发生类加载死锁问题。
		// 该问题由 Don Brown 报告；SPR-5607。
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定的详细消息构造 {@code NestedIOException}。
	 * @param msg 详细消息
	 */
	public NestedIOException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造 {@code NestedIOException}。
	 * @param msg 详细消息
	 * @param cause 嵌套异常
	 */
	public NestedIOException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息，包括嵌套异常的消息（如果有）。
	 */
	@Override
	@Nullable
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

}
