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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Extension of the {@link org.springframework.beans.factory.support.GenericBeanDefinition}
 * class, adding support for annotation metadata exposed through the
 * {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This GenericBeanDefinition variant is mainly useful for testing code that expects
 * to operate on an AnnotatedBeanDefinition, for example strategy implementations
 * in Spring's component scanning support (where the default definition class is
 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition},
 * which also implements the AnnotatedBeanDefinition interface).
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see AnnotatedBeanDefinition#getMetadata()
 * @see org.springframework.core.type.StandardAnnotationMetadata
 * @since 2.5
 */
@SuppressWarnings("serial")
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {
	/**
	 * 注解元数据
	 */
	private final AnnotationMetadata metadata;
	/**
	 * 方法元数据
	 */
	@Nullable
	private MethodMetadata factoryMethodMetadata;


	/**
	 * 为给定的bean类创建一个新的AnnotatedGenericBeanDefinition。
	 *
	 * @param beanClass 加载的bean类
	 */
	public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
		//设置bean类
		setBeanClass(beanClass);
		//根据Bean类内省注解元数据
		this.metadata = AnnotationMetadata.introspect(beanClass);
	}

	/**
	 * 为给定的注释元数据创建一个新的AnnotatedGenericBeanDefinition，允许基于ASM的处理并避免bean类的早期加载。
	 * 请注意，此构造函数在功能上等同于
	 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition ScannedGenericBeanDefinition构造函数} ，
	 * 但是后者的语义表明bean是通过组件扫描专门发现的，而不是其他方法。
	 *
	 * @param metadata 所讨论的bean类的注解元数据
	 * @since 3.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
		Assert.notNull(metadata, "AnnotationMetadata must not be null");
		if (metadata instanceof StandardAnnotationMetadata) {
			//如果注解元数据是标准的注解元数据类型，获取内省类并设置bean类
			setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		} else {
			//否则设置bean雷鸣
			setBeanClassName(metadata.getClassName());
		}
		this.metadata = metadata;
	}

	/**
	 * 基于注释类和该类上的工厂方法，为给定的注释元数据创建新的AnnotatedGenericBeanDefinition。
	 *
	 * @param metadata              所讨论的bean类的注解元数据
	 * @param factoryMethodMetadata 所选工厂方法的元数据
	 * @since 4.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
		this(metadata);
		Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
		//设置工厂方法名称
		setFactoryMethodName(factoryMethodMetadata.getMethodName());
		this.factoryMethodMetadata = factoryMethodMetadata;
	}


	@Override
	public final AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	@Nullable
	public final MethodMetadata getFactoryMethodMetadata() {
		return this.factoryMethodMetadata;
	}

}
