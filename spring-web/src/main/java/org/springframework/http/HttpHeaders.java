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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 表示 HTTP 请求或响应头的数据结构，将 String 类型的头名称映射到一个 String 值列表，并提供对常见应用程序级数据类型的访问器。
 *
 * <p>除了 {@link Map} 定义的常规方法之外，该类还提供许多常见的便利方法，例如:
 * <ul>
 * <li>{@link #getFirst(String)} 返回与给定头名称关联的第一个值</li>
 * <li>{@link #add(String, String)} 将头值添加到头名称的值列表中</li>
 * <li>{@link #set(String, String)} 将头值设置为单个字符串值</li>
 * </ul>
 *
 * <p>注意，{@code HttpHeaders} 通常以大小写不敏感的方式处理头名称。
 * <p>
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @author Sam Brannen
 * @since 3.0
 */
public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = -8578554704772377436L;


	/**
	 * HTTP {@code Accept} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 第 5.3.2 节</a>
	 */
	public static final String ACCEPT = "Accept";

	/**
	 * HTTP {@code Accept-Charset} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.3">RFC 7231 第 5.3.3 节</a>
	 */
	public static final String ACCEPT_CHARSET = "Accept-Charset";

	/**
	 * HTTP {@code Accept-Encoding} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.4">RFC 7231 第 5.3.4 节</a>
	 */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";

	/**
	 * HTTP {@code Accept-Language} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 第 5.3.5 节</a>
	 */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";

	/**
	 * HTTP {@code Accept-Patch} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc5789#section-3.1">RFC 5789 第 3.1 节</a>
	 * @since 5.3.6
	 */
	public static final String ACCEPT_PATCH = "Accept-Patch";

	/**
	 * HTTP {@code Accept-Ranges} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.3">RFC 7233 第 2.3 节</a>
	 */
	public static final String ACCEPT_RANGES = "Accept-Ranges";

	/**
	 * CORS {@code Access-Control-Allow-Credentials} 响应头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

	/**
	 * CORS {@code Access-Control-Allow-Headers} 响应头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

	/**
	 * CORS {@code Access-Control-Allow-Methods} 响应头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

	/**
	 * CORS {@code Access-Control-Allow-Origin} 响应头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	/**
	 * CORS {@code Access-Control-Expose-Headers} 响应头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

	/**
	 * CORS {@code Access-Control-Max-Age} 响应头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	/**
	 * CORS {@code Access-Control-Request-Headers} 请求头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

	/**
	 * CORS {@code Access-Control-Request-Method} 请求头字段名称。
	 *
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C 推荐</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

	/**
	 * HTTP {@code Age} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.1">RFC 7234 第 5.1 节</a>
	 */
	public static final String AGE = "Age";

	/**
	 * HTTP {@code Allow} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">RFC 7231 第 7.4.1 节</a>
	 */
	public static final String ALLOW = "Allow";

	/**
	 * HTTP {@code Authorization} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">RFC 7235 第 4.2 节</a>
	 */
	public static final String AUTHORIZATION = "Authorization";

	/**
	 * HTTP {@code Cache-Control} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC 7234 第 5.2 节</a>
	 */
	public static final String CACHE_CONTROL = "Cache-Control";

	/**
	 * HTTP {@code Connection} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.1">RFC 7230 第 6.1 节</a>
	 */
	public static final String CONNECTION = "Connection";

	/**
	 * HTTP {@code Content-Encoding} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.2.2">RFC 7231 第 3.1.2.2 节</a>
	 */
	public static final String CONTENT_ENCODING = "Content-Encoding";

	/**
	 * HTTP {@code Content-Disposition} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
	 */
	public static final String CONTENT_DISPOSITION = "Content-Disposition";

	/**
	 * HTTP {@code Content-Language} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.3.2">RFC 7231 第 3.1.3.2 节</a>
	 */
	public static final String CONTENT_LANGUAGE = "Content-Language";

	/**
	 * HTTP {@code Content-Length} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">RFC 7230 第 3.3.2 节</a>
	 */
	public static final String CONTENT_LENGTH = "Content-Length";

	/**
	 * HTTP {@code Content-Location} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">RFC 7231 第 3.1.4.2 节</a>
	 */
	public static final String CONTENT_LOCATION = "Content-Location";

	/**
	 * HTTP {@code Content-Range} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.2">RFC 7233 第 4.2 节</a>
	 */
	public static final String CONTENT_RANGE = "Content-Range";

	/**
	 * HTTP {@code Content-Type} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">RFC 7231 第 3.1.1.5 节</a>
	 */
	public static final String CONTENT_TYPE = "Content-Type";

	/**
	 * HTTP {@code Cookie} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.3.4">RFC 2109 第 4.3.4 节</a>
	 */
	public static final String COOKIE = "Cookie";

	/**
	 * HTTP {@code Date} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">RFC 7231 第 7.1.1.2 节</a>
	 */
	public static final String DATE = "Date";

	/**
	 * HTTP {@code ETag} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 第 2.3 节</a>
	 */
	public static final String ETAG = "ETag";

	/**
	 * HTTP {@code Expect} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.1">RFC 7231 第 5.1.1 节</a>
	 */
	public static final String EXPECT = "Expect";

	/**
	 * HTTP {@code Expires} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.3">RFC 7234 第 5.3 节</a>
	 */
	public static final String EXPIRES = "Expires";

	/**
	 * HTTP {@code From} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.1">RFC 7231 第 5.5.1 节</a>
	 */
	public static final String FROM = "From";

	/**
	 * HTTP {@code Host} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.4">RFC 7230 第 5.4 节</a>
	 */
	public static final String HOST = "Host";

	/**
	 * HTTP {@code If-Match} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">RFC 7232 第 3.1 节</a>
	 */
	public static final String IF_MATCH = "If-Match";

	/**
	 * HTTP {@code If-Modified-Since} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.3">RFC 7232 第 3.3 节</a>
	 */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	/**
	 * HTTP {@code If-None-Match} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.2">RFC 7232 第 3.2 节</a>
	 */
	public static final String IF_NONE_MATCH = "If-None-Match";

	/**
	 * HTTP {@code If-Range} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.2">RFC 7233 第 3.2 节</a>
	 */
	public static final String IF_RANGE = "If-Range";

	/**
	 * HTTP {@code If-Unmodified-Since} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.4">RFC 7232 第 3.4 节</a>
	 */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

	/**
	 * HTTP {@code Last-Modified} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.2">RFC 7232 第 2.2 节</a>
	 */
	public static final String LAST_MODIFIED = "Last-Modified";

	/**
	 * HTTP {@code Link} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 */
	public static final String LINK = "Link";

	/**
	 * HTTP {@code Location} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">RFC 7231 第 7.1.2 节</a>
	 */
	public static final String LOCATION = "Location";

	/**
	 * HTTP {@code Max-Forwards} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.2">RFC 7231 第 5.1.2 节</a>
	 */
	public static final String MAX_FORWARDS = "Max-Forwards";

	/**
	 * HTTP {@code Origin} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
	 */
	public static final String ORIGIN = "Origin";

	/**
	 * HTTP {@code Pragma} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">RFC 7234 第 5.4 节</a>
	 */
	public static final String PRAGMA = "Pragma";

	/**
	 * HTTP {@code Proxy-Authenticate} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.3">RFC 7235 第 4.3 节</a>
	 */
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";

	/**
	 * HTTP {@code Proxy-Authorization} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.4">RFC 7235 第 4.4 节</a>
	 */
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

	/**
	 * HTTP {@code Range} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.1">RFC 7233 第 3.1 节</a>
	 */
	public static final String RANGE = "Range";

	/**
	 * HTTP {@code Referer} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.2">RFC 7231 第 5.5.2 节</a>
	 */
	public static final String REFERER = "Referer";

	/**
	 * HTTP {@code Retry-After} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">RFC 7231 第 7.1.3 节</a>
	 */
	public static final String RETRY_AFTER = "Retry-After";

	/**
	 * HTTP {@code Server} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">RFC 7231 第 7.4.2 节</a>
	 */
	public static final String SERVER = "Server";

	/**
	 * HTTP {@code Set-Cookie} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.2.2">RFC 2109 第 4.2.2 节</a>
	 */
	public static final String SET_COOKIE = "Set-Cookie";

	/**
	 * HTTP {@code Set-Cookie2} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2965">RFC 2965</a>
	 */
	public static final String SET_COOKIE2 = "Set-Cookie2";

	/**
	 * HTTP {@code TE} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.3">RFC 7230 第 4.3 节</a>
	 */
	public static final String TE = "TE";

	/**
	 * HTTP {@code Trailer} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.4">RFC 7230 第 4.4 节</a>
	 */
	public static final String TRAILER = "Trailer";

	/**
	 * HTTP {@code Transfer-Encoding} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">RFC 7230 第 3.3.1 节</a>
	 */
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";

	/**
	 * HTTP {@code Upgrade} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.7">RFC 7230 第 6.7 节</a>
	 */
	public static final String UPGRADE = "Upgrade";

	/**
	 * HTTP {@code User-Agent} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.3">RFC 7231 第 5.5.3 节</a>
	 */
	public static final String USER_AGENT = "User-Agent";

	/**
	 * HTTP {@code Vary} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.4">RFC 7231 第 7.1.4 节</a>
	 */
	public static final String VARY = "Vary";

	/**
	 * HTTP {@code Via} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.7.1">RFC 7230 第 5.7.1 节</a>
	 */
	public static final String VIA = "Via";

	/**
	 * HTTP {@code Warning} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.5">RFC 7234 第 5.5 节</a>
	 */
	public static final String WARNING = "Warning";

	/**
	 * HTTP {@code WWW-Authenticate} 头字段名称。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.1">RFC 7235 第 4.1 节</a>
	 */
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";


	/**
	 * 一个空的 {@code HttpHeaders} 实例（不可变的）。
	 *
	 * @since 5.0
	 */
	public static final HttpHeaders EMPTY = new ReadOnlyHttpHeaders(new LinkedMultiValueMap<>());

	/**
	 * 匹配 ETag 头字段的多个字段值的模式，例如 "If-Match", "If-None-Match"。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 第 2.3 节</a>
	 */
	private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

	/**
	 * 十进制格式符号
	 */
	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);

	/**
	 * GMT时区
	 */
	private static final ZoneId GMT = ZoneId.of("GMT");

	/**
	 * HTTP RFC 中指定的带有时区的日期格式，用于格式化。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">RFC 7231 第 7.1.1.1 节</a>
	 */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(GMT);

	/**
	 * HTTP RFC 中指定的带有时区的日期格式，用于解析。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">RFC 7231 第 7.1.1.1 节</a>
	 */
	private static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[]{
			DateTimeFormatter.RFC_1123_DATE_TIME,
			DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.US).withZone(GMT)
	};

	/**
	 * 头部值映射
	 */
	final MultiValueMap<String, String> headers;


	/**
	 * 构造一个新的空的 {@code HttpHeaders} 对象实例。
	 * <p>这是常见的构造函数，使用不区分大小写的映射结构。
	 */
	public HttpHeaders() {
		this(CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH)));
	}

	/**
	 * 构造一个由现有映射支持的新的 {@code HttpHeaders} 实例。
	 * <p>此构造函数作为一种优化，用于适应现有的标头映射结构，主要用于框架内部使用。
	 *
	 * @param headers 标头映射（预期操作不区分大小写的键）
	 * @since 5.1
	 */
	public HttpHeaders(MultiValueMap<String, String> headers) {
		Assert.notNull(headers, "MultiValueMap must not be null");
		this.headers = headers;
	}


	/**
	 * 获取给定标头名称的标头值列表（如果有）。
	 *
	 * @param headerName 标头名称
	 * @return 标头值列表，如果不存在则返回空列表
	 * @since 5.2
	 */
	public List<String> getOrEmpty(Object headerName) {
		List<String> values = get(headerName);
		return (values != null ? values : Collections.emptyList());
	}

	/**
	 * 设置可接受的 {@linkplain MediaType 媒体类型} 列表，如 {@code Accept} 标头所指定。
	 */
	public void setAccept(List<MediaType> acceptableMediaTypes) {
		set(ACCEPT, MediaType.toString(acceptableMediaTypes));
	}

	/**
	 * 返回可接受的 {@linkplain MediaType 媒体类型} 列表，如 {@code Accept} 标头所指定。
	 * <p>当未指定可接受的媒体类型时，返回空列表。
	 */
	public List<MediaType> getAccept() {
		return MediaType.parseMediaTypes(get(ACCEPT));
	}

	/**
	 * 设置可接受的语言范围，如 {@literal Accept-Language} 标头所指定。
	 *
	 * @since 5.0
	 */
	public void setAcceptLanguage(List<Locale.LanguageRange> languages) {
		Assert.notNull(languages, "LanguageRange List must not be null");
		// 创建带有自定义符号的十进制格式
		DecimalFormat decimal = new DecimalFormat("0.0", DECIMAL_FORMAT_SYMBOLS);
		// 将语言范围流映射为带权重的语言范围字符串
		List<String> values = languages.stream()
				.map(range ->
						range.getWeight() == Locale.LanguageRange.MAX_WEIGHT ?
								range.getRange() :
								range.getRange() + ";q=" + decimal.format(range.getWeight()))
				.collect(Collectors.toList());
		// 将语言范围设置为逗号分隔的字符串
		set(ACCEPT_LANGUAGE, toCommaDelimitedString(values));
	}

	/**
	 * 从 {@literal "Accept-Language"} 标头返回语言范围。
	 * <p>如果您只需要排序的首选区域，只需使用 {@link #getAcceptLanguageAsLocales()}，
	 * 或者如果需要根据支持的区域列表进行过滤，可以将返回的列表传递给 {@link Locale#filter(List, Collection)}。
	 *
	 * @throws IllegalArgumentException 如果值无法转换为语言范围
	 * @since 5.0
	 */
	public List<Locale.LanguageRange> getAcceptLanguage() {
		// 获取Accept-Language头部的第一个值
		String value = getFirst(ACCEPT_LANGUAGE);
		// 如果值不为空，则解析为语言范围列表，否则返回空列表
		return (StringUtils.hasText(value) ? Locale.LanguageRange.parse(value) : Collections.emptyList());
	}

	/**
	 * 使用 {@link Locale} 的变体 {@link #setAcceptLanguage(List)}。
	 *
	 * @since 5.0
	 */
	public void setAcceptLanguageAsLocales(List<Locale> locales) {
		// 设置接受的语言范围
		setAcceptLanguage(locales.stream()
				// 将语言标签转换为语言范围，并收集为列表
				.map(locale -> new Locale.LanguageRange(locale.toLanguageTag()))
				.collect(Collectors.toList()));
	}

	/**
	 * 将每个 {@link java.util.Locale.LanguageRange} 转换为 {@link Locale} 的 {@link #getAcceptLanguage()} 的变体。
	 *
	 * @return 区域设置或空列表
	 * @throws IllegalArgumentException 如果值无法转换为区域设置
	 * @since 5.0
	 */
	public List<Locale> getAcceptLanguageAsLocales() {
		// 获取Accept-Language头部中的语言范围列表
		List<Locale.LanguageRange> ranges = getAcceptLanguage();
		// 如果范围列表为空，则返回空列表
		if (ranges.isEmpty()) {
			return Collections.emptyList();
		}
		// 将语言范围映射为Locale对象，并过滤掉不带显示名称的Locale对象
		return ranges.stream()
				.map(range -> Locale.forLanguageTag(range.getRange()))
				.filter(locale -> StringUtils.hasText(locale.getDisplayName()))
				.collect(Collectors.toList());
	}

	/**
	 * 设置 {@code PATCH} 方法的可接受 {@linkplain MediaType 媒体类型} 列表，如 {@code Accept-Patch} 标头所指定。
	 *
	 * @since 5.3.6
	 */
	public void setAcceptPatch(List<MediaType> mediaTypes) {
		set(ACCEPT_PATCH, MediaType.toString(mediaTypes));
	}

	/**
	 * 返回 {@code PATCH} 方法的可接受 {@linkplain MediaType 媒体类型} 列表，如 {@code Accept-Patch} 标头所指定。
	 * <p>当未指定可接受的媒体类型时，返回空列表。
	 *
	 * @since 5.3.6
	 */
	public List<MediaType> getAcceptPatch() {
		return MediaType.parseMediaTypes(get(ACCEPT_PATCH));
	}

	/**
	 * 设置 {@code Access-Control-Allow-Credentials} 响应标头的（新）值。
	 */
	public void setAccessControlAllowCredentials(boolean allowCredentials) {
		set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
	}

	/**
	 * 返回 {@code Access-Control-Allow-Credentials} 响应标头的值。
	 */
	public boolean getAccessControlAllowCredentials() {
		return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	/**
	 * 设置 {@code Access-Control-Allow-Headers} 响应标头的（新）值。
	 */
	public void setAccessControlAllowHeaders(List<String> allowedHeaders) {
		set(ACCESS_CONTROL_ALLOW_HEADERS, toCommaDelimitedString(allowedHeaders));
	}

	/**
	 * 返回 {@code Access-Control-Allow-Headers} 响应头的值。
	 */
	public List<String> getAccessControlAllowHeaders() {
		return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
	}

	/**
	 * 设置 {@code Access-Control-Allow-Methods} 响应头的（新）值。
	 */
	public void setAccessControlAllowMethods(List<HttpMethod> allowedMethods) {
		set(ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * 返回 {@code Access-Control-Allow-Methods} 响应头的值。
	 */
	public List<HttpMethod> getAccessControlAllowMethods() {
		// 创建一个存储HttpMethod的列表
		List<HttpMethod> result = new ArrayList<>();
		// 获取Access-Control-Allow-Methods头部的第一个值
		String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
		// 如果值不为null
		if (value != null) {
			// 将值按逗号分割成字符串数组
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			// 遍历每个字符串
			for (String token : tokens) {
				// 解析字符串为HttpMethod对象
				HttpMethod resolved = HttpMethod.resolve(token);
				// 如果解析成功，则添加到结果列表中
				if (resolved != null) {
					result.add(resolved);
				}
			}
		}
		// 返回结果列表
		return result;
	}

	/**
	 * 设置 {@code Access-Control-Allow-Origin} 响应头的（新）值。
	 */
	public void setAccessControlAllowOrigin(@Nullable String allowedOrigin) {
		setOrRemove(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
	}

	/**
	 * 返回 {@code Access-Control-Allow-Origin} 响应头的值。
	 */
	@Nullable
	public String getAccessControlAllowOrigin() {
		return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
	}

	/**
	 * 设置 {@code Access-Control-Expose-Headers} 响应头的（新）值。
	 */
	public void setAccessControlExposeHeaders(List<String> exposedHeaders) {
		set(ACCESS_CONTROL_EXPOSE_HEADERS, toCommaDelimitedString(exposedHeaders));
	}

	/**
	 * 返回 {@code Access-Control-Expose-Headers} 响应头的值。
	 */
	public List<String> getAccessControlExposeHeaders() {
		return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
	}

	/**
	 * 设置 {@code Access-Control-Max-Age} 响应头的（新）值。
	 *
	 * @since 5.2
	 */
	public void setAccessControlMaxAge(Duration maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge.getSeconds()));
	}

	/**
	 * 设置 {@code Access-Control-Max-Age} 响应标头的（新）值。
	 */
	public void setAccessControlMaxAge(long maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
	}

	/**
	 * 返回 {@code Access-Control-Max-Age} 响应标头的值。
	 * <p>当最大年龄未知时，返回 -1。
	 */
	public long getAccessControlMaxAge() {
		// 获取指定名称的第一个值
		String value = getFirst(ACCESS_CONTROL_MAX_AGE);

		// 如果值不为空，则将其解析为长整型返回；否则返回 -1
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置 {@code Access-Control-Request-Headers} 请求标头的（新）值。
	 */
	public void setAccessControlRequestHeaders(List<String> requestHeaders) {
		set(ACCESS_CONTROL_REQUEST_HEADERS, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * 返回 {@code Access-Control-Request-Headers} 请求标头的值。
	 */
	public List<String> getAccessControlRequestHeaders() {
		return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
	}

	/**
	 * 设置 {@code Access-Control-Request-Method} 请求标头的（新）值。
	 */
	public void setAccessControlRequestMethod(@Nullable HttpMethod requestMethod) {
		setOrRemove(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
	}

	/**
	 * 返回 {@code Access-Control-Request-Method} 请求标头的值。
	 */
	@Nullable
	public HttpMethod getAccessControlRequestMethod() {
		return HttpMethod.resolve(getFirst(ACCESS_CONTROL_REQUEST_METHOD));
	}

	/**
	 * 设置可接受的 {@linkplain Charset 字符集} 的列表，由 {@code Accept-Charset} 标头指定。
	 */
	public void setAcceptCharset(List<Charset> acceptableCharsets) {
		// 创建一个StringJoiner对象，用于拼接可接受的字符集
		StringJoiner joiner = new StringJoiner(", ");
		// 遍历可接受的字符集，将字符集的名称转换为小写，并添加到StringJoiner中
		for (Charset charset : acceptableCharsets) {
			joiner.add(charset.name().toLowerCase(Locale.ENGLISH));
		}
		// 将拼接好的字符串设置为ACCEPT_CHARSET头部的值
		set(ACCEPT_CHARSET, joiner.toString());
	}

	/**
	 * 返回可接受的 {@linkplain Charset 字符集} 的列表，由 {@code Accept-Charset} 标头指定。
	 */
	public List<Charset> getAcceptCharset() {
		// 获取 ACCEPT_CHARSET 头部的第一个值
		String value = getFirst(ACCEPT_CHARSET);

		// 如果值不为空
		if (value != null) {
			// 将值按逗号分隔为字符串数组
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");

			// 创建一个新的字符集列表
			List<Charset> result = new ArrayList<>(tokens.length);

			// 遍历每个令牌
			for (String token : tokens) {
				// 查找参数分隔符的索引
				int paramIdx = token.indexOf(';');

				// 获取字符集名称
				String charsetName;
				if (paramIdx == -1) {
					charsetName = token;
				} else {
					charsetName = token.substring(0, paramIdx);
				}

				// 如果字符集名称不为通配符 "*"，则将其解析为字符集并添加到结果列表中
				if (!charsetName.equals("*")) {
					result.add(Charset.forName(charsetName));
				}
			}

			// 返回结果列表
			return result;
		} else {
			// 如果值为空，则返回空列表
			return Collections.emptyList();
		}
	}

	/**
	 * 设置允许的 {@link HttpMethod HTTP 方法} 的集合，由 {@code Allow} 标头指定。
	 */
	public void setAllow(Set<HttpMethod> allowedMethods) {
		set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * 返回允许的 {@link HttpMethod HTTP 方法} 的集合，由 {@code Allow} 标头指定。
	 * <p>当未指定允许的方法时，返回空集。
	 */
	public Set<HttpMethod> getAllow() {
		// 获取ALLOW头部的第一个值
		String value = getFirst(ALLOW);
		// 如果值不为空
		if (StringUtils.hasLength(value)) {
			// 将值按逗号分割成字符串数组
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			// 创建一个存储HttpMethod的列表
			List<HttpMethod> result = new ArrayList<>(tokens.length);
			// 遍历每个字符串
			for (String token : tokens) {
				// 解析字符串为HttpMethod对象
				HttpMethod resolved = HttpMethod.resolve(token);
				// 如果解析成功，则添加到结果列表中
				if (resolved != null) {
					result.add(resolved);
				}
			}
			// 返回结果列表的EnumSet表示形式
			return EnumSet.copyOf(result);
		} else {
			// 如果值为空，则返回空的EnumSet
			return EnumSet.noneOf(HttpMethod.class);
		}
	}

	/**
	 * 将 {@linkplain #AUTHORIZATION 授权} 标头的值设置为基本身份验证，基于给定的用户名和密码。
	 * <p>请注意，此方法仅支持 {@link StandardCharsets#ISO_8859_1 ISO-8859-1} 字符集中的字符。
	 *
	 * @param username 用户名
	 * @param password 密码
	 * @throws IllegalArgumentException 如果 {@code username} 或 {@code password} 包含无法编码为 ISO-8859-1 的字符
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 * @since 5.1
	 */
	public void setBasicAuth(String username, String password) {
		setBasicAuth(username, password, null);
	}

	/**
	 * 将 {@linkplain #AUTHORIZATION 授权} 标头的值设置为基本身份验证，基于给定的用户名和密码。
	 *
	 * @param username 用户名
	 * @param password 密码
	 * @param charset  用于将凭据转换为八位序列的字符集。默认为 {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}。
	 * @throws IllegalArgumentException 如果 {@code username} 或 {@code password} 包含无法编码为给定字符集的字符
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 * @since 5.1
	 */
	public void setBasicAuth(String username, String password, @Nullable Charset charset) {
		setBasicAuth(encodeBasicAuth(username, password, charset));
	}

	/**
	 * 将 {@linkplain #AUTHORIZATION 授权} 标头的值设置为基本身份验证，基于给定的 {@linkplain #encodeBasicAuth 编码凭据}。
	 * <p>如果要缓存编码的凭据，建议使用此方法而不是 {@link #setBasicAuth(String, String)} 和
	 * {@link #setBasicAuth(String, String, Charset)}。
	 *
	 * @param encodedCredentials 编码的凭据
	 * @throws IllegalArgumentException 如果提供的凭据字符串为 {@code null} 或空白
	 * @see #setBasicAuth(String, String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 * @since 5.2
	 */
	public void setBasicAuth(String encodedCredentials) {
		Assert.hasText(encodedCredentials, "'encodedCredentials' must not be null or blank");
		set(AUTHORIZATION, "Basic " + encodedCredentials);
	}

	/**
	 * 将 {@linkplain #AUTHORIZATION Authorization} 头的值设置为给定的 Bearer 令牌。
	 *
	 * @param token Base64 编码的令牌
	 * @see <a href="https://tools.ietf.org/html/rfc6750">RFC 6750</a>
	 * @since 5.1
	 */
	public void setBearerAuth(String token) {
		set(AUTHORIZATION, "Bearer " + token);
	}

	/**
	 * 将配置好的 {@link CacheControl} 实例设置为 {@code Cache-Control} 头的新值。
	 *
	 * @since 5.0.5
	 */
	public void setCacheControl(CacheControl cacheControl) {
		setOrRemove(CACHE_CONTROL, cacheControl.getHeaderValue());
	}

	/**
	 * 设置 {@code Cache-Control} 头的（新）值。
	 */
	public void setCacheControl(@Nullable String cacheControl) {
		setOrRemove(CACHE_CONTROL, cacheControl);
	}

	/**
	 * 返回 {@code Cache-Control} 头的值。
	 */
	@Nullable
	public String getCacheControl() {
		return getFieldValues(CACHE_CONTROL);
	}

	/**
	 * 设置 {@code Connection} 头的（新）值。
	 */
	public void setConnection(String connection) {
		set(CONNECTION, connection);
	}

	/**
	 * 设置 {@code Connection} 头的（新）值。
	 */
	public void setConnection(List<String> connection) {
		set(CONNECTION, toCommaDelimitedString(connection));
	}

	/**
	 * 返回 {@code Connection} 头的值。
	 */
	public List<String> getConnection() {
		return getValuesAsList(CONNECTION);
	}

	/**
	 * 在创建 {@code "multipart/form-data"} 请求时设置 {@code Content-Disposition} 头。
	 * <p>通常应用程序不会直接设置此头，而是准备一个 {@code MultiValueMap<String, Object>}，
	 * 其中每个部分包含一个 Object 或一个 {@link org.springframework.core.io.Resource}，
	 * 然后将其传递给 {@code RestTemplate} 或 {@code WebClient}。
	 *
	 * @param name     控制名称
	 * @param filename 文件名（可能为 {@code null}）
	 * @see #getContentDisposition()
	 */
	public void setContentDispositionFormData(String name, @Nullable String filename) {
		Assert.notNull(name, "Name must not be null");
		// 创建一个 ContentDisposition.Builder 对象，用于构建表单数据的内容描述
		ContentDisposition.Builder disposition = ContentDisposition.formData().name(name);

		// 如果文件名非空
		if (StringUtils.hasText(filename)) {
			// 设置文件名
			disposition.filename(filename);
		}

		// 设置内容描述
		setContentDisposition(disposition.build());
	}

	/**
	 * 设置 {@literal Content-Disposition} 头。
	 * <p>这可以在响应中使用，以指示内容是否预期在浏览器中内联显示，或作为附件保存到本地。
	 * <p>它也可以用于 {@code "multipart/form-data"} 请求。
	 * 有关更多详细信息，请参阅 {@link #setContentDispositionFormData} 的说明。
	 *
	 * @see #getContentDisposition()
	 * @since 5.0
	 */
	public void setContentDisposition(ContentDisposition contentDisposition) {
		set(CONTENT_DISPOSITION, contentDisposition.toString());
	}

	/**
	 * 返回 {@literal Content-Disposition} 头的解析表示形式。
	 *
	 * @see #setContentDisposition(ContentDisposition)
	 * @since 5.0
	 */
	public ContentDisposition getContentDisposition() {
		// 获取CONTENT_DISPOSITION头部的第一个值
		String contentDisposition = getFirst(CONTENT_DISPOSITION);
		// 如果值不为空
		if (StringUtils.hasText(contentDisposition)) {
			// 解析值为ContentDisposition对象并返回
			return ContentDisposition.parse(contentDisposition);
		}
		// 如果值为空，则返回一个空的ContentDisposition对象
		return ContentDisposition.empty();
	}

	/**
	 * 设置内容语言的 {@link Locale}，由 {@literal Content-Language} 标头指定。
	 * <p>如果需要设置多个内容语言，请使用 {@code put(CONTENT_LANGUAGE, list)}。</p>
	 *
	 * @since 5.0
	 */
	public void setContentLanguage(@Nullable Locale locale) {
		setOrRemove(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
	}

	/**
	 * 获取内容语言的第一个 {@link Locale}，由 {@code Content-Language} 标头指定。
	 * <p>如果需要获取多个内容语言，请使用 {@link #getValuesAsList(String)}。</p>
	 *
	 * @return 内容语言的第一个 {@code Locale}，如果未知则返回 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public Locale getContentLanguage() {
		// 获取内容语言值列表，并将其转换为流
		return getValuesAsList(CONTENT_LANGUAGE)
				.stream()
				// 找到第一个内容语言值，并将其转换为Locale对象
				.findFirst()
				.map(Locale::forLanguageTag)
				// 如果找不到，则返回null
				.orElse(null);
	}

	/**
	 * 设置消息正文的字节长度，由 {@code Content-Length} 标头指定。
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * 返回消息正文的字节长度，由 {@code Content-Length} 标头指定。
	 * <p>当内容长度未知时返回 -1。</p>
	 */
	public long getContentLength() {
		// 获取 CONTENT_LENGTH 头部的第一个值
		String value = getFirst(CONTENT_LENGTH);

		// 如果值不为空，则将其解析为长整型返回；否则返回 -1
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置消息正文的 {@linkplain MediaType 媒体类型}，由 {@code Content-Type} 标头指定。
	 */
	public void setContentType(@Nullable MediaType mediaType) {
		// 如果媒体类型不为空
		if (mediaType != null) {
			Assert.isTrue(!mediaType.isWildcardType(), "Content-Type cannot contain wildcard type '*'");
			Assert.isTrue(!mediaType.isWildcardSubtype(), "Content-Type cannot contain wildcard subtype '*'");
			// 将媒体类型设置到请求头中
			set(CONTENT_TYPE, mediaType.toString());
		} else {
			// 如果媒体类型为空，则移除 CONTENT_TYPE 头部
			remove(CONTENT_TYPE);
		}
	}

	/**
	 * 返回消息正文的 {@linkplain MediaType 媒体类型}，由 {@code Content-Type} 标头指定。
	 * <p>当内容类型未知时返回 {@code null}。</p>
	 */
	@Nullable
	public MediaType getContentType() {
		// 获取第一个内容类型值
		String value = getFirst(CONTENT_TYPE);
		// 如果值非空
		return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
	}

	/**
	 * 设置消息创建时间，由 {@code Date} 标头指定。
	 *
	 * @since 5.2
	 */
	public void setDate(ZonedDateTime date) {
		setZonedDateTime(DATE, date);
	}

	/**
	 * 设置消息创建时间，由 {@code Date} 标头指定。
	 *
	 * @since 5.2
	 */
	public void setDate(Instant date) {
		setInstant(DATE, date);
	}

	/**
	 * 设置消息创建时间，由 {@code Date} 标头指定。
	 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 起的毫秒数。</p>
	 */
	public void setDate(long date) {
		setDate(DATE, date);
	}

	/**
	 * 返回消息创建时间，由 {@code Date} 标头指定。
	 * <p>日期以自 1970 年 1 月 1 日 GMT 起的毫秒数返回。当日期未知时返回 -1。</p>
	 *
	 * @throws IllegalArgumentException 如果值无法转换为日期
	 */
	public long getDate() {
		return getFirstDate(DATE);
	}

	/**
	 * 设置消息正文的 (新) 实体标签，由 {@code ETag} 标头指定。
	 */
	public void setETag(@Nullable String etag) {
		// 如果 ETag 不为空
		if (etag != null) {
			// 确保 ETag 以双引号或 'W/' 开头
			Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"),
					"Invalid ETag: does not start with W/ or \"");
			// 确保 ETag 以双引号结束
			Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
			// 将 ETag 设置到请求头中
			set(ETAG, etag);
		} else {
			// 如果 ETag 为空，则移除 ETAG 头部
			remove(ETAG);
		}
	}

	/**
	 * 返回消息正文的实体标签，由 {@code ETag} 标头指定。
	 */
	@Nullable
	public String getETag() {
		return getFirst(ETAG);
	}

	/**
	 * 设置消息失效的持续时间，由 {@code Expires} 头指定。
	 *
	 * @since 5.0.5
	 */
	public void setExpires(ZonedDateTime expires) {
		setZonedDateTime(EXPIRES, expires);
	}

	/**
	 * 设置消息失效的日期和时间，由 {@code Expires} 头指定。
	 *
	 * @since 5.2
	 */
	public void setExpires(Instant expires) {
		setInstant(EXPIRES, expires);
	}

	/**
	 * 设置消息失效的日期和时间，由 {@code Expires} 头指定。
	 * <p>日期应以自 1970 年 1 月 1 日 GMT 起的毫秒数指定。
	 */
	public void setExpires(long expires) {
		setDate(EXPIRES, expires);
	}

	/**
	 * 返回消息失效的日期和时间，由 {@code Expires} 头指定。
	 * <p>日期以自 1970 年 1 月 1 日 GMT 起的毫秒数返回。如果日期未知，则返回 -1。
	 *
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getExpires() {
		return getFirstDate(EXPIRES, false);
	}

	/**
	 * 设置 {@code Host} 头的（新）值。
	 * <p>如果给定的 {@linkplain InetSocketAddress#getPort() 端口} 是 {@code 0}，
	 * 则主机头将仅包含 {@linkplain InetSocketAddress#getHostString() 主机名}。
	 *
	 * @since 5.0
	 */
	public void setHost(@Nullable InetSocketAddress host) {
		// 如果主机不为空
		if (host != null) {
			// 获取主机字符串和端口号
			String value = host.getHostString();
			int port = host.getPort();
			// 如果端口号不为0，则将端口号添加到主机字符串中
			if (port != 0) {
				value = value + ":" + port;
			}
			// 设置HOST头部
			set(HOST, value);
		} else {
			// 如果主机为空，则移除HOST头部
			remove(HOST, null);
		}
	}

	/**
	 * 返回 {@code Host} 头的值（如果可用）。
	 * <p>如果头值不包含端口，则返回的地址中的 {@linkplain InetSocketAddress#getPort() 端口} 将为 {@code 0}。
	 *
	 * @since 5.0
	 */
	@Nullable
	public InetSocketAddress getHost() {
		// 获取 HOST 头部的第一个值
		String value = getFirst(HOST);

		// 如果值为空，则返回 null
		if (value == null) {
			return null;
		}

		// 初始化主机名和端口号
		String host = null;
		int port = 0;

		// 查找分隔符的索引位置
		int separator = (value.startsWith("[") ? value.indexOf(':', value.indexOf(']')) : value.lastIndexOf(':'));
		if (separator != -1) {
			// 如果找到分隔符，则提取主机名和端口号
			host = value.substring(0, separator);
			String portString = value.substring(separator + 1);
			try {
				// 解析端口号
				port = Integer.parseInt(portString);
			} catch (NumberFormatException ex) {
				// 忽略异常
			}
		}

		// 如果主机名为空，则设置为整个值
		if (host == null) {
			host = value;
		}

		// 创建未解析的 InetSocketAddress 对象并返回
		return InetSocketAddress.createUnresolved(host, port);
	}

	/**
	 * 设置 {@code If-Match} 头的（新）值。
	 *
	 * @since 4.3
	 */
	public void setIfMatch(String ifMatch) {
		set(IF_MATCH, ifMatch);
	}

	/**
	 * 设置 {@code If-Match} 头的（新）值。
	 *
	 * @since 4.3
	 */
	public void setIfMatch(List<String> ifMatchList) {
		set(IF_MATCH, toCommaDelimitedString(ifMatchList));
	}

	/**
	 * 返回 {@code If-Match} 头的值。
	 *
	 * @throws IllegalArgumentException 如果解析失败
	 * @since 4.3
	 */
	public List<String> getIfMatch() {
		return getETagValuesAsList(IF_MATCH);
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 *
	 * @since 5.1.4
	 */
	public void setIfModifiedSince(ZonedDateTime ifModifiedSince) {
		setZonedDateTime(IF_MODIFIED_SINCE, ifModifiedSince.withZoneSameInstant(GMT));
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 *
	 * @since 5.1.4
	 */
	public void setIfModifiedSince(Instant ifModifiedSince) {
		setInstant(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * 设置 {@code If-Modified-Since} 头部的（新）值。
	 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
	 */
	public void setIfModifiedSince(long ifModifiedSince) {
		setDate(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * 返回 {@code If-Modified-Since} 头部的值。
	 * <p>日期以自 1970 年 1 月 1 日 GMT 以来的毫秒数返回。当日期未知时返回 -1。
	 *
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getIfModifiedSince() {
		return getFirstDate(IF_MODIFIED_SINCE, false);
	}

	/**
	 * 设置 {@code If-None-Match} 头部的（新）值。
	 */
	public void setIfNoneMatch(String ifNoneMatch) {
		set(IF_NONE_MATCH, ifNoneMatch);
	}

	/**
	 * 设置 {@code If-None-Match} 头部的（新）值。
	 */
	public void setIfNoneMatch(List<String> ifNoneMatchList) {
		set(IF_NONE_MATCH, toCommaDelimitedString(ifNoneMatchList));
	}

	/**
	 * 返回 {@code If-None-Match} 头部的值。
	 *
	 * @throws IllegalArgumentException 如果解析失败
	 */
	public List<String> getIfNoneMatch() {
		return getETagValuesAsList(IF_NONE_MATCH);
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 *
	 * @since 5.1.4
	 */
	public void setIfUnmodifiedSince(ZonedDateTime ifUnmodifiedSince) {
		setZonedDateTime(IF_UNMODIFIED_SINCE, ifUnmodifiedSince.withZoneSameInstant(GMT));
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 *
	 * @since 5.1.4
	 */
	public void setIfUnmodifiedSince(Instant ifUnmodifiedSince) {
		setInstant(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * 设置 {@code If-Unmodified-Since} 头部的（新）值。
	 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
	 *
	 * @since 4.3
	 */
	public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
		setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * 返回 {@code If-Unmodified-Since} 头部的值。
	 * <p>日期以自 1970 年 1 月 1 日 GMT 以来的毫秒数返回。当日期未知时返回 -1。
	 *
	 * @see #getFirstZonedDateTime(String)
	 * @since 4.3
	 */
	public long getIfUnmodifiedSince() {
		return getFirstDate(IF_UNMODIFIED_SINCE, false);
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 *
	 * @since 5.1.4
	 */
	public void setLastModified(ZonedDateTime lastModified) {
		setZonedDateTime(LAST_MODIFIED, lastModified.withZoneSameInstant(GMT));
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 *
	 * @since 5.1.4
	 */
	public void setLastModified(Instant lastModified) {
		setInstant(LAST_MODIFIED, lastModified);
	}

	/**
	 * 设置资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
	 */
	public void setLastModified(long lastModified) {
		setDate(LAST_MODIFIED, lastModified);
	}

	/**
	 * 返回资源上次修改的时间，由 {@code Last-Modified} 头部指定。
	 * <p>日期以自 1970 年 1 月 1 日 GMT 以来的毫秒数返回。当日期未知时返回 -1。
	 *
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getLastModified() {
		return getFirstDate(LAST_MODIFIED, false);
	}

	/**
	 * 设置资源的（新）位置，由 {@code Location} 头部指定。
	 */
	public void setLocation(@Nullable URI location) {
		setOrRemove(LOCATION, (location != null ? location.toASCIIString() : null));
	}

	/**
	 * 返回 {@code Location} 头指定的资源的（新）位置。
	 * <p>当位置未知时返回 {@code null}。
	 */
	@Nullable
	public URI getLocation() {
		// 获取第一个位置值
		String value = getFirst(LOCATION);
		// 如果值不为null，则创建URI对象并返回，否则返回null
		return (value != null ? URI.create(value) : null);
	}

	/**
	 * 设置 {@code Origin} 头的（新）值。
	 */
	public void setOrigin(@Nullable String origin) {
		setOrRemove(ORIGIN, origin);
	}

	/**
	 * 返回 {@code Origin} 头的值。
	 */
	@Nullable
	public String getOrigin() {
		return getFirst(ORIGIN);
	}

	/**
	 * 设置 {@code Pragma} 头的（新）值。
	 */
	public void setPragma(@Nullable String pragma) {
		setOrRemove(PRAGMA, pragma);
	}

	/**
	 * 返回 {@code Pragma} 头的值。
	 */
	@Nullable
	public String getPragma() {
		return getFirst(PRAGMA);
	}

	/**
	 * 设置 {@code Range} 头的（新）值。
	 */
	public void setRange(List<HttpRange> ranges) {
		// 将HttpRange对象转换为字符串
		String value = HttpRange.toString(ranges);
		// 设置RANGE头部
		set(RANGE, value);
	}

	/**
	 * 返回 {@code Range} 头的值。
	 * <p>当范围未知时返回空列表。
	 */
	public List<HttpRange> getRange() {
		// 获取 RANGE 头部的第一个值
		String value = getFirst(RANGE);

		// 解析范围值并返回解析结果
		return HttpRange.parseRanges(value);
	}

	/**
	 * 设置 {@code Upgrade} 头的（新）值。
	 */
	public void setUpgrade(@Nullable String upgrade) {
		setOrRemove(UPGRADE, upgrade);
	}

	/**
	 * 返回 {@code Upgrade} 头的值。
	 */
	@Nullable
	public String getUpgrade() {
		return getFirst(UPGRADE);
	}

	/**
	 * 设置请求头的名称（例如 "Accept-Language"），该名称用于根据请求头的值进行内容协商，
	 * 并根据这些请求头的值产生变化。
	 *
	 * @param requestHeaders 请求头的名称
	 * @since 4.3
	 */
	public void setVary(List<String> requestHeaders) {
		set(VARY, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * 返回受内容协商影响的请求头名称。
	 *
	 * @since 4.3
	 */
	public List<String> getVary() {
		return getValuesAsList(VARY);
	}

	/**
	 * 将给定的日期作为字符串使用 RFC-1123 日期时间格式化程序进行格式化，并将其设置为给定标头名称下的值。
	 * 与 {@link #set(String, String)} 相同，但适用于日期标头。
	 *
	 * @since 5.0
	 */
	public void setZonedDateTime(String headerName, ZonedDateTime date) {
		set(headerName, DATE_FORMATTER.format(date));
	}

	/**
	 * 将给定的日期作为字符串使用 RFC-1123 日期时间格式化程序进行格式化，并将其设置为给定标头名称下的值。
	 * 与 {@link #set(String, String)} 相同，但适用于日期标头。
	 *
	 * @since 5.1.4
	 */
	public void setInstant(String headerName, Instant date) {
		setZonedDateTime(headerName, ZonedDateTime.ofInstant(date, GMT));
	}

	/**
	 * 将给定的日期作为字符串使用 RFC-1123 日期时间格式化程序进行格式化，并将其设置为给定标头名称下的值。
	 * 与 {@link #set(String, String)} 相同，但适用于日期标头。
	 *
	 * @see #setZonedDateTime(String, ZonedDateTime)
	 * @since 3.2.4
	 */
	public void setDate(String headerName, long date) {
		setInstant(headerName, Instant.ofEpochMilli(date));
	}

	/**
	 * 解析给定头部名称的第一个头部值作为日期，如果没有值则返回 -1，如果值无法解析为日期则抛出 {@link IllegalArgumentException}。
	 *
	 * @param headerName 头部名称
	 * @return 解析后的日期头部，如果没有则返回 -1
	 * @see #getFirstZonedDateTime(String)
	 * @since 3.2.4
	 */
	public long getFirstDate(String headerName) {
		return getFirstDate(headerName, true);
	}

	/**
	 * 解析给定头部名称的第一个头部值作为日期，如果没有值或者值无效（如果 {@code rejectInvalid=false}）则返回 -1，
	 * 如果值无法解析为日期则抛出 {@link IllegalArgumentException}。
	 *
	 * @param headerName    头部名称
	 * @param rejectInvalid 是否拒绝无效值，如果是则抛出 {@link IllegalArgumentException}，否则返回 -1
	 * @return 解析后的日期头部，如果没有或者无效则返回 -1
	 * @see #getFirstZonedDateTime(String, boolean)
	 * @since 3.2.4
	 */
	private long getFirstDate(String headerName, boolean rejectInvalid) {
		// 获取指定头部的第一个 ZonedDateTime 对象
		ZonedDateTime zonedDateTime = getFirstZonedDateTime(headerName, rejectInvalid);

		// 如果 ZonedDateTime 对象不为空，则将其转换为毫秒数并返回；否则返回 -1
		return (zonedDateTime != null ? zonedDateTime.toInstant().toEpochMilli() : -1);
	}

	/**
	 * 解析给定头部名称的第一个头部值作为日期，如果没有值则返回 {@code null}，如果值无法解析为日期则抛出 {@link IllegalArgumentException}。
	 *
	 * @param headerName 头部名称
	 * @return 解析后的日期头部，如果没有则返回 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public ZonedDateTime getFirstZonedDateTime(String headerName) {
		return getFirstZonedDateTime(headerName, true);
	}

	/**
	 * 解析给定头部名称的第一个头部值作为日期，如果没有值或者值无效（如果 {@code rejectInvalid=false}）则返回 {@code null}，
	 * 如果值无法解析为日期则抛出 {@link IllegalArgumentException}。
	 *
	 * @param headerName    头部名称
	 * @param rejectInvalid 是否拒绝无效值，如果是则抛出 {@link IllegalArgumentException}，否则返回 {@code null}
	 * @return 解析后的日期头部，如果没有或者无效则返回 {@code null}
	 */
	@Nullable
	private ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
		// 获取指定头部的第一个值
		String headerValue = getFirst(headerName);

		// 如果头部值为空，则返回 null
		if (headerValue == null) {
			// 没有发送任何头部值
			return null;
		}

		// 如果头部值的长度大于等于3
		if (headerValue.length() >= 3) {
			// 短的类似 "0" 或 "-1" 的值永远不是有效的 HTTP 日期头...
			// 让我们只针对足够长的值进行 DateTimeFormatter 解析。

			// 查找参数索引
			int parametersIndex = headerValue.indexOf(';');
			if (parametersIndex != -1) {
				headerValue = headerValue.substring(0, parametersIndex);
			}

			// 尝试使用每个日期格式化器解析日期头部值
			for (DateTimeFormatter dateFormatter : DATE_PARSERS) {
				try {
					return ZonedDateTime.parse(headerValue, dateFormatter);
				} catch (DateTimeParseException ex) {
					// 忽略异常
				}
			}
		}

		// 如果拒绝无效日期并且无法解析日期头部值，则抛出异常
		if (rejectInvalid) {
			throw new IllegalArgumentException("Cannot parse date value \"" + headerValue +
					"\" for \"" + headerName + "\" header");
		}

		// 返回 null
		return null;
	}

	/**
	 * 返回给定头部名称的所有值，即使该头部设置了多次。
	 *
	 * @param headerName 头部名称
	 * @return 所有关联值的列表
	 * @since 4.3
	 */
	public List<String> getValuesAsList(String headerName) {
		// 获取指定头部名称的值列表
		List<String> values = get(headerName);
		// 如果值列表不为null
		if (values != null) {
			// 创建一个新的结果列表
			List<String> result = new ArrayList<>();
			// 遍历值列表
			for (String value : values) {
				// 如果值不为null
				if (value != null) {
					// 使用逗号分隔符将值拆分为字符串数组，并添加到结果列表中
					Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
				}
			}
			// 返回结果列表
			return result;
		}
		// 如果值列表为null，则返回空列表
		return Collections.emptyList();
	}

	/**
	 * 移除常见的 {@code "Content-*"} HTTP 头部。
	 * <p>如果由于错误而无法写入预期的主体，则应该从响应中清除此类头部。
	 *
	 * @since 5.2.3
	 */
	public void clearContentHeaders() {
		this.headers.remove(HttpHeaders.CONTENT_DISPOSITION);
		this.headers.remove(HttpHeaders.CONTENT_ENCODING);
		this.headers.remove(HttpHeaders.CONTENT_LANGUAGE);
		this.headers.remove(HttpHeaders.CONTENT_LENGTH);
		this.headers.remove(HttpHeaders.CONTENT_LOCATION);
		this.headers.remove(HttpHeaders.CONTENT_RANGE);
		this.headers.remove(HttpHeaders.CONTENT_TYPE);
	}

	/**
	 * 从 ETag 头部的字段值获取合并的结果。
	 *
	 * @param headerName 头部名称
	 * @return 合并的结果列表
	 * @throws IllegalArgumentException 如果解析失败
	 * @since 4.3
	 */
	protected List<String> getETagValuesAsList(String headerName) {
		// 获取头部名称对应的值列表
		List<String> values = get(headerName);
		// 如果值列表不为空
		if (values != null) {
			// 创建一个新的结果列表
			List<String> result = new ArrayList<>();
			// 遍历值列表中的每个值
			for (String value : values) {
				// 如果值不为null
				if (value != null) {
					// 使用ETAG_HEADER_VALUE_PATTERN正则表达式匹配值中的ETag
					Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
					// 循环匹配结果
					while (matcher.find()) {
						// 如果匹配到"*"，则直接添加到结果列表中
						if ("*".equals(matcher.group())) {
							result.add(matcher.group());
						} else {
							// 否则，添加第一个分组的值（ETag）
							result.add(matcher.group(1));
						}
					}
					// 如果结果列表为空，抛出IllegalArgumentException异常
					if (result.isEmpty()) {
						throw new IllegalArgumentException(
								"Could not parse header '" + headerName + "' with value '" + value + "'");
					}
				}
			}
			// 返回结果列表
			return result;
		}
		// 如果值列表为空，则返回空列表
		return Collections.emptyList();
	}

	/**
	 * 从多值头字段的字段值中检索组合结果。
	 *
	 * @param headerName 标头名称
	 * @return 组合结果
	 * @since 4.3
	 */
	@Nullable
	protected String getFieldValues(String headerName) {
		// 获取指定头部的所有值
		List<String> headerValues = get(headerName);

		// 如果头部值列表不为空，则将其转换为逗号分隔的字符串并返回；否则返回 null
		return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
	}

	/**
	 * 将给定的头值列表转换为逗号分隔的结果。
	 *
	 * @param headerValues 头值列表
	 * @return 使用逗号分隔的组合结果
	 */
	protected String toCommaDelimitedString(List<String> headerValues) {
		// 创建一个StringJoiner对象，用于连接多个字符串
		StringJoiner joiner = new StringJoiner(", ");
		// 遍历头部值数组
		for (String val : headerValues) {
			// 如果值不为null，则添加到StringJoiner中
			if (val != null) {
				joiner.add(val);
			}
		}
		// 返回连接后的字符串
		return joiner.toString();
	}

	/**
	 * 设置给定的头值，或者如果为 {@code null} 则移除该头。
	 *
	 * @param headerName  头名称
	 * @param headerValue 头值，如果没有则为 {@code null}
	 */
	private void setOrRemove(String headerName, @Nullable String headerValue) {
		// 如果头部值不为 null，则设置指定头部的值为给定值；
		if (headerValue != null) {
			set(headerName, headerValue);
		} else {
			// 否则移除指定头部
			remove(headerName);
		}

	}


	// MultiValueMap 实现

	/**
	 * 返回给定头名称的第一个头值，如果有的话。
	 *
	 * @param headerName 头名称
	 * @return 第一个头值，如果没有则为 {@code null}
	 */
	@Override
	@Nullable
	public String getFirst(String headerName) {
		return this.headers.getFirst(headerName);
	}

	/**
	 * 在给定名称下添加给定的单个头值。
	 *
	 * @param headerName  头名称
	 * @param headerValue 头值
	 * @throws UnsupportedOperationException 如果不支持添加头
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, @Nullable String headerValue) {
		this.headers.add(headerName, headerValue);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		this.headers.addAll(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		this.headers.addAll(values);
	}

	/**
	 * 在给定名称下设置给定的单个头值。
	 *
	 * @param headerName  头名称
	 * @param headerValue 头值
	 * @throws UnsupportedOperationException 如果不支持添加头
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	@Override
	public void set(String headerName, @Nullable String headerValue) {
		this.headers.set(headerName, headerValue);
	}

	@Override
	public void setAll(Map<String, String> values) {
		this.headers.setAll(values);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return this.headers.toSingleValueMap();
	}


	// Map 实现

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpHeaders)) {
			return false;
		}
		return unwrap(this).equals(unwrap((HttpHeaders) other));
	}

	private static MultiValueMap<String, String> unwrap(HttpHeaders headers) {
		while (headers.headers instanceof HttpHeaders) {
			headers = (HttpHeaders) headers.headers;
		}
		return headers.headers;
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return formatHeaders(this.headers);
	}


	/**
	 * 如果必要，应用只读 {@code HttpHeaders} 包装器在给定头周围，
	 * <p>还会缓存 "Accept" 和 "Content-Type" 标头的解析表示。
	 *
	 * @param headers 要公开的头
	 * @return 头的只读变体，或原始头本身（如果它恰好是只读 {@code HttpHeaders} 实例）
	 * @since 5.3
	 */
	public static HttpHeaders readOnlyHttpHeaders(MultiValueMap<String, String> headers) {
		return (headers instanceof HttpHeaders ?
				readOnlyHttpHeaders((HttpHeaders) headers) : new ReadOnlyHttpHeaders(headers));
	}

	/**
	 * 如果必要，应用只读 {@code HttpHeaders} 包装器在给定头周围，
	 * <p>还会缓存 "Accept" 和 "Content-Type" 标头的解析表示。
	 *
	 * @param headers 要公开的头
	 * @return 头的只读变体，或原始头本身（如果它恰好是只读 {@code HttpHeaders} 实例）
	 */
	public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		return (headers instanceof ReadOnlyHttpHeaders ? headers : new ReadOnlyHttpHeaders(headers.headers));
	}

	/**
	 * 删除可能先前通过 {@link #readOnlyHttpHeaders(HttpHeaders)} 应用在给定头周围的任何只读包装器。
	 *
	 * @param headers 要公开的头
	 * @return 可写头的变体，或原始头本身
	 * @since 5.1.1
	 */
	public static HttpHeaders writableHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		if (headers == EMPTY) {
			// 如果头部为空，则返回空的头部
			return new HttpHeaders();
		}
		return (headers instanceof ReadOnlyHttpHeaders ? new HttpHeaders(headers.headers) : headers);
	}

	/**
	 * 帮助格式化 HTTP 标头值，因为 HTTP 标头值本身可以包含逗号分隔的值，
	 * 使用逗号分隔的值可以与也使用逗号分隔的条目的常规 {@link Map} 格式化变得混淆。
	 *
	 * @param headers 要格式化的头
	 * @return 头的字符串表示形式
	 * @since 5.1.4
	 */
	public static String formatHeaders(MultiValueMap<String, String> headers) {
		return headers.entrySet().stream()
				.map(entry -> {
					// 将头部映射项转换为字符串
					List<String> values = entry.getValue();
					return entry.getKey() + ":" + (values.size() == 1 ?
							"\"" + values.get(0) + "\"" :
							values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
				})
				// 使用逗号和方括号进行连接
				.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * 将给定的用户名和密码编码为基本身份验证凭据。
	 * <p>此方法返回的编码凭据可以提供给 {@link #setBasicAuth(String)} 以设置基本身份验证标头。
	 *
	 * @param username 要编码的用户名
	 * @param password 要编码的密码
	 * @param charset  要用于将凭据转换为字节序列的字符集。默认为 {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}。
	 * @throws IllegalArgumentException 如果 {@code username} 或 {@code password}
	 *                                  包含无法编码为给定字符集的字符
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 * @since 5.2
	 */
	public static String encodeBasicAuth(String username, String password, @Nullable Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		Assert.notNull(password, "Password must not be null");
		// 如果字符集为空，则使用ISO_8859_1字符集
		if (charset == null) {
			charset = StandardCharsets.ISO_8859_1;
		}

		// 获取字符集的编码器
		CharsetEncoder encoder = charset.newEncoder();
		// 如果用户名或密码包含无法编码的字符，则抛出IllegalArgumentException异常
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to " + charset.displayName());
		}

		// 构建认证字符串：用户名+":"+密码
		String credentialsString = username + ":" + password;
		// 对认证字符串进行编码，并使用指定字符集编码
		byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
		// 将编码后的字节数组转换为字符串，并使用指定字符集解码
		return new String(encodedBytes, charset);
	}

	/**
	 * Package-private: 用于ResponseCookie
	 */
	static String formatDate(long date) {
		// 将毫秒级时间戳转换为Instant对象
		Instant instant = Instant.ofEpochMilli(date);
		// 将Instant对象转换为GMT时区的ZonedDateTime对象
		ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
		// 使用日期格式化器格式化时间
		return DATE_FORMATTER.format(time);
	}

}
