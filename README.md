# Super Wallet

## Task Description:
The task is to design and implement the Super-Wallet microservice, which will act as a user wallet supporting multiple tokens (e.g., BTC, ETH). 
The system should allow users to perform operations on their funds, such as depositing, blocking, withdrawing, and releasing tokens.

## Technical Requirements:

The microservice should operate in an event-driven architecture, utilizing asynchronous communication.
Sollution should embrace idempotency and duplicate handling – ensuring that operations are safe and resistant to retries.
Scalability – the solution should support easy scalability, both in terms of user handling and infrastructure.

Key Operations:
The system should respond to commands sent from external systems:
DepositFunds – adds funds to the user’s wallet
BlockFunds – blocks a specific amount of funds
WithdrawFunds – withdraws blocked funds
ReleaseFunds – unblocks funds and returns them to the available balance

Each operation should generate the corresponding events, such as:
FundsAdded – funds have been added
FundsBlocked – funds have been blocked
FundsWithdrawn – funds have been withdrawn
FundsReleased – blocked funds have been released
Error - when an business error occours

Additionally, a dedicated topic should receive snapshots containing the current state of the user's account, including available (available) and blocked (blocked) funds for each token.


## Requirements to run service
- Java 21
- Groovy
- Gradle
- Docker

## Description
I focused on scalability and fast command processing.
The entire code, from the Kafka listener, through business logic and MongoDB, to the Kafka producer, is written reactively and follows a hexagonal architecture.

Key Entities:
Wallet – Represents a user’s wallet.
ExecutedCommand – Can be understood as a log of executed commands on the Wallet.

By combining these two entities with two schedulers, the code can seamlessly process commands and recover from failures.
Additionally, all business logic is idempotent, and automatic ACK in the Kafka listener is disabled.

Scenarios:
1. Happy Path:
Hopefully 100% of cases ;)

2. Kafka failure on the listener side:
Automatic retry and repeat of the stream, ensuring the connection is eventually restored.

3. Database failure after reading commands but before applying them to Wallet:
Configurable retries with delay.
If ACK cannot be completed, the message is ultimately sent to the Dead Letter Topic (DLT).

4. Technical validation errors (invalid field values, missing Wallet, etc.):
Automatically send a message to the DLT.

5. Business validation errors (insufficient funds, non-existent lock ID, etc.):
No retries, the system immediately sends an Error Wallet Event to Kafka.

6. Database or Kafka producer failure after applying a command and saving Wallet & ExecutedCommand (or just Wallet):
The ExecutedCommand and the event sent by the Kafka producer are stored in an Outbox Pattern for the given Wallet.

Two jobs to handle recovery:
ProcessMissingExecutedCommandsJob – Tries to create ExecutedCommand and send the event (uses MongoDB Aggregation).
SendExecutedCommandsEventsJob – Sends the event to Kafka via producer.

Both jobs operate with delayed execution and although they have a configured cron (preferably every second), they do not attempt immediate recovery. 
Instead, they process only Wallets and ExecutedCommands that were created or modified some (configurable) time ago.

This approach ensures that if a command is reattempted by the Kafka listener, there is no collision between the jobs and the command processing logic in the service.
The service attempts to process a stuck command as soon as possible, but if retries fail for a longer period, the jobs take over.

Furthermore, all subsequent commands belonging to the same Wallet are automatically delegated to the jobs to maintain processing order.
Additionally, the jobs are designed to maximize parallel execution while ensuring sequential processing for individual Wallets.

Edge case: 
When after processing command event is succesfully published by Kafka producer but database fails immediately afterward event will be send again with same ID during recovery.
Handling this would require a distributed transaction across MongoDB and Kafka, which would significantly slow down execution.

Testing:
High test coverage (over 90%) and basic performance tests are included.
Performance tests run locally by creating a base set of Wallets and ExecutedCommands, then sending additional commands to Kafka.
Results are quite satisfying (of course having in mind that tests were performed on local machine).
Average command processing time is as low as 6-7 ms.
Also, the performance remains stable as the number of database records grows (even up to few millions).

## How to run
./gradle clean test - to run all tests
./gradle clean performanceTest - to run performance tests (this might be time consumming, please check PerformanceTests class to configure test)


