(ns postgrest-ui.impl.state)

(defmacro define-stateful-component
  "Define component that either uses a local reagent ratom for state
  or takes it from the :state/:set-state! keys provided in the first
  argument."
  [name args {:keys [state] :or {state 'state}} & body]
  `(defn ~name [& args#]
     (let [state-atom# (if (and (contains? (first args#) :state)
                                (contains? (first args#) :set-state!))
                         nil
                         (reagent.core/atom nil))]
       (fn [& args#]
         (let [~args args# ; destructure arguments
               ~state (or state-atom#
                          (reagent.core/wrap (:state (first args#))
                                             #((:set-state! (first args#)) %)))]
           ~@body)))))
