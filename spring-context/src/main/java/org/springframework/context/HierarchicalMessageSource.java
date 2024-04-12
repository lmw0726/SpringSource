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

package org.springframework.context;

import org.springframework.lang.Nullable;

/**
 * 由能够分层解析消息的对象实现的MessageSource的子接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface HierarchicalMessageSource extends MessageSource {

	/**
	 * 设置将用于尝试解析此对象无法解析的消息的父级。
	 * @param parent 将用于解析此对象无法解析的消息的父级MessageSource。
	 * 可能为{@code null}，在这种情况下，将无法进行进一步的解析。
	 */
	void setParentMessageSource(@Nullable MessageSource parent);

	/**
	 * 返回此MessageSource的父级，如果没有则返回{@code null}。
	 */
	@Nullable
	MessageSource getParentMessageSource();

}
