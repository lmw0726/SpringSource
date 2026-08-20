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

/**
 * 当无法注册 MBean 时抛出的异常，
 * 例如由于命名冲突导致。
 *
 * @author Rob Harrop
 * @since 2.0
 */
@SuppressWarnings("serial")
public class UnableToRegisterMBeanException extends MBeanExportException {

	/**
	 * 使用指定的错误消息创建一个新的 {@code UnableToRegisterMBeanException}。
	 * @param msg 详细错误信息
	 */
	public UnableToRegisterMBeanException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的错误消息和根本原因创建一个新的 {@code UnableToRegisterMBeanException}。
	 * @param msg 详细错误信息
	 * @param cause 根本原因
	 */
	public UnableToRegisterMBeanException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
