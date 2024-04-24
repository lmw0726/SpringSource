/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;

/**
 * FlashMap提供了一种一次请求存储属性以供另一次请求使用的方式。
 * 当从一个URL重定向到另一个URL时最常用--例如Post/Redirect/Get模式。
 * 在重定向之前（通常是在会话中）保存FlashMap，重定向后立即可用并立即删除。
 *
 * <p>FlashMap可以设置请求路径和请求参数，以帮助识别目标请求。
 * 没有此信息，FlashMap将在下一个请求中可用，该请求可能是预期的接收者，也可能不是。
 * 在重定向时，目标URL已知，并且可以使用该信息更新FlashMap。
 * 当使用{@code org.springframework.web.servlet.view.RedirectView}时，这是自动完成的。
 *
 * <p>注意：注释控制器通常不会直接使用FlashMap。
 * 有关在注释控制器中使用闪存属性的概述，请参见{@code org.springframework.web.servlet.mvc.support.RedirectAttributes}。
 *
 * @author Rossen Stoyanchev
 * @see FlashMapManager
 * @since 3.1
 */
@SuppressWarnings("serial")
public final class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {
	/**
	 * 目标URL路径
	 */
	@Nullable
	private String targetRequestPath;
	/**
	 * 目标请求参数
	 */
	private final MultiValueMap<String, String> targetRequestParams = new LinkedMultiValueMap<>(3);
	/**
	 * 闪存映射过期时间，默认为不过期
	 */
	private long expirationTime = -1;


	/**
	 * 提供URL路径以帮助识别此FlashMap的目标请求。
	 * <p>路径可以是绝对的（例如“/application/resource”）或相对于当前请求的（例如“../resource”）。
	 */
	public void setTargetRequestPath(@Nullable String path) {
		this.targetRequestPath = path;
	}

	/**
	 * 返回目标URL路径（如果未指定，则为{@code null}）。
	 */
	@Nullable
	public String getTargetRequestPath() {
		return this.targetRequestPath;
	}

	/**
	 * 提供标识此FlashMap请求的请求参数。
	 *
	 * @param params 一个带有预期参数的名称和值的Map
	 */
	public FlashMap addTargetRequestParams(@Nullable MultiValueMap<String, String> params) {
		if (params != null) {
			params.forEach((key, values) -> {
				for (String value : values) {
					addTargetRequestParam(key, value);
				}
			});
		}
		return this;
	}

	/**
	 * 提供标识此FlashMap请求的请求参数。
	 *
	 * @param name  期望的参数名称（如果为空则跳过）
	 * @param value 期望的值（如果为空则跳过）
	 */
	public FlashMap addTargetRequestParam(String name, String value) {
		if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
			this.targetRequestParams.add(name, value);
		}
		return this;
	}

	/**
	 * 返回标识目标请求的参数，或一个空映射。
	 */
	public MultiValueMap<String, String> getTargetRequestParams() {
		return this.targetRequestParams;
	}

	/**
	 * 开始此实例的过期期限。
	 *
	 * @param timeToLive 过期前的秒数
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationTime = System.currentTimeMillis() + timeToLive * 1000;
	}

	/**
	 * 设置FlashMap的过期时间。这是为了序列化目的提供的，但也可以用于{@link #startExpirationPeriod(int)}。
	 *
	 * @since 4.2
	 */
	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	/**
	 * 返回FlashMap的过期时间，如果过期期限未开始，则返回-1。
	 *
	 * @since 4.2
	 */
	public long getExpirationTime() {
		return this.expirationTime;
	}

	/**
	 * 根据自上次调用{@link #startExpirationPeriod}以来经过的时间量判断此实例是否已过期。
	 */
	public boolean isExpired() {
		return (this.expirationTime != -1 && System.currentTimeMillis() > this.expirationTime);
	}


	/**
	 * 比较两个FlashMap，并优先指定目标URL路径或具有更多目标URL参数的FlashMap。在比较FlashMap实例之前，请确保它们与给定的请求匹配。
	 */
	@Override
	public int compareTo(FlashMap other) {
		int thisUrlPath = (this.targetRequestPath != null ? 1 : 0);
		int otherUrlPath = (other.targetRequestPath != null ? 1 : 0);
		if (thisUrlPath != otherUrlPath) {
			return otherUrlPath - thisUrlPath;
		} else {
			return other.targetRequestParams.size() - this.targetRequestParams.size();
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FlashMap)) {
			return false;
		}
		FlashMap otherFlashMap = (FlashMap) other;
		return (super.equals(otherFlashMap) &&
				ObjectUtils.nullSafeEquals(this.targetRequestPath, otherFlashMap.targetRequestPath) &&
				this.targetRequestParams.equals(otherFlashMap.targetRequestParams));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.targetRequestPath);
		result = 31 * result + this.targetRequestParams.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "FlashMap [attributes=" + super.toString() + ", targetRequestPath=" +
				this.targetRequestPath + ", targetRequestParams=" + this.targetRequestParams + "]";
	}

}
