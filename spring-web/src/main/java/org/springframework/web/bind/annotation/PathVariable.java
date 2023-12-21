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
 * 注解指示方法参数应绑定到 URI 模板变量。支持 {@link RequestMapping} 注解的处理程序方法。
 *
 * <p>如果方法参数是 {@link java.util.Map Map&lt;String, String&gt;}，
 * 则该 Map 将包含所有路径变量名称和值。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see RequestMapping
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * @since 3.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定的路径变量的名称。
	 *
	 * @since 4.3.3
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 路径变量是否为必需的。
	 * <p>默认为 {@code true}，如果传入请求中缺少路径变量，则会引发异常。
	 * 如果您希望在此情况下得到 {@code null} 或 Java 8 {@code java.util.Optional}，
	 * 则将其切换为 {@code false}。例如，在为不同请求提供服务的 {@code ModelAttribute} 方法上。
	 *
	 * @since 4.3.3
	 */
	boolean required() default true;

}
