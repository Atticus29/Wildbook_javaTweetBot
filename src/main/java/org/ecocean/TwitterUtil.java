package org.ecocean;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;
import java.lang.Math;
import org.ecocean.servlet.ServletUtilities;
import org.joda.time.LocalDateTime;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.concurrent.TimeUnit;

import org.ecocean.media.TwitterAssetStore;
import org.ecocean.media.MediaAsset;
import org.ecocean.media.MediaAssetMetadata;
import org.ecocean.media.MediaAssetFactory;
import org.ecocean.identity.IBEISIA;

import com.google.gson.Gson;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.io.FileNotFoundException;

/*
import java.net.URL;
import java.io.File;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
*/
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.Status;

public class TwitterUtil {
  private static TwitterFactory tfactory = null;

  public static Twitter init(String context) {
    tfactory = getTwitterFactory(context);
    System.out.println("INFO: initialized TwitterUtil.tfactory");
    return tfactory.getInstance();
  }

  public static boolean isActive() {
    return (tfactory != null);
  }


  //https://dev.twitter.com/rest/public/search   e.g. "whaleshark filter:media"
  public static QueryResult findTweets(String search) throws TwitterException {
    return findTweets(search, -1l);
  }
  public static QueryResult findTweets(String search, long sinceId) throws TwitterException {
    Twitter tw = tfactory.getInstance();
    Query query = new Query(search);
    if (sinceId >= 0l){
      System.out.println("sinceId is " + Long.toString(sinceId) + " and is >= 0l");
      // query.setSince("2018-01-09");
      query.setSinceId(sinceId);
    }
    return tw.search(query);
  }

  public static Status getTweet(long tweetId) {
    Twitter tw = tfactory.getInstance();
    try {
      return tw.showStatus(tweetId);
    } catch (TwitterException tex) {
      System.out.println("ERROR: TwitterUtil.getTweet(" + tweetId + ") threw " + tex.toString());
      return null;
    } catch (Exception ex) {
      System.out.println("ERROR: TwitterUtil.getTweet(" + tweetId + ") threw " + ex.toString());
    }
    return null;
  }

  public static String toJSONString(Object obj) {
    String returnVal = null;
    Gson gson = new Gson();
    returnVal = gson.toJson(obj);
    if(returnVal == null){
      System.out.println("returnVal in toJSONString is null");
    }
    System.out.println(returnVal);
    return returnVal;
  }
  public static JSONObject toJSONObject(Object obj) {
    String s = toJSONString(obj);
    if (s == null){
      System.out.println("toJSONString is null");
      return null;
    }
    try {
      JSONObject j = new JSONObject(s);
      return j;
    } catch (JSONException ex) {
      System.out.println("ERROR: TwitterUtil.toJSONObject() could not parse '" + s + "' as JSON: " + ex.toString());
      return null;
    }
  }

  //http://twitter4j.org/en/configuration.html
  public static TwitterFactory getTwitterFactory(String context) {
    Properties props = ShepherdProperties.getProperties("twitter.properties", "", context);
    if (props == null) throw new RuntimeException("no twitter.properties");
    String debug = props.getProperty("debug");
    String consumerKey = props.getProperty("consumerKey");
    if ((consumerKey == null) || consumerKey.equals("")) throw new RuntimeException("twitter.properties missing consumerKey");  //hopefully enough of a hint
    String consumerSecret = props.getProperty("consumerSecret");
    String accessToken = props.getProperty("accessToken");
    String accessTokenSecret = props.getProperty("accessTokenSecret");
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled((debug != null) && debug.toLowerCase().equals("true"))
    .setOAuthRequestTokenURL("https://api.twitter.com/oauth2/request_token")
    .setOAuthAuthorizationURL("https://api.twitter.com/oauth2/authorize")
    .setOAuthAccessTokenURL("https://api.twitter.com/oauth2/access_token")
    .setOAuthConsumerKey(consumerKey)
    .setOAuthConsumerSecret(consumerSecret)
    .setOAuthAccessToken(accessToken)
    .setJSONStoreEnabled(true)
    .setOAuthAccessTokenSecret(accessTokenSecret);
    return new TwitterFactory(cb.build());
  }

  public static void addCourtesyTweetToQueue(String screenName, String mediaType,  Twitter twitterInst, String photoUrl, String pathToQueueFile) {
    String reply = null;
    if(mediaType.equals("photo") && photoUrl != null) {
      reply = "@" + screenName + ", thank you for the photo: " + photoUrl + "! Result pending!";
    } else {
      reply = "@" + screenName + ", thanks for the tweet! Could you send me a pic in a new tweet?";
    }
    try {
      addTweetToQueue(reply, twitterInst, pathToQueueFile);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void addPhotoSpecificCourtesyTweetToQueue(org.json.JSONArray emedia, String tweeterScreenName, Twitter twitterInst, String pathToQueueFile){
    int photoCount = 0;
    org.json.JSONObject jent = null;
    String mediaType = null;
    ArrayList<String> photoUrls = null;
    try{
      photoUrls = getPhotoUrls(emedia, tweeterScreenName, twitterInst);
      // System.out.println("PhotoURLS");
      // System.out.println(photoUrls);
    } catch(Exception e){
      System.out.println("couldn't get photo urls in sendPhotoSpecificCourtesyTweet method in twitterUtil");
      e.printStackTrace();
    }
    Long mediaEntityId = null;
    for(int j=0; j<emedia.length(); j++){
      try{
        jent = emedia.getJSONObject(j);
        mediaType = jent.getString("type");
        mediaEntityId = Long.parseLong(jent.getString("id"));
      } catch(Exception e){
        System.out.println("Error with JSONObject capture");
        e.printStackTrace();
      }
      try{
        if(mediaType.equals("photo") && photoUrls.get(j) != null){

          //@ATTN Jon, do we want it tweeting a courtesy ("got your image") tweet for every image, or just the first image in each tweet that may contain one or more images?
          TwitterUtil.addCourtesyTweetToQueue(tweeterScreenName, mediaType, twitterInst, photoUrls.get(j), pathToQueueFile);
          //For now, just one courtesy tweet per tweet, even if the tweet contains multiple images
          // if(photoCount<1){
          //   TwitterUtil.sendCourtesyTweet(tweeterScreenName, mediaType, twitterInst, photoUrls.get(j));
          // }
          // photoCount += 1;
        }
      } catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static ArrayList<String> getPhotoIds(org.json.JSONArray emedia, String tweeterScreenName, Twitter twitterInst) throws Exception{
    ArrayList<String> photoIds = new ArrayList<>();
    int photoCount = 0;
    org.json.JSONObject jent = null;
    for(int j=0; j<emedia.length(); j++){
      try{
        jent = emedia.getJSONObject(j);
        photoIds.add(jent.getString("id"));
      } catch(Exception e){
        System.out.println("Error with JSONObject capture getPhotoIds method");
        e.printStackTrace();
      }
    }
    if (photoIds ==null & photoIds.size()<1){
      throw new Exception ("photoIds was null or contained no elements");
    }
    return photoIds;
  }

  public static ArrayList<String> getPhotoUrls(org.json.JSONArray emedia, String tweeterScreenName, Twitter twitterInst) throws Exception{
    ArrayList<String> photoUrls = new ArrayList<>();
    int photoCount = 0;
    org.json.JSONObject jent = null;
    // Long mediaEntityId = null;
    for(int j=0; j<emedia.length(); j++){
      try{
        jent = emedia.getJSONObject(j);
        System.out.println("photoUrl:");
        System.out.println(jent.getString("mediaURLHttps"));
        photoUrls.add(jent.getString("mediaURLHttps"));
      } catch(Exception e){
        System.out.println("Error with JSONObject capture getPhotoUrls method");
        e.printStackTrace();
      }
    }
    if (photoUrls ==null & photoUrls.size()<1){
      throw new Exception ("photoUrls was null or contained no elements");
    }
    return photoUrls;
  }


  public static JSONObject makeParentTweetMediaAssetAndSave(Shepherd myShepherd, TwitterAssetStore tas, Status tweet, JSONObject tj){
    myShepherd.beginDBTransaction();
    try{
      MediaAsset ma = tas.create(Long.toString(tweet.getId()));  //parent (aka tweet)
      ma.addLabel("_original");
      MediaAssetMetadata md = ma.updateMetadata();
      MediaAssetFactory.save(ma, myShepherd);
      // JSONObject test = TwitterUtil.toJSONObject(ma);
      tj.put("maId", ma.getId());
      tj.put("metadata", ma.getMetadata().getData());
      System.out.println(tweet.getId() + ": created tweet asset " + ma);
      myShepherd.commitDBTransaction();
      return tj;
    } catch(Exception e){
      myShepherd.rollbackDBTransaction();
      e.printStackTrace();
      return tj;
    }
  }

  public static JSONArray saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis(List<MediaAsset> mas, Long tweetID, Shepherd myShepherd, JSONObject tj, HttpServletRequest request, JSONArray tarr, JSONArray iaPendingResults, ArrayList<String> photoIds, ArrayList<String> photoUrls){
    if ((mas == null) || (mas.size() < 1)) {
      System.out.println("mas was null or size less than 1 in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis");
    } else {
      JSONArray jent = new JSONArray();
      for(int i=0; i<mas.size(); i++){
        System.out.println("Total number of media assets to looop through in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis: " + Integer.toString(mas.size()));
        MediaAsset ent = mas.get(i);
        // myShepherd.beginDBTransaction();
        try {
          JSONObject ej = new JSONObject();
          // MediaAssetMetadata entMd = ent.updateMetadata();
          MediaAssetFactory.save(ent, myShepherd);
          System.out.println("Ent's mediaAssetID in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis is " + ent.toString());
          String taskId = IBEISIA.IAIntake(ent, myShepherd, request);
          System.out.println("taskId in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis for entity " + Integer.toString(i) + " is: " + taskId);
          ej.put("maId", ent.getId());
          ej.put("taskId", taskId);
          ej.put("creationDate", new LocalDateTime());

          String tweeterScreenName = tj.getJSONObject("tweet").getJSONObject("user").getString("screenName");
          System.out.println("tweeterScreenName in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis is: " + tweeterScreenName);
          ej.put("tweeterScreenName", tweeterScreenName);
          if(photoIds.size() != mas.size()){
            System.out.println("Yikes! photoIds is not the same size as mas");
          } else{
            ej.put("photoId", photoIds.get(i));
          }
          if(photoUrls.size() != mas.size()){
            System.out.println("Yikes! PhotoUrls note the same size as mas");
          } else {
            System.out.println("photoUrl in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis method is: " + photoUrls.get(i));
            ej.put("photoUrl", photoUrls.get(i));
          }
          // jent = emedia.getJSONObject(j);
          // mediaType = jent.getString("type");
          // mediaEntityId = Long.parseLong(jent.getString("id"));
          jent.put(ej);
          iaPendingResults.put(ej);
          // myShepherd.getPM().makePersistent(ej); //maybe?
          // myShepherd.commitDBTransaction();
        } catch(Exception e){
          myShepherd.rollbackDBTransaction();
          System.out.println("something went wrong in the big series of commands in saveEntitiesAsMediaAssetsToSheperdDatabaseAndSendEachToImageAnalysis");
          e.printStackTrace();
        }
      }
      tj.put("entities", jent);
    }
    tarr.put(tj);
    return iaPendingResults;
  }


  public static void addDetectionAndIdentificationTweetToQueue(String screenName, String imageUrl, Twitter twitterInst, String whaleId, boolean detected, boolean identified, String info, String rootDir, String pathToQueueFile){ //@TODO update the calls to this
    //Note: right now, a successful detection and unsuccessful identification and a successful detection + identification but no markedIndividual match are treated with a false for the identified boolean in this method
    System.out.println("Entered addDetectionAndIdentificationTweetToQueue");
    String tweet = null, tweet2 = null;
    if(detected && identified){
      tweet = "@" + screenName + ", we detected a whale in " + imageUrl + " and identified it as " + whaleId + "!";
      tweet2 = "@" + screenName + ", here's some info on " + whaleId + ": " + info; //TODO flesh out either by pulling info from db now that whaleId is available, or by passing some info as an additional argument in this method
    } else if(detected && !identified){
      tweet =  "@" + screenName + ", we detected a whale in " + imageUrl + " but we were not able to identify it.";
      tweet2 = "@" + screenName + ", we added your whale to our database here: " + info;
    } else {
      tweet =  "@" + screenName + ", we were not able to identify a whale in " + imageUrl + ".";
      tweet2 = "@" + screenName + ", if you'd like to make a manual submission for the image " + imageUrl + ", please go to http://www.flukebook.org/submit.jsp";
    }

    try {
      addTweetToQueue(tweet, twitterInst, pathToQueueFile);
      addTweetToQueue(tweet2, twitterInst, pathToQueueFile);
      try{
        System.out.println("Entered try for removeEntryFromPendingIaByImageUrl");
        removeEntryFromPendingIaByImageUrl(imageUrl, rootDir);
        System.out.println("Got past try for removeEntryFromPendingIaByImageUrl");
      } catch(Exception f){
        System.out.println("removeEntryFromPendingIaByImageId failed inside addDetectionAndIdentificationTweetToQueue method");
        f.printStackTrace();
      }
    } catch(Exception e){
      System.out.println("addTweetToQueue failed inside addDetectionAndIdentificationTweetToQueue method");
      e.printStackTrace();
    }
  }

  public static void addTimeoutTweetToQueue(String screenName, Twitter twitterInst, String imageUrl, String rootDir, String pathToQueueFile) {
    String reply = "@" + screenName + ", analysis for image " + imageUrl + " couldn't be processed within 24 hrs.";
    String reply2 = "@" + screenName + ", if you'd like to make a manual submission, please go to http://www.flukebook.org/submit.jsp";
    try {
      addTweetToQueue(reply, twitterInst, pathToQueueFile);
      addTweetToQueue(reply2, twitterInst, pathToQueueFile);
      try{
        System.out.println("Entered try for removeEntryFromPendingIaByImageUrl");
        removeEntryFromPendingIaByImageUrl(imageUrl, rootDir);
        System.out.println("Got past try for removeEntryFromPendingIaByImageUrl");
      } catch(Exception f){
        System.out.println("removeEntryFromPendingIaByImageId failed inside addDetectionAndIdentificationTweetToQueue method");
        f.printStackTrace();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static String createTweet(String tweet, Twitter twitterInst) throws TwitterException {
    String returnVal = null;
    try {
      Status status = twitterInst.updateStatus(tweet);
      returnVal = status.getText();
    } catch(TwitterException e) {
      e.printStackTrace();
    }
    return returnVal;
  }

  public static JSONArray removePendingEntry(JSONArray pendingResults, int index){
    ArrayList<JSONObject> list = new ArrayList<>();
    for(int i = 0; i < pendingResults.length(); i++){
      if(i == index){
        continue;
      } else {
        list.add(pendingResults.getJSONObject(i));
      }
    }
    return new JSONArray(list);
  }

  public static void removeEntryFromPendingIaByImageUrl(String imageUrl, String rootDir) throws Exception{
    try{
      removeEntryFromPendingIaByGenericString("photoUrl", imageUrl, rootDir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception ("removeEntryFromPendingIaByImageUrl in TwitterUtil.java failed");
    }
  }

  public static void removeEntryFromPendingIaByImageId(String imageId, String rootDir) throws Exception{
    try{
      removeEntryFromPendingIaByGenericString("photoId", imageId, rootDir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception ("removeEntryFromPendingIaByImageId in TwitterUtil.java failed");
    }
  }

  public static void removeEntryFromPendingIaByGenericString(String targetLabel, String id, String rootDir) throws Exception{ //TODO this is ugly and could be made DRYer -Mark F.
    System.out.println("Entered removeEntryFromPendingIaByGenericString");
    ArrayList<JSONObject> list = new ArrayList<>();
    String iaPendingResultsFile = "/pendingAssetsIA.json";
    JSONArray iaPendingResults = null;
    String dataDir = ServletUtilities.dataDir("context0", rootDir);
    try {
      String iaPendingResultsAsString = Util.readFromFile(dataDir + iaPendingResultsFile);
      System.out.println(iaPendingResultsAsString);
      iaPendingResults = new JSONArray(iaPendingResultsAsString);
    } catch(Exception e){
      System.out.println("Failed to open iaPendingResults from file in TwitterUtil.java removeEntryFromPendingIaByGenericString");
      e.printStackTrace();
    }
    for(int i = 0; i < iaPendingResults.length(); i++){
      System.out.println("got into entry for loop of removeEntryFromPendingIaByGenericString");
      JSONObject entry = iaPendingResults.getJSONObject(i);
      if(entry.getString(targetLabel).equals(id)){
        System.out.println(targetLabel + " of " + id + " was detected in iaPendingResultsFile; removing now!");
        continue;
      } else {
        list.add(iaPendingResults.getJSONObject(i));
      }
    }
    JSONArray results = new JSONArray(list);
    System.out.println("iaPendingResultsFile will read: " + results.toString());
    System.out.println("Destination directory is " + dataDir + iaPendingResultsFile);
    try{
      Util.writeToFile(results.toString(), dataDir + iaPendingResultsFile);
      System.out.println("successfully wrote pendingResultsFile content to file");
    } catch(Exception e){
      System.out.println("Failed to re-write iaPendingResultsFile in removeEntryFromPendingIaByGenericString in TwitterUtil.java");
      e.printStackTrace();
    }
  }

  public static Long getSinceIdFromTwitterTimeStampFile(String path) throws Exception{
    Long sinceId = null;
    try{
    	// the timestamp is written with a new line at the end, so we need to strip that out before converting
      String timeStampAsText = Util.readFromFile(path); //dataDir + twitterTimeStampFile
      timeStampAsText = timeStampAsText.replace("\n", "");
      sinceId = Long.parseLong(timeStampAsText, 10);
    } catch(FileNotFoundException e){
    	e.printStackTrace();
    } catch(IOException e){
    	e.printStackTrace();
    } catch(NumberFormatException e){
    	e.printStackTrace();
    }
    if(sinceId != null){
        return sinceId;
    } else{
      throw new Exception ("sinceId in getSinceIdFromTwitterTimeStampFile is null");
    }
  }

  public static String findImageIdInIaPendingLogFromTaskId(String taskId, String rootDir) throws Exception{
    String returnVal = null;
    try{
      return findGenericStringItemInIaPendingLogFromTaskId("photoId", taskId, rootDir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception ("findImageIdInIaPendingLogFromTaskId in TwitterUtil.java failed");
    }
  }

  public static String findScreenNameInIaPendingLogFromTaskId(String taskId, String rootDir) throws Exception{
    String returnVal = null;
    try{
      return findGenericStringItemInIaPendingLogFromTaskId("tweeterScreenName", taskId, rootDir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception ("findScreenNameInIaPendingLogFromTaskId in TwitterUtil.java failed");
    }
  }

  public static String findGenericStringItemInIaPendingLogFromTaskId(String target, String taskId, String rootDir) throws Exception{
    String returnVal = null;
    String dataDir = ServletUtilities.dataDir("context0", rootDir);
    String iaPendingResultsFile = "/pendingAssetsIA.json";
    try {
    	String iaPendingResultsAsString = Util.readFromFile(dataDir + iaPendingResultsFile);
    	JSONArray iaPendingResults = new JSONArray(iaPendingResultsAsString);
      for(int i =0; i<iaPendingResults.length(); i++){
        JSONObject entry = iaPendingResults.getJSONObject(i);
        if (entry.getString("taskId").equals(taskId)){
          returnVal = entry.getString(target);
          break;
        }
      }
    } catch(Exception e){
    	e.printStackTrace();
    }
    if (returnVal != null){
      return returnVal;
    } else{
      throw new Exception ("imageId in findGenericStringItemInIaPendingLogFromTaskId was null");
    }
  }

  public static String findImageUrlInIaPendingLogFromTaskId(String taskId, String rootDir) throws Exception{
    String returnVal = null;
    try{
      return findGenericStringItemInIaPendingLogFromTaskId("photoUrl", taskId, rootDir);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception ("findImageUrlInIaPendingLogFromTaskId in TwitterUtil.java failed");
    }
  }

  public static String getQueryUUIDFromJSONIdentificaitonResult(JSONObject JSONResult) throws Exception{
    String UUID = null;
    try{
      UUID = JSONResult.getJSONObject("response").getJSONObject("json_result").getJSONArray("query_annot_uuid_list").getJSONObject(0).getString("__UUID__");
    } catch(Exception e){
      e.printStackTrace();
    }
    if(UUID != null){
      return UUID;
    } else{
      throw new Exception("UUID is null in getQueryUUIDFromJSONResult method");
    }
  }

  public static ArrayList<Double> getArrayOfConfidencesFromJSONIdentificaitonResult(JSONObject JSONResult) throws Exception{
    ArrayList<Double> finalConfidences = new ArrayList<Double>();
    JSONArray confidences = null;
    try{
      confidences = JSONResult.getJSONObject("response").getJSONObject("json_result").getJSONObject("inference_dict").getJSONObject("annot_pair_dict").getJSONArray("confidence_list");
    } catch(Exception e){
      // e.printStackTrace();
      System.out.println("Getting the confidence list in getArrayOfConfidencesFromJSONIdentificaitonResult failed. Possible that the result passed to it was weird: " + JSONResult.toString());
    }
    if(confidences != null){
      for(int i = 0; i<confidences.length(); i++){
        String currentConfidenceString = confidences.getString(i);
        Double confidence = Double.parseDouble(currentConfidenceString);
        finalConfidences.add(confidence);
      }
    }
    if(finalConfidences.size() > 0){
      return finalConfidences;
    } else {
      throw new Exception ("finalConfidences array in getArrayOfConfidences method in twitterUtil was empty");
    }
  }

  public static ArrayList<String> getArrayOfUUIDsFromJSONIdentificationResult(JSONObject JSONResult) throws Exception{ //@TODO test this for a case where there is more than one result
    ArrayList<String> finalUUIDs = new ArrayList<String>();
    JSONArray review_pair_list = JSONResult.getJSONObject("response").getJSONObject("json_result").getJSONObject("inference_dict").getJSONObject("annot_pair_dict").getJSONArray("review_pair_list");
    for(int i = 0; i < review_pair_list.length(); i++){
      String isItUUID1OrUUID2;
      String currentUUID1 = review_pair_list.getJSONObject(i).getJSONObject("annot_uuid_1").getString("__UUID__");
      String currentUUID2 = review_pair_list.getJSONObject(i).getJSONObject("annot_uuid_2").getString("__UUID__");
      String uuid = TwitterUtil.getQueryUUIDFromJSONIdentificaitonResult(JSONResult);
      if(currentUUID1.equals(uuid)){
        isItUUID1OrUUID2 = "2";
      } else if(currentUUID2.equals(uuid)){
        isItUUID1OrUUID2 = "1";
      } else{
        throw new Exception("uuid of query doesn't match uuid1 or uuid2 in getArrayOfUUIDs method. This may not be damning... it may just mean that I didn't understand how this would work. -Mark Fisher");
      }
      if (isItUUID1OrUUID2.equals("1")){
        finalUUIDs.add(currentUUID1);
      } else if (isItUUID1OrUUID2.equals("2")){
        finalUUIDs.add(currentUUID2);
      }
    }
    if(finalUUIDs.size() > 0){
      System.out.println(finalUUIDs);
      return finalUUIDs;
    } else{
      throw new Exception("finalUUIDs was zero length in getArrayOfUUIDs method in twitterUtil");
    }
  }

  public static String getUUIDOfBestMatchFromIdentificationJSONResults(JSONObject JSONResult) throws Exception{
    String returnVal;
    ArrayList<Double> confidences = getArrayOfConfidencesFromJSONIdentificaitonResult(JSONResult);
    if(confidences.size() < 1){
      returnVal = "";
    }
    int maxIndex = Util.getIndexOfMax(confidences);
    if(confidences.get(maxIndex) < IBEISIA.getIdentificationCutoffValue()){
      returnVal = "";
    }
    //@TODO check whether max confidence beats our cutoff! If not, throw exception.
    ArrayList<String> correspondingUUIDs = getArrayOfUUIDsFromJSONIdentificationResult(JSONResult);
    returnVal = correspondingUUIDs.get(maxIndex);
    if(returnVal != null){
      return returnVal;
    } else{
      throw new Exception("returnVal was null in getUUIDOfBestMatchFromIdentificationJSONResults method in TwitterUtil");
    }
  }

  public static boolean isSuccessfulDetection(JSONObject jsonResult){
    try{
      String species = jsonResult.getJSONObject("response").getJSONObject("json_result").getJSONArray("results_list").getJSONArray(0).getJSONObject(0).getString("species");
      return species != null;
    } catch(Exception e){
      // e.printStackTrace();
      return false;
    }
  }

  public static void sendThisManyTweetsFromTheQueue(int totalTweetCount, String pathToQueueFile, Twitter twitterInst){
    String tweetQueueAsText = null;
    if(pathToQueueFile != null){
      try{
        tweetQueueAsText = Util.readFromFile(pathToQueueFile);
      } catch(Exception e){
        e.printStackTrace();
      }
      if(tweetQueueAsText != null){
        JSONArray tweetQueue = new JSONArray(tweetQueueAsText);
        int tweetCount = Math.min(totalTweetCount, tweetQueue.length()); //just in case totalTweetCount is larger than the remaining tweets
        for(int i = 0; i < tweetCount; i++){
          JSONObject currentEntry = tweetQueue.getJSONObject(i);
          String currentReply = currentEntry.getString("reply");
          try{
            createTweet(currentReply, twitterInst);
            removeTweetFromQueue(currentReply, twitterInst, pathToQueueFile);
          } catch(TwitterException e){
            e.printStackTrace();
          } catch(Exception f){
            f.printStackTrace();
          }
          //@TODO add a ten second delay here
          try{
            TimeUnit.SECONDS.sleep(10);
          } catch(Exception e){
            e.printStackTrace();
          }
        }
      }
    }
  }

  public static void addTweetToQueue(String reply, Twitter twitterInst, String pathToQueueFile) throws Exception{
    if(pathToQueueFile != null){
      String tweetQueueAsText = Util.readFromFile(pathToQueueFile);
      JSONArray tweetQueue = new JSONArray(tweetQueueAsText);
      JSONObject newEntry = new JSONObject();
      newEntry.put("creationDate", new LocalDateTime());
      newEntry.put("reply", reply);
      tweetQueue.put(newEntry);
      tweetQueueAsText = tweetQueue.toString();
    	Util.writeToFile(tweetQueueAsText, pathToQueueFile);
    } else{
      throw new Exception ("pathToQueueFile was null in addTweetToQueue method");
    }
  }

  public static void removeTweetFromQueue(String reply, Twitter twitterInst, String pathToQueueFile) throws Exception{
    if(pathToQueueFile != null){
      String tweetQueueAsText = Util.readFromFile(pathToQueueFile);
      JSONArray tweetQueue = new JSONArray(tweetQueueAsText);
      JSONArray newTweetQueue = new JSONArray();
      for(int i=0; i<tweetQueue.length(); i++){
        JSONObject currentEntry = tweetQueue.getJSONObject(i);
        String currentReply = currentEntry.getString("reply");
        System.out.println("currentReply is: " + currentReply);
        if(currentReply.equals(reply)){
          System.out.println("We have an entry that's equal: " + reply);
          continue;
        }
        newTweetQueue.put(currentEntry);
      }
      Util.writeToFile(newTweetQueue.toString(), pathToQueueFile);
    } else{
      throw new Exception("pathToQueueFile was null in removeTweetFromQueue method");
    }
  }

  public static void updatePendingResultsWithNewIdentificationTaskID(String oldTaskId, String newTaskId, String rootDir, String encounterCatalogNumber){
    JSONArray returnVal = null;
    String dataDir = ServletUtilities.dataDir("context0", rootDir);
    String iaPendingResultsFile = "/pendingAssetsIA.json";
    JSONArray newJSONArray = new JSONArray();
    try {
    	String iaPendingResultsAsString = Util.readFromFile(dataDir + iaPendingResultsFile);
    	JSONArray iaPendingResults = new JSONArray(iaPendingResultsAsString);
      for(int i =0; i<iaPendingResults.length(); i++){
        JSONObject entry = iaPendingResults.getJSONObject(i);
        if (entry.getString("taskId").equals(oldTaskId)){
          entry.put("taskId", newTaskId);
          entry.put("encounterCatalogNumber", encounterCatalogNumber);
        }
        newJSONArray.put(entry);
      }
      Util.writeToFile(newJSONArray.toString(), dataDir + iaPendingResultsFile);
    } catch(Exception e){
    	e.printStackTrace();
    }
  }
}
