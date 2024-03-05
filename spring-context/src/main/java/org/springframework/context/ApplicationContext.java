/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;

/**
 * 提供应用程序配置的中央接口。
 * 在应用程序运行时，此接口是只读的，但如果实现支持，可能会重新加载。
 *
 * <p>ApplicationContext 提供：
 * <ul>
 * <li>用于访问应用程序组件的 Bean 工厂方法。从 {@link org.springframework.beans.factory.ListableBeanFactory} 继承。
 * <li>以通用方式加载文件资源的能力。从 {@link org.springframework.core.io.ResourceLoader} 接口继承。
 * <li>向注册的监听器发布事件的能力。从 {@link ApplicationEventPublisher} 接口继承。
 * <li>解析消息以支持国际化的能力。从 {@link MessageSource} 接口继承。
 * <li>从父上下文继承。子上下文中的定义始终具有优先级。这意味着，例如，整个 Web 应用程序可以使用单个父上下文，而每个 Servlet 都有其自己的子上下文，这些子上下文与任何其他 Servlet 的子上下文都是独立的。
 * </ul>
 *
 * <p>除了标准的 {@link org.springframework.beans.factory.BeanFactory} 生命周期功能外，ApplicationContext 实现还检测并调用 {@link ApplicationContextAware}、{@link ResourceLoaderAware}、{@link ApplicationEventPublisherAware} 和 {@link MessageSourceAware} 等 beans。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ConfigurableApplicationContext
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.core.io.ResourceLoader
 */
public interface ApplicationContext extends EnvironmentCapable, ListableBeanFactory, HierarchicalBeanFactory,
		MessageSource, ApplicationEventPublisher, ResourcePatternResolver {

	/**
	 * 返回此应用程序上下文的唯一标识符。
	 * @return 上下文的唯一标识符，如果没有则为 {@code null}
	 */
	@Nullable
	String getId();

	/**
	 * 返回此上下文所属的部署应用程序的名称。
	 * @return 部署应用程序的名称，或默认情况下为空字符串
	 */
	String getApplicationName();

	/**
	 * 返回此上下文的友好名称。
	 * @return 此上下文的显示名称（永不为 {@code null}）
	 */
	String getDisplayName();

	/**
	 * 返回此上下文首次加载时的时间戳。
	 * @return 此上下文首次加载时的时间戳（以毫秒为单位）
	 */
	long getStartupDate();

	/**
	 * 返回父上下文，如果没有父上下文并且这是上下文层次结构的根，则返回 {@code null}。
	 * @return 父上下文，如果没有父上下文，则返回 {@code null}
	 */
	@Nullable
	ApplicationContext getParent();

	/**
	 * 为此上下文公开 AutowireCapableBeanFactory 功能。
	 * <p>这通常不会被应用程序代码使用，除了为了初始化生活在应用程序上下文之外的 bean 实例，对它们应用 Spring bean 生命周期（完全或部分）。
	 * <p>另外，由 {@link ConfigurableApplicationContext} 接口公开的内部 BeanFactory 也提供了对 {@link AutowireCapableBeanFactory} 接口的访问。
	 * 此方法主要作为 ApplicationContext 接口的一个便利、特定的设施。
	 * <p><b>注意：从 4.2 版本开始，一旦应用程序上下文已关闭，此方法将始终抛出 IllegalStateException。</b>
	 * 在当前的 Spring Framework 版本中，只有可刷新的应用程序上下文才表现出这种行为；从 4.2 版本开始，所有应用程序上下文实现都将被要求遵守。
	 * @return 此上下文的 AutowireCapableBeanFactory
	 * @throws IllegalStateException 如果上下文不支持 {@link AutowireCapableBeanFactory} 接口，
	 * 或者尚未持有自动装配的 bean 工厂（例如，如果从未调用 {@code refresh()}），
	 * 或者上下文已经关闭
	 * @see ConfigurableApplicationContext#refresh()
	 * @see ConfigurableApplicationContext#getBeanFactory()
	 */
	AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException;

}
