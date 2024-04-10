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

package org.springframework.web.servlet;

/**
 * 提供有关视图的其他信息，例如它是否执行重定向。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface SmartView extends View {

	/**
	 * 视图是否执行重定向。
	 */
	boolean isRedirectView();

}
