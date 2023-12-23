/*
 * Copyright 2002-2022 the original author or authors.
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
import org.springframework.ui.Model;

import java.lang.annotation.*;

/**
 * 注解将方法参数或方法返回值绑定到命名的模型属性，并暴露给 Web 视图。支持带有 {@link RequestMapping @RequestMapping} 方法的控制器类。
 *
 * <p><strong>警告</strong>：数据绑定可能导致安全问题，通过暴露不应由外部客户端访问或修改的对象图的部分。因此，应仔细考虑数据绑定的设计和使用，涉及到安全性时尤为重要。有关详细信息，请参阅
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * 参考手册中关于数据绑定的专门部分。
 *
 * <p>{@code @ModelAttribute} 可以用于通过在 {@link RequestMapping @RequestMapping} 方法的相应参数上注解来将命令对象暴露给 Web 视图。
 *
 * <p>{@code @ModelAttribute} 还可用于通过在控制器类中的 {@link RequestMapping @RequestMapping} 方法上注解的访问器方法来将引用数据暴露给 Web 视图。
 * 此类访问器方法允许具有 {@link RequestMapping @RequestMapping} 方法支持的任何参数，并返回要暴露的模型属性值。
 *
 * <p>然而请注意，当请求处理导致 {@code Exception} 时，引用数据和所有其他模型内容在 Web 视图中是不可用的，因为异常可能在任何时候被引发，
 * 使得模型的内容不可靠。因此，{@link ExceptionHandler @ExceptionHandler} 方法不提供 {@link Model} 参数的访问权限。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see ControllerAdvice
 * @since 2.5
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModelAttribute {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到的模型属性的名称。
	 * <p>默认模型属性名称是根据声明的属性类型（即方法参数类型或方法返回类型）从非限定类名推断出的：
	 * 例如，对于类 "mypackage.OrderAddress"，模型属性名称为 "orderAddress"，
	 * 或者对于 "List&lt;mypackage.OrderAddress&gt;"，模型属性名称为 "orderAddressList"。
	 *
	 * @since 4.3
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 允许在 {@code @ModelAttribute} 方法参数或 {@code @ModelAttribute} 方法返回的属性上直接禁用数据绑定，
	 * 从而阻止该属性的数据绑定。
	 * <p>默认情况下，此选项设置为 {@code true}，即应用数据绑定。将其设置为 {@code false} 以禁用数据绑定。
	 *
	 * @since 4.3
	 */
	boolean binding() default true;

}
