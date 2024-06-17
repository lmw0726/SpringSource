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

package org.springframework.http.client;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.*;

/**
 * 使用标准 JDK 设施实现的 {@link ClientHttpRequestFactory}。
 *
 * <p>该工厂使用 JDK 提供的 {@link java.net.HttpURLConnection} 打开连接。
 * 支持设置代理、缓冲请求体、设置连接超时和读取超时等功能。
 *
 * <p>从5.0版本开始，该类也实现了 {@link org.springframework.http.client.AsyncClientHttpRequestFactory
 * AsyncClientHttpRequestFactory} 接口。然而请注意，如果输出流式传输被禁用，
 * 则不会自动处理身份验证和重定向。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see java.net.HttpURLConnection
 * @see HttpComponentsClientHttpRequestFactory
 * @since 3.0
 */
@SuppressWarnings("deprecation")
public class SimpleClientHttpRequestFactory implements ClientHttpRequestFactory, AsyncClientHttpRequestFactory {
	/**
	 * 默认分块大小
	 */
	private static final int DEFAULT_CHUNK_SIZE = 4096;

	/**
	 * 代理类
	 */
	@Nullable
	private Proxy proxy;

	/**
	 * 缓冲区请求正文
	 */
	private boolean bufferRequestBody = true;

	/**
	 * 分块大小
	 */
	private int chunkSize = DEFAULT_CHUNK_SIZE;

	/**
	 * 连接超时时间
	 */
	private int connectTimeout = -1;

	/**
	 * 读取超时时间
	 */
	private int readTimeout = -1;

	/**
	 * 是否输出流
	 */
	private boolean outputStreaming = true;

	/**
	 * 任务执行器
	 */
	@Nullable
	private AsyncListenableTaskExecutor taskExecutor;


	/**
	 * 设置用于此请求工厂的 {@link Proxy}。
	 *
	 * @param proxy 要使用的代理
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * 指示此请求工厂是否应在内部缓冲请求体。
	 *
	 * <p>默认为 {@code true}。当通过 POST 或 PUT 发送大量数据时，
	 * 建议将此属性更改为 {@code false}，以避免内存不足。这将导致 {@link ClientHttpRequest}
	 * 直接流式传输到底层的 {@link HttpURLConnection}（如果
	 * {@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}
	 * 在前面已知），或者将使用 "Chunked transfer encoding"
	 * （如果 {@code Content-Length} 在前面不知道）。
	 *
	 * @param bufferRequestBody 是否缓冲请求体
	 * @see #setChunkSize(int)
	 * @see HttpURLConnection#setFixedLengthStreamingMode(int)
	 */
	public void setBufferRequestBody(boolean bufferRequestBody) {
		this.bufferRequestBody = bufferRequestBody;
	}

	/**
	 * 当不在本地缓冲请求体时，设置每个块中要写入的字节数。
	 *
	 * <p>注意，仅当 {@link #setBufferRequestBody(boolean)}
	 * 设置为 {@code false} 且 {@link org.springframework.http.HttpHeaders#getContentLength() Content-Length}
	 * 在前面不知道时，才使用此参数。
	 *
	 * @param chunkSize 每个块中要写入的字节数
	 * @see #setBufferRequestBody(boolean)
	 */
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * 设置底层 URLConnection 的连接超时（以毫秒为单位）。
	 * 如果超时值为 0，则指定无限超时。
	 *
	 * <p>默认为系统的默认超时时间。
	 *
	 * @param connectTimeout 连接超时时间
	 * @see URLConnection#setConnectTimeout(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 设置底层 URLConnection 的读取超时（以毫秒为单位）。
	 * 如果超时值为 0，则指定无限超时。
	 *
	 * <p>默认为系统的默认超时时间。
	 *
	 * @param readTimeout 读取超时时间
	 * @see URLConnection#setReadTimeout(int)
	 */
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * 设置底层 URLConnection 是否可以设置为 'output streaming' 模式。
	 * 默认为 {@code true}。
	 *
	 * <p>启用输出流式传输时，无法自动处理身份验证和重定向。
	 * 如果禁用输出流式传输，则底层连接的 {@link HttpURLConnection#setFixedLengthStreamingMode}
	 * 和 {@link HttpURLConnection#setChunkedStreamingMode} 方法将永远不会被调用。
	 *
	 * @param outputStreaming 是否启用输出流式传输
	 */
	public void setOutputStreaming(boolean outputStreaming) {
		this.outputStreaming = outputStreaming;
	}

	/**
	 * 设置此请求工厂的任务执行器。在调用此方法之前，设置此属性对于创建异步请求是必需的。
	 *
	 * @param taskExecutor 任务执行器
	 */
	public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 创建一个用于指定 URI 和 HTTP 方法的新 {@link ClientHttpRequest} 对象。
	 *
	 * @param uri        要创建请求的 URI
	 * @param httpMethod 要执行的 HTTP 方法
	 * @return 创建的请求对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		// 打开连接并获取 HttpURL连接 对象
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		// 准备连接，设置请求方法
		prepareConnection(connection, httpMethod.name());

		// 如果需要缓冲请求体
		if (this.bufferRequestBody) {
			// 返回一个 简单缓冲客户端Http请求 对象
			return new SimpleBufferingClientHttpRequest(connection, this.outputStreaming);
		} else {
			// 返回一个 简单流客户端Http请求 对象，并指定 chunk 大小和输出流设置
			return new SimpleStreamingClientHttpRequest(connection, this.chunkSize, this.outputStreaming);
		}
	}

	/**
	 * 创建一个用于指定 URI 和 HTTP 方法的新 {@link org.springframework.http.client.AsyncClientHttpRequest} 对象。
	 *
	 * <p>在调用此方法之前，设置 {@link #setTaskExecutor taskExecutor} 属性是必需的。
	 *
	 * @param uri        要创建请求的 URI
	 * @param httpMethod 要执行的 HTTP 方法
	 * @return 创建的异步请求对象
	 * @throws IOException 如果发生 I/O 错误
	 */
	@Override
	public AsyncClientHttpRequest createAsyncRequest(URI uri, HttpMethod httpMethod) throws IOException {
		Assert.state(this.taskExecutor != null, "Asynchronous execution requires TaskExecutor to be set");

		// 打开连接并获取 HttpURL连接 对象
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		// 准备连接，设置请求方法
		prepareConnection(connection, httpMethod.name());

		// 如果需要缓冲请求体
		if (this.bufferRequestBody) {
			// 返回一个 传入连接、输出流设置和任务执行器 的 简单缓冲异步客户端Http请求 对象
			//
			return new SimpleBufferingAsyncClientHttpRequest(
					connection, this.outputStreaming, this.taskExecutor);
		} else {
			// 返回一个 传入连接、chunk 大小、输出流设置和任务执行器 的
			// 简单流式传输异步客户端Http请求 对象，
			return new SimpleStreamingAsyncClientHttpRequest(
					connection, this.chunkSize, this.outputStreaming, this.taskExecutor);
		}
	}

	/**
	 * 打开并返回到给定 URL 的连接。
	 *
	 * <p>默认实现使用给定的 {@linkplain #setProxy(java.net.Proxy) proxy} - 如果有的话 - 打开连接。
	 *
	 * @param url   要打开连接的 URL
	 * @param proxy 要使用的代理，可以为 {@code null}
	 * @return 打开的连接
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected HttpURLConnection openConnection(URL url, @Nullable Proxy proxy) throws IOException {
		// 使用代理创建或直接创建 URL连接 对象
		URLConnection urlConnection = (proxy != null ? url.openConnection(proxy) : url.openConnection());
		// 如果不是 HttpURL连接 类型的连接，则抛出异常
		if (!(urlConnection instanceof HttpURLConnection)) {
			throw new IllegalStateException(
					"HttpURLConnection required for [" + url + "] but got: " + urlConnection);
		}
		// 强制转换并返回 HttpURL连接 对象
		return (HttpURLConnection) urlConnection;
	}

	/**
	 * 为准备给定的 {@link HttpURLConnection} 的模板方法。
	 *
	 * <p>默认实现准备输入和输出连接，并设置 HTTP 方法。
	 *
	 * @param connection 要准备的连接
	 * @param httpMethod HTTP 请求方法（如 {@code GET}、{@code POST} 等）
	 * @throws IOException 如果发生 I/O 错误
	 */
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		// 如果设置了连接超时时间，则设置连接超时
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		// 如果设置了读取超时时间，则设置读取超时
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		// 判断是否可能写入数据（POST、PUT、PATCH、DELETE 请求）
		boolean mayWrite =
				("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
						"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod));

		// 允许输入流读取
		connection.setDoInput(true);
		// 对于 GET 请求，设置实例跟随重定向
		connection.setInstanceFollowRedirects("GET".equals(httpMethod));
		// 设置是否允许输出流写入
		connection.setDoOutput(mayWrite);
		// 设置请求方法（GET、POST、PUT、DELETE 等）
		connection.setRequestMethod(httpMethod);
	}

}
