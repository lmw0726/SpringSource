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

package org.springframework.context.annotation.aspectj;

import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * {@code @Configuration} 类，用于注册一个 {@code AnnotationBeanConfigurerAspect}（注解 bean 配置器切面），
 * 该切面能够为标注了 @{@link org.springframework.beans.factory.annotation.Configurable
 * Configurable} 注解的非 Spring 管理对象执行依赖注入服务。
 *
 * <p>当使用 {@link EnableSpringConfigured @EnableSpringConfigured} 注解时，此配置类会被自动导入。
 * 完整的用法详情请参阅 {@code @EnableSpringConfigured} 的 Javadoc。
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableSpringConfigured
 */
@Configuration
public class SpringConfiguredConfiguration {

	/**
	 * 配置器切面所使用的 bean 名称。
	 */
	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"org.springframework.context.config.internalBeanConfigurerAspect";

	@Bean(name = BEAN_CONFIGURER_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationBeanConfigurerAspect beanConfigurerAspect() {
		return AnnotationBeanConfigurerAspect.aspectOf();
	}

}
