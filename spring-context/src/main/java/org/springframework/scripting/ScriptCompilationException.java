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

package org.springframework.scripting;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 脚本编译失败时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ScriptCompilationException extends NestedRuntimeException {

	@Nullable
	private final ScriptSource scriptSource;


	/**
	 * ScriptCompilationException 的构造方法。
	 * @param msg 详细消息
	 */
	public ScriptCompilationException(String msg) {
		super(msg);
		this.scriptSource = null;
	}

	/**
	 * ScriptCompilationException 的构造方法。
	 * @param msg 详细消息
	 * @param cause 根本原因（通常来自底层脚本编译器 API 的使用）
	 */
	public ScriptCompilationException(String msg, Throwable cause) {
		super(msg, cause);
		this.scriptSource = null;
	}

	/**
	 * ScriptCompilationException 的构造方法。
	 * @param scriptSource 有问题的脚本的来源
	 * @param msg 详细消息
	 * @since 4.2
	 */
	public ScriptCompilationException(ScriptSource scriptSource, String msg) {
		super("Could not compile " + scriptSource + ": " + msg);
		this.scriptSource = scriptSource;
	}

	/**
	 * ScriptCompilationException 的构造方法。
	 * @param scriptSource 有问题的脚本的来源
	 * @param cause 根本原因（通常来自底层脚本编译器 API 的使用）
	 */
	public ScriptCompilationException(ScriptSource scriptSource, Throwable cause) {
		super("Could not compile " + scriptSource, cause);
		this.scriptSource = scriptSource;
	}

	/**
	 * ScriptCompilationException 的构造方法。
	 * @param scriptSource 有问题的脚本的来源
	 * @param msg 详细消息
	 * @param cause 根本原因（通常来自底层脚本编译器 API 的使用）
	 */
	public ScriptCompilationException(ScriptSource scriptSource, String msg, Throwable cause) {
		super("Could not compile " + scriptSource + ": " + msg, cause);
		this.scriptSource = scriptSource;
	}


	/**
	 * 返回有问题的脚本的来源。
	 * @return 脚本来源，如果不可用则返回 {@code null}
	 */
	@Nullable
	public ScriptSource getScriptSource() {
		return this.scriptSource;
	}

}
