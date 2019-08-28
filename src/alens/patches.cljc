(ns alens.patches
  (:require
   [clojure.datafy :as cd]
   [frame.core :as fc]
   [punk.core :as pc]))

;; override default :nav
(fc/reg-event-fx
 pc/frame :nav
 []
 (fn [{:keys [db]} [_ idx k v]]
   (let [x (get-in db [:entries idx])
         dx (cd/datafy x)
         ;; nav to next item in datafied object
         x' (cd/nav dx k (get dx k))
         ;; store this nav'd value in db for reference later
         db' (update db :entries conj x')
         idx' (count (:entries db))
         dx' (cd/datafy x')]
     {:db db'
      :emit [:nav idx {:value dx'
                       :meta (meta dx')
                       :idx idx'}]})))
