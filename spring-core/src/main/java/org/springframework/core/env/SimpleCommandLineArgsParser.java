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

package org.springframework.core.env;

/**
 * 解析命令行参数的 {@code String[]} 数组，以填充
 * {@link CommandLineArgs} 对象。
 *
 * <h3>处理选项参数</h3>
 * <p>选项参数必须严格遵循以下语法：
 *
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>也就是说，选项必须以"{@code --}"为前缀，可以指定值也可以不指定值。
 * 如果指定了值，名称和值之间必须用等号("=")分隔，<em>不能有空格</em>。
 * 值可以是空字符串。
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
 * <p>在命令行中指定的所有不带"{@code --}"选项前缀的参数都将被视为
 * "非选项参数"，并可通过 {@link CommandLineArgs#getNonOptionArgs()} 方法获取。
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
class SimpleCommandLineArgsParser {

	/**
	 * 根据{@linkplain SimpleCommandLineArgsParser 上述}描述的规则解析给定的
	 * {@code String} 数组，返回一个完全填充的 {@link CommandLineArgs} 对象。
	 * @param args 命令行参数，通常来自 {@code main()} 方法
	 */
	public CommandLineArgs parse(String... args) {
		CommandLineArgs commandLineArgs = new CommandLineArgs();
		for (String arg : args) {
			if (arg.startsWith("--")) {
				String optionText = arg.substring(2);
				String optionName;
				String optionValue = null;
				int indexOfEqualsSign = optionText.indexOf('=');
				if (indexOfEqualsSign > -1) {
					optionName = optionText.substring(0, indexOfEqualsSign);
					optionValue = optionText.substring(indexOfEqualsSign + 1);
				}
				else {
					optionName = optionText;
				}
				if (optionName.isEmpty()) {
					throw new IllegalArgumentException("Invalid argument syntax: " + arg);
				}
				commandLineArgs.addOptionArg(optionName, optionValue);
			}
			else {
				commandLineArgs.addNonOptionArg(arg);
			}
		}
		return commandLineArgs;
	}

}
