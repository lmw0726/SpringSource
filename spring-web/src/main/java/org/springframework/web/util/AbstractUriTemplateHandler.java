/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link UriTemplateHandler}实现的抽象基类。
 *
 * <p>支持{@link #setBaseUrl}和{@link #setDefaultUriVariables}属性，这些属性应该与子类中使用的URI模板扩展和编码机制相关。
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @deprecated 自5.0起，改用{@link DefaultUriBuilderFactory}
 */
@Deprecated
public abstract class AbstractUriTemplateHandler implements UriTemplateHandler {
	/**
	 * 基本URL
	 */
	@Nullable
	private String baseUrl;

	/**
	 * 默认URI变量
	 */
	private final Map<String, Object> defaultUriVariables = new HashMap<>();


	/**
	 * 配置要在URI模板之前添加的基本URL。基本URL必须具有方案和主机，但可以选择包含端口和路径。基本URL必须完全扩展和编码，可以通过{@link UriComponentsBuilder}完成。
	 *
	 * @param baseUrl 基本URL。
	 */
	public void setBaseUrl(@Nullable String baseUrl) {
		if (baseUrl != null) {
			// 如果基本URL不为空，则构建URI组件
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUrl).build();
			Assert.hasText(uriComponents.getScheme(), "'baseUrl' must have a scheme");
			Assert.hasText(uriComponents.getHost(), "'baseUrl' must have a host");
			Assert.isNull(uriComponents.getQuery(), "'baseUrl' cannot have a query");
			Assert.isNull(uriComponents.getFragment(), "'baseUrl' cannot have a fragment");
		}
		// 设置基本URL
		this.baseUrl = baseUrl;
	}

	/**
	 * 返回配置的基本URL。
	 */
	@Nullable
	public String getBaseUrl() {
		return this.baseUrl;
	}

	/**
	 * 配置要与每个扩展的URI模板一起使用的默认URI变量值。这些默认值仅在使用Map扩展时应用，并且在使用数组扩展时不应用，默认情况下，传递给{@link #expand(String, Map)}的Map可以覆盖默认值。
	 *
	 * @param defaultUriVariables 默认的URI变量值
	 * @since 4.3
	 */
	public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
		// 清空默认URI变量的映射关系
		this.defaultUriVariables.clear();
		// 如果传入的默认URI变量不为空
		if (defaultUriVariables != null) {
			// 将 传入的默认URI变量 添加到 当前默认URI变量的映射 关系中
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * 返回配置的默认URI变量的只读副本。
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}


	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		// 如果默认URI变量映射关系不为空
		if (!getDefaultUriVariables().isEmpty()) {
			// 创建一个新的映射关系map
			Map<String, Object> map = new HashMap<>();
			// 将默认URI变量映射关系添加到新的map中
			map.putAll(getDefaultUriVariables());
			// 将 传入的URI变量映射关系 添加到新的map中，覆盖 默认的URI变量映射 关系
			map.putAll(uriVariables);
			// 将 新的URI变量映射关系 赋值给 传入的URI变量映射关系
			uriVariables = map;
		}
		// 使用扩展后的URI模板和变量映射创建URI
		URI url = expandInternal(uriTemplate, uriVariables);
		// 插入基础URL到URI中并返回
		return insertBaseUrl(url);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		// 使用扩展后的URI模板和变量映射创建URI
		URI url = expandInternal(uriTemplate, uriVariables);
		// 插入基础URL到URI中并返回
		return insertBaseUrl(url);
	}


	/**
	 * 实际扩展和编码URI模板。
	 */
	protected abstract URI expandInternal(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * 实际扩展和编码URI模板。
	 */
	protected abstract URI expandInternal(String uriTemplate, Object... uriVariables);


	/**
	 * 插入基本URL（如果已配置），除非给定的URL已经具有主机。
	 */
	private URI insertBaseUrl(URI url) {
		try {
			// 获取基础URL
			String baseUrl = getBaseUrl();
			// 如果基础URL不为空，且URI的主机部分为空
			if (baseUrl != null && url.getHost() == null) {
				// 将 基础URL 和 URI的字符串形式 连接成新的URI
				url = new URI(baseUrl + url.toString());
			}
			// 返回处理后的URI
			return url;
		} catch (URISyntaxException ex) {
			// 如果发生URI语法异常，抛出IllegalArgumentException
			throw new IllegalArgumentException("Invalid URL after inserting base URL: " + url, ex);
		}
	}

}
