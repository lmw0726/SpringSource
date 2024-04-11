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

package org.springframework.web.servlet.view.freemarker;

import freemarker.core.ParseException;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.*;
import freemarker.template.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractTemplateView;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * 使用FreeMarker模板引擎的视图。
 *
 * <p>暴露以下JavaBean属性：
 * <ul>
 * <li><b>url</b>: 要包装的FreeMarker模板的位置，相对于FreeMarker模板上下文（目录）。
 * <li><b>encoding</b>（可选，默认由FreeMarker配置确定）：FreeMarker模板文件的编码。
 * </ul>
 *
 * <p>依赖于单个{@link FreeMarkerConfig}对象，例如在当前Web应用程序上下文中可访问的{@link FreeMarkerConfigurer}，
 * 并使用任何bean名称。或者，您可以将FreeMarker {@link Configuration}对象设置为bean属性。
 * 有关此方法的影响的更多详细信息，请参见{@link #setConfiguration}。
 *
 * <p>注意：Spring的FreeMarker支持需要FreeMarker 2.3或更高版本。
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @see #setUrl
 * @see #setExposeSpringMacroHelpers
 * @see #setEncoding
 * @see #setConfiguration
 * @see FreeMarkerConfig
 * @see FreeMarkerConfigurer
 * @since 03.03.2004
 */
public class FreeMarkerView extends AbstractTemplateView {

	/**
	 * 编码方式
	 */
	@Nullable
	private String encoding;
	/**
	 * FreeMark配置
	 */
	@Nullable
	private Configuration configuration;
	/**
	 * JSP标签库工厂
	 */
	@Nullable
	private TaglibFactory taglibFactory;

	/**
	 * Servlet上下文哈希模型
	 */
	@Nullable
	private ServletContextHashModel servletContextHashModel;


	/**
	 * 设置FreeMarker模板文件的编码。如果未指定，则默认由FreeMarker Configuration确定："ISO-8859-1"。
	 * <p>如果所有模板共享通用编码，则在FreeMarker Configuration中指定编码，而不是每个模板都指定。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 返回FreeMarker模板的编码。
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置此视图要使用的FreeMarker Configuration。
	 * <p>如果未设置，则将发生默认查找：在当前Web应用程序上下文中期望有一个{@link FreeMarkerConfig}，
	 * 并且可以使用任何bean名称。
	 * <strong>注意：</strong>使用此方法将导致为每个{@link FreeMarkerView}实例创建一个新的{@link TaglibFactory}实例。
	 * 从内存和初始CPU使用的角度来看，这可能相当昂贵。在生产中，建议使用一个{@link FreeMarkerConfig}，
	 * 其中包含一个共享的{@link TaglibFactory}。
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 返回此视图使用的FreeMarker配置。
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * 获取用于实际使用的FreeMarker配置。
	 *
	 * @return FreeMarker配置（永远不会为{@code null}）
	 * @throws IllegalStateException 如果未设置Configuration对象
	 * @since 5.0
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "No Configuration set");
		return configuration;
	}


	/**
	 * 在启动时调用。查找单个FreeMarkerConfig bean以找到此工厂的相关Configuration。
	 * <p>检查是否可以找到默认Locale的模板：如果找不到特定于语言环境的模板，
	 * FreeMarker将检查非特定于语言环境的模板。
	 *
	 * @see freemarker.cache.TemplateCache#getTemplate
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) throws BeansException {
		if (getConfiguration() != null) {
			// 如果已经设置了 FreeMarker 的配置，则使用已有的配置初始化JSP标签库工厂
			this.taglibFactory = new TaglibFactory(servletContext);
		} else {
			// 如果尚未设置 FreeMarker 的配置，则自动检测并设置配置
			FreeMarkerConfig config = autodetectConfiguration();
			// 设置FreeMarker配置
			setConfiguration(config.getConfiguration());
			// 根据FreeMarker配置获取JSP标签库工厂
			this.taglibFactory = config.getTaglibFactory();
		}

		GenericServlet servlet = new GenericServletAdapter();
		try {
			//根据代理Servlet配置初始化通用Servlet
			servlet.init(new DelegatingServletConfig());
		} catch (ServletException ex) {
			// 如果初始化失败，则抛出 BeanInitializationException
			throw new BeanInitializationException("Initialization of GenericServlet adapter failed", ex);
		}

		// 根据通用Servlet和包装对象创建  servlet上下文哈希模型
		this.servletContextHashModel = new ServletContextHashModel(servlet, getObjectWrapper());
	}

	/**
	 * 通过ApplicationContext自动检测一个{@link FreeMarkerConfig}对象。
	 *
	 * @return 用于FreeMarkerViews的Configuration实例
	 * @throws BeansException 如果找不到Configuration实例
	 * @see #getApplicationContext
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			// 尝试获取包括祖先在内的 FreeMarkerConfig 类型的 Bean
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		} catch (NoSuchBeanDefinitionException ex) {
			// 如果找不到指定类型的 Bean，则抛出 ApplicationContextException
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this web application context " +
							"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
							"This bean may be given any name.", ex);
		}
	}

	/**
	 * 返回配置的FreeMarker {@link ObjectWrapper}，如果未指定，则返回{@link ObjectWrapper#DEFAULT_WRAPPER default wrapper}。
	 *
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		// 获取配置中的对象包装器
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();

		// 如果对象包装器不为 null，则返回它；否则返回默认的对象包装器
		return (ow != null ? ow :
				new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
	}

	/**
	 * 检查此视图使用的FreeMarker模板是否存在且有效。
	 * <p>可以重写以自定义行为，例如在多个模板渲染到单个视图的情况下。
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		try {
			// 检查是否可以获取模板，即使我们可能随后再次获取它。
			getTemplate(url, locale);
			return true;
		} catch (FileNotFoundException ex) {
			// 允许ViewResolver链接...
			return false;
		} catch (ParseException ex) {
			throw new ApplicationContextException("Failed to parse [" + url + "]", ex);
		} catch (IOException ex) {
			throw new ApplicationContextException("Failed to load [" + url + "]", ex);
		}
	}


	/**
	 * 处理模型映射，将其与FreeMarker模板合并。输出定向到Servlet响应。
	 * <p>如果需要自定义行为，可以重写此方法。
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 将模型中的辅助方法暴露给JSP标签，以便在模板中使用。
		exposeHelpers(model, request);

		// 执行渲染操作，将模型和请求数据传递给模板，并将结果写入响应。
		doRender(model, request, response);
	}

	/**
	 * 暴露每个呈现操作特有的辅助程序。这是必需的，以便不同的渲染操作不能覆盖彼此的格式等。
	 * <p>由{@code renderMergedTemplateModel}调用。默认实现为空。可以重写此方法以向模型添加自定义辅助程序。
	 *
	 * @param model   将在合并时传递到模板的模型
	 * @param request 当前HTTP请求
	 * @throws Exception 如果在向上下文添加信息时出现致命错误
	 * @see #renderMergedTemplateModel
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * 将FreeMarker视图呈现到给定的响应中，使用包含完整模板模型的给定模型映射。
	 * <p>默认实现呈现由"url" bean属性指定的模板，通过{@code getTemplate}检索。
	 * 它委托给{@code processTemplate}方法，将模板实例与给定的模板模型合并。
	 * <p>将标准的FreeMarker哈希模型添加到模型中：请求参数、请求、会话和应用程序（ServletContext），
	 * 以及JSP标签库哈希模型。
	 * <p>可以重写此方法以自定义行为，例如将多个模板呈现到单个视图中。
	 *
	 * @param model    用于呈现的模型
	 * @param request  当前HTTP请求
	 * @param response 当前Servlet响应
	 * @throws IOException 如果无法检索模板文件
	 * @throws Exception   如果呈现失败
	 * @see #setUrl
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see #getTemplate(java.util.Locale)
	 * @see #processTemplate
	 * @see freemarker.ext.servlet.FreemarkerServlet
	 */
	protected void doRender(Map<String, Object> model, HttpServletRequest request,
							HttpServletResponse response) throws Exception {

		// 将模型暴露给JSP标签（作为请求属性）。
		exposeModelAsRequestAttributes(model, request);

		// 暴露所有标准的FreeMarker哈希模型。
		SimpleHash fmModel = buildTemplateModel(model, request, response);

		// 根据请求获取区域设置
		Locale locale = RequestContextUtils.getLocale(request);

		// 使用特定的模板和模型处理请求，并将结果写入响应。
		processTemplate(getTemplate(locale), fmModel, response);
	}

	/**
	 * 为给定的模型Map构建一个FreeMarker模板模型。
	 * <p>默认实现构建一个{@link AllHttpScopesHashModel}。
	 *
	 * @param model    用于呈现的模型
	 * @param request  当前HTTP请求
	 * @param response 当前Servlet响应
	 * @return FreeMarker模板模型，作为{@link SimpleHash}或其子类
	 */
	protected SimpleHash buildTemplateModel(Map<String, Object> model, HttpServletRequest request,
											HttpServletResponse response) {

		// 创建一个新的AllHttpScopesHashModel对象，包含所有HTTP作用域的数据。
		AllHttpScopesHashModel fmModel = new AllHttpScopesHashModel(getObjectWrapper(), getServletContext(), request);

		// 添加JSP标签库到模型中。
		fmModel.put(FreemarkerServlet.KEY_JSP_TAGLIBS, this.taglibFactory);

		// 添加应用程序级别的数据到模型中。
		fmModel.put(FreemarkerServlet.KEY_APPLICATION, this.servletContextHashModel);

		// 添加会话级别的数据到模型中。
		fmModel.put(FreemarkerServlet.KEY_SESSION, buildSessionModel(request, response));

		// 添加请求级别的数据到模型中。
		fmModel.put(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, response, getObjectWrapper()));

		// 添加请求参数到模型中。
		fmModel.put(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));

		// 将传入的model中的所有数据添加到模型中。
		fmModel.putAll(model);

		// 返回创建好的模型。
		return fmModel;
	}

	/**
	 * 为给定请求构建一个FreeMarker {@link HttpSessionHashModel}，
	 * 检测是否已经存在会话并相应地进行反应。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前Servlet响应
	 * @return FreeMarker HttpSessionHashModel
	 */
	private HttpSessionHashModel buildSessionModel(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			// 如果会话存在，则根据会话和包装对象构建 Http会话哈希模型
			return new HttpSessionHashModel(session, getObjectWrapper());
		} else {
			// 否则根据请求、响应以及包装对象构建 Http会话哈希模型
			return new HttpSessionHashModel(null, request, response, getObjectWrapper());
		}
	}

	/**
	 * 检索用于给定区域设置的FreeMarker模板，以便由此视图进行渲染。
	 * 默认情况下，将检索由"url" bean属性指定的模板。
	 *
	 * @param locale 当前区域设置
	 * @return 要渲染的FreeMarker模板
	 * @throws IOException 如果无法检索模板文件
	 * @see #setUrl
	 * @see #getTemplate(String, java.util.Locale)
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return getTemplate(url, locale);
	}

	/**
	 * 按名称检索由给定名称指定的FreeMarker模板，
	 * 使用由"encoding" bean属性指定的编码。
	 * 可以由子类调用以检索特定模板，例如将多个模板呈现到单个视图中。
	 *
	 * @param name   要检索的模板的文件名
	 * @param locale 当前区域设置
	 * @return FreeMarker模板
	 * @throws IOException 如果无法检索模板文件
	 */
	protected Template getTemplate(String name, Locale locale) throws IOException {
		// 如果编码不为空，则使用带编码参数的函数获取模板。
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(name, locale, getEncoding()) :
				obtainConfiguration().getTemplate(name, locale));
	}

	/**
	 * 将FreeMarker模板处理到servlet响应。
	 * 可以重写以自定义行为。
	 *
	 * @param template 要处理的模板
	 * @param model    模板的模型
	 * @param response servlet响应（使用此响应获取OutputStream或Writer）
	 * @throws IOException       如果无法检索模板文件
	 * @throws TemplateException 如果由FreeMarker抛出
	 * @see freemarker.template.Template#process(Object, java.io.Writer)
	 */
	protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response)
			throws IOException, TemplateException {

		template.process(model, response.getWriter());
	}


	/**
	 * 简单的适配器类，继承自{@link GenericServlet}。
	 * 在FreeMarker中需要用于访问JSP。
	 */
	@SuppressWarnings("serial")
	private static class GenericServletAdapter extends GenericServlet {

		@Override
		public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
			// no-op
		}
	}


	/**
	 * 实现了{@link ServletConfig}接口的内部类，用于传递给servlet适配器。
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		@Nullable
		public String getServletName() {
			return FreeMarkerView.this.getBeanName();
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return FreeMarkerView.this.getServletContext();
		}

		@Override
		@Nullable
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.emptySet());
		}
	}

}
