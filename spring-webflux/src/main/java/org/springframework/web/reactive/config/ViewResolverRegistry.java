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

package org.springframework.web.reactive.config;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.result.view.HttpMessageWriterView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.reactive.result.view.script.ScriptTemplateConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 辅助配置支持不同模板机制的 {@link ViewResolver} 链的属性。
 *
 * 此外，还可以通过 {@link #defaultViews(View...)} 配置默认视图，根据请求的内容类型进行选择，例如 JSON、XML 等。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ViewResolverRegistry {

	/**
	 * 应用程序上下文
	 */
	@Nullable
	private final ApplicationContext applicationContext;

	/**
	 * 视图解析器列表
	 */
	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	/**
	 * 默认视图列表
	 */
	private final List<View> defaultViews = new ArrayList<>(4);

	/**
	 * 顺序
	 */
	@Nullable
	private Integer order;


	public ViewResolverRegistry(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * 注册一个带有“.ftl”后缀的 {@code FreeMarkerViewResolver}。
	 * <p><strong>注意</strong>，您还必须通过添加 {@link FreeMarkerConfigurer} bean 配置 FreeMarker。
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		// 检查是否存在 FreeMarkerConfigurer 类型的 Bean
		if (!checkBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
					"there must also be a single FreeMarkerConfig bean in this web application context " +
					"(or its parent): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}

		// 创建 FreeMarker 视图的注册信息
		FreeMarkerRegistration registration = new FreeMarkerRegistration();

		// 获取视图解析器
		UrlBasedViewResolver resolver = registration.getViewResolver();

		// 如果应用程序上下文不为空，则设置到视图解析器中
		if (this.applicationContext != null) {
			resolver.setApplicationContext(this.applicationContext);
		}

		// 将视图解析器添加到视图解析器列表中
		this.viewResolvers.add(resolver);

		// 返回 FreeMarker 视图的注册信息
		return registration;
	}

	/**
	 * 使用空的默认视图名称前缀和后缀注册脚本模板视图解析器。
	 * <p><strong>注意</strong>，您还必须通过添加 {@link ScriptTemplateConfigurer} bean 配置脚本模板。
	 * @since 5.0.4
	 */
	public UrlBasedViewResolverRegistration scriptTemplate() {
		// 检查是否存在 ScriptTemplateConfigurer 类型的 Bean
		if (!checkBeanOfType(ScriptTemplateConfigurer.class)) {
			throw new BeanInitializationException("In addition to a script template view resolver " +
					"there must also be a single ScriptTemplateConfig bean in this web application context " +
					"(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}

		// 创建 Script 模板视图的注册信息
		ScriptRegistration registration = new ScriptRegistration();

		// 获取视图解析器
		UrlBasedViewResolver resolver = registration.getViewResolver();

		// 如果应用程序上下文不为空，则设置到视图解析器中
		if (this.applicationContext != null) {
			resolver.setApplicationContext(this.applicationContext);
		}

		// 将视图解析器添加到视图解析器列表中
		this.viewResolvers.add(resolver);

		// 返回 Script 模板视图的注册信息
		return registration;
	}

	/**
	 * 注册一个 {@link ViewResolver} bean 实例。当其他注册方法不能暴露需要设置的更高级属性时，
	 * 这可能对配置第三方解析器实现或作为该类中的替代注册方法的替代方法很有用。
	 */
	public void viewResolver(ViewResolver viewResolver) {
		this.viewResolvers.add(viewResolver);
	}

	/**
	 * 设置与任何视图名称关联的默认视图，并根据请求的内容类型选择最佳匹配的视图。
	 * <p>使用 {@link HttpMessageWriterView HttpMessageWriterView} 将任何现有的
	 * {@code HttpMessageWriter}（例如 JSON、XML）适配并用作 {@code View}。
	 */
	public void defaultViews(View... defaultViews) {
		this.defaultViews.addAll(Arrays.asList(defaultViews));
	}

	/**
	 * 是否已注册任何视图解析器。
	 */
	public boolean hasRegistrations() {
		return (!this.viewResolvers.isEmpty());
	}

	/**
	 * 设置 {@link org.springframework.web.reactive.result.view.ViewResolutionResultHandler ViewResolutionResultHandler} 的顺序。
	 * <p>默认情况下，此属性未设置，这意味着结果处理器位于 {@link Ordered#LOWEST_PRECEDENCE} 处。
	 */
	public void order(int order) {
		this.order = order;
	}


	/**
	 * 检查给定类型的 bean 是否存在于应用程序上下文中。
	 *
	 * @param beanType 要检查的 bean 类型
	 * @return 如果应用程序上下文为 null 或给定类型的 bean 存在，则返回 true；否则返回 false
	 */
	private boolean checkBeanOfType(Class<?> beanType) {
		return (this.applicationContext == null ||
				!ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						this.applicationContext, beanType, false, false)));
	}

	protected int getOrder() {
		return (this.order != null ? this.order : Ordered.LOWEST_PRECEDENCE);
	}

	protected List<ViewResolver> getViewResolvers() {
		return this.viewResolvers;
	}

	protected List<View> getDefaultViews() {
		return this.defaultViews;
	}


	/**
	 * 内部类，用于注册带有“.ftl”后缀的 {@code FreeMarkerViewResolver}。
	 */
	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		/**
		 * 构造函数，初始化 {@code FreeMarkerRegistration} 实例。
		 */
		public FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}

	/**
	 * 内部类，用于注册脚本模板视图解析器。
	 */
	private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

		/**
		 * 构造函数，初始化 {@code ScriptRegistration} 实例。
		 */
		public ScriptRegistration() {
			super(new ScriptTemplateViewResolver());
			getViewResolver();
		}
	}

}
