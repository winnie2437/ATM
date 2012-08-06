package terminal;

public abstract class Cassette extends CashSlice implements Comparable<Cassette>
{
	private int mBalance;		
	private int mCapacity = 10000;	

//	public int hashCode()
//	{
//		return getNominal().value();
//	}

//	public boolean equals(Object obj)
//	{
//		return (getCurrency() == ((Cassette)obj).getCurrency()) && (getNominal() == ((Cassette)obj).getNominal());
//		//return false;
//	}

	
	public int compareTo(Cassette ct)
	{
		if (Math.signum(this.getBalance()) < Math.signum(ct.getBalance())) return -1;
		if (Math.signum(this.getBalance()) > Math.signum(ct.getBalance())) return 1;
		
		if (this.getNominalValue() == null) return 1;		
		if (ct.getNominalValue() == null) return -1;		
		
		if (this.getNominalValue() < ct.getNominalValue()) return -1;
		if (this.getNominalValue() > ct.getNominalValue()) return 1;
		
		if (this.getBalance() < ct.getBalance()) return -1;		
		if (this == ct) return 0;		
		return 1;
	}
	
	protected Cassette clone() throws CloneNotSupportedException 
	{
		return (Cassette)super.clone();
	}

	int getBalance()
	{
		return mBalance;
	}

	int getResidualCapacity()
	{
		return mCapacity - mBalance;
	}
	
	CashSlice getBalanceAsCashSlice()
	{
		return cloneQty(mBalance);
	}
	
	
	protected CashSlice withdraw(int vQty) throws ImpossibleOperationException 
	{
		if (!(this instanceof CashOutEnabled)) throw new ImpossibleOperationException("CashOut not supported");
		if (vQty > mBalance) throw new ImpossibleOperationException("Qty requested is greater than cassette's balance");
		
		//if (vQty == 3) throw new ImpossibleOperationException("testing exception");		
		
		mBalance = mBalance - vQty;
		//return new CashSlice(this.getCurrency(),this.getNominal(),vQty);
		return cloneQty(vQty);
	}
	
	protected void deposit(int vQty) throws ImpossibleOperationException
	{
		if (!(this instanceof CashInEnabled)) throw new ImpossibleOperationException("Cash In not supported");
		if (mBalance + vQty > mCapacity) throw new ImpossibleOperationException("Insufficient capacity");
		
		mBalance = mBalance + vQty; 
	}
	
	
	Cassette(CashSlice vSlice)
	{
		super(vSlice);
		mBalance = vSlice.getQty();   
	}
}
