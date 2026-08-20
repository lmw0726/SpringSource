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

package org.springframework.mail;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 遇到邮件发送错误时抛出的异常。
 * 可以注册失败的消息及其对应的异常。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class MailSendException extends MailException {

	private final transient Map<Object, Exception> failedMessages;

	@Nullable
	private final Exception[] messageExceptions;


	/**
	 * MailSendException 构造方法。
	 * @param msg 详细消息
	 */
	public MailSendException(String msg) {
		this(msg, null);
	}

	/**
	 * MailSendException 构造方法。
	 * @param msg 详细消息
	 * @param cause 使用的邮件 API 的根本原因
	 */
	public MailSendException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
		this.failedMessages = new LinkedHashMap<>();
		this.messageExceptions = null;
	}

	/**
	 * 用于注册失败消息的构造方法，以失败的消息作为键，
	 * 以抛出的异常作为值。
	 * <p>消息应与最初传递给调用的 send 方法的消息相同。
	 * @param msg 详细消息
	 * @param cause 使用的邮件 API 的根本原因
	 * @param failedMessages 以失败的消息为键、抛出的异常为值的 Map
	 */
	public MailSendException(@Nullable String msg, @Nullable Throwable cause, Map<Object, Exception> failedMessages) {
		super(msg, cause);
		this.failedMessages = new LinkedHashMap<>(failedMessages);
		this.messageExceptions = failedMessages.values().toArray(new Exception[0]);
	}

	/**
	 * 用于注册失败消息的构造方法，以失败的消息作为键，
	 * 以抛出的异常作为值。
	 * <p>消息应与最初传递给调用的 send 方法的消息相同。
	 * @param failedMessages 以失败的消息为键、抛出的异常为值的 Map
	 */
	public MailSendException(Map<Object, Exception> failedMessages) {
		this(null, null, failedMessages);
	}


	/**
	 * 返回一个 Map，其中以失败的消息作为键，以抛出的异常作为值。
	 * <p>请注意，邮件服务器连接失败不会导致失败的消息出现在这里：
	 * 只有在实际发送消息尝试失败时，消息才会包含在此处。
	 * <p>消息将与最初传递给调用的 send 方法的消息相同，即在使用
	 * 通用 MailSender 接口时为 SimpleMailMessage。
	 * <p>如果通过 JavaMailSender 发送 MimeMessage 实例，
	 * 消息将为 MimeMessage 类型。
	 * <p><b>注意：</b>此 Map 在序列化后将不可用。
	 * 在这种情况下，请使用 {@link #getMessageExceptions()}，
	 * 它在序列化后仍然可用。
	 * @return 以失败的消息为键、抛出的异常为值的 Map
	 * @see SimpleMailMessage
	 * @see javax.mail.internet.MimeMessage
	 */
	public final Map<Object, Exception> getFailedMessages() {
		return this.failedMessages;
	}

	/**
	 * 返回一个包含抛出的消息异常的数组。
	 * <p>请注意，邮件服务器连接失败不会导致失败的消息出现在这里：
	 * 只有在实际发送消息尝试失败时，消息才会包含在此处。
	 * @return 抛出的消息异常数组，如果没有失败的消息则返回空数组
	 */
	public final Exception[] getMessageExceptions() {
		return (this.messageExceptions != null ? this.messageExceptions : new Exception[0]);
	}


	@Override
	@Nullable
	public String getMessage() {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			return super.getMessage();
		}
		else {
			StringBuilder sb = new StringBuilder();
			String baseMessage = super.getMessage();
			if (baseMessage != null) {
				sb.append(baseMessage).append(". ");
			}
			sb.append("Failed messages: ");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				sb.append(subEx.toString());
				if (i < this.messageExceptions.length - 1) {
					sb.append("; ");
				}
			}
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			return super.toString();
		}
		else {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.append("; message exceptions (").append(this.messageExceptions.length).append(") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				sb.append('\n').append("Failed message ").append(i + 1).append(": ");
				sb.append(subEx);
			}
			return sb.toString();
		}
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			super.printStackTrace(ps);
		}
		else {
			ps.println(super.toString() + "; message exception details (" +
					this.messageExceptions.length + ") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				ps.println("Failed message " + (i + 1) + ":");
				subEx.printStackTrace(ps);
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		if (ObjectUtils.isEmpty(this.messageExceptions)) {
			super.printStackTrace(pw);
		}
		else {
			pw.println(super.toString() + "; message exception details (" +
					this.messageExceptions.length + ") are:");
			for (int i = 0; i < this.messageExceptions.length; i++) {
				Exception subEx = this.messageExceptions[i];
				pw.println("Failed message " + (i + 1) + ":");
				subEx.printStackTrace(pw);
			}
		}
	}

}
