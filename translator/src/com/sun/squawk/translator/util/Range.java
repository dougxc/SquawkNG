package com.sun.squawk.translator.util;

import com.sun.squawk.translator.Assert;

/**
 * A general utility class for representing a range of (not necessarily
 * consecutive) values.
 */
public class Range {
    /** The first point (inclusive) in the range */
    private int start;
    /** The last point (inclusive) in the range. Valid values are always >= 0. */
    private int end;
    /** The next Range in a list of Ranges. */
    private Range next;

    /**
     * Create a new Range object representing a range of values.
     * @param start The start value of the range.
     * @param prev The last part of an existing range which the new range is
     * extending. If the new range starts immediately after this sub-range,
     * then no new range is created and this sub-range is extended.
     * @return the newly created or extended range.
     */
    static public Range create(int start, Range prev) {
        Range r;
        if (prev != null && (start == prev.end() + 1 || start == prev.end())) {
            r = prev;
        }
        else {
            r = new Range(start, prev);
        }
        return r;
    }

    /**
     * Append a higher range onto this one, merging the two if they are consecutive.
     * @param next The range to append.
     * @return the range containing the end point after the append.
     */
    private Range append(Range next) {
        Assert.that(next != null);
        Assert.that(start < next.start && end < next.start);
        if (next.start == end+1) {
            end = next.end;
            this.next = next.next;
            return this;
        }
        else {
            this.next = next;
            return next;
        }
    }

    private Range(int start, Range prev) {
        this.start = start;
        if (prev != null) {
            prev.next = this;
        }
        this.end = -1;
    }
    /** @return the start point of this range. */
    public int start()           { return start;   }
    /** @return the end point of this range. */
    public int end()             { return end;     }
    /** @return the sub-range after this range. */
    public Range next()          { return next;    }

    /**
     * Set the end point of this range.
     * @param end the end point.
     */
    public void setEnd(int end) {
        Assert.that(end >= this.end);
        Assert.that(end >= this.start);
        this.end = end;
    }

    /**
     * Get a string representation of this range.
     * @return
     */
    public String toString() {
        return start+"-"+end;
    }

    /**
     * Return a string representation for the list of ranges starting at head.
     * @param head
     * @return
     */
    public static String toString(Range head) {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        while (head != null) {
            buf.append(head.start).append("-").append(head.end);
            head = head.next();
            if (head != null) {
                buf.append(", ");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Compare this range to another range and return -1 if this range's start and
     * end are both less than the other range's start, 1 if the reverse is true and
     * 0 otherwise.
     * @param other
     * @return
     */
    public int compare(Range other) {
        if (this == other) {
            return 0;
        }
        if (start < other.start) {
            if (end < other.start) {
                return -1;
            }
            return 0;
        }
        // start > other.start
        if (other.end < start) {
            return 1;
        }
        return 0;
    }

    /**
     * Merge two non-intersecting ranges into one. A precondition is that the two ranges
     * do not intersect.
     * @param r1
     * @param r2
     * @return
     */
    public static Range merge(Range r1, Range r2) {
        if (r1 == null) {
            return r2;
        }
        if (r2 == null) {
            return r1;
        }
        int comp = r1.compare(r2);
        Assert.that(comp != 0);
        Range tail;
        Range head;
        if (comp == -1) {
            head = tail = r1;
            r1 = r1.next();
        }
        else {
            head = tail = r2;
            r2 = r2.next();
        }

        while (r1 != null && r2 != null) {
            comp = r1.compare(r2);
            Assert.that(comp != 0);
            if (comp == -1) {
                tail = tail.append(r1);
                r1 = r1.next();
            }
            else {
                tail = tail.append(r2);
                r2 = r2.next();
            }
        }

        if (r1 != null) {
            Assert.that(r2 == null);
            tail = tail.append(r1);
        }
        else if (r2 != null) {
            tail = tail.append(r2);
        }

        return head;
    }

    /**
     * Determine if two ranges intersect with each other.
     * @param r1
     * @param r2
     * @return
     */
    public static boolean intersects(Range r1, Range r2) {
        while (r1 != null && r2 != null) {
            int comp = r1.compare(r2);
            if (comp == 0) {
                return true;
            }
            if (comp == -1) {
                r1 = r1.next();
            }
            else {
                r2 = r2.next();
            }
        }
        return false;
    }
}
