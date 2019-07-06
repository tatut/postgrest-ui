(ns postgrest-ui.components.item-view
  "Simple item-view component for displaying one entity based on primary key."
  (:require [postgrest-ui.impl.fetch :as fetch]
            [postgrest-ui.impl.registry :as registry]
            [postgrest-ui.display :as display])
  (:require-macros [postgrest-ui.impl.state :refer [define-stateful-component]]))

(define-stateful-component item-view [{:keys [endpoint table select loading-indicator]} primary-key]
  {:state state}
  (if-let [defs @(registry/load-defs endpoint)]
    (if-let [{:keys [loaded-item]} @state]
      [:div.postgrest-ui-item-view
       (doall
        (for [column select
              :let [value (get loaded-item (if (map? column)
                                             (:table column)
                                             column))]]
          [:div.postgrest-ui-item-view-item
           [:div.postgrest-ui-item-view-label
            (display/label table column)]
           [:div.postgrest-ui-item-view-value
            [display/disp :item-view table column value]]]))]

      ;; Load item
      (do
        (-> (fetch/get-by-id endpoint defs
                             {:table table
                              :select select} primary-key)
            (.then #(swap! state merge
                           {:loaded-item %})))
        loading-indicator))
    loading-indicator))
