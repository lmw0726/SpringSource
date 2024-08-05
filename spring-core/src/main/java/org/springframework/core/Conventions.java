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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

/**
 * Provides methods to support various naming and other conventions used
 * throughout the framework. Mainly for internal use within the framework.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
public final class Conventions {

	/**
	 * Suffix added to names when using arrays.
	 */
	private static final String PLURAL_SUFFIX = "List";


	private Conventions() {
	}


	/**
	 * Determine the conventional variable name for the supplied {@code Object}
	 * based on its concrete type. The convention used is to return the
	 * un-capitalized short name of the {@code Class}, according to JavaBeans
	 * property naming rules.
	 * <p>For example:<br>
	 * {@code com.myapp.Product} becomes {@code "product"}<br>
	 * {@code com.myapp.MyProduct} becomes {@code "myProduct"}<br>
	 * {@code com.myapp.UKProduct} becomes {@code "UKProduct"}<br>
	 * <p>For arrays the pluralized version of the array component type is used.
	 * For {@code Collection}s an attempt is made to 'peek ahead' to determine
	 * the component type and return its pluralized version.
	 *
	 * @param value the value to generate a variable name for
	 * @return the generated variable name
	 */
	public static String getVariableName(Object value) {
		Assert.notNull(value, "Value must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (value.getClass().isArray()) {
			valueClass = value.getClass().getComponentType();
			pluralize = true;
		} else if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for an empty Collection");
			}
			Object valueToCheck = peekAhead(collection);
			valueClass = getClassForValue(valueToCheck);
			pluralize = true;
		} else {
			valueClass = getClassForValue(value);
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * 为给定的参数确定常规的变量名，
	 * 如果参数是泛型集合类型，则考虑该类型。
	 * <p>自5.0版本起，此方法支持响应式类型：<br>
	 * {@code Mono<com.myapp.Product>} 变为 {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} 变为 {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} 变为 {@code "myProductObservable"}<br>
	 *
	 * @param parameter 方法或构造函数的参数
	 * @return 生成的变量名
	 */
	public static String getVariableNameForParameter(MethodParameter parameter) {
		Assert.notNull(parameter, "MethodParameter must not be null");

		// 存储参数的实际类型的Class<?>类型
		Class<?> valueClass;

		// 标记是否需要将名称复数化布尔变量pluralize，
		boolean pluralize = false;

		// 存储响应式类型的后缀的字符串变量reactiveSuffix，
		String reactiveSuffix = "";

		// 判断参数是否为数组类型
		if (parameter.getParameterType().isArray()) {
			// 如果是数组类型，则获取数组的元素类型
			valueClass = parameter.getParameterType().getComponentType();
			// 标记需要将名称复数化
			pluralize = true;
		} else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
			// 如果是集合类型，则使用ResolvableType获取集合元素的类型
			valueClass = ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
			// 如果获取不到元素类型，则抛出异常
			if (valueClass == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for non-typed Collection parameter type");
			}
			// 标记需要将名称复数化
			pluralize = true;
		} else {
			// 如果参数既不是数组也不是集合
			// 直接将参数类型设置为实际的参数类型
			valueClass = parameter.getParameterType();
			// 尝试获取参数的响应式适配器
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
			// 如果存在适配器且适配器描述的不是空值
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				// 将响应式类型的简短名称作为后缀
				reactiveSuffix = ClassUtils.getShortName(valueClass);
				// 尝试获取嵌套参数类型，用于后续处理
				valueClass = parameter.nested().getNestedParameterType();
			}
		}

		// 使用ClassUtils获取实际类型的简短名称，并作为属性名
		String name = ClassUtils.getShortNameAsProperty(valueClass);
		// 如果需要复数化，则对名称进行复数化处理；否则，将reactiveSuffix附加到名称后
		return (pluralize ? pluralize(name) : name + reactiveSuffix);
	}

	/**
	 * Determine the conventional variable name for the return type of the
	 * given method, taking the generic collection type, if any, into account.
	 *
	 * @param method the method to generate a variable name for
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method) {
		return getVariableNameForReturnType(method, method.getReturnType(), null);
	}

	/**
	 * Determine the conventional variable name for the return type of the given
	 * method, taking the generic collection type, if any, into account, falling
	 * back on the given actual return value if the method declaration is not
	 * specific enough, e.g. {@code Object} return type or untyped collection.
	 *
	 * @param method the method to generate a variable name for
	 * @param value  the return value (may be {@code null} if not available)
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method, @Nullable Object value) {
		return getVariableNameForReturnType(method, method.getReturnType(), value);
	}

	/**
	 * Determine the conventional variable name for the return type of the given
	 * method, taking the generic collection type, if any, into account, falling
	 * back on the given return value if the method declaration is not specific
	 * enough, e.g. {@code Object} return type or untyped collection.
	 * <p>As of 5.0 this method supports reactive types:<br>
	 * {@code Mono<com.myapp.Product>} becomes {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} becomes {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} becomes {@code "myProductObservable"}<br>
	 *
	 * @param method       the method to generate a variable name for
	 * @param resolvedType the resolved return type of the method
	 * @param value        the return value (may be {@code null} if not available)
	 * @return the generated variable name
	 */
	public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, @Nullable Object value) {
		Assert.notNull(method, "Method must not be null");

		if (Object.class == resolvedType) {
			if (value == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for an Object return type with null value");
			}
			return getVariableName(value);
		}

		Class<?> valueClass;
		boolean pluralize = false;
		String reactiveSuffix = "";

		if (resolvedType.isArray()) {
			valueClass = resolvedType.getComponentType();
			pluralize = true;
		} else if (Collection.class.isAssignableFrom(resolvedType)) {
			valueClass = ResolvableType.forMethodReturnType(method).asCollection().resolveGeneric();
			if (valueClass == null) {
				if (!(value instanceof Collection)) {
					throw new IllegalArgumentException("Cannot generate variable name " +
							"for non-typed Collection return type and a non-Collection value");
				}
				Collection<?> collection = (Collection<?>) value;
				if (collection.isEmpty()) {
					throw new IllegalArgumentException("Cannot generate variable name " +
							"for non-typed Collection return type and an empty Collection value");
				}
				Object valueToCheck = peekAhead(collection);
				valueClass = getClassForValue(valueToCheck);
			}
			pluralize = true;
		} else {
			valueClass = resolvedType;
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
			if (adapter != null && !adapter.getDescriptor().isNoValue()) {
				reactiveSuffix = ClassUtils.getShortName(valueClass);
				valueClass = ResolvableType.forMethodReturnType(method).getGeneric().toClass();
			}
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name + reactiveSuffix);
	}

	/**
	 * 将属性名称格式的 {@code String} (例如小写，连字符分隔单词) 转换为属性名称格式 (camel-case)。例如 {@code transaction-manager} 变为 {@code “transactionManager”}。
	 */
	public static String attributeNameToPropertyName(String attributeName) {
		Assert.notNull(attributeName, "'attributeName' must not be null");
		if (!attributeName.contains("-")) {
			return attributeName;
		}
		char[] result = new char[attributeName.length() - 1]; // not completely accurate but good guess
		int currPos = 0;
		boolean upperCaseNext = false;
		for (int i = 0; i < attributeName.length(); i++) {
			char c = attributeName.charAt(i);
			if (c == '-') {
				//碰到 - 符号，将下个字符设置为大写
				upperCaseNext = true;
			} else if (upperCaseNext) {
				//如果下个字符串是大写，将当前字符设置为大写
				result[currPos++] = Character.toUpperCase(c);
				//将下个字符设置为大写的标记设置为false
				upperCaseNext = false;
			} else {
				result[currPos++] = c;
			}
		}
		return new String(result, 0, currPos);
	}

	/**
	 * Return an attribute name qualified by the given enclosing {@link Class}.
	 * For example the attribute name '{@code foo}' qualified by {@link Class}
	 * '{@code com.myapp.SomeClass}' would be '{@code com.myapp.SomeClass.foo}'
	 */
	public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
		Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
		Assert.notNull(attributeName, "'attributeName' must not be null");
		return enclosingClass.getName() + '.' + attributeName;
	}


	/**
	 * Determine the class to use for naming a variable containing the given value.
	 * <p>Will return the class of the given value, except when encountering a
	 * JDK proxy, in which case it will determine the 'primary' interface
	 * implemented by that proxy.
	 *
	 * @param value the value to check
	 * @return the class to use for naming a variable
	 */
	private static Class<?> getClassForValue(Object value) {
		Class<?> valueClass = value.getClass();
		if (Proxy.isProxyClass(valueClass)) {
			Class<?>[] ifcs = valueClass.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (!ClassUtils.isJavaLanguageInterface(ifc)) {
					return ifc;
				}
			}
		} else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
			// '$' in the class name but no inner class -
			// assuming it's a special subclass (e.g. by OpenJPA)
			valueClass = valueClass.getSuperclass();
		}
		return valueClass;
	}

	/**
	 * 将给定的名称转换为复数形式。
	 */
	private static String pluralize(String name) {
		return name + PLURAL_SUFFIX;
	}

	/**
	 * Retrieve the {@code Class} of an element in the {@code Collection}.
	 * The exact element for which the {@code Class} is retrieved will depend
	 * on the concrete {@code Collection} implementation.
	 */
	private static <E> E peekAhead(Collection<E> collection) {
		Iterator<E> it = collection.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - no element found");
		}
		E value = it.next();
		if (value == null) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - only null element found");
		}
		return value;
	}

}
