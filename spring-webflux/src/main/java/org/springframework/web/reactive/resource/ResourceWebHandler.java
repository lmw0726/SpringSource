/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code HttpRequestHandler}用于根据Page Speed、YSlow等指南以优化的方式提供静态资源。
 *
 * <p>{@linkplain #setLocations "locations"}属性接受一个Spring {@link Resource}位置的列表，
 * 允许此处理程序提供静态资源。资源可以从类路径位置提供，例如 "classpath:/META-INF/public-web-resources/"，
 * 允许在jar文件中方便地打包和提供资源，例如 .js、.css等。
 *
 * <p>此请求处理程序还可以配置{@link #setResourceResolvers(List) resourcesResolver}和
 * {@link #setResourceTransformers(List) resourceTransformer}链，以支持正在提供的资源的任意解析和转换。
 * 默认情况下，{@link PathResourceResolver}仅基于配置的 "locations" 查找资源。
 * 应用程序可以配置其他解析器和转换器，例如 {@link VersionResourceResolver}，它可以解析和准备带有URL版本的资源的URL。
 *
 * <p>此处理程序还正确评估{@code Last-Modified}标头（如果存在），以便根据需要返回{@code 304}状态代码，
 * 避免由客户端已缓存的资源造成的不必要开销。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.0
 */
public class ResourceWebHandler implements WebHandler, InitializingBean {

	/**
	 * 支持的方法列表
	 */
	private static final Set<HttpMethod> SUPPORTED_METHODS = EnumSet.of(HttpMethod.GET, HttpMethod.HEAD);

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ResourceWebHandler.class);

	/**
	 * 资源加载器
	 */
	@Nullable
	private ResourceLoader resourceLoader;

	/**
	 * 资源位置列表
	 */
	private final List<String> locationValues = new ArrayList<>(4);

	/**
	 * 静态资源列表
	 */
	private final List<Resource> locationResources = new ArrayList<>(4);

	/**
	 * 已解析的资源列表
	 */
	private final List<Resource> locationsToUse = new ArrayList<>(4);

	/**
	 * 资源解析器列表
	 */
	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	/**
	 * 资源转换器列表
	 */
	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	/**
	 * 资源解析器链
	 */
	@Nullable
	private ResourceResolverChain resolverChain;

	/**
	 * 资源转换器链
	 */
	@Nullable
	private ResourceTransformerChain transformerChain;

	/**
	 * 缓存控制
	 */
	@Nullable
	private CacheControl cacheControl;

	/**
	 * 资源消息写入器
	 */
	@Nullable
	private ResourceHttpMessageWriter resourceHttpMessageWriter;

	/**
	 * 文件扩展名 - 媒体类型映射
	 */
	@Nullable
	private Map<String, MediaType> mediaTypes;

	/**
	 * 是否使用Last-Modified标头
	 */
	private boolean useLastModified = true;

	/**
	 * 是否通过在启动时对存在性检查以优化指定位置，
	 * 过滤掉非存在目录，从而无需在每次资源访问时检查它们。
	 */
	private boolean optimizeLocations = false;


	/**
	 * 提供用于加载{@link #setLocationValues location values}的ResourceLoader。
	 *
	 * @since 5.1
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 接受要解析为{@link Resource}位置的String类型的位置值列表。
	 *
	 * @since 5.1
	 */
	public void setLocationValues(List<String> locationValues) {
		Assert.notNull(locationValues, "Location values list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locationValues);
	}

	/**
	 * 返回配置的位置值。
	 *
	 * @since 5.1
	 */
	public List<String> getLocationValues() {
		return this.locationValues;
	}

	/**
	 * 设置用于作为静态资源源的{@code List} {@code Resource}路径。
	 */
	public void setLocations(@Nullable List<Resource> locations) {
		this.locationResources.clear();
		if (locations != null) {
			this.locationResources.addAll(locations);
		}
	}

	/**
	 * 返回用于作为静态资源源的{@code List} {@code Resource}路径。
	 * <p>请注意，如果提供了{@link #setLocationValues(List) locationValues}，
	 * 而不是加载的基于Resource的位置，此方法将返回空，直到通过{@link #afterPropertiesSet()}初始化。
	 * <p><strong>注意:</strong> 从5.3.11开始，位置列表可能被过滤，
	 * 以排除那些实际上不存在的位置，因此从此方法返回的列表可能是所有给定位置的子集。
	 * 请参阅{@link #setOptimizeLocations}。
	 *
	 * @see #setLocationValues
	 * @see #setLocations
	 */
	public List<Resource> getLocations() {
		if (this.locationsToUse.isEmpty()) {
			// 可能尚未初始化，只返回已有的内容
			return this.locationResources;
		}
		return this.locationsToUse;
	}

	/**
	 * 配置要使用的{@link ResourceResolver ResourceResolvers}列表。
	 * <p>默认情况下配置了{@link PathResourceResolver}。如果使用此属性，
	 * 建议将{@link PathResourceResolver}添加为最后一个解析器。
	 */
	public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
		this.resourceResolvers.clear();
		if (resourceResolvers != null) {
			this.resourceResolvers.addAll(resourceResolvers);
		}
	}

	/**
	 * 返回已配置的资源解析器列表。
	 */
	public List<ResourceResolver> getResourceResolvers() {
		return this.resourceResolvers;
	}

	/**
	 * 配置要使用的{@link ResourceTransformer ResourceTransformers}列表。
	 * <p>默认情况下未配置任何转换器以供使用。
	 */
	public void setResourceTransformers(@Nullable List<ResourceTransformer> resourceTransformers) {
		this.resourceTransformers.clear();
		if (resourceTransformers != null) {
			this.resourceTransformers.addAll(resourceTransformers);
		}
	}

	/**
	 * 返回配置的资源转换器列表。
	 */
	public List<ResourceTransformer> getResourceTransformers() {
		return this.resourceTransformers;
	}

	/**
	 * 配置要使用的{@link ResourceHttpMessageWriter}。
	 * <p>默认情况下将配置一个{@link ResourceHttpMessageWriter}。
	 */
	public void setResourceHttpMessageWriter(@Nullable ResourceHttpMessageWriter httpMessageWriter) {
		this.resourceHttpMessageWriter = httpMessageWriter;
	}

	/**
	 * 返回配置的资源消息写入器。
	 */
	@Nullable
	public ResourceHttpMessageWriter getResourceHttpMessageWriter() {
		return this.resourceHttpMessageWriter;
	}

	/**
	 * 设置用于构建Cache-Control HTTP响应头的{@link org.springframework.http.CacheControl}实例。
	 */
	public void setCacheControl(@Nullable CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * 返回用于构建Cache-Control HTTP响应头的{@link org.springframework.http.CacheControl}实例。
	 */
	@Nullable
	public CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * 设置是否在提供静态资源时查看{@link Resource#lastModified()}，
	 * 并使用此信息驱动{@code "Last-Modified"} HTTP响应头。
	 * <p>默认情况下启用此选项，如果应忽略静态文件的元数据，则应将其关闭。
	 *
	 * @since 5.3
	 */
	public void setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
	}

	/**
	 * 返回在提供静态资源时是否使用{@link Resource#lastModified()}信息驱动HTTP响应的标志。
	 *
	 * @since 5.3
	 */
	public boolean isUseLastModified() {
		return this.useLastModified;
	}

	/**
	 * 设置是否通过在启动时对存在性检查以优化指定位置，
	 * 过滤掉非存在目录，从而无需在每次资源访问时检查它们。
	 * <p>默认值为{@code false}，以防御性地对没有目录条目的zip文件进行过滤，
	 * 这些文件无法提前公开目录的存在性。将此标志切换为{@code true}以获得一致的jar布局和目录条目的优化访问。
	 *
	 * @since 5.3.13
	 */
	public void setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
	}

	/**
	 * 返回是否通过在启动时对存在性检查以优化指定位置，
	 * 过滤掉非存在目录，从而无需在每次资源访问时检查它们。
	 *
	 * @since 5.3.13
	 */
	public boolean isOptimizeLocations() {
		return this.optimizeLocations;
	}

	/**
	 * 添加从静态{@link Resource}的文件名中提取的文件扩展名到媒体类型的映射。
	 * <p>通常不需要使用此方法，因为可以通过{@link MediaTypeFactory#getMediaType(Resource)}确定映射。
	 *
	 * @param mediaTypes 媒体类型映射
	 * @since 5.3.2
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		if (this.mediaTypes == null) {
			// 如果媒体类型为空，初始化为空的哈希映射
			this.mediaTypes = new HashMap<>(mediaTypes.size());
		}
		// 将媒体类型映射的键按照区域设置转小写后，添加到媒体类型映射中
		mediaTypes.forEach((ext, type) ->
				this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), type));
	}

	/**
	 * 返回{@link #setMediaTypes(Map) 配置的}媒体类型映射。
	 *
	 * @since 5.3.2
	 */
	public Map<String, MediaType> getMediaTypes() {
		return (this.mediaTypes != null ? this.mediaTypes : Collections.emptyMap());
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// 解析资源位置
		resolveResourceLocations();

		if (this.resourceResolvers.isEmpty()) {
			// 如果资源解析器为空，添加一个默认的路径资源解析器
			this.resourceResolvers.add(new PathResourceResolver());
		}

		// 初始化允许的资源位置
		initAllowedLocations();

		if (getResourceHttpMessageWriter() == null) {
			// 如果资源 HTTP 消息写入器为空，创建一个新的资源 HTTP 消息写入器
			this.resourceHttpMessageWriter = new ResourceHttpMessageWriter();
		}

		// 初始化不可变的解析器和转换器链
		this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
		this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);
	}

	private void resolveResourceLocations() {
		// 创建一个新的资源列表，用于存储资源位置
		List<Resource> result = new ArrayList<>(this.locationResources);

		// 如果配置了字符串形式的资源位置
		if (!this.locationValues.isEmpty()) {
			// 确保资源加载器不为空
			Assert.notNull(this.resourceLoader, "ResourceLoader is required when \"locationValues\" are configured.");
			// 确保资源位置列表为空
			Assert.isTrue(CollectionUtils.isEmpty(this.locationResources), "Please set " +
					"either Resource-based \"locations\" or String-based \"locationValues\", but not both.");
			// 遍历每个字符串形式的资源位置，将其转换为 Resource 对象，并添加到结果列表中
			for (String location : this.locationValues) {
				result.add(this.resourceLoader.getResource(location));
			}
		}

		// 如果启用了优化资源位置
		if (isOptimizeLocations()) {
			// 过滤不存在的资源，并重新构建结果列表
			result = result.stream().filter(Resource::exists).collect(Collectors.toList());
		}

		// 清空原始的资源位置列表，将结果列表添加到使用的资源位置列表中
		this.locationsToUse.clear();
		this.locationsToUse.addAll(result);
	}

	/**
	 * 查找在配置的位置列表中设置其{@code allowedLocations}属性（如果为空）的{@code PathResourceResolver}。
	 */
	protected void initAllowedLocations() {
		// 如果资源位置列表为空，则直接返回
		if (CollectionUtils.isEmpty(getLocations())) {
			return;
		}
		// 倒序遍历资源解析器列表
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			// 如果当前解析器是 PathResourceResolver
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				// 将当前解析器转换为 PathResourceResolver 类型
				PathResourceResolver resolver = (PathResourceResolver) getResourceResolvers().get(i);
				// 如果解析器的允许位置列表为空，则设置允许位置为资源位置列表
				if (ObjectUtils.isEmpty(resolver.getAllowedLocations())) {
					resolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
				}
				// 找到第一个 PathResourceResolver 后即结束循环
				break;
			}
		}
	}


	/**
	 * 处理资源请求。
	 * <p>检查配置的位置列表中请求的资源是否存在。
	 * 如果资源不存在，将向客户端返回{@code 404}响应。
	 * 如果资源存在，则将检查请求是否存在{@code Last-Modified}标头，
	 * 并将其值与给定资源的上次修改时间戳进行比较，
	 * 如果{@code Last-Modified}值大于{@code 304}状态代码，则返回。
	 * 如果资源比{@code Last-Modified}值新，或者标头不存在，则将资源的内容写入响应，
	 * 并将缓存头设置为在未来一年过期。
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return getResource(exchange)
				// 如果资源不存在，则返回 404
				.switchIfEmpty(Mono.defer(() -> {
					logger.debug(exchange.getLogPrefix() + "Resource not found");
					return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND));
				}))
				.flatMap(resource -> {
					try {
						// 如果是 OPTIONS 请求，则添加 Allow 头部，并返回空结果
						if (HttpMethod.OPTIONS.matches(exchange.getRequest().getMethodValue())) {
							exchange.getResponse().getHeaders().add("Allow", "GET,HEAD,OPTIONS");
							return Mono.empty();
						}

						// 检查请求方法是否受支持
						HttpMethod httpMethod = exchange.getRequest().getMethod();
						if (!SUPPORTED_METHODS.contains(httpMethod)) {
							// 如果不是Get和Head请求，则抛出方法不允许异常
							return Mono.error(new MethodNotAllowedException(
									exchange.getRequest().getMethodValue(), SUPPORTED_METHODS));
						}

						// 如果启用了最后修改时间，且资源未修改，则返回空结果
						if (isUseLastModified() && exchange.checkNotModified(Instant.ofEpochMilli(resource.lastModified()))) {
							logger.trace(exchange.getLogPrefix() + "Resource not modified");
							return Mono.empty();
						}

						// 设置缓存控制头部
						CacheControl cacheControl = getCacheControl();
						if (cacheControl != null) {
							// 设置缓存控制响应头部
							exchange.getResponse().getHeaders().setCacheControl(cacheControl);
						}

						// 检查资源的媒体类型
						MediaType mediaType = getMediaType(resource);
						setHeaders(exchange, resource, mediaType);

						// 写入资源内容
						ResourceHttpMessageWriter writer = getResourceHttpMessageWriter();
						Assert.state(writer != null, "No ResourceHttpMessageWriter");
						return writer.write(Mono.just(resource),
								null, ResolvableType.forClass(Resource.class), mediaType,
								exchange.getRequest(), exchange.getResponse(),
								Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix()));
					} catch (IOException ex) {
						return Mono.error(ex);
					}
				});
	}

	protected Mono<Resource> getResource(ServerWebExchange exchange) {
		String name = HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
		// 获取处理器内部路径容器
		PathContainer pathWithinHandler = exchange.getRequiredAttribute(name);

		// 处理路径
		String path = processPath(pathWithinHandler.value());

		// 如果路径为空或无效，则返回空结果
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return Mono.empty();
		}
		// 如果路径包含了无效的转义字符，返回空结果
		if (isInvalidEncodedPath(path)) {
			return Mono.empty();
		}

		// 确保 ResourceResolverChain 和 ResourceTransformerChain 已初始化
		Assert.state(this.resolverChain != null, "ResourceResolverChain not initialized");
		Assert.state(this.transformerChain != null, "ResourceTransformerChain not initialized");

		// 解析资源并进行转换
		return this.resolverChain.resolveResource(exchange, path, getLocations())
				.flatMap(resource -> this.transformerChain.transform(exchange, resource));
	}

	/**
	 * 处理给定的资源路径。
	 * <p>默认实现替换：
	 * <ul>
	 * <li>反斜杠为正斜杠。
	 * <li>斜杠的重复为单个斜杠。
	 * <li>带有"./"的当前目录引用。
	 * <li>移除以".."开头的父目录引用，直到路径不再包含这样的引用。
	 * </ul>
	 *
	 * @since 3.2.12
	 */
	protected String processPath(String path) {
		// 将\替换为/
		path = StringUtils.replace(path, "\\", "/");
		// 清除重复的斜杠
		path = cleanDuplicateSlashes(path);
		// 清除开头的斜杠
		return cleanLeadingSlash(path);
	}

	private String cleanDuplicateSlashes(String path) {
		// 创建一个 StringBuilder 对象来构建结果字符串
		StringBuilder sb = null;
		// 用于保存前一个字符的变量
		char prev = 0;
		// 遍历路径中的每个字符
		for (int i = 0; i < path.length(); i++) {
			// 获取当前字符
			char curr = path.charAt(i);
			try {
				// 如果当前字符和前一个字符都是斜杠，则跳过
				if (curr == '/' && prev == '/') {
					// 如果 StringBuilder 对象还没有创建，则创建一个并拷贝之前的字符
					if (sb == null) {
						sb = new StringBuilder(path.substring(0, i));
					}
					// 继续下一次循环
					continue;
				}
				// 如果 StringBuilder 对象已经创建，则将当前字符添加到其中
				if (sb != null) {
					sb.append(path.charAt(i));
				}
			} finally {
				// 更新 prev 为当前字符，以备下一次循环使用
				prev = curr;
			}
		}
		// 返回处理后的路径，如果没有重复的斜杠则直接返回原始路径
		return (sb != null ? sb.toString() : path);
	}

	private String cleanLeadingSlash(String path) {
		// 用于标记是否出现过斜杠
		boolean slash = false;
		// 遍历路径中的每个字符
		for (int i = 0; i < path.length(); i++) {
			// 如果当前字符是斜杠，则将 slash 标记为 true
			if (path.charAt(i) == '/') {
				slash = true;
			} else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				// 如果当前字符不是空格且不是控制字符，则判断是否需要在路径前添加斜杠
				if (i == 0 || (i == 1 && slash)) {
					// 如果当前字符是路径的开始或者是路径的第二个字符且前面出现过斜杠，则直接返回原始路径
					return path;
				}
				// 如果前面出现过斜杠，则在当前字符前面添加斜杠并返回路径的子串
				return (slash ? "/" + path.substring(i) : path.substring(i));
			}
		}
		// 如果路径中有斜杠，则返回单独的斜杠，否则返回空字符串
		return (slash ? "/" : "");
	}

	/**
	 * 检查给定路径是否包含无效的转义序列。
	 *
	 * @param path 要验证的路径
	 * @return 如果路径无效则返回 {@code true}，否则返回 {@code false}
	 */
	private boolean isInvalidEncodedPath(String path) {
		if (path.contains("%")) {
			// 如果路径包含百分号
			try {
				// 使用URLDecoder（而不是UriUtils）以保留可能已解码的UTF-8字符
				String decodedPath = URLDecoder.decode(path, "UTF-8");
				if (isInvalidPath(decodedPath)) {
					// 如果解码后的路径是无效的路径，返回true
					return true;
				}
				// 处理路径
				decodedPath = processPath(decodedPath);
				if (isInvalidPath(decodedPath)) {
					// 如果处理后的路径是无效的路径，返回true
					return true;
				}
			} catch (IllegalArgumentException ex) {
				// 可能无法解码...
			} catch (UnsupportedEncodingException ex) {
				// 不应该发生...
			}
		}
		return false;
	}

	/**
	 * 标识无效的资源路径。默认情况下拒绝：
	 * <ul>
	 * <li>包含 "WEB-INF" 或 "META-INF" 的路径
	 * <li>调用 {@link StringUtils#cleanPath} 后包含 "../" 的路径
	 * <li>表示 {@link ResourceUtils#isUrl 有效URL} 的路径或在移除前导斜杠后将表示有效URL的路径。
	 * </ul>
	 * <p><strong>注意：</strong>此方法假设前导、重复的 '/' 或控制字符（例如空格）已被修剪，
	 * 以便路径可预测地以单个 '/' 开头或不包含斜杠。
	 *
	 * @param path 要验证的路径
	 * @return 如果路径无效则返回 {@code true}，否则返回 {@code false}
	 */
	protected boolean isInvalidPath(String path) {
		// 如果路径包含 "WEB-INF" 或 "META-INF"，则记录警告并返回 true
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path with \"WEB-INF\" or \"META-INF\": [" + path + "]", -1, true));
			}
			return true;
		}

		// 如果路径包含 ":/"
		if (path.contains(":/")) {
			// 获取相关路径：如果是是以/开头，则获取/后的路径；否则就获取原路径。
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				// 如果相关路径是一个资源URL或者是以url:
				if (logger.isWarnEnabled()) {
					logger.warn(LogFormatUtils.formatValue(
							"Path represents URL or has \"url:\" prefix: [" + path + "]", -1, true));
				}
				// 返回true
				return true;
			}
		}

		// 如果路径包含 ".." 并且是规范化后的路径仍然包含../，则记录警告并返回 true
		if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]", -1, true));
			}
			return true;
		}

		// 如果以上条件都不满足，则返回 false
		return false;
	}

	@Nullable
	private MediaType getMediaType(Resource resource) {
		// 初始化媒体类型为 null
		MediaType mediaType = null;

		// 获取资源的文件名
		String filename = resource.getFilename();

		// 如果媒体类型映射不为空
		if (!CollectionUtils.isEmpty(this.mediaTypes)) {
			// 获取文件名的扩展名
			String ext = StringUtils.getFilenameExtension(filename);
			// 如果扩展名不为空
			if (ext != null) {
				// 从媒体类型映射中获取对应扩展名的媒体类型
				mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
			}
		}

		// 如果媒体类型仍为 null
		if (mediaType == null) {
			// 获取文件名的媒体类型列表
			List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
			// 如果媒体类型列表不为空
			if (!CollectionUtils.isEmpty(mediaTypes)) {
				// 取第一个媒体类型作为最终媒体类型
				mediaType = mediaTypes.get(0);
			}
		}

		// 返回媒体类型
		return mediaType;
	}

	/**
	 * 在响应中设置头信息。用于 GET 和 HEAD 请求。
	 *
	 * @param exchange  当前交换对象
	 * @param resource  已识别的资源（永不为 {@code null}）
	 * @param mediaType 资源的媒体类型（永不为 {@code null}）
	 */
	protected void setHeaders(ServerWebExchange exchange, Resource resource, @Nullable MediaType mediaType)
			throws IOException {

		// 获取响应头
		HttpHeaders headers = exchange.getResponse().getHeaders();

		// 获取资源的长度
		long length = resource.contentLength();
		// 设置内容长度到响应头
		headers.setContentLength(length);

		// 如果媒体类型不为空
		if (mediaType != null) {
			// 设置媒体类型到响应头
			headers.setContentType(mediaType);
		}

		// 如果资源是 HttpResource 类型
		if (resource instanceof HttpResource) {
			// 获取资源的响应头
			HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
			// 将资源的响应头添加到交换响应的头部
			exchange.getResponse().getHeaders().putAll(resourceHeaders);
		}
	}


	@Override
	public String toString() {
		return "ResourceWebHandler " + locationToString(getLocations());
	}

	private String locationToString(List<Resource> locations) {
		return locations.toString()
				.replaceAll("class path resource", "classpath")
				.replaceAll("ServletContext resource", "ServletContext");
	}

}
