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

package org.springframework.jmx.support;

/**
 * 当尝试注册一个已存在的 MBean 时，指示注册行为。
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @since 3.2
 */
public enum RegistrationPolicy {

	/**
	 * 当尝试在一个已存在的名称下注册 MBean 时，注册应失败。
	 */
	FAIL_ON_EXISTING,

	/**
	 * 当尝试在一个已存在的名称下注册 MBean 时，注册应忽略受影响的 MBean。
	 */
	IGNORE_EXISTING,

	/**
	 * 当尝试在一个已存在的名称下注册 MBean 时，注册应替换受影响的 MBean。
	 */
	REPLACE_EXISTING

}
