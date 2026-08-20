/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.annotation.AliasFor;

/**
 * 指示要导入的一个或多个包含 Bean 定义的资源。
 *
 * <p>与 {@link Import @Import} 类似，此注解提供的功能类似于
 * Spring XML 中的 {@code <import/>} 元素。它通常用于设计
 * 需要通过 {@link AnnotationConfigApplicationContext} 引导启动的
 * {@link Configuration @Configuration} 类，同时仍需要某些 XML 功能（例如命名空间）的场景。
 *
 * <p>默认情况下，{@link #value} 属性的参数将使用
 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader}
 * 进行处理（如果以 {@code ".groovy"} 结尾）；否则，将使用
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader}
 * 来解析 Spring {@code <beans/>} XML 文件。可选地，可以声明 {@link #reader}
 * 属性，允许用户选择自定义的 {@link BeanDefinitionReader} 实现。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Import
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ImportResource {

	/**
	 * {@link #locations} 的别名。
	 * @see #locations
	 * @see #reader
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 要导入的资源位置。
	 * <p>支持资源加载前缀，例如 {@code classpath:}、
	 * {@code file:} 等。
	 * <p>有关资源处理方式的详细信息，请参阅 {@link #reader} 的 Javadoc。
	 * @since 4.2
	 * @see #value
	 * @see #reader
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 处理通过 {@link #value} 属性指定的资源时使用的
	 * {@link BeanDefinitionReader} 实现。
	 * <p>默认情况下，读取器将根据指定的资源路径进行适配：
	 * {@code ".groovy"} 文件将使用
	 * {@link org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader GroovyBeanDefinitionReader} 处理；
	 * 而所有其他资源将使用
	 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader XmlBeanDefinitionReader} 处理。
	 * @see #value
	 */
	Class<? extends BeanDefinitionReader> reader() default BeanDefinitionReader.class;

}
