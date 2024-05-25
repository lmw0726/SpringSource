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

package org.springframework.web.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;
import org.springframework.web.util.UriComponents.UriTemplateVariables;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link UriComponents} 的构建器。
 *
 * <p>典型的使用方式包括：
 * <ol>
 * <li>使用静态工厂方法（如 {@link #fromPath(String)} 或 {@link #fromUri(URI)}）创建一个 {@code UriComponentsBuilder}。</li>
 * <li>通过相应的方法设置各种 URI 组件（{@link #scheme(String)}、{@link #userInfo(String)}、{@link #host(String)}、
 * {@link #port(int)}、{@link #path(String)}、{@link #pathSegment(String...)}、{@link #queryParam(String, Object...)} 和
 * {@link #fragment(String)}）。</li>
 * <li>使用 {@link #build()} 方法构建 {@link UriComponents} 实例。</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 * @since 3.1
 */
public class UriComponentsBuilder implements UriBuilder, Cloneable {

	/**
	 * 匹配查询参数的正则表达式模式。
	 */
	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	/**
	 * 匹配 URI 方案的正则表达式模式。
	 */
	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	/**
	 * 匹配 HTTP URI 的正则表达式模式。
	 */
	private static final String HTTP_PATTERN = "(?i)(http|https):";

	/**
	 * 匹配用户信息的正则表达式模式。
	 */
	private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

	/**
	 * 匹配 IPv4 主机的正则表达式模式。
	 */
	private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

	/**
	 * 匹配 IPv6 主机的正则表达式模式。
	 */
	private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}:.]*[%\\p{Alnum}]*]";

	/**
	 * 匹配主机的正则表达式模式。
	 */
	private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

	/**
	 * 匹配端口的正则表达式模式。
	 */
	private static final String PORT_PATTERN = "(\\{[^}]+\\}?|[^/?#]*)";

	/**
	 * 匹配路径的正则表达式模式。
	 */
	private static final String PATH_PATTERN = "([^?#]*)";

	/**
	 * 匹配查询字符串的正则表达式模式。
	 */
	private static final String QUERY_PATTERN = "([^#]*)";

	/**
	 * 匹配最后部分的正则表达式模式。
	 */
	private static final String LAST_PATTERN = "(.*)";

	/**
	 * 匹配uri的正则表达式模式。请参见RFC 3986，附录b
	 */
	private static final Pattern URI_PATTERN = Pattern.compile(
			"^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
					")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	/**
	 * 匹配 HTTP URL 的正则表达式模式。
	 */
	private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
			"^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
					PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	/**
	 * Forwarded 头部中值的模式。
	 */
	private static final String FORWARDED_VALUE = "\"?([^;,\"]+)\"?";

	/**
	 * Forwarded 头部中 Host 字段的模式。
	 */
	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("(?i:host)=" + FORWARDED_VALUE);

	/**
	 * Forwarded 头部中 Proto 字段的模式。
	 */
	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("(?i:proto)=" + FORWARDED_VALUE);

	/**
	 * Forwarded 头部中 For 字段的模式。
	 */
	private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=" + FORWARDED_VALUE);

	/**
	 * 空值数组。
	 */
	private static final Object[] EMPTY_VALUES = new Object[0];


	/**
	 * URI的方案部分。
	 */
	@Nullable
	private String scheme;

	/**
	 * URI的方案特定部分。
	 */
	@Nullable
	private String ssp;

	/**
	 * URI的用户信息部分。
	 */
	@Nullable
	private String userInfo;

	/**
	 * URI的主机部分。
	 */
	@Nullable
	private String host;

	/**
	 * URI的端口部分。
	 */
	@Nullable
	private String port;

	/**
	 * URI的路径构建器。
	 */
	private CompositePathComponentBuilder pathBuilder;

	/**
	 * URI的查询参数。
	 */
	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	/**
	 * URI的片段部分。
	 */
	@Nullable
	private String fragment;

	/**
	 * URI的变量映射。
	 */
	private final Map<String, Object> uriVariables = new HashMap<>(4);

	/**
	 * 是否对URI模板进行编码。
	 */
	private boolean encodeTemplate;

	/**
	 * 字符集。
	 */
	private Charset charset = StandardCharsets.UTF_8;


	/**
	 * 默认构造函数。受保护以防止直接实例化。
	 *
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
		this.pathBuilder = new CompositePathComponentBuilder();
	}

	/**
	 * 创建给定 UriComponentsBuilder 的深度拷贝。
	 *
	 * @param other 要复制的另一个构建器
	 * @since 4.1.3
	 */
	protected UriComponentsBuilder(UriComponentsBuilder other) {
		this.scheme = other.scheme;
		this.ssp = other.ssp;
		this.userInfo = other.userInfo;
		this.host = other.host;
		this.port = other.port;
		this.pathBuilder = other.pathBuilder.cloneBuilder();
		this.uriVariables.putAll(other.uriVariables);
		this.queryParams.addAll(other.queryParams);
		this.fragment = other.fragment;
		this.encodeTemplate = other.encodeTemplate;
		this.charset = other.charset;
	}


	// 工厂方法

	/**
	 * 创建一个新的、空的构建器。
	 *
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder newInstance() {
		return new UriComponentsBuilder();
	}

	/**
	 * 创建一个使用给定路径初始化的构建器。
	 *
	 * @param path 用于初始化的路径
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromPath(String path) {
		// 创建URL组件构建器
		UriComponentsBuilder builder = new UriComponentsBuilder();
		// 设置路径
		builder.path(path);
		return builder;
	}

	/**
	 * 创建一个从给定 {@code URI} 初始化的构建器。
	 * <p><strong>注意：</strong>结果构建器中的组件将完全编码（原始）形式，并且进一步的更改也必须提供完全编码的值，
	 * 例如通过 {@link UriUtils} 中的方法。
	 * 此外，请使用 {@link #build(boolean)} 方法并传递值为 "true"，以构建 {@link UriComponents} 实例，以指示组件已编码。
	 *
	 * @param uri 用于初始化的 URI
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		// 创建URL组件构建器
		UriComponentsBuilder builder = new UriComponentsBuilder();
		// 设置URI
		builder.uri(uri);
		return builder;
	}

	/**
	 * 使用给定的 URI 字符串初始化一个构建器。
	 * <p><strong>注意：</strong> 保留字符的存在可能会阻止 URI 字符串的正确解析。
	 * 例如，如果查询参数包含 {@code '='} 或 {@code '&'} 字符，则无法明确解析查询字符串。
	 * 此类值应替换为 URI 变量以启用正确的解析：
	 * <pre class="code">
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param uri 要初始化的 URI 字符串
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Assert.notNull(uri, "URI must not be null");
		// 使用正则表达式匹配 URI
		Matcher matcher = URI_PATTERN.matcher(uri);
		// 如果匹配成功
		if (matcher.matches()) {
			// 创建 UriComponentsBuilder 对象
			UriComponentsBuilder builder = new UriComponentsBuilder();
			// 提取 URI 中的各个部分
			// 提取方案名称
			String scheme = matcher.group(2);
			// 提取用户信息
			String userInfo = matcher.group(5);
			// 提取主机
			String host = matcher.group(6);
			// 提取端口
			String port = matcher.group(8);
			// 提取路径
			String path = matcher.group(9);
			// 提取查询参数
			String query = matcher.group(11);
			// 提取片段
			String fragment = matcher.group(13);
			// 是否是非明确层次结构的URI
			boolean opaque = false;
			// 如果方案不为空
			if (StringUtils.hasLength(scheme)) {
				// 获取除方案外的剩余字符串
				String rest = uri.substring(scheme.length());
				// 如果 剩余字符串 不是以 ":/" 开头，则将 opaque 置为 true
				if (!rest.startsWith(":/")) {
					// 是非明确层次结构的URI
					opaque = true;
				}
			}
			// 设置方案
			builder.scheme(scheme);
			// 如果不是 明确层次结构的 URI
			if (opaque) {
				//从 URI 字符串中截取了方案后的部分（Scheme-Specific Part）
				String ssp = uri.substring(scheme.length() + 1);
				if (StringUtils.hasLength(fragment)) {
					// 如果有片段，则从方案特定部分（SSP）中移除片段。
					ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
				}
				// 将处理后的方案特定部分设置到 UriComponentsBuilder 中。
				builder.schemeSpecificPart(ssp);
			} else {
				// 如果是层次结构 URI
				if (StringUtils.hasLength(scheme) && scheme.startsWith("http") && !StringUtils.hasLength(host)) {
					// 如果方案不为空，且以 "http" 开头，并且主机名为空，则抛出异常
					throw new IllegalArgumentException("[" + uri + "] is not a valid HTTP URL");
				}
				// 设置用户信息、主机、端口、路径和查询参数
				builder.userInfo(userInfo);
				builder.host(host);
				if (StringUtils.hasLength(port)) {
					builder.port(port);
				}
				builder.path(path);
				builder.query(query);
			}
			// 设置片段
			if (StringUtils.hasText(fragment)) {
				builder.fragment(fragment);
			}
			// 返回构建好的 UriComponentsBuilder 对象
			return builder;
		} else {
			// 如果 URI 格式不匹配，则抛出异常
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * 从给定的 HTTP URL 字符串创建一个 URI 组件构建器。
	 * <p><strong>注意：</strong> 保留字符的存在可能会阻止 URI 字符串的正确解析。
	 * 例如，如果查询参数包含 {@code '='} 或 {@code '&'} 字符，则无法明确解析查询字符串。
	 * 此类值应替换为 URI 变量以启用正确的解析：
	 * <pre class="code">
	 * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param httpUrl 源 URI
	 * @return URI 的 URI 组件
	 */
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Assert.notNull(httpUrl, "HTTP URL must not be null");
		// 使用正则表达式匹配 HTTP URL
		Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
		// 如果匹配成功
		if (matcher.matches()) {
			// 创建 UriComponentsBuilder 对象
			UriComponentsBuilder builder = new UriComponentsBuilder();
			// 提取 HTTP URL 中的方案部分
			String scheme = matcher.group(1);
			// 将方案转换为小写并设置到 builder 中
			builder.scheme(scheme != null ? scheme.toLowerCase() : null);
			// 设置用户信息
			builder.userInfo(matcher.group(4));
			// 提取主机名
			String host = matcher.group(5);
			// 如果方案不为空且主机名为空，则抛出异常
			if (StringUtils.hasLength(scheme) && !StringUtils.hasLength(host)) {
				throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
			}
			// 设置主机名
			builder.host(host);
			// 提取端口号
			String port = matcher.group(7);
			// 如果端口号不为空，则设置端口
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			// 设置路径
			builder.path(matcher.group(8));
			// 设置查询参数
			builder.query(matcher.group(10));
			// 提取片段
			String fragment = matcher.group(12);
			// 如果片段不为空，则设置片段
			if (StringUtils.hasText(fragment)) {
				builder.fragment(fragment);
			}
			// 返回构建好的 UriComponentsBuilder 对象
			return builder;
		} else {
			// 如果 HTTP URL 格式不匹配，则抛出异常
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	/**
	 * 从给定的 HttpRequest 中关联的 URI 创建一个新的 {@code UriComponents} 对象，同时也覆盖来自头部的值
	 * "Forwarded" (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * 或者如果未找到 "Forwarded"，则使用 "X-Forwarded-Host", "X-Forwarded-Port", 和 "X-Forwarded-Proto"。
	 *
	 * @param request 源请求
	 * @return URI 的 URI 组件
	 * @see #parseForwardedFor(HttpRequest, InetSocketAddress)
	 * @since 4.1.5
	 */
	public static UriComponentsBuilder fromHttpRequest(HttpRequest request) {
		return fromUri(request.getURI()).adaptFromForwardedHeaders(request.getHeaders());
	}

	/**
	 * 将第一个 "Forwarded: for=..." 或 "X-Forwarded-For" 头部的值解析为表示客户端地址的 {@code InetSocketAddress}。
	 *
	 * @param request       可能包含转发头部的头部请求
	 * @param remoteAddress 当前的远程地址
	 * @return 一个 {@code InetSocketAddress}，其中包含提取的主机和端口，如果头部不存在，则返回 {@code null}。
	 * @see <a href="https://tools.ietf.org/html/rfc7239#section-5.2">RFC 7239，第 5.2 节</a>
	 * @since 5.3
	 */
	@Nullable
	public static InetSocketAddress parseForwardedFor(
			HttpRequest request, @Nullable InetSocketAddress remoteAddress) {

		// 如果 远程地址 不为空，则获取其端口号；
		int port = (remoteAddress != null ?
				// 否则根据请求的方案确定默认端口号（https：443，http：80）
				remoteAddress.getPort() : "https".equals(request.getURI().getScheme()) ? 443 : 80);

		// 获取 Forwarded 头部的值
		String forwardedHeader = request.getHeaders().getFirst("Forwarded");
		// 如果 Forwarded 头部的值不为空
		if (StringUtils.hasText(forwardedHeader)) {
			// 提取第一个逗号之前的值作为 将要使用的forwarded值
			String forwardedToUse = StringUtils.tokenizeToStringArray(forwardedHeader, ",")[0];
			// 使用正则表达式匹配 将要使用的forwarded值 中的信息
			Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwardedToUse);
			// 如果匹配成功
			if (matcher.find()) {
				// 获取匹配的值并去除首尾空格
				String value = matcher.group(1).trim();
				String host = value;
				// 查找端口分隔符和方括号
				int portSeparatorIdx = value.lastIndexOf(':');
				int squareBracketIdx = value.lastIndexOf(']');
				if (portSeparatorIdx > squareBracketIdx) {
					// 如果端口分隔符在方括号之后
					if (squareBracketIdx == -1 && value.indexOf(':') != portSeparatorIdx) {
						// 如果没有方括号，且没有冒号，则抛出异常
						throw new IllegalArgumentException("Invalid IPv4 address: " + value);
					}
					// 截取 host
					host = value.substring(0, portSeparatorIdx);
					try {
						// 截取端口号
						port = Integer.parseInt(value.substring(portSeparatorIdx + 1));
					} catch (NumberFormatException ex) {
						throw new IllegalArgumentException(
								"Failed to parse a port from \"forwarded\"-type header value: " + value);
					}
				}
				// 创建并返回 InetSocketAddress 对象
				return InetSocketAddress.createUnresolved(host, port);
			}
		}

		// 获取 X-Forwarded-For 头部的值
		String forHeader = request.getHeaders().getFirst("X-Forwarded-For");
		// 如果 X-Forwarded-For 头部的值不为空
		if (StringUtils.hasText(forHeader)) {
			// 提取第一个逗号之前的值作为 host
			String host = StringUtils.tokenizeToStringArray(forHeader, ",")[0];
			// 创建并返回 InetSocketAddress 对象
			return InetSocketAddress.createUnresolved(host, port);
		}

		// 如果以上条件都不满足，则返回 null
		return null;
	}

	/**
	 * 通过解析 HTTP 请求的 "Origin" 头部来创建一个实例。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
	 */
	public static UriComponentsBuilder fromOriginHeader(String origin) {
		// 使用正则表达式匹配 origin 头部
		Matcher matcher = URI_PATTERN.matcher(origin);
		// 如果匹配成功
		if (matcher.matches()) {
			// 创建 UriComponentsBuilder 对象
			UriComponentsBuilder builder = new UriComponentsBuilder();
			// 提取方案、主机和端口
			String scheme = matcher.group(2);
			String host = matcher.group(6);
			String port = matcher.group(8);
			// 如果方案不为空，则设置方案
			if (StringUtils.hasLength(scheme)) {
				builder.scheme(scheme);
			}
			// 设置主机
			builder.host(host);
			// 如果端口不为空，则设置端口
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			// 返回构建好的 UriComponentsBuilder 对象
			return builder;
		} else {
			// 如果 origin 格式不匹配，则抛出异常
			throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
		}
	}


	// 编码方法

	/**
	 * 请求在构建时预先对 URI 模板进行编码，并在扩展时单独对 URI 变量进行编码。
	 * <p>与 {@link UriComponents#encode()} 相比，该方法对 URI 模板具有相同的效果，
	 * 即通过将非 ASCII 字符和非法字符（在 URI 组件类型内）替换为转义八位字节来对每个 URI 组件进行编码。
	 * 但是 URI 变量的编码更加严格，通过转义具有保留含义的字符。
	 * <p>对于大多数情况，此方法更有可能给出预期的结果，因为它将 URI 变量视为不透明数据，需要完全编码，
	 * 而 {@link UriComponents#encode()} 则适用于故意扩展包含保留字符的 URI 变量的情况。
	 * <p>例如 ';' 在路径中是合法的，但具有保留含义。该方法将 ";" 替换为 "%3B" 在 URI 变量中，但不在 URI 模板中。
	 * 相比之下，{@link UriComponents#encode()} 从不替换 ";"，因为它是路径中的合法字符。
	 * <p>当根本不扩展 URI 变量时，请使用 {@link UriComponents#encode()}，因为它还将对任意看起来像 URI 变量的内容进行编码。
	 *
	 * @since 5.0.8
	 */
	public final UriComponentsBuilder encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * 具有与 "UTF-8" 不同的字符集的 {@link #encode()} 变体。
	 *
	 * @param charset 用于编码的字符集
	 * @since 5.0.8
	 */
	public UriComponentsBuilder encode(Charset charset) {
		this.encodeTemplate = true;
		this.charset = charset;
		return this;
	}


	// 构建方法

	/**
	 * 从此构建器中包含的各个组件构建 {@code UriComponents} 实例。
	 *
	 * @return URI 组件
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * {@link #build()} 的变体，用于在组件已经完全编码时创建 {@link UriComponents} 实例。
	 * 例如，如果构建器是通过 {@link UriComponentsBuilder#fromUri(URI)} 创建的，则此方法很有用。
	 *
	 * @param encoded 此构建器中的组件是否已经编码
	 * @return URI 组件
	 * @throws IllegalArgumentException 如果任何组件包含应该已编码的非法字符。
	 */
	public UriComponents build(boolean encoded) {
		// 根据 encoded 的值选择编码方式，然后调用 buildInternal 方法进行构建
		return buildInternal(encoded ? EncodingHint.FULLY_ENCODED :
				(this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE));
	}

	private UriComponents buildInternal(EncodingHint hint) {
		UriComponents result;
		if (this.ssp != null) {
			// 如果 URI的方案特定部分 不为空，创建 非明确层次URI组件对象
			result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		} else {
			// 否则，创建 层次URI组件对象
			HierarchicalUriComponents uric = new HierarchicalUriComponents(this.scheme, this.fragment,
					this.userInfo, this.host, this.port, this.pathBuilder.build(), this.queryParams,
					hint == EncodingHint.FULLY_ENCODED);
			// 根据编码提示对 URI 进行编码
			result = (hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric);
		}
		if (!this.uriVariables.isEmpty()) {
			// 如果存在 URI 模板变量，则对 URI 进行扩展
			result = result.expand(name -> this.uriVariables.getOrDefault(name, UriTemplateVariables.SKIP_VALUE));
		}
		// 返回构建好的 URI组件 对象
		return result;
	}

	/**
	 * 构建 {@code UriComponents} 实例，并使用映射中的值替换 URI 模板变量。
	 * 这是一个快捷方法，它组合了调用 {@link #build()} 和 {@link UriComponents#expand(Map)}。
	 *
	 * @param uriVariables URI 变量的映射
	 * @return 具有扩展值的 URI 组件
	 */
	public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
		return build().expand(uriVariables);
	}

	/**
	 * 构建 {@code UriComponents} 实例，并使用数组中的值替换 URI 模板变量。
	 * 这是一个快捷方法，它组合了调用 {@link #build()} 和 {@link UriComponents#expand(Object...)}。
	 *
	 * @param uriVariableValues URI 变量的值
	 * @return 具有扩展值的 URI 组件
	 */
	public UriComponents buildAndExpand(Object... uriVariableValues) {
		return build().expand(uriVariableValues);
	}

	@Override
	public URI build(Object... uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	@Override
	public URI build(Map<String, ?> uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	/**
	 * 构建 URI 字符串。
	 * <p>实际上，这是一个构建、编码和返回字符串表示形式的快捷方式：
	 * <pre class="code">
	 * String uri = builder.build().encode().toUriString()
	 * </pre>
	 * <p>然而，如果 {@link #uriVariables(Map) URI 变量} 已经提供，那么 URI 模板将单独进行预编码，而不是与 URI 变量一起编码（有关详细信息，请参阅 {@link #encode()}），即等同于：
	 * <pre>
	 * String uri = builder.encode().build().toUriString()
	 * </pre>
	 *
	 * @see UriComponents#toUriString()
	 * @since 4.1
	 */
	public String toUriString() {
		// 如果 URI 模板变量为空，则构建、编码并返回 URI 字符串
		return (this.uriVariables.isEmpty() ? build().encode().toUriString() :
				// 否则，根据编码提示进行构建，并返回编码后的 URI 字符串
				buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString());
	}


	// 实例方法

	/**
	 * 从给定 URI 的组件初始化此构建器的组件。
	 *
	 * @param uri URI
	 * @return 此 UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		// 设置 URI 的方案
		this.scheme = uri.getScheme();
		// 如果 URI 是 非明确层次URI
		if (uri.isOpaque()) {
			// 设置方案特定部分
			this.ssp = uri.getRawSchemeSpecificPart();
			// 重置层次结构组件
			resetHierarchicalComponents();
		} else {
			// 如果 URI 是明确层次URI
			if (uri.getRawUserInfo() != null) {
				// 设置用户信息
				this.userInfo = uri.getRawUserInfo();
			}
			if (uri.getHost() != null) {
				// 设置主机
				this.host = uri.getHost();
			}
			if (uri.getPort() != -1) {
				// 设置端口
				this.port = String.valueOf(uri.getPort());
			}
			if (StringUtils.hasLength(uri.getRawPath())) {
				// 设置路径
				this.pathBuilder = new CompositePathComponentBuilder();
				this.pathBuilder.addPath(uri.getRawPath());
			}
			if (StringUtils.hasLength(uri.getRawQuery())) {
				// 设置查询参数
				this.queryParams.clear();
				query(uri.getRawQuery());
			}
			// 重置方案特定部分
			resetSchemeSpecificPart();
		}
		if (uri.getRawFragment() != null) {
			// 设置片段
			this.fragment = uri.getRawFragment();
		}
		// 返回当前 URI组件构建器 对象
		return this;
	}

	/**
	 * 从给定 {@link UriComponents} 实例的值设置或追加此构建器的各个 URI 组件。
	 * <p>有关每个组件的语义（即设置与追加）请检查此类上的构建器方法。例如，{@link #host(String)} 设置而 {@link #path(String)} 追加。
	 *
	 * @param uriComponents 要从中复制的 UriComponents
	 * @return 此 UriComponentsBuilder
	 */
	public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
		Assert.notNull(uriComponents, "UriComponents must not be null");
		// 复制URI组件构建器
		uriComponents.copyToUriComponentsBuilder(this);
		return this;
	}

	@Override
	public UriComponentsBuilder scheme(@Nullable String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * 设置 URI 方案特定部分。调用此方法时，它会覆盖 {@linkplain #userInfo(String) user-info}、
	 * {@linkplain #host(String) host}、{@linkplain #port(int) port}、
	 * {@linkplain #path(String) path} 和 {@link #query(String) query}。
	 *
	 * @param ssp URI 方案特定部分，可能包含 URI 模板参数
	 * @return 此 UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		// 重置层次组件
		resetHierarchicalComponents();
		return this;
	}

	@Override
	public UriComponentsBuilder userInfo(@Nullable String userInfo) {
		this.userInfo = userInfo;
		// 重置URI的方案特定部分
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder host(@Nullable String host) {
		this.host = host;
		if (host != null) {
			// 如果主机不为空，重置 URI 方案特定部分
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder port(int port) {
		Assert.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		if (port > -1) {
			// 如果端口不为 -1，重置 URI 方案特定部分
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder port(@Nullable String port) {
		this.port = port;
		if (port != null) {
			// 如果端口不为空，重置 URI 方案特定部分
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder path(String path) {
		// 添加路径至路径构建器
		this.pathBuilder.addPath(path);
		// 重置 URI 方案特定部分
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
		// 添加路径段至路径构建器
		this.pathBuilder.addPathSegments(pathSegments);
		// 重置 URI 方案特定部分
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder replacePath(@Nullable String path) {
		// 构建复合路径组件构建器
		this.pathBuilder = new CompositePathComponentBuilder();
		if (path != null) {
			// 添加路径
			this.pathBuilder.addPath(path);
		}
		// 重置 URI 方案特定部分
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder query(@Nullable String query) {
		// 如果查询参数不为空
		if (query != null) {
			// 使用正则表达式匹配查询参数
			Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
			// 循环处理匹配结果
			while (matcher.find()) {
				// 提取查询参数的名称、等号和值
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				// 添加查询参数
				queryParam(name, (value != null ? value : (StringUtils.hasLength(eq) ? "" : null)));
			}
			// 重置方案特定部分
			resetSchemeSpecificPart();
		} else {
			// 如果查询参数为空，则清空查询参数列表
			this.queryParams.clear();
		}
		// 返回当前 URI组件构建器 对象
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQuery(@Nullable String query) {
		// 清空查询参数列表
		this.queryParams.clear();
		if (query != null) {
			// 如果查询参数不为空，则重新添加查询参数
			query(query);
			// 重置方案特定部分
			resetSchemeSpecificPart();
		}
		// 返回当前 URI组件构建器 对象
		return this;
	}

	@Override
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		// 如果值不为空，则将每个值添加到查询参数列表中
		if (!ObjectUtils.isEmpty(values)) {
			// 遍历每个值
			for (Object value : values) {
				// 获取值的字符串表示形式并添加到查询参数列表中
				String valueAsString = getQueryParamValue(value);
				this.queryParams.add(name, valueAsString);
			}
		} else {
			// 如果值为空，则将空值添加到查询参数列表中
			this.queryParams.add(name, null);
		}
		// 重置方案特定部分
		resetSchemeSpecificPart();
		// 返回当前 URI组件构建器 对象
		return this;
	}

	@Nullable
	private String getQueryParamValue(@Nullable Object value) {
		// 如果值不为空
		if (value != null) {
			// 如果值是 Optional 类型，则使用 map 转换为字符串，否则直接调用 toString 方法
			return (value instanceof Optional ?
					((Optional<?>) value).map(Object::toString).orElse(null) :
					value.toString());
		}
		// 如果值为空，则返回 null
		return null;
	}

	@Override
	public UriComponentsBuilder queryParam(String name, @Nullable Collection<?> values) {
		return queryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
	}

	@Override
	public UriComponentsBuilder queryParamIfPresent(String name, Optional<?> value) {
		// 如果值存在
		value.ifPresent(o -> {
			if (o instanceof Collection) {
				// 如果值是集合类型，则添加集合中的每个元素到查询参数列表中
				queryParam(name, (Collection<?>) o);
			} else {
				// 否则，直接添加值到查询参数列表中
				queryParam(name, o);
			}
		});
		// 返回当前 URI组件构建器 对象
		return this;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 4.0
	 */
	@Override
	public UriComponentsBuilder queryParams(@Nullable MultiValueMap<String, String> params) {
		// 如果参数不为空
		if (params != null) {
			// 将参数添加到查询参数列表中
			this.queryParams.addAll(params);
			// 重置方案特定部分
			resetSchemeSpecificPart();
		}
		// 返回当前 URI组件构建器 对象
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		// 移除指定名称的查询参数
		this.queryParams.remove(name);
		if (!ObjectUtils.isEmpty(values)) {
			// 如果值不为空，则添加新的值到查询参数列表中
			queryParam(name, values);
		}
		// 重置方案特定部分
		resetSchemeSpecificPart();
		// 返回当前 URI组件构建器 对象
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQueryParam(String name, @Nullable Collection<?> values) {
		return replaceQueryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 4.2
	 */
	@Override
	public UriComponentsBuilder replaceQueryParams(@Nullable MultiValueMap<String, String> params) {
		// 清空查询参数
		this.queryParams.clear();
		if (params != null) {
			// 将参数添加到查询参数列表中
			this.queryParams.putAll(params);
		}
		// 返回当前 URI组件构建器 对象
		return this;
	}

	@Override
	public UriComponentsBuilder fragment(@Nullable String fragment) {
		// 如果片段不为空
		if (fragment != null) {
			Assert.hasLength(fragment, "Fragment must not be empty");
			// 设置片段
			this.fragment = fragment;
		} else {
			// 如果片段为空，则设置片段为 null
			this.fragment = null;
		}
		// 返回当前 URI组件构建器 对象
		return this;
	}

	/**
	 * 在构建时配置要展开的 URI 变量。
	 * <p>提供的变量可以是所有必需变量的子集。在构建时，可用变量将被展开，而未解析的 URI 占位符将保留在原处，并且稍后仍然可以展开。
	 * <p>与 {@link UriComponents#expand(Map)} 或 {@link #buildAndExpand(Map)} 相比，
	 * 当您需要在尚未构建 {@link UriComponents} 实例时提供 URI 变量时，此方法很有用，或者可能预先展开一些共享的默认值，如主机和端口。
	 *
	 * @param uriVariables 要使用的 URI 变量
	 * @return 此 UriComponentsBuilder
	 * @since 5.0.8
	 */
	public UriComponentsBuilder uriVariables(Map<String, Object> uriVariables) {
		this.uriVariables.putAll(uriVariables);
		return this;
	}

	/**
	 * 从给定的 headers 中自适应此构建器的 scheme+host+port，
	 * 具体来说是从 "Forwarded"（<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>，
	 * 或者如果未找到 "Forwarded" 则从 "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto" 中获取。
	 * <p><strong>注意:</strong> 如果存在，此方法使用转发头中的值，以反映客户端发起的协议和地址。
	 * 考虑使用 {@code ForwardedHeaderFilter} 从一个中心位置选择是提取和使用还是丢弃这些头。
	 * 有关此过滤器的更多信息，请参阅 Spring Framework 参考文档。
	 *
	 * @param headers 要考虑的 HTTP headers
	 * @return 此 UriComponentsBuilder
	 * @since 4.2.7
	 */
	UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
		try {
			// 获取 Forwarded 头部的值
			String forwardedHeader = headers.getFirst("Forwarded");
			// 如果 Forwarded 头部的值不为空
			if (StringUtils.hasText(forwardedHeader)) {
				// 使用正则表达式匹配 Forwarded 头部中的协议
				Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedHeader);
				if (matcher.find()) {
					// 设置协议
					scheme(matcher.group(1).trim());
					// 清空端口
					port(null);
				} else if (isForwardedSslOn(headers)) {
					// 如果 SSL 被打开，则设置协议为 https
					scheme("https");
					// 清空端口
					port(null);
				}
				// 使用正则表达式匹配 Forwarded 头部中的主机
				matcher = FORWARDED_HOST_PATTERN.matcher(forwardedHeader);
				if (matcher.find()) {
					// 适应 Forwarded 头部中的主机
					adaptForwardedHost(matcher.group(1).trim());
				}
			} else {
				// 如果 Forwarded 头部的值为空
				// 获取 X-Forwarded-Proto 头部的值
				String protocolHeader = headers.getFirst("X-Forwarded-Proto");
				if (StringUtils.hasText(protocolHeader)) {
					// 设置协议
					scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
					// 清空端口
					port(null);
				} else if (isForwardedSslOn(headers)) {
					// 如果 SSL 被打开，则设置协议为 https
					scheme("https");
					// 清空端口
					port(null);
				}
				// 获取 X-Forwarded-Host 头部的值
				String hostHeader = headers.getFirst("X-Forwarded-Host");
				if (StringUtils.hasText(hostHeader)) {
					// 适应 X-Forwarded-Host 头部中的主机
					adaptForwardedHost(StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
				}
				// 获取 X-Forwarded-Port 头部的值
				String portHeader = headers.getFirst("X-Forwarded-Port");
				if (StringUtils.hasText(portHeader)) {
					// 设置端口
					port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
				}
			}
		} catch (NumberFormatException ex) {
			// 如果解析端口失败，则抛出异常
			throw new IllegalArgumentException("Failed to parse a port from \"forwarded\"-type headers. " +
					"If not behind a trusted proxy, consider using ForwardedHeaderFilter " +
					"with the removeOnly=true. Request headers: " + headers);
		}

		if (this.scheme != null &&
				(((this.scheme.equals("http") || this.scheme.equals("ws")) && "80".equals(this.port)) ||
						((this.scheme.equals("https") || this.scheme.equals("wss")) && "443".equals(this.port)))) {
			// 如果方案不为空，并且端口是默认端口，则清空端口
			port(null);
		}

		// 返回当前 URI组件构建器 对象
		return this;
	}

	private boolean isForwardedSslOn(HttpHeaders headers) {
		// 获取 X-Forwarded-Ssl 头部的值
		String forwardedSsl = headers.getFirst("X-Forwarded-Ssl");
		// 如果 X-Forwarded-Ssl 头部的值不为空且为 "on"，则返回 true；否则返回 false
		return StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on");
	}

	private void adaptForwardedHost(String rawValue) {
		// 获取冒号和方括号的索引
		int portSeparatorIdx = rawValue.lastIndexOf(':');
		int squareBracketIdx = rawValue.lastIndexOf(']');
		// 如果冒号的索引大于方括号的索引
		if (portSeparatorIdx > squareBracketIdx) {
			// 如果方括号的索引为 -1，或者冒号在方括号之后，并且冒号不是方括号的一部分
			if (squareBracketIdx == -1 && rawValue.indexOf(':') != portSeparatorIdx) {
				// 抛出异常，因为 IPv4 地址无效
				throw new IllegalArgumentException("Invalid IPv4 address: " + rawValue);
			}
			// 设置主机和端口
			host(rawValue.substring(0, portSeparatorIdx));
			port(Integer.parseInt(rawValue.substring(portSeparatorIdx + 1)));
		} else {
			// 设置主机并清空端口
			host(rawValue);
			port(null);
		}
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = null;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}


	/**
	 * Object 的 {@code clone()} 方法的公共声明。
	 * 委托给 {@link #cloneBuilder()}。
	 */
	@Override
	public Object clone() {
		return cloneBuilder();
	}

	/**
	 * 克隆此 {@code UriComponentsBuilder}。
	 *
	 * @return 克隆的 {@code UriComponentsBuilder} 对象
	 * @since 4.2.7
	 */
	public UriComponentsBuilder cloneBuilder() {
		return new UriComponentsBuilder(this);
	}


	private interface PathComponentBuilder {

		@Nullable
		PathComponent build();

		PathComponentBuilder cloneBuilder();
	}


	private static class CompositePathComponentBuilder implements PathComponentBuilder {
		/**
		 * 路径组件构建器队列
		 */
		private final Deque<PathComponentBuilder> builders = new ArrayDeque<>();

		public void addPathSegments(String... pathSegments) {
			// 如果路径段不为空
			if (!ObjectUtils.isEmpty(pathSegments)) {
				// 获取最后一个 PathSegmentComponentBuilder
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				// 获取最后一个 FullPathComponentBuilder
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				// 如果没有找到 PathSegmentComponentBuilder
				if (psBuilder == null) {
					// 创建一个新的 PathSegmentComponentBuilder 并添加到 builders 列表中
					psBuilder = new PathSegmentComponentBuilder();
					this.builders.add(psBuilder);
					if (fpBuilder != null) {
						// 如果存在 FullPathComponentBuilder，则移除尾部斜杠
						fpBuilder.removeTrailingSlash();
					}
				}
				// 向 PathSegmentComponentBuilder 添加路径段
				psBuilder.append(pathSegments);
			}
		}

		public void addPath(String path) {
			// 如果路径文本不为空
			if (StringUtils.hasText(path)) {
				// 获取最后一个 PathSegmentComponentBuilder
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				// 获取最后一个 FullPathComponentBuilder
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				// 如果存在 PathSegmentComponentBuilder
				if (psBuilder != null) {
					// 如果路径不是以斜杠开头，则添加斜杠
					path = (path.startsWith("/") ? path : "/" + path);
				}
				// 如果不存在 FullPathComponentBuilder
				if (fpBuilder == null) {
					// 创建一个新的 FullPathComponentBuilder 并添加到 builders 列表中
					fpBuilder = new FullPathComponentBuilder();
					this.builders.add(fpBuilder);
				}
				// 向 FullPathComponentBuilder 添加路径文本
				fpBuilder.append(path);
			}
		}

		@SuppressWarnings("unchecked")
		@Nullable
		private <T> T getLastBuilder(Class<T> builderClass) {
			// 如果 构建器队列 不为空
			if (!this.builders.isEmpty()) {
				// 获取 构建器队列 中的最后一个路径组件构建器
				PathComponentBuilder last = this.builders.getLast();
				// 如果最后一个路径组件构建器是 builderClass 的实例
				if (builderClass.isInstance(last)) {
					// 返回最后一个路径组件构建器
					return (T) last;
				}
			}
			// 否则返回 null
			return null;
		}

		@Override
		public PathComponent build() {
			// 获取 路径组件构建器队列 的大小
			int size = this.builders.size();
			// 创建一个空的路径组件列表，大小为 构建器队列 的大小
			List<PathComponent> components = new ArrayList<>(size);
			// 遍历 构建器队列 中的每个路径组件构建器
			for (PathComponentBuilder componentBuilder : this.builders) {
				// 构建路径组件
				PathComponent pathComponent = componentBuilder.build();
				// 如果路径组件不为空，则添加到路径组件列表中
				if (pathComponent != null) {
					components.add(pathComponent);
				}
			}
			// 如果路径组件列表为空，则返回空路径组件
			if (components.isEmpty()) {
				return HierarchicalUriComponents.NULL_PATH_COMPONENT;
			}
			// 如果路径组件列表只有一个元素，则返回该元素
			if (components.size() == 1) {
				return components.get(0);
			}
			// 否则，返回路径组件列表的复合路径组件
			return new HierarchicalUriComponents.PathComponentComposite(components);
		}

		@Override
		public CompositePathComponentBuilder cloneBuilder() {
			// 创建一个复合路径组件构建器
			CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
			// 遍历 路径组件构建器队列 中的每个路径组件构建器
			for (PathComponentBuilder builder : this.builders) {
				// 克隆每个路径组件构建器并添加到复合路径组件构建器的列表中
				compositeBuilder.builders.add(builder.cloneBuilder());
			}
			// 返回复合路径组件构建器
			return compositeBuilder;
		}
	}


	private static class FullPathComponentBuilder implements PathComponentBuilder {
		/**
		 * 路径字符串构建器
		 */
		private final StringBuilder path = new StringBuilder();

		public void append(String path) {
			this.path.append(path);
		}

		@Override
		public PathComponent build() {
			// 如果路径长度为0，则返回null
			if (this.path.length() == 0) {
				return null;
			}
			// 对路径进行清理
			String sanitized = getSanitizedPath(this.path);
			// 返回新的完整路径组件
			return new HierarchicalUriComponents.FullPathComponent(sanitized);
		}

		private static String getSanitizedPath(final StringBuilder path) {
			// 查找路径中连续出现的双斜杠的索引
			int index = path.indexOf("//");
			// 如果找到了双斜杠
			if (index >= 0) {
				// 创建一个 StringBuilder 对象，用于清理路径
				StringBuilder sanitized = new StringBuilder(path);
				// 循环删除连续出现的双斜杠
				while (index != -1) {
					sanitized.deleteCharAt(index);
					index = sanitized.indexOf("//", index);
				}
				// 返回清理后的路径字符串
				return sanitized.toString();
			}
			// 返回原始路径字符串
			return path.toString();
		}

		public void removeTrailingSlash() {
			// 获取路径字符串的最后一个字符的索引
			int index = this.path.length() - 1;
			// 如果最后一个字符是斜杠
			if (this.path.charAt(index) == '/') {
				// 删除最后一个字符
				this.path.deleteCharAt(index);
			}
		}

		@Override
		public FullPathComponentBuilder cloneBuilder() {
			// 创建一个完整路径组件构建器
			FullPathComponentBuilder builder = new FullPathComponentBuilder();
			// 向构建器添加路径字符串
			builder.append(this.path.toString());
			// 返回构建器
			return builder;
		}
	}


	private static class PathSegmentComponentBuilder implements PathComponentBuilder {
		/**
		 * 路径片段列表
		 */
		private final List<String> pathSegments = new ArrayList<>();

		public void append(String... pathSegments) {
			// 遍历路径段数组
			for (String pathSegment : pathSegments) {
				// 如果路径段不为空且有文本内容
				if (StringUtils.hasText(pathSegment)) {
					// 将路径段添加到路径段列表中
					this.pathSegments.add(pathSegment);
				}
			}
		}

		@Override
		public PathComponent build() {
			// 如果路径段列表为空，则返回 null；
			return (this.pathSegments.isEmpty() ? null :
					// 否则返回新的路径段组件
					new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
		}

		@Override
		public PathSegmentComponentBuilder cloneBuilder() {
			// 创建一个路径段组件构建器
			PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
			// 将当前路径段列表中的所有元素添加到构建器的路径段列表中
			builder.pathSegments.addAll(this.pathSegments);
			// 返回构建器
			return builder;
		}
	}


	private enum EncodingHint {ENCODE_TEMPLATE, FULLY_ENCODED, NONE}

}
