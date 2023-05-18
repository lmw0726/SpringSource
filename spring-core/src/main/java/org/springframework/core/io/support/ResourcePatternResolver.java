/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into {@link Resource} objects.
 *
 * <p>This is an extension to the {@link org.springframework.core.io.ResourceLoader}
 * interface. A passed-in {@code ResourceLoader} (for example, an
 * {@link org.springframework.context.ApplicationContext} passed in via
 * {@link org.springframework.context.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>{@link PathMatchingResourcePatternResolver} is a standalone implementation
 * that is usable outside an {@code ApplicationContext}, also used by
 * {@link ResourceArrayPropertyEditor} for populating {@code Resource} array bean
 * properties.
 *
 * <p>Can be used with any sort of location pattern &mdash; for example,
 * {@code "/WEB-INF/*-context.xml"}. However, input patterns have to match the
 * strategy implementation. This interface just specifies the conversion method
 * rather than a specific pattern format.
 *
 * <p>This interface also defines a {@code "classpath*:"} resource prefix for all
 * matching resources from the class path. Note that the resource location may
 * also contain placeholders &mdash; for example {@code "/beans-*.xml"}. JAR files
 * or different directories in the class path can contain multiple files of the
 * same name.
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 * @since 1.0.2
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * “classpath*:”这与ResourceLoader的classpath URL前缀不同，它检索给定名称的所有匹配资源(例如。“/beans.xml”)，例如在所有已部署JAR文件的根目录中。
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 获取指定位置模式的多个资源
	 *
	 * @param locationPattern 位置模式
	 * @return 多个资源
	 * @throws IOException IO异常
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
