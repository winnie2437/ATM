package terminal;

class ImpossibleOperationException extends Exception 
{
	ImpossibleOperationException(String message) 
	{
		super(message);
	}
}

interface CashOutEnabled {}
interface CashInEnabled {}

