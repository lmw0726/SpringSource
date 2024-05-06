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

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 用于获取客户端应使用的公共URL路径来访问静态资源的中心组件。
 *
 * <p>此类知道用于提供静态资源的Spring WebFlux处理程序映射，并使用配置的{@code ResourceHttpRequestHandler}的{@code ResourceResolver}链来做出决策。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ResourceUrlProvider.class);

	/**
	 * 路径模式 -  Web资源处理器映射
	 */
	private final Map<PathPattern, ResourceWebHandler> handlerMap = new LinkedHashMap<>();

	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回手动配置或从Spring配置自动检测到的资源处理程序映射的只读视图。
	 */
	public Map<PathPattern, ResourceWebHandler> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}


	/**
	 * 手动配置资源处理程序映射。
	 * <p><strong>注意:</strong> 默认情况下，资源映射是从Spring {@code ApplicationContext}自动检测到的。如果使用此属性，则会关闭自动检测。
	 */
	public void registerHandlers(Map<String, ResourceWebHandler> handlerMap) {
		//清空映射
		this.handlerMap.clear();
		handlerMap.forEach((rawPattern, resourceWebHandler) -> {
			// 在模式前添加斜杠，确保模式以斜杠开头
			rawPattern = prependLeadingSlash(rawPattern);
			// 使用默认的 路径模式解析器 解析原始模式，生成路径模式对象
			PathPattern pattern = PathPatternParser.defaultInstance.parse(rawPattern);
			// 将解析后的模式和处理程序映射放入处理程序映射表中
			this.handlerMap.put(pattern, resourceWebHandler);
		});
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.applicationContext == event.getApplicationContext() && this.handlerMap.isEmpty()) {
			// 如果当前应用上下文与事件中的应用上下文相同，且处理程序映射表为空，则检测资源处理程序
			detectResourceHandlers(this.applicationContext);
		}
	}

	private void detectResourceHandlers(ApplicationContext context) {
		// 获取所有类型为 SimpleUrlHandlerMapping 的 bean
		Map<String, SimpleUrlHandlerMapping> beans = context.getBeansOfType(SimpleUrlHandlerMapping.class);
		// 将这些 bean 放入列表中
		List<SimpleUrlHandlerMapping> mappings = new ArrayList<>(beans.values());
		// 对列表中的元素进行排序
		AnnotationAwareOrderComparator.sort(mappings);

		// 遍历处理程序映射列表
		mappings.forEach(mapping ->
				// 对每个处理程序映射的处理程序映射表进行遍历
				mapping.getHandlerMap().forEach((pattern, handler) -> {
					// 如果处理程序是 ResourceWebHandler 类型的
					if (handler instanceof ResourceWebHandler) {
						// 强制转换处理程序类型为 ResourceWebHandler
						ResourceWebHandler resourceHandler = (ResourceWebHandler) handler;
						// 将模式和处理程序放入处理程序映射表中
						this.handlerMap.put(pattern, resourceHandler);
					}
				}));

		// 如果处理程序映射表为空，则记录跟踪日志
		if (this.handlerMap.isEmpty()) {
			logger.trace("No resource handling mappings found");
		}
	}


	/**
	 * 获取给定URI字符串的公共资源URL。
	 * <p>URI字符串预期是一个路径，如果它包含查询或片段，这些内容将保留在生成的公共资源URL中。
	 *
	 * @param uriString 要转换的URI字符串
	 * @param exchange  当前的交换
	 * @return 解析的公共资源URL路径，如果未解析则为空
	 */
	public final Mono<String> getForUriString(String uriString, ServerWebExchange exchange) {
		// 获取请求对象
		ServerHttpRequest request = exchange.getRequest();
		// 获取查询参数的索引
		int queryIndex = getQueryIndex(uriString);
		// 提取查找路径和查询字符串
		String lookupPath = uriString.substring(0, queryIndex);
		String query = uriString.substring(queryIndex);
		// 解析查找路径为 路径容器 对象
		PathContainer parsedLookupPath = PathContainer.parsePath(lookupPath);

		// 解析资源URL并映射到请求路径
		return resolveResourceUrl(exchange, parsedLookupPath).map(resolvedPath ->
				// 构建完整的资源URL，包括上下文路径和查询字符串
				request.getPath().contextPath().value() + resolvedPath + query);
	}

	private int getQueryIndex(String path) {
		// 默认后缀索引为路径的末尾
		int suffixIndex = path.length();
		// 检查是否存在查询字符串，更新后缀索引为查询字符串的起始位置
		int queryIndex = path.indexOf('?');
		if (queryIndex > 0) {
			suffixIndex = queryIndex;
		}
		// 检查是否存在哈希标识符，更新后缀索引为哈希标识符的起始位置
		int hashIndex = path.indexOf('#');
		if (hashIndex > 0) {
			suffixIndex = Math.min(suffixIndex, hashIndex);
		}
		return suffixIndex;
	}

	private Mono<String> resolveResourceUrl(ServerWebExchange exchange, PathContainer lookupPath) {
		return this.handlerMap.entrySet().stream()
				// 过滤出与查找路径匹配的处理程序条目
				.filter(entry -> entry.getKey().matches(lookupPath))
				// 按照路径模式的特异性比较器（specificity comparator）对路径模式进行排序
				.min((entry1, entry2) ->
						PathPattern.SPECIFICITY_COMPARATOR.compare(entry1.getKey(), entry2.getKey()))
				// 映射为一个包含处理程序的路径的 Mono
				.map(entry -> {
					// 从路径模式中提取路径
					PathContainer path = entry.getKey().extractPathWithinPattern(lookupPath);
					int endIndex = lookupPath.elements().size() - path.elements().size();
					PathContainer mapping = lookupPath.subPath(0, endIndex);
					ResourceWebHandler handler = entry.getValue();
					List<ResourceResolver> resolvers = handler.getResourceResolvers();
					// 使用默认的资源解析器链构建解析器链
					ResourceResolverChain chain = new DefaultResourceResolverChain(resolvers);
					// 解析路径，并映射为处理程序的路径
					return chain.resolveUrlPath(path.value(), handler.getLocations())
							.map(resolvedPath -> mapping.value() + resolvedPath);
				})
				// 如果没有匹配的处理程序，返回一个空的 Mono
				.orElseGet(() -> {
					if (logger.isTraceEnabled()) {
						logger.trace(exchange.getLogPrefix() + "No match for \"" + lookupPath + "\"");
					}
					return Mono.empty();
				});
	}


	private static String prependLeadingSlash(String pattern) {
		if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
			// 如果路径模式存在，并且路径模式不是 斜杠/ 开头，则在开头添加 斜杠/
			return "/" + pattern;
		} else {
			//否则，返回路径模式本身
			return pattern;
		}
	}

}
