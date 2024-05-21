/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * 辅助为给定的URL路径模式创建 {@link CorsConfiguration} 实例。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see CorsConfiguration
 * @see CorsRegistry
 * @since 4.2
 */
public class CorsRegistration {
	/**
	 * 路径模式
	 */
	private final String pathPattern;

	/**
	 * 跨域配置
	 */
	private CorsConfiguration config;


	public CorsRegistration(String pathPattern) {
		this.pathPattern = pathPattern;
		// 与 @CrossOrigin 注解相同的隐式默认值 + 允许简单方法
		this.config = new CorsConfiguration().applyPermitDefaultValues();
	}


	/**
	 * 设置从浏览器允许进行跨源请求的源。
	 * 请参阅 {@link CorsConfiguration#setAllowedOrigins(List)} 了解格式详细信息和其他注意事项。
	 *
	 * <p>默认情况下，允许所有源，但如果还设置了 {@link #allowedOriginPatterns(String...)}，
	 * 那么将优先使用后者。
	 *
	 * @see #allowedOriginPatterns(String...)
	 */
	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(Arrays.asList(origins));
		return this;
	}

	/**
	 * {@link #allowedOrigins(String...)} 的替代方法，支持更灵活的模式，
	 * 以指定从浏览器允许进行跨源请求的源。请参阅
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)} 了解格式详细信息和其他注意事项。
	 * <p>默认情况下未设置。
	 *
	 * @since 5.3
	 */
	public CorsRegistration allowedOriginPatterns(String... patterns) {
		this.config.setAllowedOriginPatterns(Arrays.asList(patterns));
		return this;
	}

	/**
	 * 设置允许的 HTTP 方法，例如 {@code "GET"}、{@code "POST"} 等。
	 * <p>特殊值 {@code "*"} 允许所有方法。
	 * <p>默认情况下允许 "简单" 方法 {@code GET}、{@code HEAD} 和 {@code POST}。
	 */
	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(Arrays.asList(methods));
		return this;
	}

	/**
	 * 设置预检请求所允许的用于实际请求的头信息列表。
	 * <p>特殊值 {@code "*"} 可用于允许所有头信息。
	 * <p>根据 CORS 规范，如果是以下情况之一，则无需列出头名:
	 * {@code Cache-Control}、{@code Content-Language}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma}。
	 * <p>默认情况下允许所有头信息。
	 */
	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 设置实际响应可能具有并且可以公开的除了 "简单" 头信息之外的响应头信息列表，
	 * 即 {@code Cache-Control}、{@code Content-Language}、{@code Content-Type}、
	 * {@code Expires}、{@code Last-Modified} 或 {@code Pragma}。
	 * <p>特殊值 {@code "*"} 允许所有头信息对非凭证请求公开。
	 * <p>默认情况下未设置。
	 */
	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 浏览器是否应发送凭证，例如与跨域请求一起发送的 Cookie。
	 * 配置的值设置为预检请求的 {@code Access-Control-Allow-Credentials} 响应头信息。
	 * <p><strong>注意:</strong> 请注意，此选项与配置的域建立了高级别的信任关系，
	 * 并增加了 Web 应用程序的攻击面，因为它公开了敏感的用户特定信息，例如 Cookie 和 CSRF 令牌。
	 * <p>默认情况下未设置，因此 {@code Access-Control-Allow-Credentials} 头信息也未设置，
	 * 因此不允许凭证。
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	/**
	 * 配置预检请求的响应可以由客户端缓存多长时间（以秒为单位）。
	 * <p>默认情况下设置为 1800 秒（30 分钟）。
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * 将给定的 {@code CorsConfiguration} 应用于通过 {@link CorsConfiguration#combine(CorsConfiguration)}
	 * 配置的 {@code CorsConfiguration}，后者又使用 {@link CorsConfiguration#applyPermitDefaultValues()} 进行初始化。
	 *
	 * @param other 要应用的配置
	 * @since 5.3
	 */
	public CorsRegistration combine(CorsConfiguration other) {
		this.config = this.config.combine(other);
		return this;
	}

	protected String getPathPattern() {
		return this.pathPattern;
	}

	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}

}
