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

import org.springframework.context.MessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * Spring的{@link MessageSourceResolvable}接口的默认实现。
 * 提供了一种简单的方式来存储解析消息所需的所有必要值，通过{@link org.springframework.context.MessageSource}。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 * @since 13.02.2004
 */
@SuppressWarnings("serial")
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {

	/**
	 * 解析此消息的代码数组
	 */
	@Nullable
	private final String[] codes;

	/**
	 * 消息参数
	 */
	@Nullable
	private final Object[] arguments;

	/**
	 * 默认消息
	 */
	@Nullable
	private final String defaultMessage;


	/**
	 * 创建一个新的DefaultMessageSourceResolvable。
	 *
	 * @param code 用于解析此消息的代码
	 */
	public DefaultMessageSourceResolvable(String code) {
		this(new String[]{code}, null, null);
	}

	/**
	 * 创建一个新的DefaultMessageSourceResolvable。
	 *
	 * @param codes 用于解析此消息的代码
	 */
	public DefaultMessageSourceResolvable(String[] codes) {
		this(codes, null, null);
	}

	/**
	 * 创建一个新的DefaultMessageSourceResolvable。
	 *
	 * @param codes          用于解析此消息的代码
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
		this(codes, null, defaultMessage);
	}

	/**
	 * 创建一个新的DefaultMessageSourceResolvable。
	 *
	 * @param codes     用于解析此消息的代码
	 * @param arguments 用于解析此消息的参数数组
	 */
	public DefaultMessageSourceResolvable(String[] codes, Object[] arguments) {
		this(codes, arguments, null);
	}

	/**
	 * 创建一个新的DefaultMessageSourceResolvable。
	 *
	 * @param codes          用于解析此消息的代码
	 * @param arguments      用于解析此消息的参数数组
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public DefaultMessageSourceResolvable(
			@Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

		this.codes = codes;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	/**
	 * 复制构造函数：从另一个可解析对象创建一个新实例。
	 *
	 * @param resolvable 要复制的可解析对象
	 */
	public DefaultMessageSourceResolvable(MessageSourceResolvable resolvable) {
		this(resolvable.getCodes(), resolvable.getArguments(), resolvable.getDefaultMessage());
	}


	/**
	 * 返回此可解析对象的默认代码，即代码数组中的最后一个。
	 */
	@Nullable
	public String getCode() {
		return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
	}

	@Override
	@Nullable
	public String[] getCodes() {
		return this.codes;
	}

	@Override
	@Nullable
	public Object[] getArguments() {
		return this.arguments;
	}

	@Override
	@Nullable
	public String getDefaultMessage() {
		return this.defaultMessage;
	}

	/**
	 * 指示是否需要为指定的默认消息渲染以替换占位符和/或{@link java.text.MessageFormat}转义。
	 *
	 * @return {@code true}，如果默认消息可能包含参数占位符；{@code false}，如果它绝对不包含占位符或自定义转义，
	 * 因此可以简单地按原样公开
	 * @see #getDefaultMessage()
	 * @see #getArguments()
	 * @see AbstractMessageSource#renderDefaultMessage
	 * @since 5.1.7
	 */
	public boolean shouldRenderDefaultMessage() {
		return true;
	}


	/**
	 * 为此MessageSourceResolvable构建默认的String表示形式：
	 * 包括代码、参数和默认消息。
	 */
	protected final String resolvableToString() {
		StringBuilder result = new StringBuilder(64);
		result.append("codes [").append(StringUtils.arrayToDelimitedString(this.codes, ","));
		result.append("]; arguments [").append(StringUtils.arrayToDelimitedString(this.arguments, ","));
		result.append("]; default message [").append(this.defaultMessage).append(']');
		return result.toString();
	}

	/**
	 * 默认实现公开此MessageSourceResolvable的属性。
	 * <p>在更具体的子类中被覆盖，可能通过{@code resolvableToString()}包含可解析的内容。
	 *
	 * @see #resolvableToString()
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": " + resolvableToString();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MessageSourceResolvable)) {
			return false;
		}
		MessageSourceResolvable otherResolvable = (MessageSourceResolvable) other;
		return (ObjectUtils.nullSafeEquals(getCodes(), otherResolvable.getCodes()) &&
				ObjectUtils.nullSafeEquals(getArguments(), otherResolvable.getArguments()) &&
				ObjectUtils.nullSafeEquals(getDefaultMessage(), otherResolvable.getDefaultMessage()));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getCodes());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArguments());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getDefaultMessage());
		return hashCode;
	}

}
