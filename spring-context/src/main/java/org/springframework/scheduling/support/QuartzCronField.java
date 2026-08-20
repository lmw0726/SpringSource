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
import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link CronField} 的扩展，用于
 * <a href="https://www.quartz-scheduler.org>Quartz</a> 特定字段。
 * 使用 {@code parse*} 方法创建，内部使用 {@link TemporalAdjuster}。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class QuartzCronField extends CronField {

	private final Type rollForwardType;

	private final TemporalAdjuster adjuster;

	private final String value;


	private QuartzCronField(Type type, TemporalAdjuster adjuster, String value) {
		this(type, type, adjuster, value);
	}

	/**
	 * 构造函数，用于需要向前滚动到与当前字段类型不同的类型的字段。
	 * 参见 {@link #parseDaysOfWeek(String)}。
	 */
	private QuartzCronField(Type type, Type rollForwardType, TemporalAdjuster adjuster, String value) {
		super(type);
		this.adjuster = adjuster;
		this.value = value;
		this.rollForwardType = rollForwardType;
	}

	/**
	 * 判断给定的值是否为 Quartz 月内日字段。
	 */
	public static boolean isQuartzDaysOfMonthField(String value) {
		return value.contains("L") || value.contains("W");
	}

	/**
	 * 将给定的值解析为月内日 {@code QuartzCronField}，即 cron 表达式的第四项。
	 * 期望给定的值中包含 "L" 或 "W"。
	 */
	public static QuartzCronField parseDaysOfMonth(String value) {
		int idx = value.lastIndexOf('L');
		if (idx != -1) {
			TemporalAdjuster adjuster;
			if (idx != 0) {
				throw new IllegalArgumentException("Unrecognized characters before 'L' in '" + value + "'");
			}
			else if (value.length() == 2 && value.charAt(1) == 'W') { // "LW"
				adjuster = lastWeekdayOfMonth();
			}
			else {
				if (value.length() == 1) { // "L"
					adjuster = lastDayOfMonth();
				}
				else { // "L-[0-9]+"
					int offset = Integer.parseInt(value.substring(idx + 1));
					if (offset >= 0) {
						throw new IllegalArgumentException("Offset '" + offset + " should be < 0 '" + value + "'");
					}
					adjuster = lastDayWithOffset(offset);
				}
			}
			return new QuartzCronField(Type.DAY_OF_MONTH, adjuster, value);
		}
		idx = value.lastIndexOf('W');
		if (idx != -1) {
			if (idx == 0) {
				throw new IllegalArgumentException("No day-of-month before 'W' in '" + value + "'");
			}
			else if (idx != value.length() - 1) {
				throw new IllegalArgumentException("Unrecognized characters after 'W' in '" + value + "'");
			}
			else { // "[0-9]+W"
				int dayOfMonth = Integer.parseInt(value.substring(0, idx));
				dayOfMonth = Type.DAY_OF_MONTH.checkValidValue(dayOfMonth);
				TemporalAdjuster adjuster = weekdayNearestTo(dayOfMonth);
				return new QuartzCronField(Type.DAY_OF_MONTH, adjuster, value);
			}
		}
		throw new IllegalArgumentException("No 'L' or 'W' found in '" + value + "'");
	}

	/**
	 * 判断给定的值是否为 Quartz 星期字段。
	 */
	public static boolean isQuartzDaysOfWeekField(String value) {
		return value.contains("L") || value.contains("#");
	}

	/**
	 * 将给定的值解析为星期 {@code QuartzCronField}，即 cron 表达式的第六项。
	 * 期望给定的值中包含 "L" 或 "#"。
	 */
	public static QuartzCronField parseDaysOfWeek(String value) {
		int idx = value.lastIndexOf('L');
		if (idx != -1) {
			if (idx != value.length() - 1) {
				throw new IllegalArgumentException("Unrecognized characters after 'L' in '" + value + "'");
			}
			else {
				TemporalAdjuster adjuster;
				if (idx == 0) {
					throw new IllegalArgumentException("No day-of-week before 'L' in '" + value + "'");
				}
				else { // "[0-7]L"
					DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
					adjuster = lastInMonth(dayOfWeek);
				}
				return new QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value);
			}
		}
		idx = value.lastIndexOf('#');
		if (idx != -1) {
			if (idx == 0) {
				throw new IllegalArgumentException("No day-of-week before '#' in '" + value + "'");
			}
			else if (idx == value.length() - 1) {
				throw new IllegalArgumentException("No ordinal after '#' in '" + value + "'");
			}
			// "[0-7]#[0-9]+"
			DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
			int ordinal = Integer.parseInt(value.substring(idx + 1));
			if (ordinal <= 0) {
				throw new IllegalArgumentException("Ordinal '" + ordinal + "' in '" + value +
						"' must be positive number ");
			}

			TemporalAdjuster adjuster = dayOfWeekInMonth(ordinal, dayOfWeek);
			return new QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value);
		}
		throw new IllegalArgumentException("No 'L' or '#' found in '" + value + "'");
	}

	private static DayOfWeek parseDayOfWeek(String value) {
		int dayOfWeek = Integer.parseInt(value);
		if (dayOfWeek == 0) {
			dayOfWeek = 7; // cron is 0 based; java.time 1 based
		}
		try {
			return DayOfWeek.of(dayOfWeek);
		}
		catch (DateTimeException ex) {
			String msg = ex.getMessage() + " '" + value + "'";
			throw new IllegalArgumentException(msg, ex);
		}
	}

	/**
	 * 返回一个重置到午夜的调整器。
	 */
	private static TemporalAdjuster atMidnight() {
		return temporal -> {
			if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
				return temporal.with(ChronoField.NANO_OF_DAY, 0);
			}
			else {
				return temporal;
			}
		};
	}

	/**
	 * 返回一个调整器，该调整器将返回设置为当前月最后一天午夜的新时间值。
	 */
	private static TemporalAdjuster lastDayOfMonth() {
		TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * 返回一个调整器，该调整器返回当月的最后一个工作日。
	 */
	private static TemporalAdjuster lastWeekdayOfMonth() {
		TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
		return temporal -> {
			Temporal lastDom = adjuster.adjustInto(temporal);
			Temporal result;
			int dow = lastDom.get(ChronoField.DAY_OF_WEEK);
			if (dow == 6) { // Saturday
				result = lastDom.minus(1, ChronoUnit.DAYS);
			}
			else if (dow == 7) { // Sunday
				result = lastDom.minus(2, ChronoUnit.DAYS);
			}
			else {
				result = lastDom;
			}
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * 返回一个时间调整器，用于查找当月倒数第 N 天。
	 * @param offset 负偏移量，例如 -3 表示倒数第三天
	 * @return 倒数第 N 天的月内日调整器
	 */
	private static TemporalAdjuster lastDayWithOffset(int offset) {
		Assert.isTrue(offset < 0, "Offset should be < 0");
		TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal).plus(offset, ChronoUnit.DAYS);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * 返回一个时间调整器，用于查找最接近给定月内日的工作日。
	 * 如果 {@code dayOfMonth} 落在星期六，日期将向前移到星期五；
	 * 如果落在星期日（或者 {@code dayOfMonth} 为 1 且落在星期六），
	 * 日期将向后移到星期一。
	 * @param dayOfMonth 目标月内日
	 * @return 最接近该工作日的调整器
	 */
	private static TemporalAdjuster weekdayNearestTo(int dayOfMonth) {
		return temporal -> {
			int current = Type.DAY_OF_MONTH.get(temporal);
			DayOfWeek dayOfWeek = DayOfWeek.from(temporal);

			if ((current == dayOfMonth && isWeekday(dayOfWeek)) || // dayOfMonth is a weekday
					(dayOfWeek == DayOfWeek.FRIDAY && current == dayOfMonth - 1) || // dayOfMonth is a Saturday, so Friday before
					(dayOfWeek == DayOfWeek.MONDAY && current == dayOfMonth + 1) || // dayOfMonth is a Sunday, so Monday after
					(dayOfWeek == DayOfWeek.MONDAY && dayOfMonth == 1 && current == 3)) { // dayOfMonth is Saturday 1st, so Monday 3rd
				return temporal;
			}
			int count = 0;
			while (count++ < CronExpression.MAX_ATTEMPTS) {
				if (current == dayOfMonth) {
					dayOfWeek = DayOfWeek.from(temporal);

					if (dayOfWeek == DayOfWeek.SATURDAY) {
						if (dayOfMonth != 1) {
							temporal = temporal.minus(1, ChronoUnit.DAYS);
						}
						else {
							// exception for "1W" fields: execute on next Monday
							temporal = temporal.plus(2, ChronoUnit.DAYS);
						}
					}
					else if (dayOfWeek == DayOfWeek.SUNDAY) {
						temporal = temporal.plus(1, ChronoUnit.DAYS);
					}
					return atMidnight().adjustInto(temporal);
				}
				else {
					temporal = Type.DAY_OF_MONTH.elapseUntil(cast(temporal), dayOfMonth);
					current = Type.DAY_OF_MONTH.get(temporal);
				}
			}
			return null;
		};
	}

	private static boolean isWeekday(DayOfWeek dayOfWeek) {
		return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
	}

	/**
	 * 返回一个调整器，该调整器返回当月给定星期几的最后一天。
	 */
	private static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
		TemporalAdjuster adjuster = TemporalAdjusters.lastInMonth(dayOfWeek);
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * 返回一个时间调整器，用于查找当月第 {@code ordinal} 次出现的给定星期几。
	 */
	private static TemporalAdjuster dayOfWeekInMonth(int ordinal, DayOfWeek dayOfWeek) {
		TemporalAdjuster adjuster = TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek);
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * 将给定的 {@code result} 回滚到午夜。当
	 * {@code current} 与 {@code result} 具有相同的月内日时，返回前者，
	 * 以确保我们不会得到比开始时间更早的结果。
	 */
	private static Temporal rollbackToMidnight(Temporal current, Temporal result) {
		if (result.get(ChronoField.DAY_OF_MONTH) == current.get(ChronoField.DAY_OF_MONTH)) {
			return current;
		}
		else {
			return atMidnight().adjustInto(result);
		}
	}

	@Override
	public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
		T result = adjust(temporal);
		if (result != null) {
			if (result.compareTo(temporal) < 0) {
				// We ended up before the start, roll forward and try again
				temporal = this.rollForwardType.rollForward(temporal);
				result = adjust(temporal);
				if (result != null) {
					result = type().reset(result);
				}
			}
		}
		return result;
	}


	@Nullable
	@SuppressWarnings("unchecked")
	private <T extends Temporal & Comparable<? super T>> T adjust(T temporal) {
		return (T) this.adjuster.adjustInto(temporal);
	}


	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof QuartzCronField)) {
			return false;
		}
		QuartzCronField other = (QuartzCronField) o;
		return type() == other.type() &&
				this.value.equals(other.value);
	}

	@Override
	public String toString() {
		return type() + " '" + this.value + "'";

	}

}
