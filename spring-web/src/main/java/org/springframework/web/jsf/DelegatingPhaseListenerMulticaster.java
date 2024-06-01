/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.jsf;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import java.util.Collection;

/**
 * JSF PhaseListener 实现类，将操作委托给来自 Spring 根 WebApplicationContext
 * 的一个或多个 Spring 管理的 PhaseListener bean。
 *
 * <p>在您的 {@code faces-config.xml} 文件中配置这个多播监听器，如下所示：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;phase-listener&gt;
 *     org.springframework.web.jsf.DelegatingPhaseListenerMulticaster
 *   &lt;/phase-listener&gt;
 *   ...
 * &lt;/application&gt;</pre>
 *
 * 多播监听器将所有 {@code beforePhase} 和 {@code afterPhase} 事件委托给所有目标 PhaseListener bean。
 * 默认情况下，这些 bean 将通过类型获取：Spring 根 WebApplicationContext 中实现 PhaseListener 接口的所有 bean 都会被获取并调用。
 *
 * <p>注意：这个多播监听器的 {@code getPhaseId()} 方法将始终返回 {@code ANY_PHASE}。
 * <b>目标监听器 bean 暴露的 phase id 将被忽略；所有事件都将传播给所有监听器。</b>
 *
 * <p>可以通过继承这个多播监听器类来改变获取监听器 bean 的策略，或改变访问 ApplicationContext 的策略
 * （通常通过 {@link FacesContextUtils#getWebApplicationContext(FacesContext)} 获取）。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @since 1.2.7
 */
@SuppressWarnings("serial")
public class DelegatingPhaseListenerMulticaster implements PhaseListener {

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		// 遍历获取的 阶段监听器 列表
		for (PhaseListener listener : getDelegates(event.getFacesContext())) {
			// 在每个监听器上调用 beforePhase 方法
			listener.beforePhase(event);
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		// 遍历获取的 阶段监听器 列表
		for (PhaseListener listener : getDelegates(event.getFacesContext())) {
			// 在每个监听器上调用 afterPhase 方法
			listener.afterPhase(event);
		}
	}


	/**
	 * 从Spring根WebApplicationContext获取委托的PhaseListener bean。
	 *
	 * @param facesContext 当前的JSF上下文
	 * @return PhaseListener对象的集合
	 * @see #getBeanFactory
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeansOfType(Class)
	 */
	protected Collection<PhaseListener> getDelegates(FacesContext facesContext) {
		// 获取 ListableBeanFactory 实例
		ListableBeanFactory bf = getBeanFactory(facesContext);

		// 使用 BeanFactoryUtils 获取包括祖先在内的所有 PhaseListener 类型的 bean，并返回它们的值
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, PhaseListener.class, true, false).values();
	}

	/**
	 * 检索Spring BeanFactory以委托bean名称解析。
	 * <p>默认实现委托给{@code getWebApplicationContext}。
	 * 可以重写以提供任意的ListableBeanFactory引用进行解析；通常，这将是一个完整的Spring ApplicationContext。
	 *
	 * @param facesContext 当前的JSF上下文
	 * @return Spring ListableBeanFactory（永不为{@code null}）
	 * @see #getWebApplicationContext
	 */
	protected ListableBeanFactory getBeanFactory(FacesContext facesContext) {
		return getWebApplicationContext(facesContext);
	}

	/**
	 * 检索用于委托bean名称解析的web应用程序上下文。
	 * <p>默认实现委托给FacesContextUtils。
	 *
	 * @param facesContext 当前的JSF上下文
	 * @return Spring web应用程序上下文（永不为{@code null}）
	 * @see FacesContextUtils#getRequiredWebApplicationContext
	 */
	protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
