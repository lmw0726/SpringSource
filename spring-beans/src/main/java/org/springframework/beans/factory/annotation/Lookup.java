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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 一个注解，用于标识“查找（lookup）”方法，由容器重写以将调用重定向回 {@link org.springframework.beans.factory.BeanFactory}
 * 进行 {@code getBean} 调用。本质上，这是基于注解的 XML {@code lookup-method} 属性的版本，
 * 在运行时效果相同。
 *
 * <p>目标 Bean 的解析可以基于返回类型（{@code getBean(Class)}）或者建议的 Bean 名称（{@code getBean(String)}），
 * 在这两种情况下，方法的参数都会传递给 {@code getBean} 调用，用作目标工厂方法参数或构造函数参数。
 *
 * <p>此类查找方法可以有默认（存根）实现，容器会将其替换；或者声明为抽象，由容器在运行时填充。
 * 无论哪种情况，容器都会通过 CGLIB 为方法所在类生成运行时子类，
 * 这就是为什么查找方法只能用于通过常规构造函数实例化的 Bean：即无法替换由工厂方法返回的 Bean，
 * 因为我们无法为其动态提供子类。
 *
 * <p><b>针对典型 Spring 配置场景的建议：</b>
 * 当某些场景需要具体类时，考虑为查找方法提供存根实现。
 * 请记住，查找方法不能用于配置类中的 {@code @Bean} 方法返回的 Bean；
 * 此时需要使用 {@code @Inject Provider<TargetBean>} 或类似方式。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.BeanFactory#getBean(Class, Object...)
 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Object...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {

	/**
	 * 此注解属性可用于指定要查找的目标 Bean 名称。
	 * 如果未指定，将根据被注解方法的返回类型声明解析目标 Bean。
	 */
	String value() default "";

}
