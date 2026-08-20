/*
 * Copyright 2002-2019 the original author or authors.
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
 * 表示要导入的一个或多个<em>组件类</em>——通常是
 * {@link Configuration @Configuration} 类。
 *
 * <p>提供了与 Spring XML 中 {@code <import/>} 元素等效的功能。
 * 允许导入 {@code @Configuration} 类、{@link ImportSelector} 和
 * {@link ImportBeanDefinitionRegistrar} 实现，以及普通组件类
 * （自 4.2 版本起；类似于 {@link AnnotationConfigApplicationContext#register}）。
 *
 * <p>导入的 {@code @Configuration} 类中声明的 {@code @Bean} 定义应该通过
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
 * 注入来访问。可以自动注入 bean 本身，或者自动注入声明该 bean 的配置类实例。
 * 后者允许在 {@code @Configuration} 类方法之间进行明确的、IDE 友好的导航。
 *
 * <p>可以在类级别或作为元注解声明。
 *
 * <p>如果需要导入 XML 或其他非 {@code @Configuration} bean 定义资源，
 * 请改用 {@link ImportResource @ImportResource} 注解。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Configuration
 * @see ImportSelector
 * @see ImportBeanDefinitionRegistrar
 * @see ImportResource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {

	/**
	 * 要导入的 {@link Configuration @Configuration}、{@link ImportSelector}、
	 * {@link ImportBeanDefinitionRegistrar} 或普通组件类。
	 */
	Class<?>[] value();

}
