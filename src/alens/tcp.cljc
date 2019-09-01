#?(:cljs
   (ns alens.tcp) ; XXX: no cljs support atm

   :clj
   (ns alens.tcp
     (:require
      [clojure.edn :as ce]
      [clojure.java.io :as cji]
      [alens.base64 :as cb]
      [alens.log :as cl]
      [alens.print :as cp]
      [frame.core :as fc]
      [punk.core :as pc]
      ;; must come after punk.core
      [alens.patches])
     (:import
      [java.net Socket SocketTimeoutException]))

   :cljr
   (ns alens.tcp
     (:require
      [clojure.clr.io :as cci]
      [clojure.edn :as ce]
      [alens.base64 :as cb]
      [alens.log :as cl]
      [alens.print :as cp]
      [frame.core :as fc]
      [punk.core :as pc]
      ;; must come after punk.core
      [alens.patches])
     (:import
      [System.Net.Sockets TcpClient])))

#?(:cljs
   nil

   :clj
   (do

     (defn connect
       [host port]
       (when-let [client (try
                           (Socket. host port)
                           (catch Exception e nil))]
         (let [writer (cji/writer client)
               reader (cji/reader client)]
           {:client client
            :writer writer
            :reader reader})))

     (defn disconnect
       [conn]
       (when conn
         (when-let [client (:client conn)]
           (when (.isConnected client)
             (.close client)))))

     (defn send-msg
       [{:keys [writer]} message]
       (.append writer (str message "\n"))
       (.flush writer))

     (defn get-line
       [{:keys [reader]}]
       (try
         (.readLine reader)
         (catch SocketTimeoutException _
           nil)))

     )

   :cljr
   (do

     (defn connect
       [host port]
       (when-let [client (try
                           (TcpClient. host port)
                           (catch Exception e nil))]
         (let [writer (cci/text-writer (.-Client client))
               reader (cci/text-reader (.-Client client))]
           {:client client
            :writer writer
            :reader reader})))

     (defn disconnect
       [conn]
       (when conn
         (when-let [client (:client conn)]
           (when (.-Connected client)
             (.Close client)))))

     (defn send-msg
       [{:keys [writer]} message]
       (.Write writer (str message "\n"))
       (.Flush writer))

     (defn get-line
       [{:keys [reader]}]
       (try
         (.ReadLine reader)
         (catch System.IO.IOException _
           nil)))

     )
   )

#?(:cljs
   nil

   :default
   (do
     (defn start-loop
       [conn]
       (loop [a-line (get-line conn)]
         (when a-line
           ;; XXX
           (cl/log-if-debug (str "received: " a-line))
           (let [partly (cb/decode a-line)
                 ;; XXX
                 _ (cl/log-if-debug (str "partly: " partly))
                 read-value (ce/read-string
                             {
                              #_#_:readers {}
                              :default tagged-literal}
                             partly)]
             ;; XXX
             (cl/log-if-debug (str "read-value: " read-value))
             (cl/log-if-debug (str "vector?: " (vector? read-value)))
             (pc/dispatch read-value)
             (recur (get-line conn))))))

     (defonce traffic-loop
       (atom nil))

     (defn m-encode
       [value]
       (cb/encode
        (binding [alens.print/*make-readable* true]
          (pr-str value))))

     (defn setup-emit-handler
       [conn]
       (fc/reg-fx
         pc/frame :emit
         (fn emit [v]
           (cl/log-if-debug (str "v: " v))
           (let [encoded (m-encode v)]
             ;; XXX
             (cl/log-if-debug (str "encoded: " encoded))
             (send-msg conn encoded)))))

     (defn start-punk
       [host port]
       (when-let [conn (connect host port)]
         ;; preparing "frame" to handle the :emit effect
         (setup-emit-handler conn)
         ;; prepare tap> to trigger dispatching
         (pc/add-taps!)
         ;; handle info from the electron app that comes via tcp
         (reset! traffic-loop
           (future (start-loop conn)))
         ;; capture this return value to work with send-msg or get-line
         conn))

     (defn stop-punk
       [conn]
       ;; XXX: no deregister fn?
       (fc/reg-fx
         pc/frame :emit
         (fn emit [v]
           nil))
       ;; XXX: could take a while?
       (disconnect conn)
       ;;
       (pc/remove-taps!)
       ;;
       (when @traffic-loop
         (future-cancel @traffic-loop))
       (reset! traffic-loop nil)
       ;; reset db
       (pc/dispatch [:clear])
       ;;
       true)

     )
   )

(comment

  (require '[alens.tcp :as ct])
  (in-ns 'alens.tcp)

  ;; convenience function for set up (see source for details)
  (def conn
    (start-punk "127.0.0.1" 1338))

  ;; try sending a value to the electron app for viewing in punk.ui
  (tap> {:a 1 :b 2})

  ;; now take a look at the electron app and start clicking a bit :)

  ;; other things to try:
  (tap> #{:ant :bee :fox :elephant})
  (tap> [2 3 5 7 9])
  (tap> {:bag #{:pencil :notepad :water-bottle}
         :position :standing
         :mind [:tune :chatter]})
  (tap> (atom {:position :sitting}))
  (tap> (Exception. "i am an exception"))
  (tap> *ns*)
  (tap> #'start-punk)

  ;; a lower-level way to send info
  (require '[alens.base64 :as cb])
  (send-msg conn (cb/encode "[:entry 0 {:value {:a 1 :b 2} :meta nil}]"))

  ;; following possibly of interest...

  ;; try to stop handling tcp info from electron by stopping thread
  (future-cancel @traffic-loop)

  )
