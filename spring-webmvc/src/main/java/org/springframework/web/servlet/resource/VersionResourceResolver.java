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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * 解析包含版本字符串的请求路径，该字符串可作为 HTTP 缓存策略的一部分，
 * 其中资源被缓存到将来（例如 1 年）的某个日期，并缓存直到版本（因此 URL）发生更改。
 *
 * <p>存在不同的版本化策略，此解析器必须配置为使用一个或多个此类策略以及指示哪个资源适用于哪个策略的路径映射。
 *
 * <p>{@code ContentVersionStrategy} 是一个很好的默认选择，除非无法使用。
 * 特别是 {@code ContentVersionStrategy} 无法与 JavaScript 模块加载器结合使用。
 * 对于这种情况，{@code FixedVersionStrategy} 是一个更好的选择。
 *
 * <p>请注意，使用此解析器来提供 CSS 文件意味着还应使用 {@link CssLinkResourceTransformer}，
 * 以修改 CSS 文件中的链接，以包含由此解析器生成的适当版本。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see VersionStrategy
 * @since 4.1
 */
public class VersionResourceResolver extends AbstractResourceResolver {

	/**
	 * Ant风格路径匹配器
	 */
	private AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 路径模式 -> 版本策略 的映射。
	 */
	private final Map<String, VersionStrategy> versionStrategyMap = new LinkedHashMap<>();


	/**
	 * 设置一个以 URL 路径为键，{@code VersionStrategy} 为值的映射。
	 * <p>支持直接 URL 匹配和 Ant 风格的模式匹配。有关语法详细信息，请参见 {@link org.springframework.util.AntPathMatcher} 的 javadoc。
	 *
	 * @param map 以 URL 为键和版本策略为值的映射
	 */
	public void setStrategyMap(Map<String, VersionStrategy> map) {
		this.versionStrategyMap.clear();
		this.versionStrategyMap.putAll(map);
	}

	/**
	 * 返回按路径模式分组的版本策略映射。
	 */
	public Map<String, VersionStrategy> getStrategyMap() {
		return this.versionStrategyMap;
	}

	/**
	 * 为与给定路径模式匹配的资源 URL 插入基于内容的版本。版本是从文件内容计算的，例如 {@code "css/main-e36d2e05253c6c7085a91522ce43a0b4.css"}。这是一个很好的默认策略，除非它不能使用，例如当使用 JavaScript 模块加载器时，应改为使用 {@link #addFixedVersionStrategy} 来提供 JavaScript 文件。
	 *
	 * @param pathPatterns 一个或多个资源 URL 路径模式，相对于配置资源处理程序的模式
	 * @return 用于链式方法调用的当前实例
	 * @see ContentVersionStrategy
	 */
	public VersionResourceResolver addContentVersionStrategy(String... pathPatterns) {
		// 添加内容版本策略
		addVersionStrategy(new ContentVersionStrategy(), pathPatterns);
		return this;
	}

	/**
	 * 为与给定路径模式匹配的资源 URL 插入固定的、基于前缀的版本，例如: <code>"{version}/js/main.js"</code>。
	 * 这在使用 JavaScript 模块加载器时很有用（与基于内容的版本相比）。
	 * <p>版本可以是一个随机数、当前日期，或者从 git 提交 sha、属性文件或环境变量中获取的值，
	 * 并且可以在配置中使用 SpEL 表达式设置（例如，请参阅 Java 配置中的 {@code @Value}）。
	 * <p>如果尚未完成，将自动配置给定的 {@code pathPatterns} 的变体，前缀为 {@code version}。
	 * 例如，添加一个 {@code "/js/**"} 路径模式也将自动配置一个 {@code "/v1.0.0/js/**"}，
	 * 其中 {@code "v1.0.0"} 是作为参数给定的 {@code version} 字符串。
	 *
	 * @param version      版本字符串
	 * @param pathPatterns 一个或多个资源 URL 路径模式，相对于配置资源处理程序的模式
	 * @return 用于链式方法调用的当前实例
	 * @see FixedVersionStrategy
	 */
	public VersionResourceResolver addFixedVersionStrategy(String version, String... pathPatterns) {
		// 将 URL 路径模式 转换为 不可修改的列表
		List<String> patternsList = Arrays.asList(pathPatterns);
		// 创建一个空的列表来存储带有版本前缀的路径模式
		List<String> prefixedPatterns = new ArrayList<>(pathPatterns.length);
		// 构建版本前缀
		String versionPrefix = "/" + version;
		// 遍历每个路径模式
		for (String pattern : patternsList) {
			// 将当前路径模式添加到 带有版本前缀的路径模式列表 中
			prefixedPatterns.add(pattern);
			// 如果当前路径模式不以版本前缀开头，并且在 路径模式列表 中也不存在带有版本前缀的模式
			if (!pattern.startsWith(versionPrefix) && !patternsList.contains(versionPrefix + pattern)) {
				// 将带有版本前缀的路径模式添加到 带有版本前缀的路径模式列表 中
				prefixedPatterns.add(versionPrefix + pattern);
			}
		}
		// 使用 固定版本策略 创建版本化的URL映射策略，并返回当前版本资源解析器
		return addVersionStrategy(new FixedVersionStrategy(version), StringUtils.toStringArray(prefixedPatterns));
	}

	/**
	 * 注册一个自定义的 VersionStrategy，应用于匹配给定路径模式的资源 URL。
	 *
	 * @param strategy     自定义策略
	 * @param pathPatterns 一个或多个资源 URL 路径模式，相对于配置资源处理程序的模式
	 * @return 用于链式方法调用的当前实例
	 * @see VersionStrategy
	 */
	public VersionResourceResolver addVersionStrategy(VersionStrategy strategy, String... pathPatterns) {
		for (String pattern : pathPatterns) {
			// 添加路径模式->版本策略映射
			getStrategyMap().put(pattern, strategy);
		}
		return this;
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
											   List<? extends Resource> locations, ResourceResolverChain chain) {

		// 尝试解析资源
		Resource resolved = chain.resolveResource(request, requestPath, locations);

		// 如果成功解析到资源，则直接返回
		if (resolved != null) {
			return resolved;
		}

		// 获取路径的版本策略
		VersionStrategy versionStrategy = getStrategyForPath(requestPath);

		// 如果未找到路径的版本策略，则返回null
		if (versionStrategy == null) {
			return null;
		}

		// 提取路径中的候选版本号
		String candidateVersion = versionStrategy.extractVersion(requestPath);

		// 如果候选版本号为空，则返回null
		if (!StringUtils.hasLength(candidateVersion)) {
			return null;
		}

		// 移除路径中的版本号，得到简化的路径
		String simplePath = versionStrategy.removeVersion(requestPath, candidateVersion);

		// 解析简化的路径对应的资源
		Resource baseResource = chain.resolveResource(request, simplePath, locations);

		// 如果未找到简化的路径对应的资源，则返回null
		if (baseResource == null) {
			return null;
		}

		// 获取实际资源的版本号
		String actualVersion = versionStrategy.getResourceVersion(baseResource);

		// 如果候选版本号与实际版本号相同，则返回带有版本号的资源
		if (candidateVersion.equals(actualVersion)) {
			return new FileNameVersionedResource(baseResource, candidateVersion);
		} else {
			// 如果候选版本号与实际版本号不同，则记录日志，并返回null
			if (logger.isTraceEnabled()) {
				logger.trace("Found resource for \"" + requestPath + "\", but version [" +
						candidateVersion + "] does not match");
			}
			return null;
		}
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
											List<? extends Resource> locations, ResourceResolverChain chain) {

		// 解析资源URL路径的基本URL
		String baseUrl = chain.resolveUrlPath(resourceUrlPath, locations);

		// 如果基本URL不为空
		if (StringUtils.hasText(baseUrl)) {
			// 获取资源URL路径的版本策略
			VersionStrategy versionStrategy = getStrategyForPath(resourceUrlPath);
			// 如果资源URL路径没有关联的版本策略，则直接返回基本URL
			if (versionStrategy == null) {
				return baseUrl;
			}
			// 解析基本URL对应的资源
			Resource resource = chain.resolveResource(null, baseUrl, locations);
			// 确保资源不为空，否则抛出异常
			Assert.state(resource != null, "Unresolvable resource");
			// 获取资源的版本号
			String version = versionStrategy.getResourceVersion(resource);
			// 将版本号添加到基本URL中，并返回带有版本号的URL
			return versionStrategy.addVersion(baseUrl, version);
		}

		// 如果基本URL为空，则直接返回null
		return baseUrl;
	}

	/**
	 * 查找请求路径的请求资源的 VersionStrategy。
	 *
	 * @return {@code VersionStrategy} 实例，如果没有与请求路径匹配的，则返回 null
	 */
	@Nullable
	protected VersionStrategy getStrategyForPath(String requestPath) {
		// 拼接请求路径的前导斜杠
		String path = "/".concat(requestPath);
		// 存储匹配的路径模式的列表
		List<String> matchingPatterns = new ArrayList<>();

		// 遍历版本策略映射中的所有路径模式
		for (String pattern : this.versionStrategyMap.keySet()) {
			// 如果请求路径与当前路径模式匹配，则将该路径模式添加到匹配的列表中
			if (this.pathMatcher.match(pattern, path)) {
				matchingPatterns.add(pattern);
			}
		}

		// 如果匹配的路径模式列表不为空
		if (!matchingPatterns.isEmpty()) {
			// 根据请求路径获取PatternComparator
			Comparator<String> comparator = this.pathMatcher.getPatternComparator(path);
			// 对匹配的路径模式进行排序
			matchingPatterns.sort(comparator);
			// 返回匹配列表中的第一个路径模式对应的版本策略
			return this.versionStrategyMap.get(matchingPatterns.get(0));
		}

		// 如果匹配的路径模式列表为空，则返回null
		return null;
	}


	private static class FileNameVersionedResource extends AbstractResource implements HttpResource {

		/**
		 * 原始资源
		 */
		private final Resource original;

		/**
		 * 资源的版本号
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
			// 创建HttpHeaders对象
			HttpHeaders headers = (this.original instanceof HttpResource ?
					// 如果原始对象是HttpResource，则获取其响应头部；否则创建一个空的HttpHeaders对象
					((HttpResource) this.original).getResponseHeaders() : new HttpHeaders());
			// 设置ETag头部，值为"W/" + 版本号 + ""
			headers.setETag("W/\"" + this.version + "\"");

			// 返回构建好的HttpHeaders对象
			return headers;
		}
	}

}
