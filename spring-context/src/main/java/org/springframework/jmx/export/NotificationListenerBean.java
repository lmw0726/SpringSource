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

package org.springframework.jmx.export;

import javax.management.NotificationListener;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.support.NotificationListenerHolder;
import org.springframework.util.Assert;

/**
 * 聚合 {@link javax.management.NotificationListener}、
 * {@link javax.management.NotificationFilter} 和任意回传对象的辅助类。
 *
 * <p>还支持将封装的
 * {@link javax.management.NotificationListener} 与任意数量的 MBean 关联，
 * 通过 {@link #setMappedObjectNames mappedObjectNames} 属性
 * 指定希望从这些 MBean 接收
 * {@link javax.management.Notification Notifications}。
 *
 * <p>注意：此类还支持使用 Spring bean 名称作为
 * {@link #setMappedObjectNames "mappedObjectNames"}，作为指定 JMX 对象名称的替代方案。
 * 请注意，对于此类 bean 名称，仅支持由同一个
 * {@link MBeanExporter} 导出的 bean。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see MBeanExporter#setNotificationListeners
 */
public class NotificationListenerBean extends NotificationListenerHolder implements InitializingBean {

	/**
	 * 创建 {@link NotificationListenerBean} 类的新实例。
	 */
	public NotificationListenerBean() {
	}

	/**
	 * 创建 {@link NotificationListenerBean} 类的新实例。
	 * @param notificationListener 封装的监听器
	 */
	public NotificationListenerBean(NotificationListener notificationListener) {
		Assert.notNull(notificationListener, "NotificationListener must not be null");
		setNotificationListener(notificationListener);
	}


	@Override
	public void afterPropertiesSet() {
		if (getNotificationListener() == null) {
			throw new IllegalArgumentException("Property 'notificationListener' is required");
		}
	}

	void replaceObjectName(Object originalName, Object newName) {
		if (this.mappedObjectNames != null && this.mappedObjectNames.contains(originalName)) {
			this.mappedObjectNames.remove(originalName);
			this.mappedObjectNames.add(newName);
		}
	}

}
