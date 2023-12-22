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
import java.util.Map;

/**
 * 注解表示方法参数应绑定到 Web 请求参数。
 *
 * <p>在 Spring MVC 和 Spring WebFlux 中的带注解处理方法中支持如下：
 * <ul>
 * <li>在 Spring MVC 中，“请求参数”对应查询参数、表单数据和多部分请求中的部分。这是因为 Servlet API 将查询参数和表单数据组合到称为“parameters”的单个映射中，
 *     并且它包括请求体的自动解析。
 * <li>在 Spring WebFlux 中，“请求参数”仅映射到查询参数。要处理所有 3 种类型，即查询、表单数据和多部分数据，您可以使用数据绑定到带有 {@link ModelAttribute} 注解的命令对象。
 * </ul>
 *
 * <p>如果方法参数类型为 {@link Map} 并指定了请求参数名称，则假定存在适当的转换策略，将请求参数值转换为 {@link Map}。
 *
 * <p>如果方法参数是 {@link java.util.Map Map&lt;String, String&gt;} 或
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}，并且未指定参数名称，
 * 则映射参数将填充所有请求参数名称和值。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see RequestMapping
 * @see RequestHeader
 * @see CookieValue
 * @since 2.5
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 绑定到的请求参数的名称。
	 *
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 参数是否为必需的。
	 * <p>默认为 {@code true}，如果请求中缺少参数，则会抛出异常。如果希望参数不存在于请求中时得到 {@code null} 值，请将其切换为 {@code false}。
	 * <p>或者，提供一个 {@link #defaultValue}，隐式将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 当请求参数未提供或其值为空时，用作回退的默认值。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}

