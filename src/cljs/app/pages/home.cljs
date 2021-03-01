(ns app.pages.home)

(defn page []
  [:div
   [:h1 "Welcome!"]
   [:p "This is a demonstration of 7guis written in ClojureScript, using reagent, re-frame, and shadow-cljs."]
   [:p
    "You can find the source code "
    [:a {:href "https://github.com/zachbutton/cljs-demo" :target "_blank"} "here"]]
   [:p
    "You can find more about 7guis "
    [:a {:href "https://eugenkiss.github.io/7guis/tasks/" :target "_blank"} "here"]]])
