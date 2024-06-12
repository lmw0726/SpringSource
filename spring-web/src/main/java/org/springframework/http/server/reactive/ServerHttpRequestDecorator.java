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

package org.springframework.http.server.reactive;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * 包装另一个 {@link ServerHttpRequest} 并将所有方法委托给它。
 * 子类可以选择性地覆盖特定方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerHttpRequestDecorator implements ServerHttpRequest {
	/**
	 * 服务端Http请求
	 */
	private final ServerHttpRequest delegate;


	public ServerHttpRequestDecorator(ServerHttpRequest delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	public ServerHttpRequest getDelegate() {
		return this.delegate;
	}


	// ServerHttpRequest 委托方法...

	@Override
	public String getId() {
		return getDelegate().getId();
	}

	@Override
	@Nullable
	public HttpMethod getMethod() {
		return getDelegate().getMethod();
	}

	@Override
	public String getMethodValue() {
		return getDelegate().getMethodValue();
	}

	@Override
	public URI getURI() {
		return getDelegate().getURI();
	}

	@Override
	public RequestPath getPath() {
		return getDelegate().getPath();
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return getDelegate().getQueryParams();
	}

	@Override
	public HttpHeaders getHeaders() {
		return getDelegate().getHeaders();
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		return getDelegate().getCookies();
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return getDelegate().getLocalAddress();
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return getDelegate().getRemoteAddress();
	}

	@Override
	@Nullable
	public SslInfo getSslInfo() {
		return getDelegate().getSslInfo();
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return getDelegate().getBody();
	}


	/**
	 * 如果可能，返回基础服务器 API 的原生请求，并在必要时取消包装 {@link ServerHttpRequestDecorator}。
	 *
	 * @param request 要检查的请求
	 * @param <T>     期望的原生请求类型
	 * @throws IllegalArgumentException 如果无法获取原生请求
	 * @since 5.3.3
	 */
	public static <T> T getNativeRequest(ServerHttpRequest request) {
		// 检查请求是否是AbstractServerHttpRequest的实例
		if (request instanceof AbstractServerHttpRequest) {
			// 如果是，返回原生请求对象
			return ((AbstractServerHttpRequest) request).getNativeRequest();
		} else if (request instanceof ServerHttpRequestDecorator) {
			// 检查请求是否是ServerHttpRequestDecorator的实例
			// 如果是，递归调用getNativeRequest方法以获取原生请求对象
			return getNativeRequest(((ServerHttpRequestDecorator) request).getDelegate());
		} else {
			// 如果请求既不是AbstractServerHttpRequest也不是ServerHttpRequestDecorator的实例
			// 抛出异常，表示无法在请求中找到原生请求对象
			throw new IllegalArgumentException(
					"Can't find native request in " + request.getClass().getName());
		}
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + getDelegate() + "]";
	}

}
