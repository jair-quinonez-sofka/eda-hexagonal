package ec.com.sofka.queries.usecases;

import ec.com.sofka.gateway.ITransactionRepository;
import ec.com.sofka.gateway.dto.TransactionDTO;
import ec.com.sofka.generics.interfaces.IUseCaseAccept;

public class TransactionCreatedViewUseCase implements IUseCaseAccept<TransactionDTO, Void> {
    private final ITransactionRepository transactionRepository;


    public TransactionCreatedViewUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }


    @Override
    public void accept(TransactionDTO dto) {
        transactionRepository.save(dto).subscribe();
    }
}
