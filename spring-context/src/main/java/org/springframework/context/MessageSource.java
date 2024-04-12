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

package org.springframework.context;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * 用于解析消息的策略接口，支持消息的参数化和国际化。
 *
 * <p>Spring提供了两个生产环境的内置实现:
 * <ul>
 * <li>{@link org.springframework.context.support.ResourceBundleMessageSource}：构建在标准{@link java.util.ResourceBundle}之上，共享其限制。
 * <li>{@link org.springframework.context.support.ReloadableResourceBundleMessageSource}：高度可配置，特别是在重新加载消息定义方面。
 * </ul>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 */
public interface MessageSource {

	/**
	 * 尝试解析消息。如果找不到消息，则返回默认消息。
	 *
	 * @param code           要查找的消息代码，例如'calculator.noRateSet'。
	 *                       鼓励MessageSource用户基于限定的类或包名称设置消息名称，以避免潜在冲突并确保最大清晰度。
	 * @param args           将填充消息中参数的参数数组（参数在消息中看起来像"{0}"、"{1,date}"、"{2,time}"），如果没有则为{@code null}
	 * @param defaultMessage 如果查找失败，则返回的默认消息
	 * @param locale         进行查找的区域设置
	 * @return 如果查找成功，则返回已解析的消息；否则返回作为参数传递的默认消息（可能为{@code null}）
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	@Nullable
	String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

	/**
	 * 尝试解析消息。如果找不到消息，则视为错误。
	 *
	 * @param code   要查找的消息代码，例如'calculator.noRateSet'。
	 *               鼓励MessageSource用户基于限定的类或包名称设置消息名称，以避免潜在冲突并确保最大清晰度。
	 * @param args   将填充消息中参数的参数数组（参数在消息中看起来像"{0}"、"{1,date}"、"{2,time}"），如果没有则为{@code null}
	 * @param locale 进行查找的区域设置
	 * @return 已解析的消息（永远不为{@code null}）
	 * @throws NoSuchMessageException 如果未找到相应的消息
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see java.text.MessageFormat
	 */
	String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

	/**
	 * 使用传入的{@code MessageSourceResolvable}参数中包含的所有属性尝试解析消息。
	 * <p>注意：在调用此方法时，我们必须抛出{@code NoSuchMessageException}，
	 * 因为在调用此方法时，我们无法确定可解析的{@code defaultMessage}属性是否为{@code null}。
	 *
	 * @param resolvable 存储解析消息所需属性的值对象（可能包括默认消息）
	 * @param locale     进行查找的区域设置
	 * @return 已解析的消息（永远不为{@code null}，因为即使是{@code MessageSourceResolvable}提供的默认消息也需要是非null的）
	 * @throws NoSuchMessageException 如果未找到相应的消息（并且{@code MessageSourceResolvable}未提供默认消息）
	 * @see MessageSourceResolvable#getCodes()
	 * @see MessageSourceResolvable#getArguments()
	 * @see MessageSourceResolvable#getDefaultMessage()
	 * @see java.text.MessageFormat
	 */
	String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
