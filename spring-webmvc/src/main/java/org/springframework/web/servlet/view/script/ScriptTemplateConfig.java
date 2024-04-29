/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.view.script;

import org.springframework.lang.Nullable;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import java.nio.charset.Charset;
import java.util.function.Supplier;

/**
 * 由在Web环境中自动查找JSR-223 {@link ScriptEngine} 的对象实现的接口，被 {@link ScriptTemplateView} 检测和使用。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public interface ScriptTemplateConfig {

	/**
	 * 返回视图要使用的 {@link ScriptEngine}。
	 */
	@Nullable
	ScriptEngine getEngine();

	/**
	 * 返回将用于实例化 {@link ScriptEngine} 的引擎供应商。
	 *
	 * @since 5.2
	 */
	@Nullable
	Supplier<ScriptEngine> getEngineSupplier();

	/**
	 * 返回将用于实例化 {@link ScriptEngine} 的引擎名称。
	 */
	@Nullable
	String getEngineName();

	/**
	 * 返回是否对所有线程使用共享引擎，还是为每个线程创建线程本地引擎实例。
	 */
	@Nullable
	Boolean isSharedEngine();

	/**
	 * 返回由脚本引擎加载的脚本（库或用户提供）。
	 */
	@Nullable
	String[] getScripts();

	/**
	 * 返回渲染函数所属的对象（可选）。
	 */
	@Nullable
	String getRenderObject();

	/**
	 * 返回渲染函数名称（可选）。如果未指定，脚本模板将使用 {@link ScriptEngine#eval(String, Bindings)} 进行评估。
	 */
	@Nullable
	String getRenderFunction();

	/**
	 * 返回响应要使用的内容类型。
	 *
	 * @since 4.2.1
	 */
	@Nullable
	String getContentType();

	/**
	 * 返回用于读取脚本和模板文件的字符集。
	 */
	@Nullable
	Charset getCharset();

	/**
	 * 返回通过Spring资源位置的资源加载器路径。
	 */
	@Nullable
	String getResourceLoaderPath();

}
