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

package org.springframework.jmx.access;

import org.springframework.jmx.JmxException;

/**
 * 当对 MBean 资源的调用因异常而失败时抛出（异常可能是反射异常或目标方法本身抛出的异常）。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 */
@SuppressWarnings("serial")
public class InvocationFailureException extends JmxException {

	/**
	 * 使用提供的错误消息创建一个新的 {@code InvocationFailureException}。
	 * @param msg 详细消息
	 */
	public InvocationFailureException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的错误消息和根本原因创建一个新的 {@code InvocationFailureException}。
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public InvocationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
