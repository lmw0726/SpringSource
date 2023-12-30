/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * 解析包含版本字符串的请求路径，用作HTTP缓存策略的一部分，使资源缓存到将来的某个日期（例如一年后），
 * 直到版本（URL）发生变化。
 *
 * <p>存在不同的版本控制策略，此解析器必须配置一个或多个此类策略，以及指示哪些资源适用于哪种策略的路径映射。
 *
 * <p>{@code ContentVersionStrategy} 是一个很好的默认选择，除非在某些情况下无法使用。特别是在无法与JavaScript模块加载器一起使用时，
 * {@code ContentVersionStrategy} 不是一个好选择，这种情况下，{@code FixedVersionStrategy} 更合适。
 *
 * <p>需要注意的是，如果要使用此解析器提供CSS文件，则还应该使用{@link CssLinkResourceTransformer}，
 * 以修改CSS文件中的链接，使其包含此解析器生成的适当版本号。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see VersionStrategy
 * @since 5.0
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	/**
	 * Ant风格匹配器
	 */
	private AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 路径模式到版本策略的映射。
	 */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<>();


	/**
	 * 设置一个以URL路径为键、{@code VersionStrategy}为值的Map。
	 * <p>支持直接URL匹配和Ant样式的模式匹配。有关语法详细信息，请参阅{@link AntPathMatcher}的javadoc。
	 *
	 * @param map URL作为键，版本策略作为值的映射
	 */
	public void setStrategyMap(Map<String, VersionStrategy> map) {
		this.versionStrategyMap.clear();
		this.versionStrategyMap.putAll(map);
	}

	/**
	 * 返回按路径模式键控的版本策略映射。
	 */
	public Map<String, VersionStrategy> getStrategyMap() {
		return this.versionStrategyMap;
	}

	/**
	 * 在匹配给定路径模式的资源URL中插入基于内容的版本。版本是从文件内容计算的，例如：
	 * {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}。
	 * 这是一个很好的默认策略，除非在无法使用的情况下，例如使用JavaScript模块加载器时，
	 * 对于提供JavaScript文件，请改用{@link #addFixedVersionStrategy}。
	 *
	 * @param pathPatterns 一个或多个资源URL路径模式，
	 *                     相对于配置资源处理程序的模式
	 * @return 当前实例，用于链式方法调用
	 * @see ContentVersionStrategy
	 */
	public VersionResourceResolver addContentVersionStrategy(String... pathPatterns) {
		addVersionStrategy(new ContentVersionStrategy(), pathPatterns);
		return this;
	}

	/**
	 * 在匹配给定路径模式的资源URL中插入基于固定前缀的版本，例如：<code>"{version}/js/main.js"</code>。
	 * 这在使用JavaScript模块加载器时很有用（相对于基于内容的版本）。
	 * <p>版本可以是随机数、当前日期，也可以是从git提交SHA、属性文件或环境变量中获取的值，
	 * 并在配置中使用SpEL表达式进行设置（例如，参见Java配置中的{@code @Value}）。
	 * <p>如果尚未完成，会自动配置给定{@code pathPatterns}的变体，使用{@code version}作为前缀。
	 * 例如，添加一个{@code "/js/**"}路径模式将自动配置一个{@code "/v1.0.0/js/**"}，
	 * 其中{@code "v1.0.0"}是作为参数给出的{@code version}字符串。
	 *
	 * @param version      版本字符串
	 * @param pathPatterns 一个或多个资源URL路径模式，
	 *                     相对于配置资源处理程序的模式
	 * @return 当前实例，用于链式方法调用
	 * @see FixedVersionStrategy
	 */
	public VersionResourceResolver addFixedVersionStrategy(String version, String... pathPatterns) {
		// 将路径模式转换为列表
		List<String> patternsList = Arrays.asList(pathPatterns);

		// 存储带有前缀的模式
		List<String> prefixedPatterns = new ArrayList<>(pathPatterns.length);

		// 构建版本前缀
		String versionPrefix = "/" + version;

		// 遍历模式列表
		for (String pattern : patternsList) {
			// 添加原始模式
			prefixedPatterns.add(pattern);

			// 如果模式不以版本前缀开头，并且模式列表中不包含带版本前缀的模式，则添加带版本前缀的模式
			if (!pattern.startsWith(versionPrefix) && !patternsList.contains(versionPrefix + pattern)) {
				prefixedPatterns.add(versionPrefix + pattern);
			}
		}

		// 添加版本策略并返回结果
		return addVersionStrategy(new FixedVersionStrategy(version), StringUtils.toStringArray(prefixedPatterns));
	}

	/**
	 * 注册自定义的VersionStrategy，应用于匹配给定路径模式的资源URL。
	 *
	 * @param strategy     自定义策略
	 * @param pathPatterns 一个或多个资源URL路径模式，
	 *                     相对于配置资源处理程序的模式
	 * @return 当前实例，用于链式方法调用
	 * @see VersionStrategy
	 */
	public VersionResourceResolver addVersionStrategy(VersionStrategy strategy, String... pathPatterns) {
		for (String pattern : pathPatterns) {
			getStrategyMap().put(pattern, strategy);
		}
		return this;
	}


	/**
	 * 解析资源内部方法。尝试从给定的位置列表中解析资源。如果找不到资源，则尝试使用版本化资源解析。
	 *
	 * @param exchange     ServerWebExchange对象，表示正在处理的Web请求和响应
	 * @param requestPath  请求的资源路径
	 * @param locations    资源位置列表
	 * @param chain        ResourceResolverChain对象，用于解析资源链
	 * @return 包含资源的Mono对象
	 */
	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
													 String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations)
				.switchIfEmpty(Mono.defer(() ->
						resolveVersionedResource(exchange, requestPath, locations, chain)));
	}

	/**
	 * 解析具有版本信息的资源方法。
	 *
	 * @param exchange     ServerWebExchange对象，表示正在处理的Web请求和响应
	 * @param requestPath  请求的资源路径
	 * @param locations    资源位置列表
	 * @param chain        ResourceResolverChain对象，用于解析资源链
	 * @return 包含版本信息的资源的Mono对象
	 */
	private Mono<Resource> resolveVersionedResource(@Nullable ServerWebExchange exchange,
													String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		// 获取资源路径的版本策略
		VersionStrategy versionStrategy = getStrategyForPath(requestPath);

		// 如果没有版本策略，返回空 Mono
		if (versionStrategy == null) {
			return Mono.empty();
		}

		// 从路径中提取候选版本
		String candidate = versionStrategy.extractVersion(requestPath);

		// 如果候选版本长度为空，返回空 Mono
		if (!StringUtils.hasLength(candidate)) {
			return Mono.empty();
		}

		// 获取不带版本的简化路径
		String simplePath = versionStrategy.removeVersion(requestPath, candidate);

		// 通过链式调用解析资源，并筛选匹配版本的资源
		return chain.resolveResource(exchange, simplePath, locations)
				.filterWhen(resource -> versionStrategy.getResourceVersion(resource)
						.map(actual -> {
							if (candidate.equals(actual)) {
								return true;
							} else {
								if (logger.isTraceEnabled()) {
									String logPrefix = exchange != null ? exchange.getLogPrefix() : "";
									logger.trace(logPrefix + "Found resource for \"" + requestPath +
											"\", but version [" + candidate + "] does not match");
								}
								return false;
							}
						}))
				.map(resource -> new FileNameVersionedResource(resource, candidate));
	}

	/**
	 * 解析URL路径内部方法。尝试从给定位置列表中解析URL路径。
	 * 如果找到基本URL路径，则尝试使用版本策略来获取资源版本信息并添加到URL中。
	 *
	 * @param resourceUrlPath  资源的URL路径
	 * @param locations        资源位置列表
	 * @param chain            ResourceResolverChain对象，用于解析资源链
	 * @return 包含URL路径的Mono对象
	 */
	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
												  List<? extends Resource> locations, ResourceResolverChain chain) {

		// 通过链式调用解析资源的 URL 路径
		return chain.resolveUrlPath(resourceUrlPath, locations)
				.flatMap(baseUrl -> {
					// 如果基本 URL 不为空
					if (StringUtils.hasText(baseUrl)) {
						// 获取用于资源路径的版本策略
						VersionStrategy strategy = getStrategyForPath(resourceUrlPath);

						// 如果没有版本策略，直接返回基本 URL
						if (strategy == null) {
							return Mono.just(baseUrl);
						}

						// 如果有版本策略，进一步通过链式调用解析资源，并获取资源版本
						return chain.resolveResource(null, baseUrl, locations)
								.flatMap(resource -> strategy.getResourceVersion(resource)
										.map(version -> strategy.addVersion(baseUrl, version)));
					}

					// 如果基本 URL 为空，返回空 Mono
					return Mono.empty();
				});
	}

	/**
	 * 查找适用于请求资源的请求路径的{@code VersionStrategy}。
	 *
	 * @param requestPath 请求资源的请求路径
	 * @return {@code VersionStrategy}的实例，如果没有匹配的请求路径则返回null
	 */
	@Nullable
	protected VersionStrategy getStrategyForPath(String requestPath) {
		// 将请求路径添加斜杠前缀
		String path = "/".concat(requestPath);

		// 存储匹配的模式列表
		List<String> matchingPatterns = new ArrayList<>();

		// 遍历版本策略映射的键集合
		for (String pattern : this.versionStrategyMap.keySet()) {
			// 如果路径与模式匹配，则将模式添加到匹配的模式列表中
			if (this.pathMatcher.match(pattern, path)) {
				matchingPatterns.add(pattern);
			}
		}

		// 如果匹配的模式列表不为空
		if (!matchingPatterns.isEmpty()) {
			// 获取路径模式比较器并对匹配的模式列表进行排序
			Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
			matchingPatterns.sort(comparator);

			// 返回第一个匹配模式对应的版本策略
			return this.versionStrategyMap.get(matchingPatterns.get(0));
		}

		// 如果没有匹配的模式，则返回空
		return null;
	}

	/**
	 * 文件名称版本资源
	 */
	private static class FileNameVersionedResource extends AbstractResource implements HttpResource {

		/**
		 * 原始资源
		 */
		private final Resource original;
		/**
		 * 版本信息
		 */
		private final String version;

		public FileNameVersionedResource(Resource original, String version) {
			this.original = original;
			this.version = version;
		}

		@Override
		public boolean exists() {
			return this.original.exists();
		}

		@Override
		public boolean isReadable() {
			return this.original.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.original.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.original.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.original.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.original.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.original.getFile();
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public long contentLength() throws IOException {
			return this.original.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.original.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.original.createRelative(relativePath);
		}

		@Override
		public String getDescription() {
			return this.original.getDescription();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.original.getInputStream();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers = (this.original instanceof HttpResource ?
					// 如果原始对象是 HttpResource，则获取响应头，否则创建一个新的 HttpHeaders 对象
					((HttpResource) this.original).getResponseHeaders() : new HttpHeaders());
			// 设置 ETag 头信息，带有版本号
			headers.setETag("W/\"" + this.version + "\"");
			return headers;
		}
	}

}
