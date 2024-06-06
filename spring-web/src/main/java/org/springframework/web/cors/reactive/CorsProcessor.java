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

package org.springframework.web.cors.reactive;

import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/**
 * 一种策略，用于根据预选的{@link CorsConfiguration}对{@link ServerWebExchange}应用CORS验证检查和更新，
 * 可以通过响应拒绝请求或添加与CORS相关的头信息。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C推荐</a>
 */
public interface CorsProcessor {

	/**
	 * 使用给定的{@code CorsConfiguration}处理请求。
	 *
	 * @param configuration 要使用的CORS配置；可能为{@code null}，
	 * 在这种情况下，预检请求将被拒绝，但允许所有其他请求。
	 * @param exchange 当前交换
	 * @return 如果请求被拒绝则返回{@code false}，否则返回{@code true}
	 */
	boolean process(@Nullable CorsConfiguration configuration, ServerWebExchange exchange);

}
