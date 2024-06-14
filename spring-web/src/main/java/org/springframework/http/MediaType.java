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

package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * {@link MimeType} 的子类，增加了对HTTP规范中定义的质量参数的支持。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1">
 * HTTP 1.1: Semantics and Content, section 3.1.1.1</a>
 * @since 3.0
 */
public class MediaType extends MimeType implements Serializable {

	private static final long serialVersionUID = 2069937152339670231L;

	/**
	 * 公共常量媒体类型，包含所有媒体范围（即 "&#42;/&#42;"）。
	 */
	public static final MediaType ALL;

	/**
	 * {@link MediaType#ALL} 的字符串等效值常量。
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 * 媒体类型常量，表示 {@code application/atom+xml} 的公共常量。
	 */
	public static final MediaType APPLICATION_ATOM_XML;

	/**
	 * {@link MediaType#APPLICATION_ATOM_XML} 的字符串等效值常量。
	 */
	public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * 媒体类型常量，表示 {@code application/cbor} 的公共常量。
	 *
	 * @since 5.2
	 */
	public static final MediaType APPLICATION_CBOR;

	/**
	 * {@link MediaType#APPLICATION_CBOR} 的字符串等效值常量。
	 *
	 * @since 5.2
	 */
	public static final String APPLICATION_CBOR_VALUE = "application/cbor";

	/**
	 * 媒体类型常量，表示 {@code application/x-www-form-urlencoded} 的公共常量。
	 */
	public static final MediaType APPLICATION_FORM_URLENCODED;

	/**
	 * {@link MediaType#APPLICATION_FORM_URLENCODED} 的字符串等效值常量。
	 */
	public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * 媒体类型常量，表示 {@code application/graphql+json} 的公共常量。
	 *
	 * @see <a href="https://github.com/graphql/graphql-over-http">GraphQL over HTTP spec</a>
	 * @since 5.3.19
	 */
	public static final MediaType APPLICATION_GRAPHQL;

	/**
	 * {@link MediaType#APPLICATION_GRAPHQL} 的字符串等效值常量。
	 *
	 * @since 5.3.19
	 */
	public static final String APPLICATION_GRAPHQL_VALUE = "application/graphql+json";

	/**
	 * 媒体类型常量，表示 {@code application/json} 的公共常量。
	 */
	public static final MediaType APPLICATION_JSON;

	/**
	 * {@link MediaType#APPLICATION_JSON} 的字符串等效值常量。
	 *
	 * @see #APPLICATION_JSON_UTF8_VALUE
	 */
	public static final String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * 媒体类型常量，表示 {@code application/json;charset=UTF-8} 的公共常量。
	 *
	 * @deprecated 自 5.2 起，请使用 {@link #APPLICATION_JSON}，
	 * 因为主流浏览器如 Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * 现在符合规范</a>，并正确解释UTF-8特殊字符，无需 {@code charset=UTF-8} 参数。
	 */
	@Deprecated
	public static final MediaType APPLICATION_JSON_UTF8;

	/**
	 * {@link MediaType#APPLICATION_JSON_UTF8} 的字符串等效值常量。
	 *
	 * @deprecated 自 5.2 起，请使用 {@link #APPLICATION_JSON_VALUE}，
	 * 因为主流浏览器如 Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * 现在符合规范</a>，并正确解释UTF-8特殊字符，无需 {@code charset=UTF-8} 参数。
	 */
	@Deprecated
	public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

	/**
	 * 媒体类型常量，表示 {@code application/octet-stream} 的公共常量。
	 */
	public static final MediaType APPLICATION_OCTET_STREAM;

	/**
	 * {@link MediaType#APPLICATION_OCTET_STREAM} 的字符串等效值常量。
	 */
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * 媒体类型常量，表示 {@code application/pdf} 的公共常量。
	 *
	 * @since 4.3
	 */
	public static final MediaType APPLICATION_PDF;

	/**
	 * {@link MediaType#APPLICATION_PDF} 的字符串等效值常量。
	 *
	 * @since 4.3
	 */
	public static final String APPLICATION_PDF_VALUE = "application/pdf";

	/**
	 * 媒体类型常量，表示 {@code application/problem+json} 的公共常量。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
	 * HTTP api的问题详细信息, 6.1. application/problem+json</a>
	 * @since 5.0
	 */
	public static final MediaType APPLICATION_PROBLEM_JSON;

	/**
	 * {@link MediaType#APPLICATION_PROBLEM_JSON} 的字符串等效值常量。
	 *
	 * @since 5.0
	 */
	public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";

	/**
	 * 媒体类型常量，表示 {@code application/problem+json}。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
	 * Problem Details for HTTP APIs, 6.1. application/problem+json</a>
	 * @since 5.0
	 * @deprecated 自 5.2 起，请使用 {@link #APPLICATION_PROBLEM_JSON}，
	 * 因为主流浏览器如 Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * 现在符合规范</a>，并正确解释UTF-8特殊字符，无需 {@code charset=UTF-8} 参数。
	 */
	@Deprecated
	public static final MediaType APPLICATION_PROBLEM_JSON_UTF8;

	/**
	 * {@link MediaType#APPLICATION_PROBLEM_JSON_UTF8} 的字符串等效值常量。
	 *
	 * @since 5.0
	 * @deprecated 自 5.2 起，请使用 {@link #APPLICATION_PROBLEM_JSON_VALUE}，
	 * 因为主流浏览器如 Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * 现在符合规范</a>，并正确解释UTF-8特殊字符，无需 {@code charset=UTF-8} 参数。
	 */
	@Deprecated
	public static final String APPLICATION_PROBLEM_JSON_UTF8_VALUE = "application/problem+json;charset=UTF-8";

	/**
	 * 媒体类型常量，表示 {@code application/problem+xml} 的公共常量。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.2">
	 * HTTP api的问题详细信息, 6.2. application/problem+xml</a>
	 * @since 5.0
	 */
	public static final MediaType APPLICATION_PROBLEM_XML;

	/**
	 * {@link MediaType#APPLICATION_PROBLEM_XML} 的字符串等效值常量。
	 *
	 * @since 5.0
	 */
	public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";

	/**
	 * 媒体类型常量，表示 {@code application/rss+xml} 的公共常量。
	 *
	 * @since 4.3.6
	 */
	public static final MediaType APPLICATION_RSS_XML;

	/**
	 * {@link MediaType#APPLICATION_RSS_XML} 的字符串等效值常量。
	 *
	 * @since 4.3.6
	 */
	public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

	/**
	 * 媒体类型常量，表示 {@code application/x-ndjson} 的公共常量。
	 *
	 * @since 5.3
	 */
	public static final MediaType APPLICATION_NDJSON;

	/**
	 * {@link MediaType#APPLICATION_NDJSON} 的字符串等效值常量。
	 *
	 * @since 5.3
	 */
	public static final String APPLICATION_NDJSON_VALUE = "application/x-ndjson";

	/**
	 * 媒体类型常量，表示 {@code application/stream+json} 的公共常量。
	 *
	 * @since 5.0
	 * @deprecated 自 5.3 起，请参阅 {@link #APPLICATION_STREAM_JSON_VALUE} 上的通知。
	 */
	@Deprecated
	public static final MediaType APPLICATION_STREAM_JSON;

	/**
	 * {@link MediaType#APPLICATION_STREAM_JSON} 的字符串等效值常量。
	 *
	 * @since 5.0
	 * @deprecated 自 5.3 起，源自 W3C 活动流规范，其用途更具体化，已被不同的 MIME 类型取代。
	 * 建议使用 {@link #APPLICATION_NDJSON} 或其他行分隔的 JSON 格式（如 JSON Lines、JSON Text Sequences）。
	 */
	@Deprecated
	public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";

	/**
	 * 媒体类型常量，表示 {@code application/xhtml+xml} 的公共常量。
	 */
	public static final MediaType APPLICATION_XHTML_XML;

	/**
	 * {@link MediaType#APPLICATION_XHTML_XML} 的字符串等效值常量。
	 */
	public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * 媒体类型常量，表示 {@code application/xml} 的公共常量。
	 */
	public static final MediaType APPLICATION_XML;

	/**
	 * {@link MediaType#APPLICATION_XML} 的字符串等效值常量。
	 */
	public static final String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * 媒体类型常量，表示 {@code image/gif} 的公共常量。
	 */
	public static final MediaType IMAGE_GIF;

	/**
	 * {@link MediaType#IMAGE_GIF} 的字符串等效值常量。
	 */
	public static final String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * 媒体类型常量，表示 {@code image/jpeg} 的公共常量。
	 */
	public static final MediaType IMAGE_JPEG;

	/**
	 * {@link MediaType#IMAGE_JPEG} 的字符串等效值常量。
	 */
	public static final String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * 媒体类型常量，表示 {@code image/png} 的公共常量。
	 */
	public static final MediaType IMAGE_PNG;

	/**
	 * {@link MediaType#IMAGE_PNG} 的字符串等效值常量。
	 */
	public static final String IMAGE_PNG_VALUE = "image/png";

	/**
	 * 媒体类型常量，表示 {@code multipart/form-data} 的公共常量。
	 */
	public static final MediaType MULTIPART_FORM_DATA;

	/**
	 * {@link MediaType#MULTIPART_FORM_DATA} 的字符串等效值常量。
	 */
	public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * 媒体类型常量，表示 {@code multipart/mixed} 的公共常量。
	 *
	 * @since 5.2
	 */
	public static final MediaType MULTIPART_MIXED;

	/**
	 * {@link MediaType#MULTIPART_MIXED} 的字符串等效值常量。
	 *
	 * @since 5.2
	 */
	public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

	/**
	 * 媒体类型常量，表示 {@code multipart/related} 的公共常量。
	 *
	 * @since 5.2.5
	 */
	public static final MediaType MULTIPART_RELATED;

	/**
	 * 媒体类型常量，表示 {@code multipart/related} 字符串等效值。
	 *
	 * @since 5.2.5
	 */
	public static final String MULTIPART_RELATED_VALUE = "multipart/related";

	/**
	 * 媒体类型常量，表示 {@code text/event-stream} 的公共常量。
	 *
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 * @since 4.3.6
	 */
	public static final MediaType TEXT_EVENT_STREAM;

	/**
	 * {@link MediaType#TEXT_EVENT_STREAM} 的字符串等效值常量。
	 *
	 * @since 4.3.6
	 */
	public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

	/**
	 * 媒体类型常量，表示 {@code text/html} 的公共常量。
	 */
	public static final MediaType TEXT_HTML;

	/**
	 * {@link MediaType#TEXT_HTML} 的字符串等效值常量。
	 */
	public static final String TEXT_HTML_VALUE = "text/html";

	/**
	 * 媒体类型常量，表示 {@code text/markdown} 的公共常量。
	 *
	 * @since 4.3
	 */
	public static final MediaType TEXT_MARKDOWN;

	/**
	 * {@link MediaType#TEXT_MARKDOWN} 的字符串等效值常量。
	 *
	 * @since 4.3
	 */
	public static final String TEXT_MARKDOWN_VALUE = "text/markdown";

	/**
	 * 媒体类型常量，表示 {@code text/plain} 的公共常量。
	 */
	public static final MediaType TEXT_PLAIN;

	/**
	 * {@link MediaType#TEXT_PLAIN} 的字符串等效值常量。
	 */
	public static final String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * 媒体类型常量，表示 {@code text/xml} 的公共常量。
	 */
	public static final MediaType TEXT_XML;

	/**
	 * {@link MediaType#TEXT_XML} 的字符串等效值常量。
	 */
	public static final String TEXT_XML_VALUE = "text/xml";

	/**
	 * 媒体类型参数的质量因子名称常量，表示 {@code q}。
	 */
	private static final String PARAM_QUALITY_FACTOR = "q";


	static {
		// 不使用 “value of” 来避免静态init成本
		ALL = new MediaType("*", "*");
		APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
		APPLICATION_CBOR = new MediaType("application", "cbor");
		APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
		APPLICATION_GRAPHQL = new MediaType("application", "graphql+json");
		APPLICATION_JSON = new MediaType("application", "json");
		APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);
		APPLICATION_NDJSON = new MediaType("application", "x-ndjson");
		APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
		APPLICATION_PDF = new MediaType("application", "pdf");
		APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");
		APPLICATION_PROBLEM_JSON_UTF8 = new MediaType("application", "problem+json", StandardCharsets.UTF_8);
		APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");
		APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
		APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");
		APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");
		APPLICATION_XML = new MediaType("application", "xml");
		IMAGE_GIF = new MediaType("image", "gif");
		IMAGE_JPEG = new MediaType("image", "jpeg");
		IMAGE_PNG = new MediaType("image", "png");
		MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
		MULTIPART_MIXED = new MediaType("multipart", "mixed");
		MULTIPART_RELATED = new MediaType("multipart", "related");
		TEXT_EVENT_STREAM = new MediaType("text", "event-stream");
		TEXT_HTML = new MediaType("text", "html");
		TEXT_MARKDOWN = new MediaType("text", "markdown");
		TEXT_PLAIN = new MediaType("text", "plain");
		TEXT_XML = new MediaType("text", "xml");
	}


	/**
	 * 创建一个新的 {@code MediaType}，用于指定的主类型。
	 * <p>子类型设置为 "&#42;"，参数为空。
	 *
	 * @param type 主类型
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type) {
		super(type);
	}

	/**
	 * 创建一个新的 {@code MediaType}，用于指定的主类型和子类型。
	 * <p>参数为空。
	 *
	 * @param type    主类型
	 * @param subtype 子类型
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype) {
		super(type, subtype, Collections.emptyMap());
	}

	/**
	 * 创建一个新的 {@code MediaType}，用于指定的主类型、子类型和字符集。
	 *
	 * @param type    主类型
	 * @param subtype 子类型
	 * @param charset 字符集
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype, Charset charset) {
		super(type, subtype, charset);
	}

	/**
	 * 创建一个新的 {@code MediaType}，用于指定的主类型、子类型和质量值。
	 *
	 * @param type         主类型
	 * @param subtype      子类型
	 * @param qualityValue 质量值
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype, double qualityValue) {
		this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
	}

	/**
	 * 复制构造函数，复制给定 {@code MediaType} 的类型、子类型和参数，并允许设置指定的字符集。
	 *
	 * @param other   另一个媒体类型
	 * @param charset 字符集
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 * @since 4.3
	 */
	public MediaType(MediaType other, Charset charset) {
		super(other, charset);
	}

	/**
	 * 复制构造函数，复制给定 {@code MediaType} 的类型和子类型，并允许设置不同的参数。
	 *
	 * @param other      另一个媒体类型
	 * @param parameters 参数，可以为 {@code null}
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(MediaType other, @Nullable Map<String, String> parameters) {
		super(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * 创建一个新的 {@code MediaType}，用于指定的主类型、子类型和参数。
	 *
	 * @param type       主类型
	 * @param subtype    子类型
	 * @param parameters 参数，可以为 {@code null}
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype, @Nullable Map<String, String> parameters) {
		super(type, subtype, parameters);
	}

	/**
	 * 根据给定的 {@link MimeType} 创建一个新的 {@code MediaType}。
	 * 将复制类型、子类型和参数信息，并执行 {@code MediaType} 特定的参数检查。
	 *
	 * @param mimeType MIME 类型
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 * @since 5.3
	 */
	public MediaType(MimeType mimeType) {
		// 调用父类构造函数，传入MimeType对象作为参数
		super(mimeType);

		// 遍历当前对象的参数映射，并对每对参数调用checkParameters方法
		getParameters().forEach(this::checkParameters);
	}


	@Override
	protected void checkParameters(String parameter, String value) {
		// 调用父类的checkParameters方法，检查参数和值
		super.checkParameters(parameter, value);

		// 如果参数是质量因子参数
		if (PARAM_QUALITY_FACTOR.equals(parameter)) {
			// 去掉参数值的引号
			value = unquote(value);

			// 将参数值转换为双精度浮点数
			double d = Double.parseDouble(value);

			// 断言该值在0.0到1.0之间，如果不在范围内则抛出异常
			Assert.isTrue(d >= 0D && d <= 1D,
					"Invalid quality value \"" + value + "\": should be between 0.0 and 1.0");
		}
	}

	/**
	 * 返回质量因子，如 {@code q} 参数所指示的，如果存在的话。
	 * 默认为 {@code 1.0}。
	 *
	 * @return 质量因子的 double 值
	 */
	public double getQualityValue() {
		// 获取质量因子参数的值
		String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);

		// 如果质量因子不为空，则解析并返回其值；
		// 否则返回默认值1.0
		return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
	}

	/**
	 * 指示此 {@code MediaType} 是否包含给定的媒体类型。
	 * <p>例如，{@code text/*} 包含 {@code text/plain} 和 {@code text/html}，
	 * {@code application/*+xml} 包含 {@code application/soap+xml}，等等。
	 * 此方法<b>不</b>是对称的。
	 * <p>简单调用 {@link MimeType#includes(MimeType)}，但使用 {@code MediaType} 参数声明以支持二进制向后兼容性。
	 *
	 * @param other 要比较的参考媒体类型
	 * @return 如果此媒体类型包含给定的媒体类型，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean includes(@Nullable MediaType other) {
		return super.includes(other);
	}

	/**
	 * 指示此 {@code MediaType} 是否与给定的媒体类型兼容。
	 * <p>例如，{@code text/*} 与 {@code text/plain}、{@code text/html} 兼容，反之亦然。
	 * 实际上，此方法类似于 {@link #includes}，但<b>是</b>对称的。
	 * <p>简单调用 {@link MimeType#isCompatibleWith(MimeType)}，但使用 {@code MediaType} 参数声明以支持二进制向后兼容性。
	 *
	 * @param other 要比较的参考媒体类型
	 * @return 如果此媒体类型与给定的媒体类型兼容，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean isCompatibleWith(@Nullable MediaType other) {
		return super.isCompatibleWith(other);
	}

	/**
	 * 返回一个具有给定 {@code MediaType} 的质量值的副本实例。
	 *
	 * @return 如果给定的 MediaType 没有质量值，则返回相同的实例；否则返回一个新实例
	 */
	public MediaType copyQualityValue(MediaType mediaType) {
		// 如果传入的媒体类型参数中不包含质量因子参数
		if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			// 返回当前对象本身
			return this;
		}

		// 创建一个新的参数映射，包含当前对象的所有参数
		Map<String, String> params = new LinkedHashMap<>(getParameters());

		// 将传入的媒体类型的质量因子参数添加到新的参数映射中
		params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));

		// 使用当前对象和新的参数映射创建一个新的MediaType对象，并返回它
		return new MediaType(this, params);
	}

	/**
	 * 返回一个去除了其质量值的副本实例。
	 *
	 * @return 如果媒体类型不包含质量值，则返回相同的实例；否则返回一个新实例
	 */
	public MediaType removeQualityValue() {
		// 如果当前参数中不包含质量因子参数
		if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			// 返回当前对象本身
			return this;
		}

		// 创建一个新的参数映射，包含当前对象的所有参数
		Map<String, String> params = new LinkedHashMap<>(getParameters());

		// 从参数映射中移除质量因子参数
		params.remove(PARAM_QUALITY_FACTOR);

		// 使用当前对象和新的参数映射创建一个新的MediaType对象，并返回它
		return new MediaType(this, params);
	}


	/**
	 * 将给定的 String 值解析为 {@code MediaType} 对象，
	 * 此方法的命名遵循 'valueOf' 命名约定（如 {@link org.springframework.core.convert.ConversionService} 支持）。
	 *
	 * @param value 要解析的字符串
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 * @see #parseMediaType(String)
	 */
	public static MediaType valueOf(String value) {
		return parseMediaType(value);
	}

	/**
	 * 将给定的 String 解析为单个 {@code MediaType}。
	 *
	 * @param mediaType 要解析的字符串
	 * @return 媒体类型
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 */
	public static MediaType parseMediaType(String mediaType) {
		// 声明一个MimeType类型的变量
		MimeType type;

		try {
			// 尝试解析媒体类型字符串为MimeType对象
			type = MimeTypeUtils.parseMimeType(mediaType);
		} catch (InvalidMimeTypeException ex) {
			// 如果解析失败，抛出无效媒体类型异常
			throw new InvalidMediaTypeException(ex);
		}

		try {
			// 尝试创建一个新的MediaType对象
			return new MediaType(type);
		} catch (IllegalArgumentException ex) {
			// 如果创建MediaType对象失败，抛出无效媒体类型异常，并包含具体错误信息
			throw new InvalidMediaTypeException(mediaType, ex.getMessage());
		}
	}

	/**
	 * 将逗号分隔的字符串解析为 {@code MediaType} 对象列表。
	 * <p>此方法可用于解析 Accept 或 Content-Type 头部。
	 *
	 * @param mediaTypes 要解析的字符串
	 * @return 媒体类型列表
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 */
	public static List<MediaType> parseMediaTypes(@Nullable String mediaTypes) {
		// 如果媒体类型字符串没有长度（为空或仅包含空白字符）
		if (!StringUtils.hasLength(mediaTypes)) {
			// 返回一个空列表
			return Collections.emptyList();
		}

		// 避免在热路径中使用 java.util.stream.Stream，使用其他方法进行处理
		// 将媒体类型字符串进行分词处理，得到一个字符串列表
		List<String> tokenizedTypes = MimeTypeUtils.tokenize(mediaTypes);

		// 创建一个结果列表，初始容量为分词后的字符串列表的大小
		List<MediaType> result = new ArrayList<>(tokenizedTypes.size());

		// 遍历每一个分词后的类型字符串
		for (String type : tokenizedTypes) {
			// 如果类型字符串不为空且包含非空白字符
			if (StringUtils.hasText(type)) {
				// 解析类型字符串，并将解析结果添加到结果列表中
				result.add(parseMediaType(type));
			}
		}

		// 返回结果列表
		return result;
	}

	/**
	 * 将给定的（可能是逗号分隔的）字符串列表解析为 {@code MediaType} 对象列表。
	 * <p>此方法可用于解析 Accept 或 Content-Type 头部。
	 *
	 * @param mediaTypes 要解析的字符串
	 * @return 媒体类型列表
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 * @since 4.3.2
	 */
	public static List<MediaType> parseMediaTypes(@Nullable List<String> mediaTypes) {
		// 如果媒体类型列表为空
		if (CollectionUtils.isEmpty(mediaTypes)) {
			// 返回一个空列表
			return Collections.emptyList();
		} else if (mediaTypes.size() == 1) {
			// 如果媒体类型列表只有一个元素
			// 解析单个媒体类型，并返回解析后的列表
			return parseMediaTypes(mediaTypes.get(0));
		} else {
			// 如果媒体类型列表有多个元素
			// 创建一个结果列表，初始容量为8
			List<MediaType> result = new ArrayList<>(8);
			// 遍历每一个媒体类型字符串
			for (String mediaType : mediaTypes) {
				// 解析媒体类型，并将解析结果添加到结果列表中
				result.addAll(parseMediaTypes(mediaType));
			}
			// 返回结果列表
			return result;
		}
	}

	/**
	 * 将给定的 MIME 类型列表重新创建为媒体类型列表。
	 *
	 * @since 5.0
	 */
	public static List<MediaType> asMediaTypes(List<MimeType> mimeTypes) {
		// 创建一个媒体类型列表，大小为MIME类型列表的大小
		List<MediaType> mediaTypes = new ArrayList<>(mimeTypes.size());

		// 遍历每一个MIME类型
		for (MimeType mimeType : mimeTypes) {
			// 将MIME类型转换为媒体类型，并添加到媒体类型列表中
			mediaTypes.add(MediaType.asMediaType(mimeType));
		}

		// 返回媒体类型列表
		return mediaTypes;
	}

	/**
	 * 将给定的 MIME 类型重新创建为媒体类型。
	 *
	 * @since 5.0
	 */
	public static MediaType asMediaType(MimeType mimeType) {
		// 如果mimeType是MediaType的实例
		if (mimeType instanceof MediaType) {
			// 直接返回转换为MediaType类型的mimeType
			return (MediaType) mimeType;
		}

		// 否则，创建一个新的MediaType对象，并返回
		return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
	}

	/**
	 * 返回给定的 {@code MediaType} 对象列表的字符串表示形式。
	 * <p>此方法可用于创建 {@code Accept} 或 {@code Content-Type} 头部的字符串表示形式。
	 *
	 * @param mediaTypes 要创建字符串表示形式的媒体类型
	 * @return 字符串表示形式
	 */
	public static String toString(Collection<MediaType> mediaTypes) {
		return MimeTypeUtils.toString(mediaTypes);
	}

	/**
	 * 根据特异性对给定的 {@code MediaType} 对象列表进行排序。
	 * <p>给定两种媒体类型：
	 * <ol>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardType() 通配符类型}，则没有通配符的媒体类型将排在前面。</li>
	 * <li>如果两种媒体类型具有不同的 {@linkplain #getType() 类型}，则它们被视为相等，保留它们当前的顺序。</li>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardSubtype() 通配符子类型}，则没有通配符的媒体类型将排在前面。</li>
	 * <li>如果两种媒体类型具有不同的 {@linkplain #getSubtype() 子类型}，则它们被视为相等，保留它们当前的顺序。</li>
	 * <li>如果两种媒体类型具有不同的 {@linkplain #getQualityValue() 质量值}，则具有较高质量值的媒体类型将排在前面。</li>
	 * <li>如果两种媒体类型具有不同数量的 {@linkplain #getParameter(String) 参数}，则具有更多参数的媒体类型将排在前面。</li>
	 * </ol>
	 * <p>例如：
	 * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote>
	 * <blockquote>audio/basic == audio/wave</blockquote>
	 *
	 * @param mediaTypes 要排序的媒体类型列表
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
	 * and Content, section 5.3.2</a>
	 */
	public static void sortBySpecificity(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		// 如果媒体类型列表的大小大于1
		if (mediaTypes.size() > 1) {
			// 对媒体类型列表按照特异性比较器进行排序
			mediaTypes.sort(SPECIFICITY_COMPARATOR);
		}
	}

	/**
	 * 根据质量值对给定的 {@code MediaType} 对象列表进行排序。
	 * <p>给定两种媒体类型：
	 * <ol>
	 * <li>如果两种媒体类型具有不同的 {@linkplain #getQualityValue() 质量值}，则具有较高质量值的媒体类型将排在前面。</li>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardType() 通配符类型}，则没有通配符的媒体类型将排在前面。</li>
	 * <li>如果两种媒体类型具有不同的 {@linkplain #getType() 类型}，则它们被视为相等，保留它们当前的顺序。</li>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardSubtype() 通配符子类型}，则没有通配符的媒体类型将排在前面。</li>
	 * <li>如果两种媒体类型具有不同的 {@linkplain #getSubtype() 子类型}，则它们被视为相等，保留它们当前的顺序。</li>
	 * <li>如果两种媒体类型具有不同数量的 {@linkplain #getParameter(String) 参数}，则具有更多参数的媒体类型将排在前面。</li>
	 * </ol>
	 *
	 * @param mediaTypes 要排序的媒体类型列表
	 * @see #getQualityValue()
	 */
	public static void sortByQualityValue(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		// 如果媒体类型列表的大小大于1
		if (mediaTypes.size() > 1) {
			// 对媒体类型列表按照质量值比较器进行排序
			mediaTypes.sort(QUALITY_VALUE_COMPARATOR);
		}
	}

	/**
	 * 将给定的 {@code MediaType} 对象列表按照特异性作为主要标准和质量值作为次要标准进行排序。
	 *
	 * @see MediaType#sortBySpecificity(List)
	 * @see MediaType#sortByQualityValue(List)
	 */
	public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");

		// 如果媒体类型列表的大小大于1
		if (mediaTypes.size() > 1) {
			// 对媒体类型列表进行排序，先按SPECIFICITY_COMPARATOR排序，再按QUALITY_VALUE_COMPARATOR排序
			mediaTypes.sort(MediaType.SPECIFICITY_COMPARATOR.thenComparing(MediaType.QUALITY_VALUE_COMPARATOR));
		}
	}


	/**
	 * 用于 {@link #sortByQualityValue(List)} 方法的比较器。
	 */
	public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = (mediaType1, mediaType2) -> {
		// 获取媒体类型1的质量因子
		double quality1 = mediaType1.getQualityValue();
		// 获取媒体类型2的质量因子
		double quality2 = mediaType2.getQualityValue();
		// 比较两个质量因子的大小
		int qualityComparison = Double.compare(quality2, quality1);
		if (qualityComparison != 0) {
			// 返回质量因子比较结果
			return qualityComparison;
		} else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) {  // 如果媒体类型1是通配符类型而媒体类型2不是
			// 媒体类型2更具体，返回1
			return 1;
		} else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) {  // 如果媒体类型2是通配符类型而媒体类型1不是
			// 媒体类型1更具体，返回-1
			return -1;
		} else if (!mediaType1.getType().equals(mediaType2.getType())) {  // 如果媒体类型1的主类型与媒体类型2的主类型不同
			// 主类型相同，返回0
			return 0;
		} else {
			// 如果媒体类型1的主类型与媒体类型2的主类型相同
			if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) {  // 如果媒体类型1是通配符子类型而媒体类型2不是
				// 媒体类型2更具体，返回1
				return 1;
			} else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) {  // 如果媒体类型2是通配符子类型而媒体类型1不是
				// 媒体类型1更具体，返回-1
				return -1;
			} else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) {  // 如果媒体类型1的子类型与媒体类型2的子类型不同
				// 子类型相同，返回0
				return 0;
			} else {
				// 获取媒体类型1和媒体类型2的参数个数
				int paramsSize1 = mediaType1.getParameters().size();
				int paramsSize2 = mediaType2.getParameters().size();
				// 比较参数个数，返回比较结果
				return Integer.compare(paramsSize2, paramsSize1);
			}
		}
	};


	/**
	 * 用于 {@link #sortBySpecificity(List)} 方法的比较器。
	 */
	public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MediaType>() {

		@Override
		protected int compareParameters(MediaType mediaType1, MediaType mediaType2) {
			// 获取媒体类型1的质量值
			double quality1 = mediaType1.getQualityValue();
			// 获取媒体类型2的质量值
			double quality2 = mediaType2.getQualityValue();
			// 比较两个质量值的大小
			int qualityComparison = Double.compare(quality2, quality1);
			// 如果质量值不同，根据质量值比较结果返回比较值
			if (qualityComparison != 0) {
				// audio/*;q=0.7 < audio/*;q=0.3
				return qualityComparison;
			}
			// 如果质量值相同，调用父类方法比较媒体类型的参数
			return super.compareParameters(mediaType1, mediaType2);
		}
	};

}
