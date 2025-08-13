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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用于操作反射 API 及处理反射异常的简单工具类。
 *
 * <p>仅供内部使用。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Costin Leau
 * @author Sam Brannen
 * @author Chris Beams
 * @since 1.2.2
 */
public abstract class ReflectionUtils {

	/**
	 * 预构建的 {@link MethodFilter}，匹配所有未在 {@code java.lang.Object} 上声明的非桥非合成方法。
	 *
	 * @since 3.0.5
	 */
	public static final MethodFilter USER_DECLARED_METHODS =
			(method -> !method.isBridge() && !method.isSynthetic() && (method.getDeclaringClass() != Object.class));

	/**
	 * 预定义的 FieldFilter，用于匹配所有非静态且非最终的字段。
	 */
	public static final FieldFilter COPYABLE_FIELDS =
			(field -> !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())));


	/**
	 * CGLIB重命名方法的命名前缀。
	 *
	 * @see #isCglibRenamedMethod
	 */
	private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

	private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];


	/**
	 * {@link Class#getDeclaredMethods()} 的缓存加上来自基于Java 8的接口的等效默认方法，允许快速迭代。
	 */
	private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256);

	/**
	 * {@link Class#getDeclaredFields()} 的缓存，允许快速迭代。
	 */
	private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256);


	// 异常处理

	/**
	 * 处理给定的反射异常。
	 * <p>仅应在目标方法不期望抛出已检查异常或访问方法或字段时出现错误的情况下调用。
	 * <p>如果是带有根本原因的 InvocationTargetException，则抛出底层的 RuntimeException 或 Error。
	 * 否则抛出带有适当消息的 IllegalStateException 或 UndeclaredThrowableException。
	 *
	 * @param ex 要处理的反射异常
	 */
	public static void handleReflectionException(Exception ex) {
		if (ex instanceof NoSuchMethodException) {
			throw new IllegalStateException("Method not found: " + ex.getMessage());
		}
		if (ex instanceof IllegalAccessException) {
			throw new IllegalStateException("Could not access method or field: " + ex.getMessage());
		}
		if (ex instanceof InvocationTargetException) {
			handleInvocationTargetException((InvocationTargetException) ex);
		}
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 处理给定的调用目标异常。仅应在目标方法不期望抛出已检查异常时调用。
	 * <p>如果有根本原因，则抛出底层的 RuntimeException 或 Error。
	 * 否则抛出 UndeclaredThrowableException。
	 *
	 * @param ex 要处理的调用目标异常
	 */
	public static void handleInvocationTargetException(InvocationTargetException ex) {
		rethrowRuntimeException(ex.getTargetException());
	}

	/**
	 * 重新抛出给定的 {@link Throwable 异常}，该异常可能是 {@link InvocationTargetException} 的
	 * <em>目标异常</em>。应该只在目标方法预期不会抛出受检异常时调用。
	 * <p>如果合适，将底层异常转换为 {@link RuntimeException} 或 {@link Error} 重新抛出；
	 * 否则，抛出 {@link UndeclaredThrowableException}。
	 *
	 * @param ex 要重新抛出的异常
	 * @throws RuntimeException 重新抛出的异常
	 */
	public static void rethrowRuntimeException(Throwable ex) {
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 重新抛出给定的 {@link Throwable 异常}，该异常可能是 {@link InvocationTargetException} 的
	 * <em>目标异常</em>。应该只在目标方法预期不会抛出受检异常时调用。
	 * <p>如果合适，将底层异常转换为 {@link Exception} 或 {@link Error} 重新抛出；
	 * 否则，抛出 {@link UndeclaredThrowableException}。
	 *
	 * @param ex 要重新抛出的异常
	 * @throws Exception 重新抛出的异常（在受检异常的情况下）
	 */
	public static void rethrowException(Throwable ex) throws Exception {
		if (ex instanceof Exception) {
			throw (Exception) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	// 构造函数处理

	/**
	 * 获取给定类和参数的可访问构造函数。
	 *
	 * @param clazz 要检查的类
	 * @param parameterTypes 所需构造函数的参数类型
	 * @return 构造函数引用
	 * @throws NoSuchMethodException 如果不存在这样的构造函数
	 * @since 5.0
	 */
	public static <T> Constructor<T> accessibleConstructor(Class<T> clazz, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes);
		makeAccessible(ctor);
		return ctor;
	}

	/**
	 * 使给定的构造函数可访问，如有必要，显式设置它可访问。{@code setaccessive (true)} 方法仅在实际必要时调用，
	 * 以避免与JVM SecurityManager (如果处于活动状态) 发生不必要的冲突。
	 *
	 * @param ctor 使可访问的构造函数
	 * @see java.lang.reflect.Constructor#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Constructor<?> ctor) {
		//如果该构造函数的修饰符不是public
		if ((!Modifier.isPublic(ctor.getModifiers()) ||
				//或者这个类的修饰符不是public并且该构造函数不可访问
				!Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) && !ctor.isAccessible()) {
			//将构造函数设置为可访问的
			ctor.setAccessible(true);
		}
	}


	// 方法处理
	/**
	 * 尝试在指定的类中查找具有指定名称且无参数的{@link Method}。
	 * 搜索所有父类直到{@code Object}。
	 * 如果找不到{@link Method}，则返回{@code null}。
	 *
	 * @param clazz 要内省的类
	 * @param name 方法的名称
	 * @return Method对象，如果未找到则返回{@code null}
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String name) {
		return findMethod(clazz, name, EMPTY_CLASS_ARRAY);
	}

	/**
	 * 尝试在指定的类中查找具有指定名称和参数类型的{@link Method}。
	 * 搜索所有父类直到{@code Object}。
	 * 如果找不到{@link Method}，则返回{@code null}。
	 *
	 * @param clazz 要内省的类
	 * @param name 方法的名称
	 * @param paramTypes 方法的参数类型（可以为{@code null}表示任何签名）
	 * @return Method对象，如果未找到则返回{@code null}
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String name, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(name, "Method name must not be null");
		Class<?> searchType = clazz;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() :
					getDeclaredMethods(searchType, false));
			for (Method method : methods) {
				if (name.equals(method.getName()) && (paramTypes == null || hasSameParams(method, paramTypes))) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
		return (paramTypes.length == method.getParameterCount() &&
				Arrays.equals(paramTypes, method.getParameterTypes()));
	}

	/**
	 * 对指定的目标对象调用指定的{@link Method}，不传递任何参数。
	 * 当调用静态{@link Method}时，目标对象可以为{@code null}。
	 * <p>抛出的异常通过调用{@link #handleReflectionException}来处理。
	 *
	 * @param method 要调用的方法
	 * @param target 要调用方法的目标对象
	 * @return 调用结果（如果有的话）
	 * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
	 */
	@Nullable
	public static Object invokeMethod(Method method, @Nullable Object target) {
		return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
	}

	/**
	 * 对指定的目标对象调用指定的{@link Method}，传递指定的参数。
	 * 当调用静态{@link Method}时，目标对象可以为{@code null}。
	 * <p>抛出的异常通过调用{@link #handleReflectionException}来处理。
	 *
	 * @param method 要调用的方法
	 * @param target 要调用方法的目标对象
	 * @param args 调用参数（可以为{@code null}）
	 * @return 调用结果（如果有的话）
	 */
	@Nullable
	public static Object invokeMethod(Method method, @Nullable Object target, @Nullable Object... args) {
		try {
			return method.invoke(target, args);
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * 确定给定的方法是否明确声明了给定的异常或其父类，
	 * 这意味着该类型的异常可以在反射调用中按原样传播。
	 *
	 * @param method 声明的方法
	 * @param exceptionType 要抛出的异常
	 * @return 如果异常可以按原样抛出则返回{@code true}；
	 *         如果需要包装则返回{@code false}
	 */
	public static boolean declaresException(Method method, Class<?> exceptionType) {
		Assert.notNull(method, "Method must not be null");
		Class<?>[] declaredExceptions = method.getExceptionTypes();
		for (Class<?> declaredException : declaredExceptions) {
			if (declaredException.isAssignableFrom(exceptionType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 对给定类中所有匹配的方法执行给定的回调操作，这些方法是本地声明的
	 * 或等效的方法（例如给定类实现的基于Java 8接口的默认方法）。
	 *
	 * @param clazz 要内省的类
	 * @param mc 为每个方法调用的回调
	 * @throws IllegalStateException 如果内省失败
	 * @see #doWithMethods
	 * @since 4.2
	 */
	public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
		Method[] methods = getDeclaredMethods(clazz, false);
		for (Method method : methods) {
			try {
				mc.doWith(method);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
	}

	/**
	 * 对给定类及其父类中所有匹配的方法执行给定的回调操作。
	 * <p>在子类和父类中出现的同名方法将出现两次，
	 * 除非被{@link MethodFilter}排除。
	 *
	 * @param clazz 要内省的类
	 * @param mc 为每个方法调用的回调
	 * @throws IllegalStateException 如果内省失败
	 * @see #doWithMethods(Class, MethodCallback, MethodFilter)
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
		doWithMethods(clazz, mc, null);
	}

	/**
	 * 对给定类和超类 (或给定接口和超接口) 的所有匹配方法执行给定的回调操作。
	 * <p> 发生在子类和超类上的相同命名方法将出现两次，除非被指定的 {@link MethodFilter} 排除。
	 *
	 * @param clazz 要内省的类
	 * @param mc    每个方法要调用的回调
	 * @param mf    确定要将回调应用于的方法的过滤器
	 * @throws IllegalStateException 如果内省失败
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc, @Nullable MethodFilter mf) {
		if (mf == USER_DECLARED_METHODS && clazz == Object.class) {
			// 如果方法过滤器为 用户声明方法过滤器，且当前的内省的类为Object类，直接结束。
			return;
		}
		//获取声明方法
		Method[] methods = getDeclaredMethods(clazz, false);
		for (Method method : methods) {
			if (mf != null && !mf.matches(method)) {
				//如果方法过滤器不为空，且该方法匹配方法过滤器的条件。跳过该方法。
				continue;
			}
			try {
				//调用回调方法
				mc.doWith(method);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
		// 继续备份继承层次结构。
		if (clazz.getSuperclass() != null && (mf != USER_DECLARED_METHODS || clazz.getSuperclass() != Object.class)) {
			//如果当前类有父类，且方法过滤器不是 用户声明方法过滤器 或者 当前类的父类不是Object类，递归调用doWithMethods处理父类
			doWithMethods(clazz.getSuperclass(), mc, mf);
		} else if (clazz.isInterface()) {
			//如果当前类是接口，获取当前接口的父接口，递归调用doWithMethods处理父接口
			for (Class<?> superIfc : clazz.getInterfaces()) {
				doWithMethods(superIfc, mc, mf);
			}
		}
	}

	/**
	 * 获取叶子类和所有父类上的所有声明方法。
	 * 叶子类的方法首先被包含。
	 *
	 * @param leafClass 要内省的类
	 * @throws IllegalStateException 如果内省失败
	 */
	public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, methods::add);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * 获取叶子类和所有父类上的唯一声明方法集合。
	 * 叶子类的方法首先被包含，在遍历父类层次结构时，
	 * 任何找到的与已包含方法签名匹配的方法都会被过滤掉。
	 *
	 * @param leafClass 要内省的类
	 * @throws IllegalStateException 如果内省失败
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
		return getUniqueDeclaredMethods(leafClass, null);
	}

	/**
	 * 获取内省类和所有超类上唯一的一组声明的方法。
	 * 首先包含内省类方法，并且在遍历超类层次结构时，将筛选出与已包含的方法相匹配的签名的任何方法。
	 *
	 * @param leafClass 要内省的类
	 * @param mf        确定要考虑方法的过滤器
	 * @throws IllegalStateException 如果反射失败
	 * @since 5.2
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass, @Nullable MethodFilter mf) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, method -> {
			//找到方法标志
			boolean knownSignature = false;
			//被协变返回类型覆盖的方法
			Method methodBeingOverriddenWithCovariantReturnType = null;
			for (Method existingMethod : methods) {
				if (method.getName().equals(existingMethod.getName()) &&
						method.getParameterCount() == existingMethod.getParameterCount() &&
						Arrays.equals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
					//如果方法名称相同，方法参数相同，且方法的所有的参数类型相同
					if (existingMethod.getReturnType() != method.getReturnType() &&
							existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
						//如果他们的返回类型不同，且他们是父子类，则将当前编译到方法设置为 被协变返回类型覆盖的方法
						methodBeingOverriddenWithCovariantReturnType = existingMethod;
					} else {
						//找到方法标志设置为true
						knownSignature = true;
					}
					break;
				}
			}
			if (methodBeingOverriddenWithCovariantReturnType != null) {
				//如果被协变返回类型覆盖的方法不为空，则移除该协变返回类型覆盖方法。
				methods.remove(methodBeingOverriddenWithCovariantReturnType);
			}
			if (!knownSignature && !isCglibRenamedMethod(method)) {
				//如果未找到该方法，且该方法不是CGLIB重命名的方法。方法列表添加当前方法。
				methods.add(method);
			}
		}, mf);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * {@link Class#getDeclaredMethods()} 的变体，它使用本地缓存来避免JVM的SecurityManager检查和新方法实例。
	 * 此外，它还包括来自本地实现的接口的Java 8默认方法，因为这些方法可以像声明的方法一样被有效地对待。
	 *
	 * @param clazz the 要内省的类
	 * @return 方法的缓存数组
	 * @throws IllegalStateException 如果反省失败
	 * @see Class#getDeclaredMethods()
	 * @since 5.2
	 */
	public static Method[] getDeclaredMethods(Class<?> clazz) {
		return getDeclaredMethods(clazz, true);
	}

	private static Method[] getDeclaredMethods(Class<?> clazz, boolean defensive) {
		Assert.notNull(clazz, "Class must not be null");
		//从缓存中获取该类的声明方法
		Method[] result = declaredMethodsCache.get(clazz);
		if (result == null) {
			try {
				//获取当前类的声明方法
				Method[] declaredMethods = clazz.getDeclaredMethods();
				//获取所有接口上继承的方法，作为默认方法
				List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
				if (defaultMethods != null) {
					result = new Method[declaredMethods.length + defaultMethods.size()];
					//将当前类的声明方法数组复制到result中。
					System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
					int index = declaredMethods.length;
					//添加接口继承的方法。
					for (Method defaultMethod : defaultMethods) {
						result[index] = defaultMethod;
						index++;
					}
				} else {
					//如果没有接口，则设置为当前类的声明方法
					result = declaredMethods;
				}
				//添加进缓存
				declaredMethodsCache.put(clazz, (result.length == 0 ? EMPTY_METHOD_ARRAY : result));
			} catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
						"] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
			}
		}
		return (result.length == 0 || !defensive) ? result : result.clone();
	}

	@Nullable
	private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
		List<Method> result = null;
		for (Class<?> ifc : clazz.getInterfaces()) {
			//遍历该类的接口
			for (Method ifcMethod : ifc.getMethods()) {
				//遍历该接口的方法
				if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
					//如果接口方法的修饰符没有Abstract，添加该接口方法
					if (result == null) {
						result = new ArrayList<>();
					}
					result.add(ifcMethod);
				}
			}
		}
		return result;
	}

	/**
	 * 确定给定的方法是否是 “equals” 方法。
	 *
	 * @see java.lang.Object#equals(Object)
	 */
	public static boolean isEqualsMethod(@Nullable Method method) {
		if (method == null) {
			//如果方法不存在，返回false
			return false;
		}
		if (method.getParameterCount() != 1) {
			//如果方法参数个数不为1，返回false
			return false;
		}
		if (!method.getName().equals("equals")) {
			//参数名不是equals，返回false
			return false;
		}
		//参数类型必须是Object
		return method.getParameterTypes()[0] == Object.class;
	}

	/**
	 * 确定给定的方法是否是"hashCode"方法。
	 *
	 * @see java.lang.Object#hashCode()
	 */
	public static boolean isHashCodeMethod(@Nullable Method method) {
		return method != null && method.getParameterCount() == 0 && method.getName().equals("hashCode");
	}

	/**
	 * 确定给定的方法是否是"toString"方法。
	 *
	 * @see java.lang.Object#toString()
	 */
	public static boolean isToStringMethod(@Nullable Method method) {
		return (method != null && method.getParameterCount() == 0 && method.getName().equals("toString"));
	}

	/**
	 * 确定给定的方法是否最初由{@link java.lang.Object}声明。
	 */
	public static boolean isObjectMethod(@Nullable Method method) {
		return (method != null && (method.getDeclaringClass() == Object.class ||
				isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method)));
	}

	/**
	 * 确定给定的方法是否是CGLIB “重命名” 方法，遵循模式 “CGLIB$methodName$0”。
	 *
	 * @param renamedMethod 要检查的方法
	 */
	public static boolean isCglibRenamedMethod(Method renamedMethod) {
		String name = renamedMethod.getName();
		if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
			int i = name.length() - 1;
			while (i >= 0 && Character.isDigit(name.charAt(i))) {
				//如果当前位置是数字字符，索引-1
				i--;
			}
			return (i > CGLIB_RENAMED_METHOD_PREFIX.length() && (i < name.length() - 1) && name.charAt(i) == '$');
		}
		return false;
	}

	/**
	 * 使给定的方法可访问，如有必要明确设置其可访问性。
	 * 只有在实际必要时才调用{@code setAccessible(true)}方法，
	 * 以避免与JVM SecurityManager（如果激活）发生不必要的冲突。
	 *
	 * @param method 要使其可访问的方法
	 * @see java.lang.reflect.Method#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) &&
				!method.isAccessible()) {
			method.setAccessible(true);
		}
	}

	// 字段处理

	/**
	 * 尝试在提供的{@link Class}上查找具有提供的{@code name}的{@link Field 字段}。
	 * 搜索所有父类直到{@link Object}。
	 *
	 * @param clazz 要内省的类
	 * @param name 字段的名称
	 * @return 对应的Field对象，如果未找到则返回{@code null}
	 */
	@Nullable
	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	/**
	 * 尝试在提供的{@link Class}上查找具有提供的{@code name}和/或{@link Class type}的{@link Field 字段}。
	 * 搜索所有父类直到{@link Object}。
	 *
	 * @param clazz 要内省的类
	 * @param name 字段的名称（如果指定了type，则可以为{@code null}）
	 * @param type 字段的类型（如果指定了name，则可以为{@code null}）
	 * @return 对应的Field对象，如果未找到则返回{@code null}
	 */
	@Nullable
	public static Field findField(Class<?> clazz, @Nullable String name, @Nullable Class<?> type) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
		Class<?> searchType = clazz;
		while (Object.class != searchType && searchType != null) {
			Field[] fields = getDeclaredFields(searchType);
			for (Field field : fields) {
				if ((name == null || name.equals(field.getName())) &&
						(type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	/**
	 * 将提供的{@linkplain Field field object}表示的字段在指定的{@linkplain Object target object}上
	 * 设置为指定的{@code value}。
	 * <p>根据{@link Field#set(Object, Object)}语义，如果底层字段是原始类型，
	 * 新值会自动解包。
	 * <p>此方法不支持设置{@code static final}字段。
	 * <p>抛出的异常通过调用{@link #handleReflectionException(Exception)}来处理。
	 *
	 * @param field 要设置的字段
	 * @param target 要设置字段的目标对象（对于静态字段可以为{@code null}）
	 * @param value 要设置的值（可以为{@code null}）
	 */
	public static void setField(Field field, @Nullable Object target, @Nullable Object value) {
		try {
			field.set(target, value);
		} catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
	}

	/**
	 * 从指定的{@link Object target object}获取提供的{@link Field field object}表示的字段。
	 * 根据{@link Field#get(Object)}语义，如果底层字段是原始类型，
	 * 返回值会自动包装。
	 * <p>抛出的异常通过调用{@link #handleReflectionException(Exception)}来处理。
	 *
	 * @param field 要获取的字段
	 * @param target 要从中获取字段的目标对象（对于静态字段可以为{@code null}）
	 * @return 字段的当前值
	 */
	@Nullable
	public static Object getField(Field field, @Nullable Object target) {
		try {
			return field.get(target);
		} catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * 对给定类中所有本地声明的字段调用给定的回调。
	 *
	 * @param clazz 要分析的目标类
	 * @param fc 为每个字段调用的回调
	 * @throws IllegalStateException 如果内省失败
	 * @see #doWithFields
	 * @since 4.2
	 */
	public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
		for (Field field : getDeclaredFields(clazz)) {
			try {
				fc.doWith(field);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
			}
		}
	}

	/**
	 * 对目标类中的所有字段调用给定的回调，沿着类层次结构向上
	 * 获取所有声明的字段。
	 *
	 * @param clazz 要分析的目标类
	 * @param fc 为每个字段调用的回调
	 * @throws IllegalStateException 如果内省失败
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc) {
		doWithFields(clazz, fc, null);
	}

	/**
	 * 对目标类中的所有字段调用给定的回调，沿着类层次结构向上
	 * 获取所有声明的字段。
	 *
	 * @param clazz 要分析的目标类
	 * @param fc 为每个字段调用的回调
	 * @param ff 确定要应用回调的字段的过滤器
	 * @throws IllegalStateException 如果内省失败
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc, @Nullable FieldFilter ff) {
		// 沿着继承层次结构向上追溯
		Class<?> targetClass = clazz;
		do {
			Field[] fields = getDeclaredFields(targetClass);
			for (Field field : fields) {
				if (ff != null && !ff.matches(field)) {
					continue;
				}
				try {
					fc.doWith(field);
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

	/**
	 * 此变体从本地缓存中检索{@link Class#getDeclaredFields()}，
	 * 以避免JVM的SecurityManager检查和防御性数组复制。
	 *
	 * @param clazz 要内省的类
	 * @return 缓存的字段数组
	 * @throws IllegalStateException 如果内省失败
	 * @see Class#getDeclaredFields()
	 */
	private static Field[] getDeclaredFields(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Field[] result = declaredFieldsCache.get(clazz);
		if (result == null) {
			try {
				result = clazz.getDeclaredFields();
				declaredFieldsCache.put(clazz, (result.length == 0 ? EMPTY_FIELD_ARRAY : result));
			} catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName() +
						"] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
			}
		}
		return result;
	}

	/**
	 * 给定源对象和目标对象，两者必须是同一类或子类，
	 * 复制所有字段，包括继承的字段。设计用于具有公共无参构造函数的对象。
	 *
	 * @throws IllegalStateException 如果内省失败
	 */
	public static void shallowCopyFieldState(final Object src, final Object dest) {
		Assert.notNull(src, "Source for field copy cannot be null");
		Assert.notNull(dest, "Destination for field copy cannot be null");
		if (!src.getClass().isAssignableFrom(dest.getClass())) {
			throw new IllegalArgumentException("Destination class [" + dest.getClass().getName() +
					"] must be same or subclass as source class [" + src.getClass().getName() + "]");
		}
		doWithFields(src.getClass(), field -> {
			makeAccessible(field);
			Object srcValue = field.get(src);
			field.set(dest, srcValue);
		}, COPYABLE_FIELDS);
	}

	/**
	 * 确定给定的字段是否是"public static final"常量。
	 *
	 * @param field 要检查的字段
	 */
	public static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
	 * 使给定的字段可访问，如有必要明确设置其可访问性。
	 * 只有在实际必要时才调用{@code setAccessible(true)}方法，
	 * 以避免与JVM SecurityManager（如果激活）发生不必要的冲突。
	 *
	 * @param field 要使其可访问的字段
	 * @see java.lang.reflect.Field#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Field field) {
		if ((!Modifier.isPublic(field.getModifiers()) ||
				!Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
				Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
			field.setAccessible(true);
		}
	}


	// 缓存处理

	/**
	 * 清除内部方法/字段缓存。
	 *
	 * @since 4.2.4
	 */
	public static void clearCache() {
		declaredMethodsCache.clear();
		declaredFieldsCache.clear();
	}


	/**
	 * 对每种方法采取的行动。
	 */
	@FunctionalInterface
	public interface MethodCallback {

		/**
		 * 使用给定的方法执行操作。
		 *
		 * @param method 操作的方法
		 */
		void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * 回调可选地，用于筛选要由方法回调操作的方法。
	 */
	@FunctionalInterface
	public interface MethodFilter {

		/**
		 * 确定给定的方法是否匹配。
		 *
		 * @param method 要检查的方法
		 */
		boolean matches(Method method);

		/**
		 * 基于此过滤器 <em> 和 <em> 提供的过滤器创建复合过滤器。
		 * <p> 如果此过滤器不匹配，则不会应用下一个过滤器。
		 *
		 * @param next 下一个 {@code MethodFilter}
		 * @return 组合的 {@code MethodFilter}
		 * @throws IllegalArgumentException 如果MethodFilter参数为 {@code null}
		 * @since 5.3.2
		 */
		default MethodFilter and(MethodFilter next) {
			Assert.notNull(next, "Next MethodFilter must not be null");
			return method -> matches(method) && next.matches(method);
		}
	}


	/**
	 * 在层次结构中的每个字段上调用的回调接口。
	 */
	@FunctionalInterface
	public interface FieldCallback {

		/**
		 * 使用给定的字段执行操作。
		 *
		 * @param field 要操作的字段
		 */
		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * 可选用于过滤字段的回调，这些字段将被字段回调操作。
	 */
	@FunctionalInterface
	public interface FieldFilter {

		/**
		 * 确定给定的字段是否匹配。
		 *
		 * @param field 要检查的字段
		 */
		boolean matches(Field field);

		/**
		 * 基于此过滤器<em>和</em>提供的过滤器创建复合过滤器。
		 * <p>如果此过滤器不匹配，则不会应用下一个过滤器。
		 *
		 * @param next 下一个{@code FieldFilter}
		 * @return 复合{@code FieldFilter}
		 * @throws IllegalArgumentException 如果FieldFilter参数为{@code null}
		 * @since 5.3.2
		 */
		default FieldFilter and(FieldFilter next) {
			Assert.notNull(next, "Next FieldFilter must not be null");
			return field -> matches(field) && next.matches(field);
		}
	}

}
