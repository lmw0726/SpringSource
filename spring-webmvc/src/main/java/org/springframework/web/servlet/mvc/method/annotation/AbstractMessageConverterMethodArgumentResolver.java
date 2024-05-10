/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 通过使用 {@link HttpMessageConverter HttpMessageConverters} 从请求体中读取方法参数值的基类。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {
	/**
	 * 支持的 HTTP 方法集合（POST、PUT、PATCH）
	 */
	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

	/**
	 * 默认请求体值
	 */
	private static final Object NO_VALUE = new Object();

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Http消息转换器列表
	 */
	protected final List<HttpMessageConverter<?>> messageConverters;

	/**
	 * 请求响应体建言链
	 */
	private final RequestResponseBodyAdviceChain advice;


	/**
	 * 仅使用转换器的基本构造函数。
	 */
	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
		this(converters, null);
	}

	/**
	 * 带有转换器和 {@code Request~} 和 {@code ResponseBodyAdvice} 的构造函数。
	 *
	 * @since 4.2
	 */
	public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters,
														  @Nullable List<Object> requestResponseBodyAdvice) {

		Assert.notEmpty(converters, "'messageConverters' must not be empty");
		this.messageConverters = converters;
		this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
	}


	/**
	 * 返回配置的 {@link RequestBodyAdvice} 和
	 * {@link RequestBodyAdvice}，其中每个实例可能被包装为
	 * {@link org.springframework.web.method.ControllerAdviceBean ControllerAdviceBean}。
	 */
	RequestResponseBodyAdviceChain getAdvice() {
		return this.advice;
	}

	/**
	 * 通过从给定的请求中读取创建预期参数类型的方法参数值。
	 *
	 * @param <T>        要创建的参数值的预期类型
	 * @param webRequest 当前请求
	 * @param parameter  方法参数描述符（可能为 {@code null}）
	 * @param paramType  要创建的参数值的类型
	 * @return 创建的方法参数值
	 * @throws IOException                        如果从请求中读取失败
	 * @throws HttpMediaTypeNotSupportedException 如果找不到适合的消息转换器
	 */
	@Nullable
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter,
												   Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		// 创建 HTTP 输入消息对象
		HttpInputMessage inputMessage = createInputMessage(webRequest);
		// 调用 readWithMessageConverters 方法读取消息体内容
		return readWithMessageConverters(inputMessage, parameter, paramType);
	}

	/**
	 * 通过从给定的 HttpInputMessage 中读取创建预期参数类型的方法参数值。
	 *
	 * @param <T>          要创建的参数值的预期类型
	 * @param inputMessage 表示当前请求的 HTTP 输入消息
	 * @param parameter    方法参数描述符
	 * @param targetType   目标类型，不一定与方法参数类型相同，例如 {@code HttpEntity<String>}。
	 * @return 创建的方法参数值
	 * @throws IOException                        如果从请求中读取失败
	 * @throws HttpMediaTypeNotSupportedException 如果找不到适合的消息转换器
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter,
												   Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {

		// 声明变量用于存储内容类型
		MediaType contentType;
		// 标记是否存在内容类型，默认为 false
		boolean noContentType = false;
		try {
			// 从输入消息中获取内容类型
			contentType = inputMessage.getHeaders().getContentType();
		} catch (InvalidMediaTypeException ex) {
			// 捕获异常并抛出对应的媒体类型不支持异常
			throw new HttpMediaTypeNotSupportedException(ex.getMessage());
		}
		// 如果内容类型为 null
		if (contentType == null) {
			// 标记不存在内容类型
			noContentType = true;
			// 默认将内容类型设置为 APPLICATION_OCTET_STREAM
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		// 获取方法参数所属的类
		Class<?> contextClass = parameter.getContainingClass();
		// 声明目标类，默认为 null
		Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
		// 如果目标类为 null
		if (targetClass == null) {
			// 解析方法参数的可解析类型
			ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
			// 获取目标类
			targetClass = (Class<T>) resolvableType.resolve();
		}

		// 获取请求方法类型
		HttpMethod httpMethod = (inputMessage instanceof HttpRequest ? ((HttpRequest) inputMessage).getMethod() : null);
		// 初始化 响应体，默认为 NO_VALUE
		Object body = NO_VALUE;

		// 初始化空体检查的 HTTP 输入消息
		EmptyBodyCheckingHttpInputMessage message = null;
		try {
			// 实例化一个空体检查的 HTTP 输入消息对象
			message = new EmptyBodyCheckingHttpInputMessage(inputMessage);

			// 遍历消息转换器列表
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				// 获取当前转换器的类型
				Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
				// 判断当前转换器是否为通用 HTTP 消息转换器
				GenericHttpMessageConverter<?> genericConverter =
						(converter instanceof GenericHttpMessageConverter ? (GenericHttpMessageConverter<?>) converter : null);
				// 如果当前转换器是通用 HTTP 消息转换器，则调用通用转换器的 canRead 方法，判断是否可以读取
				// 否则调用转换器的 canRead 方法，判断是否可以读取
				if (genericConverter != null ? genericConverter.canRead(targetType, contextClass, contentType) :
						(targetClass != null && converter.canRead(targetClass, contentType))) {
					// 如果消息体存在
					if (message.hasBody()) {
						// 在读取消息体前，调用拦截器的 beforeBodyRead 方法
						HttpInputMessage msgToUse = getAdvice().beforeBodyRead(message, parameter, targetType, converterType);
						// 读取消息体并将其转换为对应的对象
						body = (genericConverter != null ? genericConverter.read(targetType, contextClass, msgToUse) :
								((HttpMessageConverter<T>) converter).read(targetClass, msgToUse));
						// 在读取消息体后，调用拦截器的 afterBodyRead 方法
						body = getAdvice().afterBodyRead(body, msgToUse, parameter, targetType, converterType);
					} else {
						// 如果消息体不存在，调用拦截器的 handleEmptyBody 方法
						body = getAdvice().handleEmptyBody(null, message, parameter, targetType, converterType);
					}
					// 跳出循环
					break;
				}
			}
		} catch (IOException ex) {
			// 在读取输入消息时出现 I/O 错误，抛出 HTTP 消息不可读异常
			throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
		} finally {
			// 如果消息不为空，关闭消息体流
			if (message != null && message.hasBody()) {
				closeStreamIfNecessary(message.getBody());
			}
		}

		// 如果 响应体 仍然为 NO_VALUE
		if (body == NO_VALUE) {
			// 如果请求方法为空，或不支持的方法集合中不包含该方法，或者（不存在内容类型且消息没有消息体）
			if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) ||
					(noContentType && !message.hasBody())) {
				// 返回 null
				return null;
			}
			// 抛出不支持的媒体类型异常
			throw new HttpMediaTypeNotSupportedException(contentType,
					getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));
		}

		// 设置选定的内容类型为实际内容类型
		MediaType selectedContentType = contentType;
		Object theBody = body;
		// 调试日志输出
		LogFormatUtils.traceDebug(logger, traceOn -> {
			String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
			return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
		});

		// 返回读取的消息体
		return body;
	}

	/**
	 * 从给定的 {@link NativeWebRequest} 创建一个新的 {@link HttpInputMessage}。
	 *
	 * @param webRequest 要创建输入消息的 Web 请求
	 * @return 输入消息
	 */
	protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
		// 获取Http请求
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(servletRequest != null, "No HttpServletRequest");
		// 使用Http请求创建ServletServerHttpRequest
		return new ServletServerHttpRequest(servletRequest);
	}

	/**
	 * 如果适用，则验证绑定目标。
	 * <p>默认实现检查 {@code @javax.validation.Valid}、Spring 的 {@link org.springframework.validation.annotation.Validated}
	 * 以及名称以 "Valid" 开头的自定义注释。
	 *
	 * @param binder    要使用的数据绑定器
	 * @param parameter 方法参数描述符
	 * @see #isBindExceptionRequired
	 * @since 4.1.5
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		// 获取方法参数上的所有注解
		Annotation[] annotations = parameter.getParameterAnnotations();
		// 遍历每个注解
		for (Annotation ann : annotations) {
			// 获取验证提示信息（validation hints）
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			// 如果验证提示信息不为空
			if (validationHints != null) {
				// 使用验证提示信息进行数据校验
				binder.validate(validationHints);
				// 执行一次后立即跳出循环
				break;
			}
		}
	}

	/**
	 * 是否在验证错误时引发致命的绑定异常。
	 *
	 * @param binder    要执行数据绑定的数据绑定器
	 * @param parameter 方法参数描述符
	 * @return {@code true} 如果下一个方法参数不是 {@link Errors} 类型
	 * @since 4.1.5
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		// 获取参数在方法参数列表中的索引
		int i = parameter.getParameterIndex();
		// 获取方法参数列表中的参数类型数组
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		// 判断方法参数列表中是否存在绑定结果对象（Errors），并且当前参数不是最后一个参数
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		// 返回是否需要抛出绑定异常的标志（如果下一个参数不是绑定结果对象，则需要抛出异常）
		return !hasBindingResult;
	}

	/**
	 * 返回由所有提供的消息转换器支持的媒体类型，按特异性排序通过 {@link MediaType#sortBySpecificity(List)}。
	 *
	 * @since 5.3.4
	 */
	protected List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		// 创建媒体类型集合
		Set<MediaType> mediaTypeSet = new LinkedHashSet<>();
		// 遍历消息转换器列表
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			// 将每个转换器支持的媒体类型添加到集合中
			mediaTypeSet.addAll(converter.getSupportedMediaTypes(clazz));
		}
		// 将集合转换为列表
		List<MediaType> result = new ArrayList<>(mediaTypeSet);
		// 根据特异性对媒体类型进行排序
		MediaType.sortBySpecificity(result);
		// 返回排序后的媒体类型列表
		return result;
	}

	/**
	 * 必要时针对方法参数适应给定的参数。
	 *
	 * @param arg       已解析的参数
	 * @param parameter 方法参数描述符
	 * @return 适应的参数，或原始已解析的参数
	 * @since 4.3.5
	 */
	@Nullable
	protected Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {
		// 如果参数类型为Optional
		if (parameter.getParameterType() == Optional.class) {
			// 如果参数为空，或者参数为集合且为空，或者参数为对象数组且长度为0，则返回空的Optional
			if (arg == null || (arg instanceof Collection && ((Collection<?>) arg).isEmpty()) ||
					(arg instanceof Object[] && ((Object[]) arg).length == 0)) {
				return Optional.empty();
			} else {
				// 否则返回包含参数值的Optional
				return Optional.of(arg);
			}
		}
		// 否则返回参数值
		return arg;
	}

	/**
	 * 必要时关闭请求体流，例如在多部分请求中的部分流。
	 */
	void closeStreamIfNecessary(InputStream body) {
		// 默认情况下无操作：标准的 HttpInputMessage 公开 HTTP 请求流
		// (ServletRequest#getInputStream)，其生命周期由容器管理。
	}


	private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {
		/**
		 * Http请求头
		 */
		private final HttpHeaders headers;

		/**
		 * 输入流
		 */
		@Nullable
		private final InputStream body;

		public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
			// 获取输入消息的头部
			this.headers = inputMessage.getHeaders();
			// 获取输入消息的主体输入流
			InputStream inputStream = inputMessage.getBody();
			// 如果输入流支持标记
			if (inputStream.markSupported()) {
				// 标记输入流的当前位置
				inputStream.mark(1);
				// 读取一个字节，如果不是EOF，则输入流不为空，否则为空
				this.body = (inputStream.read() != -1 ? inputStream : null);
				// 重置输入流到之前标记的位置
				inputStream.reset();
			} else {
				// 如果输入流不支持标记，创建PushbackInputStream对象
				PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
				// 读取一个字节
				int b = pushbackInputStream.read();
				// 如果读取到的是EOF，主体为空
				if (b == -1) {
					this.body = null;
				} else {
					// 否则将PushbackInputStream对象设为主体
					this.body = pushbackInputStream;
					// 并将之前读取的字节推回流中
					pushbackInputStream.unread(b);
				}
			}
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public InputStream getBody() {
			return (this.body != null ? this.body : StreamUtils.emptyInput());
		}

		public boolean hasBody() {
			return (this.body != null);
		}
	}

}
