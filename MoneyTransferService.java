import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@Component
public class MoneyTransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RestTemplate restTemplate;

    private static Map<Long, Object> locks = new HashMap<>();

    @Transactional
    public boolean transfer(Long fromAccountId, Long toAccountId, double amount) {
        if (amount <= 0) {
            return false;
        }

        Object fromLock = getLock(fromAccountId);
        Object toLock = getLock(toAccountId);

        synchronized (fromLock) {
            synchronized (toLock) {
                Account fromAccount = accountRepository.findById(fromAccountId).get();
                Account toAccount = accountRepository.findById(toAccountId).get();

                if (fromAccount.getBalance() < amount) {
                    return false;
                }

                fromAccount.setBalance(fromAccount.getBalance() - amount);
                toAccount.setBalance(toAccount.getBalance() + amount);

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                auditService.log("Transferred " + amount + " from " + fromAccountId + " to " + toAccountId);

                // Notify external fraud detection API
                String result = restTemplate.getForObject("http://fraud.api/check?from=" + fromAccountId + "&to=" + toAccountId + "&amount=" + amount, String.class);
                System.out.println("Fraud check result: " + result);

                // Send notifications
                notificationService.notify(fromAccount.getOwnerEmail(), "You sent $" + amount);
                notificationService.notify(toAccount.getOwnerEmail(), "You received $" + amount);
            }
        }

        return true;
    }

    private Object getLock(Long accountId) {
        if (!locks.containsKey(accountId)) {
            locks.put(accountId, new Object());
        }
        return locks.get(accountId);
    }

}
