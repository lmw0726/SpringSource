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
 * 在尝试检索 MBean 元数据时遇到异常则抛出。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 * @see MBeanProxyFactoryBean
 */
@SuppressWarnings("serial")
public class MBeanInfoRetrievalException extends JmxException {

	/**
	 * 使用指定的错误消息创建一个新的 {@code MBeanInfoRetrievalException}。
	 * @param msg 详细消息
	 */
	public MBeanInfoRetrievalException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的错误消息和根本原因创建一个新的 {@code MBeanInfoRetrievalException}。
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public MBeanInfoRetrievalException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
