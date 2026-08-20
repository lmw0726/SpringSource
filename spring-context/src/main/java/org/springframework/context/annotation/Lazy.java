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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示 bean 是否应延迟初始化。
 *
 * <p>可用于任何直接或间接使用 {@link
 * org.springframework.stereotype.Component @Component} 注解的类上，或在使用
 * {@link Bean @Bean} 注解的方法上。
 *
 * <p>如果 {@code @Component} 或 {@code @Bean} 定义上没有此注解，
 * 则会进行急切初始化。如果存在并设置为 {@code true}，则 {@code @Bean} 或
 * {@code @Component} 将不会被初始化，直到被另一个 bean 引用或从
 * {@link org.springframework.beans.factory.BeanFactory
 * BeanFactory} 中显式获取。如果存在并设置为 {@code false}，则 bean 将在启动时
 * 由执行单例急切初始化的 bean 工厂实例化。
 *
 * <p>如果 {@link Configuration @Configuration} 类上存在 Lazy 注解，
 * 这表明该 {@code @Configuration} 中的所有 {@code @Bean} 方法都应延迟初始化。
 * 如果 {@code @Lazy} 注解在 {@code @Lazy} 注解的 {@code @Configuration} 类中的
 * {@code @Bean} 方法上存在且设置为 false，则表明覆盖了"默认延迟"行为，
 * bean 应被急切初始化。
 *
 * <p>除了在组件初始化中的角色外，此注解也可放置在
 * 使用 {@link org.springframework.beans.factory.annotation.Autowired}
 * 或 {@link javax.inject.Inject} 标记的注入点上：在此上下文中，
 * 它会导致为所有受影响的依赖创建延迟解析代理，
 * 作为使用 {@link org.springframework.beans.factory.ObjectFactory} 或
 * {@link javax.inject.Provider} 的替代方案。请注意，这种延迟解析代理将始终被注入；
 * 如果目标依赖不存在，您只能通过调用时的异常来发现。
 * 因此，这种注入点对于可选依赖会导致不直观的行为。
 * 对于编程等效方式，允许更复杂的延迟引用，请考虑使用
 * {@link org.springframework.beans.factory.ObjectProvider}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Primary
 * @see Bean
 * @see Configuration
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lazy {


	/**
	 * 是否应进行延迟初始化。
	 */
	boolean value() default true;

}