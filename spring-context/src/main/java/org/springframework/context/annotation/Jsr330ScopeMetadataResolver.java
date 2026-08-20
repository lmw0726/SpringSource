/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;

/**
 * 遵循 JSR-330 作用域规则的简单 {@link ScopeMetadataResolver} 实现：
 * 默认使用 prototype（原型）作用域，除非存在 {@link javax.inject.Singleton} 注解。
 *
 * <p>此作用域解析器可与 {@link ClassPathBeanDefinitionScanner} 和
 * {@link AnnotatedBeanDefinitionReader} 配合使用，以实现标准 JSR-330 合规性。
 * 但在实际使用中，通常会直接使用 Spring 丰富的默认作用域机制，
 * 或者通过指向扩展 Spring 作用域的自定义作用域注解来扩展此解析器。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see #registerScope
 * @see #resolveScopeName
 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
 */
public class Jsr330ScopeMetadataResolver implements ScopeMetadataResolver {

	private final Map<String, String> scopeMap = new HashMap<>();


	public Jsr330ScopeMetadataResolver() {
		registerScope("javax.inject.Singleton", BeanDefinition.SCOPE_SINGLETON);
	}


	/**
	 * 注册扩展的 JSR-330 作用域注解，将其映射到指定的 Spring 作用域名称。
	 * @param annotationType JSR-330 注解类型（Class 形式）
	 * @param scopeName Spring 作用域名称
	 */
	public final void registerScope(Class<?> annotationType, String scopeName) {
		this.scopeMap.put(annotationType.getName(), scopeName);
	}

	/**
	 * 注册扩展的 JSR-330 作用域注解，将其映射到指定的 Spring 作用域名称。
	 * @param annotationType JSR-330 注解类型名称（字符串形式）
	 * @param scopeName Spring 作用域名称
	 */
	public final void registerScope(String annotationType, String scopeName) {
		this.scopeMap.put(annotationType, scopeName);
	}

	/**
	 * 将给定的注解类型解析为命名的 Spring 作用域。
	 * <p>默认实现仅检查已注册的作用域。
	 * 可被覆盖以实现自定义映射规则，例如命名约定。
	 * @param annotationType JSR-330 注解类型
	 * @return Spring 作用域名称
	 */
	@Nullable
	protected String resolveScopeName(String annotationType) {
		return this.scopeMap.get(annotationType);
	}


	@Override
	public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
		ScopeMetadata metadata = new ScopeMetadata();
		metadata.setScopeName(BeanDefinition.SCOPE_PROTOTYPE);
		if (definition instanceof AnnotatedBeanDefinition) {
			AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;
			Set<String> annTypes = annDef.getMetadata().getAnnotationTypes();
			String found = null;
			for (String annType : annTypes) {
				Set<String> metaAnns = annDef.getMetadata().getMetaAnnotationTypes(annType);
				if (metaAnns.contains("javax.inject.Scope")) {
					if (found != null) {
						throw new IllegalStateException("Found ambiguous scope annotations on bean class [" +
								definition.getBeanClassName() + "]: " + found + ", " + annType);
					}
					found = annType;
					String scopeName = resolveScopeName(annType);
					if (scopeName == null) {
						throw new IllegalStateException(
								"Unsupported scope annotation - not mapped onto Spring scope name: " + annType);
					}
					metadata.setScopeName(scopeName);
				}
			}
		}
		return metadata;
	}

}
