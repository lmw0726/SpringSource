/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop;

import org.springframework.core.NestedRuntimeException;

/**
 * 当 AOP 调用因配置错误或意外的运行时问题而失败时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AopInvocationException extends NestedRuntimeException {

	/**
	 * AopInvocationException 的构造函数。
	 * @param msg 详细消息
	 */
	public AopInvocationException(String msg) {
		super(msg);
	}

	/**
	 * AopInvocationException 的构造函数。
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public AopInvocationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
