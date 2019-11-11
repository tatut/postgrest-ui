(ns postgrest-ui.impl.style.default-style
  "Implements element rendering for the default style"
  (:require [postgrest-ui.elements :refer [element]]
            [clojure.string :as str]
            [reagent.core :as r]))

;; Generic elements
(defmethod element [:default :loading-indicator] [_ _ & _]
  [:div.loading "Loading..."])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Elements for listing-view

(defmethod element [:default :listing-table-loading] [_ _ & [col-span]]
  [:tbody
   [:tr
    [:td {:colSpan col-span}
     "Loading..."]]])

(defmethod element [:default :listing-table-head] [_ _ & args]
  `[:thead ~@args])

(defmethod element [:default :listing-table-header-row] [_ _ & args]
  `[:tr ~args])

(defmethod element [:default :listing-table-header-cell] [_ _ & [opts label order]]
  `[:td.postgrest-ui-header-cell
    ~opts
    ~label
    [:div {:class ~(case order
                     :asc "postgrest-ui-listing-header-order-asc"
                     :desc "postgrest-ui-listing-header-order-desc"
                     "postgrest-ui-listing-header-cell-order-none")}]])

(defmethod element [:default :listing-table-body] [_ _ & args] `[:tbody ~args])

(defmethod element [:default :listing-table-row] [_ _ & [i drawer-state on-click row-class & cells]]
  [:tr (merge
         (when on-click
           {:tab-index 0
            :on-key-down (r/partial (fn [e]
                                      (when (= (.-key e) "Enter")
                                        (on-click))))})
         {:class (str (if (even? i)
                        "postgrest-ui-listing-row-even"
                        "postgrest-ui-listing-row-odd")
                      (case drawer-state
                        :no-drawer ""
                        :drawer-open " postgrest-ui-listing-row-drawer-open"
                        :drawer-closed " postgrest-ui-listing-row-drawer-closed")
                      (when row-class
                        (str " " row-class)))
          :on-click on-click})
   (doall cells)])

(defmethod element [:default :listing-table-cell] [_ _ & content] `[:td ~@content])

(defmethod element [:default :listing-table-drawer] [_ _ & [column-count drawer item]]
  [:tr.postgrest-ui-listing-drawer
   [:td {:colSpan column-count}
    [drawer item]]])

(defmethod element [:default :listing-table] [_ _ & args]
  `[:table.postgrest-ui-listing {:cellSpacing "0" :cellPadding "0"}
    ~@args])


;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Elements for item-view

(defmethod element [:default :item-view] [_ _ & args]
  `[:div.postgrest-ui-item-view ~@args])

(defmethod element [:default :item-view-field] [_ _ & args]
  `[:div.postgrest-ui-item-view-item ~@args])
(defmethod element [:default :item-view-label] [_ _ & args]
  `[:div.postgrest-ui-item-view-label ~@args])
(defmethod element [:default :item-view-value ] [_ _ & args]
  `[:div.postgrest-ui-item-view-value ~@args])

;(defmethod element [:default ] [_ _ & args] `[:foo ~args])
;(defmethod element [:default ] [_ _ & args] `[:foo ~args])


;; Form layout

(defmethod element [:default :form-group] [_ _ & [label fields]]
  [:div.postgrest-ui-form-group
   [:h3.postgrest-ui-form-group-label label]
   (into [:<>]
         (map-indexed
          (fn [i field]
            (with-meta field {:key i})) fields))])

;; Elements for input fields

(defmethod element [:default :text-input] [_ _ & [{:keys [label name type format value value-atom]}]]
  (let [[read write field-type]
        (case type
          "integer" [js/parseInt str "number"]
          [identity identity "text"])]
    [:div.postgrest-ui-form-field
     [:label {:for name} label]
     [:input {:type field-type
              :name name
              :value (or (some-> @value-atom read) "")
              :on-change #(reset! value-atom
                                  (let [v (-> % .-target .-value)]
                                    (when-not (str/blank? v)
                                      (read v))))}]]))

(defmethod element [:default :select-input] [_ _ & [{:keys [label name options value-atom
                                                             option-label option-value]
                                                      :or {option-label identity
                                                           option-value identity}}]]
  (let [option-by-value (zipmap (map (comp str option-value) options)
                                options)]
    [:div.postgrest-ui-form-field
     [:label {:for name} label]
     [:select {:value (or @value-atom "")
               :name name
               :on-change #(reset! value-atom (some-> % .-target .-value option-by-value option-value))}
      ;; Empty selection
      [:option {:value ""} ""]

      (for [option options]
        ^{:key option}
        [:option {:value (option-value option)}
         (option-label option)])]]))
