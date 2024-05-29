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

package org.springframework.web.server;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.function.Consumer;

/**
 * 包私有的 {@link ServerWebExchange.Builder} 实现。
 * <p>用于构建和修改 ServerWebExchange 实例。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerWebExchangeBuilder implements ServerWebExchange.Builder {
	/**
	 * 服务器Web交换代理类
	 */
	private final ServerWebExchange delegate;

	/**
	 * 请求
	 */
	@Nullable
	private ServerHttpRequest request;

	/**
	 * 响应
	 */
	@Nullable
	private ServerHttpResponse response;

	/**
	 * 当前请求的认证用户
	 */
	@Nullable
	private Mono<Principal> principalMono;


	DefaultServerWebExchangeBuilder(ServerWebExchange delegate) {
		Assert.notNull(delegate, "Delegate is required");
		this.delegate = delegate;
	}


	@Override
	public ServerWebExchange.Builder request(Consumer<ServerHttpRequest.Builder> consumer) {
		// 获取当前请求的构建器
		ServerHttpRequest.Builder builder = this.delegate.getRequest().mutate();
		// 使用消费者对构建器进行修改
		consumer.accept(builder);
		// 构建新的请求并返回
		return request(builder.build());
	}

	@Override
	public ServerWebExchange.Builder request(ServerHttpRequest request) {
		this.request = request;
		return this;
	}

	@Override
	public ServerWebExchange.Builder response(ServerHttpResponse response) {
		this.response = response;
		return this;
	}

	@Override
	public ServerWebExchange.Builder principal(Mono<Principal> principalMono) {
		this.principalMono = principalMono;
		return this;
	}

	@Override
	public ServerWebExchange build() {
		return new MutativeDecorator(this.delegate, this.request, this.response, this.principalMono);
	}


	/**
	 * 一个不可变的交换包装器，返回属性覆盖——传递给构造函数——否则返回原始值。
	 */
	private static class MutativeDecorator extends ServerWebExchangeDecorator {
		/**
		 * 请求
		 */
		@Nullable
		private final ServerHttpRequest request;

		/**
		 * 响应
		 */
		@Nullable
		private final ServerHttpResponse response;

		/**
		 *当前请求的认证用户
		 */
		@Nullable
		private final Mono<Principal> principalMono;

		public MutativeDecorator(ServerWebExchange delegate, @Nullable ServerHttpRequest request,
								 @Nullable ServerHttpResponse response, @Nullable Mono<Principal> principalMono) {

			super(delegate);
			this.request = request;
			this.response = response;
			this.principalMono = principalMono;
		}

		@Override
		public ServerHttpRequest getRequest() {
			return (this.request != null ? this.request : getDelegate().getRequest());
		}

		@Override
		public ServerHttpResponse getResponse() {
			return (this.response != null ? this.response : getDelegate().getResponse());
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Principal> Mono<T> getPrincipal() {
			return (this.principalMono != null ? (Mono<T>) this.principalMono : getDelegate().getPrincipal());
		}
	}

}
