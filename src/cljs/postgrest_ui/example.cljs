(ns postgrest-ui.example
  "Example/demo for PostGREST UI components."
  (:require [postgrest-ui.components.listing :as listing]
            [postgrest-ui.components.item-view :as item-view]
            [postgrest-ui.display :as display]
            [postgrest-ui.impl.style.default-style] ; require a rendering style implementation
            [postgrest-ui.impl.style.material]
            [postgrest-ui.elements :as elements]
            [postgrest-ui.components.filters :as filters]
            [reagent.core :as r]
            [clojure.string :as str]))


;; To run, make sure you have postgrest running with the dvdrental example database:
;; http://www.postgresqltutorial.com/postgresql-sample-database/
;;
;; PostgREST endpoint is http://localhost:3000
;;


;; Customize how the actor listing subselect column label is shown
(defmethod display/label ["film" {:table "actor"
                                  :select ["first_name" "last_name"]}]
  [_ _]
  "Actors")

(defmethod display/label ["film" {:table "category"
                                  :select ["name"]}]
  [_ _]
  "Categories")

(defmethod display/label ["film" {:table "language"
                                  :select ["name"]}]
  [_ _]
  "Languages")

(defmethod display/label ["film" "film_id"] [_ _] "Id")

;; Customize how the actor listing value is shown
(defmethod display/display [:listing "film" {:table "actor"
                                             :select ["first_name" "last_name"]}]
  [_ _ _ actors]
  (let [[lead others] (split-at 2 actors)]
    (str (str/join ", "
                   (map (fn [{:strs [first_name last_name]}]
                          (str first_name " " last_name))
                        lead))
         (when (seq others)
           (str " and " (count others) " others")))))

;; Render actor list differently in item-view context
(defmethod display/display [:item-view "film" {:table "actor"
                                               :select ["first_name" "last_name"]}]
  [_ _ _ actors]
  [:ul
   (doall
    (for [{:strs [first_name last_name]} actors]
      ^{:key (str first_name last_name)}
      [:li last_name ", " first_name]))])

(defn filters [opts]
  [:div (pr-str opts)])
(def endpoint "http://localhost:3000")
(defn listing-view []
  [:div
   [:h3 "Movie listing"]
    [listing/filtered-listing
     {:endpoint endpoint
      :table "film"
      :filters-view (r/partial filters/simple-search-form ["title" "description"])
      ;:filter {"description" [:ilike "%scientist%"]}
      :select ["film_id" "title" "description"
               {:table "actor"
                :select ["first_name" "last_name"]}]
      :drawer (fn [item]
                [item-view/item-view
                 {:endpoint endpoint
                  :table "film"
                  :select ["title" "description"
                           {:table "actor"
                            :select ["first_name" "last_name"]}
                           {:table "category"
                            :select ["name"]}
                           {:table "language"
                            :select ["name"]}]}
                 (get item "film_id")])
      :column-widths ["5%" "20%" "45%" "30%"]
      :order-by [["film_id" :asc]]
      :label str
      :batch-size 20}]])

(defn ^:export main []
  (elements/set-default-style! :material)
  (r/render [listing-view]
            (.getElementById js/document "app")))
