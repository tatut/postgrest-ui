(ns postgrest-ui.impl.schema
  "Query the PostgREST swagger.json definitions"
  (:require [clojure.string :as str]))

(defn primary-key? [{desc "description" :as prop}]
  ;; PENDING: this doesn't seem very robust
  (.log js/console (pr-str prop))
  (and desc
       (str/includes? desc "Note:\nThis is a Primary Key.<pk/>")))

(defn primary-key-of [defs table]
  (-> defs
      (get-in ["definitions" table "properties"])
      (as-> props
          (some (fn [[column definition]]
                  (when (primary-key? definition) column))
                props))))

(def ^:const description-fk-pattern #"<fk table='([^']+)' column='([^']+)'/>")

(defn- foreign-key-info [description]
  (let [[_ table column :as match] (re-find description-fk-pattern description)]
    (when match
      {"foreign-key" {"table" table
                      "column" column}})))

(defn column-info [defs table column]
  (let [info (get-in defs ["definitions" table "properties" column])]
    (when info
      (merge
       {"name" column
        "required?" (some #(= column %) (get-in defs ["definitions" table "required"]))}
       info
       (some-> info (get "description") foreign-key-info)))))
