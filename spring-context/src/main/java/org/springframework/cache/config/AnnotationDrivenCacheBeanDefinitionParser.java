/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cache.config;

import org.w3c.dom.Element;

import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * 的实现，允许用户轻松配置启用基于注解驱动的缓存边界划分所需的全部
 * 基础设施 bean。
 *
 * <p>默认情况下，所有代理都创建为 JDK 代理。如果你注入的是具体类而非
 * 接口，这可能会引发一些问题。为了克服这一限制，你可以将
 * '{@code proxy-target-class}' 属性设置为 '{@code true}'，这样将会
 * 创建基于类的代理。
 *
 * <p>如果 JSR-107 API 和 Spring 的 JCache 实现均存在，
 * 那么用于处理带有 {@code CacheResult}、{@code CachePut}、{@code CacheRemove}
 * 或 {@code CacheRemoveAll} 注解的方法所需的基础设施 bean 也会被注册。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 3.1
 */
class AnnotationDrivenCacheBeanDefinitionParser implements BeanDefinitionParser {

	private static final String CACHE_ASPECT_CLASS_NAME =
			"org.springframework.cache.aspectj.AnnotationCacheAspect";

	private static final String JCACHE_ASPECT_CLASS_NAME =
			"org.springframework.cache.aspectj.JCacheCacheAspect";

	private static final boolean jsr107Present;

	private static final boolean jcacheImplPresent;

	static {
		ClassLoader classLoader = AnnotationDrivenCacheBeanDefinitionParser.class.getClassLoader();
		jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
		jcacheImplPresent = ClassUtils.isPresent(
				"org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource", classLoader);
	}


	/**
	 * 解析 '{@code <cache:annotation-driven>}' 标签。如有必要，
	 * 将 {@link AopNamespaceUtils#registerAutoProxyCreatorIfNecessary
	 * 注册一个 AutoProxyCreator} 到容器中。
	 */
	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		String mode = element.getAttribute("mode");
		if ("aspectj".equals(mode)) {
			// mode="aspectj"（AspectJ 切面模式）
			registerCacheAspect(element, parserContext);
		}
		else {
			// mode="proxy"（代理模式）
			registerCacheAdvisor(element, parserContext);
		}

		return null;
	}

	private void registerCacheAspect(Element element, ParserContext parserContext) {
		SpringCachingConfigurer.registerCacheAspect(element, parserContext);
		if (jsr107Present && jcacheImplPresent) {
			JCacheCachingConfigurer.registerCacheAspect(element, parserContext);
		}
	}

	private void registerCacheAdvisor(Element element, ParserContext parserContext) {
		AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
		SpringCachingConfigurer.registerCacheAdvisor(element, parserContext);
		if (jsr107Present && jcacheImplPresent) {
			JCacheCachingConfigurer.registerCacheAdvisor(element, parserContext);
		}
	}

	/**
	 * 解析要使用的缓存解析策略。如果设置了 'cache-resolver' 属性，
	 * 则注入该属性。否则设置 'cache-manager'。如果 {@code setBoth}
	 * 为 {@code true}，则两个服务都会被注入。
	 */
	private static void parseCacheResolution(Element element, BeanDefinition def, boolean setBoth) {
		String name = element.getAttribute("cache-resolver");
		boolean hasText = StringUtils.hasText(name);
		if (hasText) {
			def.getPropertyValues().add("cacheResolver", new RuntimeBeanReference(name.trim()));
		}
		if (!hasText || setBoth) {
			def.getPropertyValues().add("cacheManager",
					new RuntimeBeanReference(CacheNamespaceHandler.extractCacheManager(element)));
		}
	}

	private static void parseErrorHandler(Element element, BeanDefinition def) {
		String name = element.getAttribute("error-handler");
		if (StringUtils.hasText(name)) {
			def.getPropertyValues().add("errorHandler", new RuntimeBeanReference(name.trim()));
		}
	}


	/**
	 * 配置支持 Spring 缓存注解所需的基础设施。
	 */
	private static class SpringCachingConfigurer {

		private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)) {
				Object eleSource = parserContext.extractSource(element);

				// 创建 CacheOperationSource 的 Bean 定义。
				RootBeanDefinition sourceDef = new RootBeanDefinition("org.springframework.cache.annotation.AnnotationCacheOperationSource");
				sourceDef.setSource(eleSource);
				sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

				// 创建 CacheInterceptor 的 Bean 定义。
				RootBeanDefinition interceptorDef = new RootBeanDefinition(CacheInterceptor.class);
				interceptorDef.setSource(eleSource);
				interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				parseCacheResolution(element, interceptorDef, false);
				parseErrorHandler(element, interceptorDef);
				CacheNamespaceHandler.parseKeyGenerator(element, interceptorDef);
				interceptorDef.getPropertyValues().add("cacheOperationSources", new RuntimeBeanReference(sourceName));
				String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

				// 创建 CacheAdvisor 的 Bean 定义。
				RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryCacheOperationSourceAdvisor.class);
				advisorDef.setSource(eleSource);
				advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
				advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
				if (element.hasAttribute("order")) {
					advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
				}
				parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME, advisorDef);

				CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
				compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME));
				parserContext.registerComponent(compositeDef);
			}
		}

		/**
		 * 注册一个缓存切面（cache aspect）。
		 * <pre class="code">
		 * &lt;bean id="cacheAspect" class="org.springframework.cache.aspectj.AnnotationCacheAspect" factory-method="aspectOf"&gt;
		 *   &lt;property name="cacheManager" ref="cacheManager"/&gt;
		 *   &lt;property name="keyGenerator" ref="keyGenerator"/&gt;
		 * &lt;/bean&gt;
		 * </pre>
		 */
		private static void registerCacheAspect(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME)) {
				RootBeanDefinition def = new RootBeanDefinition();
				def.setBeanClassName(CACHE_ASPECT_CLASS_NAME);
				def.setFactoryMethodName("aspectOf");
				parseCacheResolution(element, def, false);
				CacheNamespaceHandler.parseKeyGenerator(element, def);
				parserContext.registerBeanComponent(new BeanComponentDefinition(def, CacheManagementConfigUtils.CACHE_ASPECT_BEAN_NAME));
			}
		}
	}


	/**
	 * 配置支持标准 JSR-107 缓存注解所需的基础设施。
	 */
	private static class JCacheCachingConfigurer {

		private static void registerCacheAdvisor(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)) {
				Object source = parserContext.extractSource(element);

				// 创建 CacheOperationSource 的 Bean 定义。
				BeanDefinition sourceDef = createJCacheOperationSourceBeanDefinition(element, source);
				String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

				// 创建 CacheInterceptor 的 Bean 定义。
				RootBeanDefinition interceptorDef =
						new RootBeanDefinition("org.springframework.cache.jcache.interceptor.JCacheInterceptor");
				interceptorDef.setSource(source);
				interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				interceptorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
				parseErrorHandler(element, interceptorDef);
				String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

				// 创建 CacheAdvisor 的 Bean 定义。
				RootBeanDefinition advisorDef = new RootBeanDefinition(
						"org.springframework.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor");
				advisorDef.setSource(source);
				advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				advisorDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(sourceName));
				advisorDef.getPropertyValues().add("adviceBeanName", interceptorName);
				if (element.hasAttribute("order")) {
					advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
				}
				parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME, advisorDef);

				CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);
				compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME));
				parserContext.registerComponent(compositeDef);
			}
		}

		private static void registerCacheAspect(Element element, ParserContext parserContext) {
			if (!parserContext.getRegistry().containsBeanDefinition(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME)) {
				Object source = parserContext.extractSource(element);

				BeanDefinition cacheOperationSourceDef = createJCacheOperationSourceBeanDefinition(element, source);
				String cacheOperationSourceName = parserContext.getReaderContext().registerWithGeneratedName(cacheOperationSourceDef);

				RootBeanDefinition jcacheAspectDef = new RootBeanDefinition();
				jcacheAspectDef.setBeanClassName(JCACHE_ASPECT_CLASS_NAME);
				jcacheAspectDef.setFactoryMethodName("aspectOf");
				jcacheAspectDef.getPropertyValues().add("cacheOperationSource", new RuntimeBeanReference(cacheOperationSourceName));
				parserContext.getRegistry().registerBeanDefinition(CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME, jcacheAspectDef);

				CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), source);
				compositeDef.addNestedComponent(new BeanComponentDefinition(cacheOperationSourceDef, cacheOperationSourceName));
				compositeDef.addNestedComponent(new BeanComponentDefinition(jcacheAspectDef, CacheManagementConfigUtils.JCACHE_ASPECT_BEAN_NAME));
				parserContext.registerComponent(compositeDef);
			}
		}

		private static RootBeanDefinition createJCacheOperationSourceBeanDefinition(Element element, @Nullable Object eleSource) {
			RootBeanDefinition sourceDef =
					new RootBeanDefinition("org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource");
			sourceDef.setSource(eleSource);
			sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			// JSR-107 支持应当使用缓存管理器创建一个异常缓存解析器（exception cache resolver），
			// 并且无法通过命名空间来设置该异常缓存解析器
			parseCacheResolution(element, sourceDef, true);
			CacheNamespaceHandler.parseKeyGenerator(element, sourceDef);
			return sourceDef;
		}
	}

}
