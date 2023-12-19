/*
 * Copyright 2002-2020 the original author or authors.
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
 * Annotation for mapping web requests onto methods in request-handling classes
 * with flexible method signatures.
 *
 * <p>Both Spring MVC and Spring WebFlux support this annotation through a
 * {@code RequestMappingHandlerMapping} and {@code RequestMappingHandlerAdapter}
 * in their respective modules and package structure. For the exact list of
 * supported handler method arguments and return types in each, please use the
 * reference documentation links below:
 * <ul>
 * <li>Spring MVC
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-arguments">Method Arguments</a>
 * and
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-return-types">Return Values</a>
 * </li>
 * <li>Spring WebFlux
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-arguments">Method Arguments</a>
 * and
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-return-types">Return Values</a>
 * </li>
 * </ul>
 *
 * <p><strong>Note:</strong> This annotation can be used both at the class and
 * at the method level. In most cases, at the method level applications will
 * prefer to use one of the HTTP method specific variants
 * {@link GetMapping @GetMapping}, {@link PostMapping @PostMapping},
 * {@link PutMapping @PutMapping}, {@link DeleteMapping @DeleteMapping}, or
 * {@link PatchMapping @PatchMapping}.</p>
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @RequestMapping} and {@code @SessionAttributes} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see PatchMapping
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * 为此映射分配一个名称。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 当同时在两个级别上使用时，通过使用“#”作为分隔符连接来派生组合名称。
	 * @see org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
	 * @see org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 */
	String name() default "";

	/**
	 * 此注解表示的主要映射。
	 * <p>这是 {@link #path} 的别名。例如，
	 * {@code @RequestMapping("/foo")} 等效于
	 * {@code @RequestMapping(path="/foo")}。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 当在类型级别使用时，所有方法级别的映射都会继承此主要映射，将其缩小到特定的处理程序方法。
	 * <p><strong>注意</strong>：未映射到任何路径的处理程序方法实际上被映射到空路径。
	 */
	@AliasFor("path")
	String[] value() default {};

	/**
	 * 路径映射的 URI（例如 {@code "/profile"}）。
	 * <p>还支持 Ant 风格的路径模式（例如 {@code "/profile/**"}）。
	 * 在方法级别，相对路径（例如 {@code "edit"}）支持在类型级别表示的主要映射内。
	 * 路径映射的 URI 可能包含占位符（例如 <code>"/${profile_path}"</code>）。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 当在类型级别使用时，所有方法级别的映射都会继承此主要映射，将其缩小到特定的处理程序方法。
	 * <p><strong>注意</strong>：未映射到任何路径的处理程序方法实际上被映射到空路径。
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] path() default {};

	/**
	 * 映射到的 HTTP 请求方法，缩小主要映射范围：
	 * GET、POST、HEAD、OPTIONS、PUT、PATCH、DELETE、TRACE。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 当在类型级别使用时，所有方法级别的映射都会继承此 HTTP 方法限制。
	 */
	RequestMethod[] method() default {};

	/**
	 * 映射请求的参数，缩小主要映射范围。
	 * <p>任何环境下都采用相同的格式：一系列 “myParam=myValue” 样式表达式，
	 * 仅当每个参数被发现具有给定值时，请求才会被映射。表达式可以使用“!=”操作符否定，
	 * 例如“myParam!=myValue”。还支持“myParam”样式的表达式，这些参数必须出现在请求中
	 * （允许具有任何值）。最后，“!myParam”样式的表达式表示指定的参数不应该出现在请求中。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 当在类型级别使用时，所有方法级别的映射都会继承此参数限制。
	 */
	String[] params() default {};

	/**
	 * 映射请求的头部，缩小主要映射范围。
	 * <p>对于任何环境，格式相同：一系列 “My-Header=myValue” 样式的表达式，
	 * 仅当每个头部具有给定值时，请求才会被映射。表达式可以使用“!=”操作符否定，
	 * 如“My-Header!=myValue”。还支持“My-Header”样式的表达式，这些头部必须存在于请求中
	 * （允许具有任何值）。最后，“!My-Header”样式的表达式表示指定的头部不应该存在于请求中。
	 * <p>还支持媒体类型通配符（*），用于诸如 Accept 和 Content-Type 的头部。例如，
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * 将匹配具有 Content-Type 为 "text/html"、"text/plain" 等的请求。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 在类型级别使用时，所有方法级别的映射都会继承此头部限制。
	 * @see org.springframework.http.MediaType
	 */
	String[] headers() default {};

	/**
	 * 通过可以由映射处理程序消耗的媒体类型缩小主要映射范围。
	 * 由多个媒体类型组成，其中之一必须与请求的 {@code Content-Type} 头部匹配。示例：
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * consumes = MediaType.TEXT_PLAIN_VALUE
	 * </pre>
	 * 表达式可以使用“!”操作符否定，例如“!text/plain”，匹配所有请求的 {@code Content-Type}
	 * 不是 "text/plain" 的情况。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 如果在两个级别都指定，则方法级别的 consumes 条件会覆盖类型级别的条件。
	 * @see org.springframework.http.MediaType
	 * @see javax.servlet.http.HttpServletRequest#getContentType()
	 */
	String[] consumes() default {};

	/**
	 * 通过可以由映射处理程序生成的媒体类型缩小主要映射范围。
	 * 由多个媒体类型组成，其中之一必须通过与请求的 “可接受” 媒体类型进行内容协商而选择。
	 * 通常，这些媒体类型是从 {@code "Accept"} 头部提取的，但也可能来自查询参数或其他地方。
	 * 示例：
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = MediaType.TEXT_PLAIN_VALUE
	 * produces = "text/plain;charset=UTF-8"
	 * </pre>
	 * <p>如果声明的媒体类型包含参数（例如 "charset=UTF-8"、"type=feed"、"type=entry"），
	 * 并且如果请求的兼容媒体类型也具有该参数，则参数值必须匹配。否则，
	 * 如果请求的媒体类型不包含该参数，则假定客户端接受任何值。
	 * <p>表达式可以使用“!”操作符否定，例如“!text/plain”，匹配所有请求的 {@code Accept}
	 * 不是 "text/plain" 的情况。
	 * <p><b>支持类型级别和方法级别！</b>
	 * 如果在两个级别都指定，则方法级别的 produces 条件会覆盖类型级别的条件。
	 * @see org.springframework.http.MediaType
	 */
	String[] produces() default {};

}
