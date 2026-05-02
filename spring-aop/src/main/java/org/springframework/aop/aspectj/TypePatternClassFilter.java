/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.TypePatternMatcher;

import org.springframework.aop.ClassFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 使用 AspectJ 类型匹配的 Spring AOP {@link ClassFilter} 实现。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
public class TypePatternClassFilter implements ClassFilter {

	private String typePattern = "";

	@Nullable
	private TypePatternMatcher aspectJTypePatternMatcher;


	/**
	 * 创建 {@link TypePatternClassFilter} 类的新实例。
	 * <p>这是 JavaBean 构造函数；务必设置
	 * {@link #setTypePattern(String) typePattern} 属性，否则在首次调用
	 * {@link #matches(Class)} 方法时，无疑会抛出致命的 {@link IllegalStateException}。
	 */
	public TypePatternClassFilter() {
	}

	/**
	 * 使用给定类型模式创建完全配置的 {@link TypePatternClassFilter}。
	 * @param typePattern AspectJ 编织器应解析的类型模式
	 */
	public TypePatternClassFilter(String typePattern) {
		setTypePattern(typePattern);
	}


	/**
	 * 设置要匹配的 AspectJ 类型模式。
	 * <p>示例包括：
	 * <code class="code">
	 * org.springframework.beans.*
	 * </code>
	 * 这将匹配给定包中的任何类或接口。
	 * <code class="code">
	 * org.springframework.beans.ITestBean+
	 * </code>
	 * 这将匹配 {@code ITestBean} 接口以及实现它的任何类。
	 * <p>这些约定由 AspectJ 建立，而不是由 Spring AOP 建立。
	 * @param typePattern AspectJ 编织器应解析的类型模式
	 */
	public void setTypePattern(String typePattern) {
		Assert.notNull(typePattern, "Type pattern must not be null");
		this.typePattern = typePattern;
		this.aspectJTypePatternMatcher =
				PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution().
				parseTypePattern(replaceBooleanOperators(typePattern));
	}

	/**
	 * 返回要匹配的 AspectJ 类型模式。
	 */
	public String getTypePattern() {
		return this.typePattern;
	}


	/**
	 * 切点是否应应用于给定接口或目标类？
	 * @param clazz 候选目标类
	 * @return 通知是否应应用于此候选目标类
	 * @throws IllegalStateException 如果尚未设置 {@link #setTypePattern(String)}
	 */
	@Override
	public boolean matches(Class<?> clazz) {
		Assert.state(this.aspectJTypePatternMatcher != null, "No type pattern has been set");
		return this.aspectJTypePatternMatcher.matches(clazz);
	}

	/**
	 * 如果在 XML 中指定了类型模式，用户不能将 {@code and} 写作 "&&"
	 * （不过 &amp;&amp; 可以工作）。
	 * 我们也允许在两个子表达式之间使用 {@code and}。
	 * <p>此方法会转换回 {@code &&}，供 AspectJ 切点解析器使用。
	 */
	private String replaceBooleanOperators(String pcExpr) {
		String result = StringUtils.replace(pcExpr," and "," && ");
		result = StringUtils.replace(result, " or ", " || ");
		return StringUtils.replace(result, " not ", " ! ");
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof TypePatternClassFilter &&
				ObjectUtils.nullSafeEquals(this.typePattern, ((TypePatternClassFilter) other).typePattern)));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.typePattern);
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.typePattern;
	}

}
