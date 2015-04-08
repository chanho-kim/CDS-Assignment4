import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class Server {
		
	public static void main(String args[]){
		TCP server = new TCP(args[0]);
		Thread t = new Thread(server);
		t.start();
	}	
	
}

class TCP implements Runnable {

	ServerSocket listener;
	Socket[] link;
	BufferedReader[] dataIn;
	PrintWriter[] dataOut;
	static AtomicIntegerArray books;
	int serverID;
	int numServer;
	int numBooks;
	String[] ip;
	boolean crash;
	int k;
	int duration;
	LamportMutex mutex;
	
		
	public TCP(String s) {
		init(s);
	}
	
	void init(String s){
		try {
			Scanner scanner = new Scanner(new File(s));
			serverID = scanner.nextInt();
			numServer = scanner.nextInt();
			numBooks = scanner.nextInt();
			
			scanner.nextLine();
			ip = new String[numServer+1];
			for(int i = 1; i < numServer+1; i+=1){
				ip[i] = scanner.nextLine();
			}
			
			link = new Socket[numServer+1];
			dataIn = new BufferedReader[numServer+1];
			dataOut = new PrintWriter[numServer+1];
			books = new AtomicIntegerArray(numBooks+1);
			
			crash = false;
			if(scanner.hasNextLine()){
				if(scanner.next().equals("crash")){
					crash = true;
					k = scanner.nextInt();
					duration = scanner.nextInt();
				}
			}
			
			scanner.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("Initialization File Not Found!");
			System.exit(0);
		} 
	}

	void setup(){
		String[] local, remote;
		
		local = ip[serverID].split(":");
		try {
			listener = new ServerSocket(Integer.parseInt(local[1]), 10, InetAddress.getByName(local[0]));
		} catch (NumberFormatException | IOException e) {
			System.out.println("ServerSocket could not be opened!");
			System.exit(0);
		}
		
		for(int i=1; i < numServer + 1; i+=1){
			//accepting connections from smaller processes
			if(i > serverID){
				try {
					Socket s = listener.accept();
					BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String getline = br.readLine();
					StringTokenizer st = new StringTokenizer(getline);
					int hisId = Integer.parseInt(st.nextToken());
					int destId = Integer.parseInt(st.nextToken());
					String tag = st.nextToken();
					if(tag.equals("hello")){
						link[hisId] = s;
						dataIn[hisId] = br;
						dataOut[hisId] = new PrintWriter(s.getOutputStream());
					}
				} catch (IOException e) {
					System.out.println("Failed to establish connection with other servers!");
					System.exit(0);
				}
			}
			//contacting bigger processes
			if(i < serverID){
				remote = ip[i].split(":");
				try {
					link[i] = new Socket(InetAddress.getByName(remote[0]), Integer.parseInt(remote[1]));
					dataOut[i] = new PrintWriter(link[i].getOutputStream());
					dataIn[i] = new BufferedReader(new InputStreamReader(link[i].getInputStream()));
					dataOut[i].println(serverID + " " + i + " " + "hello" + " " + "null");
					dataOut[i].flush();
				} catch (NumberFormatException | IOException e) {
					System.out.println("Failed to establish connection with other servers!");
					System.exit(0);
				}
			}
		}
		
		mutex = new LamportMutex(numServer, serverID, dataOut, dataIn);
		
		//initialize and start the handler that will manage the messages
		ServerHandler h = new ServerHandler(numServer, serverID, dataOut, dataIn, mutex, listener);
		Thread thread = new Thread(h);
		thread.start();
	}
	
	void start(){
		while(true){
			try{
				Socket s = listener.accept();
				BufferedReader dIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
				String getline = dIn.readLine();
				StringTokenizer st = new StringTokenizer(getline);
				int hisId = Integer.parseInt(st.nextToken());
				int destId = Integer.parseInt(st.nextToken());
				String tag = st.nextToken();
	            //System.out.println("Message Received: " + getline);
				
				//if the other server wants to reconnect, send it the copy of our library
				if(tag.equals("reconnect")){
					link[hisId] = s;
					dataIn[hisId] = dIn;
					dataOut[hisId] = new PrintWriter(s.getOutputStream());
					
					String bookStatus = String.valueOf(numBooks);
					for (int i = 1; i < numBooks + 1; i+=1){
						bookStatus = bookStatus + " " + books.get(i);
					}
					dataOut[hisId].println(bookStatus);
					dataOut[hisId].flush();
				}
				else{
					//System.out.println("Client wants to connect!");
					ClientHandler h = new ClientHandler(s, mutex, k, duration, crash);
					Thread thread = new Thread(h);
					thread.start();
				}
			} catch(Exception e){
	        	try
	        	{
	        		Thread.sleep(duration);
	        		reconnect();
	        	}
	        	catch (Exception h)
	        	{
	        		System.out.println("Server " + serverID + " was not able to sleep");
	        	}
			}
		}
	}
	
	private void reconnect(){
		String[] local = ip[serverID].split(":");
		try {
			listener = new ServerSocket(Integer.parseInt(local[1]), 10, InetAddress.getByName(local[0]));
		} catch (NumberFormatException | IOException e) {
		}
		for(int i = 1; i < numServer + 1; i+=1){
			if(i != serverID){
				String[] remote = ip[i].split(":");
				try{
					link[i] = new Socket(InetAddress.getByName(remote[0]), Integer.parseInt(remote[1]));
					dataOut[i] = new PrintWriter(link[i].getOutputStream());
					dataIn[i] = new BufferedReader(new InputStreamReader(link[i].getInputStream()));
					
					dataOut[i].println(serverID + " " + i + " reconnect " + '\n');
					dataOut[i].flush();
					
					String in = dataIn[i].readLine();
					String[] tokens = in.split(" ");
					
					books = new AtomicIntegerArray(Integer.parseInt(tokens[0])+1);
					for(int j = 1; j < books.length(); j+=1){
						books.set(j, Integer.parseInt(tokens[j]));
					}
				} catch(Exception e){
	        		System.out.println("Server " + serverID + " failed to reconnect with Server " + i);
	        		i--;
				}
			}
		}
		
		mutex = new LamportMutex(numServer, serverID, dataOut, dataIn);
		
		ServerHandler h = new ServerHandler(numServer, serverID, dataOut, dataIn, mutex, listener);
		Thread thread = new Thread(h);
		thread.start();
	}
	
	void close(){
		try{
			for (int i = 1; i < numServer + 1; i+=1){
				if (i != serverID){
					if (dataOut[i] != null){
						dataOut[i].close();
					}
					if (dataIn[i] != null){
						dataIn[i].close();
					}
					if (link[i] != null){
						link[i].close();
					}
				}
			}
			listener.close();
		}
        catch (Exception e)
        {
        	System.out.println("Failed to close Server " + serverID);
        }	
	}
	
	public void run(){
		setup();
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		start();
		close();
	}
	
	class ServerHandler implements Runnable{
		
	    private BufferedReader[] dIn;
	    private int ID;
	    private int numServer;
	    private LamportMutex mutex;
	    private ServerSocket listener;
	    

	    public ServerHandler(int numServer, int id, PrintWriter[] dOut, BufferedReader[] dIn, LamportMutex mutex, ServerSocket socket){
	        this.numServer = numServer;
	        ID = id;
	        this.dIn = dIn;
	        this.mutex = mutex;
	        listener = socket;
	    }
	    	
		public void run() {
	    	int i = 1;
	    	while (mutex.getClosedServer() != ID)
	    	{
	    		if (i > numServer){
	    			i = 1;
	    		}
	    		else if (i == ID){
	    			i++;
	    		}
	    		else{
		    		try{
		    			// Check if data is found
						if (dIn[i].ready()){
							Msg msg = mutex.receiveMsg(i);
							if(msg != null) mutex.handleMsg(msg);
						}
					}
		    		catch (IOException e){
					}
		    		i++;
	    		}
	    	}	
	    	closeServer();	
		}
		
		private void closeServer(){
			try{
				listener.close();
			}
	        catch (Exception e){
	        	System.out.println("Failed to close server!");
	        }		
		}
	}
	
	class ClientHandler implements Runnable{
		
		Socket s;
		LamportMutex mutex;
		int numCommand;
		long sleep;
		boolean hasSleep;
		
		public ClientHandler(Socket s, LamportMutex mutex, int numCommand, long sleep, boolean hasSleep){
			this.s = s;
			this.mutex = mutex;
			this.numCommand = numCommand;
			this.sleep = sleep;
			this.hasSleep = hasSleep;
		}
		
		private synchronized void handleClient(){
			try {
				DataOutputStream send = new DataOutputStream(s.getOutputStream());
				BufferedReader receive = new BufferedReader(new InputStreamReader(s.getInputStream()));
				send.writeBytes("Server ack" + '\n');
				send.flush();
				
				boolean stop = false;
				String in;
				
				while((in = receive.readLine()) != null && !stop){
					
					//System.out.println("received: " + in);
					
					//read the line and parse it
					String[] command = in.split(" ");
					int bookNum = Integer.parseInt(command[1].substring(1));
					
					mutex.requestCS();
					
					//if client wants to reserve and book is available,
					if(command[2].equals("reserve") && TCP.books.get(bookNum) == 0){
						//set it to that client's number
						TCP.books.set(bookNum, Integer.parseInt(command[0].substring(1)));
						send.writeBytes(command[0] + " " + command[1] + '\n');
						send.flush();
						mutex.updateReserve(bookNum);
					}
					
					//if client wants to return a book and he/she owns it,
					else if(command[2].equals("return") && TCP.books.get(bookNum) == Integer.parseInt(command[0].substring(1))){
						//set it back to 0
						TCP.books.set(bookNum, 0);
						send.writeBytes("free " + command[0] + " " + command[1] + '\n');
						send.flush();
						mutex.updateReturn(bookNum);
					}
					
					//if nothing,
					else{
						send.writeBytes("fail " + command[0] + " " + command[1] + '\n');
						send.flush();
					}
					
					mutex.releaseCS();
					
					if(hasSleep) numCommand--;
					if(hasSleep && numCommand <= 0){
						mutex.shutdown();
						try {
							Thread.sleep(duration);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						stop = true;
					}
				}
				
			} catch (Exception e) {
				mutex.releaseCS();
			}
		}
		
		public void run() {
			handleClient();
		}
	}
}
