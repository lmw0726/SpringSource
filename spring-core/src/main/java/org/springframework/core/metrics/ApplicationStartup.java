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

/**
 * 使用 {@link StartupStep steps} 对应用启动阶段进行监控。
 * <p>核心容器及其基础设施组件可以使用 {@code ApplicationStartup}
 * 在应用启动期间标记步骤，并收集执行上下文或处理时间的数据。
 *
 * @author Brian Clozel
 * @since 5.3
 */
public interface ApplicationStartup {

	/**
	 * 默认的“无操作” {@code ApplicationStartup} 实现。
	 * <p>此变体设计为最小开销，不记录数据。
	 */
	ApplicationStartup DEFAULT = new DefaultApplicationStartup();

	/**
	 * 创建一个新步骤并标记其开始。
	 * <p>步骤名称描述当前操作或阶段。此技术名称应采用“.”命名空间，
	 * 并且可以复用于描述应用启动期间同一阶段的其他实例。
	 * @param name 步骤名称
	 */
	StartupStep start(String name);

}
