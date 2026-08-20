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

package org.springframework.mail;

/**
 * 认证失败时抛出的异常。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class MailAuthenticationException extends MailException {


	/**
	 * MailAuthenticationException 的构造方法。
	 * @param msg 消息
	 */
	public MailAuthenticationException(String msg) {
		super(msg);
	}

	/**
	 * MailAuthenticationException 的构造方法。
	 * @param msg 详细消息
	 * @param cause 所使用邮件 API 的根本原因
	 */
	public MailAuthenticationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * MailAuthenticationException 的构造方法。
	 * @param cause 所使用邮件 API 的根本原因
	 */
	public MailAuthenticationException(Throwable cause) {
		super("Authentication failed", cause);
	}

}
