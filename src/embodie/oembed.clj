(ns embodie.oembed
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [net.cgrand.enlive-html :as html]))

(def ^:const oembed-default-providers "https://oembed.com/providers.json")

(def ^:dynamic *oembed-providers* [])

(defn segmentize
  "Returns max 3 last segments of domain, trims preceeding
  wildcards and www prefixes if any."
  [domain]
  (when domain
    (->> (.split domain "\\.")
         (take-last 3)
         (drop-while #(or (= "*" %) (= "www" %))))))

(defn sanitize-url
  "Normalizes given url by lowercasing, removing protocol and returning domain
  name consisting of maximum 3 segments with no wildcards or www segment inside."
  [url]
  (->> url
       (.toLowerCase)
       (re-find #"^https?://([^\/]+)")
       (second)
       (segmentize)
       (str/join ".")))

(defn reduce-schemes
  "Returns a map where each key - a normalized scheme pattern
  is bound to the oembed url and discovery flag. Note that schemes
  are optional, in this case single-entry map will be returned."
  [schemes url discovery name]
  (let [provider {:url url :name name :discovery discovery}]
    (reduce (fn [acc scheme] (assoc acc (sanitize-url scheme) provider))
            {}
            (or schemes [url]))))

(defn reduce-provider
  "Returns a merged map of sanitized schemes, taken for each defined
  endpoint, pointing at corresponding oembed url. This is to make searching
  for a correct provider and its endpoints in more efficient way, as
  it's O(1) based on normalized url."
  [provider]
  (let [endpoints (get provider "endpoints")]
    (reduce #(merge %1 (reduce-schemes
                        (get %2 "schemes")
                        (get %2 "url")
                        (get %2 "discovery")
                        (get provider "provider_name")))
            {}
            endpoints)))

(defn rehash-providers [providers]
  (reduce #(merge %1 (reduce-provider %2)) {} providers))

(defn init-oembed-providers
  "Loads providers from a local vector or external url.
  When no arg provided fetches list of providers from http://oembed.com site."
  ([]
   (init-oembed-providers nil))
  ([location]
   (let [loc (or location oembed-default-providers)]
     (if (vector? loc)
       (rehash-providers loc)
       (->> loc
            (slurp)
            (json/parse-string)
            (rehash-providers))))))

(defn has-link-json-oembed
  "Returns true if given link points at application/json+oembed typed info."
  [link]
  (when-let [prop (get-in link [:attrs :type])]
    (= "application/json+oembed" prop)))

(defn find-oembed-provider [url]
  (get *oembed-providers* (sanitize-url url)))

(defn find-oembed-json-link [res]
  (get-in (->> (html/select res [:head :> :link])
               (filter has-link-json-oembed)
               (first))
          [:attrs :href]))

(defn compose-oembed-link [loc url]
  (str loc "?url=" (java.net.URLEncoder/encode url "UTF-8")))

(defmacro with-oembed-providers
  "Changes default binding to given oembed providers list"
  [providers & body]
  `(binding [*oembed-providers* ~providers] ~@body))
