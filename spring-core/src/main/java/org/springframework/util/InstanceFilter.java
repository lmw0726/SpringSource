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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * 一个简单的实例过滤器，基于包含和排除集合来判断给定实例是否匹配。
 *
 * <p>子类可以重写 {@link #match(Object, Object)} 方法来提供自定义的匹配算法。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <T> 实例类型
 */
public class InstanceFilter<T> {

	private final Collection<? extends T> includes;

	private final Collection<? extends T> excludes;

	private final boolean matchIfEmpty;


	/**
	 * 根据包含和排除集合创建新的实例过滤器。
	 * <p>一个元素匹配的条件是：它“匹配”包含集合中的某个元素且不匹配排除集合中的任何元素。
	 * <p>子类可以重新定义“匹配”的含义，默认情况下，元素通过 {@link Object#equals(Object)} 判断相等。
	 * <p>如果包含和排除集合均为空，则通过 {@code matchIfEmpty} 来决定匹配结果。
	 * @param includes 包含的集合
	 * @param excludes 排除的集合
	 * @param matchIfEmpty 当包含和排除集合都为空时的匹配结果
	 */
	public InstanceFilter(@Nullable Collection<? extends T> includes,
			@Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {

		this.includes = (includes != null ? includes : Collections.emptyList());
		this.excludes = (excludes != null ? excludes : Collections.emptyList());
		this.matchIfEmpty = matchIfEmpty;
	}


	/**
	 * 判断指定的 {@code instance} 是否匹配该过滤器。
	 */
	public boolean match(T instance) {
		Assert.notNull(instance, "Instance to match must not be null");

		boolean includesSet = !this.includes.isEmpty();
		boolean excludesSet = !this.excludes.isEmpty();
		if (!includesSet && !excludesSet) {
			return this.matchIfEmpty;
		}

		boolean matchIncludes = match(instance, this.includes);
		boolean matchExcludes = match(instance, this.excludes);
		if (!includesSet) {
			return !matchExcludes;
		}
		if (!excludesSet) {
			return matchIncludes;
		}
		return matchIncludes && !matchExcludes;
	}

	/**
	 * 判断指定的 {@code instance} 是否等于指定的 {@code candidate}。
	 * @param instance 需要判断的实例
	 * @param candidate 过滤器定义的候选对象
	 * @return 如果实例匹配候选对象则返回 {@code true}
	 */
	protected boolean match(T instance, T candidate) {
		return instance.equals(candidate);
	}

	/**
	 * 判断指定的 {@code instance} 是否匹配候选集合中的某个对象。
	 * <p>如果候选集合为 {@code null}，则返回 {@code false}。
	 * @param instance 需要判断的实例
	 * @param candidates 候选对象集合
	 * @return 如果实例匹配候选集合中任一对象则返回 {@code true}，否则 {@code false}
	 */
	protected boolean match(T instance, Collection<? extends T> candidates) {
		for (T candidate : candidates) {
			if (match(instance, candidate)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(": includes=").append(this.includes);
		sb.append(", excludes=").append(this.excludes);
		sb.append(", matchIfEmpty=").append(this.matchIfEmpty);
		return sb.toString();
	}

}
