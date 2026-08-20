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

package org.springframework.scheduling.support;

import java.time.DateTimeException;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link CronField} 的高效位运算扩展。
 * 使用 {@code parse*} 方法创建。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class BitsCronField extends CronField {

	private static final long MASK = 0xFFFFFFFFFFFFFFFFL;


	@Nullable
	private static BitsCronField zeroNanos = null;


	// 我们最多存储 60 位，用于秒和分钟，因此一个 64 位的 long 就足够了
	private long bits;


	private BitsCronField(Type type) {
		super(type);
	}

	/**
	 * 返回一个启用 0 纳秒的 {@code BitsCronField}。
	 */
	public static BitsCronField zeroNanos() {
		if (zeroNanos == null) {
			BitsCronField field = new BitsCronField(Type.NANO);
			field.setBit(0);
			zeroNanos = field;
		}
		return zeroNanos;
	}

	/**
	 * 将给定值解析为秒级别的 {@code BitsCronField}，即 cron 表达式的第一个条目。
	 */
	public static BitsCronField parseSeconds(String value) {
		return parseField(value, Type.SECOND);
	}

	/**
	 * 将给定值解析为分钟级别的 {@code BitsCronField}，即 cron 表达式的第二个条目。
	 */
	public static BitsCronField parseMinutes(String value) {
		return BitsCronField.parseField(value, Type.MINUTE);
	}

	/**
	 * 将给定值解析为小时级别的 {@code BitsCronField}，即 cron 表达式的第三个条目。
	 */
	public static BitsCronField parseHours(String value) {
		return BitsCronField.parseField(value, Type.HOUR);
	}

	/**
	 * 将给定值解析为月份日期级别的 {@code BitsCronField}，即 cron 表达式的第四个条目。
	 */
	public static BitsCronField parseDaysOfMonth(String value) {
		return parseDate(value, Type.DAY_OF_MONTH);
	}

	/**
	 * 将给定值解析为月级别的 {@code BitsCronField}，即 cron 表达式的第五个条目。
	 */
	public static BitsCronField parseMonth(String value) {
		return BitsCronField.parseField(value, Type.MONTH);
	}

	/**
	 * 将给定值解析为星期级别的 {@code BitsCronField}，即 cron 表达式的第六个条目。
	 */
	public static BitsCronField parseDaysOfWeek(String value) {
		BitsCronField result = parseDate(value, Type.DAY_OF_WEEK);
		if (result.getBit(0)) {
			// cron 支持 0 表示星期日；我们像 java.time 一样使用 7
			result.setBit(7);
			result.clearBit(0);
		}
		return result;
	}


	private static BitsCronField parseDate(String value, BitsCronField.Type type) {
		if (value.equals("?")) {
			value = "*";
		}
		return BitsCronField.parseField(value, type);
	}

	private static BitsCronField parseField(String value, Type type) {
		Assert.hasLength(value, "Value must not be empty");
		Assert.notNull(type, "Type must not be null");
		try {
			BitsCronField result = new BitsCronField(type);
			String[] fields = StringUtils.delimitedListToStringArray(value, ",");
			for (String field : fields) {
				int slashPos = field.indexOf('/');
				if (slashPos == -1) {
					ValueRange range = parseRange(field, type);
					result.setBits(range);
				}
				else {
					String rangeStr = field.substring(0, slashPos);
					String deltaStr = field.substring(slashPos + 1);
					ValueRange range = parseRange(rangeStr, type);
					if (rangeStr.indexOf('-') == -1) {
						range = ValueRange.of(range.getMinimum(), type.range().getMaximum());
					}
					int delta = Integer.parseInt(deltaStr);
					if (delta <= 0) {
						throw new IllegalArgumentException("Incrementer delta must be 1 or higher");
					}
					result.setBits(range, delta);
				}
			}
			return result;
		}
		catch (DateTimeException | IllegalArgumentException ex) {
			String msg = ex.getMessage() + " '" + value + "'";
			throw new IllegalArgumentException(msg, ex);
		}
	}

	private static ValueRange parseRange(String value, Type type) {
		if (value.equals("*")) {
			return type.range();
		}
		else {
			int hyphenPos = value.indexOf('-');
			if (hyphenPos == -1) {
				int result = type.checkValidValue(Integer.parseInt(value));
				return ValueRange.of(result, result);
			}
			else {
				int min = Integer.parseInt(value.substring(0, hyphenPos));
				int max = Integer.parseInt(value.substring(hyphenPos + 1));
				min = type.checkValidValue(min);
				max = type.checkValidValue(max);
				if (type == Type.DAY_OF_WEEK && min == 7) {
					// 如果作为范围的最小值使用，星期日表示 0（而不是 7）
					min = 0;
				}
				return ValueRange.of(min, max);
			}
		}
	}

	@Nullable
	@Override
	public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
		int current = type().get(temporal);
		int next = nextSetBit(current);
		if (next == -1) {
			temporal = type().rollForward(temporal);
			next = nextSetBit(0);
		}
		if (next == current) {
			return temporal;
		}
		else {
			int count = 0;
			current = type().get(temporal);
			while (current != next && count++ < CronExpression.MAX_ATTEMPTS) {
				temporal = type().elapseUntil(temporal, next);
				current = type().get(temporal);
				next = nextSetBit(current);
				if (next == -1) {
					temporal = type().rollForward(temporal);
					next = nextSetBit(0);
				}
			}
			if (count >= CronExpression.MAX_ATTEMPTS) {
				return null;
			}
			return type().reset(temporal);
		}
	}

	boolean getBit(int index) {
		return (this.bits & (1L << index)) != 0;
	}

	private int nextSetBit(int fromIndex) {
		long result = this.bits & (MASK << fromIndex);
		if (result != 0) {
			return Long.numberOfTrailingZeros(result);
		}
		else {
			return -1;
		}

	}

	private void setBits(ValueRange range) {
		if (range.getMinimum() == range.getMaximum()) {
			setBit((int) range.getMinimum());
		}
		else {
			long minMask = MASK << range.getMinimum();
			long maxMask = MASK >>> - (range.getMaximum() + 1);
			this.bits |= (minMask & maxMask);
		}
	}

	private void setBits(ValueRange range, int delta) {
		if (delta == 1) {
			setBits(range);
		}
		else {
			for (int i = (int) range.getMinimum(); i <= range.getMaximum(); i += delta) {
				setBit(i);
			}
		}
	}

	private void setBit(int index) {
		this.bits |= (1L << index);
	}

	private void clearBit(int index) {
		this.bits &=  ~(1L << index);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.bits);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BitsCronField)) {
			return false;
		}
		BitsCronField other = (BitsCronField) o;
		return type() == other.type() && this.bits == other.bits;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(type().toString());
		builder.append(" {");
		int i = nextSetBit(0);
		if (i != -1) {
			builder.append(i);
			i = nextSetBit(i+1);
			while (i != -1) {
				builder.append(", ");
				builder.append(i);
				i = nextSetBit(i+1);
			}
		}
		builder.append('}');
		return builder.toString();
	}

}
