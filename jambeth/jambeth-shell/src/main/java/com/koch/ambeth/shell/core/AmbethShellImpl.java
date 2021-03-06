package com.koch.ambeth.shell.core;

/*-
 * #%L
 * jambeth-shell
 * %%
 * Copyright (C) 2017 Koch Softwaredevelopment
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.ioc.threadlocal.IThreadLocalCleanupBean;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.shell.AmbethShell;
import com.koch.ambeth.shell.core.ShellContext.ErrorMode;
import com.koch.ambeth.shell.core.resulttype.CommandResult;
import com.koch.ambeth.shell.util.Utils;
import com.koch.ambeth.util.Arrays;
import com.koch.ambeth.util.IConversionHelper;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.format.ISO8601DateFormat;
import com.koch.ambeth.util.threading.SensitiveThreadLocal;

public class AmbethShellImpl
		implements AmbethShell, AmbethShellIntern, CommandBindingExtendable, IThreadLocalCleanupBean {
	private static final boolean HIDE_IO_DEFAULT = true;
	// private static final boolean EXIT_ON_ERROR_DEFAULT = false;
	private static final boolean VERBOSE_DEFAULT = false;
	private static final boolean ECHO_DEFAULT = false;

	private static final String PROMPT_SIGN = ">";
	private static final String PROMPT_CONNECTOR = "&&";
	private static final String DEFAULT_PROMPT = "AMBETH";
	private static final String ECHO = "echo";

	private static final String CHEVRON_OPERATOR = ">";
	private static final String DEFAULT_RESULT_PROPERTY = "lastResult";

	private static final Pattern versionExtractPattern = Pattern.compile("(\\d+\\.\\d+).*");

	@LogInstance
	private ILogger log;

	protected final ThreadLocal<DateFormat> isoDateFormatTL = new SensitiveThreadLocal<>();

	protected final Map<String, CommandBinding> commandBindings = new HashMap<>();

	protected Map<String, String> promptMap = new HashMap<>();

	@Autowired
	protected ShellContext context;

	@Autowired
	protected IConversionHelper conversionHelper;

	protected PrintStream shellOut = System.out;

	@Override
	public PrintStream getShellOut() {
		return shellOut;
	}

	@Override
	public void setShellOut(PrintStream shellOut) {
		this.shellOut = shellOut;
	}

	@Property(name = ShellContext.BATCH_FILE, mandatory = false, defaultValue = "")
	protected String batchFile;

	@Property(name = ShellContext.VARS_FOR_BATCH_FILE, mandatory = false)
	protected HashMap<String, String> varsForBatchFile;

	@Property(name = ShellContext.MAIN_ARGS, mandatory = false)
	protected String[] mainArgs;
	private PrintStream originalOut;
	private PrintStream originalErr;

	/**
	 *
	 * @return
	 */
	private static final DateFormat createIsoDateFormat() {

		DateFormat.getDateInstance().format(new Date());

		String versionProperty = System.getProperty("java.version");
		Matcher versionMatcher = versionExtractPattern.matcher(versionProperty);
		if (versionMatcher.find() && Double.parseDouble(versionMatcher.group(1)) >= 1.7) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
		else {
			return new ISO8601DateFormat();
		}
	}

	@Override
	public void startShell() {
		initSystemIO();

		if (getContext().get(ShellContext.HIDE_IO, HIDE_IO_DEFAULT)) {
			preventSystemIO();
		}

		try {
			if (batchFile != null && !batchFile.isEmpty()) {

				try (Reader reader = new FileReader(new File(batchFile));
						BufferedReader scriptReader = new BufferedReader(reader)) {
					getContext().set(ECHO, true);

					// save the variables for the batch file to shellContext
					if (varsForBatchFile != null && !varsForBatchFile.isEmpty()) {
						Set<String> keySet = varsForBatchFile.keySet();
						for (String variable : keySet) {
							getContext().set(variable, varsForBatchFile.get(variable));
						}
					}

					startInteractive(scriptReader);
				}
				catch (IOException e) {
					throw RuntimeExceptionUtil.mask(e);
				}
			}
			else if (mainArgs != null && mainArgs.length > 0) {
				try {
					executeCommand(mainArgs);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				startInteractive(new BufferedReader(new InputStreamReader(System.in)));
			}
		}
		finally {
			restoreSystemIO();
		}
	}

	private void initSystemIO() {
		shellOut = System.out;
	}

	private void restoreSystemIO() {
		if (originalOut != null) {
			System.setOut(originalOut);
		}

		if (originalErr != null) {
			System.setErr(originalErr);
		}
	}

	private void preventSystemIO() {
		PrintStream noOpSystemOut = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				if (!getContext().get(ShellContext.HIDE_IO, HIDE_IO_DEFAULT)) {
					shellOut.write(b);
				}
			}
		});

		originalOut = System.out;
		originalErr = System.err;
		System.setOut(noOpSystemOut);
		System.setErr(noOpSystemOut);
	}

	@Override
	public void startInteractive(BufferedReader br) {
		try {
			if (context.get(ShellContext.GREETING_ACTIVE, false)) {

				println("Welcome to " + context.get(ShellContext.PRODUCT_NAME, "Ambeth Shell") + " "
						+ context.get(ShellContext.PRODUCT_VERSION, ""));
			}

			String licenseNotice = "";
			Long licenseExpirationDate = context.get(ShellContext.LICENSE_EXPIRATION_DATE, Long.class);
			Long now = System.currentTimeMillis();
			if (licenseExpirationDate != null) {
				if (licenseExpirationDate > now) {
					licenseNotice = "License valid until " + new Date(licenseExpirationDate);
				}
				else {
					licenseNotice = "License expired on " + new Date(licenseExpirationDate);
					context.set(ShellContext.SHUTDOWN, true);
				}
			}

			if (context.get(ShellContext.LICENSE_TYPE, License.COMMERCIAL) == License.DEMO) {
				String licenseText = context.get(ShellContext.LICENSE_TEXT, "Demo Version");
				println(
						Utils.stringPadEnd("", Math.max(licenseText.length(), licenseNotice.length()), '='));
				println(licenseText);
				if (licenseNotice.trim().length() > 0) {
					println(licenseNotice);
				}
				println(
						Utils.stringPadEnd("", Math.max(licenseText.length(), licenseNotice.length()), '='));
			}
			else {

				if (licenseNotice.trim().length() > 0) {
					println(Utils.stringPadEnd("", licenseNotice.length(), '='));
					println(licenseNotice);
					println(Utils.stringPadEnd("", licenseNotice.length(), '='));
				}
			}

			String userInput;
			while (!context.get(ShellContext.SHUTDOWN, false)) {
				prompt();
				userInput = br.readLine();
				if (userInput == null) {
					break;
				}

				if (getContext().get(ECHO, ECHO_DEFAULT)) {
					println(userInput);
				}

				try {
					executeRawCommand(userInput);
				}
				catch (Exception e) {
					handleCommandError(e);
				}
			}
		}
		catch (IOException e) {
			log.error("failed to handle IO!");
		}
	}

	/**
	 * // TODO find a regex Guru :)
	 *
	 * @param userInput
	 * @return
	 */
	private List<String> parseCommandLine(String userInput) {
		List<String> parts = new ArrayList<>();
		String sequence = "";
		boolean insideQuotes = false;
		boolean whitespaceRegion = false;
		for (int i = 0; i < userInput.length(); i++) {
			String c = "" + userInput.charAt(i);
			if (c.matches("\\s") && !insideQuotes) {
				whitespaceRegion = true;
				continue;
			}

			if (c.matches("\"")) {
				if (insideQuotes) {
					// closing quotes
					insideQuotes = false;
				}
				else {
					// starting quotes
					insideQuotes = true;
				}
			}

			if (whitespaceRegion) {
				// end of WS region
				parts.add(sequence);
				sequence = "";
				whitespaceRegion = false;
			}
			sequence += c;
		}
		parts.add(sequence);
		return parts;
	}

	@Override
	public CommandBinding getCommandBinding(String name) {
		return commandBindings.get(name);
	}

	@Override
	public Collection<CommandBinding> getCommandBindings() {
		return commandBindings.values();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandResult executeRawCommand(String unparsedCommandLine) {
		List<String> parts = parseCommandLine(unparsedCommandLine);
		return executeCommand(parts.toArray(new String[parts.size()]));
	}

	/**
	 * examples: set foo=bar echo "hello world" open test.adf
	 */
	@Override
	public CommandResult executeCommand(String... args) {

		if (args != null && args.length > 0) {
			String commandName = args[0].trim();
			if (commandName.length() == 0) {
				return null;
			}
			Map<String, Object> dFlags = new HashMap<>();
			try {
				List<String> argSet = new ArrayList<>();
				String resultVariableName = parseArguments(dFlags, argSet, args);

				CommandBinding command = commandBindings.get(commandName.toLowerCase());
				if (command == null) {
					println("Unknown command: " + commandName);
					return null;
				}
				// FIXME use conversion helper!!!
				Object execute = command.execute(argSet);
				if (execute != null) {
					if (resultVariableName == null) {
						// chevron operator is not set, print out execute result
						print(execute);
					}
					getContext().set(
							resultVariableName != null ? resultVariableName : DEFAULT_RESULT_PROPERTY, execute);
				}
				if (execute instanceof CommandResult) {
					return (CommandResult) execute;
				}
			}
			catch (Exception e) {
				handleCommandError(e);
			}
			finally {
				for (String key : dFlags.keySet()) {
					context.set(key, dFlags.get(key));
				}
			}
		}
		return null;
	}

	/**
	 * parse all the arguments
	 *
	 * @param dParamMap
	 *          map of all -D parameter
	 * @param argSet
	 *          target list of arguments for the command which exclude -D parameter and chevron
	 *          operator
	 * @param args
	 *          all the input arguments
	 * @return variable name for the command return result
	 */
	private String parseArguments(Map<String, Object> dParamMap, List<String> argSet,
			String... args) {
		String resultVariableName = null;
		String arg;
		for (int i = 1; i < args.length; i++) {
			arg = args[i];
			if (arg.startsWith("-D")) {
				// parse -D parameter
				arg = arg.replaceAll("-D", "");
				if (arg.indexOf("=") != -1) {
					String[] kvPair = arg.split("=");
					String value = kvPair[1];
					dParamMap.put(kvPair[0], context.get(kvPair[0]));
					if (value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(1, value.length() - 1);
					}
					context.set(kvPair[0], value);
				}
				else {
					dParamMap.put(arg, context.get(arg));
					context.set(arg, "true");
				}
			}
			else {
				if (CHEVRON_OPERATOR.equals(arg)) {
					// parse chevron operator
					if (i == args.length - 1 || "".equals(args[i + 1].trim())) {
						throw new RuntimeException(
								"Please input the target variable name of the chevron operator!");
					}
					resultVariableName = args[i + 1].trim();
					break;
				}
				else {
					argSet.add(arg);
				}
			}
		}
		return resultVariableName;

	}

	/**
	 *
	 * @param cb
	 * @param unparsedArgs
	 * @param e
	 */
	private void handleCommandError(Exception e) {
		String errorModeStr = context.get(ShellContext.ERROR_MODE, ErrorMode.LOG_ONLY.toString());
		ErrorMode errorMode = ErrorMode.LOG_ONLY;

		try {
			errorMode = ErrorMode.valueOf(errorModeStr);
		}
		catch (IllegalArgumentException iae) {
			StringBuilder errorModesSB = new StringBuilder();
			Arrays.toString(errorModesSB, ErrorMode.values());
			println("Invalid error.mode: " + errorModeStr + "! Possible values are: " + errorModesSB
					+ ". Fallback to '" + errorMode + "'");
			e.addSuppressed(iae);
		}

		try {
			if (errorMode == ErrorMode.THROW_EXCPETION) {
				throw new RuntimeException("Exception during command execution, see stacktrace for details",
						e);
			}
			else {
				if (context.get(ShellContext.VERBOSE, VERBOSE_DEFAULT)) {
					e.printStackTrace(shellOut);
				}
				else {
					String message = e.getMessage();
					if (message == null && e instanceof InvocationTargetException) {
						message = ((InvocationTargetException) e).getTargetException().getMessage();
					}
					else if (message == null && e.getCause() != null) {
						message = e.getCause().getMessage();
					}
					println(message);
				}
			}
		}
		finally {
			if (errorMode == ErrorMode.EXIT_ON_ERROR) {
				exit(1);
			}
		}
	}

	private void prompt() {
		String promptValue = getPromptString();
		print(promptValue + PROMPT_SIGN);
	}

	@Override
	public void print(Object object) {
		shellOut.print(object);
	}

	@Override
	public void println() {
		shellOut.println();
	}

	@Override
	public void println(Object object) {
		shellOut.println(object);
	}

	@Override
	public void exit(int status) {
		// this is OK because our IoC context is bound to the VM shutdown hook for a graceful shutdown
		// procedure
		// System.exit(status);
		getContext().set(ShellContext.SHUTDOWN, true);
	}

	@Override
	public void register(CommandBinding commandBinding, String commandName) {
		commandBindings.put(commandName, commandBinding);
	}

	@Override
	public void unregister(CommandBinding commandBinding, String commandName) {
		commandBindings.remove(commandName);
	}

	@Override
	public ShellContext getContext() {
		return context;
	}

	@Override
	public DateFormat getDateFormat() {
		DateFormat dateFormat = isoDateFormatTL.get();
		if (dateFormat == null) {
			dateFormat = createIsoDateFormat();
			isoDateFormatTL.set(dateFormat);
		}
		return dateFormat;
	}

	@Override
	public void cleanupThreadLocal() {
		isoDateFormatTL.set(null);
	}

	@Override
	public void setPrompt(String key, String value) {
		promptMap.put(key, value);
	}

	@Override
	public void removePrompt(String key) {
		promptMap.remove(key);
	}

	@Override
	public String getPromptString() {
		String promptValue = null;
		Set<String> keySet = promptMap.keySet();
		for (String key : keySet) {
			if (promptValue == null) {
				promptValue = promptMap.get(key);
			}
			else {
				promptValue = promptValue + PROMPT_CONNECTOR + promptMap.get(key);
			}

		}
		if (promptValue == null) {
			promptValue = context.get(ShellContext.PROMPT, DEFAULT_PROMPT);
		}
		return promptValue;
	}

	@Override
	public Path getSecuredFileAsPath(String fileName) {
		if (fileName == null) {
			return null;
		}
		String baseFolder = getContext().get(SHELL_CONTEXT_BASE_FOLDER, String.class);
		Path filePath = null;
		if (baseFolder != null) {
			// check if the path is already OK
			Path checkPath = Paths.get(fileName);
			if (checkPath.isAbsolute()) {
				filePath = checkPath;
			}
			else {
				filePath = Paths.get(baseFolder, fileName);
			}
			if (!filePath.normalize().startsWith(Paths.get(baseFolder))) {
				throw new IllegalArgumentException("The path " + fileName + " was not accepted");
			}
		}
		else {
			filePath = Paths.get(fileName);
		}

		Path absolutePath = filePath.toAbsolutePath().normalize();
		// TODO: this can only handle pathes with filenames, if there is no filename not all dirs get
		// created
		// create all necessary dir's
		if (absolutePath.getParent() != null) {
			try {
				Files.createDirectories(absolutePath.getParent());
			}
			catch (IOException e) {
				throw RuntimeExceptionUtil.mask(e);
			}
		}
		return absolutePath;
	}

	@Override
	public String getSecuredFileAsString(String fileName) {
		if (fileName == null) {
			return null;
		}
		Path path = getSecuredFileAsPath(fileName);
		return path.toString();
	}

	@Override
	public Path getSecuredFileAsPath(Path resolve) {
		return getSecuredFileAsPath(resolve.toString());
	}
}
