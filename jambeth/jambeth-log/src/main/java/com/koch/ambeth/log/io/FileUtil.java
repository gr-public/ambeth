package com.koch.ambeth.log.io;

/*-
 * #%L
 * jambeth-log
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.state.AbstractStateRollback;
import com.koch.ambeth.util.state.IStateRollback;
import com.koch.ambeth.util.threading.IBackgroundWorkerDelegate;

public final class FileUtil {
	public static class DeleteDelegate implements IBackgroundWorkerDelegate {
		private final Path file;

		public DeleteDelegate(Path file) {
			this.file = file;
		}

		@Override
		public void invoke() throws Exception {
			Files.delete(file);
		}
	}

	private static final ThreadLocal<Class<?>> currentTypeScopeTL = new ThreadLocal<>();

	private static final Pattern CONFIG_SEPARATOR = Pattern.compile(";");

	private static final Pattern PATH_SEPARATOR = Pattern.compile(File.pathSeparator);

	private static FileUtilOsgi fileUtilOsgi;

	static {
		try {
			fileUtilOsgi = new FileUtilOsgi();
		}
		catch (Throwable e) {
			// intended blank
		}
	}

	public static final IStateRollback pushCurrentTypeScope(Class<?> currentTypeScope,
			IStateRollback... rollbacks) {
		final Class<?> oldCurrentTypeScope = currentTypeScopeTL.get();
		currentTypeScopeTL.set(currentTypeScope);
		return new AbstractStateRollback(rollbacks) {
			@Override
			protected void rollbackIntern() throws Exception {
				currentTypeScopeTL.set(oldCurrentTypeScope);
			}
		};
	}

	protected FileUtil() {
		// Intended blank
	}

	public static String[] splitConfigFileNames(String fileNames) {
		String[] splittedfileNames = CONFIG_SEPARATOR.split(fileNames);
		return splittedfileNames;
	}

	public static InputStream[] openFileStreams(String fileNames) {
		InputStream[] fileStreams = openFileStreams(fileNames, null);
		return fileStreams;
	}

	public static InputStream[] openFileStreams(String fileNames, ILogger log) {
		String[] splittedfileNames = splitConfigFileNames(fileNames);
		InputStream[] fileStreams = openFileStreams(splittedfileNames);
		return fileStreams;
	}

	public static InputStream[] openFileStreams(String... fileNames) {
		InputStream[] inputStreams = openFileStreams(fileNames, false, null);
		return inputStreams;
	}

	public static InputStream[] openFileStreams(String[] fileNames, ILogger log) {
		return openFileStreams(fileNames, false, log);
	}

	public static InputStream[] openFileStreams(String[] fileNames, boolean ignoreEmptyNames,
			ILogger log) {
		InputStream[] inputStreams = new InputStream[fileNames.length];

		for (int i = fileNames.length; i-- > 0;) {
			String fileName = fileNames[i];
			if (fileName == null || fileName.isEmpty()) {
				continue;
			}

			InputStream inputStream = null;
			Exception original = null;
			try {
				inputStream = openFileStream(fileName, log);
			}
			catch (IllegalArgumentException e) {
				// inputStream is null. openFileStream() threw this exception, but we could give more
				// informations.
				original = e;
			}
			if (inputStream == null) {
				String combinesFileNames = combine(fileNames);
				String workingDir = System.getProperty("user.dir");
				String msg =
						"File source '%s' not found in filesystem and classpath.  Filenames: '%s', current working directory: %s";
				throw new IllegalArgumentException(
						String.format(msg, fileName, combinesFileNames, workingDir), original);
			}
			inputStreams[i] = inputStream;
		}

		return inputStreams;
	}

	public static InputStream openFileStream(String fileName) {
		InputStream inputStream = openFileStream(fileName, null);
		return inputStream;
	}

	public static InputStream openFileStream(String fileName, ILogger log) {
		InputStream inputStream = null;
		Class<?> currentTypeScope = currentTypeScopeTL.get();
		String lookupName = fileName;
		if (currentTypeScope != null) {
			lookupName = currentTypeScope.getPackage().getName().replace('.', '/') + "/" + fileName;
			// check first to look for the fileName relative to our current typeScope
			String pathString =
					currentTypeScope.getProtectionDomain().getCodeSource().getLocation().getPath();
			if (pathString.startsWith("/")) {
				pathString = pathString.substring(1);
			}
			try {
				Path path = Paths.get(URLDecoder.decode(pathString, "UTF-8"), lookupName);
				if (Files.exists(path)) {
					inputStream = Files.newInputStream(path);
				}
				path = Paths.get(URLDecoder.decode(pathString, "UTF-8"), fileName);
				if (Files.exists(path)) {
					inputStream = Files.newInputStream(path);
				}
			}
			catch (InvalidPathException e) {
				// Ignore and continue
			}
			catch (Exception e) {
				throw RuntimeExceptionUtil.mask(e);
			}
			if (inputStream == null) {
				inputStream = currentTypeScope.getClassLoader().getResourceAsStream(lookupName);
			}
			if (inputStream == null && fileUtilOsgi != null) {
				inputStream = fileUtilOsgi.openFromOSGiTree(currentTypeScope, lookupName);
			}
			if (inputStream == null) {
				inputStream =
						Thread.currentThread().getContextClassLoader().getResourceAsStream(lookupName);
			}
		}
		if (inputStream == null) {
			lookupName = fileName;

			String pathString =
					FileUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			try {
				File file = new File(URLDecoder.decode(pathString, "UTF-8") + '/' + lookupName);
				if (file.exists()) {
					inputStream = Files.newInputStream(file.toPath());
				}
			}
			catch (Exception e) {
				throw RuntimeExceptionUtil.mask(e);
			}
			try {
				URL url = new URL(pathString + '/' + lookupName);
				inputStream = url.openStream();
			}
			catch (Throwable e) {
				// intended blank
			}
			try {
				URL url = new URL(lookupName);
				inputStream = url.openStream();
			}
			catch (Throwable e) {
				// intended blank
			}
			if (inputStream == null && currentTypeScope != null) {
				inputStream = currentTypeScope.getClassLoader().getResourceAsStream(lookupName);
			}
			if (inputStream == null && fileUtilOsgi != null) {
				inputStream = fileUtilOsgi.openFromOSGiTree(currentTypeScope, lookupName);
			}
			if (inputStream == null) {
				inputStream =
						Thread.currentThread().getContextClassLoader().getResourceAsStream(lookupName);
			}
		}
		if (inputStream != null && log != null && log.isDebugEnabled()) {
			log.debug("Using stream resource '" + lookupName + "'");
		}
		else {
			File file = openFile(fileName, log);
			if (file != null) {
				try {
					inputStream = new FileInputStream(file);
				}
				catch (FileNotFoundException e) {
					throw RuntimeExceptionUtil.mask(e);
				}
			}
		}

		if (inputStream == null) {
			String msg =
					"File source '%s' not found in filesystem and classpath. Current working directory: %s";
			String workingDir = System.getProperty("user.dir");
			throw new IllegalArgumentException(String.format(msg, fileName, workingDir));
		}

		return inputStream;
	}

	public static File openFile(String fileName) {
		return openFile(fileName, null);
	}

	public static File openFile(String fileName, ILogger log) {
		File file = tryFileSystemPosition(fileName, log);
		if (file != null) {
			return file;
		}

		String pathName = null;
		String[] classPaths = PATH_SEPARATOR.split(System.getProperty("java.class.path"));
		for (int i = 0; i < classPaths.length; i++) {
			pathName = classPaths[i];
			file = tryFileSystemPosition(pathName, fileName, log);
			if (file != null) {
				return file;
			}
		}
		return null;
	}

	protected static File tryFileSystemPosition(String fileName, ILogger log) {
		return tryFileSystemPosition(null, fileName, log);
	}

	protected static File tryFileSystemPosition(String pathName, String fileName, ILogger log) {
		File file;
		file =
				pathName == null || pathName.isEmpty() ? new File(fileName) : new File(pathName, fileName);
		if (file.canRead()) {
			if (log != null && log.isDebugEnabled()) {
				log.debug(String.format("Using file resource '%s'", file.getAbsolutePath()));
			}
			return file;
		}
		return null;
	}

	protected static String combine(String[] strings) {
		if (strings == null || strings.length == 0) {
			return "";
		}
		else if (strings.length == 1) {
			return strings[0];
		}
		else {
			StringBuilder sb = new StringBuilder(strings[0]);
			for (int i = 1; i < strings.length; i++) {
				sb.append(", ").append(strings[i]);
			}
			return sb.toString();
		}
	}

	/**
	 * tries to do a specific operation for up to 6 times while the timespan between each (failed) try
	 * will be doubled. The starting delay after the first (failed) try is 8 ms. So then 16ms, 32ms,
	 * 64ms and 128ms. This functionality is helpful for some SSD behaviors where files or folders are
	 * not yet available even if you know for sure that previously executed (and successful) code
	 * created it.
	 *
	 * @param worker The delegate to try to execute. If it fails it is re-executed up to 3 additional
	 *        times.
	 */
	public static void retry(IBackgroundWorkerDelegate worker) {
		retry(worker, 6);
	}

	public static void retry(IBackgroundWorkerDelegate worker, int maximumRetryCount) {
		long waitTime = 8;
		Throwable e = null;
		for (int a = maximumRetryCount; a-- > 0;) {
			try {
				worker.invoke();
				return;
			}
			catch (Throwable ex) {
				e = ex;
			}
			try {
				Thread.sleep(waitTime);
			}
			catch (InterruptedException e1) {
				Thread.interrupted(); // clean up the interrupted flag
			}
			waitTime *= 2;
		}
		throw RuntimeExceptionUtil.mask(e);
	}

	public static void deleteRecursive(final Path path, final boolean includeTopLevel) {
		try {
			if (path == null || !Files.exists(path)) {
				return;
			}
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					retry(new DeleteDelegate(file));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (includeTopLevel || !path.equals(dir)) {
						retry(new DeleteDelegate(dir));
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (Exception e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}
}
