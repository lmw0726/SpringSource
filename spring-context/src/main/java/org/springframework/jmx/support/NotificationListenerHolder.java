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

package org.springframework.jmx.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 辅助类，用于聚合 {@link javax.management.NotificationListener}、
 * {@link javax.management.NotificationFilter} 和任意的回调对象，
 * 以及监听器希望接收 {@link javax.management.Notification 通知} 的 MBean 名称。
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.jmx.export.NotificationListenerBean
 * @see org.springframework.jmx.access.NotificationListenerRegistrar
 */
public class NotificationListenerHolder {

	@Nullable
	private NotificationListener notificationListener;

	@Nullable
	private NotificationFilter notificationFilter;

	@Nullable
	private Object handback;

	@Nullable
	protected Set<Object> mappedObjectNames;


	/**
	 * 设置 {@link javax.management.NotificationListener}。
	 */
	public void setNotificationListener(@Nullable NotificationListener notificationListener) {
		this.notificationListener = notificationListener;
	}

	/**
	 * 获取 {@link javax.management.NotificationListener}。
	 */
	@Nullable
	public NotificationListener getNotificationListener() {
		return this.notificationListener;
	}

	/**
	 * 设置与封装的 {@link #getNotificationFilter() NotificationFilter} 相关联的
	 * {@link javax.management.NotificationFilter}。
	 * <p>可以为 {@code null}。
	 */
	public void setNotificationFilter(@Nullable NotificationFilter notificationFilter) {
		this.notificationFilter = notificationFilter;
	}

	/**
	 * 返回与封装的 {@link #getNotificationListener() NotificationListener} 相关联的
	 * {@link javax.management.NotificationFilter}。
	 * <p>可以为 {@code null}。
	 */
	@Nullable
	public NotificationFilter getNotificationFilter() {
		return this.notificationFilter;
	}

	/**
	 * 设置当 {@link javax.management.NotificationBroadcaster} 通知任何
	 * {@link javax.management.NotificationListener} 时将原样"回传"的（任意）对象。
	 * @param handback 回传对象（可以为 {@code null}）
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, Object)
	 */
	public void setHandback(@Nullable Object handback) {
		this.handback = handback;
	}

	/**
	 * 返回当 {@link javax.management.NotificationBroadcaster} 通知任何
	 * {@link javax.management.NotificationListener} 时将原样"回传"的（任意）对象。
	 * @return 回传对象（可能为 {@code null}）
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, Object)
	 */
	@Nullable
	public Object getHandback() {
		return this.handback;
	}

	/**
	 * 设置将与封装的 {@link #getNotificationFilter() NotificationFilter} 注册以监听
	 * {@link javax.management.Notification 通知} 的单个 MBean 的
	 * {@link javax.management.ObjectName} 格式名称。
	 * 可以指定为 {@code ObjectName} 实例或 {@code String}。
	 * @see #setMappedObjectNames
	 */
	public void setMappedObjectName(@Nullable Object mappedObjectName) {
		this.mappedObjectNames = (mappedObjectName != null ?
				new LinkedHashSet<>(Collections.singleton(mappedObjectName)) : null);
	}

	/**
	 * 设置将与封装的 {@link #getNotificationFilter() NotificationFilter} 注册以监听
	 * {@link javax.management.Notification 通知} 的 MBean 的
	 * {@link javax.management.ObjectName} 格式名称数组。
	 * 可以指定为 {@code ObjectName} 实例或 {@code String}。
	 * @see #setMappedObjectName
	 */
	public void setMappedObjectNames(Object... mappedObjectNames) {
		this.mappedObjectNames = new LinkedHashSet<>(Arrays.asList(mappedObjectNames));
	}

	/**
	 * 返回将注册封装的 {@link #getNotificationFilter() NotificationFilter} 作为
	 * {@link javax.management.Notification 通知} 监听器的 {@link javax.management.ObjectName} 字符串表示列表。
	 * @throws MalformedObjectNameException 如果 {@code ObjectName} 格式不正确
	 */
	@Nullable
	public ObjectName[] getResolvedObjectNames() throws MalformedObjectNameException {
		if (this.mappedObjectNames == null) {
			return null;
		}
		ObjectName[] resolved = new ObjectName[this.mappedObjectNames.size()];
		int i = 0;
		for (Object objectName : this.mappedObjectNames) {
			resolved[i] = ObjectNameManager.getInstance(objectName);
			i++;
		}
		return resolved;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NotificationListenerHolder)) {
			return false;
		}
		NotificationListenerHolder otherNlh = (NotificationListenerHolder) other;
		return (ObjectUtils.nullSafeEquals(this.notificationListener, otherNlh.notificationListener) &&
				ObjectUtils.nullSafeEquals(this.notificationFilter, otherNlh.notificationFilter) &&
				ObjectUtils.nullSafeEquals(this.handback, otherNlh.handback) &&
				ObjectUtils.nullSafeEquals(this.mappedObjectNames, otherNlh.mappedObjectNames));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.notificationListener);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.notificationFilter);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.handback);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.mappedObjectNames);
		return hashCode;
	}

}
