/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.objenesis;

import org.springframework.core.SpringProperties;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;
import org.springframework.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Spring特有的{@link ObjenesisStd}/{@link ObjenesisBase}变体，
 * 提供基于{@code Class}键而非类名的缓存，
 * 并允许选择性使用缓存。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #isWorthTrying()
 * @see #newInstance(Class, boolean)
 */
public class SpringObjenesis implements Objenesis {

	/**
	 * 指示Spring忽略Objenesis的系统属性，甚至不尝试使用它。
	 * 将此标志设置为"true"等同于让Spring在运行时发现Objenesis无法工作，
	 * 立即触发后备代码路径：最重要的是，这意味着所有CGLIB AOP代理
	 * 将通过默认构造函数的常规实例化创建。
	 */
	public static final String IGNORE_OBJENESIS_PROPERTY_NAME = "spring.objenesis.ignore";


	private final InstantiatorStrategy strategy;

	private final ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>> cache =
			new ConcurrentReferenceHashMap<>();

	private volatile Boolean worthTrying;


	/**
	 * 使用标准实例化策略创建新的{@code SpringObjenesis}实例。
	 */
	public SpringObjenesis() {
		this(null);
	}

	/**
	 * 使用给定的标准实例化策略创建新的{@code SpringObjenesis}实例。
	 * @param strategy 要使用的实例化策略
	 */
	public SpringObjenesis(InstantiatorStrategy strategy) {
		this.strategy = (strategy != null ? strategy : new StdInstantiatorStrategy());

		// 预先评估"spring.objenesis.ignore"属性...
		if (SpringProperties.getFlag(SpringObjenesis.IGNORE_OBJENESIS_PROPERTY_NAME)) {
			this.worthTrying = Boolean.FALSE;
		}
	}


	/**
	 * 返回此Objenesis实例是否值得尝试创建实例，
	 * 即是否尚未使用或已知可以工作。
	 * <p>如果配置的Objenesis实例化策略已被确定在当前JVM上完全不起作用，
	 * 或者"spring.objenesis.ignore"属性已设置为"true"，
	 * 则此方法返回{@code false}。
	 */
	public boolean isWorthTrying() {
		return (this.worthTrying != Boolean.FALSE);
	}

	/**
	 * 通过Objenesis创建给定类的新实例。
	 * @param clazz 要创建实例的类
	 * @param useCache 是否使用实例化器缓存
	 *        (通常为{@code true}，但可设置为{@code false}，
	 *        例如对于可重载类)
	 * @return 新实例(永远不会为{@code null})
	 * @throws ObjenesisException 如果实例创建失败
	 */
	public <T> T newInstance(Class<T> clazz, boolean useCache) {
		if (!useCache) {
			return newInstantiatorOf(clazz).newInstance();
		}
		return getInstantiatorOf(clazz).newInstance();
	}

	public <T> T newInstance(Class<T> clazz) {
		return getInstantiatorOf(clazz).newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
		ObjectInstantiator<?> instantiator = this.cache.get(clazz);
		if (instantiator == null) {
			ObjectInstantiator<T> newInstantiator = newInstantiatorOf(clazz);
			instantiator = this.cache.putIfAbsent(clazz, newInstantiator);
			if (instantiator == null) {
				instantiator = newInstantiator;
			}
		}
		return (ObjectInstantiator<T>) instantiator;
	}

	protected <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> clazz) {
		Boolean currentWorthTrying = this.worthTrying;
		try {
			ObjectInstantiator<T> instantiator = this.strategy.newInstantiatorOf(clazz);
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.TRUE;
			}
			return instantiator;
		}
		catch (ObjenesisException ex) {
			if (currentWorthTrying == null) {
				Throwable cause = ex.getCause();
				if (cause instanceof ClassNotFoundException || cause instanceof IllegalAccessException) {
					// 表示所选的实例化策略在当前JVM上不起作用。
					// 通常是初始化默认SunReflectionFactoryInstantiator失败。
					// 假设后续任何使用Objenesis的尝试都会失败...
					this.worthTrying = Boolean.FALSE;
				}
			}
			throw ex;
		}
		catch (NoClassDefFoundError err) {
			// 在Google App Engine生产环境中发生，由于受限的"sun.reflect.ReflectionFactory"类...
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.FALSE;
			}
			throw new ObjenesisException(err);
		}
	}

}
