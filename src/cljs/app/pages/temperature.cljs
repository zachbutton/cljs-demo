(ns app.pages.temperature
  (:require
   [reagent.core :as r]))

(defn- to-celsius [fahrenheit]
  (/ (- fahrenheit 32) 1.8))

(defn- to-fahrenheit [celsius]
  (+ (* celsius 1.8) 32))

(def temperature-celsius (r/atom 0))
(def temperature-fahrenheit (r/atom (to-fahrenheit @temperature-celsius)))

(defn- set-celsius [temp]
  (reset! temperature-celsius temp)
  (reset! temperature-fahrenheit (to-fahrenheit temp)))

(defn- set-fahrenheit [temp]
  (reset! temperature-fahrenheit temp)
  (reset! temperature-celsius (to-celsius temp)))

(defn page []
  [:div
   "Celsius:"
   [:input {:type "number" :value @temperature-celsius :on-input (fn [e]
                                                                   (set-celsius (-> e .-target .-value)))}]

   "Fahrenheit: "
   [:input {:type "number" :value @temperature-fahrenheit :on-input (fn [e]
                                                                      (set-fahrenheit (-> e .-target .-value)))}]])

