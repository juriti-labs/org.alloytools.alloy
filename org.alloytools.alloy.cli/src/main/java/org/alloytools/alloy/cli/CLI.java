package org.alloytools.alloy.cli;

import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.alloytools.alloy.dto.CommandDTO;
import org.alloytools.alloy.dto.ExecutionDTO;
import org.alloytools.alloy.dto.FieldDTO;
import org.alloytools.alloy.dto.InstanceDTO;
import org.alloytools.alloy.dto.SigDefDTO;
import org.alloytools.alloy.dto.SolutionDTO;
import org.alloytools.alloy.dto.TuplesDTO;
import org.alloytools.alloy.infrastructure.api.AlloyMain;
import org.alloytools.util.table.Table;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.env.Env;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import edu.mit.csail.sdg.alloy4.TableView;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.sim.SimTupleset;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.A4SolutionWriter;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;
import kodkod.engine.satlab.SATFactory;

/**
 * 
 * @author aqute
 *
 */
@AlloyMain
public class CLI extends Env {
	final A4Options options = new A4Options();

	public enum OutputType {
		none, text, table, json, xml, yaml, dot, rdf;
	}

	InputStream stdin = new FilterInputStream(System.in) {
		@Override
		public void close() throws IOException {
		}
	};
	PrintStream stdout = new PrintStream(new FilterOutputStream(System.out)) {
		public void close() {
			System.out.flush();
		};
	};
	PrintStream stderr = new PrintStream(new FilterOutputStream(System.err)) {
		public void close() {
			System.err.flush();
		};
	};

	/**
	 * Show the list of solvers
	 */

	/**
	 * Run all 'run' & asserts
	 */

	@Arguments(arg = "path")
	@Description("Execute an Alloy program. This will create a directory with the name of the source "
			+ "file minus the extension. You will find the solutions in this directory. "
			+ "This directory will also contain a receipt.json file that contains the solutions.")
	interface ExecOptions extends Options {
		@Description("The command to run. If no command is specified, the default command will run. The command may specify wildcards to run multiple commands. If the command is an integer, it will run the command with that index.")
		String command();

		@Description("Specify the output type: none, text, table, json, xml, yaml, dot (GraphViz), rdf (Turtle/RDF)")
		OutputType type(OutputType deflt);

		@Description("Specify where the output should go. Default is the a directory with the stem of "
				+ "the name of the source file. If a name is specified, this will become a directory "
				+ "with the different outputs ordered as separate files. A file is generated with "
				+ "the proper extension for the output type (-t/--type) and the name is the name of "
				+ "the command & the solution index. If the directory contains files, specify -f/--force to "
				+ "delete it. If the value is -, all calculated solutions or transformed files "
				+ "are sent to the console.")
		String output();

		@Description("Specify that the output directory is removed and recreated if it contains any files")
		boolean force();

		@Description("If set, the solution will include only those models in which no arithmetic overflows occurred")
		boolean nooverflow();

		@Description("Number of allowed recursion unrolls. Default is -1 (no unrolling)")
		int unrolls(int n);

		@Description("Depth for skolem analysis, default is 0")
		int depth(int n);

		@Description("Symmetry breaking removes instances that are symmetric to other instances. The parameter indicates the amount of effort Alloy can spend. The default is 20.")
		int ymmetry(int n);

		@Description("Set the solver to use. You can get a list of solver names with the 'solvers' command. The default solver is SAT4J.")
		String solver(String solver);

		@Description("Be quiet with progress information")
		boolean quiet();

		@Description("After resolving each command, start an evaluator")
		boolean evaluator();

		@Description("Find multiple solutions, up to this number. Use 0 for as many as can be found. The default is 1, only the first solution.")
		int repeat(int deflt);

		@Description("Verbose output level for LLM-friendly diagnostics: 0=quiet (default), 1=basic, 2=detailed, 3=debug")
		int verbose(int deflt);

	}

	/**
	 * Execute a Alloy program
	 * 
	 * @param options the options to use
	 */
	@Description("Execute an Alloy program. This will create a directory with the name of the source "
			+ "file minus the extension. You will find the solutions in this directory. "
			+ "This directory will also contain a receipt.json file that contains the solutions.")
	public void _exec(ExecOptions options) throws Exception {
		boolean quiet = options.quiet();
		SimpleReporter rep = new SimpleReporter(this);
		A4Options opt = this.options.dup();
		opt.noOverflow = options.nooverflow();
		opt.unrolls = options.unrolls(opt.unrolls);
		opt.skolemDepth = options.depth(opt.skolemDepth);
		opt.symmetry = options.depth(opt.symmetry);

		Optional<SATFactory> solver = SATFactory.find(options.solver("sat4j"));
		if (!solver.isPresent()) {
			error("No such solver %s: %s", options.solver(null),
					SATFactory.getSolvers().stream().map(sf -> sf.id()).collect(Collectors.joining(", ")));
			return;
		}
		opt.solver = solver.get();

		String filename = options._arguments().remove(0);
		File file = IO.getFile(filename);
		if (!file.canRead()) {
			error("Cannot read file %s", file);
			return;
		}
		if (file.isDirectory()) {
			error("%s must be a file, not a directory", file);
			return;
		}


		Map<String, String> cache = new HashMap<>();
		CompModule world = CompUtil.parseEverything_fromFile(rep, cache, filename);

		List<Command> commands = world.getAllCommands();

		Predicate<Command> run = getCommandPredicate(options, commands);

		File outdir = output(options.output(), getStem(file));

		if (outdir != null) {
			if (outdir.isFile()) {
				error("There is a file with the name of the output directory %s", outdir);
				return;
			}
			IO.mkdirs(outdir);
			if (!outdir.isDirectory()) {
				error("Cannot create output directory %s", outdir);
				return;
			}

			boolean filesExist = outdir.listFiles().length > 0;
			if (filesExist) {
				if (options.force()) {
					IO.delete(outdir);
					IO.mkdirs(outdir);
				} else {
					error("The output directory %s contains files. Delete them or use the -f option", outdir);
					return;
				}
			}
		}
		OutputTrace trace = new OutputTrace(quiet || outdir == null ? null : stderr);
		int n = 0;

		int repeat = options.repeat(1);
		if (repeat == 0) {
			repeat = Integer.MAX_VALUE;
		}

		ExecutionDTO receipt = new ExecutionDTO();
		receipt.solver = opt.solver.id();
		receipt.noOverflow = opt.noOverflow;
		receipt.symmetry = opt.symmetry;
		receipt.unrolls = opt.unrolls;
		receipt.coreGranularity = opt.coreGranularity;
		receipt.coreMinimization = opt.coreMinimization;
		receipt.decompose_mode = opt.decompose_mode;
		receipt.inferPartialInstance = opt.inferPartialInstance;
		receipt.skolemDepth = opt.skolemDepth;
		receipt.timestamp = System.currentTimeMillis();
		receipt.repeat = repeat;

		for (Sig sig : world.getAllReachableSigs()) {
			if (sig.isPrivate != null)
				continue;
			receipt.sigs.put(Util.scope(sig.label), Util.toDTO(sig));
		}

		String source = IO.collect(file);

		for (Command c : commands) {

			if (!run.test(c)) {
				trace("ignore command %s", c);
				continue;
			}

			CommandDTO commandReceipt = Util.toDTO(c, source);
			receipt.commands.put(c.label, commandReceipt);

			int index = 0;
			trace.format("%02d. %-5s %-20s ", n, c.check ? "check" : "run", c.label);
			String cname = toCName(c);

			try {
				A4Solution solution = TranslateAlloyToKodkod.execute_commandFromBook(rep, world.getAllReachableSigs(),
						c, opt);

				if (!solution.satisfiable()) {
					if (rep.output != null) {
						trace.format("  %s", cname + "." + ext(rep.output));
						String path = showTransformerFile(rep.output, outdir, cname);
						commandReceipt.transformPath = path;
					} else {
						trace.format("    0       UNSAT", options.repeat(1));
						if (c.expects == 1) {
							trace.format(" expects=%s", c.expects);
							error("'%s' was not satisfied against expectation",c);
						}
					}
				} else {

					int back = 0;
					do {
						solution.setModule(world);
						trace.back(back).format("%5d", index);
						SolutionDTO solutionDTO = solution.toDTO();
						commandReceipt.solution.add(solutionDTO);
						generate(world, solution, options.type(OutputType.table), outdir, cname, index, solutionDTO);
						index++;
						back = 5;
					} while (index < repeat && solution.isIncremental() && (solution = solution.next()).satisfiable());

					trace.back(back).format("%5d/%-5s SAT", index, options.repeat(1), c.expects);
					if (c.expects == 0) {
						trace.format(" expects=%s", c.expects);
						error("'%s' was satisfied against expectation",c);
					}
					trace.format("\n");
					if (options.evaluator()) {
						evaluator(world, solution);
					}
				}
				n++;
				if (outdir != null)
					try {
						File receiptFile = IO.getFile(outdir, "receipt.json");
						new JSONCodec().enc().to(receiptFile).put(receipt).close();
					} catch (Exception e) {
						error("failed to save receipt: %s", e.getMessage());
					}
			} catch (Exception e) {
				exception(e,"command %s could not be solved: %s", cname, Exceptions.unrollCause(e));
				trace.format("!%s", Exceptions.unrollCause(e));
			}
			trace.format("%n");
		}
	}

	private Predicate<Command> getCommandPredicate(ExecOptions options, List<Command> commands) {
		Predicate<Command> run;
		String cmd = options.command();
		if (cmd == null) {
			run = c -> true;
		} else if (cmd.matches("[0-9]+")) {
			int index = Integer.parseInt(cmd);
			if (index >= commands.size()) {
				error("command index %s is more than available commands %s", index, commands);
			}
			run = c -> commands.indexOf(c) == index;
		} else {
			Glob g = new Glob(cmd);
			run = c -> g.matches(c.label);
		}
		return run;
	}

	private String showTransformerFile(File source, File outdir, String cname) throws IOException {
		try {
			if (outdir == null) {
				IO.copy(source, stdout);
				return null;
			} else {
				String child = cname + "." + ext(source);
				File out = new File(outdir, child);
				IO.rename(source, out);
				return child;
			}
		} finally {
			IO.delete(source);
		}
	}

	private String ext(File file) {
		String parts[] = Strings.extension(file.getName());
		if (parts == null)
			return ".unknown";
		else
			return parts[1];
	}

	private String toCName(Command c) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.label);
		return sb.toString();
	}

	private String getStem(File file) {
		String parts[] = Strings.extension(file.getName());
		if (parts == null) {
			return file.getName() + "-output";
		} else
			return parts[0];
	}

	private void evaluator(CompModule world, A4Solution sol) throws Exception {
		if (sol.satisfiable()) {
			stdout.println("Evaluator for latest command");
			stdout.flush();
			Evaluator e = new Evaluator(world, sol, stdin, stdout);
			String lastCommand = e.loop();
			if (lastCommand == null || lastCommand.equals("/exit"))
				return;
		}
	}

	@Arguments(arg = "path")
	@Description("List the commands in an alloy file")
	interface CommandsOptions extends Options {

	}

	/**
	 * Execute a Alloy program
	 * 
	 * @param options the options to use
	 */
	@Description("Show all commands in an Alloy program")
	public void _commands(CommandsOptions options) throws Exception {
		SimpleReporter rep = new SimpleReporter(this);
		String filename = options._arguments().remove(0);
		Map<String, String> cache = new HashMap<>();
		CompModule world = CompUtil.parseEverything_fromFile(rep, cache, filename);
		int n = 0;
		for (Command c : world.getAllCommands()) {
			stdout.printf("%-2d. %s%n", n++, c);
		}
	}

	@Arguments(arg = {})
	@Description("Output machine-readable JSON description of CLI capabilities for LLM consumption")
	interface DescribeOptions extends Options {
		@Description("Output format: json (default), yaml")
		String format(String deflt);
	}

	/**
	 * Describe CLI capabilities in machine-readable format for LLM consumption
	 */
	@Description("Output machine-readable description of CLI capabilities for LLM consumption. Useful for automated tools and LLMs to discover available commands and options.")
	public void _describe(DescribeOptions options) throws Exception {
		String format = options.format("json");
		
		if ("json".equals(format)) {
			writeDescribeJson();
		} else if ("yaml".equals(format)) {
			writeDescribeYaml();
		} else {
			error("Unknown format: %s. Use json or yaml.", format);
		}
	}

	private void writeDescribeJson() {
		stdout.println("{");
		stdout.println("  \"name\": \"alloy-cli\",");
		stdout.println("  \"description\": \"Alloy formal specification language command line interface\",");
		stdout.println("  \"version\": \"6.x\",");
		stdout.println("  \"llm_friendly\": true,");
		stdout.println("  \"commands\": {");
		stdout.println("    \"exec\": {");
		stdout.println("      \"description\": \"Execute an Alloy program and generate solutions\",");
		stdout.println("      \"arguments\": [\"path\"],");
		stdout.println("      \"options\": {");
		stdout.println("        \"type\": {");
		stdout.println("          \"short\": \"-t\",");
		stdout.println("          \"long\": \"--type\",");
		stdout.println("          \"values\": [\"none\", \"text\", \"table\", \"json\", \"xml\", \"yaml\", \"dot\", \"rdf\"],");
		stdout.println("          \"default\": \"table\",");
		stdout.println("          \"description\": \"Output format for solutions\"");
		stdout.println("        },");
		stdout.println("        \"output\": {");
		stdout.println("          \"short\": \"-o\",");
		stdout.println("          \"long\": \"--output\",");
		stdout.println("          \"description\": \"Output directory or - for stdout\"");
		stdout.println("        },");
		stdout.println("        \"command\": {");
		stdout.println("          \"short\": \"-c\",");
		stdout.println("          \"long\": \"--command\",");
		stdout.println("          \"description\": \"Command to run (name, index, or glob pattern)\"");
		stdout.println("        },");
		stdout.println("        \"solver\": {");
		stdout.println("          \"short\": \"-s\",");
		stdout.println("          \"long\": \"--solver\",");
		stdout.println("          \"default\": \"sat4j\",");
		stdout.println("          \"description\": \"SAT solver to use\"");
		stdout.println("        },");
		stdout.println("        \"repeat\": {");
		stdout.println("          \"short\": \"-r\",");
		stdout.println("          \"long\": \"--repeat\",");
		stdout.println("          \"type\": \"integer\",");
		stdout.println("          \"default\": 1,");
		stdout.println("          \"description\": \"Number of solutions to find (0 for all)\"");
		stdout.println("        },");
		stdout.println("        \"verbose\": {");
		stdout.println("          \"short\": \"-v\",");
		stdout.println("          \"long\": \"--verbose\",");
		stdout.println("          \"type\": \"integer\",");
		stdout.println("          \"values\": [0, 1, 2, 3],");
		stdout.println("          \"default\": 0,");
		stdout.println("          \"description\": \"Verbosity level for LLM diagnostics\"");
		stdout.println("        },");
		stdout.println("        \"quiet\": {");
		stdout.println("          \"short\": \"-q\",");
		stdout.println("          \"long\": \"--quiet\",");
		stdout.println("          \"type\": \"boolean\",");
		stdout.println("          \"description\": \"Suppress progress information\"");
		stdout.println("        },");
		stdout.println("        \"force\": {");
		stdout.println("          \"short\": \"-f\",");
		stdout.println("          \"long\": \"--force\",");
		stdout.println("          \"type\": \"boolean\",");
		stdout.println("          \"description\": \"Overwrite existing output directory\"");
		stdout.println("        }");
		stdout.println("      }");
		stdout.println("    },");
		stdout.println("    \"commands\": {");
		stdout.println("      \"description\": \"List commands in an Alloy program\",");
		stdout.println("      \"arguments\": [\"path\"]");
		stdout.println("    },");
		stdout.println("    \"solvers\": {");
		stdout.println("      \"description\": \"List available SAT solvers\"");
		stdout.println("    },");
		stdout.println("    \"describe\": {");
		stdout.println("      \"description\": \"Output this CLI description in machine-readable format\",");
		stdout.println("      \"options\": {");
		stdout.println("        \"format\": {");
		stdout.println("          \"short\": \"-f\",");
		stdout.println("          \"long\": \"--format\",");
		stdout.println("          \"values\": [\"json\", \"yaml\"],");
		stdout.println("          \"default\": \"json\",");
		stdout.println("          \"description\": \"Output format\"");
		stdout.println("        }");
		stdout.println("      }");
		stdout.println("    }");
		stdout.println("  },");
		stdout.println("  \"output_formats\": {");
		stdout.println("    \"json\": {");
		stdout.println("      \"extension\": \".json\",");
		stdout.println("      \"description\": \"JSON format suitable for programmatic parsing\"");
		stdout.println("    },");
		stdout.println("    \"yaml\": {");
		stdout.println("      \"extension\": \".yaml\",");
		stdout.println("      \"description\": \"YAML format for human-readable structured data\"");
		stdout.println("    },");
		stdout.println("    \"xml\": {");
		stdout.println("      \"extension\": \".xml\",");
		stdout.println("      \"description\": \"Alloy XML format compatible with existing tools\"");
		stdout.println("    },");
		stdout.println("    \"dot\": {");
		stdout.println("      \"extension\": \".dot\",");
		stdout.println("      \"description\": \"GraphViz DOT format for visualization\"");
		stdout.println("    },");
		stdout.println("    \"rdf\": {");
		stdout.println("      \"extension\": \".ttl\",");
		stdout.println("      \"description\": \"RDF Turtle format for knowledge graph integration\"");
		stdout.println("    },");
		stdout.println("    \"table\": {");
		stdout.println("      \"extension\": \".md\",");
		stdout.println("      \"description\": \"Markdown table format\"");
		stdout.println("    },");
		stdout.println("    \"text\": {");
		stdout.println("      \"extension\": \".txt\",");
		stdout.println("      \"description\": \"Plain text format\"");
		stdout.println("    }");
		stdout.println("  },");
		stdout.println("  \"examples\": [");
		stdout.println("    {");
		stdout.println("      \"description\": \"Execute a model and output JSON to stdout\",");
		stdout.println("      \"command\": \"alloy exec -t json -o - model.als\"");
		stdout.println("    },");
		stdout.println("    {");
		stdout.println("      \"description\": \"Generate RDF knowledge graph from a model\",");
		stdout.println("      \"command\": \"alloy exec -t rdf -o output/ model.als\"");
		stdout.println("    },");
		stdout.println("    {");
		stdout.println("      \"description\": \"Find multiple solutions with verbose output\",");
		stdout.println("      \"command\": \"alloy exec -t yaml -r 5 --verbose 2 model.als\"");
		stdout.println("    }");
		stdout.println("  ]");
		stdout.println("}");
	}

	private void writeDescribeYaml() {
		stdout.println("# Alloy CLI Description - YAML Format");
		stdout.println("# For LLM and automated tool consumption");
		stdout.println();
		stdout.println("name: alloy-cli");
		stdout.println("description: Alloy formal specification language command line interface");
		stdout.println("version: \"6.x\"");
		stdout.println("llm_friendly: true");
		stdout.println();
		stdout.println("commands:");
		stdout.println("  exec:");
		stdout.println("    description: Execute an Alloy program and generate solutions");
		stdout.println("    arguments: [path]");
		stdout.println("    options:");
		stdout.println("      type:");
		stdout.println("        short: -t");
		stdout.println("        long: --type");
		stdout.println("        values: [none, text, table, json, xml, yaml, dot, rdf]");
		stdout.println("        default: table");
		stdout.println("        description: Output format for solutions");
		stdout.println("      output:");
		stdout.println("        short: -o");
		stdout.println("        long: --output");
		stdout.println("        description: Output directory or - for stdout");
		stdout.println("      command:");
		stdout.println("        short: -c");
		stdout.println("        long: --command");
		stdout.println("        description: Command to run (name, index, or glob pattern)");
		stdout.println("      solver:");
		stdout.println("        short: -s");
		stdout.println("        long: --solver");
		stdout.println("        default: sat4j");
		stdout.println("        description: SAT solver to use");
		stdout.println("      repeat:");
		stdout.println("        short: -r");
		stdout.println("        long: --repeat");
		stdout.println("        type: integer");
		stdout.println("        default: 1");
		stdout.println("        description: Number of solutions to find (0 for all)");
		stdout.println("      verbose:");
		stdout.println("        short: -v");
		stdout.println("        long: --verbose");
		stdout.println("        type: integer");
		stdout.println("        values: [0, 1, 2, 3]");
		stdout.println("        default: 0");
		stdout.println("        description: Verbosity level for LLM diagnostics");
		stdout.println("      quiet:");
		stdout.println("        short: -q");
		stdout.println("        long: --quiet");
		stdout.println("        type: boolean");
		stdout.println("        description: Suppress progress information");
		stdout.println("      force:");
		stdout.println("        short: -f");
		stdout.println("        long: --force");
		stdout.println("        type: boolean");
		stdout.println("        description: Overwrite existing output directory");
		stdout.println();
		stdout.println("  commands:");
		stdout.println("    description: List commands in an Alloy program");
		stdout.println("    arguments: [path]");
		stdout.println();
		stdout.println("  solvers:");
		stdout.println("    description: List available SAT solvers");
		stdout.println();
		stdout.println("  describe:");
		stdout.println("    description: Output this CLI description in machine-readable format");
		stdout.println("    options:");
		stdout.println("      format:");
		stdout.println("        short: -f");
		stdout.println("        long: --format");
		stdout.println("        values: [json, yaml]");
		stdout.println("        default: json");
		stdout.println("        description: Output format");
		stdout.println();
		stdout.println("output_formats:");
		stdout.println("  json:");
		stdout.println("    extension: .json");
		stdout.println("    description: JSON format suitable for programmatic parsing");
		stdout.println("  yaml:");
		stdout.println("    extension: .yaml");
		stdout.println("    description: YAML format for human-readable structured data");
		stdout.println("  xml:");
		stdout.println("    extension: .xml");
		stdout.println("    description: Alloy XML format compatible with existing tools");
		stdout.println("  dot:");
		stdout.println("    extension: .dot");
		stdout.println("    description: GraphViz DOT format for visualization");
		stdout.println("  rdf:");
		stdout.println("    extension: .ttl");
		stdout.println("    description: RDF Turtle format for knowledge graph integration");
		stdout.println("  table:");
		stdout.println("    extension: .md");
		stdout.println("    description: Markdown table format");
		stdout.println("  text:");
		stdout.println("    extension: .txt");
		stdout.println("    description: Plain text format");
		stdout.println();
		stdout.println("examples:");
		stdout.println("  - description: Execute a model and output JSON to stdout");
		stdout.println("    command: alloy exec -t json -o - model.als");
		stdout.println("  - description: Generate RDF knowledge graph from a model");
		stdout.println("    command: alloy exec -t rdf -o output/ model.als");
		stdout.println("  - description: Find multiple solutions with verbose output");
		stdout.println("    command: alloy exec -t yaml -r 5 --verbose 2 model.als");
	}

	final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'-'yyyyMMdd'T'HH-mm-ss")
			.withZone(ZoneId.systemDefault());

	private File output(String output, String stem) throws IOException {
		if (output == null)
			output = stem;
		else if (output.equals("-")) {
			return null;
		} else if (output.equals("+")) {
			output = stem + "+";
		}

		if (output.endsWith("+")) {
			Instant now = Instant.now();
			String formattedInstant = formatter.format(now);
			output = output.replaceAll("\\+$", formattedInstant);
		}

		File dir = IO.getFile(output);
		IO.mkdirs(dir);
		return dir;

	}

	private File generate(CompModule world, A4Solution solution, OutputType type, File outdir, String cname, int index,
			SolutionDTO dto) throws Exception {
		switch (type) {
		default:
		case none:
			return null;

		case text: {
			File path = getPath(outdir, cname, ".txt", index);
			try (PrintWriter pw = getPrintWriter(path)) {
				pw.println(solution.toString());
			}
			return path;
		}

		case table: {
			File path = getPath(outdir, cname, ".md", index);
			try (PrintWriter pw = getPrintWriter(path)) {
				pw.printf("%-40s %s%n", "Command", cname);
				pw.printf("%-40s %s%n", "Solution index", index);
				int loopstate = -1;
				int tracelength = 1;
				if (solution.isTemporal()) {
					tracelength = solution.getTraceLength();
					loopstate = solution.getLoopState();
					pw.printf("%-40s %s%n", "Trace length", tracelength);
					pw.printf("%-40s %s%n", "Loop state", loopstate);
				}

				for (int trace = 0; trace < tracelength; trace++) {
					pw.println();
					Table t = solution.toTable(trace);
					if (trace == loopstate) {
						Table withLoopstate = new Table(1, 2, 0);
						withLoopstate.set(0, 0, t);
						withLoopstate.set(0, 1, "<-");
						t = withLoopstate;
					}
					if (solution.isTemporal()) {
						pw.printf("%-40s %s%n", "State index", trace);
						if (trace == loopstate) {
							pw.printf("%-40s %s%n", "Loop back", "true");
						}
					}
					pw.println(t);

					List<ExprVar> skolems = solution.getAllSkolems();
					if (!skolems.isEmpty()) {
						Table skolemsTable = new Table(skolems.size() + 1, 2, 1);
						skolemsTable.set(0, 0, "skolem");
						skolemsTable.set(0, 1, "value");
						for (int skolem = 0; skolem < skolems.size(); skolem++) {
							ExprVar var = skolems.get(skolem);
							Object eval = solution.eval(var, trace);
							if (eval instanceof SimTupleset) {
								Table tt = TableView.toTable((SimTupleset) eval);
								skolemsTable.set(skolem + 1, 1, tt);
							} else
								skolemsTable.set(skolem + 1, 1, eval);
							skolemsTable.set(skolem + 1, 0, var.label);
						}
						pw.println(skolemsTable);
					}
				}
			}
			return path;
		}

		case json: {
			File path = getPath(outdir, cname, ".json", index);
			JSONCodec codec = new JSONCodec();
			codec.enc().writeDefaults().indent("  ").to(getPrintWriter(path)).put(dto);
			return path;
		}
		case xml:
			File path = getPath(outdir, cname, ".xml", index);
			try (PrintWriter pw = getPrintWriter(path)) {
				A4SolutionWriter.writeInstance(null, solution, pw, Collections.emptyList(), Collections.emptyMap());
			}
			return path;

		case yaml: {
			File yamlPath = getPath(outdir, cname, ".yaml", index);
			try (PrintWriter pw = getPrintWriter(yamlPath)) {
				writeYaml(pw, dto, cname, index, solution);
			}
			return yamlPath;
		}

		case dot: {
			File dotPath = getPath(outdir, cname, ".dot", index);
			try (PrintWriter pw = getPrintWriter(dotPath)) {
				writeDot(pw, dto, cname, index, solution);
			}
			return dotPath;
		}

		case rdf: {
			File rdfPath = getPath(outdir, cname, ".ttl", index);
			try (PrintWriter pw = getPrintWriter(rdfPath)) {
				writeRdf(pw, dto, cname, index, solution);
			}
			return rdfPath;
		}
		}
	}

	/**
	 * Write output in YAML format for LLM-friendly consumption
	 */
	private void writeYaml(PrintWriter pw, SolutionDTO dto, String cname, int index, A4Solution solution) {
		pw.println("# Alloy Solution Output - YAML Format");
		pw.println("# Generated for LLM consumption");
		pw.println();
		pw.printf("command: \"%s\"%n", OutputUtils.escapeYaml(cname));
		pw.printf("solution_index: %d%n", index);
		pw.printf("duration_ms: %d%n", dto.duration);
		pw.printf("incremental: %s%n", dto.incremental);
		pw.printf("loopstate: %d%n", dto.loopstate);
		pw.printf("utc_time: %d%n", dto.utctime);
		pw.printf("local_time: \"%s\"%n", dto.localtime);
		pw.printf("timezone: \"%s\"%n", dto.timezone);
		pw.println();

		pw.println("instances:");
		for (InstanceDTO instance : dto.instances) {
			pw.printf("  - state: %d%n", instance.state);
			pw.println("    values:");
			for (Map.Entry<String, Map<String, String[][]>> sigEntry : instance.values.entrySet()) {
				pw.printf("      %s:%n", OutputUtils.escapeYaml(sigEntry.getKey()));
				for (Map.Entry<String, String[][]> fieldEntry : sigEntry.getValue().entrySet()) {
					pw.printf("        %s:%n", OutputUtils.escapeYaml(fieldEntry.getKey()));
					String[][] tuples = fieldEntry.getValue();
					if (tuples != null) {
						for (String[] tuple : tuples) {
							pw.print("          - [");
							for (int i = 0; i < tuple.length; i++) {
								if (i > 0) pw.print(", ");
								pw.printf("\"%s\"", OutputUtils.escapeYaml(tuple[i]));
							}
							pw.println("]");
						}
					}
				}
			}
			if (!instance.skolems.isEmpty()) {
				pw.println("    skolems:");
				for (Map.Entry<String, TuplesDTO> skolem : instance.skolems.entrySet()) {
					pw.printf("      %s:%n", OutputUtils.escapeYaml(skolem.getKey()));
					pw.printf("        arity: %d%n", skolem.getValue().arity);
					pw.println("        data:");
					if (skolem.getValue().data != null) {
						for (String[] tuple : skolem.getValue().data) {
							pw.print("          - [");
							for (int i = 0; i < tuple.length; i++) {
								if (i > 0) pw.print(", ");
								pw.printf("\"%s\"", OutputUtils.escapeYaml(tuple[i]));
							}
							pw.println("]");
						}
					}
				}
			}
		}

		if (!dto.sigs.isEmpty()) {
			pw.println();
			pw.println("signatures:");
			for (Map.Entry<String, SigDefDTO> sigEntry : dto.sigs.entrySet()) {
				SigDefDTO sig = sigEntry.getValue();
				pw.printf("  %s:%n", OutputUtils.escapeYaml(sigEntry.getKey()));
				pw.printf("    name: \"%s\"%n", OutputUtils.escapeYaml(sig.name));
				if (sig.cardinality != null) {
					pw.printf("    cardinality: %s%n", sig.cardinality);
				}
				pw.printf("    is_enum: %s%n", sig.isEnum);
				pw.printf("    meta: %s%n", sig.meta);
				pw.printf("    builtin: %s%n", sig.builtin);
				if (sig.type != null) {
					pw.printf("    type: \"%s\"%n", OutputUtils.escapeYaml(sig.type));
				}
				if (!sig.fields.isEmpty()) {
					pw.println("    fields:");
					for (Map.Entry<String, FieldDTO> fieldEntry : sig.fields.entrySet()) {
						pw.printf("      %s:%n", OutputUtils.escapeYaml(fieldEntry.getKey()));
						FieldDTO field = fieldEntry.getValue();
						if (field.name != null) {
							pw.printf("        name: \"%s\"%n", OutputUtils.escapeYaml(field.name));
						}
					}
				}
			}
		}
	}

	/**
	 * Write output in DOT (GraphViz) format for visualization
	 */
	private void writeDot(PrintWriter pw, SolutionDTO dto, String cname, int index, A4Solution solution) {
		pw.println("// Alloy Solution Output - DOT/GraphViz Format");
		pw.printf("// Command: %s, Solution: %d%n", cname, index);
		pw.println();
		pw.printf("digraph \"%s_solution_%d\" {%n", OutputUtils.sanitizeId(cname), index);
		pw.println("    rankdir=LR;");
		pw.println("    node [shape=box, style=filled, fillcolor=lightblue]");
		pw.println("    edge [fontsize=10];");
		pw.println();

		// Collect all atoms from all instances
		java.util.Set<String> allAtoms = new java.util.TreeSet<>();
		java.util.List<String[]> allEdges = new java.util.ArrayList<>();

		for (InstanceDTO instance : dto.instances) {
			for (Map.Entry<String, Map<String, String[][]>> sigEntry : instance.values.entrySet()) {
				String sigName = sigEntry.getKey();
				for (Map.Entry<String, String[][]> fieldEntry : sigEntry.getValue().entrySet()) {
					String[][] tuples = fieldEntry.getValue();
					if (tuples != null) {
						for (String[] tuple : tuples) {
							if (tuple.length == 1) {
								// Unary relation - this is an atom
								allAtoms.add(tuple[0]);
							} else if (tuple.length >= 2) {
								// Binary or higher relation - create edges
								allAtoms.add(tuple[0]);
								allAtoms.add(tuple[tuple.length - 1]);
								allEdges.add(new String[]{tuple[0], tuple[tuple.length - 1], fieldEntry.getKey()});
							}
						}
					}
				}
			}
		}

		// Write nodes
		pw.println("    // Atoms");
		for (String atom : allAtoms) {
			pw.printf("    \"%s\" [label=\"%s\"];%n", OutputUtils.sanitizeId(atom), atom);
		}
		pw.println();

		// Write edges
		pw.println("    // Relations");
		for (String[] edge : allEdges) {
			pw.printf("    \"%s\" -> \"%s\" [label=\"%s\"];%n", 
					OutputUtils.sanitizeId(edge[0]), OutputUtils.sanitizeId(edge[1]), edge[2]);
		}

		pw.println("}");
	}

	/**
	 * Write output in RDF/Turtle format for knowledge graph generation
	 */
	private void writeRdf(PrintWriter pw, SolutionDTO dto, String cname, int index, A4Solution solution) {
		// RDF prefixes
		pw.println("@prefix alloy: <http://alloytools.org/ontology#> .");
		pw.println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		pw.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
		pw.println("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .");
		pw.println("@prefix sol: <http://alloytools.org/solution/> .");
		pw.println();

		String solutionUri = String.format("sol:%s_solution_%d", OutputUtils.sanitizeRdfId(cname), index);

		// Solution metadata
		pw.printf("# Alloy Solution: %s, index %d%n", cname, index);
		pw.printf("%s a alloy:Solution ;%n", solutionUri);
		pw.printf("    alloy:command \"%s\" ;%n", OutputUtils.escapeRdf(cname));
		pw.printf("    alloy:solutionIndex %d ;%n", index);
		pw.printf("    alloy:durationMs %d ;%n", dto.duration);
		pw.printf("    alloy:incremental %s ;%n", dto.incremental);
		pw.printf("    alloy:loopstate %d ;%n", dto.loopstate);
		pw.printf("    alloy:utcTime %d ;%n", dto.utctime);
		if (dto.localtime != null) {
			pw.printf("    alloy:localTime \"%s\"^^xsd:dateTime ;%n", dto.localtime);
		}
		if (dto.timezone != null) {
			pw.printf("    alloy:timezone \"%s\" ;%n", dto.timezone);
		}
		pw.println("    .");
		pw.println();

		// Instance data
		int instanceIdx = 0;
		for (InstanceDTO instance : dto.instances) {
			String instanceUri = String.format("%s_instance_%d", solutionUri, instanceIdx);
			pw.printf("%s a alloy:Instance ;%n", instanceUri);
			pw.printf("    alloy:state %d ;%n", instance.state);
			pw.printf("    alloy:partOf %s ;%n", solutionUri);
			pw.println("    .");
			pw.println();

			// Write atoms and relations
			for (Map.Entry<String, Map<String, String[][]>> sigEntry : instance.values.entrySet()) {
				String sigName = sigEntry.getKey();
				String sigUri = "sol:" + OutputUtils.sanitizeRdfId(sigName);
				
				pw.printf("# Signature: %s%n", sigName);
				pw.printf("%s a alloy:Signature ;%n", sigUri);
				pw.printf("    rdfs:label \"%s\" ;%n", OutputUtils.escapeRdf(sigName));
				pw.println("    .");
				pw.println();

				for (Map.Entry<String, String[][]> fieldEntry : sigEntry.getValue().entrySet()) {
					String fieldName = fieldEntry.getKey();
					String[][] tuples = fieldEntry.getValue();
					
					if (tuples != null && tuples.length > 0) {
						String fieldUri = sigUri + "_" + OutputUtils.sanitizeRdfId(fieldName);
						pw.printf("# Field: %s.%s%n", sigName, fieldName);
						pw.printf("%s a alloy:Field ;%n", fieldUri);
						pw.printf("    rdfs:label \"%s\" ;%n", OutputUtils.escapeRdf(fieldName));
						pw.printf("    alloy:belongsTo %s ;%n", sigUri);
						pw.println("    .");
						
						for (int t = 0; t < tuples.length; t++) {
							String[] tuple = tuples[t];
							String tupleUri = fieldUri + "_tuple_" + t;
							pw.printf("%s a alloy:Tuple ;%n", tupleUri);
							pw.printf("    alloy:inField %s ;%n", fieldUri);
							for (int i = 0; i < tuple.length; i++) {
								String atomUri = "sol:" + OutputUtils.sanitizeRdfId(tuple[i]);
								pw.printf("    alloy:element%d %s ;%n", i, atomUri);
								// Also declare the atom
								pw.printf("%s a alloy:Atom ; rdfs:label \"%s\" .%n", atomUri, OutputUtils.escapeRdf(tuple[i]));
							}
							pw.println("    .");
						}
						pw.println();
					}
				}
			}

			// Write skolems
			for (Map.Entry<String, TuplesDTO> skolem : instance.skolems.entrySet()) {
				String skolemName = skolem.getKey();
				String skolemUri = "sol:" + OutputUtils.sanitizeRdfId(skolemName);
				TuplesDTO tuplesDto = skolem.getValue();
				
				pw.printf("# Skolem: %s%n", skolemName);
				pw.printf("%s a alloy:Skolem ;%n", skolemUri);
				pw.printf("    rdfs:label \"%s\" ;%n", OutputUtils.escapeRdf(skolemName));
				pw.printf("    alloy:arity %d ;%n", tuplesDto.arity);
				pw.println("    .");
				
				if (tuplesDto.data != null) {
					for (int t = 0; t < tuplesDto.data.length; t++) {
						String[] tuple = tuplesDto.data[t];
						String tupleUri = skolemUri + "_tuple_" + t;
						pw.printf("%s a alloy:SkolemTuple ;%n", tupleUri);
						pw.printf("    alloy:inSkolem %s ;%n", skolemUri);
						for (int i = 0; i < tuple.length; i++) {
							String atomUri = "sol:" + OutputUtils.sanitizeRdfId(tuple[i]);
							pw.printf("    alloy:element%d %s ;%n", i, atomUri);
						}
						pw.println("    .");
					}
				}
				pw.println();
			}
			
			instanceIdx++;
		}

		// Write signature definitions
		for (Map.Entry<String, SigDefDTO> sigEntry : dto.sigs.entrySet()) {
			SigDefDTO sig = sigEntry.getValue();
			String sigDefUri = "sol:SigDef_" + OutputUtils.sanitizeRdfId(sigEntry.getKey());
			
			pw.printf("# Signature Definition: %s%n", sigEntry.getKey());
			pw.printf("%s a alloy:SignatureDefinition ;%n", sigDefUri);
			if (sig.name != null) {
				pw.printf("    alloy:name \"%s\" ;%n", OutputUtils.escapeRdf(sig.name));
			}
			if (sig.cardinality != null) {
				pw.printf("    alloy:cardinality \"%s\" ;%n", sig.cardinality);
			}
			pw.printf("    alloy:isEnum %s ;%n", sig.isEnum);
			pw.printf("    alloy:isMeta %s ;%n", sig.meta);
			pw.printf("    alloy:isBuiltin %s ;%n", sig.builtin);
			if (sig.type != null) {
				pw.printf("    alloy:type \"%s\" ;%n", OutputUtils.escapeRdf(sig.type));
			}
			pw.println("    .");
			pw.println();
		}
	}

	private File getPath(File outdir, String cname, String extension, int index) {
		if (outdir == null)
			return null;
		return new File(outdir, cname + "-solution-" + index + extension);
	}

	private PrintWriter getPrintWriter(File file) throws IOException {
		if (file == null)
			return new PrintWriter(stdout);

		return new PrintWriter(IO.writer(file));
	}

	@Override
	public String toString() {
		return "CLI";
	}
}
