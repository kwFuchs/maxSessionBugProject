package mdbs;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Random;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import org.apache.log4j.Logger;

/**
 * This class is the actual worker element for the example described in StackStarterMDB.
 * Its maxSession attribute is what causes the problems.
 * 
 * @author Angelo.Fuchs
 */
public class TestStack {

	private final Logger log = Logger.getLogger(this.getClass());
	
	public TestStack() {
	}

	public void onMessage(Message message) {
		ObjectMessage omsg = (ObjectMessage)message;
		try {
			byte[] data = (byte[])omsg.getObject();
			if(data == null)
				throw new JMSException("object of data was null");
			String content = new String(data, Charset.forName("UTF-8"));
			doAction(message, content);
		} catch (Exception ex) {
			log.error("could not process message", ex);
		} catch (Throwable allTheRest) {
			log.fatal("a not-Exception Throwable was thrown", allTheRest);
		}
	}
	
	/**
	 * This method does some meaningless calculations to simulate actual work.
	 */
	private void doAction(Message message, String content) {
		Date started = new Date();
		int timeToThinkAboutIt = 20*1000;
		log.info("starting message: " + content + " will think for " + timeToThinkAboutIt + " ms.");
		
		Date end = new Date(started.getTime() + timeToThinkAboutIt);
		long current = new Random().nextInt();
		while(new Date().before(end)) {
			long factor = new Random().nextInt();
			current = (current+1) * factor;
		}
		log.info("sending reply: " + content);
		sendReplyMessage(message, "result: " + current + " original: " + content);
	}

	/**
	 * Here we attempt to send the reply message.
	 */
	private void sendReplyMessage(Message messageToReplyTo, String contentOfMessage) {
		try {
			Queue queue = (Queue) messageToReplyTo.getJMSReplyTo();
			if (queue == null) {
				log.warn("don't have any reply queue");
				return; // No reply queue set, so return here.
			}
			InitialContext context = new InitialContext();
			QueueConnectionFactory connectionFactory = (QueueConnectionFactory) context.lookup("QueueConnectionFactory");
			QueueConnection queueConnection = connectionFactory.createQueueConnection();
			try {
				QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
				try {
					QueueSender queueSender = queueSession.createSender((Queue) messageToReplyTo.getJMSReplyTo());
					try {
						queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
						ObjectMessage message = queueSession.createObjectMessage();
						message.setJMSCorrelationID(messageToReplyTo.getJMSCorrelationID());
						message.setStringProperty("reply", "true");
						byte[] parallelStackStopParametersCompressed = contentOfMessage.getBytes("UTF-8");
						message.setObject(parallelStackStopParametersCompressed);
						log.info("Send message to Starter (" + contentOfMessage + "), size: " +
							parallelStackStopParametersCompressed.length + " bytes.");
						queueSender.send(message);
						log.info("Message to Starter sent.");
					} finally {
						queueSender.close();
					}
				} finally {
					queueSession.close();
				}
			} finally {
				queueConnection.close();
			}
		} catch (Exception e) {
			log.error("Error sending reply message.", e);
		}
	}
}
