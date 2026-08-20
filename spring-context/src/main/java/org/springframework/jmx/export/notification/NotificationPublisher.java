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

package org.springframework.jmx.export.notification;

import javax.management.Notification;

/**
 * 简单的接口，允许 Spring 管理的 MBean 发布 JMX 通知，
 * 而无需了解这些通知如何传输到 {@link javax.management.MBeanServer}。
 *
 * <p>托管资源可以通过实现 {@link NotificationPublisherAware} 接口来访问
 * {@code NotificationPublisher}。当特定的托管资源实例注册到
 * {@link javax.management.MBeanServer} 后，如果该资源实现了
 * {@link NotificationPublisherAware} 接口，Spring 将向其注入一个
 * {@code NotificationPublisher} 实例。
 *
 * <p>每个托管资源实例将拥有一个独立的 {@code NotificationPublisher} 实现实例。
 * 该实例将跟踪为特定托管资源注册的所有
 * {@link javax.management.NotificationListener NotificationListener}。
 *
 * <p>任何现有的、用户定义的 MBean 应使用标准 JMX API 来发布通知；
 * 此接口仅适用于 Spring 创建的 MBean。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see NotificationPublisherAware
 * @see org.springframework.jmx.export.MBeanExporter
 */
@FunctionalInterface
public interface NotificationPublisher {

	/**
	 * 将指定的 {@link javax.management.Notification} 发送给所有已注册的
	 * {@link javax.management.NotificationListener NotificationListener}。
	 * 托管资源<strong>不</strong>负责管理已注册的
	 * {@link javax.management.NotificationListener NotificationListener} 列表；
	 * 该操作将自动完成。
	 * @param notification 要发送的 JMX 通知
	 * @throws UnableToSendNotificationException 如果发送失败
	 */
	void sendNotification(Notification notification) throws UnableToSendNotificationException;

}
