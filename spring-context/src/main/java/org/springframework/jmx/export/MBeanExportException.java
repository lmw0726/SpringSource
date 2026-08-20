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

package org.springframework.jmx.export;

import org.springframework.jmx.JmxException;

/**
 * 导出 MBean 失败时抛出的异常。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see MBeanExportOperations
 */
@SuppressWarnings("serial")
public class MBeanExportException extends JmxException {

	/**
	 * 创建一个新的 {@code MBeanExportException}，包含指定的错误消息。
	 * @param msg 详细信息
	 */
	public MBeanExportException(String msg) {
		super(msg);
	}

	/**
	 * 创建一个新的 {@code MBeanExportException}，包含指定的错误消息和根本原因。
	 * @param msg 详细信息
	 * @param cause 根本原因
	 */
	public MBeanExportException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
