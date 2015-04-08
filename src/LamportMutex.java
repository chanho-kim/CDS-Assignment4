import java.io.BufferedReader;
import java.io.PrintWriter;

public class LamportMutex extends Process implements Lock 
{
	private DirectClock v;
	private int[] q; // request queue
	private int numServer;
	private int myId;
    
    public LamportMutex(int numServer, int id, PrintWriter[] dOut, BufferedReader[] dIn) 
    {
        super(numServer, id, dOut, dIn);
        this.numServer = numServer;
        myId = id;
        v = new DirectClock(numServer, myId);
        q = new int[numServer + 1];
        for (int j = 1; j < numServer + 1; j++)
        {
            q[j] = Integer.MAX_VALUE;
        }
    }
    
    public synchronized void requestCS() 
    {
        v.tick();
        q[myId] = v.getValue(myId);
        broadcastMsg("request", q[myId]);
        while (!okayCS())
        {
            myWait();
        }
    }
    
    public synchronized void releaseCS() 
    {
        q[myId] = Integer.MAX_VALUE;
        broadcastMsg("release", v.getValue(myId));
    }
    
    private boolean okayCS() 
    {
        for (int j = 1; j < numServer + 1; j++)
        {
            if (isGreater(q[myId], myId, q[j], j))
            {
                return false;
            }
            if (isGreater(q[myId], myId, v.getValue(j), j))
            {
                return false;
            }
        }
        return true;
    }
    
    public void shutdown() 
    {
  		setClosedServer(myId);
        q[myId] = Integer.MAX_VALUE;
        broadcastMsg("shutdown", v.getValue(myId));
    }
    
    public synchronized void reconnect()
    {
        v.tick();
        q[myId] = v.getValue(myId);
        broadcastMsg("reconnect", q[myId]);
        
        // Receive acks
        for (int i = 1; i < numServer + 1; i+=1)
        {
        	if (i != myId)
        	{
        		Msg msg = receiveMsg(i);	
    			handleMsg(msg);
        	}
	    }
    
        while (!okayCS())
        {
            myWait();
        }
    }
    
    
    private boolean isGreater(int entry1, int pid1, int entry2, int pid2) 
    {
        if (entry2 == Integer.MAX_VALUE)
        {
        	return false;
        }
        return ((entry1 > entry2) || ((entry1 == entry2) && (pid1 > pid2)));
    }
    
    public synchronized void handleMsg(Msg m) 
    {
        int timeStamp = m.getMessageInt();
        int src = m.getSrcId();
        String tag = m.getTag();
        
        v.receiveAction(src, timeStamp);
        if (tag.equals("request")) 
        {
            q[src] = timeStamp;
            sendMsg(src, "ack", v.getValue(myId));
        } 
        else if (tag.equals("release"))
        {
            q[src] = Integer.MAX_VALUE;
        }
        else if (tag.equals("reserve"))
        {
        	int book = m.getMessageInt();
        	TCP.books.set(book, 1);
        }
        else if (tag.equals("return"))
        {
        	int book = m.getMessageInt();
        	TCP.books.set(book, 0);
        }
        else if (tag.equals("shutdown"))
        {
        	q[src] = Integer.MAX_VALUE;
        	closeChannel(m.getSrcId());
        }
        else if (tag.equals("reconnect"))
        {
            q[src] = timeStamp;
            sendMsg(src, "ack", v.getValue(myId));
        }
        notify(); // okayCS() may be true now
    }
    
    public synchronized void updateReserve(int book) 
    {
        broadcastMsg("reserve", book);
    }
    
    public synchronized void updateReturn(int book) 
    {
        broadcastMsg("return", book);
    }
}
