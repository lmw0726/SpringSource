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

package org.springframework.web.filter;

import org.springframework.util.Assert;
import org.springframework.web.cors.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * {@link javax.servlet.Filter}用于处理CORS预检请求，并使用{@link CorsProcessor}拦截CORS简单请求和实际请求，
 * 并根据提供的{@link CorsConfigurationSource}匹配的策略更新响应，例如CORS响应头。
 *
 * <p>这是在Spring MVC Java配置和Spring MVC XML命名空间中配置CORS的一种替代方法。
 * 它适用于仅依赖spring-web（而不是spring-webmvc）的应用程序，或者对安全约束要求在{@link javax.servlet.Filter}级别执行CORS检查的应用程序。
 *
 * <p>此过滤器可以与{@link DelegatingFilterProxy}一起使用，以帮助进行初始化。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @see UrlBasedCorsConfigurationSource
 */
public class CorsFilter extends OncePerRequestFilter {
	/**
	 * 跨域配置源
	 */
	private final CorsConfigurationSource configSource;

	/**
	 * 跨域配置处理器
	 */
	private CorsProcessor processor = new DefaultCorsProcessor();


	/**
	 * 构造函数接受一个{@link CorsConfigurationSource}，该源由过滤器使用以查找每个传入请求要使用的{@link CorsConfiguration}。
	 * @see UrlBasedCorsConfigurationSource
	 */
	public CorsFilter(CorsConfigurationSource configSource) {
		Assert.notNull(configSource, "CorsConfigurationSource must not be null");
		this.configSource = configSource;
	}


	/**
	 * 配置要使用的自定义{@link CorsProcessor}来应用匹配的{@link CorsConfiguration}的请求。
	 * <p>默认情况下，使用{@link DefaultCorsProcessor}。
	 */
	public void setCorsProcessor(CorsProcessor processor) {
		Assert.notNull(processor, "CorsProcessor must not be null");
		this.processor = processor;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		// 获取请求的 跨域配置 对象
		CorsConfiguration corsConfiguration = this.configSource.getCorsConfiguration(request);
		// 使用 跨域处理器 处理请求，并检查是否有效
		boolean isValid = this.processor.processRequest(corsConfiguration, request, response);

		// 如果请求无效或是预检请求，则直接返回，不进行后续的过滤操作
		if (!isValid || CorsUtils.isPreFlightRequest(request)) {
			return;
		}

		// 否则继续调用过滤器链的下一个过滤器
		filterChain.doFilter(request, response);
	}

}
