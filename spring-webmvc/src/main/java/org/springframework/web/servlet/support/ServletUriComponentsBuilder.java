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

package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;

/**
 * UriComponentsBuilder 带有额外的静态工厂方法，根据当前 HttpServletRequest 创建链接。
 *
 * <p><strong>注意：</strong>从 5.1 开始，此类中的方法不会提取指定客户端来源地址的 {@code "Forwarded"} 和 {@code "X-Forwarded-*"} 标头。请使用
 * {@link org.springframework.web.filter.ForwardedHeaderFilter
 * ForwardedHeaderFilter}，或基础服务器中的类似工具来提取和使用这些标头，或将其丢弃。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletUriComponentsBuilder extends UriComponentsBuilder {

	/**
	 * 原始路径
	 */
	@Nullable
	private String originalPath;


	/**
	 * 默认构造函数。受保护以防止直接实例化。
	 *
	 * @see #fromContextPath(HttpServletRequest)
	 * @see #fromServletMapping(HttpServletRequest)
	 * @see #fromRequest(HttpServletRequest)
	 * @see #fromCurrentContextPath()
	 * @see #fromCurrentServletMapping()
	 * @see #fromCurrentRequest()
	 */
	protected ServletUriComponentsBuilder() {
	}

	/**
	 * 创建给定 ServletUriComponentsBuilder 的深拷贝。
	 *
	 * @param other 要复制的其他构建器
	 */
	protected ServletUriComponentsBuilder(ServletUriComponentsBuilder other) {
		super(other);
		this.originalPath = other.originalPath;
	}


	// 基于 HttpServletRequest 的工厂方法

	/**
	 * 从给定 HttpServletRequest 的主机、端口、方案和上下文路径准备一个构建器。
	 */
	public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
		// 从请求中初始化 ServletUriComponentsBuilder
		ServletUriComponentsBuilder builder = initFromRequest(request);
		// 替换请求路径为上下文路径
		builder.replacePath(request.getContextPath());
		// 返回 构建器 实例
		return builder;
	}

	/**
	 * 从给定 HttpServletRequest 的主机、端口、方案、上下文路径和 servlet 映射准备一个构建器。
	 * <p>如果 servlet 是按名称映射的，例如 {@code "/main/*"}，路径将以 "/main" 结尾。
	 * 如果 servlet 是以其他方式映射的，例如 {@code "/"} 或 {@code "*.do"}，则结果将与调用 {@link #fromContextPath(HttpServletRequest)} 的结果相同。
	 */
	public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
		// 从请求的上下文路径创建ServletUriComponentsBuilder
		ServletUriComponentsBuilder builder = fromContextPath(request);

		// 如果请求的路径包含在Servlet映射中
		if (StringUtils.hasText(UrlPathHelper.defaultInstance.getPathWithinServletMapping(request))) {
			// 将Servlet路径设置为 构建器 的路径
			builder.path(request.getServletPath());
		}

		// 返回 构建器 实例
		return builder;
	}

	/**
	 * 从 HttpServletRequest 的主机、端口、方案和路径（不包括查询）准备一个构建器。
	 */
	public static ServletUriComponentsBuilder fromRequestUri(HttpServletRequest request) {
		// 从请求中初始化 ServletUriComponentsBuilder 实例
		ServletUriComponentsBuilder builder = initFromRequest(request);
		// 从请求URI中初始化路径
		builder.initPath(request.getRequestURI());
		// 返回 构建器 实例
		return builder;
	}

	/**
	 * 通过复制 HttpServletRequest 的方案、主机、端口、路径和查询字符串来准备一个构建器。
	 */
	public static ServletUriComponentsBuilder fromRequest(HttpServletRequest request) {
		// 从请求中初始化 ServletUriComponentsBuilder 实例
		ServletUriComponentsBuilder builder = initFromRequest(request);
		// 从请求URI中初始化路径
		builder.initPath(request.getRequestURI());
		// 设置查询参数
		builder.query(request.getQueryString());
		// 返回 构建器 实例
		return builder;
	}

	/**
	 * 使用 HttpServletRequest 的方案、主机和端口（但不是路径和查询）初始化一个构建器。
	 */
	private static ServletUriComponentsBuilder initFromRequest(HttpServletRequest request) {
		// 获取请求的协议（scheme）、主机（host）和端口（port）
		String scheme = request.getScheme();
		String host = request.getServerName();
		int port = request.getServerPort();

		// 创建ServletUriComponentsBuilder对象
		ServletUriComponentsBuilder builder = new ServletUriComponentsBuilder();
		// 设置协议和主机
		builder.scheme(scheme);
		builder.host(host);

		// 如果协议为http且端口不是80，或者协议为https且端口不是443，则设置端口
		if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
			builder.port(port);
		}

		// 返回构建好的builder
		return builder;
	}


	// 依赖 RequestContextHolder 查找请求的替代方法

	/**
	 * 与 {@link #fromContextPath(HttpServletRequest)} 相同，只是通过 {@link RequestContextHolder} 获取请求。
	 */
	public static ServletUriComponentsBuilder fromCurrentContextPath() {
		return fromContextPath(getCurrentRequest());
	}

	/**
	 * 与 {@link #fromServletMapping(HttpServletRequest)} 相同，只是通过 {@link RequestContextHolder} 获取请求。
	 */
	public static ServletUriComponentsBuilder fromCurrentServletMapping() {
		return fromServletMapping(getCurrentRequest());
	}

	/**
	 * 与 {@link #fromRequestUri(HttpServletRequest)} 相同，只是通过 {@link RequestContextHolder} 获取请求。
	 */
	public static ServletUriComponentsBuilder fromCurrentRequestUri() {
		return fromRequestUri(getCurrentRequest());
	}

	/**
	 * 与 {@link #fromRequest(HttpServletRequest)} 相同，只是通过 {@link RequestContextHolder} 获取请求。
	 */
	public static ServletUriComponentsBuilder fromCurrentRequest() {
		return fromRequest(getCurrentRequest());
	}

	/**
	 * 通过 {@link RequestContextHolder} 获取当前请求。
	 */
	protected static HttpServletRequest getCurrentRequest() {
		// 获取请求属性
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		// 根据请求属性获取请求
		return ((ServletRequestAttributes) attrs).getRequest();
	}


	private void initPath(String path) {
		this.originalPath = path;
		replacePath(path);
	}

	/**
	 * 从 {@link HttpServletRequest#getRequestURI() requestURI} 中移除任何路径扩展名。
	 * 必须在调用 {@link #path(String)} 或 {@link #pathSegment(String...)} 之前调用此方法。
	 *
	 * @return 可能用于重新使用的已移除的路径扩展名，或 {@code null}
	 * @since 4.0
	 */
	@Nullable
	public String removePathExtension() {
		// 初始化扩展名为null
		String extension = null;

		// 如果原始路径不为null
		if (this.originalPath != null) {
			// 提取文件扩展名
			extension = UriUtils.extractFileExtension(this.originalPath);
			// 如果扩展名不为空
			if (StringUtils.hasLength(extension)) {
				// 截取路径中的扩展名
				int end = this.originalPath.length() - (extension.length() + 1);
				replacePath(this.originalPath.substring(0, end));
			}
			// 将原始路径设为null，避免重复操作
			this.originalPath = null;
		}

		// 返回提取到的扩展名
		return extension;
	}

	@Override
	public ServletUriComponentsBuilder cloneBuilder() {
		return new ServletUriComponentsBuilder(this);
	}

}
