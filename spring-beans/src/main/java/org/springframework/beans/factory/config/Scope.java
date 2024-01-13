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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;

/**
 * 由{@link ConfigurableBeanFactory}使用的策略接口，
 * 代表一个目标作用域，用于保存bean实例。
 * 这允许扩展BeanFactory的标准作用域
 * {@link ConfigurableBeanFactory#SCOPE_SINGLETON "singleton"} 和
 * {@link ConfigurableBeanFactory#SCOPE_PROTOTYPE "prototype"}，
 * 通过为特定键注册自定义进一步的作用域，
 * {@link ConfigurableBeanFactory#registerScope(String, Scope)}。
 *
 * <p>{@link org.springframework.context.ApplicationContext}实现，
 * 如{@link org.springframework.web.context.WebApplicationContext}，
 * 可以根据此Scope SPI注册特定于其环境的附加标准作用域，
 * 例如{@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST "request"}
 * 和{@link org.springframework.web.context.WebApplicationContext#SCOPE_SESSION "session"}。
 *
 * <p>即使其主要用途是在web环境中扩展作用域，
 * 这个SPI是完全通用的：它提供了从任何底层存储机制（例如HTTP会话或自定义对话机制）获取和放置对象的能力。
 * 传递给此类的{@code get}和{@code remove}方法的名称将标识当前作用域中的目标对象。
 *
 * <p>{@code Scope}实现应该是线程安全的。
 * 一个{@code Scope}实例可以同时与多个bean工厂一起使用（如果需要的话，除非它明确希望知道包含的BeanFactory），
 * 可以有任意数量的线程从任意数量的工厂并发访问{@code Scope}。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 * @see org.springframework.web.context.request.RequestScope
 * @see org.springframework.web.context.request.SessionScope
 * @since 2.0
 */
public interface Scope {

	/**
	 * 从底层作用域中返回具有给定名称的对象，
	 * 如果在底层存储机制中找不到，则{@link org.springframework.beans.factory.ObjectFactory#getObject()创建它}。
	 * <p>这是Scope的中心操作，也是唯一绝对必需的操作。
	 *
	 * @param name          要检索的对象的名称
	 * @param objectFactory 用于在底层存储机制中不存在时创建作用域对象的{@link ObjectFactory}
	 * @return 所需的对象（永不为{@code null}）
	 * @throws IllegalStateException 如果底层作用域当前不活动
	 */
	Object get(String name, ObjectFactory<?> objectFactory);

	/**
	 * 从底层作用域中删除具有给定{@code name}的对象。
	 * <p>如果未找到对象，则返回{@code null}；否则返回删除的{@code Object}。
	 * <p>注意，实现还应该删除指定对象的已注册销毁回调（如果有）。
	 * 但是，在这种情况下，它<i>不需要</i>执行已注册的销毁回调，
	 * 因为对象将由调用者销毁（如果适用）。
	 * <p><b>注意：这是一个可选操作。</b>如果实现不支持显式删除对象，
	 * 则实现可能抛出{@link UnsupportedOperationException}。
	 *
	 * @param name 要删除的对象的名称
	 * @return 已删除的对象，如果没有对象存在则为{@code null}
	 * @throws IllegalStateException 如果底层作用域当前不活动
	 * @see #registerDestructionCallback
	 */
	@Nullable
	Object remove(String name);

	/**
	 * 注册在作用域中指定对象销毁时（或作用域整体销毁时）执行的回调。
	 * <p><b>注意：这是一个可选操作。</b>此方法仅对具有实际销毁配置的作用域bean调用
	 * （DisposableBean、destroy-method、DestructionAwareBeanPostProcessor）。
	 * 实现应尽力在适当的时间执行给定的回调。
	 * 如果底层运行时环境根本不支持这样的回调，则<i>必须忽略回调并记录相应的警告</i>。
	 * <p>注意，“销毁”是指对象作为作用域自身生命周期的一部分的自动销毁，而不是应用程序显式删除的个别作用域对象。
	 * 如果通过此外观的{@link #remove(String)}方法删除作用域对象，
	 * 则任何已注册的销毁回调也应被删除，假设已删除的对象将被重用或手动销毁。
	 *
	 * @param name     要执行销毁回调的对象的名称
	 * @param callback 要执行的销毁回调。
	 *                 注意，传入的Runnable永远不会抛出异常，因此可以在不含有try-catch块的情况下安全地执行它。
	 *                 此外，Runnable通常应该是可序列化的，前提是其目标对象也是可序列化的。
	 * @throws IllegalStateException 如果底层作用域当前不活动
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getDestroyMethodName()
	 * @see DestructionAwareBeanPostProcessor
	 */
	void registerDestructionCallback(String name, Runnable callback);

	/**
	 * 解析给定键的上下文对象（如果有）。
	 * 例如，键为"request"时，返回HttpServletRequest对象。
	 *
	 * @param key 上下文键
	 * @return 相应的对象，如果找不到则为{@code null}
	 * @throws IllegalStateException 如果底层作用域当前不活动
	 */
	@Nullable
	Object resolveContextualObject(String key);

	/**
	 * 返回当前底层作用域的<em>会话ID</em>，如果有的话。
	 * <p>会话ID的确切含义取决于底层存储机制。
	 * 在会话作用域对象的情况下，会话ID通常等于（或派生自）{@link javax.servlet.http.HttpSession#getId()会话ID}；
	 * 在整个会话中存在的自定义对话中，当前对话的特定ID将是合适的。
	 * <p><b>注意：这是一个可选操作。</b>如果底层存储机制没有明显的候选ID，则在此方法的实现中返回{@code null}是完全有效的。
	 *
	 * @return 会话ID，如果当前作用域没有会话ID，则为{@code null}
	 * @throws IllegalStateException 如果底层作用域当前不活动
	 */
	@Nullable
	String getConversationId();

}
