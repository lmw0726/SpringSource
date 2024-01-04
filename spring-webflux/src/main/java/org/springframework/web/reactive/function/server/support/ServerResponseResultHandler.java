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

package org.springframework.web.reactive.function.server.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * {@code HandlerResultHandler} 实现，支持 {@link ServerResponse ServerResponses}。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerResponseResultHandler implements HandlerResultHandler, InitializingBean, Ordered {

	/**
	 * 消息写入器列表
	 */
	private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

	/**
	 * 视图解析器列表
	 */
	private List<ViewResolver> viewResolvers = Collections.emptyList();

	/**
	 * 顺序值
	 */
	private int order = 0;


	/**
	 * 配置用于序列化请求体的 HTTP 消息写入器。
	 * <p>默认情况下，设置为 {@link ServerCodecConfigurer} 的默认写入器。
	 *
	 * @param configurer HTTP 消息写入器的配置
	 */
	public void setMessageWriters(List<HttpMessageWriter<?>> configurer) {
		this.messageWriters = configurer;
	}

	/**
	 * 设置视图解析器列表。
	 *
	 * @param viewResolvers 视图解析器列表
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	/**
	 * 设置此结果处理程序相对于其他处理程序的顺序。
	 * <p>默认设置为 0。通常安全地将其放置在顺序的开头，因为它寻找具体的返回类型。
	 *
	 * @param order 顺序
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageWriters)) {
			throw new IllegalArgumentException("Property 'messageWriters' is required");
		}
	}

	@Override
	public boolean supports(HandlerResult result) {
		return (result.getReturnValue() instanceof ServerResponse);
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		// 从结果中获取 ServerResponse
		ServerResponse response = (ServerResponse) result.getReturnValue();

		// 断言确保获取到了 ServerResponse 对象
		Assert.state(response != null, "No ServerResponse");

		// 将 ServerResponse 写入到交换器中，并使用消息写入器列表和视图解析器列表
		return response.writeTo(exchange, new ServerResponse.Context() {
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				// 返回消息写入器列表
				return messageWriters;
			}

			@Override
			public List<ViewResolver> viewResolvers() {
				// 返回视图解析器列表
				return viewResolvers;
			}
		});

	}

}
