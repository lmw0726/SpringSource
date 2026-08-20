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

package org.springframework.jmx.export.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;

/**
 * Spring 标准 {@link MBeanExporter} 的便捷子类，
 * 通过激活注解方式对 Spring Bean 进行 JMX 暴露：
 * {@link ManagedResource}、{@link ManagedAttribute}、{@link ManagedOperation} 等。
 *
 * <p>使用 {@link AnnotationJmxAttributeSource} 设置
 * {@link MetadataNamingStrategy} 和 {@link MetadataMBeanInfoAssembler}，
 * 并默认激活 {@link #AUTODETECT_ALL} 模式。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class AnnotationMBeanExporter extends MBeanExporter {

	private final AnnotationJmxAttributeSource annotationSource =
			new AnnotationJmxAttributeSource();

	private final MetadataNamingStrategy metadataNamingStrategy =
			new MetadataNamingStrategy(this.annotationSource);

	private final MetadataMBeanInfoAssembler metadataAssembler =
			new MetadataMBeanInfoAssembler(this.annotationSource);


	public AnnotationMBeanExporter() {
		setNamingStrategy(this.metadataNamingStrategy);
		setAssembler(this.metadataAssembler);
		setAutodetectMode(AUTODETECT_ALL);
	}


	/**
	 * 指定在未指定源级别元数据时用于生成 ObjectName 的默认域。
	 * <p>默认使用 bean 名称中指定的域
	 * （如果 bean 名称遵循 JMX ObjectName 语法）；否则，
	 * 使用被管理 bean 类的包名。
	 * @see MetadataNamingStrategy#setDefaultDomain
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.metadataNamingStrategy.setDefaultDomain(defaultDomain);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.annotationSource.setBeanFactory(beanFactory);
	}

}
