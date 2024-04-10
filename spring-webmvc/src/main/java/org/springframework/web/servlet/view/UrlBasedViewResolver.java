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

package org.springframework.web.servlet.view;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * 实现了 {@link org.springframework.web.servlet.ViewResolver} 接口的简单实现，
 * 允许将符号视图名称直接解析为 URL，而无需显式的映射定义。
 * 如果您的符号名称与视图资源的名称直接相关（即符号名称是资源文件名的唯一部分），
 * 则此方法非常有用，而无需为每个视图定义专门的映射。
 *
 * <p>支持 {@link AbstractUrlBasedView} 的子类，例如 {@link InternalResourceView}
 * 和 {@link org.springframework.web.servlet.view.freemarker.FreeMarkerView}。
 * 通过 "viewClass" 属性可以指定此解析器生成的所有视图的视图类。
 *
 * <p>视图名称可以是资源 URL 本身，也可以通过指定的前缀和/或后缀进行增强。
 * 显式支持将包含 RequestContext 的属性导出到所有视图。
 *
 * <p>示例：prefix="/WEB-INF/jsp/"，suffix=".jsp"，viewname="test" &rarr;
 * "/WEB-INF/jsp/test.jsp"
 *
 * <p>作为一个特殊功能，可以通过 "redirect:" 前缀指定重定向 URL。
 * 例如："redirect:myAction" 将触发重定向到给定的 URL，而不是作为标准视图名称解析。
 * 这通常用于在完成表单工作流程后重定向到控制器 URL。
 *
 * <p>此外，可以通过 "forward:" 前缀指定转发 URL。
 * 例如："forward:myAction" 将触发向给定的 URL 的转发，而不是作为标准视图名称解析。
 * 这通常用于控制器 URL；不应将其用于 JSP URL - 在那里使用逻辑视图名称。
 *
 * <p>注意：此类不支持本地化解析，即根据当前区域设置将符号视图名称解析为不同的资源。
 *
 * <p><b>注意：</b>在链接 ViewResolver 时，UrlBasedViewResolver 将检查
 * {@linkplain AbstractUrlBasedView#checkResource 指定的资源是否实际存在}。
 * 但是，对于 {@link InternalResourceView}，通常无法事先确定目标资源的存在。
 * 在这种情况下，UrlBasedViewResolver 将始终为任何给定的视图名称返回一个视图；
 * 因此，应将其配置为链中的最后一个 ViewResolver。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @see #setViewClass
 * @see #setPrefix
 * @see #setSuffix
 * @see #setRequestContextAttribute
 * @see #REDIRECT_URL_PREFIX
 * @see AbstractUrlBasedView
 * @see InternalResourceView
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView
 * @since 13.12.2003
 */
public class UrlBasedViewResolver extends AbstractCachingViewResolver implements Ordered {

	/**
	 * 指定重定向 URL 的特殊视图名称的前缀（通常是在提交和处理表单后到控制器）。
	 * 这样的视图名称不会以配置的默认方式解析，而是被视为特殊的快捷方式。
	 */
	public static final String REDIRECT_URL_PREFIX = "redirect:";

	/**
	 * 指定转发 URL 的特殊视图名称的前缀（通常在提交和处理表单后到控制器）。
	 * 这样的视图名称不会以配置的默认方式解析，而是被视为特殊的快捷方式。
	 */
	public static final String FORWARD_URL_PREFIX = "forward:";

	/**
	 * 视图类
	 */
	@Nullable
	private Class<?> viewClass;

	/**
	 * 视图名称前缀
	 */
	private String prefix = "";

	/**
	 * 视图名称后缀
	 */
	private String suffix = "";

	/**
	 * 内容类型
	 */
	@Nullable
	private String contentType;

	/**
	 * 当重定向的路径是/开头时，是否认为是路径相对于当前ServletContext
	 */
	private boolean redirectContextRelative = true;

	/**
	 * 重定向是否需要兼容 http 1.0
	 */
	private boolean redirectHttp10Compatible = true;

	/**
	 * 表示被重定向的域名，可以为空
	 */
	@Nullable
	private String[] redirectHosts;

	/**
	 * 请求上下文属性名称
	 */
	@Nullable
	private String requestContextAttribute;

	/**
	 * 静态属性的映射，由属性名称（String）为键。
	 */
	private final Map<String, Object> staticAttributes = new HashMap<>();

	/**
	 * 暴露的路径变量
	 */
	@Nullable
	private Boolean exposePathVariables;

	/**
	 * 是否将上下文Bean暴露为属性
	 */
	@Nullable
	private Boolean exposeContextBeansAsAttributes;

	/**
	 * 暴露的上下文bean名称数组
	 */
	@Nullable
	private String[] exposedContextBeanNames;

	/**
	 * 视图名称数组
	 */
	@Nullable
	private String[] viewNames;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置用于创建视图的视图类。
	 *
	 * @param viewClass 一个可分配给所需视图类的类（默认为：AbstractUrlBasedView）
	 * @see #requiredViewClass()
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	public void setViewClass(@Nullable Class<?> viewClass) {
		if (viewClass != null && !requiredViewClass().isAssignableFrom(viewClass)) {
			throw new IllegalArgumentException("Given view class [" + viewClass.getName() +
					"] is not of type [" + requiredViewClass().getName() + "]");
		}
		this.viewClass = viewClass;
	}

	/**
	 * 返回要用于创建视图的视图类。
	 *
	 * @see #setViewClass
	 */
	@Nullable
	protected Class<?> getViewClass() {
		return this.viewClass;
	}

	/**
	 * 设置在构建 URL 时要添加到视图名称前面的前缀。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回在构建 URL 时要添加到视图名称前面的前缀。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置在构建 URL 时要添加到视图名称后面的后缀。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回在构建 URL 时要添加到视图名称后面的后缀。
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置所有视图的内容类型。
	 * <p>如果假定视图本身设置内容类型，则视图类可能会忽略此设置，例如在 JSP 的情况下。
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回所有视图的内容类型（如果有）。
	 */
	@Nullable
	protected String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否将以斜杠 ("/") 开头的给定重定向 URL 解释为相对于当前 ServletContext，即相对于 Web 应用程序根目录。
	 * <p>默认值为 "true"：以斜杠开头的重定向 URL 将解释为相对于 Web 应用程序根目录，即上下文路径将被添加到 URL 前面。
	 * <p><b>可以通过 "redirect:" 前缀指定重定向 URL。</b> 例如："redirect:myAction"
	 *
	 * @see RedirectView#setContextRelative
	 * @see #REDIRECT_URL_PREFIX
	 */
	public void setRedirectContextRelative(boolean redirectContextRelative) {
		this.redirectContextRelative = redirectContextRelative;
	}

	/**
	 * 返回是否将以斜杠 ("/") 开头的给定重定向 URL 解释为相对于当前 ServletContext，即相对于 Web 应用程序根目录。
	 */
	protected boolean isRedirectContextRelative() {
		return this.redirectContextRelative;
	}

	/**
	 * 设置重定向是否应与 HTTP 1.0 客户端保持兼容。
	 * <p>在默认实现中，这将在任何情况下强制执行 HTTP 状态码 302，即委托给 {@code HttpServletResponse.sendRedirect}。
	 * 关闭此选项将发送 HTTP 状态码 303，这是 HTTP 1.1 客户端的正确代码，但不被 HTTP 1.0 客户端理解。
	 * <p>许多 HTTP 1.1 客户端将 302 视为 303，不会有任何区别。
	 * 但是，某些客户端依赖于在 POST 请求之后重定向时的 303；在这种情况下关闭此标志。
	 * <p><b>重定向 URL 可以通过 "redirect:" 前缀指定。</b> 例如： "redirect:myAction"
	 *
	 * @see RedirectView#setHttp10Compatible
	 * @see #REDIRECT_URL_PREFIX
	 */
	public void setRedirectHttp10Compatible(boolean redirectHttp10Compatible) {
		this.redirectHttp10Compatible = redirectHttp10Compatible;
	}

	/**
	 * 返回重定向是否应与 HTTP 1.0 客户端保持兼容。
	 */
	protected boolean isRedirectHttp10Compatible() {
		return this.redirectHttp10Compatible;
	}

	/**
	 * 配置与应用程序关联的一个或多个主机。
	 * 所有其他主机将被视为外部主机。
	 * <p>实际上，此属性提供了一种通过 {@link HttpServletResponse#encodeRedirectURL} 在具有主机并且该主机未列为已知主机的 URL 上关闭编码的方法。
	 * <p>如果未设置（默认值），则所有 URL 都通过响应进行编码。
	 *
	 * @param redirectHosts 一个或多个应用程序主机
	 * @since 4.3
	 */
	public void setRedirectHosts(@Nullable String... redirectHosts) {
		this.redirectHosts = redirectHosts;
	}

	/**
	 * 返回配置的用于重定向目的的应用程序主机。
	 *
	 * @since 4.3
	 */
	@Nullable
	public String[] getRedirectHosts() {
		return this.redirectHosts;
	}

	/**
	 * 设置所有视图的 RequestContext 属性的名称。
	 *
	 * @param requestContextAttribute RequestContext 属性的名称
	 * @see AbstractView#setRequestContextAttribute
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回所有视图的 RequestContext 属性的名称（如果有）。
	 */
	@Nullable
	protected String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 为此解析器返回的所有视图设置静态属性，从 {@code java.util.Properties} 对象中获取。
	 * <p>这是设置静态属性的最便捷方式。请注意，如果模型中包含具有相同名称的值，则静态属性可以被动态属性覆盖。
	 * <p>可以使用字符串 "value"（通过 PropertiesEditor 解析）或 XML bean 定义中的 "props" 元素进行填充。
	 *
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 * @see AbstractView#setAttributes
	 */
	public void setAttributes(Properties props) {
		CollectionUtils.mergePropertiesIntoMap(props, this.staticAttributes);
	}

	/**
	 * 为此解析器返回的所有视图从 Map 中设置静态属性。这允许设置任何类型的属性值，例如 bean 引用。
	 * <p>可以使用 XML bean 定义中的 "map" 或 "props" 元素进行填充。
	 *
	 * @param attributes 名称字符串作为键，属性对象作为值的 Map
	 * @see AbstractView#setAttributesMap
	 */
	public void setAttributesMap(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			this.staticAttributes.putAll(attributes);
		}
	}

	/**
	 * 允许 Map 访问由此解析器返回的视图的静态属性，可以选择添加或覆盖特定条目。
	 * <p>用于直接指定条目的情况特别有用，例如通过 "attributesMap[myKey]"。
	 * 这对于在子视图定义中添加或覆盖条目特别有用。
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 指定此解析器解析的视图是否应将路径变量添加到模型中。
	 * <p>默认设置是让每个视图自行决定（参见 {@link AbstractView#setExposePathVariables}）。
	 * 但是，您可以使用此属性来覆盖该设置。
	 *
	 * @param exposePathVariables <ul>
	 *                            <li>{@code true} - 由此解析器解析的所有视图都将公开路径变量
	 *                            <li>{@code false} - 由此解析器解析的所有视图都不会公开路径变量
	 *                            <li>{@code null} - 单个视图可以自行决定（默认情况下使用此选项）
	 *                            </ul>
	 * @see AbstractView#setExposePathVariables
	 */
	public void setExposePathVariables(@Nullable Boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回此解析器解析的视图是否应将路径变量添加到模型中。
	 */
	@Nullable
	protected Boolean getExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否使应用程序上下文中的所有 Spring bean 可以作为请求属性访问，通过在访问属性时进行懒检查。
	 * <p>这将使所有这些 bean 在 JSP 2.0 页面中的普通 ${...} 表达式以及 JSTL 的 c:out 值表达式中都可以访问到。
	 * <p>默认为 "false"。
	 *
	 * @see AbstractView#setExposeContextBeansAsAttributes
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	@Nullable
	protected Boolean getExposeContextBeansAsAttributes() {
		return this.exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中应该公开的 bean 的名称。如果此值为非空，则只有指定的 bean 可以作为属性暴露。
	 *
	 * @see AbstractView#setExposedContextBeanNames
	 */
	public void setExposedContextBeanNames(@Nullable String... exposedContextBeanNames) {
		this.exposedContextBeanNames = exposedContextBeanNames;
	}

	@Nullable
	protected String[] getExposedContextBeanNames() {
		return this.exposedContextBeanNames;
	}

	/**
	 * 设置可以由此 {@link org.springframework.web.servlet.ViewResolver} 处理的视图名称（或名称模式）。
	 * 视图名称可以包含简单通配符，例如 'my*'、'*Report' 和 '*Repo*' 都将匹配视图名称 'myReport'。
	 *
	 * @see #canHandle
	 */
	public void setViewNames(@Nullable String... viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * 返回可以由此 {@link org.springframework.web.servlet.ViewResolver} 处理的视图名称（或名称模式）。
	 */
	@Nullable
	protected String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * 指定此 ViewResolver bean 的排序值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示无序。
	 *
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	protected void initApplicationContext() {
		// 调用父类的初始化应用上下文方法
		super.initApplicationContext();
		if (getViewClass() == null) {
			// 如果视图类为空，则抛出异常
			throw new IllegalArgumentException("Property 'viewClass' is required");
		}
	}


	/**
	 * 此实现仅返回视图名称，
	 * 因为此 ViewResolver 不支持本地化解析。
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	/**
	 * 覆盖以实现对“redirect:”前缀的检查。
	 * <p>在{@code loadView}中不可能，因为子类中覆盖的{@code loadView}版本可能依赖于
	 * 超类始终创建所需视图类的实例。
	 *
	 * @see #loadView
	 * @see #requiredViewClass
	 */
	@Override
	protected View createView(String viewName, Locale locale) throws Exception {
		// 如果此解析器不应处理给定的视图，则返回null以传递到链中的下一个解析器。
		if (!canHandle(viewName, locale)) {
			return null;
		}

		// 检查特殊的“redirect:”前缀。
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			// 如果视图名称以 redirect:开头，获取重定向的url
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			// 构建重定向视图
			RedirectView view = new RedirectView(redirectUrl,
					isRedirectContextRelative(), isRedirectHttp10Compatible());
			// 获取重定向域名
			String[] hosts = getRedirectHosts();
			if (hosts != null) {
				// 如果域名存在，则设置视图的域名
				view.setHosts(hosts);
			}
			// 应用生命周期方法
			return applyLifecycleMethods(REDIRECT_URL_PREFIX, view);
		}

		// 检查特殊的“forward:”前缀。
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			// 如果视图名称以 forward: 开头，获取转发的URL
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			//构建内部资源视图
			InternalResourceView view = new InternalResourceView(forwardUrl);
			// 应用生命周期方法
			return applyLifecycleMethods(FORWARD_URL_PREFIX, view);
		}

		// 否则，回退到超类实现：调用loadView。
		return super.createView(viewName, locale);
	}

	/**
	 * 指示此{@link org.springframework.web.servlet.ViewResolver}是否能够处理提供的视图名称。
	 * 如果不能，{@link #createView(String, java.util.Locale)}将返回{@code null}。
	 * 默认实现会检查配置的{@link #setViewNames 视图名称}。
	 *
	 * @param viewName 要检索的视图名称
	 * @param locale   要检索视图的区域设置
	 * @return 此解析器是否适用于指定的视图
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean canHandle(String viewName, Locale locale) {
		// 获取视图名称
		String[] viewNames = getViewNames();
		// 如果视图名称为空，返回true.
		// 如果当前视图和所有视图名称中的一个简单匹配，也返回true
		return (viewNames == null || PatternMatchUtils.simpleMatch(viewNames, viewName));
	}

	/**
	 * 返回此解析器所需的视图类型。
	 * 此实现返回{@link AbstractUrlBasedView}。
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
		// 获取视图类
		Class<?> viewClass = getViewClass();
		Assert.state(viewClass != null, "No view class");
		// 实例化视图类
		return (AbstractUrlBasedView) BeanUtils.instantiateClass(viewClass);
	}

	/**
	 * 委托给{@code buildView}来创建指定视图类的新实例。应用以下Spring生命周期方法
	 * (由通用Spring bean工厂支持):
	 * <ul>
	 * <li>ApplicationContextAware的{@code setApplicationContext}
	 * <li>InitializingBean的{@code afterPropertiesSet}
	 * </ul>
	 *
	 * @param viewName 要检索的视图的名称
	 * @return 视图实例
	 * @throws Exception 如果无法解析视图
	 * @see #buildView(String)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		// 根据视图名称构建视图
		AbstractUrlBasedView view = buildView(viewName);
		// 应用生命周期方法，并获取具体视图类
		View result = applyLifecycleMethods(viewName, view);
		// 如果该视图匹配当前区域设置，则返回该视图类，否则返回null。
		return (view.checkResource(locale) ? result : null);
	}

	/**
	 * 创建指定视图类的新View实例并对其进行配置。
	 * 不执行任何预定义View实例的查找。
	 * <p>此处不必调用bean容器定义的Spring生命周期方法;
	 * 在此方法返回后，{@code loadView}方法将应用这些方法。
	 * <p>子类通常会在设置进一步属性之前首先调用{@code super.buildView(viewName)}。
	 * {@code loadView}然后将在此过程结束时应用Spring生命周期方法。
	 *
	 * @param viewName 要构建的视图的名称
	 * @return 视图实例
	 * @throws Exception 如果无法解析视图
	 * @see #loadView(String, java.util.Locale)
	 */
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		// 实例化视图对象
		AbstractUrlBasedView view = instantiateView();
		// 设置视图的URL，根据前缀、视图名和后缀组成完整的URL
		view.setUrl(getPrefix() + viewName + getSuffix());
		// 设置视图的属性映射
		view.setAttributesMap(getAttributesMap());

		// 获取内容类型
		String contentType = getContentType();
		if (contentType != null) {
			// 如果内容类型不为空，则设置视图的内容类型
			view.setContentType(contentType);
		}

		// 获取请求上下文属性
		String requestContextAttribute = getRequestContextAttribute();
		if (requestContextAttribute != null) {
			// 如果请求上下文属性不为空，则设置视图的请求上下文属性
			view.setRequestContextAttribute(requestContextAttribute);
		}

		// 获取是否暴露路径变量
		Boolean exposePathVariables = getExposePathVariables();
		if (exposePathVariables != null) {
			// 如果设置了是否暴露路径变量，则设置视图的是否暴露路径变量属性
			view.setExposePathVariables(exposePathVariables);
		}

		// 获取是否将上下文Bean暴露为属性
		Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
		if (exposeContextBeansAsAttributes != null) {
			// 如果设置了是否将上下文Bean暴露为属性，则设置视图的是否暴露上下文Bean属性
			view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
		}

		// 获取需要暴露的上下文Bean名称数组
		String[] exposedContextBeanNames = getExposedContextBeanNames();
		if (exposedContextBeanNames != null) {
			// 如果设置了需要暴露的上下文Bean名称数组，则设置视图的需要暴露的上下文Bean名称数组属性
			view.setExposedContextBeanNames(exposedContextBeanNames);
		}

		return view;
	}

	/**
	 * 如果可用，则将包含的{@link ApplicationContext}的生命周期方法应用于给定的{@link View}实例。
	 *
	 * @param viewName 视图的名称
	 * @param view     刚刚创建的View实例，预先配置为具有{@link AbstractUrlBasedView}的属性
	 * @return 要使用的{@link View}实例（原始的或装饰的变体）
	 * @see #getApplicationContext()
	 * @see ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#initializeBean
	 * @since 5.0
	 */
	protected View applyLifecycleMethods(String viewName, AbstractUrlBasedView view) {
		// 获取应用程序上下文
		ApplicationContext context = getApplicationContext();
		if (context != null) {
			// 如果应用程序上下文不为空，则使用自动装配的Bean工厂初始化视图对象
			Object initialized = context.getAutowireCapableBeanFactory().initializeBean(view, viewName);
			if (initialized instanceof View) {
				// 如果初始化后的对象是视图对象，则返回初始化后的视图对象
				return (View) initialized;
			}
		}
		// 如果无法初始化，则直接返回原始的视图对象
		return view;
	}

}
