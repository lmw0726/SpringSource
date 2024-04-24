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

package org.springframework.web.servlet.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link FlashMapManager}实现的基类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1.1
 */
public abstract class AbstractFlashMapManager implements FlashMapManager {

	/**
	 * 默认的闪存映射互斥对象
	 */
	private static final Object DEFAULT_FLASH_MAPS_MUTEX = new Object();

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 闪存映射过期时间，单位为秒，默认为180秒
	 */
	private int flashMapTimeout = 180;

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;


	/**
	 * 设置FlashMap保存后（在请求完成时）到期之前的时间（以秒为单位）。
	 * <p>默认值为180秒。
	 */
	public void setFlashMapTimeout(int flashMapTimeout) {
		this.flashMapTimeout = flashMapTimeout;
	}

	/**
	 * 返回FlashMap到期之前的时间量（以秒为单位）。
	 */
	public int getFlashMapTimeout() {
		return this.flashMapTimeout;
	}

	/**
	 * 设置要用于将FlashMap实例与请求进行匹配的UrlPathHelper。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回要使用的UrlPathHelper实现。
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	@Override
	@Nullable
	public final FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response) {
		// 检索请求中的所有 FlashMap
		List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
		if (CollectionUtils.isEmpty(allFlashMaps)) {
			// 如果 FlashMap 为空，返回 null
			return null;
		}

		// 获取过期的 FlashMap
		List<FlashMap> mapsToRemove = getExpiredFlashMaps(allFlashMaps);
		// 获取与当前请求匹配的 FlashMap
		FlashMap match = getMatchingFlashMap(allFlashMaps, request);
		if (match != null) {
			// 如果找到匹配的 FlashMap，将其添加到需要移除的 FlashMap 列表中
			mapsToRemove.add(match);
		}

		if (!mapsToRemove.isEmpty()) {
			// 如果有 FlashMap 需要移除
			Object mutex = getFlashMapsMutex(request);
			if (mutex != null) {
				// 使用互斥锁进行同步
				synchronized (mutex) {
					// 重新检索 FlashMap，并移除需要移除的 FlashMap
					allFlashMaps = retrieveFlashMaps(request);
					if (allFlashMaps != null) {
						allFlashMaps.removeAll(mapsToRemove);
						// 更新 FlashMap
						updateFlashMaps(allFlashMaps, request, response);
					}
				}
			} else {
				// 如果没有互斥锁，直接移除 FlashMap
				allFlashMaps.removeAll(mapsToRemove);
				// 更新 FlashMap
				updateFlashMaps(allFlashMaps, request, response);
			}
		}

		return match;
	}

	/**
	 * 返回给定列表中包含的已过期FlashMap实例的列表。
	 */
	private List<FlashMap> getExpiredFlashMaps(List<FlashMap> allMaps) {
		List<FlashMap> result = new ArrayList<>();
		// 遍历所有的闪存映射
		for (FlashMap map : allMaps) {
			if (map.isExpired()) {
				// 如果闪存映射是过期的，则添加到结果集中
				result.add(map);
			}
		}
		return result;
	}

	/**
	 * 返回给定列表中包含的与请求匹配的FlashMap。
	 *
	 * @return 匹配的FlashMap或{@code null}
	 */
	@Nullable
	private FlashMap getMatchingFlashMap(List<FlashMap> allMaps, HttpServletRequest request) {
		// 创建一个列表以存储匹配的 FlashMap
		List<FlashMap> result = new ArrayList<>();
		// 遍历所有 FlashMap
		for (FlashMap flashMap : allMaps) {
			// 检查 FlashMap 是否与当前请求匹配
			if (isFlashMapForRequest(flashMap, request)) {
				// 如果匹配，则将其添加到结果列表中
				result.add(flashMap);
			}
		}
		if (!result.isEmpty()) {
			// 如果结果列表不为空，对其进行排序
			Collections.sort(result);
			if (logger.isTraceEnabled()) {
				logger.trace("Found " + result.get(0));
			}
			// 返回排序后的第一个 FlashMap
			return result.get(0);
		}
		// 如果没有匹配的 FlashMap，则返回 null
		return null;
	}

	/**
	 * 指定的FlashMap是否与当前请求匹配。
	 * 使用FlashMap中保存的预期请求路径和查询参数。
	 */
	protected boolean isFlashMapForRequest(FlashMap flashMap, HttpServletRequest request) {
		String expectedPath = flashMap.getTargetRequestPath();
		if (expectedPath != null) {
			String requestUri = getUrlPathHelper().getOriginatingRequestUri(request);
			if (!requestUri.equals(expectedPath) && !requestUri.equals(expectedPath + "/")) {
				return false;
			}
		}
		MultiValueMap<String, String> actualParams = getOriginatingRequestParams(request);
		MultiValueMap<String, String> expectedParams = flashMap.getTargetRequestParams();
		for (Map.Entry<String, List<String>> entry : expectedParams.entrySet()) {
			List<String> actualValues = actualParams.get(entry.getKey());
			if (actualValues == null) {
				return false;
			}
			for (String expectedValue : entry.getValue()) {
				if (!actualValues.contains(expectedValue)) {
					return false;
				}
			}
		}
		return true;
	}

	private MultiValueMap<String, String> getOriginatingRequestParams(HttpServletRequest request) {
		String query = getUrlPathHelper().getOriginatingQueryString(request);
		return ServletUriComponentsBuilder.fromPath("/").query(query).build().getQueryParams();
	}

	@Override
	public final void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response) {
		if (CollectionUtils.isEmpty(flashMap)) {
			// 如果 FlashMap 为空，直接返回
			return;
		}

		// 解码并规范化 FlashMap 的目标请求路径
		String path = decodeAndNormalizePath(flashMap.getTargetRequestPath(), request);
		// 设置目标路径
		flashMap.setTargetRequestPath(path);

		// 启动 FlashMap 的过期周期
		flashMap.startExpirationPeriod(getFlashMapTimeout());

		// 获取 FlashMap 的互斥锁对象
		Object mutex = getFlashMapsMutex(request);
		if (mutex != null) {
			// 如果互斥锁对象不为空，则在互斥锁对象上同步操作
			synchronized (mutex) {
				// 获取当前请求的所有 FlashMap
				List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
				// 如果当前请求的 FlashMap 为空，则创建一个新的 CopyOnWriteArrayList
				allFlashMaps = (allFlashMaps != null ? allFlashMaps : new CopyOnWriteArrayList<>());
				// 将新的 FlashMap 添加到所有 FlashMap 中
				allFlashMaps.add(flashMap);
				// 更新 FlashMap
				updateFlashMaps(allFlashMaps, request, response);
			}
		} else {
			// 如果互斥锁对象为空，则直接操作 FlashMap
			List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
			// 如果当前请求的 FlashMap 为空，则创建一个新的 ArrayList
			allFlashMaps = (allFlashMaps != null ? allFlashMaps : new ArrayList<>(1));
			// 将新的 FlashMap 添加到所有 FlashMap 中
			allFlashMaps.add(flashMap);
			// 更新 FlashMap
			updateFlashMaps(allFlashMaps, request, response);
		}
	}

	@Nullable
	private String decodeAndNormalizePath(@Nullable String path, HttpServletRequest request) {
		if (path != null && !path.isEmpty()) {
			// 解码请求路径中的特殊字符
			path = getUrlPathHelper().decodeRequestString(request, path);
			if (path.charAt(0) != '/') {
				// 如果路径不是以 '/' 开头，则获取请求的 URI
				String requestUri = getUrlPathHelper().getRequestUri(request);
				// 将路径添加到请求 URI 的最后一个 '/' 之后，并清理路径
				path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
				path = StringUtils.cleanPath(path);
			}
		}
		return path;
	}

	/**
	 * 从底层存储中检索已保存的FlashMap实例。
	 *
	 * @param request 当前请求
	 * @return 包含FlashMap实例的列表，如果没有找到则返回{@code null}
	 */
	@Nullable
	protected abstract List<FlashMap> retrieveFlashMaps(HttpServletRequest request);

	/**
	 * 更新底层存储中的FlashMap实例。
	 *
	 * @param flashMaps 要保存的（可能为空的）FlashMap实例列表
	 * @param request   当前请求
	 * @param response  当前响应
	 */
	protected abstract void updateFlashMaps(
			List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 获取用于修改由{@link #retrieveFlashMaps}和{@link #updateFlashMaps}处理的FlashMap列表的互斥锁。
	 * <p>默认实现返回一个共享的静态互斥锁。
	 * 鼓励子类返回一个更具体的互斥锁，或者返回{@code null}以指示不需要同步。
	 *
	 * @param request 当前请求
	 * @return 要使用的互斥锁（如果没有适用的则可能为{@code null}）
	 * @since 4.0.3
	 */
	@Nullable
	protected Object getFlashMapsMutex(HttpServletRequest request) {
		return DEFAULT_FLASH_MAPS_MUTEX;
	}

}
