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

package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;

/**
 * 由持有 AOP 代理工厂配置的类实现的接口。
 * 此配置包括拦截器和其他通知、Advisor 以及被代理的接口。
 *
 * <p>从 Spring 获得的任何 AOP 代理都可以强制转换为此接口，
 * 以允许操作其 AOP 通知。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 * @see org.springframework.aop.framework.AdvisedSupport
 */
public interface Advised extends TargetClassAware {

	/**
	 * 返回 Advised 配置是否已冻结，
	 * 在此情况下无法更改通知。
	 */
	boolean isFrozen();

	/**
	 * 我们是否代理完整的目标类而不是指定的接口？
	 */
	boolean isProxyTargetClass();

	/**
	 * 返回 AOP 代理的接口。
	 * <p>不包括目标类，目标类也可能被代理。
	 */
	Class<?>[] getProxiedInterfaces();

	/**
	 * 确定是否代理了给定接口。
	 * @param intf 要检查的接口
	 */
	boolean isInterfaceProxied(Class<?> intf);

	/**
	 * 更改此 {@code Advised} 对象使用的 {@code TargetSource}。
	 * <p>仅在配置未{@linkplain #isFrozen 冻结}时有效。
	 * @param targetSource 要使用的新 TargetSource
	 */
	void setTargetSource(TargetSource targetSource);

	/**
	 * 返回此 {@code Advised} 对象使用的 {@code TargetSource}。
	 */
	TargetSource getTargetSource();

	/**
	 * 设置代理是否应通过 AOP 框架作为 {@link ThreadLocal}
	 * 暴露，以便通过 {@link AopContext} 类检索。
	 * <p>如果被通知对象需要调用自身的方法并应用通知，
	 * 则可能需要暴露代理。否则，如果被通知对象
	 * 调用 {@code this} 上的方法，则不会应用通知。
	 * <p>默认值为 {@code false}，以获得最佳性能。
	 */
	void setExposeProxy(boolean exposeProxy);

	/**
	 * 返回工厂是否应将代理作为 {@link ThreadLocal} 暴露。
	 * <p>如果被通知对象需要调用自身的方法并应用通知，
	 * 则可能需要暴露代理。否则，如果被通知对象
	 * 调用 {@code this} 上的方法，则不会应用通知。
	 * <p>获取代理类似于 EJB 调用 {@code getEJBObject()}。
	 * @see AopContext
	 */
	boolean isExposeProxy();

	/**
	 * 设置此代理配置是否已预过滤，
	 * 使其仅包含适用的 advisor（匹配此代理的目标类）。
	 * <p>默认值为 "false"。如果 advisor 已经过预过滤，
	 * 则将此设置为 "true"，这意味着在构建代理调用的
	 * 实际 advisor 链时可以跳过 ClassFilter 检查。
	 * @see org.springframework.aop.ClassFilter
	 */
	void setPreFiltered(boolean preFiltered);

	/**
	 * 返回此代理配置是否已预过滤，
	 * 使其仅包含适用的 advisor（匹配此代理的目标类）。
	 */
	boolean isPreFiltered();

	/**
	 * 返回应用于此代理的 advisor。
	 * @return 应用于此代理的 Advisor 列表（绝不为 {@code null}）
	 */
	Advisor[] getAdvisors();

	/**
	 * 返回应用于此代理的 advisor 数量。
	 * <p>默认实现委托给 {@code getAdvisors().length}。
	 * @since 5.3.1
	 */
	default int getAdvisorCount() {
		return getAdvisors().length;
	}

	/**
	 * 在 advisor 链的末尾添加 advisor。
	 * <p>Advisor 可以是 {@link org.springframework.aop.IntroductionAdvisor}，
	 * 在下次从相关工厂获取代理时，将提供新接口。
	 * @param advisor 要添加到链末尾的 advisor
	 * @throws AopConfigException 如果通知无效
	 */
	void addAdvisor(Advisor advisor) throws AopConfigException;

	/**
	 * 在链中的指定位置添加 Advisor。
	 * @param advisor 要添加到链中指定位置的 advisor
	 * @param pos 链中的位置（0 为开头）。必须有效。
	 * @throws AopConfigException 如果通知无效
	 */
	void addAdvisor(int pos, Advisor advisor) throws AopConfigException;

	/**
	 * 移除给定的 advisor。
	 * @param advisor 要移除的 advisor
	 * @return {@code true} 如果 advisor 已被移除；{@code false}
	 * 如果未找到 advisor，因此无法移除
	 */
	boolean removeAdvisor(Advisor advisor);

	/**
	 * 移除给定索引处的 advisor。
	 * @param index 要移除的 advisor 的索引
	 * @throws AopConfigException 如果索引无效
	 */
	void removeAdvisor(int index) throws AopConfigException;

	/**
	 * 返回给定 advisor 的索引（从 0 开始），
	 * 如果没有这样的 advisor 应用于此代理，则返回 -1。
	 * <p>此方法的返回值可用于索引到 advisors 数组中。
	 * @param advisor 要搜索的 advisor
	 * @return 此 advisor 的索引（从 0 开始），如果没有这样的 advisor，则返回 -1
	 */
	int indexOf(Advisor advisor);

	/**
	 * 替换给定的 advisor。
	 * <p><b>注意：</b>如果 advisor 是 {@link org.springframework.aop.IntroductionAdvisor}
	 * 并且替换者不是或实现不同的接口，则需要重新获取代理，
	 * 否则旧接口将不被支持，新接口也不会被实现。
	 * @param a 要替换的 advisor
	 * @param b 替换它的 advisor
	 * @return 是否已替换。如果 advisor 未在 advisor 列表中找到，
	 * 则此方法返回 {@code false} 并且不执行任何操作。
	 * @throws AopConfigException 如果通知无效
	 */
	boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException;

	/**
	 * 将给定的 AOP Alliance advice 添加到通知（拦截器）链的末尾。
	 * <p>这将被包装在 DefaultPointcutAdvisor 中，并使用始终应用的切点，
	 * 并以这种包装形式从 {@code getAdvisors()} 方法返回。
	 * <p>注意，给定的 advice 将应用于代理上的所有调用，
	 * 甚至包括 {@code toString()} 方法！使用适当的 advice 实现
	 * 或指定适当的切点以应用于更窄的方法集。
	 * @param advice 要添加到链末尾的 advice
	 * @throws AopConfigException 如果通知无效
	 * @see #addAdvice(int, Advice)
	 * @see org.springframework.aop.support.DefaultPointcutAdvisor
	 */
	void addAdvice(Advice advice) throws AopConfigException;

	/**
	 * 在 advice 链的指定位置添加给定的 AOP Alliance Advice。
	 * <p>这将被包装在 {@link org.springframework.aop.support.DefaultPointcutAdvisor}
	 * 中，并使用始终应用的切点，并从 {@link #getAdvisors()}
	 * 方法以这种包装形式返回。
	 * <p>注意：给定的 advice 将应用于代理上的所有调用，
	 * 甚至包括 {@code toString()} 方法！使用适当的 advice 实现
	 * 或指定适当的切点以应用于更窄的方法集。
	 * @param pos 从 0 开始的索引
	 * @param advice 要在 advice 链的指定位置添加的 advice
	 * @throws AopConfigException 如果通知无效
	 */
	void addAdvice(int pos, Advice advice) throws AopConfigException;

	/**
	 * 移除包含给定 advice 的 Advisor。
	 * @param advice 要移除的 advice
	 * @return {@code true} 如果找到并移除了 advice；
	 * {@code false} 如果没有这样的 advice
	 */
	boolean removeAdvice(Advice advice);

	/**
	 * 返回给定 AOP Alliance Advice 的索引（从 0 开始），
	 * 如果没有这样的 advice 是此代理的 advice，则返回 -1。
	 * <p>此方法的返回值可用于索引到 advisors 数组中。
	 * @param advice 要搜索的 AOP Alliance advice
	 * @return 此 advice 的索引（从 0 开始），如果没有这样的 advice，则返回 -1
	 */
	int indexOf(Advice advice);

	/**
	 * 由于 {@code toString()} 通常委托给目标对象，
	 * 这返回 AOP 代理的等效内容。
	 * @return 代理配置的字符串描述
	 */
	String toProxyConfigString();

}
