# Weaveworks Backend Challenge

## Design 

```
                                                 +------------------------+
                                                 |                        |
                                                 |                        |
                                                 |  Stats Collector       |
                                                 |                        |
                                                 |    (Async)             |
                                                 |                        |
                                                 |                        |
                                                 |                        |
+---------------------------+                    |                        |          +-------------------------+
|                           |                    +---------+--------------+          |                         |
|                           |                              ^                         |  Event Sink (Server)    |
| Event Source  (client)    |                              |                         |                         |
|                           +------------------------------+------------------------->                         |
|    (Async)                |                                                        |    (Async)              |
|                           |                                                        |                         |
|                           |                                                        |                         |
|                           |                                                        |                         |
|                           |                                                        |                         |
+---------------------------+                                                        +-------------------------+
```

### Input
Using Java's NIO2 AsynchronousSocketChannel it is possible to do async read and writes to a socket. 
This allows large numbers of clients to be handled by a single thread. Originally my implementation used a thread per socket (See: com.stefano.weaveworks.pipe.InputReader)
but this was struggling to handle many clients, and the async version was created (com.stefano.weaveworks.pipe.AsyncInputReader). 
This continuously receives callbacks from the underlying NIO2 channel whenever data has been read. This data then undergoes the following:

1. The new data is converted into UTF-8 characters. 
2. These characters are added to any existing data
3. This data is searched for a message delimiter
4. This substring is deserialised into a Message
5. This message is passed to a series of listeners (A Stats Collector, and the destination server)
6. The original data is checked for another delimiter and the process repeats until all the data is searched

### Stats Collection
As each message is processed by the input source, it is passed to an instance of com.stefano.weaveworks.stats.StatsCollector. 
This is responsible for collecting the various stats required. To satisfy the Rate/Window requirement, the following is done

1. As messages are recieved, they are added to the tail of a Deque.
2. Starting at the head, all messages older than the Max Age (10Seconds) are popped off the Deque.
3. When a Rate/Window request is recieved, the queue is iterated over, and all messages which satisfy the window are counted
 
As the messages will arrive in time order, this approach should be efficient. The stats collector uses an ExecutorService and a task queue to: 

1. Enforce time ordered execution of messages
2. simplify thread safety of the various stat collections
3. Allow Asynchronous processing of the message(the message is not delayed by stat collection)
 
### Stats Printing
When a SIGUSR2 message is recieved, the signal handler interacts with the StatsCollector to place a task onto the queue.
This task delegates to com.stefano.weaveworks.stats.StatsJSONifier. Internally this builds a PoJo with the required fields and then uses GSON to print JSON to the console.
Again, this is done Asynchronously to simplify the treadsafety and "correctness" of the printed out information. 

## How To Run
This has been packaged up as a maven project. To execute the pre-defined config in the pom, run the following on a machine with maven3+ and jdk8.

```bash
mvn clean install exec:java
```

This will trigger a full build, unit test and start the proxy server, printing out PID and Port information to stderr. 


 