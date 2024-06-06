/*
 * Copyright 2002-2016 the original author or authors.
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
 * 由类（通常是HTTP请求处理程序）实现的接口，根据提供的响应式请求提供{@link CorsConfiguration}实例。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface CorsConfigurationSource {

	/**
	 * 根据传入的请求返回{@link CorsConfiguration}。
	 *
	 * @param exchange 当前交换
	 * @return 关联的{@link CorsConfiguration}，如果没有则返回{@code null}
	 */
	@Nullable
	CorsConfiguration getCorsConfiguration(ServerWebExchange exchange);

}
