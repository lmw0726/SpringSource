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

	private static final Object DEFAULT_FLASH_MAPS_MUTEX = new Object();


	protected final Log logger = LogFactory.getLog(getClass());

	private int flashMapTimeout = 180;

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
		List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
		if (CollectionUtils.isEmpty(allFlashMaps)) {
			return null;
		}

		List<FlashMap> mapsToRemove = getExpiredFlashMaps(allFlashMaps);
		FlashMap match = getMatchingFlashMap(allFlashMaps, request);
		if (match != null) {
			mapsToRemove.add(match);
		}

		if (!mapsToRemove.isEmpty()) {
			Object mutex = getFlashMapsMutex(request);
			if (mutex != null) {
				synchronized (mutex) {
					allFlashMaps = retrieveFlashMaps(request);
					if (allFlashMaps != null) {
						allFlashMaps.removeAll(mapsToRemove);
						updateFlashMaps(allFlashMaps, request, response);
					}
				}
			} else {
				allFlashMaps.removeAll(mapsToRemove);
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
		for (FlashMap map : allMaps) {
			if (map.isExpired()) {
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
		List<FlashMap> result = new ArrayList<>();
		for (FlashMap flashMap : allMaps) {
			if (isFlashMapForRequest(flashMap, request)) {
				result.add(flashMap);
			}
		}
		if (!result.isEmpty()) {
			Collections.sort(result);
			if (logger.isTraceEnabled()) {
				logger.trace("Found " + result.get(0));
			}
			return result.get(0);
		}
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
			return;
		}

		String path = decodeAndNormalizePath(flashMap.getTargetRequestPath(), request);
		flashMap.setTargetRequestPath(path);

		flashMap.startExpirationPeriod(getFlashMapTimeout());

		Object mutex = getFlashMapsMutex(request);
		if (mutex != null) {
			synchronized (mutex) {
				List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
				allFlashMaps = (allFlashMaps != null ? allFlashMaps : new CopyOnWriteArrayList<>());
				allFlashMaps.add(flashMap);
				updateFlashMaps(allFlashMaps, request, response);
			}
		} else {
			List<FlashMap> allFlashMaps = retrieveFlashMaps(request);
			allFlashMaps = (allFlashMaps != null ? allFlashMaps : new ArrayList<>(1));
			allFlashMaps.add(flashMap);
			updateFlashMaps(allFlashMaps, request, response);
		}
	}

	@Nullable
	private String decodeAndNormalizePath(@Nullable String path, HttpServletRequest request) {
		if (path != null && !path.isEmpty()) {
			path = getUrlPathHelper().decodeRequestString(request, path);
			if (path.charAt(0) != '/') {
				String requestUri = getUrlPathHelper().getRequestUri(request);
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
