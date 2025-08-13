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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实现Ant风格路径模式的{@link PathMatcher}。
 *
 * <p>部分映射代码来自<a href="https://ant.apache.org">Apache Ant</a>。
 *
 * <p>映射规则如下：<br>
 * <ul>
 * <li>{@code ?} 匹配单个字符</li>
 * <li>{@code *} 匹配零个或多个字符</li>
 * <li>{@code **} 匹配路径中的零个或多个<em>目录</em></li>
 * <li>{@code {spring:[a-z]+}} 将正则表达式{@code [a-z]+}作为名为"spring"的路径变量匹配</li>
 * </ul>
 *
 * <h3>示例</h3>
 * <ul>
 * <li>{@code com/t?st.jsp} &mdash; 匹配{@code com/test.jsp}，也匹配
 * {@code com/tast.jsp}或{@code com/txst.jsp}</li>
 * <li>{@code com/*.jsp} &mdash; 匹配{@code com}目录下所有{@code .jsp}文件</li>
 * <li><code>com/&#42;&#42;/test.jsp</code> &mdash; 匹配{@code com}路径下所有
 * {@code test.jsp}文件</li>
 * <li><code>org/springframework/&#42;&#42;/*.jsp</code> &mdash; 匹配
 * {@code org/springframework}路径下所有{@code .jsp}文件</li>
 * <li><code>org/&#42;&#42;/servlet/bla.jsp</code> &mdash; 匹配
 * {@code org/springframework/servlet/bla.jsp}，也匹配
 * {@code org/springframework/testing/servlet/bla.jsp}和{@code org/servlet/bla.jsp}</li>
 * <li>{@code com/{filename:\\w+}.jsp} 将匹配{@code com/test.jsp}并将值{@code test}
 * 赋给变量{@code filename}</li>
 * </ul>
 *
 * <p><strong>注意：</strong>模式和路径必须同为绝对路径或相对路径才能匹配。因此建议
 * 用户在使用时对模式进行清理，根据需要添加"/"前缀。
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Vladislav Kisel
 * @since 16.07.2003
 */
public class AntPathMatcher implements PathMatcher {

	/**
	 * 默认路径分隔符："/"。
	 */
	public static final String DEFAULT_PATH_SEPARATOR = "/";

	private static final int CACHE_TURNOFF_THRESHOLD = 65536;

	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");

	private static final char[] WILDCARD_CHARS = {'*', '?', '{'};


	private String pathSeparator;

	private PathSeparatorPatternCache pathSeparatorPatternCache;

	private boolean caseSensitive = true;

	private boolean trimTokens = false;

	@Nullable
	private volatile Boolean cachePatterns;

	private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);

	final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<>(256);


	/**
	 * 使用{@link #DEFAULT_PATH_SEPARATOR}创建新实例。
	 */
	public AntPathMatcher() {
		this.pathSeparator = DEFAULT_PATH_SEPARATOR;
		this.pathSeparatorPatternCache = new PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR);
	}

	/**
	 * 使用自定义路径分隔符的便捷构造方法。
	 *
	 * @param pathSeparator 要使用的路径分隔符，不能为{@code null}
	 * @since 4.1
	 */
	public AntPathMatcher(String pathSeparator) {
		Assert.notNull(pathSeparator, "'pathSeparator' is required");
		this.pathSeparator = pathSeparator;
		this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
	}


	/**
	 * 设置用于模式解析的路径分隔符。
	 * <p>默认为"/"，与Ant相同。
	 */
	public void setPathSeparator(@Nullable String pathSeparator) {
		this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
		this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
	}

	/**
	 * 指定是否执行区分大小写的模式匹配。
	 * <p>默认为{@code true}。设置为{@code false}可进行不区分大小写的匹配。
	 *
	 * @since 4.2
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * 指定是否对标记化的路径和模式进行修剪。
	 * <p>默认为{@code false}。
	 */
	public void setTrimTokens(boolean trimTokens) {
		this.trimTokens = trimTokens;
	}

	/**
	 * 指定是否缓存传入此匹配器{@link #match}方法的模式的解析元数据。
	 * 值为{@code true}激活无限模式缓存；值为{@code false}则完全关闭模式缓存。
	 * <p>默认开启缓存，但当运行时遇到太多要缓存的模式（阈值为65536）时，
	 * 会自动关闭缓存，假设模式会任意变化且重复出现的可能性很小。
	 *
	 * @see #getStringMatcher(String)
	 * @since 4.0.1
	 */
	public void setCachePatterns(boolean cachePatterns) {
		this.cachePatterns = cachePatterns;
	}

	private void deactivatePatternCache() {
		this.cachePatterns = false;
		this.tokenizedPatternCache.clear();
		this.stringMatcherCache.clear();
	}


	@Override
	public boolean isPattern(@Nullable String path) {
		if (path == null) {
			return false;
		}
		boolean uriVar = false;
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '*' || c == '?') {
				return true;
			}
			if (c == '{') {
				uriVar = true;
				continue;
			}
			if (c == '}' && uriVar) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean match(String pattern, String path) {
		return doMatch(pattern, path, true, null);
	}

	@Override
	public boolean matchStart(String pattern, String path) {
		return doMatch(pattern, path, false, null);
	}

	/**
	 * 实际匹配给定的{@code path}与{@code pattern}。
	 *
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径
	 * @param fullMatch 是否需要完全匹配（否则匹配到给定基础路径即可）
	 * @param uriTemplateVariables URI模板变量映射（可为{@code null}）
	 * @return 如果{@code path}匹配则返回{@code true}，否则返回{@code false}
	 */
	protected boolean doMatch(String pattern, @Nullable String path, boolean fullMatch,
							  @Nullable Map<String, String> uriTemplateVariables) {

		if (path == null || path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
			return false;
		}

		String[] pattDirs = tokenizePattern(pattern);
		if (fullMatch && this.caseSensitive && !isPotentialMatch(path, pattDirs)) {
			return false;
		}

		String[] pathDirs = tokenizePath(path);
		int pattIdxStart = 0;
		int pattIdxEnd = pattDirs.length - 1;
		int pathIdxStart = 0;
		int pathIdxEnd = pathDirs.length - 1;

		// 匹配第一个"**"之前的所有元素
		while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			String pattDir = pattDirs[pattIdxStart];
			if ("**".equals(pattDir)) {
				break;
			}
			if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
				return false;
			}
			pattIdxStart++;
			pathIdxStart++;
		}

		if (pathIdxStart > pathIdxEnd) {
			// Path已耗尽，仅当pattern的其余部分为 * 或 ** 时才匹配
			if (pattIdxStart > pattIdxEnd) {
				return (pattern.endsWith(this.pathSeparator) == path.endsWith(this.pathSeparator));
			}
			if (!fullMatch) {
				return true;
			}
			if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
				return true;
			}
			for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
				if (!pattDirs[i].equals("**")) {
					return false;
				}
			}
			return true;
		} else if (pattIdxStart > pattIdxEnd) {
			// 字符串没有耗尽，但模式是。失败。
			return false;
		} else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
			// 由于模式中的 “**” 部分，路径开始肯定匹配。
			return true;
		}

		// 直到最后一个 '**'
		while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			String pattDir = pattDirs[pattIdxEnd];
			if (pattDir.equals("**")) {
				break;
			}
			if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
				return false;
			}
			pattIdxEnd--;
			pathIdxEnd--;
		}
		if (pathIdxStart > pathIdxEnd) {
			// 字符串耗尽
			for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
				if (!pattDirs[i].equals("**")) {
					return false;
				}
			}
			return true;
		}

		while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			int patIdxTmp = -1;
			for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
				if (pattDirs[i].equals("**")) {
					patIdxTmp = i;
					break;
				}
			}
			if (patIdxTmp == pattIdxStart + 1) {
				// '**/**' 情况，所以跳过一个
				pattIdxStart++;
				continue;
			}
			// 在strIdxStart和strIdxEnd之间找到padIdxStart和padIdxTmp之间的模式
			int patLength = (patIdxTmp - pattIdxStart - 1);
			int strLength = (pathIdxEnd - pathIdxStart + 1);
			int foundIdx = -1;

			strLoop:
			for (int i = 0; i <= strLength - patLength; i++) {
				for (int j = 0; j < patLength; j++) {
					String subPat = pattDirs[pattIdxStart + j + 1];
					String subStr = pathDirs[pathIdxStart + i + j];
					if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
						continue strLoop;
					}
				}
				foundIdx = pathIdxStart + i;
				break;
			}

			if (foundIdx == -1) {
				return false;
			}

			pattIdxStart = patIdxTmp;
			pathIdxStart = foundIdx + patLength;
		}

		for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
			if (!pattDirs[i].equals("**")) {
				return false;
			}
		}

		return true;
	}

	private boolean isPotentialMatch(String path, String[] pattDirs) {
		if (!this.trimTokens) {
			int pos = 0;
			for (String pattDir : pattDirs) {
				int skipped = skipSeparator(path, pos, this.pathSeparator);
				pos += skipped;
				skipped = skipSegment(path, pos, pattDir);
				if (skipped < pattDir.length()) {
					return (skipped > 0 || (pattDir.length() > 0 && isWildcardChar(pattDir.charAt(0))));
				}
				pos += skipped;
			}
		}
		return true;
	}

	private int skipSegment(String path, int pos, String prefix) {
		int skipped = 0;
		for (int i = 0; i < prefix.length(); i++) {
			char c = prefix.charAt(i);
			if (isWildcardChar(c)) {
				return skipped;
			}
			int currPos = pos + skipped;
			if (currPos >= path.length()) {
				return 0;
			}
			if (c == path.charAt(currPos)) {
				skipped++;
			}
		}
		return skipped;
	}

	private int skipSeparator(String path, int pos, String separator) {
		int skipped = 0;
		while (path.startsWith(separator, pos + skipped)) {
			skipped += separator.length();
		}
		return skipped;
	}

	private boolean isWildcardChar(char c) {
		for (char candidate : WILDCARD_CHARS) {
			if (c == candidate) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据此匹配器的设置，将给定的路径模式标记为多个部分。
	 * <p>基于{@link #setCachePatterns}执行缓存，实际的标记算法委托给
	 * {@link #tokenizePath(String)}实现。
	 *
	 * @param pattern 要标记的模式
	 * @return 标记后的模式部分
	 */
	protected String[] tokenizePattern(String pattern) {
		String[] tokenized = null;
		Boolean cachePatterns = this.cachePatterns;
		if (cachePatterns == null || cachePatterns.booleanValue()) {
			tokenized = this.tokenizedPatternCache.get(pattern);
		}
		if (tokenized == null) {
			tokenized = tokenizePath(pattern);
			if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
				// 尝试适应运行时遇到的情况：
				// 显然这里有太多不同的模式...
				// 因此关闭缓存，因为模式不太可能重复出现
				deactivatePatternCache();
				return tokenized;
			}
			if (cachePatterns == null || cachePatterns.booleanValue()) {
				this.tokenizedPatternCache.put(pattern, tokenized);
			}
		}
		return tokenized;
	}

	/**
	 * 根据此匹配器的设置，将给定路径标记为多个部分。
	 *
	 * @param path 要标记的路径
	 * @return 标记后的路径部分
	 */
	protected String[] tokenizePath(String path) {
		return StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
	}

	/**
	 * 测试字符串是否与模式匹配。
	 *
	 * @param pattern 要匹配的模式（不为{@code null}）
	 * @param str 必须与模式匹配的字符串（不为{@code null}）
	 * @param uriTemplateVariables URI模板变量映射（可为{@code null}）
	 * @return 如果字符串与模式匹配则返回{@code true}，否则返回{@code false}
	 */
	private boolean matchStrings(String pattern, String str,
								 @Nullable Map<String, String> uriTemplateVariables) {

		return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
	}

	/**
	 * 构建或获取给定模式的{@link AntPathStringMatcher}。
	 * <p>默认实现检查此AntPathMatcher的内部缓存（参见{@link #setCachePatterns}），
	 * 如果未找到缓存副本，则创建新的AntPathStringMatcher实例。
	 * <p>当运行时遇到太多要缓存的模式（阈值为65536）时，
	 * 它会关闭默认缓存，假设模式会任意变化，重复遇到相同模式的机会很小。
	 * <p>可以重写此方法以实现自定义缓存策略。
	 *
	 * @param pattern 要匹配的模式（不为{@code null}）
	 * @return 对应的AntPathStringMatcher（不为{@code null}）
	 * @see #setCachePatterns
	 */
	protected AntPathStringMatcher getStringMatcher(String pattern) {
		AntPathStringMatcher matcher = null;
		Boolean cachePatterns = this.cachePatterns;
		if (cachePatterns == null || cachePatterns.booleanValue()) {
			matcher = this.stringMatcherCache.get(pattern);
		}
		if (matcher == null) {
			matcher = new AntPathStringMatcher(pattern, this.caseSensitive);
			if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
				// 尝试适应我们遇到的运行时情况：
				// 显然这里有太多不同的模式...
				// 因此我们关闭缓存，因为模式不太可能重复出现。
				deactivatePatternCache();
				return matcher;
			}
			if (cachePatterns == null || cachePatterns.booleanValue()) {
				this.stringMatcherCache.put(pattern, matcher);
			}
		}
		return matcher;
	}

	/**
	 * 给定模式和完整路径，确定模式映射的部分。<p>例如：<ul>
	 * <li>'{@code /docs/cvs/commit.html}'和'{@code /docs/cvs/commit.html} → ''</li>
	 * <li>'{@code /docs/*}'和'{@code /docs/cvs/commit} → '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/cvs/*.html}'和'{@code /docs/cvs/commit.html} → '{@code commit.html}'</li>
	 * <li>'{@code /docs/**}'和'{@code /docs/cvs/commit} → '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/**\/*.html}'和'{@code /docs/cvs/commit.html} → '{@code cvs/commit.html}'</li>
	 * <li>'{@code /*.html}'和'{@code /docs/cvs/commit.html} → '{@code docs/cvs/commit.html}'</li>
	 * <li>'{@code *.html}'和'{@code /docs/cvs/commit.html} → '{@code /docs/cvs/commit.html}'</li>
	 * <li>'{@code *}'和'{@code /docs/cvs/commit.html} → '{@code /docs/cvs/commit.html}'</li></ul>
	 * <p>假设{@link #match}对于'{@code pattern}'和'{@code path}'返回{@code true}，
	 * 但<strong>不</strong>强制执行此操作。
	 */
	@Override
	public String extractPathWithinPattern(String pattern, String path) {
		String[] patternParts = StringUtils.tokenizeToStringArray(pattern, this.pathSeparator, this.trimTokens, true);
		String[] pathParts = StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
		StringBuilder builder = new StringBuilder();
		boolean pathStarted = false;

		for (int segment = 0; segment < patternParts.length; segment++) {
			String patternPart = patternParts[segment];
			if (patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
				for (; segment < pathParts.length; segment++) {
					if (pathStarted || (segment == 0 && !pattern.startsWith(this.pathSeparator))) {
						builder.append(this.pathSeparator);
					}
					builder.append(pathParts[segment]);
					pathStarted = true;
				}
			}
		}

		return builder.toString();
	}

	@Override
	public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
		Map<String, String> variables = new LinkedHashMap<>();
		boolean result = doMatch(pattern, path, true, variables);
		if (!result) {
			throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
		}
		return variables;
	}

	/**
	 * 将两个模式组合成一个新模式。
	 * <p>此实现简单地连接两个模式，除非第一个模式包含文件扩展名匹配（如{@code *.html}）。
	 * 在这种情况下，第二个模式将被合并到第一个模式中。否则将抛出{@code IllegalArgumentException}。
	 *
	 * <h3>示例</h3>
	 * <table border="1">
	 * <tr><th>模式1</th><th>模式2</th><th>结果</th></tr>
	 * <tr><td>{@code null}</td><td>{@code null}</td><td>&nbsp;</td></tr>
	 * <tr><td>/hotels</td><td>{@code null}</td><td>/hotels</td></tr>
	 * <tr><td>{@code null}</td><td>/hotels</td><td>/hotels</td></tr>
	 * <tr><td>/hotels</td><td>/bookings</td><td>/hotels/bookings</td></tr>
	 * <tr><td>/hotels</td><td>bookings</td><td>/hotels/bookings</td></tr>
	 * <tr><td>/hotels/*</td><td>/bookings</td><td>/hotels/bookings</td></tr>
	 * <tr><td>/hotels/&#42;&#42;</td><td>/bookings</td><td>/hotels/&#42;&#42;/bookings</td></tr>
	 * <tr><td>/hotels</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
	 * <tr><td>/hotels/*</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
	 * <tr><td>/hotels/&#42;&#42;</td><td>{hotel}</td><td>/hotels/&#42;&#42;/{hotel}</td></tr>
	 * <tr><td>/*.html</td><td>/hotels.html</td><td>/hotels.html</td></tr>
	 * <tr><td>/*.html</td><td>/hotels</td><td>/hotels.html</td></tr>
	 * <tr><td>/*.html</td><td>/*.txt</td><td>{@code IllegalArgumentException}</td></tr>
	 * </table>
	 *
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * @return 两个模式的组合结果
	 * @throws IllegalArgumentException 如果两个模式无法组合
	 */
	@Override
	public String combine(String pattern1, String pattern2) {
		if (!StringUtils.hasText(pattern1) && !StringUtils.hasText(pattern2)) {
			return "";
		}
		if (!StringUtils.hasText(pattern1)) {
			return pattern2;
		}
		if (!StringUtils.hasText(pattern2)) {
			return pattern1;
		}

		boolean pattern1ContainsUriVar = (pattern1.indexOf('{') != -1);
		if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
			// /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
			// 但 /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
			return pattern2;
		}

		// /hotels/* + /booking -> /hotels/booking
		// /hotels/* + booking -> /hotels/booking
		if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
			return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
		}

		// /hotels/** + /booking -> /hotels/**/booking
		// /hotels/** + booking -> /hotels/**/booking
		if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
			return concat(pattern1, pattern2);
		}

		int starDotPos1 = pattern1.indexOf("*.");
		if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
			// 简单连接两个模式
			return concat(pattern1, pattern2);
		}

		String ext1 = pattern1.substring(starDotPos1 + 1);
		int dotPos2 = pattern2.indexOf('.');
		String file2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
		String ext2 = (dotPos2 == -1 ? "" : pattern2.substring(dotPos2));
		boolean ext1All = (ext1.equals(".*") || ext1.isEmpty());
		boolean ext2All = (ext2.equals(".*") || ext2.isEmpty());
		if (!ext1All && !ext2All) {
			throw new IllegalArgumentException("Cannot combine patterns: " + pattern1 + " vs " + pattern2);
		}
		String ext = (ext1All ? ext2 : ext1);
		return file2 + ext;
	}

	private String concat(String path1, String path2) {
		boolean path1EndsWithSeparator = path1.endsWith(this.pathSeparator);
		boolean path2StartsWithSeparator = path2.startsWith(this.pathSeparator);

		if (path1EndsWithSeparator && path2StartsWithSeparator) {
			return path1 + path2.substring(1);
		} else if (path1EndsWithSeparator || path2StartsWithSeparator) {
			return path1 + path2;
		} else {
			return path1 + this.pathSeparator + path2;
		}
	}

	/**
	 * 给定完整路径，返回适合按明确性排序模式的{@link Comparator}。
	 * <p>此{@code Comparator}将{@linkplain java.util.List#sort(Comparator) 排序}列表，
	 * 使更具体的模式（不含URI模板或通配符）排在通用模式之前。例如给定包含以下模式的列表，
	 * 返回的比较器将按如下顺序排序：
	 * <ol>
	 * <li>{@code /hotels/new}</li>
	 * <li>{@code /hotels/{hotel}}</li>
	 * <li>{@code /hotels/*}</li>
	 * </ol>
	 * <p>作为参数给出的完整路径用于测试精确匹配。因此当给定路径为{@code /hotels/2}时，
	 * 模式{@code /hotels/2}将排在{@code /hotels/1}之前。
	 *
	 * @param path 用于比较的完整路径
	 * @return 能够按明确性排序模式的比较器
	 */
	@Override
	public Comparator<String> getPatternComparator(String path) {
		return new AntPatternComparator(path);
	}


	/**
	 * 通过{@link Pattern}测试字符串是否与模式匹配。
	 * <p>模式可能包含特殊字符：'*'表示零个或多个字符；'?'表示且仅表示一个字符；
	 * '{'和'}'表示URI模板模式。例如<tt>/users/{user}</tt>。
	 */
	protected static class AntPathStringMatcher {

		private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

		private static final String DEFAULT_VARIABLE_PATTERN = "((?s).*)";

		private final String rawPattern;

		private final boolean caseSensitive;

		private final boolean exactMatch;

		@Nullable
		private final Pattern pattern;

		private final List<String> variableNames = new ArrayList<>();

		public AntPathStringMatcher(String pattern) {
			this(pattern, true);
		}

		public AntPathStringMatcher(String pattern, boolean caseSensitive) {
			this.rawPattern = pattern;
			this.caseSensitive = caseSensitive;
			StringBuilder patternBuilder = new StringBuilder();
			Matcher matcher = GLOB_PATTERN.matcher(pattern);
			int end = 0;
			while (matcher.find()) {
				patternBuilder.append(quote(pattern, end, matcher.start()));
				String match = matcher.group();
				if ("?".equals(match)) {
					patternBuilder.append('.');
				} else if ("*".equals(match)) {
					patternBuilder.append(".*");
				} else if (match.startsWith("{") && match.endsWith("}")) {
					int colonIdx = match.indexOf(':');
					if (colonIdx == -1) {
						patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
						this.variableNames.add(matcher.group(1));
					} else {
						String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
						patternBuilder.append('(');
						patternBuilder.append(variablePattern);
						patternBuilder.append(')');
						String variableName = match.substring(1, colonIdx);
						this.variableNames.add(variableName);
					}
				}
				end = matcher.end();
			}
			// 没有找到glob模式，这是一个精确的字符串匹配
			if (end == 0) {
				this.exactMatch = true;
				this.pattern = null;
			} else {
				this.exactMatch = false;
				patternBuilder.append(quote(pattern, end, pattern.length()));
				this.pattern = (this.caseSensitive ? Pattern.compile(patternBuilder.toString()) :
						Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE));
			}
		}

		private String quote(String s, int start, int end) {
			if (start == end) {
				return "";
			}
			return Pattern.quote(s.substring(start, end));
		}

		/**
		 * 主要入口方法。
		 *
		 * @return 如果字符串匹配模式返回{@code true}，否则返回{@code false}
		 */
		public boolean matchStrings(String str, @Nullable Map<String, String> uriTemplateVariables) {
			if (this.exactMatch) {
				return this.caseSensitive ? this.rawPattern.equals(str) : this.rawPattern.equalsIgnoreCase(str);
			} else if (this.pattern != null) {
				Matcher matcher = this.pattern.matcher(str);
				if (matcher.matches()) {
					if (uriTemplateVariables != null) {
						if (this.variableNames.size() != matcher.groupCount()) {
							throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
									this.pattern + " does not match the number of URI template variables it defines, " +
									"which can occur if capturing groups are used in a URI template regex. " +
									"Use non-capturing groups instead.");
						}
						for (int i = 1; i <= matcher.groupCount(); i++) {
							String name = this.variableNames.get(i - 1);
							if (name.startsWith("*")) {
								throw new IllegalArgumentException("Capturing patterns (" + name + ") are not " +
										"supported by the AntPathMatcher. Use the PathPatternParser instead.");
							}
							String value = matcher.group(i);
							uriTemplateVariables.put(name, value);
						}
					}
					return true;
				}
			}
			return false;
		}

	}


	/**
	 * {@link #getPatternComparator(String)}方法返回的默认{@link Comparator}实现。
	 * <p>按照以下顺序确定最"通用"的模式：
	 * <ul>
	 * <li>如果为null或是全匹配模式（即等于"/**"）</li>
	 * <li>如果另一个模式是实际匹配</li>
	 * <li>如果是通配模式（即以"**"结尾）</li>
	 * <li>如果比另一个模式包含更多"*"</li>
	 * <li>如果比另一个模式包含更多"{foo}"</li>
	 * <li>如果比另一个模式更短</li>
	 * </ul>
	 */
	protected static class AntPatternComparator implements Comparator<String> {

		private final String path;

		public AntPatternComparator(String path) {
			this.path = path;
		}

		/**
		 * 比较两个模式以确定哪个应该优先匹配，即相对于当前路径哪个更具体。
		 *
		 * @return 负整数、零或正整数，分别表示pattern1比pattern2更具体、同等具体或不够具体
		 */
		@Override
		public int compare(String pattern1, String pattern2) {
			PatternInfo info1 = new PatternInfo(pattern1);
			PatternInfo info2 = new PatternInfo(pattern2);

			if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
				return 0;
			} else if (info1.isLeastSpecific()) {
				return 1;
			} else if (info2.isLeastSpecific()) {
				return -1;
			}

			boolean pattern1EqualsPath = pattern1.equals(this.path);
			boolean pattern2EqualsPath = pattern2.equals(this.path);
			if (pattern1EqualsPath && pattern2EqualsPath) {
				return 0;
			} else if (pattern1EqualsPath) {
				return -1;
			} else if (pattern2EqualsPath) {
				return 1;
			}

			if (info1.isPrefixPattern() && info2.isPrefixPattern()) {
				return info2.getLength() - info1.getLength();
			} else if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
				return 1;
			} else if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
				return -1;
			}

			if (info1.getTotalCount() != info2.getTotalCount()) {
				return info1.getTotalCount() - info2.getTotalCount();
			}

			if (info1.getLength() != info2.getLength()) {
				return info2.getLength() - info1.getLength();
			}

			if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
				return -1;
			} else if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
				return 1;
			}

			if (info1.getUriVars() < info2.getUriVars()) {
				return -1;
			} else if (info2.getUriVars() < info1.getUriVars()) {
				return 1;
			}

			return 0;
		}


		/**
		 * 值对象类，用于保存模式相关信息，例如：
		 * "*"、"**"和"{"等模式元素的出现次数。
		 */
		private static class PatternInfo {

			@Nullable
			private final String pattern;

			private int uriVars;

			private int singleWildcards;

			private int doubleWildcards;

			private boolean catchAllPattern;

			private boolean prefixPattern;

			@Nullable
			private Integer length;

			public PatternInfo(@Nullable String pattern) {
				this.pattern = pattern;
				if (this.pattern != null) {
					initCounters();
					this.catchAllPattern = this.pattern.equals("/**");
					this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith("/**");
				}
				if (this.uriVars == 0) {
					this.length = (this.pattern != null ? this.pattern.length() : 0);
				}
			}

			protected void initCounters() {
				int pos = 0;
				if (this.pattern != null) {
					while (pos < this.pattern.length()) {
						if (this.pattern.charAt(pos) == '{') {
							this.uriVars++;
							pos++;
						} else if (this.pattern.charAt(pos) == '*') {
							if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
								this.doubleWildcards++;
								pos += 2;
							} else if (pos > 0 && !this.pattern.substring(pos - 1).equals(".*")) {
								this.singleWildcards++;
								pos++;
							} else {
								pos++;
							}
						} else {
							pos++;
						}
					}
				}
			}

			public int getUriVars() {
				return this.uriVars;
			}

			public int getSingleWildcards() {
				return this.singleWildcards;
			}

			public int getDoubleWildcards() {
				return this.doubleWildcards;
			}

			public boolean isLeastSpecific() {
				return (this.pattern == null || this.catchAllPattern);
			}

			public boolean isPrefixPattern() {
				return this.prefixPattern;
			}

			public int getTotalCount() {
				return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
			}

			/**
			 * 返回给定模式的长度，其中模板变量被视为长度为1。
			 */
			public int getLength() {
				if (this.length == null) {
					this.length = (this.pattern != null ?
							VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length() : 0);
				}
				return this.length;
			}
		}
	}


	/**
	 * 用于缓存基于配置路径分隔符的模式的简单缓存类。
	 */
	private static class PathSeparatorPatternCache {

		private final String endsOnWildCard;

		private final String endsOnDoubleWildCard;

		public PathSeparatorPatternCache(String pathSeparator) {
			this.endsOnWildCard = pathSeparator + "*";
			this.endsOnDoubleWildCard = pathSeparator + "**";
		}

		public String getEndsOnWildCard() {
			return this.endsOnWildCard;
		}

		public String getEndsOnDoubleWildCard() {
			return this.endsOnDoubleWildCard;
		}
	}

}
