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

package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基于简单字符串数组的 {@link CommandLinePropertySource} 实现。
 *
 * <h3>目的</h3>
 * <p>该 {@code CommandLinePropertySource} 实现旨在提供解析命令行参数的最简单方法。
 * 与所有 {@code CommandLinePropertySource} 实现一样，命令行参数被分为两类：
 * <em>选项参数</em> 和 <em>非选项参数</em>，如下所述
 * <em>（部分内容摘自 {@link SimpleCommandLineArgsParser} 的 Javadoc）</em>：
 *
 * <h3>处理选项参数</h3>
 * <p>选项参数必须遵循以下精确语法：
 *
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>也就是说，选项必须以 "{@code --}" 开头，可以带有也可以不带有值。
 * 如果指定了值，则名称和值之间必须 <em>无空格</em> 用等号 ("=") 连接。
 * 值也可以是空字符串。
 *
 * <h4>选项参数的有效示例</h4>
 * <pre class="code">
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>选项参数的无效示例</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>处理非选项参数</h3>
 * <p>任何未以 "{@code --}" 选项前缀开头的命令行参数都被视为“非选项参数”，
 * 并可通过 {@link CommandLineArgs#getNonOptionArgs()} 方法获取。
 *
 * <h3>典型用法</h3>
 * <pre class="code">
 * public static void main(String[] args) {
 *     PropertySource&lt;?&gt; ps = new SimpleCommandLinePropertySource(args);
 *     // ...
 * }</pre>
 *
 * 详见 {@link CommandLinePropertySource} 获取完整的通用使用示例。
 *
 * <h3>进阶使用</h3>
 *
 * <p>当需要更完整的命令行解析功能时，可考虑使用提供的
 * {@link JOptCommandLinePropertySource}，或者基于自己选择的命令行解析库
 * 自行实现 {@code CommandLinePropertySource}。
 *
 * @author Chris Beams
 * @since 3.1
 * @see CommandLinePropertySource
 * @see JOptCommandLinePropertySource
 */
public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {

	/**
	 * 使用默认名称创建一个新的 {@code SimpleCommandLinePropertySource}，
	 * 并以给定的命令行参数字符串数组作为数据源。
	 * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
	 * @see CommandLinePropertySource#CommandLinePropertySource(Object)
	 */
	public SimpleCommandLinePropertySource(String... args) {
		super(new SimpleCommandLineArgsParser().parse(args));
	}

	/**
	 * 使用给定名称创建一个新的 {@code SimpleCommandLinePropertySource}，
	 * 并以给定的命令行参数字符串数组作为数据源。
	 */
	public SimpleCommandLinePropertySource(String name, String[] args) {
		super(name, new SimpleCommandLineArgsParser().parse(args));
	}

	/**
	 * 获取所有选项参数的属性名称。
	 */
	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.getOptionNames());
	}

	@Override
	protected boolean containsOption(String name) {
		return this.source.containsOption(name);
	}

	@Override
	@Nullable
	protected List<String> getOptionValues(String name) {
		return this.source.getOptionValues(name);
	}

	@Override
	protected List<String> getNonOptionArgs() {
		return this.source.getNonOptionArgs();
	}

}
