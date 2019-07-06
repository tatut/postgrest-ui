(ns postgrest-ui.impl.fetch
  "Fetch API helpers"
  (:require [clojure.string :as str]))

(defonce fetch-impl (atom js/fetch))

(defn load-swagger-json [endpoint callback]
  (-> (@fetch-impl endpoint)
      (.then #(.json %))
      (.then #(let [clj (js->clj %)]
                (.log js/console "CLJ: " clj)
                clj))
      (.then callback)))

(defn- format-select [select]
  (str/join ","
            (map #(if (map? %)
                    (let [{:keys [table select]} %]
                      (str table "(" (format-select select) ")"))
                    %)
                 select)))
(defn load-range
  "Load a range of items. Returns promise."
  [endpoint defs {:keys [table select order-by filter]} offset limit]
  (let [path (get-in defs "paths"
                     (str "/" table))
        url (str endpoint path "?"
                 (str/join
                  "&"
                  (remove nil?
                          [(when select
                             ;; process resource embed
                             (str "select=" (format-select select)))
                           (when (seq order-by)
                             (str "order="
                                  (str/join "," (map (fn [[order dir]]
                                                       (str order (case dir
                                                                    :asc ".asc"
                                                                    :desc ".desc")))
                                                     order-by))))])))]
    (-> (@fetch-impl url
         #js {:method "GET"
              :headers (doto (js/Headers.)
                         (.append "Range" (str offset "-" (dec (+ offset limit))))
                         (.append "Range-Unit" "items"))})
        (.then #(.json %))
        (.then #(js->clj %)))))
