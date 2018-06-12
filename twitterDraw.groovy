import okhttp3.*
import sun.misc.Request
import groovy.transform.Field
import groovy.json.*
import static groovy.json.JsonOutput.*

/**
 * The twitterDraw script select a random tweet between those which accomplish the requirements (follow salenda, cite @salenda and
 * has an image upload)
 * @Author Rubén Moreno, Alberto de Ávila
 */

@Grab (group = 'com.squareup.okhttp3', module = 'okhttp', version = '3.10.0')
@Field OkHttpClient client = new OkHttpClient()

performDraw()

/**
 * Main method of the script.
 */
void performDraw(){
    List<Map> tweetsList = []
    Map searchResult

    String token = performAuthentication()
    List followersList = new groovy.json.JsonSlurper().parseText(searchFollowers(token)).ids.flatten() //Obtain all our followers

    String result = searchTweets(token) //first search
    searchResult = new groovy.json.JsonSlurper().parseText(result)
    tweetsList << searchResult
    Long minimumTweetId = tweetsList*.statuses.id.flatten().min() //Obtain the minimum tweet's id of the response (older tweet)

    if(searchResult.statuses.size() == 100) { //The response only include 100 tweets per page
        while (searchResult.statuses.size() > 1) {
            result = searchTweets(token, minimumTweetId)
            searchResult = new groovy.json.JsonSlurper().parseText(result)
            tweetsList << searchResult
            minimumTweetId = tweetsList*.statuses.id.flatten().min()
        }
    }
    //Filter the tweets published by us
    tweetsList = tweetsList.statuses.flatten().findAll{ it?.user?.screen_name != 'salenda' && it?.user?.id in followersList}

    if(tweetsList.size() != 0){
        int random = Math.abs(new Random().nextInt() % tweetsList.size())
        printWinner(tweetsList[random])
    }else{
        println("No hay tweets que cumplan con los requisitos")
    }
}

/**
 * Performs the authentication with our twitter app to be able to perform a search against the twitter api
 * The data we must include to authenticate us is the attribute header with the 'Authorization' header including:
 * "Basic {Consumer Key from our app}:{Consumer Secret from our app}
 * @return loginResult.access_token the key that we have to include to the search with our authentication
 */
String performAuthentication(){
    RequestBody body = RequestBody.create(null, "grant_type=client_credentials")
    JsonSlurper slurper = new groovy.json.JsonSlurper()

    def request = new Request.Builder()
            .url("https://api.twitter.com/oauth2/token")
            .header('Authorization', "Basic " + "6TBUpWdIoolWlzqMjwd0ladhI:dHQCSmhAFQcxPo4QylfKvTU82LiIRrT5HRldpR7PqD4xkfckkI".bytes.encodeBase64().toString())
            .header('Content-Type', 'application/x-www-form-urlencoded;charset=UTF-8')
            .post(body)
            .build()

    Response response = client.newCall(request).execute()
    Map loginResult = slurper.parseText(response.body().string())
    loginResult.access_token
}

/**
 * Sends the search to the twitter's api.
 * In the url attribute is included the text that the tweet has to include, the requirement of the image in the tweet
 * and the filter to exclude retweets.
 * The result will only contain 100 tweets per page. For this reason we include the minimumTweetId, to repeat the search
 * in the next iteration excluding the previous results
 * @param token the key to authenticate us in the search
 * @param minimumTweetId the id of the most recent tweet to search
 * @return response of the api as a String
 */
String searchTweets(String token, Long minimumTweetId = null){
    request = new Request.Builder()
            .url("https://api.twitter.com/1.1/search/tweets.json?q=%23OpenExpo18%20%40salenda -RT filter%3Aimages&count=1000${minimumTweetId ? "&max_id=$minimumTweetId" : ''}")
            .header('Authorization', "Bearer " + token)
            .header('Content-Type', 'application/x-www-form-urlencoded;charset=UTF-8')
            .build()

    response = client.newCall(request).execute()
    response.body().string()
}

/**
 * Lists all our account's followers
 * @param token the key to authenticate us in the search
 * @return response of the api as a String
 */
String searchFollowers(String token){
    request = new Request.Builder()
            .url("https://api.twitter.com/1.1/followers/ids.json?screen_name=salenda")
            .header('Authorization', "Bearer " + token)
            .header('Content-Type', 'application/x-www-form-urlencoded;charset=UTF-8')
            .build()

    response = client.newCall(request).execute()
    response.body().string()
}

/**
 * Prints all the tweets resulting of the script execution. Is a method to check the results.
 * @param tweets the list with the results of the script execution
 */
void printTweets(List tweets) {
    String allTweetsText = ""
    tweets.each { Map tweetData ->
        allTweetsText += " Author: ${tweetData.user.name} (@${tweetData.user.screen_name}) Tweet: ${tweetData.text}\n"
    }
    println(allTweetsText)
}

/**
 * Prints the winner tweet
 * @param tweet
 */
void printWinner(Map tweet) {
    String tweetText = ""
    tweetText += "\nWINNER!: ${tweet.user.name} (@${tweet.user.screen_name})\nTweet: ${tweet.text}\nURL: http://twitter.com/${tweet.user.screen_name}/status/${tweet.id}\n"
    println(tweetText)
}