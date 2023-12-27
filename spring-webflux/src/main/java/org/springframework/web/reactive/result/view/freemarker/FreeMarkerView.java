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

package org.springframework.web.reactive.result.view.freemarker;

import freemarker.core.ParseException;
import freemarker.template.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.result.view.AbstractUrlBasedView;
import org.springframework.web.reactive.result.view.RequestContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 使用 FreeMarker 模板引擎的 {@code View} 实现。
 *
 * <p>依赖于单个 {@link FreeMarkerConfig} 对象，例如 {@link FreeMarkerConfigurer} 在应用程序上下文中可访问。
 * 或者可以通过 {@link #setConfiguration} 直接在此类上设置 FreeMarker {@link Configuration}。
 *
 * <p>{@link #setUrl(String) url} 属性是相对于 FreeMarkerConfigurer 的
 * {@link FreeMarkerConfigurer#setTemplateLoaderPath templateLoaderPath} 的 FreeMarker 模板位置。
 *
 * <p>注意：Spring 的 FreeMarker 支持需要 FreeMarker 2.3 或更高版本。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class FreeMarkerView extends AbstractUrlBasedView {

	/**
	 * 在模板模型中 {@link RequestContext} 实例的属性名称，可供 Spring 的宏使用，例如用于创建
	 * {@link org.springframework.web.reactive.result.view.BindStatus BindStatus} 对象。
	 *
	 * @see #setExposeSpringMacroHelpers(boolean)
	 * @since 5.2
	 */
	public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";

	/**
	 * FreeMarker配置对象
	 */
	@Nullable
	private Configuration configuration;

	/**
	 * 字符编码
	 */
	@Nullable
	private String encoding;

	/**
	 * 是否暴露Spring宏助手
	 */
	private boolean exposeSpringMacroHelpers = true;


	/**
	 * 设置此视图使用的 FreeMarker {@link Configuration}。
	 * <p>通常情况下，此属性不会直接设置。相反，期望 Spring 应用程序上下文中存在一个单个
	 * {@link FreeMarkerConfig}，该配置用于获取 FreeMarker 配置。
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 获取此视图使用的 FreeMarker {@link Configuration}。
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * 获取用于实际使用的 FreeMarker {@link Configuration}。
	 *
	 * @return FreeMarker 配置（永远不会为 {@code null}）
	 * @throws IllegalStateException 如果未设置 {@code Configuration} 对象
	 * @see #getConfiguration()
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "未设置 Configuration");
		return configuration;
	}

	/**
	 * 设置 FreeMarker 模板文件的编码。
	 * <p>默认情况下，{@link FreeMarkerConfigurer} 将默认编码设置为 "UTF-8"。
	 * 如果所有模板共享通用编码，则建议在 FreeMarker {@link Configuration} 中指定编码，而不是每个模板。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 获取 FreeMarker 模板的编码。
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置是否公开 {@link RequestContext} 供 Spring 的宏库使用，名称为 {@value #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE}。
	 * <p>默认值为 {@code true}。
	 * <p>Spring 的 FreeMarker 默认宏所需的。请注意，对于使用 HTML 表单的模板来说，这不是必需的，
	 * 除非您希望利用 Spring 辅助宏。
	 *
	 * @see #SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE
	 * @since 5.2
	 */
	public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
		this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		// 调用父类的属性设置方法
		super.afterPropertiesSet();

		// 如果配置对象为空，则自动检测并设置FreeMarker配置对象
		if (getConfiguration() == null) {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
		}
	}

	/**
	 * 自动检测 {@code ApplicationContext} 中的 {@link FreeMarkerConfig} 对象。
	 *
	 * @return 用于此视图的 {@code FreeMarkerConfig} 实例
	 * @throws BeansException 如果找不到 {@code FreeMarkerConfig} 实例
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		} catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this application context " +
							"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
							"This bean may be given any name.", ex);
		}
	}


	/**
	 * 检查此视图所使用的 FreeMarker 模板是否存在且有效。
	 * <p>可以重写此方法以自定义行为，例如对于要呈现到单个视图的多个模板的情况。
	 */
	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		try {
			// 检查是否可以获取模板，即使可能随后再次获取。
			getTemplate(locale);
			return true;
		} catch (FileNotFoundException ex) {
			// 允许 ViewResolver 链...
			return false;
		} catch (ParseException ex) {
			throw new ApplicationContextException(
					"Failed to parse FreeMarker template for URL [" + getUrl() + "]", ex);
		} catch (IOException ex) {
			throw new ApplicationContextException(
					"Could not load FreeMarker template for URL [" + getUrl() + "]", ex);
		}
	}

	/**
	 * 准备要用于渲染的模型，可能会公开一个 {@link RequestContext} 供 Spring FreeMarker 宏使用，
	 * 然后委托给此方法的继承实现。
	 *
	 * @see #setExposeSpringMacroHelpers(boolean)
	 * @see org.springframework.web.reactive.result.view.AbstractView#getModelAttributes(Map, ServerWebExchange)
	 * @since 5.2
	 */
	@Override
	protected Mono<Map<String, Object>> getModelAttributes(
			@Nullable Map<String, ?> model, ServerWebExchange exchange) {

		if (this.exposeSpringMacroHelpers) {
			if (model != null && model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
				throw new IllegalStateException(
						"Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE +
								"' because of an existing model object of the same name");
			}
			// 创建模型的防御性副本。
			Map<String, Object> attributes = (model != null ? new HashMap<>(model) : new HashMap<>());
			// 为 Spring 宏公开 RequestContext 实例。
			attributes.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, new RequestContext(
					exchange, attributes, obtainApplicationContext(), getRequestDataValueProcessor()));
			return super.getModelAttributes(attributes, exchange);
		}
		return super.getModelAttributes(model, exchange);
	}

	/**
	 * 渲染视图的核心方法。将模型数据渲染到响应流中。
	 *
	 * @param renderAttributes 用于渲染的模型属性
	 * @param contentType      响应的媒体类型，可以为 null
	 * @param exchange         当前 ServerWebExchange 对象
	 * @return 表示渲染操作完成的 Mono
	 */
	@Override
	protected Mono<Void> renderInternal(Map<String, Object> renderAttributes,
										@Nullable MediaType contentType, ServerWebExchange exchange) {

		return exchange.getResponse().writeWith(Mono
				.fromCallable(() -> {
					// 公开所有标准的 FreeMarker 哈希模型。
					SimpleHash freeMarkerModel = getTemplateModel(renderAttributes, exchange);

					if (logger.isDebugEnabled()) {
						logger.debug(exchange.getLogPrefix() + "Rendering [" + getUrl() + "]");
					}

					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());
					DataBuffer dataBuffer = exchange.getResponse().bufferFactory().allocateBuffer();
					try {
						Charset charset = getCharset(contentType);
						Writer writer = new OutputStreamWriter(dataBuffer.asOutputStream(), charset);
						getTemplate(locale).process(freeMarkerModel, writer);
						return dataBuffer;
					} catch (IOException ex) {
						DataBufferUtils.release(dataBuffer);
						String message = "Could not load FreeMarker template for URL [" + getUrl() + "]";
						throw new IllegalStateException(message, ex);
					} catch (Throwable ex) {
						DataBufferUtils.release(dataBuffer);
						throw ex;
					}
				})
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
	}

	private Charset getCharset(@Nullable MediaType mediaType) {
		return Optional.ofNullable(mediaType).map(MimeType::getCharset).orElse(getDefaultCharset());
	}

	/**
	 * 为给定的模型映射构建 FreeMarker 模板模型。
	 * <p>默认实现构建一个 {@link SimpleHash}。
	 *
	 * @param model    用于渲染的模型
	 * @param exchange 当前交换
	 * @return FreeMarker 模板模型，作为 {@link SimpleHash} 或其子类
	 */
	protected SimpleHash getTemplateModel(Map<String, Object> model, ServerWebExchange exchange) {
		SimpleHash fmModel = new SimpleHash(getObjectWrapper());
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * 获取配置的 FreeMarker {@link ObjectWrapper}，如果没有指定，则返回 {@linkplain ObjectWrapper#DEFAULT_WRAPPER 默认包装器}。
	 *
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
		Version version = Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS;
		return (ow != null ? ow : new DefaultObjectWrapperBuilder(version).build());
	}

	/**
	 * 获取给定区域设置的 FreeMarker 模板，以便由此视图进行渲染。
	 * <p>默认情况下，将检索由 "url" bean 属性指定的模板。
	 *
	 * @param locale 当前区域设置
	 * @return 要渲染的 FreeMarker 模板
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(getUrl(), locale, getEncoding()) :
				obtainConfiguration().getTemplate(getUrl(), locale));
	}

}
