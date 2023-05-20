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

package org.springframework.beans;

import java.beans.ConstructorProperties;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.KCallablesJvm;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Static convenience methods for JavaBeans: for instantiating beans,
 * checking bean property types, copying bean properties, etc.
 *
 * <p>Mainly for internal use within the framework, but to some degree also
 * useful for application classes. Consider
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons BeanUtils</a>,
 * <a href="https://hotelsdotcom.github.io/bull/">BULL - Bean Utils Light Library</a>,
 * or similar third-party frameworks for more comprehensive bean utilities.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public abstract class BeanUtils {

	/**
	 * 参数名发现器
	 */
	private static final ParameterNameDiscoverer parameterNameDiscoverer =
			new DefaultParameterNameDiscoverer();
	/**
	 * 未知的编辑器类型集合
	 */
	private static final Set<Class<?>> unknownEditorTypes =
			Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

	/**
	 * 基本类型默认值Map
	 */
	private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

	static {
		//设置基本类型的默认值
		Map<Class<?>, Object> values = new HashMap<>();
		values.put(boolean.class, false);
		values.put(byte.class, (byte) 0);
		values.put(short.class, (short) 0);
		values.put(int.class, 0);
		values.put(long.class, 0L);
		values.put(float.class, 0F);
		values.put(double.class, 0D);
		values.put(char.class, '\0');
		DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
	}


	/**
	 * 使用其无参构造函数实例化类的方便方法。
	 *
	 * @param clazz 要实例化的类
	 * @return 新的实例
	 * @throws BeanInstantiationException 如果bean无法实例化
	 * @see Class#newInstance()
	 * @deprecated as of Spring 5.0, following the deprecation of
	 * {@link Class#newInstance()} in JDK 9
	 */
	@Deprecated
	public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			return clazz.newInstance();
		} catch (InstantiationException ex) {
			throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
		} catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
		}
	}

	/**
	 * 使用其 “主要” 构造函数 (对于Kotlin类，可能声明了默认参数)
	 * 或其默认构造函数 (对于常规Java类，期望标准的no-arg设置) 实例化类。
	 * <p> 请注意，如果给定不可访问 (即非公共) 构造函数，此方法将尝试设置可访问的构造函数。
	 *
	 * @param clazz 要实例化的类
	 * @return 新的示例
	 * @throws BeanInstantiationException 如果无法实例化bean。如果找不到主要或默认的构造函数，则原因可能明显表明 {@link NoSuchMethodException}，
	 *                                    如果无法解析类定义 (例如，由于运行时缺少依赖关系)，则原因可能是 {@link NoClassDefFoundError}
	 *                                    或其他 {@link LinkageError}，或者从构造函数调用本身引发的异常。
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			//如果当前类是接口，则抛出BeanInstantiationException
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			//按照声明的构造函数实例化这个类
			return instantiateClass(clazz.getDeclaredConstructor());
		} catch (NoSuchMethodException ex) {
			//找到主要的构造函数
			Constructor<T> ctor = findPrimaryConstructor(clazz);
			if (ctor != null) {
				//如果该构造函数不为空，则按照主要的构造函数实例化这个类
				return instantiateClass(ctor);
			}
			//否则抛出BeanInstantiationException
			throw new BeanInstantiationException(clazz, "No default constructor found", ex);
		} catch (LinkageError err) {
			throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
		}
	}

	/**
	 * 使用其无参构造函数实例化一个类，并将新实例作为指定的可分配类型返回。
	 * <p> 在要实例化的类的类型 (clazz) 不可用，但所需的类型 (可以指定的) 已知的情况下很有用。
	 * <p> 请注意，如果给定不可访问 (即非公共) 构造函数，此方法将尝试设置可访问的构造函数。
	 *
	 * @param clazz        要实例化的类
	 * @param assignableTo 类可以指定的类型
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Constructor#newInstance
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
		Assert.isAssignable(assignableTo, clazz);
		return (T) instantiateClass(clazz);
	}

	/**
	 * 使用给定构造函数实例化类的便捷方法。
	 * <p> 请注意，如果给定一个不可访问 (即非公共) 的构造函数，则此方法尝试设置可访问的构造函数，并支持具有可选参数和默认值的Kotlin类。
	 *
	 * @param ctor 要实例化的构造函数
	 * @param args 要应用的构造函数参数 (对于未指定的参数使用 {@code null}，支持Kotlin可选参数和Java原始类型)
	 * @return 新的实例
	 * @throws BeanInstantiationException 如果无法实例化bean
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
		Assert.notNull(ctor, "Constructor must not be null");
		try {
			//将构造函数设置为可以访问的。
			ReflectionUtils.makeAccessible(ctor);
			if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
				//如果存在Kotlin反射且该类是Kotlin类，则使用Kotlin实例化
				return KotlinDelegate.instantiateClass(ctor, args);
			} else {
				//获取构造函数的各项参数类型
				Class<?>[] parameterTypes = ctor.getParameterTypes();
				Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
				Object[] argsWithDefaultValues = new Object[args.length];
				for (int i = 0; i < args.length; i++) {
					if (args[i] == null) {
						//参数值为空，如果是基础类，则从DEFAULT_TYPE_VALUES中获取默认值，否则参数值设为null
						Class<?> parameterType = parameterTypes[i];
						argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
					} else {
						//否则设置对应的参数值
						argsWithDefaultValues[i] = args[i];
					}
				}
				//调用构造函数反射实例化一个对象
				return ctor.newInstance(argsWithDefaultValues);
			}
		} catch (InstantiationException ex) {
			throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
		} catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
		} catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
		} catch (InvocationTargetException ex) {
			throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
		}
	}

	/**
	 * 为提供的类返回一个可解析的构造函数，可以是带有参数的主或单个公共构造函数，也可以是带有参数的单个非公共构造函数，
	 * 或者只是一个默认构造函数。调用者必须准备好解析返回的构造函数参数 (如果有)。
	 *
	 * @param clazz 要检查的类
	 * @throws IllegalStateException 如果根本找不到唯一的构造函数
	 * @see #findPrimaryConstructor
	 * @since 5.3
	 */
	@SuppressWarnings("unchecked")
	public static <T> Constructor<T> getResolvableConstructor(Class<T> clazz) {
		//找到主要的构造函数
		Constructor<T> ctor = findPrimaryConstructor(clazz);
		if (ctor != null) {
			//如果不为空，则直接返回该构造函数
			return ctor;
		}
		//获取该类型的构造函数
		Constructor<?>[] ctors = clazz.getConstructors();
		if (ctors.length == 1) {
			//如果仅有一个构造函数
			return (Constructor<T>) ctors[0];
		} else if (ctors.length == 0) {
			//如果没有构造函数，获取声明的构造函数，public类型的构造函数
			ctors = clazz.getDeclaredConstructors();
			if (ctors.length == 1) {
				//单个非公共构造函数，例如来自非公共记录类型
				return (Constructor<T>) ctors[0];
			}
		}

		// 有好几种构造函数，获取公共的构造函数
		try {
			return clazz.getDeclaredConstructor();
		} catch (NoSuchMethodException ex) {
			// 没有公共的构造函数，放弃
		}

		// 根本没有唯一的构造函数，抛出IllegalStateException
		throw new IllegalStateException("No primary or single unique constructor found for " + clazz);
	}

	/**
	 * 返回提供的类的主构造函数。对于Kotlin类，这将返回与Kotlin主构造函数相对应的Java构造函数 (如Kotlin规范中定义的)。
	 * 否则，特别是对于非Kotlin类，这仅返回 {@code null}。
	 *
	 * @param clazz 要检查的类
	 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
	 * @since 5.0
	 */
	@Nullable
	public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		//如果存在Kotlin反射且该类是Kotlin类，则委托KotlinDelegate找到主要的构造函数
		if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
			return KotlinDelegate.findPrimaryConstructor(clazz);
		}
		return null;
	}

	/**
	 * 查找具有给定方法名称和给定参数类型的方法，该方法在给定类或其超类之一上声明。
	 * 更倾向于公共方法，但也会返回受保护的、包访问或私有方法。
	 * <p> 首先检查 {@code Class.getMethod}，然后回退到 {@code findDeclaredMethod}。
	 * 即使在Java安全设置受限的环境中，这也允许查找没有问题的公共方法。
	 *
	 * @param clazz      要检查的类
	 * @param methodName 要找的方法名称
	 * @param paramTypes 要找的方法参数类型
	 * @return Method对象，如果没有找到返回{@code null}
	 * @see Class#getMethod
	 * @see #findDeclaredMethod
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		} catch (NoSuchMethodException ex) {
			return findDeclaredMethod(clazz, methodName, paramTypes);
		}
	}

	/**
	 * 查找具有给定方法名称和给定参数类型的方法，该方法在给定类或其超类之一上声明。
	 * 将返回一个公共的、受保护的、包访问或私有方法。
	 * <p> 检查 {@code Class.getDeclaredMethod}，向上级联到所有超类。
	 *
	 * @param clazz      要检查的类
	 * @param methodName 要找的方法名称
	 * @param paramTypes 要找的方法参数类型
	 * @return Method对象，如果没有找到返回{@code null}
	 * @see Class#getDeclaredMethod
	 */
	@Nullable
	public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			//根据方法名和参数类型获取公共的方法
			return clazz.getDeclaredMethod(methodName, paramTypes);
		} catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				//如果父类不为空，则查找父类的公共方法
				return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
			}
			return null;
		}
	}

	/**
	 * 查找在给定类或其超类之一上声明的具有给定方法名称和最小参数 (最佳情况: 无) 的方法。
	 * 更倾向于公共方法，但也会返回受保护的、包访问或私有方法。
	 * <p> 首先检查 {@code Class.getMethods}，然后回退到 {@code findDeclaredMethodWithMinimalParameters}。
	 * 即使在Java安全设置受限的环境中，这也允许查找没有问题的公共方法。
	 *
	 * @param clazz      要检查的类
	 * @param methodName 要找的方法名称
	 * @return Method对象，如果没有找到返回{@code null}
	 * @throws IllegalArgumentException 如果找到了给定名称的方法，但无法将其解析为具有最小参数的唯一方法
	 * @see Class#getMethods
	 * @see #findDeclaredMethodWithMinimalParameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {
		//根据方法名和方法类型获取最小参数的方法
		Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
		if (targetMethod == null) {
			//如果目标方法为空，寻找最小参数的公共方法
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
		}
		return targetMethod;
	}

	/**
	 * 查找在给定类或其超类之一上声明的具有给定方法名称和最小参数 (最佳情况: 无) 的方法。
	 * 将返回一个公共的、受保护的、包访问或私有方法。
	 * <p> 检查 {@code Class.getDeclaredMethods}，向上级联到所有超类。
	 *
	 * @param clazz      要检查的类
	 * @param methodName 要找的方法名称
	 * @return Method对象，如果没有找到返回{@code null}
	 * @throws IllegalArgumentException 如果找到了给定名称的方法，但无法将其解析为具有最小参数的唯一方法
	 * @see Class#getDeclaredMethods
	 */
	@Nullable
	public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
		if (targetMethod == null && clazz.getSuperclass() != null) {
			//如果目标方法为空且存在父类，查询父类的最小参数的公共方法
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
		}
		return targetMethod;
	}

	/**
	 * 在给定的方法列表中找到具有给定方法名称和最小参数 (最佳情况: 无) 的方法。
	 *
	 * @param methods    要检查的多个方法
	 * @param methodName 方法名
	 * @return Method对象，如果没有找到返回{@code null}
	 * @throws IllegalArgumentException 如果找到了给定名称的方法，但无法将其解析为具有最小参数的唯一方法
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = null;
		//已发现的当前最小参数方法个数。如：某一方法的参数个数都相同，这种情况需要区分出来。
		int numMethodsFoundWithCurrentMinimumArgs = 0;
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				//获取方法的参数个数
				int numParams = method.getParameterCount();
				if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
					//目标方法为空，或者小于上一个方法的参数个数，将目标方法设置为当前方法
					targetMethod = method;
					numMethodsFoundWithCurrentMinimumArgs = 1;
				} else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
					//
					//如果方法不是桥接方法（泛型参数实现方法），如：父类的参数类型为T，当前类的实现方法为String，这就是桥接方法。
					// 且当前方法的个数与上一个方法的个数相同
					if (targetMethod.isBridge()) {
						//更倾向于常规方法，而不是桥接方法。
						//将目标方法设置为当前方法
						targetMethod = method;
					} else {
						// 相同长度的附加候选者
						numMethodsFoundWithCurrentMinimumArgs++;
					}
				}
			}
		}
		if (numMethodsFoundWithCurrentMinimumArgs > 1) {
			//有多个长度相同的方法，抛出异常
			throw new IllegalArgumentException("Cannot resolve method '" + methodName +
					"' to a unique method. Attempted to resolve to overloaded method with " +
					"the least number of parameters but there were " +
					numMethodsFoundWithCurrentMinimumArgs + " candidates.");
		}
		return targetMethod;
	}

	/**
	 * 以 {@code methodName[([arg_list])]} 的形式解析方法签名，其中 {@code arg_list} 是一个可选的、逗号分隔的
	 * 完全限定类型名称列表，并尝试根据提供的 {@code Class} 解析该签名。
	 * <p> 不提供参数列表 ({@code methodName}) 时，将返回名称匹配且参数数量最少的方法。
	 * 提供参数类型列表时，将仅返回名称和参数类型匹配的方法。
	 * <p> 请注意，{@code methodName} 和 {@code methodName()} 不能以相同的方式解析 。
	 * 签名 {@code methodName} 表示参数数量最少的名为 {@code methodName} 的方法，
	 * 而 {@code methodName()} 表示参数数量恰好为0的名为 {@code methodName} 的方法。
	 * <p> 如果找不到方法，则返回 {@code null}。
	 *
	 * @param signature 作为字符串表示形式的方法签名
	 * @param clazz     用于解析方法签名的类
	 * @return 解析好的Method
	 * @see #findMethod
	 * @see #findMethodWithMinimalParameters
	 */
	@Nullable
	public static Method resolveSignature(String signature, Class<?> clazz) {
		Assert.hasText(signature, "'signature' must not be empty");
		Assert.notNull(clazz, "Class must not be null");
		int startParen = signature.indexOf('(');
		int endParen = signature.indexOf(')');
		if (startParen > -1 && endParen == -1) {
			//如果有(， 却没有)抛出异常。
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected closing ')' for args list");
		} else if (startParen == -1 && endParen > -1) {
			//如果有)， 却没有(抛出异常。
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected opening '(' for args list");
		} else if (startParen == -1) {
			//没有(，却有）找到最小参数的方法
			return findMethodWithMinimalParameters(clazz, signature);
		} else {
			//()都有，获取方法名
			String methodName = signature.substring(0, startParen);
			//获取参数类型名称
			String[] parameterTypeNames =
					StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for (int i = 0; i < parameterTypeNames.length; i++) {
				String parameterTypeName = parameterTypeNames[i].trim();
				try {
					//通过反射获取该类型名称的Class
					parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
				} catch (Throwable ex) {
					throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
							parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
				}
			}
			//根据类、方法名、参数类型解析该方法
			return findMethod(clazz, methodName, parameterTypes);
		}
	}


	/**
	 * 检索给定类的JavaBeans {@code PropertyDescriptor}。
	 *
	 * @param clazz 要检索的属性描述符的类
	 * @return 给定类的 {@code PropertyDescriptors} 数组
	 * @throws BeansException 如果属性描述符查找失败
	 */
	public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
		return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptors();
	}

	/**
	 * 检索给定属性的JavaBeans {@code PropertyDescriptors}。
	 *
	 * @param clazz        要检索的PropertyDescriptor的类
	 * @param propertyName 属性名
	 * @return 相应的PropertyDescriptor，如果没有，则为 {@code null}
	 * @throws BeansException 如果属性描述符查找失败
	 */
	@Nullable
	public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {
		return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptor(propertyName);
	}

	/**
	 * 查找给定方法的JavaBeans {@code PropertyDescriptor}，该方法是该bean属性的读取方法或写入方法。
	 *
	 * @param method 查找对应的PropertyDescriptor的方法，内省其声明类
	 * @return 相应的PropertyDescriptor，如果没有，则为 {@code null}
	 * @throws BeansException 如果属性描述符查找失败
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
		return findPropertyForMethod(method, method.getDeclaringClass());
	}

	/**
	 * 查找给定方法的JavaBeans {@code PropertyDescriptor}，该方法是该bean属性的读取方法或写入方法。
	 *
	 * @param method 查找对应的PropertyDescriptor的方法
	 * @param clazz  要对描述符进行内省的（子）类
	 * @return 相应的PropertyDescriptor，如果没有，则为 {@code null}
	 * @throws BeansException 如果属性描述符查找失败
	 * @since 3.2.13
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
		Assert.notNull(method, "Method must not be null");
		//获取属性描述符数组
		PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
		for (PropertyDescriptor pd : pds) {
			if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
				//如果该方法是读方法或者是写方法，则返回该属性描述符
				return pd;
			}
		}
		return null;
	}

	/**
	 * 按照 'Editor' 后缀约定查找JavaBeans PropertyEditor (例如 "mypackage.MyDomainClass" &rarr; "mypackage.MyDomainClassEditor")。
	 * <p> 与 {@link java.beans.PropertyEditorManager} 实现的标准JavaBeans约定兼容，但与后者的原始类型的注册默认编辑器隔离。
	 *
	 * @param targetType 查找编辑器的类型
	 * @return 相应的编辑器，如果没有找到，则为 {@code null}
	 */
	@Nullable
	public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
		if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
			//如果该类型为空，或者该类型是一个数组或者是未知的编辑器类型，返回null
			return null;
		}
		//获取类加载器
		ClassLoader cl = targetType.getClassLoader();
		if (cl == null) {
			try {
				//类加载器为空，获取系统的类加载器
				cl = ClassLoader.getSystemClassLoader();
				if (cl == null) {
					//系统类加载器为空，返回null
					return null;
				}
			} catch (Throwable ex) {
				// 例如： 谷歌应用引擎上的AccessControlException
				return null;
			}
		}
		//获取类名
		String targetTypeName = targetType.getName();
		///编辑器名为类名+Editor
		String editorName = targetTypeName + "Editor";
		try {
			//加载这个编辑器类名
			Class<?> editorClass = cl.loadClass(editorName);
			if (editorClass != null) {
				if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
					//如果该类不是PropertyEditor的子类，将其添加到未知编辑器类型集合中。
					unknownEditorTypes.add(targetType);
					return null;
				}
				//否则实例化该编辑器
				return (PropertyEditor) instantiateClass(editorClass);
			}
			//错误行为，类加载器返回null；而不是ClassNotFoundException
			// 回到下面未知的编辑器类型注册
		} catch (ClassNotFoundException ex) {
			// 忽略-回退到下面未知的编辑器类型注册
		}
		unknownEditorTypes.add(targetType);
		return null;
	}

	/**
	 * 如果可能，从给定的类或接口中确定给定属性的bean属性类型。
	 *
	 * @param propertyName bean属性的名称
	 * @param beanClasses  要检查的类
	 * @return 属性类型，, 否则回退到 {@code Object.class}
	 */
	public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
		if (beanClasses == null) {
			//类为空，返回Object类
			return Object.class;
		}
		for (Class<?> beanClass : beanClasses) {
			PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
			if (pd != null) {
				//属性描述符不为空，获取它对应的属性类型
				return pd.getPropertyType();
			}
		}
		//如果没有找到，返回Object类
		return Object.class;
	}

	/**
	 * 为指定属性的write方法获取新的MethodParameter对象。
	 *
	 * @param pd 属性的属性描述符
	 * @return 对应的MethodParameter对象
	 */
	public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
		if (pd instanceof GenericTypeAwarePropertyDescriptor) {
			//如果属性描述符是泛型类型感知属性描述符，强转，并获取可写的方法参数
			return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
		} else {
			//获取可写方法
			Method writeMethod = pd.getWriteMethod();
			Assert.state(writeMethod != null, "No write method available");
			//返回第一个方法参数
			return new MethodParameter(writeMethod, 0);
		}
	}

	/**
	 * 基于JavaBeans {@link ConstructorProperties} 注释以及
	 * Spring的 {@link DefaultParameterNameDiscoverer}，确定给定构造函数所需的参数名称。
	 *
	 * @param ctor 用于查找参数名称的构造函数
	 * @return 参数名称 (匹配构造函数的参数个数)
	 * @throws IllegalStateException 如果参数名称不可解析
	 * @see ConstructorProperties
	 * @see DefaultParameterNameDiscoverer
	 * @since 5.3
	 */
	public static String[] getParameterNames(Constructor<?> ctor) {
		//通过ConstructorProperties注解标记餐宿名称获取参数值
		ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
		//如果获取不到，通过parameterNameDiscoverer获取参数名称
		String[] paramNames = (cp == null ? parameterNameDiscoverer.getParameterNames(ctor) : cp.value());
		Assert.state(paramNames != null, () -> "Cannot resolve parameter names for constructor " + ctor);
		Assert.state(paramNames.length == ctor.getParameterCount(),
				() -> "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor);
		return paramNames;
	}

	/**
	 * 检查给定的类型是否表示 “简单” 属性: 简单值类型或简单值类型数组。
	 * <p> 有关 <em> 简单值类型 <em> 的定义，请参见 {@link #isSimpleValueType(Class)}。
	 * <p> 用于确定属性以检查 “简单” 依赖项检查。
	 *
	 * @param type 要检查的类型
	 * @return 给定类型是否是 “简单” 属性
	 * @see org.springframework.beans.factory.support.RootBeanDefinition#DEPENDENCY_CHECK_SIMPLE
	 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#checkDependencies
	 * @see #isSimpleValueType(Class)
	 */
	public static boolean isSimpleProperty(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		//如果该类是简单类，返回true。
		//如果该类是数组，且数组内的元素是简单类型，返回true。
		return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
	}

	/**
	 * 检查给定类型是否表示 “简单” 值类型: 原始或原始包装，枚举，字符串或其他字符序列，数字，日期，时间，URI，URL，区域设置，或者一个类。
	 * <p >{@code Void} 和 {@code void} 不被认为是简单的值类型。
	 *
	 * @param type the type to check
	 * @return whether the given type represents a "simple" value type
	 * @see #isSimpleProperty(Class)
	 */
	public static boolean isSimpleValueType(Class<?> type) {
		boolean isNotVoid = Void.class != type && void.class != type;
		return (isNotVoid &&
				isSimpleType(type));
	}

	private static boolean isSimpleType(Class<?> type) {
		//如果是原始类型及其包装类
		boolean primitiveOrWrapper = ClassUtils.isPrimitiveOrWrapper(type);
		//枚举类
		boolean isEnum = Enum.class.isAssignableFrom(type);
		//字符系列类型
		boolean isCharSequence = CharSequence.class.isAssignableFrom(type);
		//数字类型
		boolean isNum = Number.class.isAssignableFrom(type);
		//日期类型
		boolean isDate = Date.class.isAssignableFrom(type);
		//时区类型
		boolean isTemporal = Temporal.class.isAssignableFrom(type);
		//URI 或URL
		boolean uriOrUrl = URI.class == type || URL.class == type;
		//本地化类型
		boolean isLocale = Locale.class == type;
		//Class类型
		boolean isClass = Class.class == type;
		return primitiveOrWrapper ||
				isEnum ||
				isCharSequence ||
				isNum ||
				isDate ||
				isTemporal ||
				uriOrUrl ||
				isLocale ||
				isClass;
	}


	/**
	 * 将给定源bean的属性值复制到目标bean中。
	 * <p> 注意: 源类和目标类不必匹配甚至彼此派生，只要属性匹配即可。
	 * 源bean公开但目标bean不公开的任何bean属性都将被静默忽略。
	 * <p> 这只是一种方便的方法。对于更复杂的传输需求，请考虑使用完整的 {@link BeanWrapper}。
	 *
	 * <p>从Spring Framework 5.3开始，当匹配源和目标对象中的属性时，此方法会遵循泛型类型信息。
	 * <p>下表提供了可以复制的源和目标属性类型以及不能复制的源和目标属性类型的非详尽示例集。
	 * <table border="1">
	 * <tr><th>源属性类型</th><th>目标属性类型</th><th>支持复制</th></tr>
	 * <tr><td>{@code Integer}</td><td>{@code Integer}</td><td>yes</td></tr>
	 * <tr><td>{@code Integer}</td><td>{@code Number}</td><td>yes</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Integer>}</td><td>yes</td></tr>
	 * <tr><td>{@code List<?>}</td><td>{@code List<?>}</td><td>yes</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<?>}</td><td>yes</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<? extends Number>}</td><td>yes</td></tr>
	 * <tr><td>{@code String}</td><td>{@code Integer}</td><td>no</td></tr>
	 * <tr><td>{@code Number}</td><td>{@code Integer}</td><td>no</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Long>}</td><td>no</td></tr>
	 * <tr><td>{@code List<Integer>}</td><td>{@code List<Number>}</td><td>no</td></tr>
	 * </table>
	 *
	 * @param source 源 bean
	 * @param target 目标 bean
	 * @throws BeansException 如果复制失败
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target) throws BeansException {
		copyProperties(source, target, null, (String[]) null);
	}

	/**
	 * 将给定源bean的属性值复制到给定的目标bean中，仅设置在给定的 “可编辑” 类 (或接口) 中定义的属性。
	 * <p>注意: 源类和目标类不必匹配甚至彼此派生，只要属性匹配即可。源bean公开但目标bean不公开的任何bean属性都将被静默忽略。
	 * <p>这只是一种方便的方法。对于更复杂的传输需求，请考虑使用完整的 {@link BeanWrapper}。
	 *
	 *
	 * <p>从Spring Framework 5.3开始，当匹配源和目标对象中的属性时，此方法会遵循泛型类型信息。
	 * 有关详细信息，请参见 {@link #copyProperties(Object, Object)} 的文档。
	 *
	 * @param source   源 bean
	 * @param target   目标 bean
	 * @param editable 要限制属性设置的类 (或接口)
	 * @throws BeansException 如果复制失败
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
		copyProperties(source, target, editable, (String[]) null);
	}

	/**
	 * 将给定源bean的属性值复制到给定的目标bean中，忽略给定的 “ignoreProperties”。
	 * <p>注意: 源类和目标类不必匹配，甚至不必彼此派生，只要属性匹配即可。
	 * 源bean公开但目标bean不公开的任何bean属性都将被静默忽略。
	 * <p>这只是一种方便的方法。对于更复杂的转移需求，请考虑使用完整的 {@link BeanWrapper}.
	 * <p>从Spring Framework 5.3开始，当匹配源和目标对象中的属性时，此方法会遵循泛型类型信息。
	 * 有关详细信息，请参见 {@link #copyProperties(Object, Object)} 的文档。
	 *
	 * @param source           源 bean
	 * @param target           目标 bean
	 * @param ignoreProperties 要忽略的属性名称数组
	 * @throws BeansException 如果复制失败
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
		copyProperties(source, target, null, ignoreProperties);
	}

	/**
	 * 将给定源bean的属性值复制到给定的目标bean中。
	 * <p> 注意: 源类和目标类不必匹配甚至彼此派生，只要属性匹配即可。
	 * 源bean公开但目标bean不公开的任何bean属性都将被静默忽略。
	 * <p> 从Spring Framework 5.3开始，此方法在匹配源和目标对象中的属性时会遵循泛型类型信息。
	 * 有关详细信息，请参见 {@link #copyProperties(Object, Object)}  的文档。
	 *
	 * @param source           源 bean
	 * @param target           目标 bean
	 * @param editable         要限制属性设置的类 (或接口)
	 * @param ignoreProperties 要忽略的属性名称数组
	 * @throws BeansException 如果复制失败
	 * @see BeanWrapper
	 */
	private static void copyProperties(Object source, Object target, @Nullable Class<?> editable,
									   @Nullable String... ignoreProperties) throws BeansException {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(target, "Target must not be null");

		Class<?> actualEditable = target.getClass();
		if (editable != null) {
			if (!editable.isInstance(target)) {
				//如果目标类不是可编辑类的实例，抛出IllegalArgumentException
				throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
						"] not assignable to Editable class [" + editable.getName() + "]");
			}
			actualEditable = editable;
		}
		//通过该类获取属性描述数组
		PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
		//要忽略的属性名称列表
		List<String> ignoreList = (ignoreProperties == null ? null : Arrays.asList(ignoreProperties));

		for (PropertyDescriptor targetPd : targetPds) {
			//获取可写方法
			Method writeMethod = targetPd.getWriteMethod();
			if (writeMethod == null || (ignoreList != null && ignoreList.contains(targetPd.getName()))) {
				//如果该属性对应的方法，不是可写方法，或者忽略属性名列表不为空，且刚好这个属性是忽略属性时，跳过。
				continue;
			}
			//根据源类和目标方法名获取源属性的属性描述
			PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
			if (sourcePd == null) {
				//如果源字段属性描述为空，跳过
				continue;
			}
			Method readMethod = sourcePd.getReadMethod();
			if (readMethod == null) {
				//如果源字段对应的方法不是可读方法，跳过
				continue;
			}
			//获取源字段和目标字段对应的方法返回类型信息
			ResolvableType sourceResolvableType = ResolvableType.forMethodReturnType(readMethod);
			ResolvableType targetResolvableType = ResolvableType.forMethodParameter(writeMethod, 0);

			// 如果任一可解析类型具有不可解析的泛型，请在可分配检查中忽略泛型。
			boolean isAssignable;
			if (sourceResolvableType.hasUnresolvableGenerics() || targetResolvableType.hasUnresolvableGenerics()) {
				//如果源字段和目标字段的返回类型含有无法解析的泛型，通过ClassUtils检查可写方法的第一个参数是否可以赋值给可读方法的返回类型。
				isAssignable = ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType());
			} else {
				//否则通过目标字段返回信息检查是否可以将该属性赋值到目标对象中
				isAssignable = targetResolvableType.isAssignableFrom(sourceResolvableType);
			}

			if (isAssignable) {
				try {
					if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
						//如果可读方法的不是public权限，设置为可访问权限。
						readMethod.setAccessible(true);
					}
					//通过反射调用可读方法，获取源对象值
					Object value = readMethod.invoke(source);
					if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
						//如果可写方法不是public权限，设置为可访问权限。
						writeMethod.setAccessible(true);
					}
					//通过反射方法，调用可写方法，值为源对象值，如setter方法
					writeMethod.invoke(target, value);
				} catch (Throwable ex) {
					throw new FatalBeanException(
							"Could not copy property '" + targetPd.getName() + "' from source to target", ex);
				}
			}
		}
	}


	/**
	 * 内部类，以避免在运行时对Kotlin的硬依赖。
	 */
	private static class KotlinDelegate {

		/**
		 * 检索与Kotlin主构造函数对应的Java构造函数 (如果有)。
		 *
		 * @param clazz Kotlin类的 {@link Class}
		 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">
		 * https://kotlinlang.org/docs/reference/classes.html#constructors</a>
		 */
		@Nullable
		public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
			try {
				//调用KClasses获取主要的构造函数
				KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
				if (primaryCtor == null) {
					//如果该构造函数为空，则返回null
					return null;
				}
				//获取主构造函数对应的Java构造函数
				Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
				if (constructor == null) {
					//如果没有则抛出IllegalStateException异常
					throw new IllegalStateException(
							"Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
				}
				return constructor;
			} catch (UnsupportedOperationException ex) {
				return null;
			}
		}

		/**
		 * 使用提供的构造函数实例化Kotlin类。
		 *
		 * @param ctor 要实例化的Kotlin类的构造函数
		 * @param args 要应用的构造函数参数 (如果需要，请为未指定的参数使用 {@code null})
		 */
		public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
				throws IllegalAccessException, InvocationTargetException, InstantiationException {
			//通过JVM反射获取kotlin构造函数
			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
			if (kotlinConstructor == null) {
				//如果构造函数为空，则使用Java反射实例化该构造函数
				return ctor.newInstance(args);
			}

			if ((!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))) {
				//如果构造函数的修饰符不是public 或者构造函数的声明类不是public，将其设置为可进入的。
				KCallablesJvm.setAccessible(kotlinConstructor, true);
			}
			//获取kotlin构造函数参数
			List<KParameter> parameters = kotlinConstructor.getParameters();

			Map<KParameter, Object> argParameters = CollectionUtils.newHashMap(parameters.size());
			Assert.isTrue(args.length <= parameters.size(),
					"Number of provided arguments should be less of equals than number of constructor parameters");
			for (int i = 0; i < args.length; i++) {
				KParameter parameter = parameters.get(i);
				if (!parameter.isOptional() || args[i] != null) {
					//如果参数不是可选的，或者参数值不为空，将其放入到HashMap中。
					argParameters.put(parameter, args[i]);
				}
			}
			//调用kotlin的callBy方法实例化这个类
			return kotlinConstructor.callBy(argParameters);
		}

	}

}
