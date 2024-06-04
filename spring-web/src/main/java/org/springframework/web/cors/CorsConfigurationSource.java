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

package org.springframework.web.cors;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

/**
 * 接口，由提供基于所提供请求的 {@link CorsConfiguration} 实例的类（通常是 HTTP 请求处理程序）实现。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public interface CorsConfigurationSource {

	/**
	 * 返回基于传入请求的 {@link CorsConfiguration}。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 相关的 {@link CorsConfiguration}，如果没有则返回 {@code null}
	 */
	@Nullable
	CorsConfiguration getCorsConfiguration(HttpServletRequest request);

}
