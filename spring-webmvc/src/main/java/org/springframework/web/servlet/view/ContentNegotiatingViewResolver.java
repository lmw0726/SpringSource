/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 基于请求文件名或{@code Accept}头解析视图的{@link ViewResolver}的实现。
 *
 * <p>{@code ContentNegotiatingViewResolver}本身不会解析视图，而是委托给其他{@link ViewResolver ViewResolvers}。
 * 默认情况下，这些其他视图解析器会自动从应用程序上下文中提取，但也可以通过使用{@link #setViewResolvers viewResolvers}属性来显式设置。
 * <strong>注意</strong>，为了使该视图解析器正常工作，{@link #setOrder order}属性需要设置为比其他视图解析器更高的优先级
 * （默认值为{@link Ordered#HIGHEST_PRECEDENCE}）。
 *
 * <p>此视图解析器使用请求的{@linkplain MediaType 媒体类型}来选择适当的{@link View}。
 * 通过配置的{@link ContentNegotiationManager}确定请求的媒体类型。一旦确定了请求的媒体类型，此解析器将查询每个委托视图解析器
 * 是否有适用于请求的媒体类型的{@link View}，并确定请求的媒体类型是否与视图的{@linkplain View#getContentType() 内容类型}兼容。
 * 返回最兼容的视图。
 *
 * <p>此外，此视图解析器公开了{@link #setDefaultViews(List) defaultViews}属性，允许您重写视图解析器提供的默认视图。
 * 请注意，这些默认视图将作为候选项提供，仍然需要通过请求的内容类型（通过文件扩展名、参数或上述{@code Accept}头）进行请求。
 *
 * <p>例如，如果请求路径为{@code /view.html}，此视图解析器将查找具有{@code text/html}内容类型的视图（基于{@code html}文件扩展名）。
 * 具有{@code text/html}请求{@code Accept}头的{@code /view}请求具有相同的结果。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see ViewResolver
 * @see InternalResourceViewResolver
 * @see BeanNameViewResolver
 * @since 3.0
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport
		implements ViewResolver, Ordered, InitializingBean {

	/**
	 * 内容协商管理器
	 */
	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	/**
	 * 内容协商管理器工厂Bean
	 */
	private final ContentNegotiationManagerFactoryBean cnmFactoryBean = new ContentNegotiationManagerFactoryBean();

	/**
	 * 是否使用不可接受的状态码
	 */
	private boolean useNotAcceptableStatusCode = false;

	/**
	 * 默认视图列表
	 */
	@Nullable
	private List<View> defaultViews;

	/**
	 * 视图解析器列表
	 */
	@Nullable
	private List<ViewResolver> viewResolvers;

	/**
	 * 顺序
	 */
	private int order = Ordered.HIGHEST_PRECEDENCE;


	/**
	 * 设置用于确定请求的媒体类型的{@link ContentNegotiationManager}。
	 * <p>如果未设置，则将使用ContentNegotiationManager的默认构造函数，
	 * 应用{@link org.springframework.web.accept.HeaderContentNegotiationStrategy}。
	 *
	 * @param contentNegotiationManager 要使用的内容协商管理器
	 * @see ContentNegotiationManager#ContentNegotiationManager()
	 */
	public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回用于确定请求的媒体类型的{@link ContentNegotiationManager}。
	 *
	 * @return 内容协商管理器
	 * @since 4.1.9
	 */
	@Nullable
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 指示是否应返回{@link HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable}状态码，
	 * 如果找不到合适的视图。
	 * <p>默认值为{@code false}，这意味着此视图解析器对{@link #resolveViewName(String, Locale)}返回{@code null}，
	 * 当找不到可接受的视图时。这将允许视图解析器链接。当此属性设置为{@code true}时，
	 * {@link #resolveViewName(String, Locale)}将响应具有{@code 406 Not Acceptable}状态的视图。
	 *
	 * @param useNotAcceptableStatusCode 是否使用不可接受的状态码
	 */
	public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
		this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
	}

	/**
	 * 返回是否在找不到合适的视图时返回HTTP状态406。
	 *
	 * @return 是否使用不可接受的状态码
	 */
	public boolean isUseNotAcceptableStatusCode() {
		return this.useNotAcceptableStatusCode;
	}

	/**
	 * 设置在无法从{@link ViewResolver}链获取更具体的视图时要使用的默认视图。
	 *
	 * @param defaultViews 默认视图列表
	 */
	public void setDefaultViews(List<View> defaultViews) {
		this.defaultViews = defaultViews;
	}

	/**
	 * 返回当无法从{@link ViewResolver}链获取更具体的视图时要使用的默认视图。
	 *
	 * @return 默认视图列表（不可修改）
	 */
	public List<View> getDefaultViews() {
		return (this.defaultViews != null ? Collections.unmodifiableList(this.defaultViews) :
				Collections.emptyList());
	}

	/**
	 * 设置要由此视图解析器包装的视图解析器。
	 * <p>如果未设置此属性，则会自动检测视图解析器。
	 *
	 * @param viewResolvers 视图解析器列表
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	public List<ViewResolver> getViewResolvers() {
		return (this.viewResolvers != null ? Collections.unmodifiableList(this.viewResolvers) :
				Collections.emptyList());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		// 获取所有匹配的ViewResolver bean
		Collection<ViewResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(obtainApplicationContext(), ViewResolver.class).values();
		if (this.viewResolvers == null) {
			// 如果当前视图解析器列表为空，则初始化为新的ArrayList，并将匹配的ViewResolver添加到列表中。
			this.viewResolvers = new ArrayList<>(matchingBeans.size());
			for (ViewResolver viewResolver : matchingBeans) {
				if (this != viewResolver) {
					this.viewResolvers.add(viewResolver);
				}
			}
		} else {
			// 否则，遍历当前视图解析器列表。
			for (int i = 0; i < this.viewResolvers.size(); i++) {
				ViewResolver vr = this.viewResolvers.get(i);
				// 如果匹配的ViewResolver列表包含当前ViewResolver，则继续下一次循环。
				if (matchingBeans.contains(vr)) {
					continue;
				}
				// 否则，为当前ViewResolver创建一个唯一的bean名称，并初始化该bean。
				String name = vr.getClass().getName() + i;
				obtainApplicationContext().getAutowireCapableBeanFactory().initializeBean(vr, name);
			}
		}

		// 对视图解析器进行排序
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
		// 设置CnmFactoryBean的ServletContext
		this.cnmFactoryBean.setServletContext(servletContext);
	}

	@Override
	public void afterPropertiesSet() {
		if (this.contentNegotiationManager == null) {
			// 如果内容协商管理器为空，则使用CnmFactoryBean构建一个。
			this.contentNegotiationManager = this.cnmFactoryBean.build();
		}
		if (this.viewResolvers == null || this.viewResolvers.isEmpty()) {
			// 如果视图解析器列表为空或者没有视图解析器配置，则记录警告消息。
			logger.warn("No ViewResolvers configured");
		}
	}


	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		// 获取请求属性
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		// 断言当前请求属性是Servlet请求属性
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		// 获取请求的媒体类型列表
		List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
		if (requestedMediaTypes != null) {
			// 获取候选视图列表
			List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
			// 获取最佳视图
			View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);
			if (bestView != null) {
				// 如果找到最佳视图，则返回它
				return bestView;
			}
		}

		// 记录关于媒体类型信息的调试日志
		String mediaTypeInfo = logger.isDebugEnabled() && requestedMediaTypes != null ?
				" given " + requestedMediaTypes.toString() : "";

		if (this.useNotAcceptableStatusCode) {
			// 如果使用406 NOT_ACCEPTABLE状态码
			if (logger.isDebugEnabled()) {
				logger.debug("Using 406 NOT_ACCEPTABLE" + mediaTypeInfo);
			}
			return NOT_ACCEPTABLE_VIEW;
		} else {
			// 否则，记录视图仍未解析的调试日志，并返回null
			logger.debug("View remains unresolved" + mediaTypeInfo);
			return null;
		}
	}

	/**
	 * 确定给定{@link HttpServletRequest}的{@link MediaType}列表。
	 *
	 * @param request 当前的servlet请求
	 * @return 如果有，则请求的媒体类型列表
	 */
	@Nullable
	protected List<MediaType> getMediaTypes(HttpServletRequest request) {
		Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
		try {
			// 将 HttpServletRequest 封装为 ServletWebRequest
			ServletWebRequest webRequest = new ServletWebRequest(request);
			// 解析可接受的媒体类型
			List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
			// 获取可生产的媒体类型
			List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);
			Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
			// 找到可兼容的媒体类型
			for (MediaType acceptable : acceptableMediaTypes) {
				for (MediaType producible : producibleMediaTypes) {
					if (acceptable.isCompatibleWith(producible)) {
						// 如果可接受的媒体类型兼容 可生产的媒体类型，则添加进 兼容的媒体类型 列表中
						compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
					}
				}
			}
			// 对兼容的媒体类型进行排序
			List<MediaType> selectedMediaTypes = new ArrayList<>(compatibleMediaTypes);
			MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
			return selectedMediaTypes;
		} catch (HttpMediaTypeNotAcceptableException ex) {
			// 如果出现HttpMediaTypeNotAcceptableException，则记录调试日志并返回null
			if (logger.isDebugEnabled()) {
				logger.debug(ex.getMessage());
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
		// 获取可生产的媒体类型
		Set<MediaType> mediaTypes = (Set<MediaType>) request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			// 如果可生产的媒体类型集合不为空，则返回一个新的ArrayList包含所有媒体类型。
			return new ArrayList<>(mediaTypes);
		} else {
			// 否则，返回一个包含MediaType.ALL的单元素列表。
			return Collections.singletonList(MediaType.ALL);
		}
	}

	/**
	 * 返回可接受的和可生产的媒体类型中更具体的，具有前者的q值。
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType);
	}

	private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes)
			throws Exception {

		List<View> candidateViews = new ArrayList<>();
		if (this.viewResolvers != null) {
			Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
			// 遍历视图解析器列表
			for (ViewResolver viewResolver : this.viewResolvers) {
				// 解析视图名称
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					// 如果解析到视图，则将其添加到候选视图列表中
					candidateViews.add(view);
				}
				// 遍历请求的媒体类型列表
				for (MediaType requestedMediaType : requestedMediaTypes) {
					// 解析文件扩展名
					List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);
					for (String extension : extensions) {
						// 构造带扩展名的视图名称
						String viewNameWithExtension = viewName + '.' + extension;
						// 解析带扩展名的视图名称
						view = viewResolver.resolveViewName(viewNameWithExtension, locale);
						if (view != null) {
							// 如果解析到带扩展名的视图，则将其添加到候选视图列表中
							candidateViews.add(view);
						}
					}
				}
			}
		}
		if (!CollectionUtils.isEmpty(this.defaultViews)) {
			// 将默认视图列表添加到候选视图列表中
			candidateViews.addAll(this.defaultViews);
		}
		return candidateViews;
	}

	@Nullable
	private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {
		for (View candidateView : candidateViews) {
			// 如果候选视图是SmartView
			if (candidateView instanceof SmartView) {
				SmartView smartView = (SmartView) candidateView;
				// 如果是重定向视图，则直接返回候选视图
				if (smartView.isRedirectView()) {
					return candidateView;
				}
			}
		}

		for (MediaType mediaType : requestedMediaTypes) {
			// 遍历请求的媒体类型列表
			for (View candidateView : candidateViews) {
				// 如果候选视图的内容类型不为空
				if (StringUtils.hasText(candidateView.getContentType())) {
					// 解析候选视图的内容类型
					MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
					// 如果请求的媒体类型与候选视图的内容类型兼容
					if (mediaType.isCompatibleWith(candidateContentType)) {
						// 移除媒体类型的质量值
						mediaType = mediaType.removeQualityValue();
						if (logger.isDebugEnabled()) {
							logger.debug("Selected '" + mediaType + "' given " + requestedMediaTypes);
						}
						// 将选择的内容类型设置为请求属性
						attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST);
						return candidateView;
					}
				}
			}
		}

		// 如果没有选择的视图，则返回null
		return null;
	}


	private static final View NOT_ACCEPTABLE_VIEW = new View() {

		@Override
		@Nullable
		public String getContentType() {
			return null;
		}

		@Override
		public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
			// 设置为406的响应码
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	};

}
