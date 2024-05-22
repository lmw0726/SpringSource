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

package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 协助配置一系列{@link org.springframework.web.servlet.ViewResolver ViewResolver}实例。
 * 此类预期通过{@link WebMvcConfigurer#configureViewResolvers}方法使用。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ViewResolverRegistry {
	/**
	 * 内容协商管理器
	 */
	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * 内容协商视图解析器
	 */
	@Nullable
	private ContentNegotiatingViewResolver contentNegotiatingResolver;

	/**
	 * 视图解析器列表
	 */
	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	/**
	 * 排序值
	 */
	@Nullable
	private Integer order;


	/**
	 * 带有{@link ContentNegotiationManager}和{@link ApplicationContext}的类构造函数。
	 *
	 * @since 4.3.12
	 */
	public ViewResolverRegistry(
			ContentNegotiationManager contentNegotiationManager, @Nullable ApplicationContext context) {

		this.contentNegotiationManager = contentNegotiationManager;
		this.applicationContext = context;
	}


	/**
	 * 是否已注册任何视图解析器。
	 */
	public boolean hasRegistrations() {
		return (this.contentNegotiatingResolver != null || !this.viewResolvers.isEmpty());
	}

	/**
	 * 启用{@link ContentNegotiatingViewResolver}以在配置的所有其他视图解析器之前进行选择，
	 * 根据客户端请求的媒体类型（例如在Accept头中）进行选择。
	 * 如果多次调用，提供的默认视图将被添加到任何其他可能已经配置的默认视图中。
	 *
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
	}

	/**
	 * 启用{@link ContentNegotiatingViewResolver}以在配置的所有其他视图解析器之前进行选择，
	 * 根据客户端请求的媒体类型（例如在Accept头中）进行选择。
	 * 如果多次调用，提供的默认视图将被添加到任何其他可能已经配置的默认视图中。
	 *
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(boolean useNotAcceptableStatus, View... defaultViews) {
		// 初始化内容协商视图解析器，并传入默认视图列表
		ContentNegotiatingViewResolver vr = initContentNegotiatingViewResolver(defaultViews);
		// 设置是否使用不可接受状态码
		vr.setUseNotAcceptableStatusCode(useNotAcceptableStatus);
	}

	private ContentNegotiatingViewResolver initContentNegotiatingViewResolver(View[] defaultViews) {
		// 将内容协商视图解析器的优先级提升到最高
		this.order = (this.order != null ? this.order : Ordered.HIGHEST_PRECEDENCE);

		// 如果内容协商视图解析器不为空
		if (this.contentNegotiatingResolver != null) {
			// 如果默认视图不为空，且内容协商视图解析器的默认视图列表不为空
			if (!ObjectUtils.isEmpty(defaultViews) &&
					!CollectionUtils.isEmpty(this.contentNegotiatingResolver.getDefaultViews())) {
				// 创建一个新的视图列表，并将现有的默认视图添加到其中
				List<View> views = new ArrayList<>(this.contentNegotiatingResolver.getDefaultViews());
				// 将新的默认视图添加到视图列表中
				views.addAll(Arrays.asList(defaultViews));
				// 设置内容协商视图解析器的默认视图列表
				this.contentNegotiatingResolver.setDefaultViews(views);
			}
		} else {
			// 否则，创建一个新的内容协商视图解析器
			this.contentNegotiatingResolver = new ContentNegotiatingViewResolver();
			// 设置 内容协商视图解析器 的默认视图列表
			this.contentNegotiatingResolver.setDefaultViews(Arrays.asList(defaultViews));
			// 设置 内容协商视图解析器 的视图解析器列表
			this.contentNegotiatingResolver.setViewResolvers(this.viewResolvers);
			// 如果 内容协商管理器 不为空，则设置内容协商管理器
			if (this.contentNegotiationManager != null) {
				this.contentNegotiatingResolver.setContentNegotiationManager(this.contentNegotiationManager);
			}
		}
		// 返回内容协商视图解析器
		return this.contentNegotiatingResolver;
	}

	/**
	 * 使用默认视图名称前缀"/WEB-INF/"和默认后缀".jsp"注册JSP视图解析器。
	 * <p>当此方法被多次调用时，每次调用将注册一个新的ViewResolver实例。
	 * 注意，由于很难确定是否存在JSP，因此在使用基于JSP的多个视图解析器时，仅在解析器上的"viewNames"属性指示哪些视图名称由哪个解析器处理才有意义。
	 */
	public UrlBasedViewResolverRegistration jsp() {
		return jsp("/WEB-INF/", ".jsp");
	}

	/**
	 * 使用指定的前缀和后缀注册JSP视图解析器。
	 * <p>当此方法被多次调用时，每次调用将注册一个新的ViewResolver实例。
	 * 注意，由于很难确定是否存在JSP，因此在使用基于JSP的多个视图解析器时，仅在解析器上的"viewNames"属性指示哪些视图名称由哪个解析器处理才有意义。
	 */
	public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
		// 创建一个内部资源视图解析器
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		// 设置视图解析器的前缀
		resolver.setPrefix(prefix);
		// 设置视图解析器的后缀
		resolver.setSuffix(suffix);
		// 将视图解析器添加到视图解析器列表中
		this.viewResolvers.add(resolver);
		// 返回基于 URL 的视图解析器注册对象
		return new UrlBasedViewResolverRegistration(resolver);
	}

	/**
	 * 注册Tiles 3.x视图解析器。
	 * <p><strong>注意</strong>，您还必须通过添加一个{@link org.springframework.web.servlet.view.tiles3.TilesConfigurer} bean来配置Tiles。
	 */
	public UrlBasedViewResolverRegistration tiles() {
		// 如果没有检查到类型为 Tiles配置 的 bean
		if (!checkBeanOfType(TilesConfigurer.class)) {
			// 抛出 Bean 初始化异常，提示需要一个 Tiles配置 bean
			throw new BeanInitializationException("In addition to a Tiles view resolver " +
					"there must also be a single TilesConfigurer bean in this web application context " +
					"(or its parent).");
		}
		// 创建一个 Tiles 注册对象
		TilesRegistration registration = new TilesRegistration();
		// 将 Tiles 视图解析器添加到视图解析器列表中
		this.viewResolvers.add(registration.getViewResolver());
		// 返回 Tiles 注册对象
		return registration;
	}

	/**
	 * 使用空的默认视图名称前缀和默认后缀".ftl"注册FreeMarker视图解析器。
	 * <p><strong>注意</strong>，您还必须通过添加一个{@link org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer} bean来配置FreeMarker。
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		// 如果没有检查到类型为 FreeMarker配置 的 bean
		if (!checkBeanOfType(FreeMarkerConfigurer.class)) {
			// 抛出 Bean 初始化异常，提示需要一个 FreeMarker配置 bean，通常实现为 FreeMarker配置
			throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
					"there must also be a single FreeMarkerConfig bean in this web application context " +
					"(or its parent): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		// 创建一个 FreeMarker 注册对象
		FreeMarkerRegistration registration = new FreeMarkerRegistration();
		// 将 FreeMarker 视图解析器添加到视图解析器列表中
		this.viewResolvers.add(registration.getViewResolver());
		// 返回 FreeMarker 注册对象
		return registration;
	}

	/**
	 * 使用空的默认视图名称前缀和默认后缀".tpl"注册Groovy标记视图解析器。
	 */
	public UrlBasedViewResolverRegistration groovy() {
		// 如果没有检查到类型为 GroovyMarkup配置 的 bean
		if (!checkBeanOfType(GroovyMarkupConfigurer.class)) {
			// 抛出 Bean 初始化异常，提示需要一个 GroovyMarkup配置 bean，通常实现为 GroovyMarkup配置
			throw new BeanInitializationException("In addition to a Groovy markup view resolver " +
					"there must also be a single GroovyMarkupConfig bean in this web application context " +
					"(or its parent): GroovyMarkupConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		// 创建一个 GroovyMarkup 注册对象
		GroovyMarkupRegistration registration = new GroovyMarkupRegistration();
		// 将 GroovyMarkup 视图解析器添加到视图解析器列表中
		this.viewResolvers.add(registration.getViewResolver());
		// 返回 GroovyMarkup 注册对象
		return registration;
	}

	/**
	 * 注册一个脚本模板视图解析器，使用空的默认视图名称前缀和后缀。
	 *
	 * @since 4.2
	 */
	public UrlBasedViewResolverRegistration scriptTemplate() {
		// 如果没有检查到类型为 Script模板配置器 的 bean
		if (!checkBeanOfType(ScriptTemplateConfigurer.class)) {
			// 抛出 Bean 初始化异常，提示需要一个 Script模板配置 bean，通常实现为 ScriptTemplateConfigurer
			throw new BeanInitializationException("In addition to a script template view resolver " +
					"there must also be a single ScriptTemplateConfig bean in this web application context " +
					"(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		// 创建一个 Script 注册对象
		ScriptRegistration registration = new ScriptRegistration();
		// 将 Script 视图解析器添加到视图解析器列表中
		this.viewResolvers.add(registration.getViewResolver());
		// 返回 Script 注册对象
		return registration;
	}

	/**
	 * 注册一个bean名称视图解析器，将视图名称解释为org.springframework.web.servlet.View bean的名称。
	 */
	public void beanName() {
		// 创建Bean名称视图解析器
		BeanNameViewResolver resolver = new BeanNameViewResolver();
		// 将该解析器添加到视图解析器列表中
		this.viewResolvers.add(resolver);
	}

	/**
	 * 注册一个ViewResolver bean实例。这可能对配置自定义（或第三方）解析器实现很有用。
	 * 当此类中的其他注册方法不暴露需要设置的一些更高级属性时，它也可以用作替代方法。
	 */
	public void viewResolver(ViewResolver viewResolver) {
		// 如果视图解析器是 ContentNegotiatingViewResolver 类型
		if (viewResolver instanceof ContentNegotiatingViewResolver) {
			// 抛出 Bean 初始化异常，提示不能使用 addViewResolver 配置 ContentNegotiatingViewResolver，应该使用 enableContentNegotiation 方法
			throw new BeanInitializationException(
					"addViewResolver cannot be used to configure a ContentNegotiatingViewResolver. " +
							"Please use the method enableContentNegotiation instead.");
		}
		// 将视图解析器添加到视图解析器列表中
		this.viewResolvers.add(viewResolver);
	}

	/**
	 * 通过此注册表注册的ViewResolver被封装在org.springframework.web.servlet.view.ViewResolverComposite
	 * ViewResolverComposite实例中，并遵循注册顺序。
	 * 此属性确定ViewResolverComposite本身相对于Spring配置中存在的任何其他ViewResolver的顺序（未在此注册）。
	 * <p>默认情况下，此属性未设置，这意味着解析器的顺序为Ordered.LOWEST_PRECEDENCE，
	 * 除非启用了内容协商，在这种情况下，如果未显式设置顺序，则顺序更改为Ordered.HIGHEST_PRECEDENCE。
	 */
	public void order(int order) {
		this.order = order;
	}


	private boolean checkBeanOfType(Class<?> beanType) {
		// 如果应用程序上下文为空，或者指定类型的 bean 在应用程序上下文（包括父上下文）中存在，则返回 true；
		// 否则返回 false
		return (this.applicationContext == null ||
				!ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						this.applicationContext, beanType, false, false)));
	}

	protected int getOrder() {
		return (this.order != null ? this.order : Ordered.LOWEST_PRECEDENCE);
	}

	protected List<ViewResolver> getViewResolvers() {
		// 如果内容协商视图解析器不为空，则返回包含内容协商视图解析器的单元素列表；
		if (this.contentNegotiatingResolver != null) {
			return Collections.singletonList(this.contentNegotiatingResolver);
		} else {
			// 否则返回视图解析器列表
			return this.viewResolvers;
		}
	}


	private static class TilesRegistration extends UrlBasedViewResolverRegistration {

		public TilesRegistration() {
			super(new TilesViewResolver());
		}
	}

	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		public FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}


	private static class GroovyMarkupRegistration extends UrlBasedViewResolverRegistration {

		public GroovyMarkupRegistration() {
			super(new GroovyMarkupViewResolver());
			getViewResolver().setSuffix(".tpl");
		}
	}


	private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

		public ScriptRegistration() {
			super(new ScriptTemplateViewResolver());
			getViewResolver();
		}
	}

}
