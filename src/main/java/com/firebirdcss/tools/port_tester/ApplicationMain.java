package com.firebirdcss.tools.port_tester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 
 * @author Scott Griffis
 *
 */
public final class ApplicationMain {
	private final static Map<String, Service> threadsByPort = new HashMap<>();
	private static boolean running =  true;
	private static Thread me = null;
	
	/**
	 * @param args - An array of ports to be tested
	 */
	public static void main(String[] args) {
		me = Thread.currentThread();
		running = true;
		if (args != null && args.length > 0) {
			for (String arg : args) {
				try {
					Service s = startServiceForPort(arg);
					threadsByPort.put(arg, s);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			
			Runtime.getRuntime().addShutdownHook(new ShutdownHook());
			
			while (running) {
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e) {
					// do nothing...
				}
			}
			
			System.out.println("Application has exited.");
			System.exit(0);
		} else {
			printProgramUsage();
		}
		
	}
	
	/**
	 * 
	 */
	protected static void shutdown() {
		running = false;
		me.interrupt();
	}
	
	/**
	 * 
	 */
	private static void printProgramUsage() {
		String message = ""
				+ "Usage: <command> <port [port...]>";
		
		System.out.println(message);
	}
	
	/**
	 * 
	 * @param port
	 * @return
	 * @throws Exception
	 */
	private static Service startServiceForPort(String port) throws Exception {
		try {
			int portInt = Integer.parseInt(port);
			Service service = new Service(portInt);
			service.start();
			
			System.out.println("A new service was started to answer requests on port: '" + port + "'");
			
			return service;
		} catch (Exception e) {
			throw new Exception("Invalid port value of: '" + port + "'", e);
		}
	}
	
	/**
	 * 
	 * 
	 * 
	 * @author Scott Griffis
	 *
	 */
	private static final class Service extends Thread {
		private boolean running = true;
		private Thread me = null;
		private int portNumber;
		
		/**
		 * 
		 * CONSTRUCTOR: 
		 *
		 * @param portNumber
		 */
		public Service(int portNumber) {
			this.portNumber = portNumber;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			me = Thread.currentThread();
			while (running) {
				try (
						ServerSocket serverSocket = new ServerSocket(portNumber);
						Socket socket = serverSocket.accept(); 
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				) {
					out.println("Hello! This is Port Tester, I will echo whatever you say.");
					String line = null;
					while ((line = in.readLine()) != null) {
						if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
							out.println("ECHO: " + line + "\nOk, Goodbye!");
							break;
						}
						out.println("ECHO: " + line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * 
		 */
		public void shutdown() {
			running = false;
			if (me != null) {
				me.interrupt();
			}
		}
	}
	
	/**
	 * 
	 * 
	 * 
	 * @author Scott Griffis
	 *
	 */
	public static final class ShutdownHook extends Thread {
		@Override
		public void run() {
			System.out.println("Shutting down application...");
			for (Service s : threadsByPort.values()) {
				s.shutdown();
				try {
					s.join(3000L);
				} catch (InterruptedException e) {
					// Just forget it...
				}
			}
			
			shutdown(); // Main shutdown...
		}
	}
}
