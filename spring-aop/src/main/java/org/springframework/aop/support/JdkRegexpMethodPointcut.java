/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 基于 {@code java.util.regex} 包的正则表达式切点。
 * 支持以下 JavaBean 属性：
 * <ul>
 * <li>pattern：用于匹配完全限定方法名的正则表达式
 * <li>patterns：可选属性，接受 String 数组形式的模式。结果将
 * 是这些模式的并集。
 * </ul>
 *
 * <p>注意：正则表达式必须是匹配整个目标的匹配。例如，
 * {@code .*get.*} 将匹配 com.mycom.Foo.getBar()。
 * {@code get.*} 则不会。
 *
 * @author Dmitriy Kopylenko
 * @author Rob Harrop
 * @since 1.1
 */
@SuppressWarnings("serial")
public class JdkRegexpMethodPointcut extends AbstractRegexpMethodPointcut {

	/**
	 * 模式的已编译形式。
	 */
	private Pattern[] compiledPatterns = new Pattern[0];

	/**
	 * 排除模式的已编译形式。
	 */
	private Pattern[] compiledExclusionPatterns = new Pattern[0];


	/**
	 * 从提供的 {@code String[]} 初始化 {@link Pattern Patterns}。
	 */
	@Override
	protected void initPatternRepresentation(String[] patterns) throws PatternSyntaxException {
		this.compiledPatterns = compilePatterns(patterns);
	}

	/**
	 * 从提供的 {@code String[]} 初始化排除用的 {@link Pattern Patterns}。
	 */
	@Override
	protected void initExcludedPatternRepresentation(String[] excludedPatterns) throws PatternSyntaxException {
		this.compiledExclusionPatterns = compilePatterns(excludedPatterns);
	}

	/**
	 * 如果索引 {@code patternIndex} 处的 {@link Pattern}
	 * 匹配提供的候选 {@code String}，则返回 {@code true}。
	 */
	@Override
	protected boolean matches(String pattern, int patternIndex) {
		Matcher matcher = this.compiledPatterns[patternIndex].matcher(pattern);
		return matcher.matches();
	}

	/**
	 * 如果索引 {@code patternIndex} 处的排除 {@link Pattern}
	 * 匹配提供的候选 {@code String}，则返回 {@code true}。
	 */
	@Override
	protected boolean matchesExclusion(String candidate, int patternIndex) {
		Matcher matcher = this.compiledExclusionPatterns[patternIndex].matcher(candidate);
		return matcher.matches();
	}


	/**
	 * 将提供的 {@code String[]} 编译为 {@link Pattern} 对象数组并返回该数组。
	 */
	private Pattern[] compilePatterns(String[] source) throws PatternSyntaxException {
		Pattern[] destination = new Pattern[source.length];
		for (int i = 0; i < source.length; i++) {
			destination[i] = Pattern.compile(source[i]);
		}
		return destination;
	}

}
