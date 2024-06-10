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

package org.springframework.web.bind;

import org.springframework.web.util.NestedServletException;

/**
 * 严重的绑定异常，当我们想将绑定异常视为不可恢复的异常时抛出。
 *
 * <p>扩展了ServletException，方便在任何Servlet资源（如Filter）中抛出，
 * 并扩展了NestedServletException以适当处理根本原因（因为普通的ServletException根本不暴露其根本原因）。
 * <p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ServletRequestBindingException extends NestedServletException {

	/**
	 * ServletRequestBindingException的构造函数。
	 *
	 * @param msg 详细信息
	 */
	public ServletRequestBindingException(String msg) {
		super(msg);
	}

	/**
	 * ServletRequestBindingException的构造函数。
	 *
	 * @param msg   详细信息
	 * @param cause 根本原因
	 */
	public ServletRequestBindingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
