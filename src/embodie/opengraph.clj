(ns embodie.opengraph
  (:require [clojure.string :as str]))

(defn tag->map [reduced tag]
  (if-let [attrs (:attrs tag)]
    (assoc reduced (:property attrs) (:content attrs))
    reduced))

(defn has-meta-og
  "Returns true if given meta tag contains open-graph info."
  [meta]
  (when-let [prop (get-in meta [:attrs :property])]
    (str/starts-with? prop "og:")))
