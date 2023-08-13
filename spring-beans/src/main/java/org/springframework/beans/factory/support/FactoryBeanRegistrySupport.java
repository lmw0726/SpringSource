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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.lang.Nullable;

import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support base class for singleton registries which need to handle
 * {@link org.springframework.beans.factory.FactoryBean} instances,
 * integrated with {@link DefaultSingletonBeanRegistry}'s singleton management.
 *
 * <p>Serves as base class for {@link AbstractBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

	/**
	 * FactoryBeans创建的单例对象的缓存: FactoryBean名称到对象。
	 */
	private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);


	/**
	 * 确定给定FactoryBean的类型。
	 *
	 * @param factoryBean 要检查的FactoryBean实例
	 * @return FactoryBean的对象类型，如果还不能确定类型则为{@code null}
	 */
	@Nullable
	protected Class<?> getTypeForFactoryBean(FactoryBean<?> factoryBean) {
		try {
			if (System.getSecurityManager() != null) {
				//通过系统安全管理器，获取他的对象类型。
				return AccessController.doPrivileged(
						(PrivilegedAction<Class<?>>) factoryBean::getObjectType, getAccessControlContext());
			} else {
				return factoryBean.getObjectType();
			}
		} catch (Throwable ex) {
			// 从FactoryBean的getObjectType实现抛出。
			logger.info("FactoryBean threw exception from getObjectType, despite the contract saying " +
					"that it should return null if the type of its object cannot be determined yet", ex);
			return null;
		}
	}

	/**
	 * 从给定的FactoryBean获取要公开的对象 (如果以缓存形式可用)。快速检查最小同步。
	 *
	 * @param beanName bean名称
	 * @return 从FactoryBean获得的对象，如果不可用，则为 {@code null}
	 */
	@Nullable
	protected Object getCachedObjectForFactoryBean(String beanName) {
		return this.factoryBeanObjectCache.get(beanName);
	}

	/**
	 * 从给定的FactoryBean获取要暴露的对象。
	 *
	 * @param factory           FactoryBean实例
	 * @param beanName          bean名称
	 * @param shouldPostProcess 该bean是否需要进行后处理
	 * @return 从FactoryBean获得的对象
	 * @throws BeanCreationException 如果FactoryBean对象创建失败
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
		if (factory.isSingleton() && containsSingleton(beanName)) {
			//如果当前的工厂bean是单例的，并且单例Map中已经含有该bean名称，锁定整个单例Map.
			synchronized (getSingletonMutex()) {
				//从工厂bean实例缓存获取该bean名称对应的实例。
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object != null) {
					//实例不为空，返回该实例。
					return object;
				}
				//从工厂bean中获取该bean名称的实例。
				object = doGetObjectFromFactoryBean(factory, beanName);
				//只有在getObject()调用期间(例如，由于自定义getBean调用触发的循环引用处理)尚未放置的情况下才进行后处理和存储。
				//再次从工厂bean实例缓存中获取bean名称对应的实例
				//这一步的目的是，避免重复的后处理和存储
				Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
				if (alreadyThere != null) {
					//如果该实例存在，直接返回该实例。
					object = alreadyThere;
					return object;
				}


				if (shouldPostProcess) {
					//如果bean需要进行后置处理
					if (isSingletonCurrentlyInCreation(beanName)) {
						// 如果bean名称对应的bean实例正在创建中，返回该对象。
						// 暂时返回非后处理对象，暂不存储 ..
						return object;
					}
					//执行单例创建前的检查
					beforeSingletonCreation(beanName);
					try {
						//从工厂bean后置处理bean对象，并获取处理后的bean实例。
						object = postProcessObjectFromFactoryBean(object, beanName);
					} catch (Throwable ex) {
						//对FactoryBean的单例对象的后处理失败
						throw new BeanCreationException(beanName,
								"Post-processing of FactoryBean's singleton object failed", ex);
					} finally {
						//调用创建单例的回调方法。
						afterSingletonCreation(beanName);
					}
				}
				if (containsSingleton(beanName)) {
					//如果bean名称是单例对象，将bean名称和实例缓存金工厂bean实例缓存中。
					this.factoryBeanObjectCache.put(beanName, object);
				}
				return object;
			}
		} else {
			//从工厂bean中获取该bean名称的实例。
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			if (shouldPostProcess) {
				//如果需要执行后置处理器
				try {
					//执行后置处理，并获取处理后的bean实例。
					object = postProcessObjectFromFactoryBean(object, beanName);
				} catch (Throwable ex) {
					throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}

	/**
	 * 从给定的FactoryBean获取要暴露的对象。
	 *
	 * @param factory  FactoryBean实例
	 * @param beanName bean名称
	 * @return 从FactoryBean获得的对象
	 * @throws BeanCreationException 如果FactoryBean对象创建失败
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	private Object doGetObjectFromFactoryBean(FactoryBean<?> factory, String beanName) throws BeanCreationException {
		Object object;
		try {
			if (System.getSecurityManager() != null) {
				//如果系统安全管理器不为空，获取bean工厂的安全上下文。
				AccessControlContext acc = getAccessControlContext();
				try {
					//授权工厂bean调用获取对象的接口，并获取对象。
					object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
				} catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			} else {
				//直接调用工厂bean的getObject()，获取对象。
				object = factory.getObject();
			}
		} catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		} catch (Throwable ex) {
			throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
		}

		//对于尚未完全初始化的FactoryBean，不要接受null值: 许多FactoryBean只是返回null。
		if (object == null) {
			//如果获取的对象为null，且bean名称对应的实例正在创建中，抛出异常
			if (isSingletonCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(
						beanName, "FactoryBean which is currently in creation returned null from getObject");
			}
			//返回NullBean对象。
			object = new NullBean();
		}
		return object;
	}

	/**
	 * 对已从FactoryBean获得的给定对象进行后处理。
	 * 结果对象将被暴露为bean引用。
	 * <p> 默认实现只是按状态返回给定的对象。子类可能会覆盖此，例如，以应用后处理器。
	 *
	 * @param object   从FactoryBean获得的对象。
	 * @param beanName bean名称
	 * @return 要暴露的对象
	 * @throws org.springframework.beans.BeansException 如果任何后处理失败
	 */
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
		return object;
	}

	/**
	 * Get a FactoryBean for the given bean if possible.
	 *
	 * @param beanName     the name of the bean
	 * @param beanInstance the corresponding bean instance
	 * @return the bean instance as FactoryBean
	 * @throws BeansException if the given bean cannot be exposed as a FactoryBean
	 */
	protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance) throws BeansException {
		if (!(beanInstance instanceof FactoryBean)) {
			throw new BeanCreationException(beanName,
					"Bean instance of type [" + beanInstance.getClass() + "] is not a FactoryBean");
		}
		return (FactoryBean<?>) beanInstance;
	}

	/**
	 * Overridden to clear the FactoryBean object cache as well.
	 */
	@Override
	protected void removeSingleton(String beanName) {
		synchronized (getSingletonMutex()) {
			super.removeSingleton(beanName);
			this.factoryBeanObjectCache.remove(beanName);
		}
	}

	/**
	 * Overridden to clear the FactoryBean object cache as well.
	 */
	@Override
	protected void clearSingletonCache() {
		synchronized (getSingletonMutex()) {
			super.clearSingletonCache();
			this.factoryBeanObjectCache.clear();
		}
	}

	/**
	 * 返回此bean工厂的安全上下文。如果设置了安全管理器，则将使用此方法返回的安全上下文的特权执行与用户代码的交互。
	 *
	 * @see AccessController#getContext()
	 */
	protected AccessControlContext getAccessControlContext() {
		return AccessController.getContext();
	}

}
