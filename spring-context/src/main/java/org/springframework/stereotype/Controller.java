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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 表明被注解的类是一个"控制器"（例如 Web 控制器）。
 *
 * <p>此注解是 {@link Component @Component} 的特化形式，
 * 允许实现类通过类路径扫描被自动检测。
 * 它通常与基于 {@link org.springframework.web.bind.annotation.RequestMapping}
 * 注解的注解处理方法结合使用。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 2.5
 * @see Component
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see org.springframework.context.annotation.ClassPathBeanDefinitionScanner
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {

	/**
	 * 该值可能表示逻辑组件名称的建议，
	 * 在自动检测组件的情况下将其转换为 Spring Bean。
	 * @return 建议的组件名称（如果有），否则为空字符串
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
