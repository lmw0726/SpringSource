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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * 描述一组{@link BeanDefinition BeanDefinitions}
 * 和{@link BeanReference BeanReferences}逻辑视图的接口，
 * 如在某些配置上下文中呈现的那样。
 *
 * <p>随着{@link org.springframework.beans.factory.xml.NamespaceHandler 可插拔自定义XML标签}的引入，
 * 现在单个逻辑配置实体（在这种情况下是XML标签）可能
 * 创建多个{@link BeanDefinition BeanDefinitions}和{@link BeanReference RuntimeBeanReferences}
 * 以提供更简洁的配置和更大的最终用户便利性。因此，不能
 * 再假设每个配置实体（例如XML标签）映射到一个{@link BeanDefinition}。
 * 对于希望为配置Spring应用程序提供可视化或支持的工具供应商和其他用户来说，
 * 重要的是有某种机制将{@link org.springframework.beans.factory.BeanFactory}中的{@link BeanDefinition BeanDefinitions}
 * 以对最终用户有具体意义的方式链接回配置数据。因此，{@link org.springframework.beans.factory.xml.NamespaceHandler}
 * 实现能够为每个正在配置的逻辑实体以{@code ComponentDefinition}的形式发布事件。
 * 第三方然后可以{@link ReaderEventListener 订阅这些事件}，
 * 从而提供以用户为中心的bean元数据视图。
 *
 * <p>每个{@code ComponentDefinition}都有一个特定于配置的{@link #getSource 源对象}。
 * 在基于XML的配置情况下，这通常是包含用户提供的配置信息的{@link org.w3c.dom.Node}。
 * 除此之外，包含在{@code ComponentDefinition}中的每个{@link BeanDefinition}
 * 都有自己的{@link BeanDefinition#getSource() 源对象}，该对象可能指向
 * 不同的、更具体的配置数据集合。除此之外，bean元数据的各个部分，如
 * {@link org.springframework.beans.PropertyValue PropertyValues}也可能有源对象，提供
 * 更高级别的详细信息。源对象提取通过{@link SourceExtractor}处理，
 * 可以根据需要进行自定义。
 *
 * <p>虽然通过{@link #getBeanReferences}提供了对重要{@link BeanReference BeanReferences}的直接访问，
 * 但工具可能希望检查所有{@link BeanDefinition BeanDefinitions}以收集
 * 完整的{@link BeanReference BeanReferences}集合。实现需要提供
 * 验证整体逻辑实体配置所需的所有{@link BeanReference BeanReferences}，
 * 以及提供配置完整用户可视化所需的那些。预期某些{@link BeanReference BeanReferences}
 * 对验证或用户配置视图并不重要，因此可能被省略。工具可能希望
 * 显示通过提供的{@link BeanDefinition BeanDefinitions}获取的任何其他{@link BeanReference BeanReferences}，
 * 但这不被认为是典型情况。
 *
 * <p>工具可以通过检查{@link BeanDefinition#getRole 角色标识符}来确定包含的{@link BeanDefinition BeanDefinitions}的重要性。
 * 角色本质上是配置提供者认为{@link BeanDefinition}对最终用户有多重要的提示。
 * 预期工具将<strong>不会</strong>显示给定{@code ComponentDefinition}的所有{@link BeanDefinition BeanDefinitions}，
 * 而是选择基于角色进行过滤。工具可能选择使这种过滤用户可配置。
 * 应特别注意{@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE角色标识符}。
 * 具有此角色分类的{@link BeanDefinition BeanDefinitions}对最终用户完全不重要，
 * 仅出于内部实现原因而需要。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see BeanComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 * @since 2.0
 */
public interface ComponentDefinition extends BeanMetadataElement {

	/**
	 * 获取此 {@code ComponentDefinition} 的用户可见名称。
	 * <p> 这应该直接链接回给定上下文中此组件的相应配置数据。
	 */
	String getName();

	/**
	 * 返回描述组件的友好描述。
	 * <p> 鼓励实现从 {@code toString()} 返回相同的值。
	 */
	String getDescription();

	/**
	 * 返回注册形成此 {@code ComponentDefinition} 的 {@link BeanDefinition}。
	 * <p> 应该注意的是，通过 {@link BeanReference 引用}，
	 * {@code ComponentDefinition} 很可能与其他 {@link BeanDefinition BeanDefinition} 相关，
	 * 但是这些 <strong> 不</strong>包括在内，因为它们可能无法立即使用。
	 * 重要的 {@link BeanReference BeanReferences} 可从 {@link #getBeanReferences()} 获得。
	 *
	 * @return BeanDefinitions的数组，如果没有，则为空数组
	 */
	BeanDefinition[] getBeanDefinitions();

	/**
	 * 返回表示此组件中所有相关内部bean的 {@link BeanDefinition BeanDefinitions}。
	 * <p> 关联的 {@link BeanDefinition BeanDefinitions} 中可能存在其他内部bean，
	 * 但是，对于验证或用户可视化，不需要这些内部bean。
	 *
	 * @return BeanDefinitions的数组，如果没有，则为空数组
	 */
	BeanDefinition[] getInnerBeanDefinitions();

	/**
	 * 返回被认为对这个 {@code ComponentDefinition} 重要的 {@link BeanReference} 的集合。
	 * <p> 其他 {@link BeanReference BeanReferences} 可能存在于关联的 {@link BeanDefinition BeanDefinitions} 中，
	 * 但是对于验证或用户可视化而言，不需要这些。
	 *
	 * @return BeanReferences的数组，如果没有，则为空数组
	 */
	BeanReference[] getBeanReferences();

}
