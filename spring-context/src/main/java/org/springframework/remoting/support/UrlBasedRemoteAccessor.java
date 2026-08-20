/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.support;

import org.springframework.beans.factory.InitializingBean;

/**
 * 通过 URL 访问远程服务的类的抽象基类。
 * 提供了一个 "serviceUrl" bean 属性，该属性为必需属性。
 *
 * @author Juergen Hoeller
 * @since 15.12.2003
 */
public abstract class UrlBasedRemoteAccessor extends RemoteAccessor implements InitializingBean {

	private String serviceUrl;


	/**
	 * 设置此远程访问器的目标服务的 URL。
	 * URL 必须与特定远程提供者的规则兼容。
	 */
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * 返回此远程访问器的目标服务的 URL。
	 */
	public String getServiceUrl() {
		return this.serviceUrl;
	}


	@Override
	public void afterPropertiesSet() {
		if (getServiceUrl() == null) {
			throw new IllegalArgumentException("Property 'serviceUrl' is required");
		}
	}

}
