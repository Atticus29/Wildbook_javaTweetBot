<%@ page contentType="text/plain; charset=utf-8" language="java"

import="org.ecocean.*,
java.util.ArrayList,
java.io.FileNotFoundException,
java.io.IOException,
java.util.List,
java.io.BufferedReader,
java.io.IOException,
java.io.InputStream,
java.io.InputStreamReader,
java.io.File,
java.util.Date,
org.json.JSONObject,
org.json.JSONArray,
org.ecocean.identity.IBEISIA,
twitter4j.QueryResult,
twitter4j.Status,
twitter4j.*,
org.ecocean.servlet.ServletUtilities,
org.ecocean.media.*,
org.ecocean.ParseDateLocation.*,
java.util.concurrent.ThreadLocalRandom,
org.joda.time.DateTime,
org.joda.time.Interval
              "
%>

<%
String context = ServletUtilities.getContext(request);
String rootDir = request.getSession().getServletContext().getRealPath("/");
String dataDir = ServletUtilities.dataDir("context0", rootDir);
String twitterTestTimeStampFile = "/twitterTestTimeStampFile.txt";
Long timeStamp = 890302524275662848L;

// Twitter Instance
Twitter twitterInst = TwitterUtil.init(request);

// ### Retrieve timestamp for the last twitter check ###

try{
	// the timestamp is written with a new line at the end, so we need to strip that out before converting
  String timeStampAsText = Util.readFromFile(dataDir + twitterTestTimeStampFile);
  timeStampAsText = timeStampAsText.replace("\n", "");
  timeStamp = Long.parseLong(timeStampAsText, 10);
} catch(FileNotFoundException e){
	e.printStackTrace();
} catch(IOException e){
	e.printStackTrace();
} catch(NumberFormatException e){
	e.printStackTrace();
}
out.println("Timestamp: " + timeStamp);

// ###           End timestamp retrieval            ###

 
// ### Test different tweets against the Twitterbot ###

// Test simple tweet
String simpleTweetText = "@wildmetweetbot! I saw a whale!";

// Test tweet with date?
String dateTweetText = "@wildmetweetbot, I saw a whale on June 3, 2017!";

// Test tweet with one image
String oneImageTweetText = "@wildmetweetbot, I took a picture of a whale!";
// Test tweet with multiple images
String multImageTweetText = "@wildmetweetbot, I took some pictures of whales!";

// ###              End tweet tests                 ###

%>