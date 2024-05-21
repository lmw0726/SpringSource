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

package org.springframework.web.servlet.config.annotation;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.accept.*;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 创建一个 {@code ContentNegotiationManager} 并配置它与一个或多个 {@link ContentNegotiationStrategy} 实例。
 *
 * <p>这个工厂提供的属性会配置底层策略。
 * 下表显示了属性名称、默认设置以及它们帮助配置的策略：
 *
 * <table>
 * <tr>
 * <th>属性设置器</th>
 * <th>默认值</th>
 * <th>底层策略</th>
 * <th>是否启用</th>
 * </tr>
 * <tr>
 * <td>{@link #favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #favorPathExtension}</td>
 * <td>false (从5.3开始)</td>
 * <td>{@link org.springframework.web.accept.PathExtensionContentNegotiationStrategy
 * PathExtensionContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>启用</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * </table>
 *
 * <p>从5.0开始，你可以通过 {@link #strategies(List)} 设置确切的策略。
 *
 * <p><strong>注意：</strong>如果必须使用基于URL的内容类型解析，使用查询参数比使用路径扩展更简单且更可取，
 * 因为后者可能会导致URI变量、路径参数和URI解码的问题。考虑将 {@link #favorPathExtension} 设置为 {@literal false}，
 * 或者通过 {@link #strategies(List)} 显式设置要使用的策略。
 * <p>
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentNegotiationConfigurer {

	/**
	 * 内容协商管理器工厂Bean
	 */
	private final ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();

	/**
	 * 文件扩展名 - 媒体类型 映射
	 */
	private final Map<String, MediaType> mediaTypes = new HashMap<>();


	/**
	 * 使用 {@link javax.servlet.ServletContext} 的类构造函数。
	 */
	public ContentNegotiationConfigurer(@Nullable ServletContext servletContext) {
		if (servletContext != null) {
			// 如果Servlet上下文不为空，则设置Servlet上下文
			this.factory.setServletContext(servletContext);
		}
	}


	/**
	 * 设置要使用的确切策略列表。
	 * <p><strong>注意：</strong>使用此方法与使用此类中的所有其他自定义默认固定策略的设置是互斥的。详见类级文档。
	 *
	 * @param strategies 要使用的策略
	 * @since 5.0
	 */
	public void strategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.factory.setStrategies(strategies);
	}

	/**
	 * 是否应该使用请求参数（默认情况下为"format"）来确定请求的媒体类型。要使此选项生效，必须注册 {@link #mediaType(String, MediaType) 媒体类型映射}。
	 * <p>默认情况下设置为 {@code false}。
	 *
	 * @see #parameterName(String)
	 */
	public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
		this.factory.setFavorParameter(favorParameter);
		return this;
	}

	/**
	 * 设置当 {@link #favorParameter} 开启时要使用的查询参数名称。
	 * <p>默认参数名称为 {@code "format"}。
	 */
	public ContentNegotiationConfigurer parameterName(String parameterName) {
		this.factory.setParameterName(parameterName);
		return this;
	}

	/**
	 * 是否应该使用URL路径中的路径扩展名来确定请求的媒体类型。
	 * <p>默认情况下设置为 {@code false}，此时路径扩展名对内容协商没有影响。
	 *
	 * @deprecated 从5.2.4开始。参见 {@link ContentNegotiationManagerFactoryBean#setFavorPathExtension(boolean)} 上的弃用说明。
	 */
	@Deprecated
	public ContentNegotiationConfigurer favorPathExtension(boolean favorPathExtension) {
		this.factory.setFavorPathExtension(favorPathExtension);
		return this;
	}

	/**
	 * 添加一个从键（从路径扩展名或查询参数中提取）到MediaType的映射。这是参数策略工作的前提。
	 * 任何在此显式注册的扩展名也会被视为安全的，用于反射文件下载攻击检测（参见Spring Framework参考文档了解更多关于RFD攻击保护的细节）。
	 * <p>路径扩展名策略还将尝试使用 {@link ServletContext#getMimeType} 和 {@link MediaTypeFactory} 来解析路径扩展名。
	 * 要改变此行为，请参见 {@link #useRegisteredExtensionsOnly} 属性。
	 *
	 * @param extension 要查找的键
	 * @param mediaType 媒体类型
	 * @see #mediaTypes(Map)
	 * @see #replaceMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
		this.mediaTypes.put(extension, mediaType);
		return this;
	}

	/**
	 * {@link #mediaType} 的替代方法。
	 *
	 * @see #mediaType(String, MediaType)
	 * @see #replaceMediaTypes(Map)
	 */
	public ContentNegotiationConfigurer mediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			this.mediaTypes.putAll(mediaTypes);
		}
		return this;
	}

	/**
	 * 类似于 {@link #mediaType}，但用于替换现有映射。
	 *
	 * @see #mediaType(String, MediaType)
	 * @see #mediaTypes(Map)
	 */
	public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
		// 清空媒体类型映射
		this.mediaTypes.clear();
		// 重新添加媒体类型映射
		mediaTypes(mediaTypes);
		return this;
	}

	/**
	 * 是否忽略无法解析为任何媒体类型的路径扩展名请求。将此设置为 {@code false} 会导致在没有匹配时抛出 {@code HttpMediaTypeNotAcceptableException}。
	 * <p>默认情况下设置为 {@code true}。
	 *
	 * @deprecated 从5.2.4开始。
	 * 参见 {@link ContentNegotiationManagerFactoryBean#setIgnoreUnknownPathExtensions(boolean)} 上的弃用说明。
	 */
	@Deprecated
	public ContentNegotiationConfigurer ignoreUnknownPathExtensions(boolean ignore) {
		this.factory.setIgnoreUnknownPathExtensions(ignore);
		return this;
	}

	/**
	 * 当设置了 {@link #favorPathExtension} 时，此属性决定是否允许使用JAF（Java Activation Framework）来将路径扩展名解析为特定的MediaType。
	 *
	 * @deprecated 从5.0开始，建议使用 {@link #useRegisteredExtensionsOnly(boolean)}，其行为相反
	 */
	@Deprecated
	public ContentNegotiationConfigurer useJaf(boolean useJaf) {
		return this.useRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * 当设置了 {@link #favorPathExtension favorPathExtension} 时，此属性决定是否仅使用注册的 {@code MediaType} 映射来将路径扩展名解析为特定的MediaType。
	 * <p>默认情况下未设置，此时 {@code PathExtensionContentNegotiationStrategy} 将使用可用的默认值。
	 */
	public ContentNegotiationConfigurer useRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.factory.setUseRegisteredExtensionsOnly(useRegisteredExtensionsOnly);
		return this;
	}

	/**
	 * 是否禁用检查 'Accept' 请求头。
	 * <p>默认情况下，此值设置为 {@code false}。
	 */
	public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.factory.setIgnoreAcceptHeader(ignoreAcceptHeader);
		return this;
	}

	/**
	 * 设置在未请求任何内容类型时要使用的默认内容类型，按优先级顺序。
	 * <p>如果存在不支持任何给定媒体类型的目标，请考虑在末尾附加 {@link MediaType#ALL}。
	 * <p>默认情况下未设置。
	 *
	 * @see #defaultContentTypeStrategy
	 */
	public ContentNegotiationConfigurer defaultContentType(MediaType... defaultContentTypes) {
		this.factory.setDefaultContentTypes(Arrays.asList(defaultContentTypes));
		return this;
	}

	/**
	 * 设置一个自定义 {@link ContentNegotiationStrategy} 来确定在未请求任何内容类型时要使用的内容类型。
	 * <p>默认情况下未设置。
	 *
	 * @see #defaultContentType
	 * @since 4.1.2
	 */
	public ContentNegotiationConfigurer defaultContentTypeStrategy(ContentNegotiationStrategy defaultStrategy) {
		this.factory.setDefaultContentTypeStrategy(defaultStrategy);
		return this;
	}


	/**
	 * 根据此配置器的设置构建一个 {@link ContentNegotiationManager}。
	 *
	 * @see ContentNegotiationManagerFactoryBean#getObject()
	 * @since 4.3.12
	 */
	protected ContentNegotiationManager buildContentNegotiationManager() {
		this.factory.addMediaTypes(this.mediaTypes);
		return this.factory.build();
	}

}
