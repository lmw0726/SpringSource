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

package org.springframework.scheduling.support;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 针对
 * <a href="https://www.manpagez.com/man/5/crontab/">Crontab 模式</a>的日期序列生成器，
 * 允许客户端指定序列匹配的模式。
 *
 * <p>该模式是一个由六个空格分隔的字段组成的列表，分别表示：
 * 秒、分、时、日、月、星期。月份和星期名称可以使用英文名称的前三个字母。
 *
 * <p>示例模式：
 * <ul>
 * <li>"0 0 * * * *" = 每天每小时的整点。</li>
 * <li>"*&#47;10 * * * * *" = 每十秒一次。</li>
 * <li>"0 0 8-10 * * *" = 每天的 8、9 和 10 点。</li>
 * <li>"0 0 6,19 * * *" = 每天的上午 6:00 和晚上 7:00。</li>
 * <li>"0 0/30 8-10 * * *" = 每天的 8:00、8:30、9:00、9:30、10:00 和 10:30。</li>
 * <li>"0 0 9-17 * * MON-FRI" = 工作日每小时的整点，从九点到五点</li>
 * <li>"0 0 0 25 12 ?" = 每年圣诞节午夜</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Ruslan Sibgatullin
 * @since 3.0
 * @see CronTrigger
 * @deprecated as of 5.3, in favor of {@link CronExpression}
 */
@Deprecated
public class CronSequenceGenerator {

	private final String expression;

	@Nullable
	private final TimeZone timeZone;

	private final BitSet months = new BitSet(12);

	private final BitSet daysOfMonth = new BitSet(31);

	private final BitSet daysOfWeek = new BitSet(7);

	private final BitSet hours = new BitSet(24);

	private final BitSet minutes = new BitSet(60);

	private final BitSet seconds = new BitSet(60);


	/**
	 * 根据提供的模式构造 {@code CronSequenceGenerator}，
	 * 使用默认的 {@link TimeZone}。
	 * @param expression 空格分隔的时间字段列表
	 * @throws IllegalArgumentException 如果无法解析该模式
	 * @see java.util.TimeZone#getDefault()
	 * @deprecated 从 5.3 开始，推荐使用 {@link CronExpression#parse(String)}
	 */
	@Deprecated
	public CronSequenceGenerator(String expression) {
		this(expression, TimeZone.getDefault());
	}

	/**
	 * 根据提供的模式构造 {@code CronSequenceGenerator}，
	 * 使用指定的 {@link TimeZone}。
	 * @param expression 空格分隔的时间字段列表
	 * @param timeZone 用于生成触发时间的时区
	 * @throws IllegalArgumentException 如果无法解析该模式
	 * @deprecated 从 5.3 开始，推荐使用 {@link CronExpression#parse(String)}
	 */
	@Deprecated
	public CronSequenceGenerator(String expression, TimeZone timeZone) {
		this.expression = expression;
		this.timeZone = timeZone;
		parse(expression);
	}

	private CronSequenceGenerator(String expression, String[] fields) {
		this.expression = expression;
		this.timeZone = null;
		doParse(fields);
	}


	/**
	 * 返回该序列生成器所构建的 cron 模式。
	 */
	String getExpression() {
		return this.expression;
	}


	/**
	 * 获取序列中匹配 Cron 模式的下一个 {@link Date}，且在提供的值之后。
	 * 返回值将包含整数秒，并且在输入值之后。
	 * @param date 种子值
	 * @return 匹配模式的下一个值
	 */
	public Date next(Date date) {
		/*
		算法步骤：

		1. 从整数秒开始（如有必要向上取整）

		2. 如果秒匹配则继续，否则找到下一个匹配：
		2.1 如果下一个匹配在下一分钟则向前滚动

		3. 如果分钟匹配则继续，否则找到下一个匹配
		3.1 如果下一个匹配在下一小时则向前滚动
		3.2 重置秒并回到步骤 2

		4. 如果小时匹配则继续，否则找到下一个匹配
		4.1 如果下一个匹配在下一天则向前滚动，
		4.2 重置分钟和秒并回到步骤 2
		*/

		Calendar calendar = new GregorianCalendar();
		calendar.setTimeZone(this.timeZone);
		calendar.setTime(date);

		// 首先，重置毫秒并尝试从那里开始计算...
		calendar.set(Calendar.MILLISECOND, 0);
		long originalTimestamp = calendar.getTimeInMillis();
		doNext(calendar, calendar.get(Calendar.YEAR));

		if (calendar.getTimeInMillis() == originalTimestamp) {
			// 我们回到了原始时间戳 - 向上取整到下一个整数秒并重试...
			calendar.add(Calendar.SECOND, 1);
			doNext(calendar, calendar.get(Calendar.YEAR));
		}

		return calendar.getTime();
	}

	private void doNext(Calendar calendar, int dot) {
		List<Integer> resets = new ArrayList<>();

		int second = calendar.get(Calendar.SECOND);
		List<Integer> emptyList = Collections.emptyList();
		int updateSecond = findNext(this.seconds, second, calendar, Calendar.SECOND, Calendar.MINUTE, emptyList);
		if (second == updateSecond) {
			resets.add(Calendar.SECOND);
		}

		int minute = calendar.get(Calendar.MINUTE);
		int updateMinute = findNext(this.minutes, minute, calendar, Calendar.MINUTE, Calendar.HOUR_OF_DAY, resets);
		if (minute == updateMinute) {
			resets.add(Calendar.MINUTE);
		}
		else {
			doNext(calendar, dot);
		}

		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int updateHour = findNext(this.hours, hour, calendar, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_WEEK, resets);
		if (hour == updateHour) {
			resets.add(Calendar.HOUR_OF_DAY);
		}
		else {
			doNext(calendar, dot);
		}

		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		int updateDayOfMonth = findNextDay(calendar, this.daysOfMonth, dayOfMonth, this.daysOfWeek, dayOfWeek, resets);
		if (dayOfMonth == updateDayOfMonth) {
			resets.add(Calendar.DAY_OF_MONTH);
		}
		else {
			doNext(calendar, dot);
		}

		int month = calendar.get(Calendar.MONTH);
		int updateMonth = findNext(this.months, month, calendar, Calendar.MONTH, Calendar.YEAR, resets);
		if (month != updateMonth) {
			if (calendar.get(Calendar.YEAR) - dot > 4) {
				throw new IllegalArgumentException("Invalid cron expression \"" + this.expression +
						"\" led to runaway search for next trigger");
			}
			doNext(calendar, dot);
		}

	}

	private int findNextDay(Calendar calendar, BitSet daysOfMonth, int dayOfMonth, BitSet daysOfWeek, int dayOfWeek,
			List<Integer> resets) {

		int count = 0;
		int max = 366;
		// java.util.Calendar 中的 DAY_OF_WEEK 值从 1（星期日）开始，
		// 但在 cron 模式中从 0 开始，所以我们在这里减去 1
		while ((!daysOfMonth.get(dayOfMonth) || !daysOfWeek.get(dayOfWeek - 1)) && count++ < max) {
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			reset(calendar, resets);
		}
		if (count >= max) {
			throw new IllegalArgumentException("Overflow in day for expression \"" + this.expression + "\"");
		}
		return dayOfMonth;
	}

	/**
	 * 在提供的位集中搜索给定值之后的下一个已设置位，
	 * 并重置日历。
	 * @param bits 表示字段允许值的 {@link BitSet}
	 * @param value 字段的当前值
	 * @param calendar 在遍历位集时递增的日历
	 * @param field 要在日历中递增的字段（参见
	 * {@link Calendar} 中定义有效字段的静态常量）
	 * @param lowerOrders 应被重置的日历字段 ID（即比目标字段
	 * 低优先级的字段）
	 * @return 日历字段中序列的下一个值
	 */
	private int findNext(BitSet bits, int value, Calendar calendar, int field, int nextField, List<Integer> lowerOrders) {
		int nextValue = bits.nextSetBit(value);
		// 如有必要则翻页
		if (nextValue == -1) {
			calendar.add(nextField, 1);
			reset(calendar, Collections.singletonList(field));
			nextValue = bits.nextSetBit(0);
		}
		if (nextValue != value) {
			calendar.set(field, nextValue);
			reset(calendar, lowerOrders);
		}
		return nextValue;
	}

	/**
	 * Reset the calendar setting all the fields provided to zero.
	 */
	private void reset(Calendar calendar, List<Integer> fields) {
		for (int field : fields) {
			calendar.set(field, field == Calendar.DAY_OF_MONTH ? 1 : 0);
		}
	}


	// Parsing logic invoked by the constructor

	/**
	 * Parse the given pattern expression.
	 */
	private void parse(String expression) throws IllegalArgumentException {
		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (!areValidCronFields(fields)) {
			throw new IllegalArgumentException(String.format(
					"Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
		}
		doParse(fields);
	}

	private void doParse(String[] fields) {
		setNumberHits(this.seconds, fields[0], 0, 60);
		setNumberHits(this.minutes, fields[1], 0, 60);
		setNumberHits(this.hours, fields[2], 0, 24);
		setDaysOfMonth(this.daysOfMonth, fields[3]);
		setMonths(this.months, fields[4]);
		setDays(this.daysOfWeek, replaceOrdinals(fields[5], "SUN,MON,TUE,WED,THU,FRI,SAT"), 8);

		if (this.daysOfWeek.get(7)) {
			// Sunday can be represented as 0 or 7
			this.daysOfWeek.set(0);
			this.daysOfWeek.clear(7);
		}
	}

	/**
	 * Replace the values in the comma-separated list (case insensitive)
	 * with their index in the list.
	 * @return a new String with the values from the list replaced
	 */
	private String replaceOrdinals(String value, String commaSeparatedList) {
		String[] list = StringUtils.commaDelimitedListToStringArray(commaSeparatedList);
		for (int i = 0; i < list.length; i++) {
			String item = list[i].toUpperCase();
			value = StringUtils.replace(value.toUpperCase(), item, "" + i);
		}
		return value;
	}

	private void setDaysOfMonth(BitSet bits, String field) {
		int max = 31;
		// Days of month start with 1 (in Cron and Calendar) so add one
		setDays(bits, field, max + 1);
		// ... and remove it from the front
		bits.clear(0);
	}

	private void setDays(BitSet bits, String field, int max) {
		if (field.contains("?")) {
			field = "*";
		}
		setNumberHits(bits, field, 0, max);
	}

	private void setMonths(BitSet bits, String value) {
		int max = 12;
		value = replaceOrdinals(value, "FOO,JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC");
		BitSet months = new BitSet(13);
		// Months start with 1 in Cron and 0 in Calendar, so push the values first into a longer bit set
		setNumberHits(months, value, 1, max + 1);
		// ... and then rotate it to the front of the months
		for (int i = 1; i <= max; i++) {
			if (months.get(i)) {
				bits.set(i - 1);
			}
		}
	}

	private void setNumberHits(BitSet bits, String value, int min, int max) {
		String[] fields = StringUtils.delimitedListToStringArray(value, ",");
		for (String field : fields) {
			if (!field.contains("/")) {
				// Not an incrementer so it must be a range (possibly empty)
				int[] range = getRange(field, min, max);
				bits.set(range[0], range[1] + 1);
			}
			else {
				String[] split = StringUtils.delimitedListToStringArray(field, "/");
				if (split.length > 2) {
					throw new IllegalArgumentException("Incrementer has more than two fields: '" +
							field + "' in expression \"" + this.expression + "\"");
				}
				int[] range = getRange(split[0], min, max);
				if (!split[0].contains("-")) {
					range[1] = max - 1;
				}
				int delta = Integer.parseInt(split[1]);
				if (delta <= 0) {
					throw new IllegalArgumentException("Incrementer delta must be 1 or higher: '" +
							field + "' in expression \"" + this.expression + "\"");
				}
				for (int i = range[0]; i <= range[1]; i += delta) {
					bits.set(i);
				}
			}
		}
	}

	private int[] getRange(String field, int min, int max) {
		int[] result = new int[2];
		if (field.contains("*")) {
			result[0] = min;
			result[1] = max - 1;
			return result;
		}
		if (!field.contains("-")) {
			result[0] = result[1] = Integer.parseInt(field);
		}
		else {
			String[] split = StringUtils.delimitedListToStringArray(field, "-");
			if (split.length > 2) {
				throw new IllegalArgumentException("Range has more than two fields: '" +
						field + "' in expression \"" + this.expression + "\"");
			}
			result[0] = Integer.parseInt(split[0]);
			result[1] = Integer.parseInt(split[1]);
		}
		if (result[0] >= max || result[1] >= max) {
			throw new IllegalArgumentException("Range exceeds maximum (" + max + "): '" +
					field + "' in expression \"" + this.expression + "\"");
		}
		if (result[0] < min || result[1] < min) {
			throw new IllegalArgumentException("Range less than minimum (" + min + "): '" +
					field + "' in expression \"" + this.expression + "\"");
		}
		if (result[0] > result[1]) {
			throw new IllegalArgumentException("Invalid inverted range: '" + field +
					"' in expression \"" + this.expression + "\"");
		}
		return result;
	}


	/**
	 * Determine whether the specified expression represents a valid cron pattern.
	 * @param expression the expression to evaluate
	 * @return {@code true} if the given expression is a valid cron expression
	 * @since 4.3
	 */
	public static boolean isValidExpression(@Nullable String expression) {
		if (expression == null) {
			return false;
		}
		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (!areValidCronFields(fields)) {
			return false;
		}
		try {
			new CronSequenceGenerator(expression, fields);
			return true;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static boolean areValidCronFields(@Nullable String[] fields) {
		return (fields != null && fields.length == 6);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CronSequenceGenerator)) {
			return false;
		}
		CronSequenceGenerator otherCron = (CronSequenceGenerator) other;
		return (this.months.equals(otherCron.months) && this.daysOfMonth.equals(otherCron.daysOfMonth) &&
				this.daysOfWeek.equals(otherCron.daysOfWeek) && this.hours.equals(otherCron.hours) &&
				this.minutes.equals(otherCron.minutes) && this.seconds.equals(otherCron.seconds));
	}

	@Override
	public int hashCode() {
		return (17 * this.months.hashCode() + 29 * this.daysOfMonth.hashCode() + 37 * this.daysOfWeek.hashCode() +
				41 * this.hours.hashCode() + 53 * this.minutes.hashCode() + 61 * this.seconds.hashCode());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + this.expression;
	}

}
