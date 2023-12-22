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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 该注解指示方法参数应绑定到 web 请求的头部。
 *
 * <p>在 Spring MVC 和 Spring WebFlux 中支持带注解的处理器方法。
 *
 * <p>如果方法参数是 {@link java.util.Map Map&lt;String, String&gt;}、
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;} 或
 * {@link org.springframework.http.HttpHeaders HttpHeaders}，
 * 那么该映射将包含所有头部名称和值。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see RequestMapping
 * @see RequestParam
 * @see CookieValue
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestHeader {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的请求头的名称。
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 头部是否为必需。
	 * <p>默认为 {@code true}，如果请求中缺少头部，则会抛出异常。
	 * 如果希望头部不存在时获得 {@code null} 值，请将其切换为 {@code false}。
	 * <p>或者，提供一个 {@link #defaultValue}，它隐式地将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 作为备用的默认值。
	 * <p>提供默认值将隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
