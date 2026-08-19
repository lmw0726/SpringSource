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

package org.springframework.beans.factory.aspectj;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * 一个切面（aspect），用于向任何其类型实现了 {@link ConfigurableObject} 接口的对象注入依赖。
 *
 * <p>该切面支持在领域对象（domain object）首次创建时以及反序列化时对其进行注入。子切面只需
 * 提供 configureBean() 方法的定义。如有需要，该方法也可以不依赖 Spring 容器来实现。
 *
 * <p>这里有两种需要处理的情况：
 * <ol>
 * <li>通过 '{@code new}' 操作符进行的普通对象创建：这可以通过对 {@code initialization()}
 * 连接点（join point）进行通知（advice）来处理。</li>
 * <li>通过反序列化创建对象：由于反序列化过程中不会调用构造函数，切面需要对反序列化机制将要调用的
 * 某个方法进行通知。理想情况下，我们不应要求用户类实现任何特定方法。这意味着我们需要
 * <i>引入</i>（introduce）所选的方法。我们还需要处理所选方法已存在于类中的情况（在这种情况下，
 * 用户对该方法的实现应优先于引入的实现）。对于所选方法有几种选择：
 * <ul>
 * <li>readObject(ObjectOutputStream)：Java 要求该方法必须是
 * {@code private}</p>。由于切面无法在保留名称的同时引入私有成员，
 * 因此该选项被排除。</li>
 * <li>readResolve()：Java 对访问修饰符没有任何限制。问题解决了！这种方法有一个（较小的）局限，
 * 即如果用户类已经拥有该方法，那么该方法必须是 {@code public}。不过，这不应造成太大负担，
 * 因为需要类实现 readResolve() 的用例（例如自定义枚举）不太可能被标记为 &#64;Configurable，
 * 而且无论如何，要求将该方法设为 {@code public} 不应带来任何过重的负担。</li>
 * </ul>
 * 如果使用 AspectJ 的一个实验性特性——{@code hasmethod()} PCD，那么用户类所需的少量协作
 * （即任何 readResolve() 的实现（如果有的话）必须是 {@code public}）也可以被解除。</li>
 * </ol>
 *
 * <p>虽然让类型实现 {@link ConfigurableObject} 接口当然是一种有效的选择，但另一种替代方案是使用
 * 另一个切面中的 'declare parents' 语句（此切面的子切面会是合乎逻辑的选择），通过提供
 * {@link ConfigurableObject} 接口来声明需要被配置的类。
 *
 * @author Ramnivas Laddad
 * @since 2.5.2
 */
public abstract aspect AbstractInterfaceDrivenDependencyInjectionAspect extends AbstractDependencyInjectionAspect {

	/**
	 * 将初始化连接点（join point）选择为对象构造
	 */
	public pointcut beanConstruction(Object bean) :
			initialization(ConfigurableObject+.new(..)) && this(bean);

	/**
	 * 选择通过 ConfigurableDeserializationSupport 的 ITD（跨类型声明，inter-type declaration）提供的反序列化连接点（join point）
	 */
	public pointcut beanDeserialization(Object bean) :
			execution(Object ConfigurableDeserializationSupport+.readResolve()) && this(bean);

	public pointcut leastSpecificSuperTypeConstruction() : initialization(ConfigurableObject.new(..));



	// 用于在对象被反序列化后重新注入依赖的实现

	/**
	 * 声明任何同时实现 Serializable 和 ConfigurableObject 的类也实现
	 * ConfigurableDeserializationSupport。这使我们能够引入 {@code readResolve()}
	 * 方法，并通过 beanDeserialization() 切点（pointcut）选择它。
	 * <p>下面是一个改进版本，它使用 hasmethod() 切点，甚至解除了对用户类的
	 * 那一小点要求：
	 * <pre class="code">
	 * declare parents: ConfigurableObject+ Serializable+
	 * && !hasmethod(Object readResolve() throws ObjectStreamException)
	 * implements ConfigurableDeserializationSupport;
	 * </pre>
	 */
	declare parents: ConfigurableObject+ && Serializable+ implements ConfigurableDeserializationSupport;

	/**
	 * 一个标记接口（marker interface），{@code readResolve()} 方法被引入到该接口上。
	 */
	static interface ConfigurableDeserializationSupport extends Serializable {
	}

	/**
	 * 引入 {@code readResolve()} 方法，以便我们可以通知（advise）它的
	 * 执行来配置对象。
	 * <p>请注意，如果 ConfigurableObject 类型的 {@code Serializable} 类中已经存在
	 * 具有相同签名的方法，那么该实现将优先（这是一件好事，因为我们
	 * 仅仅对检测反序列化的机会感兴趣。）
	 */
	public Object ConfigurableDeserializationSupport.readResolve() throws ObjectStreamException {
		return this;
	}

}
