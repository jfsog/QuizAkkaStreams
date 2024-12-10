package org.jfsog.quizakkastreams.Biscuit;

import io.vavr.control.Try;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.biscuitsec.biscuit.crypto.KeyPair;
import org.biscuitsec.biscuit.error.Error;
import org.biscuitsec.biscuit.token.Biscuit;
import org.jfsog.quizakkastreams.Models.User.Users;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.Instant;

@Service
@Slf4j
public class BiscuitTokenService {
    private static final KeyPair root = new KeyPair();
    private final RedissonClient redisson;
    public BiscuitTokenService(RedissonClient redisson) {this.redisson = redisson;}
    public String createUserToken(Users user) {
        try {
            var futuretime = Instant.now().plusSeconds(30).toEpochMilli();
            var b = Biscuit.builder(root)
                           .add_authority_fact(String.format("user(\"%s\")", user.getLogin()))
                           .add_authority_fact(String.format("role(\"%s\")", user.getRole()))
                           .add_authority_fact(String.format("expiration(%s)", futuretime))
                           .add_authority_check("check if operation(\"read\")")
                           .build();
            return Try.of(b::serialize_b64url).getOrNull();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar login Biscuit", e);
        }
    }
    //    implementar validações mais complexas
    /*adicionar mais um parâmetro de string variável para validar
        aplicar composição de facts
    * */
    public boolean validarToken(String b64Token, String... customChecks) {
        try {
            var now = Instant.now().toEpochMilli();
            var b = Biscuit.from_b64url(b64Token, root.public_key())
                           .verify(root.public_key())
                           .authorizer()
                           .add_fact("operation(\"read\")")
                           .add_check("check if expiration($0), $0 > %s ".formatted(now));
            for (var s : customChecks)
                b.add_check(s);
            var code = b.allow().authorize();
            if (code == null) {
                return false;
            }
            if (code == 0L) {
                return true;
            }
            log.info("Non zero value?: {}", code);
            return true;
        } catch (Error.FailedLogic e) {
            e.printStackTrace(System.out);
            return false;
        } catch (Error | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            e.printStackTrace(System.out);
            return false;
//            throw new RuntimeException(e);
        }
    }
    @PreDestroy
    private void cleanUp() {
        redisson.getKeys().flushdbParallel();
    }
}
