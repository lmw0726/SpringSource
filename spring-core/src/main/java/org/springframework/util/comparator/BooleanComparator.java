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

package org.springframework.util.comparator;

import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Comparator;

/**
 * 用于{@link Boolean}对象的{@link Comparator}，可以设置将{@code true}或{@code false}排在前面。
 *
 * @author Keith Donald
 * @since 1.2.2
 */
@SuppressWarnings("serial")
public class BooleanComparator implements Comparator<Boolean>, Serializable {

	/**
	 * 此比较器的共享默认实例，将{@code true}视为比{@code false}小。
	 */
	public static final BooleanComparator TRUE_LOW = new BooleanComparator(true);

	/**
	 * 此比较器的共享默认实例，将{@code true}视为比{@code false}大。
	 */
	public static final BooleanComparator TRUE_HIGH = new BooleanComparator(false);


	private final boolean trueLow;


	/**
	 * 创建一个BooleanComparator，根据提供的标志对布尔值进行排序。
	 * <p>或者，您可以使用默认的共享实例：
	 * {@code BooleanComparator.TRUE_LOW}和
	 * {@code BooleanComparator.TRUE_HIGH}。
	 * @param trueLow 是否将true视为比false小
	 * @see #TRUE_LOW
	 * @see #TRUE_HIGH
	 */
	public BooleanComparator(boolean trueLow) {
		this.trueLow = trueLow;
	}


	@Override
	public int compare(Boolean v1, Boolean v2) {
		return (v1 ^ v2) ? ((v1 ^ this.trueLow) ? 1 : -1) : 0;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof BooleanComparator &&
				this.trueLow == ((BooleanComparator) other).trueLow));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode() * (this.trueLow ? -1 : 1);
	}

	@Override
	public String toString() {
		return "BooleanComparator: " + (this.trueLow ? "true low" : "true high");
	}

}
