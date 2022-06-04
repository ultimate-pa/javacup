package com.github.jhoenicke.javacup;

import java.util.Set;
import java.util.TreeSet;

import com.github.jhoenicke.javacup.runtime.Symbol;

public class ErrorManager {

	private static ErrorManager errorManager;
	
	static enum Severity {
		Info,
		Warning,
		Error,
		Fatal,
	}

	private int infos = 0;
	private int warnings = 0;
	private int errors = 0;
	private int fatals = 0;

	private Set<String> filter;
	
	private ErrorManager() {
	}

	public static ErrorManager getManager() {
		if (errorManager == null) errorManager = new ErrorManager();
		return errorManager;
	}

	public static void clear () {
		errorManager = null;
	}

	private Set<String> getFilter () {
		if (filter == null) filter = new TreeSet<>();
		return filter;
	}

	public int getFatalCount() {
		return fatals;
	}

	public int getErrorCount() {
		return errors;
	}

	public int getWarningCount() {
		return warnings;
	}

	/**
	 * Error message format:
	 *		ERRORLEVEL : MESSAGE @ Symbol: name#token=="value"(line/column)
	 *		ERRORLEVEL : MESSAGE
	 **/
	
	private void emit (Severity severity, String message, Symbol sym) {
		if (sym != null && sym.value != null) {
			if (getFilter().contains(sym.value.toString())) return;
			getFilter().add(sym.value.toString());
		}
		StringBuilder tmp = new StringBuilder();
		tmp.append(severity.name());
		tmp.append(": ");
		tmp.append(message);
		if (sym != null) {
			tmp.append(" @ ");
			tmp.append(sym);
		}
		System.err.println(tmp);
	}

	public void emit_info(String message) {
		emit_info(message, null);
	}

	public void emit_info(String message, Symbol sym) {
		emit(Severity.Info, message, sym);
		infos++;
	}

	public void emit_warning(String message) {
		emit_warning(message, null);
	}

	public void emit_warning(String message, Symbol sym) {
		emit(Severity.Warning, message, sym);
		warnings++;
	}

	public void emit_error(String message) {
		emit_error(message, null);
	}

	public void emit_error(String message, Symbol sym) {
		emit(Severity.Error, message, sym);
		errors++;
	}

	public void emit_fatal(String message) {
		emit_fatal(message, null);
	}

	public void emit_fatal(String message, Symbol sym) {
		emit(Severity.Fatal, message, sym);
		fatals++;
	}

}
