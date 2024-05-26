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

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * {@link UriComponents}的不透明URI扩展。
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @see <a href="https://tools.ietf.org/html/rfc3986#section-1.2.3">层次化 vs 不透明URI</a>
 * @since 3.2
 */
@SuppressWarnings("serial")
final class OpaqueUriComponents extends UriComponents {
	/**
	 * 空的查询参数
	 */
	private static final MultiValueMap<String, String> QUERY_PARAMS_NONE = new LinkedMultiValueMap<>();

	/**
	 * URI的方案特定部分，方案后的部分（Scheme-Specific Part）
	 */
	@Nullable
	private final String ssp;


	OpaqueUriComponents(@Nullable String scheme, @Nullable String schemeSpecificPart, @Nullable String fragment) {
		super(scheme, fragment);
		this.ssp = schemeSpecificPart;
	}


	@Override
	@Nullable
	public String getSchemeSpecificPart() {
		return this.ssp;
	}

	@Override
	@Nullable
	public String getUserInfo() {
		return null;
	}

	@Override
	@Nullable
	public String getHost() {
		return null;
	}

	@Override
	public int getPort() {
		return -1;
	}

	@Override
	@Nullable
	public String getPath() {
		return null;
	}

	@Override
	public List<String> getPathSegments() {
		return Collections.emptyList();
	}

	@Override
	@Nullable
	public String getQuery() {
		return null;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return QUERY_PARAMS_NONE;
	}

	@Override
	public UriComponents encode(Charset charset) {
		return this;
	}

	@Override
	protected UriComponents expandInternal(UriTemplateVariables uriVariables) {
		// 扩展URI组件中的 方案部分
		String expandedScheme = expandUriComponent(getScheme(), uriVariables);
		// 扩展URI组件中的方案后部分
		String expandedSsp = expandUriComponent(getSchemeSpecificPart(), uriVariables);
		// 扩展URI组件中的片段部分
		String expandedFragment = expandUriComponent(getFragment(), uriVariables);
		// 创建一个不透明的URI组件并返回
		return new OpaqueUriComponents(expandedScheme, expandedSsp, expandedFragment);
	}

	@Override
	public UriComponents normalize() {
		return this;
	}

	@Override
	public String toUriString() {
		// 创建一个StringBuilder对象用于构建URI
		StringBuilder uriBuilder = new StringBuilder();

		if (getScheme() != null) {
			// 如果存在方案，则添加到URI中
			uriBuilder.append(getScheme());
			uriBuilder.append(':');
		}

		if (this.ssp != null) {
			// 如果存在方案后部分，则添加到URI中
			uriBuilder.append(this.ssp);
		}

		if (getFragment() != null) {
			// 如果存在片段，则添加到URI中
			uriBuilder.append('#');
			uriBuilder.append(getFragment());
		}

		// 返回构建好的URI字符串
		return uriBuilder.toString();
	}

	@Override
	public URI toUri() {
		try {
			return new URI(getScheme(), this.ssp, getFragment());
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		// 如果 URL 中的协议部分不为空
		if (getScheme() != null) {
			// 将协议部分添加到构建器中
			builder.scheme(getScheme());
		}
		// 如果 URL 中的协议特定部分不为空
		if (getSchemeSpecificPart() != null) {
			// 将协议特定部分添加到构建器中
			builder.schemeSpecificPart(getSchemeSpecificPart());
		}
		// 如果 URL 中的片段部分不为空
		if (getFragment() != null) {
			// 将片段部分添加到构建器中
			builder.fragment(getFragment());
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OpaqueUriComponents)) {
			return false;
		}
		OpaqueUriComponents otherComp = (OpaqueUriComponents) other;
		return (ObjectUtils.nullSafeEquals(getScheme(), otherComp.getScheme()) &&
				ObjectUtils.nullSafeEquals(this.ssp, otherComp.ssp) &&
				ObjectUtils.nullSafeEquals(getFragment(), otherComp.getFragment()));
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(getScheme());
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.ssp);
		result = 31 * result + ObjectUtils.nullSafeHashCode(getFragment());
		return result;
	}

}
