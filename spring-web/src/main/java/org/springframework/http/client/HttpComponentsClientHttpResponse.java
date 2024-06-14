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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Apache HttpComponents HttpClient 的 {@link ClientHttpResponse} 实现。
 *
 * <p>通过 {@link HttpComponentsClientHttpRequest} 创建。
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @see HttpComponentsClientHttpRequest#execute()
 * @since 3.1
 */
final class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {
	/**
	 * Http响应
	 */
	private final HttpResponse httpResponse;

	/**
	 * Http头部
	 */
	@Nullable
	private HttpHeaders headers;


	HttpComponentsClientHttpResponse(HttpResponse httpResponse) {
		this.httpResponse = httpResponse;
	}


	@Override
	public int getRawStatusCode() throws IOException {
		return this.httpResponse.getStatusLine().getStatusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return this.httpResponse.getStatusLine().getReasonPhrase();
	}

	@Override
	public HttpHeaders getHeaders() {
		// 如果当前对象的头部信息为空
		if (this.headers == null) {
			// 创建一个新的HttpHeaders对象
			this.headers = new HttpHeaders();

			// 遍历HTTP响应对象的所有头部信息
			for (Header header : this.httpResponse.getAllHeaders()) {
				// 将每个头部信息的名称和值添加到新创建的HttpHeaders对象中
				this.headers.add(header.getName(), header.getValue());
			}
		}

		// 返回头部信息对象
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		// 获取HTTP响应的实体对象
		HttpEntity entity = this.httpResponse.getEntity();

		// 如果实体对象不为空，则返回实体内容流；
		// 否则返回一个空输入流
		return (entity != null ? entity.getContent() : StreamUtils.emptyInput());
	}

	@Override
	public void close() {
		// 释放底层连接到连接管理器
		try {
			try {
				// 尝试通过消耗剩余内容来保持连接活动
				EntityUtils.consume(this.httpResponse.getEntity());
			} finally {
				// 如果HTTP响应实现了Closeable接口，关闭它
				if (this.httpResponse instanceof Closeable) {
					((Closeable) this.httpResponse).close();
				}
			}
		} catch (IOException ex) {
			// 关闭时出现IO异常，忽略该异常...
		}
	}

}
