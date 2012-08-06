package bank;

import monetary.Currency;

public class Account 
{
	final public int ID;
	final public Currency currency;
	final public int balance;
	
	public Account(int vID, Currency vCurrency, int vBalance)
	{
		ID = vID;
		currency = vCurrency;
		balance = vBalance;
	}

}
