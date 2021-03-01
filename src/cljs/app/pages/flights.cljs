(ns app.pages.flights
  (:require
   [reagent.core :as r]
   [app.utils :as utils :refer [now]]))

(defn- date-to-ts [date-string]
  (let [ts (js/Date.parse (str date-string " 00:00"))]
    (when (not (js/isNaN ts)) ts)))

(defn- ts-to-date [ts]
  (let [date (js/Date. ts)]
    (str
     (.getFullYear date)
     "-"
     (+ 1 (.getMonth date))
     "-"
     (.getDate date))))

(defn- ts-to-locale-string [ts]
  (let [date (js/Date. ts)]
    (.toLocaleDateString date)))

(defn- date-to-locale-string [date-string]
  (let [ts (date-to-ts date-string)]
    (if (nil? ts) nil (ts-to-locale-string ts))))

(defn- has-negative-date-span? [flight]
  (let [depart-ts (date-to-ts (:depart flight))
        return-ts (date-to-ts (:return flight))]
    (and (= "return" (:type flight))
         (< return-ts depart-ts))))

(defn- can-book? [flight]
  (let [depart-ts (date-to-ts (:depart flight))
        return-ts (date-to-ts (:return flight))]
    (not
     (or
      (nil? depart-ts)
      (< depart-ts (now))

      (has-negative-date-span? flight)

      (and (= "return" (:type flight))
           (or
            (nil? return-ts)
            (< return-ts (now))
            (< return-ts depart-ts)))))))

(defn- confirm-booking [flight]
  (if (= (:type flight) "one-way")
    (js/alert (str "You have booked a one way flight, departing on "
                   (date-to-locale-string (:depart flight))))
    (js/alert (str "You have booked a return flight, departing on "
                   (date-to-locale-string (:depart flight)) " and returning on "
                   (date-to-locale-string (:return flight))))))

(defn- set-flight-type [flight, type]
  (swap! flight assoc :type type))

(defn page []
  (let [second 1000
        minute (* second 60)
        hour (* minute 60)
        day (* hour 24)
        flight (r/atom {:type "one-way"
                        :depart (ts-to-date (+ (now) day))
                        :return (ts-to-date (+ (now) day))})]

    (fn []
      [:div.inputs
       [:div.row
        "Flight type:"
        [:select {:value (:type @flight) :on-change (fn [e]
                                                      (set-flight-type flight (-> e .-target .-value)))}
         [:option {:value "one-way"} "One-way"]
         [:option {:value "return"} "Return"]]]

       [:div.row
        "Departure date:"
        [:input {:class (if (nil? (date-to-ts (:depart @flight))) "error" "")
                 :value (if (nil? (:depart @flight)) "" (:depart @flight))
                 :on-change (fn [e] (swap! flight assoc :depart (-> e .-target .-value)))}]]

       [:div.row
        "Return date:"
        [:input {:class (if (nil? (date-to-ts (:return @flight))) "error" "")
                 :value (if (nil? (:return @flight)) "" (:return @flight))
                 :disabled (= (:type @flight) "one-way")
                 :on-change (fn [e] (swap! flight assoc :return (-> e .-target .-value)))}]]

       [:div.row
        [:button {:disabled (not (can-book? @flight)) :on-click (fn [] (confirm-booking @flight))} "Book"]]

       (if (< (date-to-ts (:depart @flight)) (now))
         [:p.error "Departure date must be in the future"] nil)

       (if (has-negative-date-span? @flight)
         [:p.error "Return date must be after departure date"] nil)])))


