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

import java.lang.annotation.*;

/**
 * 一个方便的注解，本身带有 {@link ControllerAdvice @ControllerAdvice} 和 {@link ResponseBody @ResponseBody} 注解。
 * <p>
 * 携带此注解的类型被视为控制器建议，其中 {@link ExceptionHandler @ExceptionHandler} 方法默认假定具有 {@link ResponseBody @ResponseBody} 语义。
 * <p>
 * 注意：如果配置了适当的 {@code HandlerMapping} - {@code HandlerAdapter} 对，例如 {@code RequestMappingHandlerMapping} - {@code RequestMappingHandlerAdapter} 对，
 * 则会处理 {@code @RestControllerAdvice}，它们是 MVC Java 配置和 MVC 命名空间中的默认选项。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see RestController
 * @see ControllerAdvice
 * @since 4.3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ControllerAdvice
@ResponseBody
public @interface RestControllerAdvice {

	/**
	 * {@link #basePackages} 属性的别名。
	 * <p>允许更简洁的注解声明，例如 {@code @RestControllerAdvice("org.my.pkg")} 相当于 {@code @RestControllerAdvice(basePackages = "org.my.pkg")}。
	 *
	 * @see #basePackages
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String[] value() default {};

	/**
	 * 基础包的数组。
	 * <p>属于这些基础包或其子包的控制器将被包含在内，例如 {@code @RestControllerAdvice(basePackages = "org.my.pkg")} 或 {@code @RestControllerAdvice(basePackages = {"org.my.pkg", "org.my.other.pkg"})}。
	 * <p>{@link #value} 是此属性的别名，简单地允许更简洁地使用注解。
	 * <p>还考虑使用 {@link #basePackageClasses} 作为基于类型安全的替代方案，而不是基于字符串的包名。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	String[] basePackages() default {};

	/**
	 * {@link ControllerAdvice @ControllerAdvice} 注解类中选择要建议的控制器的包的类型安全替代方案。
	 * <p>考虑在每个包中创建一个特殊的空操作标记类或接口，除了被此属性引用外，不起任何作用。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<?>[] basePackageClasses() default {};

	/**
	 * 类的数组。
	 * <p>至少与给定类型之一相匹配的控制器将由 {@code @RestControllerAdvice} 注解的类建议。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<?>[] assignableTypes() default {};

	/**
	 * 注解的数组。
	 * <p>至少使用其中一个提供的注解类型进行注释的控制器将由 {@code @RestControllerAdvice} 注解的类建议。
	 * <p>考虑创建一个自定义组合注解或使用预定义的注解，如 {@link RestController @RestController}。
	 */
	@AliasFor(annotation = ControllerAdvice.class)
	Class<? extends Annotation>[] annotations() default {};

}
