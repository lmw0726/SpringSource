/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.client;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 基于 Netty 4 的 {@link ClientHttpResponse} 实现。
 *
 * @author Arjen Poutsma
 * @since 4.1.2
 * @deprecated Spring 5.0 起已弃用，推荐使用 {@link org.springframework.http.client.reactive.ReactorClientHttpConnector}
 */
@Deprecated
class Netty4ClientHttpResponse extends AbstractClientHttpResponse {

	/**
	 * 通道处理程序上下文
	 */
	private final ChannelHandlerContext context;

	/**
	 * Netty响应
	 */
	private final FullHttpResponse nettyResponse;

	/**
	 * 响应体
	 */
	private final ByteBufInputStream body;

	/**
	 * Http头部
	 */
	@Nullable
	private volatile HttpHeaders headers;


	public Netty4ClientHttpResponse(ChannelHandlerContext context, FullHttpResponse nettyResponse) {
		Assert.notNull(context, "ChannelHandlerContext must not be null");
		Assert.notNull(nettyResponse, "FullHttpResponse must not be null");
		this.context = context;
		this.nettyResponse = nettyResponse;
		this.body = new ByteBufInputStream(this.nettyResponse.content());
		this.nettyResponse.retain();
	}


	@Override
	public int getRawStatusCode() throws IOException {
		return this.nettyResponse.getStatus().code();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.nettyResponse.getStatus().reasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		// 获取当前已缓存的头部信息，如果没有则为null
		HttpHeaders headers = this.headers;

		// 如果头部信息为空
		if (headers == null) {
			// 创建一个新的HttpHeaders对象
			headers = new HttpHeaders();

			// 遍历Netty响应对象的所有头部信息条目
			for (Map.Entry<String, String> entry : this.nettyResponse.headers()) {
				// 将每个头部信息的键值对添加到新创建的HttpHeaders对象中
				headers.add(entry.getKey(), entry.getValue());
			}

			// 将新创建的HttpHeaders对象缓存起来
			this.headers = headers;
		}

		// 返回头部信息对象
		return headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public void close() {
		// 释放Netty响应
		this.nettyResponse.release();
		// 关闭上下文
		this.context.close();
	}

}
