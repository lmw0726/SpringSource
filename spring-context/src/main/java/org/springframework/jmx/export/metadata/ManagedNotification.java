/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 元数据，用于指示由 Bean 发出的 JMX 通知。
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class ManagedNotification {

	@Nullable
	private String[] notificationTypes;

	@Nullable
	private String name;

	@Nullable
	private String description;


	/**
	 * 设置单个通知类型，或以逗号分隔的字符串形式设置多个通知类型。
	 */
	public void setNotificationType(String notificationType) {
		this.notificationTypes = StringUtils.commaDelimitedListToStringArray(notificationType);
	}

	/**
	 * 设置通知类型列表。
	 */
	public void setNotificationTypes(@Nullable String... notificationTypes) {
		this.notificationTypes = notificationTypes;
	}

	/**
	 * 返回通知类型列表。
	 */
	@Nullable
	public String[] getNotificationTypes() {
		return this.notificationTypes;
	}

	/**
	 * 设置此通知的名称。
	 */
	public void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * 返回此通知的名称。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 设置此通知的描述。
	 */
	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	/**
	 * 返回此通知的描述。
	 */
	@Nullable
	public String getDescription() {
		return this.description;
	}

}
