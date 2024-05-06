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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 用于获取客户端应使用的公共URL路径的中央组件。
 *
 * <p>此类知道用于提供静态资源的Spring MVC处理程序映射，并使用配置的{@code ResourceHttpRequestHandler}的{@code ResourceResolver}链来做出决策。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

	/**
	 * 路径匹配器
	 */
	private PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 路径模式 - 资源Http请求处理器映射
	 */
	private final Map<String, ResourceHttpRequestHandler> handlerMap = new LinkedHashMap<>();

	/**
	 * 是否自动检测资源映射
	 */
	private boolean autodetect = true;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 配置要在{@link #getForRequestUrl(javax.servlet.http.HttpServletRequest, String)}中使用的{@code UrlPathHelper}，
	 * 以便推导目标请求URL路径的查找路径。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回配置的{@code UrlPathHelper}。
	 *
	 * @since 4.2.8
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 配置在比较目标查找路径与资源映射时要使用的{@code PathMatcher}。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回配置的{@code PathMatcher}。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 手动配置资源映射。
	 * <p><strong>注意：</strong> 默认情况下，资源映射是从Spring {@code ApplicationContext}自动检测的。
	 * 但是，如果使用此属性，则会关闭自动检测。
	 */
	public void setHandlerMap(@Nullable Map<String, ResourceHttpRequestHandler> handlerMap) {
		if (handlerMap != null) {
			// 如果资源映射不为空，清空当前的
			this.handlerMap.clear();
			// 添加所有的资源映射
			this.handlerMap.putAll(handlerMap);
			// 禁用自动检测
			this.autodetect = false;
		}
	}

	/**
	 * 返回资源映射，无论是手动配置的还是在刷新Spring {@code ApplicationContext}时自动检测的。
	 */
	public Map<String, ResourceHttpRequestHandler> getHandlerMap() {
		return this.handlerMap;
	}

	/**
	 * 如果资源映射是手动配置的，则返回{@code false}，否则返回{@code true}。
	 */
	public boolean isAutodetect() {
		return this.autodetect;
	}


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// 如果事件的应用程序上下文与当前应用程序上下文相同，且自动检测已启用
		if (event.getApplicationContext() == this.applicationContext && isAutodetect()) {
			// 清空处理程序映射
			this.handlerMap.clear();
			// 检测资源处理程序
			detectResourceHandlers(this.applicationContext);
			// 如果处理程序映射不为空
			if (!this.handlerMap.isEmpty()) {
				// 将自动检测标志设置为 false
				this.autodetect = false;
			}
		}
	}

	protected void detectResourceHandlers(ApplicationContext appContext) {
		// 获取应用程序上下文中所有 SimpleUrlHandlerMapping 类型的 Bean
		Map<String, SimpleUrlHandlerMapping> beans = appContext.getBeansOfType(SimpleUrlHandlerMapping.class);
		// 将所有 SimpleUrlHandlerMapping 类型的 Bean 放入列表中
		List<SimpleUrlHandlerMapping> mappings = new ArrayList<>(beans.values());
		// 根据注解的顺序对列表进行排序
		AnnotationAwareOrderComparator.sort(mappings);

		// 遍历排序后的 SimpleUrlHandlerMapping 列表
		for (SimpleUrlHandlerMapping mapping : mappings) {
			// 遍历每个映射中的处理程序映射
			for (String pattern : mapping.getHandlerMap().keySet()) {
				// 获取处理程序
				Object handler = mapping.getHandlerMap().get(pattern);
				// 如果处理程序是 ResourceHttpRequestHandler 类型
				if (handler instanceof ResourceHttpRequestHandler) {
					// 将处理程序加入到处理程序映射中
					ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler;
					this.handlerMap.put(pattern, resourceHandler);
				}
			}
		}

		// 如果处理程序映射为空，则记录跟踪日志
		if (this.handlerMap.isEmpty()) {
			logger.trace("No resource handling mappings found");
		}
	}

	/**
	 * 一种变体，用于接受完整的请求URL路径（即包括上下文和Servlet路径）并返回要为公共使用公开的完整请求URL路径。
	 *
	 * @param request    当前请求
	 * @param requestUrl 要解析的请求URL路径
	 * @return 已解析的公共URL路径，如果未解析则为{@code null}
	 */
	@Nullable
	public final String getForRequestUrl(HttpServletRequest request, String requestUrl) {
		// 获取查找路径在请求URL中的起始索引
		int prefixIndex = getLookupPathIndex(request);
		// 获取请求URL的结束路径索引
		int suffixIndex = getEndPathIndex(requestUrl);

		// 如果查找路径的起始索引大于等于结束路径索引，则返回null
		if (prefixIndex >= suffixIndex) {
			return null;
		}

		// 提取前缀、后缀和查找路径
		String prefix = requestUrl.substring(0, prefixIndex);
		String suffix = requestUrl.substring(suffixIndex);
		String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);

		// 获取查找路径的解析结果
		String resolvedLookupPath = getForLookupPath(lookupPath);

		// 如果解析结果不为null，则将前缀、解析结果和后缀连接起来作为返回值，否则返回null
		return (resolvedLookupPath != null ? prefix + resolvedLookupPath + suffix : null);
	}

	private int getLookupPathIndex(HttpServletRequest request) {
		// 获取URL路径助手
		UrlPathHelper pathHelper = getUrlPathHelper();

		// 如果请求的路径属性为空，则解析并缓存查找路径
		if (request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) == null) {
			pathHelper.resolveAndCacheLookupPath(request);
		}

		// 获取请求的URI和解析后的查找路径
		String requestUri = pathHelper.getRequestUri(request);
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);

		// 返回请求URI中解析后的查找路径的起始索引
		return requestUri.indexOf(lookupPath);
	}

	private int getEndPathIndex(String lookupPath) {
		// 初始化后缀索引为查找路径的长度
		int suffixIndex = lookupPath.length();

		// 查找查询字符串的索引
		int queryIndex = lookupPath.indexOf('?');
		if (queryIndex > 0) {
			// 如果查询字符串存在，则将后缀索引设置为查询字符串的索引
			suffixIndex = queryIndex;
		}

		// 查找片段标识符的索引
		int hashIndex = lookupPath.indexOf('#');
		if (hashIndex > 0) {
			// 如果片段标识符存在，则将后缀索引设置为后缀索引和片段标识符索引中较小的那个
			suffixIndex = Math.min(suffixIndex, hashIndex);
		}

		// 返回计算后的后缀索引
		return suffixIndex;
	}

	/**
	 * 将给定路径与配置的资源处理程序映射进行比较，如果找到匹配项，则使用匹配的{@code ResourceHttpRequestHandler}的{@code ResourceResolver}链来解析要公开的URL路径。
	 * <p>预期给定路径是Spring MVC用于请求映射目的的路径，即不包括上下文和Servlet路径部分。
	 * <p>如果有多个处理程序映射匹配，则使用具有最具体模式的配置的处理程序。
	 *
	 * @param lookupPath 要检查的查找路径
	 * @return 已解析的公共URL路径，如果未解析则为{@code null}
	 */
	@Nullable
	public final String getForLookupPath(String lookupPath) {
		// 清除重复的斜杠，确保 模式内的路径 能够正确匹配查找路径
		String previous;
		do {
			// 保存前一个 查找路径 的值
			previous = lookupPath;
			// 替换重复的斜杠为单个斜杠
			lookupPath = StringUtils.replace(lookupPath, "//", "/");
			// 如果替换后的 查找路径 与前一个路径相等，则结束循环
		} while (!lookupPath.equals(previous));

		// 用于存储匹配的路径模式的列表
		List<String> matchingPatterns = new ArrayList<>();

		// 遍历处理程序映射中的每个路径模式
		for (String pattern : this.handlerMap.keySet()) {
			// 如果路径模式与 查找路径 匹配，则将其添加到匹配的路径模式列表中
			if (getPathMatcher().match(pattern, lookupPath)) {
				matchingPatterns.add(pattern);
			}
		}

		// 如果存在匹配的路径模式
		if (!matchingPatterns.isEmpty()) {
			// 获取用于比较路径模式的比较器
			Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
			// 根据比较器对匹配的路径模式进行排序
			matchingPatterns.sort(patternComparator);
			// 遍历排序后的匹配路径模式列表
			for (String pattern : matchingPatterns) {
				// 提取路径模式中的路径，以及模式匹配后的路径
				String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
				String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
				// 获取处理程序映射对应的处理程序和资源解析器链
				ResourceHttpRequestHandler handler = this.handlerMap.get(pattern);
				ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
				// 解析路径模式中的路径并返回
				String resolved = chain.resolveUrlPath(pathWithinMapping, handler.getLocations());
				// 如果解析后的路径为null，则继续下一个匹配路径模式的处理
				if (resolved == null) {
					continue;
				}
				// 返回路径映射加上解析后的路径
				return pathMapping + resolved;
			}
		}

		// 如果未找到匹配的路径模式，则记录跟踪日志并返回null
		if (logger.isTraceEnabled()) {
			logger.trace("No match for \"" + lookupPath + "\"");
		}

		// 返回null，表示未找到匹配的路径映射
		return null;
	}

}
