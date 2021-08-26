(ns cjtype.components
  (:require
   [clojure.string :as str]
   [cjtype.cangjie :as cj]
   [oops.core :refer [oget]]
   [reagent.core :as r :refer [atom]]
   [reagent-material-ui.core.box :refer [box]]
   [reagent-material-ui.core.button :refer [button]]
   [reagent-material-ui.core.text-field :refer [text-field]]
   [reagent-material-ui.core.form-control :refer [form-control]]
   [reagent-material-ui.core.form-control-label :refer [form-control-label]]
   [reagent-material-ui.core.form-group :refer [form-group]]
   [reagent-material-ui.core.form-label :refer [form-label]]
   [reagent-material-ui.core.grid :refer [grid]]
   [reagent-material-ui.core.radio :refer [radio]]
   [reagent-material-ui.core.radio-group :refer [radio-group]]
   [reagent-material-ui.core.paper :refer [paper]]
   [reagent-material-ui.core.select :refer [select]]
   [reagent-material-ui.core.switch-component :refer [switch]]
   [reagent-material-ui.core.slider :refer [slider]]
   [reagent-material-ui.core.typography :refer [typography]]
   [reagent-material-ui.core.menu-item :refer [menu-item]]))

(defn update-atom [a]
  #(reset! a (oget % "target.value")))

(defn atom-text-field [a props]
  (fn [a props]
    (let [on-change (:on-change props)]
      [text-field (merge props {:on-change (juxt (update-atom a)
                                                 (or on-change identity))})])))


(defn matcher-text-field
  [value matches & {:keys [on-success on-failure props converter quick-input]
                    :or   {on-success  #()
                           on-failure  #()
                           props       {}
                           converter   (fn [x] x)
                           quick-input []}}]
  (fn [value matches & {:keys [on-success on-failure props converter quick-input]
                        :or   {on-success  #()
                               on-failure  #()
                               props       {}
                               converter   (fn [x] x)
                               quick-input []}}]
   [atom-text-field value
    (merge props
           {:error       (and (not (empty? @value))
                              (not-any? (partial cj/prefix-of? @value) matches))
            :on-change   #(do (when (some #{@value}
                                          quick-input)
                                (on-success)
                                (reset! value ""))
                              (when (str/ends-with? @value
                                                    " ")
                                (if (some (partial cj/matches? @value) matches)
                                  (on-success)
                                  (on-failure))
                                (reset! value "")))
            :input-props {:value (converter @value)}})]))

(defn config-menu [config load-fn]
  (let [text-to-load (atom "")]
    (fn [config load-fn]
      [paper
       [box {:p 3}
        [grid {:container true
               :direction :column
               :spacing 2}
         [grid {:item true}
          [form-label {:id :version-input-label} "Version"
           [radio-group {:id        :version-select
                         :value     (:version @config)
                         :on-change #(swap! config assoc :version
                                            (keyword (oget % "target.value")))}
            [form-control-label {:value   :cj3
                                 :control (r/as-element [radio])
                                 :label   "Cangjie 3"}]
            [form-control-label {:value   :cj5
                                 :control (r/as-element [radio])
                                 :label   "Cangjie 5"}]
            [form-control-label {:value   :both
                                 :control (r/as-element [radio])
                                 :label   "Allow both"}]]]]
         [grid {:item true}
          [form-control-label
           {:control (r/as-element
                      [switch {:checked (:display-cangjie @config)
                               :on-change #(swap! config
                                                  assoc
                                                  :display-cangjie
                                                  (oget % "target.checked"))}])
            :label "Display symbols as Cangjie"}]]
         [grid {:item true}
          [form-label
           "Font size (px)"
           [slider {:value (:font-size @config)
                    :on-change #(swap! config assoc :font-size %2)
                    :default-value 16
                    :step 2
                    :min 16
                    :max 64
                    :value-label-display :auto
                    :marks true}]]]
         [grid {:item true}
          [atom-text-field
           text-to-load
           {:variant :outlined
            :rows 2
            :multiline true
            :label "Enter new text"
            :full-width true}]
          [button {:variant :contained
                   :color :secondary
                   :on-click #(load-fn @text-to-load)}
           "Load text"]]]]])))
