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

package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * 实现 {@link BeanDefinitionReader} 接口的 bean 定义读取器的抽象基类。
 *
 * <p>提供一些公共属性，例如要操作的 bean 工厂，以及用于加载 bean 类的类加载器。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanDefinitionReaderUtils
 * @since 11.12.2003
 */
public abstract class AbstractBeanDefinitionReader implements BeanDefinitionReader, EnvironmentCapable {

	/**
	 * 子类可用的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private final BeanDefinitionRegistry registry;

	@Nullable
	private ResourceLoader resourceLoader;

	@Nullable
	private ClassLoader beanClassLoader;

	private Environment environment;

	private BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.INSTANCE;


	/**
	 * 为给定的bean工厂创建一个新的AbstractBeanDefinitionReader。
	 * <p> 如果传入的bean工厂不仅实现了BeanDefinitionRegistry接口，而且实现了ResourceLoader接口，它也将用作默认的ResourceLoader。
	 * {@link org.springframework.context.ApplicationContext} 实现通常是这种情况。
	 * <p> 如果给定一个普通的BeanDefinitionRegistry，则默认的ResourceLoader将是 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。
	 * <p> 如果传入的bean工厂也实现了 {@link EnvironmentCapable}，则该阅读器将使用其环境。
	 * 否则，阅读器将初始化并使用 {@link StandardEnvironment}。
	 * 所有ApplicationContext实现都是环境保护的，而普通的BeanFactory实现不是。
	 *
	 * @param registry 以BeanDefinitionRegistry的形式将bean定义加载到其中的BeanFactory
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		// 确定要使用的资源。
		if (this.registry instanceof ResourceLoader) {
			this.resourceLoader = (ResourceLoader) this.registry;
		} else {
			this.resourceLoader = new PathMatchingResourcePatternResolver();
		}

		// 如果注册器是EnvironmentCapable子类，继承环境
		if (this.registry instanceof EnvironmentCapable) {
			this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
		} else {
			this.environment = new StandardEnvironment();
		}
	}


	/**
	 * 返回用于注册 bean 定义的 bean 工厂。
	 * <p>该工厂通过 BeanDefinitionRegistry 接口暴露，封装了与 bean 定义处理相关的方法。
	 *
	 * @deprecated 自 Spring Framework 5.3.15 起弃用，建议使用 {@link #getRegistry()}，将在 Spring Framework 6.0 中移除
	 */
	@Deprecated
	public final BeanDefinitionRegistry getBeanFactory() {
		return this.registry;
	}

	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置用于资源路径的 ResourceLoader。
	 * 如果指定的是 ResourcePatternResolver，则 bean 定义读取器将能够把资源路径模式解析为 Resource 数组。
	 * <p>默认值为 PathMatchingResourcePatternResolver，它也通过 ResourcePatternResolver 接口支持资源模式解析。
	 * <p>将其设置为 {@code null} 表示该 bean 定义读取器不支持绝对路径资源加载。
	 *
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Nullable
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 设置用于加载 bean 类的 ClassLoader。
	 * <p>默认值为 {@code null}，表示不应急于加载 bean 类，而应仅使用类名注册 bean 定义，
	 * 对应的类将在后续解析（或永不解析）。
	 *
	 * @see Thread#getContextClassLoader()
	 */
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * 设置读取 bean 定义时要使用的 Environment。通常用于解析 profile 信息，
	 * 以确定哪些 bean 定义应被读取，哪些应被忽略。
	 */
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置用于匿名 bean（未显式指定 bean 名称）的 BeanNameGenerator。
	 * <p>默认值为 {@link DefaultBeanNameGenerator}。
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : DefaultBeanNameGenerator.INSTANCE);
	}

	@Override
	public BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}


	@Override
	public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
		Assert.notNull(resources, "Resource array must not be null");
		int count = 0;
		for (Resource resource : resources) {
			count += loadBeanDefinitions(resource);
		}
		return count;
	}

	@Override
	public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(location, null);
	}

	/**
	 * 从指定的资源位置加载bean定义。
	 * <p>位置也可以是位置模式，前提是此bean定义读取器的ResourceLoader是ResourcePatternResolver。
	 *
	 * @param location        要与此bean定义阅读器的ResourceLoader (或ResourcePatternResolver) 一起加载的资源位置
	 * @param actualResources 要填充有在加载过程中已解析的实际资源对象的集合。
	 *                        可能是 {@code null}，以指示调用者对那些资源对象不感兴趣。
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
		//获取资源加载器
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			//如果资源加载器为空，则抛出BeanDefinitionStoreException
			throw new BeanDefinitionStoreException(
					"Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		if (resourceLoader instanceof ResourcePatternResolver) {
			//如果该资源加载器是ResourcePatternResolver
			// 资源模式匹配可用。
			try {
				//强转为ResourcePatternResolver，获取该位置模式的资源
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
				//根据多个资源加载bean定义，并返回加载的Bean定义个数
				int count = loadBeanDefinitions(resources);
				if (actualResources != null) {
					Collections.addAll(actualResources, resources);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
				}
				return count;
			} catch (IOException ex) {
				throw new BeanDefinitionStoreException(
						"Could not resolve bean definition resource pattern [" + location + "]", ex);
			}
		} else {
			// 只能通过绝对URL加载单个资源。
			Resource resource = resourceLoader.getResource(location);
			//根据资源加载bean定义
			int count = loadBeanDefinitions(resource);
			if (actualResources != null) {
				actualResources.add(resource);
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
			}
			return count;
		}
	}

	@Override
	public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
		Assert.notNull(locations, "Location array must not be null");
		int count = 0;
		for (String location : locations) {
			count += loadBeanDefinitions(location);
		}
		return count;
	}

}
