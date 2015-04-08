import java.io.*;

public class Process 
{
	private int numServer, myId;
    private PrintWriter[] dataOut;
    private BufferedReader[] dataIn;
    private int closedServer;
    
    public Process(int numServer, int id, PrintWriter[] dOut, BufferedReader[] dIn) 
    {
        myId = id;
        this.numServer = numServer;
        dataOut = dOut;
        dataIn = dIn;
        closedServer = 0;
    }
    
    public void sendMsg(int destId, String tag, String msg) 
    {
        dataOut[destId].println(myId + " " + destId + " " + tag + " " + msg);
        //System.out.println("Sending out: " + myId + " " + destId + " " + tag + " " + msg);
        dataOut[destId].flush();
    }
    
    public void sendMsg(int destId, String tag, int msg) 
    {
        sendMsg(destId, tag, String.valueOf(msg) + " ");
    }
    
    public void sendMsg(int destId, String tag, int msg1, int msg2) 
    {
        sendMsg(destId,tag,String.valueOf(msg1) + " " + String.valueOf(msg2) + " ");
    }
    
    public void sendMsg(int destId, String tag) 
    {
        sendMsg(destId, tag, " 0 ");
    }
    
    public void broadcastMsg(String tag, int msg)
    {
        for (int i = 1; i < numServer + 1; i++)
        {
            if ((i != myId) && (dataOut[i] != null)) 
            {
            	sendMsg(i, tag, msg);
        	}
        }
    }

    public Msg receiveMsg(int fromId){
        try{
        	String getLine = dataIn[fromId].readLine();
        	if(getLine.equals("")) return null;
            //System.out.println(getLine);
            String tokens[] = getLine.split(" ");
            int srcId = Integer.parseInt(tokens[0]);
            int destId = Integer.parseInt(tokens[1]);
            String tag = tokens[2];
            String msg = tokens[3];
            
            return new Msg(srcId, destId, tag, msg);
        } 
        catch (IOException e){
            return null;
        }
    }
  
    public void closeChannel(int fromId)
    {
    	dataOut[fromId].flush();
    	try
    	{
    		dataIn[fromId].close();
    		dataOut[fromId].close();
    	}
    	catch (Exception e)
    	{
    		System.out.println("Server " + myId + " was not able to close communication from server " + fromId);
    	}
    }
    
    public int getClosedServer()
    {
    	return closedServer;
    }
    
    public void setClosedServer(int server)
    {
    	closedServer = server;
    }
    
    public synchronized void myWait() 
    {
        try 
        {
        	//System.out.println("Gotta wait..");
            wait();
        } 
        catch (InterruptedException e) 
        {
        	System.err.println(e);
        }
    }
}
