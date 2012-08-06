package terminal;

import monetary.Currency;
import monetary.Nominal;

public class CashSlice implements Cloneable
{
	private final Currency mCurrency;
	private final Nominal mNominal;
	private final int mQty;
	
	//public Integer i = new Integer(11);
	//public List<String> ls = new ArrayList<String>();
	
//	public CashSlice clone() throws CloneNotSupportedException 
//	{
//		return (CashSlice)super.clone();
//	}

//	public CashSlice clone() 
//	{
//		return new CashSlice(this);
//	}	
	
	protected CashSlice cloneQty(int vQty) 
	{
		return new CashSlice(this.mCurrency,this.mNominal,vQty);
	}	
	

	public Currency getCurrency()
	{
		return mCurrency;  	
	}

	public Integer getNominalValue()
	{
		if (mNominal == null) return null;
		return mNominal.value();  	
	}
	
	public int getQty()
	{
		return mQty;  	
	}

	
	public CashSlice(Currency vCurrency, Nominal vNominal, int vQty)
	{
		mCurrency = vCurrency;
		mNominal = vNominal;
		mQty = vQty;
	}

	public CashSlice(CashSlice vSlice)
	{
		this(vSlice.mCurrency,vSlice.mNominal,vSlice.mQty); 
	}

}
