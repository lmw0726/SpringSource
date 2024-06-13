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

package org.springframework.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * 基于Netty 4的{@link ClientHttpRequest}实现。
 *
 * <p>通过{@link Netty4ClientHttpRequestFactory}创建。
 *
 * <p>已废弃，自Spring 5.0起，推荐使用{@link org.springframework.http.client.reactive.ReactorClientHttpConnector}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1.2
 * @deprecated 自Spring 5.0起，推荐使用{@link org.springframework.http.client.reactive.ReactorClientHttpConnector}
 */
@Deprecated
class Netty4ClientHttpRequest extends AbstractAsyncClientHttpRequest implements ClientHttpRequest {

	/**
	 * 使用的Netty引导器。
	 */
	private final Bootstrap bootstrap;

	/**
	 * 请求的URI。
	 */
	private final URI uri;

	/**
	 * HTTP方法。
	 */
	private final HttpMethod method;

	/**
	 * 请求体的ByteBuf输出流。
	 */
	private final ByteBufOutputStream body;


	public Netty4ClientHttpRequest(Bootstrap bootstrap, URI uri, HttpMethod method) {
		this.bootstrap = bootstrap;
		this.uri = uri;
		this.method = method;
		this.body = new ByteBufOutputStream(Unpooled.buffer(1024));
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public String getMethodValue() {
		return this.method.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public ClientHttpResponse execute() throws IOException {
		try {
			// 调用 executeAsync 方法并等待其完成，获取结果
			return executeAsync().get();
		} catch (InterruptedException ex) {
			// 如果在等待过程中被中断，重新设置中断状态，并抛出 IOException 异常
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted during request execution", ex);
		} catch (ExecutionException ex) {
			// 处理执行过程中的异常
			if (ex.getCause() instanceof IOException) {
				// 如果异常的原因是 IOException，直接抛出该异常
				throw (IOException) ex.getCause();
			} else {
				// 否则，抛出包含原因信息的 IOException 异常
				throw new IOException(ex.getMessage(), ex.getCause());
			}
		}
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		return this.body;
	}

	@Override
	protected ListenableFuture<ClientHttpResponse> executeInternal(final HttpHeaders headers) throws IOException {
		// 创建一个可设置结果的 ListenableFuture 对象
		final SettableListenableFuture<ClientHttpResponse> responseFuture = new SettableListenableFuture<>();

		// 定义连接监听器
		ChannelFutureListener connectionListener = future -> {
			if (future.isSuccess()) {
				// 如果连接成功，获取 Channel 对象
				Channel channel = future.channel();
				// 向 Channel 的管道中添加 RequestExecuteHandler 处理器，传入 responseFuture
				channel.pipeline().addLast(new RequestExecuteHandler(responseFuture));
				// 创建一个 Netty 的 FullHttpRequest 对象
				FullHttpRequest nettyRequest = createFullHttpRequest(headers);
				// 将 Netty 请求写入并刷新到 Channel
				channel.writeAndFlush(nettyRequest);
			} else {
				// 如果连接失败，设置异常到 responseFuture 中
				responseFuture.setException(future.cause());
			}
		};

		// 使用 Bootstrap 进行连接，并添加连接监听器
		this.bootstrap.connect(this.uri.getHost(), getPort(this.uri)).addListener(connectionListener);

		// 返回 responseFuture，用于异步获取响应
		return responseFuture;
	}

	private FullHttpRequest createFullHttpRequest(HttpHeaders headers) {
		// 将当前请求方法转换为 Netty 的 HttpMethod
		io.netty.handler.codec.http.HttpMethod nettyMethod =
				io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name());

		// 获取请求的主机部分和路径
		String authority = this.uri.getRawAuthority();
		String path = this.uri.toString().substring(this.uri.toString().indexOf(authority) + authority.length());

		// 创建一个新的 DefaultFullHttpRequest 对象，使用 HTTP/1.1 版本、请求方法、路径和请求体
		FullHttpRequest nettyRequest = new DefaultFullHttpRequest(
				HttpVersion.HTTP_1_1, nettyMethod, path, this.body.buffer());

		// 设置请求头 HOST，格式为主机名加端口号
		nettyRequest.headers().set(HttpHeaders.HOST, this.uri.getHost() + ":" + getPort(this.uri));

		// 设置连接为关闭状态
		nettyRequest.headers().set(HttpHeaders.CONNECTION, "close");

		// 将原始请求中的所有头信息添加到 Netty 请求中
		headers.forEach((headerName, headerValues) -> nettyRequest.headers().add(headerName, headerValues));

		// 如果 Netty 请求头中不包含 CONTENT_LENGTH 并且请求体的可读字节数大于0，则设置 CONTENT_LENGTH
		if (!nettyRequest.headers().contains(HttpHeaders.CONTENT_LENGTH) && this.body.buffer().readableBytes() > 0) {
			nettyRequest.headers().set(HttpHeaders.CONTENT_LENGTH, this.body.buffer().readableBytes());
		}

		// 返回构建好的 Netty 请求对象
		return nettyRequest;
	}

	private static int getPort(URI uri) {
		// 获取 URI 的端口号
		int port = uri.getPort();

		// 如果端口号为 -1，根据 URI 的 scheme 设置默认端口号
		if (port == -1) {
			if ("http".equalsIgnoreCase(uri.getScheme())) {
				port = 80;
			} else if ("https".equalsIgnoreCase(uri.getScheme())) {
				port = 443;
			}
		}

		// 返回最终确定的端口号
		return port;
	}


	/**
	 * 用于更新给定的 SettableListenableFuture 的 SimpleChannelInboundHandler。
	 */
	private static class RequestExecuteHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

		/**
		 * 响应结果的 可设置的Listenable Future。
		 */
		private final SettableListenableFuture<ClientHttpResponse> responseFuture;

		/**
		 * 构造函数，初始化响应结果的 SettableListenableFuture。
		 *
		 * @param responseFuture 响应结果的 SettableListenableFuture
		 */
		public RequestExecuteHandler(SettableListenableFuture<ClientHttpResponse> responseFuture) {
			this.responseFuture = responseFuture;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext context, FullHttpResponse response) throws Exception {
			this.responseFuture.set(new Netty4ClientHttpResponse(context, response));
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
			this.responseFuture.setException(cause);
		}
	}

}
