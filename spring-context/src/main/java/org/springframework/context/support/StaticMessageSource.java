/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@link org.springframework.context.MessageSource}的简单实现，允许以编程方式注册消息。
 * 此MessageSource支持基本的国际化。
 *
 * <p>用于测试，而不是用于生产系统。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class StaticMessageSource extends AbstractMessageSource {
	/**
	 * 消息代码-区域设置-消息持有者映射
	 */
	private final Map<String, Map<Locale, MessageHolder>> messageMap = new HashMap<>();

	@Override
	@Nullable
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		Map<Locale, MessageHolder> localeMap = this.messageMap.get(code);
		if (localeMap == null) {
			// 如果消息映射中不存在指定的消息代码，则返回 null
			return null;
		}
		MessageHolder holder = localeMap.get(locale);
		if (holder == null) {
			// 如果消息映射中不存在指定的地区，则返回 null
			return null;
		}
		return holder.getMessage();
	}

	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		Map<Locale, MessageHolder> localeMap = this.messageMap.get(code);
		if (localeMap == null) {
			// 如果消息映射中不存在指定的消息代码，则返回 null
			return null;
		}
		MessageHolder holder = localeMap.get(locale);
		if (holder == null) {
			// 如果消息映射中不存在指定的地区，则返回 null
			return null;
		}
		// 返回持有者中的消息格式化器
		return holder.getMessageFormat();
	}

	/**
	 * 将给定的消息与给定的代码关联起来。
	 *
	 * @param code   查找代码
	 * @param locale 应在其中找到消息的区域设置
	 * @param msg    与此查找代码关联的消息
	 */
	public void addMessage(String code, Locale locale, String msg) {
		Assert.notNull(code, "Code must not be null");
		Assert.notNull(locale, "Locale must not be null");
		Assert.notNull(msg, "Message must not be null");
		this.messageMap.computeIfAbsent(code, key -> new HashMap<>(4)).put(locale, new MessageHolder(msg, locale));
		if (logger.isDebugEnabled()) {
			logger.debug("Added message [" + msg + "] for code [" + code + "] and Locale [" + locale + "]");
		}
	}

	/**
	 * 将给定的消息值与给定的键（作为代码）关联。
	 *
	 * @param messages 要注册的消息，消息代码作为键，消息文本作为值
	 * @param locale   应在其中找到消息的区域设置
	 */
	public void addMessages(Map<String, String> messages, Locale locale) {
		Assert.notNull(messages, "Messages Map must not be null");
		messages.forEach((code, msg) -> addMessage(code, locale, msg));
	}


	@Override
	public String toString() {
		return getClass().getName() + ": " + this.messageMap;
	}


	private class MessageHolder {
		/**
		 * 格式化好的消息
		 */
		private final String message;

		/**
		 * 区域设置
		 */
		private final Locale locale;

		/**
		 * 缓存的消息格式化器
		 */
		@Nullable
		private volatile MessageFormat cachedFormat;

		public MessageHolder(String message, Locale locale) {
			this.message = message;
			this.locale = locale;
		}

		public String getMessage() {
			return this.message;
		}

		public MessageFormat getMessageFormat() {
			MessageFormat messageFormat = this.cachedFormat;
			if (messageFormat == null) {
				// 如果缓存中不存在消息格式对象，则创建一个新的消息格式对象
				messageFormat = createMessageFormat(this.message, this.locale);
				// 将新创建的消息格式对象缓存起来
				this.cachedFormat = messageFormat;
			}
			return messageFormat;
		}

		@Override
		public String toString() {
			return this.message;
		}
	}

}
