(ns postgrest-ui.example
  "Example/demo for PostGREST UI components."
  (:require [postgrest-ui.components.listing :as listing]
            [reagent.core :as r]
            [clojure.string :as str]))


;; To run, make sure you have postgrest running with the dvdrental example database:
;; http://www.postgresqltutorial.com/postgresql-sample-database/
;;
;; PostgREST endpoint is http://localhost:3000
;;


;; Customize how the actor listing subselect column label is shown
(defmethod listing/format-column-label ["film" {:table "actor"
                                                :select ["first_name" "last_name"]}]
  [_ _]
  "Actors")

;; Customize how the actor listing value is shown
(defmethod listing/format-value ["film" {:table "actor"
                                         :select ["first_name" "last_name"]}]
  [_ _ actors]
  (let [[lead others] (split-at 2 actors)]
    (str (str/join ", "
                   (map (fn [{:strs [first_name last_name]}]
                          (str first_name " " last_name))
                        lead))
         (when (seq others)
           (str " and " (count others) " others")))))

(defn listing-view []
  [:div
   [:h3 "Movie listing"]
    [listing/listing
     {:endpoint "http://localhost:3000"
      :table "film"
      :select ["film_id" "title" "description"
               {:table "actor"
                :select ["first_name" "last_name"]}]
      :column-widths ["5%" "20%" "45%" "30%"]
      :order-by [["film_id" :asc]]
      :label str
      :batch-size 20}]])

(defn main []
  (r/render [listing-view]
            (.getElementById js/document "app")))

(main)
