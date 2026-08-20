/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.Aware;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 任何希望被注入导入它的 @{@link Configuration} 类的 {@link AnnotationMetadata} 的
 * @{@link Configuration} 类都需要实现此接口。与将 @{@link Import} 用作元注解的
 * 注解结合使用时非常有用。
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ImportAware extends Aware {

	/**
	 * 设置导入此配置类的 @{@code Configuration} 类的注解元数据。
	 */
	void setImportMetadata(AnnotationMetadata importMetadata);

}
