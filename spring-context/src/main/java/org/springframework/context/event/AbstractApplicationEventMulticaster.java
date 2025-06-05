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

package org.springframework.context.event;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * {@link ApplicationEventMulticaster} 接口的抽象实现，提供基本的监听器注册功能。
 *
 * <p>默认情况下，不允许相同监听器的多个实例，因为它将监听器保存在一个链式集合（Set）中。
 * 用于保存 {@link ApplicationListener} 对象的集合类可以通过 "collectionClass" Bean 属性进行覆盖。
 *
 * <p>{@link ApplicationEventMulticaster} 接口的实际 {@link #multicastEvent} 方法的实现由子类完成。
 * {@link SimpleApplicationEventMulticaster} 简单地将所有事件广播给所有已注册的监听器，在调用线程中执行它们。
 * 替代的实现可以在这些方面更加复杂。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 1.2.3
 * @see #getApplicationListeners(ApplicationEvent, ResolvableType)
 * @see SimpleApplicationEventMulticaster
 */
public abstract class AbstractApplicationEventMulticaster
		implements ApplicationEventMulticaster, BeanClassLoaderAware, BeanFactoryAware {

	private final DefaultListenerRetriever defaultRetriever = new DefaultListenerRetriever();

	final Map<ListenerCacheKey, CachedListenerRetriever> retrieverCache = new ConcurrentHashMap<>(64);

	@Nullable
	private ClassLoader beanClassLoader;

	@Nullable
	private ConfigurableBeanFactory beanFactory;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Not running in a ConfigurableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		if (this.beanClassLoader == null) {
			this.beanClassLoader = this.beanFactory.getBeanClassLoader();
		}
	}

	private ConfigurableBeanFactory getBeanFactory() {
		if (this.beanFactory == null) {
			throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
					"because it is not associated with a BeanFactory");
		}
		return this.beanFactory;
	}


	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.defaultRetriever) {
			// 如果已注册，则显式删除代理的目标，
			// 以避免对同一侦听器的双重调用。
			Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);
			if (singletonTarget instanceof ApplicationListener) {
				this.defaultRetriever.applicationListeners.remove(singletonTarget);
			}
			this.defaultRetriever.applicationListeners.add(listener);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void addApplicationListenerBean(String listenerBeanName) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.add(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.remove(listener);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListenerBean(String listenerBeanName) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.remove(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.removeIf(predicate);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListenerBeans(Predicate<String> predicate) {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListenerBeans.removeIf(predicate);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeAllListeners() {
		synchronized (this.defaultRetriever) {
			this.defaultRetriever.applicationListeners.clear();
			this.defaultRetriever.applicationListenerBeans.clear();
			this.retrieverCache.clear();
		}
	}


	/**
	 * 返回包含所有 {@link ApplicationListener} 的集合。
	 * @return 一个包含所有 {@link ApplicationListener} 的集合
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners() {
		synchronized (this.defaultRetriever) {
			return this.defaultRetriever.getApplicationListeners();
		}
	}

	/**
	 * 返回匹配给定事件类型的 {@link ApplicationListener} 集合。
	 * 非匹配的监听器会被提前排除。
	 * @param event 要传播的事件。允许基于缓存的匹配信息提前排除非匹配的监听器。
	 * @param eventType 事件类型
	 * @return 匹配的 {@link ApplicationListener} 集合
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners(
			ApplicationEvent event, ResolvableType eventType) {

		Object source = event.getSource();
		Class<?> sourceType = (source != null ? source.getClass() : null);
		ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

		// 潜在的新检索器，用于填充缓存
		CachedListenerRetriever newRetriever = null;

		// 快速检查 ConcurrentHashMap 中是否存在现有条目
		CachedListenerRetriever existingRetriever = this.retrieverCache.get(cacheKey);
		if (existingRetriever == null) {
			// 如果可能，缓存一个新的 ListenerRetriever
			if (this.beanClassLoader == null ||
					(ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
							(sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
				newRetriever = new CachedListenerRetriever();
				existingRetriever = this.retrieverCache.putIfAbsent(cacheKey, newRetriever);
				if (existingRetriever != null) {
					newRetriever = null;  // 不需要在 retrieveApplicationListeners 中填充它
				}
			}
		}

		if (existingRetriever != null) {
			Collection<ApplicationListener<?>> result = existingRetriever.getApplicationListeners();
			if (result != null) {
				return result;
			}
			// 如果结果为 null，说明现有的检索器尚未被其他线程完全填充。
			// 对于当前的本地尝试，继续执行，就像无法缓存一样。
		}

		return retrieveApplicationListeners(eventType, sourceType, newRetriever);
	}

	/**
	 * 实际检索给定事件类型和源类型的应用监听器。
	 * @param eventType 事件类型
	 * @param sourceType 事件源类型
	 * @param retriever 如果需要填充（用于缓存目的）的 ListenerRetriever
	 * @return 给定事件和源类型的预过滤应用监听器列表
	 */
	private Collection<ApplicationListener<?>> retrieveApplicationListeners(
			ResolvableType eventType, @Nullable Class<?> sourceType, @Nullable CachedListenerRetriever retriever) {

		List<ApplicationListener<?>> allListeners = new ArrayList<>();
		Set<ApplicationListener<?>> filteredListeners = (retriever != null ? new LinkedHashSet<>() : null);
		Set<String> filteredListenerBeans = (retriever != null ? new LinkedHashSet<>() : null);

		Set<ApplicationListener<?>> listeners;
		Set<String> listenerBeans;
		synchronized (this.defaultRetriever) {
			listeners = new LinkedHashSet<>(this.defaultRetriever.applicationListeners);
			listenerBeans = new LinkedHashSet<>(this.defaultRetriever.applicationListenerBeans);
		}

		// 添加以编程方式注册的监听器，
		// 包括来自 ApplicationListenerDetector 的监听器（单例 Bean 和内部 Bean）
		for (ApplicationListener<?> listener : listeners) {
			if (supportsEvent(listener, eventType, sourceType)) {
				if (retriever != null) {
					filteredListeners.add(listener);
				}
				allListeners.add(listener);
			}
		}

		// 添加通过 Bean 名称注册的监听器，可能与上述以编程方式注册的监听器重叠，
		// 但这里可能包含额外的元数据
		if (!listenerBeans.isEmpty()) {
			ConfigurableBeanFactory beanFactory = getBeanFactory();
			for (String listenerBeanName : listenerBeans) {
				try {
					if (supportsEvent(beanFactory, listenerBeanName, eventType)) {
						ApplicationListener<?> listener =
								beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (!allListeners.contains(listener) && supportsEvent(listener, eventType, sourceType)) {
							if (retriever != null) {
								if (beanFactory.isSingleton(listenerBeanName)) {
									filteredListeners.add(listener);
								}
								else {
									filteredListenerBeans.add(listenerBeanName);
								}
							}
							allListeners.add(listener);
						}
					}
					else {
						// 移除最初来自 ApplicationListenerDetector 的非匹配监听器，
						// 可能由于额外的 BeanDefinition 元数据（例如工厂方法泛型）而被排除
						Object listener = beanFactory.getSingleton(listenerBeanName);
						if (retriever != null) {
							filteredListeners.remove(listener);
						}
						allListeners.remove(listener);
					}
				}
				catch (NoSuchBeanDefinitionException ex) {
					// 单例监听器实例（没有支持的 Bean 定义）消失了 - 可能正处于销毁阶段
				}
			}
		}

		AnnotationAwareOrderComparator.sort(allListeners);
		if (retriever != null) {
			if (filteredListenerBeans.isEmpty()) {
				retriever.applicationListeners = new LinkedHashSet<>(allListeners);
				retriever.applicationListenerBeans = filteredListenerBeans;
			}
			else {
				retriever.applicationListeners = filteredListeners;
				retriever.applicationListenerBeans = filteredListenerBeans;
			}
		}
		return allListeners;
	}

	/**
	 * 通过检查泛型声明的事件类型，在尝试实例化之前，提前过滤由 Bean 定义的监听器。
	 * <p>如果此方法在第一次检查时对给定监听器返回 {@code true}，则随后将通过
	 * {@link #supportsEvent(ApplicationListener, ResolvableType, Class)} 方法检索监听器实例并进行全面评估。
	 * @param beanFactory 包含监听器 Bean 的 BeanFactory
	 * @param listenerBeanName BeanFactory 中 Bean 的名称
	 * @param eventType 要检查的事件类型
	 * @return 给定监听器是否应包含在给定事件类型的候选者中
	 * @see #supportsEvent(Class, ResolvableType)
	 * @see #supportsEvent(ApplicationListener, ResolvableType, Class)
	 */
	private boolean supportsEvent(
			ConfigurableBeanFactory beanFactory, String listenerBeanName, ResolvableType eventType) {

		// 获取监听器的类型
		Class<?> listenerType = beanFactory.getType(listenerBeanName);
		// 如果无法确定类型，或监听器实现了 GenericApplicationListener 或 SmartApplicationListener 接口，则认为支持事件
		if (listenerType == null || GenericApplicationListener.class.isAssignableFrom(listenerType) ||
				SmartApplicationListener.class.isAssignableFrom(listenerType)) {
			return true;
		}
		// 如果监听器类型不支持该事件类型，则返回 false
		if (!supportsEvent(listenerType, eventType)) {
			return false;
		}
		try {
			// 获取合并后的 BeanDefinition
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(listenerBeanName);
			// 获取监听器泛型声明的事件类型
			ResolvableType genericEventType = bd.getResolvableType().as(ApplicationListener.class).getGeneric();
			// 如果未声明泛型或泛型事件类型是 eventType 的父类或相同类型，则认为支持事件
			return (genericEventType == ResolvableType.NONE || genericEventType.isAssignableFrom(eventType));
		}
		catch (NoSuchBeanDefinitionException ex) {
			// 忽略 - 无需检查手动注册的单例的可解析类型
			return true;
		}
	}

	/**
	 * 在尝试实例化监听器之前，通过检查其泛型声明的事件类型，提前过滤监听器。
	 * <p>如果此方法在第一次检查时对给定监听器返回 {@code true}，则随后将通过
	 * {@link #supportsEvent(ApplicationListener, ResolvableType, Class)} 方法检索监听器实例并进行全面评估。
	 * @param listenerType 由 BeanFactory 确定的监听器类型
	 * @param eventType 要检查的事件类型
	 * @return 给定监听器是否应包含在给定事件类型的候选者中
	 */
	protected boolean supportsEvent(Class<?> listenerType, ResolvableType eventType) {
		// 解析监听器声明的事件类型
		ResolvableType declaredEventType = GenericApplicationListenerAdapter.resolveDeclaredEventType(listenerType);
		// 如果未声明事件类型，或声明的事件类型是当前事件类型的父类或相同类型，则认为支持事件
		return (declaredEventType == null || declaredEventType.isAssignableFrom(eventType));
	}

	/**
	 * 确定给定的监听器是否支持给定的事件。
	 * <p>默认实现检测 {@link SmartApplicationListener} 和 {@link GenericApplicationListener} 接口。
	 * 如果是标准的 {@link ApplicationListener}，则会使用 {@link GenericApplicationListenerAdapter}
	 * 来反射目标监听器的泛型声明类型。
	 * @param listener 要检查的目标监听器
	 * @param eventType 要检查的事件类型
	 * @param sourceType 要检查的源类型
	 * @return 给定的监听器是否应包含在给定事件类型的候选者中
	 */
	protected boolean supportsEvent(
			ApplicationListener<?> listener, ResolvableType eventType, @Nullable Class<?> sourceType) {

		// 将监听器转换为 GenericApplicationListener
		GenericApplicationListener smartListener = (listener instanceof GenericApplicationListener ?
				(GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener));

		// 检查监听器是否支持事件类型和源类型
		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
	}


	/**
	 * 基于事件类型和源类型的ListenerRetrievers的缓存键。
	 */
	private static final class ListenerCacheKey implements Comparable<ListenerCacheKey> {

		private final ResolvableType eventType;

		@Nullable
		private final Class<?> sourceType;

		public ListenerCacheKey(ResolvableType eventType, @Nullable Class<?> sourceType) {
			Assert.notNull(eventType, "Event type must not be null");
			this.eventType = eventType;
			this.sourceType = sourceType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ListenerCacheKey)) {
				return false;
			}
			ListenerCacheKey otherKey = (ListenerCacheKey) other;
			return (this.eventType.equals(otherKey.eventType) &&
					ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType));
		}

		@Override
		public int hashCode() {
			return this.eventType.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.sourceType);
		}

		@Override
		public String toString() {
			return "ListenerCacheKey [eventType = " + this.eventType + ", sourceType = " + this.sourceType + "]";
		}

		@Override
		public int compareTo(ListenerCacheKey other) {
			int result = this.eventType.toString().compareTo(other.eventType.toString());
			if (result == 0) {
				if (this.sourceType == null) {
					return (other.sourceType == null ? 0 : -1);
				}
				if (other.sourceType == null) {
					return 1;
				}
				result = this.sourceType.getName().compareTo(other.sourceType.getName());
			}
			return result;
		}
	}


	/**
	 * 封装一组特定目标监听器的辅助类，
	 * 允许高效地检索预过滤的监听器。
	 * <p>该辅助类的实例会根据事件类型和源类型进行缓存。
	 */
	private class CachedListenerRetriever {

		@Nullable
		public volatile Set<ApplicationListener<?>> applicationListeners;

		@Nullable
		public volatile Set<String> applicationListenerBeans;

		@Nullable
		public Collection<ApplicationListener<?>> getApplicationListeners() {
			Set<ApplicationListener<?>> applicationListeners = this.applicationListeners;
			Set<String> applicationListenerBeans = this.applicationListenerBeans;
			if (applicationListeners == null || applicationListenerBeans == null) {
				// 尚未完全填充
				return null;
			}

			List<ApplicationListener<?>> allListeners = new ArrayList<>(
					applicationListeners.size() + applicationListenerBeans.size());
			allListeners.addAll(applicationListeners);
			if (!applicationListenerBeans.isEmpty()) {
				BeanFactory beanFactory = getBeanFactory();
				for (String listenerBeanName : applicationListenerBeans) {
					try {
						allListeners.add(beanFactory.getBean(listenerBeanName, ApplicationListener.class));
					}
					catch (NoSuchBeanDefinitionException ex) {
						// 如果没有找到对应的 Bean 定义，捕获异常
						// 可能是因为该单例监听器实例在销毁阶段被移除
					}
				}
			}
			if (!applicationListenerBeans.isEmpty()) {
				AnnotationAwareOrderComparator.sort(allListeners);
			}
			return allListeners;
		}
	}


	/**
	 * 封装一组通用目标监听器的辅助类。
	 */
	private class DefaultListenerRetriever {

		public final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

		public final Set<String> applicationListenerBeans = new LinkedHashSet<>();

		public Collection<ApplicationListener<?>> getApplicationListeners() {
			List<ApplicationListener<?>> allListeners = new ArrayList<>(
					this.applicationListeners.size() + this.applicationListenerBeans.size());
			allListeners.addAll(this.applicationListeners);
			if (!this.applicationListenerBeans.isEmpty()) {
				BeanFactory beanFactory = getBeanFactory();
				for (String listenerBeanName : this.applicationListenerBeans) {
					try {
						ApplicationListener<?> listener =
								beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (!allListeners.contains(listener)) {
							allListeners.add(listener);
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						// 单例监听器实例（没有支持的 Bean 定义）消失了 -
						// 可能正处于销毁阶段
					}
				}
			}
			AnnotationAwareOrderComparator.sort(allListeners);
			return allListeners;
		}
	}

}
