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
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;
import java.util.function.BiFunction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * cron 模式中的单个字段。使用 {@code parse*} 方法创建，
 * 主要且唯一的入口点是 {@link #nextOrSame(Temporal)}。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class CronField {

	private static final String[] MONTHS = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
			"OCT", "NOV", "DEC"};

	private static final String[] DAYS = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

	private final Type type;


	protected CronField(Type type) {
		this.type = type;
	}

	/**
	 * 返回启用了 0 纳秒的 {@code CronField}。
	 */
	public static CronField zeroNanos() {
		return BitsCronField.zeroNanos();
	}

	/**
	 * 将给定值解析为秒 {@code CronField}，即 cron 表达式的第一项。
	 */
	public static CronField parseSeconds(String value) {
		return BitsCronField.parseSeconds(value);
	}

	/**
	 * 将给定值解析为分钟 {@code CronField}，即 cron 表达式的第二项。
	 */
	public static CronField parseMinutes(String value) {
		return BitsCronField.parseMinutes(value);
	}

	/**
	 * 将给定值解析为小时 {@code CronField}，即 cron 表达式的第三项。
	 */
	public static CronField parseHours(String value) {
		return BitsCronField.parseHours(value);
	}

	/**
	 * 将给定值解析为月份中的天 {@code CronField}，即 cron 表达式的第四项。
	 */
	public static CronField parseDaysOfMonth(String value) {
		if (!QuartzCronField.isQuartzDaysOfMonthField(value)) {
			return BitsCronField.parseDaysOfMonth(value);
		}
		else {
			return parseList(value, Type.DAY_OF_MONTH, (field, type) -> {
				if (QuartzCronField.isQuartzDaysOfMonthField(field)) {
					return QuartzCronField.parseDaysOfMonth(field);
				}
				else {
					return BitsCronField.parseDaysOfMonth(field);
				}
			});
		}
	}

	/**
	 * 将给定值解析为月份 {@code CronField}，即 cron 表达式的第五项。
	 */
	public static CronField parseMonth(String value) {
		value = replaceOrdinals(value, MONTHS);
		return BitsCronField.parseMonth(value);
	}

	/**
	 * 将给定值解析为星期几 {@code CronField}，即 cron 表达式的第六项。
	 */
	public static CronField parseDaysOfWeek(String value) {
		value = replaceOrdinals(value, DAYS);
		if (!QuartzCronField.isQuartzDaysOfWeekField(value)) {
			return BitsCronField.parseDaysOfWeek(value);
		}
		else {
			return parseList(value, Type.DAY_OF_WEEK, (field, type) -> {
				if (QuartzCronField.isQuartzDaysOfWeekField(field)) {
					return QuartzCronField.parseDaysOfWeek(field);
				}
				else {
					return BitsCronField.parseDaysOfWeek(field);
				}
			});
		}
	}


	private static CronField parseList(String value, Type type, BiFunction<String, Type, CronField> parseFieldFunction) {
		Assert.hasLength(value, "Value must not be empty");
		String[] fields = StringUtils.delimitedListToStringArray(value, ",");
		CronField[] cronFields = new CronField[fields.length];
		for (int i = 0; i < fields.length; i++) {
			cronFields[i] = parseFieldFunction.apply(fields[i], type);
		}
		return CompositeCronField.compose(cronFields, type, value);
	}

	private static String replaceOrdinals(String value, String[] list) {
		value = value.toUpperCase();
		for (int i = 0; i < list.length; i++) {
			String replacement = Integer.toString(i + 1);
			value = StringUtils.replace(value, list[i], replacement);
		}
		return value;
	}


	/**
	 * 获取序列中匹配此 cron 字段的下一个或相同的 {@link Temporal}。
	 * @param temporal 种子值
	 * @return 匹配模式的下一个或相同的时间
	 */
	@Nullable
	public abstract <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal);


	protected Type type() {
		return this.type;
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Temporal & Comparable<? super T>> T cast(Temporal temporal) {
		return (T) temporal;
	}


	/**
	 * 表示 cron 字段的类型，即秒、分钟、小时、
	 * 月份中的天、月份、星期几。
	 */
	protected enum Type {
		NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
		SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
		MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		HOUR(ChronoField.HOUR_OF_DAY, ChronoUnit.DAYS, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_MONTH(ChronoField.DAY_OF_MONTH, ChronoUnit.MONTHS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		MONTH(ChronoField.MONTH_OF_YEAR, ChronoUnit.YEARS, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_WEEK(ChronoField.DAY_OF_WEEK, ChronoUnit.WEEKS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND);


		private final ChronoField field;

		private final ChronoUnit higherOrder;

		private final ChronoField[] lowerOrders;


		Type(ChronoField field, ChronoUnit higherOrder, ChronoField... lowerOrders) {
			this.field = field;
			this.higherOrder = higherOrder;
			this.lowerOrders = lowerOrders;
		}


		/**
		 * 返回给定时间的此类型的值。
		 * @return 此类型的值
		 */
		public int get(Temporal date) {
			return date.get(this.field);
		}

		/**
		 * 返回此类型的通用范围。例如，此方法
		 * 将为 {@link #MONTH} 返回 0-31。
		 * @return 此字段的范围
		 */
		public ValueRange range() {
			return this.field.range();
		}

		/**
		 * 检查给定值是否有效，即是否在
		 * {@linkplain #range() 范围} 内。
		 * @param value 要检查的值
		 * @return 传入的值
		 * @throws IllegalArgumentException 如果给定值无效
		 */
		public int checkValidValue(int value) {
			if (this == DAY_OF_WEEK && value == 0) {
				return value;
			}
			else {
				try {
					return this.field.checkValidIntValue(value);
				}
				catch (DateTimeException ex) {
					throw new IllegalArgumentException(ex.getMessage(), ex);
				}
			}
		}

		/**
		 * 将给定时间流逝，其量为此字段当前值与目标值之间的差值。通常，返回的
		 * 时间将具有给定目标作为此类型的当前值，
		 * 但 {@link #DAY_OF_MONTH} 的情况并非如此。
		 * @param temporal 要流逝的时间
		 * @param goal 目标值
		 * @param <T> 时间的类型
		 * @return 流逝后的时间，通常 {@code goal} 作为此类型的值。
		 */
		public <T extends Temporal & Comparable<? super T>> T elapseUntil(T temporal, int goal) {
			int current = get(temporal);
			ValueRange range = temporal.range(this.field);
			if (current < goal) {
				if (range.isValidIntValue(goal)) {
					return cast(temporal.with(this.field, goal));
				}
				else {
					// 目标无效，例如 2 月 29 日，所以向前滚动
					long amount = range.getMaximum() - current + 1;
					return this.field.getBaseUnit().addTo(temporal, amount);
				}
			}
			else {
				long amount = goal + range.getMaximum() - current + 1 - range.getMinimum();
				return this.field.getBaseUnit().addTo(temporal, amount);
			}
		}

		/**
		 * 向前滚动给定时间，直到它到达下一个更高
		 * 有序字段。调用此方法等效于调用
		 * {@link #elapseUntil(Temporal, int)}，目标设置为此字段范围的最小值。
		 * @param temporal 要向前滚动的时间
		 * @param <T> 时间的类型
		 * @return 向前滚动后的时间
		 */
		public <T extends Temporal & Comparable<? super T>> T rollForward(T temporal) {
			T result = this.higherOrder.addTo(temporal, 1);
			ValueRange range = result.range(this.field);
			return this.field.adjustInto(result, range.getMinimum());
		}

		/**
		 * 将给定时间及其所有低阶字段重置为其
		 * 最小值。例如，对于 {@link #MINUTE}，此方法
		 * 将纳秒、秒<strong>和</strong>分钟重置为 0。
		 * @param temporal 要重置的时间
		 * @param <T> 时间的类型
		 * @return 重置后的时间
		 */
		public <T extends Temporal> T reset(T temporal) {
			for (ChronoField lowerOrder : this.lowerOrders) {
				if (temporal.isSupported(lowerOrder)) {
					temporal = lowerOrder.adjustInto(temporal, temporal.range(lowerOrder).getMinimum());
				}
			}
			return temporal;
		}

		@Override
		public String toString() {
			return this.field.toString();
		}
	}

}
