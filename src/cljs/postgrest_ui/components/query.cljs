(ns postgrest-ui.components.query
  "Query component for showing data based on a query."
  (:require [postgrest-ui.impl.fetch :as fetch]
            [postgrest-ui.impl.registry :as registry]
            [postgrest-ui.elements :refer [element]])
  (:require-macros [postgrest-ui.impl.state :refer [define-stateful-component]]))


(define-stateful-component query [{:keys [endpoint token table select order-by where style] :as opts} view-fn]
  {:state state}
  (if-let [defs @(registry/load-defs endpoint)]
    (let [results (:results @state)]
      (when (nil? results)
        (.then (fetch/load-range endpoint token defs
                                 (select-keys opts [:table :select :order-by :where])
                                 nil nil)
               #(swap! state assoc :results %)))
      (if results
        (if (vector? view-fn)
          ;; If view-fn is a hiccup vector, add results as last parameter
          (conj view-fn results)

          ;; Otherwise call view-fn as a component function
          [view-fn results])
        (element style :loading-indicator)))
    (element style :loading-indicator)))
