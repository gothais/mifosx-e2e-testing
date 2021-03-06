// ========================================================================
// $Id: ForwardHandler.java,v 1.16 2005/08/13 00:01:26 gregwilkins Exp $
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.browsermob.proxy.jetty.http.handler;

import org.apache.commons.logging.Log;
import org.browsermob.proxy.jetty.http.*;
import org.browsermob.proxy.jetty.log.LogFactory;
import org.browsermob.proxy.jetty.util.URI;
import org.browsermob.proxy.jetty.util.UrlEncoded;

import java.io.IOException;
import java.util.Map;

// TODO: Auto-generated Javadoc
/* ------------------------------------------------------------ */
/**
 * Forward Request Handler. Forwards a request to a new URI. Experimental - use
 * with caution.
 * 
 * @version $Revision: 1.16 $
 * @author Greg Wilkins (gregw)
 */
public class ForwardHandler extends AbstractHttpHandler {

	/** The log. */
	private static Log log = LogFactory.getLog(ForwardHandler.class);

	/** The _forward. */
	PathMap _forward = new PathMap();

	/** The _root. */
	String _root;

	/** The _handle queries. */
	boolean _handleQueries = false;

	/* ------------------------------------------------------------ */
	/**
	 * Constructor.
	 */
	public ForwardHandler() {
	}

	/* ------------------------------------------------------------ */
	/**
	 * Constructor.
	 * 
	 * @param rootForward
	 *            the root forward
	 */
	public ForwardHandler(String rootForward) {
		_root = rootForward;
	}

	/* ------------------------------------------------------------ */
	/**
	 * Add a forward mapping.
	 * 
	 * @param pathSpecInContext
	 *            The path to forward from
	 * @param newPath
	 *            The path to forward to.
	 */
	public void addForward(String pathSpecInContext, String newPath) {
		_forward.put(pathSpecInContext, newPath);
	}

	/* ------------------------------------------------------------ */
	/**
	 * Add a forward mapping for root path. This allows a forward for exactly /
	 * which is the default path in a pathSpec.
	 * 
	 * @param newPath
	 *            The path to forward to.
	 */
	public void setRootForward(String newPath) {
		_root = newPath;
	}

	/* ------------------------------------------------------------ */
	/**
	 * Set the Handler up to cope with forwards to paths that contain query
	 * elements (e.g. "/blah"->"/foo?a=b").
	 * 
	 * @param b
	 *            the new handle queries
	 */
	public void setHandleQueries(boolean b) {
		_handleQueries = b;
	}

	/* ------------------------------------------------------------ */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.browsermob.proxy.jetty.http.HttpHandler#handle(java.lang.String,
	 * java.lang.String, org.browsermob.proxy.jetty.http.HttpRequest,
	 * org.browsermob.proxy.jetty.http.HttpResponse)
	 */
	public void handle(String pathInContext, String pathParams,
			HttpRequest request, HttpResponse response) throws HttpException,
			IOException {
		if (log.isTraceEnabled())
			log.trace("Look for " + pathInContext + " in " + _forward);

		String newPath = null;
		String query = null;
		if (_root != null
				&& ("/".equals(pathInContext) || pathInContext.startsWith("/;")))
			newPath = _root;
		else {
			Map.Entry entry = _forward.getMatch(pathInContext);
			if (entry != null) {
				String match = (String) entry.getValue();
				if (_handleQueries) {
					int hook = match.indexOf('?');
					if (hook != -1) {
						query = match.substring(hook + 1);
						match = match.substring(0, hook);
					}
				}
				String info = PathMap.pathInfo((String) entry.getKey(),
						pathInContext);
				if (log.isDebugEnabled())
					log.debug("Forward: match:\"" + match + "\" info:" + info
							+ "\" query:" + query);
				newPath = info == null ? match : (URI.addPaths(match, info));
			}
		}

		if (newPath != null) {
			if (log.isDebugEnabled())
				log.debug("Forward from " + pathInContext + " to " + newPath);

			int last = request.setState(HttpMessage.__MSG_EDITABLE);
			String context = getHttpContext().getContextPath();
			if (context.length() == 1)
				request.setPath(newPath);
			else
				request.setPath(URI.addPaths(context, newPath));
			if (_handleQueries && query != null) {
				// add forwarded to query string to parameters
				UrlEncoded.decodeTo(query, request.getParameters());
			}
			request.setState(last);
			getHttpContext().getHttpServer().service(request, response);
			return;
		}
	}
}
