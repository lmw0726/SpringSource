/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Default implementation of the {@link NamespaceHandlerResolver} interface.
 * Resolves namespace URIs to implementation classes based on the mappings
 * contained in mapping file.
 *
 * <p>By default, this implementation looks for the mapping file at
 * {@code META-INF/spring.handlers}, but this can be changed using the
 * {@link #DefaultNamespaceHandlerResolver(ClassLoader, String)} constructor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see NamespaceHandler
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 */
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

	/**
	 * The location to look for the mapping files. Can be present in multiple JAR files.
	 */
	public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";


	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * ClassLoader to use for NamespaceHandler classes.
	 */
	@Nullable
	private final ClassLoader classLoader;

	/**
	 * Resource location to search for.
	 */
	private final String handlerMappingsLocation;

	/**
	 * Stores the mappings from namespace URI to NamespaceHandler class name / instance.
	 */
	@Nullable
	private volatile Map<String, Object> handlerMappings;


	/**
	 * Create a new {@code DefaultNamespaceHandlerResolver} using the
	 * default mapping file location.
	 * <p>This constructor will result in the thread context ClassLoader being used
	 * to load resources.
	 *
	 * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
	 */
	public DefaultNamespaceHandlerResolver() {
		this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * 使用默认映射文件位置创建一个新的 {@code DefaultNamespaceHandlerResolver}。
	 *
	 * @param classLoader 用于加载映射资源的类加载器实例，（可能会为{@code null}）,在这种情况下，将使用线程上下文类加载器)
	 * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
	 */
	public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader) {
		this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * 使用提供的映射文件位置创建一个新的 {@code DefaultNamespaceHandlerResolver}。
	 *
	 * @param classLoader             用于加载映射资源的类加载器实例，（可能会为{@code null}）,在这种情况下，将使用线程上下文类加载器)
	 * @param handlerMappingsLocation 映射文件位置
	 */
	public DefaultNamespaceHandlerResolver(@Nullable ClassLoader classLoader, String handlerMappingsLocation) {
		Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
		//如果类加载器为空，则使用默认的类加载器，即当前线程的类加载器，否则使用当前类加载器实例
		this.classLoader = (classLoader == null ? ClassUtils.getDefaultClassLoader() : classLoader);
		this.handlerMappingsLocation = handlerMappingsLocation;
	}


	/**
	 * 从配置的映射中找到提供的命名空间URI的 {@link NamespaceHandler}。
	 *
	 * @param namespaceUri 相关的命名空间URI
	 * @return 找到的 {@link NamespaceHandler}, 如果没有找到则为 {@code null}
	 */
	@Override
	@Nullable
	public NamespaceHandler resolve(String namespaceUri) {
		//获取处理器映射
		Map<String, Object> handlerMappings = getHandlerMappings();
		//获取处理者或者类名
		Object handlerOrClassName = handlerMappings.get(namespaceUri);
		if (handlerOrClassName == null) {
			//如果为空，则返回null
			return null;
		} else if (handlerOrClassName instanceof NamespaceHandler) {
			//如果是NamespaceHandler类型，强转后直接返回
			return (NamespaceHandler) handlerOrClassName;
		} else {
			//转为类名
			String className = (String) handlerOrClassName;
			try {
				//根据类名进行反射，获取类型
				Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
				if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
					throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
							"] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
				}
				//创建NamespaceHandler实例对象
				NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
				//初始化命名空间处理者
				namespaceHandler.init();
				//添加到handlerMappings 中
				handlerMappings.put(namespaceUri, namespaceHandler);
				//返回命名空间处理者
				return namespaceHandler;
			} catch (ClassNotFoundException ex) {
				throw new FatalBeanException("Could not find NamespaceHandler class [" + className +
						"] for namespace [" + namespaceUri + "]", ex);
			} catch (LinkageError err) {
				throw new FatalBeanException("Unresolvable class definition for NamespaceHandler class [" +
						className + "] for namespace [" + namespaceUri + "]", err);
			}
		}
	}

	/**
	 * 惰性加载指定命名空间处理者映射
	 */
	private Map<String, Object> getHandlerMappings() {
		Map<String, Object> handlerMappings = this.handlerMappings;
		if (handlerMappings == null) {
			synchronized (this) {
				handlerMappings = this.handlerMappings;
				if (handlerMappings == null) {
					//采用双重检测初始化handlerMappings
					if (logger.isTraceEnabled()) {
						logger.trace("Loading NamespaceHandler mappings from [" + this.handlerMappingsLocation + "]");
					}
					try {
						//加载命名空间处理者
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
						if (logger.isTraceEnabled()) {
							logger.trace("Loaded NamespaceHandler mappings: " + mappings);
						}
						handlerMappings = new ConcurrentHashMap<>(mappings.size());
						//将mappings复制到handlerMappings中
						CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
						//替换全局的命名空间处理者映射
						this.handlerMappings = handlerMappings;
					} catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
					}
				}
			}
		}
		return handlerMappings;
	}


	@Override
	public String toString() {
		return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
	}

}
