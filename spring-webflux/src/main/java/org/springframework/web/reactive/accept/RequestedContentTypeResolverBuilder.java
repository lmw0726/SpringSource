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

package org.springframework.web.reactive.accept;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 用于构建复合 {@link RequestedContentTypeResolver} 的构建器，该解析器委托给其他实现不同策略以确定请求内容类型的解析器，例如 Accept 头、查询参数等。
 * <p>
 * 使用构建器方法按照所需顺序添加解析器。对于给定的请求，首个返回的不为空且不仅包含 {@link MediaType#ALL} 的解析器将被使用。
 * <p>
 * 默认情况下，如果未显式配置解析器，则构建器将添加 {@link HeaderContentTypeResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestedContentTypeResolverBuilder {

	/**
	 * 候选的请求内容类型解析器
	 */
	private final List<Supplier<RequestedContentTypeResolver>> candidates = new ArrayList<>();


	/**
	 * 添加一个解析器，从查询参数中获取请求的内容类型。默认情况下查询参数名为 {@code "format"}。
	 */
	public ParameterResolverConfigurer parameterResolver() {
		ParameterResolverConfigurer parameterBuilder = new ParameterResolverConfigurer();
		this.candidates.add(parameterBuilder::createResolver);
		return parameterBuilder;
	}

	/**
	 * 添加从 {@literal "Accept"} 头中获取请求的内容类型的解析器。
	 */
	public void headerResolver() {
		this.candidates.add(HeaderContentTypeResolver::new);
	}

	/**
	 * 添加返回固定一组媒体类型的解析器。
	 *
	 * @param mediaTypes 要使用的媒体类型
	 */
	public void fixedResolver(MediaType... mediaTypes) {
		this.candidates.add(() -> new FixedContentTypeResolver(Arrays.asList(mediaTypes)));
	}

	/**
	 * 添加自定义解析器。
	 *
	 * @param resolver 要添加的解析器
	 */
	public void resolver(RequestedContentTypeResolver resolver) {
		this.candidates.add(() -> resolver);
	}

	/**
	 * 构建一个 {@link RequestedContentTypeResolver}，该解析器委托给通过此构建器配置的解析器列表。
	 */
	public RequestedContentTypeResolver build() {
		// 根据候选解析器列表初始化解析器列表
		List<RequestedContentTypeResolver> resolvers = (!this.candidates.isEmpty() ?
				// 如果候选列表不为空，使用候选解析器生成解析器列表
				this.candidates.stream().map(Supplier::get).collect(Collectors.toList()) :
				// 否则使用默认的 HeaderContentTypeResolver 作为单一解析器
				Collections.singletonList(new HeaderContentTypeResolver()));

		// 返回一个处理程序，根据解析器确定请求的媒体类型
		return exchange -> {
			// 遍历所有解析器
			for (RequestedContentTypeResolver resolver : resolvers) {
				// 通过解析器解析媒体类型
				List<MediaType> mediaTypes = resolver.resolveMediaTypes(exchange);

				// 如果解析出的媒体类型为全部媒体类型列表，继续下一个解析器
				if (mediaTypes.equals(RequestedContentTypeResolver.MEDIA_TYPE_ALL_LIST)) {
					continue;
				}

				// 返回解析出的媒体类型
				return mediaTypes;
			}
			// 如果所有解析器均未找到匹配的媒体类型，则返回全部媒体类型列表
			return RequestedContentTypeResolver.MEDIA_TYPE_ALL_LIST;
		};
	}


	/**
	 * 用于创建和配置 {@link ParameterContentTypeResolver} 的辅助类。
	 */
	public static class ParameterResolverConfigurer {
		/**
		 * 参数名和媒体类型映射
		 */
		private final Map<String, MediaType> mediaTypes = new HashMap<>();

		/**
		 * 参数名称
		 */
		@Nullable
		private String parameterName;

		/**
		 * 配置查询参数值中提取的查找键与相应 {@code MediaType} 的映射关系。
		 *
		 * @param key       查找键
		 * @param mediaType 该键对应的 MediaType
		 */
		public ParameterResolverConfigurer mediaType(String key, MediaType mediaType) {
			this.mediaTypes.put(key, mediaType);
			return this;
		}

		/**
		 * {@link #mediaType(String, MediaType)} 的基于 Map 的变体。
		 *
		 * @param mediaTypes 要复制的映射关系
		 */
		public ParameterResolverConfigurer mediaType(Map<String, MediaType> mediaTypes) {
			this.mediaTypes.putAll(mediaTypes);
			return this;
		}

		/**
		 * 设置用于确定请求的媒体类型的参数名称。
		 * <p>默认情况下，此值设置为 {@literal "format"}。
		 */
		public ParameterResolverConfigurer parameterName(String parameterName) {
			this.parameterName = parameterName;
			return this;
		}

		/**
		 * 创建解析器的私有工厂方法。
		 */
		private RequestedContentTypeResolver createResolver() {
			ParameterContentTypeResolver resolver = new ParameterContentTypeResolver(this.mediaTypes);
			if (this.parameterName != null) {
				resolver.setParameterName(this.parameterName);
			}
			return resolver;
		}
	}

}
