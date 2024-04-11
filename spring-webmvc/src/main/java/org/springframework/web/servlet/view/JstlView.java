/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * 用于JSTL页面的{@link InternalResourceView}的专门化，即使用JSP标准标记库的JSP页面。
 *
 * <p>公开了JSTL特定的请求属性，指定了JSTL的格式化和消息标记的区域设置和资源包，
 * 使用Spring的区域设置和{@link org.springframework.context.MessageSource}。
 *
 * <p>使用{@link InternalResourceViewResolver}的典型用法如下，
 * 从DispatcherServlet上下文定义的角度来看：
 *
 * <pre class="code">
 * &lt;bean id="viewResolver" class="org.springframework.web.servlet.view.InternalResourceViewResolver"&gt;
 *   &lt;property name="viewClass" value="org.springframework.web.servlet.view.JstlView"/&gt;
 *   &lt;property name="prefix" value="/WEB-INF/jsp/"/&gt;
 *   &lt;property name="suffix" value=".jsp"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"&gt;
 *   &lt;property name="basename" value="messages"/&gt;
 * &lt;/bean&gt;</pre>
 * <p>
 * 从处理程序返回的每个视图名称都将被翻译为JSP资源（例如：“myView” &rarr; “/WEB-INF/jsp/myView.jsp”），使用此视图类来启用显式的JSTL支持。
 *
 * <p>指定的MessageSource从类路径中的“messages.properties”等文件加载消息。
 * 这将自动作为JSTL本地化上下文暴露给视图，JSTL fmt标记（消息等）将使用该上下文。
 * 考虑使用Spring的ReloadableResourceBundleMessageSource而不是标准的ResourceBundleMessageSource以获得更多复杂性。
 * 当然，任何其他Spring组件都可以共享同一个MessageSource。
 *
 * <p>这是一个单独的类，主要是为了避免在{@link InternalResourceView}本身中出现JSTL依赖。
 * JSTL直到J2EE 1.4之前都不是标准的J2EE的一部分，因此我们不能假设JSTL API jar可用于类路径。
 *
 * <p>提示：将{@link #setExposeContextBeansAsAttributes}标志设置为“true”，
 * 以便使应用程序上下文中的所有Spring bean都可以在JSTL表达式中访问（例如，在{@code c:out}值表达式中）。
 * 这也将使所有这样的bean可以在JSP 2.0页面中的普通{@code ${...}}表达式中访问。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.support.JstlUtils#exposeLocalizationContext
 * @see InternalResourceViewResolver
 * @see org.springframework.context.support.ResourceBundleMessageSource
 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
 * @since 27.02.2003
 */
public class JstlView extends InternalResourceView {
	/**
	 * 消息源
	 */
	@Nullable
	private MessageSource messageSource;


	/**
	 * 用作bean的构造函数。
	 *
	 * @see #setUrl
	 */
	public JstlView() {
	}

	/**
	 * 使用给定的URL创建一个新的JstlView。
	 *
	 * @param url 要转发到的URL
	 */
	public JstlView(String url) {
		super(url);
	}

	/**
	 * 使用给定的URL创建一个新的JstlView。
	 *
	 * @param url           要转发到的URL
	 * @param messageSource 要暴露给JSTL标记的MessageSource
	 *                      （将被一个了解JSTL的MessageSource包装，该MessageSource了解JSTL的
	 *                      {@code javax.servlet.jsp.jstl.fmt.localizationContext}上下文参数）
	 * @see JstlUtils#getJstlAwareMessageSource
	 */
	public JstlView(String url, MessageSource messageSource) {
		this(url);
		this.messageSource = messageSource;
	}


	/**
	 * 使用了一个了解JSTL的MessageSource，它了解JSTL的
	 * {@code javax.servlet.jsp.jstl.fmt.localizationContext}
	 * 上下文参数包装了MessageSource。
	 *
	 * @see JstlUtils#getJstlAwareMessageSource
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) {
		if (this.messageSource != null) {
			// 如果消息源不为空，则将消息源重置为Jstl感知消息源
			this.messageSource = JstlUtils.getJstlAwareMessageSource(servletContext, this.messageSource);
		}
		// 初始化Servlet上下文
		super.initServletContext(servletContext);
	}

	/**
	 * 为Spring的区域设置和MessageSource公开了一个JSTL LocalizationContext。
	 *
	 * @see JstlUtils#exposeLocalizationContext
	 */
	@Override
	protected void exposeHelpers(HttpServletRequest request) throws Exception {
		// 如果消息源不为空，将其暴露给 JSTL 视图
		if (this.messageSource != null) {
			JstlUtils.exposeLocalizationContext(request, this.messageSource);
		} else {
			// 否则，创建新的 请求上下文，并将其暴露给 JSTL 视图
			JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
		}
	}

}
