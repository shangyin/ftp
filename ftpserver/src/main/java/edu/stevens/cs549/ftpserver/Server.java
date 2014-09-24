package edu.stevens.cs549.ftpserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Stack;
import java.util.logging.Logger;

import edu.stevens.cs549.ftpinterface.IServer;

/**
 * 
 * @author dduggan; xxu
 */
public class Server extends UnicastRemoteObject implements IServer
{

	static final long serialVersionUID = 0L;

	public static Logger log = Logger
			.getLogger("edu.stevens.cs.cs549.ftpserver");

	/*
	 * For multi-homed hosts, must specify IP address on which to bind a server
	 * socket for file transfers. See the constructor for ServerSocket that
	 * allows an explicit IP address as one of its arguments.
	 */
	private InetAddress host;

	final static int backlog = 5;

	/*
	 * ********************************************************************************************
	 * Current working directory.
	 */
	static final int MAX_PATH_LEN = 1024;
	private Stack<String> cwd = new Stack<String>();

	/*
	 * ********************************************************************************************
	 * Data connection.
	 */

	enum Mode
	{
		NONE, PASSIVE, ACTIVE
	};

	private Mode mode = Mode.NONE;

	/*
	 * If passive mode, remember the server socket.
	 */

	private ServerSocket dataChan = null;

	private InetSocketAddress makePassive() throws IOException
	{
		dataChan = new ServerSocket(0, backlog, host);
		mode = Mode.PASSIVE;
		return (InetSocketAddress) (dataChan.getLocalSocketAddress());
	}

	/*
	 * If active mode, remember the client socket address.
	 */
	private InetSocketAddress clientSocketAddr = null;

	private void makeActive(InetSocketAddress s)
	{
		clientSocketAddr = s;
		mode = Mode.ACTIVE;
	}

	/*
	 * *********************************************************************************************
	 */

	/*
	 * The server can be initialized to only provide subdirectories of a
	 * directory specified at start-up.
	 */
	private final String pathPrefix;

	public Server(InetAddress host, int port, String prefix)
			throws RemoteException
	{
		super(port);
		this.host = host;
		this.pathPrefix = prefix + "/";
		log.info("A client has bound to a server instance.");
	}

	public Server(InetAddress host, int port) throws RemoteException
	{
		this(host, port, "/");
	}

	private boolean valid(String s)
	{
		// File names should not contain "/".
		return (s.indexOf('/') < 0);
	}
	
	/*
	 * class GetThread
	 * ******************************************************
	 */
	private static class GetThread implements Runnable
	{
		private ServerSocket dataChan = null;
		private FileInputStream file = null;

		public GetThread(ServerSocket s, FileInputStream f)
		{
			dataChan = s;
			file = f;
		}

		public void run()
		{
			/*
			 * TODO: Process a client request to transfer a file.
			 */
			try
			{
				// wait for the client's initial socket
				Socket xfer = dataChan.accept();

				// Prepare the output to the socket
				DataOutputStream out = new DataOutputStream(
						xfer.getOutputStream());

				// read the file from disk and write it to the socket
				byte[] sendBytes = new byte[1024];
				int iLen = 0;
				while ((iLen = file.read(sendBytes, 0, sendBytes.length)) > 0)
				{
					out.write(sendBytes, 0, iLen);
					out.flush();
				}

				log.info("file transfer done.");
				// close all the resources for this transfer
				out.close();
				xfer.close();
				file.close();
			}
			catch (Exception e)
			{
				Server.log.severe("Server exception:");
				e.printStackTrace();
			}
		}
	}
	/*
	 * end of class GetThread
	 * ******************************************************
	 */
	
	public void get(String file) throws IOException, FileNotFoundException,
			RemoteException
	{
		if (!valid(file))
		{
			throw new IOException("Bad file name: " + file);
		}
		else if (mode == Mode.ACTIVE)
		{
			log.info("get " + file + "\n" + "client port: "
					+ clientSocketAddr.getPort());
			Socket xfer = new Socket(clientSocketAddr.getAddress(),
					clientSocketAddr.getPort());

			log.info("connect to client successfully.");
			/*
			 * TODO: connect to client socket to transfer file.
			 */
			/*
			 * Prepare the input from the disk file and output to the socket.
			 */
			FileInputStream in = new FileInputStream(path() + file);
			DataOutputStream out = new DataOutputStream(xfer.getOutputStream());

			// read the file from disk and write it to the socket
			byte[] sendBytes = new byte[1024];
			int iLen = 0;
			while ((iLen = in.read(sendBytes, 0, sendBytes.length)) > 0)
			{
				out.write(sendBytes, 0, iLen);
				out.flush();
			}

			log.info("file transfer done.");
			// close all the resources for this transfer
			in.close();
			out.close();
			xfer.close();
			/*
			 * End TODO.
			 */
		}
		else if (mode == Mode.PASSIVE)
		{
			FileInputStream f = new FileInputStream(path() + file);
			new Thread(new GetThread(dataChan, f)).start();
		}
	}
	
	/*
	 * class PutThread
	 * ******************************************************
	 */
	private static class PutThread implements Runnable
	{
		private ServerSocket dataChan = null;
		private FileOutputStream file = null;

		public PutThread(ServerSocket s, FileOutputStream f)
		{
			dataChan = s;
			file = f;
		}

		public void run()
		{
			/*
			 * TODO: Process a client request to transfer a file.
			 */
			try
			{
				// wait for the client's initial socket
				Socket xfer = dataChan.accept();

				// Prepare the input from the socket
				DataInputStream in = new DataInputStream(
						xfer.getInputStream());

				// read the data from the socket and write to the disk file
				byte[] inputByte = new byte[1024];
				int iLen = 0;
				while ((iLen = in.read(inputByte, 0, inputByte.length)) > 0)
				{
					file.write(inputByte, 0, iLen);
					file.flush();
				}

				log.info("file transfer done.");
				// close all the resources for this transfer
				in.close();
				xfer.close();
				file.close();
			}
			catch (Exception e)
			{
				Server.log.severe("Server exception:");
				e.printStackTrace();
			}
		}
	}
	/*
	 * end of class PutThread
	 * ******************************************************
	 */
	
	public void put(String file) throws IOException, FileNotFoundException,
			RemoteException
	{
		/*
		 * TODO: Finish put.
		 */
		if (!valid(file))
		{
			throw new IOException("Bad file name: " + file);
		}
		else if (mode == Mode.ACTIVE)
		{
			log.info("put " + file + "\n" + "client port: "
					+ clientSocketAddr.getPort());

			Socket xfer = new Socket(clientSocketAddr.getAddress(),
					clientSocketAddr.getPort());

			log.info("connect to client successfully.");

			/*
			 * Prepare the input from the socket and output to the disk file.
			 */
			DataInputStream in = new DataInputStream(xfer.getInputStream());
			FileOutputStream out = new FileOutputStream(path() + file);

			// Read data from the socket and write it to the disk file
			byte[] inputByte = new byte[1024];
			int iLen = 0;
			while ((iLen = in.read(inputByte, 0, inputByte.length)) > 0)
			{
				out.write(inputByte, 0, iLen);
				out.flush();
			}

			// close stream
			in.close();
			out.close();
			xfer.close();
			/*
			 * End TODO.
			 */
		}
		else if (mode == Mode.PASSIVE)
		{
			FileOutputStream f = new FileOutputStream(path() + file);
			new Thread(new PutThread(dataChan, f)).start();
		}
	}

	public String[] dir() throws RemoteException
	{
		// List the contents of the current directory.
		return new File(path()).list();
	}

	public void cd(String dir) throws IOException, RemoteException
	{
		// Change current working directory (".." is parent directory)
		if (!valid(dir))
		{
			throw new IOException("Bad file name: " + dir);
		}
		else
		{
			if ("..".equals(dir))
			{
				if (cwd.size() > 0)
					cwd.pop();
				else
					throw new IOException("Already in root directory!");
			}
			else if (".".equals(dir))
			{
				;
			}
			else
			{
				File f = new File(path());
				if (!f.exists())
					throw new IOException("Directory does not exist: " + dir);
				else if (!f.isDirectory())
					throw new IOException("Not a directory: " + dir);
				else
					cwd.push(dir);
			}
		}
	}

	public String pwd() throws RemoteException
	{
		// List the current working directory.
		String p = "/";
		for (Enumeration<String> e = cwd.elements(); e.hasMoreElements();)
		{
			p += e.nextElement() + "/";
		}
		return p;
	}

	private String path() throws RemoteException
	{
		return pathPrefix + pwd();
	}

	public void port(InetSocketAddress s)
	{
		log.info("port: " + s.toString());
		makeActive(s);
	}

	public InetSocketAddress pasv() throws IOException
	{
		return makePassive();
	}

}
