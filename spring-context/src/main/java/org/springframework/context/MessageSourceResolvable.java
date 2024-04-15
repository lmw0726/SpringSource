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

package org.springframework.context;

import org.springframework.lang.Nullable;

/**
 * 用于在{@link MessageSource}中进行消息解析的对象的接口。
 *
 * <p>Spring自己的验证错误类实现了这个接口。
 *
 * @author Juergen Hoeller
 * @see MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 * @see org.springframework.validation.ObjectError
 * @see org.springframework.validation.FieldError
 */
@FunctionalInterface
public interface MessageSourceResolvable {

	/**
	 * 返回用于解析此消息的代码，按照应尝试的顺序排列。因此，最后一个代码将是默认代码。
	 *
	 * @return 与此消息相关联的代码的字符串数组
	 */
	@Nullable
	String[] getCodes();

	/**
	 * 返回用于解析此消息的参数数组。
	 * <p>默认实现简单地返回{@code null}。
	 *
	 * @return 用作参数以替换消息文本中的占位符的对象数组
	 * @see java.text.MessageFormat
	 */
	@Nullable
	default Object[] getArguments() {
		return null;
	}

	/**
	 * 返回用于解析此消息的默认消息。
	 * <p>默认实现简单地返回{@code null}。
	 * 请注意，默认消息可能与主消息代码（{@link #getCodes()}）相同，这实际上对此特定消息强制执行了
	 * {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage}。
	 *
	 * @return 默认消息，如果没有默认消息则返回{@code null}
	 */
	@Nullable
	default String getDefaultMessage() {
		return null;
	}

}
