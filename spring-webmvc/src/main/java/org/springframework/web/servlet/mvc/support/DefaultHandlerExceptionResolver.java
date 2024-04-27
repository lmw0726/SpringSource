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

package org.springframework.web.servlet.mvc.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver} 接口的默认实现，
 * 解决标准的 Spring MVC 异常并将它们转换为相应的 HTTP 状态码。
 *
 * <p>此异常解析器在通用的 Spring {@link org.springframework.web.servlet.DispatcherServlet} 中默认启用。
 *
 * <table>
 * <caption>支持的异常</caption>
 * <thead>
 * <tr>
 * <th class="colFirst">异常</th>
 * <th class="colLast">HTTP 状态码</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr class="altColor">
 * <td><p>HttpRequestMethodNotSupportedException</p></td>
 * <td><p>405 (SC_METHOD_NOT_ALLOWED)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HttpMediaTypeNotSupportedException</p></td>
 * <td><p>415 (SC_UNSUPPORTED_MEDIA_TYPE)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>HttpMediaTypeNotAcceptableException</p></td>
 * <td><p>406 (SC_NOT_ACCEPTABLE)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>MissingPathVariableException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>MissingServletRequestParameterException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>ServletRequestBindingException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>ConversionNotSupportedException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>TypeMismatchException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>HttpMessageNotReadableException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>HttpMessageNotWritableException</p></td>
 * <td><p>500 (SC_INTERNAL_SERVER_ERROR)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>MethodArgumentNotValidException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>MissingServletRequestPartException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>BindException</p></td>
 * <td><p>400 (SC_BAD_REQUEST)</p></td>
 * </tr>
 * <tr class="rowColor">
 * <td><p>NoHandlerFoundException</p></td>
 * <td><p>404 (SC_NOT_FOUND)</p></td>
 * </tr>
 * <tr class="altColor">
 * <td><p>AsyncRequestTimeoutException</p></td>
 * <td><p>503 (SC_SERVICE_UNAVAILABLE)</p></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
 * @since 3.0
 */
public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 当找不到请求的映射处理程序时要使用的日志类别。
	 *
	 * @see #pageNotFoundLogger
	 */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * 找不到请求的映射处理程序时要使用的额外日志记录器。
	 *
	 * @see #PAGE_NOT_FOUND_LOG_CATEGORY
	 */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);


	/**
	 * 将 {@linkplain #setOrder(int) order} 设置为 {@link #LOWEST_PRECEDENCE}。
	 */
	public DefaultHandlerExceptionResolver() {
		// 设置为最低优先级
		setOrder(Ordered.LOWEST_PRECEDENCE);
		// 设置为默认的日志类别
		setWarnLogCategory(getClass().getName());
	}


	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		try {
			if (ex instanceof HttpRequestMethodNotSupportedException) {
				return handleHttpRequestMethodNotSupported(
						(HttpRequestMethodNotSupportedException) ex, request, response, handler);
			} else if (ex instanceof HttpMediaTypeNotSupportedException) {
				return handleHttpMediaTypeNotSupported(
						(HttpMediaTypeNotSupportedException) ex, request, response, handler);
			} else if (ex instanceof HttpMediaTypeNotAcceptableException) {
				return handleHttpMediaTypeNotAcceptable(
						(HttpMediaTypeNotAcceptableException) ex, request, response, handler);
			} else if (ex instanceof MissingPathVariableException) {
				return handleMissingPathVariable(
						(MissingPathVariableException) ex, request, response, handler);
			} else if (ex instanceof MissingServletRequestParameterException) {
				return handleMissingServletRequestParameter(
						(MissingServletRequestParameterException) ex, request, response, handler);
			} else if (ex instanceof ServletRequestBindingException) {
				return handleServletRequestBindingException(
						(ServletRequestBindingException) ex, request, response, handler);
			} else if (ex instanceof ConversionNotSupportedException) {
				return handleConversionNotSupported(
						(ConversionNotSupportedException) ex, request, response, handler);
			} else if (ex instanceof TypeMismatchException) {
				return handleTypeMismatch(
						(TypeMismatchException) ex, request, response, handler);
			} else if (ex instanceof HttpMessageNotReadableException) {
				return handleHttpMessageNotReadable(
						(HttpMessageNotReadableException) ex, request, response, handler);
			} else if (ex instanceof HttpMessageNotWritableException) {
				return handleHttpMessageNotWritable(
						(HttpMessageNotWritableException) ex, request, response, handler);
			} else if (ex instanceof MethodArgumentNotValidException) {
				return handleMethodArgumentNotValidException(
						(MethodArgumentNotValidException) ex, request, response, handler);
			} else if (ex instanceof MissingServletRequestPartException) {
				return handleMissingServletRequestPartException(
						(MissingServletRequestPartException) ex, request, response, handler);
			} else if (ex instanceof BindException) {
				return handleBindException((BindException) ex, request, response, handler);
			} else if (ex instanceof NoHandlerFoundException) {
				return handleNoHandlerFoundException(
						(NoHandlerFoundException) ex, request, response, handler);
			} else if (ex instanceof AsyncRequestTimeoutException) {
				return handleAsyncRequestTimeoutException(
						(AsyncRequestTimeoutException) ex, request, response, handler);
			}
		} catch (Exception handlerEx) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", handlerEx);
			}
		}
		return null;
	}

	/**
	 * 处理特定 HTTP 请求方法的情况下找不到请求处理程序方法的情况。
	 * <p>默认实现记录警告，发送 HTTP 405 错误，设置 "Allow" 头，并返回一个空的 {@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将 HttpRequestMethodNotSupportedException 原样重新抛出。
	 *
	 * @param ex       要处理的 HttpRequestMethodNotSupportedException
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler  执行的处理程序，如果在异常发生时没有选择任何处理程序，则为 {@code null}
	 *                 （例如，如果多部分解析失败）
	 * @return 表示已处理异常的空 ModelAndView
	 * @throws IOException 可能由 {@link HttpServletResponse#sendError} 抛出
	 */
	protected ModelAndView handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
															   HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		String[] supportedMethods = ex.getSupportedMethods();
		if (supportedMethods != null) {
			// 设置允许的方法
			response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
		}
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理没有找到 PUT 或 POST 内容的 {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器} 的情况。
	 * <p>默认实现发送 HTTP 415 错误，设置 "Accept" 头，并返回一个空的 {@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将 HttpMediaTypeNotSupportedException 原样重新抛出。
	 *
	 * @param ex       要处理的 HttpMediaTypeNotSupportedException
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler  执行的处理程序
	 * @return 表示已处理异常的空 ModelAndView
	 * @throws IOException 可能由 {@link HttpServletResponse#sendError} 抛出
	 */
	protected ModelAndView handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
														   HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {
		// 获取支持的媒体类型
		List<MediaType> mediaTypes = ex.getSupportedMediaTypes();
		// 如果媒体类型集合不为空
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			// 设置响应头中的 Accept 字段为媒体类型集合的字符串表示
			response.setHeader("Accept", MediaType.toString(mediaTypes));
			// 如果请求方法为 PATCH
			if (request.getMethod().equals("PATCH")) {
				// 设置响应头中的 Accept-Patch 字段为媒体类型集合的字符串表示
				response.setHeader("Accept-Patch", MediaType.toString(mediaTypes));
			}
		}
		// 发送 415 不支持的媒体类型错误
		response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		// 返回一个空的 ModelAndView 对象
		return new ModelAndView();

	}

	/**
	 * 处理找不到适用于客户端的{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}（通过{@code Accept}头表达）的情况。
	 * <p>默认实现发送HTTP 406错误并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将HttpMediaTypeNotAcceptableException原样重新抛出。
	 *
	 * @param ex       要处理的HttpMediaTypeNotAcceptableException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
															HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
		return new ModelAndView();
	}

	/**
	 * 处理声明的路径变量与任何提取的URI变量不匹配的情况。
	 * <p>默认实现发送HTTP 500错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将MissingPathVariableException原样重新抛出。
	 *
	 * @param ex       要处理的MissingPathVariableException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 * @since 4.2
	 */
	protected ModelAndView handleMissingPathVariable(MissingPathVariableException ex,
													 HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理参数丢失的情况。
	 * <p>默认实现发送HTTP 400错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将MissingServletRequestParameterException原样重新抛出。
	 *
	 * @param ex       要处理的MissingServletRequestParameterException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
																HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理发生不可恢复的绑定异常的情况 - 例如，需要的标头，需要的cookie。
	 * <p>默认实现发送HTTP 400错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者原样抛出异常。
	 *
	 * @param ex       要处理的异常
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleServletRequestBindingException(ServletRequestBindingException ex,
																HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理{@link org.springframework.web.bind.WebDataBinder}转换无法发生的情况。
	 * <p>默认实现发送HTTP 500错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将ConversionNotSupportedException原样重新抛出。
	 *
	 * @param ex       要处理的ConversionNotSupportedException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleConversionNotSupported(ConversionNotSupportedException ex,
														HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * 处理{@link org.springframework.web.bind.WebDataBinder}转换错误发生的情况。
	 * <p>默认实现发送HTTP 400错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将TypeMismatchException原样重新抛出。
	 *
	 * @param ex       要处理的TypeMismatchException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
											  HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
	 * 无法从HTTP请求中读取的情况。
	 * <p>默认实现发送HTTP 400错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将HttpMessageNotReadableException原样重新抛出。
	 *
	 * @param ex       要处理的HttpMessageNotReadableException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
														HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain org.springframework.http.converter.HttpMessageConverter message converter}
	 * 无法向HTTP请求写入的情况。
	 * <p>默认实现发送HTTP 500错误，并返回一个空的{@code ModelAndView}。
	 * 或者，可以选择一个备用视图，或者将HttpMessageNotWritableException原样重新抛出。
	 *
	 * @param ex       要处理的HttpMessageNotWritableException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
														HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		sendServerError(ex, request, response);
		return new ModelAndView();
	}

	/**
	 * 处理使用{@code @Valid}注解的参数（例如{@link RequestBody}或{@link RequestPart}参数）验证失败的情况。
	 * <p>默认情况下，会向客户端发送HTTP 400错误。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
																 HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain RequestPart @RequestPart}、{@link MultipartFile}
	 * 或{@code javax.servlet.http.Part}参数是必需但缺失的情况。
	 * <p>默认情况下，会向客户端发送HTTP 400错误。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleMissingServletRequestPartException(MissingServletRequestPartException ex,
																	HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
		return new ModelAndView();
	}

	/**
	 * 处理{@linkplain ModelAttribute @ModelAttribute}方法参数绑定或验证错误，并且后面没有类型为{@link BindingResult}的其他方法参数的情况。
	 * <p>默认情况下，会向客户端发送HTTP 400错误。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 */
	protected ModelAndView handleBindException(BindException ex, HttpServletRequest request,
											   HttpServletResponse response, @Nullable Object handler) throws IOException {

		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return new ModelAndView();
	}

	/**
	 * 处理调度过程中未找到处理程序的情况。
	 * <p>默认实现发送HTTP 404错误并返回一个空的{@code ModelAndView}。或者，可以选择一个备用视图，或者将NoHandlerFoundException原样重新抛出。
	 *
	 * @param ex       要处理的NoHandlerFoundException
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序，如果在异常发生时没有选择任何处理程序，则为{@code null}（例如，如果多部分解析失败）
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 * @since 4.0
	 */
	protected ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex,
														 HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		pageNotFoundLogger.warn(ex.getMessage());
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		return new ModelAndView();
	}

	/**
	 * 处理异步请求超时的情况。
	 * <p>默认实现发送HTTP 503错误。
	 *
	 * @param ex       要处理的{@link AsyncRequestTimeoutException}
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应
	 * @param handler  执行的处理程序，如果在异常发生时没有选择任何处理程序，则为{@code null}（例如，如果多部分解析失败）
	 * @return 一个空的ModelAndView，表示已处理异常
	 * @throws IOException 可能从{@link HttpServletResponse#sendError}抛出
	 * @since 4.2.8
	 */
	protected ModelAndView handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
															  HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

		if (!response.isCommitted()) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		} else {
			logger.warn("Async request timed out");
		}
		return new ModelAndView();
	}

	/**
	 * 调用以发送服务器错误。将状态设置为500，并将请求属性"javax.servlet.error.exception"设置为异常。
	 */
	protected void sendServerError(Exception ex, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		request.setAttribute("javax.servlet.error.exception", ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

}
