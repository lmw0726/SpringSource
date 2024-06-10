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

package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.*;
import java.util.function.Function;

/**
 * 用于确定请求的 {@linkplain MediaType 媒体类型} 的中央类。
 * 这是通过委托给一系列配置的 {@code ContentNegotiationStrategy} 实例来完成的。
 *
 * <p>还提供了查找媒体类型文件扩展名的方法。
 * 这是通过委托给配置的 {@code MediaTypeFileExtensionResolver} 实例列表来完成的。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {
	/**
	 * 内容协商策略列表
	 */
	private final List<ContentNegotiationStrategy> strategies = new ArrayList<>();

	/**
	 * 媒体类型文件扩展名解析器列表
	 */
	private final Set<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<>();


	/**
	 * 使用给定的 {@code ContentNegotiationStrategy} 策略列表创建一个实例，
	 * 每个策略也可以是 {@code MediaTypeFileExtensionResolver} 的实例。
	 *
	 * @param strategies 要使用的策略
	 */
	public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
		this(Arrays.asList(strategies));
	}

	/**
	 * {@link #ContentNegotiationManager(ContentNegotiationStrategy...)} 的基于集合的替代方法。
	 *
	 * @param strategies 要使用的策略
	 * @since 3.2.2
	 */
	public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
		Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
		// 将给定的策略列表添加到 内容协商策略列表 中
		this.strategies.addAll(strategies);

		// 遍历 内容协商策略列表 中的每个策略
		for (ContentNegotiationStrategy strategy : this.strategies) {
			// 如果策略是 媒体类型文件扩展名解析器 的实例
			if (strategy instanceof MediaTypeFileExtensionResolver) {
				// 将该策略添加到解析器列表中
				this.resolvers.add((MediaTypeFileExtensionResolver) strategy);
			}
		}
	}

	/**
	 * 使用 {@link HeaderContentNegotiationStrategy} 创建一个默认实例。
	 */
	public ContentNegotiationManager() {
		this(new HeaderContentNegotiationStrategy());
	}


	/**
	 * 返回配置的内容协商策略。
	 *
	 * @since 3.2.16
	 */
	public List<ContentNegotiationStrategy> getStrategies() {
		return this.strategies;
	}

	/**
	 * 查找给定类型的 {@code ContentNegotiationStrategy}。
	 *
	 * @param strategyType 策略类型
	 * @return 第一个匹配的策略，如果没有则返回 {@code null}
	 * @since 4.3
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T extends ContentNegotiationStrategy> T getStrategy(Class<T> strategyType) {
		// 遍历策略列表中的每个策略
		for (ContentNegotiationStrategy strategy : getStrategies()) {
			// 如果策略是指定类型的实例
			if (strategyType.isInstance(strategy)) {
				// 将该策略转换为指定类型并返回
				return (T) strategy;
			}
		}

		// 如果未找到符合条件的策略，则返回 null
		return null;
	}

	/**
	 * 注册更多的 {@code MediaTypeFileExtensionResolver} 实例，以补充在构造函数中检测到的实例。
	 *
	 * @param resolvers 要添加的解析器
	 */
	public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
		Collections.addAll(this.resolvers, resolvers);
	}

	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
		// 遍历策略列表中的每个策略
		for (ContentNegotiationStrategy strategy : this.strategies) {
			// 解析当前策略的媒体类型列表
			List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);

			// 如果解析出的媒体类型列表与 媒体类型全部列表 相等，则继续下一个策略
			if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
				continue;
			}

			// 如果解析出的媒体类型列表不等于 媒体类型全部列表，则返回该列表
			return mediaTypes;
		}

		// 否则返回 媒体类型全部列表
		return MEDIA_TYPE_ALL_LIST;
	}

	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		return doResolveExtensions(resolver -> resolver.resolveFileExtensions(mediaType));
	}

	/**
	 * {@inheritDoc}
	 * <p>在启动时，此方法返回显式注册到 {@link PathExtensionContentNegotiationStrategy} 或
	 * {@link ParameterContentNegotiationStrategy} 中的扩展名。在运行时，
	 * 如果存在 "路径扩展名" 策略，并且其
	 * {@link PathExtensionContentNegotiationStrategy#setUseRegisteredExtensionsOnly(boolean)
	 * useRegisteredExtensionsOnly} 属性设置为 "false"，则扩展名列表可能会增加，
	 * 因为文件扩展名是通过 {@link org.springframework.http.MediaTypeFactory} 解析并缓存的。
	 */
	@Override
	public List<String> getAllFileExtensions() {
		return doResolveExtensions(MediaTypeFileExtensionResolver::getAllFileExtensions);
	}

	private List<String> doResolveExtensions(Function<MediaTypeFileExtensionResolver, List<String>> extractor) {
		List<String> result = null;

		// 遍历 扩展名解析器列表 中的每个解析器
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			// 应用提取器函数以获取解析器的文件扩展名列表
			List<String> extensions = extractor.apply(resolver);

			// 如果文件扩展名列表为空，则继续下一个解析器
			if (CollectionUtils.isEmpty(extensions)) {
				continue;
			}

			// 如果结果列表为 null，则初始化为一个包含 4 个元素的新列表
			result = (result != null ? result : new ArrayList<>(4));

			// 遍历文件扩展名列表
			for (String extension : extensions) {
				// 如果结果列表不包含当前扩展名，则将其添加到结果列表中
				if (!result.contains(extension)) {
					result.add(extension);
				}
			}
		}

		// 如果结果列表不为 null，则返回结果列表；否则返回空列表
		return (result != null ? result : Collections.emptyList());
	}

	/**
	 * 返回通过迭代 {@link MediaTypeFileExtensionResolver} 来注册的所有查找键到媒体类型映射。
	 *
	 * @since 5.2.4
	 */
	public Map<String, MediaType> getMediaTypeMappings() {
		Map<String, MediaType> result = null;

		// 遍历解析器列表中的每个解析器
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			// 如果解析器是 映射媒体类型文件扩展名解析器 的实例
			if (resolver instanceof MappingMediaTypeFileExtensionResolver) {
				// 获取解析器的媒体类型映射
				Map<String, MediaType> map = ((MappingMediaTypeFileExtensionResolver) resolver).getMediaTypes();

				// 如果媒体类型映射为空，则继续下一个解析器
				if (CollectionUtils.isEmpty(map)) {
					continue;
				}

				// 如果结果映射为 null，则初始化为一个包含 4 个初始容量的新映射
				result = (result != null ? result : new HashMap<>(4));

				// 将解析器的媒体类型映射添加到结果映射中
				result.putAll(map);
			}
		}

		// 如果结果映射不为 null，则返回结果映射；
		// 否则返回空映射
		return (result != null ? result : Collections.emptyMap());
	}

}
