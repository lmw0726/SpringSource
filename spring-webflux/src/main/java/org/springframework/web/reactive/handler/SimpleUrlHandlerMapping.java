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

package org.springframework.web.reactive.handler;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 实现了 {@link org.springframework.web.reactive.HandlerMapping} 接口，
 * 用于将 URL 映射到请求处理程序 bean。支持映射到 bean 实例和映射到 bean 名称；
 * 对于非单例处理程序，后者是必需的。
 *
 * <p>"urlMap" 属性适用于使用 bean 实例填充处理程序映射。
 * 可以通过 "mappings" 属性设置到 bean 名称的映射，形式可以是 {@code java.util.Properties} 类接受的形式，
 * 如下所示：
 *
 * <pre class="code">
 * /welcome.html=ticketController
 * /show.html=ticketController</pre>
 *
 * <p>语法是 {@code PATH=HANDLER_BEAN_NAME}。如果路径不以斜杠开头，则会添加斜杠。
 *
 * <p>支持直接匹配，例如注册的 "/test" 匹配 "/test"，
 * 以及各种 Ant 风格的模式匹配，例如注册的 "/t*" 模式匹配 "/test" 和 "/team"，
 * "/test/*" 匹配 "/test" 下的所有路径，"/test/**" 匹配 "/test" 下的所有路径及其子路径。
 * 有关详细信息，请参阅 {@link org.springframework.web.util.pattern.PathPattern} javadoc。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	/**
	 * URL-处理程序映射
	 */
	private final Map<String, Object> urlMap = new LinkedHashMap<>();


	/**
	 * 使用默认设置创建 {@code SimpleUrlHandlerMapping}。
	 */
	public SimpleUrlHandlerMapping() {
	}

	/**
	 * 使用提供的 URL 映射创建 {@code SimpleUrlHandlerMapping}。
	 *
	 * @param urlMap URL路径作为键，处理程序bean（或处理程序bean名称）作为值的映射
	 * @see #setUrlMap(Map)
	 * @since 5.2
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
		setUrlMap(urlMap);
	}

	/**
	 * 使用提供的 URL 映射和顺序创建 {@code SimpleUrlHandlerMapping}。
	 *
	 * @param urlMap URL路径作为键，处理程序bean（或处理程序bean名称）作为值的映射
	 * @param order  {@code SimpleUrlHandlerMapping} 的顺序值
	 * @see #setUrlMap(Map)
	 * @see #setOrder(int)
	 * @since 5.2
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
		setUrlMap(urlMap);
		setOrder(order);
	}


	/**
	 * 将 URL 路径映射到处理程序 bean 名称。
	 * 这是配置此 HandlerMapping 的典型方式。
	 *
	 * <p>支持直接 URL 匹配和 Ant 风格的模式匹配。有关语法详细信息，
	 * 请参阅 {@link org.springframework.web.util.pattern.PathPattern} javadoc。
	 *
	 * @param mappings URL作为键，bean名称作为值的属性
	 * @see #setUrlMap
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * 设置 URL 路径作为键，处理程序 bean（或处理程序 bean 名称）作为值的映射。
	 * 便于使用 bean 引用进行填充。
	 *
	 * <p>支持直接 URL 匹配和 Ant 风格的模式匹配。有关语法详细信息，
	 * 请参阅 {@link org.springframework.web.util.pattern.PathPattern} javadoc。
	 *
	 * @param urlMap URL作为键，bean作为值的映射
	 * @see #setMappings
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * 允许使用 URL 路径映射的 Map 访问，可以添加或覆盖特定条目。
	 *
	 * <p>对于直接指定条目特别有用，例如通过 "urlMap[myKey]" 进行指定。
	 * 这对于添加或覆盖子 bean 定义中的条目特别有用。
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * 调用 {@link #registerHandlers} 方法以及父类的初始化。
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * 注册 URL 映射中指定的所有处理程序。
	 *
	 * @param urlMap URL路径作为键，处理程序 bean 或 bean 名称作为值的映射
	 * @throws BeansException        如果无法注册处理程序
	 * @throws IllegalStateException 如果已注册冲突的处理程序
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.trace("No patterns in " + formatMappingName());
		} else {
			for (Map.Entry<String, Object> entry : urlMap.entrySet()) {
				String url = entry.getKey();
				Object handler = entry.getValue();
				// 如果路径不以斜杠开头，则添加斜杠。
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// 从处理程序 bean 名称中删除空格。
				if (handler instanceof String) {
					handler = ((String) handler).trim();
				}
				registerHandler(url, handler);
			}
			logMappings();
		}
	}

	private void logMappings() {
		if (mappingsLogger.isDebugEnabled()) {
			mappingsLogger.debug(formatMappingName() + " " + getHandlerMap());
		} else if (logger.isDebugEnabled()) {
			logger.debug("Patterns " + getHandlerMap().keySet() + " in " + formatMappingName());
		}
	}

}
