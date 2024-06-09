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

import org.springframework.http.converter.HttpMessageConverter;

import java.lang.annotation.*;

/**
 * 表示方法参数应绑定到 Web 请求的主体的注解。
 * 请求的主体通过 {@link HttpMessageConverter} 传递以解析方法参数，具体取决于请求的内容类型。
 * 可选地，可以通过在参数上注释 {@code @Valid} 来应用自动验证。
 * <p>
 * 支持使用注解的处理程序方法。
 *
 * @author Arjen Poutsma
 * @see RequestHeader
 * @see ResponseBody
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * @since 3.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {

	/**
	 * 是否需要主体内容。
	 * <p>默认为 {@code true}，表示在没有主体内容时会抛出异常。
	 * 如果希望在没有主体内容时传递 {@code null}，请将其切换为 {@code false}。
	 *
	 * @since 3.2
	 */
	boolean required() default true;

}
