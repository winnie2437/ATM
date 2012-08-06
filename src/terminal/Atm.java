package terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import monetary.Currency;
import bank.Account;
import bank.BankException;
import bank.TheBank;



public class Atm implements CashWithdrawal 
{
	public class CardReader   
	{
		//private String mCardNo = null;		
		//private String mPIN = null;
		private boolean mAuthorized = false;		

		public boolean isAuthorized()
		{
			return mAuthorized;
		}
		
		public boolean cardIn(String vCardNo, String vPIN)
		{
			if (mAuthorized) return false; 
				
			//mCardNo = vCardNo;		
			//mPIN = vPIN;
			
			try
			{
			    mAccountAuthorized = mBank.cardAuthorize(vCardNo, vPIN);
			}
			catch (Exception e)
			{
				mAccountAuthorized = null;
			}
			
			if (mAccountAuthorized != null)
			{
				mAuthorized = true;
				return true;
			}
			else 
			{
				cardOut();
				return false;
			}	
		}

		public void cardOut()
		{
			mAccountAuthorized = null;
			mAuthorized = false;			
			//mCardNo = null;		
			//mPIN = null;
		}
	}
	
	protected class RejectBox   
	{
		private int mBalance = 0;		
		private int mCapacity = 20000;
		
		List<List<CashSlice>> mList = new ArrayList<List<CashSlice>>(); 
		
		protected void add(List<CashSlice> vBundle) throws AtmException
		{
			int lQty = 0;
			for (CashSlice cs : vBundle) lQty = lQty + cs.getQty();
			if (mCapacity - mBalance < lQty) throw new AtmException("RejectBox overflow");
			
			mList.add(vBundle);
			mBalance = mBalance + lQty;
		}
		
		protected void add(CashSlice vCashSlice) throws AtmException
		{
			List<CashSlice> lList = new ArrayList<CashSlice>();
			lList.add(vCashSlice);
			add(lList);
		}
	}

	private int mSlotsQty;
	protected Map<Cassette,Integer> mCassettes = null;
	private RejectBox mRejectBox = new RejectBox(); 
	private CardReader mCardReader = new CardReader();
	protected Account mAccountAuthorized;
	protected TheBank mBank;

	public CardReader getCardReader()
	{
		return mCardReader;
	}
	
	
	protected RejectBox getRejectBox()
	{
		return mRejectBox;
	}
	
	public String getRejectBoxState()
	{
		return String.format("%d of %d",mRejectBox.mBalance,mRejectBox.mCapacity);
	}
	
    public List<CashSlice> getAtmBalances()
    {
    	if (mCassettes == null) return null;
    	
    	List<CashSlice> rBalances = new ArrayList<CashSlice>();

    	Map<Cassette,Integer> lCassettes = new TreeMap<Cassette,Integer>(mCassettes);
    	
        for (Map.Entry<Cassette,Integer> pt : lCassettes.entrySet())   			
   		{
   			rBalances.add(pt.getKey().getBalanceAsCashSlice());
   			//System.out.println(String.format("%d : %d : %s %d %d",pt.getValue(),pt.getKey().hashCode(),pt.getKey().getCurrency(),pt.getKey().getNominalValue(),pt.getKey().getBalance()));
   		}
   		//System.out.println();
    	return rBalances;
    }
	
    public void load(List<Cassette> vList) throws AtmException
	{
		if (mCassettes != null) throw new AtmException("Atm must be unloaded first");		
		if (vList.size() > mSlotsQty) throw new AtmException("This atm does't have enough slots");

		mCassettes = new HashMap<Cassette,Integer>();
		try
		{
			int i = 0;
			for (Cassette ct : vList) mCassettes.put(ct.clone(),i++);
		}
		catch (CloneNotSupportedException e)
		{
			throw new AtmException("Cassette is not suitable for this ATM (clone() failed)");
		}
	}

	public void unload() 
	{
		// very simple unload ...
		mCassettes = null;
	}
	

	protected SortedMap<Cassette,Integer> getOrderedCassettes(Currency vCurrency)
	{
		SortedMap<Cassette,Integer> rMap = new TreeMap<Cassette,Integer>
		    ( 
		        new Comparator<Cassette>() 
				{
			       public int compare(Cassette c1,  Cassette c2)
			       {
			    	   if (c1.getNominalValue() > c2.getNominalValue()) return -1;
			    	   //if (c1.getNominal().value() == c2.getNominal().value()) return 0;
			    	   return 1;
			       }
			       public boolean equals(Object obj)  { return this == obj; }
				}
		    );

		for (Cassette ct : mCassettes.keySet())
		{
			if ( 
				 ct instanceof CashOutEnabled && 
				 ct.getCurrency() == vCurrency && 
				 ct.getBalance() != 0
				) 
				rMap.put(ct,0);
		}
		return rMap;
	}
	
	
    protected boolean calculateWithdrawMap(SortedMap<Cassette,Integer> pMap, int vAmount)  
    {
    	int lRemainder = vAmount;
    	
    	for (Map.Entry<Cassette,Integer> pair : pMap.entrySet())
    	{
    		pair.setValue(Math.min(lRemainder / pair.getKey().getNominalValue() , pair.getKey().getBalance()));
    		lRemainder = lRemainder - (pair.getValue() * pair.getKey().getNominalValue());
    		
    		if (lRemainder == 0) break; // try to delete not used cassettes
    	}
    	return (lRemainder == 0);
    }

    public Account getAccountAuthorized() 
    {
    	if (getCardReader().isAuthorized() != true) return null;
    	return mAccountAuthorized;
    }
    

	public List<String> getAccountsInfoList()
	{
		List<String> rList = new ArrayList<String>();
		
    	if (getCardReader().isAuthorized() != true)
    	{
    		rList.add("card not authorized");
    		return rList;
    	}
    	
    	try 
    	{
			mBank.getAccountsInfoList(mAccountAuthorized, rList);
		} 
    	catch (BankException e) 
    	{
			rList.clear();
			rList.add("ERROR:" + e.getMessage());
		}
    	
    	return rList;
	}
    
	public List<String> getCardTransactions() 
	{
		List<String> rList = new ArrayList<String>();
		
    	if (getCardReader().isAuthorized() != true)
    	{
    		rList.add("card not authorized");
    		return rList;
    	}
    	
    	try 
    	{
			mBank.getAccountTransactions(mAccountAuthorized, rList);
		} 
    	catch (BankException e) 
    	{
			rList.clear();
			rList.add("ERROR:" + e.getMessage());
		}
    	
    	return rList;
   	}
	

	public List<String> getClientTransactions() 
	{
		List<String> rList = new ArrayList<String>();
		
    	if (getCardReader().isAuthorized() != true)
    	{
    		rList.add("client  not authenticated");
    		return rList;
    	}
    	
    	try 
    	{
			mBank.getAllAccountsTransactions(mAccountAuthorized, rList);
		} 
    	catch (BankException e) 
    	{
			rList.clear();
			rList.add("ERROR:" + e.getMessage());
		}
    	
    	return rList;
   	}
	
	
	public String deleteAccount(int vAccountToDelete)
	{
    	if (getCardReader().isAuthorized() != true) return "card not authorized";
    	
    	try 
    	{
    		mBank.deleteAccount(vAccountToDelete,mAccountAuthorized);
			return "DELETE OK"; 
		}
    	catch (BankException e) 
    	{
			return "ERROR:" + e.getMessage();
		}
	}
	
	public String registerAccount(Currency vCurrency)
	{
    	if (getCardReader().isAuthorized() != true) return "card not authorized";
    	
    	try 
    	{
			return "REGISTER OK ; new account number: " + mBank.registerAccount(vCurrency,mAccountAuthorized);
		}
    	catch (BankException e) 
    	{
			return "ERROR:" + e.getMessage();
		}
	}
	
	
    public String moneyTransfer(int vAccountFrom, int vAccountTo, int vAmount) 
    {
    	if (getCardReader().isAuthorized() != true) return "card not authorized";
    	
    	try 
    	{
			mBank.transaction(vAccountFrom,vAccountTo,vAmount,mAccountAuthorized);
			return "TRANSFER OK";
		}
    	catch (BankException e) 
    	{
			return "ERROR:" + e.getMessage();
		}
    }
	
	
	// Implementation of CashWithdrawal Interface
    public boolean withdrawalTransaction(Currency vCurrency, int vAmount, List<CashSlice> vBundle) throws CashOperationException, AtmException
    {
    	if (getCardReader().isAuthorized() != true) return false;
    	
    	if (mAccountAuthorized.currency != vCurrency) throw new CashOperationException(String.format("%s currency is not allowed with this card",vCurrency)); 
    	
    	try 
    	{
			mBank.cardDebitForCash(mAccountAuthorized,vAmount*100);
		}
    	catch (BankException e) 
    	{
			throw new CashOperationException(e.getMessage());
		}

    	withdraw(mAccountAuthorized.currency,vAmount,vBundle);

    	getCardReader().cardOut();
    	return true;
    }
	

    private void withdraw(Currency vCurrency, int vAmount, List<CashSlice> vBundle) throws CashOperationException, AtmException
    {
    	if (mCassettes == null) throw new CashOperationException("Atm is in unloaded state");    	
    	
    	SortedMap<Cassette,Integer> lMap = getOrderedCassettes(vCurrency);
 
    	if (!calculateWithdrawMap(lMap,vAmount)) throw new CashOperationException("unable to withdraw requested amount");    	
    	
    	//List<CashSlice> rBundle = new ArrayList<CashSlice>();
    	 
    	for (Map.Entry<Cassette,Integer> pair : lMap.entrySet())
    	{
    		try
        	{
        		if (pair.getValue() != 0 ) vBundle.add(pair.getKey().withdraw(pair.getValue()));
        	}
        	catch (ImpossibleOperationException e) 
        	{
        		getRejectBox().add(vBundle);
    			throw new CashOperationException(String.format("error in cassette #%d: %s",mCassettes.get(pair.getKey()),e.getMessage()));
    		}
		} 
    }

	public Atm(int vSlotsQty, TheBank vBank) throws AtmException
	{
		mSlotsQty = vSlotsQty;
		
		mBank = vBank;
		if (mBank == null) throw new AtmException("no bank connection");
	}
    
}
