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

package org.springframework.web.servlet.view.xslt;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleTransformErrorListener;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.util.WebUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * 基于XSLT的View，允许将响应上下文渲染为XSLT转换的结果。
 *
 * <p>XSLT Source对象作为模型中的参数提供，然后在响应渲染期间{@link #locateSource 检测}到。
 * 用户可以通过{@link #setSourceKey sourceKey}属性指定模型中的特定条目，也可以让Spring定位Source对象。
 * 此类还提供将对象转换为Source实现的基本转换。有关更多详细信息，请参见{@link #getSourceTypes() 这里}。
 *
 * <p>所有模型参数都作为参数传递给XSLT转换器。
 * 此外，用户可以配置{@link #setOutputProperties output properties}以传递给Transformer。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XsltView extends AbstractUrlBasedView {

	/**
	 * 转换工厂类
	 */
	@Nullable
	private Class<? extends TransformerFactory> transformerFactoryClass;

	/**
	 * 源模型属性的名称
	 */
	@Nullable
	private String sourceKey;

	/**
	 * URI解析器
	 */
	@Nullable
	private URIResolver uriResolver;

	/**
	 * 错误监听器
	 */
	private ErrorListener errorListener = new SimpleTransformErrorListener(logger);

	/**
	 * 是否缩进字符
	 */
	private boolean indent = true;

	/**
	 * 输出属性
	 */
	@Nullable
	private Properties outputProperties;

	/**
	 * 是否缓存模板
	 */
	private boolean cacheTemplates = true;

	/**
	 * 转换器工厂
	 */
	@Nullable
	private TransformerFactory transformerFactory;

	/**
	 * 缓存的模板
	 */
	@Nullable
	private Templates cachedTemplates;


	/**
	 * 指定要使用的XSLT TransformerFactory类。
	 * <p>将调用指定类的默认构造函数以为此视图构建TransformerFactory。
	 */
	public void setTransformerFactoryClass(Class<? extends TransformerFactory> transformerFactoryClass) {
		this.transformerFactoryClass = transformerFactoryClass;
	}

	/**
	 * 设置表示XSLT Source的模型属性的名称。如果未指定，则将在模型映射中搜索匹配的值类型。
	 * <p>以下源类型得到了支持：
	 * {@link Source}、{@link Document}、{@link Node}、{@link Reader}、
	 * {@link InputStream}和{@link Resource}。
	 *
	 * @see #getSourceTypes
	 * @see #convertSource
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	/**
	 * 设置在转换中使用的URIResolver。
	 * <p>URIResolver处理对XSLT {@code document()}函数的调用。
	 */
	public void setUriResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}

	/**
	 * 设置{@link javax.xml.transform.ErrorListener}接口的实现，用于自定义处理转换错误和警告。
	 * <p>如果未设置，将使用默认的{@link org.springframework.util.xml.SimpleTransformErrorListener}，
	 * 它只是使用视图类的记录器实例记录警告，并重新抛出错误以终止XML转换。
	 *
	 * @see org.springframework.util.xml.SimpleTransformErrorListener
	 */
	public void setErrorListener(@Nullable ErrorListener errorListener) {
		this.errorListener = (errorListener != null ? errorListener : new SimpleTransformErrorListener(logger));
	}

	/**
	 * 设置XSLT转换器在输出结果树时是否可以添加额外的空白。
	 * <p>默认为{@code true}（打开）；将其设置为{@code false}（关闭）以不指定“缩进”键，将选择权留给样式表。
	 *
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	/**
	 * 设置要应用于样式表的任意转换器输出属性。
	 * <p>这里指定的任何值都将覆盖程序化设置的默认值。
	 *
	 * @see javax.xml.transform.Transformer#setOutputProperty
	 */
	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	/**
	 * 打开/关闭XSLT {@link Templates}实例的缓存。
	 * <p>默认值为“true”。仅在开发中将其设置为“false”，其中缓存不会严重影响性能。
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	/**
	 * 初始化此XsltView的TransformerFactory。
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		// 实例化转换器工厂
		this.transformerFactory = newTransformerFactory(this.transformerFactoryClass);

		// 设置错误监听器
		this.transformerFactory.setErrorListener(this.errorListener);

		// 如果设置了 URI 解析器，则设置 URI 解析器
		if (this.uriResolver != null) {
			this.transformerFactory.setURIResolver(this.uriResolver);
		}

		// 如果启用了模板缓存，则加载模板
		if (this.cacheTemplates) {
			this.cachedTemplates = loadTemplates();
		}
	}

	/**
	 * 为此视图创建新的TransformerFactory。
	 * <p>默认实现只是调用{@link javax.xml.transform.TransformerFactory#newInstance()}。
	 * 如果已明确指定了{@link #setTransformerFactoryClass "transformerFactoryClass"}，则会调用指定类的默认构造函数。
	 * <p>可以在子类中重写。
	 *
	 * @param transformerFactoryClass 指定的工厂类（如果有）
	 * @return 新的TransactionFactory实例
	 * @see #setTransformerFactoryClass
	 * @see #getTransformerFactory()
	 */
	protected TransformerFactory newTransformerFactory(
			@Nullable Class<? extends TransformerFactory> transformerFactoryClass) {

		if (transformerFactoryClass != null) {
			try {
				// 如果指定了转换器工厂类，则尝试实例化该类
				return ReflectionUtils.accessibleConstructor(transformerFactoryClass).newInstance();
			} catch (Exception ex) {
				throw new TransformerFactoryConfigurationError(ex, "Could not instantiate TransformerFactory");
			}
		} else {
			// 否则，使用默认的 TransformerFactory 实例化方法
			return TransformerFactory.newInstance();
		}
	}

	/**
	 * 返回此XsltView使用的TransformerFactory。
	 *
	 * @return TransformerFactory（永远不为{@code null}）
	 */
	protected final TransformerFactory getTransformerFactory() {
		Assert.state(this.transformerFactory != null, "No TransformerFactory available");
		return this.transformerFactory;
	}


	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		// 从缓存中获取模板，如果没有，则加载模板
		Templates templates = this.cachedTemplates;
		if (templates == null) {
			templates = loadTemplates();
		}

		// 创建转换器
		Transformer transformer = createTransformer(templates);
		// 配置转换器
		configureTransformer(model, response, transformer);
		// 配置HTTP响应
		configureResponse(model, response, transformer);

		Source source = null;
		try {
			// 定位源
			source = locateSource(model);
			if (source == null) {
				throw new IllegalArgumentException("Unable to locate Source object in model: " + model);
			}
			// 进行转换
			transformer.transform(source, createResult(response));
		} finally {
			// 关闭源（如果有必要）
			closeSourceIfNecessary(source);
		}
	}

	/**
	 * 创建用于渲染转换结果的XSLT {@link Result}。
	 * <p>默认实现创建一个{@link StreamResult}，将其包装在提供的
	 * HttpServletResponse的{@link HttpServletResponse#getOutputStream() OutputStream}中。
	 *
	 * @param response 当前HTTP响应
	 * @return 要使用的XSLT Result
	 * @throws Exception 如果无法构建结果
	 */
	protected Result createResult(HttpServletResponse response) throws Exception {
		return new StreamResult(response.getOutputStream());
	}

	/**
	 * <p>在提供的模型中定位{@link Source}对象，
	 * 根据需要进行转换。默认实现首先尝试查找配置的{@link #setSourceKey source key}，
	 * 然后尝试查找{@link #getSourceTypes() supported type}的对象。
	 *
	 * @param model 合并的模型Map（永远不为{@code null}）
	 * @return XSLT Source对象（如果未找到，则为{@code null}）
	 * @throws Exception 在定位源时出现错误
	 * @see #setSourceKey
	 * @see #convertSource
	 */
	@Nullable
	protected Source locateSource(Map<String, Object> model) throws Exception {
		// 如果设置了源键，则从模型中获取源并转换
		if (this.sourceKey != null) {
			return convertSource(model.get(this.sourceKey));
		}

		// 从模型值中查找并转换源
		Object source = CollectionUtils.findValueOfType(model.values(), getSourceTypes());
		return (source != null ? convertSource(source) : null);
	}

	/**
	 * 返回转换为XSLT {@link Source}时支持的{@link Class Classes}数组。
	 * <p>目前支持{@link Source}、{@link Document}、{@link Node}、{@link Reader}、
	 * {@link InputStream}和{@link Resource}。
	 *
	 * @return 支持的源类型
	 */
	protected Class<?>[] getSourceTypes() {
		return new Class<?>[]{Source.class, Document.class, Node.class, Reader.class, InputStream.class, Resource.class};
	}

	/**
	 * 如果{@link Object}类型是{@link #getSourceTypes() supported}，
	 * 则将提供的{@link Object}转换为XSLT {@link Source}。
	 *
	 * @param source 原始源对象
	 * @return 适配的XSLT Source
	 * @throws IllegalArgumentException 如果给定的对象不是受支持的类型
	 */
	protected Source convertSource(Object source) throws Exception {
		if (source instanceof Source) {
			return (Source) source;
		} else if (source instanceof Document) {
			return new DOMSource(((Document) source).getDocumentElement());
		} else if (source instanceof Node) {
			return new DOMSource((Node) source);
		} else if (source instanceof Reader) {
			return new StreamSource((Reader) source);
		} else if (source instanceof InputStream) {
			return new StreamSource((InputStream) source);
		} else if (source instanceof Resource) {
			Resource resource = (Resource) source;
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		} else {
			throw new IllegalArgumentException("Value '" + source + "' cannot be converted to XSLT Source");
		}
	}

	/**
	 * 配置提供的{@link Transformer}实例。
	 * <p>默认实现将参数从模型复制到Transformer的{@link Transformer#setParameter parameter set}中。
	 * 此实现还将{@link #setOutputProperties output properties}复制到{@link Transformer}的{@link Transformer#setOutputProperty output properties}中。
	 * 还设置了缩进属性。
	 *
	 * @param model       合并的输出Map（永远不为{@code null}）
	 * @param response    当前HTTP响应
	 * @param transformer 目标transformer
	 * @see #copyModelParameters(Map, Transformer)
	 * @see #copyOutputProperties(Transformer)
	 * @see #configureIndentation(Transformer)
	 */
	protected void configureTransformer(Map<String, Object> model, HttpServletResponse response,
										Transformer transformer) {
		// 复制模型参数
		copyModelParameters(model, transformer);
		// 复制输出属性
		copyOutputProperties(transformer);
		// 配置缩进
		configureIndentation(transformer);
	}

	/**
	 * 配置所提供的{@link Transformer}的缩进设置。
	 *
	 * @param transformer 目标transformer
	 * @see org.springframework.util.xml.TransformerUtils#enableIndenting(javax.xml.transform.Transformer)
	 * @see org.springframework.util.xml.TransformerUtils#disableIndenting(javax.xml.transform.Transformer)
	 */
	protected final void configureIndentation(Transformer transformer) {
		if (this.indent) {
			// 如果允许缩进字符，则转换器启用字符缩进
			TransformerUtils.enableIndenting(transformer);
		} else {
			// 否则转换器禁用字符串
			TransformerUtils.disableIndenting(transformer);
		}
	}

	/**
	 * 将配置的输出{@link Properties}（如果有）复制到提供的{@link Transformer}的{@link Transformer#setOutputProperty output property set}中。
	 *
	 * @param transformer 目标transformer
	 */
	protected final void copyOutputProperties(Transformer transformer) {
		if (this.outputProperties != null) {
			// 如果设置了输出属性，获取输出属性名称
			Enumeration<?> en = this.outputProperties.propertyNames();
			while (en.hasMoreElements()) {
				// 遍历每一个属性名称
				String name = (String) en.nextElement();
				// 获取相应的属性值后，转换每一个属性值
				transformer.setOutputProperty(name, this.outputProperties.getProperty(name));
			}
		}
	}

	/**
	 * 将提供的Map中的所有条目复制到提供的{@link Transformer}的{@link Transformer#setParameter(String, Object) parameter set}中。
	 *
	 * @param model       合并的输出Map（永远不为{@code null}）
	 * @param transformer 目标transformer
	 */
	protected final void copyModelParameters(Map<String, Object> model, Transformer transformer) {
		model.forEach(transformer::setParameter);
	}

	/**
	 * 配置所提供的{@link HttpServletResponse}。
	 * <p>此方法的默认实现从{@link Transformer}中的“media-type”和“encoding”输出属性中设置{@link HttpServletResponse#setContentType content type}
	 * 和{@link HttpServletResponse#setCharacterEncoding encoding}。
	 *
	 * @param model       合并的输出Map（永远不为{@code null}）
	 * @param response    当前HTTP响应
	 * @param transformer 目标transformer
	 */
	protected void configureResponse(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
		// 获取配置的内容类型、媒体类型和编码
		String contentType = getContentType();
		String mediaType = transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
		String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);

		// 如果媒体类型非空，则将其赋值给内容类型
		if (StringUtils.hasText(mediaType)) {
			contentType = mediaType;
		}
		if (StringUtils.hasText(encoding)) {
			// 如果编码非空且内容类型中不包含字符集子句，则将编码添加到内容类型中
			if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
				contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
			}
		}

		// 设置响应的内容类型
		response.setContentType(contentType);
	}

	/**
	 * 加载配置位置下样式表的{@link Templates}实例。
	 */
	private Templates loadTemplates() throws ApplicationContextException {
		// 获取样式表的源
		Source stylesheetSource = getStylesheetSource();

		try {
			// 使用样式表源创建模板
			Templates templates = getTransformerFactory().newTemplates(stylesheetSource);
			return templates;
		} catch (TransformerConfigurationException ex) {
			// 捕获可能的配置异常并抛出应用程序上下文异常
			throw new ApplicationContextException("Can't load stylesheet from '" + getUrl() + "'", ex);
		} finally {
			// 关闭样式表源
			closeSourceIfNecessary(stylesheetSource);
		}
	}

	/**
	 * 创建用于进行XSLT转换的{@link Transformer}实例。
	 * <p>默认实现只是调用{@link Templates#newTransformer()}，
	 * 并且如果指定了自定义{@link URIResolver}，则配置了{@link Transformer}。
	 *
	 * @param templates 要为其创建Transformer的XSLT Templates实例
	 * @return Transformer对象
	 * @throws TransformerConfigurationException 如果创建失败
	 */
	protected Transformer createTransformer(Templates templates) throws TransformerConfigurationException {
		// 使用模板创建转换器
		Transformer transformer = templates.newTransformer();

		// 如果设置了 URI 解析器，则设置转换器的 URI 解析器
		if (this.uriResolver != null) {
			transformer.setURIResolver(this.uriResolver);
		}

		return transformer;
	}

	/**
	 * 获取用于XSLT模板的XSLT {@link Source}，位于配置的URL下。
	 *
	 * @return Source对象
	 */
	protected Source getStylesheetSource() {
		// 获取URL
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		if (logger.isDebugEnabled()) {
			logger.debug("Applying stylesheet [" + url + "]");
		}
		try {
			// 获取URL对应的字眼
			Resource resource = obtainApplicationContext().getResource(url);
			// 返回一个流资源
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		} catch (IOException ex) {
			throw new ApplicationContextException("Can't load XSLT stylesheet from '" + url + "'", ex);
		}
	}

	/**
	 * 如果适用，关闭由提供的{@link Source}管理的底层资源。
	 * <p>仅适用于{@link StreamSource StreamSources}。
	 *
	 * @param source 要关闭的XSLT Source（可能为{@code null}）
	 */
	private void closeSourceIfNecessary(@Nullable Source source) {
		// 如果源是 StreamSource 类型
		if (source instanceof StreamSource) {
			// 强制转换为 StreamSource
			StreamSource streamSource = (StreamSource) source;

			// 如果 StreamSource 中的 Reader 不为 null，则关闭 Reader
			if (streamSource.getReader() != null) {
				try {
					streamSource.getReader().close();
				} catch (IOException ex) {
					// 忽略异常
				}
			}

			// 如果 StreamSource 中的 InputStream 不为 null，则关闭 InputStream
			if (streamSource.getInputStream() != null) {
				try {
					streamSource.getInputStream().close();
				} catch (IOException ex) {
					// 忽略异常
				}
			}
		}
	}

}
