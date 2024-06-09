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
 * 用于将 HTTP {@code PATCH} 请求映射到特定处理程序方法的注解。
 * <p>
 * 具体来说，{@code @PatchMapping} 是一个组合注解，作为 {@code @RequestMapping(method = RequestMethod.PATCH)} 的快捷方式。
 *
 * @author Sam Brannen
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see RequestMapping
 * @since 4.3
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.PATCH)
public @interface PatchMapping {

	/**
	 * {@link RequestMapping#name} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String name() default "";

	/**
	 * {@link RequestMapping#value} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] value() default {};

	/**
	 * {@link RequestMapping#path} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] path() default {};

	/**
	 * {@link RequestMapping#params} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] params() default {};

	/**
	 * {@link RequestMapping#headers} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] headers() default {};

	/**
	 * {@link RequestMapping#consumes} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] consumes() default {};

	/**
	 * {@link RequestMapping#produces} 的别名。
	 */
	@AliasFor(annotation = RequestMapping.class)
	String[] produces() default {};

}
