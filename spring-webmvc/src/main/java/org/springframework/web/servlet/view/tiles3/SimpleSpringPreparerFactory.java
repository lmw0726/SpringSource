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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.PreparerException;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.preparer.factory.NoSuchPreparerException;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiles {@link org.apache.tiles.preparer.factory.PreparerFactory}实现，期望准备器类名，并为这些类构建准备器实例，
 * 通过Spring ApplicationContext创建它们以应用Spring容器回调和配置的Spring BeanPostProcessors。
 *
 * @author Juergen Hoeller
 * @see SpringBeanPreparerFactory
 * @since 3.2
 */
public class SimpleSpringPreparerFactory extends AbstractSpringPreparerFactory {

	/**
	 * 共享ViewPreparer实例的缓存：bean名称 -> bean实例。
	 */
	private final Map<String, ViewPreparer> sharedPreparers = new ConcurrentHashMap<>(16);


	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		// 首先在并发映射中进行快速检查，最小程度的锁定。
		// 从sharedPreparers映射中获取名为name的ViewPreparer
		ViewPreparer preparer = this.sharedPreparers.get(name);
		// 如果preparer为null
		if (preparer == null) {
			// 同步块，确保线程安全地检查和创建preparer
			synchronized (this.sharedPreparers) {
				// 再次检查preparer是否为null
				preparer = this.sharedPreparers.get(name);
				// 如果preparer仍然为null
				if (preparer == null) {
					try {
						// 尝试加载名为name的类
						Class<?> beanClass = ClassUtils.forName(name, context.getClassLoader());
						// 检查加载的类是否实现了ViewPreparer接口
						if (!ViewPreparer.class.isAssignableFrom(beanClass)) {
							throw new PreparerException(
									"Invalid preparer class [" + name + "]: does not implement ViewPreparer interface");
						}
						// 使用应用上下文的BeanFactory创建beanClass的实例，并将其转换为ViewPreparer类型
						preparer = (ViewPreparer) context.getAutowireCapableBeanFactory().createBean(beanClass);
						// 将创建的preparer存储在sharedPreparers映射中
						this.sharedPreparers.put(name, preparer);
					} catch (ClassNotFoundException ex) {
						// 如果类未找到，则抛出NoSuchPreparerException异常
						throw new NoSuchPreparerException("Preparer class [" + name + "] not found", ex);
					}
				}
			}
		}
		// 返回获取或创建的preparer
		return preparer;
	}

}
