/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.extra;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * An immutable interval of time between two instants.
 * <p>
 * An interval represents the time on the time-line between two {@link Instant}s.
 * The class stores the start and end instants, with the start inclusive and the end exclusive.
 * The end instant is always greater than or equal to the start instant.
 * <p>
 * The {@link Duration} of an interval can be obtained, but is a separate concept.
 * An interval is connected to the time-line, whereas a duration is not.
 * <p>
 * Intervals are not comparable. To compare the length of two intervals, it is
 * generally recommended to compare their durations.
 *
 * <h3>Implementation Requirements:</h3>
 * This class is immutable and thread-safe.
 * <p>
 * This class must be treated as a value type. Do not synchronize, rely on the
 * identity hash code or use the distinction between equals() and ==.
 */
public final class Interval
        implements Serializable {

    /**
     * An interval over the whole time-line.
     */
    public static final Interval ALL = new Interval(Instant.MIN, Instant.MAX);

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 8375285238652L;

    /**
     * The start instant (inclusive).
     */
    private final Instant start;
    /**
     * The end instant (exclusive).
     */
    private final Instant end;

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Interval} from the start and end instant.
     * <p>
     * The end instant must not be before the start instant.
     *
     * @param startInclusive  the start instant, inclusive, MIN_DATE treated as unbounded, not null
     * @param endExclusive  the end instant, exclusive, MAX_DATE treated as unbounded, not null
     * @return the half-open interval, not null
     * @throws DateTimeException if the end is before the start
     */
    public static Interval of(Instant startInclusive, Instant endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (endExclusive.isBefore(startInclusive)) {
            throw new DateTimeException("End instant must on or after start instant");
        }
        return new Interval(startInclusive, endExclusive);
    }

    /**
     * Obtains an instance of {@code Interval} from the start and a duration.
     * <p>
     * The end instant is calculated as the start plus the duration.
     * The duration must not be negative.
     *
     * @param startInclusive  the start instant, inclusive, not null
     * @param duration  the duration from the start to the end, not null
     * @return the interval, not null
     * @throws DateTimeException if the end is before the start,
     *  or if the duration addition cannot be made
     * @throws ArithmeticException if numeric overflow occurs when adding the duration
     */
    public static Interval of(Instant startInclusive, Duration duration) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new DateTimeException("Duration must not be zero or negative");
        }
        return new Interval(startInclusive, startInclusive.plus(duration));
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code Interval} from a text string such as
     * {@code 2007-12-03T10:15:30Z/2007-12-04T10:15:30Z}, where the end instant is exclusive.
     * <p>
     * The string must consist of one of the following three formats:
     * <ul>
     * <li>a representations of an {@link OffsetDateTime}, followed by a forward slash,
     *  followed by a representation of a {@link OffsetDateTime}
     * <li>a representation of an {@link OffsetDateTime}, followed by a forward slash,
     *  followed by a representation of a {@link Duration}
     * <li>a representation of a {@link Duration}, followed by a forward slash,
     *  followed by a representation of an {@link OffsetDateTime}
     * </ul>
     *
     *
     * @param text  the text to parse, not null
     * @return the parsed interval, not null
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static Interval parse(CharSequence text) {
        Objects.requireNonNull(text, "text");
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '/') {
                char firstChar = text.charAt(0);
                if (firstChar == 'P' || firstChar == 'p') {
                    // duration followed by instant
                    Duration duration = Duration.parse(text.subSequence(0, i));
                    Instant end = OffsetDateTime.parse(text.subSequence(i + 1, text.length())).toInstant();
                    return Interval.of(end.minus(duration), end);
                } else {
                    // instant followed by instant or duration
                    Instant start = OffsetDateTime.parse(text.subSequence(0, i)).toInstant();
                    if (i + 1 < text.length()) {
                        char c = text.charAt(i + 1);
                        if (c == 'P' || c == 'p') {
                            Duration duration = Duration.parse(text.subSequence(i + 1, text.length()));
                            return Interval.of(start, start.plus(duration));
                        }
                    }
                    Instant end = OffsetDateTime.parse(text.subSequence(i + 1, text.length())).toInstant();
                    return Interval.of(start, end);
                }
            }
        }
        throw new DateTimeParseException("Interval cannot be parsed, no forward slash found", text, 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param startInclusive  the start instant, inclusive, validated not null
     * @param endExclusive  the end instant, exclusive, validated not null
     */
    private Interval(Instant startInclusive, Instant endExclusive) {
        this.start = startInclusive;
        this.end = endExclusive;
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the start of this time interval, inclusive.
     * <p>
     * This will return {@link Instant#MIN} if the range is unbounded at the start.
     * In this case, the range includes all dates into the far-past.
     *
     * @return the start of the time interval
     */
    public Instant getStart() {
        return start;
    }

    /** 
     * Gets the end of this time interval, exclusive.
     * <p>
     * This will return {@link Instant#MAX} if the range is unbounded at the end.
     * In this case, the range includes all dates into the far-future.
     *
     * @return the end of the time interval, exclusive
     */
    public Instant getEnd() {
        return end;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the range is empty.
     * <p>
     * An empty range occurs when the start date equals the inclusive end date.
     * 
     * @return true if the range is empty
     */
    public boolean isEmpty() {
        return start.equals(end);
    }

    /**
     * Checks if the start of the interval is unbounded.
     * 
     * @return true if start is unbounded
     */
    public boolean isUnboundedStart() {
        return start.equals(Instant.MIN);
    }

    /**
     * Checks if the end of the interval is unbounded.
     * 
     * @return true if end is unbounded
     */
    public boolean isUnboundedEnd() {
        return end.equals(Instant.MAX);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns a copy of this range with the specified start instant.
     *
     * @param start  the start instant for the new interval, not null
     * @return an interval with the end from this interval and the specified start
     * @throws DateTimeException if the resulting interval has end before start
     */
    public Interval withStart(Instant start) {
        return Interval.of(start, end);
    }

    /**
     * Returns a copy of this range with the specified end instant.
     *
     * @param end  the end instant for the new interval, not null
     * @return an interval with the start from this interval and the specified end
     * @throws DateTimeException if the resulting interval has end before start
     */
    public Interval withEnd(Instant end) {
        return Interval.of(start, end);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this interval contains the specified instant.
     * <p>
     * This checks if the specified instant is within the bounds of this interval.
     * If this range has an unbounded start then {@code contains(Instant#MIN)} returns true.
     * If this range has an unbounded end then {@code contains(Instant#MAX)} returns true.
     * If this range is empty then this method always returns false.
     *
     * @param instant  the instant, not null
     * @return true if this interval contains the instant
     */
    public boolean contains(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return start.compareTo(instant) <= 0 && (instant.compareTo(end) < 0 || isUnboundedEnd());
    }

    /**
     * Checks if this interval encloses the specified interval.
     * <p>
     * This checks if the bounds of the specified interval are within the bounds of this interval.
     * An empty interval encloses itself.
     *
     * @param other  the other interval, not null
     * @return true if this interval contains the other interval
     */
    public boolean encloses(Interval other) {
        Objects.requireNonNull(other, "other");
        return start.compareTo(other.start) <= 0 && other.end.compareTo(end) <= 0;
    }

    /**
     * Checks if this interval abuts the specified interval.
     * <p>
     * The result is true if the end of this interval is the start of the other, or vice versa.
     * An empty interval does not abut itself.
     *
     * @param other  the other interval, not null
     * @return true if this interval abuts the other interval
     */
    public boolean abuts(Interval other) {
        Objects.requireNonNull(other, "other");
        return end.equals(other.start) ^ start.equals(other.end);
    }

    /**
     * Checks if this interval is connected to the specified interval.
     * <p>
     * The result is true if the two intervals have an enclosed interval in common, even if that interval is empty.
     * An empty interval is connected to itself.
     * <p>
     * This is equivalent to {@code (overlaps(other) || abuts(other))}.
     *
     * @param other  the other interval, not null
     * @return true if this interval is connected to the other interval
     */
    public boolean isConnected(Interval other) {
        Objects.requireNonNull(other, "other");
        return this.equals(other) || (start.compareTo(other.end) <= 0 && other.start.compareTo(end) <= 0);
    }

    /**
     * Checks if this interval overlaps the specified interval.
     * <p>
     * The result is true if the the two intervals share some part of the time-line.
     * An empty interval overlaps itself.
     * <p>
     * This is equivalent to {@code (isConnected(other) && !abuts(other))}.
     *
     * @param other  the time interval to compare to, null means a zero length interval now
     * @return true if the time intervals overlap
     */
    public boolean overlaps(Interval other) {
        Objects.requireNonNull(other, "other");
        return other.equals(this) || (start.compareTo(other.end) < 0 && other.start.compareTo(end) < 0);
    }

    //-----------------------------------------------------------------------
    /**
     * Calculates the interval that is the intersection of this interval and the specified interval.
     * <p>
     * This finds the intersection of two intervals.
     * This throws an exception if the two intervals are not {@linkplain #isConnected(Interval) connected}.
     * 
     * @param other  the other interval to check for, not null
     * @return the interval that is the intersection of the two intervals
     * @throws DateTimeException if the intervals do not connect
     */
    public Interval intersection(Interval other) {
        Objects.requireNonNull(other, "other");
        if (isConnected(other) == false) {
            throw new DateTimeException("Intervals do not connect: " + this + " and " + other);
        }
        int cmpStart = start.compareTo(other.start);
        int cmpEnd = end.compareTo(other.end);
        if (cmpStart >= 0 && cmpEnd <= 0) {
            return this;
        } else if (cmpStart <= 0 && cmpEnd >= 0) {
            return other;
        } else {
            Instant newStart = (cmpStart >= 0 ? start : other.start);
            Instant newEnd = (cmpEnd <= 0 ? end : other.end);
            return Interval.of(newStart, newEnd);
        }
    }

    /**
     * Calculates the interval that is the union of this interval and the specified interval.
     * <p>
     * This finds the union of two intervals.
     * This throws an exception if the two intervals are not {@linkplain #isConnected(Interval) connected}.
     * 
     * @param other  the other interval to check for, not null
     * @return the interval that is the union of the two intervals
     * @throws DateTimeException if the intervals do not connect
     */
    public Interval union(Interval other) {
        Objects.requireNonNull(other, "other");
        if (isConnected(other) == false) {
            throw new DateTimeException("Intervals do not connect: " + this + " and " + other);
        }
        int cmpStart = start.compareTo(other.start);
        int cmpEnd = end.compareTo(other.end);
        if (cmpStart >= 0 && cmpEnd <= 0) {
            return other;
        } else if (cmpStart <= 0 && cmpEnd >= 0) {
            return this;
        } else {
            Instant newStart = (cmpStart >= 0 ? other.start : start);
            Instant newEnd = (cmpEnd <= 0 ? other.end : end);
            return Interval.of(newStart, newEnd);
        }
    }

    /**
     * Calculates the smallest interval that encloses this interval and the specified interval.
     * <p>
     * The result of this method will {@linkplain #encloses(Interval) enclose}
     * this interval and the specified interval.
     * 
     * @param other  the other interval to check for, not null
     * @return the interval that spans the two intervals
     */
    public Interval span(Interval other) {
        Objects.requireNonNull(other, "other");
        int cmpStart = start.compareTo(other.start);
        int cmpEnd = end.compareTo(other.end);
        Instant newStart = (cmpStart >= 0 ? other.start : start);
        Instant newEnd = (cmpEnd <= 0 ? other.end : end);
        return Interval.of(newStart, newEnd);
    }

    //-------------------------------------------------------------------------
    /**
     * Checks if this interval is after the specified instant.
     * <p>
     * The result is true if the this instant starts after the specified instant.
     * An empty interval behaves as though it is an instant for comparison purposes.
     *
     * @param instant  the other instant to compare to, not null
     * @return true if the start of this interval is after the specified instant
     */
    public boolean isAfter(Instant instant) {
        return start.compareTo(instant) > 0;
    }

    /**
     * Checks if this interval is before the specified instant.
     * <p>
     * The result is true if the this instant ends before the specified instant.
     * Since intervals do not include their end points, this will return true if the
     * instant equals the end of the interval.
     * An empty interval behaves as though it is an instant for comparison purposes.
     *
     * @param instant  the other instant to compare to, not null
     * @return true if the start of this interval is before the specified instant
     */
    public boolean isBefore(Instant instant) {
        return end.compareTo(instant) <= 0 && start.compareTo(instant) < 0;
    }

    //-------------------------------------------------------------------------
    /**
     * Checks if this interval is after the specified interval.
     * <p>
     * The result is true if the this instant starts after the end of the specified interval.
     * Since intervals do not include their end points, this will return true if the
     * instant equals the end of the interval.
     * An empty interval behaves as though it is an instant for comparison purposes.
     *
     * @param interval  the other interval to compare to, not null
     * @return true if this instant is after the specified instant
     */
    public boolean isAfter(Interval interval) {
        return start.compareTo(interval.end) >= 0 && !interval.equals(this);
    }

    /**
     * Checks if this interval is before the specified interval.
     * <p>
     * The result is true if the this instant ends before the start of the specified interval.
     * Since intervals do not include their end points, this will return true if the
     * two intervals abut.
     * An empty interval behaves as though it is an instant for comparison purposes.
     *
     * @param interval  the other interval to compare to, not null
     * @return true if this instant is before the specified instant
     */
    public boolean isBefore(Interval interval) {
        return end.compareTo(interval.start) <= 0 && !interval.equals(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains the duration of this interval.
     * <p>
     * An {@code Interval} is associated with two specific instants on the time-line.
     * A {@code Duration} is simply an amount of time, separate from the time-line.
     *
     * @return the duration of the time interval
     * @throws ArithmeticException if the calculation exceeds the capacity of {@code Duration}
     */
    public Duration toDuration() {
        return Duration.between(start, end);
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if this interval is equal to another interval.
     * <p>
     * Compares this {@code Interval} with another ensuring that the two instants are the same.
     * Only objects of type {@code Interval} are compared, other types return false.
     *
     * @param obj  the object to check, null returns false
     * @return true if this is equal to the other interval
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Interval) {
            Interval other = (Interval) obj;
            return start.equals(other.start) && end.equals(other.end);
        }
        return false;
    }

    /**
     * A hash code for this interval.
     *
     * @return a suitable hash code
     */
    @Override
    public int hashCode() {
        return start.hashCode() ^ end.hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * Outputs this interval as a {@code String}, such as {@code 2007-12-03T10:15:30/2007-12-04T10:15:30}.
     * <p>
     * The output will be the ISO-8601 format formed by combining the
     * {@code toString()} methods of the two instants, separated by a forward slash.
     *
     * @return a string representation of this instant, not null
     */
    @Override
    public String toString() {
        return start.toString() + '/' + end.toString();
    }

}
