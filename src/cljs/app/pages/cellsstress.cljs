(ns app.pages.cellsstress
  (:require
   [app.components.Spreadsheet :as Spreadsheet]))

(defn page []
  (fn []
    [:div
     [Spreadsheet/component {:width 26 :height 5000}]]))