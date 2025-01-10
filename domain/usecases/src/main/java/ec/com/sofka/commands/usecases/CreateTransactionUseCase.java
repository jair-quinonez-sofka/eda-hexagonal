package ec.com.sofka.commands.usecases;

import ec.com.sofka.account.Account;
import ec.com.sofka.aggregate.account.Customer;
import ec.com.sofka.aggregate.transaction.Payment;
import ec.com.sofka.aggregate.transaction.events.TransactionCreated;
import ec.com.sofka.card.Card;
import ec.com.sofka.gateway.*;
import ec.com.sofka.gateway.dto.*;
import ec.com.sofka.generics.domain.DomainEvent;
import ec.com.sofka.generics.interfaces.IUseCase;
import ec.com.sofka.generics.interfaces.IUseCaseExecute;
import ec.com.sofka.transaction.AccountDeposit;
import ec.com.sofka.transaction.Transaction;
import ec.com.sofka.transaction.values.TransactionId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class CreateTransactionUseCase implements IUseCaseExecute<TransactionDTO, TransactionDTO> {

    private final IEventStore eventRepository;
    private final ITransactionRepository transactionRepository;
    private final IAccountRepository accountRepository;
    private final ICardRepository cardRepository;
    private final BusEvent busEvent;

    public CreateTransactionUseCase(IEventStore eventRepository, ITransactionRepository transactionRepository, IAccountRepository accountRepository, ICardRepository cardRepository, BusEvent busEvent) {
        this.eventRepository = eventRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
        this.busEvent = busEvent;
    }


    @Override
    public Mono<TransactionDTO> execute(TransactionDTO transactionDTO1) {

        if(transactionDTO1.getTransactionType().equals("BA")){
            return processAccountDepositTransaction(transactionDTO1);
        }
        return  processTransaction(transactionDTO1);

    }


    private Mono<TransactionDTO> processAccountDepositTransaction(TransactionDTO transactionDTO) {
        Payment<AccountDeposit> payment = new Payment<>();
        AccountDepositDTO accountDepositDTO = (AccountDepositDTO) transactionDTO;

        Flux<DomainEvent> eventsSender = eventRepository.findAggregate(transactionDTO.getCustomerId());
        Flux<DomainEvent> eventsReceiver = eventRepository.findAggregate(accountDepositDTO.getReceiverCustomerId());

        return Customer.from(transactionDTO.getCustomerId(), eventsSender)
                .flatMap(sender -> {
                    Account senderAccount = sender.getAccount();

                    if (senderAccount != null) {
                        return Customer.from(accountDepositDTO.getReceiverCustomerId(), eventsReceiver)
                                .flatMap(receiver -> {
                                    Account receiverAccount = receiver.getAccount();
                                    if (receiverAccount != null) {
                                        return accountRepository.findByAccountNumber(senderAccount.getAccountNumber().getValue())
                                                .switchIfEmpty(Mono.error(new RuntimeException("Sender account not found")))
                                                .flatMap(senderAccountDTO -> accountRepository.findByAccountNumber(receiverAccount.getAccountNumber().getValue())
                                                        .switchIfEmpty(Mono.error(new RuntimeException("Receiver account not found")))
                                                        .flatMap(receiverAccountDTO -> {
                                                            TransactionCreated transactionCreated = new TransactionCreated(
                                                                    new TransactionId().getValue(),
                                                                    transactionDTO.getDescription(),
                                                                    transactionDTO.getAmount(),
                                                                    transactionDTO.getTransactionType(),
                                                                    transactionDTO.getTransactionFee(),
                                                                    LocalDateTime.now(),
                                                                    senderAccount,
                                                                    null,
                                                                    null,
                                                                    null,
                                                                    receiverAccount,
                                                                    null,
                                                                    null,
                                                                    null
                                                            );

                                                            payment.createPayment(transactionCreated);
                                                            senderAccountDTO.setBalance(
                                                                    senderAccountDTO.getBalance()
                                                                            .subtract(payment.getTransaction().getAmount().getValue())
                                                                            .subtract(payment.getTransaction().getTransactionFee().getValue())
                                                            );

                                                            receiverAccountDTO.setBalance(
                                                                    receiverAccountDTO.getBalance()
                                                                            .add(payment.getTransaction().getAmount().getValue())
                                                            );

                                                            transactionDTO.setAccount(senderAccountDTO);
                                                            ((AccountDepositDTO) transactionDTO).setAccountReceiver(receiverAccountDTO);
                                                            transactionDTO.setTransactionFee(payment.getTransaction().getTransactionFee().getValue());



                                                            sender.updateAccountBalance(
                                                                    senderAccountDTO.getId(),
                                                                    senderAccountDTO.getAccountNumber(),
                                                                    senderAccountDTO.getBalance(),
                                                                    senderAccountDTO.getOwnerName(),
                                                                    senderAccountDTO.getAccountType()
                                                            );
                                                            receiver.updateAccountBalance(
                                                                    receiverAccountDTO.getId(),
                                                                    receiverAccountDTO.getAccountNumber(),
                                                                    receiverAccountDTO.getBalance(),
                                                                    receiverAccountDTO.getOwnerName(),
                                                                    receiverAccountDTO.getAccountType()
                                                            );

                                                            payment.getUncommittedEvents()
                                                                    .stream()
                                                                    .map(eventRepository::save)
                                                                    .forEach(busEvent::sendEventTransactionCreated);

                                                            receiver.getUncommittedEvents()
                                                                    .stream()
                                                                    .map(eventRepository::save)
                                                                    .forEach(busEvent::sendEventAccountUpdated);

                                                            sender.getUncommittedEvents()
                                                                    .stream()
                                                                    .map(eventRepository::save)
                                                                    .forEach(busEvent::sendEventAccountUpdated);

                                                            payment.markEventsAsCommitted();
                                                            sender.markEventsAsCommitted();
                                                            receiver.markEventsAsCommitted();

                                                            return Mono.just(transactionDTO);

                                                            /*return transactionRepository.save(transactionDTO)
                                                                    .flatMap(savedTransaction -> {
                                                                        return accountRepository.save(senderAccountDTO)
                                                                                .flatMap(savedSenderAccount -> accountRepository.save(receiverAccountDTO)
                                                                                        .flatMap(savedReceiverAccount -> {
                                                                                            sender.updateAccountBalance(
                                                                                                    savedSenderAccount.getId(),
                                                                                                    savedSenderAccount.getAccountNumber(),
                                                                                                    savedSenderAccount.getBalance(),
                                                                                                    savedSenderAccount.getOwnerName(),
                                                                                                    savedSenderAccount.getAccountType()
                                                                                            );
                                                                                            receiver.updateAccountBalance(
                                                                                                    savedReceiverAccount.getId(),
                                                                                                    savedReceiverAccount.getAccountNumber(),
                                                                                                    savedReceiverAccount.getBalance(),
                                                                                                    savedReceiverAccount.getOwnerName(),
                                                                                                    savedReceiverAccount.getAccountType()
                                                                                            );
                                                                                            return Flux.concat(
                                                                                                            Flux.fromIterable(payment.getUncommittedEvents())
                                                                                                                    .flatMap(eventRepository::save),
                                                                                                            Flux.fromIterable(sender.getUncommittedEvents())
                                                                                                                    .flatMap(eventRepository::save),
                                                                                                            Flux.fromIterable(receiver.getUncommittedEvents())
                                                                                                                    .flatMap(eventRepository::save)
                                                                                                    ).doOnTerminate(() -> {
                                                                                                                payment.markEventsAsCommitted();
                                                                                                                sender.markEventsAsCommitted();
                                                                                                                receiver.markEventsAsCommitted();
                                                                                                            }
                                                                                                    ).then()
                                                                                                    .thenReturn(transactionDTO);
                                                                                        }));
                                                                    });*/
                                                        }));
                                    }
                                    return Mono.error(new RuntimeException("Receiver account not found in store"));
                                });
                    }
                    return Mono.error(new RuntimeException("Sender account or card not found in store"));
                });
    }

    private TransactionCreated createTransactionCreated(TransactionDTO transactionDTO, Account account, Card card) {
        switch (transactionDTO.getTransactionType()){
            case "ATM":
                AtmTransactionDTO atmTransactionDTO = (AtmTransactionDTO) transactionDTO;
                return new TransactionCreated(
                        new TransactionId().getValue(),
                        atmTransactionDTO.getDescription(),
                        atmTransactionDTO.getAmount(),
                        atmTransactionDTO.getTransactionType(),
                        atmTransactionDTO.getTransactionFee(),
                        LocalDateTime.now(),
                        account,
                        card,
                        atmTransactionDTO.getAtmName(),
                        atmTransactionDTO.getOperationType(),
                        null,
                        null,
                        null,
                        null
                );
            case "WP":
                PaymentWebDTO paymentWebDTO = (PaymentWebDTO) transactionDTO;
                return new TransactionCreated(
                        new TransactionId().getValue(),
                        paymentWebDTO.getDescription(),
                        paymentWebDTO.getAmount(),
                        paymentWebDTO.getTransactionType(),
                        paymentWebDTO.getTransactionFee(),
                        LocalDateTime.now(),
                        account,
                        card,
                        null,
                        null,
                        null,
                        null,
                        paymentWebDTO.getWebsite(),
                        null
                );
            case "SP":
                PaymentStoreDTO paymentStoreDTO = (PaymentStoreDTO) transactionDTO;
                return new TransactionCreated(
                        new TransactionId().getValue(),
                        paymentStoreDTO.getDescription(),
                        paymentStoreDTO.getAmount(),
                        paymentStoreDTO.getTransactionType(),
                        paymentStoreDTO.getTransactionFee(),
                        LocalDateTime.now(),
                        account,
                        card,
                        null,
                        null,
                        null,
                        paymentStoreDTO.getMarketName(),
                        null,
                        null
                );
            case "BD":
                BranchDepositDTO branchDepositDTO = (BranchDepositDTO) transactionDTO;
                return new TransactionCreated(
                        new TransactionId().getValue(),
                        branchDepositDTO.getDescription(),
                        branchDepositDTO.getAmount(),
                        branchDepositDTO.getTransactionType(),
                        branchDepositDTO.getTransactionFee(),
                        LocalDateTime.now(),
                        account,
                        card,
                        null,
                       null,
                        null,
                        null,
                        null,
                        branchDepositDTO.getBranchName()
                );
            default:
                throw new IllegalArgumentException("Unsupported transaction type: " + transactionDTO.getClass().getName());
        }


    }


    private Mono<TransactionDTO> processTransaction(TransactionDTO transactionDTO) {

        Flux<DomainEvent> events = eventRepository.findAggregate(transactionDTO.getCustomerId());

        return Customer.from(transactionDTO.getCustomerId(), events)
                .flatMap(customer -> {
                    Account account = customer.getAccount();
                    Card card = customer.getCards().stream()
                            .filter(cardRec -> cardRec.getCardNumber().getValue().equals(transactionDTO.getCard().getCardNumber()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Getting card failed"));
                    if (account != null && card != null) {
                        return accountRepository.findByAccountNumber(account.getAccountNumber().getValue())
                                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")))
                                .flatMap(accountDTO -> cardRepository.findByCardNumber(card.getCardNumber().getValue())
                                        .switchIfEmpty(Mono.error(new RuntimeException("Card not found")))
                                        .flatMap(cardDTO -> {

                                            Payment<? extends Transaction> payment = new Payment<>();
                                            TransactionCreated transactionCreated = createTransactionCreated(transactionDTO, account, card);

                                            payment.createPayment(transactionCreated);

                                            accountDTO.setBalance(
                                                    accountDTO.getBalance()
                                                            .subtract(payment.getTransaction().getAmount().getValue())
                                                            .subtract(payment.getTransaction().getTransactionFee().getValue())
                                            );

                                            transactionDTO.setCard(cardDTO);

                                            transactionDTO.setAccount(accountDTO);
                                            transactionDTO.setTransactionFee(payment.getTransaction().getTransactionFee().getValue());

                                            payment.getUncommittedEvents()
                                                    .stream()
                                                    .map(eventRepository::save)
                                                    .forEach(busEvent::sendEventTransactionCreated);
                                            customer.updateAccountBalance(
                                                    accountDTO.getId(),
                                                    accountDTO.getAccountNumber(),
                                                    accountDTO.getBalance(),
                                                    accountDTO.getOwnerName(),
                                                    accountDTO.getAccountType()
                                            );

                                            customer.getUncommittedEvents()
                                                    .stream()
                                                    .map(eventRepository::save)
                                                    .forEach(busEvent::sendEventAccountUpdated);

                                            payment.markEventsAsCommitted();
                                            customer.markEventsAsCommitted();

                                            return Mono.just(transactionDTO);


                                            /*return transactionRepository.save(transactionDTO)
                                                    .flatMap(savedTransaction -> accountRepository.save(accountDTO)
                                                            .flatMap(savedAccountDTO -> {
                                                                customer.updateAccountBalance(
                                                                        savedAccountDTO.getId(),
                                                                        savedAccountDTO.getAccountNumber(),
                                                                        savedAccountDTO.getBalance(),
                                                                        savedAccountDTO.getOwnerName(),
                                                                        savedAccountDTO.getAccountType()
                                                                );
                                                                return Flux.concat(
                                                                        Flux.fromIterable(payment.getUncommittedEvents())
                                                                                .flatMap(eventRepository::save),
                                                                        Flux.fromIterable(customer.getUncommittedEvents())
                                                                                .flatMap(eventRepository::save)
                                                                ).doOnTerminate(() -> {
                                                                    payment.markEventsAsCommitted();
                                                                    customer.markEventsAsCommitted();
                                                                })
                                                                        .then()
                                                                        .thenReturn(transactionDTO);
                                                            }));*/
                                        }));
                    }
                    return Mono.error(new RuntimeException("Account or Card not found in store"));
                });
    }

}
