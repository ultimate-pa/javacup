package com.github.jhoenicke.javacup;

import java.util.ArrayList;

public class Options {

	/** User option -- do we print progress messages. */
	public boolean print_progress = false;
	/** User option -- do we produce a dump of the state machine */
	public boolean opt_dump_states = false;
	/** User option -- do we produce a dump of the parse tables */
	public boolean opt_dump_tables = false;
	/** User option -- do we produce a dump of the grammar */
	public boolean opt_dump_grammar = false;
	/** User option -- do we show timing information as a part of the summary */
	public boolean opt_show_timing = false;
	/** User option -- do we run produce extra debugging messages */
	public boolean opt_do_debug = false;
	/**
	 * User option -- do we compact tables by making most common reduce the default
	 * action
	 */
	public boolean opt_compact_red = false;
	/** User option -- use java 1.5 syntax (generics, annotations) */
	public boolean opt_java15 = false;

	/** Package that the resulting code goes into (null is used for unnamed). */
	public String package_name = null;

	/** Directory were the resulting code goes into (null is used for unnamed). */
	public String dest_dir = null;

	/** Name of the generated class for symbol constants. */
	public String symbol_const_class_name = "Sym";

	/** Name of the generated parser class. */
	public String parser_class_name = "Parser";

	/**
	 * TUM changes; proposed by Henning Niss 20050628: Type arguments for class
	 * declaration
	 */
	public String class_type_argument = null;

	/**
	 * User option -- should we include non terminal symbol numbers in the symbol
	 * constant class.
	 */
	public boolean include_non_terms = false;
	/** User option -- do not print a summary. */
	public boolean no_summary = false;
	/** User option -- number of conflicts to expect */
	public int expect_conflicts = 0;

	/** User declarations for direct inclusion in user action class. */
	public String action_code = null;

	/** User declarations for direct inclusion in parser class. */
	public String parser_code = null;

	/** User code for user_init() which is called during parser initialization. */
	public String init_code = null;

	/** User code for scan() which is called to get the next Symbol. */
	public String scan_code = null;

	/** User code that will be called after every reduce call. */
	public String after_reduce_code = null;

	/** List of imports (Strings containing class names) to go with actions. */
	public ArrayList<String> import_list = new ArrayList<String>();

	/** Do we skip warnings? */
	public boolean nowarn = false;

	/* frankf added this 6/18/96 */
	/** User option -- should generator update left/right values? */
	public boolean opt_lr_values = true;
	/**
	 * User option -- should generator generate old style access for left/right
	 * values?
	 */
	public boolean opt_old_lr_values = true;

	/** User option -- should symbols be put in a class or an interface? [CSA] */
	public boolean sym_interface = false;

	/**
	 * User option -- should generator suppress references to
	 * com.github.jhoenicke.javacup.runtime.Scanner for compatibility with old
	 * runtimes?
	 */
	public boolean suppress_scanner = false;

	/** User option -- erase generated files (parser, sym and dumps every run */
	public boolean opt_erase_generated = true;

	/** User option -- never use System.exit() */
	public boolean opt_no_exit = false;

	public boolean setOption(String option) {
		return setOption(option, null);
	}

	public boolean setOption(String option, String arg) {
		if (option.equals("destdir")) {
			if (arg != null) {
				dest_dir = arg;
				return true;
			} else {
				ErrorManager.getManager().emit_fatal("destdir must have a name argument");
				return false;
			}
		}
		if (option.equals("package")) {
			if (arg != null) {
				package_name = arg;
				return true;
			} else {
				ErrorManager.getManager().emit_fatal("package must have a name argument");
				return false;
			}
		}
		if (option.equals("parser")) {
			if (arg != null) {
				parser_class_name = arg;
				return true;
			} else {
				ErrorManager.getManager().emit_fatal("parser must have a name argument");
				return false;
			}
		}
		if (option.equals("typearg")) {
			if (arg != null) {
				opt_java15 = true;
				class_type_argument = option;
				return true;
			} else {
				ErrorManager.getManager().emit_fatal("symbols must have a name argument");
				return false;
			}
		}
		if (option.equals("symbols")) {
			if (arg != null) {
				symbol_const_class_name = arg;
				return true;
			} else {
				ErrorManager.getManager().emit_fatal("symbols must have a name argument");
				return false;
			}
		}
		if (option.equals("nonterms")) {
			include_non_terms = true;
			return true;
		}
		if (option.equals("expect")) {
			if (arg != null) {
				try {
					expect_conflicts = Integer.parseInt(arg);
					return true;
				} catch (NumberFormatException e) {
					ErrorManager.getManager().emit_fatal("expect must be followed by a decimal integer");
					return false;
				}
			} else {
				ErrorManager.getManager().emit_fatal("expect must have a number argument");
				return false;
			}
		}
		if (option.equals("java15")) {
			opt_java15 = true;
			return true;
		}
		if (option.equals("compact_red")) {
			opt_compact_red = true;
			return true;
		}
		if (option.equals("nosummary")) {
			no_summary = true;
			return true;
		}
		if (option.equals("nowarn")) {
			nowarn = true;
			return true;
		} 
		if (option.equals("dump_states")) {
			opt_dump_states = true;
			return true;
		}
		if (option.equals("dump_tables")) {
			opt_dump_tables = true;
			return true;
		}
		if (option.equals("progress")) {
			print_progress = true;
			return true;
		}
		if (option.equals("dump_grammar")) {
			opt_dump_grammar = true;
			return true;
		}
		if (option.equals("dump")) {
			opt_dump_states = opt_dump_tables = opt_dump_grammar = true;
			return true;
		}
		if (option.equals("time")) {
			opt_show_timing = true;
			return true;
		}
		if (option.equals("debug")) {
			opt_do_debug = true;
			return true;
		}
		if (option.equals("nopositions")) {
			opt_lr_values = false;
			opt_old_lr_values = false;
			return true;
		}
		if (option.equals("newpositions")) {
			opt_old_lr_values = false;
			return true;
		}
		if (option.equals("interface")) {
			sym_interface = true;
			return true;
		}
		if (option.equals("noscanner")) {
			suppress_scanner = true;
			return true;
		}
		if (option.equals("noerase")) {
			opt_erase_generated = false;
			return true;
		}
		if (option.equals("noexit")) {
			opt_no_exit = true;
			return true;
		}
		ErrorManager.getManager().emit_fatal("Unrecognized option \"" + option + "\"");
		return false;
	}

}
