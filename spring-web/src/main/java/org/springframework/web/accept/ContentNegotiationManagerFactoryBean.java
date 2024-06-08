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

package org.springframework.web.accept;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * 工厂类，用于创建一个{@code ContentNegotiationManager}并配置它的{@link ContentNegotiationStrategy}实例。
 *
 * <p>此工厂提供了一些属性，这些属性实际上会配置底层的策略。下表显示了属性名称、它们的默认设置，以及它们帮助配置的策略：
 *
 * <table>
 * <tr>
 * <th>属性设置器</th>
 * <th>默认值</th>
 * <th>底层策略</th>
 * <th>启用或禁用</th>
 * </tr>
 * <tr>
 * <td>{@link #setFavorParameter favorParameter}</td>
 * <td>false</td>
 * <td>{@link ParameterContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setFavorPathExtension favorPathExtension}</td>
 * <td>false (自5.3起)</td>
 * <td>{@link PathExtensionContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setIgnoreAcceptHeader ignoreAcceptHeader}</td>
 * <td>false</td>
 * <td>{@link HeaderContentNegotiationStrategy}</td>
 * <td>启用</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentType defaultContentType}</td>
 * <td>null</td>
 * <td>{@link FixedContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * <tr>
 * <td>{@link #setDefaultContentTypeStrategy defaultContentTypeStrategy}</td>
 * <td>null</td>
 * <td>{@link ContentNegotiationStrategy}</td>
 * <td>关闭</td>
 * </tr>
 * </table>
 *
 * <p>或者，您可以避免使用上述便捷构建方法，并通过{@link #setStrategies(List)}设置要使用的确切策略。
 *
 * <p><strong>废弃说明：</strong>从5.2.4开始，
 * {@link #setFavorPathExtension(boolean) favorPathExtension} 和
 * {@link #setIgnoreUnknownPathExtensions(boolean) ignoreUnknownPathExtensions}已被废弃，
 * 以阻止使用路径扩展进行内容协商，并对使用路径扩展进行请求映射的类似废弃操作
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 * RequestMappingHandlerMapping}也已被废弃。有关更多上下文，请阅读
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24719</a>。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public class ContentNegotiationManagerFactoryBean
		implements FactoryBean<ContentNegotiationManager>, ServletContextAware, InitializingBean {

	/**
	 * 内容协商策略列表。
	 */
	@Nullable
	private List<ContentNegotiationStrategy> strategies;

	/**
	 * 是否优先使用请求参数来确定所请求的媒体类型。
	 */
	private boolean favorParameter = false;

	/**
	 * 在使用{@link #favorParameter}时，指定用于确定所请求的媒体类型的查询参数名称。
	 */
	private String parameterName = "format";

	/**
	 * 是否使用URL路径中的扩展名来确定所请求的媒体类型。
	 */
	private boolean favorPathExtension = false;

	/**
	 * 媒体类型与其对应的键之间的映射。
	 */
	private Map<String, MediaType> mediaTypes = new HashMap<>();

	/**
	 * 是否忽略未知的路径扩展名。
	 */
	private boolean ignoreUnknownPathExtensions = true;

	/**
	 * 是否仅使用已注册的扩展名。
	 */
	@Nullable
	private Boolean useRegisteredExtensionsOnly;

	/**
	 * 是否忽略Accept头部信息。
	 */
	private boolean ignoreAcceptHeader = false;

	/**
	 * 默认的内容协商策略。
	 */
	@Nullable
	private ContentNegotiationStrategy defaultNegotiationStrategy;

	/**
	 * 内容协商管理器。
	 */
	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	/**
	 * Servlet上下文。
	 */
	@Nullable
	private ServletContext servletContext;


	/**
	 * 设置要使用的确切策略列表。
	 * <p><strong>注意：</strong>使用此方法与此类中的所有其他设置器相互排斥，这些设置器自定义了一个默认的、固定的策略集。有关详细信息，请参见类级别文档。
	 *
	 * @param strategies 要使用的策略列表
	 * @since 5.0
	 */
	public void setStrategies(@Nullable List<ContentNegotiationStrategy> strategies) {
		this.strategies = (strategies != null ? new ArrayList<>(strategies) : null);
	}

	/**
	 * 是否应该使用请求参数（默认为“format”）来确定请求的媒体类型。要使此选项起作用，必须注册{@link #setMediaTypes 媒体类型映射}。
	 * <p>默认设置为{@code false}。
	 *
	 * @see #setParameterName
	 */
	public void setFavorParameter(boolean favorParameter) {
		this.favorParameter = favorParameter;
	}

	/**
	 * 设置在{@link #setFavorParameter}为开启状态时要使用的查询参数名称。
	 * <p>默认参数名称为{@code "format"}。
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "parameterName is required");
		this.parameterName = parameterName;
	}

	/**
	 * 是否应使用URL路径中的路径扩展来确定请求的媒体类型。
	 * <p>默认情况下，此设置为{@code false}，在这种情况下，路径扩展不会影响内容协商。
	 *
	 * @deprecated 自5.2.4起。请参阅类级别注释，了解有关路径扩展配置选项的弃用说明。由于此方法没有替代方法，
	 * 在5.2.x中需要将其设置为{@code false}。在5.3中，默认值更改为{@code false}，并且不再需要使用此属性。
	 */
	@Deprecated
	public void setFavorPathExtension(boolean favorPathExtension) {
		this.favorPathExtension = favorPathExtension;
	}

	/**
	 * 将键映射到MediaType的映射，其中键被标准化为小写，并可能已从路径扩展、文件名扩展或作为查询参数传递中提取。
	 * <p>{@link #setFavorParameter(boolean) 参数策略}要求这样的映射才能工作，而{@link #setFavorPathExtension(boolean) 路径扩展策略}可以通过
	 * {@link ServletContext#getMimeType}和{@link org.springframework.http.MediaTypeFactory}的查找来回退。
	 * <p><strong>注意：</strong>在这里注册的映射可以通过{@link ContentNegotiationManager#getMediaTypeMappings()}访问，并且不仅可以在参数和路径扩展策略中使用。
	 * 例如，在Spring MVC配置中，例如{@code @EnableWebMvc}或{@code <mvc:annotation-driven>}，媒体类型映射也被插入到以下内容中：
	 * <ul>
	 * <li>确定使用{@code ResourceHttpRequestHandler}提供的静态资源的媒体类型。
	 * <li>确定使用{@code ContentNegotiatingViewResolver}渲染的视图的媒体类型。
	 * <li>列出用于RFD攻击检测的安全扩展（有关详细信息，请参阅Spring Framework参考文档）。
	 * </ul>
	 *
	 * @param mediaTypes 媒体类型映射
	 * @see #addMediaType(String, MediaType)
	 * @see #addMediaTypes(Map)
	 */
	public void setMediaTypes(Properties mediaTypes) {
		// 如果媒体类型列表不为空
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			// 遍历媒体类型列表，将每个媒体类型添加到媒体类型映射中
			mediaTypes.forEach((key, value) ->
					addMediaType((String) key, MediaType.valueOf((String) value)));
		}
	}

	/**
	 * 用于编程注册的{@link #setMediaTypes}的替代方法。
	 */
	public void addMediaType(String key, MediaType mediaType) {
		this.mediaTypes.put(key.toLowerCase(Locale.ENGLISH), mediaType);
	}

	/**
	 * 用于编程注册的{@link #setMediaTypes}的替代方法。
	 */
	public void addMediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
		// 如果媒体类型列表不为 null
		if (mediaTypes != null) {
			// 遍历媒体类型列表，并将每个媒体类型添加到媒体类型映射中
			mediaTypes.forEach(this::addMediaType);
		}
	}

	/**
	 * 是否忽略无法解析为任何媒体类型的路径扩展名的请求。将此设置为 {@code false} 将导致
	 * 如果没有匹配项，则会抛出 {@code HttpMediaTypeNotAcceptableException}。
	 * <p>默认情况下，此设置为 {@code true}。
	 *
	 * @deprecated 自 5.2.4 起。请参阅类级别的有关路径扩展配置选项停用的说明。
	 */
	@Deprecated
	public void setIgnoreUnknownPathExtensions(boolean ignore) {
		this.ignoreUnknownPathExtensions = ignore;
	}

	/**
	 * 指示是否使用 Java Activation Framework 作为将文件扩展名映射到媒体类型的备用选项。
	 *
	 * @deprecated 自 5.0 起，改用 {@link #setUseRegisteredExtensionsOnly(boolean)}，
	 * 其行为相反。
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	/**
	 * 当 {@link #setFavorPathExtension favorPathExtension} 或
	 * {@link #setFavorParameter(boolean)} 设置时，此属性确定是否仅使用已注册的
	 * {@code MediaType} 映射，还是允许动态解析，例如通过 {@link MediaTypeFactory}。
	 * <p>默认情况下，此值未设置，此时动态解析开启。
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	private boolean useRegisteredExtensionsOnly() {
		return (this.useRegisteredExtensionsOnly != null && this.useRegisteredExtensionsOnly);
	}

	/**
	 * 是否禁用检查 'Accept' 请求头。
	 * <p>默认情况下，此值设置为 {@code false}。
	 */
	public void setIgnoreAcceptHeader(boolean ignoreAcceptHeader) {
		this.ignoreAcceptHeader = ignoreAcceptHeader;
	}

	/**
	 * 设置在没有请求内容类型时使用的默认内容类型。
	 * <p>默认情况下，此值未设置。
	 *
	 * @see #setDefaultContentTypeStrategy
	 */
	public void setDefaultContentType(MediaType contentType) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentType);
	}

	/**
	 * 设置在没有请求内容类型时使用的默认内容类型。
	 * <p>默认情况下，此值未设置。
	 *
	 * @see #setDefaultContentTypeStrategy
	 * @since 5.0
	 */
	public void setDefaultContentTypes(List<MediaType> contentTypes) {
		this.defaultNegotiationStrategy = new FixedContentNegotiationStrategy(contentTypes);
	}

	/**
	 * 设置一个自定义的 {@link ContentNegotiationStrategy} 以确定在没有请求内容类型时使用的内容类型。
	 * <p>默认情况下，此值未设置。
	 *
	 * @see #setDefaultContentType
	 * @since 4.1.2
	 */
	public void setDefaultContentTypeStrategy(ContentNegotiationStrategy strategy) {
		this.defaultNegotiationStrategy = strategy;
	}

	/**
	 * 由 Spring 调用以注入 ServletContext。
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		build();
	}

	/**
	 * 创建并初始化一个{@link ContentNegotiationManager}实例。
	 *
	 * @since 5.0
	 */
	@SuppressWarnings("deprecation")
	public ContentNegotiationManager build() {
		// 创建内容协商策略列表
		List<ContentNegotiationStrategy> strategies = new ArrayList<>();

		// 如果策略列表不为空，则将现有策略添加到列表中
		if (this.strategies != null) {
			strategies.addAll(this.strategies);
		} else {
			// 如果偏好路径扩展
			if (this.favorPathExtension) {
				// 创建路径扩展内容协商策略
				PathExtensionContentNegotiationStrategy strategy;
				if (this.servletContext != null && !useRegisteredExtensionsOnly()) {
					// 如果servlet上下文不为空且不仅使用注册的扩展名，则创建Servlet路径扩展内容协商策略
					strategy = new ServletPathExtensionContentNegotiationStrategy(this.servletContext, this.mediaTypes);
				} else {
					// 否则创建路径扩展内容协商策略
					strategy = new PathExtensionContentNegotiationStrategy(this.mediaTypes);
				}
				// 设置是否忽略未知扩展名
				strategy.setIgnoreUnknownExtensions(this.ignoreUnknownPathExtensions);
				if (this.useRegisteredExtensionsOnly != null) {
					// 如果仅使用注册的扩展名，则设置为true
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				}
				// 将策略添加到列表中
				strategies.add(strategy);
			}
			// 如果偏好参数
			if (this.favorParameter) {
				// 创建参数内容协商策略
				ParameterContentNegotiationStrategy strategy = new ParameterContentNegotiationStrategy(this.mediaTypes);
				// 设置参数名称
				strategy.setParameterName(this.parameterName);
				if (this.useRegisteredExtensionsOnly != null) {
					// 如果仅使用注册的扩展名，则设置为true
					strategy.setUseRegisteredExtensionsOnly(this.useRegisteredExtensionsOnly);
				} else {
					// 否则设置为true（向后兼容）
					strategy.setUseRegisteredExtensionsOnly(true);
				}
				// 将策略添加到列表中
				strategies.add(strategy);
			}
			// 如果不忽略Accept头
			if (!this.ignoreAcceptHeader) {
				// 添加头部内容协商策略到列表中
				strategies.add(new HeaderContentNegotiationStrategy());
			}
			// 如果默认协商策略不为空，则将其添加到列表中
			if (this.defaultNegotiationStrategy != null) {
				strategies.add(this.defaultNegotiationStrategy);
			}
		}

		// 创建内容协商管理器
		this.contentNegotiationManager = new ContentNegotiationManager(strategies);

		// 确保媒体类型映射可通过ContentNegotiationManager#getMediaTypeMappings()获取，
		// 与路径扩展或参数策略无关。

		// 如果媒体类型不为空且不偏好路径扩展和参数
		if (!CollectionUtils.isEmpty(this.mediaTypes) && !this.favorPathExtension && !this.favorParameter) {
			// 添加文件扩展名解析器
			this.contentNegotiationManager.addFileExtensionResolvers(
					new MappingMediaTypeFileExtensionResolver(this.mediaTypes));
		}

		// 返回内容协商管理器
		return this.contentNegotiationManager;
	}


	@Override
	@Nullable
	public ContentNegotiationManager getObject() {
		return this.contentNegotiationManager;
	}

	@Override
	public Class<?> getObjectType() {
		return ContentNegotiationManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
