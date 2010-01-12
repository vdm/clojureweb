(defproject clojure-web "0.0.1"
  :description "Clojure Web Environment"
  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]

                 [org.clojars.liebke/compojure "0.3.1-master"]
		 [org.mortbay.jetty/jetty "6.1.21"]
		 [commons-codec/commons-codec "1.4"]
		 [commons-fileupload/commons-fileupload "1.2.1"]
		 [commons-io/commons-io "1.4"]]
  :dev-dependencies [[leiningen/lein-swank "1.0.0-SNAPSHOT"]])
