(ns postgrest-ui.impl.fetch
  "Fetch API helpers"
  (:require [clojure.string :as str]
            [postgrest-ui.impl.schema :as schema]))

(defonce fetch-impl (atom js/fetch))


(defn- headers [& header-maps]
  (clj->js
   (apply merge
          {"X-Requested-With" "postgrest-ui"}
          header-maps)))

(defn load-swagger-json [endpoint callback]
  (-> (@fetch-impl endpoint #js {:headers (headers)})
      (.then #(.json %))
      (.then #(js->clj %))
      (.then callback)))

(defn- format-select [select]
  (str/join ","
            (map #(if (map? %)
                    (let [{:keys [table select]} %]
                      (str table "(" (format-select select) ")"))
                    %)
                 select)))

(defn- table-endpoint-url [endpoint defs table]
  (let [path (get-in defs "paths"
                     (str "/" table))]
    (str endpoint path)))

(defn- json->clj [promise]
  (-> promise
      (.then #(.json %))
      (.then #(js->clj %))))

(defn- format-order-by [[order dir]]
  (str order (case dir
               :asc ".asc"
               :desc ".desc")))

(def combined-filter #{:and :or})

(declare format-operator)

;; http://postgrest.org/en/v5.2/api.html#horizontal-filtering-rows
(def filter-operators
  {;; Equality and comparison
   := #(str "eq." %)
   :> #(str "gt." %)
   :>= #(str "gte." %)
   :< #(str "lt." %)
   :<= #(str "lte." %)
   :not= #(str "neq." %)

   ;; Text search with LIKE
   :like #(str "like." (str/replace % "%" "*")) ; use * instead of %
   :ilike #(str "ilike." (str/replace % "%" "*"))

   :in #(str "in.(" (str/join "," (map prn %)) ")")

   ;; Exact equality
   :null? (constantly "is.null")
   :not-null? (constantly "not.is.null")
   :true? (constantly "is.true")
   :false? (constantly "is.false")

   ;; Full text search
   :fts #(str "fts." %)
   :plfts #(str "plfts." %)
   :phfts #(str "phfts." %)

   ;; Array element checks
   :contains? #(str "cs.{" (str/join "," (map prn %)) "}")
   :contained-in? #(str "cd.{" (str/join "," (map prn %)) "}")

   ;; Range checks
   :overlaps? #(str "ov.[" %1 "," %2 "]")
   :left-of? #(str "sl.(" %1 "," %2 ")")
   :right-of? #(str "sr.(" %1 "," %2 ")")
   :not-extends-left-of? #(str "nxl.(" %1 "," %2 ")")
   :not-extends-right-of? #(str "nxr.(" %1 "," %2 ")")
   :adjacent? #(str "adj.(" %1 "," %2 ")")

   :not #(str "not." (apply format-operator %&))})

(defn- authorization-header [token]
  (if token
    {"Authorization" (str "Bearer " token)}
    {}))

(defn- format-where
  ([filters]
   (format-where filters "=" "&"))
  ([filters value-separator filter-separator]
   (str/join filter-separator
             (map (fn [[column value]]
                    (if (combined-filter column)
                      ;; Format multiple clauses as AND/OR
                      (str (name column) "=(" (format-where value "." ",") ")")

                      ;; Single column filter
                      (let [[op-kw & op-args] value
                            op-fn (filter-operators op-kw)]
                        (when-not op-fn
                          (throw (ex-info "Unknown filter operator"
                                          {:operator value})))
                        (str column value-separator
                             (apply op-fn op-args)))))
                  filters))))

(defn load-range
  "Load a range of items. Returns promise."
  [endpoint token defs {:keys [table select order-by where on-fetch-response]} offset limit]
  (let [url (str (table-endpoint-url endpoint defs table) "?"
                 (str/join
                  "&"
                  (remove nil?
                          [(when select
                             ;; process resource embed
                             (str "select=" (format-select select)))
                           (when where
                             (format-where where))
                           (when (seq order-by)
                             (str "order="
                                  (str/join "," (map format-order-by order-by))))])))]
    (-> (@fetch-impl url
         #js {:method "GET"
              :headers (headers
                        {"Prefer" "count=exact"}
                        (when offset
                          {"Range" (str offset "-" (dec (+ offset limit)))
                           "Range-Unit" "items"})
                        (authorization-header token))})
        (.then (fn [response]
                 (when on-fetch-response
                   (on-fetch-response response))
                 response))
        json->clj)))

(defn get-by-id [endpoint token defs {:keys [table select]} id]
  (let [pk (schema/primary-key-of defs table)
        _ (assert pk (str "Couldn't find primary key column for table: " table))
        url (str (table-endpoint-url endpoint defs table)
                 "?select= " (format-select select)
                 "&" pk "=eq." id)]
    (-> (@fetch-impl url
         #js {:method "GET"
              :headers (headers (authorization-header token))})
        json->clj
        (.then first))))
