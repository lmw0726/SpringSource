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

import org.springframework.core.NestedRuntimeException;

/**
 * 用于在JMX错误时抛出的通用基础异常。
 * 由于JMX故障通常是致命的，因此该异常为非受检异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class JmxException extends NestedRuntimeException {

	/**
	 * JmxException的构造方法。
	 * @param msg 详细信息
	 */
	public JmxException(String msg) {
		super(msg);
	}

	/**
	 * JmxException的构造方法。
	 * @param msg 详细信息
	 * @param cause 根本原因（通常是原始的JMX API异常）
	 */
	public JmxException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
