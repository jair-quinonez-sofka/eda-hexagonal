package ec.com.sofka.card;

import ec.com.sofka.account.GetAccountByAccountNumberUseCase;
import ec.com.sofka.account.Account;
import ec.com.sofka.account.values.AccountId;
import ec.com.sofka.account.values.objects.AccountNumber;
import ec.com.sofka.account.values.objects.AccountType;
import ec.com.sofka.account.values.objects.Balance;
import ec.com.sofka.account.values.objects.OwnerName;
import ec.com.sofka.aggregate.account.Customer;
import ec.com.sofka.aggregate.account.events.CardCreated;
import ec.com.sofka.gateway.ICardRepository;
import ec.com.sofka.gateway.IEventStore;
import ec.com.sofka.gateway.dto.CardDTO;
import ec.com.sofka.generics.domain.DomainEvent;
import ec.com.sofka.generics.interfaces.IUseCase;
import ec.com.sofka.request.CardRequest;
import ec.com.sofka.request.GetAccountRequest;
import ec.com.sofka.request.GetCardByNumRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CreateCardUseCase implements IUseCase<CardRequest, CardDTO> {

    private final IEventStore eventRepository;
    private final ICardRepository cardRepository;
    private final GetAccountByAccountNumberUseCase getAccountByAccountNumberUseCase;
    private final GetCardByCardNumberUseCase getCardByCardNumberUseCase;
    private final GetCvvCardUseCase getCvvCardUseCase;



    public CreateCardUseCase(IEventStore eventStore, ICardRepository cardRepository, GetAccountByAccountNumberUseCase getAccountByAccountNumberUseCase, GetCardByCardNumberUseCase getCardByCardNumberUseCase, GetCvvCardUseCase getCvvCardUseCase) {
        this.eventRepository = eventStore;
        this.cardRepository = cardRepository;
        this.getAccountByAccountNumberUseCase = getAccountByAccountNumberUseCase;
        this.getCardByCardNumberUseCase = getCardByCardNumberUseCase;
        this.getCvvCardUseCase = getCvvCardUseCase;
    }

    @Override
    public Mono<CardDTO> execute(CardRequest cmd) {
        Flux<DomainEvent> events = eventRepository.findAggregate(cmd.getCustomerId());
        System.out.println("CMDD " + cmd.getCardNumber() + " " + cmd.getAccount().getAccountNumber()+" "+ cmd.getAggregateId());

        return Customer.from(cmd.getCustomerId(), events)
                .flatMap( customer -> {
                    Card card = customer.getCard();
                    if (card != null) {
                        return cardRepository.findByCardNumber(card.getCardNumber().getValue())
                                .flatMap(cardDTO -> Mono.error(new RuntimeException("Card already exists")));
                    }
                    return getAccountByAccountNumberUseCase.execute(new GetAccountRequest(
                                    cmd.getCustomerId(), null, null, null
                            ))
                            .switchIfEmpty(Mono.error(new RuntimeException("Account does not exist")))
                            .flatMap(accountResponse -> getCvvCardUseCase.apply()
                                    .flatMap(cvv ->{
                                        CardCreated cardCreated = new CardCreated(
                                                cmd.getCardName(),
                                                cmd.getCardNumber(),
                                                cmd.getCardType(),
                                                cmd.getCardStatus(),
                                                cmd.getCardExpiryDate(),
                                                cvv,
                                                cmd.getCardLimit(),
                                                cmd.getCardHolderName(),
                                                accountResponse.getAccount()
                                        );
                                        customer.createCard(cardCreated);

                                        CardDTO cardToSave = new CardDTO(
                                                null,
                                                customer.getCard().getCardName().getValue(),
                                                customer.getCard().getCardNumber().getValue(),
                                                customer.getCard().getCardType().getValue(),
                                                customer.getCard().getCardStatus().getValue(),
                                                customer.getCard().getCardExpiryDate().getValue(),
                                                cvv,
                                                customer.getCard().getCardLimit().getValue(),
                                                customer.getCard().getCardHolderName().getValue(),
                                                accountResponse.getAccountDTO()
                                        );
                                        return cardRepository.save(cardToSave);
                                    }))
                            .flatMap(savedCard -> Flux.fromIterable(customer.getUncommittedEvents())
                                    .flatMap(eventRepository::save)
                                    .then(Mono.just(savedCard))
                            )
                            .doOnSuccess(savedCard -> customer.markEventsAsCommitted());

                });



    }
}
