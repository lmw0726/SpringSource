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
 * Simple utility class for working with the reflection API and handling
 * reflection exceptions.
 *
 * <p>Only intended for internal use.
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
	 * Pre-built FieldFilter that matches all non-static, non-final fields.
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
	 * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
	 */
	private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256);


	// Exception handling

	/**
	 * Handle the given reflection exception.
	 * <p>Should only be called if no checked exception is expected to be thrown
	 * by a target method, or if an error occurs while accessing a method or field.
	 * <p>Throws the underlying RuntimeException or Error in case of an
	 * InvocationTargetException with such a root cause. Throws an
	 * IllegalStateException with an appropriate message or
	 * UndeclaredThrowableException otherwise.
	 *
	 * @param ex the reflection exception to handle
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
	 * Handle the given invocation target exception. Should only be called if no
	 * checked exception is expected to be thrown by the target method.
	 * <p>Throws the underlying RuntimeException or Error in case of such a root
	 * cause. Throws an UndeclaredThrowableException otherwise.
	 *
	 * @param ex the invocation target exception to handle
	 */
	public static void handleInvocationTargetException(InvocationTargetException ex) {
		rethrowRuntimeException(ex.getTargetException());
	}

	/**
	 * Rethrow the given {@link Throwable exception}, which is presumably the
	 * <em>target exception</em> of an {@link InvocationTargetException}.
	 * Should only be called if no checked exception is expected to be thrown
	 * by the target method.
	 * <p>Rethrows the underlying exception cast to a {@link RuntimeException} or
	 * {@link Error} if appropriate; otherwise, throws an
	 * {@link UndeclaredThrowableException}.
	 *
	 * @param ex the exception to rethrow
	 * @throws RuntimeException the rethrown exception
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
	 * Rethrow the given {@link Throwable exception}, which is presumably the
	 * <em>target exception</em> of an {@link InvocationTargetException}.
	 * Should only be called if no checked exception is expected to be thrown
	 * by the target method.
	 * <p>Rethrows the underlying exception cast to an {@link Exception} or
	 * {@link Error} if appropriate; otherwise, throws an
	 * {@link UndeclaredThrowableException}.
	 *
	 * @param ex the exception to rethrow
	 * @throws Exception the rethrown exception (in case of a checked exception)
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


	// Constructor handling

	/**
	 * Obtain an accessible constructor for the given class and parameters.
	 *
	 * @param clazz          the clazz to check
	 * @param parameterTypes the parameter types of the desired constructor
	 * @return the constructor reference
	 * @throws NoSuchMethodException if no such constructor exists
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


	// Method handling

	/**
	 * Attempt to find a {@link Method} on the supplied class with the supplied name
	 * and no parameters. Searches all superclasses up to {@code Object}.
	 * <p>Returns {@code null} if no {@link Method} can be found.
	 *
	 * @param clazz the class to introspect
	 * @param name  the name of the method
	 * @return the Method object, or {@code null} if none found
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String name) {
		return findMethod(clazz, name, EMPTY_CLASS_ARRAY);
	}

	/**
	 * Attempt to find a {@link Method} on the supplied class with the supplied name
	 * and parameter types. Searches all superclasses up to {@code Object}.
	 * <p>Returns {@code null} if no {@link Method} can be found.
	 *
	 * @param clazz      the class to introspect
	 * @param name       the name of the method
	 * @param paramTypes the parameter types of the method
	 *                   (may be {@code null} to indicate any signature)
	 * @return the Method object, or {@code null} if none found
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
	 * Invoke the specified {@link Method} against the supplied target object with no arguments.
	 * The target object can be {@code null} when invoking a static {@link Method}.
	 * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException}.
	 *
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @return the invocation result, if any
	 * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
	 */
	@Nullable
	public static Object invokeMethod(Method method, @Nullable Object target) {
		return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
	}

	/**
	 * Invoke the specified {@link Method} against the supplied target object with the
	 * supplied arguments. The target object can be {@code null} when invoking a
	 * static {@link Method}.
	 * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException}.
	 *
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @param args   the invocation arguments (may be {@code null})
	 * @return the invocation result, if any
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
	 * Determine whether the given method explicitly declares the given
	 * exception or one of its superclasses, which means that an exception
	 * of that type can be propagated as-is within a reflective invocation.
	 *
	 * @param method        the declaring method
	 * @param exceptionType the exception to throw
	 * @return {@code true} if the exception can be thrown as-is;
	 * {@code false} if it needs to be wrapped
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
	 * Perform the given callback operation on all matching methods of the given
	 * class, as locally declared or equivalent thereof (such as default methods
	 * on Java 8 based interfaces that the given class implements).
	 *
	 * @param clazz the class to introspect
	 * @param mc    the callback to invoke for each method
	 * @throws IllegalStateException if introspection fails
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
	 * Perform the given callback operation on all matching methods of the given
	 * class and superclasses.
	 * <p>The same named method occurring on subclass and superclass will appear
	 * twice, unless excluded by a {@link MethodFilter}.
	 *
	 * @param clazz the class to introspect
	 * @param mc    the callback to invoke for each method
	 * @throws IllegalStateException if introspection fails
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
	 * Get all declared methods on the leaf class and all superclasses.
	 * Leaf class methods are included first.
	 *
	 * @param leafClass the class to introspect
	 * @throws IllegalStateException if introspection fails
	 */
	public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, methods::add);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * Get the unique set of declared methods on the leaf class and all superclasses.
	 * Leaf class methods are included first and while traversing the superclass hierarchy
	 * any methods found with signatures matching a method already included are filtered out.
	 *
	 * @param leafClass the class to introspect
	 * @throws IllegalStateException if introspection fails
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
	 * Determine whether the given method is a "hashCode" method.
	 *
	 * @see java.lang.Object#hashCode()
	 */
	public static boolean isHashCodeMethod(@Nullable Method method) {
		return method != null && method.getParameterCount() == 0 && method.getName().equals("hashCode");
	}

	/**
	 * Determine whether the given method is a "toString" method.
	 *
	 * @see java.lang.Object#toString()
	 */
	public static boolean isToStringMethod(@Nullable Method method) {
		return (method != null && method.getParameterCount() == 0 && method.getName().equals("toString"));
	}

	/**
	 * Determine whether the given method is originally declared by {@link java.lang.Object}.
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
	 * Make the given method accessible, explicitly setting it accessible if
	 * necessary. The {@code setAccessible(true)} method is only called
	 * when actually necessary, to avoid unnecessary conflicts with a JVM
	 * SecurityManager (if active).
	 *
	 * @param method the method to make accessible
	 * @see java.lang.reflect.Method#setAccessible
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
			method.setAccessible(true);
		}
	}


	// Field handling

	/**
	 * Attempt to find a {@link Field field} on the supplied {@link Class} with the
	 * supplied {@code name}. Searches all superclasses up to {@link Object}.
	 *
	 * @param clazz the class to introspect
	 * @param name  the name of the field
	 * @return the corresponding Field object, or {@code null} if not found
	 */
	@Nullable
	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	/**
	 * Attempt to find a {@link Field field} on the supplied {@link Class} with the
	 * supplied {@code name} and/or {@link Class type}. Searches all superclasses
	 * up to {@link Object}.
	 *
	 * @param clazz the class to introspect
	 * @param name  the name of the field (may be {@code null} if type is specified)
	 * @param type  the type of the field (may be {@code null} if name is specified)
	 * @return the corresponding Field object, or {@code null} if not found
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
	 * Set the field represented by the supplied {@linkplain Field field object} on
	 * the specified {@linkplain Object target object} to the specified {@code value}.
	 * <p>In accordance with {@link Field#set(Object, Object)} semantics, the new value
	 * is automatically unwrapped if the underlying field has a primitive type.
	 * <p>This method does not support setting {@code static final} fields.
	 * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException(Exception)}.
	 *
	 * @param field  the field to set
	 * @param target the target object on which to set the field
	 *               (or {@code null} for a static field)
	 * @param value  the value to set (may be {@code null})
	 */
	public static void setField(Field field, @Nullable Object target, @Nullable Object value) {
		try {
			field.set(target, value);
		} catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
	}

	/**
	 * Get the field represented by the supplied {@link Field field object} on the
	 * specified {@link Object target object}. In accordance with {@link Field#get(Object)}
	 * semantics, the returned value is automatically wrapped if the underlying field
	 * has a primitive type.
	 * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException(Exception)}.
	 *
	 * @param field  the field to get
	 * @param target the target object from which to get the field
	 *               (or {@code null} for a static field)
	 * @return the field's current value
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
	 * Invoke the given callback on all locally declared fields in the given class.
	 *
	 * @param clazz the target class to analyze
	 * @param fc    the callback to invoke for each field
	 * @throws IllegalStateException if introspection fails
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
	 * Invoke the given callback on all fields in the target class, going up the
	 * class hierarchy to get all declared fields.
	 *
	 * @param clazz the target class to analyze
	 * @param fc    the callback to invoke for each field
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc) {
		doWithFields(clazz, fc, null);
	}

	/**
	 * Invoke the given callback on all fields in the target class, going up the
	 * class hierarchy to get all declared fields.
	 *
	 * @param clazz the target class to analyze
	 * @param fc    the callback to invoke for each field
	 * @param ff    the filter that determines the fields to apply the callback to
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc, @Nullable FieldFilter ff) {
		// Keep backing up the inheritance hierarchy.
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
	 * This variant retrieves {@link Class#getDeclaredFields()} from a local cache
	 * in order to avoid the JVM's SecurityManager check and defensive array copying.
	 *
	 * @param clazz the class to introspect
	 * @return the cached array of fields
	 * @throws IllegalStateException if introspection fails
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
	 * Given the source object and the destination, which must be the same class
	 * or a subclass, copy all fields, including inherited fields. Designed to
	 * work on objects with public no-arg constructors.
	 *
	 * @throws IllegalStateException if introspection fails
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
	 * Determine whether the given field is a "public static final" constant.
	 *
	 * @param field the field to check
	 */
	public static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
	 * Make the given field accessible, explicitly setting it accessible if
	 * necessary. The {@code setAccessible(true)} method is only called
	 * when actually necessary, to avoid unnecessary conflicts with a JVM
	 * SecurityManager (if active).
	 *
	 * @param field the field to make accessible
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


	// Cache handling

	/**
	 * Clear the internal method/field cache.
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
	 * Callback interface invoked on each field in the hierarchy.
	 */
	@FunctionalInterface
	public interface FieldCallback {

		/**
		 * Perform an operation using the given field.
		 *
		 * @param field the field to operate on
		 */
		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * Callback optionally used to filter fields to be operated on by a field callback.
	 */
	@FunctionalInterface
	public interface FieldFilter {

		/**
		 * Determine whether the given field matches.
		 *
		 * @param field the field to check
		 */
		boolean matches(Field field);

		/**
		 * Create a composite filter based on this filter <em>and</em> the provided filter.
		 * <p>If this filter does not match, the next filter will not be applied.
		 *
		 * @param next the next {@code FieldFilter}
		 * @return a composite {@code FieldFilter}
		 * @throws IllegalArgumentException if the FieldFilter argument is {@code null}
		 * @since 5.3.2
		 */
		default FieldFilter and(FieldFilter next) {
			Assert.notNull(next, "Next FieldFilter must not be null");
			return field -> matches(field) && next.matches(field);
		}
	}

}
