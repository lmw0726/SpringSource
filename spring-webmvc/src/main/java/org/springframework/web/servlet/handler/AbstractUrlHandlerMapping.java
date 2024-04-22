/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * URL 映射的抽象基类，用于实现 {@link HandlerMapping}。
 *
 * <p>支持直接匹配和模式匹配，例如 "/test/*"、"/test/**" 等。
 * 有关模式语法的详细信息，请参阅 {@link PathPattern}（当解析模式时为 {@link #usesPathPatterns() 启用}），
 * 否则请参阅 {@link AntPathMatcher}。语法基本相同，但 {@code PathPattern} 语法更适合 Web 应用程序，
 * 其实现效率更高。
 *
 * <p>将检查所有路径模式，以找到当前请求路径的最精确匹配，其中“最精确”是与当前请求路径匹配的最长路径模式。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 16.04.2003
 */
public abstract class AbstractUrlHandlerMapping extends AbstractHandlerMapping implements MatchableHandlerMapping {

	/**
	 * 此处理程序映射的根处理程序，即注册为根路径 ("/") 的处理程序。
	 */
	@Nullable
	private Object rootHandler;

	/**
	 * 是否匹配 URL 时不考虑结尾斜杠的存在。
	 */
	private boolean useTrailingSlashMatch = false;

	/**
	 * 设置是否延迟初始化处理程序。
	 */
	private boolean lazyInitHandlers = false;

	/**
	 * URL映射与处理器映射
	 */
	private final Map<String, Object> handlerMap = new LinkedHashMap<>();

	/**
	 * 路径模式与处理器映射
	 */
	private final Map<PathPattern, Object> pathPatternHandlerMap = new LinkedHashMap<>();


	@Override
	public void setPatternParser(PathPatternParser patternParser) {
		Assert.state(this.handlerMap.isEmpty(),
				"PathPatternParser must be set before the initialization of " +
						"the handler map via ApplicationContextAware#setApplicationContext.");
		super.setPatternParser(patternParser);
	}

	/**
	 * 设置此处理程序映射的根处理程序，即注册为根路径 ("/") 的处理程序。
	 * <p>默认值为 {@code null}，表示没有根处理程序。
	 */
	public void setRootHandler(@Nullable Object rootHandler) {
		this.rootHandler = rootHandler;
	}

	/**
	 * 返回此处理程序映射的根处理程序（注册为 "/"），如果没有则返回 {@code null}。
	 */
	@Nullable
	public Object getRootHandler() {
		return this.rootHandler;
	}

	/**
	 * 是否匹配 URL 时不考虑结尾斜杠的存在。
	 * 如果启用，URL 模式如 "/users" 也将匹配到 "/users/"。
	 * <p>默认值为 {@code false}。
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (getPatternParser() != null) {
			// 如果路径解析器存在，则设置此解析器生成的 路径模式 是否应自动匹配带有尾随斜杠的请求路径。
			getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
		}
	}

	/**
	 * 是否匹配 URL 时不考虑结尾斜杠的存在。
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * 设置是否延迟初始化处理程序。仅适用于单例处理程序，原型始终会延迟初始化。
	 * 默认值为 "false"，因为通过直接引用控制器对象进行更高效的急切初始化。
	 * <p>如果要允许控制器进行延迟初始化，请将其设置为“lazy-init”，并将此标志设置为 true。
	 * 仅将其设置为“lazy-init”将不起作用，因为在这种情况下，通过处理程序映射的引用来初始化它们。
	 */
	public void setLazyInitHandlers(boolean lazyInitHandlers) {
		this.lazyInitHandlers = lazyInitHandlers;
	}

	/**
	 * 查找给定请求的 URL 路径的处理程序。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 处理程序实例，如果没有找到则为 {@code null}
	 */
	@Override
	@Nullable
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		// 初始化查找路径
		String lookupPath = initLookupPath(request);
		Object handler;
		// 如果使用路径模式
		if (usesPathPatterns()) {
			// 解析请求路径
			RequestPath path = ServletRequestPathUtils.getParsedRequestPath(request);
			// 查找处理器
			handler = lookupHandler(path, lookupPath, request);
		} else {
			// 查找处理器
			handler = lookupHandler(lookupPath, request);
		}
		// 如果处理器为空
		if (handler == null) {
			// 需要直接处理默认处理器，因为我们需要为其公开 PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE。
			Object rawHandler = null;
			// 如果查找路径匹配根路径
			if (StringUtils.matchesCharacter(lookupPath, '/')) {
				// 获取根处理器，并设置为原始处理器
				rawHandler = getRootHandler();
			}
			if (rawHandler == null) {
				// 如果根处理器为空，则获取默认处理器，并设置为原始处理器
				rawHandler = getDefaultHandler();
			}
			// 如果存在原始处理器
			if (rawHandler != null) {
				// 处理处理器对象，如果处理器对象是字符串类型，则解析为真正的处理器对象
				if (rawHandler instanceof String) {
					String handlerName = (String) rawHandler;
					// 获取处理名称对应的bean类型
					rawHandler = obtainApplicationContext().getBean(handlerName);
				}
				// 验证处理器的合法性
				validateHandler(rawHandler, request);
				// 构建暴露路径的处理器
				handler = buildPathExposingHandler(rawHandler, lookupPath, lookupPath, null);
			}
		}
		// 返回处理器对象
		return handler;
	}

	/**
	 * 查找给定 URL 路径的处理程序实例。当解析的 {@code PathPattern} 被 {@link #usesPathPatterns() 启用} 时使用此方法。
	 *
	 * @param path       解析的 RequestPath
	 * @param lookupPath 用于检查直接命中的字符串 lookupPath
	 * @param request    当前 HTTP 请求
	 * @return 匹配的处理程序，如果未找到则为 {@code null}
	 * @since 5.3
	 */
	@Nullable
	protected Object lookupHandler(
			RequestPath path, String lookupPath, HttpServletRequest request) throws Exception {

		// 获取直接匹配的处理器
		Object handler = getDirectMatch(lookupPath, request);
		// 如果直接匹配到处理器，则返回该处理器
		if (handler != null) {
			return handler;
		}

		// 是否有模式匹配？
		List<PathPattern> matches = null;
		// 遍历路径模式和处理器的映射关系
		for (PathPattern pattern : this.pathPatternHandlerMap.keySet()) {
			// 如果路径模式匹配请求路径
			if (pattern.matches(path.pathWithinApplication())) {
				// 添加到匹配列表中
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(pattern);
			}
		}
		// 如果没有匹配到任何路径模式，则返回 null
		if (matches == null) {
			return null;
		}
		// 如果有多个匹配的路径模式，则按照特殊性排序
		if (matches.size() > 1) {
			matches.sort(PathPattern.SPECIFICITY_COMPARATOR);
			if (logger.isTraceEnabled()) {
				logger.trace("Matching patterns " + matches);
			}
		}
		// 获取第一个匹配的路径模式
		PathPattern pattern = matches.get(0);
		// 获取该路径模式对应的处理器
		handler = this.pathPatternHandlerMap.get(pattern);
		// 如果处理器是字符串类型，则解析为真正的处理器对象
		if (handler instanceof String) {
			String handlerName = (String) handler;
			//获取bean名称对应的处理器对象
			handler = obtainApplicationContext().getBean(handlerName);
		}
		// 验证处理器的合法性
		validateHandler(handler, request);
		// 提取请求路径在映射路径中的部分
		String pathWithinMapping = pattern.extractPathWithinPattern(path.pathWithinApplication()).value();
		// 移除路径中的分号内容
		pathWithinMapping = UrlPathHelper.defaultInstance.removeSemicolonContent(pathWithinMapping);
		// 构建暴露路径的处理器
		return buildPathExposingHandler(handler, pattern.getPatternString(), pathWithinMapping, null);
	}

	/**
	 * 查找给定 URL 路径的处理程序实例。当使用 {@code PathMatcher} 进行字符串模式匹配时使用此方法。
	 *
	 * @param lookupPath 要与模式进行匹配的路径
	 * @param request    当前 HTTP 请求
	 * @return 匹配的处理程序，如果未找到则为 {@code null}
	 * @see #exposePathWithinMapping
	 * @see AntPathMatcher
	 */
	@Nullable
	protected Object lookupHandler(String lookupPath, HttpServletRequest request) throws Exception {
		// 获取直接匹配的处理器
		Object handler = getDirectMatch(lookupPath, request);
		// 如果直接匹配到处理器，则返回该处理器
		if (handler != null) {
			return handler;
		}

		// 模式匹配？
		List<String> matchingPatterns = new ArrayList<>();
		// 遍历处理器映射中的注册路径
		for (String registeredPattern : this.handlerMap.keySet()) {
			// 如果请求路径与注册路径匹配
			if (getPathMatcher().match(registeredPattern, lookupPath)) {
				// 添加到匹配模式列表中
				matchingPatterns.add(registeredPattern);
			} else if (useTrailingSlashMatch()) {
				// 如果启用了尾部斜杠匹配，并且注册路径不是以斜杠结尾，但请求路径加上斜杠后与注册路径匹配
				if (!registeredPattern.endsWith("/") && getPathMatcher().match(registeredPattern + "/", lookupPath)) {
					// 将注册路径加斜杠添加到匹配列表中
					matchingPatterns.add(registeredPattern + "/");
				}
			}
		}

		String bestMatch = null;
		// 获取最佳匹配的路径
		Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
		if (!matchingPatterns.isEmpty()) {
			// 如果匹配模式列表不为空，按照模式匹配器排序
			matchingPatterns.sort(patternComparator);
			if (logger.isTraceEnabled() && matchingPatterns.size() > 1) {
				logger.trace("Matching patterns " + matchingPatterns);
			}
			// 获取第一个匹配到的路径
			bestMatch = matchingPatterns.get(0);
		}
		if (bestMatch != null) {
			// 获取处理器对象
			handler = this.handlerMap.get(bestMatch);
			if (handler == null) {
				// 如果最佳匹配的路径以斜杠结尾，则尝试去掉斜杠再获取处理器对象
				if (bestMatch.endsWith("/")) {
					//去掉斜杠后获取处理器
					handler = this.handlerMap.get(bestMatch.substring(0, bestMatch.length() - 1));
				}
				if (handler == null) {
					// 处理器为空，则抛出异常
					throw new IllegalStateException(
							"Could not find handler for best pattern match [" + bestMatch + "]");
				}
			}
			// 处理处理器对象，如果处理器对象是字符串类型，则解析为真正的处理器对象
			if (handler instanceof String) {
				String handlerName = (String) handler;
				// 获取bean名称对应的处理器对象
				handler = obtainApplicationContext().getBean(handlerName);
			}
			// 验证处理器的合法性
			validateHandler(handler, request);
			// 获取请求路径在匹配路径中的部分
			String pathWithinMapping = getPathMatcher().extractPathWithinPattern(bestMatch, lookupPath);

			// 可能存在多个最佳模式，让我们确保对所有的模式都有正确的 URI 模板变量
			Map<String, String> uriTemplateVariables = new LinkedHashMap<>();
			for (String matchingPattern : matchingPatterns) {
				if (patternComparator.compare(bestMatch, matchingPattern) == 0) {
					// 抽取URL模板中的路径变量参数
					Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchingPattern, lookupPath);
					// 解析路径变量
					Map<String, String> decodedVars = getUrlPathHelper().decodePathVariables(request, vars);
					// 将解析好的值添加到路径模板变量映射中
					uriTemplateVariables.putAll(decodedVars);
				}
			}
			if (logger.isTraceEnabled() && uriTemplateVariables.size() > 0) {
				logger.trace("URI variables " + uriTemplateVariables);
			}
			// 构建暴露路径的处理器
			return buildPathExposingHandler(handler, bestMatch, pathWithinMapping, uriTemplateVariables);
		}

		// 找不到处理器...
		return null;
	}

	@Nullable
	private Object getDirectMatch(String urlPath, HttpServletRequest request) throws Exception {
		// 从处理器映射中获取处理器对象
		Object handler = this.handlerMap.get(urlPath);
		// 如果找到处理器对象
		if (handler != null) {
			// 处理处理器对象，如果处理器对象是字符串类型，则解析为真正的处理器对象
			if (handler instanceof String) {
				String handlerName = (String) handler;
				// 获取bean名称对应的处理器对象
				handler = obtainApplicationContext().getBean(handlerName);
			}
			// 验证处理器的合法性
			validateHandler(handler, request);
			// 构建暴露路径的处理器
			return buildPathExposingHandler(handler, urlPath, urlPath, null);
		}
		// 找不到处理器对象
		return null;
	}

	/**
	 * 验证给定的处理程序是否符合当前请求。
	 * 默认实现为空。可以在子类中重写，例如强制执行 URL 映射中表示的特定前提条件。
	 *
	 * @param handler 处理程序对象
	 * @param request 当前 HTTP 请求
	 * @throws Exception 如果验证失败
	 */
	protected void validateHandler(Object handler, HttpServletRequest request) throws Exception {
	}

	/**
	 * 为给定的原始处理程序构建处理程序对象，在执行处理程序之前公开实际处理程序、
	 * {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}，以及 {@link #URI_TEMPLATE_VARIABLES_ATTRIBUTE}。
	 * 默认实现使用一个特殊的拦截器构建一个 {@link HandlerExecutionChain}，
	 * 该拦截器公开路径属性和 URI 模板变量。
	 *
	 * @param rawHandler           要公开的原始处理程序
	 * @param bestMatchingPattern  最佳匹配模式
	 * @param pathWithinMapping    映射内的路径
	 * @param uriTemplateVariables URI 模板变量，如果未找到变量则可以为 {@code null}
	 * @return 最终的处理程序对象
	 */
	protected Object buildPathExposingHandler(Object rawHandler, String bestMatchingPattern,
											  String pathWithinMapping, @Nullable Map<String, String> uriTemplateVariables) {

		// 创建处理器执行链
		HandlerExecutionChain chain = new HandlerExecutionChain(rawHandler);
		// 添加路径暴露处理器拦截器
		chain.addInterceptor(new PathExposingHandlerInterceptor(bestMatchingPattern, pathWithinMapping));
		if (!CollectionUtils.isEmpty(uriTemplateVariables)) {
			// 如果URI模板变量不为空，则添加URI模板变量处理器拦截器
			chain.addInterceptor(new UriTemplateVariablesHandlerInterceptor(uriTemplateVariables));
		}
		return chain;
	}

	/**
	 * 将路径暴露在当前映射中作为请求属性。
	 *
	 * @param bestMatchingPattern 最佳匹配模式
	 * @param pathWithinMapping   映射内的路径
	 * @param request             请求对象
	 * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	protected void exposePathWithinMapping(String bestMatchingPattern, String pathWithinMapping,
										   HttpServletRequest request) {

		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
		request.setAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, pathWithinMapping);
	}

	/**
	 * 将 URI 模板变量暴露为请求属性。
	 *
	 * @param uriTemplateVariables URI 模板变量
	 * @param request              请求对象
	 * @see #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE
	 */
	protected void exposeUriTemplateVariables(Map<String, String> uriTemplateVariables, HttpServletRequest request) {
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVariables);
	}

	@Override
	@Nullable
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.isNull(getPatternParser(), "This HandlerMapping uses PathPatterns.");
		// 获取解析后的查找路径
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		if (getPathMatcher().match(pattern, lookupPath)) {
			// 如果查找路径与模式匹配，则返回请求匹配结果
			return new RequestMatchResult(pattern, lookupPath, getPathMatcher());
		} else if (useTrailingSlashMatch()) {
			// 如果使用尾部斜杠匹配，并且模式不以斜杠结尾，且模式加斜杠后与查找路径匹配，则返回请求匹配结果
			if (!pattern.endsWith("/") && getPathMatcher().match(pattern + "/", lookupPath)) {
				return new RequestMatchResult(pattern + "/", lookupPath, getPathMatcher());
			}
		}
		return null;
		// 没有匹配则返回空
	}

	/**
	 * 注册指定的处理程序与给定的 URL 路径。
	 *
	 * @param urlPaths 将 bean 映射到的 URL
	 * @param beanName 处理程序 bean 的名称
	 * @throws BeansException        如果无法注册处理程序
	 * @throws IllegalStateException 如果已注册冲突的处理程序
	 */
	protected void registerHandler(String[] urlPaths, String beanName) throws BeansException, IllegalStateException {
		Assert.notNull(urlPaths, "URL path array must not be null");
		for (String urlPath : urlPaths) {
			// 注册每一个路径和bean名称的处理器
			registerHandler(urlPath, beanName);
		}
	}

	/**
	 * 注册指定的处理程序与给定的 URL 路径。
	 *
	 * @param urlPath 要将 bean 映射到的 URL
	 * @param handler 处理程序实例或处理程序 bean 名称字符串
	 *                （bean 名称将自动解析为相应的处理程序 bean）
	 * @throws BeansException        如果无法注册处理程序
	 * @throws IllegalStateException 如果已注册冲突的处理程序
	 */
	protected void registerHandler(String urlPath, Object handler) throws BeansException, IllegalStateException {
		Assert.notNull(urlPath, "URL path must not be null");
		Assert.notNull(handler, "Handler object must not be null");
		Object resolvedHandler = handler;

		// 如果不延迟初始化处理器且处理器是通过名称引用单例，则急切地解析处理器
		if (!this.lazyInitHandlers && handler instanceof String) {
			String handlerName = (String) handler;
			// 获取应用上下文
			ApplicationContext applicationContext = obtainApplicationContext();
			if (applicationContext.isSingleton(handlerName)) {
				// 如果该处理器名称是单例，则获取该名称对应的处理器对象
				resolvedHandler = applicationContext.getBean(handlerName);
			}
		}

		// 获取映射到URL路径的已映射处理器
		Object mappedHandler = this.handlerMap.get(urlPath);
		if (mappedHandler != null) {
			// 如果已映射处理器与解析后的处理器不同，则抛出异常
			if (mappedHandler != resolvedHandler) {
				throw new IllegalStateException(
						"Cannot map " + getHandlerDescription(handler) + " to URL path [" + urlPath +
								"]: There is already " + getHandlerDescription(mappedHandler) + " mapped.");
			}
		} else {
			// 如果URL路径为根路径，则设置根处理器
			if (urlPath.equals("/")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Root mapping to " + getHandlerDescription(handler));
				}
				// 设置根处理器
				setRootHandler(resolvedHandler);
			} else if (urlPath.equals("/*")) {
				// 如果URL路径为默认路径，则设置默认处理器
				if (logger.isTraceEnabled()) {
					logger.trace("Default mapping to " + getHandlerDescription(handler));
				}
				// 设置默认处理器对象
				setDefaultHandler(resolvedHandler);
			} else {
				// 将解析后的处理器映射到URL路径
				this.handlerMap.put(urlPath, resolvedHandler);
				if (getPatternParser() != null) {
					// 如果有模式解析器，则将解析后的处理器映射到路径模式处理器映射表中
					this.pathPatternHandlerMap.put(getPatternParser().parse(urlPath), resolvedHandler);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Mapped [" + urlPath + "] onto " + getHandlerDescription(handler));
				}
			}
		}
	}

	private String getHandlerDescription(Object handler) {
		return (handler instanceof String ? "'" + handler + "'" : handler.toString());
	}


	/**
	 * 获取处理程序映射的只读 Map，其中注册的路径或模式作为键，处理程序对象（或延迟初始化处理程序的处理程序 bean 名称）作为值。
	 *
	 * @see #getDefaultHandler()
	 */
	public final Map<String, Object> getHandlerMap() {
		return Collections.unmodifiableMap(this.handlerMap);
	}

	/**
	 * 与 {@link #getHandlerMap()} 相同，但在解析模式为 {@link #usesPathPatterns() 启用} 时填充；否则为空。
	 *
	 * @since 5.3
	 */
	public final Map<PathPattern, Object> getPathPatternHandlerMap() {
		return (this.pathPatternHandlerMap.isEmpty() ?
				Collections.emptyMap() : Collections.unmodifiableMap(this.pathPatternHandlerMap));
	}

	/**
	 * 指示此处理程序映射是否支持类型级别的映射。默认为 {@code false}。
	 */
	protected boolean supportsTypeLevelMappings() {
		return false;
	}


	/**
	 * 用于公开 {@link AbstractUrlHandlerMapping#PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE} 属性的特殊拦截器。
	 *
	 * @see AbstractUrlHandlerMapping#exposePathWithinMapping
	 */
	private class PathExposingHandlerInterceptor implements HandlerInterceptor {
		/**
		 * 最佳匹配模式
		 */
		private final String bestMatchingPattern;

		/**
		 * 映射内的路径
		 */
		private final String pathWithinMapping;

		public PathExposingHandlerInterceptor(String bestMatchingPattern, String pathWithinMapping) {
			this.bestMatchingPattern = bestMatchingPattern;
			this.pathWithinMapping = pathWithinMapping;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			// 暴露映射内的路径
			exposePathWithinMapping(this.bestMatchingPattern, this.pathWithinMapping, request);
			// 在请求中，设置最佳匹配处理器
			request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
			// 在请求中，设置是否支持内省级别映射
			request.setAttribute(INTROSPECT_TYPE_LEVEL_MAPPING, supportsTypeLevelMappings());
			return true;
		}

	}

	/**
	 * 用于公开 {@link AbstractUrlHandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE} 属性的特殊拦截器。
	 *
	 * @see AbstractUrlHandlerMapping#exposeUriTemplateVariables
	 */
	private class UriTemplateVariablesHandlerInterceptor implements HandlerInterceptor {
		/**
		 * URI模板变量
		 */
		private final Map<String, String> uriTemplateVariables;

		public UriTemplateVariablesHandlerInterceptor(Map<String, String> uriTemplateVariables) {
			this.uriTemplateVariables = uriTemplateVariables;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			// 暴露URL模板变量
			exposeUriTemplateVariables(this.uriTemplateVariables, request);
			return true;
		}
	}

}
