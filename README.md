# Tweet grabber

## Tangles with Twitter using the v1.1 api and processes Tweets stored locally

Intended to be a replacement for some of the tweet processing Flows originally in NodeRed.

It would be a good idea to move the tweet handling code wholesale to the service as its prefereable to
use akka-streams rather than relying on NodeRed.

~~Also adds functionality such as processing the current tweet collection's tweets
`retweeted_status` and `quoted_status`, promoting them to first class tweets as they will
have more up-to-date likes, retweet and quote counts.~~ 

~~**This should of course only happen if the retweet/quote was posted within the usual timescale**
(`firstAppearance` to `marketStartTime+24h`)~~

~~This means that the logic for checking which markets were active at the time of the tweet will need to
be performed by this service, and will need to be performed on the wrapping tweet's timestamp when 
considering a wrapped tweet's relevance.~~

> Decided against the above as it becomes too complicated to figure out whther a tweet's retweet count 
> is correct as it would have been at market start time. Prefer to just sotre all tweets / retweets 
> / quotes immuatably then they can be playd back as they happened.



## How we decide whether to store a tweet or not

When a tweet is processed, we can query the entries table to find markets where `firstAppearance` is less than the 
`tweetTime`, and also their `marketStartTime+24h` is greater than the `tweetTime`. We will store the tweet if 
any markets in this range contain a `track` and a (horse) `name` referred to in the tweet text. We will also 
tag the entry with the tweet id.

When using the data later on this does not mean we should consider tweets that are timestamped after 
the `marketStartTime`, that would be cheating. The data is kept for completeness only. We should query 
the data removing tweets that were made after the `marketStartTime` and possibly subtracting a buffer.

      Tweet A              Tweet B                        Tweet C              Tweet D
        | Not stored         | Stored                       | Stored             | Not stored
        | (<fa)              | (>fa && <mst+24)             | (>fa && <mst+24)   | (>mst+24)
    ---------+---------+---------+---------+---------+---------+---------+---------+---------+
                |<-firstAppearance          |                        |
                |   Market duration         |                        |
                |         marketStartTime ->|  marketStartTime+24h ->|

                                   |------------------------|
                                   |<- Tweet C-24h      |------------------------|
                                                        |<- Tweet D-24h

> In practice, instead of comparing `Tweet C's time` to ` marketStartTime+24h`, we compare 
> `Tweet C-24h` to `marketStartTime`. This is more convenient because we don't store 
> ` marketStartTime+24h` in the db as a field. Eash huh? 

## A note on retweeted quotes

This can happen if tweet `A` is quoted by tweet `B`, then tweet `C` retweets tweet `B`

Example:

    Original tweet (A): 1226130930445770752
    Tweet quoting original (B): 1226133914693128193
    Quoting tweet retweeted (C): 1226193581905530883

The surrounding tweet `C` (1226193581905530883 in this case) will have the quoting
tweet `B` (1226133914693128193) in its `retweeted_status`
and the original tweet `A` (1226130930445770752) in its `quoted_status`

## Some useful mongo queries

### Checking how many retweets there are
    
    db.getCollection('tweets').aggregate([
        {'$group': {
            '_id':{'$gt':["$retweeted_status", null]},
            'count': {'$sum': 1}
        }}
    ])

### A tweet with 6 retweets

    db.getCollection('tweets').find({'retweeted_status.id_str': '1201124908979359744'})


