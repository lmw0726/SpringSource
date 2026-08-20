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

package org.springframework.ui.context;

import org.springframework.lang.Nullable;

/**
 * ThemeSource 的子接口，由能够以层级方式解析主题消息的对象实现。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 */
public interface HierarchicalThemeSource extends ThemeSource {


	/**
	 * 设置父 ThemeSource，用于尝试解析当前对象无法解析的主题消息。
	 * @param parent 用于解析当前对象无法解析的消息的父 ThemeSource。
	 * 可以为 {@code null}，此时将无法进行进一步的解析。
	 */
	void setParentThemeSource(@Nullable ThemeSource parent);

	/**
	 * 返回此 ThemeSource 的父级，如果没有则返回 {@code null}。
	 */
	@Nullable
	ThemeSource getParentThemeSource();

}
