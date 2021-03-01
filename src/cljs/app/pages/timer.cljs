(ns app.pages.timer
  (:require
   [reagent.core :as r]
   [app.utils :as utils :refer [now]]))

(defn page []
  (let [t-start (r/atom (now))
        t-now (r/atom (now))
        max (r/atom 10000)
        timer (js/setInterval (fn []
                                (reset! t-now (now))
                                (when (> (- @t-now @t-start) @max)
                                  (reset! t-start (- (now) @max)))) 50)]

    (r/create-class {:reagent-render (fn [props]
                                       [:div
                                        [:div#timer-bar.bar
                                         [:div.outer
                                          [:span.elapsed "Elapsed: " (.toFixed (/ (- @t-now @t-start) 1000) 1) "s"]
                                          [:div.inner {:style {:width (str (* 100 (/ (- @t-now @t-start) @max)) "%")}}]]

                                         [:button.reset {:on-click (fn [] (reset! t-start (now)))} "Reset"]]
                                        [:div#control-bar.bar
                                         [:input.max {:type "range" :value @max :min 1000 :max 20000
                                                      :on-change (fn [ev] (reset! max (js/parseInt (-> ev .-target .-value))))}]
                                         [:span "Timer duration: " (.toFixed (/ @max 1000) 1) "s"]]])

                     :component-will-unmount (fn []
                                               (js/clearInterval timer))})))
