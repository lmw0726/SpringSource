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

package org.springframework.beans.factory.parsing;

/**
 * {@link ReaderEventListener}接口的空实现，
 * 为所有回调方法提供无操作实现。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class EmptyReaderEventListener implements ReaderEventListener {

	@Override
	public void defaultsRegistered(DefaultsDefinition defaultsDefinition) {
		// 无操作
	}

	@Override
	public void componentRegistered(ComponentDefinition componentDefinition) {
		// 无操作
	}

	@Override
	public void aliasRegistered(AliasDefinition aliasDefinition) {
		// 无操作
	}

	@Override
	public void importProcessed(ImportDefinition importDefinition) {
		// 无操作
	}

}
