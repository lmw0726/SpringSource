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

package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 当 BeanFactory 在尝试根据 Bean 定义创建 Bean 时遇到错误，
 * 将抛出此异常。
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class BeanCreationException extends FatalBeanException {

	@Nullable
	private final String beanName;

	@Nullable
	private final String resourceDescription;

	@Nullable
	private List<Throwable> relatedCauses;


	/**
	 * 创建一个新的 BeanCreationException。
	 *
	 * @param msg 详细信息
	 */
	public BeanCreationException(String msg) {
		super(msg);
		this.beanName = null;
		this.resourceDescription = null;
	}

	/**
	 * 创建一个新的 BeanCreationException。
	 *
	 * @param msg   详细信息
	 * @param cause 根本原因
	 */
	public BeanCreationException(String msg, Throwable cause) {
		super(msg, cause);
		this.beanName = null;
		this.resourceDescription = null;
	}

	/**
	 * 创建一个新的 BeanCreationException。
	 *
	 * @param beanName 被请求的 Bean 名称
	 * @param msg      详细信息
	 */
	public BeanCreationException(String beanName, String msg) {
		super("Error creating bean with name '" + beanName + "': " + msg);
		this.beanName = beanName;
		this.resourceDescription = null;
	}

	/**
	 * 创建一个新的 BeanCreationException。
	 *
	 * @param beanName 被请求的 Bean 名称
	 * @param msg      详细信息
	 * @param cause    根本原因
	 */
	public BeanCreationException(String beanName, String msg, Throwable cause) {
		this(beanName, msg);
		initCause(cause);
	}

	/**
	 * 创建一个新的 BeanCreationException。
	 *
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param beanName            被请求的 Bean 名称
	 * @param msg                 详细信息
	 */
	public BeanCreationException(@Nullable String resourceDescription, @Nullable String beanName, String msg) {
		super("Error creating bean with name '" + beanName + "'" +
				(resourceDescription != null ? " defined in " + resourceDescription : "") + ": " + msg);
		this.resourceDescription = resourceDescription;
		this.beanName = beanName;
		this.relatedCauses = null;
	}

	/**
	 * 创建一个新的 BeanCreationException。
	 *
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param beanName            被请求的 Bean 名称
	 * @param msg                 详细信息
	 * @param cause               根本原因
	 */
	public BeanCreationException(@Nullable String resourceDescription, String beanName, String msg, Throwable cause) {
		this(resourceDescription, beanName, msg);
		initCause(cause);
	}


	/**
	 * 返回 Bean 定义来源资源的描述（如果有）。
	 */
	@Nullable
	public String getResourceDescription() {
		return this.resourceDescription;
	}

	/**
	 * 返回被请求的 Bean 名称（如果有）。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 在此bean创建异常中添加一个相关原因，该原因不是导致失败的直接原因，而是在创建同一bean实例时较早发生的原因。
	 *
	 * @param ex 要添加的相关原因
	 */
	public void addRelatedCause(Throwable ex) {
		if (this.relatedCauses == null) {
			this.relatedCauses = new ArrayList<>();
		}
		this.relatedCauses.add(ex);
	}

	/**
	 * 返回相关的原因（如果有）。
	 *
	 * @return 相关原因的数组，如果没有则返回 {@code null}
	 */
	@Nullable
	public Throwable[] getRelatedCauses() {
		if (this.relatedCauses == null) {
			return null;
		}
		return this.relatedCauses.toArray(new Throwable[0]);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				sb.append("\nRelated cause: ");
				sb.append(relatedCause);
			}
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		synchronized (ps) {
			super.printStackTrace(ps);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					ps.println("Related cause:");
					relatedCause.printStackTrace(ps);
				}
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		synchronized (pw) {
			super.printStackTrace(pw);
			if (this.relatedCauses != null) {
				for (Throwable relatedCause : this.relatedCauses) {
					pw.println("Related cause:");
					relatedCause.printStackTrace(pw);
				}
			}
		}
	}

	@Override
	public boolean contains(@Nullable Class<?> exClass) {
		if (super.contains(exClass)) {
			return true;
		}
		if (this.relatedCauses != null) {
			for (Throwable relatedCause : this.relatedCauses) {
				if (relatedCause instanceof NestedRuntimeException &&
						((NestedRuntimeException) relatedCause).contains(exClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
