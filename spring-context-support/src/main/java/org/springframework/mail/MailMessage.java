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

package org.springframework.mail;

import java.util.Date;

/**
 * 这是邮件消息的通用接口，允许用户设置组装邮件消息所需的关键值，
 * 而无需了解底层消息是简单的文本消息还是更复杂的 MIME 消息。
 *
 * <p>SimpleMailMessage 和 MimeMessageHelper 都实现了此接口，
 * 使得填充消息的代码能够通过通用接口与简单消息或 MIME 消息进行交互。
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see SimpleMailMessage
 * @see org.springframework.mail.javamail.MimeMessageHelper
 */
public interface MailMessage {

	void setFrom(String from) throws MailParseException;

	void setReplyTo(String replyTo) throws MailParseException;

	void setTo(String to) throws MailParseException;

	void setTo(String... to) throws MailParseException;

	void setCc(String cc) throws MailParseException;

	void setCc(String... cc) throws MailParseException;

	void setBcc(String bcc) throws MailParseException;

	void setBcc(String... bcc) throws MailParseException;

	void setSentDate(Date sentDate) throws MailParseException;

	void setSubject(String subject) throws MailParseException;

	void setText(String text) throws MailParseException;

}
