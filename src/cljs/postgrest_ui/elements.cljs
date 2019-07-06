(ns postgrest-ui.elements
  "Protocol for constructing elements. This allows overriding
  rendering style to create completely different hiccup from
  the default style.

  See: postgrest-ui.impl.elements for the defaults.")

(defmulti element (fn [style element-name & args] [(or style :default) element-name]))
