/*
 * Copyright 2002-2018 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * 基于{@link UriComponentsBuilder}的{@link UriTemplateHandler}的默认实现，用于扩展和编码变量。
 *
 * <p>还有一些属性可用于自定义如何执行URI模板处理，包括用作所有URI模板前缀的{@link #setBaseUrl baseUrl}和一些编码相关的选项，
 * 包括{@link #setParsePath parsePath}和{@link #setStrictEncoding strictEncoding}。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @deprecated 自5.0起，改用{@link DefaultUriBuilderFactory}。
 * <p><strong>注意：</strong>{@link DefaultUriBuilderFactory}对于{@link #setParsePath(boolean) parsePath}属性的默认值不同（从false到true）。
 */
@Deprecated
public class DefaultUriTemplateHandler extends AbstractUriTemplateHandler {
	/**
	 * 是否解析路径为路径段
	 */
	private boolean parsePath;

	/**
	 * 是否执行严格编码
	 */
	private boolean strictEncoding;


	/**
	 * 是否解析URI模板字符串的路径为路径段。
	 * <p>如果设置为{@code true}，则URI模板路径将立即解析为路径段，然后对其中扩展的任何URI变量将受到路径段编码规则的影响。
	 * 实际上，路径中的URI变量的任何“/”字符都会进行百分比编码。
	 * <p>默认情况下，这设置为{@code false}，在这种情况下，路径保留为完整路径，并且扩展的URI变量将保留“/”字符。
	 *
	 * @param parsePath 是否解析路径为路径段
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * 处理程序是否配置为解析路径为路径段。
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}

	/**
	 * 是否将超出<a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986第2节</a>中定义的非保留字符集的字符进行编码。
	 * 这样可以确保URI变量值不包含任何具有保留目的的字符。
	 * <p>默认情况下，这设置为{@code false}，在这种情况下，仅对给定URI组件不合法的字符进行编码。
	 * 例如，当将URI变量扩展到路径段中时，“/”字符是不合法的并进行编码。
	 * 但是，“;”字符是合法的且未进行编码，即使它具有保留目的。
	 * <p><strong>注意：</strong>此属性取代了还设置{@link #setParsePath parsePath}属性的需要。
	 *
	 * @param strictEncoding 是否执行严格编码
	 * @since 4.3
	 */
	public void setStrictEncoding(boolean strictEncoding) {
		this.strictEncoding = strictEncoding;
	}

	/**
	 * 是否严格编码超出非保留字符集的字符。
	 */
	public boolean isStrictEncoding() {
		return this.strictEncoding;
	}


	@Override
	protected URI expandInternal(String uriTemplate, Map<String, ?> uriVariables) {
		// 根据URI模板，初始化URI组件构建器
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		// 扩展并编码URI组件
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		// 创建URI
		return createUri(uriComponents);
	}

	@Override
	protected URI expandInternal(String uriTemplate, Object... uriVariables) {
		// 根据URI模板，初始化URI组件构建器
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		// 扩展并编码URI组件
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		// 使用扩展后的URI组件创建URI
		return createUri(uriComponents);
	}

	/**
	 * 从URI模板字符串创建一个{@code UriComponentsBuilder}。
	 * 此实现还根据是否启用{@link #setParsePath parsePath}来将路径拆分为路径段。
	 */
	protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
		// 从URI模板创建URI组件构建器
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
		// 如果应该解析路径且不是严格编码
		if (shouldParsePath() && !isStrictEncoding()) {
			// 获取路径的各个段落
			List<String> pathSegments = builder.build().getPathSegments();
			// 替换路径为空
			builder.replacePath(null);
			// 逐个添加路径段落
			for (String pathSegment : pathSegments) {
				builder.pathSegment(pathSegment);
			}
		}
		// 返回构建好的URI组件构建器
		return builder;
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		// 如果不是严格编码
		if (!isStrictEncoding()) {
			// 构建并扩展URI，然后对其进行编码
			return builder.buildAndExpand(uriVariables).encode();
		} else {
			// 对URI变量进行编码
			Map<String, ?> encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
			// 构建并扩展URI
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Object[] uriVariables) {
		// 如果不是严格编码
		if (!isStrictEncoding()) {
			// 构建并扩展URI，然后对其进行编码
			return builder.buildAndExpand(uriVariables).encode();
		} else {
			// 对URI变量进行编码
			Object[] encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
			// 构建并扩展URI
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	private URI createUri(UriComponents uriComponents) {
		try {
			// 避免进一步编码（在strictEncoding=true的情况下）
			return new URI(uriComponents.toUriString());
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

}
