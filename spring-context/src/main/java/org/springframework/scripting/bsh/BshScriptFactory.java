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

package org.springframework.scripting.bsh;

import java.io.IOException;

import bsh.EvalError;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * BeanShell 脚本的 {@link org.springframework.scripting.ScriptFactory} 实现。
 *
 * <p>通常与 {@link org.springframework.scripting.support.ScriptFactoryPostProcessor} 配合使用；
 * 请参阅后者的 javadoc 了解配置示例。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see BshScriptUtils
 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor
 */
public class BshScriptFactory implements ScriptFactory, BeanClassLoaderAware {

	private final String scriptSourceLocator;

	@Nullable
	private final Class<?>[] scriptInterfaces;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private Class<?> scriptClass;

	private final Object scriptClassMonitor = new Object();

	private boolean wasModifiedForTypeCheck = false;


	/**
	 * 为给定的脚本源创建一个新的 BshScriptFactory。
	 * <p>使用此 {@code BshScriptFactory} 变体时，脚本需要声明一个完整的类或返回脚本化对象的实际实例。
	 * @param scriptSourceLocator 指向脚本源的定位器。由实际创建脚本的后处理器解释。
	 */
	public BshScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = null;
	}

	/**
	 * 为给定的脚本源创建一个新的 BshScriptFactory。
	 * <p>脚本可以是需要生成相应代理（实现指定接口）的简单脚本，也可以声明一个完整的类或返回脚本化对象的实际实例
	 * （在这种情况下，指定的接口（如果有）需要由该类/实例实现）。
	 * @param scriptSourceLocator 指向脚本源的定位器。由实际创建脚本的后处理器解释。
	 * @param scriptInterfaces 脚本化对象应实现的 Java 接口（可以为 {@code null}）
	 */
	public BshScriptFactory(String scriptSourceLocator, @Nullable Class<?>... scriptInterfaces) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
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

	/**
	 * BeanShell 脚本确实需要配置接口。
	 */
	@Override
	public boolean requiresConfigInterface() {
		return true;
	}

	/**
	 * 通过 {@link BshScriptUtils} 加载并解析 BeanShell 脚本。
	 * @see BshScriptUtils#createBshObject(String, Class[], ClassLoader)
	 */
	@Override
	@Nullable
	public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		Class<?> clazz;

		try {
			synchronized (this.scriptClassMonitor) {
				boolean requiresScriptEvaluation = (this.wasModifiedForTypeCheck && this.scriptClass == null);
				this.wasModifiedForTypeCheck = false;

				if (scriptSource.isModified() || requiresScriptEvaluation) {
					// 新脚本内容：让我们检查它是否解析为一个 Class。
					Object result = BshScriptUtils.evaluateBshScript(
							scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
					if (result instanceof Class) {
						// 一个 Class：我们将在此处缓存这个 Class，并在同步块外创建实例。
						this.scriptClass = (Class<?>) result;
					}
					else {
						// 不是一个 Class：好的，我们将在后续每次调用时
						// 通过评估脚本简单地创建 BeanShell 对象。
						// 对于这个首次检查，我们只需返回已评估的对象。
						return result;
					}
				}
				clazz = this.scriptClass;
			}
		}
		catch (EvalError ex) {
			this.scriptClass = null;
			throw new ScriptCompilationException(scriptSource, ex);
		}

		if (clazz != null) {
			// 一个 Class：我们需要为每次调用创建一个实例。
			try {
				return ReflectionUtils.accessibleConstructor(clazz).newInstance();
			}
			catch (Throwable ex) {
				throw new ScriptCompilationException(
						scriptSource, "Could not instantiate script class: " + clazz.getName(), ex);
			}
		}
		else {
			// 不是一个 Class：我们需要为每次调用评估脚本。
			try {
				return BshScriptUtils.createBshObject(
						scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
			}
			catch (EvalError ex) {
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	@Nullable
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				if (scriptSource.isModified()) {
					// 新脚本内容：让我们检查它是否解析为一个 Class。
					this.wasModifiedForTypeCheck = true;
					this.scriptClass = BshScriptUtils.determineBshObjectType(
							scriptSource.getScriptAsString(), this.beanClassLoader);
				}
				return this.scriptClass;
			}
			catch (EvalError ex) {
				this.scriptClass = null;
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		synchronized (this.scriptClassMonitor) {
			return (scriptSource.isModified() || this.wasModifiedForTypeCheck);
		}
	}


	@Override
	public String toString() {
		return "BshScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}

}
