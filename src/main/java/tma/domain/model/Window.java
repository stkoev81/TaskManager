package tma.domain.model;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Entity;

import tma.exceptions.Assertion;
import tma.exceptions.BadInput;
import tma.util.Utils;

/**
 * Represents a window of opportunity for doing a task; basically it is a period of time.
 **/
@Entity
public class Window extends BaseEntity {
	public static final Logger LOGGER = Logger.getLogger(Window.class.getName());

	public Window() {
	}

	public Window(Long start, Long end) {
		this.start = start;
		this.end = end;
	}

	private Long start;
	private Long end;

	/**
	 * Checks if the period of time represented by the arguments fits within this window.
	 * 
	 * @throws Assertion
	 *             if following not true: start < end
	 */
	public boolean fits(long start, long end) {
		Utils.assertTrue(end > start);
		boolean result = true;
		if (this.start != null && this.start > start) {
			result = false;
		}
		if (this.end != null && this.end < end) {
			result = false;
		}
		return result;
	}

	public Long getStart() {
		return start;
	}

	/**
	 * The start time of the window; if null it means that the window extends to negative infinity in time.
	 * 
	 * @param start
	 */
	public void setStart(Long start) {
		this.start = start;
	}

	public Long getEnd() {
		return end;
	}

	/**
	 * The end time of the window; if null, it means that the window extends to positive infinity in time.
	 * 
	 * @param end
	 */
	public void setEnd(Long end) {
		this.end = end;
	}

	/**
	 * A valid window obeys these rules: only the start or end, but not both, can be null; if both start and end are not
	 * null, start < end. Basically, if a window has no start and no end, it means the window is infinite in both
	 * directions and is not really a constraint so it should not be used.
	 */
	public List<BadInput> validate() {
		List<BadInput> errs = new LinkedList<BadInput>();

		if (!(start != null || end != null)) {
			errs.add(BadInput.WINDOW_START_END_INVALID);
		}
		if (start != null && end != null) {
			if (!(start < end)) {
				errs.add(BadInput.WINDOW_START_END_INVALID);
			}
		}
		return errs;
	}

}