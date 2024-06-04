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

package org.springframework.web.cors;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 一个策略接口，用于处理请求和 {@link CorsConfiguration} 并更新响应。
 *
 * <p>该组件不关心如何选择 {@code CorsConfiguration}，而是采取后续操作，例如应用 CORS 验证检查，
 * 并拒绝响应或向响应添加 CORS 头。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setCorsProcessor
 * @since 4.2
 */
public interface CorsProcessor {

	/**
	 * 根据给定的 {@code CorsConfiguration} 处理请求。
	 *
	 * @param configuration 适用的 CORS 配置（可能为 {@code null}）
	 * @param request       当前的请求
	 * @param response      当前的响应
	 * @return 如果请求被拒绝则返回 {@code false}，否则返回 {@code true}
	 * @throws IOException 如果处理请求时发生 I/O 错误
	 */
	boolean processRequest(@Nullable CorsConfiguration configuration, HttpServletRequest request,
						   HttpServletResponse response) throws IOException;

}
