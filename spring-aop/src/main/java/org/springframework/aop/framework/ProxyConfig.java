/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * 用于创建代理的配置的便捷超类，
 * 以确保所有代理创建器具有一致的属性。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AdvisedSupport
 */
public class ProxyConfig implements Serializable {

	/** 使用 Spring 1.2 中的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = -8409359707199703185L;


	private boolean proxyTargetClass = false;

	private boolean optimize = false;

	boolean opaque = false;

	boolean exposeProxy = false;

	private boolean frozen = false;


	/**
	 * 设置是否直接代理目标类，而不仅仅代理特定接口。
	 * 默认值为 "false"。
	 * <p>将其设置为 "true" 可强制为 TargetSource 暴露的目标类创建代理。
	 * 如果该目标类是接口，则会为给定接口创建 JDK 代理。
	 * 如果该目标类是任何其他类，则会为给定类创建 CGLIB 代理。
	 * <p>注意：根据具体代理工厂的配置，
	 * 如果未指定任何接口（且未激活接口自动检测），
	 * 也会应用 proxy-target-class 行为。
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * 返回是否直接代理目标类以及任何接口。
	 */
	public boolean isProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * 设置代理是否应执行激进优化。
	 * “激进优化”的确切含义会因代理而异，
	 * 但通常会有一些权衡。
	 * 默认值为 "false"。
	 * <p>在 Spring 当前的代理选项中，此标志实际上会强制使用 CGLIB 代理
	 * （类似于 {@link #setProxyTargetClass}），
	 * 但不会执行任何类验证检查（例如 final 方法等）。
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	/**
	 * 返回代理是否应执行激进优化。
	 */
	public boolean isOptimize() {
		return this.optimize;
	}

	/**
	 * 设置是否应阻止由此配置创建的代理被强制转换为 {@link Advised}
	 * 以查询代理状态。
	 * <p>默认值为 "false"，表示任何 AOP 代理都可以强制转换为 {@link Advised}。
	 */
	public void setOpaque(boolean opaque) {
		this.opaque = opaque;
	}

	/**
	 * 返回是否应阻止由此配置创建的代理被强制转换为 {@link Advised}。
	 */
	public boolean isOpaque() {
		return this.opaque;
	}

	/**
	 * 设置代理是否应由 AOP 框架作为 ThreadLocal 暴露，
	 * 以便通过 AopContext 类检索。如果被通知对象需要在自身上调用另一个被通知方法，
	 * 这会很有用。（如果它使用 {@code this}，该调用将不会被通知）。
	 * <p>默认值为 "false"，以避免不必要的额外拦截。
	 * 这意味着不保证 AopContext 访问能在被通知对象的任何方法中一致工作。
	 */
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}

	/**
	 * 返回 AOP 代理是否会为每次调用暴露 AOP 代理。
	 */
	public boolean isExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * 设置此配置是否应被冻结。
	 * <p>当配置被冻结时，不能更改通知。这有助于优化，
	 * 并且在我们不希望调用者在强制转换为 Advised 后能够操纵配置时也很有用。
	 */
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	/**
	 * 返回配置是否被冻结，并且不能更改通知。
	 */
	public boolean isFrozen() {
		return this.frozen;
	}


	/**
	 * 从另一个配置对象复制配置。
	 * @param other 要从中复制配置的对象
	 */
	public void copyFrom(ProxyConfig other) {
		Assert.notNull(other, "Other ProxyConfig object must not be null");
		this.proxyTargetClass = other.proxyTargetClass;
		this.optimize = other.optimize;
		this.exposeProxy = other.exposeProxy;
		this.frozen = other.frozen;
		this.opaque = other.opaque;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
		sb.append("optimize=").append(this.optimize).append("; ");
		sb.append("opaque=").append(this.opaque).append("; ");
		sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
		sb.append("frozen=").append(this.frozen);
		return sb.toString();
	}

}
