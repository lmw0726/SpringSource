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

package org.springframework.http.converter;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 实现 {@link HttpMessageConverter} 来读取和写入“普通”HTML表单，并且还可以写入（但不读取）多部分数据（例如文件上传）。
 *
 * <p>换句话说，此转换器可以读取和写入 {@code "application/x-www-form-urlencoded"} 媒体类型作为
 * {@link MultiValueMap MultiValueMap&lt;String, String&gt;}, 并且它还可以写入（但不读取）
 * {@code "multipart/form-data"} 和 {@code "multipart/mixed"} 媒体类型作为
 * {@link MultiValueMap MultiValueMap&lt;String, Object&gt;}.
 *
 * <h3>多部分数据</h3>
 *
 * <p>默认情况下，当 {@linkplain #write 写入} 多部分数据时，使用 {@code "multipart/form-data"} 作为内容类型。
 * 从 Spring Framework 5.2 开始，还可以使用其他多部分子类型（例如 {@code "multipart/mixed"} 和
 * {@code "multipart/related"}）写入多部分数据，只要多部分子类型注册为 {@linkplain #getSupportedMediaTypes
 * 支持的媒体类型} <em>并且</em> 指定所需的多部分子类型作为内容类型在 {@linkplain #write 写入} 多部分数据时。
 * 请注意，默认情况下，{@code "multipart/mixed"} 注册为支持的媒体类型。
 *
 * <p>在写入多部分数据时，此转换器使用其他 {@link HttpMessageConverter HttpMessageConverters} 来写入各个 MIME 部分。
 * 默认情况下，基本转换器注册用于字节数组、{@code String} 和 {@code Resource}。这些可以通过
 * {@link #setPartConverters} 覆盖或通过 {@link #addPartConverter} 增强。
 *
 * <h3>示例</h3>
 *
 * <p>以下代码片段显示如何使用 {@code "multipart/form-data"} 内容类型提交 HTML 表单。
 *
 * <pre class="code">
 * RestTemplate restTemplate = new RestTemplate();
 * // 默认情况下配置 AllEncompassingFormHttpMessageConverter
 *
 * MultiValueMap&lt;String, Object&gt; form = new LinkedMultiValueMap&lt;&gt;();
 * form.add("field 1", "value 1");
 * form.add("field 2", "value 2");
 * form.add("field 2", "value 3");
 * form.add("field 3", 4);  // 从 5.1.4 开始支持非字符串表单值
 *
 * restTemplate.postForLocation("https://example.com/myForm", form);</pre>
 *
 * <p>以下代码片段显示如何使用 {@code "multipart/form-data"} 内容类型上传文件。
 *
 * <pre class="code">
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * restTemplate.postForLocation("https://example.com/myFileUpload", parts);</pre>
 *
 * <p>以下代码片段显示如何使用 {@code "multipart/mixed"} 内容类型上传文件。
 *
 * <pre class="code">
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * HttpHeaders requestHeaders = new HttpHeaders();
 * requestHeaders.setContentType(MediaType.MULTIPART_MIXED);
 *
 * restTemplate.postForLocation("https://example.com/myFileUpload",
 *     new HttpEntity&lt;&gt;(parts, requestHeaders));</pre>
 *
 * <p>以下代码片段显示如何使用 {@code "multipart/related"} 内容类型上传文件。
 *
 * <pre class="code">
 * MediaType multipartRelated = new MediaType("multipart", "related");
 *
 * restTemplate.getMessageConverters().stream()
 *     .filter(FormHttpMessageConverter.class::isInstance)
 *     .map(FormHttpMessageConverter.class::cast)
 *     .findFirst()
 *     .orElseThrow(() -&gt; new IllegalStateException("未找到 FormHttpMessageConverter"))
 *     .addSupportedMediaTypes(multipartRelated);
 *
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 *
 * HttpHeaders requestHeaders = new HttpHeaders();
 * requestHeaders.setContentType(multipartRelated);
 *
 * restTemplate.postForLocation("https://example.com/myFileUpload",
 *     new HttpEntity&lt;&gt;(parts, requestHeaders));</pre>
 *
 * <h3>其他事项</h3>
 *
 * <p>此类中的某些方法受到 {@code org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity} 的启发。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter
 * @see org.springframework.util.MultiValueMap
 * @since 3.0
 */
public class FormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 默认表单数据媒体类型
	 */
	private static final MediaType DEFAULT_FORM_DATA_MEDIA_TYPE =
			new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET);

	/**
	 * 支持的媒体类型列表
	 */
	private List<MediaType> supportedMediaTypes = new ArrayList<>();

	/**
	 * 部分转换器列表
	 */
	private List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

	/**
	 * 字符集
	 */
	private Charset charset = DEFAULT_CHARSET;

	/**
	 * 多部分字符集
	 */
	@Nullable
	private Charset multipartCharset;


	public FormHttpMessageConverter() {
		// 添加支持的媒体类型：应用程序/x-www-form-urlencoded
		this.supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
		// 添加支持的媒体类型：多部分表单数据
		this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
		// 添加支持的媒体类型：多部分混合
		this.supportedMediaTypes.add(MediaType.MULTIPART_MIXED);

		// 添加部件转换器：字节数组 HTTP 消息转换器
		this.partConverters.add(new ByteArrayHttpMessageConverter());
		// 添加部件转换器：字符串 HTTP 消息转换器
		this.partConverters.add(new StringHttpMessageConverter());
		// 添加部件转换器：资源 HTTP 消息转换器
		this.partConverters.add(new ResourceHttpMessageConverter());

		// 应用默认字符集设置
		applyDefaultCharset();
	}


	/**
	 * 设置此转换器支持的 {@link MediaType} 对象列表。
	 *
	 * @see #addSupportedMediaTypes(MediaType...)
	 * @see #getSupportedMediaTypes()
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notNull(supportedMediaTypes, "'supportedMediaTypes' must not be null");
		// 确保内部列表是可变的。
		this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
	}

	/**
	 * 添加此转换器支持的 {@link MediaType} 对象。
	 * <p>提供的 {@code MediaType} 对象将被附加到 {@linkplain #getSupportedMediaTypes() 支持的 MediaType 对象} 列表中。
	 *
	 * @param supportedMediaTypes 要添加的一组 {@code MediaType} 对象
	 * @see #setSupportedMediaTypes(List)
	 * @since 5.2
	 */
	public void addSupportedMediaTypes(MediaType... supportedMediaTypes) {
		Assert.notNull(supportedMediaTypes, "'supportedMediaTypes' must not be null");
		Assert.noNullElements(supportedMediaTypes, "'supportedMediaTypes' must not contain null elements");
		Collections.addAll(this.supportedMediaTypes, supportedMediaTypes);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #setSupportedMediaTypes(List)
	 * @see #addSupportedMediaTypes(MediaType...)
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * 设置要使用的消息体转换器。这些转换器用于将对象转换为 MIME 部分。
	 */
	public void setPartConverters(List<HttpMessageConverter<?>> partConverters) {
		Assert.notEmpty(partConverters, "'partConverters' must not be empty");
		this.partConverters = partConverters;
	}

	/**
	 * 返回配置的 MIME 部分的转换器。
	 *
	 * @since 5.3
	 */
	public List<HttpMessageConverter<?>> getPartConverters() {
		return Collections.unmodifiableList(this.partConverters);
	}

	/**
	 * 添加消息体转换器。此类转换器用于将对象转换为 MIME 部分。
	 */
	public void addPartConverter(HttpMessageConverter<?> partConverter) {
		Assert.notNull(partConverter, "'partConverter' must not be null");
		this.partConverters.add(partConverter);
	}

	/**
	 * 设置在请求或响应的 {@code Content-Type} 头未明确指定时用于读取和写入表单数据的默认字符集。
	 * <p>从 4.3 开始，这也用于在多部分请求中转换文本主体的默认字符集。
	 * <p>从 5.0 开始，这也用于部分头包括 {@code Content-Disposition}（及其文件名参数），
	 * 除非还设置了（互斥的）{@link #setMultipartCharset multipartCharset}，在这种情况下部分头以 ASCII 编码，
	 * 并且 <i>文件名</i> 使用 RFC 2047 的 {@code encoded-word} 语法编码。
	 * <p>默认情况下，此设置为 "UTF-8"。
	 */
	public void setCharset(@Nullable Charset charset) {
		// 如果给定的字符集不等于当前字符集
		if (charset != this.charset) {
			// 更新当前字符集为给定字符集，如果给定字符集为 null，则使用默认字符集
			this.charset = (charset != null ? charset : DEFAULT_CHARSET);
			// 应用默认字符集设置
			applyDefaultCharset();
		}
	}

	/**
	 * 将配置的字符集作为默认值应用于注册的部分转换器。
	 */
	private void applyDefaultCharset() {
		// 遍历部分转换器列表
		for (HttpMessageConverter<?> candidate : this.partConverters) {
			// 如果候选转换器是 抽象Http消息转换器 的实例
			if (candidate instanceof AbstractHttpMessageConverter) {
				AbstractHttpMessageConverter<?> converter = (AbstractHttpMessageConverter<?>) candidate;
				// 如果转换器使用默认字符集
				if (converter.getDefaultCharset() != null) {
					// 将默认字符集设置为指定的字符集
					converter.setDefaultCharset(this.charset);
				}
			}
		}
	}

	/**
	 * 设置在编写多部分数据时用于编码文件名的字符集。编码基于 RFC 2047 中定义的
	 * {@code encoded-word} 语法，并依赖于 {@code javax.mail} 中的 {@code MimeUtility}。
	 * <p>从 5.0 开始，默认情况下部分头包括 {@code Content-Disposition}
	 * （及其文件名参数）将基于 {@link #setCharset(Charset)} 的设置或默认情况下的 {@code UTF-8} 进行编码。
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/MIME#Encoded-Word">Encoded-Word</a>
	 * @since 4.1.1
	 */
	public void setMultipartCharset(Charset charset) {
		this.multipartCharset = charset;
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		// 如果 类型 不是 MultiValueMap 类或其子类，则返回 false
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		// 如果 媒体类型 为 null，则返回 true
		if (mediaType == null) {
			return true;
		}
		// 遍历支持的媒体类型列表
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// 如果支持的媒体类型的主类型是 "multipart"，则跳过
			if (supportedMediaType.getType().equalsIgnoreCase("multipart")) {
				// 我们不能读取多部分，所以跳过这个支持的媒体类型。
				continue;
			}
			// 如果支持的媒体类型包含给定的 媒体类型，则返回 true
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		// 如果没有找到包含给定 媒体类型 的支持的媒体类型，则返回 false
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		// 如果 类 不是 MultiValueMap 类或其子类，则返回 false
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		// 如果 媒体类型 为 null 或者是 所有媒体类型，则返回 true
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		// 遍历支持的媒体类型列表
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// 如果支持的媒体类型与给定的 媒体类型 兼容，则返回 true
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		// 如果没有找到兼容的媒体类型，则返回 false
		return false;
	}

	@Override
	public MultiValueMap<String, String> read(@Nullable Class<? extends MultiValueMap<String, ?>> clazz,
											  HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		// 获取输入消息的内容类型
		MediaType contentType = inputMessage.getHeaders().getContentType();
		// 获取字符集，如果内容类型不为 null， 且包含字符集，则使用内容类型的字符集，否则使用默认字符集
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : this.charset);
		// 将输入消息的主体内容转换为字符串
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

		// 将字符串 body 按 '&' 分隔为键值对数组
		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
		// 创建 解析好键值对 的映射
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
		// 遍历键值对数组
		for (String pair : pairs) {
			// 查找键值对中的 '=' 符号位置
			int idx = pair.indexOf('=');
			// 如果找不到 '=' 符号，则将整个 对 视为键名，值为 null
			if (idx == -1) {
				result.add(URLDecoder.decode(pair, charset.name()), null);
			} else {
				// 否则，将 '=' 符号之前的部分作为键名，之后的部分作为值，并解码为指定字符集的字符串
				String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
				String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
				result.add(name, value);
			}
		}

		// 返回解析后的键值对映射
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(MultiValueMap<String, ?> map, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 如果是多部分内容
		if (isMultipart(map, contentType)) {
			// 写入多部分内容
			writeMultipart((MultiValueMap<String, Object>) map, contentType, outputMessage);
		} else {
			// 否则写入表单内容
			writeForm((MultiValueMap<String, Object>) map, contentType, outputMessage);
		}
	}


	private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
		// 如果内容类型不为 null
		if (contentType != null) {
			// 检查内容类型是否为 多部分（忽略大小写）
			return contentType.getType().equalsIgnoreCase("multipart");
		}

		// 遍历 映射 的每个值列表
		for (List<?> values : map.values()) {
			// 遍历每个值列表的每个值
			for (Object value : values) {
				// 如果值不为 null ，并且不是 字符串 类型，则返回 true
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}

		// 如果没有找到非 字符串 类型的值，则返回 false
		return false;
	}

	private void writeForm(MultiValueMap<String, Object> formData, @Nullable MediaType contentType,
						   HttpOutputMessage outputMessage) throws IOException {

		// 获取表单数据的内容类型
		contentType = getFormContentType(contentType);
		// 设置输出消息的内容类型
		outputMessage.getHeaders().setContentType(contentType);

		// 获取内容类型的字符集
		Charset charset = contentType.getCharset();
		// 确保字符集不为空，否则抛出异常（理论上不会发生）
		// 不应该发生的情况
		Assert.notNull(charset, "No charset");

		// 序列化表单数据，并将其转换为指定字符集的字节数组
		byte[] bytes = serializeForm(formData, charset).getBytes(charset);
		// 设置输出消息的内容长度
		outputMessage.getHeaders().setContentLength(bytes.length);

		// 如果输出消息是 流式处理Http输出消息 的实例
		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			// 设置输出消息的主体
			streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(bytes, outputStream));
		} else {
			// 否则直接将字节数组写入到输出消息的主体中
			StreamUtils.copy(bytes, outputMessage.getBody());
		}
	}

	/**
	 * 返回用于编写表单的内容类型，给定首选的内容类型。
	 * 默认情况下，此方法返回给定的内容类型，但如果没有字符集，则添加
	 * {@linkplain #setCharset(Charset) charset}。
	 * 如果 {@code contentType} 为 {@code null}，则返回
	 * {@code application/x-www-form-urlencoded; charset=UTF-8}。
	 * <p>子类可以重写此方法以更改此行为。
	 *
	 * @param contentType 首选的内容类型（可以为 {@code null}）
	 * @return 要使用的内容类型
	 * @since 5.2.2
	 */
	protected MediaType getFormContentType(@Nullable MediaType contentType) {
		// 如果内容类型为空，则返回默认的表单数据媒体类型
		if (contentType == null) {
			return DEFAULT_FORM_DATA_MEDIA_TYPE;
		} else if (contentType.getCharset() == null) {
			// 如果内容类型的字符集为空
			// 使用指定的字符集创建一个新的内容类型，并返回
			return new MediaType(contentType, this.charset);
		} else {
			// 否则直接返回内容类型
			return contentType;
		}
	}

	protected String serializeForm(MultiValueMap<String, Object> formData, Charset charset) {
		// 创建一个 字符串构建器 对象，用于构建表单数据字符串
		StringBuilder builder = new StringBuilder();
		// 遍历表单数据的每个条目
		formData.forEach((name, values) -> {
			// 如果名称为 null
			if (name == null) {
				// 确保值列表为空，否则抛出异常
				Assert.isTrue(CollectionUtils.isEmpty(values), "Null name in form data: " + formData);
				return;
			}
			// 遍历名称对应的每个值
			values.forEach(value -> {
				try {
					// 如果 字符串构建器 中已有数据，则添加 '&' 分隔符
					if (builder.length() != 0) {
						builder.append('&');
					}
					// 将名称进行 URL 编码并添加到 字符串构建器 中
					builder.append(URLEncoder.encode(name, charset.name()));
					// 如果值不为 null
					if (value != null) {
						// 进行 URL 编码并添加到 字符串构建器 中
						builder.append('=');
						builder.append(URLEncoder.encode(String.valueOf(value), charset.name()));
					}
				} catch (UnsupportedEncodingException ex) {
					// 如果编码不支持，则抛出 IllegalStateException 异常
					throw new IllegalStateException(ex);
				}
			});
		});

		// 返回构建好的表单数据字符串
		return builder.toString();
	}

	private void writeMultipart(
			MultiValueMap<String, Object> parts, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException {

		// 如果提供的内容类型为空，则使用 multipart/form-data 作为默认值。
		// 否则，依赖于 isMultipart() 方法已经验证过提供的内容类型是 多部分。
		if (contentType == null) {
			// 如果内存类型为空，则设置为 多部分表单数据
			contentType = MediaType.MULTIPART_FORM_DATA;
		}

		// 创建一个存储内容类型的参数映射
		Map<String, String> parameters = new LinkedHashMap<>(contentType.getParameters().size() + 2);
		// 将内容类型的现有参数复制到新的参数映射中
		parameters.putAll(contentType.getParameters());

		// 生成多部分请求的边界
		byte[] boundary = generateMultipartBoundary();
		// 如果文件名字符集未设置
		if (!isFilenameCharsetSet()) {
			// 如果字符集既不是 UTF-8 也不是 美国ASCII字符集
			if (!this.charset.equals(StandardCharsets.UTF_8) &&
					!this.charset.equals(StandardCharsets.US_ASCII)) {
				// 将字符集参数添加到参数映射中
				parameters.put("charset", this.charset.name());
			}
		}
		// 将边界参数添加到参数映射中，使用 美国ASCII字符集 编码
		parameters.put("boundary", new String(boundary, StandardCharsets.US_ASCII));

		// 将参数添加到内容类型中
		contentType = new MediaType(contentType, parameters);
		// 设置输出消息的内容类型
		outputMessage.getHeaders().setContentType(contentType);

		// 如果输出消息是 流式处理Http输出消息 的实例
		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			// 设置输出消息的主体
			streamingOutputMessage.setBody(outputStream -> {
				// 写入所有部分到输出流
				writeParts(outputStream, parts, boundary);
				// 写入结束边界到输出流
				writeEnd(outputStream, boundary);
			});
		} else {
			// 写入所有部分到输出消息的主体
			writeParts(outputMessage.getBody(), parts, boundary);
			// 写入结束边界到输出消息的主体
			writeEnd(outputMessage.getBody(), boundary);
		}
	}

	/**
	 * 当配置了 {@link #setMultipartCharset(Charset)}（即 RFC 2047, {@code encoded-word} 语法）时，
	 * 我们需要对部分头使用 ASCII，否则我们直接使用配置的 {@link #setCharset(Charset)} 进行编码。
	 */
	private boolean isFilenameCharsetSet() {
		return (this.multipartCharset != null);
	}

	private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
		// 遍历 部分映射 的每个条目
		for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
			// 获取条目的键（部分的名称）
			String name = entry.getKey();
			// 遍历条目的值（部分的内容列表）
			for (Object part : entry.getValue()) {
				// 如果部分内容不为空
				if (part != null) {
					// 写入边界到输出流
					writeBoundary(os, boundary);
					// 将部分内容作为 Http实体 写入输出流
					writePart(name, getHttpEntity(part), os);
					// 写入一个换行符到输出流
					writeNewLine(os);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
		// 获取 部分实体 的主体内容
		Object partBody = partEntity.getBody();
		// 如果主体内容为空，则抛出异常
		if (partBody == null) {
			throw new IllegalStateException("Empty body for part '" + name + "': " + partEntity);
		}
		// 获取主体内容的类型
		Class<?> partType = partBody.getClass();
		// 获取 部分实体 的头信息
		HttpHeaders partHeaders = partEntity.getHeaders();
		// 获取部分内容的媒体类型
		MediaType partContentType = partHeaders.getContentType();
		// 遍历所有的 Http消息转换器
		for (HttpMessageConverter<?> messageConverter : this.partConverters) {
			// 如果转换器可以写入该类型和媒体类型的内容
			if (messageConverter.canWrite(partType, partContentType)) {
				// 如果文件名字符集已设置，则使用 美国ASCII字符集，否则使用默认字符集
				Charset charset = isFilenameCharsetSet() ? StandardCharsets.US_ASCII : this.charset;
				// 创建 多部分Http输出消息 对象
				HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os, charset);
				// 设置内容处理方式为表单数据
				multipartMessage.getHeaders().setContentDispositionFormData(name, getFilename(partBody));
				// 如果部分头信息不为空
				if (!partHeaders.isEmpty()) {
					// 将其添加到 多部分消息 的头信息中
					multipartMessage.getHeaders().putAll(partHeaders);
				}
				// 使用转换器写入主体内容到 多部分消息
				((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartMessage);
				return;
			}
		}
		// 如果没有找到合适的 Http消息转换器，则抛出异常
		throw new HttpMessageNotWritableException("Could not write request: no suitable HttpMessageConverter " +
				"found for request type [" + partType.getName() + "]");
	}

	/**
	 * 生成一个多部分边界。
	 * <p>此实现委托给 {@link MimeTypeUtils#generateMultipartBoundary()}。
	 *
	 * @return 生成的多部分边界字节数组
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * 返回给定部分对象的 {@link HttpEntity}。
	 *
	 * @param part 要返回 {@link HttpEntity} 的部分对象
	 * @return 如果部分对象是 {@link HttpEntity}，则返回部分对象本身；
	 * 否则返回新构建的包装了该部分对象的 {@link HttpEntity}
	 */
	protected HttpEntity<?> getHttpEntity(Object part) {
		return (part instanceof HttpEntity ? (HttpEntity<?>) part : new HttpEntity<>(part));
	}

	/**
	 * 返回给定多部分部分的文件名。此值将用于 {@code Content-Disposition} 头部。
	 * <p>默认实现如果部分对象是 {@code Resource}，则返回 {@link Resource#getFilename()}；
	 * 否则返回 {@code null}。可以在子类中覆盖此方法。
	 *
	 * @param part 要确定文件名的多部分部分
	 * @return 文件名，如果不知道则返回 {@code null}
	 */
	@Nullable
	protected String getFilename(Object part) {
		// 如果 部分 是 Resource 的实例
		if (part instanceof Resource) {
			// 将 部分 转换为 Resource
			Resource resource = (Resource) part;
			// 获取资源的文件名
			String filename = resource.getFilename();
			// 如果文件名不为空，且 多部分字符集 也不为空
			if (filename != null && this.multipartCharset != null) {
				// 使用指定的字符集对文件名进行编码
				filename = MimeDelegate.encode(filename, this.multipartCharset.name());
			}
			// 返回编码后的文件名
			return filename;
		} else {
			// 否则返回 null
			return null;
		}
	}


	private void writeBoundary(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	private static void writeEnd(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		os.write('-');
		os.write('-');
		writeNewLine(os);
	}

	private static void writeNewLine(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}


	/**
	 * 用于编写MIME多部分消息的 {@link org.springframework.http.HttpOutputMessage} 实现。
	 */
	private static class MultipartHttpOutputMessage implements HttpOutputMessage {
		/**
		 * 输出流
		 */
		private final OutputStream outputStream;

		/**
		 * 字符集
		 */
		private final Charset charset;

		/**
		 * 头部信息
		 */
		private final HttpHeaders headers = new HttpHeaders();

		/**
		 * 是否已经写入头部信息到输出流。
		 */
		private boolean headersWritten = false;

		/**
		 * 构造函数，初始化输出流和字符集。
		 *
		 * @param outputStream 输出流
		 * @param charset      字符集
		 */
		public MultipartHttpOutputMessage(OutputStream outputStream, Charset charset) {
			this.outputStream = outputStream;
			this.charset = charset;
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public OutputStream getBody() throws IOException {
			// 写入头部信息
			writeHeaders();
			// 返回输出流
			return this.outputStream;
		}

		/**
		 * 写入头部信息到输出流。
		 *
		 * @throws IOException 写入过程中可能抛出的IO异常
		 */
		private void writeHeaders() throws IOException {
			// 如果还没有写入过头部信息
			if (!this.headersWritten) {
				// 遍历所有的头部条目
				for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
					// 获取头部名称的字节数组
					byte[] headerName = getBytes(entry.getKey());

					// 遍历头部值列表
					for (String headerValueString : entry.getValue()) {
						// 获取头部值的字节数组
						byte[] headerValue = getBytes(headerValueString);

						// 将头部名称写入输出流
						this.outputStream.write(headerName);

						// 写入冒号和空格
						this.outputStream.write(':');
						this.outputStream.write(' ');

						// 将头部值写入输出流
						this.outputStream.write(headerValue);

						// 写入换行符
						writeNewLine(this.outputStream);
					}
				}

				// 写入一个额外的换行符，表示头部结束
				writeNewLine(this.outputStream);

				// 标记头部已经写入
				this.headersWritten = true;
			}
		}

		/**
		 * 将字符串转换为字节数组，使用指定的字符集。
		 *
		 * @param name 要转换的字符串
		 * @return 使用指定字符集转换后的字节数组
		 */
		private byte[] getBytes(String name) {
			return name.getBytes(this.charset);
		}
	}


	/**
	 * 内部类，避免对 JavaMail API 的硬依赖。
	 */
	private static class MimeDelegate {

		/**
		 * 使用指定的字符集编码给定的值。
		 *
		 * @param value   要编码的值
		 * @param charset 字符集
		 * @return 编码后的文本
		 * @throws IllegalStateException 如果编码过程中出现不支持的编码异常
		 */
		public static String encode(String value, String charset) {
			try {
				// 尝试使用指定的字符集对值进行 MIME 编码
				return MimeUtility.encodeText(value, charset, null);
			} catch (UnsupportedEncodingException ex) {
				// 如果编码不支持，则抛出 IllegalStateException 异常
				throw new IllegalStateException(ex);
			}
		}
	}

}
