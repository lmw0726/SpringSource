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

package org.springframework.context.support;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * 将所有调用委托给父级MessageSource的空MessageSource。
 * 如果没有可用的父级，则它将不会解析任何消息。
 *
 * <p>如果上下文没有定义自己的MessageSource，则由AbstractApplicationContext使用作为占位符。
 * 不打算在应用程序中直接使用。
 *
 * @author Juergen Hoeller
 * @see AbstractApplicationContext
 * @since 1.1.5
 */
public class DelegatingMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

	/**
	 * 父消息源
	 */
	@Nullable
	private MessageSource parentMessageSource;


	@Override
	public void setParentMessageSource(@Nullable MessageSource parent) {
		this.parentMessageSource = parent;
	}

	@Override
	@Nullable
	public MessageSource getParentMessageSource() {
		return this.parentMessageSource;
	}


	@Override
	@Nullable
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		if (this.parentMessageSource != null) {
			// 如果存在父消息源，则委托给父消息源进行消息获取
			return this.parentMessageSource.getMessage(code, args, defaultMessage, locale);
		} else if (defaultMessage != null) {
			// 否则，如果存在默认消息，则使用默认消息进行渲染
			return renderDefaultMessage(defaultMessage, args, locale);
		} else {
			// 否则返回空
			return null;
		}
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		if (this.parentMessageSource != null) {
			// 如果存在父消息源，则委托给父消息源进行消息获取
			return this.parentMessageSource.getMessage(code, args, locale);
		} else {
			// 否则抛出 NoSuchMessageException 异常
			throw new NoSuchMessageException(code, locale);
		}
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		if (this.parentMessageSource != null) {
			// 如果存在父消息源，则委托给父消息源进行消息获取
			return this.parentMessageSource.getMessage(resolvable, locale);
		} else {
			if (resolvable.getDefaultMessage() != null) {
				// 如果默认消息不为空，则使用默认消息渲染
				return renderDefaultMessage(resolvable.getDefaultMessage(), resolvable.getArguments(), locale);
			}
			// 否则，获取消息码数组中的第一个消息码，如果为空，则抛出 NoSuchMessageException 异常
			String[] codes = resolvable.getCodes();
			String code = (codes != null && codes.length > 0 ? codes[0] : "");
			throw new NoSuchMessageException(code, locale);
		}
	}


	@Override
	public String toString() {
		return this.parentMessageSource != null ? this.parentMessageSource.toString() : "Empty MessageSource";
	}

}
