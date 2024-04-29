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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.List;

/**
 * 用于配置 FreeMarker 以在 Web 中使用的 JavaBean，通过 "configLocation" 和/或 "freemarkerSettings"
 * 和/或 "templateLoaderPath" 属性。
 * 使用此类的最简单方式是仅指定 "templateLoaderPath"；然后您不需要进一步的配置。
 *
 * <pre class="code">
 * &lt;bean id="freemarkerConfig" class="org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer"&gt;
 *   &lt;property name="templateLoaderPath"&gt;&lt;value&gt;/WEB-INF/freemarker/&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 * <p>
 * 必须将此 bean 包含在使用 Spring 的 FreeMarkerView 进行 Web MVC 的任何应用程序的应用程序上下文中。
 * 它纯粹存在于配置 FreeMarker。
 * 它不是用于应用程序组件引用的，而是仅由 FreeMarkerView 内部引用。
 * 实现 FreeMarkerConfig 以被 FreeMarkerView 发现，而不依赖于配置器的 bean 名称。
 * 如果需要，每个 DispatcherServlet 可以定义自己的 FreeMarkerConfigurer。
 *
 * <p>请注意，您还可以引用预配置的 FreeMarker Configuration 实例，例如通过 FreeMarkerConfigurationFactoryBean 设置的实例，通过 "configuration" 属性。
 * 这允许在 Web 和电子邮件使用中共享 FreeMarker Configuration，例如。
 *
 * <p>此配置程序为此包注册一个模板加载程序，允许引用此包中包含的 "spring.ftl" 宏库：
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * age is ${spring.status.value}</pre>
 * <p>
 * 注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Darren Davison
 * @author Rob Harrop
 * @see #setConfigLocation
 * @see #setFreemarkerSettings
 * @see #setTemplateLoaderPath
 * @see #setConfiguration
 * @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
 * @see FreeMarkerView
 * @since 2004-03-03
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware, ServletContextAware {
	/**
	 * FreeMarker 配置
	 */
	@Nullable
	private Configuration configuration;

	/**
	 * 标签库工厂
	 */
	@Nullable
	private TaglibFactory taglibFactory;


	/**
	 * 设置要在 FreeMarker Web 配置中使用的预配置 Configuration，例如，为 Web 和电子邮件使用共享的配置，通过 FreeMarkerConfigurationFactoryBean 设置。
	 * 如果未设置此项，则必须指定 FreeMarkerConfigurationFactory 的属性（由此类继承）。
	 *
	 * @see org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 为给定的 ServletContext 初始化 {@link TaglibFactory}。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.taglibFactory = new TaglibFactory(servletContext);
	}


	/**
	 * 如果没有被预配置的 FreeMarker Configuration 覆盖，则初始化 FreeMarkerConfigurationFactory 的 Configuration。
	 * <p>设置 ClassTemplateLoader 用于加载 Spring 宏。
	 *
	 * @see #createConfiguration
	 * @see #setConfiguration
	 */
	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		if (this.configuration == null) {
			this.configuration = createConfiguration();
		}
	}

	/**
	 * 此实现为此包注册了一个额外的 ClassTemplateLoader，添加到列表的末尾。
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
	}


	/**
	 * 返回此 bean 包装的 Configuration 对象。
	 */
	@Override
	public Configuration getConfiguration() {
		Assert.state(this.configuration != null, "No Configuration available");
		return this.configuration;
	}

	/**
	 * 返回此 bean 包装的 TaglibFactory 对象。
	 */
	@Override
	public TaglibFactory getTaglibFactory() {
		Assert.state(this.taglibFactory != null, "No TaglibFactory available");
		return this.taglibFactory;
	}

}
