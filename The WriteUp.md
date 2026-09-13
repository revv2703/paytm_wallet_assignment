## Paytm Wallet Project Write-Up

## 1. Data Model
Our relational schema is built on **PostgreSQL** to leverage strong ACID properties, row-level locking, and transactional guarantees.

### Schema Details:
* **`wallets` Table:**
    * `wallet_id` (VARCHAR(255), Primary Key): Unique wallet identifier (prefixed with `wal_`).
    * `user_id` (VARCHAR(255), Unique): Non-nullable, strictly linked to a user.
    * `balance_paise` (BIGINT): Non-negative balance represented as integer paise.
    * `created_at` (TIMESTAMPTZ).
* **`transfers` Table:**
    * `transfer_id` (VARCHAR(255), Primary Key): Unique transfer identifier (prefixed with `tx_`).
    * `from_wallet_id` (VARCHAR(255), FK $\rightarrow$ `wallets` ON DELETE CASCADE).
    * `to_wallet_id` (VARCHAR(255), FK $\rightarrow$ `wallets` ON DELETE CASCADE).
    * `amount_paise` (BIGINT).
    * `idempotency_key` (VARCHAR(255), Unique): Client-supplied unique key for transaction safety.
    * `status` (VARCHAR(32)): State of the transfer (`COMPLETED`, `DECLINED`).
    * `declined_reason` (VARCHAR(255)): Reason for declined transfers (e.g. `INSUFFICIENT_BALANCE`).
    * `created_at` (TIMESTAMPTZ).

---

## 2. Simplest-Correct Locking Mechanism
### Selected Approach:
We use **Alphabetically-Ordered Bulk Pessimistic Locking** (`SELECT ... FOR UPDATE ... ORDER BY w.wallet_id ASC`).

### Why is this the simplest-correct mechanism?
1. **Guaranteed Deadlock Prevention:** Deadlocks occur under high concurrency when Thread 1 locks Wallet A then B, while Thread 2 locks Wallet B then A. By sorting the wallet IDs lexicographically in Java and querying with `ORDER BY w.wallet_id ASC`, the database is forced to acquire locks in a globally consistent order.
2. **Single database query:** By requesting both row locks in a single, ordered SQL statement, we minimize database roundtrips and avoid connection hold latency.
3. **No Overdraft Integrity:** Once the locks are acquired, the transaction is fully isolated. We perform a simple in-memory assertion (`balance >= amount`). If insufficient, we commit a `DECLINED` transfer, freeing up locks instantly.

### Heavier Alternatives Rejected:
* **Serializable Isolation Level:** Rejected due to high transaction abort/rollback rates under contention, leading to high CPU/memory consumption on retries.
* **Distributed Lock Managers (Redis):** Rejected as it adds extra networking latency (TCP hops) and split-brain synchronization hazards. Database row-level locking is the most cohesive and low-latency choice.

---

## 3. Where Idempotency Lives
* **Enforcement:** Idempotency is enforced directly at the **Database Constraint Level** using a `UNIQUE` constraint on the `idempotency_key` in the `transfers` table.
* **Atomic Commit:** The insertion of the transfer record and the balance debit/credit are committed inside the **same transaction**.
* **Payload Conflict (`409`):** If a re-sent `idempotency_key` is detected:
    * We fetch the existing transfer. If the `from`, `to`, and `amount_paise` match, we perform an **idempotent replay** (returns `201 Created` or `200 OK` with the original state).
    * If any request parameters conflict with the persisted state, we throw an `ApiException` with status `409 Conflict`.

---

## 4. Consistency vs. Availability (CAP Theorem)
Given this is a financial workload, we explicitly choose **Consistency & Partition Tolerance (CP)** over Availability (AP):
* **What we gave up:** Low-latency AP database patterns (e.g., eventually consistent NoSQL databases like Cassandra).
* **Why:** In finance, an inconsistent balance or double-spending is a fatal correctness failure. A transaction must be 100% correct, consistent, and durable, even if a network partition makes the service temporarily unavailable.

---

## 5. AI Directed-vs-Decided Disclosure
* **Directed (Human Decided, AI Coded):**
    * The alphabetical ordering lock scheme, programmatic nested transactions (`TransactionTemplate` with `REQUIRES_NEW`), stateless Base64 security filters, and custom REST `/metrics` controller mappings.
* **Decided (AI Decided):**
    * The specific JUnit service test layouts and Maven packaging dependencies configuration to support Java 25.

---

## 6. Deployment & Cost Note
* **Public Live URL:** `https://paytmwalletassignment-production.up.railway.app/dashboard`
* **Cost:** **₹0** (completely deployed on free tiers of Railway).


## 7. Challenges & Lessons Learned
* the ai which was mostly used this time was copilot(for autocompletion, python burst script and boilerplate gen), and gemini for help with the concurrency issues and some minor fixes.
* The initial setup was done by the ai, which basically had the service very tightly coupled with the database, and the ai was not able to come up with a good solution for handling concurrency, consistency, and idempotency in the wallet operations. I had to come up with a design that would handle these requirements effectively.
* the ai created a single moduled and tightly coupled service, which was not very maintainable and testable. I had to refactor the code to have a more modular and layered architecture, with clear separation of concerns between the API layer, service layer, and repository layer.
* teh ai shined when i was having issues with concurrency handling and the implementation of bearer token auth and also writing a burst test for the wallet operations, which was very helpful in identifying these potential race conditions.
* **Concurrency Handling:** Managing concurrent wallet operations was challenging(the basic lock mechanism did not seem to handle the bursts that effectively, for this the use of ai was necessary). The use of pessimistic locking and ordered locking ensured that we avoided deadlocks and maintained consistency.
* **Idempotency Enforcement:** Implementing idempotency at the database level simplified the logic and ensured that duplicate requests did not lead to inconsistent states.
* **Testing Under Load:** Simulating high-concurrency scenarios helped identify potential race conditions and performance bottlenecks, leading to optimizations in transaction management and query efficiency.