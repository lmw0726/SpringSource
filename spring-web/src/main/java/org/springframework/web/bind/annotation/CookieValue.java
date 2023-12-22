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

import java.lang.annotation.*;

/**
 * 表示方法参数绑定到 HTTP cookie 的注解。
 *
 * <p>方法参数可以声明为 {@link javax.servlet.http.Cookie} 类型，
 * 或者作为 cookie 值的类型（String、int 等）。
 *
 * <p>请注意，在 spring-webmvc 5.3.x 及更早版本中，cookie 值会进行 URL 解码。
 * 这将在 6.0 版本中更改，但在此期间，应用程序还可以声明类型为 {@link javax.servlet.http.Cookie} 的参数以访问原始值。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see RequestMapping
 * @see RequestParam
 * @see RequestHeader
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @since 3.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CookieValue {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 绑定的 cookie 名称。
	 *
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 是否需要 cookie。
	 * <p>默认为 {@code true}，如果请求中缺少 cookie，则会抛出异常。
	 * 将此值更改为 {@code false}，如果您希望在请求中缺少 cookie 时得到 {@code null} 值。
	 * <p>或者，提供一个 {@link #defaultValue}，隐式将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 作为回退的默认值。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
