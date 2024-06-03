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

package org.springframework.web.filter;

import org.springframework.http.server.RequestPath;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 一个 {@code Filter} 用于 {@link ServletRequestPathUtils#parseAndCache 解析}
 * 并缓存一个 {@link org.springframework.http.server.RequestPath} 以便在整个过滤器链中
 * 进一步 {@link ServletRequestPathUtils#getParsedRequestPath 访问}。当在应用程序中使用解析的
 * {@link org.springframework.web.util.pattern.PathPattern}s 而不是使用
 * {@link org.springframework.util.PathMatcher} 进行字符串模式匹配时，这很有用。
 * <p>注意，在 Spring MVC 中，如果 {@code DispatcherServlet} 检测到任意
 * {@code HandlerMapping} 启用了解析的 {@code PathPatterns}，它也会解析并缓存 {@code RequestPath}，
 * 但如果它发现 {@link ServletRequestPathUtils#PATH_ATTRIBUTE} 已经存在，则会跳过。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class ServletRequestPathFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		// 获取之前请求路径的 RequestPath 对象，并从请求属性中获取 请求路径 属性
		RequestPath previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);

		// 解析并缓存当前请求路径
		ServletRequestPathUtils.parseAndCache((HttpServletRequest) request);

		try {
			// 执行过滤器链
			chain.doFilter(request, response);
		} finally {
			// 在请求完成后，恢复之前的请求路径
			ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
		}
	}

}
