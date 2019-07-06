(ns postgrest-ui.specs
  "Define clojure.spec.alpha specs for API"
  (:require [clojure.spec.alpha :as s]))

(s/def :postgrest-ui/endpoint string?)
(s/def :postgrest-ui/table string?)

(s/def :postgrest-ui/selectable (s/or :column string?
                                      :sub-select (s/keys :req-un [:postgrest-ui/table
                                                                   :postgrest-ui/select])))
(s/def :postgrest-ui/select (s/coll-of :postgrest-ui/selectable?))

(s/def :postgrest-ui/order-by (s/coll-of :postgres-ui/order-by-column))
(s/def :postgrest-ui/order-by-column (s/cat :column string?
                                            :direction #{:asc :desc}))

(s/def :postgrest-ui/table-query-def
  (s/keys :req-un [:postgrest-ui/endpoint :postgrest-ui/table]
          :opt-un [:postgrest-ui/select
                   :postgrest-ui/order-by]))
