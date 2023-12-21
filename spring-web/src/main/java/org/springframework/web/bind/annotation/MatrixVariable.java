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
 * 注解指示方法参数应绑定到路径段中的名称-值对。
 * 仅支持 {@link RequestMapping} 注解的处理器方法。
 *
 * <p>如果方法参数类型为 {@link java.util.Map}，并指定了矩阵变量名称，
 * 那么矩阵变量值将被转换为 {@link java.util.Map}，假定存在适当的转换策略。
 *
 * <p>如果方法参数是 {@link java.util.Map Map&lt;String, String&gt;} 或
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}，
 * 并且未指定变量名称，则该映射将填充所有矩阵变量名称和值。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MatrixVariable {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 矩阵变量的名称。
	 *
	 * @see #value
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 矩阵变量所在的 URI 路径变量的名称，如果需要消除歧义（例如，在多个路径段中存在具有相同名称的矩阵变量）。
	 */
	String pathVar() default ValueConstants.DEFAULT_NONE;

	/**
	 * 矩阵变量是否为必需的。
	 * <p>默认为 {@code true}，如果请求中缺少变量，则会引发异常。
	 * 将其切换为 {@code false}，如果您希望在变量丢失时获得 {@code null}。
	 * <p>或者，提供一个 {@link #defaultValue}，隐式地将此标志设置为 {@code false}。
	 */
	boolean required() default true;

	/**
	 * 作为回退的默认值使用。
	 * <p>提供默认值会隐式地将 {@link #required} 设置为 {@code false}。
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;
}
