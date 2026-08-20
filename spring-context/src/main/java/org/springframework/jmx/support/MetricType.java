/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jmx.support;

/**
 * 表示 {@code ManagedMetric} 的测量值随时间变化的方式。
 *
 * @author Jennifer Hickey
 * @since 3.0
 */
public enum MetricType {

	/**
	 * 测量值可能随时间增加或减少。
	 */
	GAUGE,

	/**
	 * 测量值将始终增加。
	 */
	COUNTER

}
