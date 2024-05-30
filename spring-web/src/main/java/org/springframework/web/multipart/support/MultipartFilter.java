/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.multipart.support;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通过 {@link MultipartResolver} 解析多部分请求的 Servlet 过滤器。
 * 在根 Web 应用程序上下文中。
 *
 * <p>在 Spring 的根 Web 应用程序上下文中查找 MultipartResolver。
 * 支持在 {@code web.xml} 中的 "multipartResolverBeanName" 过滤器 init-param；
 * 默认的 bean 名称是 "filterMultipartResolver"。
 *
 * <p>如果未找到 MultipartResolver bean，此过滤器会回退到默认的
 * MultipartResolver：对于 Servlet 3.0 使用 {@link StandardServletMultipartResolver}，
 * 基于 {@code web.xml} 中的 multipart-config 部分。
 * 但请注意，目前 Servlet 规范仅定义了如何在 Servlet 上启用多部分配置，
 * 因此在过滤器中处理多部分请求可能是不可能的，除非 Servlet 容器提供了一种解决方法，
 * 例如 Tomcat 的 "allowCasualMultipartParsing" 属性。
 *
 * <p>MultipartResolver 查找是可定制的：覆盖此过滤器的
 * {@code lookupMultipartResolver} 方法以使用自定义的 MultipartResolver 实例，
 * 例如如果不使用 Spring Web 应用程序上下文。
 * 请注意，查找方法不应为每次调用创建新的 MultipartResolver 实例，
 * 而是应返回对预构建实例的引用。
 *
 * <p>注意：此过滤器是使用 DispatcherServlet 的 MultipartResolver 支持的<b>替代方案</b>，
 * 例如用于具有自定义 Web 视图的 Web 应用程序，它们不使用 Spring 的 Web MVC，
 * 或用于在 Spring MVC DispatcherServlet 之前应用的自定义过滤器
 * （例如 {@link org.springframework.web.filter.HiddenHttpMethodFilter}）。
 * 无论如何，此过滤器不应与特定于 Servlet 的多部分解析相结合。
 *
 * @author Juergen Hoeller
 * @see #setMultipartResolverBeanName
 * @see #lookupMultipartResolver
 * @see org.springframework.web.multipart.MultipartResolver
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 08.10.2003
 */
public class MultipartFilter extends OncePerRequestFilter {

	/**
	 * 多部分解析器 bean 的默认名称。
	 */
	public static final String DEFAULT_MULTIPART_RESOLVER_BEAN_NAME = "filterMultipartResolver";

	/**
	 * 默认的多部分解析器
	 */
	private final MultipartResolver defaultMultipartResolver = new StandardServletMultipartResolver();

	/**
	 * 多部分解析器Bean名称
	 */
	private String multipartResolverBeanName = DEFAULT_MULTIPART_RESOLVER_BEAN_NAME;


	/**
	 * 设置要从 Spring 的根应用程序上下文中获取的 MultipartResolver 的 bean 名称。
	 * 默认值是 "filterMultipartResolver"。
	 */
	public void setMultipartResolverBeanName(String multipartResolverBeanName) {
		this.multipartResolverBeanName = multipartResolverBeanName;
	}

	/**
	 * 返回要从 Spring 的根应用程序上下文中获取的 MultipartResolver 的 bean 名称。
	 */
	protected String getMultipartResolverBeanName() {
		return this.multipartResolverBeanName;
	}

	/**
	 * 通过此过滤器的 MultipartResolver 检查多部分请求，
	 * 并在适当时使用 MultipartHttpServletRequest 包装原始请求。
	 * <p>过滤器链中的所有后续元素，最重要的是 servlets，都可以从
	 * 正确的参数提取中受益，并且在需要时可以转换为 MultipartHttpServletRequest。
	 */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 查找多部分解析器
		MultipartResolver multipartResolver = lookupMultipartResolver(request);

		HttpServletRequest processedRequest = request;
		// 检查请求是否为multipart请求
		if (multipartResolver.isMultipart(processedRequest)) {
			// 如果是multipart请求，并且日志级别为trace，则记录日志
			if (logger.isTraceEnabled()) {
				logger.trace("Resolving multipart request");
			}
			// 解析多部分请求
			processedRequest = multipartResolver.resolveMultipart(processedRequest);
		} else {
			// 常规请求的处理逻辑
			if (logger.isTraceEnabled()) {
				logger.trace("Not a multipart request");
			}
		}

		try {
			// 继续过滤器链处理
			filterChain.doFilter(processedRequest, response);
		} finally {
			// 清理多部分请求
			if (processedRequest instanceof MultipartHttpServletRequest) {
				multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
			}
		}
	}

	/**
	 * 查找此过滤器应使用的 MultipartResolver，
	 * 将当前的 HTTP 请求作为参数。
	 * <p>默认实现委托给没有参数的 {@code lookupMultipartResolver}。
	 *
	 * @return 要使用的 MultipartResolver
	 * @see #lookupMultipartResolver()
	 */
	protected MultipartResolver lookupMultipartResolver(HttpServletRequest request) {
		return lookupMultipartResolver();
	}

	/**
	 * 在根 Web 应用程序上下文中查找 MultipartResolver bean。
	 * 支持 "multipartResolverBeanName" 过滤器 init param；默认
	 * bean 名称是 "filterMultipartResolver"。
	 * <p>这可以被覆盖以使用自定义的 MultipartResolver 实例，
	 * 例如如果不使用 Spring Web 应用程序上下文。
	 *
	 * @return MultipartResolver 实例
	 */
	protected MultipartResolver lookupMultipartResolver() {
		// 获取Web应用上下文
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		// 获取多部分解析器的bean名称
		String beanName = getMultipartResolverBeanName();
		// 如果Web应用上下文不为空，并且包含指定名称的bean
		if (wac != null && wac.containsBean(beanName)) {
			// 如果日志级别为debug，则记录日志
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver '" + beanName + "' for MultipartFilter");
			}
			// 返回指定名称的多部分解析器 bean
			return wac.getBean(beanName, MultipartResolver.class);
		} else {
			// 返回默认的多部分解析器
			return this.defaultMultipartResolver;
		}
	}

}
