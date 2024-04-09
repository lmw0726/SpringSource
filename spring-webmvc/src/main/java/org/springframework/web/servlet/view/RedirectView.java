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

package org.springframework.web.servlet.view;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用于重定向到绝对、上下文相对或当前请求相对 URL 的视图。URL 可能是一个 URI 模板，其中 URI 模板变量将被模型中可用的值替换。
 * 默认情况下，所有原始模型属性（或其集合）将作为 HTTP 查询参数公开（假设它们尚未用作 URI 模板变量），
 * 但此行为可以通过覆盖 isEligibleProperty(String, Object) 方法进行更改。
 *
 * <p>此视图的 URL 应为 HTTP 重定向 URL，即适用于 HttpServletResponse 的 {@code sendRedirect} 方法，
 * 如果启用了 HTTP 1.0 标志，则实际上会执行重定向，或通过返回 HTTP 303 代码进行重定向 - 如果关闭了 HTTP 1.0 兼容性标志。
 *
 * <p>请注意，尽管 “contextRelative” 标志的默认值为关闭状态，但您可能希望几乎始终将其设置为 true。
 * 当标志关闭时，以 “/” 开头的 URL 将被视为相对于 Web 服务器根，而当标志打开时，它们将被视为相对于 Web 应用程序根。
 * 由于大多数 Web 应用程序永远不会知道或关心其上下文路径实际是什么，
 * 因此最好将此标志设置为 true，并提交要视为相对于 Web 应用程序根的路径。
 *
 * <p><b>在 Portlet 环境中使用此重定向视图时的注意事项：</b>确保您的控制器遵守 Portlet {@code sendRedirect} 约束。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see #setContextRelative
 * @see #setHttp10Compatible
 * @see #setExposeModelAttributes
 * @see javax.servlet.http.HttpServletResponse#sendRedirect
 */
public class RedirectView extends AbstractUrlBasedView implements SmartView {

	/**
	 * URI模板变量模式
	 */
	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	/**
	 * 是否将给定的 URL 解释为相对于当前 ServletContext
	 */
	private boolean contextRelative = false;

	/**
	 * 是否与 HTTP 1.0 客户端兼容
	 */
	private boolean http10Compatible = true;

	/**
	 * 是否将模型属性公开为查询参数
	 */
	private boolean exposeModelAttributes = true;

	/**
	 * 编码方案
	 */
	@Nullable
	private String encodingScheme;

	/**
	 * http状态码
	 */
	@Nullable
	private HttpStatus statusCode;

	/**
	 * 是否将重定向 URL 视为 URI 模板。
	 */
	private boolean expandUriTemplateVariables = true;

	/**
	 * 是否传播当前 URL 的查询参数。
	 */
	private boolean propagateQueryParams = false;

	/**
	 * 已配置的应用程序域名数组
	 */
	@Nullable
	private String[] hosts;


	/**
	 * 用作 bean 的构造方法。
	 */
	public RedirectView() {
		setExposePathVariables(false);
	}

	/**
	 * 使用给定的 URL 创建一个新的 RedirectView。
	 * <p>给定的 URL 将被视为相对于 Web 服务器，而不是相对于当前 ServletContext。
	 *
	 * @param url 要重定向到的 URL
	 * @see #RedirectView(String, boolean)
	 */
	public RedirectView(String url) {
		super(url);
		setExposePathVariables(false);
	}

	/**
	 * 使用给定的 URL 创建一个新的 RedirectView。
	 *
	 * @param url             要重定向到的 URL
	 * @param contextRelative 是否将给定的 URL 解释为相对于当前 ServletContext
	 */
	public RedirectView(String url, boolean contextRelative) {
		super(url);
		this.contextRelative = contextRelative;
		setExposePathVariables(false);
	}

	/**
	 * 使用给定的 URL 创建一个新的 RedirectView。
	 *
	 * @param url              要重定向到的 URL
	 * @param contextRelative  是否将给定的 URL 解释为相对于当前 ServletContext
	 * @param http10Compatible 是否与 HTTP 1.0 客户端兼容
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		setExposePathVariables(false);
	}

	/**
	 * 使用给定的 URL 创建一个新的 RedirectView。
	 *
	 * @param url                   要重定向到的 URL
	 * @param contextRelative       是否将给定的 URL 解释为相对于当前 ServletContext
	 * @param http10Compatible      是否与 HTTP 1.0 客户端兼容
	 * @param exposeModelAttributes 是否将模型属性公开为查询参数
	 */
	public RedirectView(String url, boolean contextRelative, boolean http10Compatible, boolean exposeModelAttributes) {
		super(url);
		this.contextRelative = contextRelative;
		this.http10Compatible = http10Compatible;
		this.exposeModelAttributes = exposeModelAttributes;
		setExposePathVariables(false);
	}


	/**
	 * 设置是否将以斜杠（“/”）开头的给定 URL 解释为相对于当前 ServletContext，即相对于 Web 应用程序根。
	 * <p>默认值为 “false”：以斜杠开头的 URL 将被视为绝对的，即保持不变。如果为 “true”，则在这种情况下将在 URL 前面添加上下文路径。
	 *
	 * @see javax.servlet.http.HttpServletRequest#getContextPath
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * 设置是否与 HTTP 1.0 客户端兼容。
	 * <p>在默认实现中，这将在任何情况下强制使用 HTTP 状态码 302，即委托给 {@code HttpServletResponse.sendRedirect}。
	 * 关闭此选项将发送 HTTP 状态码 303，这是 HTTP 1.1 客户端的正确代码，但不被 HTTP 1.0 客户端理解。
	 * <p>许多 HTTP 1.1 客户端将 302 视为 303，不产生任何区别。
	 * 但是，在 POST 请求后重定向时，某些客户端依赖于 303；在这种情况下，请关闭此标志。
	 *
	 * @see javax.servlet.http.HttpServletResponse#sendRedirect
	 */
	public void setHttp10Compatible(boolean http10Compatible) {
		this.http10Compatible = http10Compatible;
	}

	/**
	 * 设置 {@code exposeModelAttributes} 标志，表示是否将模型属性公开为 HTTP 查询参数。
	 * <p>默认为 {@code true}。
	 */
	public void setExposeModelAttributes(final boolean exposeModelAttributes) {
		this.exposeModelAttributes = exposeModelAttributes;
	}

	/**
	 * 为此视图设置编码方案。
	 * <p>默认情况下为请求的编码方案（如果没有另外指定，则为 ISO-8859-1）。
	 */
	public void setEncodingScheme(String encodingScheme) {
		this.encodingScheme = encodingScheme;
	}

	/**
	 * 为此视图设置状态码。
	 * <p>默认为发送 302/303，取决于 {@link #setHttp10Compatible(boolean) http10Compatible} 标志的值。
	 */
	public void setStatusCode(HttpStatus statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 是否将重定向 URL 视为 URI 模板。
	 * 如果重定向 URL 包含打开和关闭大括号 “{”，“}”，并且您不希望它们被解释为 URI 变量，则将此标志设置为 {@code false}。
	 * <p>默认为 {@code true}。
	 */
	public void setExpandUriTemplateVariables(boolean expandUriTemplateVariables) {
		this.expandUriTemplateVariables = expandUriTemplateVariables;
	}

	/**
	 * 当设置为 {@code true} 时，当前 URL 的查询字符串将被附加，因此通过传播到重定向的 URL。
	 * <p>默认为 {@code false}。
	 *
	 * @since 4.1
	 */
	public void setPropagateQueryParams(boolean propagateQueryParams) {
		this.propagateQueryParams = propagateQueryParams;
	}

	/**
	 * 是否传播当前 URL 的查询参数。
	 *
	 * @since 4.1
	 */
	public boolean isPropagateQueryProperties() {
		return this.propagateQueryParams;
	}

	/**
	 * 配置与应用程序关联的一个或多个主机。所有其他主机都将被视为外部主机。
	 * <p>实际上，此属性提供了一种通过 {@link HttpServletResponse#encodeRedirectURL} 关闭对具有主机且该主机未列为已知主机的 URL 进行编码的方法。
	 * <p>如果未设置（默认值），则通过响应对所有 URL 进行编码。
	 *
	 * @param hosts 一个或多个应用程序主机
	 * @since 4.3
	 */
	public void setHosts(@Nullable String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * 返回已配置的应用程序主机。
	 *
	 * @since 4.3
	 */
	@Nullable
	public String[] getHosts() {
		return this.hosts;
	}

	/**
	 * 返回 “true”，表示此视图执行重定向。
	 */
	@Override
	public boolean isRedirectView() {
		return true;
	}

	/**
	 * RedirectView 不严格要求 ApplicationContext。
	 */
	@Override
	protected boolean isContextRequired() {
		return false;
	}


	/**
	 * 将模型转换为请求参数并重定向到给定的 URL。
	 *
	 * @see #appendQueryProperties
	 * @see #sendRedirect
	 */
	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
										   HttpServletResponse response) throws IOException {
		// 创建目标URL
		String targetUrl = createTargetUrl(model, request);
		// 更新目标URL
		targetUrl = updateTargetUrl(targetUrl, model, request, response);

		// 保存闪存属性
		RequestContextUtils.saveOutputFlashMap(targetUrl, request, response);

		// 重定向
		sendRedirect(request, response, targetUrl, this.http10Compatible);
	}

	/**
	 * 通过首先检查重定向字符串是否是 URI 模板来创建目标 URL，然后使用给定模型扩展它，并根据需要附加简单类型模型属性作为查询字符串参数。
	 */
	protected final String createTargetUrl(Map<String, Object> model, HttpServletRequest request)
			throws UnsupportedEncodingException {

		// 准备目标 URL。
		StringBuilder targetUrl = new StringBuilder();
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		if (this.contextRelative && getUrl().startsWith("/")) {
			// 不要将上下文路径应用于相对 URL。
			targetUrl.append(getContextPath(request));
		}
		targetUrl.append(getUrl());

		// 设置编码方案
		String enc = this.encodingScheme;
		if (enc == null) {
			enc = request.getCharacterEncoding();
		}
		if (enc == null) {
			// 如果编码方案仍然为空，则设置为默认的编码
			enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}

		if (this.expandUriTemplateVariables && StringUtils.hasText(targetUrl)) {
			// 如果将重定向 URL 视为 URI 模板，并且目标URL存在
			// 获取当前请求的URL变量
			Map<String, String> variables = getCurrentRequestUriVariables(request);
			// 替换URL模板变量
			targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, enc);
		}
		if (isPropagateQueryProperties()) {
			// 如果设置了传播当前 URL 的查询参数。，则追加当前的查询参数到目标URL中
			appendCurrentQueryParams(targetUrl, request);
		}
		if (this.exposeModelAttributes) {
			// 如果将模型属性公开为查询参数，则追加查询属性到目标URL中
			appendQueryProperties(targetUrl, model, enc);
		}

		return targetUrl.toString();
	}

	private String getContextPath(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		while (contextPath.startsWith("//")) {
			contextPath = contextPath.substring(1);
		}
		return contextPath;
	}

	/**
	 * 使用编码的模型属性或当前请求中的 URI 变量替换目标 URL 中的 URI 模板变量。URL 中引用的模型属性将从模型中删除。
	 *
	 * @param targetUrl           要重定向的 URL
	 * @param model               包含模型属性的 Map
	 * @param currentUriVariables 要使用的当前请求 URI 变量
	 * @param encodingScheme      要使用的编码方案
	 * @return 替换 URI 模板变量后的 StringBuilder
	 * @throws UnsupportedEncodingException 如果字符串编码失败
	 */
	protected StringBuilder replaceUriTemplateVariables(
			String targetUrl, Map<String, Object> model, Map<String, String> currentUriVariables, String encodingScheme)
			throws UnsupportedEncodingException {

		StringBuilder result = new StringBuilder();
		// 使用正则表达式匹配 URI 模板变量
		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		int endLastMatch = 0;
		while (matcher.find()) {
			String name = matcher.group(1);
			// 从模型中获取变量值，如果不存在，则从当前 URI 变量中获取
			Object value = (model.containsKey(name) ? model.remove(name) : currentUriVariables.get(name));
			if (value == null) {
				// 如果找不到变量值，则抛出异常
				throw new IllegalArgumentException("Model has no value for key '" + name + "'");
			}
			// 将变量值编码并追加到结果中
			result.append(targetUrl, endLastMatch, matcher.start());
			result.append(UriUtils.encodePathSegment(value.toString(), encodingScheme));
			endLastMatch = matcher.end();
		}
		// 将最后一个匹配位置之后的部分追加到结果中
		result.append(targetUrl.substring(endLastMatch));
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getCurrentRequestUriVariables(HttpServletRequest request) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		// 从请求属性中获取 URI 模板变量的映射
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(name);
		// 如果映射不为 null，则返回映射；否则返回空映射
		return (uriVars != null) ? uriVars : Collections.emptyMap();
	}

	/**
	 * 将当前请求的查询字符串附加到目标重定向 URL。
	 *
	 * @param targetUrl 要附加属性的 StringBuilder
	 * @param request   当前请求
	 * @since 4.1
	 */
	protected void appendCurrentQueryParams(StringBuilder targetUrl, HttpServletRequest request) {
		String query = request.getQueryString();
		// 如果查询字符串不为空
		if (StringUtils.hasText(query)) {
			// 提取锚点片段，如果有的话。
			String fragment = null;
			int anchorIndex = targetUrl.indexOf("#");
			// 如果存在锚点索引
			if (anchorIndex > -1) {
				// 提取锚点片段
				fragment = targetUrl.substring(anchorIndex);
				// 从目标 URL 中删除锚点片段
				targetUrl.delete(anchorIndex, targetUrl.length());
			}

			// 如果目标 URL 中不包含查询字符串
			if (targetUrl.toString().indexOf('?') < 0) {
				// 在目标 URL 后附加查询字符串
				targetUrl.append('?').append(query);
			} else {
				// 在目标 URL 后附加查询字符串
				targetUrl.append('&').append(query);
			}
			// 将锚点片段（如果存在）附加到 URL 的末尾。
			if (fragment != null) {
				// 在目标 URL 的末尾添加锚点片段
				targetUrl.append(fragment);
			}
		}
	}

	/**
	 * 将查询属性附加到重定向 URL。
	 * 将模型属性字符串化、URL 编码并格式化为查询属性。
	 *
	 * @param targetUrl      要附加属性的 StringBuilder
	 * @param model          包含模型属性的 Map
	 * @param encodingScheme 要使用的编码方案
	 * @throws UnsupportedEncodingException 如果字符串编码失败
	 * @see #queryProperties
	 */
	@SuppressWarnings("unchecked")
	protected void appendQueryProperties(StringBuilder targetUrl, Map<String, Object> model, String encodingScheme)
			throws UnsupportedEncodingException {

		// 提取锚点片段，如果有的话。
		String fragment = null;
		int anchorIndex = targetUrl.indexOf("#");
		// 如果存在锚点索引
		if (anchorIndex > -1) {
			// 提取锚点片段
			fragment = targetUrl.substring(anchorIndex);
			// 从目标 URL 中删除锚点片段
			targetUrl.delete(anchorIndex, targetUrl.length());
		}

		// 如果还没有一些参数，我们需要一个 “?”。
		boolean first = (targetUrl.toString().indexOf('?') < 0);
		// 遍历查询属性的映射
		for (Map.Entry<String, Object> entry : queryProperties(model).entrySet()) {
			Object rawValue = entry.getValue();
			Collection<?> values;
			// 如果值不为空并且是数组类型
			if (rawValue != null && rawValue.getClass().isArray()) {
				// 将值转换为列表
				values = CollectionUtils.arrayToList(rawValue);
			} else if (rawValue instanceof Collection) {
				// 如果值是集合类型，则直接使用
				values = ((Collection<?>) rawValue);
			} else {
				// 否则将值放入单值集合中
				values = Collections.singleton(rawValue);
			}
			// 遍历值的集合
			for (Object value : values) {
				// 如果是第一个参数
				if (first) {
					// 添加查询字符串的起始符号
					targetUrl.append('?');
					first = false;
				} else {
					// 否则添加参数分隔符号
					targetUrl.append('&');
				}
				// 对键和值进行 URL 编码并附加到目标 URL
				String encodedKey = urlEncode(entry.getKey(), encodingScheme);
				String encodedValue = (value != null ? urlEncode(value.toString(), encodingScheme) : "");
				targetUrl.append(encodedKey).append('=').append(encodedValue);
			}
		}

		// 将锚点片段（如果存在）附加到 URL 的末尾。
		if (fragment != null) {
			// 在目标 URL 的末尾添加锚点片段
			targetUrl.append(fragment);
		}
	}

	/**
	 * 确定查询字符串的名称-值对，这些名称-值对将由 {@link #appendQueryProperties} 字符串化、URL 编码并格式化。
	 * <p>此实现通过检查每个元素的 {@link #isEligibleProperty(String, Object)} 来过滤模型。
	 * 默认仅接受字符串、原始类型和原始包装类型。
	 *
	 * @param model 原始模型 Map
	 * @return 合格的查询属性的过滤 Map
	 * @see #isEligibleProperty(String, Object)
	 */
	protected Map<String, Object> queryProperties(Map<String, Object> model) {
		Map<String, Object> result = new LinkedHashMap<>();
		// 遍历模型中的每个属性
		model.forEach((name, value) -> {
			// 如果属性符合条件，则将其添加到结果中
			if (isEligibleProperty(name, value)) {
				result.put(name, value);
			}
		});
		return result;
	}

	/**
	 * 确定给定模型元素是否应公开为查询属性。
	 * <p>默认实现将原始字符串和原始类型视为符合条件，并且还将数组和具有相应元素的集合/可迭代对象视为符合条件。这可以在子类中重写。
	 *
	 * @param key   模型元素的键
	 * @param value 模型元素的值
	 * @return 元素是否符合查询属性
	 */
	protected boolean isEligibleProperty(String key, @Nullable Object value) {
		if (value == null) {
			return false;
		}
		// 检查单个值是否符合条件
		if (isEligibleValue(value)) {
			return true;
		}
		// 如果值是数组
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			// 如果数组为空，则返回false
			if (length == 0) {
				return false;
			}
			// 遍历数组中的每个元素
			for (int i = 0; i < length; i++) {
				Object element = Array.get(value, i);
				// 如果数组中的任何元素不符合条件，则返回false
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		} else if (value instanceof Collection) {
			// 如果值是集合
			Collection<?> coll = (Collection<?>) value;
			// 如果集合为空，则返回false
			if (coll.isEmpty()) {
				return false;
			}
			// 遍历集合中的每个元素
			for (Object element : coll) {
				// 如果集合中的任何元素不符合条件，则返回false
				if (!isEligibleValue(element)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 确定给定值是否可以直接包含在查询属性中。
	 * <p>默认实现检查是否为原始字符串或原始类型（以及包装器）。
	 *
	 * @param value 要检查的值
	 * @return 给定值是否适合作为查询属性的结果
	 */
	protected boolean isEligibleValue(@Nullable Object value) {
		return (value != null && BeanUtils.isSimpleValueType(value.getClass()));
	}

	/**
	 * 使用给定的编码方案对输入字符串进行URL编码。
	 * <p>默认实现使用 {@code URLEncoder.encode(input, enc)}。
	 *
	 * @param input          要编码的未编码输入字符串
	 * @param encodingScheme 编码方案
	 * @return 编码后的输出字符串
	 * @throws UnsupportedEncodingException 如果 JDK URLEncoder 抛出异常
	 * @see java.net.URLEncoder#encode(String, String)
	 */
	protected String urlEncode(String input, String encodingScheme) throws UnsupportedEncodingException {
		return URLEncoder.encode(input, encodingScheme);
	}

	/**
	 * 查找已注册的 {@link RequestDataValueProcessor}（如果有），并允许其更新重定向的目标 URL。
	 *
	 * @param targetUrl 重定向的目标 URL
	 * @param model     包含模型属性的 Map
	 * @param request   当前 HTTP 请求（允许对请求方法做出反应）
	 * @param response  当前 HTTP 响应（用于发送响应头）
	 * @return 更新后的 URL 或与传入的 URL 相同
	 */
	protected String updateTargetUrl(String targetUrl, Map<String, Object> model,
									 HttpServletRequest request, HttpServletResponse response) {

		// 获取Web应用程序上下文
		WebApplicationContext wac = getWebApplicationContext();
		// 如果Web应用程序上下文为空，则尝试通过请求和ServletContext查找
		if (wac == null) {
			wac = RequestContextUtils.findWebApplicationContext(request, getServletContext());
		}

		// 如果Web应用程序上下文不为空，并且包含请求数据值处理器的bean，则使用处理器处理目标URL
		if (wac != null && wac.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			RequestDataValueProcessor processor = wac.getBean(
					RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
			return processor.processUrl(request, targetUrl);
		}

		// 如果没有可用的处理器，则返回原始的目标URL
		return targetUrl;
	}

	/**
	 * 通过适当的 HTTP 重定向发送给定 URL。
	 *
	 * @param request          当前 HTTP 请求
	 * @param response         当前 HTTP 响应
	 * @param targetUrl        要重定向到的 URL
	 * @param http10Compatible 是否与 HTTP 1.0 客户端兼容
	 * @throws IOException 如果无法发送重定向 URL
	 */
	protected void sendRedirect(HttpServletRequest request, HttpServletResponse response,
								String targetUrl, boolean http10Compatible) throws IOException {

		// 对目标URL进行编码，如果是远程主机则直接使用，否则使用response.encodeRedirectURL进行编码
		String encodedURL = (isRemoteHost(targetUrl) ? targetUrl : response.encodeRedirectURL(targetUrl));

		// 如果是HTTP 1.0兼容模式
		if (http10Compatible) {
			// 检查请求属性中是否包含响应状态码
			HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
			// 如果显式设置了状态码，则使用该状态码和编码后的URL进行重定向
			if (this.statusCode != null) {
				response.setStatus(this.statusCode.value());
				response.setHeader("Location", encodedURL);
			} else if (attributeStatusCode != null) {
				// 如果请求属性中包含响应状态码，则使用该状态码和编码后的URL进行重定向
				response.setStatus(attributeStatusCode.value());
				response.setHeader("Location", encodedURL);
			} else {
				// 否则，默认使用302状态码进行重定向
				response.sendRedirect(encodedURL);
			}
		} else {
			// 如果是HTTP 1.1或更高版本
			// 获取HTTP 1.1版本的状态码，并将其和编码后的URL一起设置到响应头中
			HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
			response.setStatus(statusCode.value());
			response.setHeader("Location", encodedURL);
		}
	}

	/**
	 * 判断给定的目标 URL 是否具有属于“外部”系统的主机，此时将不会应用 {@link HttpServletResponse#encodeRedirectURL}。
	 * 如果配置了 {@link #setHosts(String[])} 属性，并且目标 URL 具有不匹配的主机，则此方法返回 {@code true}。
	 *
	 * @param targetUrl 目标重定向 URL
	 * @return {@code true} 如果目标 URL 具有远程主机，{@code false} 如果 URL 没有主机或未配置“host”属性。
	 * @since 4.3
	 */
	protected boolean isRemoteHost(String targetUrl) {
		// 如果主机列表为空，则返回false
		if (ObjectUtils.isEmpty(getHosts())) {
			return false;
		}

		// 从目标URL中提取主机名
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();

		// 如果目标主机名为空，则返回false
		if (!StringUtils.hasLength(targetHost)) {
			return false;
		}

		// 遍历主机列表，如果目标主机名与列表中的任何一个相匹配，则返回false
		for (String host : getHosts()) {
			if (targetHost.equals(host)) {
				return false;
			}
		}

		// 如果目标主机名与主机列表中的任何一个都不匹配，则返回true
		return true;
	}

	/**
	 * 确定要在与 HTTP 1.1 兼容的请求中使用的状态码。
	 * <p>如果设置了 {@link #setStatusCode(HttpStatus)} 属性，则默认实现返回该属性值；
	 * 如果未设置，则返回 {@link #RESPONSE_STATUS_ATTRIBUTE} 属性的值。
	 * 如果两者都未设置，则默认为 {@link HttpStatus#SEE_OTHER}（303）。
	 *
	 * @param request   要检查的请求
	 * @param response  servlet 响应
	 * @param targetUrl 目标 URL
	 * @return 响应状态
	 */
	protected HttpStatus getHttp11StatusCode(
			HttpServletRequest request, HttpServletResponse response, String targetUrl) {

		// 如果指定了状态码，则返回该状态码
		if (this.statusCode != null) {
			return this.statusCode;
		}

		// 从请求属性中获取状态码
		HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);

		// 如果请求属性中存在状态码，则返回该状态码
		if (attributeStatusCode != null) {
			return attributeStatusCode;
		}

		// 如果未指定状态码且请求属性中也没有状态码，则返回默认状态码 HttpStatus.SEE_OTHER
		return HttpStatus.SEE_OTHER;
	}

}
