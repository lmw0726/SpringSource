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
 * 将方法参数绑定到会话属性的注解。
 * <p>
 * 主要目的是为了提供方便访问现有的永久会话属性（例如用户身份验证对象），并可选择/必需的检查以及将其转换为目标方法参数类型。
 * <p>
 * 对于需要添加或删除会话属性的用例，请考虑将 {@code org.springframework.web.context.request.WebRequest} 或
 * {@code javax.servlet.http.HttpSession} 注入到控制器方法中。
 * <p>
 * 对于在控制器的工作流程中将模型属性临时存储在会话中，请考虑改用 {@link SessionAttributes}。
 *
 * @author Rossen Stoyanchev
 * @see RequestMapping
 * @see SessionAttributes
 * @see RequestAttribute
 * @since 4.3
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SessionAttribute {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的会话属性的名称。
	 * <p>默认名称是从方法参数名称推断出来的。
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 会话属性是否为必需。
	 * <p>默认为 {@code true}，如果会话中缺少属性或者没有会话，则会抛出异常。
	 * 如果希望不存在属性时返回 {@code null} 或 Java 8 {@code java.util.Optional}，则将此切换为 {@code false}。
	 */
	boolean required() default true;

}
