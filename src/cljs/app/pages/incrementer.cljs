(ns app.pages.incrementer
  (:require
   [reagent.core :as r]))

(def click-count (r/atom 0))

(defn page []
  [:div
   [:input {:type "text" :disabled true :value @click-count}]
   [:button {:on-click #(swap! click-count inc)} "Increment"]])