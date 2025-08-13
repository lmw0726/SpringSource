/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.*;

/**
 * 内部工具类，用于获取包装后的 {@link Serializable} 变体的
 * {@link java.lang.reflect.Type java.lang.reflect.Types}。
 *
 * <p>可以使用 {@link #forField(Field) 字段} 或 {@link #forMethodParameter(MethodParameter) 方法参数}
 * 作为可序列化类型的根源。
 * 也可以使用普通的 {@link Class} 作为源。
 *
 * <p>返回的类型将是 {@link Class}，或者是
 * {@link GenericArrayType}、{@link ParameterizedType}、{@link TypeVariable} 或
 * {@link WildcardType} 的可序列化代理。
 * 除了 {@link Class}（它是 final）外，调用返回进一步 {@link Type 类型} 的方法
 * （例如 {@link GenericArrayType#getGenericComponentType()}）时，
 * 结果也会被自动包装。
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class SerializableTypeWrapper {

	private static final Class<?>[] SUPPORTED_SERIALIZABLE_TYPES = {
			GenericArrayType.class, ParameterizedType.class, TypeVariable.class, WildcardType.class};

	static final ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<>(256);


	private SerializableTypeWrapper() {
	}


	/**
	 * 返回 {@link Field#getGenericType()} 的可序列化变体。
	 */
	@Nullable
	public static Type forField(Field field) {
		return forTypeProvider(new FieldTypeProvider(field));
	}

	/**
	 * 返回 {@link MethodParameter#getGenericParameterType()} 的可序列化变体。
	 */
	@Nullable
	public static Type forMethodParameter(MethodParameter methodParameter) {
		return forTypeProvider(new MethodParameterTypeProvider(methodParameter));
	}

	/**
	 * 解包给定的类型，实际上返回原始的非序列化类型。
	 * @param type 要解包的类型
	 * @return 原始的非序列化类型
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Type> T unwrap(T type) {
		Type unwrapped = null;
		if (type instanceof SerializableTypeProxy) {
			unwrapped = ((SerializableTypeProxy) type).getTypeProvider().getType();
		}
		return (unwrapped != null ? (T) unwrapped : type);
	}

	/**
	 * 返回一个由 {@link TypeProvider} 支持的 {@link Serializable} {@link Type}。
	 * <p>如果当前运行环境中类型相关对象通常不可序列化，
	 * 则该代理会直接返回原始的 {@code Type}。
	 */
	@Nullable
	static Type forTypeProvider(TypeProvider provider) {
		Type providedType = provider.getType();
		if (providedType == null || providedType instanceof Serializable) {
			// 不需要序列化类型包装（例如 java.lang.Class）
			return providedType;
		}
		if (NativeDetector.inNativeImage() || !Serializable.class.isAssignableFrom(Class.class)) {
			// 如果当前运行环境中类型通常不可序列化（比如 GraalVM 原生镜像中的 java.lang.Class），
			// 则跳过任何包装尝试
			return providedType;
		}

		// 为给定的提供者获取可序列化的类型代理...
		Type cached = cache.get(providedType);
		if (cached != null) {
			return cached;
		}
		for (Class<?> type : SUPPORTED_SERIALIZABLE_TYPES) {
			if (type.isInstance(providedType)) {
				ClassLoader classLoader = provider.getClass().getClassLoader();
				Class<?>[] interfaces = new Class<?>[] {type, SerializableTypeProxy.class, Serializable.class};
				InvocationHandler handler = new TypeProxyInvocationHandler(provider);
				cached = (Type) Proxy.newProxyInstance(classLoader, interfaces, handler);
				cache.put(providedType, cached);
				return cached;
			}
		}
		throw new IllegalArgumentException("Unsupported Type class: " + providedType.getClass().getName());
	}


	/**
	 * 类型代理额外实现的接口。
	 */
	interface SerializableTypeProxy {

		/**
		 * 返回底层的类型提供者。
		 */
		TypeProvider getTypeProvider();
	}


	/**
	 * 一个 {@link Serializable} 接口，提供对 {@link Type} 的访问。
	 */
	@SuppressWarnings("serial")
	interface TypeProvider extends Serializable {

		/**
		 * 返回（可能不是 {@link Serializable} 的）{@link Type}。
		 */
		@Nullable
		Type getType();

		/**
		 * 返回类型的来源，如果未知则返回 {@code null}。
		 * <p>默认实现返回 {@code null}。
		 */
		@Nullable
		default Object getSource() {
			return null;
		}
	}


	/**
	 * 用于被代理 {@link Type} 的 {@link Serializable} {@link InvocationHandler} 实现。
	 * 提供序列化支持，并增强所有返回 {@code Type} 或 {@code Type[]} 的方法。
	 */
	@SuppressWarnings("serial")
	private static class TypeProxyInvocationHandler implements InvocationHandler, Serializable {

		private final TypeProvider provider;

		public TypeProxyInvocationHandler(TypeProvider provider) {
			this.provider = provider;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					Object other = args[0];
					// 为了性能，对代理对象进行解包
					if (other instanceof Type) {
						other = unwrap((Type) other);
					}
					return ObjectUtils.nullSafeEquals(this.provider.getType(), other);
				case "hashCode":
					return ObjectUtils.nullSafeHashCode(this.provider.getType());
				case "getTypeProvider":
					return this.provider;
			}

			if (Type.class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
				return forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, -1));
			}
			else if (Type[].class == method.getReturnType() && ObjectUtils.isEmpty(args)) {
				Type[] result = new Type[((Type[]) method.invoke(this.provider.getType())).length];
				for (int i = 0; i < result.length; i++) {
					result[i] = forTypeProvider(new MethodInvokeTypeProvider(this.provider, method, i));
				}
				return result;
			}

			try {
				return method.invoke(this.provider.getType(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * {@link TypeProvider} 的实现，
	 * 用于从 {@link Field} 获取 {@link Type Types}。
	 */
	@SuppressWarnings("serial")
	static class FieldTypeProvider implements TypeProvider {

		private final String fieldName;

		private final Class<?> declaringClass;

		private transient Field field;

		public FieldTypeProvider(Field field) {
			this.fieldName = field.getName();
			this.declaringClass = field.getDeclaringClass();
			this.field = field;
		}

		@Override
		public Type getType() {
			return this.field.getGenericType();
		}

		@Override
		public Object getSource() {
			return this.field;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				this.field = this.declaringClass.getDeclaredField(this.fieldName);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * {@link TypeProvider} 的实现，
	 * 用于从 {@link MethodParameter} 获取 {@link Type Types}。
	 */
	@SuppressWarnings("serial")
	static class MethodParameterTypeProvider implements TypeProvider {

		@Nullable
		private final String methodName;

		private final Class<?>[] parameterTypes;

		private final Class<?> declaringClass;

		private final int parameterIndex;

		private transient MethodParameter methodParameter;

		public MethodParameterTypeProvider(MethodParameter methodParameter) {
			this.methodName = (methodParameter.getMethod() != null ? methodParameter.getMethod().getName() : null);
			this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
			this.declaringClass = methodParameter.getDeclaringClass();
			this.parameterIndex = methodParameter.getParameterIndex();
			this.methodParameter = methodParameter;
		}

		@Override
		public Type getType() {
			return this.methodParameter.getGenericParameterType();
		}

		@Override
		public Object getSource() {
			return this.methodParameter;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			try {
				if (this.methodName != null) {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
				}
				else {
					this.methodParameter = new MethodParameter(
							this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
				}
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not find original class structure", ex);
			}
		}
	}


	/**
	 * {@link TypeProvider} 的实现，
	 * 用于通过调用无参数方法获取 {@link Type} 类型。
	 */
	@SuppressWarnings("serial")
	static class MethodInvokeTypeProvider implements TypeProvider {

		private final TypeProvider provider;

		private final String methodName;

		private final Class<?> declaringClass;

		private final int index;

		private transient Method method;

		@Nullable
		private transient volatile Object result;

		public MethodInvokeTypeProvider(TypeProvider provider, Method method, int index) {
			this.provider = provider;
			this.methodName = method.getName();
			this.declaringClass = method.getDeclaringClass();
			this.index = index;
			this.method = method;
		}

		@Override
		@Nullable
		public Type getType() {
			Object result = this.result;
			if (result == null) {
				// 延迟调用提供类型上的目标方法
				result = ReflectionUtils.invokeMethod(this.method, this.provider.getType());
				// 缓存结果，以便后续调用 getType() 时复用
				this.result = result;
			}
			return (result instanceof Type[] ? ((Type[]) result)[this.index] : (Type) result);
		}

		@Override
		@Nullable
		public Object getSource() {
			return null;
		}

		private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
			inputStream.defaultReadObject();
			Method method = ReflectionUtils.findMethod(this.declaringClass, this.methodName);
			if (method == null) {
				throw new IllegalStateException("Cannot find method on deserialization: " + this.methodName);
			}
			if (method.getReturnType() != Type.class && method.getReturnType() != Type[].class) {
				throw new IllegalStateException(
						"Invalid return type on deserialized method - needs to be Type or Type[]: " + method);
			}
			this.method = method;
		}
	}

}
