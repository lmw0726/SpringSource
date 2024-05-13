/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

/**
 * 一个方便的基类，用于 {@link ControllerAdvice @ControllerAdvice} 类，
 * 它希望通过 {@code @ExceptionHandler} 方法在所有 {@code @RequestMapping} 方法之间提供集中的异常处理。
 *
 * <p>这个基类提供了一个处理内部 Spring MVC 异常的 {@code @ExceptionHandler} 方法。
 * 这个方法返回一个 {@code ResponseEntity} 来写入响应，带有一个 {@link HttpMessageConverter 消息转换器}，
 * 与 {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver DefaultHandlerExceptionResolver}
 * 不同，它返回一个 {@link org.springframework.web.servlet.ModelAndView ModelAndView}。
 *
 * <p>如果没有必要将错误内容写入响应体，或者当使用视图解析（例如通过 {@code ContentNegotiatingViewResolver}）时，
 * 那么 {@code DefaultHandlerExceptionResolver} 就足够了。
 *
 * <p>请注意，为了检测到 {@code @ControllerAdvice} 子类，必须配置 {@link ExceptionHandlerExceptionResolver}。
 *
 * @author Rossen Stoyanchev
 * @see #handleException(Exception, WebRequest)
 * @see org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
 * @since 3.2
 */
public abstract class ResponseEntityExceptionHandler {

	/**
	 * 当没有为请求找到映射的处理程序时使用的日志类别。
	 *
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 当没有为请求找到映射的处理程序时使用的特定日志记录器。
	 *
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	/**
	 * 子类中使用的通用日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 为标准 Spring MVC 异常提供处理。
	 *
	 * @param ex      目标异常
	 * @param request 当前请求
	 */
	@ExceptionHandler({
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingPathVariableException.class,
			MissingServletRequestParameterException.class,
			ServletRequestBindingException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestPartException.class,
			BindException.class,
			NoHandlerFoundException.class,
			AsyncRequestTimeoutException.class
	})
	@Nullable
	public final ResponseEntity<Object> handleException(Exception ex, WebRequest request) throws Exception {
		HttpHeaders headers = new HttpHeaders();

		if (ex instanceof HttpRequestMethodNotSupportedException) {
			HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
			return handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, headers, status, request);
		} else if (ex instanceof HttpMediaTypeNotSupportedException) {
			HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			return handleHttpMediaTypeNotSupported((HttpMediaTypeNotSupportedException) ex, headers, status, request);
		} else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			HttpStatus status = HttpStatus.NOT_ACCEPTABLE;
			return handleHttpMediaTypeNotAcceptable((HttpMediaTypeNotAcceptableException) ex, headers, status, request);
		} else if (ex instanceof MissingPathVariableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleMissingPathVariable((MissingPathVariableException) ex, headers, status, request);
		} else if (ex instanceof MissingServletRequestParameterException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestParameter((MissingServletRequestParameterException) ex, headers, status, request);
		} else if (ex instanceof ServletRequestBindingException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleServletRequestBindingException((ServletRequestBindingException) ex, headers, status, request);
		} else if (ex instanceof ConversionNotSupportedException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, request);
		} else if (ex instanceof TypeMismatchException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleTypeMismatch((TypeMismatchException) ex, headers, status, request);
		} else if (ex instanceof HttpMessageNotReadableException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, request);
		} else if (ex instanceof HttpMessageNotWritableException) {
			HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
			return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, request);
		} else if (ex instanceof MethodArgumentNotValidException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, request);
		} else if (ex instanceof MissingServletRequestPartException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMissingServletRequestPart((MissingServletRequestPartException) ex, headers, status, request);
		} else if (ex instanceof BindException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleBindException((BindException) ex, headers, status, request);
		} else if (ex instanceof NoHandlerFoundException) {
			HttpStatus status = HttpStatus.NOT_FOUND;
			return handleNoHandlerFoundException((NoHandlerFoundException) ex, headers, status, request);
		} else if (ex instanceof AsyncRequestTimeoutException) {
			HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
			return handleAsyncRequestTimeoutException((AsyncRequestTimeoutException) ex, headers, status, request);
		} else {
			// 未知异常，通常是一个包装了常见 MVC 异常的包装器（因为 @ExceptionHandler 类型声明也会匹配一级原因）：
			// 我们只在这里处理顶级 MVC 异常，所以让我们将给定的异常重新抛出，以便通过 HandlerExceptionResolver 链进一步处理。
			throw ex;
		}
	}

	/**
	 * 自定义 HttpRequestMethodNotSupportedException 的响应。
	 * <p>该方法记录一个警告，设置 "Allow" 头，并委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		// 记录页面未找到异常的警告消息
		pageNotFoundLogger.warn(ex.getMessage());

		// 获取异常支持的 HTTP 方法集合
		Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
		// 如果支持的方法集合不为空
		if (!CollectionUtils.isEmpty(supportedMethods)) {
			// 设置响应头的 Allow 字段为支持的 HTTP 方法集合
			headers.setAllow(supportedMethods);
		}
		// 处理异常并生成 ResponseEntity 实例
		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMediaTypeNotSupportedException 的响应。
	 * <p>该方法设置 "Accept" 头，并委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		// 获取异常支持的媒体类型列表
		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		// 如果媒体类型列表不为空
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			// 设置响应头的 Accept 字段为支持的媒体类型列表
			headers.setAccept(mediaTypes);
			// 如果请求是 ServletWebRequest 的实例
			if (request instanceof ServletWebRequest) {
				// 将请求转换为 ServletWebRequest 类型
				ServletWebRequest servletWebRequest = (ServletWebRequest) request;
				// 如果请求方法是 PATCH
				if (HttpMethod.PATCH.equals(servletWebRequest.getHttpMethod())) {
					// 设置响应头的 Accept-Patch 字段为支持的媒体类型列表
					headers.setAcceptPatch(mediaTypes);
				}
			}
		}
		// 处理异常并生成 ResponseEntity 实例
		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMediaTypeNotAcceptableException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
			HttpMediaTypeNotAcceptableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingPathVariableException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 * @since 4.2
	 */
	protected ResponseEntity<Object> handleMissingPathVariable(
			MissingPathVariableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingServletRequestParameterException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 ServletRequestBindingException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleServletRequestBindingException(
			ServletRequestBindingException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 ConversionNotSupportedException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleConversionNotSupported(
			ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 TypeMismatchException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMessageNotReadableException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 HttpMessageNotWritableException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleHttpMessageNotWritable(
			HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MethodArgumentNotValidException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 MissingServletRequestPartException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleMissingServletRequestPart(
			MissingServletRequestPartException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 BindException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 */
	protected ResponseEntity<Object> handleBindException(
			BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 NoHandlerFoundException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex      异常
	 * @param headers 要写入响应的头
	 * @param status  选择的响应状态
	 * @param request 当前请求
	 * @return ResponseEntity 实例
	 * @since 4.0
	 */
	protected ResponseEntity<Object> handleNoHandlerFoundException(
			NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	/**
	 * 自定义 AsyncRequestTimeoutException 的响应。
	 * <p>该方法委托给 {@link #handleExceptionInternal}。
	 *
	 * @param ex         异常
	 * @param headers    要写入响应的头
	 * @param status     选择的响应状态
	 * @param webRequest 当前请求
	 * @return ResponseEntity 实例
	 * @since 4.2.8
	 */
	@Nullable
	protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
			AsyncRequestTimeoutException ex, HttpHeaders headers, HttpStatus status, WebRequest webRequest) {

		// 如果 WebRequest 是 ServletWebRequest 的实例
		if (webRequest instanceof ServletWebRequest) {
			// 将 WebRequest 转换为 ServletWebRequest 类型
			ServletWebRequest servletWebRequest = (ServletWebRequest) webRequest;
			// 获取 HttpServletResponse 实例
			HttpServletResponse response = servletWebRequest.getResponse();
			// 如果响应不为空且已提交
			if (response != null && response.isCommitted()) {
				// 如果日志记录级别为 WARN
				if (logger.isWarnEnabled()) {
					// 记录异步请求超时的警告日志
					logger.warn("Async request timed out");
				}
				// 返回空值
				return null;
			}
		}
		// 处理异常并生成 ResponseEntity 实例
		return handleExceptionInternal(ex, null, headers, status, webRequest);
	}

	/**
	 * 对所有异常类型的响应体进行自定义。
	 * <p>默认实现设置 {@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE} 请求属性，并从给定的
	 * body、headers 和 status 创建一个 {@link ResponseEntity}。
	 *
	 * @param ex      异常
	 * @param body    响应体
	 * @param headers 响应头
	 * @param status  响应状态
	 * @param request 当前请求
	 */
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

		// 如果状态码为内部服务器错误（500），则将异常设置为请求属性中的错误异常属性
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		}
		// 返回一个包含给定正文、标头和状态码的 ResponseEntity 实例
		return new ResponseEntity<>(body, headers, status);
	}

}
