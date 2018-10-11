### Database-Related Limitations

We are using H2 as the underlying database and here are its limitations: http://www.h2database.com/html/advanced.html#limits_limitations.

* **General**
   * Maximum database size: 4TB
* **Generic Ledgers:**
   * Maximum number of blocks in all ledgers: 2^64
   * Maximum block size: 5096 bytes
* **Generic LedgerState:**
   * Maximum partition size: 5096 bytes
   * Maximum number of partitions in all ledger states: 2^64
* **Identity LedgerState:**
   * Maximum number of public keys: 2^64
* **Chimeric LedgersState:**
   * Maximum number of currencies: 2^64
   * Maximum number of unspent transaction outputs and addresses: 2^64