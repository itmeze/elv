(defproject elv "0.1.0"
  :description "Exception logger and viewer for clojure's Ring based applications"
  :url "http://github.com/itmeze/elv"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-mock "0.2.0"]
                 [ring-server "0.3.1"]
                 [hiccup "1.0.5"]
                 [cheshire "5.3.1"]])
