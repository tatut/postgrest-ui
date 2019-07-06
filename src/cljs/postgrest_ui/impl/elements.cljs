(ns postgrest-ui.impl.elements
  (:require [postgrest-ui.elements :refer [element]]))

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

(defmethod element [:default :listing-table-row] [_ _ & [i drawer-state on-click & cells]]
  [:tr {:class (str (if (even? i)
                      "postgrest-ui-listing-row-even"
                       "postgrest-ui-listing-row-odd")
                     (case drawer-state
                       :no-drawer ""
                       :drawer-open " postgrest-ui-listing-row-drawer-open"
                       :drawer-closed " postgrest-ui-listing-row-drawer-closed"))
        :on-click on-click}
   (doall cells)])

(defmethod element [:default :listing-table-cell] [_ _ & content] `[:td ~@content])

(defmethod element [:default :listing-table-drawer] [_ _ & [column-count drawer item]]
  [:tr.postgrest-ui-listing-drawer
   [:td {:colSpan column-count}
    [drawer item]]])

(defmethod element [:default :listing-table] [_ _ & args]
  `[:table.postgrest-ui-listing {:cellSpacing "0" :cellPadding "0"}
    ~@args])

(defmethod element [:default ] [_ _ & args] `[:foo ~args])
(defmethod element [:default ] [_ _ & args] `[:foo ~args])
(defmethod element [:default ] [_ _ & args] `[:foo ~args])
