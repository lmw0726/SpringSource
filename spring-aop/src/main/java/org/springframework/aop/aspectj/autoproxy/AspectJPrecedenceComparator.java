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

import java.util.Comparator;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJAopUtils;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * 按优先级（<i>不是</i>调用顺序）对 AspectJ 通知/通知器进行排序。
 *
 * <p>给定两条通知 {@code A} 和 {@code B}：
 * <ul>
 * <li>如果 {@code A} 和 {@code B} 定义在不同的切面中，则具有最低顺序值的
 * 切面中的通知具有最高优先级。</li>
 * <li>如果 {@code A} 和 {@code B} 定义在同一切面中，如果 {@code A} 或 {@code B}
 * 中的一个是 <em>after</em> 形式的通知，则最后声明的通知具有最高优先级。
 * 如果 {@code A} 和 {@code B} 都不是 <em>after</em> 形式的通知，
 * 则最先声明的通知具有最高优先级。</li>
 * </ul>
 *
 * <p>重要：此比较器与 AspectJ 的
 * {@link org.aspectj.util.PartialOrder PartialOrder} 排序实用程序一起使用。
 * 因此，与普通的 {@link Comparator} 不同，此比较器返回 {@code 0} 值
 * 意味着我们不关心顺序，而不是两个元素必须相同地排序。
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
class AspectJPrecedenceComparator implements Comparator<Advisor> {

	private static final int HIGHER_PRECEDENCE = -1;  // 更高优先级

	private static final int SAME_PRECEDENCE = 0;  // 相同优先级

	private static final int LOWER_PRECEDENCE = 1;  // 更低优先级


	private final Comparator<? super Advisor> advisorComparator;


	/**
	 * 创建一个默认的 {@code AspectJPrecedenceComparator}。
	 */
	public AspectJPrecedenceComparator() {
		this.advisorComparator = AnnotationAwareOrderComparator.INSTANCE;
	}

	/**
	 * 创建一个 {@code AspectJPrecedenceComparator}，使用给定的 {@link Comparator}
	 * 来比较 {@link org.springframework.aop.Advisor} 实例。
	 * @param advisorComparator 用于通知器的 {@code Comparator}
	 */
	public AspectJPrecedenceComparator(Comparator<? super Advisor> advisorComparator) {
		Assert.notNull(advisorComparator, "Advisor comparator must not be null");
		this.advisorComparator = advisorComparator;
	}


	@Override
	public int compare(Advisor o1, Advisor o2) {
		int advisorPrecedence = this.advisorComparator.compare(o1, o2);
		if (advisorPrecedence == SAME_PRECEDENCE && declaredInSameAspect(o1, o2)) {
			advisorPrecedence = comparePrecedenceWithinAspect(o1, o2);
		}
		return advisorPrecedence;
	}

	private int comparePrecedenceWithinAspect(Advisor advisor1, Advisor advisor2) {
		boolean oneOrOtherIsAfterAdvice =
				(AspectJAopUtils.isAfterAdvice(advisor1) || AspectJAopUtils.isAfterAdvice(advisor2));
		int adviceDeclarationOrderDelta = getAspectDeclarationOrder(advisor1) - getAspectDeclarationOrder(advisor2);

		if (oneOrOtherIsAfterAdvice) {
			// 最后声明的通知具有更高优先级
			if (adviceDeclarationOrderDelta < 0) {
				// advice1 在 advice2 之前声明
				// 因此 advice1 具有更低优先级
				return LOWER_PRECEDENCE;
			}
			else if (adviceDeclarationOrderDelta == 0) {
				return SAME_PRECEDENCE;
			}
			else {
				return HIGHER_PRECEDENCE;
			}
		}
		else {
			// 最先声明的通知具有更高优先级
			if (adviceDeclarationOrderDelta < 0) {
				// advice1 在 advice2 之前声明
				// 因此 advice1 具有更高优先级
				return HIGHER_PRECEDENCE;
			}
			else if (adviceDeclarationOrderDelta == 0) {
				return SAME_PRECEDENCE;
			}
			else {
				return LOWER_PRECEDENCE;
			}
		}
	}

	private boolean declaredInSameAspect(Advisor advisor1, Advisor advisor2) {
		return (hasAspectName(advisor1) && hasAspectName(advisor2) &&
				getAspectName(advisor1).equals(getAspectName(advisor2)));
	}

	private boolean hasAspectName(Advisor advisor) {
		return (advisor instanceof AspectJPrecedenceInformation ||
				advisor.getAdvice() instanceof AspectJPrecedenceInformation);
	}

	// 前置条件是 hasAspectName 返回 true
	private String getAspectName(Advisor advisor) {
		AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
		Assert.state(precedenceInfo != null, () -> "Unresolvable AspectJPrecedenceInformation for " + advisor);
		return precedenceInfo.getAspectName();
	}

	private int getAspectDeclarationOrder(Advisor advisor) {
		AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
		return (precedenceInfo != null ? precedenceInfo.getDeclarationOrder() : 0);
	}

}
