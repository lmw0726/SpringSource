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
 * 提供支持框架中使用的各种命名及其他约定的方法。
 * 主要供框架内部使用。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.0
 */
public final class Conventions {

	/**
	 * 用于数组名的后缀。
	 */
	private static final String PLURAL_SUFFIX = "List";


	private Conventions() {
	}


	/**
	 * 根据具体类型确定给定 {@code Object} 的惯用变量名。
	 * 命名约定是根据 JavaBeans 属性命名规则返回类名的小写开头简短名。
	 * <p>例如：<br>
	 * {@code com.myapp.Product} 返回 {@code "product"}<br>
	 * {@code com.myapp.MyProduct} 返回 {@code "myProduct"}<br>
	 * {@code com.myapp.UKProduct} 返回 {@code "UKProduct"}<br>
	 * <p>对于数组，使用数组组件类型的复数形式。
	 * 对于 {@code Collection}，尝试“提前查看”以确定组件类型并返回其复数形式。
	 *
	 * @param value 要生成变量名的值
	 * @return 生成的变量名
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
	 * 确定给定方法返回类型的常规变量名，考虑泛型集合类型（如果有）。
	 *
	 * @param method 要生成变量名的方法
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method) {
		return getVariableNameForReturnType(method, method.getReturnType(), null);
	}

	/**
	 * 确定给定方法返回类型的常规变量名，考虑泛型集合类型（如果有）。
	 * 如果方法声明不够具体（如返回类型为 {@code Object} 或未指定类型的集合），
	 * 则退回到给定的实际返回值。
	 *
	 * @param method 要生成变量名的方法
	 * @param value  返回值（如果不可用，可能为 {@code null}）
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method, @Nullable Object value) {
		return getVariableNameForReturnType(method, method.getReturnType(), value);
	}

	/**
	 * 确定给定方法返回类型的常规变量名，考虑泛型集合类型（如果有）。
	 * 如果方法声明不够具体（如返回类型为 {@code Object} 或未指定类型的集合），
	 * 则退回到给定的返回值。
	 * <p>从 5.0 版本开始，此方法支持响应式类型：<br>
	 * {@code Mono<com.myapp.Product>} 变为 {@code "productMono"}<br>
	 * {@code Flux<com.myapp.MyProduct>} 变为 {@code "myProductFlux"}<br>
	 * {@code Observable<com.myapp.MyProduct>} 变为 {@code "myProductObservable"}<br>
	 *
	 * @param method       要生成变量名的方法
	 * @param resolvedType 方法的已解析返回类型
	 * @param value        返回值（如果不可用，可能为 {@code null}）
	 * @return 生成的变量名
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
	 * 返回由给定包围 {@link Class} 限定的属性名。
	 * 例如，属性名 '{@code foo}' 由类 '{@code com.myapp.SomeClass}' 限定后
	 * 应为 '{@code com.myapp.SomeClass.foo}'。
	 */
	public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
		Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
		Assert.notNull(attributeName, "'attributeName' must not be null");
		return enclosingClass.getName() + '.' + attributeName;
	}


	/**
	 * 确定用于命名包含给定值的变量的类。
	 * <p>通常返回给定值的类，遇到 JDK 动态代理时，
	 * 则返回该代理实现的“主”接口。
	 *
	 * @param value 要检查的值
	 * @return 用于命名变量的类
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
			// 类名中包含 '$' 但无内部类声明，
			// 假设这是一个特殊子类（例如由 OpenJPA 生成）
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
	 * 获取 {@code Collection} 中元素的 {@code Class}。
	 * 具体获取哪个元素的 {@code Class} 取决于具体的 {@code Collection} 实现。
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
