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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于Apache HttpComponents HttpAsyncClient的 {@link ClientHttpResponse} 实现。
 *
 * <p>通过 {@link HttpComponentsAsyncClientHttpRequest} 创建。
 *
 * @author Oleg Kalnichevski
 * @author Arjen Poutsma
 * @since 4.0
 * @deprecated 自Spring 5.0起，推荐使用 {@link org.springframework.http.client.reactive.HttpComponentsClientHttpConnector}
 */
@Deprecated
final class HttpComponentsAsyncClientHttpResponse extends AbstractClientHttpResponse {
	/**
	 * Http响应
	 */
	private final HttpResponse httpResponse;

	/**
	 * Http头部
	 */
	@Nullable
	private HttpHeaders headers;


	HttpComponentsAsyncClientHttpResponse(HttpResponse httpResponse) {
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
		// 如果当前没有设置头信息
		if (this.headers == null) {
			// 初始化一个新的HttpHeaders对象
			this.headers = new HttpHeaders();
			// 遍历 Http头部 对象的所有头字段
			for (Header header : this.httpResponse.getAllHeaders()) {
				// 将每个头字段的名称和值添加到 Http头部 对象中
				this.headers.add(header.getName(), header.getValue());
			}
		}
		// 返回存储了所有头信息的Http头部
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		// 获取Http响应对象的实体
		HttpEntity entity = this.httpResponse.getEntity();
		// 如果实体不为null，则返回实体的内容；
		// 否则返回空输入流
		return (entity != null ? entity.getContent() : StreamUtils.emptyInput());
	}

	@Override
	public void close() {
		// 异步HTTP客户端返回的HTTP响应未绑定到活动连接，不必释放任何资源...
	}

}
