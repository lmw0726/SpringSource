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

package org.springframework.web.server.adapter;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * 从“Forwarded”和“X-Forwarded-*”头中提取值，以覆盖请求URI（即{@link ServerHttpRequest#getURI()}），使其反映客户端发起的协议和地址。
 *
 * <p>此类的实例通常声明为名为“forwardedHeaderTransformer”的bean，并通过{@link WebHttpHandlerBuilder#applicationContext(ApplicationContext)}检测到，
 * 或者也可以通过{@link WebHttpHandlerBuilder#forwardedHeaderTransformer(ForwardedHeaderTransformer)}直接注册。
 *
 * <p>有关转发头的安全性考虑，因为应用程序无法知道这些头是由代理添加的（如预期的那样）还是由恶意客户端添加的。
 * 这就是为什么应该在信任边界上的代理上配置删除来自外部的不受信任的Forwarded头。
 *
 * <p>您还可以使用{@link #setRemoveOnly removeOnly}配置ForwardedHeaderFilter，以便仅删除而不使用头。
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 5.1
 */
public class ForwardedHeaderTransformer implements Function<ServerHttpRequest, ServerHttpRequest> {
	/**
	 * 转发头名称集合
	 */
	static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
	}

	/**
	 * 是否仅删除
	 */
	private boolean removeOnly;


	/**
	 * 启用“仅删除”模式，在此模式下将仅删除任何“Forwarded”或“X-Forwarded-*”头，且其中的信息将被忽略。
	 *
	 * @param removeOnly 是否丢弃并忽略转发头
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	/**
	 * 是否处于“仅删除”模式。
	 *
	 * @see #setRemoveOnly
	 */
	public boolean isRemoveOnly() {
		return this.removeOnly;
	}


	/**
	 * 应用和删除，或者删除Forwarded类型的头。
	 *
	 * @param request 请求
	 */
	@Override
	public ServerHttpRequest apply(ServerHttpRequest request) {
		if (hasForwardedHeaders(request)) {
			// 如果请求包含转发头部信息
			ServerHttpRequest.Builder builder = request.mutate();
			if (!this.removeOnly) {
				// 如果不仅仅是移除转发头部信息
				URI uri = UriComponentsBuilder.fromHttpRequest(request).build(true).toUri();
				// 设置URI
				builder.uri(uri);
				// 获取转发前缀
				String prefix = getForwardedPrefix(request);
				if (prefix != null) {
					// 添加前缀到路径和上下文路径
					builder.path(prefix + uri.getRawPath());
					builder.contextPath(prefix);
				}
				// 获取远程地址
				InetSocketAddress remoteAddress = request.getRemoteAddress();
				// 解析转发地址，并将其设置为远程地址
				remoteAddress = UriComponentsBuilder.parseForwardedFor(request, remoteAddress);
				if (remoteAddress != null) {
					// 设置远程地址
					builder.remoteAddress(remoteAddress);
				}
			}
			// 移除转发头部信息
			removeForwardedHeaders(builder);
			request = builder.build();
		}
		// 返回请求
		return request;
	}

	/**
	 * 请求是否具有任何Forwarded头。
	 *
	 * @param request 请求
	 */
	protected boolean hasForwardedHeaders(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		// 遍历转发请求头名称集合
		for (String headerName : FORWARDED_HEADER_NAMES) {
			// 检查是否包含转发头部信息
			if (headers.containsKey(headerName)) {
				return true;
			}
		}
		// 如果没有转发头部信息，则返回false
		return false;
	}

	private void removeForwardedHeaders(ServerHttpRequest.Builder builder) {
		builder.headers(map -> FORWARDED_HEADER_NAMES.forEach(map::remove));
	}


	@Nullable
	private static String getForwardedPrefix(ServerHttpRequest request) {
		// 获取请求头
		HttpHeaders headers = request.getHeaders();
		// 获取第一个转发前缀
		String header = headers.getFirst("X-Forwarded-Prefix");
		if (header == null) {
			// 如果没有转发前缀头部信息，则返回null
			return null;
		}
		// 创建前缀构建器
		StringBuilder prefix = new StringBuilder(header.length());
		// 将转发前缀按照 ‘.’ 分割成字符串数组
		String[] rawPrefixes = StringUtils.tokenizeToStringArray(header, ",");
		// 遍历转发前缀数组
		for (String rawPrefix : rawPrefixes) {
			int endIndex = rawPrefix.length();
			// 去除末尾的斜杠
			while (endIndex > 1 && rawPrefix.charAt(endIndex - 1) == '/') {
				endIndex--;
			}
			// 如果含有末尾斜杠，则添加斜杠之前的值；否则添加原始的转发前缀
			prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
		}
		// 返回处理后的前缀字符串
		return prefix.toString();
	}

}
