package ec.com.sofka.card;


import ec.com.sofka.gateway.ICardRepository;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.function.Supplier;


public class GetCvvCardUseCase {

    private final ICardRepository cardRepository;

    public GetCvvCardUseCase(ICardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Mono<String> apply() {
        Supplier<String> cvvGenerator = GetCvvCardUseCase::generateCvvCode;

        return Mono.defer(() -> Mono.just(cvvGenerator.get()))
                .flatMap(cvv -> existsCvv(cvv)
                        .flatMap(exists -> exists ? Mono.empty() : Mono.just(cvv)))
                .repeat()
                .next();
    }

    public static String generateCvvCode() {
        Random random = new Random();
        int randomNumber = 1000 + random.nextInt(9000);
        return String.valueOf(randomNumber);
    }

    private Mono<Boolean> existsCvv(String cvv) {
        return cardRepository.existsByCardCVV(cvv);
    }
}
