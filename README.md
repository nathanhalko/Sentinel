# Sentinel

The Sentinels are a fictional variety of mutant-hunting robots.
They also might be good at detecting non-human 
robotic type entities on Twitter.

# Setup

### SBT

An sbt script is included in the project directory.  Enter the
sbt console and run the learining scripts or get to the scala repl:

```
projectDir> ./sbt

sbt> testOnly *RunLogisticRegressionExa

sbt> console 
```

We kind of abuse the test framework to just give us a runnable container
for the learning algorithms, but it works quite nicely.

### Mongo DB

TwitterCrawler fetches data from the twitter api and stores it in a 
local [MongoDB](http://mongodb.github.io/mongo-scala-driver/2.0/) instance
at the default location /data/db. Feature creation then reads from Mongo and
creates csv files for ingestion by the learning algorithms. These are stored
in $projectRoot/data.

Its possible to run the learning algorithms with just the supplied
csv files, however if you'd like to get at the source try

On mac
```
> brew install mongo
```

To start
```
> mongod
```


[Github](https://github.com/mongodb/mongo-scala-driver) docs for the scala driver.

## Twitter API keys

We use [twitter4j](http://twitter4j.org/en/index.html) to access the api.
Create a twitter [app](https://apps.twitter.com/) to obtain your own api keys.
Fill them in `src/main/resources/twitter4j.properties`.

NOTE: The keys provided here for DataSummit 2017 are my own active keys, feel 
free to use them but realize others might also be hacking away at the rate limits.

| Docs | twitter4j | twitter api |
|------|-----------|--------------|
| User | [docs](http://twitter4j.org/javadoc/twitter4j/User.html) | [docs](https://dev.twitter.com/rest/reference/get/users/lookup) |
| Status | [docs](http://twitter4j.org/javadoc/twitter4j/Status.html) | [docs](https://dev.twitter.com/rest/reference/get/statuses/user_timeline) |

