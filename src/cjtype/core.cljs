(ns cjtype.core
    (:require
      [reagent.core :as r :refer [atom]]
      [reagent.dom :as d]
      [cjtype.cangjie :as cj]
      [cjtype.components :as comp]
      [reagent-material-ui.core.box :refer [box]]
      [reagent-material-ui.core.text-field :refer [text-field]]
      [reagent-material-ui.core.grid :refer [grid]]
      [reagent-material-ui.core.paper :refer [paper]]
      [clojure.string :as str]
      [clojure.core.async :refer [alt! go timeout]]
      [cljs-http.client :as http]))

;; -------------------------
;; Views

(def translate-api-url
  (str "https://ap-southeast-1.aws.webhooks.mongodb-realm.com"
       "/api/client/v2.0/app/cjtype-backend-fpsjk/service/api/incoming_webhook/translate"))

(defn read-translations [result]
  ;;(println result)
  (let [body (:body result)]
    (into {}
          (map #(vector (:char %)
                        (dissoc % :char)))
          body)))

(def sample-text
  (str "倉頡輸入法是一種常用的中文輸入法，"
       "由有「中文電腦之父」美譽的朱邦復先生於1976年創製。\n"
       "初期只有正體中文版本，原名「形意檢字法」，"
       "用以解決電腦處理漢字的問題，"
       "包括漢字輸入、字形輸出、內碼儲存、漢字排序等。\n"
       "朱邦復發明此輸入法時正值他為三軍大學發展中文通訊系統之際，"
       "時任三軍大學校長的蔣緯國為紀念上古時期倉頡造字的精神，"
       "乃於1978年將此輸入法重新定名為「倉頡輸入法」。 "))

(defn home-page []
  (let [text-input   (atom "")
        translations (atom {})
        text-to-type (atom "")
        state        (atom :loading)
        not-found    (atom "")
        index        (atom 0)
        wrong-input  (atom nil)
        config       (atom {:version         :cj5
                            :display-cangjie true
                            :font-size       16})]

    (defn get-conversion-fn []
      (if (:display-cangjie @config)
        cj/str->cangjie
        cj/cangjie->str))

    (defn get-matches []
      (let [c (get @text-to-type @index)
            m (get @translations c)]
        (case (:version @config)
          :cj3  (into #{} (:cj3 m))
          :cj5  (into #{} (:cj5 m))
          :both (into #{} (concat (:cj3 m) (:cj5 m))))))

    (defn get-quick-matches []
      (let [c (get @text-to-type @index)
            m (get @translations c)]
        (:quick m)))

    (defn load-new-text [text]
      (reset! state :loading)
      (go
        (alt!
          (http/get translate-api-url
                    {:query-params {:secret "BdnDVxRT4kGfYCi"
                                    :text   text}})
          ([result]
           (reset! translations (read-translations result))
           (reset! text-to-type text)
           (reset! state :ok))

          (timeout 3000) (reset! text-to-type "connection timed out :<"))))

    (load-new-text sample-text)


    (fn []
      [:span.main
       [:h1 "Cangjie Typing Tool"]
       [grid {:container true :spacing 3 :align-items :stretch}
        [grid {:item true :xs 12}
         [:div {:style {:white-space :nowrap
                        :overflow    :hidden
                        :line-height "1.5em"
                        :font-size   (:font-size @config)}}
          [:span {:style {:color :red}}
           (get @text-to-type @index)]
          (subs @text-to-type (inc @index))]]
        [grid {:container true
               :item      true
               :spacing   3}
         [grid {:item true :xs 12 :md 8}
          [comp/matcher-text-field text-input (get-matches)
           :on-success #(do (reset! wrong-input nil)
                            (swap! index inc)
                            (reset! not-found "")
                            (loop []
                                (when-let [c (get @text-to-type @index)]
                                  (when (not (get @translations c))
                                    (swap! index inc)
                                    (if-not (= "\n" c)
                                            (swap! not-found str c))
                                    (recur)))))

           :on-failure #(reset! wrong-input @text-input)
           :props {:variant    :filled
                   :full-width true
                   :label      "Type Here (Use QWERTY Input)"
                   :disabled   (= :loading @state)}
           :converter (get-conversion-fn)
           :quick-input (get-quick-matches)]
          (when (not-empty @not-found)
            [:div {:style {:color :red}}
             (str "Couldn't find any translations for " \" @not-found \" ", so skipped")])
          (when @wrong-input
            [:span
             [:div {:style {:color :red}}
              "You entered: " ((get-conversion-fn) @wrong-input)]
             [:div "Correct inputs: " (str/join " / "
                                                (map #(str \u201c ;; wrap in smart quotes
                                                           %
                                                           \u201d)
                                                     (concat (map (get-conversion-fn)
                                                                  (get-matches))
                                                             (get-quick-matches))))]])]
         [grid {:item true :xs 12 :md 4}
          [comp/config-menu config load-new-text]]]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
