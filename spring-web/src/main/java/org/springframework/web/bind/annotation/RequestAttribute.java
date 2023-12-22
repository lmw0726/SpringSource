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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 注解用于将方法参数绑定到请求属性。
 *
 * <p>主要目的是为了从控制器方法方便地访问请求属性，可以进行可选/必需检查，并将其转换为目标方法参数类型。
 *
 * @author Rossen Stoyanchev
 * @see RequestMapping
 * @see SessionAttribute
 * @since 4.3
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestAttribute {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的请求属性的名称。
	 * <p>默认名称从方法参数名称推断出来。
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 请求属性是否为必需。
	 * <p>默认为 {@code true}，如果属性丢失，则会抛出异常。
	 * 如果您希望 {@code null} 或 Java 8 的 {@code java.util.Optional} 来表示属性不存在，请将其切换为 {@code false}。
	 */
	boolean required() default true;

}
