package monetary;

public enum Nominal 
{ 
	ONE(1), FIVE(5), 
	TEN(10), FIFTY(50), 
	ONE_HUNDRED(100), FIVE_HUNDRED(500),
	ONE_THOUSAND(1000), FIVE_THOUSAND(5000);
	
	private int mValue;

	public static  Nominal enumOf(int vValue)
	{
		switch (vValue)
		{
		    case    1: return ONE;
		    case    5: return FIVE;		    
		    case   10: return TEN;
		    case   50: return FIFTY;
		    case  100: return ONE_HUNDRED;
		    case  500: return FIVE_HUNDRED;
		    case 1000: return ONE_THOUSAND;
		    case 5000: return FIVE_THOUSAND;
		    default: throw new IllegalArgumentException(vValue + " is not valid nominal");
		}
	}
	
	
	public int value()
	{
		return mValue;
	}
	
	private Nominal(int vValue)
	{
	    mValue = vValue;
	}
}