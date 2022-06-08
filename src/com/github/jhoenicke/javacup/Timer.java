package com.github.jhoenicke.javacup;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class Timer {

	public enum TIMESTAMP {
		preliminary_time,
		final_time,

		parse_time,
		check_time,
		build_time,
		emit_time,
		dump_time,
		
		nullability_time,
		first_time,
		machine_time,
		table_time,
		reduce_check_time,
		
		symbols_time,
		action_code_time,
		production_table_time,
		action_table_time,
		goto_table_time,
		parser_time,
		
	}

	private Stack<Long> starts;
	
	private Map<TIMESTAMP, Long> times = null;

	private Stack<Long> getStarts() {
		if (starts == null) starts = new Stack<>();
		return starts;
	}

	private Map<TIMESTAMP, Long> getTimes() {
		if (times == null) times = new TreeMap<>();
		return times;
	}
	
	public void clearAllTimer () {
		starts = null;
		times = null;
	}

	public void pushTimer () {
		getStarts().add(System.currentTimeMillis());
	}

	public void popTimer (TIMESTAMP timeStamp) {
		if (getStarts().isEmpty()) {
			ErrorManager.getManager().emit_fatal("Timer stack empty for : "+timeStamp.name());
		}
		long started = getStarts().pop();
		long current = System.currentTimeMillis();
		getTimes().put(timeStamp, current-started );
	}

	public void insertTime (TIMESTAMP timeStamp) {
		getTimes().put(timeStamp, System.currentTimeMillis());
	}

	public boolean hasTime (TIMESTAMP timeStamp) {
		return getTimes().containsKey(timeStamp);
	}
	
	public long getTime (TIMESTAMP timeStamp) {
		return getTimes().get(timeStamp);
	}

}
