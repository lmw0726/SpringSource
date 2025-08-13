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

import java.util.function.Predicate;

/**
 * 表示一个配置环境的 Profile 谓词，可被 {@link Environment} 接受。
 *
 * <p>可直接实现该接口，通常通过 {@link #of(String...)} 工厂方法创建。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.1
 */
@FunctionalInterface
public interface Profiles {

	/**
	 * 判断该 {@code Profiles} 实例是否与给定的活跃配置条件匹配。
	 * @param activeProfiles 一个判断某个 profile 是否激活的谓词
	 * @return 是否匹配
	 */
	boolean matches(Predicate<String> activeProfiles);


	/**
	 * 创建一个新的 {@link Profiles} 实例，用于检测给定的 <em>profile 字符串</em> 是否匹配。
	 * <p>返回的实例只要有任意一个给定的 profile 字符串匹配，{@link #matches(Predicate)} 就返回 true。
	 * <p>profile 字符串可以是简单的 profile 名称（例如 {@code "production"}）或 profile 表达式。
	 * profile 表达式允许更复杂的逻辑，如 {@code "production & cloud"}。
	 * <p>支持的表达式操作符包括：
	 * <ul>
	 * <li>{@code !} - 逻辑非</li>
	 * <li>{@code &} - 逻辑与</li>
	 * <li>{@code |} - 逻辑或</li>
	 * </ul>
	 * <p>注意，{@code &} 和 {@code |} 不能混合使用，除非使用括号，例如 {@code "a & b | c"} 是无效的，
	 * 应写成 {@code "(a & b) | c"} 或 {@code "a & (b | c)"}。
	 * <p>从 Spring Framework 5.1.17 起，使用相同 <em>profile 字符串</em> 创建的两个 {@code Profiles} 实例在
	 * {@code equals()} 和 {@code hashCode()} 上被视为相等。
	 * @param profiles 要包含的 <em>profile 字符串</em>
	 * @return 新的 {@link Profiles} 实例
	 */
	static Profiles of(String... profiles) {
		return ProfilesParser.parse(profiles);
	}

}
