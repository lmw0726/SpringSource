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

package org.springframework.web.reactive.result.view.script;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.scripting.support.StandardScriptEvalException;
import org.springframework.scripting.support.StandardScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.script.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ScriptTemplateView 的子类，设计用于运行基于 JSR-223 脚本引擎的任何模板库。
 *
 * <p>如果未设置，则通过查找 Web 应用程序上下文中的单个 {@link ScriptTemplateConfig} bean 来自动检测每个属性，
 * 并使用它来获取配置的属性。
 *
 * <p>Nashorn JavaScript 引擎需要 Java 8+，并且可能需要将 {@code sharedEngine} 属性设置为 {@code false} 才能正常运行。
 * 有关详细信息，请参见 {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)}。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 * @since 5.0
 */
public class ScriptTemplateView extends AbstractUrlBasedView {
	/**
	 * 默认资源加载器路径为classpath:
	 */
	private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";

	/**
	 * 脚本引擎
	 */
	@Nullable
	private ScriptEngine engine;
	/**
	 * 脚本引擎供应商
	 */
	@Nullable
	private Supplier<ScriptEngine> engineSupplier;
	/**
	 * 脚本引擎名称
	 */
	@Nullable
	private String engineName;
	/**
	 * 共享脚本引擎
	 */
	@Nullable
	private Boolean sharedEngine;
	/**
	 * 脚本
	 */
	@Nullable
	private String[] scripts;

	/**
	 * 渲染对象和函数
	 */
	@Nullable
	private String renderObject;
	/**
	 * 渲染函数
	 */
	@Nullable
	private String renderFunction;

	/**
	 * 资源加载器路径
	 */
	@Nullable
	private String[] resourceLoaderPaths;

	/**
	 * 脚本引擎管理器
	 */
	@Nullable
	private volatile ScriptEngineManager scriptEngineManager;

	/**
	 * 作为 bean 使用的构造方法。
	 *
	 * @see #setUrl
	 */
	public ScriptTemplateView() {
	}

	/**
	 * 使用给定的 URL 创建一个新的 ScriptTemplateView。
	 */
	public ScriptTemplateView(String url) {
		super(url);
	}


	/**
	 * 参见 {@link ScriptTemplateConfigurer#setEngine(ScriptEngine)} 文档。
	 */
	public void setEngine(ScriptEngine engine) {
		this.engine = engine;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setEngineSupplier(Supplier)} 文档。
	 *
	 * @since 5.2
	 */
	public void setEngineSupplier(Supplier<ScriptEngine> engineSupplier) {
		this.engineSupplier = engineSupplier;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setEngineName(String)} 文档。
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} 文档。
	 */
	public void setSharedEngine(Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setScripts(String...)} 文档。
	 */
	public void setScripts(String... scripts) {
		this.scripts = scripts;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setRenderObject(String)} 文档。
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setRenderFunction(String)} 文档。
	 */
	public void setRenderFunction(String functionName) {
		this.renderFunction = functionName;
	}

	/**
	 * 参见 {@link ScriptTemplateConfigurer#setResourceLoaderPath(String)} 文档。
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		// 将逗号分隔的资源加载器路径转换为数组
		String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);

		// 创建资源加载器路径数组，长度为传入路径数组长度加一
		this.resourceLoaderPaths = new String[paths.length + 1];

		// 设置第一个路径为空字符串
		this.resourceLoaderPaths[0] = "";

		// 循环遍历传入的路径数组
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			// 如果路径不是以斜杠或冒号结尾，则添加斜杠
			if (!path.endsWith("/") && !path.endsWith(":")) {
				path = path + "/";
			}
			// 将处理后的路径放入资源加载器路径数组中
			this.resourceLoaderPaths[i + 1] = path;
		}
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext context) {
		super.setApplicationContext(context);

		// 检测并设置脚本模板配置
		ScriptTemplateConfig viewConfig = autodetectViewConfig();

		// 如果当前实例的引擎为null，且视图配置中引擎不为null，则使用视图配置中的引擎
		if (this.engine == null && viewConfig.getEngine() != null) {
			this.engine = viewConfig.getEngine();
		}

		// 如果当前实例的引擎供应商为null，且视图配置中引擎供应商不为null，则使用视图配置中的引擎供应商
		if (this.engineSupplier == null && viewConfig.getEngineSupplier() != null) {
			this.engineSupplier = viewConfig.getEngineSupplier();
		}

		// 如果当前实例的引擎名称为null，且视图配置中引擎名称不为null，则使用视图配置中的引擎名称
		if (this.engineName == null && viewConfig.getEngineName() != null) {
			this.engineName = viewConfig.getEngineName();
		}

		// 如果当前实例的脚本数组为null，且视图配置中脚本数组不为null，则使用视图配置中的脚本数组
		if (this.scripts == null && viewConfig.getScripts() != null) {
			this.scripts = viewConfig.getScripts();
		}

		// 如果当前实例的渲染对象为null，且视图配置中渲染对象不为null，则使用视图配置中的渲染对象
		if (this.renderObject == null && viewConfig.getRenderObject() != null) {
			this.renderObject = viewConfig.getRenderObject();
		}

		// 如果当前实例的渲染函数为null，且视图配置中渲染函数不为null，则使用视图配置中的渲染函数
		if (this.renderFunction == null && viewConfig.getRenderFunction() != null) {
			this.renderFunction = viewConfig.getRenderFunction();
		}

		// 如果视图配置中指定了字符集，则设置默认字符集
		if (viewConfig.getCharset() != null) {
			setDefaultCharset(viewConfig.getCharset());
		}

		// 如果当前实例的资源加载器路径数组为null，则设置资源加载器路径数组
		if (this.resourceLoaderPaths == null) {
			String resourceLoaderPath = viewConfig.getResourceLoaderPath();
			setResourceLoaderPath(resourceLoaderPath != null ? resourceLoaderPath : DEFAULT_RESOURCE_LOADER_PATH);
		}

		// 如果当前实例的共享引擎属性为null，且视图配置中共享引擎属性不为null，则使用视图配置中的共享引擎属性
		if (this.sharedEngine == null && viewConfig.isSharedEngine() != null) {
			this.sharedEngine = viewConfig.isSharedEngine();
		}

		// 统计设置的引擎数量，确保只有一个引擎被设置
		int engineCount = 0;
		if (this.engine != null) {
			engineCount++;
		}
		if (this.engineSupplier != null) {
			engineCount++;
		}
		if (this.engineName != null) {
			engineCount++;
		}
		Assert.isTrue(engineCount == 1,
				"You should define either 'engine', 'engineSupplier' or 'engineName'.");

		// 当共享引擎属性为false时，确保引擎为null
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			Assert.isTrue(this.engine == null,
					"When 'sharedEngine' is set to false, you should specify the " +
							"script engine using 'engineName' or 'engineSupplier' , not 'engine'.");
		}
		// 当引擎不为null时，加载脚本
		else if (this.engine != null) {
			loadScripts(this.engine);
		}
		// 当引擎名称不为null时，创建引擎并设置
		else if (this.engineName != null) {
			setEngine(createEngineFromName(this.engineName));
		}
		// 其他情况下，创建引擎并设置
		else {
			setEngine(createEngineFromSupplier());
		}

		// 当渲染函数不为null，且引擎不为null时，确保引擎实现Invocable接口
		if (this.renderFunction != null && this.engine != null) {
			Assert.isInstanceOf(Invocable.class, this.engine,
					"ScriptEngine must implement Invocable when 'renderFunction' is specified");
		}
	}

	/**
	 * 获取引擎。
	 * 如果 sharedEngine 属性为 false，则根据 engineName 或 supplier 创建引擎；
	 * 否则，返回已有的共享引擎。
	 */
	protected ScriptEngine getEngine() {
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			// 当共享引擎设置为false时，根据引擎名称或供应商创建引擎
			if (this.engineName != null) {
				return createEngineFromName(this.engineName);
			} else {
				return createEngineFromSupplier();
			}
		} else {
			// 当共享引擎设置为true时，确保引擎不为null
			Assert.state(this.engine != null, "No shared engine available");
			return this.engine;
		}
	}

	/**
	 * 根据引擎名称创建脚本引擎
	 *
	 * @param engineName 引擎名称
	 * @return 脚本引擎
	 */
	protected ScriptEngine createEngineFromName(String engineName) {
		// 获取已存在的 ScriptEngineManager 或创建一个新的
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			scriptEngineManager = new ScriptEngineManager(obtainApplicationContext().getClassLoader());
			this.scriptEngineManager = scriptEngineManager;
		}

		// 根据引擎名称检索 ScriptEngine 并加载脚本
		ScriptEngine engine = StandardScriptUtils.retrieveEngineByName(scriptEngineManager, engineName);
		loadScripts(engine);
		return engine;
	}

	/**
	 * 从供应者创建脚本引擎
	 *
	 * @return 脚本引擎
	 */
	private ScriptEngine createEngineFromSupplier() {
		Assert.state(this.engineSupplier != null, "No engine supplier available");
		ScriptEngine engine = this.engineSupplier.get();
		if (this.renderFunction != null) {
			Assert.isInstanceOf(Invocable.class, engine,
					"ScriptEngine must implement Invocable when 'renderFunction' is specified");
		}
		//使用脚本引擎加载脚本
		loadScripts(engine);
		return engine;
	}

	/**
	 * 使用脚本引擎加载脚本
	 *
	 * @param engine 加载脚本
	 */
	protected void loadScripts(ScriptEngine engine) {
		// 如果脚本数组不为空，加载每个脚本
		if (!ObjectUtils.isEmpty(this.scripts)) {
			for (String script : this.scripts) {
				// 获取资源并验证其存在性
				Resource resource = getResource(script);
				if (resource == null) {
					throw new IllegalStateException("Script resource [" + script + "] not found");
				}
				try {
					// 通过 ScriptEngine 执行脚本
					engine.eval(new InputStreamReader(resource.getInputStream()));
				} catch (Throwable ex) {
					throw new IllegalStateException("Failed to evaluate script [" + script + "]", ex);
				}
			}
		}
	}

	@Nullable
	protected Resource getResource(String location) {
		// 如果资源加载路径数组不为空，遍历路径
		if (this.resourceLoaderPaths != null) {
			for (String path : this.resourceLoaderPaths) {
				// 获取资源并验证其存在性
				Resource resource = obtainApplicationContext().getResource(path + location);
				if (resource.exists()) {
					return resource;
				}
			}
		}
		return null;
	}

	/**
	 * 自动检测视图配置。
	 * 尝试获取当前 Web 应用程序上下文或父根上下文中的 ScriptTemplateConfig bean。
	 * 如果找不到，抛出 ApplicationContextException 异常。
	 */
	protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), ScriptTemplateConfig.class, true, false);
		} catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
					"web application context or the parent root context: ScriptTemplateConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return (getResource(url) != null);
	}


	/**
	 * 渲染视图
	 *
	 * @param model       组合输出映射（永不为{@code null}），其中动态值优先于静态属性
	 * @param contentType 选择用于渲染的内容类型，应与{@linkplain #getSupportedMediaTypes()支持的媒体类型}之一匹配
	 * @param exchange    当前交换对象
	 * @return 无返回值
	 */
	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		return exchange.getResponse().writeWith(Mono.fromCallable(() -> {
			try {
				// 获取引擎和 URL
				ScriptEngine engine = getEngine();
				String url = getUrl();
				Assert.state(url != null, "'url' not set");

				// 获取模板内容
				String template = getTemplate(url);

				// 模板加载器，用于加载模板
				Function<String, String> templateLoader = path -> {
					try {
						return getTemplate(path);
					} catch (IOException ex) {
						throw new IllegalStateException(ex);
					}
				};

				// 获取区域设置和渲染上下文
				Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());
				RenderingContext context = new RenderingContext(
						obtainApplicationContext(), locale, templateLoader, url);

				Object html;
				if (this.renderFunction == null) {
					// 若渲染函数为空，则使用引擎执行模板绑定的数据
					SimpleBindings bindings = new SimpleBindings();
					bindings.putAll(model);
					model.put("renderingContext", context);
					html = engine.eval(template, bindings);
				} else if (this.renderObject != null) {
					// 若有渲染对象，则调用渲染函数
					Object thiz = engine.eval(this.renderObject);
					html = ((Invocable) engine).invokeMethod(thiz, this.renderFunction, template, model, context);
				} else {
					// 调用渲染函数
					html = ((Invocable) engine).invokeFunction(this.renderFunction, template, model, context);
				}

				// 转换 HTML 为字节数组
				byte[] bytes = String.valueOf(html).getBytes(StandardCharsets.UTF_8);
				// 包装成响应流
				return exchange.getResponse().bufferFactory().wrap(bytes);
			} catch (ScriptException ex) {
				throw new IllegalStateException("Failed to render script template", new StandardScriptEvalException(ex));
			} catch (Exception ex) {
				throw new IllegalStateException("Failed to render script template", ex);
			}
		}));
	}

	/**
	 * 获取模板内容
	 *
	 * @param path 资源路径
	 * @return 模板内容
	 * @throws IOException IO异常
	 */
	protected String getTemplate(String path) throws IOException {
		Resource resource = getResource(path);
		if (resource == null) {
			throw new IllegalStateException("Template resource [" + path + "] not found");
		}
		InputStreamReader reader = new InputStreamReader(resource.getInputStream(), getDefaultCharset());
		return FileCopyUtils.copyToString(reader);
	}

}
