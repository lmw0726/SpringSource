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

package org.springframework.web.jsf;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.context.WebApplicationContext;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

/**
 * JSF NavigationHandler 实现类，将操作委托给从 Spring 根 WebApplicationContext
 * 获取的 NavigationHandler bean。
 *
 * <p>在您的 {@code faces-config.xml} 文件中配置这个处理器代理，如下所示：
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;navigation-handler&gt;
 *     org.springframework.web.jsf.DelegatingNavigationHandlerProxy
 *   &lt;/navigation-handler&gt;
 *   ...
 * &lt;/application&gt;</pre>
 * <p>
 * 默认情况下，Spring ApplicationContext 将根据名称 "jsfNavigationHandler" 搜索 NavigationHandler。
 * 在最简单的情况下，这是一个简单的 Spring bean 定义，如下所示。不过，Spring 的所有 bean 配置
 * 功能都可以应用于这样的 bean，特别是所有形式的依赖注入。
 *
 * <pre class="code">
 * &lt;bean name="jsfNavigationHandler" class="mypackage.MyNavigationHandler"&gt;
 *   &lt;property name="myProperty" ref="myOtherBean"/&gt;
 * &lt;/bean&gt;</pre>
 * <p>
 * 目标 NavigationHandler bean 通常会扩展标准的 JSF NavigationHandler 类。然而，请注意，在这种情况下
 * 装饰原始的 NavigationHandler（JSF 提供者的默认处理器）是<i>不</i>受支持的，因为我们无法按照标准
 * JSF 样式注入原始处理器（即，作为构造函数参数）。
 *
 * <p>对于<b>装饰原始的 NavigationHandler</b>，请确保您的目标 bean 扩展了 Spring 的<b>DecoratingNavigationHandler</b>
 * 类。这允许将原始处理器作为方法参数传入，该代理会自动检测到这一点。注意，DecoratingNavigationHandler 子类
 * 仍然可以作为标准的 JSF NavigationHandler 使用！
 *
 * <p>这个代理可以被子类化，以改变用于搜索导航处理器的 bean 名称，改变获取目标处理器的策略，
 * 或者改变访问 ApplicationContext 的策略（通常通过 {@link FacesContextUtils#getWebApplicationContext(FacesContext)} 获取）。
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @see DecoratingNavigationHandler
 * @since 1.2.7
 */
public class DelegatingNavigationHandlerProxy extends NavigationHandler {

	/**
	 * Spring 应用程序上下文中目标 bean 的默认名称："jsfNavigationHandler"。
	 */
	public static final String DEFAULT_TARGET_BEAN_NAME = "jsfNavigationHandler";
	/**
	 * 原始导航处理器
	 */
	@Nullable
	private NavigationHandler originalNavigationHandler;


	/**
	 * 创建一个新的 DelegatingNavigationHandlerProxy。
	 */
	public DelegatingNavigationHandlerProxy() {
	}

	/**
	 * 创建一个新的 DelegatingNavigationHandlerProxy。
	 *
	 * @param originalNavigationHandler 原始的 NavigationHandler
	 */
	public DelegatingNavigationHandlerProxy(NavigationHandler originalNavigationHandler) {
		this.originalNavigationHandler = originalNavigationHandler;
	}


	/**
	 * 处理指定参数所隐含的导航请求，通过委托给 Spring 应用程序上下文中的目标 bean。
	 * <p>目标 bean 需要扩展 JSF NavigationHandler 类。如果它扩展了 Spring 的 DecoratingNavigationHandler，
	 * 则带有原始 NavigationHandler 作为参数的重载 {@code handleNavigation} 方法将被使用。
	 * 否则，将调用标准的 {@code handleNavigation} 方法。
	 */
	@Override
	public void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
		// 获取导航处理器
		NavigationHandler handler = getDelegate(facesContext);

		// 检查导航处理器是否是 DecoratingNavigationHandler 类型的实例
		if (handler instanceof DecoratingNavigationHandler) {
			// 如果是 DecoratingNavigationHandler 类型的实例，则调用其 handleNavigation 方法
			((DecoratingNavigationHandler) handler).handleNavigation(
					facesContext, fromAction, outcome, this.originalNavigationHandler);
		} else {
			// 如果不是 DecoratingNavigationHandler 类型的实例，则调用普通的 handleNavigation 方法
			handler.handleNavigation(facesContext, fromAction, outcome);
		}
	}

	/**
	 * 返回要委托的目标 NavigationHandler。
	 * <p>默认情况下，从 Spring 根 WebApplicationContext 中获取名称为 "jsfNavigationHandler" 的 bean，
	 * 每次调用都会获取。
	 *
	 * @param facesContext 当前的 JSF 上下文
	 * @return 要委托的目标 NavigationHandler
	 * @see #getTargetBeanName
	 * @see #getBeanFactory
	 */
	protected NavigationHandler getDelegate(FacesContext facesContext) {
		// 获取目标 bean 的名称
		String targetBeanName = getTargetBeanName(facesContext);

		// 从 bean 工厂中获取 导航处理器 的实例
		return getBeanFactory(facesContext).getBean(targetBeanName, NavigationHandler.class);
	}

	/**
	 * 返回 BeanFactory 中目标 NavigationHandler bean 的名称。
	 * 默认是 "jsfNavigationHandler"。
	 *
	 * @param facesContext 当前的 JSF 上下文
	 * @return 目标 bean 的名称
	 */
	protected String getTargetBeanName(FacesContext facesContext) {
		return DEFAULT_TARGET_BEAN_NAME;
	}

	/**
	 * 获取 Spring BeanFactory 以委托 bean 名称解析。
	 * <p>默认实现委托给 {@code getWebApplicationContext}。
	 * 可以被重写以提供一个任意的 BeanFactory 引用进行解析；
	 * 通常，这将是一个完整的 Spring ApplicationContext。
	 *
	 * @param facesContext 当前的 JSF 上下文
	 * @return Spring BeanFactory（不会是 {@code null}）
	 * @see #getWebApplicationContext
	 */
	protected BeanFactory getBeanFactory(FacesContext facesContext) {
		return getWebApplicationContext(facesContext);
	}

	/**
	 * 获取 WebApplicationContext 以委托 bean 名称解析。
	 * <p>默认实现委托给 FacesContextUtils。
	 *
	 * @param facesContext 当前的 JSF 上下文
	 * @return Spring web 应用程序上下文（不会是 {@code null}）
	 * @see FacesContextUtils#getRequiredWebApplicationContext
	 */
	protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
