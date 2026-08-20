/*
 * Copyright 2002-2022 the original author or authors.
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
import java.lang.reflect.InvocationTargetException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 基于 JSR-223 脚本引擎抽象（Java 内置）的
 * {@link org.springframework.scripting.ScriptFactory} 实现。
 * 支持 JavaScript、Groovy、JRuby 及其他符合 JSR-223 规范的引擎。
 *
 * <p>通常与 {@link org.springframework.scripting.support.ScriptFactoryPostProcessor}
 * 配合使用；配置示例请参阅后者的 javadoc。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see ScriptFactoryPostProcessor
 */
public class StandardScriptFactory implements ScriptFactory, BeanClassLoaderAware {

	@Nullable
	private final String scriptEngineName;

	private final String scriptSourceLocator;

	@Nullable
	private final Class<?>[] scriptInterfaces;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private volatile ScriptEngine scriptEngine;


	/**
	 * 为给定的脚本源创建一个新的 StandardScriptFactory。
	 * @param scriptSourceLocator 指向脚本源的定位器。
	 * 由实际创建脚本的后处理器进行解释。
	 */
	public StandardScriptFactory(String scriptSourceLocator) {
		this(null, scriptSourceLocator, (Class<?>[]) null);
	}

	/**
	 * 为给定的脚本源创建一个新的 StandardScriptFactory。
	 * @param scriptSourceLocator 指向脚本源的定位器。
	 * 由实际创建脚本的后处理器进行解释。
	 * @param scriptInterfaces 脚本对象需要实现的 Java 接口
	 */
	public StandardScriptFactory(String scriptSourceLocator, Class<?>... scriptInterfaces) {
		this(null, scriptSourceLocator, scriptInterfaces);
	}

	/**
	 * 为给定的脚本源创建一个新的 StandardScriptFactory。
	 * @param scriptEngineName 要使用的 JSR-223 ScriptEngine 的名称
	 * （显式指定，而非从脚本源推断）
	 * @param scriptSourceLocator 指向脚本源的定位器。
	 * 由实际创建脚本的后处理器进行解释。
	 */
	public StandardScriptFactory(String scriptEngineName, String scriptSourceLocator) {
		this(scriptEngineName, scriptSourceLocator, (Class<?>[]) null);
	}

	/**
	 * 为给定的脚本源创建一个新的 StandardScriptFactory。
	 * @param scriptEngineName 要使用的 JSR-223 ScriptEngine 的名称
	 * （显式指定，而非从脚本源推断）
	 * @param scriptSourceLocator 指向脚本源的定位器。
	 * 由实际创建脚本的后处理器进行解释。
	 * @param scriptInterfaces 脚本对象需要实现的 Java 接口
	 */
	public StandardScriptFactory(
			@Nullable String scriptEngineName, String scriptSourceLocator, @Nullable Class<?>... scriptInterfaces) {

		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptEngineName = scriptEngineName;
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	@Override
	@Nullable
	public Class<?>[] getScriptInterfaces() {
		return this.scriptInterfaces;
	}

	@Override
	public boolean requiresConfigInterface() {
		return false;
	}


	/**
	 * 通过 JSR-223 的 ScriptEngine 加载并解析脚本。
	 */
	@Override
	@Nullable
	public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		Object script = evaluateScript(scriptSource);

		if (!ObjectUtils.isEmpty(actualInterfaces)) {
			boolean adaptationRequired = false;
			for (Class<?> requestedIfc : actualInterfaces) {
				if (script instanceof Class ? !requestedIfc.isAssignableFrom((Class<?>) script) :
						!requestedIfc.isInstance(script)) {
					adaptationRequired = true;
					break;
				}
			}
			if (adaptationRequired) {
				script = adaptToInterfaces(script, scriptSource, actualInterfaces);
			}
		}

		if (script instanceof Class) {
			Class<?> scriptClass = (Class<?>) script;
			try {
				return ReflectionUtils.accessibleConstructor(scriptClass).newInstance();
			}
			catch (NoSuchMethodException ex) {
				throw new ScriptCompilationException(
						"No default constructor on script class: " + scriptClass.getName(), ex);
			}
			catch (InstantiationException ex) {
				throw new ScriptCompilationException(
						scriptSource, "Unable to instantiate script class: " + scriptClass.getName(), ex);
			}
			catch (IllegalAccessException ex) {
				throw new ScriptCompilationException(
						scriptSource, "Could not access script constructor: " + scriptClass.getName(), ex);
			}
			catch (InvocationTargetException ex) {
				throw new ScriptCompilationException(
						"Failed to invoke script constructor: " + scriptClass.getName(), ex.getTargetException());
			}
		}

		return script;
	}

	protected Object evaluateScript(ScriptSource scriptSource) {
		try {
			ScriptEngine scriptEngine = this.scriptEngine;
			if (scriptEngine == null) {
				scriptEngine = retrieveScriptEngine(scriptSource);
				if (scriptEngine == null) {
					throw new IllegalStateException("Could not determine script engine for " + scriptSource);
				}
				this.scriptEngine = scriptEngine;
			}
			return scriptEngine.eval(scriptSource.getScriptAsString());
		}
		catch (Exception ex) {
			throw new ScriptCompilationException(scriptSource, ex);
		}
	}

	@Nullable
	protected ScriptEngine retrieveScriptEngine(ScriptSource scriptSource) {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager(this.beanClassLoader);

		if (this.scriptEngineName != null) {
			return StandardScriptUtils.retrieveEngineByName(scriptEngineManager, this.scriptEngineName);
		}

		if (scriptSource instanceof ResourceScriptSource) {
			String filename = ((ResourceScriptSource) scriptSource).getResource().getFilename();
			if (filename != null) {
				String extension = StringUtils.getFilenameExtension(filename);
				if (extension != null) {
					ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
					if (engine != null) {
						return engine;
					}
				}
			}
		}

		return null;
	}

	@Nullable
	protected Object adaptToInterfaces(
			@Nullable Object script, ScriptSource scriptSource, Class<?>... actualInterfaces) {

		Class<?> adaptedIfc;
		if (actualInterfaces.length == 1) {
			adaptedIfc = actualInterfaces[0];
		}
		else {
			adaptedIfc = ClassUtils.createCompositeInterface(actualInterfaces, this.beanClassLoader);
		}

		if (adaptedIfc != null) {
			ScriptEngine scriptEngine = this.scriptEngine;
			if (!(scriptEngine instanceof Invocable)) {
				throw new ScriptCompilationException(scriptSource,
						"ScriptEngine must implement Invocable in order to adapt it to an interface: " + scriptEngine);
			}
			Invocable invocable = (Invocable) scriptEngine;
			if (script != null) {
				script = invocable.getInterface(script, adaptedIfc);
			}
			if (script == null) {
				script = invocable.getInterface(adaptedIfc);
				if (script == null) {
					throw new ScriptCompilationException(scriptSource,
							"Could not adapt script to interface [" + adaptedIfc.getName() + "]");
				}
			}
		}

		return script;
	}

	@Override
	@Nullable
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		return null;
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		return scriptSource.isModified();
	}


	@Override
	public String toString() {
		return "StandardScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}

}
