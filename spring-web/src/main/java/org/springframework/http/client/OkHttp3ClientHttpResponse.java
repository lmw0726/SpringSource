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

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 OkHttp 3.x 的 {@link ClientHttpResponse} 实现。
 *
 * @author Luciano Leggieri
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 4.3
 */
class OkHttp3ClientHttpResponse extends AbstractClientHttpResponse {

	/**
	 * Http响应
	 */
	private final Response response;

	/**
	 * Http头部
	 */
	@Nullable
	private HttpHeaders headers;


	public OkHttp3ClientHttpResponse(Response response) {
		Assert.notNull(response, "Response must not be null");
		this.response = response;
	}


	@Override
	public int getRawStatusCode() {
		return this.response.code();
	}

	@Override
	public String getStatusText() {
		return this.response.message();
	}

	@Override
	public InputStream getBody() throws IOException {
		// 获取当前对象的响应主体对象
		ResponseBody body = this.response.body();

		// 如果响应主体对象不为空，则返回其字节流
		// 否则返回一个空的输入流
		return (body != null ? body.byteStream() : StreamUtils.emptyInput());
	}

	@Override
	public HttpHeaders getHeaders() {
		// 获取当前对象的 Http头部 属性
		HttpHeaders headers = this.headers;

		// 如果 Http头部 为空，则进行以下操作
		if (headers == null) {
			// 创建一个新的 HttpHeaders 对象
			headers = new HttpHeaders();

			// 遍历响应的所有头部名称
			for (String headerName : this.response.headers().names()) {
				// 对于每个头部名称，遍历其对应的所有值
				for (String headerValue : this.response.headers(headerName)) {
					// 将头部名称和值添加到 HttpHeaders 对象中
					headers.add(headerName, headerValue);
				}
			}

			// 将填充好的 HttpHeaders 对象赋值给当前对象的 Http头部 属性
			this.headers = headers;
		}

		// 返回填充好的 HttpHeaders 对象
		return headers;
	}

	@Override
	public void close() {
		// 获取当前对象的响应主体对象
		ResponseBody body = this.response.body();

		// 如果响应主体对象不为空，则执行以下操作
		if (body != null) {
			// 关闭响应主体，释放相关资源
			body.close();
		}
	}

}
