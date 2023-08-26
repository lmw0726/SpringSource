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

package org.springframework.beans.factory.support;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Set of method overrides, determining which, if any, methods on a
 * managed object the Spring IoC container will override at runtime.
 *
 * <p>The currently supported {@link MethodOverride} variants are
 * {@link LookupOverride} and {@link ReplaceOverride}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see MethodOverride
 * @since 1.1
 */
public class MethodOverrides {

	/**
	 * 属性覆盖获属性替换列表
	 */
	private final Set<MethodOverride> overrides = new CopyOnWriteArraySet<>();


	/**
	 * 创建新的方法。
	 */
	public MethodOverrides() {
	}

	/**
	 * 深度复制构造函数。
	 */
	public MethodOverrides(MethodOverrides other) {
		addOverrides(other);
	}


	/**
	 * 将所有给定的方法重写复制到此对象中。
	 */
	public void addOverrides(@Nullable MethodOverrides other) {
		if (other != null) {
			this.overrides.addAll(other.overrides);
		}
	}

	/**
	 * 添加给定的方法覆盖。
	 */
	public void addOverride(MethodOverride override) {
		this.overrides.add(override);
	}

	/**
	 * 返回此对象包含的所有方法重写。
	 *
	 * @return 一组MethodOverride对象
	 * @see MethodOverride
	 */
	public Set<MethodOverride> getOverrides() {
		return this.overrides;
	}

	/**
	 * 返回方法覆盖Set是否为空。
	 */
	public boolean isEmpty() {
		return this.overrides.isEmpty();
	}

	/**
	 * 返回给定方法的覆盖 (如果有)。
	 *
	 * @param method 检查是否为覆盖的方法
	 * @return 方法覆盖，如果没有，则为 {@code null}
	 */
	@Nullable
	public MethodOverride getOverride(Method method) {
		MethodOverride match = null;
		for (MethodOverride candidate : this.overrides) {
			//遍历方法覆盖Set，如果匹配则返回该方法覆盖。
			if (candidate.matches(method)) {
				match = candidate;
			}
		}
		return match;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverrides)) {
			return false;
		}
		MethodOverrides that = (MethodOverrides) other;
		return this.overrides.equals(that.overrides);
	}

	@Override
	public int hashCode() {
		return this.overrides.hashCode();
	}

}
