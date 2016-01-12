(ns ansi-logging.example
  "ANSI terminal logging example"
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp :refer [pprint]]
            [taoensso.timbre :as timbre])
  (:gen-class))

;; helper functions

(defn assoc-if
  "associate k v in m iff v"
  {:added "0.2.0"}
  [m k v]
  (if v
    (assoc m k v)
    m))

(defn remove-fn
  "replaces any (fn? v) in the map m with \"<fn>\", recursively"
  {:added "0.2.0"}
  ([v]
   (cond
     (fn? v) "<fn>"
     (vector? v) (mapv remove-fn v)
     (map? v) (reduce-kv remove-fn {} v)
     :else v))
  ([m k v]
   (assoc m k
     (cond
       (fn? v) "<fn>"
       (vector? v) (mapv remove-fn v)
       (map? v) (reduce-kv remove-fn {} v)
       :else v))))

;; environment variables

(def ^:dynamic *lein-silent* (not (empty? (System/getenv "LEIN_SILENT"))))
(def ^:dynamic *debug*  (not (empty? (System/getenv "DEBUG"))))
(def ^:dynamic *term*  (System/getenv "TERM"))
(def ^:dynamic *ansi-terminal* false) ;; ANSI color enabled?

;; logging setup

(def default-colors {:trace :cyan
                     :debug :purple
                     :info :green
                     :warn :yellow
                     :error :red})

(def default-hide-levels [:info])

(defn ansi-output-fn
  "Logging output function"
  ([data]
   (ansi-output-fn nil data))
  ([{:keys [no-stacktrace?] :as opts} data]
   (let [{:keys [config level ?err_ vargs_ msg_ ?ns-str hostname_ timestamp_]} data
         {:keys [timestamp-opts hide-levels colors]} config
         {:keys [pattern]} timestamp-opts
         color (if colors (get colors level))
         msg (str
               (if-not (empty? pattern)
                    (str (force timestamp_) " "))
               (if (or (empty? hide-levels) (not-any? #(= % level) hide-levels))
                 (string/upper-case (str (name level) " ")))
               (force msg_))
         msg (if (and color *ansi-terminal*)
               (timbre/color-str color msg) msg)]
     (str msg
         (when-not no-stacktrace?
           (when-let [err (force ?err_)]
             (str "\n"
               (if *ansi-terminal*
                 (timbre/stacktrace err)
                 (timbre/stacktrace err {:stacktrace-fonts nil})))))))))

(defn colorize-terminal?
  "Does the current configuration and TERM setting indicate this
  is an ANSI terminal that should be colorized?"
  [&[config]]
  (let [config (or config timbre/*config*)
        not-ansi-terminal (:not-ansi-terminal config)]
    (or (nil? not-ansi-terminal)
      (false? not-ansi-terminal)
      (if (string? not-ansi-terminal)
        (not= *term* not-ansi-terminal)
        (not-any? #(= % *term*) not-ansi-terminal)))))

(defn ansi-println-appender
  "Returns a simple `println` appender for Clojure with
  colorization on ANSI terminals.
  :stream - e/o #{:auto :*out* :*err* :std-err :std-out <io-stream>}."
  [& [{:keys [stream] :or {stream :auto}}]]
  (let [stream (case stream
                :std-err timbre/default-err
                :std-out timbre/default-out
                stream)]
    {:enabled?   true
     :async?     false
     :min-level  nil
     :rate-limit nil
     :output-fn  :inherit
     :fn
     (fn [data]
       (let [{:keys [output-fn config level]} data
             stream (case stream
                      :auto  (if (:error-level? data) *err* *out*)
                      :*out* *out*
                      :*err* *err*
                      stream)
             ansi-terminal? (colorize-terminal? config)
             msg (binding [*ansi-terminal* ansi-terminal?]
                   (output-fn data))]
         (binding [*out* stream] (println msg))))}))

(def default-config
  {:level :info
   :colors default-colors
   :hide-levels default-hide-levels
   ;; :not-ansi-terminal false ;; == assume ANSI terminal
   :output-fn ansi-output-fn
   :appenders {:console true}})

(defn timbre-config
  "Will merge config into defaults to create a timbre compatible config"
  [&[config]]
  (let [config (or config {})
        config (merge default-config config)
        {:keys [level timestamp-opts appenders]} config
        level (if *debug* :trace (if *lein-silent* :warn (or level :info)))
        {:keys [pattern locale timezone]} timestamp-opts
        pattern (or pattern "")
        locale (or locale :jvm-default)
        timezone (or timezone :jvm-default)
        timestamp-opts {:pattern pattern :locale locale :timezone timezone}
        {:keys [console file]} appenders
        console (if (true? console) (ansi-println-appender) console)
        file (if (string? file) (timbre/spit-appender {:fname file}) file)
        appenders (-> {}
                    (assoc-if :console console)
                    (assoc-if :file file))
        config (merge config {:level level
                              :timestamp-opts timestamp-opts
                              :appenders appenders})]
    ;; (println "config:")
    ;; (pprint (remove-fn config))
    ;; (println "---------------------------------")
    config))

(defn log-at-each-level []
  (log/trace "This is for detailed traces")
  (log/debug "This is for debugging")
  (log/info "This is an informational message")
  (log/warn "This is a warning message")
  (log/error "This is an error message"))

(defn -main
  "main function"
  {:added "0.2.0"}
  [& args]
  (println "Logging example ( LEIN_SILENT" *lein-silent*
    "DEBUG" *debug* "TERM" *term* ")")

  (println "\nBasic test:")
  (timbre/set-config! (timbre-config))
  (log-at-each-level)

  (println "\nTerminal detection, timestamp and add file appender:")
  (timbre/set-config! (timbre-config {:not-ansi-terminal "dumb"
                                      :timestamp-opts
                                      {:pattern "yy-MMM-dd HH:mm:ss.SSS"}
                                      :appenders
                                      {:console true
                                       :file "logs/example.log"}}))
  (log-at-each-level)

  (println "\nNow for a stacktrace")
  (try
    (/ 1 0)
    (catch Exception e
      (log/errorf e "Oops, caught an exception")))

  (println "\nLevel :warn, Terminal detection vector, UTC, custom colors, don't hide-levels, and just console appender:")
  (timbre/set-config! (timbre-config {:level :warn
                                      :not-ansi-terminal ["dumb" "xterm-black"]
                                      :hide-levels nil
                                      :timestamp-opts
                                      {:pattern "HH:mm:ss.SSS"
                                       :timezone :utc}
                                      :colors
                                      {:trace :yellow
                                       :debug :red
                                       :info :cyan
                                       :warn :purple}}))
  (log-at-each-level))
