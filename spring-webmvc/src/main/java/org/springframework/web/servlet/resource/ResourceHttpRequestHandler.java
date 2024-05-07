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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code HttpRequestHandler}，根据Page Speed、YSlow等指南以优化方式提供静态资源。
 *
 * <p>{@linkplain #setLocations "locations"}和{@linkplain #setLocationValues "locationValues"}属性接受此处理程序可以提供静态资源的位置。
 * 这可以是相对于Web应用程序的根，也可以是类路径，例如“classpath:/META-INF/public-web-resources/”，允许在jar文件中方便地打包和提供资源，如 .js、.css 等。
 *
 * <p>此请求处理程序还可以配置{@link #setResourceResolvers(List) resourcesResolver}和{@link #setResourceTransformers(List) resourceTransformer}链，
 * 以支持正在提供的资源的任意解析和转换。默认情况下，{@link PathResourceResolver}仅基于配置的“locations”查找资源。
 * 应用程序可以配置其他解析器和转换器，例如{@link VersionResourceResolver}，它可以解析并准备带有URL版本的资源的URL。
 *
 * <p>此处理程序还正确评估{@code Last-Modified}标头（如果存在），以便根据需要返回{@code 304}状态码，从而避免不必要的开销，
 * 因为客户端已经缓存了资源。
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 3.0.4
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
		implements HttpRequestHandler, EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {

	private static final Log logger = LogFactory.getLog(ResourceHttpRequestHandler.class);

	private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";


	private final List<String> locationValues = new ArrayList<>(4);

	private final List<Resource> locationResources = new ArrayList<>(4);

	private final List<Resource> locationsToUse = new ArrayList<>(4);

	private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

	private final List<ResourceResolver> resourceResolvers = new ArrayList<>(4);

	private final List<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

	@Nullable
	private ResourceResolverChain resolverChain;

	@Nullable
	private ResourceTransformerChain transformerChain;

	@Nullable
	private ResourceHttpMessageConverter resourceHttpMessageConverter;

	@Nullable
	private ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	private final Map<String, MediaType> mediaTypes = new HashMap<>(4);

	@Nullable
	private CorsConfiguration corsConfiguration;

	@Nullable
	private UrlPathHelper urlPathHelper;

	private boolean useLastModified = true;

	private boolean optimizeLocations = false;

	@Nullable
	private StringValueResolver embeddedValueResolver;


	public ResourceHttpRequestHandler() {
		super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
	}


	/**
	 * 配置用于提供资源的基于字符串的位置。
	 * <p>例如，{{@code "/"}、{@code "classpath:/META-INF/public-web-resources/"}} 允许从 Web 应用程序根目录和
	 * 类路径上的任何包含 {@code /META-INF/public-web-resources/} 目录的 JAR 中提供资源，其中 Web 应用程序根目录中的资源优先。
	 * <p>对于 {@link org.springframework.core.io.UrlResource URL-based 资源}（例如文件、HTTP URL 等），
	 * 此方法支持特殊前缀来指示与 URL 相关联的字符集，以便可以正确编码附加到它的相对路径，例如 {@code "[charset=Windows-31J]https://example.org/path"}。
	 *
	 * @see #setLocations(List)
	 * @since 4.3.13
	 */
	public void setLocationValues(List<String> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locationValues.clear();
		this.locationValues.addAll(locations);
	}

	/**
	 * 配置用于提供资源的位置为预先解析的 Resource。
	 *
	 * @see #setLocationValues(List)
	 */
	public void setLocations(List<Resource> locations) {
		Assert.notNull(locations, "Locations list must not be null");
		this.locationResources.clear();
		this.locationResources.addAll(locations);
	}

	/**
	 * 返回已配置的 {@code Resource} 位置的 {@code List}，包括通过 {@link #setLocationValues(List) setLocationValues} 提供的基于字符串的位置
	 * 和通过 {@link #setLocations(List) setLocations} 提供的预先解析的 {@code Resource} 位置。
	 * <p>请注意，在通过 {@link #afterPropertiesSet()} 进行初始化之后，返回的列表才完全初始化。
	 * <p><strong>注意:</strong> 从 5.3.11 开始，位置列表可能会被过滤，以排除那些实际不存在的位置，
	 * 因此从此方法返回的列表可能是所有给定位置的子集。参见 {@link #setOptimizeLocations}。
	 *
	 * @see #setLocationValues
	 * @see #setLocations
	 */
	public List<Resource> getLocations() {
		if (this.locationsToUse.isEmpty()) {
			// 可能尚未初始化，仅返回到目前为止的内容
			return this.locationResources;
		}
		return this.locationsToUse;
	}

	/**
	 * 配置要使用的 {@link ResourceResolver ResourceResolvers} 列表。
	 * <p>默认情况下，配置了 {@link PathResourceResolver}。如果使用此属性，建议将 {@link PathResourceResolver} 添加为最后一个解析器。
	 */
	public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
		this.resourceResolvers.clear();
		if (resourceResolvers != null) {
			this.resourceResolvers.addAll(resourceResolvers);
		}
	}

	/**
	 * 返回配置的资源解析器列表。
	 */
	public List<ResourceResolver> getResourceResolvers() {
		return this.resourceResolvers;
	}

	/**
	 * 配置要使用的 {@link ResourceTransformer ResourceTransformers} 列表。
	 * <p>默认情况下，不配置使用任何转换器。
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
	 * 配置要使用的 {@link ResourceHttpMessageConverter}。
	 * <p>默认情况下，将配置一个 {@link ResourceHttpMessageConverter}。
	 *
	 * @since 4.3
	 */
	public void setResourceHttpMessageConverter(@Nullable ResourceHttpMessageConverter messageConverter) {
		this.resourceHttpMessageConverter = messageConverter;
	}

	/**
	 * 返回配置的资源转换器。
	 *
	 * @since 4.3
	 */
	@Nullable
	public ResourceHttpMessageConverter getResourceHttpMessageConverter() {
		return this.resourceHttpMessageConverter;
	}

	/**
	 * 配置要使用的 {@link ResourceRegionHttpMessageConverter}。
	 * <p>默认情况下，将配置一个 {@link ResourceRegionHttpMessageConverter}。
	 *
	 * @since 4.3
	 */
	public void setResourceRegionHttpMessageConverter(@Nullable ResourceRegionHttpMessageConverter messageConverter) {
		this.resourceRegionHttpMessageConverter = messageConverter;
	}

	/**
	 * 返回配置的资源区域转换器。
	 *
	 * @since 4.3
	 */
	@Nullable
	public ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
		return this.resourceRegionHttpMessageConverter;
	}

	/**
	 * 配置一个 {@code ContentNegotiationManager}，以帮助确定正在提供的资源的媒体类型。
	 * 如果管理器包含路径扩展策略，则会检查已注册的文件扩展名。
	 *
	 * @since 4.3
	 * @deprecated 自 5.2.4 起弃用，建议使用 {@link #setMediaTypes(Map)}，
	 * 其中的映射可能是通过 {@link ContentNegotiationManager#getMediaTypeMappings()} 获取的。
	 */
	@Deprecated
	public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回配置的内容协商管理器。
	 *
	 * @since 4.3
	 * @deprecated 自 5.2.4 起弃用
	 */
	@Nullable
	@Deprecated
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 添加从静态 {@link Resource} 的文件名中提取的文件扩展名与要设置在响应中的相应媒体类型之间的映射关系。
	 * <p>通常情况下不需要使用此方法，因为映射通常通过
	 * {@link javax.servlet.ServletContext#getMimeType(String)} 或者
	 * {@link MediaTypeFactory#getMediaType(Resource)} 确定。
	 *
	 * @param mediaTypes 媒体类型映射
	 * @since 5.2.4
	 */
	public void setMediaTypes(Map<String, MediaType> mediaTypes) {
		mediaTypes.forEach((ext, mediaType) ->
				this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), mediaType));
	}

	/**
	 * 返回 {@link #setMediaTypes(Map) 配置的} 媒体类型。
	 *
	 * @since 5.2.4
	 */
	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * 指定为此处理程序提供资源的 CORS 配置。
	 * <p>默认情况下，未设置此配置，从而允许跨域请求。
	 */
	public void setCorsConfiguration(CorsConfiguration corsConfiguration) {
		this.corsConfiguration = corsConfiguration;
	}

	/**
	 * 返回指定的 CORS 配置。
	 */
	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		return this.corsConfiguration;
	}

	/**
	 * 提供对用于将请求映射到静态资源的 {@link UrlPathHelper} 的引用。这有助于获取有关查找路径的信息，例如它是否已解码。
	 *
	 * @since 4.3.13
	 */
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 已配置的 {@link UrlPathHelper}。
	 *
	 * @since 4.3.13
	 */
	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 设置是否在提供资源时查看 {@link Resource#lastModified()}，并使用此信息来驱动 {@code "Last-Modified"} HTTP 响应头。
	 * <p>默认情况下启用此选项，如果要忽略静态文件的元数据，则应将其关闭。
	 *
	 * @since 5.3
	 */
	public void setUseLastModified(boolean useLastModified) {
		this.useLastModified = useLastModified;
	}

	/**
	 * 返回是否在提供静态资源时使用 {@link Resource#lastModified()} 信息来驱动 HTTP 响应。
	 *
	 * @since 5.3
	 */
	public boolean isUseLastModified() {
		return this.useLastModified;
	}

	/**
	 * 设置是否通过在启动时对指定的位置进行存在性检查来优化它们，从而提前过滤掉不存在的目录，以便不必在每次资源访问时检查它们。
	 * <p>默认值为 {@code false}，用于对抗没有目录条目的 zip 文件，无法提前暴露目录的存在性。在一致的 jar 布局且具有目录条目的情况下，将此标志切换为 {@code true} 以实现优化的访问。
	 *
	 * @since 5.3.13
	 */
	public void setOptimizeLocations(boolean optimizeLocations) {
		this.optimizeLocations = optimizeLocations;
	}

	/**
	 * 返回是否通过在启动时对指定的位置进行存在性检查来优化它们，从而提前过滤掉不存在的目录，以便不必在每次资源访问时检查它们。
	 *
	 * @since 5.3.13
	 */
	public boolean isOptimizeLocations() {
		return this.optimizeLocations;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		resolveResourceLocations();

		if (this.resourceResolvers.isEmpty()) {
			this.resourceResolvers.add(new PathResourceResolver());
		}

		initAllowedLocations();

		// Initialize immutable resolver and transformer chains
		this.resolverChain = new DefaultResourceResolverChain(this.resourceResolvers);
		this.transformerChain = new DefaultResourceTransformerChain(this.resolverChain, this.resourceTransformers);

		if (this.resourceHttpMessageConverter == null) {
			this.resourceHttpMessageConverter = new ResourceHttpMessageConverter();
		}
		if (this.resourceRegionHttpMessageConverter == null) {
			this.resourceRegionHttpMessageConverter = new ResourceRegionHttpMessageConverter();
		}

		ContentNegotiationManager manager = getContentNegotiationManager();
		if (manager != null) {
			setMediaTypes(manager.getMediaTypeMappings());
		}

		@SuppressWarnings("deprecation")
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy =
				initContentNegotiationStrategy();
		if (strategy != null) {
			setMediaTypes(strategy.getMediaTypes());
		}
	}

	private void resolveResourceLocations() {
		List<Resource> result = new ArrayList<>();
		if (!this.locationValues.isEmpty()) {
			ApplicationContext applicationContext = obtainApplicationContext();
			for (String location : this.locationValues) {
				if (this.embeddedValueResolver != null) {
					String resolvedLocation = this.embeddedValueResolver.resolveStringValue(location);
					if (resolvedLocation == null) {
						throw new IllegalArgumentException("Location resolved to null: " + location);
					}
					location = resolvedLocation;
				}
				Charset charset = null;
				location = location.trim();
				if (location.startsWith(URL_RESOURCE_CHARSET_PREFIX)) {
					int endIndex = location.indexOf(']', URL_RESOURCE_CHARSET_PREFIX.length());
					if (endIndex == -1) {
						throw new IllegalArgumentException("Invalid charset syntax in location: " + location);
					}
					String value = location.substring(URL_RESOURCE_CHARSET_PREFIX.length(), endIndex);
					charset = Charset.forName(value);
					location = location.substring(endIndex + 1);
				}
				Resource resource = applicationContext.getResource(location);
				if (location.equals("/") && !(resource instanceof ServletContextResource)) {
					throw new IllegalStateException(
							"The String-based location \"/\" should be relative to the web application root " +
									"but resolved to a Resource of type: " + resource.getClass() + ". " +
									"If this is intentional, please pass it as a pre-configured Resource via setLocations.");
				}
				result.add(resource);
				if (charset != null) {
					if (!(resource instanceof UrlResource)) {
						throw new IllegalArgumentException("Unexpected charset for non-UrlResource: " + resource);
					}
					this.locationCharsets.put(resource, charset);
				}
			}
		}

		result.addAll(this.locationResources);
		if (isOptimizeLocations()) {
			result = result.stream().filter(Resource::exists).collect(Collectors.toList());
		}

		this.locationsToUse.clear();
		this.locationsToUse.addAll(result);
	}

	/**
	 * 在配置的资源解析器中查找 {@code PathResourceResolver}，并设置其 {@code allowedLocations} 属性（如果为空）以匹配此类配置的 {@link #setLocations 位置}。
	 */
	protected void initAllowedLocations() {
		if (CollectionUtils.isEmpty(getLocations())) {
			return;
		}
		for (int i = getResourceResolvers().size() - 1; i >= 0; i--) {
			if (getResourceResolvers().get(i) instanceof PathResourceResolver) {
				PathResourceResolver pathResolver = (PathResourceResolver) getResourceResolvers().get(i);
				if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
					pathResolver.setAllowedLocations(getLocations().toArray(new Resource[0]));
				}
				if (this.urlPathHelper != null) {
					pathResolver.setLocationCharsets(this.locationCharsets);
					pathResolver.setUrlPathHelper(this.urlPathHelper);
				}
				break;
			}
		}
	}

	/**
	 * 初始化用于确定资源的媒体类型的策略。
	 *
	 * @deprecated 自 5.2.4 版本起，此方法返回 {@code null}，如果子类返回实际实例，则该实例仅用作媒体类型映射的源（如果包含任何）。请改用 {@link #setMediaTypes(Map)}，或者如果需要更改行为，则可以覆盖 {@link #getMediaType(HttpServletRequest, Resource)}。
	 */
	@Nullable
	@Deprecated
	@SuppressWarnings("deprecation")
	protected org.springframework.web.accept.PathExtensionContentNegotiationStrategy initContentNegotiationStrategy() {
		return null;
	}


	/**
	 * 处理资源请求。
	 * <p>检查请求的资源是否存在于配置的位置列表中。如果资源不存在，则将返回 {@code 404} 响应给客户端。
	 * 如果资源存在，则将检查请求中是否存在 {@code Last-Modified} 标头，如果存在，则将其值与给定资源的最后修改时间戳进行比较，如果 {@code Last-Modified} 值大于资源的最后修改时间戳，则返回 {@code 304} 状态码。
	 * 如果资源新于 {@code Last-Modified} 值，或者标头不存在，则将资源的内容写入响应，并将缓存头设置为在将来的一年内过期。
	 */
	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// For very general mappings (e.g. "/") we need to check 404 first
		Resource resource = getResource(request);
		if (resource == null) {
			logger.debug("Resource not found");
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return;
		}

		// Supported methods and required session
		checkRequest(request);

		// Header phase
		if (isUseLastModified() && new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
			logger.trace("Resource not modified");
			return;
		}

		// Apply cache settings, if any
		prepareResponse(response);

		// Check the media type for the resource
		MediaType mediaType = getMediaType(request, resource);
		setHeaders(response, resource, mediaType);

		// Content phase
		ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
		if (request.getHeader(HttpHeaders.RANGE) == null) {
			Assert.state(this.resourceHttpMessageConverter != null, "Not initialized");
			this.resourceHttpMessageConverter.write(resource, mediaType, outputMessage);
		} else {
			Assert.state(this.resourceRegionHttpMessageConverter != null, "Not initialized");
			ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);
			try {
				List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				this.resourceRegionHttpMessageConverter.write(
						HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
			} catch (IllegalArgumentException ex) {
				response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			}
		}
	}

	@Nullable
	protected Resource getResource(HttpServletRequest request) throws IOException {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '" +
					HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}

		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return null;
		}
		if (isInvalidEncodedPath(path)) {
			return null;
		}

		Assert.notNull(this.resolverChain, "ResourceResolverChain not initialized.");
		Assert.notNull(this.transformerChain, "ResourceTransformerChain not initialized.");

		Resource resource = this.resolverChain.resolveResource(request, path, getLocations());
		if (resource != null) {
			resource = this.transformerChain.transform(request, resource);
		}
		return resource;
	}

	/**
	 * 处理给定的资源路径。
	 * <p>默认实现替换：
	 * <ul>
	 * <li>反斜杠为正斜杠。
	 * <li>斜杠的重复出现为单个斜杠。
	 * <li>以及前导斜杠和控制字符（00-1F 和 7F）的任何组合为单个 "/" 或 ""。例如 {@code "  / // foo/bar"} 变成 {@code "/foo/bar"}。
	 * </ul>
	 *
	 * @since 3.2.12
	 */
	protected String processPath(String path) {
		path = StringUtils.replace(path, "\\", "/");
		path = cleanDuplicateSlashes(path);
		return cleanLeadingSlash(path);
	}

	private String cleanDuplicateSlashes(String path) {
		StringBuilder sb = null;
		char prev = 0;
		for (int i = 0; i < path.length(); i++) {
			char curr = path.charAt(i);
			try {
				if ((curr == '/') && (prev == '/')) {
					if (sb == null) {
						sb = new StringBuilder(path.substring(0, i));
					}
					continue;
				}
				if (sb != null) {
					sb.append(path.charAt(i));
				}
			} finally {
				prev = curr;
			}
		}
		return sb != null ? sb.toString() : path;
	}

	private String cleanLeadingSlash(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			} else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				return (slash ? "/" + path.substring(i) : path.substring(i));
			}
		}
		return (slash ? "/" : "");
	}

	/**
	 * 检查给定路径是否包含无效的转义序列。
	 *
	 * @param path 要验证的路径
	 * @return 如果路径无效，则返回 {@code true}，否则返回 {@code false}
	 */
	private boolean isInvalidEncodedPath(String path) {
		if (path.contains("%")) {
			try {
				// 使用 URLDecoder（而不是 UriUtils）以保留可能已解码的 UTF-8 字符
				String decodedPath = URLDecoder.decode(path, "UTF-8");
				if (isInvalidPath(decodedPath)) {
					return true;
				}
				decodedPath = processPath(decodedPath);
				if (isInvalidPath(decodedPath)) {
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
	 * 识别无效的资源路径。默认情况下拒绝：
	 * <ul>
	 * <li>包含 "WEB-INF" 或 "META-INF" 的路径
	 * <li>{@link org.springframework.util.StringUtils#cleanPath} 调用后包含 "../" 的路径
	 * <li>表示 {@link org.springframework.util.ResourceUtils#isUrl 有效 URL} 或在移除前导斜杠后将表示有效 URL 的路径
	 * </ul>
	 * <p><strong>注意：</strong>此方法假定前导、重复的 '/' 或控制字符（例如空白字符）已被修剪，
	 * 以便路径可预测地以单个 '/' 开头或不以 '/' 开头。
	 *
	 * @param path 要验证的路径
	 * @return 如果路径无效，则返回 {@code true}，否则返回 {@code false}
	 * @since 3.0.6
	 */
	protected boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path with \"WEB-INF\" or \"META-INF\": [" + path + "]", -1, true));
			}
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				if (logger.isWarnEnabled()) {
					logger.warn(LogFormatUtils.formatValue(
							"Path represents URL or has \"url:\" prefix: [" + path + "]", -1, true));
				}
				return true;
			}
		}
		if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]", -1, true));
			}
			return true;
		}
		return false;
	}

	/**
	 * 确定请求和匹配到的资源的媒体类型。此实现尝试使用以下查找之一来确定 MediaType：
	 * <ol>
	 * <li>{@link javax.servlet.ServletContext#getMimeType(String)}
	 * <li>{@link #getMediaTypes()}
	 * <li>{@link MediaTypeFactory#getMediaTypes(String)}
	 * </ol>
	 *
	 * @param request  当前请求
	 * @param resource 要检查的资源
	 * @return 对应的媒体类型；如果未找到，则返回 {@code null}
	 */
	@Nullable
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		MediaType result = null;
		String mimeType = request.getServletContext().getMimeType(resource.getFilename());
		if (StringUtils.hasText(mimeType)) {
			result = MediaType.parseMediaType(mimeType);
		}
		if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
			MediaType mediaType = null;
			String filename = resource.getFilename();
			String ext = StringUtils.getFilenameExtension(filename);
			if (ext != null) {
				mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
			}
			if (mediaType == null) {
				List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
				if (!CollectionUtils.isEmpty(mediaTypes)) {
					mediaType = mediaTypes.get(0);
				}
			}
			if (mediaType != null) {
				result = mediaType;
			}
		}
		return result;
	}

	/**
	 * 在给定的 Servlet 响应上设置头信息。
	 * 用于处理 GET 请求和 HEAD 请求。
	 *
	 * @param response  当前的 Servlet 响应
	 * @param resource  已识别的资源（永远不会为 {@code null}）
	 * @param mediaType 资源的媒体类型（永远不会为 {@code null}）
	 * @throws IOException 在设置头信息时出现错误时抛出
	 */
	protected void setHeaders(HttpServletResponse response, Resource resource, @Nullable MediaType mediaType)
			throws IOException {

		if (mediaType != null) {
			response.setContentType(mediaType.toString());
		}

		if (resource instanceof HttpResource) {
			HttpHeaders resourceHeaders = ((HttpResource) resource).getResponseHeaders();
			resourceHeaders.forEach((headerName, headerValues) -> {
				boolean first = true;
				for (String headerValue : headerValues) {
					if (first) {
						response.setHeader(headerName, headerValue);
					} else {
						response.addHeader(headerName, headerValue);
					}
					first = false;
				}
			});
		}

		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
	}


	@Override
	public String toString() {
		return "ResourceHttpRequestHandler " + locationToString(getLocations());
	}

	private String locationToString(List<Resource> locations) {
		return locations.toString()
				.replaceAll("class path resource", "classpath")
				.replaceAll("ServletContext resource", "ServletContext");
	}

}
