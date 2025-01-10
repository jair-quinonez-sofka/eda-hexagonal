package ec.com.sofka.database.account;

import ec.com.sofka.data.UserEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface IUserMongoRepository extends ReactiveMongoRepository<UserEntity, String> {
}
