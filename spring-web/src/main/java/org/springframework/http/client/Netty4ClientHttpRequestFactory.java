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
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * {@link org.springframework.http.client.ClientHttpRequestFactory}的实现，
 * 使用<a href="https://netty.io/">Netty 4</a>来创建请求。
 *
 * <p>允许使用预配置的{@link EventLoopGroup}实例：便于多个客户端之间共享。
 *
 * <p>注意，这个实现会在每次请求时一致地关闭HTTP连接。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Mark Paluch
 * @since 4.1.2
 * @deprecated 自Spring 5.0起，不再推荐使用，
 * 推荐使用{@link org.springframework.http.client.reactive.ReactorClientHttpConnector}
 */
@Deprecated
public class Netty4ClientHttpRequestFactory implements ClientHttpRequestFactory,
		AsyncClientHttpRequestFactory, InitializingBean, DisposableBean {

	/**
	 * 默认的最大响应大小。
	 *
	 * @see #setMaxResponseSize(int)
	 */
	public static final int DEFAULT_MAX_RESPONSE_SIZE = 1024 * 1024 * 10;

	/**
	 * 事件循环组
	 */
	private final EventLoopGroup eventLoopGroup;

	/**
	 * 是否是默认的事件循环组
	 */
	private final boolean defaultEventLoopGroup;

	/**
	 * 最大的响应大小
	 */
	private int maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;

	/**
	 * SSL上下文
	 */
	@Nullable
	private SslContext sslContext;

	/**
	 * 连接超时时间
	 */
	private int connectTimeout = -1;

	/**
	 * 读取超时时间
	 */
	private int readTimeout = -1;

	/**
	 * Netty引导程序
	 */
	@Nullable
	private volatile Bootstrap bootstrap;


	/**
	 * 使用默认的{@link NioEventLoopGroup}创建一个新的{@code Netty4ClientHttpRequestFactory}。
	 */
	public Netty4ClientHttpRequestFactory() {
		int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
		this.eventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
		this.defaultEventLoopGroup = true;
	}

	/**
	 * 使用给定的{@link EventLoopGroup}创建一个新的{@code Netty4ClientHttpRequestFactory}。
	 * <p><b>注意：</b>给定的组将<strong>不会</strong>被此工厂
	 * {@linkplain EventLoopGroup#shutdownGracefully() 优雅关闭}；
	 * 关闭操作将成为调用者的责任。
	 */
	public Netty4ClientHttpRequestFactory(EventLoopGroup eventLoopGroup) {
		Assert.notNull(eventLoopGroup, "EventLoopGroup must not be null");
		this.eventLoopGroup = eventLoopGroup;
		this.defaultEventLoopGroup = false;
	}


	/**
	 * 设置默认的最大响应大小。
	 * <p>默认情况下，这是设置为{@link #DEFAULT_MAX_RESPONSE_SIZE}。
	 *
	 * @see HttpObjectAggregator#HttpObjectAggregator(int)
	 * @since 4.1.5
	 */
	public void setMaxResponseSize(int maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	/**
	 * 设置SSL上下文。配置时，将用于创建并插入
	 * {@link io.netty.handler.ssl.SslHandler}到通道管道中。
	 * <p>如果没有提供，将配置一个默认的客户端SslContext。
	 */
	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * 设置底层的连接超时时间（以毫秒为单位）。
	 * 超时值为0表示无限超时。
	 *
	 * @see ChannelConfig#setConnectTimeoutMillis(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层URLConnection的读取超时时间（以毫秒为单位）。
	 * 超时值为0表示无限超时。
	 *
	 * @see ReadTimeoutHandler
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.sslContext == null) {
			// 如果SSL上下文为空，则设置为默认的客户端SSL上下文
			this.sslContext = getDefaultClientSslContext();
		}
	}

	private SslContext getDefaultClientSslContext() {
		try {
			// 创建默认的客户端Ssl上下文
			return SslContextBuilder.forClient().build();
		} catch (SSLException ex) {
			// 创建失败则抛出 状态非法异常
			throw new IllegalStateException("Could not create default client SslContext", ex);
		}
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		return createRequestInternal(uri, httpMethod);
	}

	private Netty4ClientHttpRequest createRequestInternal(URI uri, HttpMethod httpMethod) {
		return new Netty4ClientHttpRequest(getBootstrap(uri), uri, httpMethod);
	}

	private Bootstrap getBootstrap(URI uri) {
		// 判断是否是安全连接（端口为443或使用HTTPS协议）
		boolean isSecure = (uri.getPort() == 443 || "https".equalsIgnoreCase(uri.getScheme()));
		// 如果是安全连接
		if (isSecure) {
			// 构建并返回安全的 引导程序 对象
			return buildBootstrap(uri, true);
		} else {
			// 如果 引导程序对象 未初始化
			Bootstrap bootstrap = this.bootstrap;
			if (bootstrap == null) {
				// 构建并返回非安全的 引导程序 对象
				bootstrap = buildBootstrap(uri, false);
				this.bootstrap = bootstrap;
			}
			// 返回非安全的 引导程序对象
			return bootstrap;
		}
	}

	private Bootstrap buildBootstrap(URI uri, boolean isSecure) {
		// 创建新的 引导程序 对象
		Bootstrap bootstrap = new Bootstrap();
		// 配置 引导程序 对象，设置事件循环组和通道类型
		bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)
				// 设置通道初始化器
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						// 配置通道
						configureChannel(channel.config());
						// 获取通道管道
						ChannelPipeline pipeline = channel.pipeline();
						// 如果是安全连接
						if (isSecure) {
							Assert.notNull(sslContext, "sslContext should not be null");
							// 添加 SSL 处理器到管道中
							pipeline.addLast(sslContext.newHandler(channel.alloc(), uri.getHost(), uri.getPort()));
						}
						// 添加 HTTP 客户端编解码器到管道中
						pipeline.addLast(new HttpClientCodec());
						// 添加 HTTP 对象聚合器到管道中
						pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
						// 如果设置了读取超时时间
						if (readTimeout > 0) {
							// 添加读取超时处理器到管道中
							pipeline.addLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));
						}
					}
				});
		// 返回配置好的 引导程序 对象
		return bootstrap;
	}

	/**
	 * 用于更改给定的{@link SocketChannelConfig}属性的模板方法。
	 * <p>默认实现根据设置的属性设置连接超时。
	 *
	 * @param config 通道配置
	 */
	protected void configureChannel(SocketChannelConfig config) {
		if (this.connectTimeout >= 0) {
			// 如果连接超时时间大于等于0，则设置连接超时时间
			config.setConnectTimeoutMillis(this.connectTimeout);
		}
	}


	@Override
	public void destroy() throws InterruptedException {
		if (this.defaultEventLoopGroup) {
			// 如果是默认的事件循环组，则正常关闭它。
			// 如果我们在构造函数中创建了事件循环组，请清理它
			this.eventLoopGroup.shutdownGracefully().sync();
		}
	}

}
