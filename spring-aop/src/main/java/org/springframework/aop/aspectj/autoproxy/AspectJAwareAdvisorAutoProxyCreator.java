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

package org.springframework.aop.aspectj.autoproxy;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.aspectj.AspectJProxyUtils;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@link org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * 子类，暴露 AspectJ 的调用上下文，并理解当多条 advice 来自同一切面时
 * AspectJ 的 advice 优先级规则。
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

	private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();


	/**
	 * 根据 AspectJ 优先级对提供的 {@link Advisor} 实例排序。
	 * <p>如果两条 advice 来自同一切面，它们将具有相同顺序。
	 * 来自同一切面的 advice 随后会根据以下规则进一步排序：
	 * <ul>
	 * <li>如果其中任一方是 <em>after</em> advice，则最后声明的 advice
	 * 具有最高优先级（即最后运行）。</li>
	 * <li>否则，最先声明的 advice 具有最高优先级（即最先运行）。</li>
	 * </ul>
	 * <p><b>重要：</b>Advisor 按优先级顺序排序，从最高优先级到最低优先级。
	 * 在“进入”连接点时，最高优先级的 advisor 应首先运行。
	 * 在“离开”连接点时，最高优先级的 advisor 应最后运行。
	 */
	@Override
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		// 创建一个新的列表，用于存放“可部分比较”的 Advisor 包装对象
		List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
		// 遍历原始 Advisor 列表
		for (Advisor advisor : advisors) {
			// 将 Advisor 包装成 PartiallyComparableAdvisorHolder
			// 该包装类的作用：
			// 1. 让 Advisor 支持“部分排序”（Partial Order）
			// 2. 内部使用 DEFAULT_PRECEDENCE_COMPARATOR 作为优先级比较器
			partiallyComparableAdvisors.add(
					new PartiallyComparableAdvisorHolder(advisor, DEFAULT_PRECEDENCE_COMPARATOR));
		}
		// 使用 AspectJ 的 PartialOrder 算法进行排序
		List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
		if (sorted != null) {
			List<Advisor> result = new ArrayList<>(advisors.size());
			// 遍历排序后的包装对象
			for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
				// 取出原始 Advisor，加入结果列表
				result.add(pcAdvisor.getAdvisor());
			}
			// 返回排序后的 Advisor 列表
			return result;
		}
		else {
			// 如果 PartialOrder 排序失败（例如存在循环依赖或无法确定顺序）
			// 回退到父类排序逻辑（通常是 AnnotationAwareOrderComparator）
			// 即使用 @Order / Ordered 接口进行排序
			return super.sortAdvisors(advisors);
		}
	}

	/**
	 * 将 {@link ExposeInvocationInterceptor} 添加到 advice 链开头。
	 * <p>使用 AspectJ pointcut 表达式以及 AspectJ 风格 advice 时需要此附加 advice。
	 */
	@Override
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
		AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
	}

	@Override
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		// TODO: 考虑通过缓存切面名称列表进行优化
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		for (Advisor advisor : candidateAdvisors) {
			if (advisor instanceof AspectJPointcutAdvisor &&
					((AspectJPointcutAdvisor) advisor).getAspectName().equals(beanName)) {
				return true;
			}
		}
		return super.shouldSkip(beanClass, beanName);
	}


	/**
	 * 实现 AspectJ 的 {@link PartialComparable} 接口，用于定义偏序关系。
	 */
	private static class PartiallyComparableAdvisorHolder implements PartialComparable {

		private final Advisor advisor;

		private final Comparator<Advisor> comparator;

		public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
			this.advisor = advisor;
			this.comparator = comparator;
		}

		@Override
		public int compareTo(Object obj) {
			Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
			return this.comparator.compare(this.advisor, otherAdvisor);
		}

		@Override
		public int fallbackCompareTo(Object obj) {
			return 0;
		}

		public Advisor getAdvisor() {
			return this.advisor;
		}

		@Override
		public String toString() {
			Advice advice = this.advisor.getAdvice();
			StringBuilder sb = new StringBuilder(ClassUtils.getShortName(advice.getClass()));
			boolean appended = false;
			if (this.advisor instanceof Ordered) {
				sb.append(": order = ").append(((Ordered) this.advisor).getOrder());
				appended = true;
			}
			if (advice instanceof AbstractAspectJAdvice) {
				sb.append(!appended ? ": " : ", ");
				AbstractAspectJAdvice ajAdvice = (AbstractAspectJAdvice) advice;
				sb.append("aspect name = ");
				sb.append(ajAdvice.getAspectName());
				sb.append(", declaration order = ");
				sb.append(ajAdvice.getDeclarationOrder());
			}
			return sb.toString();
		}
	}

}
