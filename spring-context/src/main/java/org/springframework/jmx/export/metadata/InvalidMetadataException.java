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

package org.springframework.jmx.export.metadata;

import org.springframework.jmx.JmxException;

/**
 * 当 {@code JmxAttributeSource} 在受管资源或其方法上遇到
 * 不正确的元数据时抛出此异常。
 *
 * @author Rob Harrop
 * @since 1.2
 * @see JmxAttributeSource
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler
 */
@SuppressWarnings("serial")
public class InvalidMetadataException extends JmxException {

	/**
	 * 使用指定的错误消息创建一个新的 {@code InvalidMetadataException}。
	 * @param msg 详细错误消息
	 */
	public InvalidMetadataException(String msg) {
		super(msg);
	}

}
