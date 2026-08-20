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

package org.springframework.jmx.export.naming;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ObjectNamingStrategy} 接口的实现类，用于从源码级元数据中读取 {@code ObjectName}。
 * 如果源码级元数据中未找到 {@code ObjectName}，则回退使用 bean key（即 bean 名称）。
 *
 * <p>使用 {@link JmxAttributeSource} 策略接口，以便通过任何支持的实现来读取元数据。
 * 开箱即用时，
 * {@link org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource}
 * 会对 Spring 提供的一组定义良好的注解进行内省。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see ObjectNamingStrategy
 * @see org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource
 */
public class MetadataNamingStrategy implements ObjectNamingStrategy, InitializingBean {

	/**
	 * 用于读取元数据的 {@code JmxAttributeSource} 实现。
	 */
	@Nullable
	private JmxAttributeSource attributeSource;

	@Nullable
	private String defaultDomain;


	/**
	 * 创建一个新的 {@code MetadataNamingStrategy}，需要通过 {@link #setAttributeSource} 方法进行配置。
	 */
	public MetadataNamingStrategy() {
	}

	/**
	 * 为给定的 {@code JmxAttributeSource} 创建一个新的 {@code MetadataNamingStrategy}。
	 * @param attributeSource 要使用的 JmxAttributeSource
	 */
	public MetadataNamingStrategy(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}


	/**
	 * 设置在读取源码级元数据时要使用的 {@code JmxAttributeSource} 接口实现。
	 */
	public void setAttributeSource(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}

	/**
	 * 指定在未指定源码级元数据时用于生成 ObjectName 的默认域。
	 * <p>默认情况下使用 bean 名称中指定的域（如果 bean 名称遵循 JMX ObjectName 语法）；
	 * 否则使用被管理 bean 类的包名。
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.attributeSource == null) {
			throw new IllegalArgumentException("Property 'attributeSource' is required");
		}
	}


	/**
	 * 从与被管理资源的 {@code Class} 关联的源码级元数据中读取 {@code ObjectName}。
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
		Assert.state(this.attributeSource != null, "No JmxAttributeSource set");
		Class<?> managedClass = AopUtils.getTargetClass(managedBean);
		ManagedResource mr = this.attributeSource.getManagedResource(managedClass);

		// 检查是否已指定对象名称。
		if (mr != null && StringUtils.hasText(mr.getObjectName())) {
			return ObjectNameManager.getInstance(mr.getObjectName());
		}
		else {
			Assert.state(beanKey != null, "No ManagedResource attribute and no bean key specified");
			try {
				return ObjectNameManager.getInstance(beanKey);
			}
			catch (MalformedObjectNameException ex) {
				String domain = this.defaultDomain;
				if (domain == null) {
					domain = ClassUtils.getPackageName(managedClass);
				}
				Hashtable<String, String> properties = new Hashtable<>();
				properties.put("type", ClassUtils.getShortName(managedClass));
				properties.put("name", beanKey);
				return ObjectNameManager.getInstance(domain, properties);
			}
		}
	}

}
