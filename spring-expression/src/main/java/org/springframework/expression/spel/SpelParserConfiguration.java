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

package org.springframework.expression.spel;

import org.springframework.core.SpringProperties;
import org.springframework.lang.Nullable;

/**
 * Configuration object for the SpEL expression parser.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @see org.springframework.expression.spel.standard.SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 * @since 3.0
 */
public class SpelParserConfiguration {

	/**
	 * SpEL表达式解析器，配置默认编译器模式的系统属性: {@value}。
	 */
	public static final String SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spring.expression.compiler.mode";


	/**
	 * SpringEL默认编译器模式
	 */
	private static final SpelCompilerMode defaultCompilerMode;

	static {
		//获取SpringEL编译器模式值
		String compilerMode = SpringProperties.getProperty(SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
		//如果编译器模式值为空，默认为关闭编译器模式
		defaultCompilerMode = (compilerMode != null ?
				SpelCompilerMode.valueOf(compilerMode.toUpperCase()) : SpelCompilerMode.OFF);
	}

	/**
	 * SpringEL编译器模式
	 */
	private final SpelCompilerMode compilerMode;

	/**
	 * 编译器类加载器
	 */
	@Nullable
	private final ClassLoader compilerClassLoader;

	/**
	 * null引用是否自动增长。
	 */
	private final boolean autoGrowNullReferences;

	/**
	 * 集合是否自动增长
	 */
	private final boolean autoGrowCollections;

	/**
	 * 集合自动增长的最大值
	 */
	private final int maximumAutoGrowSize;


	/**
	 * 使用默认设置创建一个新的 {@code SpelParserConfiguration} 实例。
	 */
	public SpelParserConfiguration() {
		this(null, null, false, false, Integer.MAX_VALUE);
	}

	/**
	 * 创建一个新的 {@code SpelParserConfiguration} 实例。
	 *
	 * @param compilerMode        解析器的编译器模式
	 * @param compilerClassLoader 用作表达式编译基础的类加载器
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
		this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
	}

	/**
	 * 创建一个新的 {@code SpelParserConfiguration} 实例。
	 *
	 * @param autoGrowNullReferences 如果空引用应该自动增长
	 * @param autoGrowCollections    如果集合应该自动增长
	 * @see #SpelParserConfiguration(boolean, boolean, int)
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
	}

	/**
	 * 创建一个新的 {@code SpelParserConfiguration} 实例。
	 *
	 * @param autoGrowNullReferences 如果空引用应该自动增长
	 * @param autoGrowCollections    如果集合应该自动增长
	 * @param maximumAutoGrowSize    集合可以自动增长的最大大小
	 */
	public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
		this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
	}

	/**
	 * 创建一个新的 {@code SpelParserConfiguration} 实例。
	 *
	 * @param compilerMode           使用此配置对象的解析器应该使用的编译器模式
	 * @param compilerClassLoader    用作表达式编译基础的类加载器
	 * @param autoGrowNullReferences 如果空引用应该自动增长
	 * @param autoGrowCollections    如果集合应该自动增长
	 * @param maximumAutoGrowSize    集合可以自动增长的最大大小
	 */
	public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
								   boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

		this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
		this.compilerClassLoader = compilerClassLoader;
		this.autoGrowNullReferences = autoGrowNullReferences;
		this.autoGrowCollections = autoGrowCollections;
		this.maximumAutoGrowSize = maximumAutoGrowSize;
	}


	/**
	 * 返回使用此配置对象的解析器的编译器模式。
	 */
	public SpelCompilerMode getCompilerMode() {
		return this.compilerMode;
	}

	/**
	 * 返回要用作表达式编译基础的类加载器。
	 */
	@Nullable
	public ClassLoader getCompilerClassLoader() {
		return this.compilerClassLoader;
	}

	/**
	 * 如果 {@code null} 引用应该自动增长，则返回 {@code true}。
	 */
	public boolean isAutoGrowNullReferences() {
		return this.autoGrowNullReferences;
	}

	/**
	 * 如果是集合应该自动增长，则返回 {@code true}。
	 */
	public boolean isAutoGrowCollections() {
		return this.autoGrowCollections;
	}

	/**
	 * 返回集合可以自动增长的最大大小。
	 */
	public int getMaximumAutoGrowSize() {
		return this.maximumAutoGrowSize;
	}

}
