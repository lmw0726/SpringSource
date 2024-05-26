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

package org.springframework.web.util;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * {@link UriComponents}的层次化URI扩展。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Sam Brannen
 * @see <a href="https://tools.ietf.org/html/rfc3986#section-1.2.3">层次化URI</a>
 * @since 3.1.3
 */
@SuppressWarnings("serial")
final class HierarchicalUriComponents extends UriComponents {
	/**
	 * 路径分隔符
	 */
	private static final char PATH_DELIMITER = '/';

	/**
	 * 路径分隔符字符串
	 */
	private static final String PATH_DELIMITER_STRING = String.valueOf(PATH_DELIMITER);

	/**
	 * 空的查询查询参数
	 */
	private static final MultiValueMap<String, String> EMPTY_QUERY_PARAMS =
			CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());


	/**
	 * 表示一个空路径。
	 */
	static final PathComponent NULL_PATH_COMPONENT = new PathComponent() {
		@Override
		public String getPath() {
			return "";
		}

		@Override
		public List<String> getPathSegments() {
			return Collections.emptyList();
		}

		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			return this;
		}

		@Override
		public void verify() {
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			return this;
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	};

	/**
	 * 用户信息
	 */
	@Nullable
	private final String userInfo;

	/**
	 * 主机地址
	 */
	@Nullable
	private final String host;

	/**
	 * 端口
	 */
	@Nullable
	private final String port;

	/**
	 * 路径组件
	 */
	private final PathComponent path;

	/**
	 * 查询参数
	 */
	private final MultiValueMap<String, String> queryParams;

	/**
	 * 编码状态
	 */
	private final EncodeState encodeState;

	/**
	 * 变量编码器
	 */
	@Nullable
	private UnaryOperator<String> variableEncoder;


	/**
	 * 包内构造函数。所有参数都是可选的，可以为 {@code null}。
	 *
	 * @param scheme   协议
	 * @param userInfo 用户信息
	 * @param host     主机
	 * @param port     端口
	 * @param path     路径
	 * @param query    查询参数
	 * @param fragment 片段
	 * @param encoded  组件是否已编码
	 */
	HierarchicalUriComponents(@Nullable String scheme, @Nullable String fragment, @Nullable String userInfo,
							  @Nullable String host, @Nullable String port, @Nullable PathComponent path,
							  @Nullable MultiValueMap<String, String> query, boolean encoded) {

		super(scheme, fragment);

		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = path != null ? path : NULL_PATH_COMPONENT;
		this.queryParams = query != null ? CollectionUtils.unmodifiableMultiValueMap(query) : EMPTY_QUERY_PARAMS;
		this.encodeState = encoded ? EncodeState.FULLY_ENCODED : EncodeState.RAW;

		// 检查非法字符
		if (encoded) {
			verify();
		}
	}

	private HierarchicalUriComponents(@Nullable String scheme, @Nullable String fragment,
									  @Nullable String userInfo, @Nullable String host, @Nullable String port,
									  PathComponent path, MultiValueMap<String, String> queryParams,
									  EncodeState encodeState, @Nullable UnaryOperator<String> variableEncoder) {

		super(scheme, fragment);

		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.path = path;
		this.queryParams = queryParams;
		this.encodeState = encodeState;
		this.variableEncoder = variableEncoder;
	}


	// 组件 getter 方法

	@Override
	@Nullable
	public String getSchemeSpecificPart() {
		return null;
	}

	@Override
	@Nullable
	public String getUserInfo() {
		return this.userInfo;
	}

	@Override
	@Nullable
	public String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		// 如果端口号为空
		if (this.port == null) {
			// 返回 -1 表示未指定端口
			return -1;
		} else if (this.port.contains("{")) {
			// 如果端口号包含 '{'，则抛出异常
			throw new IllegalStateException(
					"The port contains a URI variable but has not been expanded yet: " + this.port);
		}
		try {
			// 尝试将端口号解析为整数
			return Integer.parseInt(this.port);
		} catch (NumberFormatException ex) {
			// 如果无法解析为整数，则抛出异常，端口号必须是整数
			throw new IllegalStateException("The port must be an integer: " + this.port);
		}
	}

	@Override
	@NonNull
	public String getPath() {
		return this.path.getPath();
	}

	@Override
	public List<String> getPathSegments() {
		return this.path.getPathSegments();
	}

	@Override
	@Nullable
	public String getQuery() {
		// 如果查询参数不为空
		if (!this.queryParams.isEmpty()) {
			// 创建一个字符串构建器来构建查询字符串
			StringBuilder queryBuilder = new StringBuilder();
			// 遍历查询参数的键值对
			this.queryParams.forEach((name, values) -> {
				// 如果值集合为空
				if (CollectionUtils.isEmpty(values)) {
					if (queryBuilder.length() != 0) {
						// 如果构建器长度不为 0，则添加 '&' 字符
						queryBuilder.append('&');
					}
					// 添加参数名
					queryBuilder.append(name);
				} else {
					// 如果值集合不为空，则遍历值
					for (Object value : values) {
						// 如果构建器长度不为 0，则添加 '&' 字符
						if (queryBuilder.length() != 0) {
							queryBuilder.append('&');
						}
						// 添加参数名
						queryBuilder.append(name);
						if (value != null) {
							// 如果值不为空，则添加 '=' 字符和值的字符串表示
							queryBuilder.append('=').append(value.toString());
						}
					}
				}
			});
			// 返回构建的查询字符串
			return queryBuilder.toString();
		} else {
			// 如果查询参数为空，则返回 null
			return null;
		}
	}

	/**
	 * 返回查询参数的映射。如果未设置查询，则返回空。
	 */
	@Override
	public MultiValueMap<String, String> getQueryParams() {
		return this.queryParams;
	}


	// 编码

	/**
	 * 与 {@link #encode()} 相同，但跳过 URI 变量占位符。
	 * 同时 {@link #variableEncoder} 使用给定的字符集初始化，
	 * 以便在扩展 URI 变量时后续使用。
	 */
	HierarchicalUriComponents encodeTemplate(Charset charset) {
		// 如果已经进行了编码，则返回当前实例
		if (this.encodeState.isEncoded()) {
			return this;
		}

		// 记住字符集以便稍后编码 URI 变量..
		this.variableEncoder = value -> encodeUriComponent(value, charset, Type.URI);

		// 创建 URI模板编码器 实例，用于对 URI 进行编码
		UriTemplateEncoder encoder = new UriTemplateEncoder(charset);
		// 对协议部分进行编码
		String schemeTo = (getScheme() != null ? encoder.apply(getScheme(), Type.SCHEME) : null);
		// 对片段部分进行编码
		String fragmentTo = (getFragment() != null ? encoder.apply(getFragment(), Type.FRAGMENT) : null);
		// 对用户信息部分进行编码
		String userInfoTo = (getUserInfo() != null ? encoder.apply(getUserInfo(), Type.USER_INFO) : null);
		// 对主机部分进行编码
		String hostTo = (getHost() != null ? encoder.apply(getHost(), getHostType()) : null);
		// 对路径部分进行编码
		PathComponent pathTo = this.path.encode(encoder);
		// 对查询参数进行编码
		MultiValueMap<String, String> queryParamsTo = encodeQueryParams(encoder);

		// 返回一个新的 层次化URI组件 实例
		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo,
				hostTo, this.port, pathTo, queryParamsTo, EncodeState.TEMPLATE_ENCODED, this.variableEncoder);
	}

	@Override
	public HierarchicalUriComponents encode(Charset charset) {
		// 如果已经进行了编码，则返回当前实例
		if (this.encodeState.isEncoded()) {
			return this;
		}

		// 获取URI方案
		String scheme = getScheme();
		// 获取URI片段
		String fragment = getFragment();
		// 对协议部分进行编码
		String schemeTo = (scheme != null ? encodeUriComponent(scheme, charset, Type.SCHEME) : null);
		// 对片段部分进行编码
		String fragmentTo = (fragment != null ? encodeUriComponent(fragment, charset, Type.FRAGMENT) : null);
		// 对用户信息部分进行编码
		String userInfoTo = (this.userInfo != null ? encodeUriComponent(this.userInfo, charset, Type.USER_INFO) : null);
		// 对主机部分进行编码
		String hostTo = (this.host != null ? encodeUriComponent(this.host, charset, getHostType()) : null);
		// 创建一个编码函数，用于对各个部分进行编码
		BiFunction<String, Type, String> encoder = (s, type) -> encodeUriComponent(s, charset, type);
		// 对路径部分进行编码
		PathComponent pathTo = this.path.encode(encoder);
		// 对查询参数进行编码
		MultiValueMap<String, String> queryParamsTo = encodeQueryParams(encoder);

		// 返回一个新的 层次化URI组件 实例
		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo,
				hostTo, this.port, pathTo, queryParamsTo, EncodeState.FULLY_ENCODED, null);
	}

	private MultiValueMap<String, String> encodeQueryParams(BiFunction<String, Type, String> encoder) {
		// 获取查询参数的数量
		int size = this.queryParams.size();
		// 创建一个与查询参数数量相同大小的 MultiValueMap
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(size);
		// 遍历查询参数的键值对
		this.queryParams.forEach((key, values) -> {
			// 对参数名进行编码
			String name = encoder.apply(key, Type.QUERY_PARAM);
			// 创建一个存储经过编码的参数值的列表
			List<String> encodedValues = new ArrayList<>(values.size());
			// 遍历参数值，对每个值进行编码
			for (String value : values) {
				encodedValues.add(value != null ? encoder.apply(value, Type.QUERY_PARAM) : null);
			}
			// 将编码后的参数名和值列表放入结果 MultiValueMap 中
			result.put(name, encodedValues);
		});
		// 返回一个不可修改的 MultiValueMap，其中的键和值都已经进行了编码
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	/**
	 * 使用给定组件指定的规则和选项对给定源进行编码，生成编码的字符串。
	 *
	 * @param source   源字符串
	 * @param encoding 源字符串的编码
	 * @param type     源的 URI 组件
	 * @return 编码的 URI
	 * @throws IllegalArgumentException 当给定值不是有效的 URI 组件时
	 */
	static String encodeUriComponent(String source, String encoding, Type type) {
		return encodeUriComponent(source, Charset.forName(encoding), type);
	}

	/**
	 * 使用给定组件指定的规则和选项对给定源进行编码，生成编码的字符串。
	 *
	 * @param source  源字符串
	 * @param charset 源字符串的编码
	 * @param type    源的 URI 组件
	 * @return 编码的 URI
	 * @throws IllegalArgumentException 当给定值不是有效的 URI 组件时
	 */
	static String encodeUriComponent(String source, Charset charset, Type type) {
		// 如果源字符串为空，则直接返回源字符串
		if (!StringUtils.hasLength(source)) {
			return source;
		}
		// 检查字符集和类型是否为空
		Assert.notNull(charset, "Charset must not be null");
		Assert.notNull(type, "Type must not be null");

		// 将源字符串转换为字节数组
		byte[] bytes = source.getBytes(charset);
		// 是否允许源字符串中的所有字符
		boolean original = true;
		// 检查源字符串中的每个字节是否都是允许的
		for (byte b : bytes) {
			if (!type.isAllowed(b)) {
				// 如果当前的字节不允许，则将标记设置为false，并跳出循环。
				original = false;
				break;
			}
		}
		// 如果源字符串中的所有字符都是允许的，则直接返回源字符串
		if (original) {
			return source;
		}

		// 创建一个字节数组输出流
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
		// 遍历源字符串的字节，根据类型是否允许来进行编码
		for (byte b : bytes) {
			if (type.isAllowed(b)) {
				// 如果当前的字节是允许的，则写入字节数组输出流中
				baos.write(b);
			} else {
				// 如果字节不允许，则将其转换为百分号编码形式
				baos.write('%');
				// 计算字节的高位和低位，并转换为十六进制字符
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				// 将十六进制字符写入输出流
				baos.write(hex1);
				baos.write(hex2);
			}
		}
		// 将输出流中的内容以指定字符集转换为字符串并返回
		return StreamUtils.copyToString(baos, charset);
	}

	private Type getHostType() {
		return (this.host != null && this.host.startsWith("[") ? Type.HOST_IPV6 : Type.HOST_IPV4);
	}

	// 验证

	/**
	 * 检查任何 URI 组件是否包含任何非法字符。
	 *
	 * @throws IllegalArgumentException 如果任何组件有非法字符
	 */
	private void verify() {
		// 验证协议部分是否符合要求
		verifyUriComponent(getScheme(), Type.SCHEME);
		// 验证用户信息部分是否符合要求
		verifyUriComponent(this.userInfo, Type.USER_INFO);
		// 验证主机部分是否符合要求
		verifyUriComponent(this.host, getHostType());
		// 验证路径部分是否符合要求
		this.path.verify();
		// 遍历查询参数，验证参数名和值是否符合要求
		this.queryParams.forEach((key, values) -> {
			// 验证参数名是否符合要求
			verifyUriComponent(key, Type.QUERY_PARAM);
			for (String value : values) {
				// 验证参数值是否符合要求
				verifyUriComponent(value, Type.QUERY_PARAM);
			}
		});
		// 验证片段部分是否符合要求
		verifyUriComponent(getFragment(), Type.FRAGMENT);
	}

	private static void verifyUriComponent(@Nullable String source, Type type) {
		// 如果源字符串为 null，则直接返回
		if (source == null) {
			return;
		}
		// 获取源字符串的长度
		int length = source.length();
		// 遍历源字符串的每个字符
		for (int i = 0; i < length; i++) {
			// 获取当前字符
			char ch = source.charAt(i);
			// 如果当前字符是 '%'，则需要验证是否为有效的百分号编码
			if (ch == '%') {
				// 检查是否还有足够的字符组成编码
				if ((i + 2) < length) {
					// 获取编码的两个十六进制字符
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					// 将十六进制字符转换为数字
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					// 如果其中一个字符无法转换为数字，则抛出异常
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" +
								source.substring(i) + "\"");
					}
					// 将索引前进两步，跳过已经检查过的字符
					i += 2;
				} else {
					// 如果 '%' 后面没有足够的字符组成编码，则抛出异常
					throw new IllegalArgumentException("Invalid encoded sequence \"" +
							source.substring(i) + "\"");
				}
			} else if (!type.isAllowed(ch)) {
				// 如果当前字符不是 '%'，但也不在允许的字符集中，则抛出异常
				throw new IllegalArgumentException("Invalid character '" + ch + "' for " +
						type.name() + " in \"" + source + "\"");
			}
		}
	}


	// 展开

	@Override
	protected HierarchicalUriComponents expandInternal(UriTemplateVariables uriVariables) {
		Assert.state(!this.encodeState.equals(EncodeState.FULLY_ENCODED),
				"URI components already encoded, and could not possibly contain '{' or '}'.");

		// 基于数组的变量依赖于以下顺序...
		// 扩展URI组件的方案
		String schemeTo = expandUriComponent(getScheme(), uriVariables, this.variableEncoder);
		// 扩展URI组件的用户信息
		String userInfoTo = expandUriComponent(this.userInfo, uriVariables, this.variableEncoder);
		// 扩展URI组件的主机地址
		String hostTo = expandUriComponent(this.host, uriVariables, this.variableEncoder);
		// 扩展URI组件的端口号
		String portTo = expandUriComponent(this.port, uriVariables, this.variableEncoder);
		// 扩展URI组件的路径组件
		PathComponent pathTo = this.path.expand(uriVariables, this.variableEncoder);
		// 展开查询参数
		MultiValueMap<String, String> queryParamsTo = expandQueryParams(uriVariables);
		// 扩展片段
		String fragmentTo = expandUriComponent(getFragment(), uriVariables, this.variableEncoder);

		// 返回新的层次结构URI组件
		return new HierarchicalUriComponents(schemeTo, fragmentTo, userInfoTo,
				hostTo, portTo, pathTo, queryParamsTo, this.encodeState, this.variableEncoder);
	}

	private MultiValueMap<String, String> expandQueryParams(UriTemplateVariables variables) {
		// 获取当前URI的查询参数的数量
		int size = this.queryParams.size();

		// 创建一个具有初始容量为size的LinkedMultiValueMap以存储扩展后的查询参数
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(size);

		// 创建一个包含查询参数的模板变量对象
		UriTemplateVariables queryVariables = new QueryUriTemplateVariables(variables);

		// 遍历当前URI的查询参数，对每个参数进行扩展
		this.queryParams.forEach((key, values) -> {
			// 对查询参数的键进行扩展
			String name = expandUriComponent(key, queryVariables, this.variableEncoder);
			// 创建一个用于存储扩展后的参数值的列表
			List<String> expandedValues = new ArrayList<>(values.size());
			// 遍历当前参数的所有值，并对每个值进行扩展
			for (String value : values) {
				expandedValues.add(expandUriComponent(value, queryVariables, this.variableEncoder));
			}
			// 将扩展后的参数键值对放入结果Map中
			result.put(name, expandedValues);
		});

		// 返回不可修改的扩展后的查询参数Map
		return CollectionUtils.unmodifiableMultiValueMap(result);
	}

	@Override
	public UriComponents normalize() {
		// 获取当前URI的路径并进行规范化处理
		String normalizedPath = StringUtils.cleanPath(getPath());

		// 创建一个完整路径组件，用规范化后的路径作为参数
		FullPathComponent path = new FullPathComponent(normalizedPath);

		// 创建一个新的层次结构URI组件，使用当前URI的各个部分并包括已规范化的路径
		return new HierarchicalUriComponents(getScheme(), getFragment(), this.userInfo, this.host, this.port,
				path, this.queryParams, this.encodeState, this.variableEncoder);
	}


	// 其他功能

	@Override
	public String toUriString() {
		// 创建一个StringBuilder对象来构建URI字符串
		StringBuilder uriBuilder = new StringBuilder();

		// 如果URI有方案，则将其添加到URI字符串中
		if (getScheme() != null) {
			uriBuilder.append(getScheme()).append(':');
		}

		// 如果URI有 用户信息 或 主机地址，则添加'//'到URI字符串中
		if (this.userInfo != null || this.host != null) {
			uriBuilder.append("//");

			// 如果有 用户信息，则添加到URI字符串中
			if (this.userInfo != null) {
				uriBuilder.append(this.userInfo).append('@');
			}

			// 添加 主机地址 到URI字符串中
			if (this.host != null) {
				uriBuilder.append(this.host);
			}

			// 如果有端口号，则添加到URI字符串中
			if (getPort() != -1) {
				uriBuilder.append(':').append(this.port);
			}
		}

		// 获取路径并添加到URI字符串中
		String path = getPath();
		if (StringUtils.hasLength(path)) {
			// 如果URI字符串不为空且路径不是以'/'开头，则添加一个'/'到URI字符串中
			if (uriBuilder.length() != 0 && path.charAt(0) != PATH_DELIMITER) {
				uriBuilder.append(PATH_DELIMITER);
			}
			// 添加路径
			uriBuilder.append(path);
		}

		// 获取查询参数并添加到URI字符串中
		String query = getQuery();
		if (query != null) {
			// 添加 ? 并添加查询参数
			uriBuilder.append('?').append(query);
		}

		// 如果有 片段，则添加到URI字符串中
		if (getFragment() != null) {
			// 添加 # 并添加片段
			uriBuilder.append('#').append(getFragment());
		}

		// 返回构建的URI字符串
		return uriBuilder.toString();
	}

	@Override
	public URI toUri() {
		try {
			// 如果已经编码过，则直接创建一个URI对象
			if (this.encodeState.isEncoded()) {
				return new URI(toUriString());
			} else {
				// 如果未编码，则手动构建URI对象

				// 获取路径
				String path = getPath();

				// 如果路径不为空且不以'/'开头，则在前面添加一个'/'，但仅在路径前有其他内容时才添加
				if (StringUtils.hasLength(path) && path.charAt(0) != PATH_DELIMITER) {
					// 如果在路径分隔符之前存在，则仅在其前面加上前缀
					if (getScheme() != null || getUserInfo() != null || getHost() != null || getPort() != -1) {
						// 如果 方案、用户信息、主机地址、端口号其中都一个部位空，则添加 / 前缀
						path = PATH_DELIMITER + path;
					}
				}

				// 使用给定的各个URI部分构建URI对象
				return new URI(getScheme(), getUserInfo(), getHost(), getPort(), path, getQuery(), getFragment());
			}
		} catch (URISyntaxException ex) {
			// 捕获URISyntaxException异常，并将其包装为IllegalStateException抛出
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
		// 如果存在 方案，则将其设置到 URI组件构建器 中
		if (getScheme() != null) {
			builder.scheme(getScheme());
		}

		// 如果存在 用户信息，则将其设置到 URI组件构建器 中
		if (getUserInfo() != null) {
			builder.userInfo(getUserInfo());
		}
		// 如果存在 主机地址，则将其设置到 URI组件构建器 中
		if (getHost() != null) {
			builder.host(getHost());
		}
		// 避免解析端口，可能有URI变量 ..
		// 如果存在 端口号，则将其设置到 URI组件构建器 中
		if (this.port != null) {
			builder.port(this.port);
		}

		// 将 路径 复制到 URI组件构建器 中
		this.path.copyToUriComponentsBuilder(builder);

		// 如果存在查询参数，则将其设置到 URI组件构建器 r中
		if (!getQueryParams().isEmpty()) {
			builder.queryParams(getQueryParams());
		}

		// 如果存在片段，则将其设置到 URI组件构建器 中
		if (getFragment() != null) {
			builder.fragment(getFragment());
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HierarchicalUriComponents)) {
			return false;
		}
		HierarchicalUriComponents otherComp = (HierarchicalUriComponents) other;
		return (ObjectUtils.nullSafeEquals(getScheme(), otherComp.getScheme()) &&
				ObjectUtils.nullSafeEquals(getUserInfo(), otherComp.getUserInfo()) &&
				ObjectUtils.nullSafeEquals(getHost(), otherComp.getHost()) &&
				getPort() == otherComp.getPort() &&
				this.path.equals(otherComp.path) &&
				this.queryParams.equals(otherComp.queryParams) &&
				ObjectUtils.nullSafeEquals(getFragment(), otherComp.getFragment()));
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(getScheme());
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.userInfo);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.host);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.port);
		result = 31 * result + this.path.hashCode();
		result = 31 * result + this.queryParams.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(getFragment());
		return result;
	}


	// 嵌套类型

	/**
	 * 用于识别每个URI组件允许的字符的枚举。
	 * <p>包含方法指示给定字符在特定URI组件中是否有效。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
	 */
	enum Type {

		SCHEME {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是字母、数字、'+'、'-'或'.'中的一个，则返回true，否则返回false
				return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
			}
		},
		AUTHORITY {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是未保留字符、子分隔符、':'或'@'中的一个，则返回true，否则返回false
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
			}
		},
		USER_INFO {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是未保留字符、子分隔符或':'中的一个，则返回true，否则返回false
				return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
			}
		},
		HOST_IPV4 {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是未保留字符或子分隔符中的一个，则返回true，否则返回false
				return isUnreserved(c) || isSubDelimiter(c);
			}
		},
		HOST_IPV6 {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是未保留字符、子分隔符、'['、']'或':'中的一个，则返回true，否则返回false
				return isUnreserved(c) || isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
			}
		},
		PORT {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是数字，则返回true，否则返回false
				return isDigit(c);
			}
		},
		PATH {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是pchar字符或'/'，则返回true，否则返回false
				return isPchar(c) || '/' == c;
			}
		},
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是pchar字符，则返回true，否则返回false
				return isPchar(c);
			}
		},
		QUERY {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是pchar字符或'/'或者‘?’，则返回true，否则返回false
				return isPchar(c) || '/' == c || '?' == c;
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '&' == c) {
					// 如果字符是'='或'&'，则返回false
					return false;
				} else {
					// 否则，如果字符是pchar字符、'/'、或'?'中的一个，则返回true，否则返回false
					return isPchar(c) || '/' == c || '?' == c;
				}
			}
		},
		FRAGMENT {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是pchar字符、'/'或'?'中的一个，则返回true，否则返回false
				return isPchar(c) || '/' == c || '?' == c;
			}
		},
		URI {
			@Override
			public boolean isAllowed(int c) {
				// 如果字符是未保留字符，则返回true，否则返回false
				return isUnreserved(c);
			}
		};

		/**
		 * 指示给定字符是否允许在此URI组件中。
		 *
		 * @return 如果字符允许，则为{@code true}；否则为{@code false}
		 */
		public abstract boolean isAllowed(int c);

		/**
		 * 指示给定字符是否在{@code ALPHA}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isAlpha(int c) {
			return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z');
		}

		/**
		 * 指示给定字符是否在{@code DIGIT}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isDigit(int c) {
			return (c >= '0' && c <= '9');
		}

		/**
		 * 指示给定字符是否在{@code gen-delims}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isGenericDelimiter(int c) {
			// 如果字符是':'、'/'、'?'、'#'、'['、']'或'@'中的一个，则返回true，否则返回false
			return (':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c);
		}

		/**
		 * 指示给定字符是否在{@code sub-delims}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isSubDelimiter(int c) {
			// 如果字符是'!'、'$'、'&'、'\''、'('、')'、'*'、'+'、','、';'或'='中的一个，则返回true，否则返回false
			return ('!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
					',' == c || ';' == c || '=' == c);
		}

		/**
		 * 指示给定字符是否在{@code reserved}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isReserved(int c) {
			// 如果字符是通用分隔符或子分隔符中的一个，则返回true，否则返回false
			return (isGenericDelimiter(c) || isSubDelimiter(c));
		}

		/**
		 * 指示给定字符是否在{@code unreserved}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isUnreserved(int c) {
			// 如果字符是字母、数字、'-'、'.'、'_'或'~'中的一个，则返回true，否则返回false
			return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
		}

		/**
		 * 指示给定字符是否在{@code pchar}集合中。
		 *
		 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986, 附录A</a>
		 */
		protected boolean isPchar(int c) {
			// 如果字符是未保留字符、子分隔符、':'或'@'中的一个，则返回true，否则返回false
			return (isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c);
		}
	}


	private enum EncodeState {

		/**
		 * 未编码。
		 */
		RAW,

		/**
		 * 首先扩展URI变量，然后对每个URI组件进行编码，只对该URI组件中的非法字符进行引用。
		 */
		FULLY_ENCODED,

		/**
		 * 首先通过引用非法字符仅对URI模板进行编码，然后在扩展时对URI变量进行更严格的编码，同时引用非法字符和具有保留含义的字符。
		 */
		TEMPLATE_ENCODED;


		public boolean isEncoded() {
			return this.equals(FULLY_ENCODED) || this.equals(TEMPLATE_ENCODED);
		}
	}


	/**
	 * {@code BiFunction}实现，用于编码URI模板中的变量和文字部分。
	 */
	private static class UriTemplateEncoder implements BiFunction<String, Type, String> {

		/**
		 * 编码URI时要使用的字符集。
		 */
		private final Charset charset;

		/**
		 * 当前的文字部分。
		 */
		private final StringBuilder currentLiteral = new StringBuilder();

		/**
		 * 当前的变量部分。
		 */
		private final StringBuilder currentVariable = new StringBuilder();

		/**
		 * 输出结果。
		 */
		private final StringBuilder output = new StringBuilder();

		/**
		 * 指示当前变量是否包含名称和正则表达式。
		 */
		private boolean variableWithNameAndRegex;

		/**
		 * 构造一个{@code UriTemplateEncoder}实例。
		 *
		 * @param charset 用于URI编码的字符集
		 */
		public UriTemplateEncoder(Charset charset) {
			this.charset = charset;
		}

		/**
		 * 对URI模板进行编码。
		 *
		 * @param source 源字符串
		 * @param type   字符串类型
		 * @return 编码后的结果
		 */
		@Override
		public String apply(String source, Type type) {
			// 检查源字符串是否为URI变量
			if (isUriVariable(source)) {
				// 如果是，直接返回源字符串
				return source;
			}
			// 如果源字符串中不包含 '{' 字符，表示它是纯文本模板
			if (source.indexOf('{') == -1) {
				// 对纯文本进行 URI 编码并返回
				return encodeUriComponent(source, this.charset, type);
			}
			// 初始化变量级别为0
			int level = 0;
			// 清空当前字面量、当前变量和输出
			clear(this.currentLiteral);
			clear(this.currentVariable);
			clear(this.output);
			// 遍历源字符串中的每个字符
			for (int i = 0; i < source.length(); i++) {
				char c = source.charAt(i);
				// 如果字符为 ':' 且级别为1，则表示变量包含名称和正则表达式
				if (c == ':' && level == 1) {
					this.variableWithNameAndRegex = true;
				}
				if (c == '{') {
					// 如果字符为 '{'，增加级别
					level++;
					if (level == 1) {
						// 如果级别为1，则添加当前字面量
						append(this.currentLiteral, true, type);
					}
				}
				if (c == '}' && level > 0) {
					// 如果字符为 '}'，减少级别
					level--;
					// 添加 ‘}’字符
					this.currentVariable.append('}');
					if (level == 0) {
						// 如果级别为0，则添加当前变量
						// 如果变量需要编码，则对其进行编码
						boolean encode = !isUriVariable(this.currentVariable);
						append(this.currentVariable, encode, type);
					} else if (!this.variableWithNameAndRegex) {
						// 如果变量不包含名称和正则表达式，则添加当前变量
						append(this.currentVariable, true, type);
						// 将级别设置为0
						level = 0;
					}
				} else if (level > 0) {
					// 级别大于0，将当前字符添加进当前变量中
					this.currentVariable.append(c);
				} else {
					// 否则，将当前字符添加进当前字面量中
					this.currentLiteral.append(c);
				}
			}
			// 如果在遍历结束后级别仍大于0，将当前变量添加到当前字面量中
			if (level > 0) {
				this.currentLiteral.append(this.currentVariable);
			}
			// 处理最后的当前字面量并返回结果
			append(this.currentLiteral, true, type);
			return this.output.toString();
		}

		/**
		 * 检查给定的字符串是否是一个可以扩展的URI变量。
		 * 它必须由'{'和'}'括起来，包含非空文本，且不允许嵌套的占位符，除非它是具有正则表达式语法的变量，例如{@code "/{year:\\d{1,4}}"}。
		 *
		 * @param source 要检查的字符串
		 * @return 如果是URI变量，则返回{@code true}，否则返回{@code false}
		 */
		private boolean isUriVariable(CharSequence source) {
			if (source.length() < 2 || source.charAt(0) != '{' || source.charAt(source.length() - 1) != '}') {
				// 如果源字符串长度小于2或者首尾不是 '{' 和 '}'，则不是URI变量
				return false;
			}
			boolean hasText = false;
			// 遍历源字符串中的每个字符（除去首尾的 '{' 和 '}'）
			for (int i = 1; i < source.length() - 1; i++) {
				char c = source.charAt(i);
				// 如果字符为 ':' 且索引大于1，则表示包含变量名称和正则表达式，返回true
				if (c == ':' && i > 1) {
					return true;
				}
				// 如果字符为 '{' 或 '}'，则不是合法的URI变量，返回false
				if (c == '{' || c == '}') {
					return false;
				}
				// 判断字符是否为空格之外的文本
				hasText = (hasText || !Character.isWhitespace(c));
			}
			// 返回是否存在文本内容
			return hasText;
		}

		/**
		 * 向输出结果中追加字符串。
		 *
		 * @param sb     要追加的字符串
		 * @param encode 是否需要进行编码
		 * @param type   字符串类型
		 */
		private void append(StringBuilder sb, boolean encode, Type type) {
			// 将编码后的字符串（如果需要）或者原始字符串追加到输出中
			this.output.append(encode ? encodeUriComponent(sb.toString(), this.charset, type) : sb);
			// 清空StringBuilder
			clear(sb);
			// 重置变量是否包含名称和正则表达式的标志为false
			this.variableWithNameAndRegex = false;
		}

		/**
		 * 清空字符串构建器。
		 *
		 * @param sb 要清空的字符串构建器
		 */
		private void clear(StringBuilder sb) {
			sb.delete(0, sb.length());
		}
	}


	/**
	 * 定义路径（segments）的契约。
	 */
	interface PathComponent extends Serializable {

		/**
		 * 获取路径。
		 *
		 * @return 路径字符串
		 */
		String getPath();

		/**
		 * 获取路径段列表。
		 *
		 * @return 路径段列表
		 */
		List<String> getPathSegments();

		/**
		 * 对路径进行编码。
		 *
		 * @param encoder 编码器
		 * @return 编码后的路径组件
		 */
		PathComponent encode(BiFunction<String, Type, String> encoder);

		/**
		 * 验证路径组件。
		 */
		void verify();

		/**
		 * 扩展路径组件，用URI模板变量替换占位符。
		 *
		 * @param uriVariables URI模板变量
		 * @param encoder      编码器
		 * @return 扩展后的路径组件
		 */
		PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder);

		/**
		 * 将路径组件复制到UriComponentsBuilder。
		 *
		 * @param builder UriComponentsBuilder实例
		 */
		void copyToUriComponentsBuilder(UriComponentsBuilder builder);
	}


	/**
	 * 表示由字符串支持的路径。
	 */
	static final class FullPathComponent implements PathComponent {

		/**
		 * 路径字符串。
		 */
		private final String path;

		/**
		 * 构造一个{@code FullPathComponent}实例。
		 *
		 * @param path 路径字符串
		 */
		public FullPathComponent(@Nullable String path) {
			this.path = (path != null ? path : "");
		}

		/**
		 * 获取路径。
		 *
		 * @return 路径字符串
		 */
		@Override
		public String getPath() {
			return this.path;
		}

		/**
		 * 获取路径段列表。
		 *
		 * @return 路径段列表
		 */
		@Override
		public List<String> getPathSegments() {
			// 使用 路径分隔符 将 路径字符串 分割为字符串数组
			String[] segments = StringUtils.tokenizeToStringArray(getPath(), PATH_DELIMITER_STRING);
			// 将字符串数组转换为不可修改的列表并返回
			return Collections.unmodifiableList(Arrays.asList(segments));
		}

		/**
		 * 对路径进行编码。
		 *
		 * @param encoder 编码器
		 * @return 编码后的路径组件
		 */
		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			// 使用编码器对路径进行编码
			String encodedPath = encoder.apply(getPath(), Type.PATH);
			// 创建并返回一个新的全路径组件实例
			return new FullPathComponent(encodedPath);
		}

		/**
		 * 验证路径组件。
		 */
		@Override
		public void verify() {
			verifyUriComponent(getPath(), Type.PATH);
		}

		/**
		 * 扩展路径组件，用URI模板变量替换占位符。
		 *
		 * @param uriVariables URI模板变量
		 * @param encoder      编码器
		 * @return 扩展后的路径组件
		 */
		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			// 使用URI组件展开函数对路径进行展开
			String expandedPath = expandUriComponent(getPath(), uriVariables, encoder);
			// 创建并返回一个新的全路径组件实例
			return new FullPathComponent(expandedPath);
		}

		/**
		 * 将路径组件复制到UriComponentsBuilder。
		 *
		 * @param builder UriComponentsBuilder实例
		 */
		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			builder.path(getPath());
		}

		/**
		 * 检查对象是否等于该路径组件。
		 *
		 * @param other 要比较的对象
		 * @return 如果对象等于该路径组件，则返回{@code true}，否则返回{@code false}
		 */
		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof FullPathComponent &&
					getPath().equals(((FullPathComponent) other).getPath())));
		}

		/**
		 * 获取路径组件的哈希码。
		 *
		 * @return 哈希码值
		 */
		@Override
		public int hashCode() {
			return getPath().hashCode();
		}
	}


	/**
	 * 表示由字符串列表（即路径片段）支持的路径。
	 */
	static final class PathSegmentComponent implements PathComponent {

		/**
		 * 路径段列表。
		 */
		private final List<String> pathSegments;

		/**
		 * 构造一个{@code PathSegmentComponent}实例。
		 *
		 * @param pathSegments 路径段列表
		 */
		public PathSegmentComponent(List<String> pathSegments) {
			Assert.notNull(pathSegments, "List must not be null");
			this.pathSegments = Collections.unmodifiableList(new ArrayList<>(pathSegments));
		}

		/**
		 * 获取路径。
		 *
		 * @return 路径字符串
		 */
		@Override
		public String getPath() {
			// 设置路径分隔符为路径分隔符字符串
			String delimiter = PATH_DELIMITER_STRING;
			// 创建一个StringJoiner用于构建路径
			StringJoiner pathBuilder = new StringJoiner(delimiter, delimiter, "");
			// 遍历路径段列表并添加到路径构建器中
			for (String pathSegment : this.pathSegments) {
				pathBuilder.add(pathSegment);
			}
			// 返回构建好的路径字符串
			return pathBuilder.toString();
		}

		/**
		 * 获取路径段列表。
		 *
		 * @return 路径段列表
		 */
		@Override
		public List<String> getPathSegments() {
			return this.pathSegments;
		}

		/**
		 * 对路径进行编码。
		 *
		 * @param encoder 编码器
		 * @return 编码后的路径组件
		 */
		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			// 获取路径段列表
			List<String> pathSegments = getPathSegments();
			// 创建一个用于存储编码后路径段的列表，大小与原列表相同
			List<String> encodedPathSegments = new ArrayList<>(pathSegments.size());
			// 遍历路径段列表，并对每个路径段进行编码
			for (String pathSegment : pathSegments) {
				// 使用编码器对路径段进行编码
				String encodedPathSegment = encoder.apply(pathSegment, Type.PATH_SEGMENT);
				// 将编码后的路径段添加到编码后路径段列表中
				encodedPathSegments.add(encodedPathSegment);
			}
			// 创建并返回一个新的 路径片段组件 实例
			return new PathSegmentComponent(encodedPathSegments);
		}

		/**
		 * 验证路径组件。
		 */
		@Override
		public void verify() {
			for (String pathSegment : getPathSegments()) {
				// 对每一个片段进行验证
				verifyUriComponent(pathSegment, Type.PATH_SEGMENT);
			}
		}

		/**
		 * 扩展路径组件，用URI模板变量替换占位符。
		 *
		 * @param uriVariables URI模板变量
		 * @param encoder      编码器
		 * @return 扩展后的路径组件
		 */
		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			// 获取路径段列表
			List<String> pathSegments = getPathSegments();
			// 创建一个用于存储展开后路径段的列表，大小与原列表相同
			List<String> expandedPathSegments = new ArrayList<>(pathSegments.size());
			// 遍历路径段列表，并对每个路径段进行展开
			for (String pathSegment : pathSegments) {
				// 使用URI组件展开函数对路径段进行展开
				String expandedPathSegment = expandUriComponent(pathSegment, uriVariables, encoder);
				// 将展开后的路径段添加到展开后路径段列表中
				expandedPathSegments.add(expandedPathSegment);
			}
			// 创建并返回一个新的 路径片段组件 实例
			return new PathSegmentComponent(expandedPathSegments);
		}

		/**
		 * 将路径组件复制到UriComponentsBuilder。
		 *
		 * @param builder UriComponentsBuilder实例
		 */
		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			builder.pathSegment(StringUtils.toStringArray(getPathSegments()));
		}

		/**
		 * 检查对象是否等于该路径组件。
		 *
		 * @param other 要比较的对象
		 * @return 如果对象等于该路径组件，则返回{@code true}，否则返回{@code false}
		 */
		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof PathSegmentComponent &&
					getPathSegments().equals(((PathSegmentComponent) other).getPathSegments())));
		}

		/**
		 * 获取路径组件的哈希码。
		 *
		 * @return 哈希码值
		 */
		@Override
		public int hashCode() {
			return getPathSegments().hashCode();
		}
	}


	/**
	 * 表示 路径组件 的集合。
	 */
	static final class PathComponentComposite implements PathComponent {
		/**
		 * 路径组件列表
		 */
		private final List<PathComponent> pathComponents;

		/**
		 * 构造一个 PathComponentComposite 实例。
		 *
		 * @param pathComponents 要组合的 PathComponent 列表，不能为空
		 */
		public PathComponentComposite(List<PathComponent> pathComponents) {
			Assert.notNull(pathComponents, "PathComponent List must not be null");
			this.pathComponents = pathComponents;
		}

		@Override
		public String getPath() {
			// 创建一个StringBuilder用于构建路径
			StringBuilder pathBuilder = new StringBuilder();
			// 遍历路径组件列表，并将每个路径组件的路径添加到路径构建器中
			for (PathComponent pathComponent : this.pathComponents) {
				pathBuilder.append(pathComponent.getPath());
			}
			// 返回构建好的路径字符串
			return pathBuilder.toString();
		}

		@Override
		public List<String> getPathSegments() {
			// 创建一个用于存储路径段的列表
			List<String> result = new ArrayList<>();
			// 遍历路径组件列表，并将每个路径组件的路径段添加到结果列表中
			for (PathComponent pathComponent : this.pathComponents) {
				result.addAll(pathComponent.getPathSegments());
			}
			// 返回结果列表
			return result;
		}

		@Override
		public PathComponent encode(BiFunction<String, Type, String> encoder) {
			// 创建一个用于存储编码后路径组件的列表，大小与原列表相同
			List<PathComponent> encodedComponents = new ArrayList<>(this.pathComponents.size());
			// 遍历路径组件列表，并对每个路径组件进行编码
			for (PathComponent pathComponent : this.pathComponents) {
				// 使用编码器对路径组件进行编码，并将编码后的路径组件添加到编码后路径组件列表中
				encodedComponents.add(pathComponent.encode(encoder));
			}
			// 创建并返回一个新的 路径复合组件 实例，
			return new PathComponentComposite(encodedComponents);
		}

		@Override
		public void verify() {
			// 遍历路径组件列表，并对每个路径组件进行验证
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.verify();
			}
		}

		@Override
		public PathComponent expand(UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
			// 创建一个用于存储展开后路径组件的列表，大小与原列表相同
			List<PathComponent> expandedComponents = new ArrayList<>(this.pathComponents.size());
			// 遍历路径组件列表，并对每个路径组件进行展开
			for (PathComponent pathComponent : this.pathComponents) {
				// 使用URI变量和编码器对路径组件进行展开，并将展开后的路径组件添加到展开后路径组件列表中
				expandedComponents.add(pathComponent.expand(uriVariables, encoder));
			}
			// 创建并返回一个新的 路径复合组件 实例
			return new PathComponentComposite(expandedComponents);
		}

		@Override
		public void copyToUriComponentsBuilder(UriComponentsBuilder builder) {
			// 遍历路径组件列表，并将每个路径组件复制到URI组件构建器中
			for (PathComponent pathComponent : this.pathComponents) {
				pathComponent.copyToUriComponentsBuilder(builder);
			}
		}
	}


	private static class QueryUriTemplateVariables implements UriTemplateVariables {
		/**
		 * URI模板变量
		 */
		private final UriTemplateVariables delegate;

		public QueryUriTemplateVariables(UriTemplateVariables delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object getValue(@Nullable String name) {
			// 获取指定名称的值
			Object value = this.delegate.getValue(name);
			// 如果值为数组，将其转换为逗号分隔的字符串
			if (ObjectUtils.isArray(value)) {
				value = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(value));
			}
			// 返回处理后的值
			return value;
		}
	}

}
