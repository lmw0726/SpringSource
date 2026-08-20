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
 * 表示被注解的类是一个"Repository"（仓储），最初由领域驱动设计（Evans, 2003）定义为
 * "一种封装存储、检索和搜索行为的机制，模拟对象集合"。
 *
 * <p>实现传统 Java EE 模式（如"Data Access Object"）的团队也可以将此构造型应用于
 * DAO 类，但在这样做之前应该理解数据访问对象与 DDD 风格仓储之间的区别。
 * 此注解是一个通用的构造型，各个团队可以根据需要缩小其语义和用法范围。
 *
 * <p>这样注解的类在与 {@link
 * org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 * PersistenceExceptionTranslationPostProcessor} 结合使用时，符合 Spring
 * {@link org.springframework.dao.DataAccessException DataAccessException} 翻译的条件。
 * 被注解的类还可以明确其在整体应用程序架构中的角色，用于工具、切面等目的。
 *
 * <p>从 Spring 2.5 开始，此注解还作为 {@link Component @Component} 的特化，
 * 允许实现类通过类路径扫描被自动检测。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see Component
 * @see Service
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Repository {

	/**
	 * 该值可能指示逻辑组件名称的建议，
	 * 在自动检测组件的情况下将转换为 Spring bean。
	 * @return 建议的组件名称（如果有），否则为空字符串
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
