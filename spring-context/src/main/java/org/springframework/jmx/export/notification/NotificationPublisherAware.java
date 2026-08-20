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

package org.springframework.jmx.export.notification;

import org.springframework.beans.factory.Aware;

/**
 * 需要由任何通过 Spring 管理的、要注册到 {@link javax.management.MBeanServer}
 * 的资源实现的接口，该资源希望发送 JMX
 * {@link javax.management.Notification javax.management.Notifications}。
 *
 * <p>为通过 Spring 创建的托管资源提供 {@link NotificationPublisher}，
 * 一旦它们注册到 {@link javax.management.MBeanServer} 即可使用。
 *
 * <p><b>注意：</b>此接口仅适用于通过 Spring 的
 * {@link org.springframework.jmx.export.MBeanExporter} 导出的简单 Spring 管理的 Bean。
 * 它不适用于任何未导出的 Bean；也不适用于由 Spring 导出的标准 MBean。
 * 对于标准 JMX MBean，请考虑实现
 * {@link javax.management.modelmbean.ModelMBeanNotificationBroadcaster}
 * 接口（或实现完整的 {@link javax.management.modelmbean.ModelMBean}）。
 *
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 * @see NotificationPublisher
 */
public interface NotificationPublisherAware extends Aware {

	/**
	 * 为当前托管资源实例设置 {@link NotificationPublisher} 实例。
	 */
	void setNotificationPublisher(NotificationPublisher notificationPublisher);

}
