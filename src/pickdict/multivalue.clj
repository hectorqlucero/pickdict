(ns pickdict.multivalue
  "Multivalue field parsing and formatting utilities"
  (:require [clojure.string :as str]))

(defn parse-multivalue
  "Parse a multivalue string into a vector using ']' as delimiter"
  [s]
  (if (string? s)
    (str/split s #"\]")
    [s]))

(defn format-multivalue
  "Format a vector into a multivalue string using ']' as delimiter"
  [v]
  (if (sequential? v)
    (str/join "]" (map str v))
    (str v)))

(defn extract-multivalue-numbers
  "Extract numbers from a multivalue string"
  [s]
  (if (string? s)
    (map #(Double/parseDouble %) (str/split s #"\]"))
    [s]))
