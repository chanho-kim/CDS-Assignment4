import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client implements Runnable{
	
	int clientID;
	int numServer;
	String[] ip;
	ArrayList<String> command;
	int serverPointer;
	Socket socket;
	DataOutputStream send;
	BufferedReader receive;
	
	public static void main(String args[]){
		Client c = new Client(args[0]);
		Thread t = new Thread(c);
		t.start();
	}
	
	public Client(String s) {
		try {
			Scanner scanner = new Scanner(new File(s));
			clientID = Integer.parseInt(scanner.next().substring(1));
			numServer = scanner.nextInt();
			
			scanner.nextLine();
			ip = new String[numServer+1];
			for(int i = 1; i < numServer+1; i+=1){
				ip[i] = scanner.nextLine();
			}
			serverPointer = 1;
			
			command = new ArrayList<String>();
			while(scanner.hasNextLine()){
				command.add(scanner.nextLine());
			}
			
			scanner.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("Initialization File Not Found!");
			System.exit(0);
		}
	}
	
	void connect(){
		socket = null;
		while(socket == null){
			String[] s = ip[serverPointer].split(":");
			serverPointer += 1;
			if(serverPointer > numServer) serverPointer = 1;
			try {
				socket = new Socket(InetAddress.getByName(s[0]), Integer.parseInt(s[1]));
				//System.out.println("connected to: " + s[0] + ":" + s[1]);
				socket.setSoTimeout(100);
				send = new DataOutputStream(socket.getOutputStream());
				receive = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				send.writeBytes(0 + " " + 0 + " " + "client" + " " + '\n');
				send.flush();
				receive.readLine();
			} catch (Exception e) {
				//System.out.println("Failed to connect to the server! Retrying!");
			}
		}
	}
	
	synchronized boolean executeCommand(String command){
		try{
			//send command
		  	send.writeBytes("c" + clientID + " " + command + '\n');
		  	send.flush();
		  	
		  	//Thread.sleep(3000);
		  	//if acknowledgement comes, we're good to go
		  	System.out.println(receive.readLine());
		  	return true;
		} catch (Exception e)	{
		    close();
		    connect();
		    return false;
		}
	}
	
	void checkCommand(){
		if(socket != null){
			int i = 0;
			while(i < command.size()){
				String action = command.get(i);
				String[] s = action.split(" ");
				if(s[0].equals("sleep")){
					try {
						Thread.sleep(Integer.parseInt(s[1]));
					} catch (NumberFormatException | InterruptedException e) {
						e.printStackTrace();
					}
					i+=1;
				}
				else{
					if(s[1].equals("return") || s[1].equals("reserve")){
						if(executeCommand(action)) i+=1;
					}
				}
			}
		}
	}
	
	void close(){
		if(socket != null){
			try{
				send.close();
				receive.close();
				socket.close();
			} catch (Exception e){
			}
		}
	}
	
	public void run() {
		connect();
		checkCommand();
		close();
	}
}
