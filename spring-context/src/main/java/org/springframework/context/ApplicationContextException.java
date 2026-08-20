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

package org.springframework.context;

import org.springframework.beans.FatalBeanException;

/**
 * 在应用上下文初始化期间抛出的异常。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class ApplicationContextException extends FatalBeanException {

	/**
	 * 创建一个新的 {@code ApplicationContextException}，
	 * 指定详细消息，不包含根因。
	 * @param msg 详细消息
	 */
	public ApplicationContextException(String msg) {
		super(msg);
	}

	/**
	 * 创建一个新的 {@code ApplicationContextException}，
	 * 指定详细消息和给定的根因。
	 * @param msg 详细消息
	 * @param cause 根因
	 */
	public ApplicationContextException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
