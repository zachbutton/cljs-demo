(ns app.pages.cells
  (:require
   [app.components.Spreadsheet :as Spreadsheet]

   [app.routes :as routes]
   [reagent.core :as r]))

(def sheet (r/atom {}))

(defn page []
  (fn []
    [:div
     ;;[Spreadsheet/component {:width 26 :height 100}]]))
     [Spreadsheet/component {:width 26 :height 99 :sheet sheet}]
     [:h3 "Notice:"]
     [:ul
      [:li "Cells are only rendered when scrolled into view (scales better to higher cell count)"]
      [:li "Cell formulas are only calculated when both stale and within the viewport."]
      [:li "When Cell A depends on Cell B, and Cell B is modified, Cell A is marked as stale."]
      [:li "You can see cells being marked as stale, and updating, by looking at the console output."]
      [:li "The following operations are supported: "
       [:pre "SUM, SUB, MUL, DIV, AVG"]]
      [:li       "This challenge calls for dimensions of A-Z, 0-99. Below is a link to a stress test of A-Z, 0-500,000:"]]
     [:p
      [:a {:href (routes/url-for :cells-stress)} "Stress Test"]]]))