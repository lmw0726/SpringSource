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
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表示可以通过{@link #expand(Map)}、{@link #expand(Object[])}扩展URI变量或通过{@link #match(String)}匹配URL的URI模板。
 * 此类设计为线程安全且可重用，并允许任意数量的扩展或匹配调用。
 *
 * <p><strong>注意：</strong>此类在内部使用{@link UriComponentsBuilder}来扩展URI模板，仅是已准备好的URI模板的快捷方式。
 * 对于更动态的准备和额外的灵活性，例如URI编码，请考虑使用{@code UriComponentsBuilder}或更高级别的{@link DefaultUriBuilderFactory}，
 * 后者在{@code UriComponentsBuilder}之上添加了几种编码模式。
 * 有关更多详细信息，请参阅
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-uri-building">参考文档</a>。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 */
@SuppressWarnings("serial")
public class UriTemplate implements Serializable {
	/**
	 * URI模板
	 */
	private final String uriTemplate;

	/**
	 * URI组件
	 */
	private final UriComponents uriComponents;

	/**
	 * 变量名称列表
	 */
	private final List<String> variableNames;

	/**
	 * 模式匹配器
	 */
	private final Pattern matchPattern;


	/**
	 * 使用给定的URI字符串构造一个新的{@code UriTemplate}。
	 *
	 * @param uriTemplate URI模板字符串
	 */
	public UriTemplate(String uriTemplate) {
		Assert.hasText(uriTemplate, "'uriTemplate' must not be null");
		this.uriTemplate = uriTemplate;
		this.uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).build();

		TemplateInfo info = TemplateInfo.parse(uriTemplate);
		this.variableNames = Collections.unmodifiableList(info.getVariableNames());
		this.matchPattern = info.getMatchPattern();
	}


	/**
	 * 返回模板中变量的名称列表，按顺序排列。
	 *
	 * @return 模板变量名称
	 */
	public List<String> getVariableNames() {
		return this.variableNames;
	}

	/**
	 * 给定变量的映射，将此模板扩展为URI。映射键表示变量名称，映射值表示变量值。变量的顺序不重要。
	 * <p>例如：
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("https://example.com/hotels/{hotel}/bookings/{booking}");
	 * Map&lt;String, String&gt; uriVariables = new HashMap&lt;String, String&gt;();
	 * uriVariables.put("booking", "42");
	 * uriVariables.put("hotel", "Rest &amp; Relax");
	 * System.out.println(template.expand(uriVariables));
	 * </pre>
	 * 将会打印： <blockquote>{@code https://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
	 *
	 * @param uriVariables URI变量的映射
	 * @return 扩展后的URI
	 * @throws IllegalArgumentException 如果{@code uriVariables}为{@code null}；
	 *                                  或者如果它不包含所有变量名称的值
	 */
	public URI expand(Map<String, ?> uriVariables) {
		// 使用给定的变量扩展URI组件
		UriComponents expandedComponents = this.uriComponents.expand(uriVariables);
		// 对扩展后的URI组件进行编码
		UriComponents encodedComponents = expandedComponents.encode();
		// 将编码后的URI组件转换为URI并返回
		return encodedComponents.toUri();
	}

	/**
	 * 给定一个变量数组，将此模板扩展为完整的URI。数组表示变量值。变量的顺序很重要。
	 * <p>例如：
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("https://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.expand("Rest &amp; Relax", 42));
	 * </pre>
	 * 将会打印：<blockquote>{@code https://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
	 *
	 * @param uriVariableValues URI变量的数组
	 * @return 扩展后的URI
	 * @throws IllegalArgumentException 如果{@code uriVariables}为{@code null}
	 *                                  或者如果它不包含足够的变量
	 */
	public URI expand(Object... uriVariableValues) {
		// 使用给定的变量值扩展URI组件
		UriComponents expandedComponents = this.uriComponents.expand(uriVariableValues);
		// 对扩展后的URI组件进行编码
		UriComponents encodedComponents = expandedComponents.encode();
		// 将编码后的URI组件转换为URI并返回
		return encodedComponents.toUri();
	}

	/**
	 * 指示给定的URI是否与此模板匹配。
	 *
	 * @param uri 要匹配的URI
	 * @return 如果匹配则为{@code true}；否则为{@code false}
	 */
	public boolean matches(@Nullable String uri) {
		// 如果URI为空，则返回false
		if (uri == null) {
			return false;
		}
		// 使用匹配模式创建Matcher对象
		Matcher matcher = this.matchPattern.matcher(uri);
		// 返回是否匹配成功
		return matcher.matches();
	}

	/**
	 * 将给定的URI与变量值映射匹配。返回的映射中的键是变量名称，值是在给定URI中出现的变量值。
	 * 例如：
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("https://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.match("https://example.com/hotels/1/bookings/42"));
	 * </pre>
	 * 将会打印： <blockquote>{@code {hotel=1, booking=42}}</blockquote>
	 *
	 * @param uri 要匹配的URI
	 * @return 变量值的映射
	 */
	public Map<String, String> match(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		// 创建一个LinkedHashMap来存储变量名和对应的值
		Map<String, String> result = CollectionUtils.newLinkedHashMap(this.variableNames.size());
		// 使用匹配模式创建Matcher对象
		Matcher matcher = this.matchPattern.matcher(uri);
		// 如果找到匹配的部分
		if (matcher.find()) {
			// 遍历匹配的组
			for (int i = 1; i <= matcher.groupCount(); i++) {
				// 获取变量名
				String name = this.variableNames.get(i - 1);
				// 获取匹配的值
				String value = matcher.group(i);
				// 将变量名和值放入result中
				result.put(name, value);
			}
		}
		// 返回存储了URI变量名和对应值的map
		return result;
	}

	@Override
	public String toString() {
		return this.uriTemplate;
	}


	/**
	 * 辅助类，用于提取变量名称和用于匹配实际URL的正则表达式。
	 */
	private static final class TemplateInfo {
		/**
		 * 变量列表
		 */
		private final List<String> variableNames;

		/**
		 * 匹配模式
		 */
		private final Pattern pattern;

		private TemplateInfo(List<String> vars, Pattern pattern) {
			this.variableNames = vars;
			this.pattern = pattern;
		}

		public List<String> getVariableNames() {
			return this.variableNames;
		}

		public Pattern getMatchPattern() {
			return this.pattern;
		}

		public static TemplateInfo parse(String uriTemplate) {
			int level = 0;
			// 存储URI模板中的变量名
			List<String> variableNames = new ArrayList<>();
			// 存储用于构建正则表达式模式的字符串
			StringBuilder pattern = new StringBuilder();
			// 用于构建变量名的临时字符串
			StringBuilder builder = new StringBuilder();
			// 遍历URI模板的每个字符
			for (int i = 0; i < uriTemplate.length(); i++) {
				char c = uriTemplate.charAt(i);
				// 如果遇到'{'，表示URI变量的开始
				if (c == '{') {
					level++;
					if (level == 1) {
						// URI变量的开始
						pattern.append(quote(builder));
						builder = new StringBuilder();
						continue;
					}
				} else if (c == '}') {
					level--;
					if (level == 0) {
						// URI变量的结束
						String variable = builder.toString();
						int idx = variable.indexOf(':');
						if (idx == -1) {
							pattern.append("([^/]*)");
							variableNames.add(variable);
						} else {
							if (idx + 1 == variable.length()) {
								throw new IllegalArgumentException(
										"No custom regular expression specified after ':' in \"" + variable + "\"");
							}
							// 获取自定义正则表达式
							String regex = variable.substring(idx + 1);
							pattern.append('(');
							pattern.append(regex);
							pattern.append(')');
							// 将变量名添加到列表中
							variableNames.add(variable.substring(0, idx));
						}
						builder = new StringBuilder();
						continue;
					}
				}
				// 将字符添加到builder中
				builder.append(c);
			}
			// 处理剩余的builder内容
			if (builder.length() > 0) {
				pattern.append(quote(builder));
			}
			// 返回包含变量名和模式的TemplateInfo对象
			return new TemplateInfo(variableNames, Pattern.compile(pattern.toString()));
		}

		private static String quote(StringBuilder builder) {
			return (builder.length() > 0 ? Pattern.quote(builder.toString()) : "");
		}
	}

}
