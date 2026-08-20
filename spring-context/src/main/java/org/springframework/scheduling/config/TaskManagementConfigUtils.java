/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.config;

/**
 * 用于子包之间内部共享的配置常量。
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class TaskManagementConfigUtils {

	/**
	 * 内部管理的 Scheduled 注解处理器的 Bean 名称。
	 */
	public static final String SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalScheduledAnnotationProcessor";

	/**
	 * 内部管理的 Async 注解处理器的 Bean 名称。
	 */
	public static final String ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.context.annotation.internalAsyncAnnotationProcessor";

	/**
	 * 内部管理的 AspectJ 异步执行切面的 Bean 名称。
	 */
	public static final String ASYNC_EXECUTION_ASPECT_BEAN_NAME =
			"org.springframework.scheduling.config.internalAsyncExecutionAspect";

}
