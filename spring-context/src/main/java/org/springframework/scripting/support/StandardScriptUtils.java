/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

/**
 * 处理 JSR-223 {@link ScriptEngine} 的通用操作。
 *
 * @author Juergen Hoeller
 * @since 4.2.2
 */
public abstract class StandardScriptUtils {

	/**
	 * 根据名称从给定的 {@link ScriptEngineManager} 中检索
	 * {@link ScriptEngine}，委托给 {@link ScriptEngineManager#getEngineByName}，
	 * 但在未找到或初始化失败时抛出描述性异常。
	 * @param scriptEngineManager 要使用的 ScriptEngineManager
	 * @param engineName 引擎名称
	 * @return 对应的 ScriptEngine（不为 {@code null}）
	 * @throws IllegalArgumentException 如果未找到匹配的引擎
	 * @throws IllegalStateException 如果所需引擎初始化失败
	 */
	public static ScriptEngine retrieveEngineByName(ScriptEngineManager scriptEngineManager, String engineName) {
		ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
		if (engine == null) {
			Set<String> engineNames = new LinkedHashSet<>();
			for (ScriptEngineFactory engineFactory : scriptEngineManager.getEngineFactories()) {
				List<String> factoryNames = engineFactory.getNames();
				if (factoryNames.contains(engineName)) {
					// 特殊情况：getEngineByName 返回 null 但引擎实际存在...
					// 假设初始化失败（ScriptEngineManager 会静默吞掉此类异常）。
					// 如果现在恰好初始化成功了，那没问题，但我们预期会抛出异常。
					try {
						engine = engineFactory.getScriptEngine();
						engine.setBindings(scriptEngineManager.getBindings(), ScriptContext.GLOBAL_SCOPE);
					}
					catch (Throwable ex) {
						throw new IllegalStateException("Script engine with name '" + engineName +
								"' failed to initialize", ex);
					}
				}
				engineNames.addAll(factoryNames);
			}
			throw new IllegalArgumentException("Script engine with name '" + engineName +
					"' not found; registered engine names: " + engineNames);
		}
		return engine;
	}

	static Bindings getBindings(Map<String, Object> bindings) {
		return (bindings instanceof Bindings ? (Bindings) bindings : new SimpleBindings(bindings));
	}

}
