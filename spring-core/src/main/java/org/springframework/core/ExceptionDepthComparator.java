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

package org.springframework.core;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 能根据异常类型距离抛出异常的层级深度进行排序的比较器。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 3.0.3
 */
public class ExceptionDepthComparator implements Comparator<Class<? extends Throwable>> {

	private final Class<? extends Throwable> targetException;


	/**
	 * 为给定异常创建新的 ExceptionDepthComparator。
	 * @param exception 目标异常，用于比较层级深度
	 */
	public ExceptionDepthComparator(Throwable exception) {
		Assert.notNull(exception, "Target exception must not be null");
		this.targetException = exception.getClass();
	}

	/**
	 * 为给定异常类型创建新的 ExceptionDepthComparator。
	 * @param exceptionType 目标异常类型，用于比较层级深度
	 */
	public ExceptionDepthComparator(Class<? extends Throwable> exceptionType) {
		Assert.notNull(exceptionType, "Target exception type must not be null");
		this.targetException = exceptionType;
	}


	@Override
	public int compare(Class<? extends Throwable> o1, Class<? extends Throwable> o2) {
		int depth1 = getDepth(o1, this.targetException, 0);
		int depth2 = getDepth(o2, this.targetException, 0);
		return (depth1 - depth2);
	}

	private int getDepth(Class<?> declaredException, Class<?> exceptionToMatch, int depth) {
		if (exceptionToMatch.equals(declaredException)) {
			// 找到了！
			return depth;
		}
		// 如果已经到达最顶层但仍未找到...
		if (exceptionToMatch == Throwable.class) {
			return Integer.MAX_VALUE;
		}
		return getDepth(declaredException, exceptionToMatch.getSuperclass(), depth + 1);
	}


	/**
	 * 从给定异常类型集合中获取与目标异常最接近的匹配异常类型。
	 * @param exceptionTypes  异常类型集合
	 * @param targetException 目标异常，查找最接近匹配
	 * @return 从集合中最接近匹配的异常类型
	 */
	public static Class<? extends Throwable> findClosestMatch(
			Collection<Class<? extends Throwable>> exceptionTypes, Throwable targetException) {

		Assert.notEmpty(exceptionTypes, "Exception types must not be empty");
		if (exceptionTypes.size() == 1) {
			return exceptionTypes.iterator().next();
		}
		List<Class<? extends Throwable>> handledExceptions = new ArrayList<>(exceptionTypes);
		handledExceptions.sort(new ExceptionDepthComparator(targetException));
		return handledExceptions.get(0);
	}

}
