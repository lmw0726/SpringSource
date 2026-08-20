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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.Assert;

/**
 * {@code AnnotationBeanNameGenerator} 的扩展，当没有通过支持的类型级注解
 *（例如 {@code @Component}，详见 {@link AnnotationBeanNameGenerator}）
 * 提供显式的 Bean 名称时，使用全限定类名作为默认的 Bean 名称。
 *
 * <p>如果由于多个自动检测到的组件具有相同的非限定类名（即类名相同但位于
 * 不同包中）而导致命名冲突，建议优先使用此 Bean 命名策略，
 * 而非 {@code AnnotationBeanNameGenerator}。
 *
 * <p>注意，该类的实例默认用于配置级别的导入；而组件扫描的默认命名策略
 * 是普通的 {@code AnnotationBeanNameGenerator}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2.3
 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator
 * @see AnnotationBeanNameGenerator
 * @see ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR
 */
public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

	/**
	 * 默认 {@code FullyQualifiedAnnotationBeanNameGenerator} 实例的便捷常量，
	 * 用于配置级别的导入。
	 * @since 5.2.11
	 */
	public static final FullyQualifiedAnnotationBeanNameGenerator INSTANCE =
			new FullyQualifiedAnnotationBeanNameGenerator();


	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		return beanClassName;
	}

}
