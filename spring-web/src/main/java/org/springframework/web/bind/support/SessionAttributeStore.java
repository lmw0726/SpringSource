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

package org.springframework.web.bind.support;

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.WebRequest;

/**
 * 用于在后端会话中存储模型属性的策略接口。
 *
 * @author Juergen Hoeller
 * @see org.springframework.web.bind.annotation.SessionAttributes
 * @since 2.5
 */
public interface SessionAttributeStore {

	/**
	 * 将提供的属性存储在后端会话中。
	 * <p>可以为新属性调用，也可以为现有属性调用。
	 * 在后一种情况下，这表明属性值可能已被修改。
	 *
	 * @param request        当前请求
	 * @param attributeName  属性的名称
	 * @param attributeValue 要存储的属性值
	 */
	void storeAttribute(WebRequest request, String attributeName, Object attributeValue);

	/**
	 * 从后端会话中检索指定的属性。
	 * <p>通常会在期望属性已经存在的情况下调用，如果此方法返回{@code null}，则会抛出异常。
	 *
	 * @param request       当前请求
	 * @param attributeName 属性的名称
	 * @return 当前的属性值，如果没有则返回{@code null}
	 */
	@Nullable
	Object retrieveAttribute(WebRequest request, String attributeName);

	/**
	 * 清理后端会话中指定的属性。
	 * <p>表示该属性名称将不再使用。
	 *
	 * @param request       当前请求
	 * @param attributeName 属性的名称
	 */
	void cleanupAttribute(WebRequest request, String attributeName);

}
