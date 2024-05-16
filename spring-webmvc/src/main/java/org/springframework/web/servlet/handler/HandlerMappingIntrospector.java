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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 辅助类，用于从{@code HandlerMapping}中获取将为特定请求提供服务的信息。
 *
 * <p>提供以下方法：
 * <ul>
 * <li>{@link #getMatchableHandlerMapping} &mdash; 获取一个{@code HandlerMapping}来检查请求匹配条件。
 * <li>{@link #getCorsConfiguration} &mdash; 获取请求的CORS配置。
 * </ul>
 *
 * <p><strong>注意：</strong> 这主要是一个SPI，允许Spring Security将其模式匹配与对于给定请求将在Spring MVC中使用的相同的模式匹配对齐，以避免安全问题。应避免将此内省器用于其他目的，因为它会产生解析请求处理程序的开销。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospector
		implements CorsConfigurationSource, ApplicationContextAware, InitializingBean {
	/**
	 * 应用上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;

	/**
	 * 处理器映射列表
	 */
	@Nullable
	private List<HandlerMapping> handlerMappings;

	/**
	 * 处理器映射 - 路径匹配映射映射
	 */
	private Map<HandlerMapping, PathPatternMatchableHandlerMapping> pathPatternHandlerMappings = Collections.emptyMap();


	/**
	 * 用于与{@link ApplicationContextAware}一起使用的构造函数。
	 */
	public HandlerMappingIntrospector() {
	}

	/**
	 * 构造函数，检测给定{@code ApplicationContext}中配置的{@code HandlerMapping}，或者回退到像{@code DispatcherServlet}一样的“DispatcherServlet.properties”。
	 *
	 * @deprecated 自4.3.12起，改用{@link #setApplicationContext}
	 */
	@Deprecated
	public HandlerMappingIntrospector(ApplicationContext context) {
		this.handlerMappings = initHandlerMappings(context);
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		// 如果处理程序映射为null，则初始化处理程序映射和路径模式处理程序映射
		if (this.handlerMappings == null) {
			Assert.notNull(this.applicationContext, "No ApplicationContext");
			// 初始化处理程序映射
			this.handlerMappings = initHandlerMappings(this.applicationContext);
			// 初始化路径模式匹配处理程序映射
			this.pathPatternHandlerMappings = initPathPatternMatchableHandlerMappings(this.handlerMappings);
		}
	}

	/**
	 * 返回配置或检测到的{@code HandlerMapping}。
	 */
	public List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? this.handlerMappings : Collections.emptyList());
	}


	/**
	 * 查找将处理给定请求的{@link HandlerMapping}，并将其作为{@link MatchableHandlerMapping}返回，以便用于测试请求匹配条件。
	 * <p>如果匹配的HandlerMapping不是{@link MatchableHandlerMapping}的实例，则会引发IllegalStateException。
	 *
	 * @param request 当前请求
	 * @return 解析的匹配器，或{@code null}
	 * @throws Exception 如果任何HandlerMapping引发异常
	 */
	@Nullable
	public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
		// 使用保留属性的请求对象
		HttpServletRequest wrappedRequest = new AttributesPreservingRequest(request);

		// 调用doWithMatchingMapping方法，处理匹配的映射
		return doWithMatchingMapping(wrappedRequest, false, (matchedMapping, executionChain) -> {
			// 检查匹配的映射是否是MatchableHandlerMapping类型
			if (matchedMapping instanceof MatchableHandlerMapping) {
				// 如果是PathPatternMatchableHandlerMapping类型，则从路径模式处理映射中获取映射
				PathPatternMatchableHandlerMapping mapping = this.pathPatternHandlerMappings.get(matchedMapping);
				if (mapping != null) {
					// 获取解析的请求路径
					RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(wrappedRequest);
					// 返回一个带有路径设置的HandlerMapping对象
					return new PathSettingHandlerMapping(mapping, requestPath);
				} else {
					// 否则，从请求的UrlPathHelper.PATH_ATTRIBUTE属性中获取查找路径，并返回带有路径设置的HandlerMapping对象
					String lookupPath = (String) wrappedRequest.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
					return new PathSettingHandlerMapping((MatchableHandlerMapping) matchedMapping, lookupPath);
				}
			}
			// 如果匹配的映射不是MatchableHandlerMapping类型，则抛出异常
			throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
		});
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		// 创建一个保留属性的请求对象
		AttributesPreservingRequest wrappedRequest = new AttributesPreservingRequest(request);

		// 调用doWithMatchingMappingIgnoringException方法，处理匹配的映射并忽略异常
		return doWithMatchingMappingIgnoringException(wrappedRequest, (handlerMapping, executionChain) -> {
			// 遍历处理程序执行链中的拦截器列表
			for (HandlerInterceptor interceptor : executionChain.getInterceptorList()) {
				// 如果拦截器是CorsConfigurationSource类型，则获取CORS配置并返回
				if (interceptor instanceof CorsConfigurationSource) {
					return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrappedRequest);
				}
			}
			// 如果处理程序是CorsConfigurationSource类型，则获取CORS配置并返回
			if (executionChain.getHandler() instanceof CorsConfigurationSource) {
				return ((CorsConfigurationSource) executionChain.getHandler()).getCorsConfiguration(wrappedRequest);
			}
			// 如果没有找到适当的CORS配置，则返回null
			return null;
		});
	}

	@Nullable
	private <T> T doWithMatchingMapping(
			HttpServletRequest request, boolean ignoreException,
			BiFunction<HandlerMapping, HandlerExecutionChain, T> matchHandler) throws Exception {

		// 断言处理程序映射已经初始化
		Assert.notNull(this.handlerMappings, "Handler mappings not initialized");

		// 是否解析请求路径取决于是否存在路径模式处理程序映射
		boolean parseRequestPath = !this.pathPatternHandlerMappings.isEmpty();

		RequestPath previousPath = null;
		if (parseRequestPath) {
			// 如果需要解析请求路径，则缓存先前的路径，并在此处解析请求路径
			previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
			ServletRequestPathUtils.parseAndCache(request);
		}

		try {
			// 遍历处理程序映射列表
			for (HandlerMapping handlerMapping : this.handlerMappings) {
				HandlerExecutionChain chain = null;
				try {
					// 尝试获取处理程序执行链
					chain = handlerMapping.getHandler(request);
				} catch (Exception ex) {
					// 如果发生异常且不忽略异常，则重新抛出异常
					if (!ignoreException) {
						throw ex;
					}
				}
				// 如果处理程序执行链为空，则继续下一个处理程序映射
				if (chain == null) {
					continue;
				}
				// 使用matchHandler函数应用处理程序映射和处理程序执行链，并返回结果
				return matchHandler.apply(handlerMapping, chain);
			}
		} finally {
			if (parseRequestPath) {
				// 如果需要解析请求路径，则在finally块中恢复先前的路径状态
				ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
			}
		}

		// 如果未找到匹配的处理程序映射，则返回null
		return null;
	}

	@Nullable
	private <T> T doWithMatchingMappingIgnoringException(
			HttpServletRequest request, BiFunction<HandlerMapping, HandlerExecutionChain, T> matchHandler) {

		try {
			// 调用doWithMatchingMapping方法处理匹配的映射
			return doWithMatchingMapping(request, true, matchHandler);
		} catch (Exception ex) {
			// 如果捕获到异常，则抛出IllegalStateException异常，其中包含原始异常信息
			throw new IllegalStateException("HandlerMapping exception not suppressed", ex);
		}
	}


	private static List<HandlerMapping> initHandlerMappings(ApplicationContext applicationContext) {
		// 获取应用程序上下文中所有的HandlerMapping bean
		Map<String, HandlerMapping> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				applicationContext, HandlerMapping.class, true, false);
		// 如果存在HandlerMapping bean
		if (!beans.isEmpty()) {
			// 创建HandlerMapping列表，并对其进行排序
			List<HandlerMapping> mappings = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(mappings);
			return Collections.unmodifiableList(mappings);
		} else {
			// 如果不存在HandlerMapping bean，则初始化回退列表
			return Collections.unmodifiableList(initFallback(applicationContext));
		}
	}

	private static List<HandlerMapping> initFallback(ApplicationContext applicationContext) {
		// 初始化Properties对象
		Properties props;
		String path = "DispatcherServlet.properties";
		try {
			// 从类路径中加载属性文件
			Resource resource = new ClassPathResource(path, DispatcherServlet.class);
			props = PropertiesLoaderUtils.loadProperties(resource);
		} catch (IOException ex) {
			throw new IllegalStateException("Could not load '" + path + "': " + ex.getMessage());
		}

		// 从属性文件中获取HandlerMapping类名字符串
		String value = props.getProperty(HandlerMapping.class.getName());
		// 将类名字符串转换为数组
		String[] names = StringUtils.commaDelimitedListToStringArray(value);
		// 创建HandlerMapping列表
		List<HandlerMapping> result = new ArrayList<>(names.length);
		// 遍历类名数组
		for (String name : names) {
			try {
				// 根据类名加载类
				Class<?> clazz = ClassUtils.forName(name, DispatcherServlet.class.getClassLoader());
				// 创建HandlerMapping对象并添加到结果列表中
				Object mapping = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
				result.add((HandlerMapping) mapping);
			} catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
			}
		}
		return result;
	}

	private static Map<HandlerMapping, PathPatternMatchableHandlerMapping> initPathPatternMatchableHandlerMappings(
			List<HandlerMapping> mappings) {

		return mappings.stream()
				// 过滤出实现了MatchableHandlerMapping接口的HandlerMapping对象
				.filter(mapping -> mapping instanceof MatchableHandlerMapping)
				// 将过滤出的HandlerMapping对象转换为MatchableHandlerMapping类型
				.map(mapping -> (MatchableHandlerMapping) mapping)
				// 过滤出 模式解析器 不为空的HandlerMapping对象
				.filter(mapping -> mapping.getPatternParser() != null)
				// 将符合条件的HandlerMapping对象映射为PathPatternMatchableHandlerMapping对象，并收集到Map中
				.collect(Collectors.toMap(mapping -> mapping, PathPatternMatchableHandlerMapping::new));
	}


	/**
	 * 请求包装器，用于缓冲请求属性，以保护底层请求免受属性更改的影响。
	 */
	private static class AttributesPreservingRequest extends HttpServletRequestWrapper {
		/**
		 * 请求属性
		 */
		private final Map<String, Object> attributes;

		AttributesPreservingRequest(HttpServletRequest request) {
			super(request);
			this.attributes = initAttributes(request);
		}

		private Map<String, Object> initAttributes(HttpServletRequest request) {
			// 创建一个空的HashMap对象，用于存储请求属性的名称和值
			Map<String, Object> map = new HashMap<>();

			// 获取请求中的所有属性名称的枚举
			Enumeration<String> names = request.getAttributeNames();
			// 遍历枚举中的每个属性名称
			while (names.hasMoreElements()) {
				// 获取下一个属性名称
				String name = names.nextElement();
				// 将属性名称及其对应的值存储到map中
				map.put(name, request.getAttribute(name));
			}

			// 返回包含请求属性名称和值的map
			return map;
		}

		@Override
		public void setAttribute(String name, Object value) {
			this.attributes.put(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			return this.attributes.get(name);
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return Collections.enumeration(this.attributes.keySet());
		}

		@Override
		public void removeAttribute(String name) {
			this.attributes.remove(name);
		}
	}


	private static class PathSettingHandlerMapping implements MatchableHandlerMapping {
		/**
		 * 代理的可匹配的处理器映射
		 */
		private final MatchableHandlerMapping delegate;

		/**
		 * 路径对象
		 */
		private final Object path;

		/**
		 * 路径参数名称
		 */
		private final String pathAttributeName;

		PathSettingHandlerMapping(MatchableHandlerMapping delegate, Object path) {
			this.delegate = delegate;
			this.path = path;
			this.pathAttributeName = (path instanceof RequestPath ?
					ServletRequestPathUtils.PATH_ATTRIBUTE : UrlPathHelper.PATH_ATTRIBUTE);
		}

		@Nullable
		@Override
		public RequestMatchResult match(HttpServletRequest request, String pattern) {
			// 获取之前的路径属性值
			Object previousPath = request.getAttribute(this.pathAttributeName);
			// 将当前路径设置为请求的路径属性值
			request.setAttribute(this.pathAttributeName, this.path);
			try {
				// 调用委托对象的匹配方法
				return this.delegate.match(request, pattern);
			} finally {
				// 将之前的路径属性值恢复到请求中
				request.setAttribute(this.pathAttributeName, previousPath);
			}
		}

		@Nullable
		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
			return this.delegate.getHandler(request);
		}
	}

}
