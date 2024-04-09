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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

import java.util.Locale;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 的实现，使用专用的 XML 文件作为视图定义的 bean 定义，
 * 资源位置由 location 指定。
 * 该文件通常位于 WEB-INF 目录中；默认值为 "/WEB-INF/views.xml"。
 *
 * <p>此 {@code ViewResolver} 不支持在其定义资源级别进行国际化。如果需要针对每个区域设置不同的视图资源，
 * 请考虑使用 {@link ResourceBundleViewResolver}。
 *
 * <p>注意：此 {@code ViewResolver} 实现了 {@link Ordered} 接口，以允许灵活地参与 {@code ViewResolver} 链。
 * 例如，可以通过此 {@code ViewResolver} 定义一些特殊视图（将其作为 "order" 值为 0 的参数），
 * 而所有其他视图可以由 {@link UrlBasedViewResolver} 解析。
 *
 * @author Juergen Hoeller
 * @see org.springframework.context.ApplicationContext#getResource
 * @see UrlBasedViewResolver
 * @see BeanNameViewResolver
 * @since 2003-06-18
 * @deprecated 自 5.3 版本起，建议使用 Spring 提供的通用视图解析器变种和/或自定义解析器实现
 */
@Deprecated
public class XmlViewResolver extends AbstractCachingViewResolver
		implements Ordered, InitializingBean, DisposableBean {

	/**
	 * 如果没有其他位置，则使用默认位置。
	 */
	public static final String DEFAULT_LOCATION = "/WEB-INF/views.xml";

	/**
	 * 资源位置
	 */
	@Nullable
	private Resource location;

	/**
	 * 缓存的bean工厂
	 */
	@Nullable
	private ConfigurableApplicationContext cachedFactory;

	/**
	 * 默认值：与非有序相同
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置定义视图 bean 的 XML 文件的位置。
	 * <p>默认值为 "/WEB-INF/views.xml"。
	 *
	 * @param location XML 文件的位置。
	 */
	public void setLocation(Resource location) {
		this.location = location;
	}

	/**
	 * 为此 ViewResolver bean 指定排序值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示非有序。
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

	/**
	 * 预先从 XML 文件初始化工厂。
	 * 仅在启用缓存时有效。
	 */
	@Override
	public void afterPropertiesSet() throws BeansException {
		if (isCache()) {
			// 如果启用了缓存，则初始化bean工厂
			initFactory();
		}
	}


	/**
	 * 此实现仅返回视图名称，
	 * 因为 XmlViewResolver 不支持本地化解析。
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws BeansException {
		// 初始化bean工厂
		BeanFactory factory = initFactory();
		try {
			// 根据视图名称加载具体视图类
			return factory.getBean(viewName, View.class);
		} catch (NoSuchBeanDefinitionException ex) {
			// 允许 ViewResolver 链接...
			return null;
		}
	}

	/**
	 * 从 XML 文件初始化视图 bean 工厂。
	 * 因为有并行线程访问，所以采用同步方式。
	 *
	 * @throws BeansException 如果初始化出现错误
	 */
	protected synchronized BeanFactory initFactory() throws BeansException {
		if (this.cachedFactory != null) {
			// 如果已经有缓存的工厂，则直接返回它
			return this.cachedFactory;
		}

		// 获取应用程序上下文
		ApplicationContext applicationContext = obtainApplicationContext();

		Resource actualLocation = this.location;
		if (actualLocation == null) {
			// 如果没有指定位置，则使用默认位置
			actualLocation = applicationContext.getResource(DEFAULT_LOCATION);
		}

		// 创建用于视图的子 ApplicationContext
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		// 设置父应用上下文
		factory.setParent(applicationContext);
		// 设置Servlet应用上下文
		factory.setServletContext(getServletContext());

		// 使用上下文感知的实体解析器加载 XML 资源
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
		// 设置环境
		reader.setEnvironment(applicationContext.getEnvironment());
		// 设置实体解析器
		reader.setEntityResolver(new ResourceEntityResolver(applicationContext));
		// 从实际位置上加载bean定义
		reader.loadBeanDefinitions(actualLocation);

		// 刷新应用上下文
		factory.refresh();

		if (isCache()) {
			// 如果启用了缓存，则将工厂缓存起来
			this.cachedFactory = factory;
		}
		return factory;
	}


	/**
	 * 在上下文关闭时关闭视图 bean 工厂。
	 */
	@Override
	public void destroy() throws BeansException {
		if (this.cachedFactory != null) {
			this.cachedFactory.close();
		}
	}

}
