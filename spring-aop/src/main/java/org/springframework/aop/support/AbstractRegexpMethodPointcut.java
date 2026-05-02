/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 抽象的正则表达式切点 Bean 基类。JavaBean 属性包括：
 * <ul>
 * <li>pattern：用于匹配完全限定方法名的正则表达式。
 * 确切的正则表达式语法取决于子类（例如 Perl5 正则表达式）
 * <li>patterns：可选属性，接受 String 数组形式的模式。
 * 结果将是这些模式的并集。
 * </ul>
 *
 * <p>注意：正则表达式必须是匹配整个目标的匹配。例如，
 * {@code .*get.*} 将匹配 com.mycom.Foo.getBar()。
 * {@code get.*} 则不会。
 *
 * <p>此基类可序列化。子类应将所有字段声明为 transient；
 * {@link #initPatternRepresentation} 方法将在反序列化时再次调用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1
 * @see JdkRegexpMethodPointcut
 */
@SuppressWarnings("serial")
public abstract class AbstractRegexpMethodPointcut extends StaticMethodMatcherPointcut
		implements Serializable {

	/**
	 * 要匹配的正则表达式。
	 */
	private String[] patterns = new String[0];

	/**
	 * <strong>不</strong>要匹配的正则表达式。
	 */
	private String[] excludedPatterns = new String[0];


	/**
	 * 当只有单个模式时使用的便捷方法。
	 * 使用此方法或 {@link #setPatterns}，不要同时使用两者。
	 * @see #setPatterns
	 */
	public void setPattern(String pattern) {
		setPatterns(pattern);
	}

	/**
	 * 设置定义要匹配方法的正则表达式。
	 * 匹配结果将是所有这些表达式的并集；如果任一表达式匹配，则切点匹配。
	 * @see #setPattern
	 */
	public void setPatterns(String... patterns) {
		Assert.notEmpty(patterns, "'patterns' must not be empty");
		this.patterns = new String[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			this.patterns[i] = StringUtils.trimWhitespace(patterns[i]);
		}
		initPatternRepresentation(this.patterns);
	}

	/**
	 * 返回用于方法匹配的正则表达式。
	 */
	public String[] getPatterns() {
		return this.patterns;
	}

	/**
	 * 当只有单个排除模式时使用的便捷方法。
	 * 使用此方法或 {@link #setExcludedPatterns}，不要同时使用两者。
	 * @see #setExcludedPatterns
	 */
	public void setExcludedPattern(String excludedPattern) {
		setExcludedPatterns(excludedPattern);
	}

	/**
	 * 设置定义要排除匹配的方法的正则表达式。
	 * 匹配结果将是所有这些表达式的并集；如果任一表达式匹配，则切点匹配。
	 * @see #setExcludedPattern
	 */
	public void setExcludedPatterns(String... excludedPatterns) {
		Assert.notEmpty(excludedPatterns, "'excludedPatterns' must not be empty");
		this.excludedPatterns = new String[excludedPatterns.length];
		for (int i = 0; i < excludedPatterns.length; i++) {
			this.excludedPatterns[i] = StringUtils.trimWhitespace(excludedPatterns[i]);
		}
		initExcludedPatternRepresentation(this.excludedPatterns);
	}

	/**
	 * 返回用于排除匹配的正则表达式。
	 */
	public String[] getExcludedPatterns() {
		return this.excludedPatterns;
	}


	/**
	 * 尝试将正则表达式与目标类的完全限定名以及方法声明类的完全限定名
	 * 加上方法名进行匹配。
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return (matchesPattern(ClassUtils.getQualifiedMethodName(method, targetClass)) ||
				(targetClass != method.getDeclaringClass() &&
						matchesPattern(ClassUtils.getQualifiedMethodName(method, method.getDeclaringClass()))));
	}

	/**
	 * 将指定候选项与配置的模式进行匹配。
	 * @param signatureString "java.lang.Object.hashCode" 风格的签名
	 * @return 候选项是否至少匹配一个指定模式
	 */
	protected boolean matchesPattern(String signatureString) {
		for (int i = 0; i < this.patterns.length; i++) {
			boolean matched = matches(signatureString, i);
			if (matched) {
				for (int j = 0; j < this.excludedPatterns.length; j++) {
					boolean excluded = matchesExclusion(signatureString, j);
					if (excluded) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}


	/**
	 * 子类必须实现此方法来初始化正则表达式切点。
	 * 可被调用多次。
	 * <p>此方法将从 {@link #setPatterns} 方法中调用，
	 * 并且也会在反序列化时调用。
	 * @param patterns 要初始化的模式
	 * @throws IllegalArgumentException 如果模式无效
	 */
	protected abstract void initPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * 子类必须实现此方法来初始化正则表达式切点。
	 * 可被调用多次。
	 * <p>此方法将从 {@link #setExcludedPatterns} 方法中调用，
	 * 并且也会在反序列化时调用。
	 * @param patterns 要初始化的模式
	 * @throws IllegalArgumentException 如果模式无效
	 */
	protected abstract void initExcludedPatternRepresentation(String[] patterns) throws IllegalArgumentException;

	/**
	 * 给定索引处的模式是否匹配给定的 String？
	 * @param pattern 要匹配的 {@code String} 模式
	 * @param patternIndex 模式索引（从 0 开始）
	 * @return 如果匹配则为 {@code true}，否则为 {@code false}
	 */
	protected abstract boolean matches(String pattern, int patternIndex);

	/**
	 * 给定索引处的排除模式是否匹配给定的 String？
	 * @param pattern 要匹配的 {@code String} 模式
	 * @param patternIndex 模式索引（从 0 开始）
	 * @return 如果匹配则为 {@code true}，否则为 {@code false}
	 */
	protected abstract boolean matchesExclusion(String pattern, int patternIndex);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractRegexpMethodPointcut)) {
			return false;
		}
		AbstractRegexpMethodPointcut otherPointcut = (AbstractRegexpMethodPointcut) other;
		return (Arrays.equals(this.patterns, otherPointcut.patterns) &&
				Arrays.equals(this.excludedPatterns, otherPointcut.excludedPatterns));
	}

	@Override
	public int hashCode() {
		int result = 27;
		for (String pattern : this.patterns) {
			result = 13 * result + pattern.hashCode();
		}
		for (String excludedPattern : this.excludedPatterns) {
			result = 13 * result + excludedPattern.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		return getClass().getName() + ": patterns " + ObjectUtils.nullSafeToString(this.patterns) +
				", excluded patterns " + ObjectUtils.nullSafeToString(this.excludedPatterns);
	}

}
