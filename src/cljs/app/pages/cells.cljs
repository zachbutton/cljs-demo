(ns app.pages.cells
  (:require
   [app.components.Spreadsheet :as Spreadsheet]))

(defn page []
  (fn []
    [:div
     [Spreadsheet/component {:width 26 :height 100}]]))
     ;;[Spreadsheet/component {:width 3 :height 3}]]))