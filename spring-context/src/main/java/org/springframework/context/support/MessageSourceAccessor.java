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

package org.springframework.context.support;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;

/**
 * 便捷类，用于轻松访问 MessageSource 中的消息，
 * 提供多种重载的 getMessage 方法。
 *
 * <p>可通过 ApplicationObjectSupport 获取，也可作为独立辅助工具
 * 在应用对象中复用和委托。
 *
 * @author Juergen Hoeller
 * @since 23.10.2003
 * @see ApplicationObjectSupport#getMessageSourceAccessor
 */
public class MessageSourceAccessor {

	private final MessageSource messageSource;

	@Nullable
	private final Locale defaultLocale;


	/**
	 * 创建新的 MessageSourceAccessor，使用 LocaleContextHolder 的区域设置作为默认区域。
	 * @param messageSource 要包装的 MessageSource
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	public MessageSourceAccessor(MessageSource messageSource) {
		this.messageSource = messageSource;
		this.defaultLocale = null;
	}

	/**
	 * 创建新的 MessageSourceAccessor，使用给定的默认区域。
	 * @param messageSource 要包装的 MessageSource
	 * @param defaultLocale 用于消息访问的默认区域
	 */
	public MessageSourceAccessor(MessageSource messageSource, Locale defaultLocale) {
		this.messageSource = messageSource;
		this.defaultLocale = defaultLocale;
	}


	/**
	 * 返回未显式指定区域时使用的默认区域。
	 * <p>默认实现返回传入对应构造方法的默认区域，
	 * 或者以 LocaleContextHolder 的区域作为后备。子类可覆盖此方法。
	 * @see #MessageSourceAccessor(org.springframework.context.MessageSource, java.util.Locale)
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	protected Locale getDefaultLocale() {
		return (this.defaultLocale != null ? this.defaultLocale : LocaleContextHolder.getLocale());
	}

	/**
	 * 根据给定的消息代码和默认区域检索消息。
	 * @param code 消息代码
	 * @param defaultMessage 查找失败时返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage) {
		String msg = this.messageSource.getMessage(code, null, defaultMessage, getDefaultLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 根据给定的消息代码和指定区域检索消息。
	 * @param code 消息代码
	 * @param defaultMessage 查找失败时返回的字符串
	 * @param locale 用于查找的区域
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage, Locale locale) {
		String msg = this.messageSource.getMessage(code, null, defaultMessage, locale);
		return (msg != null ? msg : "");
	}

	/**
	 * 根据给定的消息代码和默认区域检索消息。
	 * @param code 消息代码
	 * @param args 消息参数，无参数时为 {@code null}
	 * @param defaultMessage 查找失败时返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
		String msg = this.messageSource.getMessage(code, args, defaultMessage, getDefaultLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 根据给定的消息代码和指定区域检索消息。
	 * @param code 消息代码
	 * @param args 消息参数，无参数时为 {@code null}
	 * @param defaultMessage 查找失败时返回的字符串
	 * @param locale 用于查找的区域
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage, Locale locale) {
		String msg = this.messageSource.getMessage(code, args, defaultMessage, locale);
		return (msg != null ? msg : "");
	}

	/**
	 * 根据给定的消息代码和默认区域检索消息。
	 * @param code 消息代码
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, null, getDefaultLocale());
	}

	/**
	 * 根据给定的消息代码和指定区域检索消息。
	 * @param code 消息代码
	 * @param locale 用于查找的区域
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, null, locale);
	}

	/**
	 * 根据给定的消息代码和默认区域检索消息。
	 * @param code 消息代码
	 * @param args 消息参数，无参数时为 {@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, getDefaultLocale());
	}

	/**
	 * 根据给定的消息代码和指定区域检索消息。
	 * @param code 消息代码
	 * @param args 消息参数，无参数时为 {@code null}
	 * @param locale 用于查找的区域
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, locale);
	}

	/**
	 * 在默认区域中检索给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable 要解析的 MessageSourceResolvable
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, getDefaultLocale());
	}

	/**
	 * 在指定区域中检索给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable 要解析的 MessageSourceResolvable
	 * @param locale 用于查找的区域
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果未找到
	 */
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, locale);
	}

}
