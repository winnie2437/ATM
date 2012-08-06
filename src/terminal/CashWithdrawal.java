package terminal;

import java.util.List;

import monetary.Currency;


public interface CashWithdrawal 
{
	boolean withdrawalTransaction(Currency vCurrency, int vAmount, List<CashSlice> pBundle) throws CashOperationException, AtmException;	
}

