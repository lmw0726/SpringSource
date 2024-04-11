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

package org.springframework.web.servlet.view.groovy;

import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.springframework.web.util.NestedServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * 基于Groovy XML/XHTML标记模板的{@link AbstractTemplateView}子类。
 *
 * <p>Spring的Groovy标记模板支持需要Groovy 2.3.1及更高版本。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see GroovyMarkupViewResolver
 * @see GroovyMarkupConfigurer
 * @see <a href="http://groovy-lang.org/templating.html#_the_markuptemplateengine">
 * Groovy标记模板引擎文档</a>
 * @since 4.1
 */
public class GroovyMarkupView extends AbstractTemplateView {
	/**
	 * 标记模板引擎
	 */
	@Nullable
	private MarkupTemplateEngine engine;


	/**
	 * 设置此视图中要使用的MarkupTemplateEngine。
	 * <p>如果未设置，将通过查找Web应用程序上下文中的单个{@link GroovyMarkupConfig} bean
	 * 并使用它来获取配置的{@code MarkupTemplateEngine}实例。
	 *
	 * @see GroovyMarkupConfig
	 */
	public void setTemplateEngine(MarkupTemplateEngine engine) {
		this.engine = engine;
	}

	/**
	 * 在启动时调用。
	 * 如果未手动设置{@link #setTemplateEngine(MarkupTemplateEngine) templateEngine}，
	 * 此方法将通过类型查找{@link GroovyMarkupConfig} bean并使用它来获取Groovy标记模板引擎。
	 *
	 * @see GroovyMarkupConfig
	 * @see #setTemplateEngine(groovy.text.markup.MarkupTemplateEngine)
	 */
	@Override
	protected void initApplicationContext(ApplicationContext context) {
		// 调用父类的初始化应用上下文方法
		super.initApplicationContext();
		if (this.engine == null) {
			// 如果模板引擎不存在，则自动检测标记模板引擎
			setTemplateEngine(autodetectMarkupTemplateEngine());
		}
	}

	/**
	 * 通过ApplicationContext自动检测MarkupTemplateEngine。
	 * 如果未手动配置MarkupTemplateEngine，则调用此方法。
	 */
	protected MarkupTemplateEngine autodetectMarkupTemplateEngine() throws BeansException {
		try {
			// 构建Groovy标记配置，并获取模板引擎
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(obtainApplicationContext(),
					GroovyMarkupConfig.class, true, false).getTemplateEngine();
		} catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single GroovyMarkupConfig bean in the current " +
					"Servlet web application context or the parent root context: GroovyMarkupConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}


	@Override
	public boolean checkResource(Locale locale) throws Exception {
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");
		try {
			// 通过URL解析模板
			this.engine.resolveTemplate(getUrl());
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model,
											 HttpServletRequest request, HttpServletResponse response) throws Exception {

		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		// 获取模板
		Template template = getTemplate(url);
		// 写入模型后，写入到HTTP相应中
		template.make(model).writeTo(new BufferedWriter(response.getWriter()));
	}

	/**
	 * 返回由配置的Groovy标记模板引擎编译的给定视图URL的模板。
	 */
	protected Template getTemplate(String viewUrl) throws Exception {
		Assert.state(this.engine != null, "No MarkupTemplateEngine set");
		try {
			// 根据视图URL创建模板
			return this.engine.createTemplateByPath(viewUrl);
		} catch (ClassNotFoundException ex) {
			Throwable cause = (ex.getCause() != null ? ex.getCause() : ex);
			throw new NestedServletException(
					"Could not find class while rendering Groovy Markup view with name '" +
							getUrl() + "': " + ex.getMessage() + "'", cause);
		}
	}

}
