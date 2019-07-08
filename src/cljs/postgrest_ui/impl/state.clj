(ns postgrest-ui.impl.state)

(defmacro define-stateful-component
  "Define component that either uses a local reagent ratom for state
  or takes it from the :state/:set-state! keys provided in the first
  argument.

  If :component-will-receive-props is in state, it is treated as code
  to run with new props. The props destructured and the state bound."
  [name args {:keys [state component-will-receive-props] :or {state 'state}} & body]
  (let [state-atom (gensym "state-atom")]
    `(defn ~name [& args#]
       (let [~state-atom (if (and (contains? (first args#) :state)
                                  (contains? (first args#) :set-state!))
                           nil
                           (reagent.core/atom nil))]
         (reagent.core/create-class
          (merge
           ~(when component-will-receive-props
              {:component-will-receive-props
               `(fn [_# [_# & new-args#]]
                  (let [~args new-args#
                        ~state (or ~state-atom
                                   (reagent.core/wrap (:state (first new-args#))
                                                      #((:set-state! (first new-args#)) %)))]
                    ~component-will-receive-props))})
           {:reagent-render
            (fn [& args#]
              (let [~args args#           ; destructure arguments
                    ~state (or ~state-atom
                               (reagent.core/wrap (:state (first args#))
                                                  #((:set-state! (first args#)) %)))]
                ~@body))}))))))
