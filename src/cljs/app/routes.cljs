(ns app.routes
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]
            [app.events :as events]))


(def routes ["/" {"" :home
                  "incrementer" :incrementer
                  "temperature" :temperature
                  "flights" :flights
                  "timer" :timer
                  "crud" :crud
                  "circles" :circles
                  "cells" :cells}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn- dispatch-route [matched-route]
  (let [page-name (keyword (str (name (:handler matched-route)) "-page"))]
    (re-frame/dispatch [::events/set-active-page page-name])))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))