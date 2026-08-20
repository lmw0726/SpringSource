/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 辅助类，允许将 Spring
 * {@link org.springframework.context.MessageSource} 作为 {@link java.util.ResourceBundle} 进行访问。
 * 例如用于将 Spring MessageSource 暴露给 JSTL Web 视图。
 *
 * @author Juergen Hoeller
 * @since 27.02.2003
 * @see org.springframework.context.MessageSource
 * @see java.util.ResourceBundle
 * @see org.springframework.web.servlet.support.JstlUtils#exposeLocalizationContext
 */
public class MessageSourceResourceBundle extends ResourceBundle {

	private final MessageSource messageSource;

	private final Locale locale;


	/**
	 * 为给定的 MessageSource 和 Locale 创建新的 MessageSourceResourceBundle。
	 * @param source 用于获取消息的 MessageSource
	 * @param locale 用于获取消息的 Locale
	 */
	public MessageSourceResourceBundle(MessageSource source, Locale locale) {
		Assert.notNull(source, "MessageSource must not be null");
		this.messageSource = source;
		this.locale = locale;
	}

	/**
	 * 为给定的 MessageSource 和 Locale 创建新的 MessageSourceResourceBundle。
	 * @param source 用于获取消息的 MessageSource
	 * @param locale 用于获取消息的 Locale
	 * @param parent 当未找到本地消息时委托的父 ResourceBundle
	 */
	public MessageSourceResourceBundle(MessageSource source, Locale locale, ResourceBundle parent) {
		this(source, locale);
		setParent(parent);
	}


	/**
	 * 此实现在 MessageSource 中解析代码。
	 * 如果无法解析消息则返回 {@code null}。
	 */
	@Override
	@Nullable
	protected Object handleGetObject(String key) {
		try {
			return this.messageSource.getMessage(key, null, this.locale);
		}
		catch (NoSuchMessageException ex) {
			return null;
		}
	}

	/**
	 * 此实现检查目标 MessageSource 是否可以解析
	 * 给定键的消息，并相应地转换 {@code NoSuchMessageException}。
	 * 与 JDK 1.6 中 ResourceBundle 的默认实现不同，
	 * 此实现不依赖于枚举消息键的能力。
	 */
	@Override
	public boolean containsKey(String key) {
		try {
			this.messageSource.getMessage(key, null, this.locale);
			return true;
		}
		catch (NoSuchMessageException ex) {
			return false;
		}
	}

	/**
	 * 此实现抛出 {@code UnsupportedOperationException}，
	 * 因为 MessageSource 不允许枚举已定义的消息代码。
	 */
	@Override
	public Enumeration<String> getKeys() {
		throw new UnsupportedOperationException("MessageSourceResourceBundle does not support enumerating its keys");
	}

	/**
	 * 此实现暴露指定的 Locale，以便通过
	 * 标准 {@code ResourceBundle.getLocale()} 方法进行自省。
	 */
	@Override
	public Locale getLocale() {
		return this.locale;
	}

}
