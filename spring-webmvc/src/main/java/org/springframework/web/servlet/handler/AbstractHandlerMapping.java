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

package org.springframework.web.servlet.handler;

import org.apache.commons.logging.Log;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogDelegateFactory;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.*;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} 实现的抽象基类。支持排序、默认处理程序、处理程序拦截器，
 * 包括根据路径模式映射的处理程序拦截器。
 *
 * <p>注意：此基类不支持 {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE} 的公开。对该属性的支持取决于具体的子类，
 * 通常基于请求 URL 映射。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 * @since 07.04.2003
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	/**
	 * 专用于请求映射的“隐藏”记录器。
	 */
	protected final Log mappingsLogger =
			LogDelegateFactory.getHiddenLog(HandlerMapping.class.getName() + ".Mappings");


	/**
	 * 默认的处理器
	 */
	@Nullable
	private Object defaultHandler;

	/**
	 * 路径模式解析器
	 */
	@Nullable
	private PathPatternParser patternParser;

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	/**
	 * 路径匹配器
	 */
	private PathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 处理程序拦截器
	 */
	private final List<Object> interceptors = new ArrayList<>();

	/**
	 * 适配的处理程序拦截器列表
	 */
	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

	/**
	 * 跨域配置源
	 */
	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	/**
	 * 跨域处理器
	 */
	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	/**
	 * 排序，默认为无排序
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * bean名称
	 */
	@Nullable
	private String beanName;


	/**
	 * 设置此处理程序映射的默认处理程序。如果没有找到特定的映射，则将返回此处理程序。
	 * <p>默认值为 {@code null}，表示没有默认处理程序。
	 *
	 * @param defaultHandler 默认处理程序
	 */
	public void setDefaultHandler(@Nullable Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * 返回此处理程序映射的默认处理程序，如果没有则返回 {@code null}。
	 *
	 * @return 默认处理程序
	 */
	@Nullable
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * 启用预解析的 {@link PathPattern} 作为 {@link AntPathMatcher} 的字符串模式匹配的替代方法。
	 * 语法基本相同，但 {@code PathPattern} 语法更适用于 Web 应用程序，其实现更高效。
	 * <p>此属性与以下其他属性互斥，当设置此属性时，这些属性将被忽略：
	 * <ul>
	 * <li>{@link #setAlwaysUseFullPath} -- {@code PathPatterns} 始终使用完整路径，并忽略解码的 servletPath/pathInfo，
	 * 因此无法与 {@link HttpServletRequest#getRequestURI() requestURI} 进行比较。
	 * <li>{@link #setRemoveSemicolonContent} -- {@code PathPatterns} 始终忽略用于路径匹配的分号内容，
	 * 但路径参数仍然可通过 {@code @MatrixVariable} 在控制器中使用。
	 * <li>{@link #setUrlDecode} -- {@code PathPatterns} 一次匹配一个已解码的路径段，永远不需要完整的已解码路径，
	 * 因为完整的已解码路径可能因已解码的保留字符而出现问题。
	 * <li>{@link #setUrlPathHelper} -- 请求路径由 {@link org.springframework.web.servlet.DispatcherServlet
	 * DispatcherServlet} 或由 {@link org.springframework.web.filter.ServletRequestPathFilter
	 * ServletRequestPathFilter} 使用 {@link ServletRequestPathUtils} 全局预解析，并保存在请求属性中以便重用。
	 * <li>{@link #setPathMatcher} -- 将模式解析为 {@code PathPatterns}，并用于与 {@code PathMatcher} 的字符串匹配替代。
	 * </ul>
	 * <p>默认情况下未设置。
	 *
	 * @param patternParser 要使用的解析器
	 * @since 5.3
	 */
	public void setPatternParser(PathPatternParser patternParser) {
		this.patternParser = patternParser;
	}

	/**
	 * 返回 {@link #setPatternParser(PathPatternParser) 配置的} {@code PathPatternParser}，如果没有则返回 {@code null}。
	 *
	 * @since 5.3
	 */
	@Nullable
	public PathPatternParser getPatternParser() {
		return this.patternParser;
	}

	/**
	 * 对配置的 {@code UrlPathHelper} 上的相同属性进行快捷设置。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)} 设置互斥，并且在设置时将被忽略。
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			// 如果跨域配置源是 UrlBasedCorsConfigurationSource 类型，则设置是否经常使用全路径
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setAlwaysUseFullPath(alwaysUseFullPath);
		}
	}

	/**
	 * 对底层 {@code UrlPathHelper} 上的相同属性进行快捷设置。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)} 设置互斥，并且在设置时将被忽略。
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			// 如果跨域配置源是 UrlBasedCorsConfigurationSource 类型，则设置是否Url解码
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlDecode(urlDecode);
		}
	}

	/**
	 * 对底层 {@code UrlPathHelper} 上的相同属性进行快捷设置。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)} 设置互斥，并且在设置时将被忽略。
	 *
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	@SuppressWarnings("deprecation")
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			// 如果跨域配置源是 UrlBasedCorsConfigurationSource 类型，则设置去除分号内容
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setRemoveSemicolonContent(removeSemicolonContent);
		}
	}

	/**
	 * 配置要用于查找路径的 UrlPathHelper。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)} 设置互斥。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			// 如果跨域配置源是 UrlBasedCorsConfigurationSource 类型，则设置URL路径助手
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlPathHelper(urlPathHelper);
		}
	}

	/**
	 * 返回 {@link #setUrlPathHelper 配置的} {@code UrlPathHelper}。
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 配置要使用的 PathMatcher。
	 * <p><strong>注意：</strong>此属性与 {@link #setPatternParser(PathPatternParser)} 设置互斥，并且在设置时将被忽略。
	 * <p>默认情况下是 {@link AntPathMatcher}。
	 *
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			// 如果跨域配置源是 UrlBasedCorsConfigurationSource 则设置路径匹配器
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setPathMatcher(pathMatcher);
		}
	}

	/**
	 * 返回 {@link #setPathMatcher 配置的} {@code PathMatcher}。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 设置要应用于此处理程序映射中的所有处理程序的拦截器。
	 * <p>支持的拦截器类型包括 {@link HandlerInterceptor}、{@link WebRequestInterceptor} 和 {@link MappedInterceptor}。
	 * Mapped 拦截器仅适用于与其路径模式匹配的请求 URL。在初始化期间还会检测到 Mapped 拦截器 bean 的类型。
	 *
	 * @param interceptors 处理程序拦截器数组
	 * @see #adaptInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see MappedInterceptor
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * 设置“全局”CORS 配置映射。第一个匹配的 URL 模式确定要使用的 {@code CorsConfiguration}，
	 * 然后进一步与所选处理程序的 {@code CorsConfiguration} 进行 {@link CorsConfiguration#combine(CorsConfiguration) 结合}。
	 * <p>这与 {@link #setCorsConfigurationSource(CorsConfigurationSource)} 互斥。
	 *
	 * @see #setCorsProcessor(CorsProcessor)
	 * @since 4.2
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		if (CollectionUtils.isEmpty(corsConfigurations)) {
			// 跨域配置为空，则设置跨域配置源为空，结束
			this.corsConfigurationSource = null;
			return;
		}
		UrlBasedCorsConfigurationSource source;
		if (getPatternParser() != null) {
			// 使用模式解析器创建UrlBasedCorsConfigurationSource
			source = new UrlBasedCorsConfigurationSource(getPatternParser());
			// 设置跨域配置
			source.setCorsConfigurations(corsConfigurations);
		} else {
			// 使用默认构造函数创建UrlBasedCorsConfigurationSource
			source = new UrlBasedCorsConfigurationSource();
			// 设置跨域配置
			source.setCorsConfigurations(corsConfigurations);
			// 设置路径匹配器
			source.setPathMatcher(this.pathMatcher);
			// 设置URL路径助手
			source.setUrlPathHelper(this.urlPathHelper);
		}
		// 设置跨域配置源
		setCorsConfigurationSource(source);
	}

	/**
	 * 为“全局”CORS 配置设置 {@code CorsConfigurationSource}。
	 * 由源确定的 {@code CorsConfiguration} 与所选处理程序的 {@code CorsConfiguration}
	 * 进行 {@link CorsConfiguration#combine(CorsConfiguration) 结合}。
	 * <p>这与 {@link #setCorsConfigurations(Map)} 互斥。
	 *
	 * @see #setCorsProcessor(CorsProcessor)
	 * @since 5.1
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource source) {
		Assert.notNull(source, "CorsConfigurationSource must not be null");
		this.corsConfigurationSource = source;
		if (source instanceof UrlBasedCorsConfigurationSource) {
			// 如果跨域配置源是 UrlBasedCorsConfigurationSource 类型，则设置不允许初始化查找路径
			((UrlBasedCorsConfigurationSource) source).setAllowInitLookupPath(false);
		}
	}

	/**
	 * 返回配置的 {@link #setCorsConfigurationSource(CorsConfigurationSource) CorsConfigurationSource}（如果有）。
	 *
	 * @since 5.3
	 */
	@Nullable
	public CorsConfigurationSource getCorsConfigurationSource() {
		return this.corsConfigurationSource;
	}

	/**
	 * 配置要使用的自定义 {@link CorsProcessor} 来应用匹配的 {@link CorsConfiguration}。
	 * <p>默认情况下使用 {@link DefaultCorsProcessor}。
	 *
	 * @since 4.2
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * 返回配置的 {@link CorsProcessor}。
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * 指定此 HandlerMapping bean 的排序值。
	 * <p>默认值是 {@code Ordered.LOWEST_PRECEDENCE}，表示非排序的。
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

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : getClass().getName();
	}


	/**
	 * 初始化拦截器。
	 *
	 * @see #extendInterceptors(java.util.List)
	 * @see #initInterceptors()
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		// 扩展拦截器列表
		extendInterceptors(this.interceptors);
		// 检测映射的拦截器
		detectMappedInterceptors(this.adaptedInterceptors);
		// 初始化拦截器
		initInterceptors();
	}

	/**
	 * 子类可以重写的扩展钩子方法，用于注册额外的拦截器，
	 * 给定配置的拦截器（参见 {@link #setInterceptors}）。
	 * <p>在 {@link #initInterceptors()} 将指定的拦截器适配为
	 * {@link HandlerInterceptor} 实例之前调用。
	 * <p>默认实现为空。
	 *
	 * @param interceptors 配置的拦截器 List（永不为 {@code null}），
	 *                     允许在现有拦截器之前和之后添加更多拦截器
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * 检测类型为 {@link MappedInterceptor} 的 bean，并将其添加到
	 * 映射拦截器列表中。
	 * <p>这将被调用，除了可能已经通过 {@link #setInterceptors} 提供的
	 * 任何 {@link MappedInterceptor} 之外，默认情况下，将从当前上下文及其
	 * 祖先中添加所有类型为 {@link MappedInterceptor} 的 bean。子类可以
	 * 重写并优化此策略。
	 *
	 * @param mappedInterceptors 要添加的空列表
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(BeanFactoryUtils.beansOfTypeIncludingAncestors(
				// 获取应用上下文中的所有 MappedInterceptor 类型的 Bean，包括祖先上下文中的 Bean
				obtainApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * 初始化指定的拦截器，将 {@link WebRequestInterceptor} 适配为
	 * {@link HandlerInterceptor}。
	 *
	 * @see #setInterceptors
	 * @see #adaptInterceptor
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				// 获取拦截器数组中的每一个元素
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					// 如果拦截器为 null，则抛出 IllegalArgumentException 异常
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				// 将适配后的拦截器添加到适配后拦截器列表中
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * 将给定的拦截器对象适配为 {@link HandlerInterceptor}。
	 * <p>默认情况下，支持的拦截器类型为
	 * {@link HandlerInterceptor} 和 {@link WebRequestInterceptor}。
	 * 每个给定的 {@link WebRequestInterceptor} 都会被包装在
	 * {@link WebRequestHandlerInterceptorAdapter} 中。
	 *
	 * @param interceptor 拦截器
	 * @return 向下转型或适配为 HandlerInterceptor 的拦截器
	 * @throws IllegalArgumentException 如果不支持的拦截器类型
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			// 如果拦截器是 HandlerInterceptor 类型，则直接返回
			return (HandlerInterceptor) interceptor;
		} else if (interceptor instanceof WebRequestInterceptor) {
			// 如果拦截器是 WebRequestInterceptor 类型，则包装成 WebRequestHandlerInterceptorAdapter 后返回
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		} else {
			// 如果拦截器类型不受支持，则抛出 IllegalArgumentException 异常
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * 返回适配后的拦截器数组 {@link HandlerInterceptor}。
	 *
	 * @return {@link HandlerInterceptor HandlerInterceptor} 数组，
	 * 如果没有则返回 {@code null}
	 */
	@Nullable
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return (!this.adaptedInterceptors.isEmpty() ?
				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * 返回所有配置的 {@link MappedInterceptor} 数组。
	 *
	 * @return {@link MappedInterceptor MappedInterceptor} 数组，
	 * 如果没有则返回 {@code null}
	 */
	@Nullable
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			// 如果拦截器是 MappedInterceptor 类型，则将其添加到列表中
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		// 如果存在 MappedInterceptor 类型的拦截器，则返回数组形式的拦截器列表；否则返回 null
		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
	}


	/**
	 * 如果此 {@code HandlerMapping} 已启用解析的 {@code PathPattern}，则返回 "true"。
	 *
	 * @return 如果已启用解析的 {@code PathPattern}，则返回 "true"
	 */
	@Override
	public boolean usesPathPatterns() {
		return getPatternParser() != null;
	}

	/**
	 * 查找给定请求的处理器，如果没有找到特定的处理器，则返回默认处理器。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 对应的处理器实例，或默认处理器
	 * @throws Exception 如果发生内部错误
	 * @see #getHandlerInternal
	 */
	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		// 获取内部处理器
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			// 如果处理器为空，则使用默认处理器
			handler = getDefaultHandler();
		}
		if (handler == null) {
			// 如果处理器仍为空，则返回null
			return null;
		}
		if (handler instanceof String) {
			// 如果 handler 是一个字符串
			String handlerName = (String) handler;
			// 表示它是一个 Bean 的名称，需要解析成实际的处理程序对象
			handler = obtainApplicationContext().getBean(handlerName);
		}

		// 确保拦截器和其他内容的缓存查找路径存在
		if (!ServletRequestPathUtils.hasCachedPath(request)) {
			// 初始化查找路径
			initLookupPath(request);
		}

		// 获取处理程序的执行链
		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

		// 如果日志记录处于跟踪级别，则记录映射的处理程序
		if (logger.isTraceEnabled()) {
			logger.trace("Mapped to " + handler);
		} else if (logger.isDebugEnabled() && !DispatcherType.ASYNC.equals(request.getDispatcherType())) {
			// 如果日志记录处于调试级别，并且请求的调度类型不是异步的，则记录执行链中的处理程序
			logger.debug("Mapped to " + executionChain.getHandler());
		}

		if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {
			// 如果处理程序具有 CORS 配置源或请求是预检请求，则获取 CORS 配置
			CorsConfiguration config = getCorsConfiguration(handler, request);
			if (getCorsConfigurationSource() != null) {
				// 如果跨域配置源存在，则从跨域配置源中获取跨域配置
				CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);
				// 如果全局配置存在，则组合当前的跨域配置，否则返回当前跨域配置
				config = (globalConfig != null ? globalConfig.combine(config) : config);
			}
			if (config != null) {
				// 跨域配置存在，则校验是否允许支持用户凭证
				config.validateAllowCredentials();
			}
			// 获取 CORS 处理程序的执行链
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}

		return executionChain;
	}

	/**
	 * 查找给定请求的处理器，如果没有找到特定的处理器，则返回 {@code null}。
	 * 此方法由 {@link #getHandler} 调用；返回 {@code null} 将导致默认处理器（如果已设置）。
	 * <p>
	 * 对于 CORS 预检请求，此方法应该返回一个匹配的处理器，而不是预检请求本身，
	 * 而是基于 URL 路径、来自 "Access-Control-Request-Method" 头的 HTTP 方法，以及来自
	 * "Access-Control-Request-Headers" 头的头部信息，从而允许通过 {@link #getCorsConfiguration(Object, HttpServletRequest)} 获取 CORS 配置，
	 * </p>
	 * <p>
	 * 注意：此方法还可以返回预先构建的 {@link HandlerExecutionChain}，将处理器对象与动态确定的拦截器结合起来。
	 * 静态指定的拦截器将合并到这样一个现有的链中。
	 * </p>
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 对应的处理器实例，如果没有找到则返回 {@code null}
	 * @throws Exception 如果发生内部错误
	 */
	@Nullable
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * 初始化用于请求映射的路径。
	 * <p>
	 * 当解析的模式 {@link #usesPathPatterns() 已启用} 时，期望由
	 * {@link org.springframework.web.servlet.DispatcherServlet} 或 {@link org.springframework.web.filter.ServletRequestPathFilter}
	 * 外部 {@link ServletRequestPathUtils#parseAndCache(HttpServletRequest) 解析} 的
	 * {@code RequestPath} 已 {@link ServletRequestPathUtils#getParsedRequestPath(HttpServletRequest) 解析}。
	 * </p>
	 * <p>
	 * 否则，对于通过 {@code PathMatcher} 进行字符串模式匹配的情况，路径是由此方法 {@link UrlPathHelper#resolveAndCacheLookupPath 解析和缓存} 的。
	 * </p>
	 *
	 * @param request 当前请求
	 * @return 解析的路径
	 * @since 5.3
	 */
	protected String initLookupPath(HttpServletRequest request) {
		// 如果使用路径模式
		if (usesPathPatterns()) {
			// 移除请求中的路径属性
			request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);
			// 获取解析后的请求路径
			RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(request);
			// 获取应用程序内的路径
			String lookupPath = requestPath.pathWithinApplication().value();
			// 移除路径中的分号内容并返回
			return UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);
		} else {
			// 否则，使用 URL 路径助手解析并缓存查找路径，并返回
			return getUrlPathHelper().resolveAndCacheLookupPath(request);
		}
	}

	/**
	 * 为给定的处理器构建一个 {@link HandlerExecutionChain}，包括适用的拦截器。
	 * <p>
	 * 默认实现会构建一个标准的 {@link HandlerExecutionChain}，其中包括给定的处理器、处理器映射的常用拦截器，
	 * 以及与当前请求 URL 匹配的任何 {@link MappedInterceptor}。拦截器会按照它们被注册的顺序添加。
	 * 子类可以重写此方法以扩展/重新排列拦截器列表。
	 * </p>
	 * <p>
	 * <b>注意：</b>传入的处理器对象可能是原始处理器或预先构建的 {@link HandlerExecutionChain}。
	 * 此方法应明确处理这两种情况，要么构建一个新的 {@link HandlerExecutionChain}，要么扩展现有的链。
	 * </p>
	 * <p>
	 * 如果只需在自定义子类中添加一个拦截器，请考虑调用 {@code super.getHandlerExecutionChain(handler, request)}，
	 * 并在返回的链对象上调用 {@link HandlerExecutionChain#addInterceptor}。
	 * </p>
	 *
	 * @param handler 被解析的处理器实例（永远不会为 {@code null}）
	 * @param request 当前的 HTTP 请求
	 * @return HandlerExecutionChain（永远不会为 {@code null}）
	 * @see #getAdaptedInterceptors()
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		// 创建 处理器执行链 对象，如果 handler 已经是 处理器执行链 类型，则直接使用，否则创建一个新的 处理器执行链 对象
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		// 遍历适配后的拦截器集合
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				// 如果拦截器是 MappedInterceptor 类型
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(request)) {
					// 判断是否匹配当前请求，匹配则添加到 HandlerExecutionChain 中
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			} else {
				// 如果不是 MappedInterceptor 类型，则直接添加到 处理器执行链 中
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * 如果此处理器存在 {@link CorsConfigurationSource}，则返回 {@code true}。
	 *
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		if (handler instanceof HandlerExecutionChain) {
			// 如果 handler 是 HandlerExecutionChain 类型，则获取其中包装的实际 handler
			handler = ((HandlerExecutionChain) handler).getHandler();
		}
		// 返回结果为 handler 是否为 CorsConfigurationSource 类型，或者存在 跨域配置源
		return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);
	}

	/**
	 * 获取给定处理器的 CORS 配置。
	 *
	 * @param handler 要检查的处理器（永远不会为 {@code null}）。
	 * @param request 当前请求。
	 * @return 处理器的 CORS 配置，如果没有则返回 {@code null}
	 * @since 4.2
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			// 如果 handler 是 HandlerExecutionChain 类型，则获取其中包装的实际 handler
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			// 如果 resolvedHandler 是 CorsConfigurationSource 类型，则返回其获取的 跨域配置
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		// 否则返回 null
		return null;
	}

	/**
	 * 更新用于处理 CORS 相关内容的 HandlerExecutionChain。
	 * <p>
	 * 对于预检请求， 默认实现会将选定的处理器替换为一个简单的 HttpRequestHandler，
	 * 该处理器调用配置的 {@link #setCorsProcessor}。
	 * </p>
	 * <p>
	 * 对于实际请求， 默认实现会插入一个 HandlerInterceptor，执行 CORS 相关检查并添加 CORS 标头。
	 * </p>
	 *
	 * @param request 当前请求
	 * @param chain   处理器链
	 * @param config  适用的 CORS 配置（可能为 {@code null}）
	 * @since 4.2
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
																 HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		} else {
			chain.addInterceptor(0, new CorsInterceptor(config));
			return chain;
		}
	}


	/**
	 * {@code PreFlightHandler} 类是用于处理预检请求的 HTTP 请求处理器和跨域配置源的组合。
	 * <p>
	 * 它实现了 {@link HttpRequestHandler} 接口和 {@link CorsConfigurationSource} 接口。
	 * </p>
	 */
	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {
		/**
		 * 跨域配置
		 */
		@Nullable
		private final CorsConfiguration config;

		/**
		 * 创建一个 {@code PreFlightHandler} 实例。
		 *
		 * @param config 跨域配置信息
		 */
		public PreFlightHandler(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		/**
		 * 处理预检请求。
		 *
		 * @param request  当前的 HTTP 请求
		 * @param response 当前的 HTTP 响应
		 * @throws IOException 处理过程中可能抛出的 IO 异常
		 */
		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		/**
		 * 获取当前请求的跨域配置信息。
		 *
		 * @param request 当前的 HTTP 请求
		 * @return 当前请求的跨域配置信息
		 */
		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	/**
	 * {@code CorsInterceptor} 类是一个拦截器和跨域配置源的组合。
	 * <p>
	 * 它实现了 {@link HandlerInterceptor} 接口和 {@link CorsConfigurationSource} 接口。
	 * </p>
	 */
	private class CorsInterceptor implements HandlerInterceptor, CorsConfigurationSource {
		/**
		 * 跨域配置
		 */
		@Nullable
		private final CorsConfiguration config;

		/**
		 * 创建一个 {@code CorsInterceptor} 实例。
		 *
		 * @param config 跨域配置信息，可以为 {@code null}
		 */
		public CorsInterceptor(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		/**
		 * 在请求处理之前执行跨域处理逻辑。
		 *
		 * @param request  当前的 HTTP 请求
		 * @param response 当前的 HTTP 响应
		 * @param handler  处理请求的处理器对象
		 * @return 如果处理成功返回 {@code true}，否则返回 {@code false}
		 * @throws Exception 处理过程中可能抛出的异常
		 */
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			// 与 CorsFilter 一致，忽略异步调度
			// 获取 WebAsyncManager 对象
			WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
			// 如果已经存在并发结果，则返回 true
			if (asyncManager.hasConcurrentResult()) {
				return true;
			}
			// 处理 CORS 请求并返回结果
			return corsProcessor.processRequest(this.config, request, response);
		}

		/**
		 * 获取当前请求的跨域配置信息。
		 *
		 * @param request 当前的 HTTP 请求
		 * @return 当前请求的跨域配置信息
		 */
		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}
