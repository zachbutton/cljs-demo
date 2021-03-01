(ns app.pages.crud
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [app.events :as events]
   [clojure.string :as string]
   [app.utils :as utils :refer [now]]))

(rf/reg-sub
 ::crud-items
 (fn [db _]
   (if (nil? (:crud-items db)) {} (:crud-items db))))

(rf/reg-sub
 ::crud-list
 (fn [db _]
   (if (nil? (:crud-order db)) []
       (map (fn [id]
              (assoc (get (:crud-items db) id) :id id)) (:crud-order db)))))


(defn is-selection-valid? [crud-items selected-id]
  (and (not (or (nil? selected-id) (string/blank? selected-id)))
       (not (nil? (get crud-items selected-id)))))

(defn filter-items [surname-filter]
  (rf/dispatch [::events/update-crud-filter surname-filter])
  (rf/dispatch [::events/refresh-crud-order]))

(defn create-item [name surname]
  (rf/dispatch [::events/create-crud-item {:name name :surname surname}])
  (rf/dispatch [::events/refresh-crud-order]))

(defn delete-item [id]
  (rf/dispatch [::events/delete-crud-item id])
  (rf/dispatch [::events/refresh-crud-order]))

(defn update-item [id item]
  (rf/dispatch [::events/update-crud-item id item])
  (rf/dispatch [::events/refresh-crud-order]))

(defn page []
  (let [crud-items (rf/subscribe [::crud-items])
        crud-list (rf/subscribe [::crud-list])
        inputs (r/atom {:name ""
                        :surname ""
                        :selected nil})]
    (fn []
      [:div
       [:div.filter "Filter prefix: " [:input {:type "text" :on-input (fn [ev] (filter-items (-> ev .-target .-value)))}]]
       [:div.users
        [:select.user-selection {:multiple true
                                 :on-change (fn [ev]
                                              (swap! inputs assoc :selected (-> ev .-target .-value))
                                              (swap! inputs assoc :name (:name (get @crud-items (:selected @inputs))))
                                              (swap! inputs assoc :surname (:surname (get @crud-items (:selected @inputs)))))}
         (for [{:keys [id surname name]} @crud-list]
           [:option {:value id :key id} (str surname ", " name)])]
        [:div.user-fields
         [:div.user-field
          "Name:"
          [:input {:type "text" :value (:name @inputs) :on-input (fn [ev] (swap! inputs assoc :name (-> ev .-target .-value)))}]]
         [:div.user-field
          "Surname:"
          [:input {:type "text" :value (:surname @inputs) :on-input (fn [ev] (swap! inputs assoc :surname (-> ev .-target .-value)))}]]]]
       [:div.actions
        [:button {:disabled (or (string/blank? (:name @inputs)) (string/blank? (:surname @inputs)))
                  :on-click (fn []
                              (create-item (:name @inputs) (:surname @inputs))
                              (swap! inputs assoc :name "")
                              (swap! inputs assoc :surname ""))}
         "Create"]
        [:button {:disabled (or (not (is-selection-valid? @crud-items (:selected @inputs)))
                                (string/blank? (:name @inputs)) (string/blank? (:surname @inputs)))
                  :on-click (fn [] (update-item (:selected @inputs) {:name (:name @inputs) :surname (:surname @inputs)}))}
         "Update"]
        [:button {:disabled (not (is-selection-valid? @crud-items (:selected @inputs)))
                  :on-click (fn [] (delete-item (:selected @inputs)))}
         "Delete"]]])))

