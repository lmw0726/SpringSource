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

package org.springframework.test.web.reactive.server;

import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * 简单的 {@link ClientHttpResponse} 扩展，还公开了从底层模拟服务器交换中获取的结果对象，
 * 以便在执行请求后进一步对服务器响应的状态进行断言。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public interface MockServerClientHttpResponse extends ClientHttpResponse {

	/**
	 * 返回带有服务器请求和响应的结果对象。
	 */
	Object getServerResult();

}
