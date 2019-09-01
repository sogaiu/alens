(ns alens.core
  (:require
   #?(:cljs
      [alens.ws :as aw]
      :default
      [alens.tcp :as at])
   #?(:clj
      [clojure.java.shell :as c_s]
      :cljr
      [clojure.clr.shell :as c_s]))
  #?(:cljr
     (:import
      [System.IO Path])))

#?(:clj
   (defn sleep
     [ms]
     (Thread/sleep ms))

   :cljr
   (defn sleep
     [ms]
     (System.Threading.Thread/Sleep ms))

   )

#?(:cljs
   nil

   :default
   (do

     (defn start-antoine
       [parent-dir is-mac is-win port]
       (cond
         is-mac
         (c_s/sh (str parent-dir "/antoine.app/Contents/MacOS/antoine")
           (str port))
         is-win
         (c_s/sh (str parent-dir "/antoine.exe")
           (str port))
         :else
         (c_s/sh (str parent-dir "/antoine")
           "--no-sandbox" ; XXX: too much for all linuxen?
           (str port))))

     (defn plat-suffix
       [is-mac is-win]
       (cond
         is-win "win"
         ;;
         is-mac "mac"
         ;;
         :else "linux"))

     (defn find-app-dir
       [paths pattern]
       (->> paths
         (filter #(re-matches pattern %))
         first))

     (defn coord-start-up
       [parent-dir is-mac is-win port attempts]
       (future
         (start-antoine parent-dir is-mac is-win port))
       (sleep 3000)
       (loop [i (dec attempts)]
         (if-let [conn (at/start-punk "127.0.0.1" port)]
           conn
           (do
             (sleep 1000)
             (when (> i 0)
               (recur (dec i)))))))

     )
   )

#?(:clj
   (defn start-ui
     ([]
      (start-ui 1338))
     ([port]
      (let [osn (System/getProperty "os.name")
            is-mac (re-matches #"^Mac.*" osn)
            is-win (re-matches #"^Windows.*" osn)
            paths (-> (System/getProperty "java.class.path")
                    (.split (System/getProperty "path.separator"))
                    seq) ; XXX: seq unnecessary?
            antoine-dir
            (find-app-dir paths
              (re-pattern (str ".*antoine-"
                             (plat-suffix is-mac is-win)
                             ".*")))]
        (when antoine-dir
          (coord-start-up antoine-dir is-mac is-win port 3)))))

   :cljr
   (defn start-ui
     ([]
      (start-ui 1338))
     ([port]
      (let [is-unix (#{4 6 128} ; XXX: "case" didn't work
                     (clojure.lang.Util/ConvertToInt
                       (.-Platform Environment/OSVersion)))
            is-mac (when is-unix ; XXX: more fun hacks
                     (re-matches #"^Darwin.*" (:out (c_s/sh "uname" "-s"))))
            is-win (not is-unix)
            paths
            (-> (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH")
              (.Split (.ToCharArray (str System.IO.Path/PathSeparator))))
            antoine-dir
            (find-app-dir paths
              (re-pattern (str ".*antoine-"
                             (plat-suffix is-mac is-win)
                             ".*")))]
        (when antoine-dir
          (coord-start-up antoine-dir is-mac is-win port 3)))))

   )

(comment

  ;; for cljs, see alens.ws

  )
