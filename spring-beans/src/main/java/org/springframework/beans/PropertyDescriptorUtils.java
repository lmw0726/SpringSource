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

package org.springframework.beans;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Enumeration;

/**
 * Spring内部 {@link PropertyDescriptor} 实现的通用委托方法。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
abstract class PropertyDescriptorUtils {

	/**
	 * 详见 {@link java.beans.FeatureDescriptor}。
	 */
	public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target) {
		target.setExpert(source.isExpert());
		target.setHidden(source.isHidden());
		target.setPreferred(source.isPreferred());
		target.setName(source.getName());
		target.setShortDescription(source.getShortDescription());
		target.setDisplayName(source.getDisplayName());

		// 复制所有属性（模拟私有 FeatureDescriptor#addTable 的行为）
		Enumeration<String> keys = source.attributeNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			target.setValue(key, source.getValue(key));
		}

		// 详见 java.beans.PropertyDescriptor#PropertyDescriptor(PropertyDescriptor)
		target.setPropertyEditorClass(source.getPropertyEditorClass());
		target.setBound(source.isBound());
		target.setConstrained(source.isConstrained());
	}

	/**
	 * 详见 {@link java.beans.PropertyDescriptor#findPropertyType}.
	 */
	@Nullable
	public static Class<?> findPropertyType(@Nullable Method readMethod, @Nullable Method writeMethod)
			throws IntrospectionException {

		Class<?> propertyType = null;

		if (readMethod != null) {
			if (readMethod.getParameterCount() != 0) {
				throw new IntrospectionException("Bad read method arg count: " + readMethod);
			}
			propertyType = readMethod.getReturnType();
			if (propertyType == Void.TYPE) {
				throw new IntrospectionException("Read method returns void: " + readMethod);
			}
		}

		if (writeMethod != null) {
			Class<?>[] params = writeMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("Bad write method arg count: " + writeMethod);
			}
			if (propertyType != null) {
				if (propertyType.isAssignableFrom(params[0])) {
					// 写方法的属性类型可能更具体
					propertyType = params[0];
				}
				else if (params[0].isAssignableFrom(propertyType)) {
					// 使用读方法的属性类型继续处理
				}
				else {
					throw new IntrospectionException(
							"Type mismatch between read and write methods: " + readMethod + " - " + writeMethod);
				}
			}
			else {
				propertyType = params[0];
			}
		}

		return propertyType;
	}

	/**
	 * 详见 {@link java.beans.IndexedPropertyDescriptor#findIndexedPropertyType}.
	 */
	@Nullable
	public static Class<?> findIndexedPropertyType(String name, @Nullable Class<?> propertyType,
			@Nullable Method indexedReadMethod, @Nullable Method indexedWriteMethod) throws IntrospectionException {

		Class<?> indexedPropertyType = null;

		if (indexedReadMethod != null) {
			Class<?>[] params = indexedReadMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("Bad indexed read method arg count: " + indexedReadMethod);
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException("Non int index to indexed read method: " + indexedReadMethod);
			}
			indexedPropertyType = indexedReadMethod.getReturnType();
			if (indexedPropertyType == Void.TYPE) {
				throw new IntrospectionException("Indexed read method returns void: " + indexedReadMethod);
			}
		}

		if (indexedWriteMethod != null) {
			Class<?>[] params = indexedWriteMethod.getParameterTypes();
			if (params.length != 2) {
				throw new IntrospectionException("Bad indexed write method arg count: " + indexedWriteMethod);
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException("Non int index to indexed write method: " + indexedWriteMethod);
			}
			if (indexedPropertyType != null) {
				if (indexedPropertyType.isAssignableFrom(params[1])) {
					// 写方法的属性类型可能更具体
					indexedPropertyType = params[1];
				}
				else if (params[1].isAssignableFrom(indexedPropertyType)) {
					// 使用读方法的属性类型继续处理
				}
				else {
					throw new IntrospectionException("Type mismatch between indexed read and write methods: " +
							indexedReadMethod + " - " + indexedWriteMethod);
				}
			}
			else {
				indexedPropertyType = params[1];
			}
		}

		if (propertyType != null && (!propertyType.isArray() ||
				propertyType.getComponentType() != indexedPropertyType)) {
			throw new IntrospectionException("Type mismatch between indexed and non-indexed methods: " +
					indexedReadMethod + " - " + indexedWriteMethod);
		}

		return indexedPropertyType;
	}

	/**
	 * 比较给定的 {@code PropertyDescriptors} 并在它们等价时返回 {@code true}，
	 * 即它们的读方法、写方法、属性类型、属性编辑器和标志都等价。
	 * @see java.beans.PropertyDescriptor#equals(Object)
	 */
	public static boolean equals(PropertyDescriptor pd, PropertyDescriptor otherPd) {
		return (ObjectUtils.nullSafeEquals(pd.getReadMethod(), otherPd.getReadMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getWriteMethod(), otherPd.getWriteMethod()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyType(), otherPd.getPropertyType()) &&
				ObjectUtils.nullSafeEquals(pd.getPropertyEditorClass(), otherPd.getPropertyEditorClass()) &&
				pd.isBound() == otherPd.isBound() && pd.isConstrained() == otherPd.isConstrained());
	}

}
