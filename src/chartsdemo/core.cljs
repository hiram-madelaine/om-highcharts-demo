(ns ^:figwheel-always chartsdemo.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [timeout chan put! >! <! alts!]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(def app-state (atom {:svg [[2 3]]}))


#_(defn draw-graph-nv [owner div-id]
  (.addGraph js/nv
             (fn []
               (let [chart (.. js/nv -models discreteBarChart
                               (x #(.-label %))
                               (y #(.-value %))
                               (staggerLabels false)
                               (tooltips false)
                               (id "donut1")
                               (duration 350))
                     data (clj->js [{:key "foo"
                                     :values [{:label "deposits" :value 4000}
                                              {:label "withdrawls" :value -1000}]}])]
                 (.. js/d3 (select (str "#" div-id " svg"))
                     (datum (clj->js data))
                     (call chart)))))
  )

(def chart-config
  {:chart   {:renderTo "chartdiv"}
   :title   {:text  "HighCharts Demo"
             :style {:fontWeight "bold"
                     :fontSize   "25px"}}
   :credits {:enabled false}
   :series  [{:title        "Serie title"
              :id 0
              :showInLegend true
              :name         "Tokyo"
              :data         [7.0, 6.9, 9.5, 14.5, 18.2, 21.5, 25.2, 26.5, 23.3, 18.3, 13.9, 9.6]
              :lineWidth    1
              }
             ]}
  )

(defn build-chart
  [config]
  (js/Highcharts.Chart. (clj->js config)))

#_(defn draw-chart []
  (js/$ (fn []
          (.highcharts (js/$ "#chartdiv")
                       (clj->js (chart-config))))))


(defn graph-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/div #js {:id "chartdiv"}
          (dom/svg nil))
        (dom/div #js {:id "nvd3div"}
          (dom/svg nil))))
    om/IDidMount
    (did-mount [_]
      (let [chart (build-chart chart-config)
            in (chan)]
        (om/set-state! owner :chart chart)
        (go-loop []
                 (let [v (<! in)]
                   (.addPoint (.get chart 0) v true false true))
                 (recur))
        (go-loop []
                 (<! (timeout 200))
                 (>! in (rand-int 20))
                 (recur)))
      #_(draw-graph-nv owner "nvd3div")))
  )


(defn random-color
  []
  (str "#" (-> (.random js/Math)
               (* 16777215)
               (js/Math.floor)
               (.toString 16)))
  ;'#'+Math.floor(Math.random()*16777215).toString(16);
   )

(defn random-size
  []
  (str (rand-int 15) "px"))

(defn draw-rect
  [[x y] owner]
  (om/component
    (let [size (random-size)]
      (dom/rect #js {:width  size
                     :height size
                     :x      x
                     :y      y
                     :fill   (random-color)}))))


(defn draw-circle
  [[x y] owner]
  (om/component
    (let [size (random-size)]
      (dom/circle #js {:r size
                       :cx x
                       :cy y
                       :fill (random-color)}))))

(defn draw-svg
  [app owner]
  (om/component
    (dom/div #js {:id "show"}
             (apply dom/svg #js {:className "svg"
                                 :width  "400px"
                                 :height "400px"}
                    (om/build-all draw-rect (:svg app)))
             (apply dom/svg #js {:className "svg"
                                 :width  "400px"
                                 :height "400px"}
                    (om/build-all draw-circle (:svg app))))))

(om/root
  (fn [data owner]
    (reify
      om/IDidMount
      (did-mount [_]
        (go-loop []
                 (<! (timeout 200))
                 (om/transact! data :svg #(conj % [(rand-int 400) (rand-int 400)]) )
                 (recur)))
      om/IRender
      (render [_]
        (dom/div nil
                 (om/build draw-svg data)
                 (dom/h1 nil (:text data))
                 (om/build graph-view nil)))))
  app-state
  {:target (. js/document (getElementById "app"))})


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

