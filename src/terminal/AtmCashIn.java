package terminal;

import java.util.ArrayList;
import java.util.List;

import bank.BankException;
import bank.TheBank;

public class AtmCashIn extends Atm implements CashDeposit 
{

	// Implementation of CashDeposit Interface	
	public boolean depositTransaction(CashSlice vSlice) throws CashOperationException, AtmException
	{
    	if (getCardReader().isAuthorized() != true) return false;

    	deposit(vSlice);

    	try 
    	{
			mBank.cardCreditWithCash(mAccountAuthorized,vSlice.getQty()*vSlice.getNominalValue()*100);
		}
    	catch (BankException e) 
    	{
			throw new CashOperationException(e.getMessage());
		}
    	
    	
    	getCardReader().cardOut();
    	return true;
	}
	
	
    
	protected void deposit(CashSlice vSlice) throws CashOperationException, AtmException
	{
		List<Cassette> lCollectors = new ArrayList<Cassette>();
		for (Cassette ct : mCassettes.keySet())
		{
			if 
			( 
				ct instanceof CashInEnabled && 
				ct.getCurrency() == vSlice.getCurrency() &&
				ct.getNominalValue() == vSlice.getNominalValue() &&
				ct.getResidualCapacity() >= vSlice.getQty()
			) 
			{
				try	
				{
					ct.deposit(vSlice.getQty());
				}
				catch (ImpossibleOperationException e)
				{
	        		getRejectBox().add(vSlice);
	    			throw new CashOperationException(String.format("error in cassette #%d: %s",mCassettes.get(ct),e.getMessage()));
				}
				return;				
			}
			
			if (ct instanceof CashInEnabled && ct.getCurrency() == null && ct.getResidualCapacity() >= vSlice.getQty())
				lCollectors.add(ct);
		}
		
		for (Cassette ct : lCollectors)
		{
			try	
			{
				ct.deposit(vSlice.getQty());
			}
			catch (ImpossibleOperationException e)
			{
        		getRejectBox().add(vSlice);
    			throw new CashOperationException(String.format("error in cassette #%d: %s",mCassettes.get(ct),e.getMessage()));
			}
			return;				
		}
		
		getRejectBox().add(vSlice);
		throw new CashOperationException("no suitable cassettes to deposit");
	}
	
	public AtmCashIn(int vSlotsQty, TheBank vBank) throws AtmException
	{
		super(vSlotsQty,vBank);
	}
}
