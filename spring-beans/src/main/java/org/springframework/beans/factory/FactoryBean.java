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
 * 由{@link BeanFactory}中使用的对象实现的接口，它们本身是单个对象的工厂。
 * 如果一个bean实现了这个接口，它将被用作要公开的对象的工厂，而不是直接作为将被公开的bean实例。
 *
 * <p><b>NB: 实现此接口的bean不能作为普通bean使用。</b>
 * FactoryBean是以bean样式定义的，但用于bean引用的对象({@link #getObject()})始终是它创建的对象。
 *
 * <p>FactoryBeans可以支持单例和原型，并且可以根据需要延迟或在启动时急切地创建对象。
 * {@link SmartFactoryBean}接口允许公开更精细的行为元数据。
 *
 * <p>这个接口在框架内部被广泛使用，例如对于AOP {@link org.springframework.aop.framework.ProxyFactoryBean}
 * 或{@link org.springframework.jndi.JndiObjectFactoryBean}。
 * 它也可以用于自定义组件；然而，这只是基础代码中的常见情况。
 *
 * <p><b>{@code FactoryBean}是一种编程合约。 实现不应依赖于基于注解的注入或其他反射工具。</b>
 * {@link #getObjectType()} {@link #getObject()}调用可能会在引导过程的早期出现，
 * 甚至可能出现在任何后处理器设置之前。 如果需要访问其他bean，请实现{@link BeanFactoryAware}并以编程方式获取它们。
 *
 * <p><b>容器只负责管理FactoryBean实例的生命周期，而不是FactoryBean创建的对象的生命周期。</b>
 * 因此，对于公开的bean对象(例如{@link java.io.Closeable#close()}，将不会自动调用销毁方法。
 * 相反，FactoryBean应该实现{@link DisposableBean}并将任何这样的关闭调用委托给底层对象。
 *
 * <p>最后，FactoryBean对象参与包含BeanFactory的bean创建的同步。 通常，
 * 除了在FactoryBean本身(或类似的情况下)进行延迟初始化之外，没有内部同步的必要。
 *
 * @param <T> bean的类型
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
