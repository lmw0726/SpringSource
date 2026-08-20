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

package org.springframework.mail.javamail;

import javax.mail.internet.MimeMessage;

/**
 * 用于准备 JavaMail MIME 消息的回调接口。
 *
 * <p>对应的 {@link JavaMailSender} 的 {@code send} 方法
 * 将负责 {@link MimeMessage} 实例的实际创建以及适当的异常转换。
 *
 * <p>通常使用 {@link MimeMessageHelper} 来填充传入的 MimeMessage 会更加方便，
 * 特别是在处理附件或特殊字符编码时。
 * 参见 {@link MimeMessageHelper MimeMessageHelper 的 javadoc} 获取示例。
 *
 * @author Juergen Hoeller
 * @since 07.10.2003
 * @see JavaMailSender#send(MimeMessagePreparator)
 * @see JavaMailSender#send(MimeMessagePreparator[])
 * @see MimeMessageHelper
 */
@FunctionalInterface
public interface MimeMessagePreparator {

	/**
	 * 准备给定的 MimeMessage 实例。
	 * @param mimeMessage 要准备的消息
	 * @throws javax.mail.MessagingException 传递 MimeMessage 方法抛出的任何异常，
	 * 以便自动转换到 MailException 层次结构
	 * @throws java.io.IOException 传递 MimeMessage 方法抛出的任何异常，
	 * 以便自动转换到 MailException 层次结构
	 * @throws Exception 如果邮件准备失败，例如无法为邮件文本渲染 FreeMarker 模板
	 */
	void prepare(MimeMessage mimeMessage) throws Exception;

}
