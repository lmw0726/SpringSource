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

package org.springframework.web.reactive.config;

import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 辅助配置 {@code HandlerMapping} 的路径匹配选项。
 *
 * <p>通过此类可以配置路径匹配选项。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class PathMatchConfigurer {

	/**
	 * 是否匹配尾部斜杠
	 */
	@Nullable
	private Boolean trailingSlashMatch;

	/**
	 * 是否区分大小写
	 */
	@Nullable
	private Boolean caseSensitiveMatch;

	/**
	 * 路径前缀与类的断言映射关系的集合
	 */
	@Nullable
	private Map<String, Predicate<Class<?>>> pathPrefixes;


	/**
	 * 设置是否对 URL 进行区分大小写的匹配。
	 * 如果启用，映射到 "/users" 的方法将不会匹配 "/Users/"。
	 * <p>默认值为 {@code false}。
	 *
	 * @param caseSensitiveMatch 是否对大小写敏感
	 * @return 当前配置类
	 */
	public PathMatchConfigurer setUseCaseSensitiveMatch(Boolean caseSensitiveMatch) {
		this.caseSensitiveMatch = caseSensitiveMatch;
		return this;
	}

	/**
	 * 设置是否匹配 URL 末尾是否有斜杠。
	 * 如果启用，映射到 "/users" 的方法也将匹配 "/users/"。
	 * <p>默认值为 {@code true}。
	 *
	 * @param trailingSlashMatch 是否匹配末尾斜杠
	 * @return 当前配置类
	 */
	public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
		this.trailingSlashMatch = trailingSlashMatch;
		return this;
	}

	/**
	 * 配置要应用于匹配控制器方法的路径前缀。
	 * <p>前缀用于扩充每个 {@code @RequestMapping} 方法的映射，
	 * 其中控制器类型与相应的 {@code Predicate} 匹配。
	 * 使用第一个匹配谓词的前缀。
	 * <p>考虑使用 {@link org.springframework.web.method.HandlerTypePredicate HandlerTypePredicate} 对控制器进行分组。
	 *
	 * @param prefix    要应用的路径前缀
	 * @param predicate 用于匹配控制器类型的谓词
	 * @return 当前配置类
	 * @since 5.1
	 */
	public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
		if (this.pathPrefixes == null) {
			this.pathPrefixes = new LinkedHashMap<>();
		}
		this.pathPrefixes.put(prefix, predicate);
		return this;
	}

	/**
	 * 获取是否使用末尾斜杠匹配。
	 *
	 * @return 是否使用末尾斜杠匹配的布尔值，可以为 {@code null}
	 */
	@Nullable
	protected Boolean isUseTrailingSlashMatch() {
		return this.trailingSlashMatch;
	}

	/**
	 * 获取是否使用区分大小写的匹配。
	 *
	 * @return 是否使用区分大小写的布尔值，可以为 {@code null}
	 */
	@Nullable
	protected Boolean isUseCaseSensitiveMatch() {
		return this.caseSensitiveMatch;
	}

	/**
	 * 获取路径前缀映射。
	 *
	 * @return 路径前缀映射
	 */
	@Nullable
	protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}
}
