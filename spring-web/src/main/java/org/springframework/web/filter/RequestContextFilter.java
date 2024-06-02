/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.filter;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通过{@link org.springframework.context.i18n.LocaleContextHolder}和
 * {@link RequestContextHolder}将请求暴露给当前线程的Servlet过滤器。
 * 应在{@code web.xml}中注册为过滤器。
 *
 * <p>另外，Spring的{@link org.springframework.web.context.request.RequestContextListener}
 * 和Spring的{@link org.springframework.web.servlet.DispatcherServlet}也将相同的请求上下文暴露给当前线程。
 *
 * <p>此过滤器主要用于第三方Servlet，例如JSF FacesServlet。在Spring自己的web支持中，
 * DispatcherServlet的处理已经完全足够了。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rossen Stoyanchev
 * @since 2.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.context.request.RequestContextHolder
 * @see org.springframework.web.context.request.RequestContextListener
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class RequestContextFilter extends OncePerRequestFilter {
	/**
	 * 是否继承线程上下文
	 */
	private boolean threadContextInheritable = false;


	/**
	 * 设置是否通过{@link java.lang.InheritableThreadLocal}将LocaleContext和RequestAttributes
	 * 暴露给子线程。
	 * <p>默认值为"false"，以避免对生成的后台线程产生副作用。
	 * 将此值设置为"true"以启用继承，适用于在请求处理中生成且仅用于此请求的自定义子线程
	 * （即在其初始任务结束后终止，而不重用线程）。
	 * <p><b>警告：</b>如果您正在访问一个配置为按需添加新线程的线程池（例如JDK的
	 * {@link java.util.concurrent.ThreadPoolExecutor}），请不要为子线程使用继承，
	 * 因为这会将继承的上下文暴露给这样的池化线程。
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}


	/**
	 * 返回"false"，以便过滤器可以在每个异步调度的线程中设置请求上下文。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回"false"，以便过滤器可以在错误调度中设置请求上下文。
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 创建一个 ServletRequestAttributes 对象，绑定请求和响应
		ServletRequestAttributes attributes = new ServletRequestAttributes(request, response);

		// 初始化上下文持有者
		initContextHolders(request, attributes);

		try {
			// 继续调用过滤器链的下一个过滤器
			filterChain.doFilter(request, response);
		} finally {
			// 重置上下文持有者
			resetContextHolders();

			// 如果日志记录级别为 TRACE，则记录清除线程绑定的请求上下文的日志
			if (logger.isTraceEnabled()) {
				logger.trace("Cleared thread-bound request context: " + request);
			}

			// 标记请求处理完成
			attributes.requestCompleted();
		}
	}

	private void initContextHolders(HttpServletRequest request, ServletRequestAttributes requestAttributes) {
		// 将请求的区域设置绑定到当前线程，指定是否继承线程上下文
		LocaleContextHolder.setLocale(request.getLocale(), this.threadContextInheritable);

		// 将请求属性绑定到当前线程，指定是否继承线程上下文
		RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);

		// 如果日志记录级别为 TRACE，则记录绑定请求上下文到线程的日志
		if (logger.isTraceEnabled()) {
			logger.trace("Bound request context to thread: " + request);
		}
	}

	private void resetContextHolders() {
		// 重置当前线程的区域设置上下文
		LocaleContextHolder.resetLocaleContext();

		// 重置当前线程的请求属性上下文
		RequestContextHolder.resetRequestAttributes();
	}

}
