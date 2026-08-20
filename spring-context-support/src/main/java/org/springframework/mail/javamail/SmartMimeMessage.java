/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.mail.javamail;

import javax.activation.FileTypeMap;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.springframework.lang.Nullable;

/**
 * 标准 JavaMail {@link MimeMessage} 的特殊子类，携带一个默认编码，
 * 用于填充消息时使用，以及一个默认的 Java Activation {@link FileTypeMap}，
 * 用于解析附件类型。
 *
 * <p>由 {@link JavaMailSenderImpl} 在指定了默认编码
 * 和/或默认 FileTypeMap 时创建。由 {@link MimeMessageHelper} 自动检测，
 * 除非被显式覆盖，否则将使用携带的编码和 FileTypeMap。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see JavaMailSenderImpl#createMimeMessage()
 * @see MimeMessageHelper#getDefaultEncoding(javax.mail.internet.MimeMessage)
 * @see MimeMessageHelper#getDefaultFileTypeMap(javax.mail.internet.MimeMessage)
 */
class SmartMimeMessage extends MimeMessage {

	@Nullable
	private final String defaultEncoding;

	@Nullable
	private final FileTypeMap defaultFileTypeMap;


	/**
	 * 创建一个新的 SmartMimeMessage。
	 * @param session 用于创建消息的 JavaMail Session
	 * @param defaultEncoding 默认编码，如果没有则为 {@code null}
	 * @param defaultFileTypeMap 默认的 FileTypeMap，如果没有则为 {@code null}
	 */
	public SmartMimeMessage(
			Session session, @Nullable String defaultEncoding, @Nullable FileTypeMap defaultFileTypeMap) {

		super(session);
		this.defaultEncoding = defaultEncoding;
		this.defaultFileTypeMap = defaultFileTypeMap;
	}


	/**
	 * 返回此消息的默认编码，如果没有则为 {@code null}。
	 */
	@Nullable
	public final String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 返回此消息的默认 FileTypeMap，如果没有则为 {@code null}。
	 */
	@Nullable
	public final FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}

}
