package ec.com.sofka.aggregate.account;

import ec.com.sofka.account.Account;
import ec.com.sofka.aggregate.account.events.AccountBalanceUpdated;
import ec.com.sofka.aggregate.account.events.AccountCreated;
import ec.com.sofka.aggregate.account.events.CardCreated;
import ec.com.sofka.aggregate.account.values.CustomerId;
import ec.com.sofka.card.Card;
import ec.com.sofka.generics.domain.DomainEvent;
import ec.com.sofka.generics.utils.AggregateRoot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;


public class Customer extends AggregateRoot<CustomerId> {
    private Account account;
    private Card card;

    public Customer() {
        super(new CustomerId());
        setSubscription(new CustomerHandler(this));
    }


    private Customer(final String id) {
        super(CustomerId.of(id));
        setSubscription(new CustomerHandler(this));
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public void createAccount(BigDecimal accountBalance, String accountNumber, String name, String accountType ) {
        addEvent(new AccountCreated(accountNumber,accountBalance, name, accountType)).apply();

    }

    public void createCard(CardCreated createCardEv) {
        addEvent(createCardEv).apply();
    }

    public void updateAccountBalance(String id, String accountNumber, BigDecimal accountBalance, String ownerName, String accountType ) {
        addEvent(new AccountBalanceUpdated(id, accountNumber, accountBalance, ownerName, accountType)).apply();
    }


    public static Mono<Customer> from(final String id, Flux<DomainEvent> events) {
        Customer customer = new Customer(id);
        return events
                .filter(eventsFilter -> id.equals(eventsFilter.getAggregateRootId()))
                .concatMap(event -> Mono.just(event)
                        .doOnNext(e -> customer.addEvent(e).apply())
                )
                .then(Mono.just(customer));
    }


}
