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

package org.springframework.web.servlet.mvc;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 URL 的虚拟路径转换为视图名称并返回该视图的简单 {@code Controller} 实现。
 *
 * <p>可以选择在 URL 文件名中添加前缀 {@link #setPrefix prefix} 和/或后缀 {@link #setSuffix suffix} 以构建视图名称。
 *
 * <p>下面是一些示例：
 * <ol>
 * <li>{@code "/index" -> "index"}</li>
 * <li>{@code "/index.html" -> "index"}</li>
 * <li>{@code "/index.html"} + prefix {@code "pre_"} and suffix {@code "_suf" -> "pre_index_suf"}</li>
 * <li>{@code "/products/view.html" -> "products/view"}</li>
 * </ol>
 *
 * <p>感谢 David Barri 提出前缀/后缀支持！
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see #setPrefix
 * @see #setSuffix
 */
public class UrlFilenameViewController extends AbstractUrlViewController {

	/**
	 * 前缀
	 */
	private String prefix = "";

	/**
	 * 后缀
	 */
	private String suffix = "";

	/**
	 * 请求 URL 路径字符串到视图名称字符串的映射。
	 */
	private final Map<String, String> viewNameCache = new ConcurrentHashMap<>(256);


	/**
	 * 设置要添加到请求 URL 文件名之前的前缀以构建视图名称。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回要添加到请求 URL 文件名之前的前缀。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置要添加到请求 URL 文件名之后的后缀以构建视图名称。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回要添加到请求 URL 文件名之后的后缀。
	 */
	protected String getSuffix() {
		return this.suffix;
	}


	/**
	 * 根据 URL 文件名返回视图名称，适当时应用前缀/后缀。
	 *
	 * @see #extractViewNameFromUrlPath
	 * @see #setPrefix
	 * @see #setSuffix
	 */
	@Override
	protected String getViewNameForRequest(HttpServletRequest request) {
		// 提取可操作的 URL 路径
		String uri = extractOperableUrl(request);
		// 根据 URL 路径获取视图名称
		return getViewNameForUrlPath(uri);
	}

	/**
	 * 从给定请求中提取适用于视图名称提取的 URL 路径。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 用于视图名称提取的 URL
	 */
	protected String extractOperableUrl(HttpServletRequest request) {
		// 获取处理程序映射属性中的 URL 路径
		String urlPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (!StringUtils.hasText(urlPath)) {
			// 如果 URL 路径为空，则尝试从缓存中获取路径值
			urlPath = ServletRequestPathUtils.getCachedPathValue(request);
		}
		return urlPath;
	}

	/**
	 * 根据 URL 文件名返回视图名称，适当时应用前缀/后缀。
	 *
	 * @param uri 请求 URI；例如 {@code "/index.html"}
	 * @return 提取的 URI 文件名；例如 {@code "index"}
	 * @see #extractViewNameFromUrlPath
	 * @see #postProcessViewName
	 */
	protected String getViewNameForUrlPath(String uri) {
		return this.viewNameCache.computeIfAbsent(uri, u -> postProcessViewName(extractViewNameFromUrlPath(u)));
	}

	/**
	 * 从给定请求 URI 提取 URL 文件名。
	 *
	 * @param uri 请求 URI；例如 {@code "/index.html"}
	 * @return 提取的 URI 文件名；例如 {@code "index"}
	 */
	protected String extractViewNameFromUrlPath(String uri) {
		// 确定开始索引，如果 URI 以斜杠开头，则开始索引为 1，否则为 0
		int start = (uri.charAt(0) == '/' ? 1 : 0);
		// 获取 URI 中最后一个点的索引
		int lastIndex = uri.lastIndexOf('.');
		// 确定结束索引，如果没有找到点，则结束索引为 URI 的长度，否则为最后一个点的索引
		int end = (lastIndex < 0 ? uri.length() : lastIndex);
		// 返回截取的 URI 子字符串，从开始索引（含）到结束索引（不含）
		return uri.substring(start, end);
	}

	/**
	 * 根据 URL 路径中指示的视图名称构建完整视图名称。
	 * <p>默认实现只是应用前缀和后缀。可以重写此方法以进行大写/小写等操作。
	 *
	 * @param viewName 原始视图名称，由 URL 路径指示
	 * @return 要使用的完整视图名称
	 * @see #getPrefix()
	 * @see #getSuffix()
	 */
	protected String postProcessViewName(String viewName) {
		return getPrefix() + viewName + getSuffix();
	}

}
