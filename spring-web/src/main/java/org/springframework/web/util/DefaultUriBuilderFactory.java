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
 * {@code UriBuilderFactory} that relies on {@link UriComponentsBuilder} for
 * the actual building of the URI.
 *
 * <p>Provides options to create {@link UriBuilder} instances with a common
 * base URI, alternative encoding mode strategies, among others.
 *
 * @author Rossen Stoyanchev
 * @see UriComponentsBuilder
 * @since 5.0
 */
public class DefaultUriBuilderFactory implements UriBuilderFactory {

	@Nullable
	private final UriComponentsBuilder baseUri;

	private EncodingMode encodingMode = EncodingMode.TEMPLATE_AND_VALUES;

	private final Map<String, Object> defaultUriVariables = new HashMap<>();

	private boolean parsePath = true;


	/**
	 * Default constructor without a base URI.
	 * <p>The target address must be specified on each UriBuilder.
	 */
	public DefaultUriBuilderFactory() {
		this.baseUri = null;
	}

	/**
	 * Constructor with a base URI.
	 * <p>The given URI template is parsed via
	 * {@link UriComponentsBuilder#fromUriString} and then applied as a base URI
	 * to every UriBuilder via {@link UriComponentsBuilder#uriComponents} unless
	 * the UriBuilder itself was created with a URI template that already has a
	 * target address.
	 *
	 * @param baseUriTemplate the URI template to use a base URL
	 */
	public DefaultUriBuilderFactory(String baseUriTemplate) {
		this.baseUri = UriComponentsBuilder.fromUriString(baseUriTemplate);
	}

	/**
	 * Variant of {@link #DefaultUriBuilderFactory(String)} with a
	 * {@code UriComponentsBuilder}.
	 */
	public DefaultUriBuilderFactory(UriComponentsBuilder baseUri) {
		this.baseUri = baseUri;
	}


	/**
	 * Set the {@link EncodingMode encoding mode} to use.
	 * <p>By default this is set to {@link EncodingMode#TEMPLATE_AND_VALUES
	 * EncodingMode.TEMPLATE_AND_VALUES}.
	 * <p><strong>Note:</strong> Prior to 5.1 the default was
	 * {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}
	 * therefore the {@code WebClient} {@code RestTemplate} have switched their
	 * default behavior.
	 *
	 * @param encodingMode the encoding mode to use
	 */
	public void setEncodingMode(EncodingMode encodingMode) {
		this.encodingMode = encodingMode;
	}

	/**
	 * Return the configured encoding mode.
	 */
	public EncodingMode getEncodingMode() {
		return this.encodingMode;
	}

	/**
	 * Provide default URI variable values to use when expanding URI templates
	 * with a Map of variables.
	 *
	 * @param defaultUriVariables default URI variable values
	 */
	public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables.clear();
		if (defaultUriVariables != null) {
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * Return the configured default URI variable values.
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}

	/**
	 * Whether to parse the input path into path segments if the encoding mode
	 * is set to {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT},
	 * which ensures that URI variables in the path are encoded according to
	 * path segment rules and for example a '/' is encoded.
	 * <p>By default this is set to {@code true}.
	 *
	 * @param parsePath whether to parse the path into path segments
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * Whether to parse the path into path segments if the encoding mode is set
	 * to {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}.
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}


	// UriTemplateHandler

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	// UriBuilderFactory

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return new DefaultUriBuilder(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return new DefaultUriBuilder("");
	}


	/**
	 * Enum to represent multiple URI encoding strategies. The following are
	 * available:
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
		 * Pre-encode the URI template first, then strictly encode URI variables
		 * when expanded, with the following rules:
		 * <ul>
		 * <li>For the URI template replace <em>only</em> non-ASCII and illegal
		 * (within a given URI component type) characters with escaped octets.
		 * <li>For URI variables do the same and also replace characters with
		 * reserved meaning.
		 * </ul>
		 * <p>For most cases, this mode is most likely to give the expected
		 * result because in treats URI variables as opaque data to be fully
		 * encoded, while {@link #URI_COMPONENT} by comparison is useful only
		 * if intentionally expanding URI variables with reserved characters.
		 *
		 * @see UriComponentsBuilder#encode()
		 * @since 5.0.8
		 */
		TEMPLATE_AND_VALUES,

		/**
		 * Does not encode the URI template and instead applies strict encoding
		 * to URI variables via {@link UriUtils#encodeUriVariables} prior to
		 * expanding them into the template.
		 *
		 * @see UriUtils#encodeUriVariables(Object...)
		 * @see UriUtils#encodeUriVariables(Map)
		 */
		VALUES_ONLY,

		/**
		 * Expand URI variables first, and then encode the resulting URI
		 * component values, replacing <em>only</em> non-ASCII and illegal
		 * (within a given URI component type) characters, but not characters
		 * with reserved meaning.
		 *
		 * @see UriComponents#encode()
		 */
		URI_COMPONENT,

		/**
		 * No encoding should be applied.
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
