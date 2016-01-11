# ansi-logging

Demonstration of logging with [timbre](https://github.com/ptaoussanis/timbre/)

## Usage

This is a technical study in using logging on ANSI terminals.

The idea is to use the config map from [timbre](https://github.com/ptaoussanis/timbre/)
with a couple of additions:

* The default logging level is **:info**
* The [leiningen](https://github.com/technomancy/leiningen/) environment
  variables will adjust the desired logging level (if set)
  * `DEBUG` will set level to **:trace**
  * `LEIN_SILENT` will set level to **:warn**
* The **:appenders** map has two special keys:
  * **:console** if this is set to *true* then a println appender is
    added to the config which can colorize the output (`ansi-println-appender`).
    Set to *true* by default.
  * **:file** if this is set to string representing the desired log filename
    then a file appender is added to the config (off by default).
* The **:timestamp-opts** map has three special keys:
  * **:pattern** This is the timestamp pattern to use based on [SimpleDateFormat](http://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html).
     For example, to show milliseconds set the pattern
     to `"yy-MMM-dd HH:mm:ss.SSS"`. If set to *nil* or the empty string then
     a timestamp will *not* be printed. Defaults to *nil*.
 * **:timezone** This is the timezone to be used for timestamps based
     [TimeZone](http://docs.oracle.com/javase/8/docs/api/java/util/TimeZone.html). There are keyword values which can be used as shortcuts (`:jvm-default` is the default):
    * **:utc** To show time in UTC
    * **:jvm-default** To show time using the current locale timezone.
 * **:locale** This is the locale to be used for timestamps based
     [Local](http://docs.oracle.com/javase/8/docs/api/java/util/Locale.html). There special keyword value `:jvm-default` can be used as shortcut to refer to the default locale (`:jvm-default` is the default):
* A new key **:not-ansi-terminal** controls colorization best on the `TERM` environment variable
  * if the value is *nil* or *false* colorization is always on (the default)
  * if the value is a string (e.g. `"dumb"`) then colorization will be turned
    *off* when `TERM=dumb`.
  * if the value is a vector of strings (e.g. `["dumb" "xterm-black"]`) then
    colorization will be turned *off* when `TERM` matches any string in that
    vector.
* A new key **:hide-levels** is vector of levels to exclude from
  outputting the level name in the message. By default the value
  is `[:info]`. If the value is *nil* then all levels will have the
  level name printed.
* A new key **:colors** is used to colorize the various log levels
  based on the [ANSI color scheme](https://en.wikipedia.org/wiki/ANSI_escape_code#Colors)
  and keys defined in [timbre/color-str](https://github.com/ptaoussanis/timbre/blob/master/src/taoensso/timbre.cljx#L549). This mapping is (by default)
  ````
{:trace :cyan
 :debug :purple
 :info :green
 :warn :yellow
 :error :red}
  ````

* The default **:output-fn** is `ansi-output-fn` which will colorize the
  log based on the settings in the config map and the value of the `TERM`
  environment variable. Usually there is no need to replace this default.

### config documented

Here is the annotated config map:

````
{:level :info ;; envars DEBUG ==> :trace, LEIN_SILENT ==> :warn
 :appenders {:console true ;; defaults to true - print to the console
             :file nil ;; defaults to nil - no file output
              ;; may be a filename string "logs/example.log"
              }
 :timestamp-opts {:pattern nil ;; no timestamp by default
                    ;; may also be a string like "yy-MMM-dd HH:mm:ss.SSS"
                  :timezone :jvm-default ;; may also be :utc
                  :locale :jvm-default}
 :not-ansi-terminal false ;; defaults to colorize all the time
   ;; may also be a string "dumb" to match a TERM setting (don't colorize)
   ;; or a vector of strings ["dumb" "xterm-black"] to match multiple TERM values
 :hide-levels [:info] ;; may be nil to show all levels
 :colors {:trace :cyan ;; default colors shown
          :debug :purple
          :info :green
          :warn :yellow
          :error :red}}
````

Use the function `timbre-config` to convert your config map
to a config map usable by **timbre**.

### code snippet #1: the default

````
(timbre/set-config! (timbre-config))
(log/info "This is an informational message")
````

### code snippet #2: quiet output (unless :warn level or higher)

````
(timbre/set-config! (timbre-config {:level :warn}))
(log/info "This is an informational message")
````

### code snippet #3: don't colorize on "dumb" terminals

````
(timbre/set-config! (timbre-config {:not-ansi-terminal "dumb"}))
(log/info "This is an informational message")
````

### code snippet #4: add a timestamp

````
(timbre/set-config! (timbre-config {:timestamp-opts {:pattern "yy-MMM-dd HH:mm:ss.SSS"}}))
(log/info "This is an informational message")
````

### code snippet #5: send logs to the console *and* a file

````
(timbre/set-config! (timbre-config {:appenders
                                    {:console true
                                     :file "logs/example.log"}}))
(log/info "This is an informational message")
````

### code snippet #6: print a colorized stack trace

The trick to getting a nicely formatted stack trace is
to call one of the logging functions ending in "f"
(you can provide additional context with a string
as the second argument):

````
(timbre/set-config! (timbre-config))
(try
  (/ 1 0)
  (catch Exception e
    (log/errorf e "Oops, caught an exception")))
````

## Copyright and license

Copyright © 2015 Tom Marble

Licensed under the [MIT](http://opensource.org/licenses/MIT) [LICENSE](LICENSE)
