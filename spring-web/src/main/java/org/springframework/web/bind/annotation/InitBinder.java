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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注解标识初始化{@link org.springframework.web.bind.WebDataBinder}的方法，
 * 该Binder将用于填充带有注解处理程序方法的命令和表单对象参数。
 *
 * <p><strong>警告</strong>：数据绑定可能导致安全问题，通过暴露对象图中不应由外部客户端访问或修改的部分。因此，应谨慎考虑数据绑定的设计和使用，特别是在安全方面。有关详细信息，请参阅参考手册中关于
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * 数据绑定的专门章节。
 *
 * <p>{@code @InitBinder}方法支持所有{@link RequestMapping @RequestMapping}方法支持的参数，但不支持命令/表单对象及其相应的验证结果对象。{@code @InitBinder}方法不得有返回值；它们通常声明为{@code void}。
 *
 * <p>常见参数是{@link org.springframework.web.bind.WebDataBinder}，结合{@link org.springframework.web.context.request.WebRequest}
 * 或{@link java.util.Locale}，允许注册特定上下文的编辑器。
 *
 * @author Juergen Hoeller
 * @see ControllerAdvice
 * @see org.springframework.web.bind.WebDataBinder
 * @see org.springframework.web.context.request.WebRequest
 * @since 2.5
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InitBinder {

	/**
	 * 此init-binder方法应用的命令/表单属性和/或请求参数的名称。
	 * <p>默认值是应用于由注解处理程序类处理的所有命令/表单属性和所有请求参数。在此处指定模型属性名称或请求参数名称将限制init-binder方法仅适用于那些特定的属性/参数，通常不同的init-binder方法适用于不同的属性或参数组。
	 */
	String[] value() default {};

}
