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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.cors.CorsConfiguration;

import java.lang.annotation.*;
import java.util.List;

/**
 * 用于允许特定处理程序类和/或处理程序方法上的跨源请求的注解。如果配置了适当的 {@code HandlerMapping}，
 * 则会处理该注解。
 *
 * <p>Spring Web MVC 和 Spring WebFlux 都通过它们各自模块中的 {@code RequestMappingHandlerMapping} 支持此注解。
 * 每个类型和方法级别注解对应的值将添加到 {@link CorsConfiguration} 中，然后通过 {@link CorsConfiguration#applyPermitDefaultValues()} 应用默认值。
 *
 * <p>全局和本地配置组合的规则通常是添加性的 -- 例如，所有全局和所有本地源。对于仅可以接受单个值的属性，如 {@code allowCredentials}
 * 和 {@code maxAge}，本地值将覆盖全局值。有关更多详情，请参阅 {@link CorsConfiguration#combine(CorsConfiguration)}。
 *
 * @author Russell Allen
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Ruslan Akhundov
 * @since 4.2
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {

	/**
	 * @deprecated 自 Spring 5.0 起弃用，使用 {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	String[] DEFAULT_ORIGINS = {"*"};

	/**
	 * @deprecated 自 Spring 5.0 起弃用，使用 {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	String[] DEFAULT_ALLOWED_HEADERS = {"*"};

	/**
	 * @deprecated 自 Spring 5.0 起弃用，使用 {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	boolean DEFAULT_ALLOW_CREDENTIALS = false;

	/**
	 * @deprecated 自 Spring 5.0 起弃用，使用 {@link CorsConfiguration#applyPermitDefaultValues}
	 */
	@Deprecated
	long DEFAULT_MAX_AGE = 1800;


	/**
	 * {@link #origins} 的别名。
	 */
	@AliasFor("origins")
	String[] value() default {};

	/**
	 * 允许跨源请求的源列表。请参阅 {@link CorsConfiguration#setAllowedOrigins(List)} 了解详情。
	 * <p>默认情况下，除非 {@link #originPatterns} 也被设置了，否则允许所有源。
	 */
	@AliasFor("value")
	String[] origins() default {};

	/**
	 * 替代 {@link #origins}，支持更灵活的源模式。请参阅 {@link CorsConfiguration#setAllowedOriginPatterns(List)} 了解详情。
	 * <p>默认情况下，不设置此值。
	 *
	 * @since 5.3
	 */
	String[] originPatterns() default {};

	/**
	 * 允许实际请求中的请求头列表，可能为 {@code "*"} 以允许所有请求头。
	 * <p>请求的头将列在预检请求的 {@code Access-Control-Allow-Headers} 响应头中。
	 * <p>如果请求头是 {@code Cache-Control}、{@code Content-Language}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma} 之一，则不需要将其列出，根据 CORS 规范。
	 * <p>默认情况下，允许所有请求头。
	 */
	String[] allowedHeaders() default {};

	/**
	 * 用户代理将允许客户端访问的实际响应中的响应头列表，除了 "simple" 头，即
	 * {@code Cache-Control}、{@code Content-Language}、{@code Content-Type}、
	 * {@code Expires}、{@code Last-Modified} 或 {@code Pragma}。
	 * <p>暴露的头将列在实际 CORS 请求的 {@code Access-Control-Expose-Headers} 响应头中。
	 * <p>特殊值 {@code "*"} 允许非凭证请求暴露所有头。
	 * <p>默认情况下，不列出任何头。
	 */
	String[] exposedHeaders() default {};

	/**
	 * 支持的 HTTP 请求方法列表。
	 * <p>默认情况下，支持的方法与控制器方法映射的方法相同。
	 */
	RequestMethod[] methods() default {};

	/**
	 * 是否应该在跨域请求中向注解的端点发送凭据，例如 cookie。配置的值设置为预检请求的
	 * {@code Access-Control-Allow-Credentials} 响应头。
	 * <p><strong>注意：</strong>请注意，此选项与配置的域建立了高度信任，并且通过公开敏感的用户特定信息，
	 * 如 cookie 和 CSRF 令牌，增加了 Web 应用程序的攻击面。
	 * <p>默认情况下，未设置此项，因此也不设置 {@code Access-Control-Allow-Credentials} 头，
	 * 因此不允许凭据。
	 */
	String allowCredentials() default "";

	/**
	 * 预检响应的缓存持续时间的最大年龄（以秒为单位）。
	 * <p>此属性控制预检请求的 {@code Access-Control-Max-Age} 响应头的值。
	 * <p>设置合理的值可以减少浏览器需要的预检请求/响应交互次数。负值表示 <em>未定义</em>。
	 * <p>默认情况下，此项设置为 {@code 1800} 秒（30 分钟）。
	 */
	long maxAge() default -1;

}
