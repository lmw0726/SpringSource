/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scripting.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * 针对 Groovy 脚本的 {@link org.springframework.scripting.ScriptFactory} 实现。
 *
 * <p>通常与
 * {@link org.springframework.scripting.support.ScriptFactoryPostProcessor} 配合使用；
 * 配置示例请参见后者的 javadoc。
 *
 * <p>注意：Spring 4.0 支持 Groovy 1.8 及更高版本。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rod Johnson
 * @since 2.0
 * @see groovy.lang.GroovyClassLoader
 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor
 */
public class GroovyScriptFactory implements ScriptFactory, BeanFactoryAware, BeanClassLoaderAware {

	private final String scriptSourceLocator;

	@Nullable
	private GroovyObjectCustomizer groovyObjectCustomizer;

	@Nullable
	private CompilerConfiguration compilerConfiguration;

	@Nullable
	private GroovyClassLoader groovyClassLoader;

	@Nullable
	private Class<?> scriptClass;

	@Nullable
	private Class<?> scriptResultClass;

	@Nullable
	private CachedResultHolder cachedResult;

	private final Object scriptClassMonitor = new Object();

	private boolean wasModifiedForTypeCheck = false;


	/**
	 * 为给定的脚本源创建一个新的 GroovyScriptFactory。
	 * <p>此处无需指定脚本接口，因为
	 * Groovy 脚本自身会定义其 Java 接口。
	 * @param scriptSourceLocator 指向脚本源的定位器，
	 * 由实际创建脚本的后处理器进行解释。
	 */
	public GroovyScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
	}

	/**
	 * 为给定的脚本源创建一个新的 GroovyScriptFactory，
	 * 指定一个策略接口，可以创建自定义 MetaClass
	 * 以提供缺失的方法并以其他方式更改对象的行为。
	 * @param scriptSourceLocator 指向脚本源的定位器，
	 * 由实际创建脚本的后处理器进行解释。
	 * @param groovyObjectCustomizer 可以设置自定义元类
	 * 或对此工厂创建的 GroovyObject 进行其他更改的定制器
	 * （可能为 {@code null}）
	 * @see GroovyObjectCustomizer#customize
	 */
	public GroovyScriptFactory(String scriptSourceLocator, @Nullable GroovyObjectCustomizer groovyObjectCustomizer) {
		this(scriptSourceLocator);
		this.groovyObjectCustomizer = groovyObjectCustomizer;
	}

	/**
	 * 为给定的脚本源创建一个新的 GroovyScriptFactory，
	 * 指定一个策略接口，可以创建自定义 MetaClass
	 * 以提供缺失的方法并以其他方式更改对象的行为。
	 * @param scriptSourceLocator 指向脚本源的定位器，
	 * 由实际创建脚本的后处理器进行解释。
	 * @param compilerConfiguration 要应用于
	 * GroovyClassLoader 的自定义编译器配置（可能为 {@code null}）
	 * @since 4.3.3
	 * @see GroovyClassLoader#GroovyClassLoader(ClassLoader, CompilerConfiguration)
	 */
	public GroovyScriptFactory(String scriptSourceLocator, @Nullable CompilerConfiguration compilerConfiguration) {
		this(scriptSourceLocator);
		this.compilerConfiguration = compilerConfiguration;
	}

	/**
	 * 为给定的脚本源创建一个新的 GroovyScriptFactory，
	 * 指定一个策略接口，可以自定义底层 GroovyClassLoader 中
	 * Groovy 的编译过程。
	 * @param scriptSourceLocator 指向脚本源的定位器，
	 * 由实际创建脚本的后处理器进行解释。
	 * @param compilationCustomizers 要应用于
	 * GroovyClassLoader 编译器配置的一个或多个定制器
	 * @since 4.3.3
	 * @see CompilerConfiguration#addCompilationCustomizers
	 * @see org.codehaus.groovy.control.customizers.ImportCustomizer
	 */
	public GroovyScriptFactory(String scriptSourceLocator, CompilationCustomizer... compilationCustomizers) {
		this(scriptSourceLocator);
		if (!ObjectUtils.isEmpty(compilationCustomizers)) {
			this.compilerConfiguration = new CompilerConfiguration();
			this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
		}
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) beanFactory).ignoreDependencyType(MetaClass.class);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (classLoader instanceof GroovyClassLoader &&
				(this.compilerConfiguration == null ||
						((GroovyClassLoader) classLoader).hasCompatibleConfiguration(this.compilerConfiguration))) {
			this.groovyClassLoader = (GroovyClassLoader) classLoader;
		}
		else {
			this.groovyClassLoader = buildGroovyClassLoader(classLoader);
		}
	}

	/**
	 * 返回此脚本工厂使用的 GroovyClassLoader。
	 */
	public GroovyClassLoader getGroovyClassLoader() {
		synchronized (this.scriptClassMonitor) {
			if (this.groovyClassLoader == null) {
				this.groovyClassLoader = buildGroovyClassLoader(ClassUtils.getDefaultClassLoader());
			}
			return this.groovyClassLoader;
		}
	}

	/**
	 * 为给定的 {@code ClassLoader} 构建一个 {@link GroovyClassLoader}。
	 * @param classLoader 要为其构建 GroovyClassLoader 的 ClassLoader
	 * @since 4.3.3
	 */
	protected GroovyClassLoader buildGroovyClassLoader(@Nullable ClassLoader classLoader) {
		return (this.compilerConfiguration != null ?
				new GroovyClassLoader(classLoader, this.compilerConfiguration) : new GroovyClassLoader(classLoader));
	}


	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	/**
	 * Groovy 脚本自行确定其接口，
	 * 因此我们无需在此显式暴露接口。
	 * @return 始终为 {@code null}
	 */
	@Override
	@Nullable
	public Class<?>[] getScriptInterfaces() {
		return null;
	}

	/**
	 * Groovy 脚本不需要配置接口，
	 * 因为其 setter 方法已作为公共方法暴露。
	 */
	@Override
	public boolean requiresConfigInterface() {
		return false;
	}


	/**
	 * 通过 GroovyClassLoader 加载并解析 Groovy 脚本。
	 * @see groovy.lang.GroovyClassLoader
	 */
	@Override
	@Nullable
	public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				Class<?> scriptClassToExecute;
				this.wasModifiedForTypeCheck = false;

				if (this.cachedResult != null) {
					Object result = this.cachedResult.object;
					this.cachedResult = null;
					return result;
				}

				if (this.scriptClass == null || scriptSource.isModified()) {
					// 新的脚本内容...
					this.scriptClass = getGroovyClassLoader().parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

					if (Script.class.isAssignableFrom(this.scriptClass)) {
						// 一个 Groovy 脚本，可能在创建实例：让我们执行它。
						Object result = executeScript(scriptSource, this.scriptClass);
						this.scriptResultClass = (result != null ? result.getClass() : null);
						return result;
					}
					else {
						this.scriptResultClass = this.scriptClass;
					}
				}
				scriptClassToExecute = this.scriptClass;

				// 在 synchronized 块之外处理重新执行。
				return executeScript(scriptSource, scriptClassToExecute);
			}
			catch (CompilationFailedException ex) {
				this.scriptClass = null;
				this.scriptResultClass = null;
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
				if (this.scriptClass == null || scriptSource.isModified()) {
					// 新的脚本内容...
					this.wasModifiedForTypeCheck = true;
					this.scriptClass = getGroovyClassLoader().parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

					if (Script.class.isAssignableFrom(this.scriptClass)) {
						// 一个 Groovy 脚本，可能在创建实例：让我们执行它。
						Object result = executeScript(scriptSource, this.scriptClass);
						this.scriptResultClass = (result != null ? result.getClass() : null);
						this.cachedResult = new CachedResultHolder(result);
					}
					else {
						this.scriptResultClass = this.scriptClass;
					}
				}
				return this.scriptResultClass;
			}
			catch (CompilationFailedException ex) {
				this.scriptClass = null;
				this.scriptResultClass = null;
				this.cachedResult = null;
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


	/**
	 * 实例化给定的 Groovy 脚本类，并在必要时运行它。
	 * @param scriptSource 底层脚本的源
	 * @param scriptClass Groovy 脚本类
	 * @return 结果对象（脚本类的实例
	 * 或运行脚本实例的结果）
	 * @throws ScriptCompilationException 实例化失败时抛出
	 */
	@Nullable
	protected Object executeScript(ScriptSource scriptSource, Class<?> scriptClass) throws ScriptCompilationException {
		try {
			GroovyObject goo = (GroovyObject) ReflectionUtils.accessibleConstructor(scriptClass).newInstance();

			if (this.groovyObjectCustomizer != null) {
				// 允许元类和其他定制。
				this.groovyObjectCustomizer.customize(goo);
			}

			if (goo instanceof Script) {
				// 一个 Groovy 脚本，可能在创建实例：让我们执行它。
				return ((Script) goo).run();
			}
			else {
				// 脚本类的一个实例：直接返回。
				return goo;
			}
		}
		catch (NoSuchMethodException ex) {
			throw new ScriptCompilationException(
					"No default constructor on Groovy script class: " + scriptClass.getName(), ex);
		}
		catch (InstantiationException ex) {
			throw new ScriptCompilationException(
					scriptSource, "Unable to instantiate Groovy script class: " + scriptClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new ScriptCompilationException(
					scriptSource, "Could not access Groovy script constructor: " + scriptClass.getName(), ex);
		}
		catch (InvocationTargetException ex) {
			throw new ScriptCompilationException(
					"Failed to invoke Groovy script constructor: " + scriptClass.getName(), ex.getTargetException());
		}
	}


	@Override
	public String toString() {
		return "GroovyScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}


	/**
	 * 用于临时缓存结果对象的包装器。
	 */
	private static class CachedResultHolder {

		@Nullable
		public final Object object;

		public CachedResultHolder(@Nullable Object object) {
			this.object = object;
		}
	}

}
