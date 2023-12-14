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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.util.Assert;

/**
 * Qualifier for resolving autowire candidates. A bean definition that
 * includes one or more such qualifiers enables fine-grained matching
 * against annotations on a field or parameter to be autowired.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.annotation.Qualifier
 * @since 2.5
 */
@SuppressWarnings("serial")
public class AutowireCandidateQualifier extends BeanMetadataAttributeAccessor {

	/**
	 * 用于存储值的键的名称。
	 */
	public static final String VALUE_KEY = "value";

	/**
	 * 类型名称
	 */
	private final String typeName;


	/**
	 * 构造一个限定符以与给定类型的注解匹配。
	 *
	 * @param type 注解类型
	 */
	public AutowireCandidateQualifier(Class<?> type) {
		this(type.getName());
	}

	/**
	 * 构造一个限定符与给定类型名称的注解匹配。
	 * <p> 类型名称可能与注解的完全限定的类名称或短类名称 (不带包) 匹配。
	 *
	 * @param typeName 注解类型的名称
	 */
	public AutowireCandidateQualifier(String typeName) {
		Assert.notNull(typeName, "Type name must not be null");
		this.typeName = typeName;
	}

	/**
	 * 构造一个限定符与给定类型的注解匹配，该注释的 {@code value} 属性也与指定的值匹配。
	 *
	 * @param type  注解类型
	 * @param value 要匹配的注解值
	 */
	public AutowireCandidateQualifier(Class<?> type, Object value) {
		this(type.getName(), value);
	}

	/**
	 * 构造一个限定符，与匹配给定类型名称的注解，该注解的 {@code value} 属性也与指定的值匹配。
	 * <p> 类型名称可能与注解的完全限定的类名称或短类名称 (不带包) 匹配。
	 *
	 * @param typeName 注解类型名称
	 * @param value    要匹配的注解值
	 */
	public AutowireCandidateQualifier(String typeName, Object value) {
		Assert.notNull(typeName, "Type name must not be null");
		this.typeName = typeName;
		setAttribute(VALUE_KEY, value);
	}


	/**
	 * 检索类型名称。如果将类实例提供给构造函数，则此值将与提供给构造函数的类型名称或完全限定的类名称相同。
	 */
	public String getTypeName() {
		return this.typeName;
	}

}
