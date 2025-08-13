/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cglib.core;

/**
 * CGLIB 的 {@link DefaultNamingPolicy} 的自定义扩展，
 * 将生成的类名中的标记由 "ByCGLIB" 修改为 "BySpringCGLIB"。
 *
 * <p>此改动主要是为了避免普通 CGLIB 版本（可能被其他库使用）
 * 与 Spring 内嵌版本之间的冲突，
 * 以防同一个类因为不同用途被多次代理时产生命名重复。
 *
 * @author Juergen Hoeller
 * @since 3.2.8
 */
public class SpringNamingPolicy extends DefaultNamingPolicy {

	public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

	@Override
	protected String getTag() {
		return "BySpringCGLIB";
	}

}
