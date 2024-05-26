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

package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 基于RFC 3986的URI编码和解码的实用方法。
 *
 * <p>有两种类型的编码方法：
 * <ul>
 * <li>{@code "encodeXyz"} -- 这些方法通过百分号编码非法字符来编码特定的URI组件（例如路径、查询），
 * 这包括非US-ASCII字符，以及其他在给定URI组件类型中非法的字符，如RFC 3986中定义的。
 * 此方法对编码的影响类似于使用{@link URI}的多参数构造函数。
 * <li>{@code "encode"} 和 {@code "encodeUriVariables"} -- 这些方法可用于通过百分号编码所有字符来
 * 编码URI变量值，这些字符在URI中任何位置都是非法的，或者具有任何保留的意义。
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 * @since 3.0
 */
public abstract class UriUtils {

	/**
	 * 使用给定的编码对给定的URI方案进行编码。
	 *
	 * @param scheme   要编码的方案
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的方案
	 */
	public static String encodeScheme(String scheme, String encoding) {
		return encode(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定的编码对给定的URI方案进行编码。
	 *
	 * @param scheme  要编码的方案
	 * @param charset 要编码到的字符编码
	 * @return 编码后的方案
	 * @since 5.0
	 */
	public static String encodeScheme(String scheme, Charset charset) {
		return encode(scheme, charset, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定的编码对给定的URI权限进行编码。
	 *
	 * @param authority 要编码的权限
	 * @param encoding  要编码到的字符编码
	 * @return 编码后的权限
	 */
	public static String encodeAuthority(String authority, String encoding) {
		return encode(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定的编码对给定的URI权限进行编码。
	 *
	 * @param authority 要编码的权限
	 * @param charset   要编码到的字符编码
	 * @return 编码后的权限
	 * @since 5.0
	 */
	public static String encodeAuthority(String authority, Charset charset) {
		return encode(authority, charset, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定的编码对给定的URI用户信息进行编码。
	 *
	 * @param userInfo 要编码的用户信息
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的用户信息
	 */
	public static String encodeUserInfo(String userInfo, String encoding) {
		return encode(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定的编码对给定的URI用户信息进行编码。
	 *
	 * @param userInfo 要编码的用户信息
	 * @param charset  要编码到的字符编码
	 * @return 编码后的用户信息
	 * @since 5.0
	 */
	public static String encodeUserInfo(String userInfo, Charset charset) {
		return encode(userInfo, charset, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定的编码对给定的URI主机进行编码。
	 *
	 * @param host     要编码的主机
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的主机
	 */
	public static String encodeHost(String host, String encoding) {
		return encode(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定的编码对给定的URI主机进行编码。
	 *
	 * @param host    要编码的主机
	 * @param charset 要编码到的字符编码
	 * @return 编码后的主机
	 * @since 5.0
	 */
	public static String encodeHost(String host, Charset charset) {
		return encode(host, charset, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定的编码对给定的URI端口进行编码。
	 *
	 * @param port     要编码的端口
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的端口
	 */
	public static String encodePort(String port, String encoding) {
		return encode(port, encoding, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定的编码对给定的URI端口进行编码。
	 *
	 * @param port    要编码的端口
	 * @param charset 要编码到的字符编码
	 * @return 编码后的端口
	 * @since 5.0
	 */
	public static String encodePort(String port, Charset charset) {
		return encode(port, charset, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定的编码对给定的URI路径进行编码。
	 *
	 * @param path     要编码的路径
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的路径
	 */
	public static String encodePath(String path, String encoding) {
		return encode(path, encoding, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定的编码对给定的URI路径进行编码。
	 *
	 * @param path    要编码的路径
	 * @param charset 要编码到的字符编码
	 * @return 编码后的路径
	 * @since 5.0
	 */
	public static String encodePath(String path, Charset charset) {
		return encode(path, charset, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定的编码对给定的URI路径段进行编码。
	 *
	 * @param segment  要编码的路径段
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的路径段
	 */
	public static String encodePathSegment(String segment, String encoding) {
		return encode(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定的编码对给定的URI路径段进行编码。
	 *
	 * @param segment 要编码的路径段
	 * @param charset 要编码到的字符编码
	 * @return 编码后的路径段
	 * @since 5.0
	 */
	public static String encodePathSegment(String segment, Charset charset) {
		return encode(segment, charset, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定的编码对给定的URI查询进行编码。
	 *
	 * @param query    要编码的查询
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的查询
	 */
	public static String encodeQuery(String query, String encoding) {
		return encode(query, encoding, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定的编码对给定的URI查询进行编码。
	 *
	 * @param query   要编码的查询
	 * @param charset 要编码到的字符编码
	 * @return 编码后的查询
	 * @since 5.0
	 */
	public static String encodeQuery(String query, Charset charset) {
		return encode(query, charset, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定的编码对给定的URI查询参数进行编码。
	 *
	 * @param queryParam 要编码的查询参数
	 * @param encoding   要编码到的字符编码
	 * @return 编码后的查询参数
	 */
	public static String encodeQueryParam(String queryParam, String encoding) {
		return encode(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * 使用给定的编码对给定的URI查询参数进行编码。
	 *
	 * @param queryParam 要编码的查询参数
	 * @param charset    要编码到的字符编码
	 * @return 编码后的查询参数
	 * @since 5.0
	 */
	public static String encodeQueryParam(String queryParam, Charset charset) {
		return encode(queryParam, charset, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * 使用UTF-8对给定的{@code MultiValueMap}中的查询参数进行编码。
	 * <p>当从已编码的模板构建URI时，可以与{@link UriComponentsBuilder#queryParams(MultiValueMap)}一起使用。
	 * <pre class="code">{@code
	 * MultiValueMap<String, String> params = new LinkedMultiValueMap<>(2);
	 * // 添加到参数
	 *
	 * ServletUriComponentsBuilder.fromCurrentRequest()
	 *         .queryParams(UriUtils.encodeQueryParams(params))
	 *         .build(true)
	 *         .toUriString();
	 * }</pre>
	 *
	 * @param params 要编码的参数
	 * @return 具有已编码名称和值的新的{@code MultiValueMap}
	 * @since 5.2.3
	 */
	public static MultiValueMap<String, String> encodeQueryParams(MultiValueMap<String, String> params) {
		// 设置字符集为 UTF-8
		Charset charset = StandardCharsets.UTF_8;
		// 创建一个空的 MultiValueMap 以存储编码后的参数
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(params.size());
		// 遍历参数集合，对每个参数进行编码并添加到结果中
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			for (String value : entry.getValue()) {
				// 编码参数的键和值，并添加到结果中
				result.add(encodeQueryParam(entry.getKey(), charset), encodeQueryParam(value, charset));
			}
		}
		// 返回编码后的参数结果
		return result;
	}

	/**
	 * 使用给定的编码对给定的URI片段进行编码。
	 *
	 * @param fragment 要编码的片段
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的片段
	 */
	public static String encodeFragment(String fragment, String encoding) {
		return encode(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
	}

	/**
	 * 使用给定的编码对给定的URI片段进行编码。
	 *
	 * @param fragment 要编码的片段
	 * @param charset  要编码到的字符编码
	 * @return 编码后的片段
	 * @since 5.0
	 */
	public static String encodeFragment(String fragment, Charset charset) {
		return encode(fragment, charset, HierarchicalUriComponents.Type.FRAGMENT);
	}


	/**
	 * 使用字符串字符集的变体对要编码的字符串进行编码。
	 *
	 * @param source   要编码的字符串
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的字符串
	 */
	public static String encode(String source, String encoding) {
		return encode(source, encoding, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * 将URI中任意位置的所有非法字符或具有任何保留含义的字符进行编码，如<a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>中所定义。
	 * 这对于确保给定的字符串将被保留为原样，并且不会对URI的结构或含义产生任何影响非常有用。
	 *
	 * @param source  要编码的字符串
	 * @param charset 要编码到的字符编码
	 * @return 编码后的字符串
	 * @since 5.0
	 */
	public static String encode(String source, Charset charset) {
		return encode(source, charset, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * 将 {@link #encode(String, Charset)} 应用于所有给定的URI变量值的便利方法。
	 *
	 * @param uriVariables 要编码的URI变量值
	 * @return 编码后的字符串
	 * @since 5.0
	 */
	public static Map<String, String> encodeUriVariables(Map<String, ?> uriVariables) {
		// 创建一个新的 LinkedHashMap 以存储编码后的 URI 变量
		Map<String, String> result = CollectionUtils.newLinkedHashMap(uriVariables.size());
		// 遍历 URI 变量集合，对每个键值对进行编码并添加到结果中
		uriVariables.forEach((key, value) -> {
			// 将值转换为字符串，如果值为 null，则设置为空字符串
			String stringValue = (value != null ? value.toString() : "");
			// 编码值，并将键值对添加到结果中
			result.put(key, encode(stringValue, StandardCharsets.UTF_8));
		});
		// 返回编码后的 URI 变量结果
		return result;
	}

	/**
	 * 将 {@link #encode(String, Charset)} 应用于所有给定的URI变量值的便利方法。
	 *
	 * @param uriVariables 要编码的URI变量值
	 * @return 编码后的字符串
	 * @since 5.0
	 */
	public static Object[] encodeUriVariables(Object... uriVariables) {
		return Arrays.stream(uriVariables)
				.map(value -> {
					// 将值转换为字符串，如果值为 null，则设置为空字符串
					String stringValue = (value != null ? value.toString() : "");
					// 编码值
					return encode(stringValue, StandardCharsets.UTF_8);
				})
				.toArray();
	}

	private static String encode(String scheme, String encoding, HierarchicalUriComponents.Type type) {
		return HierarchicalUriComponents.encodeUriComponent(scheme, encoding, type);
	}

	private static String encode(String scheme, Charset charset, HierarchicalUriComponents.Type type) {
		return HierarchicalUriComponents.encodeUriComponent(scheme, charset, type);
	}


	/**
	 * 解码给定的已编码URI组件。
	 * <p>有关解码规则，请参阅 {@link StringUtils#uriDecode(String, Charset)}。
	 *
	 * @param source   要解码的已编码字符串
	 * @param encoding 要使用的字符编码
	 * @return 解码后的值
	 * @throws IllegalArgumentException 如果给定的源包含无效的编码序列
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) {
		return StringUtils.uriDecode(source, Charset.forName(encoding));
	}

	/**
	 * 解码给定的已编码URI组件。
	 * <p>有关解码规则，请参阅 {@link StringUtils#uriDecode(String, Charset)}。
	 *
	 * @param source  要解码的已编码字符串
	 * @param charset 要使用的字符编码
	 * @return 解码后的值
	 * @throws IllegalArgumentException 如果给定的源包含无效的编码序列
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 * @since 5.0
	 */
	public static String decode(String source, Charset charset) {
		return StringUtils.uriDecode(source, charset);
	}

	/**
	 * 从给定的URI路径中提取文件扩展名。
	 *
	 * @param path URI路径（例如 "/products/index.html"）
	 * @return 提取的文件扩展名（例如 "html"）
	 * @since 4.3.2
	 */
	@Nullable
	public static String extractFileExtension(String path) {
		// 获取查询字符串的起始位置
		int end = path.indexOf('?');
		// 获取片段标识的位置
		int fragmentIndex = path.indexOf('#');
		if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
			// 如果存在片段标识，并且查询字符串的起始位置不存在，或者片段标识在查询字符串之前
			// 将结束位置设置为片段标识的位置
			end = fragmentIndex;
		}
		if (end == -1) {
			// 如果不存在查询字符串，则将结束位置设置为路径字符串的长度
			end = path.length();
		}
		// 获取文件名的起始位置，从路径字符串中最后一个斜杠后一位开始
		int begin = path.lastIndexOf('/', end) + 1;
		// 获取参数字符串的起始位置
		int paramIndex = path.indexOf(';', begin);
		// 如果存在参数字符串，并且参数字符串在结束位置之前，则将结束位置设置为参数字符串的位置
		end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
		// 获取文件扩展名的起始位置
		int extIndex = path.lastIndexOf('.', end);
		if (extIndex != -1 && extIndex >= begin) {
			// 如果存在文件扩展名，并且文件扩展名的位置在起始位置之后，则返回文件扩展名
			return path.substring(extIndex + 1, end);
		}
		// 如果不存在文件扩展名，则返回空值
		return null;
	}

}
