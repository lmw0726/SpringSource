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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 通过将源对象委托给源对象上的方法或目标类型上的静态工厂方法或构造函数，使用约定将源对象转换为 {@code targetType} 的通用转换器。
 *
 * <h3>转换算法</h3>
 * <ol>
 * <li>如果源对象存在一个返回类型可分配给 {@code targetType} 的非静态 {@code to[targetType.simpleName]()} 方法，则调用该方法。例如，{@code org.example.Bar Foo#toBar()} 就是遵循此约定的方法。
 * <li>否则，如果 {@code targetType} 上存在一个与 {@code targetType} 有关的返回类型的 <em>静态</em> {@code valueOf(sourceType)} 或 Java 8 风格的 <em>静态</em> {@code of(sourceType)} 或 {@code from(sourceType)} 方法，则调用该方法。例如，返回 {@code Foo}、{@code SuperFooType} 或 {@code SubFooType} 的静态 {@code Foo.of(sourceType)} 方法就是遵循此约定的方法。{@link java.time.ZoneId#of(String)} 就是一个返回 {@code ZoneId} 的子类型的具体静态工厂方法的示例。
 * <li>否则，如果 {@code targetType} 上存在一个接受单个 {@code sourceType} 参数的构造函数，则调用该构造函数。
 * <li>否则，抛出 {@link ConversionFailedException} 或 {@link IllegalStateException}。
 * </ol>
 *
 * <p><strong>警告</strong>：此转换器不支持将 {@code sourceType} 转换为 {@code java.lang.String} 的 {@link Object#toString()} 或 {@link String#valueOf(Object)} 方法。如果需要 {@code toString()} 支持，请改用 {@link FallbackObjectToStringConverter}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see FallbackObjectToStringConverter
 * @since 3.0
 */
final class ObjectToObjectConverter implements ConditionalGenericConverter {

	// 用于缓存在给定类上解析的最新的to-method、静态工厂方法或工厂构造函数
	// 使用ConcurrentReferenceHashMap作为线程安全的缓存
	private static final Map<Class<?>, Executable> conversionExecutableCache =
			new ConcurrentReferenceHashMap<>(32);


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, Object.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (sourceType.getType() != targetType.getType() &&
				hasConversionMethodOrConstructor(targetType.getType(), sourceType.getType()));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则直接返回null
		if (source == null) {
			return null;
		}
		// 获取源类型和目标类型的Class对象
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		// 获取验证后的可执行方法或构造函数
		Executable executable = getValidatedExecutable(targetClass, sourceClass);

		try {
			// 如果可执行对象是方法
			if (executable instanceof Method) {
				Method method = (Method) executable;
				// 设置方法为可访问状态
				ReflectionUtils.makeAccessible(method);
				// 如果方法不是静态方法
				if (!Modifier.isStatic(method.getModifiers())) {
					// 调用方法并返回结果
					return method.invoke(source);
				} else {
					// 调用静态方法并返回结果
					return method.invoke(null, source);
				}
			} else if (executable instanceof Constructor) {
				// 如果可执行对象是构造函数
				Constructor<?> ctor = (Constructor<?>) executable;
				// 设置构造函数为可访问状态
				ReflectionUtils.makeAccessible(ctor);
				// 使用构造函数创建新实例并返回结果
				return ctor.newInstance(source);
			}
		} catch (InvocationTargetException ex) {
			// 处理反射调用异常，并抛出转换失败异常
			throw new ConversionFailedException(sourceType, targetType, source, ex.getTargetException());
		} catch (Throwable ex) {
			// 处理其他异常，并抛出转换失败异常
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}

		// 如果方法或构造函数不存在，则抛出状态异常，提供详细错误信息
		throw new IllegalStateException(String.format("No to%3$s() method exists on %1$s, " +
						"and no static valueOf/of/from(%1$s) method or %3$s(%1$s) constructor exists on %2$s.",
				sourceClass.getName(), targetClass.getName(), targetClass.getSimpleName()));
	}


	static boolean hasConversionMethodOrConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return (getValidatedExecutable(targetClass, sourceClass) != null);
	}

	@Nullable
	private static Executable getValidatedExecutable(Class<?> targetClass, Class<?> sourceClass) {
		// 从缓存中获取已解析的可执行方法或构造函数
		Executable executable = conversionExecutableCache.get(targetClass);
		// 如果已存在适用于源和目标类的可执行对象，则直接返回
		if (isApplicable(executable, sourceClass)) {
			return executable;
		}

		// 确定目标类和源类之间的转换方法
		executable = determineToMethod(targetClass, sourceClass);
		// 如果未找到转换方法，则尝试确定工厂方法
		if (executable == null) {
			executable = determineFactoryMethod(targetClass, sourceClass);
			// 如果未找到工厂方法，则尝试确定工厂构造函数
			if (executable == null) {
				executable = determineFactoryConstructor(targetClass, sourceClass);
				// 如果仍未找到适用的方法或构造函数，则返回null
				if (executable == null) {
					return null;
				}
			}
		}

		// 将已确定的可执行对象放入缓存中，并返回该对象
		conversionExecutableCache.put(targetClass, executable);
		return executable;
	}

	private static boolean isApplicable(Executable executable, Class<?> sourceClass) {
		// 如果可执行对象是方法
		if (executable instanceof Method) {
			Method method = (Method) executable;
			// 如果方法不是静态方法，则检查方法声明的类是否可分配给源类
			// 如果方法是静态方法，则检查方法的第一个参数类型是否与源类相同
			return (!Modifier.isStatic(method.getModifiers()) ?
					ClassUtils.isAssignable(method.getDeclaringClass(), sourceClass) :
					method.getParameterTypes()[0] == sourceClass);
		} else if (executable instanceof Constructor) {
			// 如果可执行对象是构造函数
			Constructor<?> ctor = (Constructor<?>) executable;
			// 检查构造函数的第一个参数类型是否与源类相同
			return (ctor.getParameterTypes()[0] == sourceClass);
		} else {
			// 如果可执行对象既不是方法也不是构造函数，则返回false
			return false;
		}

	}

	@Nullable
	private static Method determineToMethod(Class<?> targetClass, Class<?> sourceClass) {
		// 如果目标类或源类是String类型，则返回null，不接受String类自身的toString()方法或任何to方法
		if (String.class == targetClass || String.class == sourceClass) {
			return null;
		}

		// 尝试获取名称为"to" + 目标类名的方法
		Method method = ClassUtils.getMethodIfAvailable(sourceClass, "to" + targetClass.getSimpleName());

		// 如果方法不为空且不是静态方法，并且目标类可分配给方法的返回类型，则返回该方法，否则返回null
		return (method != null && !Modifier.isStatic(method.getModifiers()) &&
				ClassUtils.isAssignable(targetClass, method.getReturnType()) ? method : null);
	}

	@Nullable
	private static Method determineFactoryMethod(Class<?> targetClass, Class<?> sourceClass) {
		// 如果目标类是String类型，则返回null，不接受String.valueOf(Object)方法
		if (String.class == targetClass) {
			return null;
		}

		// 尝试获取名称为"valueOf"、"of"或"from"的静态方法，参数类型为源类
		Method method = ClassUtils.getStaticMethod(targetClass, "valueOf", sourceClass);
		if (method == null) {
			method = ClassUtils.getStaticMethod(targetClass, "of", sourceClass);
			if (method == null) {
				method = ClassUtils.getStaticMethod(targetClass, "from", sourceClass);
			}
		}

		// 如果方法不为空且目标类与方法的返回类型存在关联，则返回该方法，否则返回null
		return (method != null && areRelatedTypes(targetClass, method.getReturnType()) ? method : null);
	}

	/**
	 * 确定这两种类型是否位于同一类型层次结构中（即，类型 1 可分配给类型 2，反之亦然）。
	 *
	 * @see ClassUtils#isAssignable(Class, Class)
	 * @since 5.3.21
	 */
	private static boolean areRelatedTypes(Class<?> type1, Class<?> type2) {
		return (ClassUtils.isAssignable(type1, type2) || ClassUtils.isAssignable(type2, type1));
	}

	@Nullable
	private static Constructor<?> determineFactoryConstructor(Class<?> targetClass, Class<?> sourceClass) {
		return ClassUtils.getConstructorIfAvailable(targetClass, sourceClass);
	}

}
