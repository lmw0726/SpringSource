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

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 表示被注解的类是一个"Service（服务）"，最初由领域驱动设计（Evans, 2003）定义为
 * "作为独立接口提供的操作，模型中不包含封装状态"。
 *
 * <p>也可表示一个类是"Business Service Facade（业务服务外观）"（符合 Core J2EE
 * 设计模式的含义），或类似的东西。此注解是一个通用的刻板印象（stereotype），
 * 各团队可根据实际情况缩小其语义范围并适当使用。
 *
 * <p>此注解作为 {@link Component @Component} 的特化，
 * 允许实现类通过类路径扫描被自动检测。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see Component
 * @see Repository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Service {

	/**
	 * 该值可指定一个逻辑组件名称的建议，
	 * 在自动检测组件时将其转换为 Spring bean。
	 * @return 建议的组件名称（如有），否则返回空字符串
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
