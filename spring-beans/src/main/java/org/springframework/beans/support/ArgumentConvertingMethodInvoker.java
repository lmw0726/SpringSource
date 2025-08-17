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

package org.springframework.beans.support;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyEditor;
import java.lang.reflect.Method;

/**
 * {@link MethodInvoker} 的子类，尝试通过 {@link TypeConverter} 将给定参数转换为实际目标方法所需类型。
 *
 * <p>支持灵活的参数转换，特别是在调用特定重载方法时。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.beans.BeanWrapperImpl#convertIfNecessary
 */
public class ArgumentConvertingMethodInvoker extends MethodInvoker {

	@Nullable
	private TypeConverter typeConverter;

	private boolean useDefaultConverter = true;


	/**
	 * 设置用于参数类型转换的 TypeConverter。
	 * <p>默认是 {@link org.springframework.beans.SimpleTypeConverter}。
	 * 可以使用任何 TypeConverter 实现覆盖，通常是预配置的 SimpleTypeConverter 或 BeanWrapperImpl 实例。
	 * @see org.springframework.beans.SimpleTypeConverter
	 * @see org.springframework.beans.BeanWrapperImpl
	 */
	public void setTypeConverter(@Nullable TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
		this.useDefaultConverter = (typeConverter == null);
	}

	/**
	 * 返回用于参数类型转换的 TypeConverter。
	 * <p>如果希望直接访问底层 PropertyEditors，可将其强制转换为 {@link org.springframework.beans.PropertyEditorRegistry}，
	 * 前提是当前 TypeConverter 实现了 PropertyEditorRegistry 接口。
	 */
	@Nullable
	public TypeConverter getTypeConverter() {
		if (this.typeConverter == null && this.useDefaultConverter) {
			this.typeConverter = getDefaultTypeConverter();
		}
		return this.typeConverter;
	}

	/**
	 * 获取该方法调用器的默认 TypeConverter。
	 * <p>如果未指定显式 TypeConverter，将调用此方法。
	 * 默认实现创建 {@link org.springframework.beans.SimpleTypeConverter}。
	 * 可在子类中覆盖。
	 */
	protected TypeConverter getDefaultTypeConverter() {
		return new SimpleTypeConverter();
	}

	/**
	 * 为给定类型的所有属性注册自定义属性编辑器。
	 * <p>通常与默认 {@link org.springframework.beans.SimpleTypeConverter} 一起使用；
	 * 也可用于实现了 PropertyEditorRegistry 接口的任何 TypeConverter。
	 * @param requiredType 属性类型
	 * @param propertyEditor 要注册的编辑器
	 * @see #setTypeConverter
	 * @see org.springframework.beans.PropertyEditorRegistry#registerCustomEditor
	 */
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		TypeConverter converter = getTypeConverter();
		if (!(converter instanceof PropertyEditorRegistry)) {
			throw new IllegalStateException(
					"TypeConverter does not implement PropertyEditorRegistry interface: " + converter);
		}
		((PropertyEditorRegistry) converter).registerCustomEditor(requiredType, propertyEditor);
	}


	/**
	 * 该实现查找具有匹配参数类型的方法。
	 * @see #doFindMatchingMethod
	 */
	@Override
	protected Method findMatchingMethod() {
		Method matchingMethod = super.findMatchingMethod();
		// 第二轮尝试：查找参数可被转换为方法参数类型的方法
		if (matchingMethod == null) {
			// 将参数数组解释为单独的方法参数
			matchingMethod = doFindMatchingMethod(getArguments());
		}
		if (matchingMethod == null) {
			// 将参数数组解释为单个数组类型参数
			matchingMethod = doFindMatchingMethod(new Object[] {getArguments()});
		}
		return matchingMethod;
	}

	/**
	 * 实际查找具有匹配参数类型的方法，即每个参数值都可赋值给对应的方法参数类型。
	 * @param arguments 要匹配的方法参数值
	 * @return 匹配的方法，若没有匹配则返回 {@code null}
	 */
	@Nullable
	protected Method doFindMatchingMethod(Object[] arguments) {
		TypeConverter converter = getTypeConverter();
		if (converter != null) {
			String targetMethod = getTargetMethod();
			Method matchingMethod = null;
			int argCount = arguments.length;
			Class<?> targetClass = getTargetClass();
			Assert.state(targetClass != null, "No target class set");
			Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
			int minTypeDiffWeight = Integer.MAX_VALUE;
			Object[] argumentsToUse = null;
			for (Method candidate : candidates) {
				if (candidate.getName().equals(targetMethod)) {
					// 检查方法参数数量是否正确
					int parameterCount = candidate.getParameterCount();
					if (parameterCount == argCount) {
						Class<?>[] paramTypes = candidate.getParameterTypes();
						Object[] convertedArguments = new Object[argCount];
						boolean match = true;
						for (int j = 0; j < argCount && match; j++) {
							// 验证提供的参数是否可赋值给方法参数类型
							try {
								convertedArguments[j] = converter.convertIfNecessary(arguments[j], paramTypes[j]);
							}
							catch (TypeMismatchException ex) {
								// 不匹配则忽略
								match = false;
							}
						}
						if (match) {
							int typeDiffWeight = getTypeDifferenceWeight(paramTypes, convertedArguments);
							if (typeDiffWeight < minTypeDiffWeight) {
								minTypeDiffWeight = typeDiffWeight;
								matchingMethod = candidate;
								argumentsToUse = convertedArguments;
							}
						}
					}
				}
			}
			if (matchingMethod != null) {
				setArguments(argumentsToUse);
				return matchingMethod;
			}
		}
		return null;
	}

}
