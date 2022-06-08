package com.github.jhoenicke.javacup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;

import com.github.jhoenicke.javacup.runtime.ComplexSymbolFactory;

/**
 * This class serves as the main driver for the JavaCup system. It accepts user
 * options and coordinates overall control flow. The main flow of control
 * includes the following activities:
 * <ul>
 * <li>Parse user supplied arguments and options.
 * <li>Open output files.
 * <li>Parse the specification from standard input.
 * <li>Check for unused terminals, non-terminals, and productions.
 * <li>Build the state machine, tables, etc.
 * <li>Output the generated code.
 * <li>Close output files.
 * <li>Print a summary if requested.
 * </ul>
 * 
 * Options to the main program include:
 * <dl>
 * <dt>-package name
 * <dd>specify package generated classes go in [default none]
 * <dt>-parser name
 * <dd>specify parser class name [default "parser"]
 * <dt>-symbols name
 * <dd>specify name for symbol constant class [default "sym"]
 * <dt>-interface
 * <dd>emit symbol constant <i>interface</i>, rather than class
 * <dt>-nonterms
 * <dd>put non terminals in symbol constant class
 * <dt>-expect #
 * <dd>number of conflicts expected/allowed [default 0]
 * <dt>-compact_red
 * <dd>compact tables by defaulting to most frequent reduce
 * <dt>-nowarn
 * <dd>don't warn about useless productions, etc.
 * <dt>-nosummary
 * <dd>don't print the usual summary of parse states, etc.
 * <dt>-progress
 * <dd>print messages to indicate progress of the system
 * <dt>-time
 * <dd>print time usage summary
 * <dt>-dump_grammar
 * <dd>produce a dump of the symbols and grammar
 * <dt>-dump_states
 * <dd>produce a dump of parse state machine
 * <dt>-dump_tables
 * <dd>produce a dump of the parse tables
 * <dt>-dump
 * <dd>produce a dump of all of the above
 * <dt>-debug
 * <dd>turn on debugging messages within JavaCup
 * <dt>-nopositions
 * <dd>don't generate the positions code
 * <dt>-noscanner
 * <dd>don't refer to com.github.jhoenicke.javacup.runtime.Scanner in the parser
 * (for compatibility with old runtimes)
 * <dt>-version
 * <dd>print version information for JavaCUP and halt.
 * </dl>
 * 
 * @version last updated: 7/3/96
 * @author Frank Flannery
 */

public class Main {

	private Timer timer;
	private Options options;

	/** Count of the number of non-reduced productions found. */
	public int not_reduced = 0;

	/** Count of unused terminals. */
	public int unused_term = 0;

	/** Count of unused non terminals. */
	public int unused_non_term = 0;

	/* Additional timing information is also collected in emit */

	/*-----------------------------------------------------------*/
	/*--- Constructor(s) ----------------------------------------*/
	/*-----------------------------------------------------------*/

	public Main() {
		ErrorManager.clear();
		timer = new Timer();
		timer.pushTimer(); // global time
		timer.pushTimer(); // startup time
		options = new Options();
	}

	public int run() throws Exception {
		boolean did_output = false;

		timer.popTimer(Timer.TIMESTAMP.preliminary_time);

		/* parse spec into internal data structures */
		timer.pushTimer();
		if (options.print_progress)
			ErrorManager.getManager().emit_info("Parsing specification...");
		Grammar grammar = parse_grammar_spec();
		if (grammar == null) return 1;
		grammar.add_wildcard_rules();
		timer.popTimer(Timer.TIMESTAMP.parse_time);

		/* check for unused bits */
		timer.pushTimer();
		if (options.print_progress)
			ErrorManager.getManager().emit_info("Checking specification...");
		check_unused(grammar);
		timer.popTimer(Timer.TIMESTAMP.check_time);

		timer.pushTimer();
		/* don't proceed unless we are error free */
		if (ErrorManager.getManager().getFatalCount() == 0 && ErrorManager.getManager().getErrorCount() == 0) {

			/* build the state machine and parse tables */
			timer.pushTimer();
			if (options.print_progress)
				ErrorManager.getManager().emit_info("Building parse tables...");
			build_parser(grammar);
			timer.popTimer(Timer.TIMESTAMP.build_time);

			if (options.opt_erase_generated) 
				emit_erase(grammar);
				
			/* output the generated code, if # of conflicts permits */
			if (ErrorManager.getManager().getWarningCount() != options.expect_conflicts) {
				// conflicts! don't emit code, don't dump tables.
				options.opt_dump_tables = false;
			} else { // everything's okay, emit parser.
				if (options.print_progress)
					ErrorManager.getManager().emit_info("Writing parser...");
				emit_symbols(grammar);
				emit_parser(grammar);
				did_output = true;
				if (options.print_progress)
					ErrorManager.getManager().emit_info("Done...");
			}
		}
		timer.popTimer(Timer.TIMESTAMP.emit_time);

		timer.pushTimer();
		/* do requested dumps */
		if (options.opt_dump_grammar || options.opt_dump_states || options.opt_dump_tables)
			emit_dumps(grammar);
		timer.popTimer(Timer.TIMESTAMP.dump_time);

		timer.popTimer(Timer.TIMESTAMP.final_time);
		/* produce a summary if desired */
		if (!options.no_summary)
			emit_summary(grammar, did_output);
		
		return ErrorManager.getManager().getErrorCount() == 0 ? 0 : 1;

	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/**
	 * Print a "usage message" that described possible command line options, then
	 * exit.
	 * 
	 * @param message a specific error message to preface the usage message by.
	 */
	private void usage() {
		System.err.println();
		System.err.println("Usage: " + version.program_name + " [options] [filename]\n"
				+ "  and expects a specification file on standard input if no filename is given.\n"
				+ "  Legal options include:\n"
				+ "    -package name  specify package generated classes go in [default none]\n"
				+ "    -destdir name  specify the destination directory, to store the generated files in\n"
				+ "    -parser name   specify parser class name [default \"parser\"]\n"
				+ "    -typearg args  specify type arguments for parser class\n"
				+ "    -symbols name  specify name for symbol constant class [default \"sym\"]\n"
				+ "    -interface     put symbols in an interface, rather than a class\n"
				+ "    -nonterms      put non terminals in symbol constant class\n"
				+ "    -expect #      number of conflicts expected/allowed [default 0]\n"
				+ "    -compact_red   compact tables by defaulting to most frequent reduce\n"
				+ "    -newpositions  don't generate old style access for left and right token\n"
				+ "    -nowarn        don't warn about useless productions, etc.\n"
				+ "    -nosummary     don't print the usual summary of parse states, etc.\n"
				+ "    -nopositions   don't propagate the left and right token position values\n"
				+ "    -noscanner     don't refer to com.github.jhoenicke.javacup.runtime.Scanner\n"
				+ "    -progress      print messages to indicate progress of the system\n"
				+ "    -time          print time usage summary\n"
				+ "    -dump_grammar  produce a human readable dump of the symbols and grammar\n"
				+ "    -dump_states   produce a dump of parse state machine\n"
				+ "    -dump_tables   produce a dump of the parse tables\n"
				+ "    -dump          produce a dump of all of the above\n"
				+ "    -version       print the version information for CUP and exit\n");
	}

	/**
	 * Parse command line options and arguments to set various user-option flags and
	 * variables.
	 * 
	 * @param argv the command line arguments to be parsed.
	 */
	private int parse_args(String argv[]) {
		int len = argv.length;
		int i;

		/* parse the options */
		for (i = 0; i < len; i++) {
			String option = argv[i];
			/* try to get the various options */
			if (option.equals("-package") || option.equals("-destdir") || option.equals("-parser")
					|| option.equals("-typearg") || option.equals("-symbols") || option.equals("-expect")) {
				/* must have an arg */
				if (++i >= len || argv[i].startsWith("-") || argv[i].endsWith(".cup"))
					ErrorManager.getManager().emit_fatal(option + " must have an argument");
				else if (!options.setOption(option.substring(1), argv[i])) {
					usage();
					return 1;
				}
			} else if (argv[i].equals("-version")) {
				System.out.println(version.title_str);
				return 1;
			} else if (option.startsWith("-")) {
				if (!options.setOption(option.substring(1))) {
					usage();
					return 1;
				}
			}
			/* CSA 24-Jul-1999; suggestion by Jean Vaucher */
			else if (!argv[i].startsWith("-") && i == len - 1) {
				/* use input from file. */
				try {
					input_file = new FileInputStream(argv[i]);
				} catch (FileNotFoundException e) {
					ErrorManager.getManager().emit_fatal("Unable to open \"" + argv[i] + "\" for input");
					usage();
					return 1;
				}
			} else {
				ErrorManager.getManager().emit_fatal("Unrecognized option \"" + argv[i] + "\"");
				usage();
				return 1;
			}
		}
		return 0;
	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/*-------*/
	/* Files */
	/*-------*/

	/** Input file. This defaults to System.in. */
	private InputStream input_file = System.in;

	/**
	 * Parse the grammar specification from standard input. This produces sets of
	 * terminal, non-terminals, and productions which can be accessed via variables
	 * of the respective classes, as well as the setting of various variables
	 * (mostly in the emit class) for small user supplied items such as the code to
	 * scan with.
	 */
	private Grammar parse_grammar_spec() {
		Parser parser_obj;

		/* create a parser and parse with it */
		ComplexSymbolFactory csf = new ComplexSymbolFactory();
		parser_obj = new Parser(new Lexer(input_file, csf), csf);
		parser_obj.options = options;
		try {
			Grammar grammar;
			if (options.opt_do_debug)
				grammar = (Grammar) parser_obj.debug_parse().value;
			else
				grammar = (Grammar) parser_obj.parse().value;
			if (input_file != System.in)
				input_file.close();
			return grammar;
		} catch (Exception e) {
			ErrorManager.getManager().emit_fatal(e.getMessage());
			return null;
		}
	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/**
	 * Check for unused symbols. Unreduced productions get checked when tables are
	 * created.
	 */
	private void check_unused(Grammar grammar) {
		/* check for unused terminals */
		unused_term = 0;
		for (terminal term : grammar.terminals()) {
			/* don't issue a message for EOF */
			if (term == terminal.EOF)
				continue;

			/* or error */
			if (term == terminal.error)
				continue;

			/* is this one unused */
			if (term.use_count() == 0) {
				/* count it and warn if we are doing warnings */
				unused_term++;
				if (!options.nowarn) {
					ErrorManager.getManager()
							.emit_warning("Terminal \"" + term.name() + "\" was declared but never used");
				}
			}
		}

		unused_non_term = 0;
		/* check for unused non terminals */
		for (non_terminal nt : grammar.non_terminals()) {
			/* is this one unused */
			if (nt.use_count() == 0 || nt.productions().size() == 0) {
				/* count and warn if we are doing warnings */
				unused_non_term++;
				if (!options.nowarn) {
					String reason;
					if (nt.use_count() == 0)
						reason = "\" was declared but never used";
					else
						reason = "\" has no production";
					ErrorManager.getManager().emit_warning("Non terminal \"" + nt.name() + reason);
				}
			}
		}

	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/**
	 * Build the (internal) parser from the previously parsed specification. This
	 * includes:
	 * <ul>
	 * <li>Computing nullability of non-terminals.
	 * <li>Computing first sets of non-terminals and productions.
	 * <li>Building the viable prefix recognizer machine.
	 * <li>Filling in the (internal) parse tables.
	 * <li>Checking for unreduced productions.
	 * </ul>
	 */
	private void build_parser(Grammar grammar) {
		timer.pushTimer();
		/* compute nullability of all non terminals */
		if (options.opt_do_debug || options.print_progress)
			ErrorManager.getManager().emit_info("  Computing non-terminal nullability...");
		grammar.compute_nullability();
		timer.popTimer(Timer.TIMESTAMP.nullability_time);

		timer.pushTimer();
		/* compute first sets of all non terminals */
		if (options.opt_do_debug || options.print_progress)
			ErrorManager.getManager().emit_info("  Computing first sets...");
		grammar.compute_first_sets();
		timer.popTimer(Timer.TIMESTAMP.first_time);

		timer.pushTimer();
		/* build the LR viable prefix recognition machine */
		if (options.opt_do_debug || options.print_progress)
			ErrorManager.getManager().emit_info("  Building state machine...");
		grammar.build_machine();
		timer.popTimer(Timer.TIMESTAMP.machine_time);

		timer.pushTimer();
		/* build the LR parser action and reduce-goto tables */
		if (options.opt_do_debug || options.print_progress)
			ErrorManager.getManager().emit_info("  Filling in tables...");
		grammar.build_tables(options.opt_compact_red);
		timer.popTimer(Timer.TIMESTAMP.table_time);

		timer.pushTimer();
		/* check and warn for non-reduced productions */
		if (options.opt_do_debug || options.print_progress)
			ErrorManager.getManager().emit_info("  Checking for non-reduced productions...");
		if (!options.nowarn)
			grammar.check_tables();
		timer.popTimer(Timer.TIMESTAMP.reduce_check_time);

		/* if we have more conflicts than we expected issue a message and die */
		if (grammar.num_conflicts() > options.expect_conflicts) {
			ErrorManager.getManager()
					.emit_error("*** More conflicts encountered than expected " + "-- parser generation aborted");
			// indicate the problem.
			// we'll die on return, after clean up.
		}
	}

	private File buildFile (String filename, String extension) {
		String packageAsFileSystem = options.package_name.replace(".", "/");
		File folder = new File(new File(options.dest_dir == null ? "." : options.dest_dir), packageAsFileSystem);
		folder.mkdirs();
		File file = new File(folder, filename+"."+extension);
		return file;
	}

	private void safeDelete (File file) {
		try {
			if (file.exists())
				file.delete();
		} catch (Exception e) {
			ErrorManager.getManager().emit_fatal("Can't delete \"" + file.getAbsolutePath() + "\"");
		}
	}

	private PrintWriter safeOpen (File file) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file), 4096));
		} catch (Exception e) {
			ErrorManager.getManager().emit_fatal("Can't open \"" + file.getAbsolutePath() + "\"");
		}
		return writer;
	}
	
	private void safeClose (PrintWriter writer, File file) {
		try {
			if (writer != null)
				writer.close();
		} catch (Exception e) {
			ErrorManager.getManager().emit_fatal("Can't close \"" + file.getAbsolutePath() + "\"");
		}
	}
	
	private void emit_erase(Grammar grammar) {
		safeDelete (buildFile(options.symbol_const_class_name, "java"));
		safeDelete (buildFile(options.parser_class_name, "java"));
		safeDelete (buildFile(options.parser_class_name, "dump"));
	}
	
	private void emit_symbols(Grammar grammar) {
		File file = buildFile(options.symbol_const_class_name, "java");
		PrintWriter symbol_class_file = safeOpen (file);
		if (symbol_class_file != null) {
			ErrorManager.getManager().emit_info("Generate Symbol file : "+file.getPath());
			emit emit = new emit (options, timer);
			emit.symbols(symbol_class_file, grammar);
		}
		safeClose(symbol_class_file, file);
	}

	private void emit_parser(Grammar grammar) {
		File file = buildFile(options.parser_class_name, "java");
		PrintWriter parser_class_file = safeOpen (file);
		if (parser_class_file != null) {
			ErrorManager.getManager().emit_info("Generate Parser file : "+file.getPath());
			emit emit = new emit (options, timer);
			emit.parser(parser_class_file, grammar);
		}
		safeClose(parser_class_file, file);
	}

	private void emit_dumps(Grammar grammar) {
		File file = buildFile(options.parser_class_name, "dump");
		PrintWriter dump_file = safeOpen (file);
		if (dump_file != null) {
			ErrorManager.getManager().emit_info("Generate Dump file : "+file.getPath());
			emit emit = new emit (options, timer);
			emit.dumps(dump_file, grammar);
		}
		safeClose(dump_file, file);
	}


	/**
	 * Helper routine to optionally return a plural or non-plural ending.
	 * 
	 * @param val the numerical value determining plurality.
	 */
	private String plural(int val) {
		if (val == 1)
			return "";
		else
			return "s";
	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/**
	 * Emit a long summary message to standard error (System.err) which summarizes
	 * what was found in the specification, how many states were produced, how many
	 * conflicts were found, etc. A detailed timing summary is also produced if it
	 * was requested by the user.
	 * 
	 * @param output_produced did the system get far enough to generate code.
	 */
	private void emit_summary(Grammar grammar, boolean output_produced) {

		System.err.println("------- " + version.title_str + " Parser Generation Summary -------");

		/* error and warning count */
		System.err.println("  " + ErrorManager.getManager().getErrorCount() + " error"
				+ plural(ErrorManager.getManager().getErrorCount()) + " and "
				+ ErrorManager.getManager().getWarningCount() + " warning"
				+ plural(ErrorManager.getManager().getWarningCount()));

		/* basic stats */
		System.err.print("  " + grammar.num_terminals() + " terminal" + plural(grammar.num_terminals()) + ", ");
		System.err
				.print(grammar.num_non_terminals() + " non-terminal" + plural(grammar.num_non_terminals()) + ", and ");
		System.err
				.println(grammar.num_productions() + " production" + plural(grammar.num_productions()) + " declared, ");
		System.err.print("  producing " + grammar.lalr_states().size() + " unique parse states,");
		System.err.println(" " + grammar.num_actions() + " unique action" + plural(grammar.num_actions()) + ". ");

		/* unused symbols */
		System.err.println("  " + unused_term + " terminal" + plural(unused_term) + " declared but not used.");
		System.err.println(
				"  " + unused_non_term + " non-terminal" + plural(unused_non_term) + " declared but not used.");

		/* productions that didn't reduce */
		System.err.println("  " + not_reduced + " production" + plural(not_reduced) + " never reduced.");

		/* conflicts */
		System.err.println("  " + grammar.num_conflicts() + " conflict" + plural(grammar.num_conflicts()) + " detected"
				+ " (" + options.expect_conflicts + " expected).");

		/* code location */
		if (output_produced)
			System.err.println("  Code written to \"" + options.parser_class_name + ".java\", and \""
					+ options.symbol_const_class_name + ".java\".");
		else
			System.err.println("  No code produced.");

		if (options.opt_show_timing)
			show_times();

		System.err.println("---------------------------------------------------- (" + version.version_str + ")");
	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/** Produce the optional timing summary as part of an overall summary. */
	private void show_times() {
		long total_time = timer.getTime(Timer.TIMESTAMP.final_time);

		System.err.println("----------------------------------------------------");
		System.err.println("  Timing Summary");
		System.err.println(timestr("    Total time       ", timer.getTime(Timer.TIMESTAMP.final_time), total_time));
		System.err.println(timestr("      Startup        ", timer.getTime(Timer.TIMESTAMP.preliminary_time), total_time));
		System.err.println(timestr("      Parse          ", timer.getTime(Timer.TIMESTAMP.parse_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.check_time))
			System.err.println(timestr("      Checking       ", timer.getTime(Timer.TIMESTAMP.check_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.build_time))
			System.err.println(timestr("      Parser Build   ", timer.getTime(Timer.TIMESTAMP.build_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.nullability_time))
			System.err.println(timestr("        Nullability  ", timer.getTime(Timer.TIMESTAMP.nullability_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.first_time))
			System.err.println(timestr("        First sets   ", timer.getTime(Timer.TIMESTAMP.first_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.machine_time))
			System.err.println(timestr("        State build  ", timer.getTime(Timer.TIMESTAMP.machine_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.table_time))
			System.err.println(timestr("        Table build  ", timer.getTime(Timer.TIMESTAMP.table_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.reduce_check_time))
			System.err.println(timestr("        Checking     ", timer.getTime(Timer.TIMESTAMP.reduce_check_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.emit_time))
			System.err.println(timestr("      Code Output    ", timer.getTime(Timer.TIMESTAMP.emit_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.symbols_time))
			System.err.println(timestr("        Symbols      ", timer.getTime(Timer.TIMESTAMP.symbols_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.parser_time))
			System.err.println(timestr("        Parser class ", timer.getTime(Timer.TIMESTAMP.parser_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.action_code_time))
			System.err.println(timestr("          Actions    ", timer.getTime(Timer.TIMESTAMP.action_code_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.production_table_time))
			System.err.println(timestr("          Prod table ", timer.getTime(Timer.TIMESTAMP.production_table_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.action_code_time))
			System.err.println(timestr("          Action tab ", timer.getTime(Timer.TIMESTAMP.action_code_time), total_time));
		if (timer.hasTime(Timer.TIMESTAMP.goto_table_time))
			System.err.println(timestr("          Reduce tab ", timer.getTime(Timer.TIMESTAMP.goto_table_time), total_time));

		System.err.println(timestr("      Dump Output    ", timer.getTime(Timer.TIMESTAMP.dump_time), total_time));
	}

	/* . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . */

	/**
	 * Helper routine to format a decimal based display of seconds and percentage of
	 * total time given counts of milliseconds. Note: this is broken for use with
	 * some instances of negative time (since we don't use any negative time here,
	 * we let if be for now).
	 * 
	 * @param prefix     a prefix to prepend to the formatted value.
	 * @param time_val   the value being formatted (in ms).
	 * @param total_time total time percentages are calculated against (in ms).
	 */
	private String timestr(String prefix, long time_val, long total_time) {
		long percent10;

		/* pull out seconds and ms */
		/* construct a pad to blank fill seconds out to 4 places */
		String sec = "   " + (time_val / 1000);
		String ms = "00" + (time_val % 1000);

		/* calculate 10 times the percentage of total */
		percent10 = (time_val * 1000) / total_time;

		StringBuilder tmp = new StringBuilder();
		tmp.append(prefix);
		tmp.append(sec.substring(sec.length() - 4));
		tmp.append(".");
		tmp.append(ms.substring(ms.length() - 3));
		tmp.append("sec");
		tmp.append(" (");
		tmp.append(percent10 / 10);
		tmp.append(".");
		tmp.append(percent10 % 10);
		tmp.append("%)");
		return tmp.toString();
	}

	/*-----------------------------------------------------------*/

	/*-----------------------------------------------------------*/
	/*--- Main Program ------------------------------------------*/
	/*-----------------------------------------------------------*/

	/**
	 * The main driver for the system.
	 * 
	 * @param argv an array of strings containing command line arguments.
	 */
	public static void main(String argv[]) throws Exception {
		Main main = new Main();

		/* process user options and arguments */
		int code;
		code = main.parse_args(argv);
		if (code == 0) {
			/* run parser, analyser and generater*/
			code = main.run();
		}
		if (code == 0) return;
		if (! main.options.opt_no_exit) {
			System.exit(code);
		}
	}

}
