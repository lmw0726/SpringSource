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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

/**
 * 静态 {@link org.springframework.beans.factory.BeanFactory} 实现，
 * 允许以编程方式注册现有的单例实例。
 *
 * <p>不支持原型 bean 或别名。
 *
 * <p>作为 {@link org.springframework.beans.factory.ListableBeanFactory} 接口的简单实现示例，
 * 管理现有的 bean 实例而不是基于 bean 定义创建新的 bean，并且不实现任何扩展 SPI 接口
 * （例如 {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}）。
 *
 * <p>要使用基于 bean 定义的完整工厂，请参阅 {@link DefaultListableBeanFactory}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see DefaultListableBeanFactory
 * @since 06.01.2003
 */
public class StaticListableBeanFactory implements ListableBeanFactory {

	/**
	 * 从bean名称映射到bean实例。
	 */
	private final Map<String, Object> beans;


	/**
	 * 创建一个常规的 {@code StaticListableBeanFactory}，通过 {@link #addBean} 调用来填充单例 bean 实例。
	 */
	public StaticListableBeanFactory() {
		this.beans = new LinkedHashMap<>();
	}

	/**
	 * 创建一个 {@code StaticListableBeanFactory} 包装给定的 {@code Map}。
	 * <p>请注意，给定的 {@code Map} 可能已经预先填充了 bean；
	 * 或者是新的，仍然允许通过 {@link #addBean} 注册 bean；
	 * 或者是 {@link java.util.Collections#emptyMap()}，用于对空 bean 集合进行操作的虚拟工厂。
	 *
	 * @param beans 用于保存该工厂的 bean 的 {@code Map}，以 bean 名称作为键，相应的单例对象作为值
	 * @since 4.3
	 */
	public StaticListableBeanFactory(Map<String, Object> beans) {
		Assert.notNull(beans, "Beans Map must not be null");
		this.beans = beans;
	}


	/**
	 * 添加一个新的单例 bean。
	 * <p>将覆盖给定名称的任何现有实例。
	 *
	 * @param name bean 的名称
	 * @param bean bean 实例
	 */
	public void addBean(String name, Object bean) {
		this.beans.put(name, bean);
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		// 获取规范的bean名称
		String beanName = BeanFactoryUtils.transformedBeanName(name);
		// 根据 beanName 从 beans 中获取 bean
		Object bean = this.beans.get(beanName);

		if (bean == null) {
			// 如果 bean 为 null，则抛出 NoSuchBeanDefinitionException 异常
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}

		// 如果该名称是工厂取消引用，但 bean 不是工厂，则抛出 BeanIsNotAFactoryException 异常
		if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, bean.getClass());
		}

		if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			// 如果 bean 是 FactoryBean，并且该名称不是工厂取消引用
			try {
				// 尝试获取 FactoryBean 中的暴露对象
				Object exposedObject = ((FactoryBean<?>) bean).getObject();
				if (exposedObject == null) {
					// 如果 暴露对象 不存在，则抛出 BeanCreationException 异常
					throw new BeanCreationException(beanName, "FactoryBean exposed null object");
				}
				return exposedObject;
			} catch (Exception ex) {
				// 如果 FactoryBean 在创建对象时抛出异常，则抛出 BeanCreationException 异常
				throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
			}
		} else {
			// 如果 bean 不是 FactoryBean，则直接返回 bean
			return bean;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, @Nullable Class<T> requiredType) throws BeansException {
		// 获取指定名称的 bean
		Object bean = getBean(name);

		// 如果 存在所需类型 ，并且 bean 不是 所需类型 的实例，则抛出 BeanNotOfRequiredTypeException 异常
		if (requiredType != null && !requiredType.isInstance(bean)) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
		}

		// 将 bean 强制转换为 T 类型，并返回
		return (T) bean;
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(name);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		// 获取指定类型的所有 bean 的名称数组
		String[] beanNames = getBeanNamesForType(requiredType);

		if (beanNames.length == 1) {
			// 如果只有一个匹配的 bean，则返回该 bean
			return getBean(beanNames[0], requiredType);
		} else if (beanNames.length > 1) {
			// 如果有多个匹配的 bean，则抛出 NoUniqueBeanDefinitionException 异常
			throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
		} else {
			// 如果没有匹配的 bean，则抛出 NoSuchBeanDefinitionException 异常
			throw new NoSuchBeanDefinitionException(requiredType);
		}
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		if (!ObjectUtils.isEmpty(args)) {
			throw new UnsupportedOperationException(
					"StaticListableBeanFactory does not support explicit bean creation arguments");
		}
		return getBean(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
		return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(requiredType, true);
	}

	@Override
	public boolean containsBean(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		// 获取指定名称的 bean
		Object bean = getBean(name);

		// 如果 bean 是 FactoryBean 类型，则返回创建的对象的单例状态
		if (bean instanceof FactoryBean) {
			return ((FactoryBean<?>) bean).isSingleton();
		}

		// 如果不是 FactoryBean，则默认为单例
		return true;
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		// 获取指定名称的 bean
		Object bean = getBean(name);

		// 如果 bean 是 SmartFactoryBean，并且是原型类型，则返回 true；
		// 或者如果 bean 是 FactoryBean，并且不是单例类型，则返回 true
		return ((bean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) bean).isPrototype()) ||
				(bean instanceof FactoryBean && !((FactoryBean<?>) bean).isSingleton()));
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		// 获取指定名称的 bean 的类型
		Class<?> type = getType(name);

		// 如果类型不为 null，并且可以分配给指定的 typeToMatch 类型，则返回 true
		return (type != null && typeToMatch.isAssignableFrom(type));
	}

	@Override
	public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		// 获取指定名称的 bean 的类型
		Class<?> type = getType(name);

		// 如果 typeToMatch 为 null，或者类型不为 null 且可以分配给指定的 typeToMatch 类型，则返回 true
		return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(name, true);
	}

	@Override
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		// 转换 bean 名称
		String beanName = BeanFactoryUtils.transformedBeanName(name);

		// 从 bean 容器中获取指定名称的 bean 实例
		Object bean = this.beans.get(beanName);

		// 如果 bean 为空，则抛出 NoSuchBeanDefinitionException 异常
		if (bean == null) {
			throw new NoSuchBeanDefinitionException(beanName,
					"Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
		}

		// 如果 bean 是 FactoryBean 且该名称不是工厂取消引用，则返回 FactoryBean 创建的对象类型
		if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
			return ((FactoryBean<?>) bean).getObjectType();
		}

		// 如果不是 FactoryBean，则返回其类的类型
		return bean.getClass();
	}

	@Override
	public String[] getAliases(String name) {
		return new String[0];
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String name) {
		return this.beans.containsKey(name);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.beans.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return StringUtils.toStringArray(this.beans.keySet());
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		return new ObjectProvider<T>() {
			@Override
			public T getObject() throws BeansException {
				// 获取指定类型的所有 bean 名称
				String[] beanNames = getBeanNamesForType(requiredType);

				// 如果找到唯一一个匹配的 bean，则返回该 bean
				if (beanNames.length == 1) {
					return (T) getBean(beanNames[0], requiredType);
				} else if (beanNames.length > 1) {
					// 如果找到多个匹配的 bean，则抛出 NoUniqueBeanDefinitionException 异常
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				} else {
					// 如果找不到匹配的 bean，则抛出 NoSuchBeanDefinitionException 异常
					throw new NoSuchBeanDefinitionException(requiredType);
				}
			}

			@Override
			public T getObject(Object... args) throws BeansException {
				// 获取指定类型的所有 bean 名称
				String[] beanNames = getBeanNamesForType(requiredType);

				if (beanNames.length == 1) {
					// 如果找到唯一一个匹配的 bean，则根据参数获取该 bean，并返回
					return (T) getBean(beanNames[0], args);
				} else if (beanNames.length > 1) {
					// 如果找到多个匹配的 bean，则抛出 NoUniqueBeanDefinitionException 异常
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				} else {
					// 如果找不到匹配的 bean，则抛出 NoSuchBeanDefinitionException 异常
					throw new NoSuchBeanDefinitionException(requiredType);
				}
			}

			@Override
			@Nullable
			public T getIfAvailable() throws BeansException {
				// 获取指定类型的所有 bean 名称
				String[] beanNames = getBeanNamesForType(requiredType);

				if (beanNames.length == 1) {
					// 如果找到唯一一个匹配的 bean，则根据该 bean 名称获取该 bean，并返回
					return (T) getBean(beanNames[0]);
				} else if (beanNames.length > 1) {
					// 如果找到多个匹配的 bean，则抛出 NoUniqueBeanDefinitionException 异常
					throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
				} else {
					// 如果找不到匹配的 bean，则返回 null
					return null;
				}
			}

			@Override
			@Nullable
			public T getIfUnique() throws BeansException {
				// 获取指定类型的所有 bean 名称
				String[] beanNames = getBeanNamesForType(requiredType);

				if (beanNames.length == 1) {
					// 如果找到唯一一个匹配的 bean，则根据该 bean 名称获取该 bean，并返回
					return (T) getBean(beanNames[0]);
				} else {
					// 如果找不到或者找到多个匹配的 bean，则返回 null
					return null;
				}
			}

			@Override
			public Stream<T> stream() {
				return Arrays.stream(getBeanNamesForType(requiredType)).map(name -> (T) getBean(name));
			}

			@Override
			public Stream<T> orderedStream() {
				return stream().sorted(OrderComparator.INSTANCE);
			}
		};
	}

	@Override
	public String[] getBeanNamesForType(@Nullable ResolvableType type) {
		return getBeanNamesForType(type, true, true);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable ResolvableType type,
										boolean includeNonSingletons, boolean allowEagerInit) {

		// 解析类型
		Class<?> resolved = (type != null ? type.resolve() : null);
		// 判断是否为 FactoryBean 类型
		boolean isFactoryType = resolved != null && FactoryBean.class.isAssignableFrom(resolved);
		// 存储匹配的 bean 名称列表
		List<String> matches = new ArrayList<>();

		// 遍历所有的 bean 实例
		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			// 获取 bean 名称
			String beanName = entry.getKey();
			// 获取 bean 实例
			Object beanInstance = entry.getValue();

			if (beanInstance instanceof FactoryBean && !isFactoryType) {
				// 如果 bean 实例是 FactoryBean 且不是 FactoryBean 类型
				FactoryBean<?> factoryBean = (FactoryBean<?>) beanInstance;
				// 获取 FactoryBean 创建的对象类型
				Class<?> objectType = factoryBean.getObjectType();
				// 如果包括非单例或者 FactoryBean 是单例，且对象类型不为空且匹配要求，则加入匹配列表
				if ((includeNonSingletons || factoryBean.isSingleton()) &&
						objectType != null && (type == null || type.isAssignableFrom(objectType))) {
					matches.add(beanName);
				}
			} else {
				// 如果 bean 实例的类型为空或者匹配要求，则加入匹配列表
				if (type == null || type.isInstance(beanInstance)) {
					matches.add(beanName);
				}
			}
		}

		// 将匹配列表转换为字符串数组并返回
		return StringUtils.toStringArray(matches);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		return getBeanNamesForType(ResolvableType.forClass(type));
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return getBeanNamesForType(ResolvableType.forClass(type), includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		// 判断是否为 FactoryBean 类型
		boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
		// 存储匹配的 bean 名称与实例的映射关系
		Map<String, T> matches = new LinkedHashMap<>();

		// 遍历所有的 bean 实例
		for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
			// 获取 bean 名称
			String beanName = entry.getKey();
			// 获取 bean 实例
			Object beanInstance = entry.getValue();

			// 如果 bean 实例是 FactoryBean 且不是 FactoryBean 类型
			if (beanInstance instanceof FactoryBean && !isFactoryType) {
				FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
				// 获取 FactoryBean 创建的对象类型
				Class<?> objectType = factory.getObjectType();
				// 如果包括非单例或者 FactoryBean 是单例，且对象类型不为空且匹配要求，则加入匹配列表
				if ((includeNonSingletons || factory.isSingleton()) &&
						objectType != null && (type == null || type.isAssignableFrom(objectType))) {
					matches.put(beanName, getBean(beanName, type));
				}
			} else {
				if (type == null || type.isInstance(beanInstance)) {
					// 如果 bean 实例的类型为空或者bean实例是该类型的实例
					if (isFactoryType) {
						// 如果匹配的类型是 FactoryBean，返回 FactoryBean 本身；
						beanName = FACTORY_BEAN_PREFIX + beanName;
					}
					//添加进匹配列表
					matches.put(beanName, (T) beanInstance);
				}
			}
		}

		// 返回匹配列表
		return matches;
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		// 存储匹配的 bean 名称的列表
		List<String> results = new ArrayList<>();

		// 遍历所有的 bean 名称
		for (String beanName : this.beans.keySet()) {
			// 如果 bean 上存在指定类型的注解，则将其加入结果列表
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.add(beanName);
			}
		}

		// 将结果列表转换为字符串数组并返回
		return StringUtils.toStringArray(results);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		// 存储匹配的 bean 名称及其对应的实例的映射
		Map<String, Object> results = new LinkedHashMap<>();

		// 遍历所有的 bean 名称
		for (String beanName : this.beans.keySet()) {
			// 如果 bean 上存在指定类型的注解，则将其加入结果映射中
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				// 将 bean 名称和对应的实例加入结果映射中
				results.put(beanName, getBean(beanName));
			}
		}

		// 返回结果映射
		return results;
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		return findAnnotationOnBean(beanName, annotationType, true);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(
			String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {

		// 获取指定 bean 的类型
		Class<?> beanType = getType(beanName, allowFactoryBeanInit);

		// 如果类型不为空，则查找并返回类型上的合并注解
		return (beanType != null ? AnnotatedElementUtils.findMergedAnnotation(beanType, annotationType) : null);
	}

}
