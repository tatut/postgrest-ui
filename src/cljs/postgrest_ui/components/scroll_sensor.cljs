(ns postgrest-ui.components.scroll-sensor
  "Scroll sensor component that has no visual element
  but detects when it comes into view."
  (:require [reagent.core :as r]
            [goog.object :as gobj]))

(defn scroll-sensor [on-scroll]
  (let [node (atom nil)
        listener (fn [_]
                   (let [y-min 0
                         y-max (.-innerHeight js/window)
                         y (.-top (.getBoundingClientRect @node))]
                     (when (<= y-min y y-max)
                       (on-scroll))))]
    (r/create-class
     {:component-did-mount
      #(do (reset! node
                   (some-> %
                           (gobj/get "refs" nil)
                           (gobj/get "sensor" nil)))
           ;; Fire immediately if we are in scroll range
           (listener nil)
           (.addEventListener js/window "scroll" listener))

      :component-will-unmount
      #(.removeEventListener js/window "scroll" listener)

      :reagent-render
      (fn [_]
        [:span {:ref "sensor"}])})))
