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

package org.springframework.aop;

import java.io.Serializable;

/**
 * 匹配所有类的规范 ClassFilter 实例。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
final class TrueClassFilter implements ClassFilter, Serializable {

	public static final TrueClassFilter INSTANCE = new TrueClassFilter();

	/**
	 * 强制执行单例模式。
	 */
	private TrueClassFilter() {
	}

	@Override
	public boolean matches(Class<?> clazz) {
		return true;
	}

	/**
	 * 支持序列化所必需。在反序列化时替换为规范实例，
	 * 从而保护单例模式。可作为重写 {@code equals()} 的替代方案。
	 */
	private Object readResolve() {
		return INSTANCE;
	}

	@Override
	public String toString() {
		return "ClassFilter.TRUE";
	}

}
