package ec.com.sofka.usecases.gateway;

public interface IPasswordHasher {
    String hash(String password);
}
