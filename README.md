# elv

A Clojure library designed to help with logging and viewing exceptions on the ring server.

Elv does 2 things:
- **logs** all your unhandled exceptions
- **displays** logged exceptions on seperate page

Elv offers a simple, yet configurable, **ring middleware** that will catch any exception thrown during processing of a request by a ring server. All of ring's request parameters will get logged.
Beside that elv offers a neat functionality - a page that will list all logged exceptions:

![image](https://cloud.githubusercontent.com/assets/562298/5712412/76a0b3c0-9ab2-11e4-9088-8510cc8fac4f.png)

As you can see, at the moment simply paging is offered. Searching and ordering of exceptions list is coming next.

'Details' links takes you to the page with details of an exception (stacktrace) and request map.

## Geting started

Elv artifacts are [deployed to clojars] (https://clojars.org/elv) 

With Leiningen:

    [elv "0.1.0"]

With Gradle:
    compile "elv:elv:0.1.0"

With Maven:

    <dependency>
      <groupId>elv</groupId>
      <artifactId>elv</artifactId>
      <version>0.1.0</version>
    </dependency>
    

##Usage

Elv offers **wrap-exception** middleware so the easiest would be to use it like that:

``` clojure
(def app
  (-> your-handler (wrap-exception)))
```

Wrap exception middleware offers following optional parameters:
- :path - uri to the page that displays list of errors. By default it is "/log"
- :storage - where execeptions are stored. By default those are stored in memory (more on seperate section)
- :log-page-handler - handler for the :path - this is where your security check should go. By default elv limit access to local call only

##Choosing storage

By default elv stores caught errors in memory. This is of course something that we do not want in production. At the moment there are 2 additional options for a storage: File storage and mongodb storage. Both available via seperate libraries: [elv.file-storage](https://github.com/itmeze/elv.file-storage) and [elv.mongodb-storage](https://github.com/itmeze/elv.mongodb-storage). Details how to use them you will find on linked github repositories, but those may look like that:

``` clojure
(def app
  (-> your-handler (wrap-exception :storage (->LocalFileSystemStorage "some file system path")))
```

``` clojure
(def app
  (-> your-handler (wrap-exception :storage (->MongoStorage {:uri "mongodb://user:password@ds029911.mongolab.com:29911/elv-test" :coll "elv-test"}))))
```
More to come (especially for relational databases)

##Protecting exception viewer

Another configurable aspect is a way to protect access to error logs. By default elv limits access to page with exception logs to users from a local computer. This is up to you how you decide to secure it. One option would be to protect it via [basic authentication middleware](https://github.com/remvee/ring-basic-authentication):

``` clojure
(require '[ring.middleware.basic-authentication :refer [wrap-basic-authentication]])

(defn authenticated? [name pass]
  (and (= name "foo")
       (= pass "bar")))
       
(def app
  (-> your-handler (wrap-exception :log-page-handler #(wrap-basic-authentication % authenticated?))))
```

##Pre-reading body
Body part of the ring request is of some InputStream type (like org.eclipse.jetty.server.HttpInput if you use jetty). It may be read just once, so that at the moment when exception happens value of the body may differ from the one that was originally send. 
In order to preserve original value, elv provides 'body-pre-read-middleware', that will slurp request's body and save it under :pre-read-body key (keeping :body of the request as it was) .
This is entirely optional:
``` clojure
(def app
  (-> your-handler (wrap-exception) (body-pre-read-middleware)))
```


## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
