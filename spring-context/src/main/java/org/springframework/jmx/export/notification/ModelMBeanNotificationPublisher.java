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

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;

import org.springframework.util.Assert;

/**
 * 使用 {@link ModelMBean} 接口提供的基础设施来跟踪
 * {@link javax.management.NotificationListener javax.management.NotificationListeners}
 * 并向这些监听器发送 {@link Notification Notifications} 的 {@link NotificationPublisher} 实现。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 * @see javax.management.modelmbean.ModelMBeanNotificationBroadcaster
 * @see NotificationPublisherAware
 */
public class ModelMBeanNotificationPublisher implements NotificationPublisher {

	/**
	 * 将被管理资源封装为 {@link ModelMBean} 的实例，
	 * 此 {@code NotificationPublisher} 将被注入到该实例中。
	 */
	private final ModelMBeanNotificationBroadcaster modelMBean;

	/**
	 * 与 {@link ModelMBean modelMBean} 关联的 {@link ObjectName}。
	 */
	private final ObjectName objectName;

	/**
	 * 与 {@link ModelMBean modelMBean} 关联的被管理资源。
	 */
	private final Object managedResource;


	/**
	 * 创建一个新的 {@link ModelMBeanNotificationPublisher} 实例，
	 * 该实例将把所有 {@link javax.management.Notification Notifications} 发布到提供的 {@link ModelMBean}。
	 * @param modelMBean 目标 {@link ModelMBean}；不能为 {@code null}
	 * @param objectName 源 {@link ModelMBean} 的 {@link ObjectName}
	 * @param managedResource 由提供的 {@link ModelMBean} 暴露的被管理资源
	 * @throws IllegalArgumentException 如果任一参数为 {@code null}
	 */
	public ModelMBeanNotificationPublisher(
			ModelMBeanNotificationBroadcaster modelMBean, ObjectName objectName, Object managedResource) {

		Assert.notNull(modelMBean, "'modelMBean' must not be null");
		Assert.notNull(objectName, "'objectName' must not be null");
		Assert.notNull(managedResource, "'managedResource' must not be null");
		this.modelMBean = modelMBean;
		this.objectName = objectName;
		this.managedResource = managedResource;
	}


	/**
	 * 使用被封装的 {@link ModelMBean} 实例发送提供的 {@link Notification}。
	 * @param notification 要发送的 {@link Notification}
	 * @throws IllegalArgumentException 如果提供的 {@code notification} 为 {@code null}
	 * @throws UnableToSendNotificationException 如果提供的 {@code notification} 无法发送
	 */
	@Override
	public void sendNotification(Notification notification) {
		Assert.notNull(notification, "Notification must not be null");
		replaceNotificationSourceIfNecessary(notification);
		try {
			if (notification instanceof AttributeChangeNotification) {
				this.modelMBean.sendAttributeChangeNotification((AttributeChangeNotification) notification);
			}
			else {
				this.modelMBean.sendNotification(notification);
			}
		}
		catch (MBeanException ex) {
			throw new UnableToSendNotificationException("Unable to send notification [" + notification + "]", ex);
		}
	}

	/**
	 * 必要时替换通知源。
	 * 根据 {@link Notification javadoc}：
	 * <i>"强烈建议通知发送者使用对象名称而非 MBean 对象的引用作为源。"</i>
	 * @param notification 其 {@link javax.management.Notification#getSource()} 可能需要处理的 {@link Notification}
	 */
	private void replaceNotificationSourceIfNecessary(Notification notification) {
		if (notification.getSource() == null || notification.getSource().equals(this.managedResource)) {
			notification.setSource(this.objectName);
		}
	}

}
