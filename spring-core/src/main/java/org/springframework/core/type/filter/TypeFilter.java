/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.type.filter;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

/**
 * 使用 {@link org.springframework.core.type.classreading.MetadataReader} 的类型过滤器基础接口。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 */
@FunctionalInterface
public interface TypeFilter {

	/**
	 * 判断此过滤器是否匹配给定元数据描述的类。
	 * @param metadataReader 目标类的元数据读取器
	 * @param metadataReaderFactory 用于获取其他类（如超类和接口）元数据读取器的工厂
	 * @return 是否匹配
	 * @throws IOException 读取元数据时发生 I/O 错误
	 */
	boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException;

}
