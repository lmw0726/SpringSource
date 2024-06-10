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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ChannelOperationsId;
import reactor.netty.http.server.HttpServerResponse;

import java.nio.file.Path;
import java.util.List;

/**
 * 将 {@link ServerHttpResponse} 适配到 {@link HttpServerResponse}。
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ReactorServerHttpResponse extends AbstractServerHttpResponse implements ZeroCopyHttpOutputMessage {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ReactorServerHttpResponse.class);

	/**
	 * Http服务器响应
	 */
	private final HttpServerResponse response;


	public ReactorServerHttpResponse(HttpServerResponse response, DataBufferFactory bufferFactory) {
		super(bufferFactory, new HttpHeaders(new NettyHeadersAdapter(response.responseHeaders())));
		Assert.notNull(response, "HttpServerResponse must not be null");
		this.response = response;
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	@Override
	public HttpStatus getStatusCode() {
		// 获取父类的状态码
		HttpStatus status = super.getStatusCode();
		// 如果父类的状态码不为空，则返回父类的状态码；
		// 否则解析响应状态码并返回
		return (status != null ? status : HttpStatus.resolve(this.response.status().code()));
	}

	@Override
	public Integer getRawStatusCode() {
		// 获取父类的原始状态码
		Integer status = super.getRawStatusCode();
		// 如果父类的原始状态码不为空，则返回父类的原始状态码；
		// 否则返回响应状态码
		return (status != null ? status : this.response.status().code());
	}

	@Override
	protected void applyStatusCode() {
		// 获取父类的原始状态码
		Integer status = super.getRawStatusCode();
		// 如果父类的原始状态码不为空
		if (status != null) {
			// 设置响应状态码为父类的原始状态码
			this.response.status(status);
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> publisher) {
		return this.response.send(toByteBufs(publisher)).then();
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
		return this.response.sendGroups(Flux.from(publisher).map(this::toByteBufs)).then();
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		// Netty Cookie不支持同一站点。当这个问题得到解决时，我们可以再次进行适配:
		// https://github.com/netty/netty/issues/8161
		// 遍历所有的响应Cookie列表
		for (List<ResponseCookie> cookies : getCookies().values()) {
			// 遍历所有的响应Coolie
			for (ResponseCookie cookie : cookies) {
				// 将响应中的Cookie添加到响应头中
				this.response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}

	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		return doCommit(() -> this.response.sendFile(file, position, count).then());
	}

	private Publisher<ByteBuf> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
		// 如果数据缓冲区是Mono类型，则将其转换为ByteBuf
		return dataBuffers instanceof Mono ?
				Mono.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf) :
				// 如果数据缓冲区是Flux类型，则将其转换为ByteBuf
				Flux.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf);
	}

	@Override
	protected void touchDataBuffer(DataBuffer buffer) {
		// 如果调试日志启用
		if (logger.isDebugEnabled()) {
			// 如果Reactor Netty请求通道操作Id存在
			if (ReactorServerHttpRequest.reactorNettyRequestChannelOperationsIdPresent) {
				// 如果成功触摸了缓冲区，直接返回
				if (ChannelOperationsIdHelper.touch(buffer, this.response)) {
					return;
				}
			}
			// 使用响应的连接
			this.response.withConnection(connection -> {
				// 获取连接的通道ID
				ChannelId id = connection.channel().id();
				// 触摸缓冲区，记录通道ID
				DataBufferUtils.touch(buffer, "Channel id: " + id.asShortText());
			});
		}
	}


	private static class ChannelOperationsIdHelper {

		public static boolean touch(DataBuffer dataBuffer, HttpServerResponse response) {
			// 如果响应是reactor.netty.ChannelOperationsId的实例
			if (response instanceof reactor.netty.ChannelOperationsId) {
				// 获取通道ID的长文本形式
				String id = ((ChannelOperationsId) response).asLongText();
				// 触摸数据缓冲区，记录通道ID
				DataBufferUtils.touch(dataBuffer, "Channel id: " + id);
				// 返回true表示成功触摸了缓冲区
				return true;
			}
			// 返回false表示未触摸缓冲区
			return false;
		}
	}


}
