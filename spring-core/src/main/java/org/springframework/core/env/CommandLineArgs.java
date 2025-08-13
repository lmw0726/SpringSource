/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.*;

/**
 * 命令行参数的简单表示，分为"选项参数"和"非选项参数"。
 *
 * @author Chris Beams
 * @since 3.1
 * @see SimpleCommandLineArgsParser
 */
class CommandLineArgs {

	private final Map<String, List<String>> optionArgs = new HashMap<>();
	private final List<String> nonOptionArgs = new ArrayList<>();

	/**
	 * 为给定的选项名称添加一个选项参数，并将给定的值添加到与此选项关联的值列表中
	 * （可能有零个或多个值）。给定的值可能为 {@code null}，表示选项被指定时没有
	 * 关联的值（例如 "--foo" 与 "--foo=bar" 的区别）。
	 */
	public void addOptionArg(String optionName, @Nullable String optionValue) {
		if (!this.optionArgs.containsKey(optionName)) {
			this.optionArgs.put(optionName, new ArrayList<>());
		}
		if (optionValue != null) {
			this.optionArgs.get(optionName).add(optionValue);
		}
	}

	/**
	 * 返回命令行中存在的所有选项参数的集合。
	 */
	public Set<String> getOptionNames() {
		return Collections.unmodifiableSet(this.optionArgs.keySet());
	}

	/**
	 * 返回具有给定名称的选项是否存在于命令行中。
	 */
	public boolean containsOption(String optionName) {
		return this.optionArgs.containsKey(optionName);
	}

	/**
	 * 返回与给定选项关联的值列表。{@code null} 表示该选项不存在；
	 * 空列表表示没有与此选项关联的值。
	 */
	@Nullable
	public List<String> getOptionValues(String optionName) {
		return this.optionArgs.get(optionName);
	}

	/**
	 * 将给定的值添加到非选项参数列表中。
	 */
	public void addNonOptionArg(String value) {
		this.nonOptionArgs.add(value);
	}

	/**
	 * 返回在命令行中指定的非选项参数列表。
	 */
	public List<String> getNonOptionArgs() {
		return Collections.unmodifiableList(this.nonOptionArgs);
	}

}
