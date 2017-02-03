package mdbs;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 * This MDB starts a given amount of subprocesses that take a while. It is used to demonstrate a
 * misbehaviour in JBoss EAP 6. The replymessages sent by the subprocesses are sent only after the
 * listener stops, if more then maxSession messages are processed (maxSession of TestStack).
 * Expected behaviour would be: no Exception of style "ERROR [mdbs.TestStack] Error sending reply
 * message.: javax.jms.InvalidDestinationException: Destination $DESTINATION-ID does not exist"
 *
 * To activate send a message with an int property "amount". First one that is below maxSession,
 * then one that is above maxSession.
 *
 * @author Angelo.Fuchs
 */
@MessageDriven(mappedName = "StackStarterMDB", activationConfig = {
	@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
	@ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/queue/TestStackStarterQueue")
})
public class StackStarterMDB implements MessageListener {

	private final Logger log = Logger.getLogger(this.getClass());
	private QueueConnection queueConnection;
	private QueueSession queueSession;
	private TemporaryQueue temporaryQueue;
	private MessageConsumer messageConsumer;

	public StackStarterMDB() {
	}

	@Override
	public void onMessage(Message inputMessage) {
		try {
			if (inputMessage.propertyExists("amount")) {
				handleStartOfParalellStacks(inputMessage);
			} else {
				// handle a parallel stack
				new TestStack().onMessage(inputMessage);
			}
		} catch (Exception ex) {
			log.error("unexpected exception.", ex);
		}
	}

	private void handleStartOfParalellStacks(Message inputMessage) throws NamingException, UnsupportedEncodingException, JMSException, StackException {
		openTemporaryMessageQueue();
		try {
			int amount = inputMessage.getIntProperty("amount");
			InitialContext context = new InitialContext();
			Queue testQueue = (Queue) context.lookup("queue/TestStackStarterQueue");
			QueueSender send = queueSession.createSender(testQueue);
			for (int ii = 0; ii < amount; ii++) {
				ObjectMessage outputMessage = queueSession.createObjectMessage();
				String content = "start process " + ii;
				outputMessage.setObject(content.getBytes("UTF-8"));
				outputMessage.setJMSReplyTo(temporaryQueue);
				log.info("send message: " + ii);
				send.send(outputMessage);
			}

			int messagesReceived = 0;
			for (int ii = 0; ii < 6 && messagesReceived < amount;) { // one minute or until everything is done.
				Message answer = messageConsumer.receive(10000);// Use timeout to be able to check for stop request while waiting for ParallelStack messages.
				if (answer == null) {
					log.info("No messages received in time, attempt: " + ii);
					ii++;
				} else {
					messagesReceived++;
					log.info("answer: " + new String(((byte[]) ((ObjectMessage) answer).getObject()), Charset.forName("UTF-8")));
				}
			}
			if (messagesReceived != amount) {
				log.error("did not receive all replies");
			} else {
				log.info("all messages received");
			}
		} finally {
			closeTemporaryMessageQueue();
		}
	}

	private void openTemporaryMessageQueue() throws StackException {
		try {
			log.info("Open temporary message queue...");
			try {
				InitialContext context = new InitialContext();
				QueueConnectionFactory connectionFactory = (QueueConnectionFactory) context.lookup("QueueConnectionFactory");
				queueConnection = connectionFactory.createQueueConnection();
				queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				temporaryQueue = queueSession.createTemporaryQueue();
				messageConsumer = queueSession.createConsumer(temporaryQueue, "reply" + " = 'true'");
				queueConnection.start();
				log.info("Temporary message queue opened.");
			} catch (NamingException e) {
				throw new StackException("QueueConnectionFactory lookup failed.", e);
			} catch (JMSException e) {
				throw new StackException("Creating QueueConnection failed.", e);
			}
		} catch (StackException e) {
			closeTemporaryMessageQueue();// Close JMS resources which have been opened above.
			throw e;
		}
	}

	private void closeTemporaryMessageQueue() {

		log.info("Close temporary message queue.");

		try {
			if (messageConsumer != null) {
				messageConsumer.close();
			}
		} catch (JMSException e) {
			log.error("Closing QueueReceiver failed.", e);
		}
		messageConsumer = null;

		try {
			if (queueSession != null) {
				queueSession.close();
			}
		} catch (JMSException e) {
			log.error("Closing QueueSession failed.", e);
		}
		queueSession = null;

		try {
			if (queueConnection != null) {
				queueConnection.close();
			}
		} catch (JMSException e) {
			log.error("Closing QueueConnection failed.", e);
		}
		queueConnection = null;
	}
}
