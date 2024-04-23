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

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 实现了 {@link org.springframework.web.servlet.HandlerMapping} 接口，用于将 URL 映射到请求处理器 bean。
 * 支持将映射到 bean 实例和映射到 bean 名称；后者对于非单例处理器是必需的。
 *
 * <p>"urlMap" 属性适用于使用 bean 引用填充处理器映射，例如通过 XML bean 定义中的 map 元素。
 *
 * <p>可以通过 "mappings" 属性设置映射到 bean 名称，格式如下：
 *
 * <pre class="code">
 * /welcome.html=ticketController
 * /show.html=ticketController</pre>
 *
 * <p>语法为 {@code PATH=HANDLER_BEAN_NAME}。如果路径不以斜杠开头，则会添加一个斜杠。
 *
 * <p>支持直接匹配（给定 "/test" -&gt; 注册为 "/test"）和 "*" 模式匹配（给定 "/test" -&gt; 注册为 "/t*"）。
 * 注意，默认情况下是在当前 Servlet 映射内进行映射；请参阅 {@link #setAlwaysUseFullPath "alwaysUseFullPath"} 属性。
 * 有关模式选项的详细信息，请参阅 {@link org.springframework.util.AntPathMatcher} javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see #setMappings
 * @see #setUrlMap
 * @see BeanNameUrlHandlerMapping
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	/**
	 * URL路径与处理器bean或者处理bean名称的映射
	 */
	private final Map<String, Object> urlMap = new LinkedHashMap<>();


	/**
	 * 创建默认设置的 {@code SimpleUrlHandlerMapping}。
	 */
	public SimpleUrlHandlerMapping() {
	}

	/**
	 * 使用提供的 URL 映射创建 {@code SimpleUrlHandlerMapping}。
	 *
	 * @param urlMap URL 路径作为键，处理器 bean（或处理器 bean 名称）作为值的映射
	 * @see #setUrlMap(Map)
	 * @since 5.2
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
		setUrlMap(urlMap);
	}

	/**
	 * 使用提供的 URL 映射和顺序创建 {@code SimpleUrlHandlerMapping}。
	 *
	 * @param urlMap URL 路径作为键，处理器 bean（或处理器 bean 名称）作为值的映射
	 * @param order  此 {@code SimpleUrlHandlerMapping} 的顺序值
	 * @see #setUrlMap(Map)
	 * @see #setOrder(int)
	 * @since 5.2
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
		// 设置URL映射
		setUrlMap(urlMap);
		// 设置排序
		setOrder(order);
	}


	/**
	 * 将 URL 路径映射到处理器 bean 名称。
	 * 这是配置此 HandlerMapping 的典型方式。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。有关语法详细信息，请参阅 {@link org.springframework.util.AntPathMatcher} javadoc。
	 *
	 * @param mappings URL 作为键和 bean 名称作为值的属性
	 * @see #setUrlMap
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * 设置一个 Map，其中 URL 路径作为键，处理器 bean（或处理器 bean 名称）作为值。
	 * 用于填充 bean 引用时很方便。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。有关语法详细信息，请参阅 {@link org.springframework.util.AntPathMatcher} javadoc。
	 *
	 * @param urlMap 包含 URL 作为键和 bean 作为值的映射
	 * @see #setMappings
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * 允许通过 Map 访问 URL 路径映射，以添加或覆盖特定条目的选项。
	 * <p>对于直接指定条目很有用，例如通过 "urlMap[myKey]"。
	 * 这对于在子 bean 定义中添加或覆盖条目特别有用。
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * 在超类的初始化之外调用 {@link #registerHandlers} 方法。
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * 注册 URL 映射中指定的所有处理器。
	 *
	 * @param urlMap 包含 URL 路径作为键和处理器 bean 或 bean 名称作为值的映射
	 * @throws BeansException        如果无法注册处理器
	 * @throws IllegalStateException 如果已注册冲突的处理器
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.trace("No patterns in " + formatMappingName());
		} else {
			urlMap.forEach((url, handler) -> {
				// 如果路径不以斜杠开头，则添加斜杠。
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				if (handler instanceof String) {
					// 从处理器 bean 名称中删除空格。
					handler = ((String) handler).trim();
				}
				// 注册处理器
				registerHandler(url, handler);
			});
			logMappings();
		}
	}

	private void logMappings() {
		if (mappingsLogger.isDebugEnabled()) {
			Map<String, Object> map = new LinkedHashMap<>(getHandlerMap());
			if (getRootHandler() != null) {
				// 添加根处理器映射
				map.put("/", getRootHandler());
			}
			if (getDefaultHandler() != null) {
				// 添加默认处理器映射
				map.put("/**", getDefaultHandler());
			}
			mappingsLogger.debug(formatMappingName() + " " + map);
		} else if (logger.isDebugEnabled()) {
			List<String> patterns = new ArrayList<>();
			if (getRootHandler() != null) {
				patterns.add("/");
			}
			if (getDefaultHandler() != null) {
				patterns.add("/**");
			}
			patterns.addAll(getHandlerMap().keySet());
			logger.debug("Patterns " + patterns + " in " + formatMappingName());
		}
	}

}
