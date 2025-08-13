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

package org.springframework.core.metrics;

import org.springframework.lang.Nullable;

import java.util.function.Supplier;

/**
 * 记录 {@link ApplicationStartup} 过程中某个阶段或操作的指标的步骤。
 *
 * <p>{@code StartupStep} 的生命周期如下：
 * <ol>
 * <li>步骤被创建并通过调用 {@link ApplicationStartup#start(String)} 启动，
 * 并分配一个唯一的 {@link StartupStep#getId() id}。
 * <li>处理过程中可以通过 {@link Tags} 添加信息标签
 * <li>然后需要调用 {@link #end()} 标记步骤结束
 * </ol>
 *
 * <p>实现类可以跟踪步骤的“执行时间”或其他指标。
 *
 * @author Brian Clozel
 * @since 5.3
 */
public interface StartupStep {

	/**
	 * 返回启动步骤的名称。
	 * <p>步骤名称描述当前的动作或阶段。
	 * 该技术名称应采用“.”分隔的命名空间，可以重复用于描述应用启动期间其他类似步骤实例。
	 */
	String getName();

	/**
	 * 返回该步骤在应用启动过程中的唯一ID。
	 */
	long getId();

	/**
	 * 返回父步骤的ID（如果存在）。
	 * <p>父步骤是当前步骤创建时最近启动的步骤。
	 */
	@Nullable
	Long getParentId();

	/**
	 * 向步骤添加一个 {@link Tag}。
	 * @param key 标签键
	 * @param value 标签值
	 */
	StartupStep tag(String key, String value);

	/**
	 * 向步骤添加一个 {@link Tag}。
	 * @param key 标签键
	 * @param value 标签值的 {@link Supplier}
	 */
	StartupStep tag(String key, Supplier<String> value);

	/**
	 * 返回该步骤的 {@link Tag} 集合。
	 */
	Tags getTags();

	/**
	 * 记录步骤的状态及可能的其他指标，如执行时间。
	 * <p>一旦结束，步骤状态将不可再更改。
	 */
	void end();


	/**
	 * {@link Tag} 的不可变集合。
	 */
	interface Tags extends Iterable<Tag> {
	}


	/**
	 * 用于存储步骤元数据的简单键值对关联。
	 */
	interface Tag {

		/**
		 * 返回 {@code Tag} 的名称（键）。
		 */
		String getKey();

		/**
		 * 返回 {@code Tag} 的值。
		 */
		String getValue();
	}

}
