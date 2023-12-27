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

package org.springframework.web.reactive.result.view;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将重定向到绝对或上下文相关的 URL 的视图。URL 可能是 URI 模板，在这种情况下，URI 模板变量将使用来自模型的值或当前请求的 URI 变量替换。
 *
 * <p>默认情况下使用 {@link HttpStatus#SEE_OTHER}，但可以通过构造函数或设置器参数提供替代状态码。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RedirectView extends AbstractUrlBasedView {
	/**
	 * URI模板变量的正则表达式模式
	 */
	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	/**
	 * 重定向的状态码，默认为SEE_OTHER
	 */
	private HttpStatus statusCode = HttpStatus.SEE_OTHER;

	/**
	 * 是否使用相对于上下文的URL，默认为true
	 */
	private boolean contextRelative = true;

	/**
	 * 是否将当前URL的查询字符串追加到重定向URL
	 */
	private boolean propagateQuery = false;

	/**
	 * 可能的主机名
	 */
	@Nullable
	private String[] hosts;

	/**
	 * 作为 bean 使用的构造方法。
	 */
	public RedirectView() {
	}

	/**
	 * 使用给定的重定向 URL 创建新的 {@code RedirectView}。
	 * <p>默认情况下使用 {@link HttpStatus#SEE_OTHER}。
	 */
	public RedirectView(String redirectUrl) {
		super(redirectUrl);
	}

	/**
	 * 使用给定的 URL 和替代的重定向状态码创建新的 {@code RedirectView}，如 {@link HttpStatus#TEMPORARY_REDIRECT}
	 * 或 {@link HttpStatus#PERMANENT_REDIRECT}。
	 */
	public RedirectView(String redirectUrl, HttpStatus statusCode) {
		super(redirectUrl);
		setStatusCode(statusCode);
	}

	/**
	 * 设置替代的重定向状态码，如 {@link HttpStatus#TEMPORARY_REDIRECT} 或 {@link HttpStatus#PERMANENT_REDIRECT}。
	 */
	public void setStatusCode(HttpStatus statusCode) {
		Assert.isTrue(statusCode.is3xxRedirection(), "Not a redirect status code");
		this.statusCode = statusCode;
	}

	/**
	 * 获取要使用的重定向状态码。
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	/**
	 * 是否将以斜杠（"/"）开头的给定重定向 URL 解释为相对于当前上下文路径（{@code true}，默认值）还是相对于 Web 服务器根路径（{@code false}）。
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * 是否将 URL 解释为相对于当前上下文路径。
	 */
	public boolean isContextRelative() {
		return this.contextRelative;
	}

	/**
	 * 是否将当前 URL 的查询字符串追加到重定向 URL（{@code true}）还是不追加（{@code false}，默认值）。
	 */
	public void setPropagateQuery(boolean propagateQuery) {
		this.propagateQuery = propagateQuery;
	}

	/**
	 * 是否将当前 URL 的查询字符串追加到重定向 URL。
	 */
	public boolean isPropagateQuery() {
		return this.propagateQuery;
	}

	/**
	 * 配置与应用程序关联的一个或多个主机。
	 * <p>所有其他主机都将被视为外部主机。
	 * <p>实际上，这提供了一种方法，关闭具有主机并且该主机未列为已知主机的 URL 的编码。
	 * <p>如果未设置（默认值），则对所有重定向 URL 进行编码。
	 *
	 * @param hosts 一个或多个应用程序主机
	 */
	public void setHosts(@Nullable String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * 返回配置的应用程序主机。
	 */
	@Nullable
	public String[] getHosts() {
		return this.hosts;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}


	@Override
	public boolean isRedirectView() {
		return true;
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		return true;
	}

	/**
	 * 将模型转换为请求参数并重定向到给定的 URL。
	 */
	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		String targetUrl = createTargetUrl(model, exchange);
		return sendRedirect(targetUrl, exchange);
	}

	/**
	 * 创建目标 URL，如果必要，添加 contextPath，展开 URI 模板变量，附加当前请求查询，
	 * 并应用配置的 {@link #getRequestDataValueProcessor()
	 * RequestDataValueProcessor}。
	 */
	protected final String createTargetUrl(Map<String, Object> model, ServerWebExchange exchange) {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		// 获取当前请求
		ServerHttpRequest request = exchange.getRequest();

		// 构建目标URL字符串
		StringBuilder targetUrl = new StringBuilder();

		// 如果是相对于上下文的URL并且URL以斜杠开头，将上下文路径加入目标URL中
		if (isContextRelative() && url.startsWith("/")) {
			targetUrl.append(request.getPath().contextPath().value());
		}
		targetUrl.append(url);

		// 如果目标URL有文本内容，替换其中的模板变量为实际值
		if (StringUtils.hasText(targetUrl)) {
			Map<String, String> uriVars = getCurrentUriVariables(exchange);
			targetUrl = expandTargetUrlTemplate(targetUrl.toString(), model, uriVars);
		}

		// 如果设置了propagateQuery为true，则将当前请求的查询参数追加到目标URL中
		if (isPropagateQuery()) {
			targetUrl = appendCurrentRequestQuery(targetUrl.toString(), request);
		}

		String result = targetUrl.toString();

		// 获取请求数据值处理器并处理最终的目标URL
		RequestDataValueProcessor processor = getRequestDataValueProcessor();
		return (processor != null ? processor.processUrl(exchange, result) : result);
	}

	/**
	 * 获取当前请求的 URI 变量。
	 *
	 * @param exchange 当前交换信息
	 * @return 包含当前请求的 URI 变量的映射，如果不存在则返回空映射
	 */
	private Map<String, String> getCurrentUriVariables(ServerWebExchange exchange) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return exchange.getAttributeOrDefault(name, Collections.emptyMap());
	}

	/**
	 * 使用模型属性值或作为当前请求的 URI 变量值的 URI 模板变量来扩展目标 URL。对值进行编码。
	 */
	protected StringBuilder expandTargetUrlTemplate(String targetUrl,
													Map<String, Object> model, Map<String, String> uriVariables) {

		// 使用模板变量的正则表达式匹配目标URL
		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		boolean found = matcher.find();

		// 如果未找到模板变量，则直接返回目标URL
		if (!found) {
			return new StringBuilder(targetUrl);
		}

		// 如果有模板变量需要替换，遍历替换模板变量为实际值
		StringBuilder result = new StringBuilder();
		int endLastMatch = 0;
		while (found) {
			String name = matcher.group(1);
			Object value = (model.containsKey(name) ? model.get(name) : uriVariables.get(name));
			Assert.notNull(value, () -> "No value for URI variable '" + name + "'");
			result.append(targetUrl, endLastMatch, matcher.start());
			result.append(encodeUriVariable(value.toString()));
			endLastMatch = matcher.end();
			found = matcher.find();
		}

		// 拼接替换完成后的结果，并返回
		result.append(targetUrl, endLastMatch, targetUrl.length());
		return result;
	}

	/**
	 * 对 URI 变量进行编码，严格编码所有保留的 URI 字符。
	 *
	 * @param text 待编码的文本
	 * @return 编码后的 URI 变量
	 */
	private String encodeUriVariable(String text) {
		// 对所有保留的 URI 字符进行严格编码
		return UriUtils.encode(text, StandardCharsets.UTF_8);
	}

	/**
	 * 将当前请求的查询附加到目标重定向 URL。
	 *
	 * @param targetUrl 目标重定向的 URL
	 * @param request   当前请求
	 * @return 包含查询的目标 URL
	 */
	protected StringBuilder appendCurrentRequestQuery(String targetUrl, ServerHttpRequest request) {
		// 获取当前请求的查询字符串
		String query = request.getURI().getRawQuery();

		// 如果查询字符串为空，直接返回目标URL
		if (!StringUtils.hasText(query)) {
			return new StringBuilder(targetUrl);
		}

		// 获取目标URL中的片段部分
		int index = targetUrl.indexOf('#');
		String fragment = (index > -1 ? targetUrl.substring(index) : null);

		// 构建结果字符串，将查询字符串追加到目标URL中
		StringBuilder result = new StringBuilder();
		result.append(index != -1 ? targetUrl.substring(0, index) : targetUrl);
		result.append(targetUrl.indexOf('?') < 0 ? '?' : '&').append(query);

		// 将片段部分（如果存在）追加到结果字符串中
		if (fragment != null) {
			result.append(fragment);
		}

		return result;
	}

	/**
	 * 向 HTTP 客户端发送重定向。
	 *
	 * @param targetUrl 目标重定向的 URL
	 * @param exchange  当前交换
	 * @return 一个空的 Mono
	 */
	protected Mono<Void> sendRedirect(String targetUrl, ServerWebExchange exchange) {
		String transformedUrl = (isRemoteHost(targetUrl) ? targetUrl : exchange.transformUrl(targetUrl));
		ServerHttpResponse response = exchange.getResponse();
		response.getHeaders().setLocation(URI.create(transformedUrl));
		response.setStatusCode(getStatusCode());
		return Mono.empty();
	}

	/**
	 * 检查给定的 targetUrl 是否具有外部系统的主机，此时 {@link javax.servlet.http.HttpServletResponse#encodeRedirectURL}
	 * 不会应用。如果配置了 {@link #setHosts(String[])} 属性并且目标 URL 具有不匹配的主机，则此方法返回 {@code true}。
	 *
	 * @param targetUrl 目标重定向的 URL
	 * @return {@code true} 如果目标 URL 具有远程主机，{@code false} 如果 URL 没有主机或未配置 "host" 属性
	 */
	protected boolean isRemoteHost(String targetUrl) {
		if (ObjectUtils.isEmpty(this.hosts)) {
			return false;
		}
		// 获取目标URL的主机
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
		if (!StringUtils.hasLength(targetHost)) {
			return false;
		}
		for (String host : this.hosts) {
			if (targetHost.equals(host)) {
				return false;
			}
		}
		return true;
	}

}
