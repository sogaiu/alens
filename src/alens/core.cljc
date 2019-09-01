(ns alens.core
  (:require
   #?(:cljs
      [alens.ws :as aw]
      :default
      [alens.tcp :as at])
   #?(:clj
      [clojure.java.shell :as cjs]
      :cljr
      [clojure.clr.shell :as ccs]))
  #?(:cljr
     (:import [System.IO Path])))

#?(:clj
   (defn start-ui
     ([]
      (start-ui 1338))
     ([port]
      (let [jcp (-> (System/getProperty "java.class.path")
                  (.split (System/getProperty "path.separator"))
                  seq) ; XXX: seq unnecessary?
            parent-dir (->> jcp
                         (filter #(re-matches #".*antoine-bin.*" %))
                         first)
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
      (let [clp-seq
            (-> (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH")
              (.Split (.ToCharArray (str System.IO.Path/PathSeparator))))
            parent-dir (->> clp-seq
                         (filter #(re-matches #".*antoine-bin.*" %))
                         first)
            antoine-path (str parent-dir "/antoine")]
        (when parent-dir
          (future (ccs/sh antoine-path
                    "--no-sandbox" ; only for arch linux and derivatives?
                    (str port)))
          ;; XXX: timing issues?
          (System.Threading.Thread/Sleep 3000)
          (at/start-punk "127.0.0.1" port)))))

   )

(comment

  ;; for cljs, see alens.ws

  )
