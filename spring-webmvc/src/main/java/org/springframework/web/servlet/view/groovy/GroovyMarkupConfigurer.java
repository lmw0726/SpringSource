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

package org.springframework.web.servlet.view.groovy;

import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import groovy.text.markup.TemplateResolver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Groovy 的 {@link groovy.text.markup.TemplateConfiguration} 扩展，
 * 以及 Spring MVC 的 {@link GroovyMarkupConfig} 实现，用于在 Web 应用程序中创建
 * {@code MarkupTemplateEngine} 供使用。配置此类最基本的方法是设置 "resourceLoaderPath"。
 * 例如：
 *
 * <pre class="code">
 *
 * // 将以下内容添加到一个 &#64;Configuration 类中
 *
 * &#64;Bean
 * public GroovyMarkupConfig groovyMarkupConfigurer() {
 *     GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
 *     configurer.setResourceLoaderPath("classpath:/WEB-INF/groovymarkup/");
 *     return configurer;
 * }
 * </pre>
 * <p>
 * 默认情况下，此 bean 将创建一个 {@link MarkupTemplateEngine}，具有：
 * <ul>
 * <li>用于加载带有引用的 Groovy 模板的父 ClassLoader
 * <li>基类 {@link TemplateConfiguration} 中的默认配置
 * <li>用于解析模板文件的 {@link groovy.text.markup.TemplateResolver}
 * </ul>
 * <p>
 * 您可以直接向此 bean 提供 {@link MarkupTemplateEngine} 实例，此时所有其他属性将被忽略。
 *
 * <p>此 bean 必须包含在使用 Spring MVC 的 {@link GroovyMarkupView} 进行呈现的任何应用程序的应用程序上下文中。
 * 它纯粹用于配置 Groovy 的 Markup 模板。它不应直接被应用程序组件引用。它实现了 GroovyMarkupConfig，
 * 以便在不依赖于 bean 名称的情况下被 GroovyMarkupView 发现。如果需要，每个 DispatcherServlet 都可以定义其自己的 GroovyMarkupConfigurer。
 *
 * <p>请注意，默认情况下，在 {@link MarkupTemplateEngine} 中启用了资源缓存。
 * 使用 {@link #setCacheTemplates(boolean)} 进行必要的配置。
 *
 * <p>Spring 的 Groovy Markup 模板支持需要 Groovy 2.3.1 或更高版本。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see GroovyMarkupView
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">
 * Groovy Markup Template engine documentation</a>
 * @since 4.1
 */
public class GroovyMarkupConfigurer extends TemplateConfiguration
		implements GroovyMarkupConfig, ApplicationContextAware, InitializingBean {

	/**
	 * Groovy Markup 模板资源加载路径
	 */
	private String resourceLoaderPath = "classpath:";

	/**
	 * Groovy Markup 模板引擎
	 */
	@Nullable
	private MarkupTemplateEngine templateEngine;

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;


	/**
	 * 设置 Groovy Markup 模板资源加载路径，使用 Spring 资源位置。
	 * 允许多个位置，作为逗号分隔的路径列表。
	 * 标准 URL（如 "file:" 和 "classpath:"）和伪 URL 受 Spring 的 {@link org.springframework.core.io.ResourceLoader} 的支持。
	 * 在 ApplicationContext 中运行时，允许相对路径。
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

	/**
	 * 设置预配置的 MarkupTemplateEngine，用于 Groovy Markup 模板 Web 配置。
	 * <p>请注意，此引擎实例必须手动配置，因为此配置器的所有其他 bean 属性将被忽略。
	 */
	public void setTemplateEngine(MarkupTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public MarkupTemplateEngine getTemplateEngine() {
		Assert.state(this.templateEngine != null, "No MarkupTemplateEngine set");
		return this.templateEngine;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		return this.applicationContext;
	}

	/**
	 * 不应使用此方法，因为解析模板的考虑 Locale 是当前 HTTP 请求的 Locale，
	 * 通过 {@link org.springframework.context.i18n.LocaleContextHolder LocaleContextHolder} 获取。
	 */
	@Override
	public void setLocale(Locale locale) {
		super.setLocale(locale);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.templateEngine == null) {
			this.templateEngine = createTemplateEngine();
		}
	}

	protected MarkupTemplateEngine createTemplateEngine() throws IOException {
		// 如果模板引擎为空
		if (this.templateEngine == null) {
			// 创建模板类加载器
			ClassLoader templateClassLoader = createTemplateClassLoader();
			// 使用模板类加载器和本地模板解析器创建标记模板引擎
			this.templateEngine = new MarkupTemplateEngine(templateClassLoader, this, new LocaleTemplateResolver());
		}
		// 返回模板引擎
		return this.templateEngine;
	}

	/**
	 * 创建用于 Groovy 加载和编译模板的父 ClassLoader。
	 */
	protected ClassLoader createTemplateClassLoader() throws IOException {
		// 将资源加载路径转换为字符串数组
		String[] paths = StringUtils.commaDelimitedListToStringArray(getResourceLoaderPath());

		// 创建 URL 列表
		List<URL> urls = new ArrayList<>();

		// 遍历路径数组
		for (String path : paths) {
			// 获取路径对应的资源数组
			Resource[] resources = getApplicationContext().getResources(path);
			// 如果资源数组不为空
			if (resources.length > 0) {
				// 遍历资源数组
				for (Resource resource : resources) {
					// 如果资源存在，则将其 URL 添加到 URL 列表中
					if (resource.exists()) {
						urls.add(resource.getURL());
					}
				}
			}
		}

		// 获取应用程序上下文的类加载器
		ClassLoader classLoader = getApplicationContext().getClassLoader();
		Assert.state(classLoader != null, "No ClassLoader");

		// 如果 URL 列表不为空，则创建 URLClassLoader，并设置父类加载器为应用程序上下文的类加载器
		// 否则返回应用程序上下文的类加载器
		return (!urls.isEmpty() ? new URLClassLoader(urls.toArray(new URL[0]), classLoader) : classLoader);
	}

	/**
	 * 解析给定模板路径的模板。
	 * 默认实现使用与当前请求关联的 Locale（通过 {@link org.springframework.context.i18n.LocaleContextHolder LocaleContextHolder} 获取），
	 * 查找模板文件。实际上，引擎级别配置的 Locale 被忽略。
	 *
	 * @see LocaleContextHolder
	 * @see #setLocale
	 */
	protected URL resolveTemplate(ClassLoader classLoader, String templatePath) throws IOException {
		// 解析模板路径
		MarkupTemplateEngine.TemplateResource resource = MarkupTemplateEngine.TemplateResource.parse(templatePath);

		// 获取当前语言环境
		Locale locale = LocaleContextHolder.getLocale();

		// 尝试根据带下划线的语言环境查找模板资源的 URL
		URL url = classLoader.getResource(resource.withLocale(StringUtils.replace(locale.toString(), "-", "_")).toString());

		if (url == null) {
			// 如果 URL 为空，则尝试根据语言查找模板资源的 URL
			url = classLoader.getResource(resource.withLocale(locale.getLanguage()).toString());
		}

		if (url == null) {
			// 如果 URL 为空，则尝试查找默认语言的模板资源的 URL
			url = classLoader.getResource(resource.withLocale(null).toString());
		}

		if (url == null) {
			// 如果 URL 仍为空，则抛出 IOException
			throw new IOException("Unable to load template:" + templatePath);
		}

		// 返回找到的 URL
		return url;
	}


	/**
	 * 简单委托给 {@link #resolveTemplate(ClassLoader, String)} 的自定义 {@link TemplateResolver 模板解析器}。
	 */
	private class LocaleTemplateResolver implements TemplateResolver {
		/**
		 * 类加载器
		 */
		@Nullable
		private ClassLoader classLoader;

		@Override
		public void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
			this.classLoader = templateClassLoader;
		}

		@Override
		public URL resolveTemplate(String templatePath) throws IOException {
			Assert.state(this.classLoader != null, "No template ClassLoader available");
			return GroovyMarkupConfigurer.this.resolveTemplate(this.classLoader, templatePath);
		}
	}

}
