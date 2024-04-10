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

package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于{@link org.springframework.web.servlet.ViewResolver}实现的便捷基类。
 * 一旦解析，它会缓存{@link org.springframework.web.servlet.View}对象：
 * 这意味着无论初始视图检索的成本如何，视图解析都不会成为性能问题。
 *
 * <p>子类需要实现{@link #loadView}模板方法，为特定的视图名称和区域设置构建View对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #loadView
 */
public abstract class AbstractCachingViewResolver extends WebApplicationObjectSupport implements ViewResolver {

	/**
	 * 视图缓存的默认最大条目数：1024。
	 */
	public static final int DEFAULT_CACHE_LIMIT = 1024;

	/**
	 * 缓存Maps中未解析视图的虚拟标记对象。
	 */
	private static final View UNRESOLVED_VIEW = new View() {
		@Override
		@Nullable
		public String getContentType() {
			return null;
		}

		@Override
		public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
		}
	};

	/**
	 * 默认的缓存过滤器，始终进行缓存。
	 */
	private static final CacheFilter DEFAULT_CACHE_FILTER = (view, viewName, locale) -> true;

	/**
	 * 缓存中的条目最大数量。
	 */
	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	/**
	 * 如果视图一旦未解析，是否应避免再次解析。
	 */
	private boolean cacheUnresolved = true;

	/**
	 * 确定是否应将视图缓存的过滤器功能。
	 */
	private CacheFilter cacheFilter = DEFAULT_CACHE_FILTER;

	/**
	 * 用于快速访问视图的缓存，返回已经缓存的实例而不需要全局锁。
	 */
	private final Map<Object, View> viewAccessCache = new ConcurrentHashMap<>(DEFAULT_CACHE_LIMIT);

	/**
	 * 从视图键到视图实例的映射，用于同步视图创建。
	 */
	@SuppressWarnings("serial")
	private final Map<Object, View> viewCreationCache =
			new LinkedHashMap<Object, View>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Object, View> eldest) {
					if (size() > getCacheLimit()) {
						viewAccessCache.remove(eldest.getKey());
						return true;
					} else {
						return false;
					}
				}
			};


	/**
	 * 指定视图缓存的最大条目数。
	 * 默认值为1024。
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * 返回视图缓存的最大条目数。
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * 启用或禁用缓存。
	 * <p>这相当于将{@link #setCacheLimit "cacheLimit"}属性设置为默认限制（1024）或0。
	 * <p>默认值为"true"：启用缓存。
	 * 仅在调试和开发时禁用此选项。
	 */
	public void setCache(boolean cache) {
		this.cacheLimit = (cache ? DEFAULT_CACHE_LIMIT : 0);
	}

	/**
	 * 返回是否启用了缓存。
	 */
	public boolean isCache() {
		return (this.cacheLimit > 0);
	}

	/**
	 * 一旦解析为{@code null}的视图名称是否应该被缓存，并且随后自动解析为{@code null}。
	 * <p>默认值为"true"：自Spring 3.1起，未解析的视图名称将被缓存。
	 * 请注意，此标志仅在一般的{@link #setCache "cache"}标志保持其默认值"true"的情况下才适用。
	 * <p>特别感兴趣的是，某些AbstractUrlBasedView实现（如FreeMarker、Tiles）通过{@link AbstractUrlBasedView#checkResource(Locale)}检查基础资源是否存在。
	 * 将此标志设置为"false"，则会注意到并使用重新出现的基础资源。
	 * 将此标志设置为"true"，则仅进行一次检查。
	 */
	public void setCacheUnresolved(boolean cacheUnresolved) {
		this.cacheUnresolved = cacheUnresolved;
	}

	/**
	 * 返回是否启用了未解析视图的缓存。
	 */
	public boolean isCacheUnresolved() {
		return this.cacheUnresolved;
	}

	/**
	 * 设置确定视图是否应该被缓存的过滤器。
	 * 默认行为是缓存所有视图。
	 *
	 * @since 5.2
	 */
	public void setCacheFilter(CacheFilter cacheFilter) {
		Assert.notNull(cacheFilter, "CacheFilter must not be null");
		this.cacheFilter = cacheFilter;
	}

	/**
	 * 返回确定视图是否应该被缓存的过滤器函数。
	 *
	 * @since 5.2
	 */
	public CacheFilter getCacheFilter() {
		return this.cacheFilter;
	}

	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		// 如果不使用缓存，则直接创建视图对象
		if (!isCache()) {
			return createView(viewName, locale);
		} else {
			// 计算缓存键
			Object cacheKey = getCacheKey(viewName, locale);
			// 从缓存中获取视图对象
			View view = this.viewAccessCache.get(cacheKey);
			// 如果缓存中没有找到视图对象
			if (view == null) {
				synchronized (this.viewCreationCache) {
					// 再次尝试从缓存中获取视图对象
					view = this.viewCreationCache.get(cacheKey);
					if (view == null) {
						// 调用子类方法创建视图对象
						view = createView(viewName, locale);
						// 如果视图对象为空且允许缓存未解析的视图，则将UNRESOLVED_VIEW添加到缓存中
						if (view == null && this.cacheUnresolved) {
							view = UNRESOLVED_VIEW;
						}
						// 如果视图对象不为空且通过缓存过滤器，则将其添加到视图缓存中
						if (view != null && this.cacheFilter.filter(view, viewName, locale)) {
							this.viewAccessCache.put(cacheKey, view);
							this.viewCreationCache.put(cacheKey, view);
						}
					}
				}
			} else {
				// 如果缓存中找到视图对象，则记录调试日志
				if (logger.isTraceEnabled()) {
					logger.trace(formatKey(cacheKey) + " served from cache");
				}
			}
			return (view != UNRESOLVED_VIEW ? view : null);
		}
	}

	private static String formatKey(Object cacheKey) {
		return "View with key [" + cacheKey + "] ";
	}

	/**
	 * 返回给定视图名称和给定语言环境的缓存键。
	 * <p>默认是由视图名称和语言环境后缀组成的字符串。
	 * 可以在子类中重写。
	 * <p>通常需要考虑语言环境，因为不同的语言环境可能会导致不同的视图资源。
	 */
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName + '_' + locale;
	}

	/**
	 * 提供清除特定视图缓存的功能。
	 * <p>如果开发人员能够在运行时修改视图（例如FreeMarker模板），则需要清除指定视图的缓存。
	 *
	 * @param viewName 需要删除缓存的视图名称（如果有的话）
	 * @param locale   应删除视图对象的区域设置
	 */
	public void removeFromCache(String viewName, Locale locale) {
		if (!isCache()) {
			// 如果缓存关闭，则记录警告日志
			logger.warn("Caching is OFF (removal not necessary)");
		} else {
			// 计算缓存键
			Object cacheKey = getCacheKey(viewName, locale);
			Object cachedView;
			synchronized (this.viewCreationCache) {
				// 从视图访问缓存中移除缓存键对应的视图对象
				this.viewAccessCache.remove(cacheKey);
				// 从视图创建缓存中移除缓存键对应的视图对象，并获取被移除的视图对象
				cachedView = this.viewCreationCache.remove(cacheKey);
			}
			if (logger.isDebugEnabled()) {
				// 记录调试日志，说明是否成功从缓存中移除了视图对象
				logger.debug(formatKey(cacheKey) +
						(cachedView != null ? " cleared from cache" : " not found in the cache"));
			}
		}
	}

	/**
	 * 清除整个视图缓存，删除所有缓存的视图对象。
	 * 后续的解析调用将导致重新创建所需的视图对象。
	 */
	public void clearCache() {
		logger.debug("Clearing all views from the cache");
		synchronized (this.viewCreationCache) {
			this.viewAccessCache.clear();
			this.viewCreationCache.clear();
		}
	}


	/**
	 * 创建实际的View对象。
	 * <p>默认实现委托给{@link #loadView}。
	 * 这可以被重写以特殊方式解析某些视图名称，然后再委托给子类提供的实际{@code loadView}实现。
	 *
	 * @param viewName 要检索的视图的名称
	 * @param locale   用于检索视图的区域设置
	 * @return View实例，如果未找到则为{@code null}（可选，以允许ViewResolver链式处理）
	 * @throws Exception 如果无法解析视图
	 * @see #loadView
	 */
	@Nullable
	protected View createView(String viewName, Locale locale) throws Exception {
		return loadView(viewName, locale);
	}

	/**
	 * 子类必须实现此方法，为指定的视图构建View对象。
	 * 返回的View对象将由此ViewResolver基类缓存。
	 * <p>子类不强制支持国际化：
	 * 不支持国际化的子类可以简单地忽略locale参数。
	 *
	 * @param viewName 要检索的视图的名称
	 * @param locale   用于检索视图的区域设置
	 * @return View实例，如果未找到则为{@code null}（可选，以允许ViewResolver链式处理）
	 * @throws Exception 如果无法解析视图
	 * @see #resolveViewName
	 */
	@Nullable
	protected abstract View loadView(String viewName, Locale locale) throws Exception;

	/**
	 * 过滤器，确定是否应该缓存视图。
	 */
	@FunctionalInterface
	public interface CacheFilter {

		/**
		 * 指示是否应该缓存给定的视图。
		 * 还提供用于解析视图的名称和区域设置。
		 *
		 * @param view     视图
		 * @param viewName 用于解析{@code view}的名称
		 * @param locale   用于解析{@code view}的区域设置
		 * @return 如果应该缓存视图，则为{@code true}；否则为{@code false}
		 */
		boolean filter(View view, String viewName, Locale locale);
	}

}
