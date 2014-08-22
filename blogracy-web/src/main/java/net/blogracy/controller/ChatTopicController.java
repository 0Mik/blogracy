package net.blogracy.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import javax.xml.parsers.*;

import net.blogracy.config.Configurations;
import net.blogracy.model.hashes.Hashes;

import org.apache.shindig.social.opensocial.model.ActivityEntry;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ChatTopicController implements javax.jms.MessageListener  {
	
	static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
	static final String CACHE_FOLDER = Configurations.getPathConfig()
            .getCachedFilesDirectoryPath();
	
	private String userId;
	private ActivitiesController topicActivities;
	private ChatController topicChat;
	private DistributedHashTable topicDht;
	
	private ArrayList<String> activeChannels = new ArrayList<String>();
	private HashMap<String, ArrayList<String>> followersChannels = new HashMap<String, ArrayList<String>>();
	public HashMap<String, String> aggregatorsID = new HashMap<String, String>();
	
	private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Destination topic;
    private MessageConsumer consumer;
    private static final String TOPIC_NAME = "CHAT.DEMO";

	
	private static final ChatTopicController theInstance = new ChatTopicController();
	
	public static ChatTopicController getSingleton() {
        return theInstance;
    }
	
	public ChatTopicController(){
		
		ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		topicActivities = ActivitiesController.getSingleton();
		topicChat = ChatController.getSingleton();
		topicChat.localUser = Configurations.getUserConfig().getUser().getLocalNick();
		topicDht = DistributedHashTable.getSingleton();
		userId = Configurations.getUserConfig().getUser().getHash()
                .toString();
		updateChannels();
		try {
            connectionFactory = new ActiveMQConnectionFactory(
                    ActiveMQConnection.DEFAULT_BROKER_URL);
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = session.createTopic(TOPIC_NAME);
            consumer = session.createConsumer(topic);
            consumer.setMessageListener(this);
        } catch (Exception e) {
            System.out.println("JMS error: creating the text listener");
        }
	}
	
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
	
	/*
	 * 
	 * method to register a channel and join it
	 * 
	 */
	public void insertTopicChannel(String channel){
		if(!activeChannels.contains(channel)){
			
			topicChat.joinChannel(channel);
			activeChannels.add(channel);
			//insert the channel in a recordFile
			try {
				File chanList = new File(CACHE_FOLDER + File.separator + "chanList.txt");
				if(chanList.exists()){
					
					FileWriter  w = new FileWriter(chanList, true);
				    w.write(channel + "\n");
				    w.close();
				}
				else{
					
					FileWriter  w = new FileWriter(chanList);
					w.write(channel + "\n");
					w.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			
			topicChat.joinChannel(channel);
		}
	}
	
	/*
	 * 
	 * method to register a followed users channel
	 * 
	 */
	public void insertFollowersChannel(String followerId, String followerChannel){
		if(followersChannels.containsKey(followerId)){
			if(!((followersChannels.get(followerId)).contains(followerChannel))){
					followersChannels.get(followerId).add(followerChannel);
					try {
						File followersList = new File(CACHE_FOLDER + File.separator + "followersList.txt");
						if(followersList.exists()){
							
							FileWriter  w = new FileWriter(followersList, true);
						    w.write(followerId + " " + followerChannel + "\n");
						    w.close();
						}
						else{
							
							FileWriter  w = new FileWriter(followersList);
							w.write(followerId + " " + followerChannel + "\n");
							w.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		else{
			
			ArrayList<String> followersTag = new ArrayList<String>();
			followersTag.add(followerChannel);
			followersChannels.put(followerId, followersTag);
			try {
				File followersList = new File(CACHE_FOLDER + File.separator + "followersList.txt");
				if(followersList.exists()){
					
					FileWriter  w = new FileWriter(followersList, true);
				    w.write(followerId + followerChannel + "\n");
				    w.close();
				}
				else{
					
					FileWriter  w = new FileWriter(followersList);
					w.write(followerId + followerChannel + "\n");
					w.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * 
	 * method to retrieve followed user's feed for a specific channel
	 * 
	 */
	public List<ActivityEntry> getFollowersChannel(String followerId, String followerChannel){
		
		List<ActivityEntry> result = new ArrayList<ActivityEntry>();
		if(followersChannels.containsKey(followerId)){
			
			topicDht.lookup(followerId);
			ArrayList<String> followerChannels = followersChannels.get(followerId);
			if(!followerChannels.isEmpty()){
				if(followerChannel == null){
					
					Iterator<String> it = followerChannels.iterator();
					while(it.hasNext()){
						
						String channel = it.next();
						result.addAll(topicActivities.getFeed(followerId, 0, channel));
					}
				}
				else{
					if(followerChannels.contains(followerChannel)){
						
						result.addAll(topicActivities.getFeed(followerId, 0, followerChannel));
					}
					else{
						
						System.err.println("Follower channel not present.");
					}
				}
			}
			else{
					
				System.err.println("Follower channels is empty.");
			}
		}
		else{
			
			System.err.println("Follower not registrated");
		}
		return result;
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
			
			if(type.equals("chat") && activeChannels.contains(channel)){ 	
				String aggregatorID = checkAggregatorId(text);
				if(aggregatorID == null){
					
					addTopicActivity(channel, nick, text);
				}
				else{
					aggregatorsID.put(aggregatorID, channel);
					topicDht.lookup(aggregatorID);
				}
			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
 	/*
 	 * 
 	 * method to obtain the list of hashtag in a text message
 	 * 
 	 */
	public ArrayList<String> checkMessageHashtag(String message){
		ArrayList<String> hashtag = new ArrayList<String>();
		while(message.contains("#")){
			
			int hashtagIndex = message.indexOf("#");
			int hashtagEndsIndex = message.indexOf(" ", hashtagIndex);
			if((hashtagEndsIndex < 0) || (hashtagEndsIndex > (message.length()) ))
				hashtagEndsIndex = message.length();
			
			hashtag.add(message.substring(hashtagIndex + 1, hashtagEndsIndex));
			message = message.replaceAll(message.substring(hashtagIndex, hashtagEndsIndex), "");
		}
		return hashtag;
	}
	
	/*
	 * 
	 * method to check the presence of the aggregator ID in a message
	 * 
	 */
	public String checkAggregatorId(String message){
		
		String aggregatorId = null;
		if(message.contains("aggregator_info:")){
			
			aggregatorId = message.replaceAll("aggregator_info:", "");
		}
		return aggregatorId;
	}

	/*
	 * 
	 * method to create a topic channel from a text message
	 * 
	 */
	public void createTopicActivity(String message){
		if(message.contains("#")){
			
			String text = message;
			ArrayList<String> tags = new ArrayList<String>();
			while(message.contains("#")){
			
				int hashtagIndex = message.indexOf("#");
				int hashtagEndsIndex = message.indexOf(" ", hashtagIndex);
				if((hashtagEndsIndex < 0) || (hashtagEndsIndex > (message.length()) ))
					hashtagEndsIndex = message.length();
				String hashtag = message.substring(hashtagIndex + 1, hashtagEndsIndex);
				tags.add(hashtag);
				message = message.replaceAll(message.substring(hashtagIndex, hashtagEndsIndex), "");
			}
			Iterator<String> it = tags.iterator();
			while(it.hasNext()){
			
				String tag = it.next();
				insertTopicChannel(tag);                 //join the channel/add channel
				topicChat.sendMessage(tag, text);		 //send msg to the channel
			}
		}
		else{
			
			System.out.println("No tags included");
		}
	}
	
	/*
	 * 
	 * method to save topic message in a special activity 
	 * 
	 */
	public void addTopicActivity(String channel, String nickname, String text){
		if(activeChannels.contains(channel)){
			
			String now = ISO_DATE_FORMAT.format(new java.util.Date());
			String channelHash = Hashes.newHash(channel).toString();
			topicActivities.addFeedEntry(channelHash, nickname + ": " + now + " " + text, null);
		}
		else{
			
			System.err.println("Channel not registered.");
		}
	}
	
	/*
	 * 
	 * method to get topic activity of users
	 * 
	 */
	public List<ActivityEntry> getUserTopicActivity(String user, String tag){
		
		List<ActivityEntry> result = new ArrayList<ActivityEntry>();
		if(user.equals(userId)){
			File chanList = new File(CACHE_FOLDER + File.separator + "chanList.txt");
			try {
				
				FileReader r = new FileReader(chanList);
				Scanner in = new Scanner(r);
				while(in.hasNextLine()){
					
					String channel = in.nextLine();
					if(tag == null){
						
						result.addAll(topicActivities.getFeed(userId, 0, channel));
					}
					else{
						if(channel.equals(tag)){
							
							result.addAll(topicActivities.getFeed(userId, 0, channel));
						}
					}
				}
				r.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			
			result = getFollowersChannel(user, tag);
		}
		return result;
	}
	
	/*
	 * 
	 * method to get topic activity
	 * 
	 */
	public List<ActivityEntry> getTopicActivity(String channel){
		
		List<ActivityEntry> result = new ArrayList<ActivityEntry>();
		if(activeChannels.contains(channel)){
			
			String channelHash = Hashes.newHash(channel).toString();
			result.addAll(topicActivities.getFeed(channelHash, 2, null));
		}
		else{
			
			System.err.println("Channel not registered.");
		}
		return result;
		
	}
	
	/*
	 * 
	 * method to obtain the list of registered channels
	 * 
	 */
	public ArrayList<String> getUserChannels(String user){
		
		if(user.equals(userId)){
			
			return activeChannels;
		}
		else{
			
			return followersChannels.get(user);
		}
	}
	
	/*
	 * 
	 * method to refresh at startup the list of followed channels
	 * 
	 */
	private void updateChannels(){
		//retrieving registered channels
		try {
			File chanList = new File(CACHE_FOLDER + File.separator + "chanList.txt");
			if(chanList.exists()){
		
				FileReader r = new FileReader(chanList);
				BufferedReader br = new BufferedReader(r);
				while(true){
					
					String chan = br.readLine();
					if(chan!=null){
						activeChannels.add(chan);
						topicChat.joinChannel(chan);
					}
					else break;
				}
			}
			else System.err.println("chanFile is not present.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//retrieving followers channels
		try{
			File followersList = new File(CACHE_FOLDER + File.separator + "followersList.txt");
			if(followersList.exists()){
		
				FileReader r = new FileReader(followersList);
				BufferedReader br = new BufferedReader(r);
				while(true){
					
					String chan = br.readLine();
					if(chan!=null){
						String[] idAndChannel = chan.split(" ", 2);
						String followerId = idAndChannel[0];
						String followerChannel = idAndChannel[1];
						if(followersChannels.containsKey(followerId)){
							
							if(!((followersChannels.get(followerId)).contains(followerChannel)))
									followersChannels.get(followerId).add(followerChannel);
						}
						else{
							ArrayList<String> followersTag = new ArrayList<String>();
							followersTag.add(followerChannel);
							followersChannels.put(followerId, followersTag);
						
						}
					}
					else break;
				}
			}
			else System.err.println("followersFile is not present.");
		}catch(IOException e1){
			e1.printStackTrace();
		}
	}

}
