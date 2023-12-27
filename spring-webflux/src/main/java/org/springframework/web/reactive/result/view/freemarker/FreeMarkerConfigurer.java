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

package org.springframework.web.reactive.result.view.freemarker;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;

/**
 * 通过“configLocation”、“freemarkerSettings”和/或“templateLoaderPath”属性配置 FreeMarker 以供 Web 使用。
 * 最简单的使用方式是仅指定“templateLoaderPath”（例如“classpath:templates”）；然后您无需进一步配置。
 *
 * <p>此 Bean 必须包含在任何使用 {@link FreeMarkerView} 的应用程序上下文中。它纯粹用于配置 FreeMarker。不是用于被应用程序组件引用，而是由 {@code FreeMarkerView} 内部引用。
 * 实现 {@link FreeMarkerConfig} 以供 {@code FreeMarkerView} 发现，而不依赖于配置器的 Bean 名称。
 *
 * <p>请注意，还可以通过“configuration”属性引用预配置的 FreeMarker Configuration 实例。这允许共享用于 Web 和邮件使用的 FreeMarker Configuration。
 *
 * <p>此配置器为此包注册了模板加载程序，允许引用此包中包含的“spring.ftl”宏库：
 *
 * <pre class="code">
 * &lt;#import "/spring.ftl" as spring/&gt;
 * &lt;@spring.bind "person.age"/&gt;
 * 年龄是 ${spring.status.value}</pre>
 * <p>
 * 注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FreeMarkerConfigurer extends FreeMarkerConfigurationFactory
		implements FreeMarkerConfig, InitializingBean, ResourceLoaderAware {

	@Nullable
	private Configuration configuration;


	public FreeMarkerConfigurer() {
		setDefaultEncoding("UTF-8");
	}


	/**
	 * 设置要用于 FreeMarker Web 配置的预配置 Configuration，例如用于 Web 和邮件使用的共享配置。如果未设置此项，必须指定 FreeMarkerConfigurationFactory 的属性（此类继承了这些属性）。
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}


	/**
	 * 如果没有通过预配置的 FreeMarker Configuration 覆盖，初始化 FreeMarkerConfigurationFactory 的 Configuration。
	 * <p>设置一个 ClassTemplateLoader 以用于加载 Spring 宏。
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
	 * 此实现为 Spring 提供的宏注册了额外的 ClassTemplateLoader，添加到列表末尾。
	 */
	@Override
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
		templateLoaders.add(new ClassTemplateLoader(FreeMarkerConfigurer.class, ""));
	}


	/**
	 * 返回此 Bean 封装的 Configuration 对象。
	 */
	@Override
	public Configuration getConfiguration() {
		Assert.state(this.configuration != null, "No Configuration available");
		return this.configuration;
	}

}
