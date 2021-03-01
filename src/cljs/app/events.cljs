(ns app.events
  (:require [re-frame.core :as re-frame]
            [clojure.string :as string]))

(re-frame/reg-event-db
 ::set-active-page
 (fn [db [_ active-page]]
   (assoc db :active-page active-page)))

(re-frame/reg-event-db
 ::create-crud-item
 (fn [db [_ item]]
   (assoc db :crud-items (assoc (get db :crud-items {}) (str (random-uuid)) item))))

(re-frame/reg-event-db
 ::delete-crud-item
 (fn [db [_ id]]
   (assoc db :crud-items (dissoc (get db :crud-items) id))))

(re-frame/reg-event-db
 ::update-crud-item
 (fn [db [_ id item]]
   (assoc db :crud-items (assoc (get db :crud-items {}) id item))))

(re-frame/reg-event-db
 ::update-crud-filter
 (fn [db [_ crud-filter]]
   (assoc db :crud-filter crud-filter)))

(re-frame/reg-event-db
 ::refresh-crud-order
 (fn [db [_]]
   (assoc db :crud-order
          (map (fn [item] (:id item))
               (sort-by (juxt :surname :name)
                        (filter (fn [{:keys [surname]}]
                                  (or (not (:crud-filter db))
                                      (string/starts-with? (string/lower-case surname) (string/lower-case (:crud-filter db)))))
                                (map (fn [[id item]]
                                       (assoc item :id id)) (:crud-items db))))))))



(re-frame/reg-event-db
 ::create-circle-action
 (fn [db [_ action]]
   (let [curr-actions (vec (if (nil? (:circle-actions db)) [] (:circle-actions db)))
         ptr (if (number? (:circle-step-pointer db)) (:circle-step-pointer db) 0)]
     (assoc db :circle-actions
            (concat (subvec curr-actions 0 (min (count curr-actions) (+ ptr 1))) (vec [action]))))))

(re-frame/reg-event-db
 ::circle-set-step-pointer
 (fn [db [_ loc]]
   (let [curr-actions (vec (if (nil? (:circle-actions db)) [] (:circle-actions db)))
         ptr (if (number? (:circle-step-pointer db)) (:circle-step-pointer db) 0)]
     (assoc db :circle-step-pointer (case loc
                                      :end (- (count (:circle-actions db)) 1)
                                      :undo (max -1 (- ptr 1))
                                      :redo (min (- (count curr-actions) 1) (+ ptr 1))
                                      loc)))))

;;(if (= index :end) (- (count (:circle-actions db)) 1) index)