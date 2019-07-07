(ns postgrest-ui.impl.style.material
  "Render using Material UI components"
  (:require [reagent.core :as r]
            [goog.object :as gobj]
            [postgrest-ui.elements :refer [element]]))

(defonce MaterialUI (delay
                      (let [mui (gobj/get js/window "MaterialUI" nil)]
                        (when-not mui
                          (.error js/console "No MaterialUI object found in page!"))
                        mui)))
(defonce material-ui-components (atom {}))

(defn- mc [kw]
  (get (swap! material-ui-components
              (fn [cs]
                (if (contains? cs kw)
                  cs
                  (let [mui-class (gobj/get @MaterialUI (name kw))]
                    (if-not mui-class
                      (do (.error js/console "No MaterialUI class found: " (name kw))
                          cs)
                      (assoc cs kw (r/adapt-react-class mui-class)))))))
       kw))

;;; Generic elements

(defmethod element [:material :loading-indicator] [_ _ & _]
  [(mc :CircularProgress)])

;;; Listing table elements

(defmethod element [:material :listing-table-loading] [_ _ & [col-span]]
  [(mc :TableBody)
   [(mc :TableRow)
    [(mc :TableCell) {:colSpan col-span :align "center"}
     [(mc :CircularProgress)]]]])

(defmethod element [:material :listing-table] [_ _ & args]
  (into [(mc :Table) {}]
        args))

(defmethod element [:material :listing-table-head] [_ _ & args]
  (into [(mc :TableHead)] args))

(defmethod element [:material :listing-table-header-row] [_ _ & args]
  (into [(mc :TableRow)] args))

(defmethod element [:material :listing-table-header-cell] [_ _ & [opts label order]]
  [(mc :TableCell)
   {:sortDirection (if order (name order) false)}
   [(mc :TableSortLabel)
    {:active (or (= order :asc) (= order :desc))
     :hideSortIcon true
     :direction (if (= :asc order) "asc" "desc")
     :onClick (:on-click opts)}
    label]])

(defmethod element [:material :listing-table-body] [_ _ & args]
  (into [(mc :TableBody)] args))

(defmethod element [:material :listing-table-row] [_ _ & [i drawer-state on-click & cells]]
  (into [(mc :TableRow) {:onClick on-click}] cells))

(defmethod element [:material :listing-table-cell] [_ _ & content]
  (into [(mc :TableCell)] content))

(defmethod element [:material :listing-table-drawer] [_ _ & [column-count drawer item]]
  [(mc :TableRow)
   [(mc :TableCell)
    {:colSpan column-count}
    [drawer item]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Elements for item-view

(defmethod element [:material :item-view] [_ _ & args]
  [(mc :Paper)
   [(mc :Card)
    (into [(mc :CardContent)] args)]])

(defmethod element [:material :item-view-field] [_ _ & args]
  (into [:<>] args))

(defmethod element [:material :item-view-label] [_ _ & args]
  (into [(mc :Typography) {:variant "h5"}] args))

(defmethod element [:material :item-view-value ] [_ _ & args]
  (into [(mc :Typography) {:variant "body2"}] args))
