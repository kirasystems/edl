(defproject edl "0.1.0-SNAPSHOT"
  :description "A demonstration of how to extract a schema from an sql database and
  use it to check and generate sql."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [environ "0.4.0"]]
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "9.2-1003-jdbc4"]]}})
