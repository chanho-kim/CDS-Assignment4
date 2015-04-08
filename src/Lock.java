public interface Lock 
{
    public void requestCS(); //may block
    public void releaseCS();
}
