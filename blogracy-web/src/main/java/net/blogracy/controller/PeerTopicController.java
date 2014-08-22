package net.blogracy.controller;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import net.blogracy.config.Configurations;
import net.blogracy.model.hashes.Hashes;

public class PeerTopicController{
	
	class StoreListener implements MessageListener{
		
		
		@Override
		public void onMessage(Message message) {
			
			if (message instanceof TextMessage) {
				try {
					String text = ((TextMessage) message).getText();
					parseXML(text);
				} catch (JMSException e) {
					System.err.println("JMS error: reading messages");
				}
			}
			else {
				System.err.println("This is not a TextMessage");
			}
			
		}
		
		
		private void parseXML(String xml){
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xml));
				Document doc = db.parse(is);

				NodeList nodes = doc.getElementsByTagName("message");
				Element element = (Element) nodes.item(0);
				String channel = element.getAttribute("channel");
				String type = element.getAttribute("type");
				String nick = element.getAttribute("from");
				String text = element.getTextContent();
				
				if(type.equals("chat") && channel.equals(listenerChannel)) 	
					if(!nick.equals(topicChat.localUser))
						addTopicActivity(channel, nick, text);
				

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
	}

	
	private ActivitiesController topicActivities;
	private ChatController topicChat;
	private String userId;
	
	private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Destination topic;
    private MessageConsumer consumer;
    private static final String TOPIC_NAME = "CHAT.DEMO";

    static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");

    static final String CACHE_FOLDER = Configurations.getPathConfig()
            .getCachedFilesDirectoryPath();
    
    public String listenerChannel;
    public String listenerChannelHash;
    
    private static final PeerTopicController theInstance = new PeerTopicController();

    public static PeerTopicController getSingleton() {
        return theInstance;
    }
    
    public PeerTopicController(){
    	
    	ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    	topicActivities = ActivitiesController.getSingleton();
		topicChat = ChatController.getSingleton();
		userId = Configurations.getUserConfig().getUser().getHash().toString();
		topicChat.localUser = Configurations.getUserConfig().getUser().getLocalNick();
    	try {
    		connectionFactory = new ActiveMQConnectionFactory(
                    ActiveMQConnection.DEFAULT_BROKER_URL);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = session.createTopic(TOPIC_NAME);
            consumer = session.createConsumer(topic);
			consumer.setMessageListener(new StoreListener());
            
        } catch (JMSException e) {
			e.printStackTrace();
		}
    }
    
    /*
     * 
     * method to set the chat channel 
     * 
     */
	public void setChannelListener(String channel){
		listenerChannel = channel;
		listenerChannelHash = Hashes.newHash(channel).toString();
		topicChat.joinChannel(listenerChannel);
		System.out.println("PeerTopicController acting on the channel: " + listenerChannel);
	}
	
	/*
	 * 
	 * method to save the chat activities
	 * 
	 */
	public void addTopicActivity(String channel, String nickname, String text){
		if(channel.equals(listenerChannel)){
			
			String now = ISO_DATE_FORMAT.format(new java.util.Date());
			topicActivities.addFeedEntry(userId, nickname + ": " + now + " " + text, null, true);
		}
		
	}
	
	/*
	 * 
	 * method to send the peer information periodically
	 * 
	 */
	public void sendingAggregatorInfo(){
		final int TOTAL_WAIT = 5 * 60 * 1000;
		while(true){
			
			int randomWait = (int) (TOTAL_WAIT * Math.random());
			System.out.println("Topic server: waiting for " + randomWait/1000 +
						" secs before sending my Id");
			try {
				Thread.currentThread().sleep(randomWait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			topicChat.sendMessage(listenerChannel, "aggregator_info:" 
					+ Configurations.getUserConfig().getUser().getHash().toString());
		}
	}
	
}
