(ns postgrest-ui.components.filters
  "Helpers to create forms that filter listing views."
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [postgrest-ui.elements :refer [element]])
  (:require-macros [postgrest-ui.impl.state :refer [define-stateful-component]]))

(defn simple-search-form
  "Simple searh form containing one text field used to search from given fields."
  [fields-to-search {:keys [state set-state! style] :as opts}]
  ;; State is a filter in the following form:
  ;; {:or {"field1" [:ilike "%text%"] ... "fieldn" [:ilike "%text"]}}
  (let [text (or (some-> state first val first val second
                         (as-> txt
                             (subs txt 1 (dec (count txt)))))
                 "")]
    (element style :text-input
             {:label "Search"
              :value-atom (r/wrap text
                                  (fn [text]
                                     (if (str/blank? text)
                                       (set-state! nil)
                                       (let [flt [:ilike (str "%" text "%")]]
                                         (set-state! {:or (into {}
                                                                (map (fn [field]
                                                                       [field flt])
                                                                     fields-to-search))})))))})))
