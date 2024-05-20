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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.resource.*;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} 解析 {@code resources} 元素，
 * 注册一个 {@link ResourceHttpRequestHandler} 并注册一个 {@link SimpleUrlHandlerMapping} 用于映射资源请求，
 * 以及一个 {@link HttpRequestHandlerAdapter}。
 * 还将创建一个资源处理链，其中包含 {@link ResourceResolver ResourceResolvers}
 * 和 {@link ResourceTransformer ResourceTransformers}。
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Brian Clozel
 * @since 3.0.4
 */
class ResourcesBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * 管理资源处理链的缓存键。
	 */
	private static final String RESOURCE_CHAIN_CACHE = "spring-resource-chain-cache";

	/**
	 * 版本解析器元素名称。
	 */
	private static final String VERSION_RESOLVER_ELEMENT = "version-resolver";

	/**
	 * 版本策略元素名称。
	 */
	private static final String VERSION_STRATEGY_ELEMENT = "version-strategy";

	/**
	 * 固定版本策略元素名称。
	 */
	private static final String FIXED_VERSION_STRATEGY_ELEMENT = "fixed-version-strategy";

	/**
	 * 内容版本策略元素名称。
	 */
	private static final String CONTENT_VERSION_STRATEGY_ELEMENT = "content-version-strategy";

	/**
	 * MVC 资源 URL 提供程序的 bean 名称。
	 */
	private static final String RESOURCE_URL_PROVIDER = "mvcResourceUrlProvider";

	/**
	 * 是否存在 WebJars。
	 */
	private static final boolean webJarsPresent = ClassUtils.isPresent(
			"org.webjars.WebJarAssetLocator", ResourcesBeanDefinitionParser.class.getClassLoader());


	@Override
	public BeanDefinition parse(Element element, ParserContext context) {
		// 从元素中提取源对象
		Object source = context.extractSource(element);

		// 注册 URL 提供程序
		registerUrlProvider(context, source);

		// 注册路径匹配器
		RuntimeBeanReference pathMatcherRef = MvcNamespaceUtils.registerPathMatcher(null, context, source);
		// 注册路径助手
		RuntimeBeanReference pathHelperRef = MvcNamespaceUtils.registerUrlPathHelper(null, context, source);

		// 注册资源处理器，并获取资源处理器的 bean 名称
		String resourceHandlerName = registerResourceHandler(context, element, pathHelperRef, source);
		if (resourceHandlerName == null) {
			// 如果资源处理器的 bean 名称为空，则返回 null
			return null;
		}

		// 创建 URL 映射
		Map<String, String> urlMap = new ManagedMap<>();
		// 获取资源请求路径
		String resourceRequestPath = element.getAttribute("mapping");
		if (!StringUtils.hasText(resourceRequestPath)) {
			// 如果资源请求路径为空，则报错并返回 null
			context.getReaderContext().error("The 'mapping' attribute is required.", context.extractSource(element));
			return null;
		}
		// 将资源请求路径添加到 URL 映射
		urlMap.put(resourceRequestPath, resourceHandlerName);

		// 创建简单 URL 处理器映射的 根bean 定义
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		// 设置源
		handlerMappingDef.setSource(source);
		// 将角色设置为基础设施角色
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 设置 URL 映射、路径匹配器和路径助手属性
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		handlerMappingDef.getPropertyValues().add("pathMatcher", pathMatcherRef).add("urlPathHelper", pathHelperRef);

		// 获取元素中的 "order" 属性值，并设置到 handlerMappingDef 中
		String orderValue = element.getAttribute("order");
		// 使用默认值，降低优先级
		Object order = StringUtils.hasText(orderValue) ? orderValue : Ordered.LOWEST_PRECEDENCE - 1;
		// 设置排序值
		handlerMappingDef.getPropertyValues().add("order", order);

		// 注册跨域配置
		RuntimeBeanReference corsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
		// 设置跨域配置属性
		handlerMappingDef.getPropertyValues().add("corsConfigurations", corsRef);

		// 生成 bean 名称
		String beanName = context.getReaderContext().generateBeanName(handlerMappingDef);
		// 注册 bean 定义到注册表中
		context.getRegistry().registerBeanDefinition(beanName, handlerMappingDef);
		// 将类型为 简单URL处理器映射的 根bean定义 注册为组件
		context.registerComponent(new BeanComponentDefinition(handlerMappingDef, beanName));

		// 确保 BeanNameUrlHandlerMapping 和默认的 HandlerAdapters 不会被禁用
		// 注册 HttpRequestHandlerAdapter
		MvcNamespaceUtils.registerDefaultComponents(context, source);

		return null;
	}

	private void registerUrlProvider(ParserContext context, @Nullable Object source) {
		// 如果注册表中不存在名称为 mvcResourceUrlProvider 的 bean 定义
		if (!context.getRegistry().containsBeanDefinition(RESOURCE_URL_PROVIDER)) {
			// 创建 ResourceUrlProvider 的 根bean 定义
			RootBeanDefinition urlProvider = new RootBeanDefinition(ResourceUrlProvider.class);
			// 设置源
			urlProvider.setSource(source);
			// 设置角色为基础设施角色
			urlProvider.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 注册 ResourceUrlProvider 的 bean 定义到注册表中
			context.getRegistry().registerBeanDefinition(RESOURCE_URL_PROVIDER, urlProvider);
			// 将名称为 mvcResourceUrlProvider 和类型为 ResourceUrlProvider 的Bean组合定义 注册为组件
			context.registerComponent(new BeanComponentDefinition(urlProvider, RESOURCE_URL_PROVIDER));

			// 创建 ResourceUrlProviderExposingInterceptor 的 根bean 定义
			RootBeanDefinition interceptor = new RootBeanDefinition(ResourceUrlProviderExposingInterceptor.class);
			// 设置源
			interceptor.setSource(source);
			// 设置该Bean定义的第一个构造函数参数为 ResourceUrlProvider 的 根bean 定义
			interceptor.getConstructorArgumentValues().addIndexedArgumentValue(0, urlProvider);

			// 创建 MappedInterceptor 的 根bean 定义
			RootBeanDefinition mappedInterceptor = new RootBeanDefinition(MappedInterceptor.class);
			// 设置源
			mappedInterceptor.setSource(source);
			// 设置角色为基础设施角色
			mappedInterceptor.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将构造函数的第一个参数值设置为null
			mappedInterceptor.getConstructorArgumentValues().addIndexedArgumentValue(0, (Object) null);
			// 将构造函数的第二个参数值设置为 ResourceUrlProviderExposingInterceptor 的 根bean 定义
			mappedInterceptor.getConstructorArgumentValues().addIndexedArgumentValue(1, interceptor);
			// 生成 bean 名称并注册 bean 定义到注册表中
			String mappedInterceptorName = context.getReaderContext().registerWithGeneratedName(mappedInterceptor);
			// 注册组件
			context.registerComponent(new BeanComponentDefinition(mappedInterceptor, mappedInterceptorName));
		}
	}

	@Nullable
	private String registerResourceHandler(ParserContext context, Element element,
										   RuntimeBeanReference pathHelperRef, @Nullable Object source) {

		// 获取元素中的 "location" 属性值
		String locationAttr = element.getAttribute("location");
		// 如果 "location" 属性值为空，则报错并返回 null
		if (!StringUtils.hasText(locationAttr)) {
			context.getReaderContext().error("The 'location' attribute is required.", context.extractSource(element));
			return null;
		}

		// 创建 ResourceHttpRequestHandler 的 bean 定义
		RootBeanDefinition resourceHandlerDef = new RootBeanDefinition(ResourceHttpRequestHandler.class);
		// 设置源
		resourceHandlerDef.setSource(source);
		// 设置角色为基础设施角色
		resourceHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		// 获取该Bean定义的属性值
		MutablePropertyValues values = resourceHandlerDef.getPropertyValues();
		// 设置URL路径助手
		values.add("urlPathHelper", pathHelperRef);
		// 设置 位置值 属性值
		values.add("locationValues", StringUtils.commaDelimitedListToStringArray(locationAttr));

		// 获取 "cache-period" 属性值
		String cacheSeconds = element.getAttribute("cache-period");
		if (StringUtils.hasText(cacheSeconds)) {
			// 如果 cache-period 属性值不为空，则设置到属性值中
			values.add("cacheSeconds", cacheSeconds);
		}

		// 获取 "cache-control" 子元素
		Element cacheControlElement = DomUtils.getChildElementByTagName(element, "cache-control");
		if (cacheControlElement != null) {
			// 解析成 CacheControl 对象
			CacheControl cacheControl = parseCacheControl(cacheControlElement);
			// 设置到属性值中
			values.add("cacheControl", cacheControl);
		}

		// 获取 "resource-chain" 子元素
		Element resourceChainElement = DomUtils.getChildElementByTagName(element, "resource-chain");
		if (resourceChainElement != null) {
			// 解析资源链
			parseResourceChain(resourceHandlerDef, context, resourceChainElement, source);
		}

		// 获取内容协商管理器
		Object manager = MvcNamespaceUtils.getContentNegotiationManager(context);
		if (manager != null) {
			// 如果内容协商管理器存在，将其设置到属性值中
			values.add("contentNegotiationManager", manager);
		}

		// 生成 bean 名称
		String beanName = context.getReaderContext().generateBeanName(resourceHandlerDef);
		// 注册 bean 定义到注册表中
		context.getRegistry().registerBeanDefinition(beanName, resourceHandlerDef);
		// 注册组件
		context.registerComponent(new BeanComponentDefinition(resourceHandlerDef, beanName));
		// 返回 bean 名称
		return beanName;
	}


	private CacheControl parseCacheControl(Element element) {
		CacheControl cacheControl;
		// 如果 no-cache 属性值为true
		if ("true".equals(element.getAttribute("no-cache"))) {
			// 创建 noCache 类型的 CacheControl
			cacheControl = CacheControl.noCache();
		} else if ("true".equals(element.getAttribute("no-store"))) {
			// 否则，如果元素的 "no-store" 属性值为 "true"，则创建 noStore 类型的 CacheControl
			cacheControl = CacheControl.noStore();
		} else if (element.hasAttribute("max-age")) {
			// 否则，如果元素具有 "max-age" 属性，则创建 maxAge 类型的 CacheControl，时间单位为秒
			cacheControl = CacheControl.maxAge(Long.parseLong(element.getAttribute("max-age")), TimeUnit.SECONDS);
		} else {
			// 否则，创建一个空的 CacheControl 对象
			cacheControl = CacheControl.empty();
		}

		if ("true".equals(element.getAttribute("must-revalidate"))) {
			// 如果元素的 "must-revalidate" 属性值为 "true"，重新验证 缓存控制对象
			cacheControl = cacheControl.mustRevalidate();
		}

		if ("true".equals(element.getAttribute("no-transform"))) {
			// 如果元素的 "no-transform" 属性值为 "true"，无须转换 缓存控制对象
			cacheControl = cacheControl.noTransform();
		}

		if ("true".equals(element.getAttribute("cache-public"))) {
			// 如果元素的 "cache-public" 属性值为 "true"，为 缓存控制对象 添加公共指令
			cacheControl = cacheControl.cachePublic();
		}

		if ("true".equals(element.getAttribute("cache-private"))) {
			// 如果元素的 "cache-private" 属性值为 "true"，将 缓存控制对象 设为私有的
			cacheControl = cacheControl.cachePrivate();
		}

		if ("true".equals(element.getAttribute("proxy-revalidate"))) {
			// 如果元素的 "proxy-revalidate" 属性值为 "true"，将 缓存控制对象的 代理重新验证属性设置为true
			cacheControl = cacheControl.proxyRevalidate();
		}

		if (element.hasAttribute("s-maxage")) {
			// 如果元素具有 "s-maxage" 属性，，设置 缓存控制对象 的 最大生存时间，单位为秒
			cacheControl = cacheControl.sMaxAge(Long.parseLong(element.getAttribute("s-maxage")), TimeUnit.SECONDS);
		}

		if (element.hasAttribute("stale-while-revalidate")) {
			// 如果元素具有 "stale-while-revalidate" 属性，设置 缓存控制对象 的重新验证时失效时间，单位为秒
			cacheControl = cacheControl.staleWhileRevalidate(
					Long.parseLong(element.getAttribute("stale-while-revalidate")), TimeUnit.SECONDS);
		}

		if (element.hasAttribute("stale-if-error")) {
			// 如果元素具有 "stale-if-error" 属性，设置 缓存控制对象 的错误时失效时间，单位为秒
			cacheControl = cacheControl.staleIfError(
					Long.parseLong(element.getAttribute("stale-if-error")), TimeUnit.SECONDS);
		}

		// 返回配置好的 CacheControl 对象
		return cacheControl;
	}

	private void parseResourceChain(
			RootBeanDefinition resourceHandlerDef, ParserContext context, Element element, @Nullable Object source) {

		// 获取元素的 auto-registration 属性值
		String autoRegistration = element.getAttribute("auto-registration");
		// 判断是否自动注册，如果属性值为 "false"，则不自动注册
		boolean isAutoRegistration = !(StringUtils.hasText(autoRegistration) && "false".equals(autoRegistration));

		// 创建 ManagedList 对象，用于存储资源解析器和资源转换器
		ManagedList<Object> resourceResolvers = new ManagedList<>();
		// 设置 资源解析器列表 的属性源
		resourceResolvers.setSource(source);
		ManagedList<Object> resourceTransformers = new ManagedList<>();
		// 设置 资源管理器列表 的属性源
		resourceTransformers.setSource(source);

		// 解析资源缓存
		parseResourceCache(resourceResolvers, resourceTransformers, element, source);

		// 解析资源解析器和转换器
		parseResourceResolversTransformers(
				isAutoRegistration, resourceResolvers, resourceTransformers, context, element, source);

		if (!resourceResolvers.isEmpty()) {
			// 如果资源解析器列表不为空，将其添加到资源处理器定义的属性中
			resourceHandlerDef.getPropertyValues().add("resourceResolvers", resourceResolvers);
		}

		if (!resourceTransformers.isEmpty()) {
			// 如果资源转换器列表不为空，将其添加到资源处理器定义的属性中
			resourceHandlerDef.getPropertyValues().add("resourceTransformers", resourceTransformers);
		}
	}

	private void parseResourceCache(ManagedList<Object> resourceResolvers,
									ManagedList<Object> resourceTransformers, Element element, @Nullable Object source) {

		// 获取元素的 resource-cache 属性值
		String resourceCache = element.getAttribute("resource-cache");

		// 如果 resource-cache 属性值为 "true"
		if ("true".equals(resourceCache)) {
			// 创建构造函数参数值对象
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();

			// 创建 CachingResourceResolver 的 根bean定义
			RootBeanDefinition cachingResolverDef = new RootBeanDefinition(CachingResourceResolver.class);
			// 设置属性源
			cachingResolverDef.setSource(source);
			// 设置角色为基础设施角色
			cachingResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 添加 构造函数参数值对象
			cachingResolverDef.setConstructorArgumentValues(cargs);

			// 创建 CachingResourceTransformer 的 根bean定义
			RootBeanDefinition cachingTransformerDef = new RootBeanDefinition(CachingResourceTransformer.class);
			// 设置属性源
			cachingTransformerDef.setSource(source);
			// 设置角色为基础设施角色
			cachingTransformerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 添加 构造函数参数值对象
			cachingTransformerDef.setConstructorArgumentValues(cargs);

			// 获取元素的 cache-manager 和 cache-name 属性值
			String cacheManagerName = element.getAttribute("cache-manager");
			String cacheName = element.getAttribute("cache-name");

			// 如果 cache-manager 和 cache-name 属性都不为空
			if (StringUtils.hasText(cacheManagerName) && StringUtils.hasText(cacheName)) {
				// 创建一个 cache-manager 运行时Bean引用
				RuntimeBeanReference cacheManagerRef = new RuntimeBeanReference(cacheManagerName);
				// 并将其与 cache-name 一起添加到构造函数参数中
				cargs.addIndexedArgumentValue(0, cacheManagerRef);
				cargs.addIndexedArgumentValue(1, cacheName);
			} else {
				// 如果 cache-manager 或 cache-name 不存在或为空
				// 创建一个 缓存构造函数参数值对象
				ConstructorArgumentValues cacheCavs = new ConstructorArgumentValues();
				// 第一个构造参数设置为 管理资源处理链的缓存键
				cacheCavs.addIndexedArgumentValue(0, RESOURCE_CHAIN_CACHE);

				// 创建 ConcurrentMapCache 的 根Bean定义
				RootBeanDefinition cacheDef = new RootBeanDefinition(ConcurrentMapCache.class);
				// 设置属性源
				cacheDef.setSource(source);
				// 设置角色为基础设施角色
				cacheDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// 设置构造函数为上面的缓存构造参数值
				cacheDef.setConstructorArgumentValues(cacheCavs);

				// 将 ConcurrentMapCache 的Bean定义添加到构造函数参数中
				cargs.addIndexedArgumentValue(0, cacheDef);
			}

			// 将缓存资源解析器和缓存资源转换器添加到相应的列表中
			resourceResolvers.add(cachingResolverDef);
			resourceTransformers.add(cachingTransformerDef);
		}
	}

	private void parseResourceResolversTransformers(boolean isAutoRegistration,
													ManagedList<Object> resourceResolvers, ManagedList<Object> resourceTransformers,
													ParserContext context, Element element, @Nullable Object source) {

		// 获取元素中名为 "resolvers" 的子元素
		Element resolversElement = DomUtils.getChildElementByTagName(element, "resolvers");

		// 如果存在 "resolvers" 子元素
		if (resolversElement != null) {
			// 遍历所有 "resolvers" 子元素
			for (Element beanElement : DomUtils.getChildElements(resolversElement)) {
				// 如果子元素的名称是 "version-resolver"
				if (VERSION_RESOLVER_ELEMENT.equals(beanElement.getLocalName())) {
					// 解析版本解析器定义并设置相关属性
					RootBeanDefinition versionResolverDef = parseVersionResolver(context, beanElement, source);
					versionResolverDef.setSource(source);
					// 将版本解析器添加到资源解析器列表中
					resourceResolvers.add(versionResolverDef);
					// 如果启用自动注册
					if (isAutoRegistration) {
						// 创建 CssLinkResourceTransformer 的 根Bean定义 并设置相关属性
						RootBeanDefinition cssLinkTransformerDef = new RootBeanDefinition(CssLinkResourceTransformer.class);
						cssLinkTransformerDef.setSource(source);
						cssLinkTransformerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
						// 将 CSS 链接资源转换器添加到资源转换器列表中
						resourceTransformers.add(cssLinkTransformerDef);
					}
				} else {
					// 解析其他类型的子元素并将其添加到资源解析器列表中
					Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
					resourceResolvers.add(object);
				}
			}
		}

		// 如果启用自动注册
		if (isAutoRegistration) {
			// 如果 WebJars 存在
			if (webJarsPresent) {
				// 创建 WebJarsResourceResolver 的 根Bean定义 并设置相关属性
				RootBeanDefinition webJarsResolverDef = new RootBeanDefinition(WebJarsResourceResolver.class);
				webJarsResolverDef.setSource(source);
				webJarsResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// 将 WebJars 资源解析器添加到资源解析器列表中
				resourceResolvers.add(webJarsResolverDef);
			}
			// 创建 PathResourceResolver 的 RootBeanDefinition 并设置相关属性
			RootBeanDefinition pathResolverDef = new RootBeanDefinition(PathResourceResolver.class);
			pathResolverDef.setSource(source);
			pathResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// 将路径资源解析器添加到资源解析器列表中
			resourceResolvers.add(pathResolverDef);
		}

		// 获取元素中名为 "transformers" 的子元素
		Element transformersElement = DomUtils.getChildElementByTagName(element, "transformers");

		// 如果存在 "transformers" 子元素
		if (transformersElement != null) {
			// 遍历所有 "transformers" 子元素，并解析 "bean" 和 "ref" 子元素
			for (Element beanElement : DomUtils.getChildElementsByTagName(transformersElement, "bean", "ref")) {
				// 解析子元素并将其添加到资源转换器列表中
				Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
				resourceTransformers.add(object);
			}
		}
	}

	private RootBeanDefinition parseVersionResolver(ParserContext context, Element element, @Nullable Object source) {
		// 创建一个 ManagedMap 来存储策略映射
		ManagedMap<String, Object> strategyMap = new ManagedMap<>();
		strategyMap.setSource(source);

		// 创建 VersionResourceResolver 的 根Bean定义 并设置相关属性
		RootBeanDefinition versionResolverDef = new RootBeanDefinition(VersionResourceResolver.class);
		versionResolverDef.setSource(source);
		versionResolverDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 设置策略映射
		versionResolverDef.getPropertyValues().addPropertyValue("strategyMap", strategyMap);

		// 遍历所有子元素
		for (Element beanElement : DomUtils.getChildElements(element)) {
			// 将子元素的 "patterns" 属性转换为字符串数组
			String[] patterns = StringUtils.commaDelimitedListToStringArray(beanElement.getAttribute("patterns"));
			Object strategy = null;
			// 如果子元素是固定版本策略
			if (FIXED_VERSION_STRATEGY_ELEMENT.equals(beanElement.getLocalName())) {
				// 创建构造参数值
				ConstructorArgumentValues cargs = new ConstructorArgumentValues();
				// 将子元素的 "version" 属性添加到第一个构造参数值
				cargs.addIndexedArgumentValue(0, beanElement.getAttribute("version"));
				// 创建 FixedVersionStrategy 的 根Bean定义 并设置相关属性
				RootBeanDefinition strategyDef = new RootBeanDefinition(FixedVersionStrategy.class);
				strategyDef.setSource(source);
				strategyDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				// 设置构造参数
				strategyDef.setConstructorArgumentValues(cargs);
				strategy = strategyDef;
			} else if (CONTENT_VERSION_STRATEGY_ELEMENT.equals(beanElement.getLocalName())) {
				// 创建 ContentVersionStrategy 的 根Bean定义 并设置相关属性
				RootBeanDefinition strategyDef = new RootBeanDefinition(ContentVersionStrategy.class);
				strategyDef.setSource(source);
				strategyDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				strategy = strategyDef;
			} else if (VERSION_STRATEGY_ELEMENT.equals(beanElement.getLocalName())) {
				// 获取子元素中的 "bean" 或 "ref" 子元素
				Element childElement = DomUtils.getChildElementsByTagName(beanElement, "bean", "ref").get(0);
				// 解析子元素并返回策略对象
				strategy = context.getDelegate().parsePropertySubElement(childElement, null);
			}
			// 将策略对象与模式关联并添加到策略映射中
			for (String pattern : patterns) {
				strategyMap.put(pattern, strategy);
			}
		}

		// 返回配置好的 版本资源解析器 Bean定义
		return versionResolverDef;
	}

}
