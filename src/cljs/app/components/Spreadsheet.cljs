(ns app.components.Spreadsheet
  (:require
   [reagent.core :as r]
   [clojure.string :as string]
   [cljs.pprint :as pprint :refer [pprint]]
   [instaparse.core :as insta]))

(def operators {:SUB (fn [& args] (apply - args))
                :SUM (fn [& args] (apply + args))
                :MUL (fn [& args] (apply * args))
                :DIV (fn [& args] (apply / args))
                :AVG (fn [& args] (/ (apply + args) (count args)))})

(def ref-regex #"[A-Z]+[0-9]+")

(def loop-exception {:message "Loop detected in cell dependency chain"})
(def formula-exception {:message "Formula resulted in non-number"})

(defn char-to-col [char]
  (- (.charCodeAt char 0) 65))

(defn col-to-char [coord]
  (js/String.fromCharCode (+ coord 65)))

(defn ref-to-key [ref]
  (let [xs (last (re-matches #"([A-Z]+).*" ref))
        ys (last (re-matches #"[^0-9]+([0-9]+)" ref))]
    [(char-to-col xs) (- (js/parseInt ys) 1)]))

(defn key-to-ref [key]
  (str (col-to-char (first key)) (+ 1 (last key))))

(defn get-word-types [s]
  (let [words (string/split s #" ")]
    (vec (map (fn [word]
                (if (re-matches ref-regex word)
                  {:type "ref" :coords (ref-to-key word)}
                  {:type "str" :value word})) words))))

(defn get-refs [word-types]
  (vec (filter (fn [t] (= (:type t) "ref")) word-types)))

(defn create-cell [key] (r/atom {:key key
                                 :id (random-uuid)
                                 :value nil
                                 :raw-value nil
                                 :tokens []
                                 :deps []
                                 :provides []
                                 :stale false}))

(defn get-cell [sheet key]
  (when (nil? (get @sheet key))
    (swap! sheet assoc key (create-cell key)))
  (get @sheet key))

(def parse-cell-formula
  (insta/parser
   "FORMULA = <'='> EXP
   EXP      = OPERATOR <'('> OPERAND+ <')'>
   OPERATOR = 'SUM' | 'SUB' | 'MUL' | 'DIV' | 'AVG'
   OPERAND  = REF | SPAN | NUM | EXP | <' '>
   REF      = #'[A-Z]+[0-9]+'
   NUM      = #'[0-9]+'
   SPAN     = REF <':'> REF
     "
   :output-format :enlive))

(declare calculate-cell-expression)
(declare calculate-cell-value)

(defn calculate-cell-operand [sheet starting-cell operand]
  (case (:tag operand)
    :NUM {:deps [] :val (first (:content operand))}
    :EXP (let [exp-res (calculate-cell-expression sheet starting-cell operand)]
           {:deps (:deps exp-res) :val (:val exp-res)})

    :REF (let [ref-cell (get-cell sheet (ref-to-key (first (:content operand))))]
           (when (= (:key @starting-cell) (:key @ref-cell)) (throw {:message loop-exception}))
           (let [ref-res (if (:stale @ref-cell)
                           (calculate-cell-value sheet starting-cell (get-cell sheet (ref-to-key (first (:content operand)))))
                           {:deps (:deps @ref-cell) :val (:value @ref-cell)})]
             (doall (for [dep (:deps ref-res)]
                      (when (= dep (:key @starting-cell)) (throw {:message loop-exception}))))
             {:deps [(ref-to-key (first (:content operand)))]
              :val (if (nil? (:val ref-res)) js/NaN
                       (if (string/blank? (:val ref-res)) js/NaN
                           (js/Number (:val ref-res))))}))
    nil))

(defn calculate-cell-expression [sheet starting-cell exp]
  (let [exp (reduce (fn [acc node]
                      (case (:tag node)
                        :OPERATOR (assoc acc :op (first (:content node)))
                        :OPERAND (let [res (calculate-cell-operand sheet starting-cell (first (:content node)))
                                       val (:val res)
                                       deps (:deps res)
                                       acc-with-deps (assoc acc :deps (concat (:deps acc) deps))]
                                   (if val (assoc acc-with-deps :terms (concat (:terms acc) [(js/Number val)])) acc-with-deps))
                        acc)) {:terms [] :deps []} (:content exp))


        op-fn (get operators (keyword (:op exp)))]
    {:val (apply op-fn (:terms exp))
     :deps (:deps exp)}))

(defn calculate-cell-value [sheet starting-cell cell]
  (let [parsed-formula (parse-cell-formula (if (nil? (:raw-value @cell)) "" (:raw-value @cell)))
        is-formula (= (:tag parsed-formula) :FORMULA)]
    (if is-formula
      (calculate-cell-expression sheet starting-cell (first (:content parsed-formula)))
      {:deps [] :val (:raw-value @cell)})))

(defn update-cell
  ([sheet cell] (update-cell sheet cell nil))
  ([sheet cell raw-value]
   (try
     (js/console.log "Updating cell " (key-to-ref (:key @cell)))

     (when (not (nil? raw-value))
       (swap! cell assoc :raw-value raw-value))

     (let [res (calculate-cell-value sheet cell cell)
           deps (:deps res)
           value (:val res)]

       ;; Clear self from obsolete deps provides
       (doall (for [key (:deps @cell)]
                (when (not (some #(= key %) deps))
                  (let [pr-cell (get-cell sheet key)]
                    (swap! pr-cell assoc :provides (remove #{key} (:provides pr-cell)))))))

       (swap! cell assoc :deps (vec deps))

       (swap! cell assoc :value value)
       (swap! cell assoc :stale false))

     ;; Apply self to deps provides
     (doall (for [key (:deps @cell)] (let [pr-cell (get-cell sheet key)]
                                       (swap! pr-cell assoc :provides
                                              (distinct
                                               (concat (:provides @pr-cell) [(:key @cell)]))))))


     (doall (for [key (:provides @cell)] (do
                                           (js/console.log "Marking" (key-to-ref key) "stale")
                                           (swap! (get-cell sheet key) assoc :stale true))))
     (swap! cell assoc :error false)

     (catch js/Object err
       (js/console.error (str err))
       (swap! cell assoc :value "ERROR")
       ;;(swap! cell assoc :stale false) ;; Prevent constant recalculations on circular reference
       (swap! cell assoc :error true)))))

(defn Cell []
  (fn [props]
    (let [cell (:cell props)
          on-stale (:on-stale props)]
      (when (:stale @cell) (on-stale))
      [:div {:class ["cell-contents" (when (:error @cell) "error") (when (:state @cell) "stale")] :style (:style props)}
       [:input {:value (:raw-value @cell) :on-change (:on-change props)}]
       [:span (str (:value @cell))]])))

(defn component [props]
  (let [sheet (:sheet props)
        cell-width 125
        cell-height 22
        table-width (+ cell-width (* cell-width (:width props)))
        table-height (+ cell-height (* cell-height (:height props)))
        render-range (r/atom {:x [0 1000] :y [0 1000]})
        scroll-timeout (r/atom nil)] ;; Defaults to 1000x1000px render. Should determine this more intelligently
    (fn []
      [:div.spreadsheet {:on-scroll (fn [ev] (let [elem (-> ev .-target)
                                                   scrollTop (-> elem .-scrollTop)
                                                   scrollLeft (-> elem .-scrollLeft)
                                                   rect (.getBoundingClientRect elem)
                                                   width (-> rect .-width)
                                                   height (-> rect .-height)]
                                               (js/clearTimeout scroll-timeout)
                                               (reset! scroll-timeout
                                                       (js/setTimeout
                                                        (fn []
                                                          (reset! render-range
                                                                  {:x [(- scrollLeft cell-width) (+ scrollLeft width cell-width)]
                                                                   :y [(- scrollTop cell-height) (+ scrollTop height cell-height)]}))
                                                        100))))}
       [:div.spreadsheet-inner {:style {:width (str table-width "px")
                                        :height (str table-height "px")}}

        (doall (for [x (range (- (quot (first (:x @render-range)) cell-width) 8)
                              (+ (quot (last (:x @render-range)) cell-width) 8))
                     y (range (- (quot (first (:y @render-range)) cell-height) 2)
                              (+ (quot (last (:y @render-range)) cell-height) 2))]

                 (when (and (> (:width props) x -2) (> (:height props) y -2))
                   (let [xpos (* (+ x 1) cell-width)
                         ypos (* (+ y 1) cell-height)
                         style {:top (str ypos "px")
                                :left (str xpos "px")
                                :width (str cell-width "px")
                                :height (str cell-height "px")
                                :line-height (str cell-height "px")}]

                     (if (= x -1)
                       [:div.cell-contents.header.left {:style style :key (str x "-" y)} (when (not= y -1) (+ y 1))]
                       (if (= y -1)
                         [:div.cell-contents.header.top {:style style :key (str x "-" y)} (when (not= x -1) (col-to-char x))]
                         (let [cell (get-cell sheet [x y])]
                           [Cell {:cell cell
                                  :key (str x "-" y)
                                  :style style
                                  :on-stale (fn [] (update-cell sheet cell))
                                  :on-change (fn [ev] (update-cell sheet cell (-> ev .-target .-value)))}])))))))]])))