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

package org.springframework.web.servlet.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * {@link HandlerExceptionResolver} 实现的抽象基类。
 *
 * <p>支持映射的 {@linkplain #setMappedHandlers handlers} 和
 * {@linkplain #setMappedHandlerClasses handler 类}，它们应用于解析器并实现了 {@link Ordered} 接口。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {
	/**
	 * Cache-Control 请求头
	 */
	private static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/**
	 * 供子类使用的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * 排序
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;
	/**
	 * 异常解析器应用的处理程序集合
	 */
	@Nullable
	private Set<?> mappedHandlers;

	/**
	 * 异常解析器应用的类集合。
	 */
	@Nullable
	private Class<?>[] mappedHandlerClasses;
	/**
	 * 警告日志记录器
	 */
	@Nullable
	private Log warnLogger;
	/**
	 * 是否阻止由此异常解析器解析的任何视图的 HTTP 响应缓存。
	 */
	private boolean preventResponseCaching = false;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定此异常解析器应用的处理程序集合。
	 * <p>异常映射和默认错误视图仅适用于指定的处理程序。
	 * <p>如果未设置处理程序或处理程序类，则异常映射和默认错误视图将应用于所有处理程序。
	 * 这意味着指定的默认错误视图将用作所有异常的后备；
	 * 在这种情况下，链中的任何进一步的 HandlerExceptionResolver 都将被忽略。
	 */
	public void setMappedHandlers(Set<?> mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * 指定此异常解析器应用的类集合。
	 * <p>异常映射和默认错误视图仅适用于指定类型的处理程序；指定的类型也可以是处理程序的接口或超类。
	 * <p>如果未设置处理程序或处理程序类，则异常映射和默认错误视图将应用于所有处理程序。
	 * 这意味着指定的默认错误视图将用作所有异常的后备；在这种情况下，链中的任何进一步的 HandlerExceptionResolver 都将被忽略。
	 */
	public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
	}

	/**
	 * 设置警告日志记录的日志类别。该名称将通过 Commons Logging 传递给底层的日志记录器实现，
	 * 根据日志记录器的配置被解释为日志类别。如果传递了 {@code null} 或空字符串，则警告日志记录将被关闭。
	 * <p>默认情况下没有警告日志记录，尽管诸如 {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver} 这样的子类可以更改该默认值。
	 * 指定此设置以激活将警告日志记录到特定类别。或者，覆盖 {@link #logException} 方法进行自定义日志记录。
	 *
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = (StringUtils.hasLength(loggerName) ? LogFactory.getLog(loggerName) : null);
	}

	/**
	 * 指定是否阻止由此异常解析器解析的任何视图的 HTTP 响应缓存。
	 * <p>默认值为 {@code false}。将其切换为 {@code true}，以便自动生成抑制响应缓存的 HTTP 响应头。
	 */
	public void setPreventResponseCaching(boolean preventResponseCaching) {
		this.preventResponseCaching = preventResponseCaching;
	}


	/**
	 * 检查此解析器是否应用（即是否提供的处理程序与配置的 {@linkplain #setMappedHandlers handlers}
	 * 或 {@linkplain #setMappedHandlerClasses handler 类} 匹配），然后委托给 {@link #doResolveException} 模板方法。
	 */
	@Override
	@Nullable
	public ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		if (shouldApplyTo(request, handler)) {
			// 如果应该应用异常解析器处理当前请求和处理程序
			// 准备响应
			prepareResponse(ex, response);
			// 调用子类实现的doResolveException方法处理异常
			ModelAndView result = doResolveException(request, response, handler, ex);
			// 如果处理结果不为空
			if (result != null) {
				// 如果调试日志记录器处于启用状态且警告日志记录器未启用，打印调试消息
				if (logger.isDebugEnabled() && (this.warnLogger == null || !this.warnLogger.isWarnEnabled())) {
					logger.debug(buildLogMessage(ex, request) + (result.isEmpty() ? "" : " to " + result));
				}
				// 在 logException 方法中明确配置了警告日志记录器
				logException(ex, request);
			}
			// 返回处理结果
			return result;
		} else {
			// 如果不应用异常解析器，则返回null
			return null;
		}
	}

	/**
	 * 检查此解析器是否应用于给定的处理程序。
	 * <p>默认实现会检查配置的 {@linkplain #setMappedHandlers handlers} 和
	 * {@linkplain #setMappedHandlerClasses handler 类}（如果有）。
	 *
	 * @param request 当前的 HTTP 请求
	 * @param handler 已执行的处理程序，如果在异常发生时没有选择任何处理程序（例如，如果多部分解析失败），则为 {@code null}
	 * @return 是否应该为给定的请求和处理程序继续解析异常
	 * @see #setMappedHandlers
	 * @see #setMappedHandlerClasses
	 */
	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
		// 如果处理程序不为空
		if (handler != null) {
			// 如果映射处理程序列表不为空且包含当前处理程序，则返回true
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			// 如果映射处理程序类列表不为空
			if (this.mappedHandlerClasses != null) {
				// 遍历映射处理程序类列表
				for (Class<?> handlerClass : this.mappedHandlerClasses) {
					// 如果当前处理程序是指定类的实例，则返回true
					if (handlerClass.isInstance(handler)) {
						return true;
					}
				}
			}
		}
		// 如果没有处理程序映射，则返回!hasHandlerMappings()的结果
		return !hasHandlerMappings();
	}

	/**
	 * 是否通过 {@link #setMappedHandlers(Set)} 或 {@link #setMappedHandlerClasses(Class[])}
	 * 注册了任何处理程序映射。
	 *
	 * @since 5.3
	 */
	protected boolean hasHandlerMappings() {
		return (this.mappedHandlers != null || this.mappedHandlerClasses != null);
	}

	/**
	 * 在警告级别记录给定的异常，前提是通过 {@link #setWarnLogCategory "warnLogCategory"} 属性已激活了警告日志记录。
	 * <p>调用 {@link #buildLogMessage} 以确定要记录的具体消息。
	 *
	 * @param ex      在处理程序执行期间抛出的异常
	 * @param request 当前的 HTTP 请求（用于获取元数据）
	 * @see #setWarnLogCategory
	 * @see #buildLogMessage
	 * @see org.apache.commons.logging.Log#warn(Object, Throwable)
	 */
	protected void logException(Exception ex, HttpServletRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request));
		}
	}

	/**
	 * 为给定的异常构建一个日志消息，在处理给定请求期间发生了异常。
	 *
	 * @param ex      在处理程序执行期间抛出的异常
	 * @param request 当前的 HTTP 请求（用于获取元数据）
	 * @return 要使用的日志消息
	 */
	protected String buildLogMessage(Exception ex, HttpServletRequest request) {
		return "Resolved [" + LogFormatUtils.formatValue(ex, -1, true) + "]";
	}

	/**
	 * 为异常情况准备响应。
	 * <p>如果已将 {@link #setPreventResponseCaching "preventResponseCaching"} 属性设置为 "true"，
	 * 默认实现会阻止响应被缓存。
	 *
	 * @param ex       在处理程序执行期间抛出的异常
	 * @param response 当前的 HTTP 响应
	 * @see #preventCaching
	 */
	protected void prepareResponse(Exception ex, HttpServletResponse response) {
		if (this.preventResponseCaching) {
			// 如果 阻止响应缓存，则添加 Cache-Control请求头，并且将他的值设置为 no-store
			preventCaching(response);
		}
	}

	/**
	 * 通过设置相应的 HTTP {@code Cache-Control: no-store} 标头，防止响应被缓存。
	 *
	 * @param response 当前的 HTTP 响应
	 */
	protected void preventCaching(HttpServletResponse response) {
		response.addHeader(HEADER_CACHE_CONTROL, "no-store");
	}


	/**
	 * 实际解析在处理程序执行期间抛出的给定异常，如果适用，则返回表示特定错误页面的 {@link ModelAndView}。
	 * <p>可以在子类中重写，以应用特定的异常检查。
	 * 请注意，此模板方法将在检查此解析器是否适用（"mappedHandlers" 等）之后被调用，因此实现可能只需继续其实际的异常处理。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @param handler  已执行的处理程序，如果在异常发生时没有选择任何处理程序（例如，如果多部分解析失败），则为 {@code null}
	 * @param ex       在处理程序执行期间抛出的异常
	 * @return 相应的 {@code ModelAndView}，用于转发，或者在解析链中进行默认处理时返回 {@code null}
	 */
	@Nullable
	protected abstract ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex);

}
