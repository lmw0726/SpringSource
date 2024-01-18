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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * Utility class that contains various methods useful for the implementation of
 * autowire-capable bean factories.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @see AbstractAutowireCapableBeanFactory
 * @since 1.1.2
 */
abstract class AutowireUtils {

	public static final Comparator<Executable> EXECUTABLE_COMPARATOR = (e1, e2) -> {
		int result = Boolean.compare(Modifier.isPublic(e2.getModifiers()), Modifier.isPublic(e1.getModifiers()));
		return result != 0 ? result : Integer.compare(e2.getParameterCount(), e1.getParameterCount());
	};


	/**
	 * Sort the given constructors, preferring public constructors and "greedy" ones with
	 * a maximum number of arguments. The result will contain public constructors first,
	 * with decreasing number of arguments, then non-public constructors, again with
	 * decreasing number of arguments.
	 *
	 * @param constructors the constructor array to sort
	 */
	public static void sortConstructors(Constructor<?>[] constructors) {
		Arrays.sort(constructors, EXECUTABLE_COMPARATOR);
	}

	/**
	 * Sort the given factory methods, preferring public methods and "greedy" ones
	 * with a maximum of arguments. The result will contain public methods first,
	 * with decreasing number of arguments, then non-public methods, again with
	 * decreasing number of arguments.
	 *
	 * @param factoryMethods the factory method array to sort
	 */
	public static void sortFactoryMethods(Method[] factoryMethods) {
		Arrays.sort(factoryMethods, EXECUTABLE_COMPARATOR);
	}

	/**
	 * 确定给定的bean属性是否被排除在依赖性检查之外。
	 * <p>此实现排除由CGLIB定义的属性。
	 *
	 * @param pd bean属性的PropertyDescriptor
	 * @return bean属性是否被排除
	 */
	public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		Method wm = pd.getWriteMethod();
		if (wm == null) {
			//如果没有set方法，返回false
			return false;
		}
		if (!wm.getDeclaringClass().getName().contains("$$")) {
			// 不是CGLIB方法，所以是可以的。
			return false;
		}
		// 它是由CGLIB声明的，但如果它实际上是由超类声明的，我们仍然可能想要进行自动装配。
		Class<?> superclass = wm.getDeclaringClass().getSuperclass();
		// 如果超类中没有该set方法，返回true
		return !ClassUtils.hasMethod(superclass, wm);
	}

	/**
	 * 返回给定bean属性的setter方法是否在给定接口中的任何一个中定义。
	 *
	 * @param pd         bean属性的PropertyDescriptor
	 * @param interfaces 接口的Set（Class对象）
	 * @return setter方法是否由接口定义
	 */
	public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
		Method setter = pd.getWriteMethod();
		if (setter != null) {
			// 如果存在set方法，获取set方法的类
			Class<?> targetClass = setter.getDeclaringClass();
			for (Class<?> ifc : interfaces) {
				if (ifc.isAssignableFrom(targetClass) && ClassUtils.hasMethod(ifc, setter)) {
					//如果set方法的类是接口的实现类并且该接口方法有该名称的set方法，返回true
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Resolve the given autowiring value against the given required type,
	 * e.g. an {@link ObjectFactory} value to its actual object result.
	 *
	 * @param autowiringValue the value to resolve
	 * @param requiredType    the type to assign the result to
	 * @return the resolved value
	 */
	public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
		if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
			ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
			if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
				autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(), new Class<?>[]{requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
			} else {
				return factory.getObject();
			}
		}
		return autowiringValue;
	}

	/**
	 * 确定给定 <em>泛型工厂方法 </em> 的泛型返回类型的目标类型 ，其中形式类型变量在给定方法本身上声明。
	 * <p>例如，给定具有以下签名的工厂方法，如果{@code resolveReturnTypeForFactoryMethod()} 使用反射方法
	 * 调用{@code createProxy()}，并且{@code Object[]}数组包含{@code MyService.class}，{@code resolveReturnTypeForFactoryMethod()}将推断目标返回类型为MyService。
	 * <pre class="code">{@code public static <T> T createProxy(Class<T> clazz)}</pre>
	 * <h4>可能的返回值</h4>
	 * <ul>
	 * <li>目标返回类型（如果可以推断）</li>
	 * <li>标准返回类型，如果给定方法没有声明任何正式类型变量</li>
	 * <li>标准返回类型，如果无法推断目标返回类型（例如，由于类型擦除）</li>
	 * <li>null，如果给定参数数组的长度短于给定方法的形式参数列表的长度</li>
	 * </ul>
	 *
	 * @param method      内省的方法 (从不为 {@code null})
	 * @param args        调用该方法时将提供给该方法的参数 (从不为 {@code null})
	 * @param classLoader 类加载器，用于根据需要解析类名 (从不 {@code null})
	 * @return 解析的目标返回类型或标准方法返回类型
	 * @since 3.2.5
	 */
	public static Class<?> resolveReturnTypeForFactoryMethod(Method method, Object[] args, @Nullable ClassLoader classLoader) {

		Assert.notNull(method, "Method must not be null");
		Assert.notNull(args, "Argument array must not be null");
		//获取方法的泛型类型
		TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
		//获取方法的通用返回类型（含有泛型的返回类型）
		Type genericReturnType = method.getGenericReturnType();
		//获取方法的通用参数类型
		Type[] methodParameterTypes = method.getGenericParameterTypes();
		Assert.isTrue(args.length == methodParameterTypes.length, "Argument array does not match parameter count");

		// 确保类型变量 (例如，T) 直接在方法本身上声明 (例如，通过 <T>)，而不是在封闭类或接口上声明。
		boolean locallyDeclaredTypeVariableMatchesReturnType = false;
		for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
			//遍历方法的泛型类型
			if (currentTypeVariable.equals(genericReturnType)) {
				//如果当前的泛型类型与通用返回类型相同，本地声明的类型变量匹配返回类型
				locallyDeclaredTypeVariableMatchesReturnType = true;
				break;
			}
		}

		if (locallyDeclaredTypeVariableMatchesReturnType) {
			for (int i = 0; i < methodParameterTypes.length; i++) {
				//按照索引下标遍历参数类型
				Type methodParameterType = methodParameterTypes[i];
				Object arg = args[i];
				if (methodParameterType.equals(genericReturnType)) {
					//如果方法参数类型和通用返回类型相同
					if (arg instanceof TypedStringValue) {
						//如果当前参数值是类型字符串值
						TypedStringValue typedValue = ((TypedStringValue) arg);
						if (typedValue.hasTargetType()) {
							//如果类型字符串值有目标类型，返回该目标类型
							return typedValue.getTargetType();
						}
						try {
							//根据类加载器解析该目标类型
							Class<?> resolvedType = typedValue.resolveTargetType(classLoader);
							if (resolvedType != null) {
								//解析好的类型不为空，返回该类型
								return resolvedType;
							}
						} catch (ClassNotFoundException ex) {
							throw new IllegalStateException("Failed to resolve value type [" + typedValue.getTargetTypeName() + "] for factory method argument", ex);
						}
					} else if (arg != null && !(arg instanceof BeanMetadataElement)) {
						//如果该参数值不为空，且参数值不是BeanMetadataElement类型，返回该参数的类型
						return arg.getClass();
					}
					//获取方法的返回类型
					return method.getReturnType();
				} else if (methodParameterType instanceof ParameterizedType) {
					//如果当前参数类型为参数化类型，如：List<String>或者Map<String,Object>
					ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
					//获取泛型的实际参数类型
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					for (Type typeArg : actualTypeArguments) {
						if (typeArg.equals(genericReturnType)) {
							//如果泛型的实际参数类型与泛型化的返回类型相同。
							//如：public T methodX(List<T> list){ return null; }
							if (arg instanceof Class) {
								//如果参数的类型为Class类型，则返回Class类
								return (Class<?>) arg;
							} else {
								String className = null;
								if (arg instanceof String) {
									//如果是参数值是字符串类型，类名为参数值
									className = (String) arg;
								} else if (arg instanceof TypedStringValue) {
									//如果参数值是TypedStringValue类型
									TypedStringValue typedValue = ((TypedStringValue) arg);
									//获取目标类型名称
									String targetTypeName = typedValue.getTargetTypeName();
									if (targetTypeName == null || Class.class.getName().equals(targetTypeName)) {
										//如果目标类型名称为空，或者目标类型为Class类，类名为类型字符串值的值
										className = typedValue.getValue();
									}
								}
								if (className != null) {
									//如果类名不为空
									try {
										//调用反射获取类名对应的类型，并返回该类型。
										return ClassUtils.forName(className, classLoader);
									} catch (ClassNotFoundException ex) {
										throw new IllegalStateException("Could not resolve class name [" + arg + "] for factory method argument", ex);
									}
								}
								// 返回方法的返回类型
								return method.getReturnType();
							}
						}
					}
				}
			}
		}

		// 返回方法的返回类型
		return method.getReturnType();
	}


	/**
	 * Reflective {@link InvocationHandler} for lazy access to the current target object.
	 */
	@SuppressWarnings("serial")
	private static class ObjectFactoryDelegatingInvocationHandler implements InvocationHandler, Serializable {

		private final ObjectFactory<?> objectFactory;

		ObjectFactoryDelegatingInvocationHandler(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of proxy.
					return System.identityHashCode(proxy);
				case "toString":
					return this.objectFactory.toString();
			}
			try {
				return method.invoke(this.objectFactory.getObject(), args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
