/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 特化的{@link Component @Component}，用于声明{@link ExceptionHandler @ExceptionHandler}、
 * {@link InitBinder @InitBinder}或{@link ModelAttribute @ModelAttribute}方法，以便在多个
 * {@code @Controller}类之间共享。
 *
 * <p>使用{@code @ControllerAdvice}注解的类可以显式声明为Spring bean，也可以通过类路径扫描自动检测。
 * 所有这些bean都基于{@link org.springframework.core.Ordered Ordered}语义或
 * {@link org.springframework.core.annotation.Order @Order} / {@link javax.annotation.Priority @Priority}声明进行排序，
 * 其中{@code Ordered}语义优先于{@code @Order} / {@code @Priority}声明。
 * 然后在运行时按此顺序应用{@code @ControllerAdvice} bean。
 * 但需要注意的是，实现{@link org.springframework.core.PriorityOrdered PriorityOrdered}的
 * {@code @ControllerAdvice} bean并不优先于实现{@code Ordered}的{@code @ControllerAdvice} bean。
 * 此外，{@code Ordered}对于范围{@code @ControllerAdvice} bean（例如配置为请求范围或会话范围的bean）不起作用。
 * 对于处理异常，首先匹配到的具有匹配异常处理方法的{@code @ExceptionHandler}将被选中。
 * 对于模型属性和数据绑定初始化，{@code @ModelAttribute}和{@code @InitBinder}方法将遵循
 * {@code @ControllerAdvice}顺序。
 *
 * <p>注意：对于{@code @ExceptionHandler}方法，在特定的advice bean的处理方法中，
 * 首选根异常匹配，而不是仅匹配当前异常的原因。
 * 然而，高优先级advice上的原因匹配仍将优先于低优先级advice bean上的任何匹配（无论是根级别还是原因级别的匹配）。
 * 因此，请在具有相应顺序的优先advice bean上声明主要的根异常映射。
 *
 * <p>默认情况下，{@code @ControllerAdvice}中的方法全局适用于所有控制器。
 * 使用选择器，如{@link #annotations}、{@link #basePackageClasses}和{@link #basePackages}
 * （或其别名{@link #value}）来定义更窄的目标控制器子集。
 * 如果声明了多个选择器，则应用布尔“或”逻辑，这意味着选定的控制器应至少匹配一个选择器。
 * 请注意，选择器检查是在运行时进行的，因此添加许多选择器可能会对性能产生负面影响并增加复杂性。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @see org.springframework.stereotype.Controller
 * @see RestControllerAdvice
 * @since 3.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ControllerAdvice {

	/**
	 * {@link #basePackages}属性的别名。
	 * <p>允许更简洁的注解声明 — 例如，{@code @ControllerAdvice("org.my.pkg")}等同于
	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")}.
	 *
	 * @see #basePackages
	 * @since 4.0
	 */
	@AliasFor("basePackages")
	String[] value() default {};

	/**
	 * 基础包数组。
	 * <p>属于这些基础包或其子包的控制器将被包含 — 例如，
	 * {@code @ControllerAdvice(basePackages = "org.my.pkg")}或
	 * {@code @ControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}.
	 * <p>{@link #value}是此属性的别名，只是允许更简洁地使用注解。
	 * <p>也可以考虑使用{@link #basePackageClasses}作为基于字符串的包名的类型安全替代方案。
	 *
	 * @since 4.0
	 */
	@AliasFor("value")
	String[] basePackages() default {};

	/**
	 * {@link #basePackages}的类型安全替代方案，用于指定选择控制器的包，以便由
	 * {@code @ControllerAdvice}注解类通知。
	 * <p>考虑在每个包中创建一个特殊的无操作标记类或接口，其唯一目的是被此属性引用。
	 *
	 * @since 4.0
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * 类数组。
	 * <p>分配给至少一个给定类型的控制器将由{@code @ControllerAdvice}注解类通知。
	 *
	 * @since 4.0
	 */
	Class<?>[] assignableTypes() default {};

	/**
	 * 注解类型数组。
	 * <p>带有至少一个提供的注解类型的控制器将由{@code @ControllerAdvice}注解类通知。
	 * <p>考虑创建自定义组合注解或使用预定义的注解，如{@link RestController @RestController}。
	 *
	 * @since 4.0
	 */
	Class<? extends Annotation>[] annotations() default {};

}
