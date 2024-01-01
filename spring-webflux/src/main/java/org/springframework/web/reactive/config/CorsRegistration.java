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

package org.springframework.web.reactive.config;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

/**
 * 用于创建给定 URL 路径模式的 {@link CorsConfiguration} 实例的辅助类。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @see CorsConfiguration
 * @see CorsRegistry
 * @since 5.0
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
	 * 设置从浏览器允许的跨源请求的来源。
	 * 请参阅 {@link CorsConfiguration#setAllowedOrigins(List)} 以获取格式详细信息和其他注意事项。
	 *
	 * <p>默认情况下，允许所有来源，但如果还设置了
	 * {@link #allowedOriginPatterns(String...) allowedOriginPatterns}，则优先考虑那个。
	 *
	 * @see #allowedOriginPatterns(String...)
	 */
	public CorsRegistration allowedOrigins(String... origins) {
		this.config.setAllowedOrigins(Arrays.asList(origins));
		return this;
	}

	/**
	 * {@link #allowedOrigins(String...)} 的替代方法，支持更灵活的模式来指定允许从浏览器发出的跨源
	 * 请求的来源。请参阅 {@link CorsConfiguration#setAllowedOriginPatterns(List)} 获取格式详细信息和其他注意事项。
	 * <p>默认情况下，不设置。
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
	 * <p>默认情况下，允许 "简单" 方法 {@code GET}、{@code HEAD} 和 {@code POST}。
	 */
	public CorsRegistration allowedMethods(String... methods) {
		this.config.setAllowedMethods(Arrays.asList(methods));
		return this;
	}

	/**
	 * 设置预检请求可以列为实际请求期间允许使用的头的列表。
	 * <p>特殊值 {@code "*"} 可用于允许所有头。
	 * <p>如果是以下之一，则无需列出标头名称：
	 * {@code Cache-Control}、{@code Content-Language}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma}，根据 CORS 规范。
	 * <p>默认情况下，允许所有头。
	 */
	public CorsRegistration allowedHeaders(String... headers) {
		this.config.setAllowedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 设置实际响应可能具有并且可以公开的除了 "简单" 头之外的响应头，
	 * 即 {@code Cache-Control}、{@code Content-Language}、{@code Content-Type}、
	 * {@code Expires}、{@code Last-Modified} 或 {@code Pragma}。
	 * <p>特殊值 {@code "*"} 允许将所有头公开给非凭证请求。
	 * <p>默认情况下，不设置。
	 */
	public CorsRegistration exposedHeaders(String... headers) {
		this.config.setExposedHeaders(Arrays.asList(headers));
		return this;
	}

	/**
	 * 浏览器是否应发送凭据，例如在跨域请求时与 cookies 一起发送到注解的端点。
	 * 配置的值设置在预检请求的 {@code Access-Control-Allow-Credentials} 响应头上。
	 * <p><strong>注意：</strong>请注意，此选项与配置的域建立了高度的信任级别，
	 * 并通过公开敏感用户特定信息（如 cookies 和 CSRF 令牌）增加了 Web 应用程序的攻击面。
	 * <p>默认情况下，未设置此选项，此时 {@code Access-Control-Allow-Credentials} 标头也未设置，
	 * 因此不允许凭证。
	 */
	public CorsRegistration allowCredentials(boolean allowCredentials) {
		this.config.setAllowCredentials(allowCredentials);
		return this;
	}

	/**
	 * 配置预检请求的响应可以被客户端缓存多少秒。
	 * <p>默认情况下，设置为 1800 秒（30 分钟）。
	 */
	public CorsRegistration maxAge(long maxAge) {
		this.config.setMaxAge(maxAge);
		return this;
	}

	/**
	 * 将给定的 {@code CorsConfiguration} 应用于通过
	 * {@link CorsConfiguration#combine(CorsConfiguration)} 配置的正在配置的配置，
	 * 后者已通过 {@link CorsConfiguration#applyPermitDefaultValues()} 进行初始化。
	 *
	 * @param other 要应用的配置
	 * @since 5.3
	 */
	public CorsRegistration combine(CorsConfiguration other) {
		this.config = this.config.combine(other);
		return this;
	}

	/**
	 * 获取路径模式。
	 * @return 路径模式
	 */
	protected String getPathPattern() {
		return this.pathPattern;
	}


	/**
	 * 获取 CORS 配置信息。
	 * @return CORS 配置
	 */
	protected CorsConfiguration getCorsConfiguration() {
		return this.config;
	}

}
