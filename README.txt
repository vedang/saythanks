;; -*- mode: org; -*-

* saythanks

A Clojure library designed to thank people who wish you on Facebook on
your birthday. You can see an image of it in action here: http://imgur.com/w2xZl

*UPDATE*: The last time I ran this code successfully was in 2014.
Facebook as since changed the way that they return birthday posts in
the Graph API. We no longer have access to every individual post, we
can only access the entire group of posts via the API (Eg: "Vedang and
30 other friends have wished you on your Birthday"). Therefore it is
no longer possible to reply to each individual post.

Keeping the repo around for nostalgia :)

** Pre-requisites

You need to install Redis in order to use this code. We need Redis to
store the point to which we've replied to posts. This allows us keep
continuously polling Facebook and reply to everyone who wishes us.

** Usage

1. Create the file ~src/saythanks/access.clj~

Add the following code to it:

#+BEGIN_SRC clojure
  (ns saythanks.access)
  (def access-token "<your access token>")
#+END_SRC

You can get your access-token from https://developers.facebook.com/tools/explorer. You need the ~user_posts~ (under User Data Permissions) and ~publish_actions~ (under Extended Permissions) permissions.

2. Change the default thank-you messages in ~src/saythanks/core.clj~

(If you wish, I think the defaults are quite good.)
A thank-you message will be picked at random and used to reply to the
facebook post.

3. Go to the terminal and run:

#+BEGIN_SRC shell
  $ lein uberjar
  $ java -jar ./target/saythanks-0.2.0-standalone.jar
#+END_SRC

Enjoy :)

* Author - Vedang Manerikar

* Contributors
 - Kiran Kulkarni ([[https://twitter.com/kiran_kulkarni][@kiran_kulkarni]]): the awesome regex which is at the heart of the code.

* License

Copyright © 2012, 2013, 2014, 2015 Vedang Manerikar ([[https://twitter.com/vedang][@vedang]])

Distributed under the Eclipse Public License, the same as Clojure.
