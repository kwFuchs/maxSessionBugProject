In this project I reproduce a Bug in JBoss / HornetQ.

WHEN a Message Driven Bean sends Messages to itself
AND the amount of Messages send exceeds maxSession
THEN some of the messages are only delivered AFTER the initial instance is finished running.

This project consists of two parts: A JEE project that produces an .ear file and a JMS client that produces the initial Message to start the example.

The projects have been built with Netbeans, but the requirements are so narrow that it should be trivial to start the code in any other setup, IDE, Commandline or otherwise.
