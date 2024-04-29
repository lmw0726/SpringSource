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
 * 用于在Web应用程序中创建 {@code ScriptEngine} 的Spring MVC的 {@link ScriptTemplateConfig} 实现。
 *
 * <pre class="code">
 * // 将以下内容添加到 @Configuration 类中
 * &#64;Bean
 * public ScriptTemplateConfigurer mustacheConfigurer() {
 *    ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
 *    configurer.setEngineName("nashorn");
 *    configurer.setScripts("mustache.js");
 *    configurer.setRenderObject("Mustache");
 *    configurer.setRenderFunction("render");
 *    return configurer;
 * }
 * </pre>
 *
 * <p><b>注意:</b> 通过将 {@link #setSharedEngine sharedEngine} 属性设置为 {@code false}，可以使用非线程安全的脚本引擎，
 * 与 Handlebars 或 React 等不设计并发性的模板库一起使用，例如运行在 Nashorn 上的脚本引擎。
 *
 * @author Sebastien Deleuze
 * @see ScriptTemplateView
 * @since 4.2
 */
public class ScriptTemplateConfigurer implements ScriptTemplateConfig {

	/**
	 * 脚本引擎
	 */
	@Nullable
	private ScriptEngine engine;

	/**
	 * 脚本引擎供应者
	 */
	@Nullable
	private Supplier<ScriptEngine> engineSupplier;

	/**
	 * 引擎名称
	 */
	@Nullable
	private String engineName;

	/**
	 * 当设置为 {@code false} 时，使用线程局部 {@link ScriptEngine} 实例，而不是一个单一的共享实例。
	 */
	@Nullable
	private Boolean sharedEngine;

	/**
	 * 脚本引擎加载的脚本
	 */
	@Nullable
	private String[] scripts;

	/**
	 * 渲染函数所属的对象
	 */
	@Nullable
	private String renderObject;

	/**
	 * 渲染函数名称
	 */
	@Nullable
	private String renderFunction;

	/**
	 * 响应要使用的内容类型
	 */
	@Nullable
	private String contentType;

	/**
	 * 用于读取脚本和模板文件的字符集
	 */
	@Nullable
	private Charset charset;

	/**
	 * 通过 Spring 资源位置的资源加载器路径
	 */
	@Nullable
	private String resourceLoaderPath;


	/**
	 * 默认构造函数。
	 */
	public ScriptTemplateConfigurer() {
	}

	/**
	 * 使用给定的引擎名称创建新的 ScriptTemplateConfigurer。
	 */
	public ScriptTemplateConfigurer(String engineName) {
		this.engineName = engineName;
	}


	/**
	 * 设置视图要使用的 {@link ScriptEngine}。
	 * 如果指定了 {@code renderFunction}，则脚本引擎必须实现 {@code Invocable}。
	 * 你必须定义 {@code engine} 或 {@code engineName}，而不能同时定义两者。
	 * <p>当 {@code sharedEngine} 标志设置为 {@code false} 时，你不应该使用此 setter 指定脚本引擎，
	 * 而应该使用 {@link #setEngineName(String)} 或 {@link #setEngineSupplier(Supplier)}，
	 * 因为它暗示了脚本引擎的多次惰性实例化。
	 *
	 * @see #setEngineName(String)
	 * @see #setEngineSupplier(Supplier)
	 */
	public void setEngine(@Nullable ScriptEngine engine) {
		this.engine = engine;
	}

	@Override
	@Nullable
	public ScriptEngine getEngine() {
		return this.engine;
	}

	/**
	 * 设置视图要使用的 {@link ScriptEngine} 供应商，通常与 {@link #setSharedEngine(Boolean)} 设置为 {@code false} 一起使用。
	 * 如果指定了 {@code renderFunction}，则脚本引擎必须实现 {@code Invocable}。
	 * 你必须定义 {@code engineSupplier}、{@code engine} 或 {@code engineName}。
	 *
	 * @see #setEngine(ScriptEngine)
	 * @see #setEngineName(String)
	 * @since 5.2
	 */
	public void setEngineSupplier(@Nullable Supplier<ScriptEngine> engineSupplier) {
		this.engineSupplier = engineSupplier;
	}

	@Override
	@Nullable
	public Supplier<ScriptEngine> getEngineSupplier() {
		return this.engineSupplier;
	}

	/**
	 * 设置将用于实例化 {@link ScriptEngine} 的引擎名称。
	 * 如果指定了 {@code renderFunction}，则脚本引擎必须实现 {@code Invocable}。
	 * 你必须定义 {@code engine} 或 {@code engineName}，而不能同时定义两者。
	 *
	 * @see #setEngine(ScriptEngine)
	 * @see #setEngineSupplier(Supplier)
	 */
	public void setEngineName(@Nullable String engineName) {
		this.engineName = engineName;
	}

	@Override
	@Nullable
	public String getEngineName() {
		return this.engineName;
	}

	/**
	 * 当设置为 {@code false} 时，使用线程局部 {@link ScriptEngine} 实例，而不是一个单一的共享实例。
	 * 当使用不设计并发性的模板库，如运行在 Nashorn 上的 Handlebars 或 React 时，
	 * 应将此标志设置为 {@code false}。
	 * <p>当此标志设置为 {@code false} 时，无法使用 {@link #setEngine(ScriptEngine)} 指定脚本引擎，
	 * 因为需要惰性地创建多个脚本引擎实例（每个线程一个）。
	 *
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/javax/script/ScriptEngineFactory.html#getParameter-java.lang.String-">THREADING ScriptEngine parameter</a>
	 */
	public void setSharedEngine(@Nullable Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	@Override
	@Nullable
	public Boolean isSharedEngine() {
		return this.sharedEngine;
	}

	/**
	 * 设置要由脚本引擎加载的脚本（库或用户提供）。
	 * 由于 {@code resourceLoaderPath} 的默认值是 "classpath:"，你可以轻松地加载类路径上可用的任何脚本。
	 * <p>例如，为了使用作为 WebJars 依赖项的 JavaScript 库和自定义的 "render.js" 文件，你应该调用
	 * {@code configurer.setScripts("/META-INF/resources/webjars/library/version/library.js",
	 * "com/myproject/script/render.js");}。
	 *
	 * @see #setResourceLoaderPath
	 * @see <a href="https://www.webjars.org">WebJars</a>
	 */
	public void setScripts(@Nullable String... scriptNames) {
		this.scripts = scriptNames;
	}

	@Override
	@Nullable
	public String[] getScripts() {
		return this.scripts;
	}

	/**
	 * 设置渲染函数所属的对象（可选）。
	 * 例如，为了调用 {@code Mustache.render()}，{@code renderObject} 应该设置为 {@code "Mustache"}，
	 * {@code renderFunction} 应该设置为 {@code "render"}。
	 */
	public void setRenderObject(@Nullable String renderObject) {
		this.renderObject = renderObject;
	}

	@Override
	@Nullable
	public String getRenderObject() {
		return this.renderObject;
	}

	/**
	 * 设置渲染函数名称（可选）。如果未指定，脚本模板将使用 {@link ScriptEngine#eval(String, Bindings)} 进行评估。
	 * <p>此函数将使用以下参数调用：
	 * <ol>
	 * <li>{@code String template}：模板内容</li>
	 * <li>{@code Map model}：视图模型</li>
	 * <li>{@code RenderingContext context}：渲染上下文（自 5.0 开始）</li>
	 * </ol>
	 *
	 * @see RenderingContext
	 */
	public void setRenderFunction(@Nullable String renderFunction) {
		this.renderFunction = renderFunction;
	}

	@Override
	@Nullable
	public String getRenderFunction() {
		return this.renderFunction;
	}

	/**
	 * 设置响应要使用的内容类型。
	 * （默认为 {@code text/html}）。
	 *
	 * @since 4.2.1
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回响应要使用的内容类型。
	 *
	 * @since 4.2.1
	 */
	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置用于读取脚本和模板文件的字符集。
	 * （默认为 {@code UTF-8}）。
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	@Override
	@Nullable
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 设置通过 Spring 资源位置的资源加载器路径。
	 * 接受多个位置作为逗号分隔的路径列表。
	 * 支持标准的 URL，如 "file:" 和 "classpath:"，以及伪 URL，
	 * 此处的伪 URL 由 Spring 的 {@link org.springframework.core.io.ResourceLoader} 理解。
	 * 当在 ApplicationContext 中运行时，允许相对路径。
	 * <p>默认为 "classpath:"。
	 */
	public void setResourceLoaderPath(@Nullable String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	@Override
	@Nullable
	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

}
