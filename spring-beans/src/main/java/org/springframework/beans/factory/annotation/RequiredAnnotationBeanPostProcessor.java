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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} 实现，
 * 用于强制要求 JavaBean 属性必须已被配置。
 * 必需的 Bean 属性通过注解检测：
 * 默认情况下使用 Spring 的 {@link Required} 注解。
 *
 * <p>存在此 BeanPostProcessor 的动机是允许开发者在自己类的 setter 属性上
 * 使用任意 JDK 1.5 注解来指示容器必须检查依赖注入值的配置情况。
 * 这将检查的责任巧妙地转移给容器（在逻辑上应该由容器负责），
 * 并在一定程度上消除了开发者编写方法来检查所有必需属性是否被设置的需要。
 *
 * <p>请注意，可能仍需要实现“init”方法（也可能仍然是可取的），
 * 因为此类仅仅确保“required”属性已经被配置了值。
 * 它<b>不</b>检查其他任何内容……特别是不检查配置的值是否为 {@code null}。
 *
 * <p>注意：默认的 RequiredAnnotationBeanPostProcessor 会由
 * "context:annotation-config" 和 "context:component-scan" XML 标签注册。
 * 如果打算指定自定义的 RequiredAnnotationBeanPostProcessor Bean 定义，
 * 请在此处移除或关闭默认注解配置。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setRequiredAnnotationType
 * @see Required
 * @deprecated 自 5.1 起，推荐使用构造器注入来设置必需属性
 * （或使用自定义 {@link org.springframework.beans.factory.InitializingBean} 实现）
 */
@Deprecated
public class RequiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
		MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	/**
	 * Bean 定义属性，可能指示在执行此后置处理器的必需属性检查时，
	 * 是否应跳过某个给定的 Bean。
	 * @see #shouldSkip
	 */
	public static final String SKIP_REQUIRED_CHECK_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(RequiredAnnotationBeanPostProcessor.class, "skipRequiredCheck");


	private Class<? extends Annotation> requiredAnnotationType = Required.class;

	private int order = Ordered.LOWEST_PRECEDENCE - 1;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * 已验证的 Bean 名称缓存，用于跳过对相同 Bean 的重复验证。
	 */
	private final Set<String> validatedBeanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(64));


	/**
	 * 设置 “required” 注解类型，用于 Bean 属性的 setter 方法。
	 * <p>默认的 required 注解类型是 Spring 提供的 {@link Required} 注解。
	 * <p>提供此 setter 属性是为了让开发者可以提供自己的（非 Spring 特定）注解类型，
	 * 以指示某个属性值是必需的。
	 */
	public void setRequiredAnnotationType(Class<? extends Annotation> requiredAnnotationType) {
		Assert.notNull(requiredAnnotationType, "'requiredAnnotationType' must not be null");
		this.requiredAnnotationType = requiredAnnotationType;
	}

	/**
	 * 返回 “required” 注解类型。
	 */
	protected Class<? extends Annotation> getRequiredAnnotationType() {
		return this.requiredAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) {

		if (!this.validatedBeanNames.contains(beanName)) {
			if (!shouldSkip(this.beanFactory, beanName)) {
				List<String> invalidProperties = new ArrayList<>();
				for (PropertyDescriptor pd : pds) {
					if (isRequiredProperty(pd) && !pvs.contains(pd.getName())) {
						invalidProperties.add(pd.getName());
					}
				}
				if (!invalidProperties.isEmpty()) {
					throw new BeanInitializationException(buildExceptionMessage(invalidProperties, beanName));
				}
			}
			this.validatedBeanNames.add(beanName);
		}
		return pvs;
	}

	/**
	 * 检查给定的 BeanDefinition 是否不受此后处理器执行的基于注解的必需属性检查的影响。
	 * <p>默认实现会检查 BeanDefinition 中是否存在 {@link #SKIP_REQUIRED_CHECK_ATTRIBUTE} 属性（如果有的话）。
	 * 如果 BeanDefinition 设置了 "factory-bean" 引用，也会建议跳过检查，假设基于实例的工厂会预先填充 Bean。
	 *
	 * @param beanFactory 要检查的 BeanFactory
	 * @param beanName 要检查的 Bean 名称
	 * @return 如果跳过该 Bean，则返回 {@code true}；如果处理该 Bean，则返回 {@code false}
	 */
	protected boolean shouldSkip(@Nullable ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanFactory == null || !beanFactory.containsBeanDefinition(beanName)) {
			return false;
		}
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
		if (beanDefinition.getFactoryBeanName() != null) {
			return true;
		}
		Object value = beanDefinition.getAttribute(SKIP_REQUIRED_CHECK_ATTRIBUTE);
		return (value != null && (Boolean.TRUE.equals(value) || Boolean.parseBoolean(value.toString())));
	}

	/**
	 * 判断所提供的属性是否必须具有值（即，是否需要依赖注入）。
	 * <p>此实现会检查所提供的 {@link PropertyDescriptor property} 上是否存在
	 * {@link #setRequiredAnnotationType "required" 注解}。
	 *
	 * @param propertyDescriptor 目标 PropertyDescriptor（永不为 {@code null}）
	 * @return 如果所提供的属性被标记为必需，则返回 {@code true}；
	 *         如果没有标记为必需，或所提供的属性没有 setter 方法，则返回 {@code false}
	 */
	protected boolean isRequiredProperty(PropertyDescriptor propertyDescriptor) {
		Method setter = propertyDescriptor.getWriteMethod();
		return (setter != null && AnnotationUtils.getAnnotation(setter, getRequiredAnnotationType()) != null);
	}

	/**
	 * 为给定的无效属性列表构建异常信息。
	 *
	 * @param invalidProperties 无效属性名称列表
	 * @param beanName Bean 的名称
	 * @return 异常信息
	 */
	private String buildExceptionMessage(List<String> invalidProperties, String beanName) {
		int size = invalidProperties.size();
		StringBuilder sb = new StringBuilder();
		sb.append(size == 1 ? "Property" : "Properties");
		for (int i = 0; i < size; i++) {
			String propertyName = invalidProperties.get(i);
			if (i > 0) {
				if (i == (size - 1)) {
					sb.append(" and");
				}
				else {
					sb.append(',');
				}
			}
			sb.append(" '").append(propertyName).append('\'');
		}
		sb.append(size == 1 ? " is" : " are");
		sb.append(" required for bean '").append(beanName).append('\'');
		return sb.toString();
	}

}
