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

package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 用于测试目的的简单{@link ConfigurableEnvironment}实现，暴露了{@link #setProperty(String, String)}和{@link #withProperty(String, String)}方法。
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @see org.springframework.mock.env.MockPropertySource
 * @since 3.2
 */
public class MockEnvironment extends AbstractEnvironment {

	/**
	 * 模拟属性源
	 */
	private MockPropertySource propertySource = new MockPropertySource();

	/**
	 * 创建一个带有单个{@link MockPropertySource}的新{@code MockEnvironment}。
	 */
	public MockEnvironment() {
		getPropertySources().addLast(this.propertySource);
	}

	/**
	 * 在此环境的基础{@link MockPropertySource}上设置属性。
	 */
	public void setProperty(String key, String value) {
		this.propertySource.setProperty(key, value);
	}

	/**
	 * {@link #setProperty}的方便同义词，返回当前实例。
	 * 用于方法链接和流畅式使用。
	 *
	 * @return 此{@link MockEnvironment}实例
	 * @see MockPropertySource#withProperty
	 */
	public MockEnvironment withProperty(String key, String value) {
		this.setProperty(key, value);
		return this;
	}

}
