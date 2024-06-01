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

package org.springframework.web.filter;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 重写了{@link HttpServletResponse#sendRedirect(String)}方法，并通过设置HTTP状态码和"Location"头来处理它，
 * 从而防止Servlet容器将相对重定向URL重写为绝对URL。虽然Servlet容器要求这样做，但违反了
 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">RFC 7231第7.1.2节</a>的建议，而且不一定考虑"X-Forwarded"头。
 *
 * <p><strong>注意:</strong> 虽然RFC推荐使用相对重定向，但在某些配置下，使用反向代理可能不起作用。
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {
	/**
	 * 重定向状态码
	 */
	private HttpStatus redirectStatus = HttpStatus.SEE_OTHER;


	/**
	 * 设置重定向时使用的默认HTTP状态。
	 * <p>默认为{@link HttpStatus#SEE_OTHER}。
	 * @param status 3xx重定向状态
	 */
	public void setRedirectStatus(HttpStatus status) {
		Assert.notNull(status, "Property 'redirectStatus' is required");
		Assert.isTrue(status.is3xxRedirection(), "Not a redirect status code");
		this.redirectStatus = status;
	}

	/**
	 * 返回配置的重定向状态。
	 */
	public HttpStatus getRedirectStatus() {
		return this.redirectStatus;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		// 将响应对象包装成一个相对路径的重定向响应（如果需要）
		response = RelativeRedirectResponseWrapper.wrapIfNecessary(response, this.redirectStatus);

		// 继续调用过滤器链的下一个过滤器
		filterChain.doFilter(request, response);
	}

}
