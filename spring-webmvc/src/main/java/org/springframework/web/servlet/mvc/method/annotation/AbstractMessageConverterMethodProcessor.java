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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * AbstractMessageConverterMethodProcessor 扩展了 AbstractMessageConverterMethodArgumentResolver，
 * 具有使用 HttpMessageConverter 写入响应的方法返回值的能力。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractMessageConverterMethodProcessor extends AbstractMessageConverterMethodArgumentResolver
		implements HandlerMethodReturnValueHandler {

	/**
	 * 与内置消息转换器关联的扩展
	 */
	private static final Set<String> SAFE_EXTENSIONS = new HashSet<>(Arrays.asList(
			"txt", "text", "yml", "properties", "csv",
			"json", "xml", "atom", "rss",
			"png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp"));

	/**
	 * 安全媒体基本类型
	 */
	private static final Set<String> SAFE_MEDIA_BASE_TYPES = new HashSet<>(
			Arrays.asList("audio", "image", "video"));

	/**
	 * 所有应用媒体类型列表
	 */
	private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES =
			Arrays.asList(MediaType.ALL, new MediaType("application"));

	/**
	 * 资源区域列表类型
	 */
	private static final Type RESOURCE_REGION_LIST_TYPE =
			new ParameterizedTypeReference<List<ResourceRegion>>() {
			}.getType();


	/**
	 * 内容协商管理器
	 */
	private final ContentNegotiationManager contentNegotiationManager;

	/**
	 * 安全扩展名集合
	 */
	private final Set<String> safeExtensions = new HashSet<>();


	/**
	 * 仅包含转换器列表的构造函数。
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters) {
		this(converters, null, null);
	}

	/**
	 * 带有转换器列表和 ContentNegotiationManager 的构造函数。
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
													  @Nullable ContentNegotiationManager contentNegotiationManager) {

		this(converters, contentNegotiationManager, null);
	}

	/**
	 * 带有转换器列表、ContentNegotiationManager以及请求/响应体建议实例的构造函数。
	 */
	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
													  @Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {

		super(converters, requestResponseBodyAdvice);

		this.contentNegotiationManager = (manager != null ? manager : new ContentNegotiationManager());
		this.safeExtensions.addAll(this.contentNegotiationManager.getAllFileExtensions());
		this.safeExtensions.addAll(SAFE_EXTENSIONS);
	}


	/**
	 * 从给定的 {@link NativeWebRequest} 创建一个新的 {@link HttpOutputMessage}。
	 *
	 * @param webRequest 要创建输出消息的网络请求
	 * @return 输出消息
	 */
	protected ServletServerHttpResponse createOutputMessage(NativeWebRequest webRequest) {
		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		Assert.state(response != null, "No HttpServletResponse");
		return new ServletServerHttpResponse(response);
	}

	/**
	 * 将给定的返回值写入给定的 Web 请求。委托给 {@link #writeWithMessageConverters(Object, MethodParameter, ServletServerHttpRequest, ServletServerHttpResponse)}
	 */
	protected <T> void writeWithMessageConverters(T value, MethodParameter returnType, NativeWebRequest webRequest)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		// 创建输入的 ServletServerHttpRequest
		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		// 创建输出的 ServletServerHttpResponse
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);
		// 使用消息转换器将值写入输出消息
		writeWithMessageConverters(value, returnType, inputMessage, outputMessage);
	}

	/**
	 * 将给定的返回值写入给定的输出消息。
	 *
	 * @param value         要写入输出消息的值
	 * @param returnType    值的类型
	 * @param inputMessage  输入消息。用于检查 {@code Accept} 标头。
	 * @param outputMessage 要写入的输出消息
	 * @throws IOException                         发生 I/O 错误时抛出
	 * @throws HttpMediaTypeNotAcceptableException 如果请求的 {@code Accept} 标头指示的条件
	 *                                             无法满足消息转换器
	 * @throws HttpMessageNotWritableException     如果给定的消息无法
	 *                                             被转换器写入，或者服务器选择的内容类型
	 *                                             没有兼容的转换器时抛出
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected <T> void writeWithMessageConverters(@Nullable T value, MethodParameter returnType,
												  ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)
			throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

		// 响应体
		Object body;
		// 值类型
		Class<?> valueType;
		// 目标类型
		Type targetType;

		// 如果值是 CharSequence 类型
		if (value instanceof CharSequence) {
			// 转换为字符串
			body = value.toString();
			// 设置值类型为 String
			valueType = String.class;
			// 设置目标类型为 String
			targetType = String.class;
		} else {
			// 否则直接赋值
			body = value;
			// 获取返回值类型
			valueType = getReturnValueType(body, returnType);
			// 解析目标类型
			targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
		}

		// 如果返回值是资源类型
		if (isResourceType(value, returnType)) {
			// 设置响应头中的 Accept-Ranges 为 bytes
			outputMessage.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");
			// 如果请求头中包含 Range，并且响应状态码是 200
			if (value != null && inputMessage.getHeaders().getFirst(HttpHeaders.RANGE) != null &&
					outputMessage.getServletResponse().getStatus() == 200) {
				// 获取资源对象
				Resource resource = (Resource) value;
				try {
					// 获取请求头中的 HttpRange
					List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();
					// 设置响应状态码为 206
					outputMessage.getServletResponse().setStatus(HttpStatus.PARTIAL_CONTENT.value());
					// 将 HttpRange 转换为 ResourceRegion
					body = HttpRange.toResourceRegions(httpRanges, resource);
					// 设置值类型为 ResourceRegion
					valueType = body.getClass();
					// 设置目标类型为 资源区域列表类型
					targetType = RESOURCE_REGION_LIST_TYPE;
				} catch (IllegalArgumentException ex) {
					// 设置响应头中的 Content-Range
					outputMessage.getHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
					// 设置响应状态码为 416
					outputMessage.getServletResponse().setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
				}
			}
		}

		MediaType selectedMediaType = null;
		// 获取响应消息头中的 内容类型
		MediaType contentType = outputMessage.getHeaders().getContentType();
		boolean isContentTypePreset = contentType != null && contentType.isConcrete();
		// 如果 内容类型 已经设置
		if (isContentTypePreset) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found 'Content-Type:" + contentType + "' in response");
			}
			// 使用设置的 内容类型
			selectedMediaType = contentType;
		} else {
			// 获取请求对象
			HttpServletRequest request = inputMessage.getServletRequest();
			// 获取请求可接受的媒体类型
			List<MediaType> acceptableTypes;
			try {
				acceptableTypes = getAcceptableMediaTypes(request);
			} catch (HttpMediaTypeNotAcceptableException ex) {
				// 如果请求不可接受，且返回值为 null 或状态码为 4xx 或 5xx，则忽略异常
				int series = outputMessage.getServletResponse().getStatus() / 100;
				if (body == null || series == 4 || series == 5) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring error response content (if any). " + ex);
					}
					return;
				}
				throw ex;
			}
			// 获取可生产的媒体类型
			List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);
			// 如果返回值不为空但可生产的类型为空，抛出异常
			if (body != null && producibleTypes.isEmpty()) {
				throw new HttpMessageNotWritableException(
						"No converter found for return value of type: " + valueType);
			}
			// 获取可用的媒体类型
			List<MediaType> mediaTypesToUse = new ArrayList<>();
			for (MediaType requestedType : acceptableTypes) {
				for (MediaType producibleType : producibleTypes) {
					if (requestedType.isCompatibleWith(producibleType)) {
						mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
					}
				}
			}
			// 如果可用的媒体类型为空
			if (mediaTypesToUse.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("No match for " + acceptableTypes + ", supported: " + producibleTypes);
				}
				// 如果返回值不为空，抛出异常
				if (body != null) {
					throw new HttpMediaTypeNotAcceptableException(producibleTypes);
				}
				return;
			}
			// 将可用的媒体类型排序
			MediaType.sortBySpecificityAndQuality(mediaTypesToUse);
			// 选择最匹配的媒体类型
			// 遍历可用的媒体类型列表
			for (MediaType mediaType : mediaTypesToUse) {
				// 如果媒体类型是具体的
				if (mediaType.isConcrete()) {
					// 选中该媒体类型
					selectedMediaType = mediaType;
					// 结束循环
					break;
				} else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
					// 如果媒体类型存在于所有应用程序媒体类型中
					// 选中应用程序八位字节流类型
					selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
					// 结束循环
					break;
				}
			}

			// 如果调试日志启用，记录所使用的媒体类型
			if (logger.isDebugEnabled()) {
				logger.debug("Using '" + selectedMediaType + "', given " +
						acceptableTypes + " and supported " + producibleTypes);
			}
		}

		// 如果选择了媒体类型
		if (selectedMediaType != null) {
			// 从选择的媒体类型中移除 质量值
			selectedMediaType = selectedMediaType.removeQualityValue();
			// 遍历消息转换器
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				GenericHttpMessageConverter genericConverter = (converter instanceof GenericHttpMessageConverter ?
						(GenericHttpMessageConverter<?>) converter : null);
				// 如果是泛型转换器
				if (genericConverter != null ?
						((GenericHttpMessageConverter) converter).canWrite(targetType, valueType, selectedMediaType) :
						converter.canWrite(valueType, selectedMediaType)) {
					// 在写入响应体前调用通知方法
					body = getAdvice().beforeBodyWrite(body, returnType, selectedMediaType,
							(Class<? extends HttpMessageConverter<?>>) converter.getClass(),
							inputMessage, outputMessage);
					// 如果响应体不为空
					if (body != null) {
						// 记录日志
						Object theBody = body;
						LogFormatUtils.traceDebug(logger, traceOn ->
								"Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");
						// 添加内容协商头
						addContentDispositionHeader(inputMessage, outputMessage);
						// 如果是泛型转换器，调用泛型写方法，否则调用普通写方法
						if (genericConverter != null) {
							genericConverter.write(body, targetType, selectedMediaType, outputMessage);
						} else {
							((HttpMessageConverter) converter).write(body, selectedMediaType, outputMessage);
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Nothing to write: null body");
						}
					}
					return;
				}
			}
		}

		// 如果响应体不为空
		if (body != null) {
			// 获取请求中可生产的媒体类型
			Set<MediaType> producibleMediaTypes =
					(Set<MediaType>) inputMessage.getServletRequest()
							.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
			// 如果已设置 内容类型 或者可生产的媒体类型不为空
			if (isContentTypePreset || !CollectionUtils.isEmpty(producibleMediaTypes)) {
				// 抛出异常
				throw new HttpMessageNotWritableException(
						"No converter for [" + valueType + "] with preset Content-Type '" + contentType + "'");
			}
			// 否则，抛出不可接受的媒体类型异常
			throw new HttpMediaTypeNotAcceptableException(getSupportedMediaTypes(body.getClass()));
		}
	}

	/**
	 * 返回要写入响应的值的类型。通常这是通过对值调用 getClass 进行简单检查，但如果值为 null，
	 * 则需要检查返回类型，可能包括泛型类型确定（例如 {@code ResponseEntity<T>}）。
	 */
	protected Class<?> getReturnValueType(@Nullable Object value, MethodParameter returnType) {
		// 如果值不为 null，则返回其类型；否则返回方法参数的类型
		return (value != null ? value.getClass() : returnType.getParameterType());
	}

	/**
	 * 返回返回值或声明的返回类型是否扩展了 {@link Resource}。
	 */
	protected boolean isResourceType(@Nullable Object value, MethodParameter returnType) {
		// 获取返回值类型
		Class<?> clazz = getReturnValueType(value, returnType);
		// 如果类型是 InputStreamResource，并且返回值类型是 Resource及其子类
		return clazz != InputStreamResource.class && Resource.class.isAssignableFrom(clazz);
	}

	/**
	 * 返回 {@code returnType} 的泛型类型（如果它是 {@link HttpEntity} 的话返回嵌套类型）。
	 */
	private Type getGenericType(MethodParameter returnType) {
		// 如果返回类型是 HttpEntity 或其子类，则返回其泛型类型
		if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
			return ResolvableType.forType(returnType.getGenericParameterType()).getGeneric().getType();
		} else {
			// 否则，返回返回类型的泛型参数类型
			return returnType.getGenericParameterType();
		}
	}

	/**
	 * 返回可以生成的媒体类型。
	 *
	 * @see #getProducibleMediaTypes(HttpServletRequest, Class, Type)
	 */
	@SuppressWarnings("unused")
	protected List<MediaType> getProducibleMediaTypes(HttpServletRequest request, Class<?> valueClass) {
		return getProducibleMediaTypes(request, valueClass, null);
	}

	/**
	 * 返回可以生成的媒体类型。结果媒体类型为：
	 * <ul>
	 * <li>请求映射中指定的可生成媒体类型，或者
	 * <li>可以写入特定返回值的配置转换器的媒体类型，或者
	 * <li>{@link MediaType#ALL}
	 * </ul>
	 *
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	protected List<MediaType> getProducibleMediaTypes(
			HttpServletRequest request, Class<?> valueClass, @Nullable Type targetType) {

		// 获取可生产的媒体类型集合
		Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		// 如果媒体类型集合不为空，则返回其副本
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<>(mediaTypes);
		}
		// 如果媒体类型集合为空，则遍历消息转换器列表以获取支持的媒体类型
		List<MediaType> result = new ArrayList<>();
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter instanceof GenericHttpMessageConverter && targetType != null) {
				// 如果转换器是通用 HTTP 消息转换器且目标类型不为空，则检查是否可以写入
				if (((GenericHttpMessageConverter<?>) converter).canWrite(targetType, valueClass, null)) {
					// 如果可以写入，则添加可以支持的媒体类型
					result.addAll(converter.getSupportedMediaTypes(valueClass));
				}
			} else if (converter.canWrite(valueClass, null)) {
				// 如果转换器可以写入，添加可以支持的媒体类型
				result.addAll(converter.getSupportedMediaTypes(valueClass));
			}
		}
		// 如果没有支持的媒体类型，则返回包含 MediaType.ALL 的列表
		return (result.isEmpty() ? Collections.singletonList(MediaType.ALL) : result);
	}

	private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request)
			throws HttpMediaTypeNotAcceptableException {
		// 使用内容协商器解析媒体类型
		return this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
	}

	/**
	 * 返回可接受和可生成媒体类型中更具体的一个，并带有前者的 q 值。
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceTypeToUse) <= 0 ? acceptType : produceTypeToUse);
	}

	/**
	 * 检查路径是否具有文件扩展名，以及扩展名是否在 {@link #SAFE_EXTENSIONS 安全扩展名} 列表中或者显式地 {@link ContentNegotiationManager#getAllFileExtensions() 注册}。
	 * 如果不是，并且状态在 2xx 范围内，则添加一个 'Content-Disposition' 头，其具有安全的附件文件名（"f.txt"）以防止 RFD 攻击。
	 */
	private void addContentDispositionHeader(ServletServerHttpRequest request, ServletServerHttpResponse response) {
		// 获取响应头部
		HttpHeaders headers = response.getHeaders();
		// 如果响应头部包含 Content-Disposition，则直接返回，不做处理
		if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			return;
		}

		try {
			// 获取响应状态码
			int status = response.getServletResponse().getStatus();
			// 如果状态码小于 200 或者大于 299 且小于 400，则直接返回，不做处理
			if (status < 200 || (status > 299 && status < 400)) {
				return;
			}
		} catch (Throwable ex) {
			// 忽略异常
		}

		// 获取原始请求 URI
		HttpServletRequest servletRequest = request.getServletRequest();
		String requestUri = UrlPathHelper.rawPathInstance.getOriginatingRequestUri(servletRequest);

		// 获取文件名和路径参数
		int index = requestUri.lastIndexOf('/') + 1;
		String filename = requestUri.substring(index);
		String pathParams = "";

		// 处理文件名中的路径参数
		index = filename.indexOf(';');
		if (index != -1) {
			// 获取路径参数
			pathParams = filename.substring(index);
			// 获取文件名
			filename = filename.substring(0, index);
		}

		// 解码文件名
		filename = UrlPathHelper.defaultInstance.decodeRequestString(servletRequest, filename);
		// 获取文件扩展名
		String ext = StringUtils.getFilenameExtension(filename);

		// 解码路径参数并获取其中的扩展名
		pathParams = UrlPathHelper.defaultInstance.decodeRequestString(servletRequest, pathParams);
		// 获取文件扩展名
		String extInPathParams = StringUtils.getFilenameExtension(pathParams);

		if (!safeExtension(servletRequest, ext) || !safeExtension(servletRequest, extInPathParams)) {
			// 如果文件扩展名或路径参数中的扩展名不安全，则设置默认的 Content-Disposition 头部
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
		}
	}

	@SuppressWarnings("unchecked")
	private boolean safeExtension(HttpServletRequest request, @Nullable String extension) {
		// 如果文件扩展名为空，则认为是安全的
		if (!StringUtils.hasText(extension)) {
			return true;
		}
		// 将扩展名转换为小写
		extension = extension.toLowerCase(Locale.ENGLISH);
		// 如果扩展名在安全扩展名集合中，则认为是安全的
		if (this.safeExtensions.contains(extension)) {
			return true;
		}
		// 获取请求的最佳匹配模式
		String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		// 如果最佳匹配模式以该扩展名结尾，则认为是安全的
		if (pattern != null && pattern.endsWith("." + extension)) {
			return true;
		}
		// 如果扩展名为 html
		if (extension.equals("html")) {
			// 获取可生产的媒体类型集合
			String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
			Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(name);
			// 如果媒体类型集合不为空且包含 TEXT_HTML 类型，则认为是安全的
			if (!CollectionUtils.isEmpty(mediaTypes) && mediaTypes.contains(MediaType.TEXT_HTML)) {
				return true;
			}
		}
		// 解析媒体类型
		MediaType mediaType = resolveMediaType(request, extension);
		// 如果媒体类型不为空且是安全的媒体类型，则认为是安全的
		return (mediaType != null && (safeMediaType(mediaType)));
	}

	@Nullable
	private MediaType resolveMediaType(ServletRequest request, String extension) {
		// 初始化结果为 null
		MediaType result = null;
		// 从 Servlet上下文 获取文件扩展名对应的 MIME 类型
		String rawMimeType = request.getServletContext().getMimeType("file." + extension);
		if (StringUtils.hasText(rawMimeType)) {
			// 如果获取到的 MIME 类型不为空
			// 解析 MIME 类型字符串为 MediaType 对象
			result = MediaType.parseMediaType(rawMimeType);
		}
		// 如果结果仍为 null 或者是 APPLICATION_OCTET_STREAM 类型
		if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
			// 从 媒体类型工厂 获取文件扩展名对应的 媒体类型，如果不存在则返回 null
			result = MediaTypeFactory.getMediaType("file." + extension).orElse(null);
		}
		// 返回解析得到的 媒体类型
		return result;
	}

	private boolean safeMediaType(MediaType mediaType) {
		// 如果媒体类型是安全媒体类型，或者是子类型是+xml结尾
		return (SAFE_MEDIA_BASE_TYPES.contains(mediaType.getType()) ||
				mediaType.getSubtype().endsWith("+xml"));
	}

}
