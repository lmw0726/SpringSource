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

package org.springframework.web.bind.support;

/**
 * {@link SessionStatus}接口的简单实现，
 * 将{@code complete}标志作为实例变量。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class SimpleSessionStatus implements SessionStatus {
	/**
	 * 是否完成
	 */
	private boolean complete = false;


	@Override
	public void setComplete() {
		this.complete = true;
	}

	@Override
	public boolean isComplete() {
		return this.complete;
	}

}
