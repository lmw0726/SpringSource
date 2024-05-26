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

package org.springframework.web.util;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.Nullable;

import javax.servlet.ServletException;

/**
 * {@link ServletException} 的子类，以与 NestedChecked/RuntimeException 相同的方式处理根本原因的消息和堆栈跟踪。
 *
 * <p>请注意，普通的 ServletException 根本不公开其根本原因，无论是在异常消息中还是在打印的堆栈跟踪中！
 * 虽然这可能在后续的 Servlet API 变体中得到修复（甚至对于相同的 API 版本，不同供应商的行为也有所不同），
 * 但在 Servlet 2.4（Spring 3.x 所需的最低版本）中不可靠地可用，这就是为什么我们需要自己来处理的原因。
 *
 * <p>这个类与 NestedChecked/RuntimeException 类之间的相似性是不可避免的，因为这个类需要派生自 ServletException。
 *
 * @author Juergen Hoeller
 * @see #getMessage
 * @see #printStackTrace
 * @see org.springframework.core.NestedCheckedException
 * @see org.springframework.core.NestedRuntimeException
 * @since 1.2.5
 */
public class NestedServletException extends ServletException {

	/**
	 * 从 Spring 1.2 中使用 serialVersionUID 实现互操作性。
	 */
	private static final long serialVersionUID = -5292377985529381145L;

	static {
		// 急切地加载 NestedExceptionUtils 类，
		// 以避免在调用 getMessage() 时在 OSGi 上出现类加载器死锁问题。
		// Don Brown 报告的；SPR-5607。
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定的详细消息构造 {@code NestedServletException}。
	 *
	 * @param msg 详细消息
	 */
	public NestedServletException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造 {@code NestedServletException}。
	 *
	 * @param msg   详细消息
	 * @param cause 嵌套异常
	 */
	public NestedServletException(@Nullable String msg, @Nullable Throwable cause) {
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
