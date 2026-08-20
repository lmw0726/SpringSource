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

import java.io.IOException;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Spring {@link ScriptEvaluator} 策略接口的基于 {@code javax.script} (JSR-223) 的实现。
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 * @see ScriptEngine#eval(String)
 */
public class StandardScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

	@Nullable
	private String engineName;

	@Nullable
	private volatile Bindings globalBindings;

	@Nullable
	private volatile ScriptEngineManager scriptEngineManager;


	/**
	 * 构造一个新的 {@code StandardScriptEvaluator}。
	 */
	public StandardScriptEvaluator() {
	}

	/**
	 * 为给定的类加载器构造一个新的 {@code StandardScriptEvaluator}。
	 * @param classLoader 用于脚本引擎检测的类加载器
	 */
	public StandardScriptEvaluator(ClassLoader classLoader) {
		this.scriptEngineManager = new ScriptEngineManager(classLoader);
	}

	/**
	 * 为给定的 JSR-223 {@link ScriptEngineManager} 构造一个新的 {@code StandardScriptEvaluator}，
	 * 用于获取脚本引擎。
	 * @param scriptEngineManager 要使用的 ScriptEngineManager（或其子类）
	 * @since 4.2.2
	 */
	public StandardScriptEvaluator(ScriptEngineManager scriptEngineManager) {
		this.scriptEngineManager = scriptEngineManager;
	}


	/**
	 * 设置用于执行脚本的语言名称（例如 "Groovy"）。
	 * <p>这实际上是 {@link #setEngineName "engineName"} 的别名，
	 * 潜在地（但尚未）提供某些语言的常用缩写，
	 * 超出 JSR-223 脚本引擎工厂所暴露的范围。
	 * @see #setEngineName
	 */
	public void setLanguage(String language) {
		this.engineName = language;
	}

	/**
	 * 设置用于执行脚本的脚本引擎名称（例如 "Groovy"），
	 * 由 JSR-223 脚本引擎工厂所暴露的名称。
	 * @since 4.2.2
	 * @see #setLanguage
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * 在底层脚本引擎管理器上设置全局作用域的绑定，
	 * 由所有脚本共享，作为脚本参数绑定的替代方案。
	 * @since 4.2.2
	 * @see #evaluate(ScriptSource, Map)
	 * @see javax.script.ScriptEngineManager#setBindings(Bindings)
	 * @see javax.script.SimpleBindings
	 */
	public void setGlobalBindings(Map<String, Object> globalBindings) {
		Bindings bindings = StandardScriptUtils.getBindings(globalBindings);
		this.globalBindings = bindings;
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager != null) {
			scriptEngineManager.setBindings(bindings);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			scriptEngineManager = new ScriptEngineManager(classLoader);
			this.scriptEngineManager = scriptEngineManager;
			Bindings bindings = this.globalBindings;
			if (bindings != null) {
				scriptEngineManager.setBindings(bindings);
			}
		}
	}


	@Override
	@Nullable
	public Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	@Nullable
	public Object evaluate(ScriptSource script, @Nullable Map<String, Object> argumentBindings) {
		ScriptEngine engine = getScriptEngine(script);
		try {
			if (CollectionUtils.isEmpty(argumentBindings)) {
				return engine.eval(script.getScriptAsString());
			}
			else {
				Bindings bindings = StandardScriptUtils.getBindings(argumentBindings);
				return engine.eval(script.getScriptAsString(), bindings);
			}
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script for ScriptEngine", ex);
		}
		catch (ScriptException ex) {
			throw new ScriptCompilationException(script, new StandardScriptEvalException(ex));
		}
	}

	/**
	 * 获取用于给定脚本的 JSR-223 ScriptEngine。
	 * @param script 要执行的脚本
	 * @return ScriptEngine（不为 {@code null}）
	 */
	protected ScriptEngine getScriptEngine(ScriptSource script) {
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			scriptEngineManager = new ScriptEngineManager();
			this.scriptEngineManager = scriptEngineManager;
		}

		if (StringUtils.hasText(this.engineName)) {
			return StandardScriptUtils.retrieveEngineByName(scriptEngineManager, this.engineName);
		}
		else if (script instanceof ResourceScriptSource) {
			Resource resource = ((ResourceScriptSource) script).getResource();
			String extension = StringUtils.getFilenameExtension(resource.getFilename());
			if (extension == null) {
				throw new IllegalStateException(
						"No script language defined, and no file extension defined for resource: " + resource);
			}
			ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
			if (engine == null) {
				throw new IllegalStateException("No matching engine found for file extension '" + extension + "'");
			}
			return engine;
		}
		else {
			throw new IllegalStateException(
					"No script language defined, and no resource associated with script: " + script);
		}
	}

}
