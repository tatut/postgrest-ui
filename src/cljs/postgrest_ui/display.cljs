(ns postgrest-ui.display
  "Protocol for customizing the way items are displayed in different contexts.

  When items are being displayed, they are first tried with the context specific
  display. For example when rendering a tabular listing, the :listing context is used.

  If no context specific display is implemented, then the :default context is used.

  If no dispatch value is found, the fallback display will aggregate arrays
  and multivalued items and stringify all others.

  Default display uses the type of column to format the value and falls back
  to just converting it to string."
  (:require [clojure.string :as str]
            [goog.date :as goog-date]
            [goog.i18n.DateTimeFormat]
            [postgrest-ui.impl.schema :as schema]))

(def ^:const contexts #{:listing :item-view :default})

(defmulti display (fn [context table column value defs]
                    [context table column]))

(defmulti label (fn [table column] [table column]))

(defmulti format-value (fn [ctx type format value]
                         [ctx type format]))


(defmethod format-value :default [_ _ _ value] (str value))

(defmethod format-value [:default "string" "daterange"] [_ _ _ value]
  ;; dates are always represented as having non-exclusive end
  (let [[start end] (->> value
                         (re-find #"\[([^,]+),([^\)]+)\)")
                         (drop 1)
                         (map #(goog-date/fromIsoString %)))
        df (goog.i18n.DateTimeFormat. "d.M.yyyy")]

    (str (.format df start) " — " (.format df (doto end
                                                ;; Decrement one day
                                                (.add (goog-date/Interval. 0 0 -1)))))))

(defmethod format-value [:default "string" "numrange"] [_ _ _ value]
  (let [[n1 n2] (str/split (subs value 1 (dec (count value))) #",")]
    (str n1 " — " n2)))

(defmethod display :default [ctx table column value defs]
  (when (= ctx :default)
    (if (map? column)
      ;; Map describing a subselect, format all values
      (let [{:keys [table select]} column
            format-fields (fn [key value]
                            ^{:key key} ; FIXME: render via element multimethod, not hiccup
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

      ;; Regular value, call format-value with column info
      (let [{:strs [type format]} (schema/column-info defs table column)]
        (format-value ctx type format value)))))

(defmethod label :default [table column]
  (if (map? column)
    (let [{:keys [table select]} column]
      (str table "(" (str/join "," select) ")"))
    (str column)))

(defn disp
  "Display item in given context or fall back to default context."
  [context table column value defs]
  (or (display context table column value defs)
      (display :default table column value defs)))
