/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;
import java.util.function.Supplier;

/**
 * {@link org.springframework.web.reactive.HandlerResultHandler HandlerResultHandler} 的基类，
 * 支持内容协商和访问 {@code ReactiveAdapter} 注册表。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class HandlerResultHandlerSupport implements Ordered {

	/**
	 * 包含所有应用程序媒体类型的静态不可变列表
	 */
	private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES =
			Arrays.asList(MediaType.ALL, new MediaType("application"));

	/**
	 * 用于记录日志的最终日志对象
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 请求内容类型解析器的最终不可变实例
	 */
	private final RequestedContentTypeResolver contentTypeResolver;

	/**
	 * 反应式适配器注册表的最终不可变实例
	 */
	private final ReactiveAdapterRegistry adapterRegistry;

	/**
	 * 用于结果处理程序顺序的初始值，默认为最低优先级
	 */
	private int order = LOWEST_PRECEDENCE;


	/**
	 * 构造函数，需要 {@code RequestedContentTypeResolver} 和 {@code ReactiveAdapterRegistry}。
	 *
	 * @param contentTypeResolver 请求的内容类型解析器
	 * @param adapterRegistry     反应式适配器注册表
	 */
	protected HandlerResultHandlerSupport(RequestedContentTypeResolver contentTypeResolver,
										  ReactiveAdapterRegistry adapterRegistry) {

		Assert.notNull(contentTypeResolver, "RequestedContentTypeResolver is required");
		Assert.notNull(adapterRegistry, "ReactiveAdapterRegistry is required");
		this.contentTypeResolver = contentTypeResolver;
		this.adapterRegistry = adapterRegistry;
	}


	/**
	 * 返回配置的 {@link ReactiveAdapterRegistry}。
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}

	/**
	 * 返回配置的 {@link RequestedContentTypeResolver}。
	 */
	public RequestedContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * 设置此结果处理程序相对于其他处理程序的顺序。
	 * <p>默认设置为 {@link Ordered#LOWEST_PRECEDENCE}，但请参阅子类的 Javadoc，可能会更改此默认值。
	 *
	 * @param order 顺序
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 获取顶层返回值类型的 {@code ReactiveAdapter}。
	 *
	 * @param result 处理结果
	 * @return 匹配的适配器，如果没有则返回 {@code null}
	 */
	@Nullable
	protected ReactiveAdapter getAdapter(HandlerResult result) {
		return getAdapterRegistry().getAdapter(result.getReturnType().resolve(), result.getReturnValue());
	}

	/**
	 * 通过内容协商算法为当前请求选择最佳媒体类型。
	 *
	 * @param exchange                当前请求
	 * @param producibleTypesSupplier 当前请求可以生成的媒体类型的供应商
	 * @return 选择的媒体类型，如果没有则返回 {@code null}
	 */
	@Nullable
	protected MediaType selectMediaType(
			ServerWebExchange exchange, Supplier<List<MediaType>> producibleTypesSupplier) {
		// 获取响应的内容类型
		MediaType contentType = exchange.getResponse().getHeaders().getContentType();

		// 检查响应的内容类型是否存在且为具体类型
		if (contentType != null && contentType.isConcrete()) {
			if (logger.isDebugEnabled()) {
				// 记录调试信息，指示在响应中找到了'Content-Type: {contentType}'。
				logger.debug(exchange.getLogPrefix() + "Found 'Content-Type:" + contentType + "' in response");
			}
			return contentType;
		}

		// 获取可接受的媒体类型列表和可生成的媒体类型列表
		List<MediaType> acceptableTypes = getAcceptableTypes(exchange);
		List<MediaType> producibleTypes = getProducibleTypes(exchange, producibleTypesSupplier);

		// 通过内容协商算法找到兼容的媒体类型
		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType acceptable : acceptableTypes) {
			for (MediaType producible : producibleTypes) {
				// 如果可接受的媒体类型与可生成的媒体类型兼容，则选择更具体的媒体类型并添加到集合中
				if (acceptable.isCompatibleWith(producible)) {
					compatibleMediaTypes.add(selectMoreSpecificMediaType(acceptable, producible));
				}
			}
		}

		// 将兼容的媒体类型按特定性和质量进行排序
		List<MediaType> result = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(result);

		// 选择最终的媒体类型
		MediaType selected = null;
		// 遍历经过排序的媒体类型列表
		for (MediaType mediaType : result) {
			// 如果媒体类型为具体类型，则选择该媒体类型并跳出循环
			if (mediaType.isConcrete()) {
				selected = mediaType;
				break;
			} else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
				// 如果媒体类型在所有应用程序媒体类型列表中，则选择应用程序八位字节流作为媒体类型，并跳出循环
				selected = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}


		// 移除选定媒体类型的质量值，并记录相应的调试信息
		if (selected != null) {
			selected = selected.removeQualityValue();
			if (logger.isDebugEnabled()) {
				logger.debug(exchange.getLogPrefix() + "Using '" + selected + "' given " + acceptableTypes +
						" and supported " + producibleTypes);
			}
		} else if (logger.isDebugEnabled()) {
			// 记录调试信息，指示没有匹配的媒体类型
			logger.debug(exchange.getLogPrefix() +
					"No match for " + acceptableTypes + ", supported: " + producibleTypes);
		}

		// 返回最终选择的媒体类型
		return selected;
	}

	/**
	 * 获取可接受的媒体类型列表。
	 *
	 * @param exchange 当前的服务器Web交换对象
	 * @return 可接受的媒体类型列表
	 */
	private List<MediaType> getAcceptableTypes(ServerWebExchange exchange) {
		// 使用内容类型解析器解析媒体类型
		return getContentTypeResolver().resolveMediaTypes(exchange);
	}


	/**
	 * 获取可生成的媒体类型列表。
	 *
	 * @param exchange                    当前的服务器Web交换对象
	 * @param producibleTypesSupplier     可生成媒体类型的供应商
	 * @return 可生成的媒体类型列表
	 */
	private List<MediaType> getProducibleTypes(
			ServerWebExchange exchange, Supplier<List<MediaType>> producibleTypesSupplier) {
		// 获取可生成媒体类型的属性
		Set<MediaType> mediaTypes = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		// 如果可生成媒体类型存在，则将其转换为列表返回；否则，通过供应商获取可生成的媒体类型并返回
		return (mediaTypes != null ? new ArrayList<>(mediaTypes) : producibleTypesSupplier.get());
	}


	/**
	 * 选择更具体的媒体类型。
	 *
	 * @param acceptable 可接受的媒体类型
	 * @param producible 可生成的媒体类型
	 * @return 更具体的媒体类型
	 */
	private MediaType selectMoreSpecificMediaType(MediaType acceptable, MediaType producible) {
		// 将可生成的媒体类型的质量值设为与可接受的媒体类型相同，并返回
		producible = producible.copyQualityValue(acceptable);
		// 使用特定性比较器比较可接受的媒体类型和可生成的媒体类型，选择特定性更高的媒体类型返回
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptable, producible) <= 0 ? acceptable : producible);
	}

}
