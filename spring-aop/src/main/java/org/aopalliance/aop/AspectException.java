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

package org.aopalliance.aop;

/**
 * 所有 AOP 基础设施异常的超类。
 * 非受检异常，因为此类异常是致命的，
 * 不应强制最终用户代码捕获它们。
 *
 * @author Rod Johnson
 * @author Bob Lee
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class AspectException extends RuntimeException {

	/**
	 * AspectException 构造方法。
	 * @param message 异常消息
	 */
	public AspectException(String message) {
		super(message);
	}

	/**
	 * AspectException 构造方法。
	 * @param message 异常消息
	 * @param cause 根本原因（如果有）
	 */
	public AspectException(String message, Throwable cause) {
		super(message, cause);
	}

}
