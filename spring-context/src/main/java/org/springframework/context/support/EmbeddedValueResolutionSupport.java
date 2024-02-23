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

package org.springframework.context.support;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * 带有嵌入式值解析需求的组件的便捷基类
 * （即 {@link org.springframework.context.EmbeddedValueResolverAware} 的消费者）。
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public class EmbeddedValueResolutionSupport implements EmbeddedValueResolverAware {
	/**
	 * 字符串值解析器
	 */
	@Nullable
	private StringValueResolver embeddedValueResolver;

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	/**
	 * 通过此实例的 {@link StringValueResolver} 解析给定的嵌入值。
	 *
	 * @param value 要解析的值
	 * @return 已解析的值；如果没有可用的解析器，则始终为原始值
	 * @see #setEmbeddedValueResolver
	 */
	@Nullable
	protected String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}
}

