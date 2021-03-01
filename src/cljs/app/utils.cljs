(ns app.utils)

(defn now [] (.getTime (js/Date.)))