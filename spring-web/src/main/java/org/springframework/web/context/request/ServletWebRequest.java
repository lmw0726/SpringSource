/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.context.request;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link WebRequest}适配器，用于{@link javax.servlet.http.HttpServletRequest}。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 * @since 2.0
 */
public class ServletWebRequest extends ServletRequestAttributes implements NativeWebRequest {
	/**
	 * 安全方法集合
	 */
	private static final List<String> SAFE_METHODS = Arrays.asList("GET", "HEAD");

	/**
	 * 匹配标题中ETag多个字段值的模式，例如“If-Match”、“If-None-Match”。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232的第2.3节</a>
	 */
	private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

	/**
	 * HTTP RFC中指定的日期格式。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">RFC 7231的第7.1.1.1节</a>
	 */
	private static final String[] DATE_FORMATS = new String[]{
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	/**
	 * 是否未曾修改
	 */
	private boolean notModified = false;


	/**
	 * 为给定请求创建一个新的ServletWebRequest实例。
	 *
	 * @param request 当前HTTP请求
	 */
	public ServletWebRequest(HttpServletRequest request) {
		super(request);
	}

	/**
	 * 为给定请求/响应对创建一个新的ServletWebRequest实例。
	 *
	 * @param request  当前HTTP请求
	 * @param response 当前HTTP响应（用于自动处理最后修改的操作）
	 */
	public ServletWebRequest(HttpServletRequest request, @Nullable HttpServletResponse response) {
		super(request, response);
	}


	@Override
	public Object getNativeRequest() {
		return getRequest();
	}

	@Override
	public Object getNativeResponse() {
		return getResponse();
	}

	@Override
	public <T> T getNativeRequest(@Nullable Class<T> requiredType) {
		return WebUtils.getNativeRequest(getRequest(), requiredType);
	}

	@Override
	public <T> T getNativeResponse(@Nullable Class<T> requiredType) {
		// 获取响应对象
		HttpServletResponse response = getResponse();
		// 如果响应对象不为 null，则将其转换为所需类型的原生响应对象并返回，否则返回 null
		return (response != null ? WebUtils.getNativeResponse(response, requiredType) : null);
	}

	/**
	 * 返回请求的HTTP方法。
	 *
	 * @since 4.0.2
	 */
	@Nullable
	public HttpMethod getHttpMethod() {
		return HttpMethod.resolve(getRequest().getMethod());
	}

	@Override
	@Nullable
	public String getHeader(String headerName) {
		return getRequest().getHeader(headerName);
	}

	@Override
	@Nullable
	public String[] getHeaderValues(String headerName) {
		// 获取指定请求头的所有值，并转换为字符串数组
		String[] headerValues = StringUtils.toStringArray(getRequest().getHeaders(headerName));
		// 如果数组不为空，则返回数组，否则返回null
		return (!ObjectUtils.isEmpty(headerValues) ? headerValues : null);
	}

	@Override
	public Iterator<String> getHeaderNames() {
		return CollectionUtils.toIterator(getRequest().getHeaderNames());
	}

	@Override
	@Nullable
	public String getParameter(String paramName) {
		return getRequest().getParameter(paramName);
	}

	@Override
	@Nullable
	public String[] getParameterValues(String paramName) {
		return getRequest().getParameterValues(paramName);
	}

	@Override
	public Iterator<String> getParameterNames() {
		return CollectionUtils.toIterator(getRequest().getParameterNames());
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return getRequest().getParameterMap();
	}

	@Override
	public Locale getLocale() {
		return getRequest().getLocale();
	}

	@Override
	public String getContextPath() {
		return getRequest().getContextPath();
	}

	@Override
	@Nullable
	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	@Override
	@Nullable
	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String role) {
		return getRequest().isUserInRole(role);
	}

	@Override
	public boolean isSecure() {
		return getRequest().isSecure();
	}


	@Override
	public boolean checkNotModified(long lastModifiedTimestamp) {
		return checkNotModified(null, lastModifiedTimestamp);
	}

	@Override
	public boolean checkNotModified(String etag) {
		return checkNotModified(etag, -1);
	}

	@Override
	public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
		// 获取响应对象
		HttpServletResponse response = getResponse();
		// 如果请求未修改，或者响应不为空且状态码不是OK
		if (this.notModified || (response != null && HttpStatus.OK.value() != response.getStatus())) {
			// 返回当前未曾修改属性值
			return this.notModified;
		}

		// 根据优先级顺序评估条件
		// 参考：https://tools.ietf.org/html/rfc7232#section-6

		// 如果验证未修改自指定时间以来的响应
		if (validateIfUnmodifiedSince(lastModifiedTimestamp)) {
			// 如果未修改且响应不为空
			if (this.notModified && response != null) {
				// 设置响应状态码为412
				response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
			}
			// 返回当前未曾修改属性值
			return this.notModified;
		}

		//  根据 ETag 验证请求是否匹配
		boolean validated = validateIfNoneMatch(etag);
		if (!validated) {
			// 根据最后修改时间戳验证请求是否修改
			validateIfModifiedSince(lastModifiedTimestamp);
		}

		// 更新响应
		if (response != null) {
			// 判断是否为GET或HEAD请求
			boolean isHttpGetOrHead = SAFE_METHODS.contains(getRequest().getMethod());
			// 如果未修改
			if (this.notModified) {
				// 如果为GET或HEAD请求，则设置响应状态码为304，否则为412
				response.setStatus(isHttpGetOrHead ?
						HttpStatus.NOT_MODIFIED.value() : HttpStatus.PRECONDITION_FAILED.value());
			}
			// 如果为GET或HEAD请求
			if (isHttpGetOrHead) {
				// 如果最后修改时间大于0且响应头中的LAST_MODIFIED值未设置
				if (lastModifiedTimestamp > 0 && parseDateValue(response.getHeader(HttpHeaders.LAST_MODIFIED)) == -1) {
					// 设置响应头中的LAST_MODIFIED值为最后修改时间
					response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedTimestamp);
				}
				// 如果ETag值不为空且响应头中的ETAG值未设置
				if (StringUtils.hasLength(etag) && response.getHeader(HttpHeaders.ETAG) == null) {
					// 设置响应头中的ETAG值
					response.setHeader(HttpHeaders.ETAG, padEtagIfNecessary(etag));
				}
			}
		}
		// 返回当前未曾修改属性值
		return this.notModified;
	}

	private boolean validateIfUnmodifiedSince(long lastModifiedTimestamp) {
		// 如果最后修改时间戳小于 0，则返回假
		if (lastModifiedTimestamp < 0) {
			return false;
		}
		// 解析请求头中的 If-Unmodified-Since 时间戳
		long ifUnmodifiedSince = parseDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
		// 如果解析结果为 -1，则返回假
		if (ifUnmodifiedSince == -1) {
			return false;
		}
		// 执行验证...
		this.notModified = (ifUnmodifiedSince < (lastModifiedTimestamp / 1000 * 1000));
		// 返回真
		return true;
	}

	private boolean validateIfNoneMatch(@Nullable String etag) {
		// 如果ETag值为空，返回false
		if (!StringUtils.hasLength(etag)) {
			return false;
		}

		Enumeration<String> ifNoneMatch;
		try {
			// 获取请求头中的 If-None-Match值 的枚举对象
			ifNoneMatch = getRequest().getHeaders(HttpHeaders.IF_NONE_MATCH);
		} catch (IllegalArgumentException ex) {
			return false;
		}
		// 如果 If-None-Match值 为空，则返回false
		if (!ifNoneMatch.hasMoreElements()) {
			return false;
		}

		// 执行ETag值的验证
		etag = padEtagIfNecessary(etag);
		if (etag.startsWith("W/")) {
			// 裁剪掉ETag值的 “W/” 前缀
			etag = etag.substring(2);
		}
		while (ifNoneMatch.hasMoreElements()) {
			// 客户端ETags值
			String clientETags = ifNoneMatch.nextElement();
			// 匹配ETags的正则表达式
			Matcher etagMatcher = ETAG_HEADER_VALUE_PATTERN.matcher(clientETags);
			// 比较强/弱ETags，参考：https://tools.ietf.org/html/rfc7232#section-2.3
			while (etagMatcher.find()) {
				// 如果客户端ETags和服务器端ETag匹配
				if (StringUtils.hasLength(etagMatcher.group()) && etag.equals(etagMatcher.group(3))) {
					// 设置notModified为true
					this.notModified = true;
					break;
				}
			}
		}

		return true;
	}

	private String padEtagIfNecessary(String etag) {
		// 如果 ETag 为空或长度为零，则直接返回 ETag
		if (!StringUtils.hasLength(etag)) {
			return etag;
		}
		if ((etag.startsWith("\"") || etag.startsWith("W/\"")) && etag.endsWith("\"")) {
			// 如果 ETag 以双引号或以 W/\" 开头，并且以双引号结尾，则直接返回 ETag
			return etag;
		}
		// 否则，在 ETag 前后添加双引号后返回
		return "\"" + etag + "\"";
	}

	private boolean validateIfModifiedSince(long lastModifiedTimestamp) {
		// 如果最后修改时间小于0，则返回false
		if (lastModifiedTimestamp < 0) {
			return false;
		}
		// 获取请求头中的 If-Modified-Since值
		long ifModifiedSince = parseDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
		// 如果 If-Modified-Since值 为-1，则返回false
		if (ifModifiedSince == -1) {
			return false;
		}
		// 执行修改时间的验证
		// 如果 If-Modified-Since值 大于等于（最后修改时间的秒数 * 1000）
		this.notModified = ifModifiedSince >= (lastModifiedTimestamp / 1000 * 1000);
		return true;
	}

	public boolean isNotModified() {
		return this.notModified;
	}

	private long parseDateHeader(String headerName) {
		// 初始化日期值为 -1
		long dateValue = -1;
		try {
			// 尝试从请求头中获取日期值
			dateValue = getRequest().getDateHeader(headerName);
		} catch (IllegalArgumentException ex) {
			// 捕获 IllegalArgumentException 异常
			// 可能是 IE 10 样式的值："Wed, 09 Apr 2014 09:57:42 GMT; length=13774"
			String headerValue = getHeader(headerName);
			// 如果请求头值不为 null
			if (headerValue != null) {
				// 查找分号的索引
				int separatorIndex = headerValue.indexOf(';');
				// 如果找到分号
				if (separatorIndex != -1) {
					// 提取日期部分
					String datePart = headerValue.substring(0, separatorIndex);
					// 解析日期值
					dateValue = parseDateValue(datePart);
				}
			}
		}
		// 返回日期值
		return dateValue;
	}

	private long parseDateValue(@Nullable String headerValue) {
		// 如果请求头值为空，则返回-1
		if (headerValue == null) {
			// 没有发送任何头值
			return -1;
		}
		if (headerValue.length() >= 3) {
			// 短的“0”或“-1”这样的值永远不是有效的HTTP日期头...
			// 只有在值足够长的情况下才会考虑SimpleDateFormat解析。
			// 遍历日期格式数组
			for (String dateFormat : DATE_FORMATS) {
				// 创建 SimpleDateFormat 对象，使用指定的日期格式和美国本地化
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
				// 设置时区为 GMT
				simpleDateFormat.setTimeZone(GMT);
				try {
					// 尝试解析日期值并返回时间戳
					return simpleDateFormat.parse(headerValue).getTime();
				} catch (ParseException ex) {
					// 解析失败时忽略异常
				}
			}
		}
		return -1;
	}

	@Override
	public String getDescription(boolean includeClientInfo) {
		// 获取请求对象
		HttpServletRequest request = getRequest();
		// 创建 字符串构建器 对象
		StringBuilder sb = new StringBuilder();
		// 添加 URI 到字符串中
		sb.append("uri=").append(request.getRequestURI());
		// 如果包含客户端信息
		if (includeClientInfo) {
			// 获取客户端地址
			String client = request.getRemoteAddr();
			// 如果客户端地址不为空
			if (StringUtils.hasLength(client)) {
				// 添加客户端地址到字符串构建器中
				sb.append(";client=").append(client);
			}
			// 获取会话对象
			HttpSession session = request.getSession(false);
			// 如果会话对象不为 null
			if (session != null) {
				// 添加会话 ID 到字符串构建器中
				sb.append(";session=").append(session.getId());
			}
			// 获取远程用户
			String user = request.getRemoteUser();
			// 如果远程用户不为空
			if (StringUtils.hasLength(user)) {
				// 添加远程用户到字符串构建器中
				sb.append(";user=").append(user);
			}
		}
		// 返回字符串
		return sb.toString();
	}


	@Override
	public String toString() {
		return "ServletWebRequest: " + getDescription(true);
	}

}
