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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.View;

import java.util.*;

/**
 * 一个使用ResourceBundle中的bean定义的{@link org.springframework.web.servlet.ViewResolver}实现，由bundle基本名称指定。
 * <p>Bundle通常在类路径中的属性文件中定义。默认的bundle基本名称是“views”。
 * <p>此{@code ViewResolver}支持本地化视图定义，使用{@link java.util.PropertyResourceBundle}的默认支持。
 * 例如，基本名称“views”将被解析为类路径资源“views_de_AT.properties”、“views_de.properties”、“views.properties”-对于给定的区域设置“de_AT”。
 * <p>注意：此{@code ViewResolver}实现了{@link Ordered}接口，以便灵活参与{@code ViewResolver}链。例如，可以通过此{@code ViewResolver}定义一些特殊视图（将其“order”值设置为0），而所有剩余的视图都可以由{@link UrlBasedViewResolver}解析。
 *
 * @deprecated 自5.3起，建议使用Spring的通用视图解析器变体和/或自定义解析器实现
 */
@Deprecated
public class ResourceBundleViewResolver extends AbstractCachingViewResolver
		implements Ordered, InitializingBean, DisposableBean {

	/**
	 * 如果没有提供其他基本名称，则使用的默认基本名称。
	 */
	public static final String DEFAULT_BASENAME = "views";

	/**
	 * 资源的基本名称数组
	 */
	private String[] basenames = new String[]{DEFAULT_BASENAME};

	/**
	 * 资源包类加载器
	 */
	private ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();

	/**
	 * 默认的父视图名称
	 */
	@Nullable
	private String defaultParentView;

	/**
	 * 要初始化的区域设置
	 */
	@Nullable
	private Locale[] localesToInitialize;

	/**
	 * 资源的顺序
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/* 区域设置 -> BeanFactory */
	private final Map<Locale, BeanFactory> localeCache = new HashMap<>();

	/* ResourceBundle列表 -> BeanFactory */
	private final Map<List<ResourceBundle>, ConfigurableApplicationContext> bundleCache = new HashMap<>();


	/**
	 * 设置单个基本名称，遵循{@link java.util.ResourceBundle}的约定。
	 * 默认值为"views"。
	 * <p>{@code ResourceBundle}支持不同的区域设置后缀。例如，
	 * 基本名称为"views"可能对应{@code ResourceBundle}文件
	 * "views"、"views_en_au"和"views_de"。
	 * <p>请注意，ResourceBundle名称实际上是类路径位置：因此，
	 * JDK的标准ResourceBundle将点视为包分隔符。
	 * 这意味着"test.theme"实际上等同于"test/theme"，
	 * 就像对于编程式{@code java.util.ResourceBundle}用法一样。
	 *
	 * @see #setBasenames
	 * @see ResourceBundle#getBundle(String)
	 * @see ResourceBundle#getBundle(String, Locale)
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * 设置基本名称数组，每个名称都遵循{@link java.util.ResourceBundle}的约定。
	 * 默认情况下，只有一个基本名称"views"。
	 * <p>{@code ResourceBundle}支持不同的区域设置后缀。例如，
	 * 基本名称为"views"可能对应{@code ResourceBundle}文件
	 * "views"、"views_en_au"和"views_de"。
	 * <p>在解析消息代码时，相关联的资源包将按顺序检查。
	 * 请注意，由于顺序查找，<i>前一个</i>资源包中的消息定义将覆盖后续包中的消息定义。
	 * <p>请注意，ResourceBundle名称实际上是类路径位置：因此，
	 * JDK的标准ResourceBundle将点视为包分隔符。
	 * 这意味着"test.theme"实际上等同于"test/theme"，
	 * 就像对于编程式{@code java.util.ResourceBundle}用法一样。
	 *
	 * @see #setBasename
	 * @see ResourceBundle#getBundle(String)
	 * @see ResourceBundle#getBundle(String, Locale)
	 */
	public void setBasenames(String... basenames) {
		this.basenames = basenames;
	}

	/**
	 * 设置加载资源包的{@link ClassLoader}。
	 * 默认值为线程上下文{@code ClassLoader}。
	 */
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * 返回用于加载资源包的{@link ClassLoader}。
	 * <p>默认值为指定的bundle {@code ClassLoader}，
	 * 通常为线程上下文{@code ClassLoader}。
	 */
	protected ClassLoader getBundleClassLoader() {
		return this.bundleClassLoader;
	}

	/**
	 * 设置{@code ResourceBundle}中定义的视图的默认父级。
	 * <p>这样可以避免在bundle中重复定义"yyy1.(parent)=xxx"、"yyy2.(parent)=xxx"等内容，
	 * 尤其是如果所有已定义的视图共享相同的父级。
	 * <p>父级通常定义视图类和公共属性。
	 * 具体视图可能只是包含URL定义：
	 * 如"yyy1.url=/my.jsp"、"yyy2.url=/your.jsp"。
	 * <p>定义自己的父级或携带自己类的视图仍然可以覆盖此设置。
	 * 严格来说，一个默认父级设置不适用于携带类的bean定义，这是为了向后兼容。
	 * 它仍然符合典型用例。
	 */
	public void setDefaultParentView(String defaultParentView) {
		this.defaultParentView = defaultParentView;
	}

	/**
	 * 指定要急切初始化的区域设置，而不是在实际访问时延迟初始化。
	 * <p>允许预初始化常见的区域设置，急切地检查这些区域设置的视图配置。
	 */
	public void setLocalesToInitialize(Locale... localesToInitialize) {
		this.localesToInitialize = localesToInitialize;
	}

	/**
	 * 指定此ViewResolver bean的排序值。
	 * <p>默认值为{@code Ordered.LOWEST_PRECEDENCE}，意味着无序。
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
	 * 如有必要，急切地初始化区域设置。
	 *
	 * @see #setLocalesToInitialize
	 */
	@Override
	public void afterPropertiesSet() throws BeansException {
		if (this.localesToInitialize != null) {
			for (Locale locale : this.localesToInitialize) {
				initFactory(locale);
			}
		}
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		// 初始化bean工厂
		BeanFactory factory = initFactory(locale);
		try {
			// 获取根据视图名称获取视图实现类
			return factory.getBean(viewName, View.class);
		} catch (NoSuchBeanDefinitionException ex) {
			// 允许ViewResolver链...
			return null;
		}
	}

	/**
	 * 初始化给定区域设置的View {@link BeanFactory}从{@code ResourceBundle}中。
	 * <p>由于并行线程的访问而同步。
	 *
	 * @param locale 目标{@code Locale}
	 * @return 给定Locale的View工厂
	 * @throws BeansException 在初始化错误的情况下
	 */
	protected synchronized BeanFactory initFactory(Locale locale) throws BeansException {
		// 尝试查找已缓存的用于特定Locale的工厂:
		// 我们之前是否已经遇到过这个Locale？
		if (isCache()) {
			BeanFactory cachedFactory = this.localeCache.get(locale);
			if (cachedFactory != null) {
				return cachedFactory;
			}
		}

		// 构建Locale的ResourceBundle引用列表。
		List<ResourceBundle> bundles = new ArrayList<>(this.basenames.length);
		for (String basename : this.basenames) {
			bundles.add(getBundle(basename, locale));
		}

		// 尝试查找已缓存的用于ResourceBundle列表的工厂:
		// 即使Locale不同，相同的bundles可能已经被找到了。
		if (isCache()) {
			BeanFactory cachedFactory = this.bundleCache.get(bundles);
			if (cachedFactory != null) {
				// 添加仅区域设置缓存中
				this.localeCache.put(locale, cachedFactory);
				return cachedFactory;
			}
		}

		// 创建用于视图的子ApplicationContext。
		GenericWebApplicationContext factory = new GenericWebApplicationContext();
		// 设置父应用上下文
		factory.setParent(getApplicationContext());
		// 设置Servlet上下文
		factory.setServletContext(getServletContext());

		// 从资源包加载bean定义。
		org.springframework.beans.factory.support.PropertiesBeanDefinitionReader reader =
				new org.springframework.beans.factory.support.PropertiesBeanDefinitionReader(factory);
		reader.setDefaultParentBean(this.defaultParentView);
		for (ResourceBundle bundle : bundles) {
			// 从资源包中注册bean定义
			reader.registerBeanDefinitions(bundle);
		}
		// 刷新bean工厂
		factory.refresh();

		// 对Locale和ResourceBundle列表都进行缓存。
		if (isCache()) {
			this.localeCache.put(locale, factory);
			this.bundleCache.put(bundles, factory);
		}

		return factory;
	}

	/**
	 * 获取给定基本名称和{@link Locale}的资源包。
	 *
	 * @param basename 要查找的基本名称
	 * @param locale   要查找的{@code Locale}
	 * @return 相应的{@code ResourceBundle}
	 * @throws MissingResourceException 如果找不到匹配的bundle
	 * @see ResourceBundle#getBundle(String, Locale, ClassLoader)
	 */
	protected ResourceBundle getBundle(String basename, Locale locale) throws MissingResourceException {
		return ResourceBundle.getBundle(basename, locale, getBundleClassLoader());
	}


	/**
	 * 在上下文关闭时关闭bundle View工厂。
	 */
	@Override
	public void destroy() throws BeansException {
		for (ConfigurableApplicationContext factory : this.bundleCache.values()) {
			// 关闭bean工厂
			factory.close();
		}
		// 清空区域设置缓存
		this.localeCache.clear();
		// 清空资源包缓存
		this.bundleCache.clear();
	}

}
