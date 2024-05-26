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

package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表示不可变的URI组件集合，将组件类型映射到字符串值。包含所有组件的便捷获取器。
 * 与{@link java.net.URI}类似，但具有更强大的编码选项和对URI模板变量的支持。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see UriComponentsBuilder
 * @since 3.1
 */
@SuppressWarnings("serial")
public abstract class UriComponents implements Serializable {

	/**
	 * 捕获URI模板变量名称的正则表达式。
	 */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

	/**
	 * URI方案
	 */
	@Nullable
	private final String scheme;

	/**
	 * URI片段
	 */
	@Nullable
	private final String fragment;


	protected UriComponents(@Nullable String scheme, @Nullable String fragment) {
		this.scheme = scheme;
		this.fragment = fragment;
	}


	// 组件获取器

	/**
	 * 返回方案。可以为{@code null}。
	 */
	@Nullable
	public final String getScheme() {
		return this.scheme;
	}

	/**
	 * 返回片段。可以为{@code null}。
	 */
	@Nullable
	public final String getFragment() {
		return this.fragment;
	}

	/**
	 * 返回方案特定部分。可以为{@code null}。
	 */
	@Nullable
	public abstract String getSchemeSpecificPart();

	/**
	 * 返回用户信息。可以为{@code null}。
	 */
	@Nullable
	public abstract String getUserInfo();

	/**
	 * 返回主机。可以为{@code null}。
	 */
	@Nullable
	public abstract String getHost();

	/**
	 * 返回端口。如果未设置端口，则为{@code -1}。
	 */
	public abstract int getPort();

	/**
	 * 返回路径。可以为{@code null}。
	 */
	@Nullable
	public abstract String getPath();

	/**
	 * 返回路径段的列表。如果未设置路径，则为空。
	 */
	public abstract List<String> getPathSegments();

	/**
	 * 返回查询。可以为{@code null}。
	 */
	@Nullable
	public abstract String getQuery();

	/**
	 * 返回查询参数的映射。如果未设置查询，则为空。
	 */
	public abstract MultiValueMap<String, String> getQueryParams();


	/**
	 * 在扩展URI变量后调用此方法以对结果的URI组件值进行编码。
	 * <p>与{@link UriComponentsBuilder#encode()}相比，此方法仅替换非ASCII和非法（在给定URI组件类型中）的字符，
	 * 但不替换具有保留含义的字符。对于大多数情况，{@link UriComponentsBuilder#encode()}更可能给出预期的结果。
	 *
	 * @see UriComponentsBuilder#encode()
	 */
	public final UriComponents encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * 使用不同于“UTF-8”的字符集的{@link #encode()}的变体。
	 *
	 * @param charset 用于编码的字符集
	 * @see UriComponentsBuilder#encode(Charset)
	 */
	public abstract UriComponents encode(Charset charset);

	/**
	 * 使用给定映射中的值替换所有URI模板变量。
	 * <p>给定的映射键表示变量名称；相应的值表示变量值。变量的顺序不重要。
	 *
	 * @param uriVariables URI变量的映射
	 * @return 扩展后的URI组件
	 */
	public final UriComponents expand(Map<String, ?> uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(new MapTemplateVariables(uriVariables));
	}

	/**
	 * 使用给定数组中的值替换所有URI模板变量。
	 * <p>给定的数组表示变量值。变量的顺序很重要。
	 *
	 * @param uriVariableValues URI变量值
	 * @return 扩展后的URI组件
	 */
	public final UriComponents expand(Object... uriVariableValues) {
		Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");
		return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
	}

	/**
	 * 使用给定的{@link UriTemplateVariables}中的值替换所有URI模板变量。
	 *
	 * @param uriVariables URI模板值
	 * @return 扩展后的URI组件
	 */
	public final UriComponents expand(UriTemplateVariables uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(uriVariables);
	}

	/**
	 * 使用给定的{@link UriTemplateVariables}中的值替换所有URI模板变量。
	 *
	 * @param uriVariables URI模板值
	 * @return 扩展后的URI组件
	 */
	abstract UriComponents expandInternal(UriTemplateVariables uriVariables);

	/**
	 * 规范化路径，删除类似“path/..”的序列。注意，规范化应用于整个路径，而不是单个路径段。
	 *
	 * @see org.springframework.util.StringUtils#cleanPath(String)
	 */
	public abstract UriComponents normalize();

	/**
	 * 连接所有URI组件以返回完整的URI字符串。
	 * <p>此方法相当于将当前URI组件值简单字符串连接起来，因此结果可能包含非法URI字符，
	 * 例如如果未展开URI变量或者未通过{@link UriComponentsBuilder#encode()}或{@link #encode()}进行编码。
	 */
	public abstract String toUriString();

	/**
	 * 使用此实例创建{@link URI}如下：
	 * <p>如果当前实例已{@link #encode()编码}，则通过{@link #toUriString()}形成完整的URI字符串，
	 * 然后将其传递给单参数{@link URI}构造函数，该构造函数保留百分比编码。
	 * <p>如果尚未编码，则将单个URI组件值传递给多参数{@link URI}构造函数，该构造函数引用不能出现在各自URI组件中的非法字符。
	 */
	public abstract URI toUri();

	/**
	 * 对{@link #toUriString()}进行简单传递。
	 */
	@Override
	public final String toString() {
		return toUriString();
	}

	/**
	 * 将所有组件设置到给定的UriComponentsBuilder中。
	 *
	 * @since 4.2
	 */
	protected abstract void copyToUriComponentsBuilder(UriComponentsBuilder builder);


	// 静态扩展助手

	@Nullable
	static String expandUriComponent(@Nullable String source, UriTemplateVariables uriVariables) {
		return expandUriComponent(source, uriVariables, null);
	}

	@Nullable
	static String expandUriComponent(@Nullable String source, UriTemplateVariables uriVariables,
									 @Nullable UnaryOperator<String> encoder) {

		// 如果源字符串为空，则返回null
		if (source == null) {
			return null;
		}
		// 如果源字符串中不包含'{'，则直接返回源字符串
		if (source.indexOf('{') == -1) {
			return source;
		}
		// 如果源字符串中包含':'，则对源字符串进行清理
		if (source.indexOf(':') != -1) {
			source = sanitizeSource(source);
		}
		// 提取 URI模板变量名称
		Matcher matcher = NAMES_PATTERN.matcher(source);
		// 用于构建替换后的字符串
		StringBuffer sb = new StringBuffer();
		// 循环匹配
		while (matcher.find()) {
			// 获取匹配的字符串
			String match = matcher.group(1);
			// 获取变量名
			String varName = getVariableName(match);
			// 获取变量值
			Object varValue = uriVariables.getValue(varName);
			// 如果变量值为 UriTemplateVariables 类名，则跳过
			if (UriTemplateVariables.SKIP_VALUE.equals(varValue)) {
				continue;
			}
			// 将变量值转换为字符串
			String formatted = getVariableValueAsString(varValue);
			// 使用编码器对格式化后的字符串进行编码，如果编码器为null，则使用Matcher.quoteReplacement进行转义
			formatted = encoder != null ? encoder.apply(formatted) : Matcher.quoteReplacement(formatted);
			// 将替换后的字符串追加到sb中
			matcher.appendReplacement(sb, formatted);
		}
		// 将匹配的剩余部分追加到sb中
		matcher.appendTail(sb);
		// 返回替换后的字符串
		return sb.toString();
	}

	/**
	 * 删除嵌套的“{}”，例如带有正则表达式的URI变量。
	 */
	private static String sanitizeSource(String source) {
		int level = 0;
		// 记录最后一个有效字符的索引
		int lastCharIndex = 0;
		// 创建一个字符数组，用于存储处理后的字符串
		char[] chars = new char[source.length()];
		// 遍历源字符串中的每个字符
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			// 如果是'{'，增加level
			if (c == '{') {
				level++;
			}
			// 如果是'}'，减少level
			if (c == '}') {
				level--;
			}
			// 如果level大于1或者(level等于1且当前字符是'}')，则跳过当前字符
			if (level > 1 || (level == 1 && c == '}')) {
				continue;
			}
			// 将当前字符添加到字符数组中，并更新lastCharIndex
			chars[lastCharIndex++] = c;
		}
		// 返回处理后的字符串，即字符数组中从0到lastCharIndex之间的字符组成的字符串
		return new String(chars, 0, lastCharIndex);
	}

	private static String getVariableName(String match) {
		// 查找冒号的位置
		int colonIdx = match.indexOf(':');
		// 如果找到冒号，则返回冒号之前的子字符串，否则返回整个match字符串
		return (colonIdx != -1 ? match.substring(0, colonIdx) : match);
	}

	private static String getVariableValueAsString(@Nullable Object variableValue) {
		return (variableValue != null ? variableValue.toString() : "");
	}


	/**
	 * 定义URI模板变量的合同。
	 *
	 * @see HierarchicalUriComponents#expand
	 */
	public interface UriTemplateVariables {

		/**
		 * 表示URI变量名应该被忽略并保留原样的值。这对于部分展开某些但不是所有URI变量很有用。
		 */
		Object SKIP_VALUE = UriTemplateVariables.class;

		/**
		 * 获取给定URI变量名称的值。
		 * 如果值为{@code null}，则扩展为空字符串。
		 * 如果值为{@link #SKIP_VALUE}，则不会展开URI变量。
		 *
		 * @param name 变量名称
		 * @return 变量值，可能为{@code null}或{@link #SKIP_VALUE}
		 */
		@Nullable
		Object getValue(@Nullable String name);
	}


	/**
	 * 由映射支持的URI模板变量。
	 */
	private static class MapTemplateVariables implements UriTemplateVariables {
		/**
		 * URI变量映射
		 */
		private final Map<String, ?> uriVariables;

		public MapTemplateVariables(Map<String, ?> uriVariables) {
			this.uriVariables = uriVariables;
		}

		@Override
		@Nullable
		public Object getValue(@Nullable String name) {
			if (!this.uriVariables.containsKey(name)) {
				throw new IllegalArgumentException("Map has no value for '" + name + "'");
			}
			return this.uriVariables.get(name);
		}
	}


	/**
	 * 由可变参数数组支持的URI模板变量。
	 */
	private static class VarArgsTemplateVariables implements UriTemplateVariables {
		/**
		 * 值迭代器
		 */
		private final Iterator<Object> valueIterator;

		public VarArgsTemplateVariables(Object... uriVariableValues) {
			this.valueIterator = Arrays.asList(uriVariableValues).iterator();
		}

		@Override
		@Nullable
		public Object getValue(@Nullable String name) {
			if (!this.valueIterator.hasNext()) {
				throw new IllegalArgumentException("Not enough variable values available to expand '" + name + "'");
			}
			return this.valueIterator.next();
		}
	}

}
