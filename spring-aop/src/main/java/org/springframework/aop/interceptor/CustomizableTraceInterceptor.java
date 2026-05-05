/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code MethodInterceptor} 实现，使用占位符提供高度可定制的
 * 方法级跟踪。
 *
 * <p>跟踪消息在方法进入时写入，如果方法调用成功则在方法退出时写入。
 * 如果调用导致异常，则写入异常消息。这些跟踪消息的内容是完全可定制的，
 * 特殊的占位符可用于在日志消息中包含运行时信息。可用的占位符有：
 *
 * <p><ul>
 * <li>{@code $[methodName]} - 替换为被调用方法的名称</li>
 * <li>{@code $[targetClassName]} - 替换为调用目标类的名称</li>
 * <li>{@code $[targetClassShortName]} - 替换为调用目标类的短名称</li>
 * <li>{@code $[returnValue]} - 替换为调用返回的值</li>
 * <li>{@code $[argumentTypes]} - 替换为方法参数的短类名逗号分隔列表</li>
 * <li>{@code $[arguments]} - 替换为方法参数的 {@code String} 表示逗号分隔列表</li>
 * <li>{@code $[exception]} - 替换为调用期间引发的任何 {@code Throwable} 的 {@code String} 表示</li>
 * <li>{@code $[invocationTime]} - 替换为方法调用所用的时间（毫秒）</li>
 * </ul>
 *
 * <p>对于哪些占位符可以在哪些消息中使用有限制：
 * 有关有效占位符的详细信息，请参阅各个消息属性。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setEnterMessage
 * @see #setExitMessage
 * @see #setExceptionMessage
 * @see SimpleTraceInterceptor
 */
@SuppressWarnings("serial")
public class CustomizableTraceInterceptor extends AbstractTraceInterceptor {

	/**
	 * {@code $[methodName]} 占位符。
	 * 替换为被调用方法的名称。
	 */
	public static final String PLACEHOLDER_METHOD_NAME = "$[methodName]";

	/**
	 * {@code $[targetClassName]} 占位符。
	 * 替换为方法调用目标的 {@code Class} 的完全限定名称。
	 */
	public static final String PLACEHOLDER_TARGET_CLASS_NAME = "$[targetClassName]";

	/**
	 * {@code $[targetClassShortName]} 占位符。
	 * 替换为方法调用目标的 {@code Class} 的短名称。
	 */
	public static final String PLACEHOLDER_TARGET_CLASS_SHORT_NAME = "$[targetClassShortName]";

	/**
	 * {@code $[returnValue]} 占位符。
	 * 替换为方法调用返回的值的 {@code String} 表示。
	 */
	public static final String PLACEHOLDER_RETURN_VALUE = "$[returnValue]";

	/**
	 * {@code $[argumentTypes]} 占位符。
	 * 替换为方法调用的参数类型的逗号分隔列表。
	 * 参数类型写为短类名。
	 */
	public static final String PLACEHOLDER_ARGUMENT_TYPES = "$[argumentTypes]";

	/**
	 * {@code $[arguments]} 占位符。
	 * 替换为方法调用的参数值的逗号分隔列表。
	 * 依赖每个参数类型的 {@code toString()} 方法。
	 */
	public static final String PLACEHOLDER_ARGUMENTS = "$[arguments]";

	/**
	 * {@code $[exception]} 占位符。
	 * 替换为方法调用期间引发的任何 {@code Throwable} 的 {@code String} 表示。
	 */
	public static final String PLACEHOLDER_EXCEPTION = "$[exception]";

	/**
	 * {@code $[invocationTime]} 占位符。
	 * 替换为调用所用的时间（毫秒）。
	 */
	public static final String PLACEHOLDER_INVOCATION_TIME = "$[invocationTime]";

	/**
	 * 用于写入方法进入消息的默认消息。
	 */
	private static final String DEFAULT_ENTER_MESSAGE = "Entering method '" +
			PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * 用于写入方法退出消息的默认消息。
	 */
	private static final String DEFAULT_EXIT_MESSAGE = "Exiting method '" +
			PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * 用于写入异常消息的默认消息。
	 */
	private static final String DEFAULT_EXCEPTION_MESSAGE = "Exception thrown in method '" +
			PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * 用于匹配占位符的 {@code Pattern}。
	 */
	private static final Pattern PATTERN = Pattern.compile("\\$\\[\\p{Alpha}+]");

	/**
	 * 允许的占位符的 {@code Set}。
	 */
	private static final Set<Object> ALLOWED_PLACEHOLDERS =
			new Constants(CustomizableTraceInterceptor.class).getValues("PLACEHOLDER_");


	/**
	 * 方法进入的消息。
	 */
	private String enterMessage = DEFAULT_ENTER_MESSAGE;

	/**
	 * 方法退出的消息。
	 */
	private String exitMessage = DEFAULT_EXIT_MESSAGE;

	/**
	 * 方法执行期间异常的消息。
	 */
	private String exceptionMessage = DEFAULT_EXCEPTION_MESSAGE;


	/**
	 * 设置用于方法进入日志消息的模板。
	 * 此模板可以包含以下任何占位符：
	 * <ul>
	 * <li>{@code $[targetClassName]}</li>
	 * <li>{@code $[targetClassShortName]}</li>
	 * <li>{@code $[argumentTypes]}</li>
	 * <li>{@code $[arguments]}</li>
	 * </ul>
	 */
	public void setEnterMessage(String enterMessage) throws IllegalArgumentException {
		Assert.hasText(enterMessage, "enterMessage must not be empty");
		checkForInvalidPlaceholders(enterMessage);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_RETURN_VALUE,
				"enterMessage cannot contain placeholder " + PLACEHOLDER_RETURN_VALUE);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_EXCEPTION,
				"enterMessage cannot contain placeholder " + PLACEHOLDER_EXCEPTION);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_INVOCATION_TIME,
				"enterMessage cannot contain placeholder " + PLACEHOLDER_INVOCATION_TIME);
		this.enterMessage = enterMessage;
	}

	/**
	 * 设置用于方法退出日志消息的模板。
	 * 此模板可以包含以下任何占位符：
	 * <ul>
	 * <li>{@code $[targetClassName]}</li>
	 * <li>{@code $[targetClassShortName]}</li>
	 * <li>{@code $[argumentTypes]}</li>
	 * <li>{@code $[arguments]}</li>
	 * <li>{@code $[returnValue]}</li>
	 * <li>{@code $[invocationTime]}</li>
	 * </ul>
	 */
	public void setExitMessage(String exitMessage) {
		Assert.hasText(exitMessage, "exitMessage must not be empty");
		checkForInvalidPlaceholders(exitMessage);
		Assert.doesNotContain(exitMessage, PLACEHOLDER_EXCEPTION,
				"exitMessage cannot contain placeholder" + PLACEHOLDER_EXCEPTION);
		this.exitMessage = exitMessage;
	}

	/**
	 * 设置用于方法异常日志消息的模板。
	 * 此模板可以包含以下任何占位符：
	 * <ul>
	 * <li>{@code $[targetClassName]}</li>
	 * <li>{@code $[targetClassShortName]}</li>
	 * <li>{@code $[argumentTypes]}</li>
	 * <li>{@code $[arguments]}</li>
	 * <li>{@code $[exception]}</li>
	 * </ul>
	 */
	public void setExceptionMessage(String exceptionMessage) {
		Assert.hasText(exceptionMessage, "exceptionMessage must not be empty");
		checkForInvalidPlaceholders(exceptionMessage);
		Assert.doesNotContain(exceptionMessage, PLACEHOLDER_RETURN_VALUE,
				"exceptionMessage cannot contain placeholder " + PLACEHOLDER_RETURN_VALUE);
		this.exceptionMessage = exceptionMessage;
	}


	/**
	 * 在调用之前根据 {@code enterMessage} 的值写入日志消息。
	 * 如果调用成功，则在退出时根据 {@code exitMessage} 的值写入日志消息。
	 * 如果在调用期间发生异常，则根据 {@code exceptionMessage} 的值写入消息。
	 * @see #setEnterMessage
	 * @see #setExitMessage
	 * @see #setExceptionMessage
	 */
	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		// 获取限定的方法名称
		String name = ClassUtils.getQualifiedMethodName(invocation.getMethod());
		StopWatch stopWatch = new StopWatch(name);
		Object returnValue = null;
		boolean exitThroughException = false;
		try {
			// 开启计时器
			stopWatch.start(name);
			// 写入Trace级别的日志
			writeToLog(logger,
					replacePlaceholders(this.enterMessage, invocation, null, null, -1));
			// 处理目标方法，并返回结果
			returnValue = invocation.proceed();
			return returnValue;
		}
		catch (Throwable ex) {
			if (stopWatch.isRunning()) {
				stopWatch.stop();
			}
			exitThroughException = true;
			writeToLog(logger, replacePlaceholders(
					this.exceptionMessage, invocation, null, ex, stopWatch.getTotalTimeMillis()), ex);
			throw ex;
		}
		finally {
			if (!exitThroughException) {
				if (stopWatch.isRunning()) {
					// 结束计时器
					stopWatch.stop();
				}
				// 将总耗时写入日志
				writeToLog(logger, replacePlaceholders(
						this.exitMessage, invocation, returnValue, null, stopWatch.getTotalTimeMillis()));
			}
		}
	}

	/**
	 * 用提供的值或从提供的值派生的值替换给定消息中的占位符。
	 * @param message 包含要替换的占位符的消息模板
	 * @param methodInvocation 正在记录的 {@code MethodInvocation}。
	 * 用于派生除 {@code $[exception]} 和 {@code $[returnValue]} 之外的所有占位符的值。
	 * @param returnValue 调用返回的任何值。
	 * 用于替换 {@code $[returnValue]} 占位符。可能为 {@code null}。
	 * @param throwable 调用期间引发的任何 {@code Throwable}。
	 * {@code Throwable.toString()} 的值被替换为 {@code $[exception]} 占位符。
	 * 可能为 {@code null}。
	 * @param invocationTime 要代替 {@code $[invocationTime]} 占位符写入的值
	 * @return 要写入日志的格式化输出
	 */
	protected String replacePlaceholders(String message, MethodInvocation methodInvocation,
			@Nullable Object returnValue, @Nullable Throwable throwable, long invocationTime) {

		Matcher matcher = PATTERN.matcher(message);
		Object target = methodInvocation.getThis();
		Assert.state(target != null, "Target must not be null");

		StringBuffer output = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group();
			if (PLACEHOLDER_METHOD_NAME.equals(match)) {
				matcher.appendReplacement(output, Matcher.quoteReplacement(methodInvocation.getMethod().getName()));
			}
			else if (PLACEHOLDER_TARGET_CLASS_NAME.equals(match)) {
				String className = getClassForLogging(target).getName();
				matcher.appendReplacement(output, Matcher.quoteReplacement(className));
			}
			else if (PLACEHOLDER_TARGET_CLASS_SHORT_NAME.equals(match)) {
				String shortName = ClassUtils.getShortName(getClassForLogging(target));
				matcher.appendReplacement(output, Matcher.quoteReplacement(shortName));
			}
			else if (PLACEHOLDER_ARGUMENTS.equals(match)) {
				matcher.appendReplacement(output,
						Matcher.quoteReplacement(StringUtils.arrayToCommaDelimitedString(methodInvocation.getArguments())));
			}
			else if (PLACEHOLDER_ARGUMENT_TYPES.equals(match)) {
				appendArgumentTypes(methodInvocation, matcher, output);
			}
			else if (PLACEHOLDER_RETURN_VALUE.equals(match)) {
				appendReturnValue(methodInvocation, matcher, output, returnValue);
			}
			else if (throwable != null && PLACEHOLDER_EXCEPTION.equals(match)) {
				matcher.appendReplacement(output, Matcher.quoteReplacement(throwable.toString()));
			}
			else if (PLACEHOLDER_INVOCATION_TIME.equals(match)) {
				matcher.appendReplacement(output, Long.toString(invocationTime));
			}
			else {
				// Should not happen since placeholders are checked earlier.
				throw new IllegalArgumentException("Unknown placeholder [" + match + "]");
			}
		}
		matcher.appendTail(output);

		return output.toString();
	}

	/**
	 * 将方法返回值的 {@code String} 表示添加到提供的 {@code StringBuffer}。
	 * 正确处理 {@code null} 和 {@code void} 结果。
	 * @param methodInvocation 返回值的 {@code MethodInvocation}
	 * @param matcher 包含匹配占位符的 {@code Matcher}
	 * @param output 要写入输出的 {@code StringBuffer}
	 * @param returnValue 方法调用返回的值。
	 */
	private void appendReturnValue(
			MethodInvocation methodInvocation, Matcher matcher, StringBuffer output, @Nullable Object returnValue) {

		if (methodInvocation.getMethod().getReturnType() == void.class) {
			matcher.appendReplacement(output, "void");
		}
		else if (returnValue == null) {
			matcher.appendReplacement(output, "null");
		}
		else {
			matcher.appendReplacement(output, Matcher.quoteReplacement(returnValue.toString()));
		}
	}

	/**
	 * 将方法参数类型的短 {@code Class} 名称的逗号分隔列表添加到输出中。
	 * 例如，如果方法的签名是 {@code put(java.lang.String, java.lang.Object)}，
	 * 则返回的值将是 {@code String, Object}。
	 * @param methodInvocation 正在记录的 {@code MethodInvocation}。
	 * 参数将从相应的 {@code Method} 中检索。
	 * @param matcher 包含输出状态的 {@code Matcher}
	 * @param output 包含输出的 {@code StringBuffer}
	 */
	private void appendArgumentTypes(MethodInvocation methodInvocation, Matcher matcher, StringBuffer output) {
		Class<?>[] argumentTypes = methodInvocation.getMethod().getParameterTypes();
		String[] argumentTypeShortNames = new String[argumentTypes.length];
		for (int i = 0; i < argumentTypeShortNames.length; i++) {
			argumentTypeShortNames[i] = ClassUtils.getShortName(argumentTypes[i]);
		}
		matcher.appendReplacement(output,
				Matcher.quoteReplacement(StringUtils.arrayToCommaDelimitedString(argumentTypeShortNames)));
	}

	/**
	 * 检查提供的 {@code String} 是否有任何未在此类中指定为常量的占位符，
	 * 如果有则抛出 {@code IllegalArgumentException}。
	 */
	private void checkForInvalidPlaceholders(String message) throws IllegalArgumentException {
		Matcher matcher = PATTERN.matcher(message);
		while (matcher.find()) {
			String match = matcher.group();
			if (!ALLOWED_PLACEHOLDERS.contains(match)) {
				throw new IllegalArgumentException("Placeholder [" + match + "] is not valid");
			}
		}
	}

}
