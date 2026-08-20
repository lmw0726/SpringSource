/*
 * Copyright 2002-2014 the original author or authors.
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
 * 该接口定义了发送简单邮件的策略。由于需求简单，可以为多种邮件系统实现此接口。
 * 如需更丰富的功能（如 MIME 消息），请考虑使用 JavaMailSender。
 *
 * <p>便于客户端的轻松测试，因为它不依赖于 JavaMail 的
 * 基础设施类：无需模拟 JavaMail Session 或 Transport。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 * @see org.springframework.mail.javamail.JavaMailSender
 */
public interface MailSender {

	/**
	 * 发送指定的简单邮件消息。
	 * @param simpleMessage 要发送的消息
	 * @throws MailParseException 解析消息失败时抛出
	 * @throws MailAuthenticationException 认证失败时抛出
	 * @throws MailSendException 发送消息失败时抛出
	 */
	void send(SimpleMailMessage simpleMessage) throws MailException;

	/**
	 * 批量发送指定的简单邮件消息数组。
	 * @param simpleMessages 要发送的消息
	 * @throws MailParseException 解析消息失败时抛出
	 * @throws MailAuthenticationException 认证失败时抛出
	 * @throws MailSendException 发送消息失败时抛出
	 */
	void send(SimpleMailMessage... simpleMessages) throws MailException;

}
