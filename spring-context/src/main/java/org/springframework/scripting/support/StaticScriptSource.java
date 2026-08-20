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

package org.springframework.scripting.support;

import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.scripting.ScriptSource} 接口的静态实现，
 * 封装一个包含脚本源码文本的给定字符串。
 * 支持以编程方式更新脚本字符串。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class StaticScriptSource implements ScriptSource {

	private String script = "";

	private boolean modified;

	@Nullable
	private String className;


	/**
	 * 为给定的脚本创建一个新的 StaticScriptSource。
	 * @param script 脚本字符串
	 */
	public StaticScriptSource(String script) {
		setScript(script);
	}

	/**
	 * 为给定的脚本创建一个新的 StaticScriptSource。
	 * @param script 脚本字符串
	 * @param className 建议的脚本类名
	 * （可以为 {@code null}）
	 */
	public StaticScriptSource(String script, @Nullable String className) {
		setScript(script);
		this.className = className;
	}

	/**
	 * 设置新的脚本字符串，覆盖之前的脚本。
	 * @param script 脚本字符串
	 */
	public synchronized void setScript(String script) {
		Assert.hasText(script, "Script must not be empty");
		this.modified = !script.equals(this.script);
		this.script = script;
	}


	@Override
	public synchronized String getScriptAsString() {
		this.modified = false;
		return this.script;
	}

	@Override
	public synchronized boolean isModified() {
		return this.modified;
	}

	@Override
	@Nullable
	public String suggestedClassName() {
		return this.className;
	}


	@Override
	public String toString() {
		return "static script" + (this.className != null ? " [" + this.className + "]" : "");
	}

}
