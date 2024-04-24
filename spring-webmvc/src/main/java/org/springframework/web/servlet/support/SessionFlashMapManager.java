/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 将{@link FlashMap}实例存储到HTTP会话中，并从中检索。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1.1
 */
public class SessionFlashMapManager extends AbstractFlashMapManager {
	/**
	 * Flash映射会话属性
	 */
	private static final String FLASH_MAPS_SESSION_ATTRIBUTE = SessionFlashMapManager.class.getName() + ".FLASH_MAPS";


	/**
	 * 从HTTP会话中检索保存的FlashMap实例（如果有）。
	 */
	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
		// 获取当前请求的 HttpSession，如果不存在则返回 null
		HttpSession session = request.getSession(false);
		// 如果 HttpSession 存在，则尝试获取其中的 FlashMap 列表
		return (session != null ? (List<FlashMap>) session.getAttribute(FLASH_MAPS_SESSION_ATTRIBUTE) : null);
	}

	/**
	 * 将给定的FlashMap实例保存在HTTP会话中。
	 */
	@Override
	protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response) {
		// 如果 flashMaps 不为空，则将其设置为请求的 HttpSession 属性 FLASH_MAPS_SESSION_ATTRIBUTE
		// 如果 flashMaps 为空，则将 FLASH_MAPS_SESSION_ATTRIBUTE 属性从 HttpSession 中移除
		WebUtils.setSessionAttribute(request, FLASH_MAPS_SESSION_ATTRIBUTE, (!flashMaps.isEmpty() ? flashMaps : null));
	}

	/**
	 * 公开最佳可用会话互斥体。
	 *
	 * @see org.springframework.web.util.WebUtils#getSessionMutex
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 */
	@Override
	protected Object getFlashMapsMutex(HttpServletRequest request) {
		// 获取与给定 HttpSession 相关联的会话互斥对象（session mutex）
		return WebUtils.getSessionMutex(request.getSession());
	}

}
