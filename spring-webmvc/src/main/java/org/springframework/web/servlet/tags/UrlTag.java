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

package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;
import org.springframework.web.util.UriUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

/**
 * {@code <url>} 标签用于创建 URL。模仿 JSTL {@code c:url} 标签，考虑了向后兼容性。
 *
 * <p>对 JSTL 功能的增强包括：
 * <ul>
 * <li>URL 编码的模板 URI 变量</li>
 * <li>URL 的 HTML/XML 转义</li>
 * <li>URL 的 JavaScript 转义</li>
 * </ul>
 *
 * <p>模板 URI 变量在 {@link #setValue(String) 'value'} 属性中表示，并用大括号 '{variableName}' 标记。
 * 大括号和属性名称将被替换为通过 body 中的 spring:param 标签定义的参数的 URL 编码值。如果没有可用的参数，
 * 则通过文字值传递。与模板变量匹配的参数将不会添加到查询字符串中。
 *
 * <p>强烈推荐使用 spring:param 标签来定义 URI 模板变量，而不是直接使用 EL 替换，因为值已经进行了 URL 编码。
 * 如果未正确编码 URL，应用程序可能会容易受到 XSS 和其他注入攻击的威胁。
 *
 * <p>通过将 {@link #setHtmlEscape(boolean) 'htmlEscape'} 属性设置为 'true'，
 * 可以对 URL 进行 HTML/XML 转义。检测 HTML 转义设置，可以是此标签实例上的设置，也可以是页面级别或 {@code web.xml} 级别的设置。
 * 默认值为 'false'。不建议在将 URL 值设置到变量时进行转义。
 *
 * <p>示例用法：
 * <pre class="code">&lt;spring:url value="/url/path/{variableName}"&gt;
 *   &lt;spring:param name="variableName" value="more than JSTL c:url" /&gt;
 * &lt;/spring:url&gt;</pre>
 *
 * <p>上述示例结果为：
 * {@code /currentApplicationContext/url/path/more%20than%20JSTL%20c%3Aurl}
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>必需?</th>
 * <th>运行时表达式?</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>value</td>
 * <td>true</td>
 * <td>true</td>
 * <td>要构建的 URL。此值可以包含用于替换为命名参数的 URL 编码值的模板 {placeholders}。
 * 必须使用 body 中的 param 标签定义参数。</td>
 * </tr>
 * <tr>
 * <td>context</td>
 * <td>false</td>
 * <td>true</td>
 * <td>指定远程应用程序上下文路径。默认为当前应用程序上下文路径。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>要将 URL 值导出到的变量名称。如果未指定，则将 URL 写入输出。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>要将 URL 变量导出到的范围。支持 'application'、'session'、'request' 和 'page' 范围。
 * 默认为 page 范围。只有在还定义了 var 属性时，此属性才有效。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置 HTML 转义，作为布尔值。覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置 JavaScript 转义，作为布尔值。默认为 "false"。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Scott Andrews
 * @see ParamTag
 * @since 3.0
 */
@SuppressWarnings("serial")
public class UrlTag extends HtmlEscapingAwareTag implements ParamAware {

	private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

	private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

	private static final String URL_TYPE_ABSOLUTE = "://";


	private List<Param> params = Collections.emptyList();

	private Set<String> templateParams = Collections.emptySet();

	/**
	 * URL类型
	 */
	@Nullable
	private UrlType type;

	@Nullable
	private String value;

	/**
	 * URL 的上下文路径
	 */
	@Nullable
	private String context;

	@Nullable
	private String var;

	private int scope = PageContext.PAGE_SCOPE;

	private boolean javaScriptEscape = false;


	/**
	 * 设置 URL 的值。
	 */
	public void setValue(String value) {
		// 如果值中包含绝对 URL 标志
		if (value.contains(URL_TYPE_ABSOLUTE)) {
			// 设置类型为绝对 URL
			this.type = UrlType.ABSOLUTE;
			// 将值设置为传入的值
			this.value = value;
		} else if (value.startsWith("/")) {
			// 如果值以斜杠开头
			// 设置类型为相对于上下文的相对 URL
			this.type = UrlType.CONTEXT_RELATIVE;
			this.value = value;
		} else {
			// 如果不符合以上两种情况
			// 设置类型为相对 URL
			this.type = UrlType.RELATIVE;
			this.value = value;
		}
	}

	/**
	 * 设置 URL 的上下文路径。默认为当前上下文。
	 */
	public void setContext(String context) {
		if (context.startsWith("/")) {
			this.context = context;
		} else {
			this.context = "/" + context;
		}
	}

	/**
	 * 设置要将 URL 导出到的变量名称。
	 * 默认情况下，将 URL 渲染到当前 JspWriter。
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置要将 URL 变量导出到的范围。
	 * 只有在还定义了 var 时，此属性才有效。
	 */
	public void setScope(String scope) {
		this.scope = TagUtils.getScope(scope);
	}

	/**
	 * 设置 JavaScript 转义，作为布尔值。默认为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}

	@Override
	public void addParam(Param param) {
		this.params.add(param);
	}


	@Override
	public int doStartTagInternal() throws JspException {
		this.params = new ArrayList<>();
		this.templateParams = new HashSet<>();
		return EVAL_BODY_INCLUDE;
	}

	@Override
	public int doEndTag() throws JspException {
		// 创建 URL
		String url = createUrl();

		// 获取请求数据值处理器
		RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
		// 获取 Servlet 请求
		ServletRequest request = this.pageContext.getRequest();
		// 如果存在请求数据值处理器且请求是 HttpServletRequest 的实例
		if ((processor != null) && (request instanceof HttpServletRequest)) {
			// 处理 URL
			url = processor.processUrl((HttpServletRequest) request, url);
		}

		// 如果未指定变量名
		if (this.var == null) {
			// 将 URL 打印到 writer
			try {
				this.pageContext.getOut().print(url);
			} catch (IOException ex) {
				// 抛出 JSP 异常
				throw new JspException(ex);
			}
		} else {
			// 将 URL 存储为变量
			this.pageContext.setAttribute(this.var, url, this.scope);
		}
		// 返回页面的评估
		return EVAL_PAGE;
	}


	/**
	 * 从标签属性和参数构建标签的 URL。
	 *
	 * @return URL 值作为字符串
	 */
	String createUrl() throws JspException {
		Assert.state(this.value != null, "No value set");
		// 获取 HttpServletRequest 对象
		HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
		// 获取 HttpServletResponse 对象
		HttpServletResponse response = (HttpServletResponse) this.pageContext.getResponse();

		// 创建 StringBuilder 对象来构建 URL
		StringBuilder url = new StringBuilder();
		// 如果 URL 类型是相对于上下文的相对 URL
		if (this.type == UrlType.CONTEXT_RELATIVE) {
			// 将 上下文路径 添加到 URL
			if (this.context == null) {
				url.append(request.getContextPath());
			} else {
				// 如果URL上下文路径以 / 结尾
				if (this.context.endsWith("/")) {
					// 添加 / 之前的字符串到URL中
					url.append(this.context, 0, this.context.length() - 1);
				} else {
					// 否则直接添加上下文路径
					url.append(this.context);
				}
			}
		}
		// 如果 URL 类型不是相对和绝对 URL，并且值不以斜杠开头
		if (this.type != UrlType.RELATIVE && this.type != UrlType.ABSOLUTE && !this.value.startsWith("/")) {
			// 添加 /
			url.append('/');
		}
		// 替换 URI 模板参数并添加到 URL
		url.append(replaceUriTemplateParams(this.value, this.params, this.templateParams));
		// 创建查询字符串并添加到 URL
		url.append(createQueryString(this.params, this.templateParams, (url.indexOf("?") == -1)));

		// 将 StringBuilder 转换为 String
		String urlStr = url.toString();
		// 如果 URL 类型不是绝对 URL
		if (this.type != UrlType.ABSOLUTE) {
			// 如果需要，在 URL 中添加会话标识符（不要将会话标识符嵌入到远程链接中！）
			urlStr = response.encodeURL(urlStr);
		}

		// 进行 HTML 和/或 JavaScript 转义，如果需要
		urlStr = htmlEscape(urlStr);
		urlStr = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(urlStr) : urlStr);

		// 返回 URL 字符串
		return urlStr;
	}

	/**
	 * 从可用参数构建查询字符串，这些参数尚未作为模板参数应用。
	 * <p>参数的名称和值将进行 URL 编码。
	 *
	 * @param params                      从中构建查询字符串的参数
	 * @param usedParams                  已应用为模板参数的参数名称集
	 * @param includeQueryStringDelimiter 如果查询字符串应该以 '?' 而不是 '&' 开头，则为 true
	 * @return 查询字符串
	 */
	protected String createQueryString(List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter)
			throws JspException {

		// 获取响应的字符编码
		String encoding = this.pageContext.getResponse().getCharacterEncoding();
		// 创建 StringBuilder 对象来构建查询字符串
		StringBuilder qs = new StringBuilder();
		// 遍历参数列表
		for (Param param : params) {
			// 如果参数未被使用过且参数名不为空
			if (!usedParams.contains(param.getName()) && StringUtils.hasLength(param.getName())) {
				// 如果查询字符串需要以 ? 开头，并且查询字符串为空
				if (includeQueryStringDelimiter && qs.length() == 0) {
					// 添加 ?
					qs.append('?');
				} else {
					// 否则添加 &
					qs.append('&');
				}
				try {
					// 将参数名编码，并添加进查询字符串中
					qs.append(UriUtils.encodeQueryParam(param.getName(), encoding));
					// 如果参数值不为空
					if (param.getValue() != null) {
						// 添加参数值
						qs.append('=');
						// 将参数值编码，并添加进查询字符串中
						qs.append(UriUtils.encodeQueryParam(param.getValue(), encoding));
					}
				} catch (UnsupportedCharsetException ex) {
					// 抛出 JSP 异常
					throw new JspException(ex);
				}
			}
		}
		// 返回查询字符串
		return qs.toString();
	}

	/**
	 * 替换 URL 中的模板标记，匹配可用参数。匹配参数的名称将添加到已使用参数集。
	 * <p>参数值将进行 URL 编码。
	 *
	 * @param uri        要替换为模板参数的 URL
	 * @param params     用于替换模板标记的参数
	 * @param usedParams 已替换的模板参数名称集
	 * @return 替换了模板参数的 URL
	 */
	protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams)
			throws JspException {

		// 获取Http响应的字符编码
		String encoding = this.pageContext.getResponse().getCharacterEncoding();
		// 遍历参数列表
		for (Param param : params) {
			// 构建参数模板
			String template = URL_TEMPLATE_DELIMITER_PREFIX + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
			// 如果 URI 中包含参数模板
			if (uri.contains(template)) {
				// 将参数标记为已使用
				usedParams.add(param.getName());
				// 获取参数值
				String value = param.getValue();
				try {
					// 替换参数模板为编码后的参数值
					uri = StringUtils.replace(uri, template,
							(value != null ? UriUtils.encodePath(value, encoding) : ""));
				} catch (UnsupportedCharsetException ex) {
					// 抛出 JSP 异常
					throw new JspException(ex);
				}
			} else {
				// 如果 URI 中不包含参数模板
				// 构建另一种参数模板
				template = URL_TEMPLATE_DELIMITER_PREFIX + '/' + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
				// 如果 URI 中包含参数模板
				if (uri.contains(template)) {
					// 将参数标记为已使用
					usedParams.add(param.getName());
					// 获取参数值
					String value = param.getValue();
					try {
						// 替换参数模板为编码后的参数值
						uri = StringUtils.replace(uri, template,
								(value != null ? UriUtils.encodePathSegment(value, encoding) : ""));
					} catch (UnsupportedCharsetException ex) {
						// 抛出 JSP 异常
						throw new JspException(ex);
					}
				}
			}
		}
		// 返回替换参数后的 URI
		return uri;
	}


	/**
	 * 内部枚举，按类型对 URL 进行分类。
	 */
	private enum UrlType {

		CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
	}

}
