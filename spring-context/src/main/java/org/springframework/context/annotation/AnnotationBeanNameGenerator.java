/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.Introspector;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 针对使用 {@link org.springframework.stereotype.Component @Component} 注解
 * 标注的 Bean 类（或使用了以 {@code @Component} 作为元注解的其他注解标注的 Bean 类）
 * 的 {@link BeanNameGenerator} 实现。
 * 例如，Spring 的原型注解（如 {@link org.springframework.stereotype.Repository @Repository}）
 * 本身就使用了 {@code @Component} 进行标注。
 *
 * <p>同时支持 Java EE 6 的 {@link javax.annotation.ManagedBean} 和
 * JSR-330 的 {@link javax.inject.Named} 注解（如果可用的话）。
 * 请注意，Spring 组件注解始终会覆盖此类标准注解。
 *
 * <p>如果注解的 value 属性未指定 Bean 名称，则会基于类的短名称（首字母小写）生成合适的名称，
 * 除非前两个字母均为大写。例如：
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 * <pre class="code">com.xyz.URLFooServiceImpl -&gt; URLFooServiceImpl</pre>
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see org.springframework.stereotype.Component#value()
 * @see org.springframework.stereotype.Repository#value()
 * @see org.springframework.stereotype.Service#value()
 * @see org.springframework.stereotype.Controller#value()
 * @see javax.inject.Named#value()
 * @see FullyQualifiedAnnotationBeanNameGenerator
 * @since 2.5
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

	/**
	 * 一个便捷的常量，表示默认的 {@code AnnotationBeanNameGenerator} 实例，
	 * 用于组件扫描。
	 *
	 * @since 5.2
	 */
	public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

	private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";

	private final Map<String, Set<String>> metaAnnotationTypesCache = new ConcurrentHashMap<>();


	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		if (definition instanceof AnnotatedBeanDefinition) {
			//从注解中推断Bean名称
			String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
			if (StringUtils.hasText(beanName)) {
				// 找到显式的 Bean 名称。
				return beanName;
			}
		}
		// 回退：生成唯一的默认 Bean 名称。
		//生成唯一的默认BeanName
		return buildDefaultBeanName(definition, registry);
	}

	/**
	 * 从类上的某个注解中推导 Bean 名称。
	 *
	 * @param annotatedDef 支持注解的 Bean 定义
	 * @return Bean 名称，如果未找到则返回 {@code null}
	 */
	@Nullable
	protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
		AnnotationMetadata amd = annotatedDef.getMetadata();
		Set<String> types = amd.getAnnotationTypes();
		String beanName = null;
		for (String type : types) {
			//从注解继承体系中获取注解的所有属性。
			AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
			if (attributes != null) {
				Set<String> metaTypes = this.metaAnnotationTypesCache.computeIfAbsent(type, key -> {
					//获取元注解的类型
					Set<String> result = amd.getMetaAnnotationTypes(key);
					return (result.isEmpty() ? Collections.emptySet() : result);
				});
				//是否允许从@Component继承体系以及@ManagedBean、@Named注解中的value()获取到值。
				if (isStereotypeWithNameValue(type, metaTypes, attributes)) {
					Object value = attributes.get("value");
					if (value instanceof String) {
						String strVal = (String) value;
						if (StringUtils.hasLength(strVal)) {
							if (beanName != null && !strVal.equals(beanName)) {
								throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
										"component names: '" + beanName + "' versus '" + strVal + "'");
							}
							beanName = strVal;
						}
					}
				}
			}
		}
		return beanName;
	}

	/**
	 * 检查给定的注解是否是允许通过其 {@code value()} 方法指定组件名称的原型注解。
	 *
	 * @param annotationType      要检查的注解类的名称
	 * @param metaAnnotationTypes 给定注解上的元注解名称
	 * @param attributes          给定注解的属性映射
	 * @return 该注解是否符合带组件名称的原型注解条件
	 */
	protected boolean isStereotypeWithNameValue(String annotationType,
												Set<String> metaAnnotationTypes, @Nullable Map<String, Object> attributes) {

		boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME) ||
				metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) ||
				annotationType.equals("javax.annotation.ManagedBean") ||
				annotationType.equals("javax.inject.Named");

		return (isStereotype && attributes != null && attributes.containsKey("value"));
	}

	/**
	 * 从给定的 Bean 定义中推导默认的 Bean 名称。
	 * <p>默认实现委托给 {@link #buildDefaultBeanName(BeanDefinition)}。
	 *
	 * @param definition 要为其构建 Bean 名称的 Bean 定义
	 * @param registry   给定 Bean 定义正在注册到的注册表
	 * @return 默认的 Bean 名称（永不为 {@code null}）
	 */
	protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return buildDefaultBeanName(definition);
	}

	/**
	 * 从给定的 Bean 定义中推导默认的 Bean 名称。
	 * <p>默认实现简单地构建一个首字母小写的短类名版本：
	 * 例如 "mypackage.MyJdbcDao" &rarr; "myJdbcDao"。
	 * <p>请注意，内部类的名称格式将是 "outerClassName.InnerClassName"，
	 * 由于名称中包含点号，如果按名称进行自动装配可能会出现问题。
	 *
	 * @param definition 要为其构建 Bean 名称的 Bean 定义
	 * @return 默认的 Bean 名称（永不为 {@code null}）
	 */
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		String shortClassName = ClassUtils.getShortName(beanClassName);
		//将首字母转为小写的形式返回
		return Introspector.decapitalize(shortClassName);
	}

}
