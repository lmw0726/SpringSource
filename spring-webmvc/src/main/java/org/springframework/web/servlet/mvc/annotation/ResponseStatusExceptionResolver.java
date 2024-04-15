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

package org.springframework.web.servlet.mvc.annotation;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 一个 {@link org.springframework.web.servlet.HandlerExceptionResolver
 * HandlerExceptionResolver}，它使用 {@link ResponseStatus @ResponseStatus}
 * 注解将异常映射到 HTTP 状态码。
 *
 * <p>这个异常解析器在 {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}、
 * MVC Java 配置和 MVC 命名空间中默认启用。
 *
 * <p>从 4.2 版本开始，该解析器还会递归查找异常原因上是否存在 {@code @ResponseStatus}，
 * 从 4.2.2 版本开始，该解析器还支持自定义组合注解中的 {@code @ResponseStatus} 的属性重写。
 *
 * <p>从 5.0 版本开始，该解析器还支持 {@link ResponseStatusException}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see ResponseStatus
 * @see ResponseStatusException
 * @since 3.0
 */
public class ResponseStatusExceptionResolver extends AbstractHandlerExceptionResolver implements MessageSourceAware {

	/**
	 * 消息源
	 */
	@Nullable
	private MessageSource messageSource;


	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}


	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		try {
			if (ex instanceof ResponseStatusException) {
				// 如果异常是 ResponseStatusException 类型的，则解析 ResponseStatusException
				return resolveResponseStatusException((ResponseStatusException) ex, request, response, handler);
			}

			// 查找异常类上是否有 ResponseStatus 注解
			ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
			if (status != null) {
				// 如果找到了 ResponseStatus 注解，则解析 ResponseStatus
				return resolveResponseStatus(status, request, response, handler, ex);
			}

			if (ex.getCause() instanceof Exception) {
				// 如果异常的原因是另一个异常，则递归解析
				return doResolveException(request, response, handler, (Exception) ex.getCause());
			}
		} catch (Exception resolveEx) {
			// 解析异常时出现异常，记录警告日志
			if (logger.isWarnEnabled()) {
				logger.warn("Failure while trying to resolve exception [" + ex.getClass().getName() + "]", resolveEx);
			}
		}
		// 默认情况下返回 null
		return null;
	}

	/**
	 * 处理 {@link ResponseStatus @ResponseStatus} 注解的模板方法。
	 * <p>默认实现委托给 {@link #applyStatusAndReason} 方法，使用注解中的状态码和原因。
	 *
	 * @param responseStatus {@code @ResponseStatus} 注解
	 * @param request        当前 HTTP 请求
	 * @param response       当前 HTTP 响应
	 * @param handler        执行的处理程序，如果在异常发生时没有选择处理程序，则为 {@code null}，例如，如果多部分解析失败
	 * @param ex             异常
	 * @return 一个空的 ModelAndView，即异常已解决
	 */
	protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
												 HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {
		// 获取状态码
		int statusCode = responseStatus.code().value();
		// 获取原因
		String reason = responseStatus.reason();
		// 将状态码和原因写入到Http响应中
		return applyStatusAndReason(statusCode, reason, response);
	}

	/**
	 * 处理 {@link ResponseStatusException} 的模板方法。
	 * <p>默认实现应用 {@link ResponseStatusException#getResponseHeaders()} 中的头，并委托给 {@link #applyStatusAndReason}
	 * 使用异常中的状态码和原因。
	 *
	 * @param ex       异常
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler  执行的处理程序，如果在异常发生时没有选择处理程序，则为 {@code null}，例如，如果多部分解析失败
	 * @return 一个空的 ModelAndView，即异常已解决
	 * @since 5.0
	 */
	protected ModelAndView resolveResponseStatusException(ResponseStatusException ex,
														  HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws Exception {

		// 遍历异常的响应头
		ex.getResponseHeaders().forEach((name, values) ->
				// 将异常信息中每个响应头的值添加到Http响应请求头中
				values.forEach(value -> response.addHeader(name, value)));

		// 将状态码和原因写入到Http响应中
		return applyStatusAndReason(ex.getRawStatusCode(), ex.getReason(), response);
	}

	/**
	 * 将解析后的状态码和原因应用到响应中。
	 * <p>默认实现使用 {@link HttpServletResponse#sendError(int)} 或 {@link HttpServletResponse#sendError(int, String)} 发送响应错误，
	 * 如果有原因，则返回一个空的 ModelAndView。
	 *
	 * @param statusCode HTTP 状态码
	 * @param reason     相关原因（可能为 {@code null} 或空）
	 * @param response   当前 HTTP 响应
	 * @since 5.0
	 */
	protected ModelAndView applyStatusAndReason(int statusCode, @Nullable String reason, HttpServletResponse response)
			throws IOException {

		if (!StringUtils.hasLength(reason)) {
			// 如果原因为空，则发送错误状态码
			response.sendError(statusCode);
		} else {
			// 如果原因不为空，则解析原因并发送错误状态码
			String resolvedReason = (this.messageSource != null ?
					// 使用消息源解析原因，转换成国际化消息
					this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale()) :
					reason);
			response.sendError(statusCode, resolvedReason);
		}
		// 返回一个空的 ModelAndView 对象
		return new ModelAndView();
	}

}
