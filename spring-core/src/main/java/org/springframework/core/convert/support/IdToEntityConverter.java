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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;

/**
 * 通过在目标实体类型上调用静态查找方法，将实体标识符转换为实体引用。
 *
 * <p>为了使该转换器匹配，查找方法必须是静态的，具有签名{@code find[EntityName]([IdType])}，并返回所需实体类型的实例。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class IdToEntityConverter implements ConditionalGenericConverter {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public IdToEntityConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Method finder = getFinder(targetType.getType());
		return (finder != null &&
				this.conversionService.canConvert(sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0])));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为空，则直接返回null
		if (source == null) {
			return null;
		}

		// 获取目标类型的查找方法
		Method finder = getFinder(targetType.getType());

		// 断言确保查找方法不为空
		Assert.state(finder != null, "No finder method");

		// 将源对象转换为查找方法的参数类型
		Object id = this.conversionService.convert(
				source, sourceType, TypeDescriptor.valueOf(finder.getParameterTypes()[0]));

		// 通过反射调用查找方法并返回结果
		return ReflectionUtils.invokeMethod(finder, source, id);
	}

	@Nullable
	private Method getFinder(Class<?> entityClass) {
		// 构建查找方法名
		String finderMethod = "find" + getEntityName(entityClass);

		// 初始化方法数组和本地过滤标志位
		Method[] methods;
		boolean localOnlyFiltered;

		try {
			// 尝试获取实体类的声明方法
			methods = entityClass.getDeclaredMethods();
			localOnlyFiltered = true;
		} catch (SecurityException ex) {
			// 如果无法访问非公共方法，则回退到仅检查本地声明的公共方法
			methods = entityClass.getMethods();
			localOnlyFiltered = false;
		}

		// 遍历方法数组，寻找匹配的查找方法
		for (Method method : methods) {
			if (Modifier.isStatic(method.getModifiers()) && method.getName().equals(finderMethod) &&
					method.getParameterCount() == 1 && method.getReturnType().equals(entityClass) &&
					(localOnlyFiltered || method.getDeclaringClass().equals(entityClass))) {
				return method;
			}
		}

		// 如果找不到匹配的查找方法，则返回null
		return null;
	}

	private String getEntityName(Class<?> entityClass) {
		// 获取实体类的简称
		String shortName = ClassUtils.getShortName(entityClass);

		// 查找最后一个点的位置
		int lastDot = shortName.lastIndexOf('.');

		// 如果存在点，则返回点之后的部分，否则返回原始简称
		if (lastDot != -1) {
			return shortName.substring(lastDot + 1);
		} else {
			return shortName;
		}
	}

}
