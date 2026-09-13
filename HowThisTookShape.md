okay, so create a digital wallet with basic functionalities like creating a wallet, adding money, transferring money, and checking balance.

i am supposed to handle concurrency, consistency, and idempotency in the wallet operations.
the requirements clearly state:
Conservation(total money in the system should remain constant)
No overdraft(dont allow negative balances basically)
Exactly-once transfer(idempotency, no double, triple spending)
Race-free get-or-create(the same user cannot create multiple wallets, and if a wallet is being created, it should be locked or something for that user until the operation is complete)


challenges:
- handling concurrency in wallet operations, especially when multiple requests are trying to modify the same wallet balance, and yeah the spam sometimes
- ensuring idempotency in transfer operations to prevent double spending
- maintaining consistency and conservation of total money in the system

hmmmm... can use redis(very short ttl) for storing recent transfer requests to ensure idempotency(request comes in, check redis for the idempotency key, is it there? sure return 201, with some status of pending or something), and also for caching wallet balances to reduce db load.
maybe later on i can add redis

i have been asked to use postgres for this, and i mean it is actually a viable option, it has support for transactions, row-level locking, and other features that can help with concurrency control

## the DESIGN CALLS TO REASON ABOUT
### The simplest-correct mechanism for conservation + no-overdraft:

- hmmmm... about the transactions, conservation and no overdraft, i could use the @Transactional annotation in Spring Boot to manage transactions for wallet operations. This will ensure that all operations within a transaction are completed successfully, or none of them are applied in case of an error.
- i could also use the @Lock annotation to apply pessimistic locking on wallet rows during balance updates, to prevent concurrent modifications and ensure consistency.
- i could lock the rows to be updated, and sort the wallet ids to avoid deadlocks, and then update the balances in a single transaction.
- also why use table-wide locks and serializable isolation when they add unnecessary contention and complexity?
- add a check of balance before updating, and if the balance is insufficient, throw an exception to prevent overdraft.

### Consistency vs availability for a money workload:
- it is about money, so definitely consistency. i mean availability is important, but not at the cost of losing money or having inconsistent balances(i mean, who uses something that handles money and is not consistent, right?)
- again same thing, use transactions and row-level locking to ensure consistency during concurrent operations.





i can use java, makes handling this situation way easier, and spring boot makes it even easier with its annotations and transaction management.


cool
use Java and Spring Boot for the backend, PostgreSQL for the database, and Redis for caching whatevs.



not having a fun time dealing with concurrency issues :)
maybe stop with the cache impl cause that seems to be introducing consistency issues, and just focus on the db for now, caching can be added later once the core functionality is stable and correct.

dont have much time to create a dashboard(more like dont wanna build one) to consume the metrics api, might just use the spring-boot-admin dep, gives you a nice dash 