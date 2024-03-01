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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内部类，为 Java 类缓存 JavaBeans 的 PropertyDescriptor 信息。不适合应用程序直接使用。
 *
 * <p>在应用程序的 ClassLoader 中缓存 bean 描述符是 Spring 的必要需求，而不是依赖于 JDK 的系统范围的 BeanInfo 缓存
 * （以避免在共享 JVM 中的单个应用程序关闭时发生泄漏）。
 *
 * <p>信息是静态缓存的，因此我们不需要为我们操作的每个 JavaBean 创建新的对象。因此，该类实现了工厂设计模式，
 * 使用私有构造函数和静态 forClass(Class) 工厂方法来获取实例。
 *
 * <p>请注意，为了使缓存有效工作，需要满足一些前提条件：首选安排是 Spring jars 与应用程序类位于同一个 ClassLoader 中，
 * 这允许在任何情况下与应用程序的生命周期一起进行干净的缓存。对于 Web 应用程序，请考虑在 web.xml 中声明本地
 * org.springframework.web.util.IntrospectorCleanupListener，在多 ClassLoader 布局的情况下，这样做将允许进行有效的缓存。
 *
 * <p>如果没有设置清理监听器的非干净 ClassLoader 排列，在垃圾收集器删除它们时，这个类将退回到基于弱引用的缓存模型，
 * 并重新创建许多请求的条目。在这种情况下，请考虑 IGNORE_BEANINFO_PROPERTY_NAME 系统属性。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #acceptClassLoader(ClassLoader)
 * @see #clearClassLoader(ClassLoader)
 * @see #forClass(Class)
 * @since 2001年5月5日
 */
public final class CachedIntrospectionResults {

	/**
	 * 系统属性，指示 Spring 在调用 JavaBeans 的 Introspector 时使用 Introspector.IGNORE_ALL_BEANINFO 模式：
	 * "spring.beaninfo.ignore"，值为 "true" 时跳过搜索 BeanInfo 类（通常是因为应用程序中根本没有为 bean 定义这样的类的场景）。
	 * <p>默认值为 "false"，考虑所有的 BeanInfo 元数据类，就像标准的 Introspector#getBeanInfo(Class) 调用一样。
	 * 如果你在启动时或延迟加载时经历了对不存在的 BeanInfo 类的重复 ClassLoader 访问，请考虑将此标志切换为 "true"。
	 * <p>请注意，这种效果也可能表明缓存的工作效果不好：最好的方式是 Spring jars 与应用程序类位于同一个 ClassLoader 中，
	 * 这样无论如何都可以与应用程序的生命周期一起进行干净的缓存。对于 Web 应用程序，在 web.xml 中声明本地的
	 * org.springframework.web.util.IntrospectorCleanupListener，这样做将允许进行有效的缓存。
	 *
	 * @see Introspector#getBeanInfo(Class, int)
	 */
	public static final String IGNORE_BEANINFO_PROPERTY_NAME = "spring.beaninfo.ignore";

	/**
	 * 空的属性描述符数组
	 */
	private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = {};


	/**
	 * 判断是否应该让 Introspector 忽略 Beaninfo 类。
	 * 获取系统属性 spring.beaninfo.ignore 的布尔值，如果为 true，则表示应该让 Introspector 忽略 Beaninfo 类，
	 * 否则，Introspector 将考虑所有的 BeanInfo 元数据类。
	 */
	private static final boolean shouldIntrospectorIgnoreBeaninfoClasses =
			SpringProperties.getFlag(IGNORE_BEANINFO_PROPERTY_NAME);

	/**
	 * 存储BeanInfoFactory实例
	 */
	private static final List<BeanInfoFactory> beanInfoFactories = SpringFactoriesLoader.loadFactories(
			BeanInfoFactory.class, CachedIntrospectionResults.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(CachedIntrospectionResults.class);

	/**
	 * 这个 CachedIntrospectionResults 类始终接受类的 ClassLoader 集合，
	 * 即使这些类不符合缓存安全的条件。
	 */
	static final Set<ClassLoader> acceptedClassLoaders =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/**
	 * 使用 Class 作为键的 Map，包含 CachedIntrospectionResults，强引用保持。
	 * 这个变体用于缓存安全的 bean 类。
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache =
			new ConcurrentHashMap<>(64);

	/**
	 * 使用 Class 作为键的 Map，包含 CachedIntrospectionResults，软引用保持。
	 * 这个变体用于非缓存安全的 bean 类。
	 */
	static final ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache =
			new ConcurrentReferenceHashMap<>(64);


	/**
	 * 接受给定的 ClassLoader 作为缓存安全，即使它的类在这个 CachedIntrospectionResults 类中不符合缓存安全的条件。
	 * <p>这个配置方法仅适用于 Spring 类位于“共享”ClassLoader（例如系统ClassLoader）的场景，
	 * 其生命周期与应用程序不耦合。在这种情况下，CachedIntrospectionResults 默认不会缓存任何应用程序的类，
	 * 因为它们会在共享的ClassLoader中创建一个泄漏。
	 * <p>在应用程序启动时的任何 acceptClassLoader 调用应该与应用程序关闭时的 clearClassLoader 调用配对。
	 *
	 * @param classLoader 要接受的ClassLoader
	 */
	public static void acceptClassLoader(@Nullable ClassLoader classLoader) {
		if (classLoader != null) {
			acceptedClassLoaders.add(classLoader);
		}
	}

	/**
	 * 清除给定 ClassLoader 的内省缓存，移除该 ClassLoader（及其子类）下所有类的内省结果，
	 * 并从接受列表中移除该 ClassLoader（及其子类）。
	 *
	 * @param classLoader 要清除缓存的ClassLoader
	 */
	public static void clearClassLoader(@Nullable ClassLoader classLoader) {
		acceptedClassLoaders.removeIf(registeredLoader ->
				isUnderneathClassLoader(registeredLoader, classLoader));
		strongClassCache.keySet().removeIf(beanClass ->
				isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
		softClassCache.keySet().removeIf(beanClass ->
				isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
	}

	/**
	 * 为给定的 bean 类创建 CachedIntrospectionResults。
	 *
	 * @param beanClass 要分析的 bean 类
	 * @return 相应的 CachedIntrospectionResults
	 * @throws BeansException 在内省失败的情况下
	 */
	static CachedIntrospectionResults forClass(Class<?> beanClass) throws BeansException {
		// 从 强类型缓存 中获取缓存的 CachedIntrospectionResults
		CachedIntrospectionResults results = strongClassCache.get(beanClass);
		if (results != null) {
			return results;
		}
		// 从 弱类型缓存 中获取缓存的 CachedIntrospectionResults
		results = softClassCache.get(beanClass);
		if (results != null) {
			return results;
		}

		// 如果两个缓存都没有，则创建一个新的 CachedIntrospectionResults 对象
		results = new CachedIntrospectionResults(beanClass);
		// 确定要使用的类缓存
		ConcurrentMap<Class<?>, CachedIntrospectionResults> classCacheToUse;

		// 如果 beanClass 是缓存安全的，或者其类加载器是被接受的，则使用 strongClassCache
		if (ClassUtils.isCacheSafe(beanClass, CachedIntrospectionResults.class.getClassLoader()) ||
				isClassLoaderAccepted(beanClass.getClassLoader())) {
			classCacheToUse = strongClassCache;
		} else {
			// 否则，使用 softClassCache
			if (logger.isDebugEnabled()) {
				logger.debug("Not strongly caching class [" + beanClass.getName() + "] because it is not cache-safe");
			}
			classCacheToUse = softClassCache;
		}

		// 将结果放入类缓存中，如果已存在则返回已存在的结果
		CachedIntrospectionResults existing = classCacheToUse.putIfAbsent(beanClass, results);
		return (existing != null ? existing : results);
	}

	/**
	 * 检查此 CachedIntrospectionResults 类是否配置为接受给定的 ClassLoader。
	 *
	 * @param classLoader 要检查的 ClassLoader
	 * @return 给定的 ClassLoader 是否被接受
	 * @see #acceptClassLoader
	 */
	private static boolean isClassLoaderAccepted(ClassLoader classLoader) {
		// 遍历所有被接受的类加载器
		for (ClassLoader acceptedLoader : acceptedClassLoaders) {
			// 检查当前类加载器是否位于被接受的类加载器的下层，若是，则返回 true
			if (isUnderneathClassLoader(classLoader, acceptedLoader)) {
				return true;
			}
		}
		// 若未找到任何被接受的类加载器，则返回 false
		return false;
	}

	/**
	 * 检查给定的 ClassLoader 是否位于给定的父级之下，即父级是否在候选者的层次结构内。
	 *
	 * @param candidate 要检查的候选 ClassLoader
	 * @param parent    要检查的父级 ClassLoader
	 */
	private static boolean isUnderneathClassLoader(@Nullable ClassLoader candidate, @Nullable ClassLoader parent) {
		// 若待检查的类加载器与父类加载器相同，则返回 true
		if (candidate == parent) {
			return true;
		}
		// 若待检查的类加载器为空，则返回 false
		if (candidate == null) {
			return false;
		}
		// 初始化待检查的类加载器
		ClassLoader classLoaderToCheck = candidate;
		// 循环遍历待检查的类加载器及其父类加载器，直至找到与父类加载器相同的类加载器
		while (classLoaderToCheck != null) {
			classLoaderToCheck = classLoaderToCheck.getParent();
			if (classLoaderToCheck == parent) {
				return true;
			}
		}
		// 若未找到与父类加载器相同的类加载器，则返回 false
		return false;
	}

	/**
	 * 检索给定目标类的 BeanInfo 描述符。
	 *
	 * @param beanClass 要内省的目标类
	 * @return 结果 BeanInfo 描述符（永远不会为 null）
	 * @throws IntrospectionException 来自底层 Introspector 的异常
	 */
	private static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		// 遍历所有的 BeanInfoFactory 实例
		for (BeanInfoFactory beanInfoFactory : beanInfoFactories) {
			// 获取 beanClass 的 BeanInfo
			BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanClass);
			// 如果成功获取到 BeanInfo，则直接返回
			if (beanInfo != null) {
				return beanInfo;
			}
		}
		// 如果 Introspector 忽略 Beaninfo 类，则使用 Introspector.IGNORE_ALL_BEANINFO 获取 BeanInfo，否则直接使用 Introspector.getBeanInfo 获取 BeanInfo
		return (shouldIntrospectorIgnoreBeaninfoClasses ?
				Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO) :
				Introspector.getBeanInfo(beanClass));
	}


	/**
	 * 内省的 bean 类的 BeanInfo 对象。
	 */
	private final BeanInfo beanInfo;

	/**
	 * 以属性名称字符串为键的 PropertyDescriptor 对象。
	 */
	private final Map<String, PropertyDescriptor> propertyDescriptors;

	/**
	 * 以 PropertyDescriptor 为键的 TypeDescriptor 对象。
	 */
	private final ConcurrentMap<PropertyDescriptor, TypeDescriptor> typeDescriptorCache;


	/**
	 * 为给定的类创建一个新的 CachedIntrospectionResults 实例。
	 *
	 * @param beanClass 要分析的 bean 类
	 * @throws BeansException 如果内省失败
	 */
	private CachedIntrospectionResults(Class<?> beanClass) throws BeansException {
		try {
			// 获取 beanClass 的 BeanInfo
			if (logger.isTraceEnabled()) {
				logger.trace("Getting BeanInfo for class [" + beanClass.getName() + "]");
			}
			this.beanInfo = getBeanInfo(beanClass);

			// 缓存 beanClass 的 PropertyDescriptors
			if (logger.isTraceEnabled()) {
				logger.trace("Caching PropertyDescriptors for class [" + beanClass.getName() + "]");
			}
			this.propertyDescriptors = new LinkedHashMap<>();

			// 用于存储已经处理过的 read 方法的方法名
			Set<String> readMethodNames = new HashSet<>();

			// 获取 beanClass 的所有 PropertyDescriptor
			PropertyDescriptor[] pds = this.beanInfo.getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				// 对于特定的类和属性，进行一些特殊处理
				if (Class.class == beanClass && !("name".equals(pd.getName()) ||
						(pd.getName().endsWith("Name") && String.class == pd.getPropertyType()))) {
					// 仅允许类属性的所有名称变体
					continue;
				}
				if (URL.class == beanClass && "content".equals(pd.getName())) {
					// 仅允许URL属性内省，不允许内容解析
					continue;
				}
				if (pd.getWriteMethod() == null && isInvalidReadOnlyPropertyType(pd.getPropertyType())) {
					// 忽略只读属性，如ClassLoader-不需要绑定到那些
					continue;
				}
				// 记录属性描述符，属性名作为 key
				pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
				this.propertyDescriptors.put(pd.getName(), pd);
				// 如果有读方法，记录读方法的方法名
				Method readMethod = pd.getReadMethod();
				if (readMethod != null) {
					readMethodNames.add(readMethod.getName());
				}
			}

			// 检查实现的接口中的 setter/getter 方法
			Class<?> currClass = beanClass;
			while (currClass != null && currClass != Object.class) {
				introspectInterfaces(beanClass, currClass, readMethodNames);
				currClass = currClass.getSuperclass();
			}

			// 检查不带前缀的记录式访问器，例如： “lastName()”
			// -直接引用相同名称的实例字段的访问器方法
			// -与Java 15记录类的组件访问器的相同约定
			introspectPlainAccessors(beanClass, readMethodNames);

			// 创建一个类型描述符缓存
			this.typeDescriptorCache = new ConcurrentReferenceHashMap<>();
		} catch (IntrospectionException ex) {
			throw new FatalBeanException("Failed to obtain BeanInfo for class [" + beanClass.getName() + "]", ex);
		}
	}

	/**
	 * 内省给定的接口，并处理它们的属性方法。
	 *
	 * @param beanClass       目标 bean 类
	 * @param currClass       当前处理的接口
	 * @param readMethodNames 读取方法的名称集合
	 * @throws IntrospectionException 内省异常
	 */
	private void introspectInterfaces(Class<?> beanClass, Class<?> currClass, Set<String> readMethodNames)
			throws IntrospectionException {

		// 遍历当前类实现的接口
		for (Class<?> ifc : currClass.getInterfaces()) {
			// 若接口不是 Java 语言接口，则继续处理
			if (!ClassUtils.isJavaLanguageInterface(ifc)) {
				// 遍历接口的属性描述符
				for (PropertyDescriptor pd : getBeanInfo(ifc).getPropertyDescriptors()) {
					// 获取已存在属性描述符
					PropertyDescriptor existingPd = this.propertyDescriptors.get(pd.getName());
					// 如果已存在的属性描述符为空，或者已存在的属性描述符没有读方法且新的属性描述符存在读方法
					if (existingPd == null ||
							(existingPd.getReadMethod() == null && pd.getReadMethod() != null)) {
						// 使用泛型感知属性描述符创建属性描述符
						// GenericTypeAwarePropertyDescriptor针对声明的read方法从宽地解析set write方法，因此我们更喜欢在这里读取方法描述符。
						pd = buildGenericTypeAwarePropertyDescriptor(beanClass, pd);
						// 如果新的属性描述符的写方法为空且属性类型是无效的只读属性类型，则忽略此属性
						if (pd.getWriteMethod() == null && isInvalidReadOnlyPropertyType(pd.getPropertyType())) {
							// 忽略只读属性，如ClassLoader不需要绑定到那些
							continue;
						}
						// 将新的属性描述符加入到属性描述符映射中
						this.propertyDescriptors.put(pd.getName(), pd);
						// 获取属性描述符的读方法，并将其名称加入到读方法名称集合中
						Method readMethod = pd.getReadMethod();
						if (readMethod != null) {
							readMethodNames.add(readMethod.getName());
						}
					}
				}
				// 递归处理接口的父接口
				introspectInterfaces(ifc, ifc, readMethodNames);
			}
		}
	}

	/**
	 * 内省普通访问器（非 JavaBean 属性访问器），处理给定 bean 类的普通方法。
	 *
	 * @param beanClass       目标 bean 类
	 * @param readMethodNames 读取方法的名称集合
	 * @throws IntrospectionException 内省异常
	 */
	private void introspectPlainAccessors(Class<?> beanClass, Set<String> readMethodNames)
			throws IntrospectionException {

		// 遍历类中的方法
		for (Method method : beanClass.getMethods()) {
			// 如果属性描述符中不包含该方法的名称，并且读方法名称集合中也不包含该方法的名称，并且该方法是普通访问器方法
			if (!this.propertyDescriptors.containsKey(method.getName()) &&
					!readMethodNames.contains(method.getName()) && isPlainAccessor(method)) {
				// 将方法名称作为键，创建一个泛型感知属性描述符并加入到属性描述符映射中
				this.propertyDescriptors.put(method.getName(),
						new GenericTypeAwarePropertyDescriptor(beanClass, method.getName(), method, null, null));
				// 将方法名称加入到读方法名称集合中
				readMethodNames.add(method.getName());
			}
		}

	}

	/**
	 * 检查给定方法是否为普通访问器。
	 *
	 * @param method 要检查的方法
	 * @return 如果是普通访问器，则为 true；否则为 false
	 */
	private boolean isPlainAccessor(Method method) {
		// 检查方法的修饰符是否为静态，或者声明类是否为 Object 类或 Class 类，
		// 或者方法参数数量是否大于 0，或者方法返回类型是否为 void，或者返回类型是否为无效的只读属性类型
		if (Modifier.isStatic(method.getModifiers()) ||
				method.getDeclaringClass() == Object.class || method.getDeclaringClass() == Class.class ||
				method.getParameterCount() > 0 || method.getReturnType() == void.class ||
				isInvalidReadOnlyPropertyType(method.getReturnType())) {
			return false;
		}
		try {
			// 是否是引用同名实例字段的访问器方法？
			method.getDeclaringClass().getDeclaredField(method.getName());
			return true;
		} catch (Exception ex) {
			return false;
		}

	}

	/**
	 * 检查给定的返回类型是否为无效的只读属性类型。
	 *
	 * @param returnType 要检查的返回类型
	 * @return 如果是无效的只读属性类型，则为 true；否则为 false
	 */
	private boolean isInvalidReadOnlyPropertyType(@Nullable Class<?> returnType) {
		// 返回类型不为 null 且是 AutoCloseable 类型、ClassLoader 类型或 ProtectionDomain 类型
		return (returnType != null && (AutoCloseable.class.isAssignableFrom(returnType) ||
				ClassLoader.class.isAssignableFrom(returnType) ||
				ProtectionDomain.class.isAssignableFrom(returnType)));
	}


	BeanInfo getBeanInfo() {
		return this.beanInfo;
	}

	Class<?> getBeanClass() {
		return this.beanInfo.getBeanDescriptor().getBeanClass();
	}

	/**
	 * 根据属性名称获取属性描述符
	 *
	 * @param name 属性名称
	 * @return 属性描述符
	 */
	@Nullable
	PropertyDescriptor getPropertyDescriptor(String name) {
		// 获取给定名称的属性描述符
		PropertyDescriptor pd = this.propertyDescriptors.get(name);
		// 如果属性描述符为 null 并且名称不为空
		if (pd == null && StringUtils.hasLength(name)) {
			// 以宽松的方式回退检查
			// 尝试将属性名的第一个字符小写后再次查找
			pd = this.propertyDescriptors.get(StringUtils.uncapitalize(name));
			// 如果仍然为 null
			if (pd == null) {
				// 尝试将属性名的第一个字符大写后再次查找
				pd = this.propertyDescriptors.get(StringUtils.capitalize(name));
			}
		}
		// 返回属性描述符
		return pd;
	}

	/**
	 * 获取所有的属性描述符
	 *
	 * @return 所有的属性描述符
	 */
	PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors.values().toArray(EMPTY_PROPERTY_DESCRIPTOR_ARRAY);
	}

	/**
	 * 构建具有泛型类型感知能力的 PropertyDescriptor。
	 *
	 * @param beanClass 属性所属的类
	 * @param pd        要构建的 PropertyDescriptor
	 * @return 具有泛型类型感知能力的 PropertyDescriptor
	 * @throws FatalBeanException 如果重新检查类时失败
	 */
	private PropertyDescriptor buildGenericTypeAwarePropertyDescriptor(Class<?> beanClass, PropertyDescriptor pd) {
		try {
			return new GenericTypeAwarePropertyDescriptor(beanClass, pd.getName(), pd.getReadMethod(),
					pd.getWriteMethod(), pd.getPropertyEditorClass());
		} catch (IntrospectionException ex) {
			throw new FatalBeanException("Failed to re-introspect class [" + beanClass.getName() + "]", ex);
		}
	}

	/**
	 * 添加类型描述符
	 *
	 * @param pd 属性描述符
	 * @param td 类型描述符
	 * @return 缓存成功的类型描述符
	 */
	TypeDescriptor addTypeDescriptor(PropertyDescriptor pd, TypeDescriptor td) {
		TypeDescriptor existing = this.typeDescriptorCache.putIfAbsent(pd, td);
		return (existing != null ? existing : td);
	}

	/**
	 * 根据属性描述符获取类型描述符
	 *
	 * @param pd 属性描述符
	 * @return 类型描述符
	 */
	@Nullable
	TypeDescriptor getTypeDescriptor(PropertyDescriptor pd) {
		return this.typeDescriptorCache.get(pd);
	}

}
