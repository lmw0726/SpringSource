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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 用于检索和保存FlashMap实例的策略接口。
 * 有关Flash属性的概述，请参阅{@link FlashMap}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * 查找之前请求保存的FlashMap，该FlashMap与当前请求匹配，从底层存储中删除它，并删除其他过期的FlashMap实例。
	 * <p>与{@link #saveOutputFlashMap}相比，该方法在每个请求的开头调用，后者仅在需要保存Flash属性时调用 - 即在重定向之前。
	 *
	 * @param request  当前请求
	 * @param response 当前响应
	 * @return 匹配当前请求的FlashMap，如果没有则返回{@code null}
	 */
	@Nullable
	FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response);

	/**
	 * 保存给定的FlashMap，并在某个底层存储中设置其过期期限的开始。
	 * <p><strong>注意:</strong>在重定向之前调用此方法，以便在提交响应之前允许将FlashMap保存在HTTP会话或响应cookie中。
	 *
	 * @param flashMap 要保存的FlashMap
	 * @param request  当前请求
	 * @param response 当前响应
	 */
	void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);

}
