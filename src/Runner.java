import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import monetary.Currency;
import monetary.Nominal;
import terminal.AtmCashIn;
import terminal.AtmException;
import terminal.CashOperationException;
import terminal.CashOutCassette;
import terminal.CashSlice;
import terminal.Cassette;
import terminal.RecycleCassette;
import bank.TheBank;

public class Runner 
{
	private static AtmCashIn sATM; 
	private static TheBank sBank;	
	

	public static void clientLoop() throws IOException, AtmException
	{
		Scanner lScanner = new Scanner(System.in);
		
		String lInputStr;
		String lCommand = "";

		do 
		{
			System.out.println("enter your command:");
			lInputStr = lScanner.nextLine();
			String[] lTokens = lInputStr.split("\\s");
			
			if (lTokens.length > 0) lCommand = lTokens[0];
			
			Currency lCurrency = null;
 
			switch (lCommand.charAt(0)) 
			{
			    case 'c': 
			    	if (sATM.getCardReader().isAuthorized())
			    	{
			    		sATM.getCardReader().cardOut();
			    		System.out.println("CARD OUT");
			    	}
			    	return;
			    case '?':	
			    	if (sATM.getAccountAuthorized() == null)
			    	{
			    		System.out.println("client not authenticated");
			    	}
			    	else 
			    	{
			    		System.out.println(String.format("CARD BALANCE: %s %10.2f",sATM.getAccountAuthorized().currency,((double)sATM.getAccountAuthorized().balance)/100));
			    	}
			    	System.out.println("OK");
			    	break;
			    	
			    case 'a': 
			    	List<String> lAccountsInfo = sATM.getAccountsInfoList();
			    	for (String str : lAccountsInfo) System.out.println(str);
			    	System.out.println("OK");
			    	break;
			    	
			    case 't': 
			    	List<String> lTransactionsInfo = sATM.getCardTransactions();
			    	for (String str : lTransactionsInfo) System.out.println(str);
			    	System.out.println("OK");
			    	break;

			    case 'T': 
			    	List<String> lAllTransactionsInfo = sATM.getClientTransactions();
			    	for (String str : lAllTransactionsInfo) System.out.println(str);
			    	System.out.println("OK");
			    	break;
			    	
			    case 'r': 
					if (lTokens.length > 1) 
					{
						try {lCurrency = Currency.valueOf(lTokens[1]);}
						catch (Exception e) 
						{
							System.out.println("unknown currency, please try again");
							continue;
						}
					}
			    	System.out.println(sATM.registerAccount(lCurrency));
			    	break;
			    	
			    case 'd':
			    	int lAccountToDelete = 0;			    	
					if (lTokens.length > 1) 
					{
						try {lAccountToDelete = Integer.parseInt(lTokens[1]);}
						catch (Exception e) 
						{
							System.out.println("incorrect account number, please try again");
							continue;
						}
					}
			    	System.out.println(sATM.deleteAccount(lAccountToDelete));
			    	break;
			    	
			    case 'm':
			    	int lAccountFrom = 0;			    	
					if (lTokens.length > 1) 
					{
						try {lAccountFrom = Integer.parseInt(lTokens[1]);}
						catch (Exception e) 
						{
							System.out.println("incorrect debit account number, please try again");
							continue;
						}
					}
					
			    	int lAccountTo = 0;			    	
					if (lTokens.length > 2) 
					{
						try {lAccountTo = Integer.parseInt(lTokens[2]);}
						catch (Exception e) 
						{
							System.out.println("incorrect credit account number, please try again");
							continue;
						}
					}
					
			    	int lSum = 0;
					if (lTokens.length > 3) 
					{
						try {lSum = Math.round(Float.parseFloat(lTokens[3]) * 100);}
						catch (Exception e) 
						{
							System.out.println("incorrect amount for transfer, please try again");
							continue;
						}
					}
					else
					{
						System.out.println("missing an amount for withdrawal, please try again");
						continue;
					}
					
			    	System.out.println(sATM.moneyTransfer(lAccountFrom,lAccountTo,lSum));
			    	break;
			    	
			    case '-': 
					if (lTokens.length > 1) 
					{
						try {lCurrency = Currency.valueOf(lTokens[1]);}
						catch (Exception e) 
						{
							System.out.println("unknown currency, please try again");
							continue;
						}
					}
			    	int lAmount = 0;
					if (lTokens.length > 2) 
					{
						try {lAmount = Integer.parseInt(lTokens[2]);}
						catch (Exception e) 
						{
							System.out.println("incorrect amount for withdrawal, please try again");
							continue;
						}
					}
					else
					{
						System.out.println("missing an amount for withdrawal, please try again");
						continue;
					}
					
			    	List<CashSlice> lBundle = new ArrayList<CashSlice>();
					try
					{
						if (!sATM.withdrawalTransaction(lCurrency,lAmount,lBundle)) continue;
					}
					catch (CashOperationException e)
					{
						System.out.println("ERROR : " + e.getMessage());
						continue;
					}
			    	for (CashSlice cs : lBundle) System.out.println(String.format("%d %d",cs.getNominalValue(),cs.getQty()));
			    	System.out.println("OK");
			    	break;
			    case '+':
					if (lTokens.length > 1) 
					{
						try {lCurrency = Currency.valueOf(lTokens[1]);}
						catch (Exception e) 
						{
							System.out.println("unknown currency, please try again");
							continue;
						}
					}
			    	
					Nominal lNominal = null;			    	
			    	int lQty = 0;
			    	
					if (lTokens.length > 3) 
					{
						try {lNominal = Nominal.enumOf(Integer.parseInt(lTokens[2]));}
						catch (Exception e) 
						{
							System.out.println("unknown banknote, please try again");
							continue;
						}
						
						
						try {lQty = Integer.parseInt(lTokens[3]);}
						catch (Exception e) 
						{
							System.out.println("incorrect qty for deposit, please try again");
							continue;
						}
					}
					else
					{
						System.out.println("missing nominal or/and qty for deposit, please try again");
						continue;
					}
					
					CashSlice lSlice = new CashSlice(lCurrency,lNominal,lQty);
					try
					{
						if (!sATM.depositTransaction(lSlice))
						{
							System.out.println("REJECTED: " + lInputStr);
							continue;							
						}
						
					}
					catch (CashOperationException e)
					{
						System.out.println("ERROR : " + e.getMessage());
						continue;
					}
			    	System.out.println("OK");
			    	break;
			    	
			    default: System.out.println("unknown command, please try again");
			}
			if (!sATM.getCardReader().isAuthorized())
			{
	    		System.out.println("CARD OUT");
	    		return;
			}
			
		} while (true);
	}
	
	
	public static void main(String[] args) throws IOException, AtmException, SQLException
	{
		Scanner lScanner = new Scanner(System.in);
		
		String lInputStr;
		String lCommand = "";

		boolean lExit = false;		

		sBank = TheBank.getBank(); 
		
		//Atm sATM = new Atm(9);
		sATM = new AtmCashIn(9,sBank);
		
		ArrayList<Cassette> lCassettes = new ArrayList<Cassette>();
		for (int i = 0; i < 8; i++ ) lCassettes.add(new CashOutCassette(new CashSlice(Currency.UAH,Nominal.values()[i],100)));    	
		//lCassettes.add(new CashInCassette(new CashSlice(null,null,0)));
		//lCassettes.add(new CashInCassette(new CashSlice(Currency.UAH,Nominal.ONE_HUNDRED,0)));
		lCassettes.add(new RecycleCassette(new CashSlice(Currency.USD,Nominal.ONE_HUNDRED,100)));
		
		sATM.load(lCassettes);
		
		do 
		{
			System.out.println("insert your card:");
			lInputStr = lScanner.nextLine();
			String[] lTokens = lInputStr.split("\\s");

			if (lTokens.length > 0) lCommand = lTokens[0];			
			
			switch (lCommand.charAt(0)) 
			{
			    case 'e':
			    	lExit = true;
			    	break;
			    case 'i':
			    	if (lTokens.length == 3)
			    	{
			    		if (sATM.getCardReader().cardIn(lTokens[1],lTokens[2]))
			    		{
			    			System.out.println("CARD ACCEPTED");
			    			clientLoop();
			    		}
			    		else System.out.println("CARD REJECTED"); 
			    	}
			    	else System.out.println("unknown card, try another");
			    	break;
			    case 'u':	
			    	sATM.unload();
			    	System.out.println("ATM unloaded");
			    	break;
			    case 'l':	
			    	//sATM.load(lCassettes);
			    	System.out.println("nothing to do");
			    	break;
			    case '?':	
			    	List<CashSlice> lBalances = sATM.getAtmBalances();
			    	if (lBalances == null)
			    	{
			    		System.out.println("ATM is in unloaded state");
			    	}
			    	else 
			    	{
			    		for (CashSlice cs : lBalances)	System.out.println(String.format("%s %d %d",cs.getCurrency(),cs.getNominalValue(),cs.getQty()));
			    	}
			    	System.out.println("REJECT: " + sATM.getRejectBoxState());
			    	System.out.println("OK");
			    	break;
			    default: System.out.println("unknown or wrong command, please try again");			    	
			}
		} while (!lExit);
		System.out.println("ATM is shutting down ...");
	}
}

