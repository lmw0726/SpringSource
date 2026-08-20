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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 ASM ClassReader 的 {@link org.springframework.beans.factory.support.GenericBeanDefinition}
 * 类的扩展，支持通过 {@link AnnotatedBeanDefinition} 接口暴露的注解元数据。
 *
 * <p>此类<i>不会</i>提前加载 bean 的 {@code Class}。
 * 它而是从 ".class" 文件本身中检索所有相关的元数据，
 * 通过 ASM ClassReader 进行解析。其功能等同于
 * {@link AnnotatedGenericBeanDefinition#AnnotatedGenericBeanDefinition(AnnotationMetadata)}，
 * 但按类型区分<em>通过扫描</em>发现的 bean 与通过其他方式注册或检测到的 bean。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #getMetadata()
 * @see #getBeanClassName()
 * @see org.springframework.core.type.classreading.MetadataReaderFactory
 * @see AnnotatedGenericBeanDefinition
 * @since 2.5
 */
@SuppressWarnings("serial")
public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {
	/**
	 * 注解元数据
	 */
	private final AnnotationMetadata metadata;


	/**
	 * 为给定的MetadataReader描述的类创建一个新的ScannedGenericBeanDefinition。
	 *
	 * @param metadataReader 扫描目标类的MetadataReader
	 */
	public ScannedGenericBeanDefinition(MetadataReader metadataReader) {
		Assert.notNull(metadataReader, "MetadataReader must not be null");
		//通过MetadataReader读取类的注解元数据，并赋值给metadata
		this.metadata = metadataReader.getAnnotationMetadata();
		//设置bean类名
		setBeanClassName(this.metadata.getClassName());
		//设置源
		setResource(metadataReader.getResource());
	}


	@Override
	public final AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	@Nullable
	public MethodMetadata getFactoryMethodMetadata() {
		return null;
	}

}
