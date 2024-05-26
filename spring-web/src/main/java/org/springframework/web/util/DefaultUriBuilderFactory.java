/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.*;

/**
 * {@code UriBuilderFactory}依赖于{@link UriComponentsBuilder}来实际构建URI。
 *
 * <p>提供了创建具有共同基本URI、替代编码模式策略等的{@link UriBuilder}实例的选项。
 *
 * @author Rossen Stoyanchev
 * @see UriComponentsBuilder
 * @since 5.0
 */
public class DefaultUriBuilderFactory implements UriBuilderFactory {
	/**
	 * 基本URI组件构建器
	 */
	@Nullable
	private final UriComponentsBuilder baseUri;

	/**
	 * 编码模式
	 */
	private EncodingMode encodingMode = EncodingMode.TEMPLATE_AND_VALUES;

	/**
	 * 默认URI变量
	 */
	private final Map<String, Object> defaultUriVariables = new HashMap<>();

	/**
	 * 是否解析路径为路径段
	 */
	private boolean parsePath = true;


	/**
	 * 没有基本URI的默认构造函数。
	 * <p>必须在每个UriBuilder上指定目标地址。
	 */
	public DefaultUriBuilderFactory() {
		this.baseUri = null;
	}

	/**
	 * 带有基本URI的构造函数。
	 * <p>给定的URI模板通过{@link UriComponentsBuilder#fromUriString}解析，
	 * 然后通过{@link UriComponentsBuilder#uriComponents}应用为每个UriBuilder的基本URI，
	 * 除非UriBuilder本身是使用已经具有目标地址的URI模板创建的。
	 *
	 * @param baseUriTemplate 用作基本URL的URI模板
	 */
	public DefaultUriBuilderFactory(String baseUriTemplate) {
		this.baseUri = UriComponentsBuilder.fromUriString(baseUriTemplate);
	}

	/**
	 * 使用{@code UriComponentsBuilder}的{@link #DefaultUriBuilderFactory(String)}的变体。
	 */
	public DefaultUriBuilderFactory(UriComponentsBuilder baseUri) {
		this.baseUri = baseUri;
	}


	/**
	 * 设置要使用的{@link EncodingMode 编码模式}。
	 * <p>默认情况下，此值设置为{@link EncodingMode#TEMPLATE_AND_VALUES EncodingMode.TEMPLATE_AND_VALUES}。
	 * <p><strong>注意：</strong>在5.1之前，默认值为{@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}，
	 * 因此{@code WebClient}和{@code RestTemplate}已更改了其默认行为。
	 *
	 * @param encodingMode 要使用的编码模式
	 */
	public void setEncodingMode(EncodingMode encodingMode) {
		this.encodingMode = encodingMode;
	}

	/**
	 * 返回配置的编码模式。
	 */
	public EncodingMode getEncodingMode() {
		return this.encodingMode;
	}

	/**
	 * 当使用变量映射扩展URI模板时提供要使用的默认URI变量值。
	 *
	 * @param defaultUriVariables 默认的URI变量值
	 */
	public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
		// 清空默认URI变量的映射关系
		this.defaultUriVariables.clear();
		// 如果传入的默认URI变量不为空
		if (defaultUriVariables != null) {
			// 将传入的默认URI变量添加到当前默认URI变量的映射关系中
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * 返回配置的默认URI变量值。
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}

	/**
	 * 如果编码模式设置为{@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}，
	 * 则是否解析输入路径为路径段，这样可以确保路径中的URI变量根据路径段规则进行编码，例如'/'会被编码。
	 * <p>默认情况下，此值设置为{@code true}。
	 *
	 * @param parsePath 是否解析路径为路径段
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * 如果编码模式设置为{@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}，
	 * 则是否解析路径为路径段。
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}


	// URI模板处理器

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	// URI构建器工厂

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return new DefaultUriBuilder(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return new DefaultUriBuilder("");
	}


	/**
	 * 枚举类型，表示多种URI编码策略。以下是可用的策略：
	 * <ul>
	 * <li>{@link #TEMPLATE_AND_VALUES}
	 * <li>{@link #VALUES_ONLY}
	 * <li>{@link #URI_COMPONENT}
	 * <li>{@link #NONE}
	 * </ul>
	 *
	 * @see #setEncodingMode
	 */
	public enum EncodingMode {

		/**
		 * 首先对URI模板进行预编码，然后严格对URI变量进行编码，具体规则如下：
		 * <ul>
		 * <li>对于URI模板，仅替换非ASCII字符和非法字符（在给定的URI组件类型内）为转义的八位字节。
		 * <li>对于URI变量，做相同操作，并且还会替换具有保留含义的字符。
		 * </ul>
		 * <p>对于大多数情况而言，此模式最可能给出预期的结果，因为它将URI变量视为不透明数据进行完全编码，
		 * 而相比之下，{@link #URI_COMPONENT}仅在有意将URI变量与保留字符扩展时才有用。
		 *
		 * @see UriComponentsBuilder#encode()
		 * @since 5.0.8
		 */
		TEMPLATE_AND_VALUES,

		/**
		 * 不对URI模板进行编码，而是通过{@link UriUtils#encodeUriVariables}在将其扩展到模板之前严格对URI变量进行编码。
		 *
		 * @see UriUtils#encodeUriVariables(Object...)
		 * @see UriUtils#encodeUriVariables(Map)
		 */
		VALUES_ONLY,

		/**
		 * 首先扩展URI变量，然后对生成的URI组件值进行编码，仅替换非ASCII字符和非法字符（在给定的URI组件类型内），
		 * 但不替换具有保留含义的字符。
		 *
		 * @see UriComponents#encode()
		 */
		URI_COMPONENT,

		/**
		 * 不应用编码。
		 */
		NONE
	}


	/**
	 * {@link DefaultUriBuilderFactory}特定的UriBuilder实现。
	 */
	private class DefaultUriBuilder implements UriBuilder {
		/**
		 * URI组件构建器
		 */
		private final UriComponentsBuilder uriComponentsBuilder;

		public DefaultUriBuilder(String uriTemplate) {
			this.uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		}

		private UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
			// 创建一个 URI组件构建器 对象
			UriComponentsBuilder result;
			// 如果 URI 模板为空
			if (!StringUtils.hasLength(uriTemplate)) {
				// 如果存在基础 URI，则克隆基础 URI 的构建器；否则创建一个新的构建器
				result = (baseUri != null ? baseUri.cloneBuilder() : UriComponentsBuilder.newInstance());
			} else if (baseUri != null) {
				// 如果 URI 模板不为空且存在基础 URI
				// 使用 URI 模板创建一个 URI组件构建器 对象
				UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
				UriComponents uri = builder.build();
				// 如果 URI 中的主机名为空，则克隆基础 URI 的构建器，并设置 URI 组件
				result = (uri.getHost() == null ? baseUri.cloneBuilder().uriComponents(uri) : builder);
			} else {
				// 如果 URI 模板不为空且不存在基础 URI
				// 使用 URI 模板创建一个 URI组件构建器 对象
				result = UriComponentsBuilder.fromUriString(uriTemplate);
			}
			// 如果编码模式为 TEMPLATE_AND_VALUES，则对 URI 进行编码
			if (encodingMode.equals(EncodingMode.TEMPLATE_AND_VALUES)) {
				result.encode();
			}
			// 解析路径（如果需要）
			parsePathIfNecessary(result);
			// 返回构建的 URI组件构建器 对象
			return result;
		}

		private void parsePathIfNecessary(UriComponentsBuilder result) {
			// 如果需要解析路径，并且编码模式为 URI组件
			if (parsePath && encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				// 构建 URI组件 对象
				UriComponents uric = result.build();
				// 获取 URI 的路径
				String path = uric.getPath();
				// 清除当前路径
				result.replacePath(null);
				// 将路径中的每个段添加到新的 URI组件构建器 中
				for (String segment : uric.getPathSegments()) {
					result.pathSegment(segment);
				}
				// 如果原始路径不为空且以斜杠结尾，则在新的路径中添加斜杠
				if (path != null && path.endsWith("/")) {
					result.path("/");
				}
			}
		}


		@Override
		public DefaultUriBuilder scheme(@Nullable String scheme) {
			this.uriComponentsBuilder.scheme(scheme);
			return this;
		}

		@Override
		public DefaultUriBuilder userInfo(@Nullable String userInfo) {
			this.uriComponentsBuilder.userInfo(userInfo);
			return this;
		}

		@Override
		public DefaultUriBuilder host(@Nullable String host) {
			this.uriComponentsBuilder.host(host);
			return this;
		}

		@Override
		public DefaultUriBuilder port(int port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder port(@Nullable String port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder path(String path) {
			this.uriComponentsBuilder.path(path);
			return this;
		}

		@Override
		public DefaultUriBuilder replacePath(@Nullable String path) {
			this.uriComponentsBuilder.replacePath(path);
			return this;
		}

		@Override
		public DefaultUriBuilder pathSegment(String... pathSegments) {
			this.uriComponentsBuilder.pathSegment(pathSegments);
			return this;
		}

		@Override
		public DefaultUriBuilder query(String query) {
			this.uriComponentsBuilder.query(query);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQuery(@Nullable String query) {
			this.uriComponentsBuilder.replaceQuery(query);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParam(String name, Object... values) {
			this.uriComponentsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParam(String name, @Nullable Collection<?> values) {
			this.uriComponentsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParamIfPresent(String name, Optional<?> value) {
			this.uriComponentsBuilder.queryParamIfPresent(name, value);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParams(MultiValueMap<String, String> params) {
			this.uriComponentsBuilder.queryParams(params);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParam(String name, Object... values) {
			this.uriComponentsBuilder.replaceQueryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParam(String name, @Nullable Collection<?> values) {
			this.uriComponentsBuilder.replaceQueryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParams(MultiValueMap<String, String> params) {
			this.uriComponentsBuilder.replaceQueryParams(params);
			return this;
		}

		@Override
		public DefaultUriBuilder fragment(@Nullable String fragment) {
			this.uriComponentsBuilder.fragment(fragment);
			return this;
		}

		@Override
		public URI build(Map<String, ?> uriVars) {
			// 如果默认的 URI 变量不为空
			if (!defaultUriVariables.isEmpty()) {
				// 创建一个新的映射，将默认 URI 变量和当前 URI 变量合并
				Map<String, Object> map = new HashMap<>();
				map.putAll(defaultUriVariables);
				map.putAll(uriVars);
				// 将当前 URI 变量更新为合并后的映射
				uriVars = map;
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				// 如果编码模式为 仅对值编码，则对 URI 变量进行编码
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			// 使用 URI 组件构建器构建 URI组件，并使用 URI 变量扩展它
			UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
			// 创建 URI
			return createUri(uric);
		}

		@Override
		public URI build(Object... uriVars) {
			// 如果 URI 变量为空且默认 URI 变量不为空
			if (ObjectUtils.isEmpty(uriVars) && !defaultUriVariables.isEmpty()) {
				// 使用空映射构建 URI
				return build(Collections.emptyMap());
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				// 如果编码模式为 仅对值编码，则对 URI 变量进行编码
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			// 使用 URI 组件构建器构建 UriComponents，并使用 URI 变量扩展它
			UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
			// 创建 URI
			return createUri(uric);
		}

		private URI createUri(UriComponents uric) {
			if (encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				// 如果编码模式为 URI组件模式，则对 URI 进行编码
				uric = uric.encode();
			}
			// 创建 URI 对象
			return URI.create(uric.toString());
		}
	}

}
