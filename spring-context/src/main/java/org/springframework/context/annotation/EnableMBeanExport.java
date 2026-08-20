/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * 启用从 Spring 上下文中默认导出所有标准 {@code MBean} 以及所有标注了
 * {@code @ManagedResource} 的 Bean。
 *
 * <p>生成的 {@link org.springframework.jmx.export.MBeanExporter MBeanExporter}
 * Bean 以名称 "mbeanExporter" 定义。或者，也可以考虑显式定义自定义的
 * {@link AnnotationMBeanExporter} Bean。
 *
 * <p>此注解模仿了 Spring XML 的 {@code <context:mbean-export/>} 元素，
 * 功能上与之等效。
 *
 * @author Phillip Webb
 * @since 3.2
 * @see MBeanExportConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MBeanExportConfiguration.class)
public @interface EnableMBeanExport {

	/**
	 * 生成 JMX ObjectName 时使用的默认域。
	 */
	String defaultDomain() default "";

	/**
	 * MBeans 应导出到的 MBeanServer 的 Bean 名称。默认使用平台的默认 MBeanServer。
	 */
	String server() default "";

	/**
	 * 当尝试在已存在的 {@link javax.management.ObjectName} 下注册 MBean 时使用的策略。
	 * 默认为 {@link RegistrationPolicy#FAIL_ON_EXISTING}。
	 */
	RegistrationPolicy registration() default RegistrationPolicy.FAIL_ON_EXISTING;
}
