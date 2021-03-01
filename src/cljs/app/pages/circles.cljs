(ns app.pages.circles
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [app.events :as events]))

(rf/reg-sub
 ::circles
 (fn [db _]
   (filter (fn [action] (not (nil? action)))
           (map (fn [action]
                  (if (= (:type action) "place")
                    {:id (:id action) :x (:x action) :y (:y action) :rad 20}
                    nil))

                (subvec (vec (:circle-actions db)) 0
                        (if (nil? (:circle-step-pointer db)) 0 (+ 1 (:circle-step-pointer db))))))))

(rf/reg-sub
 ::circle-sizes
 (fn [db _]
   (reduce (fn [acc action] (assoc acc (:id action) action))
           (map (fn [action]
                  (when (= (:type action) "resize")
                    {:id (:id action) :rad (:rad action)}))

                (subvec (vec (:circle-actions db)) 0
                        (if (nil? (:circle-step-pointer db)) 0 (+ 1 (:circle-step-pointer db))))))))


(rf/reg-sub
 ::can-undo?
 (fn [db _]
   (> (if (nil? (:circle-step-pointer db)) -1 (:circle-step-pointer db))
      -1)))

(rf/reg-sub
 ::can-redo?
 (fn [db _]
   (> (if (nil? (:circle-actions db)) 0 (count (:circle-actions db)))
      (+ (if (nil? (:circle-step-pointer db)) -1 (:circle-step-pointer db)) 1))))

;;(rf/reg-sub
;; ::circles
;; (fn [db _]
;;   (reduce (fn [acc action]
;;             (assoc acc (:id action) action)
;;           (map (fn [action]
;;                  (when (= (:type action) "place")
;;                    {:id (:id action) :x (:x action) :y (:y action) :rad 20}))
;;
;;                (subvec (vec (:circle-actions db)) 0
;;                        (if (nil? (:circle-step-pointer db)) 0 (+ 1 (:circle-step-pointer db)))))))))

(defn resize-circle [id rad]
  (rf/dispatch [::events/create-circle-action {:type "resize" :id id :rad rad}])
  (rf/dispatch [::events/circle-set-step-pointer :end]))

(defn place-circle [x y]
  (rf/dispatch [::events/create-circle-action {:type "place" :x x :y y :id (str (random-uuid))}])
  (rf/dispatch [::events/circle-set-step-pointer :end]))

(defn undo []
  (rf/dispatch [::events/circle-set-step-pointer :undo]))

(defn redo []
  (rf/dispatch [::events/circle-set-step-pointer :redo]))

(defn page []
  (let [circles (rf/subscribe [::circles])
        circle-sizes (rf/subscribe [::circle-sizes])
        can-undo? (rf/subscribe [::can-undo?])
        can-redo? (rf/subscribe [::can-redo?])
        selected-circle (r/atom nil)
        resizing-circle (r/atom nil)]
    (fn []
      [:div
       (when (not (nil? @resizing-circle))
         [:div#resize-dialog

          [:input {:type "range"
                   :min 5
                   :max 100
                   :value (:rad @resizing-circle)
                   :on-change (fn [ev] (swap! resizing-circle assoc :rad (-> ev .-target .-value)))}]

          [:br]
          [:button {:on-click (fn []
                                (resize-circle (:id @resizing-circle) (:rad @resizing-circle))
                                (reset! resizing-circle nil)
                                (reset! selected-circle nil))} "Save"]
          [:button {:on-click (fn []
                                (reset! resizing-circle nil)
                                (reset! selected-circle nil))} "Cancel"]])
       [:div

        [:div.controls
         [:button {:disabled (not @can-undo?)
                   :on-click (fn [] (undo) (reset! selected-circle nil) (reset! resizing-circle nil))} "Undo"]
         [:button {:disabled (not @can-redo?)
                   :on-click (fn [] (redo) (reset! selected-circle nil) (reset! resizing-circle nil))} "Redo"]]

        [:div.canvas-container
         (when (not (nil? @selected-circle))
           [:button.resize-button-box {:style {:left (str (:x @selected-circle) "px")
                                               :top (str (:y @selected-circle) "px")}
                                       :on-click (fn [] (reset! resizing-circle @selected-circle))}
            "Resize"])
         [:svg.canvas {:on-click (fn [ev] (let [rect (.getBoundingClientRect (-> ev .-target))
                                                relX (- (-> ev .-clientX) (-> rect .-left))
                                                relY (- (-> ev .-clientY) (-> rect .-top))]
                                            (place-circle relX relY)
                                            (reset! selected-circle nil)
                                            (reset! resizing-circle nil)))}

          (let [circs @circles]
            (doall
             (for [{:keys [id x y rad]} circs]
               (let [sized-rad
                     (if (or (nil? @resizing-circle) (not= (:id @resizing-circle) id))
                       (if (nil? (:rad (get @circle-sizes id))) rad (:rad (get @circle-sizes id)))
                       (:rad @resizing-circle))]
                 [:circle {:key id
                           :class (if (or (= id (:id @resizing-circle)) (= id (:id @selected-circle))) "selected" "")
                           :cx x
                           :cy y
                           :r sized-rad
                           :on-click (fn [ev]
                                       (.stopPropagation ev)
                                       (reset! resizing-circle nil)
                                       (reset! selected-circle {:id id :x x :y y :rad sized-rad}))}]))))]]]])))

