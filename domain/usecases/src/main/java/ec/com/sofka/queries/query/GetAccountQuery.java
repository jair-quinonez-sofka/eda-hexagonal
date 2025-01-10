package ec.com.sofka.queries.query;

import ec.com.sofka.generics.utils.Query;

import java.math.BigDecimal;

public class GetAccountQuery extends Query {
    private final BigDecimal balance;
    private final String accountNumber;
    private final String ownerName;


    public GetAccountQuery(final String aggregateId, final String numberAcc, String accountNumber, String ownerName) {
        super(aggregateId);
        this.accountNumber = accountNumber;
        this.ownerName = null;
        this.balance = null;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
