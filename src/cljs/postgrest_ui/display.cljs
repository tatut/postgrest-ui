(ns postgrest-ui.display
  "Protocol for customizing the way items are displayed in different contexts.

  When items are being displayed, they are first tried with the context specific
  display. For example when rendering a tabular listing, the :listing context is used.

  If no context specific display is implemented, then the :default context is used.

  If no dispatch value is found, the fallback display will aggregate arrays
  and multivalued items and stringify all others."
  (:require [clojure.string :as str]))

(def ^:const contexts #{:listing :item-view :default})

(defmulti display (fn [context table column value]
                    [context table column]))

(defmulti label (fn [table column] [table column]))

(defmethod display :default [ctx _ column value]
  (when (= ctx :default)
    (if (map? column)
      ;; Map describing a subselect, format all values
      (let [{:keys [table select]} column
            format-fields (fn [key value]
                            ^{:key key}
                            [:div.postgrest-ui-display-multi
                             (doall
                              (for [column select
                                    :let [v (get value column)]]
                                ^{:key column}
                                [:span (display ctx table column v)]))])]
        (if (vector? value)
          ;; Multiple entries, show all
          [:div.postgrest-ui-display-array
           (doall
            (map-indexed
             (fn [i value]
               (format-fields i value))
             value))]
          (format-fields "one" value)))

      ;; Regular value, just stringify
      (str value))))

(defmethod label :default [table column]
  (if (map? column)
    (let [{:keys [table select]} column]
      (str table "(" (str/join "," select) ")"))
    (str column)))

(defn disp
  "Display item in given context or fall back to default context."
  [context table column value]
  (or (display context table column value)
      (display :default table column value)))
