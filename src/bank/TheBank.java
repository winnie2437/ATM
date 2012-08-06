package bank;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import monetary.Currency;
import com.mysql.jdbc.Driver;

public class TheBank 
{
	private static TheBank sBank = null;
    Connection mConnection;	
	
	private TheBank()
	{
	}

	public static TheBank getBank() 
	{
		try 
		{
			Class.forName("com.mysql.jdbc.Driver");
		} 
		catch (ClassNotFoundException e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		if (sBank == null)  
		{
			try
			{
				sBank = new TheBank();
				sBank.initDB();
				sBank.init();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				sBank = null;
			}
		}
		return sBank;
	}
	
	protected void finalize() throws Throwable
	{
		super.finalize();
		try
		{
		    mConnection.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();			
		}
		
	}
	
	public void init() throws SQLException
	{
	    Properties lProps = new Properties();
	    lProps.put("user", "root");
	    lProps.put("password", "000");
		
	    String lURL = "jdbc:" + "mysql" + "://" + "localhost" +  ":" + "3306" + "/";
	    mConnection = DriverManager.getConnection(lURL, lProps);
	    mConnection.setCatalog("atmdb");
	}
	
	
	public Account cardAuthorize(String vCard, String vPIN) throws BankException
	{
		try 
		(
			Statement lStatement = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 
			ResultSet lSet1 = lStatement.executeQuery(String.format("select ACCOUNT_ID from CARDS WHERE CARD_ID = %s AND PIN = '%s'", vCard,vPIN));
		)			
		{
		    if (!lSet1.next()) throw new BankException("wrong card or PIN");
		    int lID = lSet1.getInt("ACCOUNT_ID");
		    
		    try (ResultSet lSet2 = lStatement.executeQuery(String.format("select CURRENCY, BALANCE from ACCOUNTS WHERE ACCOUNT_ID = %d", lID));) 
		    {
			    if (!lSet2.next()) throw new BankException("INTERNAL ERROR - card account not found");
			    return new Account(lID,Currency.valueOf(lSet2.getString("CURRENCY")),lSet2.getInt("BALANCE"));
		    }
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in cardAuthorize: " + e.getMessage());
		}
	}
	
	public void deleteAccount(int vAccountToDelete, Account vAccountBase) throws BankException
	{
		try	(Statement lStatement = mConnection.createStatement())			
		{
	    	if (lStatement.executeUpdate(String.format("delete from ACCOUNTS where ACCOUNT_ID = %1$d and BALANCE = 0 AND not exists(select * from TRANSACTIONS where ACCOUNT_ID_1 = %1$d or ACCOUNT_ID_2 = %1$d)",vAccountToDelete)) != 1) 
	    		throw new BankException("account not found, not empty or not clean(dependencies exist)");;
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in cardAuthorize: " + e.getMessage());
		}
	}
	

	public int registerAccount(Currency vCurrency, Account vAccountBase) throws BankException
	{
		int lClientID;
		
		try 
		(
			Statement lStatement = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 
			ResultSet lSet = lStatement.executeQuery(String.format("select CLIENT_ID from ACCOUNTS WHERE ACCOUNT_ID = %d",vAccountBase.ID));
		)			
		{
		    if (!lSet.next()) throw new BankException("INTERNAL ERROR - base account not found");
		    lClientID = lSet.getInt("CLIENT_ID");
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in registerAccount: " + e.getMessage());
		}
		
		
		try	(Statement lStatement = mConnection.createStatement())			
		{
	    	//lStatement.executeUpdate(String.format("insert into ACCOUNTS values(0,%d,'%s',0)",lClientID,vCurrency));		    
	    	lStatement.executeUpdate(String.format("insert into ACCOUNTS values(0,%d,'%s',0)",lClientID,vCurrency),Statement.RETURN_GENERATED_KEYS);

	    	try (ResultSet lSet = lStatement.getGeneratedKeys())
	    	{
		    	if (lSet != null && lSet.next()) return lSet.getInt(1);
		    	else throw new BankException("INTERNAL ERROR - unable to return ACCOUNT_ID");
	    	}
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in cardAuthorize: " + e.getMessage());
		}
	}
	
	
	public void transaction(int vAccountFrom, int vAccountTo, int vAmount, Account vCardAccount) throws BankException
	{
	    if (vAmount <= 0) throw new BankException("wrong usage");		
	    //if (vAccountFrom.currency != vAccountTo.currency) throw new BankException("currency mismatch");		
		
		try 
		(
			Statement lStatementC = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 
			ResultSet lSet = lStatementC.executeQuery(String.format("SELECT COUNT(DISTINCT CLIENT_ID) AS DIFF FROM ACCOUNTS WHERE ACCOUNT_ID = %d OR ACCOUNT_ID = %d", vAccountFrom,vCardAccount.ID));
		)			
		{
		    if (!lSet.next()) throw new BankException("something wrong with database");
			if (lSet.getInt("DIFF") != 1) throw new BankException("ILLEGAL ACCOUNT ACCESS, fraud suspected");
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in cardAuthorize: " + e.getMessage());
		}
	    
		Statement lStatementA = null;		
		Statement lStatementT = null;		
		try 
		{   // deadlocks are possible !!! try it later
			
			mConnection.setAutoCommit(false);
			lStatementA = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			
			ResultSet lSetFrom = lStatementA.executeQuery(String.format("select ACCOUNT_ID, CURRENCY, BALANCE from ACCOUNTS WHERE ACCOUNT_ID = %d  FOR UPDATE", vAccountFrom));
		    if (!lSetFrom.next()) throw new BankException("INTERNAL ERROR - debit account not found");
		    Currency lCurrencyFrom = Currency.valueOf(lSetFrom.getString("CURRENCY"));
		    //if (Currency.valueOf(lSetFrom.getString("CURRENCY")) != vAccountFrom.currency) throw new BankException("currency check 1 failed, fraud suspected");
		    int lBalance = lSetFrom.getInt("BALANCE");
		    if (lBalance < vAmount) throw new BankException("insufficient funds on debit account"); 
		    lSetFrom.updateInt("BALANCE", lBalance - vAmount );
		    lSetFrom.updateRow();
		    
			ResultSet lSetTo = lStatementA.executeQuery(String.format("select ACCOUNT_ID, CURRENCY, BALANCE, CLIENT_ID from ACCOUNTS WHERE ACCOUNT_ID = %d  FOR UPDATE", vAccountTo));		    
			if (!lSetTo.next()) throw new BankException("INTERNAL ERROR - credit account not found");
			if (lSetTo.getInt("CLIENT_ID") == 0) throw new BankException("ILLEGAL ACCOUNT ACCESS 2, fraud suspected");			
		    Currency lCurrencyTo = Currency.valueOf(lSetTo.getString("CURRENCY"));			
		    //if (Currency.valueOf(lSetTo.getString("CURRENCY")) != vAccountTo.currency) throw new BankException("currency check 2 failed, fraud suspected");			
		    if (lCurrencyFrom != lCurrencyTo) throw new BankException("currency mismatch, fraud suspected");		    
 			
			lSetTo.updateInt("BALANCE", lSetTo.getInt("BALANCE") + vAmount);
			lSetTo.updateRow();

			lStatementT = mConnection.createStatement();
		    lStatementT.executeUpdate(String.format("insert into TRANSACTIONS values(0,%d,%d,%d,null)",vAccountFrom,vAccountTo,vAmount));
			
			mConnection.commit();		    
		}
		catch (Exception e)
		{
			try {mConnection.rollback();} catch (SQLException e1) {e1.printStackTrace();}			
			if (e instanceof BankException) throw (BankException)e;
			else e.printStackTrace();
		}
		finally
		{
			if (lStatementT != null) try {lStatementT.close();} catch (SQLException e) {e.printStackTrace();}			
			if (lStatementA != null) try {lStatementA.close();} catch (SQLException e) {e.printStackTrace();}			
			try {mConnection.setAutoCommit(true);} catch (SQLException e) {e.printStackTrace();}		
		}
	}
	
	
	public void cashTransaction(Account vAccount, int vAmount) throws BankException
	{
		Statement lStatementA = null;		
		Statement lStatementT = null;		
		try 
		{
			mConnection.setAutoCommit(false);
			lStatementA = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			
			ResultSet lAccountSet1 = lStatementA.executeQuery(String.format("select ACCOUNT_ID, BALANCE from ACCOUNTS WHERE ACCOUNT_ID = %d  FOR UPDATE", vAccount.ID));
		    if (!lAccountSet1.next()) throw new BankException("INTERNAL ERROR - card account not found");
		    int lAccount1 = lAccountSet1.getInt("ACCOUNT_ID"); 
		    int lBalance = lAccountSet1.getInt("BALANCE");
		    if (lBalance < vAmount) throw new BankException("insufficient funds on account"); 
		    lAccountSet1.updateInt("BALANCE", lBalance - vAmount );
		    lAccountSet1.updateRow();
		    	
			ResultSet lAccountSet2 = lStatementA.executeQuery(String.format("select ACCOUNT_ID, BALANCE from ACCOUNTS WHERE CLIENT_ID = 0  and CURRENCY = '%s'  FOR UPDATE", vAccount.currency.toString()));
			if (!lAccountSet2.next()) throw new BankException("INTERNAL ERROR - currency not supported by the bank");
		    int lAccount2 = lAccountSet2.getInt("ACCOUNT_ID");			
			lAccountSet2.updateInt("BALANCE", lAccountSet2.getInt("BALANCE") + vAmount);
			lAccountSet2.updateRow();

			lStatementT = mConnection.createStatement();
			if (vAmount > 0)
			{
			    lStatementT.executeUpdate(String.format("insert into TRANSACTIONS values(0,%d,%d,%d,null)",lAccount1,lAccount2,vAmount));
			}
			else
			{
			    lStatementT.executeUpdate(String.format("insert into TRANSACTIONS values(0,%d,%d,%d,null)",lAccount2,lAccount1,-vAmount));
			}	
			
			mConnection.commit();		    
		}
		catch (Exception e)
		{
			try {mConnection.rollback();} catch (SQLException e1) {e1.printStackTrace();}			
			if (e instanceof BankException) throw (BankException)e;
			else e.printStackTrace();
		}
		finally
		{
			if (lStatementT != null) try {lStatementT.close();} catch (SQLException e) {e.printStackTrace();}			
			if (lStatementA != null) try {lStatementA.close();} catch (SQLException e) {e.printStackTrace();}			
			try {mConnection.setAutoCommit(true);} catch (SQLException e) {e.printStackTrace();}		
		}
	}
	
	
	public void cardDebitForCash(Account vAccount, int vAmount) throws BankException
	{
		cashTransaction(vAccount,vAmount);		
	}


	public void cardCreditWithCash(Account vAccount, int vAmount) throws BankException
	{
		cashTransaction(vAccount,-vAmount);		
	}
	

	public void getAccountsInfoList(Account vAccount, List<String> pList) throws BankException
	{
		try 
		(
			Statement lStatement = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 
			ResultSet lSet = lStatement.executeQuery(String.format("SELECT acl.ACCOUNT_ID, acl.CURRENCY, acl.BALANCE FROM  ACCOUNTS a LEFT OUTER JOIN ACCOUNTS acl ON a.CLIENT_ID = acl.CLIENT_ID WHERE a.ACCOUNT_ID = %d ORDER BY acl.ACCOUNT_ID", vAccount.ID));
		)			
		{
		    while (lSet.next()) 
		    {
		    	pList.add(String.format("%3d %3s %10.2f",lSet.getInt("ACCOUNT_ID"),lSet.getString("CURRENCY"),lSet.getFloat("BALANCE")/100));
		    }
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in cardAuthorize: " + e.getMessage());
		}
	}
	
	
	public void getAccountTransactions(Account vAccount, List<String> pList) throws BankException
	{
		try 
		(
			Statement lStatement = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); 
			ResultSet lSet = lStatement.executeQuery(String.format("SELECT t.TRANSACTION_ID, t.TS, t.AMOUNT, t.ACCOUNT_ID_1, c1.CLIENT_ID CLIENT_ID_1, t.ACCOUNT_ID_2, c2.CLIENT_ID CLIENT_ID_2 FROM TRANSACTIONS t LEFT OUTER JOIN ACCOUNTS a1 ON t.ACCOUNT_ID_1 = a1.ACCOUNT_ID LEFT OUTER JOIN ACCOUNTS a2 ON t.ACCOUNT_ID_2 = a2.ACCOUNT_ID LEFT OUTER JOIN CLIENTS c1 ON a1.CLIENT_ID = c1.CLIENT_ID LEFT OUTER JOIN CLIENTS c2 ON a2.CLIENT_ID = c2.CLIENT_ID WHERE t.ACCOUNT_ID_1 = %d OR t.ACCOUNT_ID_2 = %d ORDER BY t.TS", vAccount.ID, vAccount.ID));
		)			
		{
		    while (lSet.next()) 
		    {
		    	String lFrom = lSet.getInt("CLIENT_ID_1") == 0 ? "cash deposit" : String.valueOf(lSet.getInt("ACCOUNT_ID_1")) ;
		    	String lTo = lSet.getInt("CLIENT_ID_2") == 0 ? "cash withdrawal" : String.valueOf(lSet.getInt("ACCOUNT_ID_2")) ;		    	
		    	pList.add(String.format("%1$3d %2$tY.%2$tm.%2$td %2$tH:%2$tM:%2$tS %3$10.2f  %4$12s  %5$15s",lSet.getInt("TRANSACTION_ID"),lSet.getTimestamp("TS"),lSet.getFloat("AMOUNT")/100,lFrom,lTo));
		    }
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in getAccountTransactions: " + e.getMessage());
		}
	}

	
	public void getAllAccountsTransactions(Account vAccount, List<String> pList) throws BankException
	{
		String lSQL = String.format
				(
				  "SELECT	t.TRANSACTION_ID, t.TS,	a1.CURRENCY, t.AMOUNT, t.ACCOUNT_ID_1, c1.CLIENT_ID CLIENT_ID_1, t.ACCOUNT_ID_2, c2.CLIENT_ID CLIENT_ID_2 " +
                  "FROM TRANSACTIONS t LEFT OUTER JOIN ACCOUNTS a1 ON t.ACCOUNT_ID_1 = a1.ACCOUNT_ID LEFT OUTER JOIN CLIENTS c1 ON a1.CLIENT_ID = c1.CLIENT_ID LEFT OUTER JOIN ACCOUNTS a11 ON c1.CLIENT_ID = a11.CLIENT_ID and a11.ACCOUNT_ID = %1$d LEFT OUTER JOIN ACCOUNTS a2 ON t.ACCOUNT_ID_2 = a2.ACCOUNT_ID LEFT OUTER JOIN CLIENTS c2 ON a2.CLIENT_ID = c2.CLIENT_ID LEFT OUTER JOIN ACCOUNTS a22 ON c2.CLIENT_ID = a22.CLIENT_ID  and a22.ACCOUNT_ID = %1$d " +
			      "WHERE a11.ACCOUNT_ID = %1$d OR a22.ACCOUNT_ID = %1$d ORDER BY t.TS",
			      vAccount.ID
                );
		try 
		(
			Statement lStatement = mConnection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ResultSet lSet = lStatement.executeQuery(lSQL);
		)			
		{
		    while (lSet.next()) 
		    {
		    	String lFrom = lSet.getInt("CLIENT_ID_1") == 0 ? "cash deposit" : String.valueOf(lSet.getInt("ACCOUNT_ID_1")) ;
		    	String lTo = lSet.getInt("CLIENT_ID_2") == 0 ? "cash withdrawal" : String.valueOf(lSet.getInt("ACCOUNT_ID_2")) ;		    	
		    	pList.add(String.format("%1$3d %2$tY.%2$tm.%2$td %2$tH:%2$tM:%2$tS %3$3s %4$10.2f  %5$12s  %6$15s",lSet.getInt("TRANSACTION_ID"),lSet.getTimestamp("TS"),lSet.getString("CURRENCY"),lSet.getFloat("AMOUNT")/100,lFrom,lTo));
		    }
		}
		catch (SQLException e)
		{
			throw new BankException("SQLException in getAllAccountsTransactions: " + e.getMessage());
		}
	}
	
	
	public void initDB() throws SQLException
	{
	    Connection lConnection = null;
	    Properties lProps = new Properties();
	    lProps.put("user", "root");
	    lProps.put("password", "000");
		
	    String lURL = "jdbc:" + "mysql" + "://" + "localhost" +  ":" + "3306" + "/";
	    lConnection = DriverManager.getConnection(lURL, lProps);
	      
	    Statement lStatement; 
	    String lDDL;
	    
	    lStatement = lConnection.createStatement();	    
	    lDDL = "CREATE DATABASE IF NOT EXISTS " + "atmdb";
	    lStatement.executeUpdate(lDDL);
	    lStatement.close();

	    lConnection.setCatalog("atmdb");	    
	    lStatement = lConnection.createStatement();	    

	    //lStatement.executeUpdate("DROP TABLE IF EXISTS CARDS");
	    //lStatement.executeUpdate("DROP TABLE IF EXISTS TRANSACTIONS");
	    //lStatement.executeUpdate("DROP TABLE IF EXISTS ACCOUNTS");
	    //lStatement.executeUpdate("DROP TABLE IF EXISTS CLIENTS");	    
	    
	    lDDL = "create table IF NOT EXISTS CLIENTS " +
		  	     "(" + 
		  	     "CLIENT_ID int NOT NULL, " +
		  	     "CLIENT_NAME varchar(100) NOT NULL, " +	          
		  	     "PRIMARY KEY (CLIENT_ID)" + 
		  	     ")";
	    lStatement.executeUpdate(lDDL);	    

	    
	    lDDL = "create table IF NOT EXISTS ACCOUNTS " +
	             "(" +	          
	             "ACCOUNT_ID int NOT NULL AUTO_INCREMENT, " +
	             "CLIENT_ID int NOT NULL, " +	          
		         "CURRENCY char(3) NOT NULL, " +	          
	             "BALANCE int NOT NULL, " +	            
	             "PRIMARY KEY (ACCOUNT_ID), " +
	             "FOREIGN KEY (CLIENT_ID) REFERENCES CLIENTS (CLIENT_ID)" + 
	             ")";	          
	    lStatement.executeUpdate(lDDL);	    

	    
	    lDDL = "create table IF NOT EXISTS TRANSACTIONS " +
	             "(" +	          
	             "TRANSACTION_ID int NOT NULL AUTO_INCREMENT, " +	          
	             "ACCOUNT_ID_1 int NOT NULL, " +
	             "ACCOUNT_ID_2 int NOT NULL, " +
	             "AMOUNT int NOT NULL, " +
	             "TS timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " +	            
	             "PRIMARY KEY (TRANSACTION_ID), " +
	             "FOREIGN KEY (ACCOUNT_ID_1) REFERENCES ACCOUNTS (ACCOUNT_ID), " +	            
	             "FOREIGN KEY (ACCOUNT_ID_2) REFERENCES ACCOUNTS (ACCOUNT_ID)" + 
	             ")";	          
	    lStatement.executeUpdate(lDDL);	    

	    
	    lDDL = "create table IF NOT EXISTS CARDS " +
	             "(" +	          
	             "CARD_ID int NOT NULL, " +
	             "ACCOUNT_ID int NOT NULL, " +	          
		         "PIN char(4) NOT NULL, " +	          
	             "PRIMARY KEY (CARD_ID), " +
	             "FOREIGN KEY (ACCOUNT_ID) REFERENCES ACCOUNTS (ACCOUNT_ID)" + 
	             ")";	          
	    lStatement.executeUpdate(lDDL);	    

	    if (lStatement.executeUpdate("UPDATE CLIENTS SET CLIENT_ID = CLIENT_ID WHERE CLIENT_ID = 0") == 0 )
	    {
	    	lStatement.executeUpdate("insert into CLIENTS values(0,'The Bank')");
	    	lStatement.executeUpdate("insert into CLIENTS values(1,'Rich Client')");	    	
	    	lStatement.executeUpdate("insert into CLIENTS values(2,'Poor Client')");	    	
	    	lStatement.executeUpdate("insert into CLIENTS values(3,'Fraudster')");
	    	
	    //}
	    //if (lStatement.executeUpdate("UPDATE ACCOUNTS SET ACCOUNT_ID = ACCOUNT_ID WHERE CLIENT_ID = 0") == 0 )
	    //{
	    	lStatement.executeUpdate("insert into ACCOUNTS values(1,0,'UAH',0)");
	    	lStatement.executeUpdate("insert into ACCOUNTS values(2,0,'USD',0)");	    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(3,0,'EUR',0)");	    	
	    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(4,1,'UAH',99999999)");
	    	lStatement.executeUpdate("insert into ACCOUNTS values(5,1,'USD',99999999)");	    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(6,1,'CAD',99999999)");	    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(7,1,'EUR',99999999)");	    	

	    	lStatement.executeUpdate("insert into ACCOUNTS values(8,2,'UAH',10000)");
	    	lStatement.executeUpdate("insert into ACCOUNTS values(9,2,'RUR',10000)");	    	
    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(10,3,'USD',0)");	    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(11,3,'CAD',0)");	    	
	    	lStatement.executeUpdate("insert into ACCOUNTS values(12,3,'RUR',0)");
	    	lStatement.executeUpdate("insert into ACCOUNTS values(13,3,'EUR',0)");
	    	lStatement.executeUpdate("insert into ACCOUNTS values(14,3,'UAH',0)");	    	
	    //}
	    //if (lStatement.executeUpdate("UPDATE CARDS SET CARD_ID = CARD_ID WHERE CARD_ID = 104") == 0 )
	    //{
	    	lStatement.executeUpdate("insert into CARDS values(104,4,'1111')");
	    	lStatement.executeUpdate("insert into CARDS values(105,5,'1111')");	    	
	    	lStatement.executeUpdate("insert into CARDS values(106,6,'1111')");
	    	lStatement.executeUpdate("insert into CARDS values(107,7,'1111')");
	    	
	    	lStatement.executeUpdate("insert into CARDS values(208,8,'2222')");
	    	lStatement.executeUpdate("insert into CARDS values(209,9,'2222')");	    	

	    	lStatement.executeUpdate("insert into CARDS values(310,10,'3333')");	    	
	    	lStatement.executeUpdate("insert into CARDS values(311,11,'3333')");	    	
	    	lStatement.executeUpdate("insert into CARDS values(312,12,'3333')");	    	
	    	lStatement.executeUpdate("insert into CARDS values(313,13,'3333')");	    	
	    	lStatement.executeUpdate("insert into CARDS values(314,14,'3333')");
	    	
	    	lStatement.executeUpdate("delete from TRANSACTIONS");	    	
	    }
	    
	    lStatement.close();
	    lConnection.close();
	}
}





