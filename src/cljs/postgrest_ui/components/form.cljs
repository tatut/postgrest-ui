(ns postgrest-ui.components.form
  (:require [postgrest-ui.impl.registry :as registry]
            [postgrest-ui.elements :refer [element]]
            [postgrest-ui.impl.schema :as schema]
            [postgrest-ui.display :as display]
            [reagent.core :as r]
            [postgrest-ui.impl.fetch :as fetch])
  (:require-macros [postgrest-ui.impl.state :refer [define-stateful-component]]))


(defn- text-input [style label {:strs [name type format]} value-atom]
  (element style :text-input {:label label
                              :name name
                              :type type
                              :format format
                              :value-atom value-atom}))

(defn- enum-input [style label {:strs [name enum]} value-atom]
  (element style :select-input {:label label
                                :name name
                                :options enum
                                :value-atom value-atom}))


(defn- foreign-key-link [{:keys [style endpoint token defs column info options-atom value-atom]}]
  ;; Load options, if not present
  (when (nil? @options-atom)
    (.then (fetch/load-range endpoint token defs
                             {:table (get-in info ["foreign-key" "table"])
                              :select [(get-in info ["foreign-key" "column"])
                                       (:option-label column)]}
                             nil nil)
           (fn [options]
             (reset! options-atom options))))
  (fn [{:keys [style label column info options-atom value-atom]}]
    (element style :select-input {:label label
                                  :name (:column column)
                                  :options @options-atom
                                  :option-label #(get % (:option-label column))
                                  :option-value #(get % (get-in info ["foreign-key" "column"]))
                                  :value-atom value-atom})))

(defn- form-group [{:keys [style endpoint token table]} defs state {:keys [label columns]}]
  (element style :form-group
           label
           (for [column columns
                 :let [column-name (if (map? column)
                                     (:column column)
                                     column)
                       info (when column-name
                              (schema/column-info defs table column-name))
                       label (display/label table column)
                       value-atom (r/wrap (get-in @state [:data column-name])
                                          #(swap! state assoc-in [:data column-name] %))]]

             (cond
               (contains? info "foreign-key")
               [foreign-key-link {:style style
                                  :endpoint endpoint
                                  :token token
                                  :defs defs
                                  :column column
                                  :info info
                                  :options-atom (r/wrap (get-in @state [:options column-name])
                                                        #(swap! state assoc-in [:options column-name] %))
                                  :value-atom value-atom}]

               ;; Enum selection
               (contains? info "enum")
               (enum-input style label info value-atom)

               :else
               (text-input style label info value-atom))))
  )
(define-stateful-component form [{:keys [endpoint token table layout style
                                         header-fn footer-fn] :as opts}]
  {:state state}
  (let [defs @(registry/load-defs endpoint)
        current-state @state]
    (if-not defs
      (element style :loading-indicator)
      [:<>
       [:pre (pr-str defs)]
       (when header-fn
         (header-fn current-state))
       (doall
        (map-indexed (fn [i group]
                       ^{:key i}
                       [form-group (select-keys opts [:endpoint :token :table :style]) defs state group])
                     layout))
       (when footer-fn
         (footer-fn current-state))])))
