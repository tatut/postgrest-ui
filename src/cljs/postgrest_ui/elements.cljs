(ns postgrest-ui.elements
  "Protocol for constructing elements. This allows overriding
  rendering style to create completely different hiccup from
  the default style.

  The style can be overridden for all components by calling `set-default-style!`
  prior to rendering or per component with the `:style` option.

  See: postgrest-ui.impl.elements for the defaults.")

(defonce default-style (atom :default))

(defn set-default-style! [style]
  (reset! default-style style))

(defmulti element (fn [style element-name & args] [(or style @default-style) element-name]))
