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

package org.springframework.mail.javamail;

import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;

/**
 * 扩展的 {@link org.springframework.mail.MailSender} 接口，用于 JavaMail，
 * 支持将 MIME 消息作为直接参数传递，也支持通过预处理回调进行传递。
 * 通常与 {@link MimeMessageHelper} 类结合使用，以便于创建 JavaMail
 * {@link MimeMessage MimeMessages}，包括附件等功能。
 *
 * <p>如果客户端需要超出 {@link org.springframework.mail.SimpleMailMessage}
 * 功能的邮件功能，应通过此接口与邮件发送器进行通信。
 * 生产环境的实现是 {@link JavaMailSenderImpl}；用于测试时，
 * 可以基于此接口创建模拟对象。客户端通常通过依赖注入获取
 * JavaMailSender 引用。
 *
 * <p>推荐使用此接口的方式是 {@link MimeMessagePreparator}
 * 机制，可以配合 {@link MimeMessageHelper} 来填充消息内容。
 * 参见 {@link MimeMessageHelper MimeMessageHelper 的 javadoc} 了解示例。
 *
 * <p>整个 JavaMail {@link javax.mail.Session} 管理被
 * JavaMailSender 抽象化了。客户端代码不应以任何方式处理 Session，
 * 而应将整个 JavaMail 配置和资源处理交由
 * JavaMailSender 实现来完成。这也增强了可测试性。
 *
 * <p>JavaMailSender 客户端不像普通的
 * {@link org.springframework.mail.MailSender} 客户端那样容易测试，
 * 但与传统 JavaMail 代码相比仍然比较直观：只需让
 * {@link #createMimeMessage()} 返回一个使用
 * {@code Session.getInstance(new Properties())} 调用创建的普通
 * {@link MimeMessage}，然后在各种 {@code send} 方法的
 * 模拟实现中检查传入的消息即可。
 *
 * @author Juergen Hoeller
 * @since 07.10.2003
 * @see javax.mail.internet.MimeMessage
 * @see javax.mail.Session
 * @see JavaMailSenderImpl
 * @see MimeMessagePreparator
 * @see MimeMessageHelper
 */
public interface JavaMailSender extends MailSender {

	/**
	 * 为发送器底层的 JavaMail Session 创建一个新的 JavaMail MimeMessage。
	 * 需要调用此方法来创建 MimeMessage 实例，客户端可以对其进行预处理
	 * 并传递给 send(MimeMessage)。
	 * @return 新的 MimeMessage 实例
	 * @see #send(MimeMessage)
	 * @see #send(MimeMessage[])
	 */
	MimeMessage createMimeMessage();

	/**
	 * 为发送器底层的 JavaMail Session 创建一个新的 JavaMail MimeMessage，
	 * 使用给定的输入流作为消息源。
	 * @param contentStream 消息的原始 MIME 输入流
	 * @return 新的 MimeMessage 实例
	 * @throws org.springframework.mail.MailParseException
	 * 消息创建失败时抛出
	*/
	MimeMessage createMimeMessage(InputStream contentStream) throws MailException;

	/**
	 * 发送给定的 JavaMail MIME 消息。
	 * 该消息需要通过 {@link #createMimeMessage()} 创建。
	 * @param mimeMessage 要发送的消息
	 * @throws org.springframework.mail.MailAuthenticationException
	 * 认证失败时抛出
	 * @throws org.springframework.mail.MailSendException
	 * 发送消息失败时抛出
	 * @see #createMimeMessage
	 */
	void send(MimeMessage mimeMessage) throws MailException;

	/**
	 * 批量发送给定的 JavaMail MIME 消息数组。
	 * 这些消息需要通过 {@link #createMimeMessage()} 创建。
	 * @param mimeMessages 要发送的消息
	 * @throws org.springframework.mail.MailAuthenticationException
	 * 认证失败时抛出
	 * @throws org.springframework.mail.MailSendException
	 * 发送消息失败时抛出
	 * @see #createMimeMessage
	 */
	void send(MimeMessage... mimeMessages) throws MailException;

	/**
	 * 发送由给定的 MimeMessagePreparator 准备的 JavaMail MIME 消息。
	 * <p>这是准备 MimeMessage 实例的另一种方式，替代
	 * {@link #createMimeMessage()} 和 {@link #send(MimeMessage)} 调用。
	 * 负责进行适当的异常转换。
	 * @param mimeMessagePreparator 要使用的预处理器
	 * @throws org.springframework.mail.MailPreparationException
	 * 准备消息失败时抛出
	 * @throws org.springframework.mail.MailParseException
	 * 解析消息失败时抛出
	 * @throws org.springframework.mail.MailAuthenticationException
	 * 认证失败时抛出
	 * @throws org.springframework.mail.MailSendException
	 * 发送消息失败时抛出
	 */
	void send(MimeMessagePreparator mimeMessagePreparator) throws MailException;

	/**
	 * 发送由给定的 MimeMessagePreparators 准备的 JavaMail MIME 消息。
	 * <p>这是准备 MimeMessage 实例的另一种方式，替代
	 * {@link #createMimeMessage()} 和 {@link #send(MimeMessage[])} 调用。
	 * 负责进行适当的异常转换。
	 * @param mimeMessagePreparators 要使用的预处理器
	 * @throws org.springframework.mail.MailPreparationException
	 * 准备消息失败时抛出
	 * @throws org.springframework.mail.MailParseException
	 * 解析消息失败时抛出
	 * @throws org.springframework.mail.MailAuthenticationException
	 * 认证失败时抛出
	 * @throws org.springframework.mail.MailSendException
	 * 发送消息失败时抛出
	 */
	void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException;

}
