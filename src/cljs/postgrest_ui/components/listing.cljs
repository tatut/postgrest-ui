(ns postgrest-ui.components.listing
  "Tabular listing of table/view contents."
  (:require [reagent.core :as r]
            [postgrest-ui.components.scroll-sensor :as scroll-sensor]
            [postgrest-ui.impl.registry :as registry]
            [postgrest-ui.impl.fetch :as fetch]
            [clojure.string :as str])
  (:require-macros [postgrest-ui.impl.state :refer [define-stateful-component]]))

(defmulti format-column-label
  "Format a column label for the given table and column specificaiton. Must return valid hiccup."
  (fn [table column]
    [table column]))

(defmulti format-value
  "Format a value for the given table and column specification. Must return valid hiccup.
  Default implementation stringifies normal values and combines all fields of a subselect into a
  element."
  (fn [table column value]
    [table column]))

(defmethod format-column-label :default [table column]
  (if (map? column)
    (let [{:keys [table select]} column]
      (str table "(" (str/join "," select) ")"))
    (str column)))

(defmethod format-value :default [_ column value]
  (if (map? column)
    ;; Map describing a subselect, format all values
    (let [{:keys [table select]} column
          format-fields (fn [key value]
                          ^{:key key}
                          [:div.postgrest-ui-listing-multi
                           (doall
                            (for [column select
                                  :let [v (get value column)]]
                              ^{:key column}
                              [:span (format-value table column v)]))])]
      (if (vector? value)
        ;; Multiple entries, show all
        [:div.postgrest-ui-listing-array
         (doall
          (map-indexed
           (fn [i value]
             (format-fields i value))
           value))]
        (format-fields "one" value)))

    ;; Regular value, just stringify
    (str value)))

(defn- listing-header [{:keys [table select on-click column-widths]} order-by]
  [:thead
   [:tr
    (doall
     (map (fn [column width]
            (let [order (some (fn [[col dir]]
                                 (when (= col column)
                                   dir))
                              order-by)]
              ^{:key column}
              [:td.postgrest-ui-header-cell (merge {:on-click #(on-click column order)}
                                                   (when width
                                                     {:style {:width width}}))
               (format-column-label table column)
               [:div {:class (case order
                               :asc "postgrest-ui-listing-header-order-asc"
                               :desc "postgrest-ui-listing-header-order-desc"
                               "postgrest-ui-listing-header-cell-order-none")}]]))
          select (or column-widths (repeat nil))))]])

(defn- listing-batch [{:keys [table select
                              drawer
                              drawer-open
                              toggle-drawer!]}
                      start-offset items]
  [:tbody
   (doall
    (mapcat
     (fn [i item]
       (let [drawer-open? (get drawer-open item)]
         (into [^{:key i}
                [:tr {:class (str (if (even? (+ start-offset i))
                                    "postgrest-ui-listing-row-even"
                                    "postgrest-ui-listing-row-odd")
                                  (when drawer
                                    (if drawer-open?
                                      " postgrest-ui-listing-row-drawer-open"
                                      " postgrest-ui-listing-row-drawer-closed")))
                      :on-click (when drawer
                                  #(do
                                     (.preventDefault %)
                                     (toggle-drawer! item)))}
                 (doall
                  (for [column select
                        :let [value (get item (if (map? column)
                                                (:table column)
                                                column))]]
                    ^{:key column}
                    [:td [format-value table column value]]))]]

               ;; If drawer component is specified and open for this row
               (when (and drawer (get drawer-open item))
                 [^{:key (str i "-drawer")}
                  [:tr.postgrest-ui-listing-drawer
                   [:td {:colSpan (count select)}
                    [drawer item]]]]))))
     (range) items))])

(define-stateful-component listing [{:keys [endpoint table label batch-size loading-indicator
                                            column-widths drawer]
                                     :or {batch-size 20
                                          label str
                                          loading-indicator [:div "Loading..."]}
                                     :as opts}]
  {:state state}
  (if-let [defs @(registry/load-defs endpoint)]
    (let [ ;; Get current state
          {:keys [batches all-items-loaded? loading? order-by
                  drawer-open]
           :or {drawer-open #{}}} @state

          order-by (or order-by (:order-by opts)) ; use order-by in state or default from options
          load-batch! (fn [batch-number]
                        (swap! state merge {:loading? true})
                        (-> (fetch/load-range endpoint defs
                                              (merge (select-keys opts [:table :select])
                                                     {:order-by order-by})
                                              (* batch-number batch-size)
                                              batch-size)
                            (.then #(swap! state merge
                                           {:batches (conj (or batches []) %)
                                            :loading? false
                                            :all-items-loaded? (< (count %) batch-size)}))))
          initial-loading? (when (empty? batches)
                             ;; Load the first batch
                             (load-batch! 0))]
      [:<>
       [:table.postgrest-ui-listing {:cellSpacing "0" :cellPadding "0"}
        [listing-header (merge
                         (select-keys opts [:table :select])
                         {:on-click (fn [col current-order-by]
                                      (swap! state merge {:batches nil ; reload everything
                                                          :order-by [[col (if (= :asc current-order-by)
                                                                            :desc
                                                                            :asc)]]}))
                          :column-widths column-widths})
         order-by]
        (if initial-loading?
          ^{:key "initial-loading"}
          [:tbody
           [:tr
            [:td {:colSpan (count (:select opts))}
             loading-indicator]]]
          (doall
           (map-indexed
            (fn [i batch]
              ^{:key i}
              [listing-batch (merge (select-keys opts [:table :select :label :drawer])
                                    (when drawer
                                      {:drawer drawer
                                       :drawer-open drawer-open
                                       :toggle-drawer! #(swap! state update :drawer-open
                                                               (fn [set]
                                                                 (let [set (or set #{})]
                                                                   (if (set %)
                                                                     (disj set %)
                                                                     (conj set %)))))}))
               (* i batch-size) batch])
            batches)))]

       ;; Check if there are still items not loaded
       (when (and (not loading?)
                  (seq batches)
                  (not all-items-loaded?))
         [scroll-sensor/scroll-sensor
          #(load-batch! (count batches))])])
    loading-indicator))
