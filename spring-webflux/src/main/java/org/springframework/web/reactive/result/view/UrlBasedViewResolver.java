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

package org.springframework.web.reactive.result.view;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.function.Function;

/**
 * 允许将符号视图名称直接解析为 URL 的 {@link ViewResolver}，无需显式映射定义。
 * 如果符号名称与视图资源的名称直接匹配（即符号名称是资源文件名的唯一部分），而不需要为每个视图定义专用的映射，则此功能很有用。
 * <p>
 * 支持 {@link AbstractUrlBasedView} 子类，如 {@link org.springframework.web.reactive.result.view.freemarker.FreeMarkerView}。
 * 此解析器生成的所有视图的视图类都可以通过 "viewClass" 属性指定。
 * <p>
 * 视图名称可以是资源 URL 本身，也可以通过指定前缀和/或后缀进行增强。显式支持将持有 RequestContext 的属性导出到所有视图。
 * <p>
 * 示例：prefix="templates/", suffix=".ftl", viewname="test" &rarr; "templates/test.ftl"
 * <p>
 * 作为特殊功能，重定向 URL 可以通过 "redirect:" 前缀指定。例如："redirect:myAction" 将触发重定向到给定的 URL，而不是作为标准视图名称解析。
 * 这通常用于在完成表单工作流程后将重定向到控制器 URL。
 * <p>
 * 注意：此类不支持本地化解析，即根据当前区域设置将符号视图名称解析为不同的资源。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.0
 */
public class UrlBasedViewResolver extends ViewResolverSupport
		implements ViewResolver, ApplicationContextAware, InitializingBean {

	/**
	 * 指定重定向 URL 的特殊视图名称的前缀（通常是在表单提交和处理后到控制器的重定向）。
	 * 这样的视图名称不会以配置的默认方式解析，而是作为特殊快捷方式处理。
	 */
	public static final String REDIRECT_URL_PREFIX = "redirect:";

	/**
	 * 视图类。
	 */
	@Nullable
	private Class<?> viewClass;

	/**
	 * 视图前缀。
	 */
	private String prefix = "";

	/**
	 * 视图后缀。
	 */
	private String suffix = "";

	/**
	 * 视图名称数组。
	 */
	@Nullable
	private String[] viewNames;

	/**
	 * 重定向视图提供者函数。
	 */
	private Function<String, RedirectView> redirectViewProvider = RedirectView::new;

	/**
	 * 请求上下文属性。
	 */
	@Nullable
	private String requestContextAttribute;

	/**
	 * 应用上下文。
	 */
	@Nullable
	private ApplicationContext applicationContext;


	/**
	 * 设置用于创建视图的视图类。
	 *
	 * @param viewClass 可分配给所需视图类（默认为 AbstractUrlBasedView）的类
	 * @see #requiredViewClass()
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	public void setViewClass(@Nullable Class<?> viewClass) {
		if (viewClass != null && !requiredViewClass().isAssignableFrom(viewClass)) {
			String name = viewClass.getName();
			throw new IllegalArgumentException("Given view class [" + name + "] " +
					"is not of type [" + requiredViewClass().getName() + "]");
		}
		this.viewClass = viewClass;
	}

	/**
	 * 返回用于创建视图的视图类。
	 *
	 * @see #setViewClass
	 */
	@Nullable
	protected Class<?> getViewClass() {
		return this.viewClass;
	}

	/**
	 * 设置构建 URL 时要添加到视图名称前面的前缀。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回构建 URL 时要添加到视图名称前面的前缀。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置构建 URL 时要追加到视图名称的后缀。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回构建 URL 时要追加到视图名称的后缀。
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置此 {@link ViewResolver} 可处理的视图名称（或名称模式）。
	 * 视图名称可以包含简单的通配符，例如 'my*'、'*Report' 和 '*Repo*'，它们都将匹配视图名称 'myReport'。
	 *
	 * @see #canHandle
	 */
	public void setViewNames(@Nullable String... viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * 返回此 {@link ViewResolver} 可处理的视图名称（或名称模式）。
	 */
	@Nullable
	protected String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * URL 类型的 {@link RedirectView} 提供者，可以用于提供具有自定义默认状态码的重定向视图。
	 */
	public void setRedirectViewProvider(Function<String, RedirectView> redirectViewProvider) {
		this.redirectViewProvider = redirectViewProvider;
	}

	/**
	 * 设置所有视图的 {@link RequestContext} 属性的名称。
	 *
	 * @param requestContextAttribute RequestContext 属性的名称
	 * @see AbstractView#setRequestContextAttribute
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回所有视图的 {@link RequestContext} 属性的名称（如果有的话）。
	 */
	@Nullable
	protected String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 接受包含的 {@code ApplicationContext}（如果有）。
	 * <p>用于初始化新创建的 {@link View} 实例，应用生命周期回调并提供对包含环境的访问。
	 *
	 * @see #setViewClass
	 * @see #createView
	 * @see #applyLifecycleMethods
	 */
	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回包含的 {@code ApplicationContext}（如果有）。
	 *
	 * @see #setApplicationContext
	 */
	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (getViewClass() == null) {
			throw new IllegalArgumentException("Property 'viewClass' is required");
		}
	}

	/**
	 * 解析视图
	 *
	 * @param viewName 视图的名称
	 * @param locale   请求的语言环境
	 * @return 解析好的视图
	 */
	@Override
	public Mono<View> resolveViewName(String viewName, Locale locale) {
		// 检查是否能处理视图名称和语言环境。
		if (!canHandle(viewName, locale)) {
			return Mono.empty();
		}

		AbstractUrlBasedView urlBasedView;
		// 如果视图名称以重定向前缀开头。
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			// 获取重定向 URL。
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			// 使用重定向视图提供者创建 URL 基础视图。
			urlBasedView = this.redirectViewProvider.apply(redirectUrl);
		} else {
			// 创建视图。
			urlBasedView = createView(viewName);
		}

		// 应用生命周期方法到视图。
		View view = applyLifecycleMethods(viewName, urlBasedView);
		try {
			// 检查资源是否存在于指定语言环境下。
			return (urlBasedView.checkResourceExists(locale) ? Mono.just(view) : Mono.empty());
		} catch (Exception ex) {
			return Mono.error(ex);
		}
	}

	/**
	 * 指示此 {@link ViewResolver} 是否可以处理提供的视图名称。
	 * 如果不能，将返回空结果。默认实现检查配置的 {@link #setViewNames 视图名称}。
	 *
	 * @param viewName 视图名称
	 * @param locale   区域设置信息
	 * @return 解析器是否适用于指定的视图
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean canHandle(String viewName, Locale locale) {
		String[] viewNames = getViewNames();
		return (viewNames == null || PatternMatchUtils.simpleMatch(viewNames, viewName));
	}

	/**
	 * 返回此解析器所需的视图类型。此实现返回 {@link AbstractUrlBasedView}。
	 *
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	protected Class<?> requiredViewClass() {
		return AbstractUrlBasedView.class;
	}

	/**
	 * 实例化指定的视图类。
	 * <p>默认实现使用反射来实例化类。
	 *
	 * @return 视图类的新实例
	 * @see #setViewClass
	 * @since 5.3
	 */
	protected AbstractUrlBasedView instantiateView() {
		Class<?> viewClass = getViewClass();
		Assert.state(viewClass != null, "No view class");
		return (AbstractUrlBasedView) BeanUtils.instantiateClass(viewClass);
	}

	/**
	 * 创建指定视图类的新视图实例并配置它。
	 * <p>不执行任何预定义视图实例的查找。
	 * <p>Spring bean容器定义的生命周期方法不必在此调用：
	 * 如果存在 {@link #setApplicationContext ApplicationContext}，它们将自动应用。
	 *
	 * @param viewName 要构建的视图的名称
	 * @return 视图实例
	 * @see #getViewClass()
	 * @see #applyLifecycleMethods
	 */
	protected AbstractUrlBasedView createView(String viewName) {
		// 实例化视图。
		AbstractUrlBasedView view = instantiateView();
		// 设置支持的媒体类型。
		view.setSupportedMediaTypes(getSupportedMediaTypes());
		// 设置默认字符集。
		view.setDefaultCharset(getDefaultCharset());
		// 设置视图的 URL。
		view.setUrl(getPrefix() + viewName + getSuffix());

		// 获取请求上下文属性。
		String requestContextAttribute = getRequestContextAttribute();
		// 如果请求上下文属性不为 null。
		if (requestContextAttribute != null) {
			// 设置视图的请求上下文属性。
			view.setRequestContextAttribute(requestContextAttribute);
		}

		return view;
	}

	/**
	 * 如果存在 {@link ApplicationContext}，将其包含的生命周期方法应用于给定的 {@link View} 实例。
	 *
	 * @param viewName 视图名称
	 * @param view     新创建的 View 实例，预配置了 {@link AbstractUrlBasedView} 的属性
	 * @return 要使用的 {@link View} 实例（原始实例或装饰变体）
	 * @see #getApplicationContext()
	 * @see ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#initializeBean
	 */
	protected View applyLifecycleMethods(String viewName, AbstractUrlBasedView view) {
		// 获取应用程序上下文。
		ApplicationContext context = getApplicationContext();
		// 如果应用程序上下文不为 null。
		if (context != null) {
			// 初始化视图。
			Object initialized = context.getAutowireCapableBeanFactory().initializeBean(view, viewName);
			// 如果初始化后的对象是视图。
			if (initialized instanceof View) {
				// 返回视图。
				return (View) initialized;
			}
		}
		// 返回视图。
		return view;
	}

}
