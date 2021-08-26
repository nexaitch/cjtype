(ns cjtype.cangjie
  (:require [clojure.string :refer [starts-with?]]))

(def alphabet "abcdefghijklmnopqrstuvwxyz")
(def cangjie-alphabet "日月金木水火土竹戈十大中一弓人心手口尸廿山女田難卜重")
(def letter? (set alphabet))
(def cangjie? (set cangjie-alphabet))

(def letter->cangjie (merge (zipmap alphabet cangjie-alphabet) (zipmap cangjie-alphabet cangjie-alphabet)))
(def cangjie->letter (merge (zipmap cangjie-alphabet alphabet) (zipmap alphabet alphabet)))
(defn str->cangjie [s] (apply str (map letter->cangjie s)))
(defn cangjie->str [cj] (apply str (map cangjie->letter cj)))

(defn matches? [this that] (= (cangjie->str this) (cangjie->str that)))
(defn prefix-of? [prefix s] (starts-with? (cangjie->str s) (cangjie->str prefix)))
