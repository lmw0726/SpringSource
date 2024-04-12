/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 基础类，用于消息源实现，提供支持基础设施，如{@link java.text.MessageFormat}处理，但不实现{@link org.springframework.context.MessageSource}中定义的具体方法。
 *
 * <p>{@link AbstractMessageSource}从这个类派生，提供了具体的{@code getMessage}实现，它们将委托给一个中心模板方法来解析消息代码。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public abstract class MessageSourceSupport {

	/**
	 * 无效的消息格式化器
	 */
	private static final MessageFormat INVALID_MESSAGE_FORMAT = new MessageFormat("");

	/**
	 * 子类可用的日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 是否总是使用消息格式化器
	 */
	private boolean alwaysUseMessageFormat = false;

	/**
	 * 缓存以保存已生成的每条消息的MessageFormats。
	 * 用于传入的默认消息。已解析代码的MessageFormats在子类中以特定方式进行缓存。
	 */
	private final Map<String, Map<Locale, MessageFormat>> messageFormatsPerMessage = new HashMap<>();


	/**
	 * 设置是否始终应用{@code MessageFormat}规则，即使是没有参数的消息也要进行解析。
	 * <p>默认值为{@code false}：默认情况下，没有参数的消息将按原样返回，而不会通过{@code MessageFormat}进行解析。
	 * 将其设置为{@code true}以强制对所有消息使用{@code MessageFormat}，期望所有消息文本都使用{@code MessageFormat}转义编写。
	 * <p>例如，{@code MessageFormat}期望单引号被转义为两个相邻的单引号（{@code "''"}）。
	 * 如果您的消息文本都使用这样的转义编写，即使没有定义参数占位符，您也需要将此标志设置为{@code true}。
	 * 否则，只有实际带有参数的消息文本才应该使用{@code MessageFormat}转义编写。
	 *
	 * @see java.text.MessageFormat
	 */
	public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
		this.alwaysUseMessageFormat = alwaysUseMessageFormat;
	}

	/**
	 * 返回是否始终应用{@code MessageFormat}规则，即使是没有参数的消息也要进行解析。
	 */
	protected boolean isAlwaysUseMessageFormat() {
		return this.alwaysUseMessageFormat;
	}


	/**
	 * 渲染给定的默认消息字符串。将传入的默认消息渲染为完全格式化的默认消息，显示给用户。
	 * <p>默认实现将字符串传递给{@code formatMessage}，解析其中找到的任何参数占位符。子类可以覆盖此方法以插入默认消息的自定义处理。
	 *
	 * @param defaultMessage 传入的默认消息字符串
	 * @param args           参数数组，将填充消息中的参数，如果没有参数，则为{@code null}。
	 * @param locale         用于格式化的区域设置
	 * @return 渲染后的默认消息（已解析参数）
	 * @see #formatMessage(String, Object[], java.util.Locale)
	 */
	protected String renderDefaultMessage(String defaultMessage, @Nullable Object[] args, Locale locale) {
		return formatMessage(defaultMessage, args, locale);
	}

	/**
	 * 格式化给定的消息字符串，使用缓存的{@code MessageFormat}。
	 * 默认情况下，对于传入的默认消息，会调用此方法来解析其中找到的任何参数占位符。
	 *
	 * @param msg    要格式化的消息
	 * @param args   参数数组，将填充消息中的参数，如果没有参数，则为{@code null}
	 * @param locale 用于格式化的区域设置
	 * @return 格式化后的消息（已解析参数）
	 */
	protected String formatMessage(String msg, @Nullable Object[] args, Locale locale) {
		// 如果不总是使用消息格式化且参数数组为空，则直接返回原始消息
		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			return msg;
		}
		MessageFormat messageFormat = null;
		// 同步处理消息格式对象
		synchronized (this.messageFormatsPerMessage) {
			// 根据消息代码获取区域设置-消息格式化器映射
			Map<Locale, MessageFormat> messageFormatsPerLocale = this.messageFormatsPerMessage.get(msg);
			if (messageFormatsPerLocale != null) {
				// 如果该映射存在，获取消息格式化器
				messageFormat = messageFormatsPerLocale.get(locale);
			} else {
				// 创建一个HashMap，并添加进缓存中
				messageFormatsPerLocale = new HashMap<>();
				this.messageFormatsPerMessage.put(msg, messageFormatsPerLocale);
			}
			if (messageFormat == null) {
				try {
					// 根据消息代码和区域设置创建消息格式化器
					messageFormat = createMessageFormat(msg, locale);
				} catch (IllegalArgumentException ex) {
					// 无效的消息格式 - 可能不是用于格式化的，而是使用没有涉及参数的消息结构...
					if (isAlwaysUseMessageFormat()) {
						throw ex;
					}
					// 如果不强制使用格式，则静默处理原始消息...
					messageFormat = INVALID_MESSAGE_FORMAT;
				}
				// 添加进缓存中
				messageFormatsPerLocale.put(locale, messageFormat);
			}
		}
		// 如果消息格式对象为无效对象，则直接返回原始消息
		if (messageFormat == INVALID_MESSAGE_FORMAT) {
			return msg;
		}
		// 同步处理消息格式化
		synchronized (messageFormat) {
			// 使用解析后的参数数组格式化消息并返回
			return messageFormat.format(resolveArguments(args, locale));
		}
	}

	/**
	 * 为给定的消息和Locale创建一个{@code MessageFormat}。
	 *
	 * @param msg    要为其创建{@code MessageFormat}的消息
	 * @param locale 要为其创建{@code MessageFormat}的Locale
	 * @return {@code MessageFormat}实例
	 */
	protected MessageFormat createMessageFormat(String msg, Locale locale) {
		return new MessageFormat(msg, locale);
	}

	/**
	 * 用于解析参数对象的模板方法。
	 * <p>默认实现简单地返回给定的参数数组。
	 * 可以在子类中重写以解析特殊的参数类型。
	 *
	 * @param args   原始参数数组
	 * @param locale 要解析的Locale
	 * @return 已解析的参数数组
	 */
	protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
		return (args != null ? args : new Object[0]);
	}

}
