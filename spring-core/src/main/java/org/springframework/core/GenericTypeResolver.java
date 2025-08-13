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
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于解析泛型类型与类型变量的辅助类。
 *
 * <p>主要供框架内部使用，用于解析方法参数类型，即使它们是泛型声明的。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 2.5.2
 */
public final class GenericTypeResolver {

	/**
	 * 从 Class 到 TypeVariable 映射的缓存。
	 */
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentReferenceHashMap<>();


	private GenericTypeResolver() {
	}


	/**
	 * 确定给定泛型参数类型的目标类型。
	 *
	 * @param methodParameter     方法参数描述
	 * @param implementationClass 用于解析类型变量的类
	 * @return 对应的泛型参数或返回类型
	 * @deprecated 自 5.2 起，推荐使用 {@code methodParameter.withContainingClass(implementationClass).getParameterType()}
	 */
	@Deprecated
	public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "Class must not be null");
		methodParameter.setContainingClass(implementationClass);
		return methodParameter.getParameterType();
	}

	/**
	 * 确定给定方法的泛型返回类型的目标类型，
	 * 其中形式类型变量声明在给定类上。
	 *
	 * @param method 要分析的方法
	 * @param clazz  用于解析类型变量的类
	 * @return 对应的泛型参数或返回类型
	 */
	public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(clazz, "Class must not be null");
		return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
	}

	/**
	 * 解析给定泛型接口的单一类型参数，
	 * 针对假定返回该接口或其实现的目标方法。
	 *
	 * @param method     目标方法，用于检查返回类型
	 * @param genericIfc 泛型接口或父类，用于解析类型参数
	 * @return 方法返回类型的解析后参数类型，如果无法解析或单个参数为 {@link WildcardType}，则返回 {@code null}
	 */
	@Nullable
	public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
		Assert.notNull(method, "Method must not be null");
		ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericIfc);
		if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	/**
	 * 解析给定泛型接口的单个类型参数，针对给定的目标类（假定目标类实现了该泛型接口并可能声明了其类型变量的具体类型）。
	 *
	 * @param clazz      要检查的目标类
	 * @param genericIfc 要从中解析类型参数的泛型接口或超类
	 * @return 参数的已解析类型，如果无法解析则返回{@code null}
	 */
	@Nullable
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
		// 获取可解析类型
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
		if (!resolvableType.hasGenerics()) {
			// 如果没有泛型参数，返回null
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	/**
	 * 获取单个泛型
	 *
	 * @param resolvableType 可解析类型
	 * @return 单个泛型
	 */
	@Nullable
	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		Assert.isTrue(resolvableType.getGenerics().length == 1,
				() -> "Expected 1 type argument on generic interface [" + resolvableType +
						"] but found " + resolvableType.getGenerics().length);
		return resolvableType.getGeneric().resolve();
	}


	/**
	 * 解析给定泛型接口在目标类上的类型参数，假设目标类实现了该泛型接口，
	 * 并可能为其类型变量声明了具体类型。
	 *
	 * @param clazz      目标类，用于检查
	 * @param genericIfc 需要解析类型参数的泛型接口或父类
	 * @return 解析后的每个类型参数的类型，数组大小对应实际类型参数个数；
	 *         如果无法解析则返回 {@code null}
	 */
	@Nullable
	public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
		ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
		if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
			return null;
		}
		return type.resolveGenerics(Object.class);
	}

	/**
	 * 根据给定的上下文类解析指定的泛型类型，尽可能替换类型变量。
	 *
	 * @param genericType 需要解析的（可能是泛型的）类型
	 * @param contextClass 上下文类，例如目标类型出现在方法签名中的类（可为 {@code null}）
	 * @return 解析后的类型（可能为传入的泛型类型本身）
	 * @since 5.0
	 */
	public static Type resolveType(Type genericType, @Nullable Class<?> contextClass) {
		if (contextClass != null) {
			if (genericType instanceof TypeVariable) {
				ResolvableType resolvedTypeVariable = resolveVariable(
						(TypeVariable<?>) genericType, ResolvableType.forClass(contextClass));
				if (resolvedTypeVariable != ResolvableType.NONE) {
					Class<?> resolved = resolvedTypeVariable.resolve();
					if (resolved != null) {
						return resolved;
					}
				}
			} else if (genericType instanceof ParameterizedType) {
				ResolvableType resolvedType = ResolvableType.forType(genericType);
				if (resolvedType.hasUnresolvableGenerics()) {
					ParameterizedType parameterizedType = (ParameterizedType) genericType;
					Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
					Type[] typeArguments = parameterizedType.getActualTypeArguments();
					ResolvableType contextType = ResolvableType.forClass(contextClass);
					for (int i = 0; i < typeArguments.length; i++) {
						Type typeArgument = typeArguments[i];
						if (typeArgument instanceof TypeVariable) {
							ResolvableType resolvedTypeArgument = resolveVariable(
									(TypeVariable<?>) typeArgument, contextType);
							if (resolvedTypeArgument != ResolvableType.NONE) {
								generics[i] = resolvedTypeArgument.resolve();
							} else {
								generics[i] = ResolvableType.forType(typeArgument).resolve();
							}
						} else {
							generics[i] = ResolvableType.forType(typeArgument).resolve();
						}
					}
					Class<?> rawClass = resolvedType.getRawClass();
					if (rawClass != null) {
						return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
					}
				}
			}
		}
		return genericType;
	}

	private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
		ResolvableType resolvedType;
		if (contextType.hasGenerics()) {
			resolvedType = ResolvableType.forType(typeVariable, contextType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}

		ResolvableType superType = contextType.getSuperType();
		if (superType != ResolvableType.NONE) {
			resolvedType = resolveVariable(typeVariable, superType);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		for (ResolvableType ifc : contextType.getInterfaces()) {
			resolvedType = resolveVariable(typeVariable, ifc);
			if (resolvedType.resolve() != null) {
				return resolvedType;
			}
		}
		return ResolvableType.NONE;
	}

	/**
	 * 根据给定的 TypeVariable 映射解析指定的泛型类型。
	 * <p>由 Spring Data 使用。
	 *
	 * @param genericType 需要解析的泛型类型
	 * @param map         用于解析的 TypeVariable 映射
	 * @return 如果解析为 Class，则返回该 Class，否则返回 {@code Object.class}
	 */
	@SuppressWarnings("rawtypes")
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
		return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
	}

	/**
	 * 构建指定 {@link Class} 的 {@link TypeVariable#getName TypeVariable 名称} 到
	 * {@link Class 具体类} 的映射。
	 * 会搜索所有父类型、包围类型和接口。
	 *
	 * @see #resolveType(Type, Map)
	 */
	@SuppressWarnings("rawtypes")
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<>();
			buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
			typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
		}
		return typeVariableMap;
	}

	@SuppressWarnings("rawtypes")
	private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
		if (type != ResolvableType.NONE) {
			Class<?> resolved = type.resolve();
			if (resolved != null && type.getType() instanceof ParameterizedType) {
				TypeVariable<?>[] variables = resolved.getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					ResolvableType generic = type.getGeneric(i);
					while (generic.getType() instanceof TypeVariable<?>) {
						generic = generic.resolveType();
					}
					if (generic != ResolvableType.NONE) {
						typeVariableMap.put(variables[i], generic.getType());
					}
				}
			}
			buildTypeVariableMap(type.getSuperType(), typeVariableMap);
			for (ResolvableType interfaceType : type.getInterfaces()) {
				buildTypeVariableMap(interfaceType, typeVariableMap);
			}
			if (resolved != null && resolved.isMemberClass()) {
				buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
			}
		}
	}


	@SuppressWarnings({"serial", "rawtypes"})
	private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

		private final Map<TypeVariable, Type> typeVariableMap;

		public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
			this.typeVariableMap = typeVariableMap;
		}

		@Override
		@Nullable
		public ResolvableType resolveVariable(TypeVariable<?> variable) {
			Type type = this.typeVariableMap.get(variable);
			return (type != null ? ResolvableType.forType(type) : null);
		}

		@Override
		public Object getSource() {
			return this.typeVariableMap;
		}
	}

}
