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

package org.springframework.jmx;

/**
 * 当无法定位 {@code MBeanServer} 实例，或发现多个实例时抛出的异常。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jmx.support.JmxUtils#locateMBeanServer
 */
@SuppressWarnings("serial")
public class MBeanServerNotFoundException extends JmxException {

	/**
	 * 使用提供的错误消息创建新的 {@code MBeanServerNotFoundException}。
	 * @param msg 错误消息
	 */
	public MBeanServerNotFoundException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的错误消息和根本原因创建新的 {@code MBeanServerNotFoundException}。
	 * @param msg 错误消息
	 * @param cause 根本原因
	 */
	public MBeanServerNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
