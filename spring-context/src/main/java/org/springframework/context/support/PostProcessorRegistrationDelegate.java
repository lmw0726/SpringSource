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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	/**
	 * 调用BeanFactory后置处理器。
	 *
	 * @param beanFactory Bean工厂
	 * @param beanFactoryPostProcessors BeanFactory后置处理器列表
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// 警告：虽然看起来可以轻松地重构此方法的主体以避免使用多个循环和多个列表，但使用多个列表和多次传递处理器名称是有意的。
		// 我们必须确保我们遵守 PriorityOrdered 和 Ordered 处理器的合同。具体来说，我们不能导致处理器被实例化（通过 getBean() 调用）
		// 或以错误的顺序在 ApplicationContext 中注册。

		// 处理过的 Bean 名称集合，用于避免重复处理
		Set<String> processedBeans = new HashSet<>();

		// 如果 BeanFactory 是 BeanDefinitionRegistry 类型，则处理 BeanDefinitionRegistryPostProcessors
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 分离 BeanFactoryPostProcessor 和 BeanDefinitionRegistryPostProcessor
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// 如果当前的 BeanFactoryPostProcessor 实现了 BeanDefinitionRegistryPostProcessor 接口
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 调用 postProcessBeanDefinitionRegistry 方法处理 BeanDefinitionRegistry
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 将处理器添加到 BeanDefinitionRegistryPostProcessor 列表中
					registryProcessors.add(registryProcessor);
				} else {
					// 如果不实现 BeanDefinitionRegistryPostProcessor 接口，则将处理器添加到常规处理器列表中
					regularPostProcessors.add(postProcessor);
				}
			}

			// 不要在此处初始化 FactoryBeans，因为需要保持所有常规 Bean 保持未初始化状态，以便 BeanFactoryPostProcessors 应用到它们！
			// 将实现 PriorityOrdered、Ordered 和其余部分的 BeanDefinitionRegistryPostProcessor 分开。

			// 处理实现 PriorityOrdered 的 BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// 对实现 PriorityOrdered 接口的 Bean 进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			// 调用 BeanDefinitionRegistryPostProcessors
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			currentRegistryProcessors.clear();

			// 处理实现 Ordered 的 BeanDefinitionRegistryPostProcessor
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// 对实现 Ordered 接口的 Bean 进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			// 调用 BeanDefinitionRegistryPostProcessors
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			currentRegistryProcessors.clear();

			// 最后，调用所有其他 BeanDefinitionRegistryPostProcessors，直到没有更多的出现。
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				// 调用 BeanDefinitionRegistryPostProcessors
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				currentRegistryProcessors.clear();
			}

			// 现在，调用到目前为止处理的所有处理器的 postProcessBeanFactory 回调。
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		} else {
			// 如果不是 BeanDefinitionRegistry，则调用上下文实例注册的工厂处理器。
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// 不要在此处初始化 FactoryBeans，因为需要保持所有常规 Bean 保持未初始化状态，以便 BeanFactoryPostProcessors 应用到它们！
		// 分离实现 PriorityOrdered、Ordered 和其他的 BeanFactoryPostProcessor
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// 跳过 - 在上面的第一阶段已经处理过了
			} else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 处理实现 PriorityOrdered 接口的 BeanFactoryPostProcessor
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 处理实现 Ordered 接口的 BeanFactoryPostProcessor
				orderedPostProcessorNames.add(ppName);
			} else {
				// 未排序的 BeanFactoryPostProcessor
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 首先，调用实现 PriorityOrdered 的 BeanFactoryPostProcessor
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 接下来，调用实现 Ordered 的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 最后，调用所有其他 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// 清除缓存的合并 bean 定义，因为后处理器可能已经修改了原始元数据，例如替换值中的占位符...
		beanFactory.clearMetadataCache();
	}

	/**
	 * 注册BeanPostProcessor到ConfigurableListableBeanFactory中，
	 * 使用给定的AbstractApplicationContext进行配置。
	 *
	 * @param beanFactory        可配置的可列出的Bean工厂
	 * @param applicationContext 给定的AbstractApplicationContext上下文
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// 警告：虽然可能会认为此方法的主体可以轻松地重构以避免使用多个循环和多个列表，
		// 但是使用多个列表和对处理器名称的多次循环是有意的。
		// 我们必须确保遵守PriorityOrdered和Ordered处理器的契约。
		// 具体而言，我们不能导致处理器以错误的顺序实例化（通过getBean()调用），
		// 也不能以错误的顺序在ApplicationContext中注册处理器。
		// 在提交更改此方法的拉取请求（PR）之前，请查阅涉及更改PostProcessorRegistrationDelegate的所有已拒绝的PR的列表，
		// 以确保您的建议不会导致破坏性更改：
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// 获取所有BeanPostProcessor的名称
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// 注册BeanPostProcessorChecker，用于在实例化BeanPostProcessor期间创建bean时记录信息消息
		// bean处理器目标计数
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// 分离实现PriorityOrdered、Ordered和其他的BeanPostProcessors
		// 优先级排序后处理器列表
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 内部后处理器列表
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		// 有序后处理器名称列表
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 无序的后处理器名称列表中
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				//如果有@PriorityOrdered注解，获取该BeanPostProcessor的实例。
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					// 如果该beanPostProcessor实现了MergedBeanDefinitionPostProcessor接口，
					// 将该处理器的名称添加进内部后处理器列表中
					internalPostProcessors.add(pp);
				}
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 如果有@Ordered注解，则添加进 有序后处理器名称列表中
				orderedPostProcessorNames.add(ppName);
			} else {
				// 否则添加到无序的后处理器名称列表中
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 首先，注册实现PriorityOrdered的BeanPostProcessors
		// 先对优先级排序后处理器列表进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// 接下来，注册实现Ordered的BeanPostProcessors
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			// 获取后置处理的实例
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			// 将其添加进 有序后处理器列表 中
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				// 如果该实例同时也实现了MergedBeanDefinitionPostProcessor接口，则将其添加进内部后处理器列表中。
				internalPostProcessors.add(pp);
			}
		}
		// 对有序后处理器列表进行排序。
		sortPostProcessors(orderedPostProcessors, beanFactory);
		//注册有序后置处理器
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// 现在，注册所有常规的BeanPostProcessors
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			// 获取后置处理器实例
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			// 将其添加进无序后置处理器列表中
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				// 如果该实例同时也实现了MergedBeanDefinitionPostProcessor接口，将其添加进内部后处理器列表中。
				internalPostProcessors.add(pp);
			}
		}
		// 注册无序后置处理器
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// 最后，重新注册所有内部BeanPostProcessors
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// 重新注册用于检测内部bean作为ApplicationListeners的后处理器，
		// 将其移动到处理器链的末尾（用于获取代理等）
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// 如果后处理器数量小于等于1，则无需排序
		if (postProcessors.size() <= 1) {
			return;
		}

		// 获取用于排序的比较器
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}

		// 如果比较器为空，则使用OrderComparator.INSTANCE作为默认比较器
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}

		// 使用比较器对后处理器进行排序
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * 调用给定的BeanFactoryPostProcessor bean。
	 *
	 * @param postProcessors BeanFactory后置处理器集合
	 * @param beanFactory 可配置的可列表的Bean工厂
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		// 对于每个 BeanFactoryPostProcessor
		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			// 启动一个 StartupStep，用于记录 BeanFactory 后处理过程
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			// 调用 postProcessBeanFactory 方法对 BeanFactory 进行后处理
			postProcessor.postProcessBeanFactory(beanFactory);
			// 结束当前的 StartupStep
			postProcessBeanFactory.end();
		}
	}

	/**
	 * 注册给定的BeanPostProcessor bean。
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		// 如果beanFactory是AbstractBeanFactory的实例，则使用批量添加方法效率更高
		if (beanFactory instanceof AbstractBeanFactory) {
			// 在CopyOnWriteArrayList上进行批量添加更加高效
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		} else {
			// 否则，逐个添加每个后处理器
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
