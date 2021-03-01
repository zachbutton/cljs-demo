(ns app.pages.cells
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [app.events :as events]
   [clojure.string :as string]
   [app.components.Spreadsheet :as Spreadsheet]
   [app.utils :as utils :refer [now]]))

(defn page []
  (fn []
    [:div
     [Spreadsheet/component {:width 26 :height 100}]]))
     ;;[Spreadsheet/component {:width 3 :height 3}]]))