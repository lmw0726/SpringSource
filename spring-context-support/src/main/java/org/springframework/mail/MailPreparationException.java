/*
 * Copyright 2002-2016 the original author or authors.
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
 * 当邮件无法正确准备时，由用户代码抛出的异常，
 * 例如无法为邮件文本渲染 FreeMarker 模板时。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.ui.freemarker.FreeMarkerTemplateUtils#processTemplateIntoString
 */
@SuppressWarnings("serial")
public class MailPreparationException extends MailException {


	/**
	 * MailPreparationException 的构造函数。
	 * @param msg 详细消息
	 */
	public MailPreparationException(String msg) {
		super(msg);
	}

	/**
	 * MailPreparationException 的构造函数。
	 * @param msg 详细消息
	 * @param cause 来自所使用邮件 API 的根本原因
	 */
	public MailPreparationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public MailPreparationException(Throwable cause) {
		super("Could not prepare mail", cause);
	}

}
