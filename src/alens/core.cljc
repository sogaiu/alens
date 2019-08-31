(ns alens.core
  (:require
   #?(:cljs
      [alens.ws :as aw]
      :default
      [alens.tcp :as at])
   #?(:clj
      [clojure.java.shell :as cjs]
      :cljr
      [clojure.clr.shell :as cls])))

#?(:clj
   (defn start-ui
     ([]
      (start-ui 1338))
     ([port]
      (let [jcp (System/getProperty "java.class.path")
            [_ parent-dir] (re-find #":?([^:]+/antoine-bin)[^:]+:?"
                             jcp)
            antoine-path (str parent-dir "/antoine")]
        ;; XXX: keep return value?
        (future (cjs/sh antoine-path
                  "--no-sandbox" ; only for arch linux and derivatives?
                  (str port)))
        ;; XXX: timing issues?
        (Thread/sleep 3000)
        (at/start-punk "127.0.0.1" port))))

   :cljr
   (defn start-ui
     ([]
      (start-ui 1338))
     ([port]
      (let [cljr-load-path
            (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH")
            [_ parent-dir] (re-find #":?([^:]+/antoine-bin)[^:]+:?"
                             cljr-load-path)
            antoine-path (str parent-dir "/antoine")]
        (when parent-dir
          (future (cls/sh antoine-path
                    "--no-sandbox" ; only for arch linux and derivatives?
                    (str port)))
          ;; XXX: timing issues?
          (System.Threading.Thread/Sleep 3000)
          (at/start-punk "127.0.0.1" port)))))

   )

(comment

  ;; for cljs, see alens.ws

  )
