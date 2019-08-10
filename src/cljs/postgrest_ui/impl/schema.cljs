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

(defn column-info [defs table column]
  (merge
   (get-in defs ["definitions" table "properties" column])
   {"required?" (some #(= column %) (get-in defs ["definitions" table "required"]))}))
