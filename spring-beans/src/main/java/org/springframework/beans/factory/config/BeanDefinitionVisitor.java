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

package org.springframework.beans.factory.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringValueResolver;

import java.util.*;

/**
 * Visitor class for traversing {@link BeanDefinition} objects, in particular
 * the property values and constructor argument values contained in them,
 * resolving bean metadata values.
 *
 * <p>Used by {@link PlaceholderConfigurerSupport} to parse all String values
 * contained in a BeanDefinition, resolving any placeholders found.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see BeanDefinition
 * @see BeanDefinition#getPropertyValues
 * @see BeanDefinition#getConstructorArgumentValues
 * @see PlaceholderConfigurerSupport
 * @since 1.2
 */
public class BeanDefinitionVisitor {

	@Nullable
	private StringValueResolver valueResolver;


	/**
	 * Create a new BeanDefinitionVisitor, applying the specified
	 * value resolver to all bean metadata values.
	 *
	 * @param valueResolver the StringValueResolver to apply
	 */
	public BeanDefinitionVisitor(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.valueResolver = valueResolver;
	}

	/**
	 * Create a new BeanDefinitionVisitor for subclassing.
	 * Subclasses need to override the {@link #resolveStringValue} method.
	 */
	protected BeanDefinitionVisitor() {
	}


	/**
	 * 遍历给定的 BeanDefinition 对象以及其中包含的 MutablePropertyValues 和 ConstructorArgumentValues。
	 *
	 * @param beanDefinition 要遍历的 BeanDefinition 对象
	 * @see #resolveStringValue(String)
	 */
	public void visitBeanDefinition(BeanDefinition beanDefinition) {
		// 访问父 bean 的名称
		visitParentName(beanDefinition);
		// 访问 bean 类的名称
		visitBeanClassName(beanDefinition);
		// 访问工厂 bean 的名称
		visitFactoryBeanName(beanDefinition);
		// 访问工厂方法的名称
		visitFactoryMethodName(beanDefinition);
		// 访问作用域
		visitScope(beanDefinition);
		// 如果 bean 定义具有属性值，则访问属性值
		if (beanDefinition.hasPropertyValues()) {
			visitPropertyValues(beanDefinition.getPropertyValues());
		}
		// 如果 bean 定义具有构造函数参数值，则访问构造函数参数值
		if (beanDefinition.hasConstructorArgumentValues()) {
			ConstructorArgumentValues cas = beanDefinition.getConstructorArgumentValues();
			// 访问索引参数值
			visitIndexedArgumentValues(cas.getIndexedArgumentValues());
			// 访问通用参数值
			visitGenericArgumentValues(cas.getGenericArgumentValues());
		}
	}

	protected void visitParentName(BeanDefinition beanDefinition) {
		String parentName = beanDefinition.getParentName();
		if (parentName != null) {
			String resolvedName = resolveStringValue(parentName);
			if (!parentName.equals(resolvedName)) {
				beanDefinition.setParentName(resolvedName);
			}
		}
	}

	protected void visitBeanClassName(BeanDefinition beanDefinition) {
		String beanClassName = beanDefinition.getBeanClassName();
		if (beanClassName != null) {
			String resolvedName = resolveStringValue(beanClassName);
			if (!beanClassName.equals(resolvedName)) {
				beanDefinition.setBeanClassName(resolvedName);
			}
		}
	}

	protected void visitFactoryBeanName(BeanDefinition beanDefinition) {
		String factoryBeanName = beanDefinition.getFactoryBeanName();
		if (factoryBeanName != null) {
			String resolvedName = resolveStringValue(factoryBeanName);
			if (!factoryBeanName.equals(resolvedName)) {
				beanDefinition.setFactoryBeanName(resolvedName);
			}
		}
	}

	protected void visitFactoryMethodName(BeanDefinition beanDefinition) {
		String factoryMethodName = beanDefinition.getFactoryMethodName();
		if (factoryMethodName != null) {
			String resolvedName = resolveStringValue(factoryMethodName);
			if (!factoryMethodName.equals(resolvedName)) {
				beanDefinition.setFactoryMethodName(resolvedName);
			}
		}
	}

	protected void visitScope(BeanDefinition beanDefinition) {
		String scope = beanDefinition.getScope();
		if (scope != null) {
			String resolvedScope = resolveStringValue(scope);
			if (!scope.equals(resolvedScope)) {
				beanDefinition.setScope(resolvedScope);
			}
		}
	}

	/**
	 * 访问属性值
	 *
	 * @param pvs 多属性值
	 */
	protected void visitPropertyValues(MutablePropertyValues pvs) {
		// 获取属性值数组
		PropertyValue[] pvArray = pvs.getPropertyValues();
		// 遍历属性值数组
		for (PropertyValue pv : pvArray) {
			// 解析属性值中的新值
			Object newVal = resolveValue(pv.getValue());
			// 如果新值与原始值不相等，则更新属性值
			if (!ObjectUtils.nullSafeEquals(newVal, pv.getValue())) {
				pvs.add(pv.getName(), newVal);
			}
		}
	}

	protected void visitIndexedArgumentValues(Map<Integer, ConstructorArgumentValues.ValueHolder> ias) {
		for (ConstructorArgumentValues.ValueHolder valueHolder : ias.values()) {
			Object newVal = resolveValue(valueHolder.getValue());
			if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
				valueHolder.setValue(newVal);
			}
		}
	}

	protected void visitGenericArgumentValues(List<ConstructorArgumentValues.ValueHolder> gas) {
		for (ConstructorArgumentValues.ValueHolder valueHolder : gas) {
			Object newVal = resolveValue(valueHolder.getValue());
			if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
				valueHolder.setValue(newVal);
			}
		}
	}

	/**
	 * 解析属性值
	 *
	 * @param value 属性值
	 * @return 解析好的值
	 */
	@SuppressWarnings("rawtypes")
	@Nullable
	protected Object resolveValue(@Nullable Object value) {
		// 如果值是BeanDefinition类型，则访问BeanDefinition
		if (value instanceof BeanDefinition) {
			visitBeanDefinition((BeanDefinition) value);
		}
		// 如果值是BeanDefinitionHolder类型，则访问其包含的BeanDefinition
		else if (value instanceof BeanDefinitionHolder) {
			visitBeanDefinition(((BeanDefinitionHolder) value).getBeanDefinition());
		}
		// 如果值是RuntimeBeanReference类型，则解析其引用的Bean名称，并更新引用的Bean名称
		else if (value instanceof RuntimeBeanReference) {
			RuntimeBeanReference ref = (RuntimeBeanReference) value;
			String newBeanName = resolveStringValue(ref.getBeanName());
			if (newBeanName == null) {
				return null;
			}
			if (!newBeanName.equals(ref.getBeanName())) {
				return new RuntimeBeanReference(newBeanName);
			}
		}
		// 如果值是RuntimeBeanNameReference类型，则解析其引用的Bean名称，并更新引用的Bean名称
		else if (value instanceof RuntimeBeanNameReference) {
			RuntimeBeanNameReference ref = (RuntimeBeanNameReference) value;
			// 解析bean名称的字符串值
			String newBeanName = resolveStringValue(ref.getBeanName());
			// 如果解析后的新bean名称为null，则返回null
			if (newBeanName == null) {
				return null;
			}
			// 如果新的bean名称与原bean名称不同，则创建一个新的RuntimeBeanNameReference对象并返回
			if (!newBeanName.equals(ref.getBeanName())) {
				return new RuntimeBeanNameReference(newBeanName);
			}
		}
		// 如果值是数组类型，则遍历数组并访问数组元素
		else if (value instanceof Object[]) {
			visitArray((Object[]) value);
		}
		// 如果值是List类型，则遍历列表并访问列表元素
		else if (value instanceof List) {
			visitList((List) value);
		}
		// 如果值是Set类型，则遍历集合并访问集合元素
		else if (value instanceof Set) {
			visitSet((Set) value);
		}
		// 如果值是Map类型，则遍历映射并访问映射元素
		else if (value instanceof Map) {
			visitMap((Map) value);
		}
		// 如果值是TypedStringValue类型，则解析其字符串值，并更新为解析后的字符串值
		else if (value instanceof TypedStringValue) {
			TypedStringValue typedStringValue = (TypedStringValue) value;
			String stringValue = typedStringValue.getValue();
			if (stringValue != null) {
				// 如果字符串值不为null，则解析字符串值并更新TypedStringValue的值为解析后的值
				String visitedString = resolveStringValue(stringValue);
				typedStringValue.setValue(visitedString);
			}
		}
		// 如果值是String类型，则解析其字符串值
		else if (value instanceof String) {
			return resolveStringValue((String) value);
		}
		return value;
	}

	protected void visitArray(Object[] arrayVal) {
		for (int i = 0; i < arrayVal.length; i++) {
			Object elem = arrayVal[i];
			Object newVal = resolveValue(elem);
			if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
				arrayVal[i] = newVal;
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void visitList(List listVal) {
		for (int i = 0; i < listVal.size(); i++) {
			Object elem = listVal.get(i);
			Object newVal = resolveValue(elem);
			if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
				listVal.set(i, newVal);
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void visitSet(Set setVal) {
		Set newContent = new LinkedHashSet();
		boolean entriesModified = false;
		for (Object elem : setVal) {
			int elemHash = (elem != null ? elem.hashCode() : 0);
			Object newVal = resolveValue(elem);
			int newValHash = (newVal != null ? newVal.hashCode() : 0);
			newContent.add(newVal);
			entriesModified = entriesModified || (newVal != elem || newValHash != elemHash);
		}
		if (entriesModified) {
			setVal.clear();
			setVal.addAll(newContent);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void visitMap(Map<?, ?> mapVal) {
		Map newContent = new LinkedHashMap();
		boolean entriesModified = false;
		for (Map.Entry entry : mapVal.entrySet()) {
			Object key = entry.getKey();
			int keyHash = (key != null ? key.hashCode() : 0);
			Object newKey = resolveValue(key);
			int newKeyHash = (newKey != null ? newKey.hashCode() : 0);
			Object val = entry.getValue();
			Object newVal = resolveValue(val);
			newContent.put(newKey, newVal);
			entriesModified = entriesModified || (newVal != val || newKey != key || newKeyHash != keyHash);
		}
		if (entriesModified) {
			mapVal.clear();
			mapVal.putAll(newContent);
		}
	}

	/**
	 * 解析给定的字符串值，例如解析占位符。
	 *
	 * @param strVal 原始字符串值
	 * @return 解析后的字符串值
	 */
	@Nullable
	protected String resolveStringValue(String strVal) {
		if (this.valueResolver == null) {
			throw new IllegalStateException("No StringValueResolver specified - pass a resolver " +
					"object into the constructor or override the 'resolveStringValue' method");
		}
		// 解析字符串值
		String resolvedValue = this.valueResolver.resolveStringValue(strVal);
		// 如果未修改，则返回原始字符串。
		return (strVal.equals(resolvedValue) ? strVal : resolvedValue);
	}

}
