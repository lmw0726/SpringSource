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

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * {@link HierarchicalMessageSource}接口的抽象实现，实现了消息变体的常见处理，
 * 使得为具体的MessageSource实现特定策略变得容易。
 *
 * <p>子类必须实现抽象的{@link #resolveCode}方法。为了有效地解析没有参数的消息，
 * 还应该重写{@link #resolveCodeWithoutArguments}方法，以解析没有涉及MessageFormat的消息。
 *
 * <p><b>注意:</b> 默认情况下，只有在为消息传递了参数时，消息文本才会通过MessageFormat进行解析。
 * 如果没有参数，消息文本将按原样返回。因此，您应该只对具有实际参数的消息使用MessageFormat转义，
 * 并保留所有其他消息未转义。如果您希望转义所有消息，请将“alwaysUseMessageFormat”标志设置为“true”。
 *
 * <p>不仅支持MessageSourceResolvable作为主要消息，而且还支持解析作为消息参数的MessageSourceResolvable本身。
 *
 * <p>该类不实现按代码缓存消息，因此子类可以随时间动态更改消息。鼓励子类以感知修改的方式缓存消息，
 * 允许更新消息的热部署。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see #resolveCode(String, java.util.Locale)
 * @see #resolveCodeWithoutArguments(String, java.util.Locale)
 * @see #setAlwaysUseMessageFormat
 * @see java.text.MessageFormat
 */
public abstract class AbstractMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {
	/**
	 * 父消息源
	 */
	@Nullable
	private MessageSource parentMessageSource;

	/**
	 * 公共消息
	 */
	@Nullable
	private Properties commonMessages;
	/**
	 * 是否使用代码作为默认消息
	 */
	private boolean useCodeAsDefaultMessage = false;


	@Override
	public void setParentMessageSource(@Nullable MessageSource parent) {
		this.parentMessageSource = parent;
	}

	@Override
	@Nullable
	public MessageSource getParentMessageSource() {
		return this.parentMessageSource;
	}

	/**
	 * 指定与消息代码作为键和完整消息字符串（可能包含参数占位符）作为值的与区域设置无关的常见消息。
	 * <p>还可以链接到外部定义的Properties对象，例如通过{@link org.springframework.beans.factory.config.PropertiesFactoryBean}定义的对象。
	 */
	public void setCommonMessages(@Nullable Properties commonMessages) {
		this.commonMessages = commonMessages;
	}

	/**
	 * 返回定义与区域设置无关的常见消息的Properties对象，如果有的话。
	 */
	@Nullable
	protected Properties getCommonMessages() {
		return this.commonMessages;
	}

	/**
	 * 设置是否使用消息代码作为默认消息而不是抛出NoSuchMessageException。在开发和调试中很有用。默认为“false”。
	 * <p>注意: 如果存在MessageSourceResolvable具有多个代码（例如FieldError）和具有父MessageSource的MessageSource，
	 * 请勿在父级中激活“useCodeAsDefaultMessage”：
	 * 否则，将仅通过父级返回第一个代码作为消息，而不会尝试检查其他代码。
	 * <p>要能够在父级中打开“useCodeAsDefaultMessage”，
	 * AbstractMessageSource和AbstractApplicationContext包含特殊检查，以在可用时委托到内部{@link #getMessageInternal}方法。
	 * 一般来说，建议仅在开发过程中使用“useCodeAsDefaultMessage”，而不要在生产中依赖它。
	 *
	 * @see #getMessage(String, Object[], Locale)
	 * @see org.springframework.validation.FieldError
	 */
	public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
		this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
	}

	/**
	 * 返回是否使用消息代码作为默认消息而不是抛出NoSuchMessageException。在开发和调试中很有用。默认为“false”。
	 * <p>或者，考虑重写{@link #getDefaultMessage}方法，为不可解析的代码返回自定义回退消息。
	 *
	 * @see #getDefaultMessage(String)
	 */
	protected boolean isUseCodeAsDefaultMessage() {
		return this.useCodeAsDefaultMessage;
	}


	@Override
	public final String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		// 获取内部消息
		String msg = getMessageInternal(code, args, locale);
		// 如果内部消息不为空，则返回内部消息
		if (msg != null) {
			return msg;
		}
		// 如果默认消息为空，则获取默认消息
		if (defaultMessage == null) {
			return getDefaultMessage(code);
		}
		// 否则渲染默认消息，并返回
		return renderDefaultMessage(defaultMessage, args, locale);
	}

	@Override
	public final String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		// 获取内部消息
		String msg = getMessageInternal(code, args, locale);
		// 如果该消息存在，则返回该消息
		if (msg != null) {
			return msg;
		}
		// 获取默认消息
		String fallback = getDefaultMessage(code);
		// 如果成功获取到默认消息，则返回该消息
		if (fallback != null) {
			return fallback;
		}
		// 如果都未成功获取到消息，则抛出 NoSuchMessageException 异常
		throw new NoSuchMessageException(code, locale);
	}

	@Override
	public final String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		// 获取消息代码数组
		String[] codes = resolvable.getCodes();
		if (codes != null) {
			// 如果存在消息代码数组，遍历每一个消息代码
			for (String code : codes) {
				// 根据消息代码、参数、区域设置获取内部消息
				String message = getMessageInternal(code, resolvable.getArguments(), locale);
				if (message != null) {
					// 如果内部消息存在，则返回该消息
					return message;
				}
			}
		}
		// 获取默认消息
		String defaultMessage = getDefaultMessage(resolvable, locale);
		if (defaultMessage != null) {
			// 默认消息存在，则返回该消息
			return defaultMessage;
		}
		// 抛出异常
		throw new NoSuchMessageException(!ObjectUtils.isEmpty(codes) ? codes[codes.length - 1] : "", locale);
	}


	/**
	 * 在给定的区域设置中将给定代码和参数解析为消息，如果未找到则返回{@code null}。不会回退到代码作为默认消息。
	 * 由{@code getMessage}方法调用。
	 *
	 * @param code   要查找的代码，例如'calculator.noRateSet'
	 * @param args   将用于填充消息中参数的参数数组
	 * @param locale 要查找的区域设置
	 * @return 已解析的消息，如果未找到则为{@code null}
	 * @see #getMessage(String, Object[], String, Locale)
	 * @see #getMessage(String, Object[], Locale)
	 * @see #getMessage(MessageSourceResolvable, Locale)
	 * @see #setUseCodeAsDefaultMessage
	 */
	@Nullable
	protected String getMessageInternal(@Nullable String code, @Nullable Object[] args, @Nullable Locale locale) {
		if (code == null) {
			// 消息代码为空，则返回null
			return null;
		}
		if (locale == null) {
			// 区域设置不存在，则获取默认区域设置
			locale = Locale.getDefault();
		}
		Object[] argsToUse = args;

		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			// 如果没有经常使用MessageFormat并且参数为空
			// 优化的解析：没有要应用的参数，
			// 因此不需要涉及MessageFormat。
			// 请注意，默认实现仍然使用MessageFormat；
			// 可以在特定子类中进行覆盖。
			// 使用无参函数解析消息代码
			String message = resolveCodeWithoutArguments(code, locale);
			if (message != null) {
				// 消息存在，则返回该消息
				return message;
			}
		} else {
			// 为了解决消息定义在父MessageSource中但可解析的参数定义在子MessageSource中的情况，急切地解决参数。
			argsToUse = resolveArguments(args, locale);
			// 解析消息代码，并获取消息格式化器
			MessageFormat messageFormat = resolveCode(code, locale);
			if (messageFormat != null) {
				synchronized (messageFormat) {
					// 如果存在消息格式化器，加锁并格式化消息
					return messageFormat.format(argsToUse);
				}
			}
		}

		// 检查具有给定消息代码的与区域设置无关的常见消息。
		Properties commonMessages = getCommonMessages();
		if (commonMessages != null) {
			// 如果存在公共消息
			// 根据消息代码获取公共消息
			String commonMessage = commonMessages.getProperty(code);
			if (commonMessage != null) {
				// 公共消息存在，则格式化改公共消息
				return formatMessage(commonMessage, args, locale);
			}
		}

		// 未找到->如果有父级，检查父级。
		return getMessageFromParent(code, argsToUse, locale);
	}

	/**
	 * 尝试从父{@code MessageSource}检索给定消息（如果存在）。
	 *
	 * @param code   要查找的代码，例如“calculator.noRateSet”
	 * @param args   将用于填充消息中参数的参数数组
	 * @param locale 要查找的区域设置
	 * @return 已解析的消息，如果未找到则为{@code null}
	 * @see #getParentMessageSource()
	 */
	@Nullable
	protected String getMessageFromParent(String code, @Nullable Object[] args, Locale locale) {
		// 获取父消息源
		MessageSource parent = getParentMessageSource();
		if (parent != null) {
			// 存在父消息源
			if (parent instanceof AbstractMessageSource) {
				// 调用内部方法以避免在激活“useCodeAsDefaultMessage”时返回默认代码。
				return ((AbstractMessageSource) parent).getMessageInternal(code, args, locale);
			} else {
				// 检查父MessageSource，如果在那里找不到，则返回null。
				// 包括自定义MessageSource实现和DelegatingMessageSource。
				return parent.getMessage(code, args, null, locale);
			}
		}
		// 父级中也找不到。
		return null;
	}

	/**
	 * 获取给定{@code MessageSourceResolvable}的默认消息。
	 * <p>如果可用，此实现将完全呈现默认消息，或者如果主消息代码正在用作默认消息，则只返回普通默认消息{@code String}。
	 *
	 * @param resolvable 要解析默认消息的值对象
	 * @param locale     当前区域设置
	 * @return 默认消息，如果没有则为{@code null}
	 * @see #renderDefaultMessage(String, Object[], Locale)
	 * @see #getDefaultMessage(String)
	 * @since 4.3.6
	 */
	@Nullable
	protected String getDefaultMessage(MessageSourceResolvable resolvable, Locale locale) {
		// 获取默认消息
		String defaultMessage = resolvable.getDefaultMessage();
		// 获取消息代码数组
		String[] codes = resolvable.getCodes();
		// 如果存在默认消息
		if (defaultMessage != null) {
			// 如果 MessageSourceResolvable 是 DefaultMessageSourceResolvable 类型
			// 并且 不呈现默认消息，则直接返回原始消息
			if (resolvable instanceof DefaultMessageSourceResolvable &&
					!((DefaultMessageSourceResolvable) resolvable).shouldRenderDefaultMessage()) {
				// 给定的默认消息不包含任何参数占位符
				// （并且也未针对alwaysUseMessageFormat进行了转义）->原样返回。
				return defaultMessage;
			}
			// 如果消息代码数组不为空且默认消息等于消息代码数组的第一个元素，则直接返回默认消息
			if (!ObjectUtils.isEmpty(codes) && defaultMessage.equals(codes[0])) {
				// 不要对代码作为默认消息进行格式化，即使alwaysUseMessageFormat=true
				return defaultMessage;
			}
			// 否则，根据默认消息、参数数组和区域设置渲染默认消息
			return renderDefaultMessage(defaultMessage, resolvable.getArguments(), locale);
		}
		// 如果消息代码数组不为空，则返回消息代码数组的第一个元素的默认消息，否则返回 null
		return (!ObjectUtils.isEmpty(codes) ? getDefaultMessage(codes[0]) : null);
	}

	/**
	 * 如果有的话，返回给定代码的回退默认消息。
	 * <p>如果激活了“useCodeAsDefaultMessage”，则默认返回代码本身，否则不返回回退。在没有回退消息的情况下，
	 * 调用者通常会从{@code getMessage}接收到{@code NoSuchMessageException}。
	 *
	 * @param code 我们无法解析并且没有收到显式默认消息的消息代码
	 * @return 要使用的默认消息，如果没有则为{@code null}
	 * @see #setUseCodeAsDefaultMessage
	 */
	@Nullable
	protected String getDefaultMessage(String code) {
		if (isUseCodeAsDefaultMessage()) {
			// 如果使用消息代码作为默认消息，则返回该消息代码
			return code;
		}
		return null;
	}

	/**
	 * 搜索给定对象数组，查找任何MessageSourceResolvable对象并解析它们。
	 * <p>允许消息具有MessageSourceResolvable作为参数。
	 *
	 * @param args   对消息的参数数组
	 * @param locale 要解析的区域设置
	 * @return 带有任何已解析的MessageSourceResolvable的参数数组
	 */
	@Override
	protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
		// 如果参数数组为空，则调用父类方法解析参数
		if (ObjectUtils.isEmpty(args)) {
			return super.resolveArguments(args, locale);
		}

		// 创建一个列表用于存储解析后的参数
		List<Object> resolvedArgs = new ArrayList<>(args.length);
		// 遍历参数数组
		for (Object arg : args) {
			// 如果参数是 MessageSourceResolvable 类型，则通过消息源获取消息并添加到解析后的参数列表中
			if (arg instanceof MessageSourceResolvable) {
				resolvedArgs.add(getMessage((MessageSourceResolvable) arg, locale));
			} else {
				// 如果参数不是 MessageSourceResolvable 类型，则直接添加到解析后的参数列表中
				resolvedArgs.add(arg);
			}
		}
		// 将解析后的参数列表转换为数组并返回
		return resolvedArgs.toArray();
	}

	/**
	 * 子类可以重写此方法以在优化的方式下解析没有参数的消息，即在不涉及 MessageFormat 的情况下解析。
	 * <p>默认实现确实使用了 MessageFormat，通过委托给 {@link #resolveCode} 方法。鼓励子类将其替换为优化的解析。
	 * <p>不幸的是，{@code java.text.MessageFormat} 的实现方式不高效。特别是，它不会检测到消息模式根本不包含参数占位符。
	 * 因此，建议对没有参数的消息规避 MessageFormat。
	 *
	 * @param code   要解析的消息的代码
	 * @param locale 要解析代码的语言环境
	 *               （鼓励子类支持国际化）
	 * @return 消息字符串，如果未找到则为 {@code null}
	 * @see #resolveCode
	 * @see java.text.MessageFormat
	 */
	@Nullable
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		// 解析消息格式
		MessageFormat messageFormat = resolveCode(code, locale);
		// 如果成功解析到消息格式
		if (messageFormat != null) {
			synchronized (messageFormat) {
				// 加锁并格式化消息
				return messageFormat.format(new Object[0]);
			}
		}
		// 如果未解析到消息格式，则返回 null
		return null;
	}

	/**
	 * 子类必须实现此方法来解析消息。
	 * <p>返回 MessageFormat 实例而不是消息字符串，以便在子类中适当缓存 MessageFormats。
	 * <p><b>鼓励子类为没有参数的消息提供优化的解析，不涉及 MessageFormat。</b>
	 * 详情请参阅 {@link #resolveCodeWithoutArguments} 的 javadoc。
	 *
	 * @param code   要解析的消息的代码
	 * @param locale 要解析代码的语言环境
	 *               （鼓励子类支持国际化）
	 * @return 消息的 MessageFormat，如果未找到则为 {@code null}
	 * @see #resolveCodeWithoutArguments(String, java.util.Locale)
	 */
	@Nullable
	protected abstract MessageFormat resolveCode(String code, Locale locale);

}
