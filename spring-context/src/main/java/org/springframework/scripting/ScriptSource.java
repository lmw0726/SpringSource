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

package org.springframework.scripting;

import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * 定义脚本源的接口。
 * 跟踪底层脚本是否已被修改。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface ScriptSource {

	/**
	 * 以字符串形式检索当前脚本源文本。
	 * @return 脚本文本
	 * @throws IOException 如果脚本检索失败
	 */
	String getScriptAsString() throws IOException;

	/**
	 * 指示自上次调用 {@link #getScriptAsString()} 以来，
	 * 底层脚本数据是否已被修改。
	 * 如果脚本尚未被读取，则返回 {@code true}。
	 * @return 脚本数据是否已被修改
	 */
	boolean isModified();

	/**
	 * 确定底层脚本的类名。
	 * @return 建议的类名，如果没有可用的则返回 {@code null}
	 */
	@Nullable
	String suggestedClassName();

}
