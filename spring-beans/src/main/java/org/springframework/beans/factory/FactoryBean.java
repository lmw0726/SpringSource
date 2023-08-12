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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects used within a {@link BeanFactory} which
 * are themselves factories for individual objects. If a bean implements this
 * interface, it is used as a factory for an object to expose, not directly as a
 * bean instance that will be exposed itself.
 *
 * <p><b>NB: A bean that implements this interface cannot be used as a normal bean.</b>
 * A FactoryBean is defined in a bean style, but the object exposed for bean
 * references ({@link #getObject()}) is always the object that it creates.
 *
 * <p>FactoryBeans can support singletons and prototypes, and can either create
 * objects lazily on demand or eagerly on startup. The {@link SmartFactoryBean}
 * interface allows for exposing more fine-grained behavioral metadata.
 *
 * <p>This interface is heavily used within the framework itself, for example for
 * the AOP {@link org.springframework.aop.framework.ProxyFactoryBean} or the
 * {@link org.springframework.jndi.JndiObjectFactoryBean}. It can be used for
 * custom components as well; however, this is only common for infrastructure code.
 *
 * <p><b>{@code FactoryBean} is a programmatic contract. Implementations are not
 * supposed to rely on annotation-driven injection or other reflective facilities.</b>
 * {@link #getObjectType()} {@link #getObject()} invocations may arrive early in the
 * bootstrap process, even ahead of any post-processor setup. If you need access to
 * other beans, implement {@link BeanFactoryAware} and obtain them programmatically.
 *
 * <p><b>The container is only responsible for managing the lifecycle of the FactoryBean
 * instance, not the lifecycle of the objects created by the FactoryBean.</b> Therefore,
 * a destroy method on an exposed bean object (such as {@link java.io.Closeable#close()}
 * will <i>not</i> be called automatically. Instead, a FactoryBean should implement
 * {@link DisposableBean} and delegate any such close call to the underlying object.
 *
 * <p>Finally, FactoryBean objects participate in the containing BeanFactory's
 * synchronization of bean creation. There is usually no need for internal
 * synchronization other than for purposes of lazy initialization within the
 * FactoryBean itself (or the like).
 *
 * @param <T> the bean type
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 * @since 08.03.2003
 */
public interface FactoryBean<T> {

	/**
	 * 在 {@link org.springframework.beans.factory.config.BeanDefinition} 上可以是
	 * {@link org.springframework.core.AttributeAccessor#setAttribute 设置} 的属性的名称，
	 * 以便工厂bean在无法从工厂bean类推导出时，可以标志其对象类型。
	 *
	 * @since 5.2
	 */
	String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";


	/**
	 * 返回此工厂管理的对象的实例 (可能是共享的或独立的)。
	 * <p> 与 {@link BeanFactory} 一样，这允许同时支持单例和原型设计模式。
	 * <p> 如果此FactoryBean在调用时尚未完全初始化 (例如，因为它涉及循环引用)，则抛出相应的 {@link FactoryBeanNotInitializedException}。
	 * <p> 从Spring 2.0开始，允许FactoryBeans返回 {@code null} 对象。
	 * 工厂将此视为要使用的正常值; 在这种情况下，它将不再抛出FactoryBeanNotInitializedException。
	 * 鼓励FactoryBean实现现在酌情自行抛出FactoryBeanNotInitializedException。
	 *
	 * @return bean的实例 (可以是 {@code null})
	 * @throws Exception 在创建错误的情况下
	 * @see FactoryBeanNotInitializedException
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * 返回此FactoryBean创建的对象类型，如果事先不知道，则返回 {@code null}。
	 * <p> 这允许人们检查特定类型的bean，而无需实例化对象，例如在自动执行中。
	 * <p> 对于正在创建单例对象的实现，此方法应尽量避免单例创建。它应该事先估计类型。对于原型，也建议在此返回有意义的类型。
	 * <p> 此方法可以在 <i> 此FactoryBean已完全初始化之前调用 </i>。
	 * 它不能依赖于在初始化期间创建的状态; 当然，如果可用，它仍然可以使用这种状态。
	 * <p><b> 注意:</b> 自动执行将简单地忽略此处返回 {@code null} 的FactoryBeans。
	 * 因此，强烈建议使用FactoryBean的当前状态正确实现此方法。
	 *
	 * @return 此FactoryBean创建的对象类型，如果在调用时未知，则为 {@code null}
	 * @see ListableBeanFactory#getBeansOfType
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * 这个工厂管理的对象是单例吗？也就是说，{@link #getObject()} 是否总是返回相同的对象 (可以缓存的引用)？
	 * <p><b> 注意:</b> 如果FactoryBean指示保存一个单例对象，则从 {@code getObject()} 返回的对象可能会被拥有的BeanFactory缓存。
	 * 因此，除非FactoryBean始终公开相同的引用，否则不要返回 {@code true}。
	 * <p> FactoryBean本身的单例状态通常由拥有的BeanFactory提供; 通常，它必须在那里定义为单例。
	 * <p><b> 注意:<b> 此方法返回 {@code false} 并不一定指示返回的对象是独立的实例。
	 * 扩展的 {@link SmartFactoryBean} 接口的实现可以通过其 {@link SmartFactoryBean#isPrototype()} 方法显式指示独立实例。
	 * 如果 {@code isSingleton()} 实现返回 {@code false}，则简单地假定不实现此扩展接口的纯 {@link FactoryBean} 实现始终返回独立实例。
	 * <p> 默认实现返回 {@code true}，因为 {@code FactoryBean} 通常管理一个单例实例。
	 *
	 * @return 暴露的对象是否为单例
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 */
	default boolean isSingleton() {
		return true;
	}

}
