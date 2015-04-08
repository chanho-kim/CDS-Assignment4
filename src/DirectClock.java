public class DirectClock 
{
    public int[] clock;
    private int myId;
    
    public DirectClock(int numProc, int id) 
    {
        myId = id;
        clock = new int[numProc + 1];
        for (int i = 0; i < numProc + 1; i++) clock[i] = 0;
        clock[myId] = 1;
    }
    
    public int getValue(int i) 
    {
        return clock[i];
    }
    
    public void tick() 
    {
        clock[myId]++;
    }
    
    public void sendAction() 
    {
        tick();
    }
    
    public void receiveAction(int sender, int sentValue)
    {
        clock[sender] = Math.max(clock[sender], sentValue);
        clock[myId] = Math.max(clock[myId], sentValue) + 1;
    }
}

