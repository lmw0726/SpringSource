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
import org.springframework.http.HttpStatus;

import java.lang.annotation.*;

/**
 * 使用 {@link #code} 和 {@link #reason} 标记方法或异常类应返回的状态。
 *
 * <p>当调用处理程序方法时，状态码将应用于 HTTP 响应，并覆盖通过其他方式设置的状态信息，如 {@code ResponseEntity} 或 {@code "redirect:"}。
 *
 * <p><strong>警告</strong>：在异常类上使用此注解，或在此注解的 {@code reason} 属性上设置值时，将使用 {@code HttpServletResponse.sendError} 方法。
 *
 * <p>使用 {@code HttpServletResponse.sendError}，响应被视为已完成，并且不应进一步写入。此外，Servlet 容器通常会编写 HTML 错误页面，因此不适合在 REST API 中使用 {@code reason}。
 * 对于这种情况，最好使用 {@link org.springframework.http.ResponseEntity} 作为返回类型，并完全避免使用 {@code @ResponseStatus}。
 *
 * <p>请注意，控制器类还可以使用 {@code @ResponseStatus} 进行注释，然后该注解将被该类及其子类中的所有 {@code @RequestMapping} 和 {@code @ExceptionHandler} 方法继承，除非在方法上使用本地的 {@code @ResponseStatus} 声明进行覆盖。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @see org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
 * @since 3.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {

	/**
	 * {@link #code} 的别名。
	 */
	@AliasFor("code")
	HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * 用于响应的状态 <em>code</em>。
	 * <p>默认为 {@link HttpStatus#INTERNAL_SERVER_ERROR}，通常应更改为更合适的值。
	 *
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 * @since 4.2
	 */
	@AliasFor("value")
	HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 * 用于响应的 <em>reason</em>。
	 * <p>默认为空字符串，将被忽略。将原因设置为非空值以将其用于响应。
	 *
	 * @see javax.servlet.http.HttpServletResponse#sendError(int, String)
	 */
	String reason() default "";

}
