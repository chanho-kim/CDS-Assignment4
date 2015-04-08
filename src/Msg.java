
public class Msg 
{
	int srcId;
	int destId;
	String tag;
	String msgBuf;
	
	public Msg(int s, int d, String t, String m)
	{
		srcId = s;
		destId = d;
		tag = t;
		msgBuf = m;
	}
	
	public int getSrcId()
	{
		return srcId;
	}
	
	public int getDestId()
	{
		return destId;
	}
	
	public String getTag()
	{
		return tag;
	}
	
	public String getMessage()
	{
		return msgBuf;
	}
	
	public int getMessageInt()
	{
		return Integer.parseInt(msgBuf);
	}
		
	public String toString()
	{
		return srcId + " " + destId + " " + tag + " " + msgBuf;
	}
}
