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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * 各种 {@code java.lang.Class} 工具方法。
 * 主要用于框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @see TypeUtils
 * @see ReflectionUtils
 * @since 1.1
 */
public abstract class ClassUtils {

	/**
	 * 数组类名称的后缀: {@code "[]"}。
	 */
	public static final String ARRAY_SUFFIX = "[]";

	/**
	 * 内部数组类名称的前缀： {@code "["}.
	 */
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/**
	 * 内部非原始数组类名的前缀： {@code "[L"}.
	 */
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/**
	 * 可重用的空Class数组常量。
	 */
	private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

	/**
	 * 包分隔符: {@code '.'}.
	 */
	private static final char PACKAGE_SEPARATOR = '.';

	/**
	 * 路径分隔符：{@code '/'}。
	 */
	private static final char PATH_SEPARATOR = '/';

	/**
	 * 嵌套类分隔符: {@code '$'}.
	 */
	private static final char NESTED_CLASS_SEPARATOR = '$';

	/**
	 * CGLIB 类分隔符： {@code "$$"}.
	 */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/**
	 * .class 文件后缀名
	 */
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * 以原始包装器类型为键，以对应的原始类型为值的映射，例如: Integer.class -> int.class。
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

	/**
	 * 以原始类型为键，以对应的包装类型为值的映射，例如: int.class -> Integer.class。
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);

	/**
	 * 基本类型名称与对应基本类型的映射表，
	 * 例如："int" -> int.class
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * 以通用Java语言类名为键，对应类为值的映射。主要用于远程调用的高效反序列化。
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * 在查找"主要"用户级接口时应忽略的通用Java语言接口集合。
	 */
	private static final Set<Class<?>> javaLanguageInterfaces;

	/**
	 * 声明类实现的接口上等效方法的缓存。
	 */
	private static final Map<Method, Method> interfaceMethodCache = new ConcurrentReferenceHashMap<>(256);


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);
		primitiveWrapperTypeMap.put(Void.class, void.class);

		// 与使用lambdas的forEach相比，映射条目迭代的初始化成本更低
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
	}


	/**
	 * 将给定的通用类注册到ClassUtils缓存中。
	 */
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * 返回默认的ClassLoader：通常是线程上下文ClassLoader（如果可用）；
	 * 否则使用加载ClassUtils类的ClassLoader作为后备。
	 * <p>在明确需要非null ClassLoader引用的场景下调用此方法，
	 * 例如类路径资源加载（但不一定适用于{@code Class.forName}，
	 * 它也接受{@code null} ClassLoader引用）。
	 *
	 * @return 默认ClassLoader（只有在系统ClassLoader也不可访问时才返回{@code null}）
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	@Nullable
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
			// 无法访问线程上下文ClassLoader - 回退中...
		}
		if (cl == null) {
			// 没有线程上下文ClassLoader -> 使用本类的ClassLoader
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader()返回null表示启动类加载器
				try {
					cl = ClassLoader.getSystemClassLoader();
				} catch (Throwable ex) {
					// 无法访问系统ClassLoader - 好吧，也许调用者可以接受null...
				}
			}
		}
		return cl;
	}

	/**
	 * 如有必要（即当bean ClassLoader与当前线程上下文ClassLoader不同时），
	 * 使用环境中的bean ClassLoader覆盖线程上下文ClassLoader。
	 *
	 * @param classLoaderToUse 要设置为线程上下文ClassLoader的实际ClassLoader
	 * @return 原始的线程上下文ClassLoader，如果未被覆盖则返回{@code null}
	 */
	@Nullable
	public static ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		} else {
			return null;
		}
	}

	/**
	 * 替换 {@code Class.forName()}，它还返回基本类型 (例如 “int”) 和数组类名称 (例如 “String[]”) 的类实例。
	 * 此外，它还能够以Java源码风格 (例如 “java.lang.Thread.State” 而不是 “java.lang.Thread#State”) 解析嵌套类名。
	 *
	 * @param name        类名
	 * @param classLoader 要使用的类加载器 (可能是 {@code null}，表示默认的类加载器)
	 * @return 提供名称的类实例
	 * @throws ClassNotFoundException 如果找不到类
	 * @throws LinkageError           如果无法加载类文件
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Assert.notNull(name, "Name must not be null");
		//解析基本类型的类名
		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			//如果类为空，从通用的类缓存获取该名称对应的类
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			//类不为空，返回该类
			return clazz;
		}

		// "java.lang.String[]" 风格的数组
		if (name.endsWith(ARRAY_SUFFIX)) {
			//如果该名称以 [] 结尾（即数组类名称）
			//获取 [] 之前的名称作为元素类名
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			//递归调用，获取该元素类名的类型实例
			Class<?> elementClass = forName(elementClassName, classLoader);
			//构建成数组类型的类实例返回
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" 风格的数组
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			//如果该类名以 [L 开头并且以 ; 结尾（即内部非原始数组类名），获取这两种字符中间的字符串作为元素类型名称
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			//递归调用，获取该元素类名的类型实例
			Class<?> elementClass = forName(elementName, classLoader);
			//构建成数组类型的类实例返回
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" or "[[Ljava.lang.String;" 风格的数组
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			//如果类名以 [ 开头（即内部数组类），截取 [ 后的字符串作为元素类型名称
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			//递归调用，获取该元素类名的类型实例
			Class<?> elementClass = forName(elementName, classLoader);
			//构建成数组类型的类实例返回
			return Array.newInstance(elementClass, 0).getClass();
		}
		//构建一个类加载器的局部变量，使后续赋值不影响原来的类加载器变量。
		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			//如果类加载器为空，使用默认的类加载器
			clToUse = getDefaultClassLoader();
		}
		try {
			//反射调用该类名，获取该类的类实例
			return Class.forName(name, false, clToUse);
		} catch (ClassNotFoundException ex) {
			//如果是以包分隔符 . 结尾
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				//在包分隔符后插入$符号
				String nestedClassName =
						name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					//反射调用该类名，获取该类的类实例
					return Class.forName(nestedClassName, false, clToUse);
				} catch (ClassNotFoundException ex2) {
					// 吞掉异常 - 让原始异常通过
				}
			}
			throw ex;
		}
	}

	/**
	 * 将给定的类名解析为Class实例。支持基本类型（如"int"）和数组类名（如"String[]"）。
	 * <p>此方法实际上等效于具有相同参数的{@code forName}方法，
	 * 唯一区别在于类加载失败时抛出的异常类型不同。
	 *
	 * @param className   要解析的类名
	 * @param classLoader 使用的类加载器
	 *                   （可为{@code null}，表示使用默认类加载器）
	 * @return 对应类名的Class实例
	 * @throws IllegalArgumentException 如果类名无法解析
	 *                                  （即找不到类或无法加载类文件）
	 * @throws IllegalStateException    如果类可解析但在继承层次结构中存在可读性不匹配
	 *                                  （通常是由于Jigsaw模块定义中缺少待加载类的父类或接口的依赖声明）
	 * @see #forName(String, ClassLoader)
	 */
	public static Class<?> resolveClassName(String className, @Nullable ClassLoader classLoader)
			throws IllegalArgumentException {

		try {
			return forName(className, classLoader);
		} catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		} catch (LinkageError err) {
			throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
		}
	}

	/**
	 * 判断指定类名对应的{@link Class}是否存在且可加载。
	 * 如果类或其依赖项不存在或无法加载，则返回{@code false}。
	 *
	 * @param className   要检查的类名
	 * @param classLoader 使用的类加载器
	 *                   （可为{@code null}，表示使用默认类加载器）
	 * @return 指定类是否存在（包括其所有父类和接口）
	 * @throws IllegalStateException 如果类可解析但在继承层次结构中存在可读性不匹配
	 *                              （通常是由于Jigsaw模块定义中缺少待检查类的父类或接口的依赖声明）
	 */
	public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		} catch (IllegalAccessError err) {
			throw new IllegalStateException("Readability mismatch in inheritance hierarchy of class [" +
					className + "]: " + err.getMessage(), err);
		} catch (Throwable ex) {
			// 通常是ClassNotFoundException或NoClassDefFoundError...
			return false;
		}
	}

	/**
	 * 检查给定类在指定的ClassLoader中是否可见。
	 *
	 * @param clazz       要检查的类（通常是一个接口）
	 * @param classLoader 要检查的ClassLoader
	 *                   （可为{@code null}，此时该方法始终返回{@code true}）
	 */
	public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			if (clazz.getClassLoader() == classLoader) {
				return true;
			}
		} catch (SecurityException ex) {
			// 继续执行下面的可加载检查
		}

		// 如果能从给定ClassLoader加载相同的类，则认为可见
		return isLoadable(clazz, classLoader);
	}

	/**
	 * 检查给定类在指定上下文中是否是缓存安全的，
	 * 即是否由给定ClassLoader或其父加载器加载。
	 *
	 * @param clazz       要分析的类
	 * @param classLoader 可能缓存元数据的ClassLoader
	 *                   （可为{@code null}，表示系统类加载器）
	 */
	public static boolean isCacheSafe(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// 常见情况处理
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// 检查祖先加载器是否匹配 -> 匹配则返回true
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// 检查子加载器是否匹配 -> 匹配则返回false
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		} catch (SecurityException ex) {
			// 继续执行下面的可加载检查
		}

		// 对于没有父子关系的ClassLoader的回退方案：
		// 如果能从给定ClassLoader加载相同的类，则认为安全
		return (classLoader != null && isLoadable(clazz, classLoader));
	}

	/**
	 * 检查给定类是否能在指定的ClassLoader中加载。
	 *
	 * @param clazz       要检查的类（通常是一个接口）
	 * @param classLoader 要检查的ClassLoader
	 * @since 5.0.6
	 */
	private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// 否则：找到了同名的不同类
		} catch (ClassNotFoundException ex) {
			// 完全没有找到对应的类
			return false;
		}
	}

	/**
	 * 根据JVM对原始类的命名规则，如果合适的话，将给定的类名解析为原始类。
	 * <p> 还支持JVM的原始数组的内部类名称。
	 * <i> 不 <i> 支持原始数组的 “[]” 后缀表示法; 这仅由 {@link #forName(String, ClassLoader)} 支持。
	 *
	 * @param name 潜在原始类的名称
	 * @return 原始类，如果名称不表示原始类或原始数组类，则为 {@code null}
	 */
	@Nullable
	public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
		Class<?> result = null;
		//考虑到它们应该放在一个包中，大多数类名会很长，所以长度检查是值得的。
		if (name != null && name.length() <= 7) {
			// 如果类名不为空，且类名长度小于7，从原始类型类型Map获取该类型
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * 检查给定类是否表示基本类型的包装类，
	 * 即Boolean、Byte、Character、Short、Integer、Long、Float、Double或Void。
	 *
	 * @param clazz 要检查的类
	 * @return 给定类是否是基本类型的包装类
	 */
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * 检查给定类是否表示基本类型（即boolean、byte、char、short、int、long、float或double）、
	 * {@code void}，或这些类型的包装类（即Boolean、Byte、Character、Short、Integer、Long、
	 * Float、Double或Void）。
	 *
	 * @param clazz 要检查的类
	 * @return 如果给定类表示基本类型、void或包装类，则返回{@code true}
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * 检查给定类是否表示基本类型的数组，
	 * 即boolean、byte、char、short、int、long、float或double的数组。
	 *
	 * @param clazz 要检查的类
	 * @return 给定类是否是基本类型数组类
	 */
	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	/**
	 * 检查给定类是否表示基本类型包装类的数组，
	 * 即Boolean、Byte、Character、Short、Integer、Long、Float或Double的数组。
	 *
	 * @param clazz 要检查的类
	 * @return 给定类是否是基本类型包装类的数组类
	 */
	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	/**
	 * 解析给定的类，如果它是一个原始类，则返回相应的原始包装类型。
	 *
	 * @param clazz 要检查的类
	 * @return 原始类，或原始类型的包装器
	 */
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		//如果该类是原始类型，且该类不是void类型，通过primitiveTypeToWrapperMap获取对应的包装类型，返回该类型。
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * 假设通过反射设置，请检查右侧类型是否可以分配给左侧类型。将原始包装器类视为可分配给相应的原始类型。
	 *
	 * @param lhsType 目标类型
	 * @param rhsType 应该分配给目标类型的值类型
	 * @return 如果目标类型是可从值类型分配的
	 * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
	 */
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			//如果右边的类型是左边类型的子类，返回true
			return true;
		}
		if (lhsType.isPrimitive()) {
			//如果左边类型是原始类型，获取右边类型对应的原始类型，并对他们进行比较。
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			return (lhsType == resolvedPrimitive);
		} else {
			//获取右边类型的包装类型
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			//如果包装类型不为空，且包装类型是左边类型的子类，返回true
			return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
		}
	}

	/**
	 * 假设通过反射设置，确定给定类型是否可从给定值分配。将原始包装器类视为可分配给相应的原始类型。
	 *
	 * @param type  目标类型
	 * @param value 应该分配给类型的值
	 * @return 如果可以从值中分配类型
	 */
	public static boolean isAssignableValue(Class<?> type, @Nullable Object value) {
		Assert.notNull(type, "Type must not be null");
		//如果值为空，则判断目标类型不是原始类型
		//否则调用isAssignable方法，判断两者的类型是否是派生出来的。
		return (value == null ? !type.isPrimitive() : isAssignable(type, value.getClass()));
	}

	/**
	 * 将基于"/"的资源路径转换为基于"."的完全限定类名。
	 *
	 * @param resourcePath 指向类的资源路径
	 * @return 对应的完全限定类名
	 */
	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	/**
	 * 将基于"."的完全限定类名转换为基于"/"的资源路径。
	 *
	 * @param className 完全限定类名
	 * @return 指向类的对应资源路径
	 */
	public static String convertClassNameToResourcePath(String className) {
		Assert.notNull(className, "Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 返回适用于{@code ClassLoader.getResource}使用的路径
	 * (也可通过在前面添加斜杠('/')适用于{@code Class.getResource})。
	 * 通过获取指定类文件的包名，将所有点('.')转换为斜杠('/')，
	 * 必要时添加尾部斜杠，并将指定的资源名称拼接在后面。
	 * <br/>因此，此方法可用于构建与类文件在同一包中的资源文件的加载路径，
	 * 尽管通常使用{@link org.springframework.core.io.ClassPathResource}更为方便。
	 *
	 * @param clazz        用作基础路径的类
	 * @param resourceName 要附加的资源名称。开头的斜杠是可选的。
	 * @return 构建的资源路径
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.notNull(resourceName, "Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * 给定一个输入类对象，返回由类的包名组成的路径字符串，
	 * 即所有点('.')都被替换为斜杠('/')。不添加开头或结尾的斜杠。
	 * 结果可以与斜杠和资源名称连接，并直接用于{@code ClassLoader.getResource()}。
	 * 如果要用于{@code Class.getResource}，则需要在返回值前添加一个前导斜杠。
	 *
	 * @param clazz 输入类。{@code null}值或默认(空)包将返回空字符串("")。
	 * @return 表示包名的路径
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String classPackageAsResourcePath(@Nullable Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 构建由给定数组中类/接口名称组成的字符串。
	 * <p>基本类似于{@code AbstractCollection.toString()}，但会去除
	 * 每个类名前的"class "/"interface "前缀。
	 *
	 * @param classes 类对象数组
	 * @return 形如"[com.foo.Bar, com.foo.Baz]"的字符串
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	/**
	 * 构建由给定集合中类/接口名称组成的字符串。
	 * <p>基本类似于{@code AbstractCollection.toString()}，但会去除
	 * 每个类名前的"class "/"interface "前缀。
	 *
	 * @param classes 类对象集合（可为{@code null}）
	 * @return 形如"[com.foo.Bar, com.foo.Baz]"的字符串
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(@Nullable Collection<Class<?>> classes) {
		if (CollectionUtils.isEmpty(classes)) {
			return "[]";
		}
		StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
		for (Class<?> clazz : classes) {
			stringJoiner.add(clazz.getName());
		}
		return stringJoiner.toString();
	}

	/**
	 * 将给定的{@code Collection}复制到{@code Class}数组中。
	 * <p>{@code Collection}必须仅包含{@code Class}元素。
	 *
	 * @param collection 要复制的集合
	 * @return 类数组
	 * @see StringUtils#toStringArray
	 * @since 3.1
	 */
	public static Class<?>[] toClassArray(@Nullable Collection<Class<?>> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
	}

	/**
	 * 返回给定实例实现的所有接口数组，包括超类实现的接口。
	 *
	 * @param instance 要分析接口的实例
	 * @return 实例实现的所有接口数组
	 */
	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	/**
	 * 返回给定类实现的所有接口数组，包括超类实现的接口。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 *
	 * @param clazz 要分析接口的类
	 * @return 给定对象实现的所有接口数组
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	/**
	 * 返回给定类实现的所有接口数组，包括超类实现的接口。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 *
	 * @param clazz       要分析接口的类
	 * @param classLoader 接口需要可见的ClassLoader
	 *                   （可为{@code null}，表示接受所有声明的接口）
	 * @return 给定对象实现的所有接口数组
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, @Nullable ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	/**
	 * 返回给定实例实现的所有接口Set，包括超类实现的接口。
	 *
	 * @param instance 要分析接口的实例
	 * @return 给定实例实现的所有接口Set
	 */
	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	/**
	 * 返回给定类实现的所有接口Set，包括超类实现的接口。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 *
	 * @param clazz 要分析接口的类
	 * @return 给定对象实现的所有接口Set
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	/**
	 * 返回给定类实现的所有接口Set，包括超类实现的接口。
	 * <p>如果类本身是接口，则将其作为唯一接口返回。
	 *
	 * @param clazz       要分析接口的类
	 * @param classLoader 接口需要可见的ClassLoader
	 *                   （可为{@code null}，表示接受所有声明的接口）
	 * @return 给定对象实现的所有接口Set
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (isVisible(ifc, classLoader)) {
					interfaces.add(ifc);
				}
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * 为给定接口创建组合接口Class，将多个接口合并为一个Class。
	 * <p>此实现为给定接口构建一个JDK代理类。
	 *
	 * @param interfaces  要合并的接口数组
	 * @param classLoader 创建组合Class的ClassLoader
	 * @return 合并后的接口Class
	 * @throws IllegalArgumentException 如果指定的接口存在冲突的方法签名
	 *                                  （或违反类似约束）
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	@SuppressWarnings("deprecation")  // JDK 9上已弃用
	public static Class<?> createCompositeInterface(Class<?>[] interfaces, @Nullable ClassLoader classLoader) {
		Assert.notEmpty(interfaces, "Interface array must not be empty");
		return Proxy.getProxyClass(classLoader, interfaces);
	}

	/**
	 * 确定给定类的共同祖先 (如果有)。
	 *
	 * @param clazz1 要内省的类
	 * @param clazz2 要内省的其他类
	 * @return 公共祖先 (即公共超类，一个接口扩展另一个)，或者 {@code null}，如果没有找到。
	 * 如果给定的任何一个类是 {@code null}，则将返回另一个类。
	 * @since 3.2.6
	 */
	@Nullable
	public static Class<?> determineCommonAncestor(@Nullable Class<?> clazz1, @Nullable Class<?> clazz2) {
		//如果两者中的其中一个类为空，返回另外一个类
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			//如果class2是class1的子类或者子接口，那么返回class1类型
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			//如果class1是class2的子类或者子接口，那么返回class2类型
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			//获取父类
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				//如果父类为空，或者父类为Object类，返回null。
				return null;
			}
			//如果class2不是父类的子类或者子接口，跳出循环
		} while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	/**
	 * 判断给定接口是否是常见的Java语言接口：
	 * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
	 * {@link Cloneable}, {@link Comparable} - 这些接口在查找"主要"用户级接口时都可以忽略。
	 * 共同特征：没有服务级操作，没有bean属性方法，没有默认方法。
	 *
	 * @param ifc 要检查的接口
	 * @since 5.0.3
	 */
	public static boolean isJavaLanguageInterface(Class<?> ifc) {
		return javaLanguageInterfaces.contains(ifc);
	}

	/**
	 * 判断提供的类是否是<em>内部类</em>，
	 * 即外部类的非静态成员类。
	 *
	 * @return 如果提供的类是内部类则返回{@code true}
	 * @see Class#isMemberClass()
	 * @since 5.0.5
	 */
	public static boolean isInnerClass(Class<?> clazz) {
		return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
	}

	/**
	 * 判断提供的{@link Class}是否是JVM为lambda表达式或方法引用生成的实现类。
	 * <p>此方法基于在现代主流JVM上有效的检查，尽力做出判断。
	 *
	 * @param clazz 要检查的类
	 * @return 如果类是lambda实现类则返回{@code true}
	 * @since 5.3.19
	 */
	public static boolean isLambdaClass(Class<?> clazz) {
		return (clazz.isSynthetic() && (clazz.getSuperclass() == Object.class) &&
				(clazz.getInterfaces().length > 0) && clazz.getName().contains("$$Lambda"));
	}

	/**
	 * 检查给定对象是否是CGLIB代理。
	 *
	 * @param object 要检查的对象
	 * @see #isCglibProxyClass(Class)
	 * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
	 * @deprecated 自5.2起，建议使用自定义（可能更严格）的检查
	 */
	@Deprecated
	public static boolean isCglibProxy(Object object) {
		return isCglibProxyClass(object.getClass());
	}

	/**
	 * 检查指定类是否是CGLIB生成的类。
	 *
	 * @param clazz 要检查的类
	 * @see #isCglibProxyClassName(String)
	 * @deprecated 自5.2起，建议使用自定义（可能更严格）的检查
	 */
	@Deprecated
	public static boolean isCglibProxyClass(@Nullable Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * 检查指定类名是否是CGLIB生成的类。
	 *
	 * @param className 要检查的类名
	 * @deprecated 自5.2起，建议使用自定义（可能更严格）的检查
	 */
	@Deprecated
	public static boolean isCglibProxyClassName(@Nullable String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * 返回给定实例的用户定义类：通常就是实例的类，
	 * 但对于CGLIB生成的子类则返回原始类。
	 *
	 * @param instance 要检查的实例
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getUserClass(instance.getClass());
	}

	/**
	 * 返回给定类的用户定义的类: 通常只是给定的类，但在CGLIB生成的子类的情况下是原始类。
	 *
	 * @param clazz 要检查的类
	 * @return the user-defined class
	 */
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			//如果类名含有CGLIB的类分隔符，获取该类的父类
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				//如果父类不为空，且该类不是Object类，则返回该父类
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * 返回给定对象类型的描述性名称：通常直接返回类名，
	 * 但对于数组返回组件类型类名+"[]"，
	 * 对于JDK代理类则附加实现的接口列表。
	 *
	 * @param value 要内省的对象
	 * @return 类的限定名称
	 */
	@Nullable
	public static String getDescriptiveType(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			String prefix = clazz.getName() + " implementing ";
			StringJoiner result = new StringJoiner(",", prefix, "");
			for (Class<?> ifc : clazz.getInterfaces()) {
				result.add(ifc.getName());
			}
			return result.toString();
		} else {
			return clazz.getTypeName();
		}
	}

	/**
	 * 检查给定的类是否与用户指定的类型名称匹配。
	 *
	 * @param clazz    要检查的class类
	 * @param typeName 要匹配的类型名称
	 */
	public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
		//匹配类名或者简单类名是否相同
		return (typeName != null &&
				(typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
	}

	/**
	 * 获取不包含包名的类名。
	 *
	 * @param className 要获取短名称的类名
	 * @return 不包含包名的类名
	 * @throws IllegalArgumentException 如果类名为空
	 */
	public static String getShortName(String className) {
		Assert.hasLength(className, "Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	/**
	 * 获取不包含包名的类名。
	 *
	 * @param clazz 要获取短名称的类
	 * @return 不包含包名的类名
	 */
	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	/**
	 * 返回一个 Java 类的小写 JavaBeans 属性格式的短名称。如果是嵌套类，则去除外部类名。
	 *
	 * @param clazz 类
	 * @return 以标准 JavaBeans 属性格式呈现的短名称
	 * @see java.beans.Introspector#decapitalize(String)
	 */
	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return Introspector.decapitalize(shortName);
	}

	/**
	 * 获取类文件名（相对于包路径），例如："String.class"。
	 *
	 * @param clazz 目标类
	 * @return ".class"文件的文件名
	 */
	public static String getClassFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	/**
	 * 获取给定类的包名，例如：{@code java.lang.String} 类返回 "java.lang"。
	 *
	 * @param clazz 目标类
	 * @return 包名，如果是默认包则返回空字符串
	 */
	public static String getPackageName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return getPackageName(clazz.getName());
	}

	/**
	 * 根据全限定类名获取包名，例如：{@code java.lang.String} 返回 "java.lang"。
	 *
	 * @param fqClassName 全限定类名
	 * @return 包名，如果是默认包则返回空字符串
	 */
	public static String getPackageName(String fqClassName) {
		Assert.notNull(fqClassName, "Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	/**
	 * 获取类的限定名称：通常返回类名，对于数组则返回组件类型类名+"[]"。
	 *
	 * @param clazz 目标类
	 * @return 类的限定名称
	 */
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return clazz.getTypeName();
	}

	/**
	 * 返回给定方法的限定名称，由完全限定接口/类名 + "." + 方法名组成。
	 *
	 * @param method 方法对象
	 * @return 方法的限定名称
	 */
	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	/**
	 * 返回给定方法的限定名称，由完全限定接口/类名 + "." + 方法名组成。
	 *
	 * @param method 方法对象
	 * @param clazz  调用该方法的类
	 *              （可为{@code null}，表示使用方法声明的类）
	 * @return 方法的限定名称
	 * @since 4.3.4
	 */
	public static String getQualifiedMethodName(Method method, @Nullable Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	/**
	 * 判断给定类是否具有指定参数类型的公共构造函数。
	 * <p>本质上将{@code NoSuchMethodException}转换为"false"。
	 *
	 * @param clazz      要分析的类
	 * @param paramTypes 方法的参数类型
	 * @return 类是否有对应的构造函数
	 * @see Class#getConstructor
	 */
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * 判断给定类是否具有指定参数类型的公共构造函数，
	 * 如果存在则返回它（否则返回{@code null}）。
	 * <p>本质上将{@code NoSuchMethodException}转换为{@code null}。
	 *
	 * @param clazz      要分析的类
	 * @param paramTypes 方法的参数类型
	 * @return 构造函数，如果找不到则返回{@code null}
	 * @see Class#getConstructor
	 */
	@Nullable
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 判断给定类是否具有指定方法的公共方法。
	 *
	 * @param clazz  要分析的类
	 * @param method 要查找的方法
	 * @return 类是否有对应的方法
	 * @since 5.2.3
	 */
	public static boolean hasMethod(Class<?> clazz, Method method) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(method, "Method must not be null");
		if (clazz == method.getDeclaringClass()) {
			return true;
		}
		String methodName = method.getName();
		Class<?>[] paramTypes = method.getParameterTypes();
		return getMethodOrNull(clazz, methodName, paramTypes) != null;
	}

	/**
	 * 判断给定类是否具有指定签名(方法名和参数类型)的公共方法。
	 * <p>本质上将{@code NoSuchMethodException}转换为"false"。
	 *
	 * @param clazz      要分析的类
	 * @param methodName 方法名
	 * @param paramTypes 方法的参数类型
	 * @return 类是否有对应的方法
	 * @see Class#getMethod
	 */
	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	/**
	 * 判断给定类是否具有指定签名(方法名和参数类型)的公共方法，
	 * 如果存在则返回它（否则抛出{@code IllegalStateException}）。
	 * <p>当未指定参数类型时，仅在存在唯一候选方法时返回，
	 * 即只有一个具有指定名称的公共方法。
	 * <p>本质上将{@code NoSuchMethodException}转换为{@code IllegalStateException}。
	 *
	 * @param clazz      要分析的类
	 * @param methodName 方法名
	 * @param paramTypes 方法的参数类型
	 *                  （可为{@code null}表示接受任何签名）
	 * @return 方法对象（永不为{@code null}）
	 * @throws IllegalStateException 如果未找到方法
	 * @see Class#getMethod
	 */
	public static Method getMethod(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			} catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		} else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			} else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			} else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	/**
	 * 判断给定类是否具有指定签名(方法名和参数类型)的公共方法，
	 * 如果存在则返回它（否则返回{@code null}）。
	 * <p>当未指定参数类型时，仅在存在唯一候选方法时返回，
	 * 即只有一个具有指定名称的公共方法。
	 * <p>本质上将{@code NoSuchMethodException}转换为{@code null}。
	 *
	 * @param clazz      要分析的类
	 * @param methodName 方法名
	 * @param paramTypes 方法的参数类型
	 *                  （可为{@code null}表示接受任何签名）
	 * @return 方法对象，如果未找到则返回{@code null}
	 * @see Class#getMethod
	 */
	@Nullable
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			return getMethodOrNull(clazz, methodName, paramTypes);
		} else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	/**
	 * 返回给定类及其超类的具有给定名称 (具有任何参数类型) 的方法数量。包括非公共方法。
	 *
	 * @param clazz      要检查的类
	 * @param methodName 方法的名称
	 * @return 具有给定名称的方法数量
	 */
	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		int count = 0;
		//获取该类的所有方法
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			//如果方法名相同，则count+1
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		//获取实现的所有接口
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			//递归调用，获取接口相同名称的方法数量，并添加到count中。
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			//如果父类不为空，递归调用，获取父类的相同名称的方法数量，并添加到count中。
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * 判断给定类或其超类中是否至少有一个具有指定名称（任意参数类型）的方法。
	 * 包括非公共方法。
	 *
	 * @param clazz      要检查的类
	 * @param methodName 方法名称
	 * @return 如果至少存在一个指定名称的方法则返回true
	 */
	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	/**
	 * 给定一个可能来自接口的方法，以及当前反射调用中使用的目标类，
	 * 查找对应的目标方法（如果存在）。例如方法可能是{@code IFoo.bar()}，
	 * 而目标类可能是{@code DefaultFoo}。此时，方法可能是{@code DefaultFoo.bar()}。
	 * 这样可以找到该方法上的属性。
	 * <p><b>注意：</b>与{@link org.springframework.aop.support.AopUtils#getMostSpecificMethod}不同，
	 * 此方法<i>不会</i>自动解析桥接方法。如果需要桥接方法解析（例如为了从原始方法定义获取元数据），
	 * 请调用{@link org.springframework.core.BridgeMethodResolver#findBridgedMethod}。
	 * <p><b>注意：</b>从Spring 3.1.1开始，如果Java安全设置不允许反射访问
	 * （例如调用{@code Class#getDeclaredMethods}等），此实现将回退返回原始提供的方法。
	 *
	 * @param method      要调用的方法（可能来自接口）
	 * @param targetClass 当前调用的目标类
	 *                   （可能为{@code null}或甚至未实现该方法）
	 * @return 特定的目标方法，如果{@code targetClass}未实现则返回原始方法
	 * @see #getInterfaceMethodIfPossible(Method, Class)
	 */
	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
		if (targetClass != null && targetClass != method.getDeclaringClass() && isOverridable(method, targetClass)) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					} catch (NoSuchMethodException ex) {
						return method;
					}
				} else {
					Method specificMethod =
							ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			} catch (SecurityException ex) {
				// 安全设置不允许反射访问，回退到原始方法
			}
		}
		return method;
	}

	/**
	 * 为给定方法句柄确定对应的接口方法（如果可能）。
	 * <p>这对于在Jigsaw上获取公共导出类型特别有用，
	 * 可以无需非法访问警告地进行反射调用。
	 *
	 * @param method 要调用的方法（可能来自实现类）
	 * @return 对应的接口方法，如果未找到则返回原始方法
	 * @since 5.1
	 * @deprecated 推荐使用{@link #getInterfaceMethodIfPossible(Method, Class)}
	 */
	@Deprecated
	public static Method getInterfaceMethodIfPossible(Method method) {
		return getInterfaceMethodIfPossible(method, null);
	}

	/**
	 * 为给定方法句柄确定对应的接口方法（如果可能）。
	 * <p>这对于在Jigsaw上获取公共导出类型特别有用，
	 * 可以无需非法访问警告地进行反射调用。
	 *
	 * @param method      要调用的方法（可能来自实现类）
	 * @param targetClass 要检查声明接口的目标类
	 * @return 对应的接口方法，如果未找到则返回原始方法
	 * @see #getMostSpecificMethod
	 * @since 5.3.16
	 */
	public static Method getInterfaceMethodIfPossible(Method method, @Nullable Class<?> targetClass) {
		if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
			return method;
		}
		// 尝试在声明类中查找缓存的接口方法
		Method result = interfaceMethodCache.computeIfAbsent(method,
				key -> findInterfaceMethodIfPossible(key, key.getDeclaringClass(), Object.class));
		if (result == method && targetClass != null) {
			// 尚未找到接口方法 -> 尝试给定的目标类（可能是声明类的子类，
			// 将基类方法延迟绑定到子类声明的接口：
			// 参见例如HashMap.HashIterator.hasNext）
			result = findInterfaceMethodIfPossible(method, targetClass, method.getDeclaringClass());
		}
		return result;
	}

	private static Method findInterfaceMethodIfPossible(Method method, Class<?> startClass, Class<?> endClass) {
		Class<?> current = startClass;
		while (current != null && current != endClass) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				try {
					return ifc.getMethod(method.getName(), method.getParameterTypes());
				} catch (NoSuchMethodException ex) {
					// ignore
				}
			}
			current = current.getSuperclass();
		}
		return method;
	}

	/**
	 * 判断给定方法是否由用户声明或至少指向用户声明的方法。
	 * <p>检查{@link Method#isSynthetic()}（针对实现方法）以及
	 * {@code GroovyObject}接口（针对接口方法；在实现类上，
	 * {@code GroovyObject}方法的实现将被标记为synthetic）。
	 * 注意，尽管是synthetic，桥接方法（{@link Method#isBridge()}）仍被视为
	 * 用户级方法，因为它们最终指向用户声明的泛型方法。
	 *
	 * @param method 要检查的方法
	 * @return 如果方法可视为用户声明则返回{@code true}；否则返回{@code false}
	 */
	public static boolean isUserLevelMethod(Method method) {
		Assert.notNull(method, "Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	/**
	 * 判断给定方法在目标类中是否可被重写。
	 *
	 * @param method      要检查的方法
	 * @param targetClass 要检查的目标类
	 */
	private static boolean isOverridable(Method method, @Nullable Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return (targetClass == null ||
				getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
	}

	/**
	 * 获取类的公共静态方法。
	 *
	 * @param clazz      定义方法的类
	 * @param methodName 静态方法名
	 * @param args       方法的参数类型
	 * @return 静态方法，如果找不到则返回{@code null}
	 * @throws IllegalArgumentException 如果方法名为空或类为null
	 */
	@Nullable
	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}


	@Nullable
	private static Method getMethodOrNull(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		} catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static Set<Method> findMethodCandidatesByName(Class<?> clazz, String methodName) {
		Set<Method> candidates = new HashSet<>(1);
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (methodName.equals(method.getName())) {
				candidates.add(method);
			}
		}
		return candidates;
	}

}
