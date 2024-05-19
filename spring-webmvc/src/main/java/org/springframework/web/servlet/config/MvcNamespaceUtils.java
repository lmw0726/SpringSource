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

package org.springframework.web.servlet.config;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.util.UrlPathHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用于 MVC 命名空间 BeanDefinitionParsers 的便捷方法。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Marten Deinum
 * @since 3.1
 */
public abstract class MvcNamespaceUtils {

	/**
	 * BeanNameUrlHandlerMapping 的 Bean 名称
	 */
	private static final String BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME =
			BeanNameUrlHandlerMapping.class.getName();

	/**
	 * SimpleControllerHandlerAdapter 的 Bean 名称
	 */
	private static final String SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME =
			SimpleControllerHandlerAdapter.class.getName();

	/**
	 * HttpRequestHandlerAdapter 的 Bean 名称
	 */
	private static final String HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME =
			HttpRequestHandlerAdapter.class.getName();

	/**
	 * UrlPathHelper 的 Bean 名称
	 */
	private static final String URL_PATH_HELPER_BEAN_NAME = "mvcUrlPathHelper";

	/**
	 * PathMatcher 的 Bean 名称
	 */
	private static final String PATH_MATCHER_BEAN_NAME = "mvcPathMatcher";

	/**
	 * CORS 配置的 Bean 名称
	 */
	private static final String CORS_CONFIGURATION_BEAN_NAME = "mvcCorsConfigurations";

	/**
	 * HandlerMappingIntrospector 的 Bean 名称
	 */
	private static final String HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME = "mvcHandlerMappingIntrospector";


	public static void registerDefaultComponents(ParserContext context, @Nullable Object source) {
		// 注册 BeanNameUrlHandlerMapping，确保基于 Bean 名称的 URL 映射处理器
		registerBeanNameUrlHandlerMapping(context, source);

		// 注册 HttpRequestHandlerAdapter，确保 HttpRequestHandler 的适配器
		registerHttpRequestHandlerAdapter(context, source);

		// 注册 SimpleControllerHandlerAdapter，确保 SimpleController 的适配器
		registerSimpleControllerHandlerAdapter(context, source);

		// 注册 HandlerMappingIntrospector，确保处理器映射的内省工具
		registerHandlerMappingIntrospector(context, source);

		// 注册 LocaleResolver，确保 Locale 解析器
		registerLocaleResolver(context, source);

		// 注册 ThemeResolver，确保主题解析器
		registerThemeResolver(context, source);

		// 注册 ViewNameTranslator，确保视图名称翻译器
		registerViewNameTranslator(context, source);

		// 注册 FlashMapManager，确保 FlashMap 管理器
		registerFlashMapManager(context, source);

	}

	/**
	 * 为已存在的知名名称添加别名，或在该知名名称下注册一个新的 {@link UrlPathHelper} 实例，除非已经注册。
	 *
	 * @return 指向该 {@link UrlPathHelper} 实例的 RuntimeBeanReference
	 */
	public static RuntimeBeanReference registerUrlPathHelper(
			@Nullable RuntimeBeanReference urlPathHelperRef, ParserContext context, @Nullable Object source) {

		// 如果存在 Url路径助手引用
		if (urlPathHelperRef != null) {
			// 如果 UrlPathHelper 的 Bean 名称 在注册表中是一个别名，则移除该别名
			if (context.getRegistry().isAlias(URL_PATH_HELPER_BEAN_NAME)) {
				context.getRegistry().removeAlias(URL_PATH_HELPER_BEAN_NAME);
			}
			// 注册 Url路径助手引用的Bean名称 为 UrlPathHelper的Bean名称 的别名
			context.getRegistry().registerAlias(urlPathHelperRef.getBeanName(), URL_PATH_HELPER_BEAN_NAME);
		}
		// 如果 UrlPathHelper的Bean名称 不是一个别名，且该Bean名称没有被注释为Bean定义。
		else if (!context.getRegistry().isAlias(URL_PATH_HELPER_BEAN_NAME) &&
				!context.getRegistry().containsBeanDefinition(URL_PATH_HELPER_BEAN_NAME)) {
			// 创建 UrlPathHelper 的根 Bean 定义
			RootBeanDefinition urlPathHelperDef = new RootBeanDefinition(UrlPathHelper.class);
			// 设置源
			urlPathHelperDef.setSource(source);
			// 设置角色为基础结构角色
			urlPathHelperDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 注册 UrlPathHelper的Bean名称 的 Bean 定义
			context.getRegistry().registerBeanDefinition(URL_PATH_HELPER_BEAN_NAME, urlPathHelperDef);
			// 注册 UrlPathHelper的Bean名称 的 Bean 组件定义
			context.registerComponent(new BeanComponentDefinition(urlPathHelperDef, URL_PATH_HELPER_BEAN_NAME));
		}
		// 返回 UrlPathHelper的Bean名称 的运行时 Bean 引用
		return new RuntimeBeanReference(URL_PATH_HELPER_BEAN_NAME);
	}

	/**
	 * 为已存在的知名名称添加别名，或在该知名名称下注册一个新的 {@link PathMatcher} 实例，除非已经注册。
	 *
	 * @return 指向该 {@link PathMatcher} 实例的 RuntimeBeanReference
	 */
	public static RuntimeBeanReference registerPathMatcher(@Nullable RuntimeBeanReference pathMatcherRef,
														   ParserContext context, @Nullable Object source) {

		// 如果存在 PathMatcher引用
		if (pathMatcherRef != null) {
			// 如果 mvcPathMatcher 是一个别名，则移除它
			if (context.getRegistry().isAlias(PATH_MATCHER_BEAN_NAME)) {
				context.getRegistry().removeAlias(PATH_MATCHER_BEAN_NAME);
			}
			// 注册 PathMatcher引用 的别名为 mvcPathMatcher
			context.getRegistry().registerAlias(pathMatcherRef.getBeanName(), PATH_MATCHER_BEAN_NAME);
		} else if (!context.getRegistry().isAlias(PATH_MATCHER_BEAN_NAME) &&
				!context.getRegistry().containsBeanDefinition(PATH_MATCHER_BEAN_NAME)) {
			// 如果 mvcPathMatcher 既不是别名，也不是 bean 定义
			// 创建 PathMatcher 的根 Bean 定义
			RootBeanDefinition pathMatcherDef = new RootBeanDefinition(AntPathMatcher.class);
			// 设置源
			pathMatcherDef.setSource(source);
			// 设置角色为基础结构角色
			pathMatcherDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将名称为 mvcPathMatcher 和类型为 AntPathMatcher 注册成Bean定义
			context.getRegistry().registerBeanDefinition(PATH_MATCHER_BEAN_NAME, pathMatcherDef);
			// 在上下文中注册 BeanComponentDefinition，以便路径匹配器可以在应用程序中使用
			context.registerComponent(new BeanComponentDefinition(pathMatcherDef, PATH_MATCHER_BEAN_NAME));
		}
		// 返回 mvcPathMatcher 的 RuntimeBeanReference
		return new RuntimeBeanReference(PATH_MATCHER_BEAN_NAME);
	}

	/**
	 * 在已知名称下注册一个 {@link HttpRequestHandlerAdapter}，除非已经注册。
	 */
	private static void registerBeanNameUrlHandlerMapping(ParserContext context, @Nullable Object source) {
		if (!context.getRegistry().containsBeanDefinition(BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME)) {
			// 如果 BeanNameUrlHandlerMapping的Bean名称 未被注册成 bean 定义
			// 创建 BeanNameUrlHandlerMapping 的根 Bean 定义
			RootBeanDefinition mappingDef = new RootBeanDefinition(BeanNameUrlHandlerMapping.class);
			// 设置源
			mappingDef.setSource(source);
			// 设置角色为基础结构角色
			mappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 设置排序属性值为 2，与 WebMvcConfigurationSupport 保持一致的顺序
			mappingDef.getPropertyValues().add("order", 2);
			// 注册 CORS 配置，若为空则为 null
			RuntimeBeanReference corsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
			mappingDef.getPropertyValues().add("corsConfigurations", corsRef);
			// 将 BeanNameUrlHandlerMapping 和 根 Bean 定义 注册为bean定义
			context.getRegistry().registerBeanDefinition(BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME, mappingDef);
			// 在上下文中注册 BeanComponentDefinition，以便 URL 处理映射器可以在应用程序中使用
			context.registerComponent(new BeanComponentDefinition(mappingDef, BEAN_NAME_URL_HANDLER_MAPPING_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@link HttpRequestHandlerAdapter}，除非已经注册。
	 */
	private static void registerHttpRequestHandlerAdapter(ParserContext context, @Nullable Object source) {
		if (!context.getRegistry().containsBeanDefinition(HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME)) {
			// 如果 HttpRequestHandlerAdapter 还未注册为 bean 定义
			// 创建 HttpRequestHandlerAdapter 的根 Bean 定义
			RootBeanDefinition adapterDef = new RootBeanDefinition(HttpRequestHandlerAdapter.class);
			// 设置源
			adapterDef.setSource(source);
			// 设置角色为基础结构角色
			adapterDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将名称为 HttpRequestHandlerAdapter 和 类型为 HttpRequestHandlerAdapter 注册为 bean 定义
			context.getRegistry().registerBeanDefinition(HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME, adapterDef);
			// 在上下文中注册 BeanComponentDefinition，以便 HTTP 请求处理适配器可以在应用程序中使用
			context.registerComponent(new BeanComponentDefinition(adapterDef, HTTP_REQUEST_HANDLER_ADAPTER_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@link SimpleControllerHandlerAdapter}，除非已经注册。
	 */
	private static void registerSimpleControllerHandlerAdapter(ParserContext context, @Nullable Object source) {
		if (!context.getRegistry().containsBeanDefinition(SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME)) {
			// 如果 SimpleControllerHandlerAdapter 还未注册为 bean 定义
			// 创建 SimpleControllerHandlerAdapter 的根 Bean 定义
			RootBeanDefinition beanDef = new RootBeanDefinition(SimpleControllerHandlerAdapter.class);
			// 设置源
			beanDef.setSource(source);
			// 设置角色为基础结构角色
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将名称为 SimpleControllerHandlerAdapter 和 类型为 SimpleControllerHandlerAdapter 注册为 bean 定义
			context.getRegistry().registerBeanDefinition(SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME, beanDef);
			// 注册 SimpleControllerHandlerAdapter Bean组件定义
			context.registerComponent(new BeanComponentDefinition(beanDef, SIMPLE_CONTROLLER_HANDLER_ADAPTER_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@code Map<String, CorsConfiguration>}（映射的 {@code CorsConfiguration}），除非已经注册。
	 * 如果提供了非空的 CORS 配置，则可以更新 bean 定义。
	 *
	 * @return 指向该 {@code Map<String, CorsConfiguration>} 实例的 RuntimeBeanReference
	 */
	public static RuntimeBeanReference registerCorsConfigurations(
			@Nullable Map<String, CorsConfiguration> corsConfigurations,
			ParserContext context, @Nullable Object source) {

		if (!context.getRegistry().containsBeanDefinition(CORS_CONFIGURATION_BEAN_NAME)) {
			// 如果 mvcCorsConfigurations 还未注册为 bean 定义
			// 创建 LinkedHashMap 的根 Bean 定义
			RootBeanDefinition corsDef = new RootBeanDefinition(LinkedHashMap.class);
			// 设置属性源
			corsDef.setSource(source);
			// 设置角色为基础结构角色
			corsDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			if (corsConfigurations != null) {
				// 如果有跨域配置，则将跨域配置添加到第一个构造函数属性值中
				corsDef.getConstructorArgumentValues().addIndexedArgumentValue(0, corsConfigurations);
			}
			// 将名称为 mvcCorsConfigurations 和 类型为 LinkedHashMap 注册为 bean 定义
			context.getReaderContext().getRegistry().registerBeanDefinition(CORS_CONFIGURATION_BEAN_NAME, corsDef);
			// 注册 mvcCorsConfigurations Bean组件定义
			context.registerComponent(new BeanComponentDefinition(corsDef, CORS_CONFIGURATION_BEAN_NAME));
		} else if (corsConfigurations != null) {
			// 如果 mvcCorsConfigurations 已经注册为 bean 定义，且跨域配置不为空
			// 获取跨域Bean定义
			BeanDefinition corsDef = context.getRegistry().getBeanDefinition(CORS_CONFIGURATION_BEAN_NAME);
			// 将跨域配置添加到第一个构造函数属性值中
			corsDef.getConstructorArgumentValues().addIndexedArgumentValue(0, corsConfigurations);
		}
		// 返回名称为 mvcCorsConfigurations 运行时Bean引用
		return new RuntimeBeanReference(CORS_CONFIGURATION_BEAN_NAME);
	}

	/**
	 * 在已知名称下注册一个 {@link HandlerMappingIntrospector}，除非已经注册。
	 */
	private static void registerHandlerMappingIntrospector(ParserContext context, @Nullable Object source) {
		if (!context.getRegistry().containsBeanDefinition(HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME)) {
			// 如果 mvcHandlerMappingIntrospector 还未注册为 bean 定义
			// 创建 HandlerMappingIntrospector 的根 Bean 定义
			RootBeanDefinition beanDef = new RootBeanDefinition(HandlerMappingIntrospector.class);
			// 设置源
			beanDef.setSource(source);
			// 设置角色为基础结构角色
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将该bean设置懒加载
			beanDef.setLazyInit(true);
			// 将名称为 mvcHandlerMappingIntrospector 和 类型为 HandlerMappingIntrospector 注册为 bean 定义
			context.getRegistry().registerBeanDefinition(HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME, beanDef);
			// 注册 mvcHandlerMappingIntrospector Bean组件定义
			context.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_INTROSPECTOR_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@link AcceptHeaderLocaleResolver}，除非已经注册。
	 */
	private static void registerLocaleResolver(ParserContext context, @Nullable Object source) {
		if (!containsBeanInHierarchy(context, DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)) {
			// 如果 localeResolver 还在上下层级中未注册为 bean 定义
			// 创建 AcceptHeaderLocaleResolver 的根 Bean 定义
			RootBeanDefinition beanDef = new RootBeanDefinition(AcceptHeaderLocaleResolver.class);
			// 设置源
			beanDef.setSource(source);
			// 设置角色为基础结构角色
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将名称为 localeResolver 和类型为 AcceptHeaderLocaleResolver 注册为 bean 定义
			context.getRegistry().registerBeanDefinition(DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME, beanDef);
			// 注册 localeResolver Bean组件定义
			context.registerComponent(new BeanComponentDefinition(beanDef, DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@link FixedThemeResolver}，除非已经注册。
	 */
	private static void registerThemeResolver(ParserContext context, @Nullable Object source) {
		if (!containsBeanInHierarchy(context, DispatcherServlet.THEME_RESOLVER_BEAN_NAME)) {
			// 如果 themeResolver 还在上下层级中未注册为 bean 定义
			RootBeanDefinition beanDef = new RootBeanDefinition(FixedThemeResolver.class);
			// 设置源
			beanDef.setSource(source);
			// 设置角色为基础结构角色
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将名称为 themeResolver 和类型为 FixedThemeResolver 注册为 bean 定义
			context.getRegistry().registerBeanDefinition(DispatcherServlet.THEME_RESOLVER_BEAN_NAME, beanDef);
			// 注册 themeResolver Bean组件定义
			context.registerComponent(new BeanComponentDefinition(beanDef, DispatcherServlet.THEME_RESOLVER_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@link DefaultRequestToViewNameTranslator}，除非已经注册。
	 */
	private static void registerViewNameTranslator(ParserContext context, @Nullable Object source) {
		if (!containsBeanInHierarchy(context, DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME)) {
			// 如果 viewNameTranslator 还在上下层级中未注册为 bean 定义
			// 创建 DefaultRequestToViewNameTranslator 的根 Bean 定义
			RootBeanDefinition beanDef = new RootBeanDefinition(DefaultRequestToViewNameTranslator.class);
			// 设置源
			beanDef.setSource(source);
			// 设置角色为基础结构角色
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将名称为 viewNameTranslator 和类型为 DefaultRequestToViewNameTranslator 注册为Bean定义
			context.getRegistry().registerBeanDefinition(
					DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, beanDef);
			// 注册 viewNameTranslator Bean组件定义
			context.registerComponent(
					new BeanComponentDefinition(beanDef, DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME));
		}
	}

	/**
	 * 在已知名称下注册一个 {@link SessionFlashMapManager}，除非已经注册。
	 */
	private static void registerFlashMapManager(ParserContext context, @Nullable Object source) {
		if (!containsBeanInHierarchy(context, DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME)) {
			// 如果 flashMapManager 还在上下层级中未注册为 bean 定义
			// 创建 SessionFlashMapManager 的根 Bean 定义
			RootBeanDefinition beanDef = new RootBeanDefinition(SessionFlashMapManager.class);
			// 设置源
			beanDef.setSource(source);
			// 设置角色为基础结构角色
			beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 注册 flashMapManager Bean定义
			context.getRegistry().registerBeanDefinition(DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME, beanDef);
			// 注册 flashMapManager Bean组件定义
			context.registerComponent(new BeanComponentDefinition(beanDef, DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME));
		}
	}

	/**
	 * 查找由 {@code annotation-driven} 元素创建或注册的 {@code ContentNegotiationManager} bean。
	 *
	 * @return 一个 bean 定义、bean 引用，或 {@code null} 如果没有定义
	 */
	@Nullable
	public static Object getContentNegotiationManager(ParserContext context) {
		// 获取 RequestMappingHandlerMapping 的 bean 名称
		String name = AnnotationDrivenBeanDefinitionParser.HANDLER_MAPPING_BEAN_NAME;
		// 如果 解析上下文 中包含名称为 RequestMappingHandlerMapping 的 bean 定义
		if (context.getRegistry().containsBeanDefinition(name)) {
			// 获取名为 RequestMappingHandlerMapping 的 bean 定义
			BeanDefinition handlerMappingBeanDef = context.getRegistry().getBeanDefinition(name);
			// 返回 Bean定义 中的 contentNegotiationManager 属性值
			return handlerMappingBeanDef.getPropertyValues().get("contentNegotiationManager");
		}
		// 获取 ContentNegotiationManager 的 bean 名称 —— mvcContentNegotiationManager
		name = AnnotationDrivenBeanDefinitionParser.CONTENT_NEGOTIATION_MANAGER_BEAN_NAME;
		// 如果 解析上下文 中包含名称为 mvcContentNegotiationManager 的 bean 定义
		if (context.getRegistry().containsBeanDefinition(name)) {
			// 返回名为 mvcContentNegotiationManager  的 运行时Bean引用
			return new RuntimeBeanReference(name);
		}

		// 如果未找到匹配的 bean 定义，则返回 null
		return null;
	}

	/**
	 * 检查给定名称的现有 bean，最好在整个上下文层次结构中（通过 {@code containsBean} 调用），
	 * 因为这也是 {@code DispatcherServlet} 所做的，或否则仅在本地上下文中（通过 {@code containsBeanDefinition}）。
	 */
	private static boolean containsBeanInHierarchy(ParserContext context, String beanName) {
		// 获取Bean定义注册器
		BeanDefinitionRegistry registry = context.getRegistry();
		// 检查 Bean定义注册器 是否是 BeanFactory 的实例，如果是，则检查是否包含给定名称的 bean
		return (registry instanceof BeanFactory ? ((BeanFactory) registry).containsBean(beanName) :
				// 否则，检查注册表中是否包含给定名称的 bean 定义
				registry.containsBeanDefinition(beanName));
	}

}
