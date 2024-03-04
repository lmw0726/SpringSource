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

package org.springframework.beans.factory;

/**
 * 在 {@link BeanFactory} 启动期间的单例预实例化阶段结束时触发的回调接口。可以由单例 bean 实现此接口，
 * 以在常规单例实例化算法之后执行一些初始化操作，避免因意外早期初始化（例如 {@link ListableBeanFactory#getBeansOfType} 调用）而产生的副作用。
 * 在这个意义上，它是 {@link InitializingBean} 的一个替代，后者在 bean 的本地构造阶段结束时触发。
 *
 * <p>这个回调变体在某种程度上类似于 {@link org.springframework.context.event.ContextRefreshedEvent}，但不需要实现
 * {@link org.springframework.context.ApplicationListener}，也不需要在上下文层次结构中过滤上下文引用等。
 * 它还意味着对仅 {@code beans} 包的更小依赖，并且由独立的 {@link ListableBeanFactory} 实现来支持，而不仅仅是在
 * {@link org.springframework.context.ApplicationContext} 环境中。
 *
 * <p><b>注意:</b> 如果您打算启动/管理异步任务，请优先实现 {@link org.springframework.context.Lifecycle}，
 * 它提供了更丰富的运行时管理模型，并允许分阶段的启动/关闭。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface SmartInitializingSingleton {

	/**
	 * 在单例预实例化阶段的最后，保证所有常规单例 bean 已经创建。在此方法内的 {@link ListableBeanFactory#getBeansOfType} 调用
	 * 不会在引导期间触发意外副作用。
	 * <p><b>注意:</b> 此回调不会触发延迟初始化的单例 bean，它们是在 {@link BeanFactory} 引导后按需懒初始化的，并且也不会
	 * 对任何其他 bean 作用域触发。仅对具有预期引导语义的 bean 小心使用它。
	 */
	void afterSingletonsInstantiated();

}
