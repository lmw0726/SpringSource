/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.validation;

import org.springframework.lang.Nullable;

/**
 * 从验证错误代码构建消息代码的策略接口。
 * 被 DataBinder 用于构建 ObjectError 和 FieldError 的代码列表。
 *
 * <p>生成的消息代码对应于 MessageSourceResolvable
 * （由 ObjectError 和 FieldError 实现）的代码。
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 * @see DataBinder#setMessageCodesResolver
 * @see ObjectError
 * @see FieldError
 * @see org.springframework.context.MessageSourceResolvable#getCodes()
 */
public interface MessageCodesResolver {

	/**
	 * 为给定的错误代码和对象名称构建消息代码。
	 * 用于构建 ObjectError 的代码列表。
	 * @param errorCode 用于拒绝对象的错误代码
	 * @param objectName 对象的名称
	 * @return 要使用的消息代码
	 */
	String[] resolveMessageCodes(String errorCode, String objectName);

	/**
	 * 为给定的错误代码和字段规格构建消息代码。
	 * 用于构建 FieldError 的代码列表。
	 * @param errorCode 用于拒绝值的错误代码
	 * @param objectName 对象的名称
	 * @param field 字段名称
	 * @param fieldType 字段类型（如果无法确定则为 {@code null}）
	 * @return 要使用的消息代码
	 */
	String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class<?> fieldType);

}
