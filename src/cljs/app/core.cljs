(ns app.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [app.routes :as routes]

   [app.pages.home :as home]
   [app.pages.incrementer :as incrementer]
   [app.pages.temperature :as temperature]
   [app.pages.flights :as flights]
   [app.pages.timer :as timer]
   [app.pages.crud :as crud]
   [app.pages.circles :as circles]
   [app.pages.cells :as cells]
   [app.pages.cellsstress :as cells-stress]))

(rf/reg-sub
 ::active-page
 (fn [db _]
   (:active-page db)))

(defn pages [page-name]
  (case page-name
    :home-page [home/page]
    :incrementer-page [incrementer/page]
    :temperature-page [temperature/page]
    :flights-page [flights/page]
    :timer-page [timer/page]
    :crud-page [crud/page]
    :circles-page [circles/page]
    :cells-page [cells/page]
    :cells-stress-page [cells-stress/page]
    nil))

(defn layout []
  (let [active-page (rf/subscribe [::active-page])]
    [:div.main_container
     [:header
      [:div.links
       [:a {:class (if (= (str @active-page) ":home-page") "active" "") :href (routes/url-for :home)} "Home"]
       [:a {:class (if (= (str @active-page) ":incrementer-page") "active" "") :href (routes/url-for :incrementer)} "Incrementer"]
       [:a {:class (if (= (str @active-page) ":temperature-page") "active" "") :href (routes/url-for :temperature)} "Temperature"]
       [:a {:class (if (= (str @active-page) ":flights-page") "active" "") :href (routes/url-for :flights)} "Flights"]
       [:a {:class (if (= (str @active-page) ":timer-page") "active" "") :href (routes/url-for :timer)} "Timer"]
       [:a {:class (if (= (str @active-page) ":crud-page") "active" "") :href (routes/url-for :crud)} "CRUD"]
       [:a {:class (if (= (str @active-page) ":circles-page") "active" "") :href (routes/url-for :circles)} "Circles"]
       [:a {:class (if (= (str @active-page) ":cells-page") "active" "") :href (routes/url-for :cells)} "Cells"]]]

     [:div.main_content
      [:div {:id (subs (str @active-page) 1)}
       [pages @active-page]]]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [layout] (.getElementById js/document "app")))

;;(defn ^:export main []
;;  (mount-root))

(defn ^:export main []
  (.log js/console "running init")
  (routes/app-routes)
  (mount-root))
