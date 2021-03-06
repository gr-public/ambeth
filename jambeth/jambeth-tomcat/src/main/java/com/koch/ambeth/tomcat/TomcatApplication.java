package com.koch.ambeth.tomcat;

/*-
 * #%L
 * jambeth-tomcat
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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.FileResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import com.koch.ambeth.util.exception.RuntimeExceptionUtil;

/**
 * This class lets you start an embedded Tomcat server. It is not a bean and does not start Ambeth,
 * but when the {@link AmbethServletListener} is on the classpath, then Ambeth is started by Tomcat.
 */
public class TomcatApplication implements Closeable {
	public static final String WEBSERVER_PORT_DEFAULT = "8080";
	public static final String WEBSERVER_PORT = "webserver.port";

	public static final String APP_CONTEXT_ROOT_DEFAULT = "";
	public static final String APP_CONTEXT_ROOT = "app.context.root";
	private Tomcat tomcat;

	public static TomcatApplication run() throws Throwable {
		TomcatApplication ambethApp = new TomcatApplication();
		ambethApp.doRun();
		return ambethApp;
	}

	@Override
	public void close() throws IOException {
		if (tomcat != null) {
			try {
				tomcat.stop();
			}
			catch (LifecycleException e) {
				throw RuntimeExceptionUtil.mask(e);
			}
			tomcat = null;
		}
	}

	public void doRun() throws Throwable {
		if (tomcat != null) {
			throw new IllegalStateException("Tomcat already running");
		}
		startEmeddedTomcat();
	}

	private void startEmeddedTomcat() throws ServletException, LifecycleException {
		String webappDirLocation = "src/main/webapp/";
		tomcat = new Tomcat();
		tomcat.setPort(Integer.valueOf(System.getProperty(WEBSERVER_PORT, WEBSERVER_PORT_DEFAULT)));

		Context ctx = tomcat.addWebapp(System.getProperty(APP_CONTEXT_ROOT, APP_CONTEXT_ROOT_DEFAULT),
				new File(webappDirLocation).getAbsolutePath());
		WebResourceRoot resources = prepareTomcatResources(ctx);

		ctx.setResources(resources);

		tomcat.start();
		tomcat.getServer().await();
	}

	private WebResourceRoot prepareTomcatResources(Context context) {
		WebResourceRoot resources = new StandardRoot(context);

		String cp = System.getProperty("java.class.path");
		StringTokenizer st = new StringTokenizer(cp, ";");
		while (st.hasMoreElements()) {
			String pathElement = st.nextToken();
			File pe = new File(pathElement);
			if (pe.isFile()) {
				resources.addPreResources(new FileResourceSet(resources, "/WEB-INF/lib/" + pe.getName(),
						pe.getAbsolutePath(), "/"));
			}
			else {
				resources.addPreResources(
						new DirResourceSet(resources, "/WEB-INF/classes", pe.getAbsolutePath(), "/"));
			}
		}
		return resources;
	}
}
