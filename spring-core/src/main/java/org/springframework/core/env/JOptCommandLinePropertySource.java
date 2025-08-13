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

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 由 JOpt {@link OptionSet} 支持的 {@link CommandLinePropertySource} 实现。
 *
 * <h2>典型用法</h2>
 *
 * 针对提供给 {@code main} 方法的 {@code String[]} 参数配置并执行一个 {@code OptionParser}，
 * 然后使用生成的 {@code OptionSet} 对象创建一个 {@link JOptCommandLinePropertySource}：
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     OptionParser parser = new OptionParser();
 *     parser.accepts("option1");
 *     parser.accepts("option2").withRequiredArg();
 *     OptionSet options = parser.parse(args);
 *     PropertySource&lt;?&gt; ps = new JOptCommandLinePropertySource(options);
 *     // ...
 * }</pre>
 *
 * 有关完整的通用用法示例，请参阅 {@link CommandLinePropertySource}。
 *
 * <p>需要 JOpt Simple 4.3 或更高版本。已针对 JOpt 5.0 及以下版本进行测试。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Dave Syer
 * @since 3.1
 * @see CommandLinePropertySource
 * @see joptsimple.OptionParser
 * @see joptsimple.OptionSet
 */
public class JOptCommandLinePropertySource extends CommandLinePropertySource<OptionSet> {

	/**
	 * 创建一个具有默认名称且由给定 {@code OptionSet} 支持的新 {@code JOptCommandLinePropertySource}。
	 * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
	 * @see CommandLinePropertySource#CommandLinePropertySource(Object)
	 */
	public JOptCommandLinePropertySource(OptionSet options) {
		super(options);
	}

	/**
	 * 创建一个具有给定名称且由给定 {@code OptionSet} 支持的新 {@code JOptCommandLinePropertySource}。
	 */
	public JOptCommandLinePropertySource(String name, OptionSet options) {
		super(name, options);
	}


	@Override
	protected boolean containsOption(String name) {
		return this.source.has(name);
	}

	@Override
	public String[] getPropertyNames() {
		List<String> names = new ArrayList<>();
		for (OptionSpec<?> spec : this.source.specs()) {
			String lastOption = CollectionUtils.lastElement(spec.options());
			if (lastOption != null) {
				// 只使用最长的名称来枚举
				names.add(lastOption);
			}
		}
		return StringUtils.toStringArray(names);
	}

	@Override
	@Nullable
	public List<String> getOptionValues(String name) {
		List<?> argValues = this.source.valuesOf(name);
		List<String> stringArgValues = new ArrayList<>();
		for (Object argValue : argValues) {
			stringArgValues.add(argValue.toString());
		}
		if (stringArgValues.isEmpty()) {
			return (this.source.has(name) ? Collections.emptyList() : null);
		}
		return Collections.unmodifiableList(stringArgValues);
	}

	@Override
	protected List<String> getNonOptionArgs() {
		List<?> argValues = this.source.nonOptionArguments();
		List<String> stringArgValues = new ArrayList<>();
		for (Object argValue : argValues) {
			stringArgValues.add(argValue.toString());
		}
		return (stringArgValues.isEmpty() ? Collections.emptyList() :
				Collections.unmodifiableList(stringArgValues));
	}

}
