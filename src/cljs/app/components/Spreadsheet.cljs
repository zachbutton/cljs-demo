(ns app.components.Spreadsheet
  (:require
   [reagent.core :as r]
   [clojure.string :as string]
   [instaparse.core :as insta]))

;; ;; Keeping this stuff for when parsing formulas
;; 
;; (def operators {:sub (fn [& args] (apply - args))
;;                 :sum (fn [& args] (apply + args))
;;                 :mul (fn [& args] (apply * args))
;;                 :div (fn [& args] (apply / args))})
;; 
;; (def spread-parse
;;   (insta/parser
;;    "F = OP
;;      "))


(def ref-regex #"[A-Z]+[0-9]+")

(def loop-exception {:message "Loop detected in cell dependency chain"})

(defn char-to-coord [char]
  (- (.charCodeAt char 0) 65))

(defn coord-to-char [coord]
  (js/String.fromCharCode (+ coord 64)))

(defn ref-to-coords [ref]
  (let [xs (last (re-matches #"([A-Z]+).*" ref))
        ys (last (re-matches #"[^0-9]+([0-9]+)" ref))]
    [(char-to-coord xs) (- (js/parseInt ys) 1)]))

(defn get-word-types [s]
  (let [words (string/split s #" ")]
    (vec (map (fn [word]
                (if (re-matches ref-regex word)
                  {:type "ref" :coords (ref-to-coords word)}
                  {:type "str" :value word})) words))))

(defn get-refs [word-types]
  (vec (filter (fn [t] (= (:type t) "ref")) word-types)))

(defn create-cell [key] (r/atom {:key key
                                 :value nil
                                 :raw-value nil
                                 :tokens []
                                 :label ""
                                 :dependends-on []
                                 :provides []}))

(defn create-sheet [width height]
  (into (sorted-map)
        (for [x (range width) y (range height)]
          (let [cell (create-cell [x y])]
            {(:key @cell) cell}))))

(defn generate-cell-label [sheet cells-in-chain cell]
  (if (some #(= cell %) cells-in-chain)
    (throw loop-exception)
    (string/join " "
                 (for [token (:tokens @cell)]
                   (if (= (:type token) "str")
                     (:value token)
                     (generate-cell-label sheet (concat cells-in-chain [cell]) (get sheet (:coords token))))))))

(defn update-cell
  ([sheet orig cell] (when (= orig cell) (throw loop-exception))
                     (update-cell sheet orig cell nil))
  ([sheet orig cell raw-value]
   (try
     (js/console.log "Updating cell " (str (:key @cell)))

     (let [tokens (get-word-types (if (nil? raw-value) "" raw-value))
           depends-on (vec (map #(:coords %) (get-refs tokens)))]


       (when (not (nil? raw-value))
         (swap! cell assoc :raw-value raw-value)
         (swap! cell assoc :tokens tokens)
         (doall (for [dep depends-on] (let [dependency (get sheet dep)]
                                        (swap! dependency assoc :provides (concat (:provides @dependency) [(:key @cell)])))))
         (doall (for [prev-dep (:depends-on @cell)] (when (not (some #(= prev-dep %) depends-on))
                                                      (let [provider-cell (get sheet prev-dep)]
                                                        (swap! provider-cell assoc :provides
                                                               (distinct (remove #(= % (:key @cell))
                                                                                 (:provides @provider-cell))))))))

         (swap! cell assoc :depends-on depends-on))

       (doall (for [key (:provides @cell)] (let [cell-to-update (get sheet key)]
                                             (if (= cell cell-to-update)
                                               (when (= orig cell)
                                                 (throw loop-exception))
                                               (update-cell sheet orig cell-to-update)))))

       (swap! cell assoc :label (generate-cell-label sheet [] cell))
       (swap! cell assoc :error false))
     (catch js/Object err
       (js/console.error (:message err))
       (swap! cell assoc :label "ERROR")
       (swap! cell assoc :error true)))))

(defn Cell []
  (fn [props]
    (js/console.log "Re-rendering cell at " (str (:key (deref (:cell props)))))
    [:div {:class ["cell-contents" (when (:error (deref (:cell props))) "error")]}
     [:input {:value (:raw-value (deref (:cell props))) :on-change (:on-change props)}]
     [:span (:label (deref (:cell props)))]]))

(defn component [props]
  (let [sheet (create-sheet (:width props) (:height props))]
    (fn []
      [:div.spreadsheet
       [:table
        [:tbody
         [:tr
          (for [x (range (+ 1 (:width props)))]
            (if (= x 0) [:td.header.top {:key x}]
                [:td.header.top {:key x} (coord-to-char x)]))]
         (for [y (range (:height props))]
           [:tr {:key y}
            [:td.header.side (+ y 1)]
            (for [x (range (:width props))]
              [:td {:key x}
               (let [cell (get sheet [x y])]
                 [Cell {:cell cell
                        :on-change (fn [ev] (update-cell sheet cell cell (-> ev .-target .-value)))}])])])]]])))