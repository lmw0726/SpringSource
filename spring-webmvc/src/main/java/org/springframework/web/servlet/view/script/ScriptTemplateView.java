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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.scripting.support.StandardScriptEvalException;
import org.springframework.scripting.support.StandardScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.script.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link AbstractUrlBasedView}的子类，设计用于运行基于JSR-223脚本引擎的任何模板库。
 *
 * <p>如果未设置，则通过查找Web应用程序上下文中的单个{@link ScriptTemplateConfig} bean，并使用它来获取配置的属性来自动检测每个属性。
 *
 * <p>Nashorn JavaScript引擎需要Java 8+，并且可能需要将{@code sharedEngine}属性设置为{@code false}才能正常运行。有关更多详细信息，请参见{@link ScriptTemplateConfigurer#setSharedEngine(Boolean)}。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 * @since 4.2
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

	/**
	 * 视图的默认内容类型。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "text/html";
	/**
	 * 默认的字符编码
	 */
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * 默认资源加载路径
	 */
	private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";

	/**
	 * 引擎持有者
	 */
	private static final ThreadLocal<Map<Object, ScriptEngine>> enginesHolder =
			new NamedThreadLocal<>("ScriptTemplateView engines");

	/**
	 * 脚本引擎
	 */
	@Nullable
	private ScriptEngine engine;

	/**
	 * 引擎提供方
	 */
	@Nullable
	private Supplier<ScriptEngine> engineSupplier;

	/**
	 * 引擎名称
	 */
	@Nullable
	private String engineName;

	/**
	 * 是否是共享引擎
	 */
	@Nullable
	private Boolean sharedEngine;

	/**
	 * 脚本数组
	 */
	@Nullable
	private String[] scripts;

	/**
	 * 渲染的对象
	 */
	@Nullable
	private String renderObject;

	/**
	 * 渲染函数
	 */
	@Nullable
	private String renderFunction;

	/**
	 * 字符编码
	 */
	@Nullable
	private Charset charset;

	/**
	 * 资源加载路径数组
	 */
	@Nullable
	private String[] resourceLoaderPaths;

	/**
	 * 脚本引擎管理器
	 */
	@Nullable
	private volatile ScriptEngineManager scriptEngineManager;


	/**
	 * 用作bean的构造函数。
	 *
	 * @see #setUrl
	 */
	public ScriptTemplateView() {
		setContentType(null);
	}

	/**
	 * 使用给定的URL创建一个新的ScriptTemplateView。
	 *
	 * @since 4.2.1
	 */
	public ScriptTemplateView(String url) {
		super(url);
		setContentType(null);
	}


	/**
	 * 参见{@link ScriptTemplateConfigurer#setEngine(ScriptEngine)}文档。
	 */
	public void setEngine(ScriptEngine engine) {
		this.engine = engine;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setEngineSupplier(Supplier)}文档。
	 *
	 * @since 5.2
	 */
	public void setEngineSupplier(Supplier<ScriptEngine> engineSupplier) {
		this.engineSupplier = engineSupplier;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setEngineName(String)}文档。
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setSharedEngine(Boolean)}文档。
	 */
	public void setSharedEngine(Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setScripts(String...)}文档。
	 */
	public void setScripts(String... scripts) {
		this.scripts = scripts;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setRenderObject(String)}文档。
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setRenderFunction(String)}文档。
	 */
	public void setRenderFunction(String functionName) {
		this.renderFunction = functionName;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setCharset(Charset)}文档。
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * 参见{@link ScriptTemplateConfigurer#setResourceLoaderPath(String)}文档。
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
		this.resourceLoaderPaths = new String[paths.length + 1];
		this.resourceLoaderPaths[0] = "";
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (!path.endsWith("/") && !path.endsWith(":")) {
				path = path + "/";
			}
			this.resourceLoaderPaths[i + 1] = path;
		}
	}


	@Override
	protected void initApplicationContext(ApplicationContext context) {
		// 调用父类方法初始化应用程序上下文
		super.initApplicationContext(context);

		// 检测视图配置并设置视图相关属性
		ScriptTemplateConfig viewConfig = autodetectViewConfig();
		// 设置脚本引擎
		if (this.engine == null && viewConfig.getEngine() != null) {
			this.engine = viewConfig.getEngine();
		}
		// 设置引擎提供方
		if (this.engineSupplier == null && viewConfig.getEngineSupplier() != null) {
			this.engineSupplier = viewConfig.getEngineSupplier();
		}
		// 设置引擎名称
		if (this.engineName == null && viewConfig.getEngineName() != null) {
			this.engineName = viewConfig.getEngineName();
		}
		// 暂存视图配置中的脚本
		if (this.scripts == null && viewConfig.getScripts() != null) {
			this.scripts = viewConfig.getScripts();
		}
		// 设置渲染对象
		if (this.renderObject == null && viewConfig.getRenderObject() != null) {
			this.renderObject = viewConfig.getRenderObject();
		}
		// 设置渲染函数
		if (this.renderFunction == null && viewConfig.getRenderFunction() != null) {
			this.renderFunction = viewConfig.getRenderFunction();
		}
		// 设置HTTP内容类型
		if (this.getContentType() == null) {
			setContentType(viewConfig.getContentType() != null ? viewConfig.getContentType() : DEFAULT_CONTENT_TYPE);
		}
		//设置字符编码
		if (this.charset == null) {
			this.charset = (viewConfig.getCharset() != null ? viewConfig.getCharset() : DEFAULT_CHARSET);
		}
		if (this.resourceLoaderPaths == null) {
			// 如果资源加载路径不存在，先从视图配置中获取资源加载路径
			String resourceLoaderPath = viewConfig.getResourceLoaderPath();
			// 如果资源加载路径不存在，则使用默认的资源加载路径，并设置资源加载路径
			setResourceLoaderPath(resourceLoaderPath != null ? resourceLoaderPath : DEFAULT_RESOURCE_LOADER_PATH);
		}
		// 设置是否是共享引擎
		if (this.sharedEngine == null && viewConfig.isSharedEngine() != null) {
			this.sharedEngine = viewConfig.isSharedEngine();
		}

		// 确保只定义了一个脚本引擎
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

		// 当不是共享引擎时，检查脚本引擎的配置是否正确
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			Assert.isTrue(this.engine == null,
					"When 'sharedEngine' is set to false, you should specify the " +
							"script engine using 'engineName' or 'engineSupplier' , not 'engine'.");
		} else if (this.engine != null) {
			// 引擎存在，则加载引擎
			loadScripts(this.engine);
		} else if (this.engineName != null) {
			// 引擎名称存在，则根据引擎名称创建并设置脚本引擎
			setEngine(createEngineFromName(this.engineName));
		} else {
			// 根据引擎提供方创建并设置引擎
			setEngine(createEngineFromSupplier());
		}

		// 如果定义了 '渲染函数'，则验证脚本引擎是否实现了 Invocable 接口
		if (this.renderFunction != null && this.engine != null) {
			Assert.isInstanceOf(Invocable.class, this.engine,
					"ScriptEngine must implement Invocable when 'renderFunction' is specified");
		}
	}

	protected ScriptEngine getEngine() {
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			// 如果不是共享引擎
			Map<Object, ScriptEngine> engines = enginesHolder.get();
			if (engines == null) {
				engines = new HashMap<>(4);
				enginesHolder.set(engines);
			}
			// 生成引擎的键
			String name = (this.engineName != null ? this.engineName : "");
			Object engineKey = (!ObjectUtils.isEmpty(this.scripts) ? new EngineKey(name, this.scripts) : name);
			// 检查是否已经存在引擎，如果不存在则创建一个
			ScriptEngine engine = engines.get(engineKey);
			if (engine == null) {
				if (this.engineName != null) {
					// 如果引擎名称存在，则从名称中创建引擎
					engine = createEngineFromName(this.engineName);
				} else {
					// 否则，从供应者中创建引擎
					engine = createEngineFromSupplier();
				}
				engines.put(engineKey, engine);
			}
			return engine;
		} else {
			// 返回配置的共享脚本引擎
			Assert.state(this.engine != null, "No shared engine available");
			return this.engine;
		}
	}

	protected ScriptEngine createEngineFromName(String engineName) {
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			// 如果ScriptEngineManager为空，使用应用程序上下文的类加载器创建一个新的脚本引擎管理器
			scriptEngineManager = new ScriptEngineManager(obtainApplicationContext().getClassLoader());
			this.scriptEngineManager = scriptEngineManager;
		}

		// 通过引擎名称检索引擎
		ScriptEngine engine = StandardScriptUtils.retrieveEngineByName(scriptEngineManager, engineName);
		// 加载脚本到引擎中
		loadScripts(engine);
		return engine;
	}

	private ScriptEngine createEngineFromSupplier() {
		Assert.state(this.engineSupplier != null, "No engine supplier available");
		// 获取脚本引擎
		ScriptEngine engine = this.engineSupplier.get();
		if (this.renderFunction != null) {
			Assert.isInstanceOf(Invocable.class, engine,
					"ScriptEngine must implement Invocable when 'renderFunction' is specified");
		}
		// 通过引擎加载脚本
		loadScripts(engine);
		return engine;
	}

	protected void loadScripts(ScriptEngine engine) {
		if (!ObjectUtils.isEmpty(this.scripts)) {
			// 遍历所有脚本路径
			for (String script : this.scripts) {
				// 获取资源
				Resource resource = getResource(script);
				if (resource == null) {
					// 如果资源为空，则抛出异常
					throw new IllegalStateException("Script resource [" + script + "] not found");
				}
				try {
					// 通过引擎执行脚本
					engine.eval(new InputStreamReader(resource.getInputStream()));
				} catch (Throwable ex) {
					// 捕获可能的异常，并抛出新的异常
					throw new IllegalStateException("Failed to evaluate script [" + script + "]", ex);
				}
			}
		}
	}

	@Nullable
	protected Resource getResource(String location) {
		if (this.resourceLoaderPaths != null) {
			// 遍历资源加载路径
			for (String path : this.resourceLoaderPaths) {
				// 获取资源
				Resource resource = obtainApplicationContext().getResource(path + location);
				// 如果资源存在，则返回该资源
				if (resource.exists()) {
					return resource;
				}
			}
		}
		// 如果资源不存在，则返回null
		return null;
	}

	protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
		try {
			// 尝试获取上下文中的 ScriptTemplateConfig 类型的 bean
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), ScriptTemplateConfig.class, true, false);
		} catch (NoSuchBeanDefinitionException ex) {
			// 如果未找到对应的 bean，则抛出 ApplicationContextException 异常
			throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
					"Servlet web application context or the parent root context: ScriptTemplateConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}


	@Override
	public boolean checkResource(Locale locale) throws Exception {
		// 获取URL
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		// 通过URL获取资源，并判断该资源是否存在
		return (getResource(url) != null);
	}

	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		super.prepareResponse(request, response);

		// 设置响应内容类型
		setResponseContentType(request, response);
		// 如果字符集不为空，则设置响应字符编码
		if (this.charset != null) {
			response.setCharacterEncoding(this.charset.name());
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
										   HttpServletResponse response) throws Exception {

		try {
			// 获取脚本引擎
			ScriptEngine engine = getEngine();
			// 获取视图的 URL
			String url = getUrl();
			// 检查 URL 是否为空
			Assert.state(url != null, "'url' not set");
			// 获取模板内容
			String template = getTemplate(url);

			// 定义模板加载器函数
			Function<String, String> templateLoader = path -> {
				try {
					return getTemplate(path);
				} catch (IOException ex) {
					throw new IllegalStateException(ex);
				}
			};

			// 获取请求的区域设置
			Locale locale = RequestContextUtils.getLocale(request);
			// 创建渲染上下文对象
			RenderingContext context = new RenderingContext(obtainApplicationContext(), locale, templateLoader, url);

			Object html;
			if (this.renderFunction == null) {
				// 如果未指定渲染函数，则使用默认方式进行渲染
				SimpleBindings bindings = new SimpleBindings();
				bindings.putAll(model);
				model.put("renderingContext", context);
				// 使用脚本引擎执行模板
				html = engine.eval(template, bindings);
			} else if (this.renderObject != null) {
				// 如果指定了渲染对象和函数，则调用指定的渲染函数
				Object thiz = engine.eval(this.renderObject);
				html = ((Invocable) engine).invokeMethod(thiz, this.renderFunction, template, model, context);
			} else {
				// 调用指定的渲染函数
				html = ((Invocable) engine).invokeFunction(this.renderFunction, template, model, context);
			}

			// 将渲染结果写入响应
			response.getWriter().write(String.valueOf(html));
		} catch (ScriptException ex) {
			// 捕获脚本异常并抛出 Servlet 异常
			throw new ServletException("Failed to render script template", new StandardScriptEvalException(ex));
		}
	}

	protected String getTemplate(String path) throws IOException {
		Resource resource = getResource(path);
		if (resource == null) {
			// 如果模板资源未找到，则抛出异常
			throw new IllegalStateException("Template resource [" + path + "] not found");
		}
		InputStreamReader reader = (this.charset != null ?
				// 如果设置了字符集，则使用指定字符集创建输入流读取模板内容
				new InputStreamReader(resource.getInputStream(), this.charset) :
				// 否则直接创建输入流读取模板内容
				new InputStreamReader(resource.getInputStream()));
		// 将模板内容读取为字符串并返回
		return FileCopyUtils.copyToString(reader);
	}


	/**
	 * {@code enginesHolder ThreadLocal}的关键类。
	 * 仅在指定了脚本时使用；否则，将直接使用{@code engineName String}作为缓存键。
	 */
	private static class EngineKey {
		/**
		 * 引擎名称
		 */
		private final String engineName;

		/**
		 * 脚本数组
		 */
		private final String[] scripts;

		/**
		 * 构造函数。
		 *
		 * @param engineName 引擎名称
		 * @param scripts    脚本数组
		 */
		public EngineKey(String engineName, String[] scripts) {
			this.engineName = engineName;
			this.scripts = scripts;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof EngineKey)) {
				return false;
			}
			EngineKey otherKey = (EngineKey) other;
			return (this.engineName.equals(otherKey.engineName) && Arrays.equals(this.scripts, otherKey.scripts));
		}

		@Override
		public int hashCode() {
			return (this.engineName.hashCode() * 29 + Arrays.hashCode(this.scripts));
		}
	}

}
