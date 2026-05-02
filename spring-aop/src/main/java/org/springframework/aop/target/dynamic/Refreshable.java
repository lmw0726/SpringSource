/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.target.dynamic;

/**
 * 由动态目标对象实现的接口，
 * 支持重新加载以及可选地轮询更新。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 2.0
 */
public interface Refreshable {

	/**
	 * 刷新底层目标对象。
	 */
	void refresh();

	/**
	 * 返回自启动以来的实际刷新次数。
	 */
	long getRefreshCount();

	/**
	 * 返回最后一次实际刷新发生的时间（作为时间戳）。
	 */
	long getLastRefreshTime();

}
