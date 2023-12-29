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

package org.springframework.web.reactive.result.view;

import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ViewResolver} 实现的基类，具有共享属性。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class ViewResolverSupport implements Ordered {

	/**
	 * 视图的默认 {@link MediaType content-type}。
	 */
	public static final MediaType DEFAULT_CONTENT_TYPE = MediaType.parseMediaType("text/html;charset=UTF-8");

	/**
	 * 媒体类型列表
	 */
	private List<MediaType> mediaTypes = new ArrayList<>(4);
	/**
	 * 默认字符集
	 */
	private Charset defaultCharset = StandardCharsets.UTF_8;
	private int order = Ordered.LOWEST_PRECEDENCE;

	public ViewResolverSupport() {
		this.mediaTypes.add(DEFAULT_CONTENT_TYPE);
	}

	/**
	 * 设置此视图支持的媒体类型。
	 * 默认为 "text/html;charset=UTF-8"。
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.mediaTypes.clear();
		this.mediaTypes.addAll(supportedMediaTypes);
	}

	/**
	 * 返回此视图支持的配置媒体类型。
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * 设置此视图的默认字符集，在 {@linkplain #setSupportedMediaTypes(List) content type} 不包含字符集时使用。
	 * 默认为 {@linkplain StandardCharsets#UTF_8 UTF 8}。
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "Default Charset must not be null");
		this.defaultCharset = defaultCharset;
	}

	/**
	 * 返回默认字符集，在 {@linkplain #setSupportedMediaTypes(List) content type} 不包含字符集时使用。
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}

	/**
	 * 为此 ViewResolver bean 指定顺序值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示无序。
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

}
