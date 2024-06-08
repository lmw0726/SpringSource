/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.context.support;

import org.springframework.context.ApplicationEvent;
import org.springframework.lang.Nullable;

/**
 * 在 ApplicationContext 中处理请求时引发的事件。
 *
 * <p>由 Spring 自己的 FrameworkServlet 支持（通过特定的 ServletRequestHandledEvent 子类），但也可以由任何其他 Web 组件引发。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ServletRequestHandledEvent
 * @see org.springframework.web.servlet.FrameworkServlet
 * @see org.springframework.context.ApplicationContext#publishEvent
 * @since January 17, 2001
 */
@SuppressWarnings("serial")
public class RequestHandledEvent extends ApplicationEvent {

	/**
	 * 应用于请求的会话 ID（如果有）。
	 */
	@Nullable
	private String sessionId;

	/**
	 * 通常是 用户主体。
	 */
	@Nullable
	private String userName;

	/**
	 * 请求处理时间（毫秒）。
	 */
	private final long processingTimeMillis;

	/**
	 * 失败的原因（如果有）。
	 */
	@Nullable
	private Throwable failureCause;

	/**
	 * 创建一个带有会话信息的新 RequestHandledEvent。
	 *
	 * @param source               发布事件的组件
	 * @param sessionId            HTTP 会话的 ID（如果有）
	 * @param userName             与请求关联的用户的名称（通常是 UserPrincipal）
	 * @param processingTimeMillis 请求的处理时间（毫秒）
	 */
	public RequestHandledEvent(Object source, @Nullable String sessionId, @Nullable String userName,
							   long processingTimeMillis) {
		super(source);
		this.sessionId = sessionId;
		this.userName = userName;
		this.processingTimeMillis = processingTimeMillis;
	}

	/**
	 * 创建一个带有会话信息的新 RequestHandledEvent。
	 *
	 * @param source               发布事件的组件
	 * @param sessionId            HTTP 会话的 ID（如果有）
	 * @param userName             与请求关联的用户的名称（通常是 UserPrincipal）
	 * @param processingTimeMillis 请求的处理时间（毫秒）
	 * @param failureCause         失败的原因（如果有）
	 */
	public RequestHandledEvent(Object source, @Nullable String sessionId, @Nullable String userName,
							   long processingTimeMillis, @Nullable Throwable failureCause) {
		this(source, sessionId, userName, processingTimeMillis);
		this.failureCause = failureCause;
	}


	/**
	 * 返回请求的处理时间（毫秒）。
	 */
	public long getProcessingTimeMillis() {
		return this.processingTimeMillis;
	}

	/**
	 * 返回 HTTP 会话的 ID（如果有）。
	 */
	@Nullable
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * 返回与请求关联的用户的名称（通常是 UserPrincipal）。
	 */
	@Nullable
	public String getUserName() {
		return this.userName;
	}

	/**
	 * 返回请求是否失败。
	 */
	public boolean wasFailure() {
		return (this.failureCause != null);
	}

	/**
	 * 返回失败的原因（如果有）。
	 */
	@Nullable
	public Throwable getFailureCause() {
		return this.failureCause;
	}


	/**
	 * 返回此事件的简短描述，只涉及最重要的上下文数据。
	 */
	public String getShortDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("session=[").append(this.sessionId).append("]; ");
		sb.append("user=[").append(this.userName).append("]; ");
		return sb.toString();
	}

	/**
	 * 返回此事件的完整描述，涉及所有可用的上下文数据。
	 */
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("session=[").append(this.sessionId).append("]; ");
		sb.append("user=[").append(this.userName).append("]; ");
		sb.append("time=[").append(this.processingTimeMillis).append("ms]; ");
		sb.append("status=[");
		if (!wasFailure()) {
			sb.append("OK");
		} else {
			sb.append("failed: ").append(this.failureCause);
		}
		sb.append(']');
		return sb.toString();
	}

	@Override
	public String toString() {
		return ("RequestHandledEvent: " + getDescription());
	}

}
