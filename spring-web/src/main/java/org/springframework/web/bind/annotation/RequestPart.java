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
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;

import java.beans.PropertyEditor;
import java.lang.annotation.*;

/**
 * 注解可用于将“multipart/form-data”请求的一部分与方法参数关联起来。
 * <p>
 * 支持的方法参数类型包括与 Spring 的 {@link MultipartResolver} 抽象配合使用的 {@link MultipartFile}，
 * 与 Servlet 3.0 多部分请求配合使用的 {@code javax.servlet.http.Part}，或者对于任何其他方法参数，
 * 该部分的内容都会通过 {@link HttpMessageConverter} 传递，考虑到请求部分的 'Content-Type' 头。
 * 这类似于 @{@link RequestBody} 根据常规请求的内容解析参数的方式。
 * <p>
 * 注意，@{@link RequestParam} 注解也可用于将“multipart/form-data”请求的一部分与支持相同方法参数类型的方法参数关联起来。
 * 主要区别在于当方法参数不是 String 或原始的 {@code MultipartFile} / {@code Part} 时，
 * {@code @RequestParam} 依赖于通过已注册的 {@link Converter} 或 {@link PropertyEditor} 进行类型转换，
 * 而 {@link RequestPart} 则依赖于 {@link HttpMessageConverter HttpMessageConverters}，
 * 考虑到请求部分的 'Content-Type' 头。
 * {@link RequestParam} 可能与名称-值表单字段一起使用，而 {@link RequestPart} 可能与包含更复杂内容的部分一起使用，例如 JSON、XML。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @see RequestParam
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * @since 3.1
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPart {

	/**
	 * {@link #name} 的别名。
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * 要绑定到“multipart/form-data”请求中的部分的名称。
	 *
	 * @since 4.2
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 部分是否是必需的。
	 * <p>默认为 {@code true}，如果请求中缺少该部分，则会抛出异常。
	 * 如果希望在请求中部分不存在时得到 {@code null} 值，则将其切换为 {@code false}。
	 */
	boolean required() default true;

}
