package com.test.weather.v2;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * modified timeout and retry
 * @author johne
 *
 */
@WebServlet(urlPatterns = "/Weather/v2", asyncSupported = true)
public class WeatherDataStream extends HttpServlet {

	private static final long serialVersionUID = 1L;
	// Keeps all open connections from browsers
	private Set<AsyncContext> asyncContexts = new HashSet<AsyncContext>();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
        
		
		// Check that it is SSE request
		if ("text/event-stream".equals(request.getHeader("Accept"))) {
			log("--SSE REQUEST--");
			// send streaming data to all open connections
			// Set header fields
			response.setContentType("text/event-stream");
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Connection", "keep-alive");
			response.setCharacterEncoding("UTF-8");
			
			// tell the browser if connection
            // fails to reopen it after 10 seconds
            response.getWriter().println("retry: 10000\n");

			// Start asynchronous context and add listeners to remove it in case
			// of errors
			final AsyncContext ac = request.startAsync();
			log("Default timeout for this asyncContext: "+ac.getTimeout());
			// The timeout will expire if neither the complete() method nor any of the dispatch methods are called on the
			// asyncContext. 
			// this value times out the connection
			ac.setTimeout(Integer.MAX_VALUE);
			log("Changed timeout for this asyncContext: "+ac.getTimeout());
			
			ac.addListener(new AsyncListener() {
				@Override
				public void onComplete(AsyncEvent event) throws IOException {
					log("--ASYNC EVENT COMPLETE-- ");
					asyncContexts.remove(event.getAsyncContext());
				}

				@Override
				public void onError(AsyncEvent event) throws IOException {
					log("--ASYNC EVENT ERROR--");
					asyncContexts.remove(event.getAsyncContext());
				}

				@Override
				public void onStartAsync(AsyncEvent event) throws IOException {
					log("--ASYNC EVENT START--");
				}

				@Override
				public void onTimeout(AsyncEvent event) throws IOException {
					log("--ASYNC EVENT TIMEOUT--");
					event.getAsyncContext().complete();
					asyncContexts.remove(event.getAsyncContext());
				}
			});
			// Put context in a map
			asyncContexts.add(ac);
			log("Event Registration for connection obj: "+ac.toString());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log("--WEATHER POST DATA RECEIVED--");
		log("Current set of connections: " + asyncContexts);
		WeatherToken token = new WeatherToken(request.getParameter("city"), request.getParameter("temp"));
		
		// Sends the message to all the asyncContext's response
		for (AsyncContext asyncContext : asyncContexts) {
			log("Sending MSG to connection obj: " + asyncContext);
			boolean errorStatus = sendMessage(asyncContext.getResponse().getWriter(), token);
			if (errorStatus) {
				throw new RuntimeException("Connection closed by client");
			}
		}
	}

	private boolean sendMessage(PrintWriter writer, WeatherToken token) {
		writer.println("event: city");
		writer.print("data: ");
		writer.println(token.getType()+":"+token.getData());
		writer.println(); 			//new line marks an event boundary
		return writer.checkError(); //checkError() calls writer.flush();
	}

	@Override
	public void destroy() {
		log("--SERVLET DESTROYED--");
		for(AsyncContext asyncContext: asyncContexts){
			asyncContext.complete();
		}
		super.destroy();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		log("--SERVLET INITIALIZED--");
		super.init(config);
	}

	public void log(String output) {
		System.out.println(LocalDateTime.now() +" [" + Thread.currentThread().getName() + "]" + output);
	}
}

class WeatherToken {
	private String tokenType;
	private String tokenValue;

	public WeatherToken(String data, String type) {
		this.tokenValue = data;
		this.tokenType = type;
	}

	public String getType() {
		return tokenType;
	}

	public String getData() {
		return tokenValue;
	}
}
