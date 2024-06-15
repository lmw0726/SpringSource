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

package org.springframework.http.server;

import org.springframework.lang.Nullable;

import java.net.URI;

/**
 * {@link PathContainer} 的特化版本，将路径分为 {@link #contextPath()} 和剩余的 {@link #pathWithinApplication()}。
 * 后者通常用于应用程序内的请求映射，而前者在准备指向应用程序的外部链接时很有用。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestPath extends PathContainer {

	/**
	 * 返回表示应用程序的URL路径部分。上下文路径始终位于路径的开头，并以 "/" 开头但不以 "/" 结尾。
	 * 它在相同应用程序的URL中是共享的。
	 * <p>上下文路径可能来自底层的运行时API，例如在将WAR部署到Servlet容器时，或者可以通过
	 * {@link org.springframework.http.server.reactive.ContextPathCompositeHandler ContextPathCompositeHandler}
	 * 在WebFlux应用程序中分配。</p>
	 */
	PathContainer contextPath();

	/**
	 * 上下文路径之后的请求路径部分，通常用于应用程序内的请求映射。
	 */
	PathContainer pathWithinApplication();

	/**
	 * 返回具有修改上下文路径的新 {@code RequestPath} 实例。
	 * 新的上下文路径必须匹配开始处的0个或更多个路径段。
	 *
	 * @param contextPath 新的上下文路径
	 * @return 新的 {@code RequestPath} 实例
	 */
	RequestPath modifyContextPath(String contextPath);


	/**
	 * 将请求的URI解析为 {@code RequestPath} 实例。
	 *
	 * @param uri         请求的URI
	 * @param contextPath URI路径的上下文路径部分
	 * @return 解析后的 {@code RequestPath} 实例
	 */
	static RequestPath parse(URI uri, @Nullable String contextPath) {
		return parse(uri.getRawPath(), contextPath);
	}

	/**
	 * 使用编码的 {@link URI#getRawPath() raw path} 的变体解析。
	 *
	 * @param rawPath     路径
	 * @param contextPath URI路径的上下文路径部分
	 * @return 解析后的 {@code RequestPath} 实例
	 * @since 5.3
	 */
	static RequestPath parse(String rawPath, @Nullable String contextPath) {
		return new DefaultRequestPath(rawPath, contextPath);
	}

}
