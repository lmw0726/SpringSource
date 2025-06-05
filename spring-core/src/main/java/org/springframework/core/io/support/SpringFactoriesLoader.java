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

package org.springframework.core.io.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 框架内部使用的通用工厂加载机制。
 *
 * <p>{@code SpringFactoriesLoader} 会从类路径中可能存在于多个 JAR 文件内的
 * {@value #FACTORIES_RESOURCE_LOCATION} 文件中 {@linkplain #loadFactories 加载}
 * 并实例化指定类型的工厂。该 {@code spring.factories} 文件必须为 {@link Properties} 格式，
 * 其中 key 是接口或抽象类的全限定类名，value 是逗号分隔的实现类名列表。例如：
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 * <p>
 * 其中 {@code example.MyService} 是接口名，{@code MyServiceImpl1} 和 {@code MyServiceImpl2}
 * 是两个实现类。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.2
 */
public final class SpringFactoriesLoader {

	/**
	 * 工厂配置文件的位置。
	 * <p>可以存在于多个 JAR 文件中。
	 */
	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";


	private static final Log logger = LogFactory.getLog(SpringFactoriesLoader.class);

	static final Map<ClassLoader, Map<String, List<String>>> cache = new ConcurrentReferenceHashMap<>();


	private SpringFactoriesLoader() {
	}


	/**
	 * 使用给定的类加载器从 {@value #FACTORIES_RESOURCE_LOCATION} 加载并实例化指定类型的
	 * 工厂实现类。
	 * <p>返回的工厂实例会通过 {@link AnnotationAwareOrderComparator} 进行排序。
	 * <p>如果需要自定义实例化策略，可使用 {@link #loadFactoryNames} 获取所有已注册的工厂类名。
	 * <p>从 Spring Framework 5.3 开始，如果为某个工厂类型发现了重复的实现类名，
	 * 则每个重复的实现类只会被实例化一次。
	 *
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param classLoader 用于加载的类加载器（可为 {@code null}，此时使用默认加载器）
	 * @throws IllegalArgumentException 如果某个工厂实现类无法加载，或在实例化过程中出错
	 * @see #loadFactoryNames
	 */
	public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
		Assert.notNull(factoryType, "'factoryType' must not be null");
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
		}
		List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
		if (logger.isTraceEnabled()) {
			logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
		}
		List<T> result = new ArrayList<>(factoryImplementationNames.size());
		for (String factoryImplementationName : factoryImplementationNames) {
			result.add(instantiateFactory(factoryImplementationName, factoryType, classLoaderToUse));
		}
		AnnotationAwareOrderComparator.sort(result);
		return result;
	}

	/**
	 * 使用给定的类加载器从 {@value #FACTORIES_RESOURCE_LOCATION} 加载指定类型的
	 * 工厂实现类的全限定类名。
	 * <p>从 Spring Framework 5.3 开始，如果为给定的工厂类型发现某个实现类名多次，
	 * 则会忽略重复项。
	 *
	 * @param factoryType 表示工厂的接口或抽象类
	 * @param classLoader 用于加载资源的类加载器；为 {@code null} 时使用默认加载器
	 * @throws IllegalArgumentException 如果加载工厂类名时发生错误
	 * @see #loadFactories
	 */
	public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = SpringFactoriesLoader.class.getClassLoader();
		}
		String factoryTypeName = factoryType.getName();
		return loadSpringFactories(classLoaderToUse).getOrDefault(factoryTypeName, Collections.emptyList());
	}

	private static Map<String, List<String>> loadSpringFactories(ClassLoader classLoader) {
		Map<String, List<String>> result = cache.get(classLoader);
		if (result != null) {
			return result;
		}

		result = new HashMap<>();
		try {
			// 从 META-INF/spring.factories 下加载资源URL
			Enumeration<URL> urls = classLoader.getResources(FACTORIES_RESOURCE_LOCATION);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				// 通过URL创建URL资源
				UrlResource resource = new UrlResource(url);
				// 加载URL资源中的属性值对
				Properties properties = PropertiesLoaderUtils.loadProperties(resource);
				for (Map.Entry<?, ?> entry : properties.entrySet()) {
					String factoryTypeName = ((String) entry.getKey()).trim();
					// 根据 , 划分成多个工厂实现类名称
					String[] factoryImplementationNames =
							StringUtils.commaDelimitedListToStringArray((String) entry.getValue());
					for (String factoryImplementationName : factoryImplementationNames) {
						result.computeIfAbsent(factoryTypeName, key -> new ArrayList<>())
								.add(factoryImplementationName.trim());
					}
				}
			}

			// 将所有列表替换为包含唯一元素的不可变列表
			result.replaceAll((factoryType, implementations) -> implementations.stream().distinct()
					.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList)));
			cache.put(classLoader, result);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Unable to load factories from location [" +
					FACTORIES_RESOURCE_LOCATION + "]", ex);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static <T> T instantiateFactory(String factoryImplementationName, Class<T> factoryType, ClassLoader classLoader) {
		try {
			Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementationName, classLoader);
			if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
				throw new IllegalArgumentException(
						"Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
			}
			return (T) ReflectionUtils.accessibleConstructor(factoryImplementationClass).newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException(
				"Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]",
				ex);
		}
	}

}
