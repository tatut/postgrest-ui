(ns postgrest-ui.components.listing
  "Tabular listing of table/view contents."
  (:require [reagent.core :as r]
            [postgrest-ui.components.scroll-sensor :as scroll-sensor]
            [postgrest-ui.impl.registry :as registry]
            [postgrest-ui.impl.fetch :as fetch]
            [postgrest-ui.display :as display]
            [postgrest-ui.elements :refer [element]]
            [postgrest-ui.impl.elements]
            [clojure.string :as str])
  (:require-macros [postgrest-ui.impl.state :refer [define-stateful-component]]))

(defn- listing-header [{:keys [table select on-click column-widths style]} order-by]
  (element style :listing-table-head
           (element style :listing-table-header-row
                    (doall
                     (map (fn [column width]
                            (let [order (some (fn [[col dir]]
                                                (when (= col column)
                                                  dir))
                                              order-by)]
                              (with-meta
                                (element style :listing-table-header-cell
                                         (merge {:on-click #(on-click column order)}
                                                (when width
                                                  {:style {:width width}}))
                                         (display/label table column)
                                         order)
                                {:key column})))
                          select (or column-widths (repeat nil)))))))

(defn- listing-batch [_ _ _]
  (r/create-class
   {:should-component-update
    (fn [_
         [_ {old-drawer-open :drawer-open} _ old-items]
         [_ {new-drawer-open :drawer-open} _ new-items]]
      (or
       ;; Items have changed (shouldn't happen in normal listing views)
       (not (identical? old-items new-items))

       ;; Drawer state change
       (and
        ;; Some drawer state has changed
        (not= old-drawer-open new-drawer-open)

        ;; And it affects a row in this batch
        (not= (set (keep old-drawer-open old-items))
              (set (keep new-drawer-open new-items))))))
    :reagent-render
    (fn [{:keys [table select style
                              drawer
                              drawer-open
                              toggle-drawer!]}
         start-offset items]
      (element style :listing-table-body
               (doall
                (mapcat
                 (fn [i item]
                   (let [drawer-open? (get drawer-open item)]
                     (into [(with-meta
                              (element style :listing-table-row
                                       (+ start-offset i)
                                       (cond
                                         (nil? drawer) :no-drawer
                                         drawer-open? :drawer-open
                                         :else :drawer-closed)
                                       (when drawer
                                         #(do
                                            (.preventDefault %)
                                            (toggle-drawer! item)))

                                       ;; cells
                                       (for [column select
                                             :let [value (get item (if (map? column)
                                                                     (:table column)
                                                                     column))]]
                                         (with-meta
                                           (element style :listing-table-cell
                                                    [display/disp :listing table column value])
                                           {:key column})))
                              {:key i})]

                           ;; If drawer component is specified and open for this row
                           (when (and drawer (get drawer-open item))
                             [(with-meta
                                (element style :listing-table-drawer (count select) drawer item)
                                {:key (str i "-drawer")})]))))
                 (range) items))))}))

(define-stateful-component listing [{:keys [endpoint table label batch-size loading-indicator
                                            column-widths drawer style]
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

          style (or style :default)
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
       (element style :listing-table
                [listing-header (merge
                                 {:style :default}
                                 (select-keys opts [:table :select :style])
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
                      [listing-batch (merge {:style :default}
                                            (select-keys opts [:table :select :label :drawer :style])
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
                    batches))))

       ;; Check if there are still items not loaded
       (when (and (not loading?)
                  (seq batches)
                  (not all-items-loaded?))
         [scroll-sensor/scroll-sensor
          #(load-batch! (count batches))])])
    loading-indicator))
