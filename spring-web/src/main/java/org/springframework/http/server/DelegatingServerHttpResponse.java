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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 实现了 {@code ServerHttpResponse} 接口，将所有调用委托给给定的目标 {@code ServerHttpResponse}。
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
public class DelegatingServerHttpResponse implements ServerHttpResponse {
	/**
	 * 服务端Http响应
	 */
	private final ServerHttpResponse delegate;

	/**
	 * 创建一个新的 {@code DelegatingServerHttpResponse} 实例。
	 *
	 * @param delegate 要委托的响应对象
	 */
	public DelegatingServerHttpResponse(ServerHttpResponse delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * 返回此响应对象委托的目标响应对象。
	 *
	 * @return 委托的目标响应对象
	 */
	public ServerHttpResponse getDelegate() {
		return this.delegate;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		this.delegate.setStatusCode(status);
	}

	@Override
	public void flush() throws IOException {
		this.delegate.flush();
	}

	@Override
	public void close() {
		this.delegate.close();
	}

	@Override
	public OutputStream getBody() throws IOException {
		return this.delegate.getBody();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.delegate.getHeaders();
	}

}
