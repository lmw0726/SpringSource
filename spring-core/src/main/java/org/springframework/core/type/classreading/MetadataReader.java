/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.core.type.classreading;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * 访问类元数据的简单外观，
 * 由 ASM {@link org.springframework.asm.ClassReader} 读取。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface MetadataReader {

	/**
	 * 返回类文件对应的资源引用。
	 */
	Resource getResource();

	/**
	 * 读取底层类的基础类元数据。
	 */
	ClassMetadata getClassMetadata();

	/**
	 * 读取底层类的完整注解元数据，
	 * 包括注解方法的元数据。
	 */
	AnnotationMetadata getAnnotationMetadata();

}
