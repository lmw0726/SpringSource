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

package org.springframework.web.cors;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 一个包含CORS配置的容器，以及用于检查给定请求的实际来源、HTTP方法和头信息的方法。
 *
 * <p>默认情况下，新创建的{@code CorsConfiguration}不允许任何跨域请求，必须显式配置以指示应允许什么。
 * 使用{@link #applyPermitDefaultValues()}将初始化模型翻转，以从开放的默认值开始，允许所有GET、HEAD和POST请求的跨域请求。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Ruslan Akhundov
 * @see <a href="https://www.w3.org/TR/cors/">CORS spec</a>
 * @since 4.2
 */
public class CorsConfiguration {

	/**
	 * 表示<em>所有</em>源、方法或头信息的通配符。
	 */
	public static final String ALL = "*";

	/**
	 * 代表允许所有来源的列表。
	 */
	private static final List<String> ALL_LIST = Collections.singletonList(ALL);

	/**
	 * 代表允许所有来源的 {@link OriginPattern}。使用 "*" 表示所有来源。
	 */
	private static final OriginPattern ALL_PATTERN = new OriginPattern("*");

	/**
	 * 允许所有来源。
	 */
	private static final List<OriginPattern> ALL_PATTERN_LIST = Collections.singletonList(ALL_PATTERN);

	/**
	 * 默认情况下允许所有的列表。
	 */
	private static final List<String> DEFAULT_PERMIT_ALL = Collections.singletonList(ALL);

	/**
	 * 默认允许的 HTTP 方法列表。包含  GET、HEAD。
	 */
	private static final List<HttpMethod> DEFAULT_METHODS = Collections.unmodifiableList(
			Arrays.asList(HttpMethod.GET, HttpMethod.HEAD));

	/**
	 * 默认情况下允许的 HTTP 方法列表。包含 GET、HEAD、POST。
	 */
	private static final List<String> DEFAULT_PERMIT_METHODS = Collections.unmodifiableList(
			Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name()));

	/**
	 * 允许的来源列表。
	 */
	@Nullable
	private List<String> allowedOrigins;

	/**
	 * 允许的来源模式列表。
	 */
	@Nullable
	private List<OriginPattern> allowedOriginPatterns;

	/**
	 * 允许的 HTTP 方法列表。
	 */
	@Nullable
	private List<String> allowedMethods;

	/**
	 * 解析后的允许的 HTTP 方法列表。
	 */
	@Nullable
	private List<HttpMethod> resolvedMethods = DEFAULT_METHODS;

	/**
	 * 允许的请求头列表。
	 */
	@Nullable
	private List<String> allowedHeaders;

	/**
	 * 允许的暴露头列表。
	 */
	@Nullable
	private List<String> exposedHeaders;

	/**
	 * 是否允许携带凭据。
	 */
	@Nullable
	private Boolean allowCredentials;

	/**
	 * 最大的缓存时间（秒）。
	 */
	@Nullable
	private Long maxAge;


	/**
	 * 使用默认设置构造一个新的 {@code CorsConfiguration} 实例，其中默认情况下不允许任何来源的跨域请求。
	 *
	 * @see #applyPermitDefaultValues()
	 */
	public CorsConfiguration() {
	}

	/**
	 * 通过从提供的 {@code CorsConfiguration} 复制所有值来构造一个新的 {@code CorsConfiguration} 实例。
	 */
	public CorsConfiguration(CorsConfiguration other) {
		this.allowedOrigins = other.allowedOrigins;
		this.allowedOriginPatterns = other.allowedOriginPatterns;
		this.allowedMethods = other.allowedMethods;
		this.resolvedMethods = other.resolvedMethods;
		this.allowedHeaders = other.allowedHeaders;
		this.exposedHeaders = other.exposedHeaders;
		this.allowCredentials = other.allowCredentials;
		this.maxAge = other.maxAge;
	}


	/**
	 * 设置允许的跨域请求的来源列表。值可以是特定的域名，例如 {@code "https://domain1.com"}，
	 * 也可以是 CORS 定义的特殊值 {@code "*"} 代表所有来源。
	 * <p>对于匹配的预检请求和实际请求，{@code Access-Control-Allow-Origin} 响应头将被设置为匹配的域值或 {@code "*"}。
	 * 但请注意，CORS 规范不允许当 {@link #setAllowCredentials allowCredentials} 设置为 {@code true} 时使用 {@code "*"}，
	 * 自 5.3 起该组合被拒绝，建议使用 {@link #setAllowedOriginPatterns allowedOriginPatterns} 替代。
	 * <p>默认情况下未设置，这意味着不允许任何来源。但是，此类的一个实例通常会进一步初始化，例如通过 {@link #applyPermitDefaultValues()} 进行 {@code @CrossOrigin} 初始化。
	 */
	public void setAllowedOrigins(@Nullable List<String> origins) {
		this.allowedOrigins = (origins == null ? null :
				origins.stream().filter(Objects::nonNull).map(this::trimTrailingSlash).collect(Collectors.toList()));
	}

	private String trimTrailingSlash(String origin) {
		return (origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin);
	}

	/**
	 * 返回已配置的允许的来源，如果没有则返回 {@code null}。
	 */
	@Nullable
	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
	 * {@link #setAllowedOrigins} 的变体，用于一次添加一个来源。
	 */
	public void addAllowedOrigin(@Nullable String origin) {
		// 如果来源为空，则返回
		if (origin == null) {
			return;
		}
		// 如果允许的来源列表为null，则初始化为包含4个元素的ArrayList
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<>(4);
		}
		// 如果允许的来源列表为默认允许所有，并且允许的来源模式为空
		else if (this.allowedOrigins == DEFAULT_PERMIT_ALL && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
			// 设置允许的来源列表为默认允许所有
			setAllowedOrigins(DEFAULT_PERMIT_ALL);
		}
		// 去除来源末尾的斜杠，并添加到允许的来源列表中
		origin = trimTrailingSlash(origin);
		this.allowedOrigins.add(origin);
	}

	/**
	 * {@link #setAllowedOrigins}的替代方法，支持更灵活的源模式，其中"*"可以出现在主机名的任何位置，
	 * 以及端口列表。示例：
	 * <ul>
	 * <li>{@literal https://*.domain1.com} -- 域名以 domain1.com 结尾
	 * <li>{@literal https://*.domain1.com:[8080,8081]} -- 域名以 domain1.com 结尾，并且端口为 8080 或 8081
	 * <li>{@literal https://*.domain1.com:[*]} -- 域名以 domain1.com 结尾，并且端口可以是任何端口，包括默认端口
	 * </ul>
	 * <p>与 {@link #setAllowedOrigins(List) allowedOrigins} 相比，该方法支持更多的源模式，
	 * 并且可以与 {@code allowCredentials} 一起使用。当匹配到允许的源模式时，
	 * {@code Access-Control-Allow-Origin} 响应头将被设置为匹配的源，而不是 {@code "*"} 或模式本身。
	 * 因此，allowedOriginPatterns 可以与 {@link #setAllowCredentials} 设置为 {@code true} 结合使用。
	 * <p>默认情况下未设置。
	 *
	 * @param allowedOriginPatterns 要允许的源模式列表，如果未设置则为 {@code null}
	 * @return this
	 * @since 5.3
	 */
	public CorsConfiguration setAllowedOriginPatterns(@Nullable List<String> allowedOriginPatterns) {
		// 如果 要允许的源模式列表 为null
		if (allowedOriginPatterns == null) {
			// 将当前实例的 要允许的源模式列表 设置为null
			this.allowedOriginPatterns = null;
		} else {
			// 如果 要允许的源模式列表 不为null
			// 创建一个新的ArrayList，大小与 要允许的源模式列表 相同
			this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns.size());
			// 遍历 要允许的源模式列表 中的每个元素
			for (String patternValue : allowedOriginPatterns) {
				// 调用addAllowedOriginPattern方法，将每个元素添加到 要允许的源模式列表 中
				addAllowedOriginPattern(patternValue);
			}
		}
		// 返回当前实例
		return this;
	}

	/**
	 * 返回配置的要允许的源模式列表，如果未设置则返回 {@code null}。
	 *
	 * @return 要允许的源模式列表，如果未设置则为 {@code null}
	 * @since 5.3
	 */
	@Nullable
	public List<String> getAllowedOriginPatterns() {
		// 如果允许的来源模式列表为null，则返回null
		if (this.allowedOriginPatterns == null) {
			return null;
		}
		// 将允许的来源模式列表转换为模式字符串列表并返回
		return this.allowedOriginPatterns.stream()
				.map(OriginPattern::getDeclaredPattern)
				.collect(Collectors.toList());
	}

	/**
	 * {@link #setAllowedOriginPatterns} 的变体，用于一次添加一个源模式。
	 *
	 * @param originPattern 要添加的源模式
	 * @since 5.3
	 */
	public void addAllowedOriginPattern(@Nullable String originPattern) {
		// 如果来源模式为null，则直接返回，不执行后续操作
		if (originPattern == null) {
			return;
		}

		// 如果 要允许的源模式列表 为null，则初始化为一个初始容量为4的ArrayList
		if (this.allowedOriginPatterns == null) {
			this.allowedOriginPatterns = new ArrayList<>(4);
		}

		// 去除 来源模式 末尾的斜杠
		originPattern = trimTrailingSlash(originPattern);

		// 创建 来源模式对象 并添加到 要允许的源模式列表 中
		this.allowedOriginPatterns.add(new OriginPattern(originPattern));

		// 如果 允许的来源列表 为默认的所有权限，则将其设置为null
		if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
			this.allowedOrigins = null;
		}
	}

	/**
	 * 设置要允许的HTTP方法，例如 {@code "GET"}、{@code "POST"}、{@code "PUT"} 等。
	 * <p>特殊值 {@code "*"} 允许所有方法。
	 * <p>如果未设置，只允许 {@code "GET"} 和 {@code "HEAD"}。
	 * <p>默认情况下未设置。
	 * <p><strong>注意：</strong> CORS 检查使用 "Forwarded"（
	 * <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>）、
	 * "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto" 头，
	 * 如果存在的话，以反映客户端发起的地址。考虑使用 {@code ForwardedHeaderFilter}
	 * 从一个中心位置选择是提取和使用还是丢弃这些头。有关此过滤器的更多信息，请参阅 Spring Framework 参考文档。
	 *
	 * @param allowedMethods 要允许的HTTP方法列表，如果未设置则为 {@code null}
	 */
	public void setAllowedMethods(@Nullable List<String> allowedMethods) {
		// 如果允许的方法列表不为null，则创建一个新的ArrayList并复制该列表
		this.allowedMethods = (allowedMethods != null ? new ArrayList<>(allowedMethods) : null);
		// 如果允许的方法列表不为空
		if (!CollectionUtils.isEmpty(allowedMethods)) {
			// 创建一个新的ArrayList以保存解析后的方法
			this.resolvedMethods = new ArrayList<>(allowedMethods.size());
			// 遍历每一个允许方法
			for (String method : allowedMethods) {
				// 如果允许的方法包含 "*" 则表示允许所有方法
				if (ALL.equals(method)) {
					// 将解析后的方法列表设为null，并跳出循环
					this.resolvedMethods = null;
					break;
				}
				// 解析方法并添加到解析后的方法列表中
				this.resolvedMethods.add(HttpMethod.resolve(method));
			}
		} else {
			// 如果允许的方法列表为空，则使用默认的允许方法列表
			this.resolvedMethods = DEFAULT_METHODS;
		}
	}

	/**
	 * 返回允许的 HTTP 方法列表，如果未设置则返回 {@code null}，此时只允许 {@code "GET"} 和 {@code "HEAD"}。
	 *
	 * @see #addAllowedMethod(HttpMethod)
	 * @see #addAllowedMethod(String)
	 * @see #setAllowedMethods(List)
	 */
	@Nullable
	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
	 * 添加一个允许的 HTTP 方法。
	 */
	public void addAllowedMethod(HttpMethod method) {
		addAllowedMethod(method.name());
	}

	/**
	 * 添加一个允许的 HTTP 方法。
	 */
	public void addAllowedMethod(String method) {
		if (StringUtils.hasText(method)) {
			// 如果方法不为空
			if (this.allowedMethods == null) {
				// 如果允许的方法列表为空
				// 创建一个初始容量为4的允许方法列表
				this.allowedMethods = new ArrayList<>(4);
				// 创建一个初始容量为4的解析后的方法列表
				this.resolvedMethods = new ArrayList<>(4);
			} else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
				// 如果允许的方法列表等于默认的允许方法列表
				// 设置允许的方法列表为默认的允许方法列表
				setAllowedMethods(DEFAULT_PERMIT_METHODS);
			}
			// 将当前方法添加到允许的方法列表中
			this.allowedMethods.add(method);
			if (ALL.equals(method)) {
				// 如果当前方法是"ALL"
				// 将解析后的方法列表置为空
				this.resolvedMethods = null;
			} else if (this.resolvedMethods != null) {
				// 如果解析后的方法列表不为空
				// 将解析后的HTTP方法添加到解析后的方法列表中
				this.resolvedMethods.add(HttpMethod.resolve(method));
			}
		}
	}

	/**
	 * 设置预检请求中允许列出为实际请求使用的头列表。
	 * <p>特殊值 {@code "*"} 允许实际请求发送任何头。
	 * <p>如果一个头是以下头之一，则不需要列出头名：
	 * {@code Cache-Control}、{@code Content-Language}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma}。
	 * <p>默认情况下未设置此项。
	 */
	public void setAllowedHeaders(@Nullable List<String> allowedHeaders) {
		this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
	}

	/**
	 * 返回允许的实际请求头列表，如果没有则返回 {@code null}。
	 *
	 * @see #addAllowedHeader(String)
	 * @see #setAllowedHeaders(List)
	 */
	@Nullable
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
	 * 添加一个允许的实际请求头。
	 */
	public void addAllowedHeader(String allowedHeader) {
		// 如果允许的请求头列表为null，则创建一个新的ArrayList
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<>(4);
		} else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
			// 如果允许的请求头列表为默认的允许所有请求头列表，则设置允许的请求头列表为默认的允许所有请求头列表
			setAllowedHeaders(DEFAULT_PERMIT_ALL);
		}
		// 将允许的请求头添加到允许的请求头列表中
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * 设置除简单头之外的响应头列表（即{@code Cache-Control}、{@code Content-Language}、
	 * {@code Content-Type}、{@code Expires}、{@code Last-Modified}或{@code Pragma}之外的头），
	 * 它们可能存在于实际响应中并且可以被公开。
	 * <p>特殊值{@code "*"}允许对非凭证请求公开所有头。
	 * <p>默认情况下未设置。
	 *
	 * @param exposedHeaders 要公开的响应头列表，如果未设置则为{@code null}
	 */
	public void setExposedHeaders(@Nullable List<String> exposedHeaders) {
		this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
	}

	/**
	 * 返回配置的要公开的响应头列表，如果未设置则返回{@code null}。
	 *
	 * @return 要公开的响应头列表，如果未设置则为{@code null}
	 * @see #addExposedHeader(String)
	 * @see #setExposedHeaders(List)
	 */
	@Nullable
	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
	 * 添加要公开的响应头。
	 * <p>特殊值{@code "*"}允许对非凭证请求公开所有头。
	 *
	 * @param exposedHeader 要公开的响应头
	 */
	public void addExposedHeader(String exposedHeader) {
		// 如果暴露的头部列表为空
		if (this.exposedHeaders == null) {
			// 创建一个初始容量为4的新列表
			this.exposedHeaders = new ArrayList<>(4);
		}
		// 将暴露的头部添加到列表中
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * 是否支持用户凭证。
	 * <p>默认情况下，此项未设置（即不支持用户凭证）。
	 *
	 * @param allowCredentials 是否允许用户凭证
	 */
	public void setAllowCredentials(@Nullable Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * 返回配置的{@code allowCredentials}标志，如果未设置则返回{@code null}。
	 *
	 * @return {@code allowCredentials}标志，如果未设置则返回{@code null}
	 * @see #setAllowCredentials(Boolean)
	 */
	@Nullable
	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * 配置预检请求响应可以被客户端缓存的时间段。
	 *
	 * @param maxAge 响应缓存的时间段
	 * @see #setMaxAge(Long)
	 * @since 5.2
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge.getSeconds();
	}

	/**
	 * 配置预检请求响应可以被客户端缓存的时间长度，单位为秒。
	 * <p>默认情况下未设置。
	 *
	 * @param maxAge 缓存时间长度，以秒为单位，为null表示未设置
	 */
	public void setMaxAge(@Nullable Long maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * 返回配置的{@code maxAge}值，如果未设置则返回{@code null}。
	 *
	 * @return maxAge值，如果未设置则返回{@code null}
	 * @see #setMaxAge(Long)
	 */
	@Nullable
	public Long getMaxAge() {
		return this.maxAge;
	}


	/**
	 * 默认情况下，{@code CorsConfiguration}不允许任何跨域请求，并且必须显式配置。
	 * 使用此方法切换到允许所有跨域请求的默认值（GET、HEAD和POST），但不覆盖已设置的任何值。
	 * <p>对于未设置的值，应用以下默认值：
	 * <ul>
	 * <li>允许所有源，使用CORS规范中定义的特殊值{@code "*"}。仅当{@link #setAllowedOrigins origins}和
	 * {@link #setAllowedOriginPatterns originPatterns}都未设置时才设置。</li>
	 * <li>允许“简单”方法{@code GET}、{@code HEAD}和{@code POST}。</li>
	 * <li>允许所有头信息。</li>
	 * <li>将最大缓存时间设置为1800秒（30分钟）。</li>
	 * </ul>
	 */
	public CorsConfiguration applyPermitDefaultValues() {
		// 如果允许的源为空且允许的源模式为空
		if (this.allowedOrigins == null && this.allowedOriginPatterns == null) {
			// 设置允许的源为默认允许全部
			this.allowedOrigins = DEFAULT_PERMIT_ALL;
		}
		// 如果允许的方法为空
		if (this.allowedMethods == null) {
			// 设置允许的方法为默认允许的方法
			this.allowedMethods = DEFAULT_PERMIT_METHODS;
			// 使用默认允许的方法解析方法并收集为列表
			this.resolvedMethods = DEFAULT_PERMIT_METHODS
					.stream().map(HttpMethod::resolve).collect(Collectors.toList());
		}
		// 如果允许的头部为空
		if (this.allowedHeaders == null) {
			// 设置允许的头部为默认允许全部
			this.allowedHeaders = DEFAULT_PERMIT_ALL;
		}
		// 如果最大年龄为空
		if (this.maxAge == null) {
			// 设置最大年龄为1800秒
			this.maxAge = 1800L;
		}
		// 返回当前对象
		return this;
	}

	/**
	 * 验证当{@link #setAllowCredentials allowCredentials}为true时，
	 * {@link #setAllowedOrigins allowedOrigins}不包含特殊值{@code "*"}，
	 * 因为在这种情况下，“Access-Control-Allow-Origin”不能设置为{@code "*"}。
	 *
	 * @throws IllegalArgumentException 如果验证失败
	 * @since 5.3
	 */
	public void validateAllowCredentials() {
		// 如果允许凭据，并且允许的来源列表不为null，并且包含所有的特殊值"*"
		if (this.allowCredentials == Boolean.TRUE &&
				this.allowedOrigins != null && this.allowedOrigins.contains(ALL)) {
			// 抛出异常
			throw new IllegalArgumentException(
					"When allowCredentials is true, allowedOrigins cannot contain the special value \"*\" " +
							"since that cannot be set on the \"Access-Control-Allow-Origin\" response header. " +
							"To allow credentials to a set of origins, list them explicitly " +
							"or consider using \"allowedOriginPatterns\" instead.");
		}

	}

	/**
	 * 将提供的{@code CorsConfiguration}的非null属性与此配置合并。
	 * <p>当合并单个值（例如{@code allowCredentials}或{@code maxAge}）时，
	 * 如果有的话，非null的{@code other}属性将覆盖{@code this}属性。
	 * <p>将列表（例如{@code allowedOrigins}、{@code allowedMethods}、
	 * {@code allowedHeaders}或{@code exposedHeaders}）合并的方式是加法。
	 * 例如，将{@code ["GET", "POST"]}与{@code ["PATCH"]}合并会得到{@code ["GET", "POST", "PATCH"]}。
	 * 但是，将{@code ["GET", "POST"]}与{@code ["*"]}合并会得到{@code ["*"]}。
	 * 还要注意，由{@link CorsConfiguration＃applyPermitDefaultValues（）}设置的默认许可值
	 * 会被任何显式定义的值覆盖。
	 *
	 * @param other 要合并的{@code CorsConfiguration}
	 * @return 合并后的{@code CorsConfiguration}，如果提供的配置为{@code null}，则返回{@code this}配置
	 */
	public CorsConfiguration combine(@Nullable CorsConfiguration other) {
		// 如果传入的对象为空，则直接返回当前对象
		if (other == null) {
			return this;
		}
		// 绕过setAllowedOrigins以避免重新编译模式
		CorsConfiguration config = new CorsConfiguration(this);
		// 合并允许的源
		List<String> origins = combine(getAllowedOrigins(), other.getAllowedOrigins());
		// 合并源模式
		List<OriginPattern> patterns = combinePatterns(this.allowedOriginPatterns, other.allowedOriginPatterns);
		// 如果允许的源为默认允许全部且模式列表不为空，则将允许的源设置为空，否则设置为合并后的列表
		config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);
		config.allowedOriginPatterns = patterns;
		// 设置允许的方法为合并后的列表
		config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
		// 设置允许的头部为合并后的列表
		config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
		// 设置暴露的头部为合并后的列表
		config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));
		// 设置是否允许凭证
		Boolean allowCredentials = other.getAllowCredentials();
		if (allowCredentials != null) {
			config.setAllowCredentials(allowCredentials);
		}
		// 设置最大年龄
		Long maxAge = other.getMaxAge();
		if (maxAge != null) {
			config.setMaxAge(maxAge);
		}
		// 返回新的配置对象
		return config;
	}

	private List<String> combine(@Nullable List<String> source, @Nullable List<String> other) {
		// 如果另一个集合为null，则返回source集合，如果source也为null，则返回空列表
		if (other == null) {
			return (source != null ? source : Collections.emptyList());
		}
		// 如果source集合为null，则返回另一个集合
		if (source == null) {
			return other;
		}
		// 如果source或other集合为默认允许所有或默认允许方法，则返回另一个集合
		if (source == DEFAULT_PERMIT_ALL || source == DEFAULT_PERMIT_METHODS) {
			return other;
		}
		// 如果另一个集合为默认允许所有或默认允许方法，则返回source集合
		if (other == DEFAULT_PERMIT_ALL || other == DEFAULT_PERMIT_METHODS) {
			return source;
		}
		// 如果source集合包含所有或other集合包含所有，则返回包含所有的列表
		if (source.contains(ALL) || other.contains(ALL)) {
			return ALL_LIST;
		}
		// 否则，创建一个包含两个集合所有元素的新集合
		Set<String> combined = new LinkedHashSet<>(source.size() + other.size());
		combined.addAll(source);
		combined.addAll(other);
		return new ArrayList<>(combined);
	}

	private List<OriginPattern> combinePatterns(
			@Nullable List<OriginPattern> source, @Nullable List<OriginPattern> other) {

		// 如果另一个集合为null，则返回source集合，如果source也为null，则返回空列表
		if (other == null) {
			return (source != null ? source : Collections.emptyList());
		}
		// 如果source集合为null，则返回另一个集合
		if (source == null) {
			return other;
		}
		// 如果source集合包含所有模式或other集合包含所有模式，则返回包含所有模式的列表
		if (source.contains(ALL_PATTERN) || other.contains(ALL_PATTERN)) {
			return ALL_PATTERN_LIST;
		}
		// 否则，创建一个包含两个集合所有元素的新集合
		Set<OriginPattern> combined = new LinkedHashSet<>(source.size() + other.size());
		combined.addAll(source);
		combined.addAll(other);
		return new ArrayList<>(combined);
	}


	/**
	 * 检查请求的来源是否在配置的允许来源列表中。
	 *
	 * @param origin 要检查的来源
	 * @return 用于响应的来源，或 {@code null} 表示请求的来源不被允许
	 */
	@Nullable
	public String checkOrigin(@Nullable String origin) {
		// 如果源为空，则返回null
		if (!StringUtils.hasText(origin)) {
			return null;
		}
		// 去除源的尾部斜杠
		String originToCheck = trimTrailingSlash(origin);
		// 如果允许的源不为空
		if (!ObjectUtils.isEmpty(this.allowedOrigins)) {
			// 如果允许的源包含ALL，则返回ALL
			if (this.allowedOrigins.contains(ALL)) {
				// 验证是否允许凭证
				validateAllowCredentials();
				return ALL;
			}
			// 遍历允许的源列表
			for (String allowedOrigin : this.allowedOrigins) {
				// 如果源与允许的源匹配，则返回源
				if (originToCheck.equalsIgnoreCase(allowedOrigin)) {
					return origin;
				}
			}
		}
		// 如果允许的源模式不为空
		if (!ObjectUtils.isEmpty(this.allowedOriginPatterns)) {
			// 遍历允许的源模式列表
			for (OriginPattern p : this.allowedOriginPatterns) {
				// 如果模式为ALL或者源匹配模式，则返回源
				if (p.getDeclaredPattern().equals(ALL) || p.getPattern().matcher(originToCheck).matches()) {
					return origin;
				}
			}
		}
		// 如果源不在允许的源列表或者允许的源模式列表中，则返回null
		return null;
	}

	/**
	 * 检查 HTTP 请求方法（或预检请求中的 {@code Access-Control-Request-Method} 头中的方法）是否在配置的允许方法列表中。
	 *
	 * @param requestMethod 要检查的 HTTP 请求方法
	 * @return 预检请求响应中要列出的 HTTP 方法列表，如果提供的 {@code requestMethod} 不允许，则为 {@code null}
	 */
	@Nullable
	public List<HttpMethod> checkHttpMethod(@Nullable HttpMethod requestMethod) {
		// 如果请求方法为null，则返回null
		if (requestMethod == null) {
			return null;
		}
		// 如果允许的方法列表为null，则返回一个只包含请求方法的列表
		if (this.resolvedMethods == null) {
			return Collections.singletonList(requestMethod);
		}
		// 如果允许的方法列表包含请求方法，则返回允许的方法列表，否则返回null
		return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
	}

	/**
	 * 检查提供的请求头（或预检请求中列在 {@code Access-Control-Request-Headers} 头中的头）是否在配置的允许头列表中。
	 *
	 * @param requestHeaders 要检查的请求头
	 * @return 预检请求响应中要列出的允许头列表，如果提供的请求头都不允许，则为 {@code null}
	 */
	@Nullable
	public List<String> checkHeaders(@Nullable List<String> requestHeaders) {
		// 如果请求头为空，则返回null
		if (requestHeaders == null) {
			return null;
		}
		// 如果请求头列表为空，则返回空列表
		if (requestHeaders.isEmpty()) {
			return Collections.emptyList();
		}
		// 如果允许的头部为空，则返回null
		if (ObjectUtils.isEmpty(this.allowedHeaders)) {
			return null;
		}

		// 检查是否允许任何头部
		boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
		List<String> result = new ArrayList<>(requestHeaders.size());
		// 遍历请求头列表
		for (String requestHeader : requestHeaders) {
			if (StringUtils.hasText(requestHeader)) {
				// 去除头部的空白字符
				requestHeader = requestHeader.trim();
				// 如果允许任何头部，则直接添加到结果列表中
				if (allowAnyHeader) {
					result.add(requestHeader);
				} else {
					// 否则遍历允许的头部列表，如果请求头与允许的头部匹配，则添加到结果列表中
					for (String allowedHeader : this.allowedHeaders) {
						if (requestHeader.equalsIgnoreCase(allowedHeader)) {
							result.add(requestHeader);
							break;
						}
					}
				}
			}
		}
		// 如果结果列表为空，则返回null，否则返回结果列表
		return (result.isEmpty() ? null : result);
	}


	/**
	 * 包含用户声明的模式（例如 "https://*.domain.com"）和从中派生的正则表达式 {@link Pattern}。
	 */
	private static class OriginPattern {
		/**
		 * 端口匹配模式
		 */
		private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

		/**
		 * 声明的模式
		 */
		private final String declaredPattern;

		/**
		 * 匹配的模式
		 */
		private final Pattern pattern;

		OriginPattern(String declaredPattern) {
			this.declaredPattern = declaredPattern;
			this.pattern = initPattern(declaredPattern);
		}

		private static Pattern initPattern(String patternValue) {
			// 初始化端口列表为null
			String portList = null;

			// 使用正则表达式模式匹配 模式字符串
			Matcher matcher = PORTS_PATTERN.matcher(patternValue);
			// 如果匹配成功
			if (matcher.matches()) {
				// 提取出正则表达式的主体部分，并赋值给 模式字符串
				patternValue = matcher.group(1);
				// 提取出端口列表，并赋值给portList
				portList = matcher.group(2);
			}

			// 对 模式字符串 进行转义处理，将其包裹在\Q和\E之间
			patternValue = "\\Q" + patternValue + "\\E";

			// 将 模式字符串 中的*替换为正则表达式中的匹配任意字符的表达式
			patternValue = patternValue.replace("*", "\\E.*\\Q");

			// 如果端口列表不为null
			if (portList != null) {
				// 如果端口列表为ALL，则匹配任意端口
				patternValue += (portList.equals(ALL) ? "(:\\d+)?" : ":(" + portList.replace(',', '|') + ")");
			}

			// 使用修改后的 模式字符串 创建并返回Pattern对象
			return Pattern.compile(patternValue);
		}

		public String getDeclaredPattern() {
			return this.declaredPattern;
		}

		public Pattern getPattern() {
			return this.pattern;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || !getClass().equals(other.getClass())) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(
					this.declaredPattern, ((OriginPattern) other).declaredPattern);
		}

		@Override
		public int hashCode() {
			return this.declaredPattern.hashCode();
		}

		@Override
		public String toString() {
			return this.declaredPattern;
		}
	}

}
