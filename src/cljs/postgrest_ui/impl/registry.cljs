(ns postgrest-ui.impl.registry
  "Registry containing loaded swagger definitions."
  (:require [postgrest-ui.impl.fetch :as fetch]
            [reagent.core :as r]
            [clojure.string :as str]))

(defonce registry (r/atom {}))

(defn load-defs
  "Load PostgREST swagger definitions for the given endpoint into the registry.
  Endpoint is the URL for the swagger.json definitions.

  Returns a cursor that can be derefed to get the definitions.
  The value will be nil while the definitions are being loaded."
  [endpoint & path-into-defs]

  (when-not (get @registry endpoint)
    (fetch/load-swagger-json
     endpoint
     #(swap! registry assoc endpoint %)))
  (r/cursor registry (into [endpoint] path-into-defs)))

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
