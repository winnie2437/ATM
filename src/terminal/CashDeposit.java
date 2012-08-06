package terminal;

public interface CashDeposit 
{
	boolean depositTransaction(CashSlice vSlice) throws CashOperationException, AtmException;
}
