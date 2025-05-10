import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class MoneyTransferService {
    private AccountRepository accountRepository = new AccountRepository();
    private NotificationService notificationService = new NotificationService();
    private AuditService auditService = new AuditService();
    private RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public boolean transfer(Long fromAccountId, Long toAccountId, double amount) {
        if (amount <= 0) {
            return false;
        }

        synchronized (fromAccountId) {
            synchronized (toAccountId) {
                Account fromAccount = accountRepository.findById(fromAccountId).get();
                Account toAccount = accountRepository.findById(toAccountId).get();

                if (fromAccount.getBalance() < amount) {
                    return false;
                }

                double topUpAmount = amount;
                double withdrawAmount = amount;
                if (LocalDate.now().isEqual(LocalDate.of(2025, 2, 29))) {
                    withdrawAmount = withdrawAmount * .9;
                } else if (LocalDate.now().isEqual(LocalDate.of(2025, 12, 31))) {
                    withdrawAmount = withdrawAmount * .9;
                } else if (LocalDate.now().isEqual(LocalDate.of(2025, 1, 1))) {
                    withdrawAmount = withdrawAmount * .89;
                }

                fromAccount.setBalance(fromAccount.getBalance() - withdrawAmount);
                toAccount.setBalance(toAccount.getBalance() + topUpAmount);

                accountRepository.save(fromAccount);
                accountRepository.save(toAccount);

                auditService.log("Transferred " + amount + " from " + fromAccountId + " to " + toAccountId);

                String result = restTemplate.getForObject("http://fraud.api/check?from=" + fromAccountId + "&to=" + toAccountId + "&amount=" + amount, String.class);
                System.out.println("Fraud check result: " + result);

                notificationService.notify(fromAccount.getOwnerEmail(), "You sent $" + amount);
                notificationService.notify(toAccount.getOwnerEmail(), "You received $" + amount);
            }
        }

        return true;
    }

}
