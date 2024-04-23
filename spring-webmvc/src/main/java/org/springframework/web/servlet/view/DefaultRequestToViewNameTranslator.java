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

package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * {@link RequestToViewNameTranslator} 实现，简单地将传入请求的 URI 转换为视图名称。
 *
 * <p>可以在 {@link org.springframework.web.servlet.DispatcherServlet} 上下文中显式定义为 {@code viewNameTranslator} bean。
 * 否则，将使用普通的默认实例。
 *
 * <p>默认转换简单地删除 URI 的前导和尾随斜杠以及文件扩展名，并将结果作为视图名称返回，添加了配置的 {@link #setPrefix prefix} 和 {@link #setSuffix suffix}。
 *
 * <p>可以使用 {@link #setStripLeadingSlash stripLeadingSlash} 和 {@link #setStripExtension stripExtension} 属性分别禁用前导斜杠和文件扩展名的删除。
 *
 * <p>以下是一些请求到视图名称转换的示例。
 * <ul>
 * <li>{@code http://localhost:8080/gamecast/display.html} &raquo; {@code display}</li>
 * <li>{@code http://localhost:8080/gamecast/displayShoppingCart.html} &raquo; {@code displayShoppingCart}</li>
 * <li>{@code http://localhost:8080/gamecast/admin/index.html} &raquo; {@code admin/index}</li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.servlet.RequestToViewNameTranslator
 * @see org.springframework.web.servlet.ViewResolver
 */
public class DefaultRequestToViewNameTranslator implements RequestToViewNameTranslator {

	/**
	 * 斜杠
	 */
	private static final String SLASH = "/";

	/**
	 * 前缀
	 */
	private String prefix = "";

	/**
	 * 后缀
	 */
	private String suffix = "";

	/**
	 * 分割字符
	 */
	private String separator = SLASH;

	/**
	 * 是否删除 URI 中的前导斜杠
	 */
	private boolean stripLeadingSlash = true;

	/**
	 * 是否删除 URI 中的尾随斜杠。
	 */
	private boolean stripTrailingSlash = true;

	/**
	 * 是否从 URI 中删除文件扩展名。
	 */
	private boolean stripExtension = true;


	/**
	 * 设置要添加到生成的视图名称前面的前缀。
	 * @param prefix 要添加到生成的视图名称前面的前缀
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 设置要追加到生成的视图名称后面的后缀。
	 * @param suffix 要追加到生成的视图名称后面的后缀
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 设置作为视图名称中的分隔符的值。默认行为仅将 '{@code /}' 保留为分隔符。
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * 设置在生成视图名称时是否应删除 URI 中的前导斜杠。默认为 "true"。
	 */
	public void setStripLeadingSlash(boolean stripLeadingSlash) {
		this.stripLeadingSlash = stripLeadingSlash;
	}

	/**
	 * 设置在生成视图名称时是否应删除 URI 中的尾随斜杠。默认为 "true"。
	 */
	public void setStripTrailingSlash(boolean stripTrailingSlash) {
		this.stripTrailingSlash = stripTrailingSlash;
	}

	/**
	 * 设置在生成视图名称时是否应从 URI 中删除文件扩展名。默认为 "true"。
	 */
	public void setStripExtension(boolean stripExtension) {
		this.stripExtension = stripExtension;
	}

	/**
	 * 将 ";"（分号）内容从请求 URI 中删除的设置。
	 * @deprecated 自 5.3 开始，路径在外部解析，并使用 {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)} 获取
	 */
	@Deprecated
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
	}

	/**
	 * 设置用于查找路径解析的 {@link org.springframework.web.util.UrlPathHelper}。
	 * <p>使用此方法来使用自定义子类覆盖默认的 UrlPathHelper，或者在多个 Web 组件之间共享通用的 UrlPathHelper 设置。
	 * @deprecated 自 5.3 开始，路径在外部解析，并使用 {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)} 获取
	 */
	@Deprecated
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
	}


	/**
	 * 根据配置的参数将传入 {@link HttpServletRequest} 的请求 URI 转换为视图名称。
	 * @throws IllegalArgumentException 如果未解析和缓存请求属性中的解析的 RequestPath 或 String lookupPath，则抛出异常
	 * @see ServletRequestPathUtils#getCachedPath(ServletRequest)
	 * @see #transformPath
	 */
	@Override
	public String getViewName(HttpServletRequest request) {
		// 获取缓存的路径
		String path = ServletRequestPathUtils.getCachedPathValue(request);
		return (this.prefix + transformPath(path) + this.suffix);
	}

	/**
	 * 转换请求 URI（在 webapp 上下文中）去掉斜杠和扩展名，并根据需要替换分隔符。
	 * @param lookupPath 当前请求的查找路径，由 UrlPathHelper 确定
	 * @return 转换后的路径，去掉了斜杠和扩展名（如果需要）
	 */
	@Nullable
	protected String transformPath(String lookupPath) {
		// 将 lookupPath 赋值给 path
		String path = lookupPath;
		// 如果 stripLeadingSlash 为 true 并且 path 以斜杠开头
		if (this.stripLeadingSlash && path.startsWith(SLASH)) {
			// 去掉开头的斜杠
			path = path.substring(1);
		}
		// 如果 stripTrailingSlash 为 true 并且 path 以斜杠结尾
		if (this.stripTrailingSlash && path.endsWith(SLASH)) {
			// 去掉结尾的斜杠
			path = path.substring(0, path.length() - 1);
		}
		// 如果 stripExtension 为 true
		if (this.stripExtension) {
			// 去掉文件扩展名
			path = StringUtils.stripFilenameExtension(path);
		}
		// 如果 separator 不是斜杠
		if (!SLASH.equals(this.separator)) {
			// 替换斜杠为指定的分隔符
			path = StringUtils.replace(path, SLASH, this.separator);
		}
		// 返回处理后的路径
		return path;
	}

}
