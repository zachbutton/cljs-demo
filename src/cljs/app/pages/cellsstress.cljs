(ns app.pages.cellsstress
  (:require
   [app.components.Spreadsheet :as Spreadsheet]
   [reagent.core :as r]))

(def sheet (r/atom {}))

(defn page []
  (fn []
    [:div
     [Spreadsheet/component {:width 26 :height 500000 :sheet sheet}]]))