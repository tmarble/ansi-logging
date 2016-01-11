(defproject net.info9/ansi-logging "0.2.0"
  :description "ANSI terminal logging example"
  :url "https://github.com/tmarble/ansi-logging"
  :scm {:url "https://github.com/tmarble/ansi-logging.git"}
  :pom-addition [:developers [:developer [:name "Tom Marble"]]]
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/tools.reader "1.0.0-alpha3"]
                 [com.taoensso/timbre "4.2.0"]
                 [com.fzakaria/slf4j-timbre "0.2.2"]
                 [org.clojure/tools.logging "0.3.1"]]
  :main ansi-logging.example
  :aot [ansi-logging.example]
  :profiles {:dev {:resource-paths ^:replace []}}
  :test-paths [] ;; running this program is a test
  :uberjar {:omit-source true})
