/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jndi.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.ResolvableType;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.jndi.TypeMismatchNamingException;
import org.springframework.lang.Nullable;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.util.*;

/**
 * Spring的简单基于JNDI的 {@link org.springframework.beans.factory.BeanFactory} 接口的实现。
 * 不支持枚举 Bean 定义，因此不实现 {@link org.springframework.beans.factory.ListableBeanFactory} 接口。
 *
 * <p>此工厂将给定的 Bean 名称解析为 Java EE 应用程序的 "java:comp/env/" 命名空间中的 JNDI 名称。
 * 它缓存了所有获取对象的已解析类型，并且可选择地也缓存可共享的对象（如果它们显式标记为 {@link #addShareableResource shareable resource}）。
 *
 * <p>此工厂的主要目的是与 Spring 的 {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor} 结合使用，
 * 配置为 {@code @Resource} 注解的 JNDI 对象的解析器，而不需要中间 Bean 定义。
 * 当然，如果需要 BeanFactory 风格的类型检查，它也可以用于类似的查找场景。
 *
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
 * @since 2.5
 */
public class SimpleJndiBeanFactory extends JndiLocatorSupport implements BeanFactory {

	/**
	 * 已知可共享的资源的 JNDI 名称集合
	 */
	private final Set<String> shareableResources = new HashSet<>();

	/**
	 * 可共享的单例对象的缓存：Bean 名称到 Bean 实例的映射
	 */
	private final Map<String, Object> singletonObjects = new HashMap<>();

	/**
	 * 非共享资源类型的缓存：Bean 名称到 Bean 类型的映射
	 */
	private final Map<String, Class<?>> resourceTypes = new HashMap<>();


	public SimpleJndiBeanFactory() {
		setResourceRef(true);
	}


	/**
	 * 添加一个可共享的 JNDI 资源的名称，
	 * 一旦获取到，此工厂就允许缓存它。
	 *
	 * @param shareableResource JNDI 名称（通常位于 "java:comp/env/" 命名空间）
	 */
	public void addShareableResource(String shareableResource) {
		this.shareableResources.add(shareableResource);
	}

	/**
	 * 设置一个可共享的 JNDI 资源名称列表，
	 * 一旦获取到，此工厂就允许缓存它。
	 *
	 * @param shareableResources JNDI 名称（通常位于 "java:comp/env/" 命名空间）
	 */
	public void setShareableResources(String... shareableResources) {
		Collections.addAll(this.shareableResources, shareableResources);
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------


	@Override
	public Object getBean(String name) throws BeansException {
		return getBean(name, Object.class);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		try {
			// 检查 bean 是否为单例
			if (isSingleton(name)) {
				// 如果是单例，则调用 doGetSingleton 方法获取单例 bean
				return doGetSingleton(name, requiredType);
			} else {
				// 如果不是单例，则调用 lookup 方法进行查找
				return lookup(name, requiredType);
			}
		} catch (NameNotFoundException ex) {
			// 如果在 JNDI 环境中找不到指定的 bean，则抛出 NoSuchBeanDefinitionException 异常
			throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
		} catch (TypeMismatchNamingException ex) {
			// 如果 JNDI 环境中的 bean 类型与要求的类型不匹配，则抛出 BeanNotOfRequiredTypeException 异常
			throw new BeanNotOfRequiredTypeException(name, ex.getRequiredType(), ex.getActualType());
		} catch (NamingException ex) {
			// 如果 JNDI 查找过程中发生命名异常，则抛出 BeanDefinitionStoreException 异常
			throw new BeanDefinitionStoreException("JNDI environment", name, "JNDI lookup failed", ex);
		}
	}

	@Override
	public Object getBean(String name, @Nullable Object... args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"SimpleJndiBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(requiredType.getSimpleName(), requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
		if (args != null) {
			throw new UnsupportedOperationException(
					"SimpleJndiBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		return new ObjectProvider<T>() {
			@Override
			public T getObject() throws BeansException {
				return getBean(requiredType);
			}

			@Override
			public T getObject(Object... args) throws BeansException {
				return getBean(requiredType, args);
			}

			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				try {
					return getBean(requiredType);
				} catch (NoUniqueBeanDefinitionException ex) {
					throw ex;
				} catch (NoSuchBeanDefinitionException ex) {
					return null;
				}
			}

			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				try {
					return getBean(requiredType);
				} catch (NoSuchBeanDefinitionException ex) {
					return null;
				}
			}
		};
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		throw new UnsupportedOperationException(
				"SimpleJndiBeanFactory does not support resolution by ResolvableType");
	}

	@Override
	public boolean containsBean(String name) {
		if (this.singletonObjects.containsKey(name) || this.resourceTypes.containsKey(name)) {
			// 如果指定名称的 bean 存在于单例对象映射或资源类型映射中，则返回 true
			return true;
		}
		try {
			// 否则，尝试获取指定名称的类型
			doGetType(name);
			// 如果能够成功获取类型，则返回 true
			return true;
		} catch (NamingException ex) {
			// 如果获取类型过程中发生命名异常，则返回 false
			return false;
		}
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return this.shareableResources.contains(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return !this.shareableResources.contains(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (type != null && typeToMatch.isAssignableFrom(type));
	}

	@Override
	public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(name);
		return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		try {
			return doGetType(name);
		} catch (NameNotFoundException ex) {
			throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
		} catch (NamingException ex) {
			return null;
		}
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	@SuppressWarnings("unchecked")
	private <T> T doGetSingleton(String name, @Nullable Class<T> requiredType) throws NamingException {
		synchronized (this.singletonObjects) {
			// 从单例对象映射中获取指定名称的 bean 实例
			Object singleton = this.singletonObjects.get(name);
			// 如果能够找到指定名称的单例对象
			if (singleton != null) {
				// 如果指定了期望的类型，并且单例对象的类型不符合要求，则抛出类型不匹配的异常
				if (requiredType != null && !requiredType.isInstance(singleton)) {
					throw new TypeMismatchNamingException(convertJndiName(name), requiredType, singleton.getClass());
				}
				// 返回单例对象
				return (T) singleton;
			}
			// 否则，通过 JNDI 进行查找并获取指定名称的对象
			T jndiObject = lookup(name, requiredType);
			// 将查找到的对象放入单例对象映射中
			this.singletonObjects.put(name, jndiObject);
			// 返回查找到的对象
			return jndiObject;
		}
	}

	private Class<?> doGetType(String name) throws NamingException {
		if (isSingleton(name)) {
			// 如果指定名称的 bean 是单例的，则直接从单例对象映射中获取其类型
			return doGetSingleton(name, null).getClass();
		} else {
			synchronized (this.resourceTypes) {
				// 否则，通过 JNDI 查找指定名称的资源类型
				Class<?> type = this.resourceTypes.get(name);
				// 如果资源类型为空，则进行 JNDI 查找，并将其类型放入资源类型映射中
				if (type == null) {
					type = lookup(name, null).getClass();
					this.resourceTypes.put(name, type);
				}
				// 返回资源类型
				return type;
			}
		}
	}

}
